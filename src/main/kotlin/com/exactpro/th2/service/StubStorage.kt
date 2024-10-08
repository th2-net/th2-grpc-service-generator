/*
 * Copyright 2020-2024 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.th2.service

import com.google.protobuf.Message
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.stub.AbstractStub
import io.grpc.stub.AbstractStub.StubFactory

interface StubStorage<T : AbstractStub<T>> {

    fun getStub(message: Message, stubFactory: StubFactory<T>) : T

    fun getStub(message: Message, stubFactory: StubFactory<T>, properties: Map<String, String>) : T {
        if (properties.isNotEmpty()) {
            LOGGER.warn { "gRPC routing is unsupported, $properties are ignored" }
        }
        return getStub(message, stubFactory)
    }

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }
}