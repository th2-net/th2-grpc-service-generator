/*
 *  Copyright 2022 Exactpro (Exactpro Systems Limited)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.exactpro.th2.service;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractRetryObserver<R, D extends StreamObserver<R>> implements StreamObserver<R> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRetryObserver.class);
    protected final D delegate;
    protected final RetryPolicy retryPolicy;
    protected final AtomicInteger currentAttempt = new AtomicInteger(0);

    public AbstractRetryObserver(RetryPolicy retryPolicy, D delegate) {
        this.retryPolicy = retryPolicy;
        this.delegate = delegate;
    }

    @Override
    public void onNext(R value) {
        delegate.onNext(value);
    }

    @Override
    public void onError(Throwable t) {
        int attempt = currentAttempt.getAndIncrement();
        if (attempt < retryPolicy.getMaxAttempts() && t instanceof StatusRuntimeException) {

            if (((StatusRuntimeException)t).getStatus() == Status.UNKNOWN) { // Server side thrown an exception
                delegate.onError(t);
            } else {
                LOGGER.warn("Can not send GRPC async request. Retrying. Current attempt = {}", currentAttempt.get() + 1, t);

                try {
                    Thread.sleep(retryPolicy.getDelay(attempt));

                    accept();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    delegate.onError(t);
                }
            }
        } else {
            delegate.onError(t);
        }
    }

    @Override
    public void onCompleted() {
        delegate.onCompleted();
    }

    protected abstract void accept();
}
