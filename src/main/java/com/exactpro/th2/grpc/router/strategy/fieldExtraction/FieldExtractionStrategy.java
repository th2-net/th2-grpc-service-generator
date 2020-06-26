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
package com.exactpro.th2.grpc.router.strategy.fieldExtraction;

import com.google.protobuf.Message;

import java.util.Map;

/**
 * An interface describing a method {@link #getFields(Message)}
 * for extracting fields from a message
 */
public interface FieldExtractionStrategy {

    /**
     * Converts message fields to {@code Map<String,String>}
     *
     * @param message the message from which the fields will be extracted
     * @return {@code Map<String,String>} containing {@code message} fields
     */
    Map<String, String> getFields(Message message);

}
