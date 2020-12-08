/*
 * Copyright 2020-2020 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exactpro.th2.service;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.StreamObserver;

public abstract class AbstractGrpcService<S extends AbstractStub<S>> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractGrpcService.class);
    private final RetryPolicy retryPolicy;
    private final StubStorage<S> stubStorage;

    public AbstractGrpcService(@NotNull RetryPolicy retryPolicy, @NotNull StubStorage<S> stubStorage) {
        this.retryPolicy = Objects.requireNonNull(retryPolicy, "Retry policy can not be null");
        this.stubStorage = Objects.requireNonNull(stubStorage, "Service configuration can not be null");
    }

    protected <T> T createBlockingRequest(Supplier<T> method) {

        RuntimeException exception = new RuntimeException("Can not execute GRPC blocking request");

        for (int i = 0; i < retryPolicy.getMaxAttempts(); i++) {
            try {
                return method.get();
            } catch (StatusRuntimeException e) {
                exception.addSuppressed(e);
                logger.warn("Can not send GRPC blocking request. Retrying. Current attempt = {}", i + 1, e);
            } catch (Exception e) {
                exception.addSuppressed(e);
                throw exception;
            }

            try {
                Thread.sleep(retryPolicy.getDelay(i));
            } catch (InterruptedException e) {
                exception.addSuppressed(e);
                throw exception;
            }
        }

        throw exception;
    }

    protected <T> void createAsyncRequest(StreamObserver<T> observer,  Consumer<StreamObserver<T>> method) {
        method.accept(new RetryStreamObserver<>(observer, method));
    }

    protected abstract S createStub(Channel channel, CallOptions callOptions);

    protected S getStub(Message message) {
        return stubStorage.getStub(message, this::createStub);
    }

    private class RetryStreamObserver<T> implements StreamObserver<T> {

        private final StreamObserver<T> delegate;
        private final Consumer<StreamObserver<T>> method;
        private final AtomicInteger currentAttempt = new AtomicInteger(0);

        public RetryStreamObserver(StreamObserver<T> delegate, Consumer<StreamObserver<T>> method) {
            this.delegate = delegate;
            this.method = method;
        }

        @Override
        public void onNext(T value) {
            delegate.onNext(value);
        }

        @Override
        public void onError(Throwable t) {
            int attempt = currentAttempt.getAndIncrement();
            if (attempt < retryPolicy.getMaxAttempts() && t instanceof StatusRuntimeException) {

                logger.warn("Can not send GRPC async request. Retrying. Current attempt = {}", currentAttempt.get() + 1, t);

                try {
                    Thread.sleep(retryPolicy.getDelay(attempt));

                    method.accept(this);
                } catch (InterruptedException e) {
                    delegate.onError(t);
                }
            } else {
                delegate.onError(t);
            }
        }

        @Override
        public void onCompleted() {
            delegate.onCompleted();
        }
    }

}
