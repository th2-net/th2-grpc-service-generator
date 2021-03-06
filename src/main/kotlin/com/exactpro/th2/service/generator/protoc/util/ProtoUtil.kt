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
package com.exactpro.th2.service.generator.protoc.util

import com.google.protobuf.DescriptorProtos
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.squareup.javapoet.ClassName

fun FileDescriptorProto.javaPackage() : String {
    val javaPackage = if (this.options.hasJavaPackage()) this.options.javaPackage else this.`package`
    return if (this.options.javaMultipleFiles) {
        javaPackage
    } else {
        ClassName.get(javaPackage, if (this.options.hasJavaOuterClassname()) this.options.javaOuterClassname else this.name).toString()
    }
}

/**
 * Returns the full message name in format:
 *
 * + `package.msg_name` - if the file has package
 * + `msg_name` - if the file does not have package
 */
fun FileDescriptorProto.fullNameFor(message: DescriptorProtos.DescriptorProto): String {
    return (if (hasPackage()) "${`package`}." else "") + message.name
}