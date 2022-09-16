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

import io.grpc.stub.StreamObserver;

import java.util.function.Consumer;

public class RetryStreamObserver<T>
        extends AbstractRetryObserver<T, StreamObserver<T>>
        implements StreamObserver<T> {
    private final Consumer<StreamObserver<T>> method;

    public RetryStreamObserver(RetryPolicy retryPolicy, StreamObserver<T> delegate, Consumer<StreamObserver<T>> method) {
        super(retryPolicy, delegate);
        this.method = method;
    }

    @Override
    protected void accept() {
        method.accept(this);
    }
}
