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
package com.exactpro.th2.service.generator.protoc.generator.java

import com.exactpro.th2.service.generator.protoc.Generator
import com.exactpro.th2.service.generator.protoc.generator.FileSpec
import com.exactpro.th2.service.generator.protoc.util.javaPackage
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.TypeName
import java.nio.file.Path

abstract class AbstractJavaServiceGenerator : Generator {

    override fun generate(fileDescriptor: FileDescriptorProto, messageNameToJavaPackage: Map<String, String>): List<FileSpec> {
        val javaPackage = fileDescriptor.javaPackage()

        return fileDescriptor.serviceList.let {
            if (it.isNotEmpty()) {
                it.flatMap { service -> generateForService(service, javaPackage, messageNameToJavaPackage) }
            } else emptyList()
        }
    }

    protected abstract fun generateForService(serviceDescriptorProto: ServiceDescriptorProto,
                                              javaPackage: String,
                                              messageNameToJavaPackage: Map<String, String>): List<FileSpec>


    protected fun getBlockingServiceName(protoName: String): String = "${protoName}Service"

    protected fun getAsyncServiceName(protoName: String): String = "Async${protoName}Service"

    protected fun getBlockingDefaultImplName(protoName: String): String = "${protoName}DefaultBlockingImpl"

    protected fun getAsyncDefaultImplName(protoName: String): String = "${protoName}DefaultAsyncImpl"

    protected fun getBlockingStubClassName(javaPackage: String, protoName: String): ClassName = ClassName.get(javaPackage, "${protoName}Grpc", "${protoName}BlockingStub")

    protected fun getAsyncStubClassName(javaPackage: String, protoName: String): ClassName = ClassName.get(javaPackage, "${protoName}Grpc", "${protoName}Stub")

    protected fun createType(protoMessage: String, messageNameToJavaPackage: Map<String, String>): TypeName {
        val name = protoMessage.substringAfterLast('.');
        return ClassName.get(messageNameToJavaPackage.get(name) ?: "", name)
    }

    protected fun createAnnotation(clazz: Class<*>, parameters: Map<String, String>): AnnotationSpec =
        AnnotationSpec.builder(clazz).also {
            parameters.forEach { parameter ->
                it.addMember(parameter.key, CodeBlock.of(parameter.value))
            }
        }.build()

    protected fun createAnnotation(className: ClassName, parameters: Map<String, String>): AnnotationSpec =
        AnnotationSpec.builder(className).also {
            parameters.forEach { parameter ->
                it.addMember(parameter.key, CodeBlock.of(parameter.value))
            }
        }.build()

    protected fun createPathToJavaFile(javaPackage: String, javaClassName: String): String = Path
        .of(
            javaPackage.replace('.', '/'),
            "$javaClassName.java")
        .toString()

    protected fun createPathToJavaFile(rootDir: Path?, javaPackage: String, javaClassName: String): String = rootDir
        ?.resolve(javaPackage.replace('.', '/'))
        ?.resolve("$javaClassName.java")
        ?.toString()
        ?: createPathToJavaFile(javaPackage, javaClassName)

}