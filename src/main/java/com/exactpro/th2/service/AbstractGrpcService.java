/*
 * Copyright 2020-2022 Exactpro (Exactpro Systems Limited)
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

import com.google.protobuf.Message;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;

public abstract class AbstractGrpcService<S extends AbstractStub<S>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGrpcService.class);
    private final RetryPolicy retryPolicy;
    private final StubStorage<S> stubStorage;

    public AbstractGrpcService() {
        retryPolicy = null;
        stubStorage = null;
    }

    public AbstractGrpcService(@NotNull RetryPolicy retryPolicy, @NotNull StubStorage<S> stubStorage) {
        this.retryPolicy = Objects.requireNonNull(retryPolicy, "Retry policy can not be null");
        this.stubStorage = Objects.requireNonNull(stubStorage, "Service configuration can not be null");
    }

    protected <T> T createBlockingRequest(Supplier<T> method) {

        if (retryPolicy == null || stubStorage == null) {
            throw new IllegalStateException("Not yet init");
        }

        RuntimeException exception = new RuntimeException("Can not execute GRPC blocking request");

        for (int i = 0; i < retryPolicy.getMaxAttempts(); i++) {
            try {
                return method.get();
            } catch (StatusRuntimeException e) {
                exception.addSuppressed(e);
                if (e.getStatus() == Status.UNKNOWN) { // Server side thrown an exception
                    throw exception;
                }
                LOGGER.warn("Can not send GRPC blocking request. Retrying. Current attempt = {}", i + 1, e);
            } catch (Exception e) {
                exception.addSuppressed(e);
                throw exception;
            }

            try {
                Thread.sleep(retryPolicy.getDelay(i));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                exception.addSuppressed(e);
                throw exception;
            }
        }

        throw exception;
    }

    protected <T> void createAsyncRequest(StreamObserver<T> observer,  Consumer<StreamObserver<T>> method) {
        if (retryPolicy == null || stubStorage == null) {
            throw new IllegalStateException("Not yet init");
        }

        if (observer instanceof ClientResponseObserver) {
            method.accept(new RetryClientResponseObserver(retryPolicy, (ClientResponseObserver<?, T>) observer, method));
        } else {
            method.accept(new RetryStreamObserver<>(retryPolicy, observer, method));
        }
    }

    protected abstract S createStub(Channel channel, CallOptions callOptions);

    @Deprecated(since = "3.2.0", forRemoval = false)
    // This method is left for backward compatibility
    protected S getStub(Message message) {
        return getStub(message, emptyMap());
    }

    protected S getStub(Message message, Map<String, String> properties) {
        return stubStorage.getStub(message, this::createStub, properties);
    }
}