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
package com.exactpro.th2.service.generator.protoc.run

import com.exactpro.th2.service.generator.protoc.Generator
import com.exactpro.th2.service.generator.protoc.util.fullNameFor
import com.exactpro.th2.service.generator.protoc.util.javaPackage
import com.google.protobuf.compiler.PluginProtos
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Properties
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse

class PluginRun(private val generators: List<Generator>) {

    fun generateResponse(inputStream: InputStream, outputStream: OutputStream) {

        val generatorRequest = try {
            inputStream.buffered().use {
                PluginProtos.CodeGeneratorRequest.parseFrom(it)
            }
        } catch (failure: Exception) {
            throw IOException("Attempted to run proto extension generator as protoc plugin, but could not read CodeGeneratorRequest.", failure)
        }

        val prop = Properties()

        generatorRequest.parameter.split(',').forEach {
            val index = it.indexOf('=')
            if (index > 0) {
                prop.setProperty(it.substring(0, index), it.substring(index + 1, it.length))
            }
        }

        val builder = PluginProtos.CodeGeneratorResponse.newBuilder()

        val descriptorMap = generatorRequest.protoFileList.map { it.name to it }.toMap()

        val messageNameToPackage = HashMap<String, String>()

        generatorRequest.protoFileList.forEach { file ->
            val javaPackage = file.javaPackage()
            file.messageTypeList.forEach { message ->
                val fullMessageName = file.fullNameFor(message)
                messageNameToPackage[fullMessageName] = javaPackage
            }
        }

        generatorRequest.fileToGenerateList.map(descriptorMap::getValue).forEach { descriptor ->
            generators.forEach {
                it.init(prop)
                it.generate(descriptor, messageNameToPackage).forEach { fileSpec ->
                    builder.addFile(PluginProtos.CodeGeneratorResponse.File.newBuilder().also {
                        it.name = fileSpec.getFilePath()
                        it.content = fileSpec.getContent()
                    })
                }
            }
        }

        val buffered = outputStream.buffered()
        builder.supportedFeatures = CodeGeneratorResponse.Feature.FEATURE_PROTO3_OPTIONAL_VALUE.toLong()
        builder.build().writeTo(buffered)
        buffered.flush()
    }

}
