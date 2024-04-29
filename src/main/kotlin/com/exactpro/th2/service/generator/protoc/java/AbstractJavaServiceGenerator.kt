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

package com.exactpro.th2.service.generator.protoc.java

import com.exactpro.th2.service.generator.protoc.FileSpec
import com.exactpro.th2.service.generator.protoc.Generator
import com.exactpro.th2.service.generator.protoc.util.javaPackage
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import java.nio.file.Path
import java.util.Properties

abstract class AbstractJavaServiceGenerator : Generator {
    private var enableGeneration = true

    override fun init(prop: Properties) {
        prop.getProperty(ENABLE_JAVA_GENERATION_OPTION_NAME)?.also {
            enableGeneration = it.toBoolean()
        }
    }

    override fun generate(fileDescriptor: FileDescriptorProto, messageNameToJavaPackage: Map<String, String>): List<FileSpec> =
        if (enableGeneration) {
            val javaPackage = fileDescriptor.javaPackage()
            fileDescriptor.serviceList.flatMap {
                    service -> generateForService(service, javaPackage, messageNameToJavaPackage)
            }
        } else {
            emptyList()
        }

    protected abstract fun generateForService(service: ServiceDescriptorProto,
                                              javaPackage: String,
                                              messageNameToJavaPackage: Map<String, String>): List<FileSpec>

    protected fun getBlockingServiceName(protoName: String): String = protoName + if (protoName.endsWith("Service")) "" else "Service"
    protected fun getAsyncServiceName(protoName: String): String = "Async" + getBlockingServiceName(protoName)
    protected fun getBlockingDefaultImplName(protoName: String): String = "${protoName}DefaultBlockingImpl"
    protected fun getAsyncDefaultImplName(protoName: String): String = "${protoName}DefaultAsyncImpl"
    protected fun getBlockingStubClassName(javaPackage: String, protoName: String): ClassName = ClassName.get(javaPackage, "${protoName}Grpc", "${protoName}BlockingStub")
    protected fun getAsyncStubClassName(javaPackage: String, protoName: String): ClassName = ClassName.get(javaPackage, "${protoName}Grpc", "${protoName}Stub")

    /**
     * The [protoMessage] has the following format: `.package.message_name`
     */
    protected fun createType(protoMessage: String, messageNameToJavaPackage: Map<String, String>): TypeName {
        val name = protoMessage.substringAfterLast('.')
        val fullName = protoMessage.trimStart('.')
        return ClassName.get(messageNameToJavaPackage.getOrDefault(fullName, ""), name)
    }

    protected fun wrapStreaming(type: TypeName, methodDescriptorProto: DescriptorProtos.MethodDescriptorProto): TypeName {
        return if (methodDescriptorProto.serverStreaming) {
            ParameterizedTypeName.get(ClassName.get(Iterator::class.java), type)
        } else {
            type
        }
    }

    private fun createPathToJavaFile(javaPackage: String, javaClassName: String): String = Path
        .of(
            javaPackage.replace('.', '/'),
            "$javaClassName.java")
        .toString()

    protected fun createPathToJavaFile(rootDir: Path?, javaPackage: String, javaClassName: String): String = rootDir
        ?.resolve(javaPackage.replace('.', '/'))
        ?.resolve("$javaClassName.java")
        ?.toString()
        ?: createPathToJavaFile(javaPackage, javaClassName)

    companion object {
        private const val ENABLE_JAVA_GENERATION_OPTION_NAME = "enableJava"
    }
}