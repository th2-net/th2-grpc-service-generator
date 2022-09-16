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

import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;

import java.util.function.Consumer;

public class RetryClientResponseObserver<ReqT, RespT>
        extends AbstractRetryObserver<RespT, ClientResponseObserver<ReqT, RespT>>
        implements ClientResponseObserver<ReqT, RespT> {
    private final Consumer<ClientResponseObserver<ReqT, RespT>> method;


    public RetryClientResponseObserver(RetryPolicy retryPolicy, ClientResponseObserver<ReqT, RespT> delegate, Consumer<ClientResponseObserver<ReqT, RespT>> method) {
        super(retryPolicy, delegate);
        this.method = method;
    }

    @Override
    public void beforeStart(ClientCallStreamObserver<ReqT> requestStream) {
        delegate.beforeStart(requestStream);
    }

    @Override
    protected void accept() {
        method.accept(this);
    }
}