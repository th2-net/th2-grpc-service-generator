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

package com.exactpro.th2.service.generator.protoc.java

import com.exactpro.th2.service.generator.protoc.FileSpec
import com.exactpro.th2.service.generator.protoc.Generator
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import io.grpc.stub.StreamObserver
import java.nio.file.Path
import java.util.Locale
import java.util.Properties
import javax.lang.model.element.Modifier.ABSTRACT
import javax.lang.model.element.Modifier.PUBLIC

class ServiceInterfaceGenerator : AbstractJavaServiceGenerator(), Generator {

    companion object {
        private const val ROOT_PATH_OPTION_NAME = "javaInterfacesPath"
    }

    private var rootPath: Path? = null

    override fun init(prop: Properties) {
        super.init(prop)
        rootPath = prop.getProperty(ROOT_PATH_OPTION_NAME)?.let { Path.of(it) }
    }

    override fun generateForService(service: ServiceDescriptorProto, javaPackage: String, messageNameToJavaPackage: Map<String, String>): List<FileSpec> {
        return listOf(
            generateInterface(service, false, javaPackage, messageNameToJavaPackage),
            generateInterface(service, true, javaPackage, messageNameToJavaPackage)
        )
    }

    private fun generateMethod(
        method: DescriptorProtos.MethodDescriptorProto,
        messageNameToJavaPackage: Map<String, String>,
        async: Boolean,
        withFilter: Boolean
    ): MethodSpec {
        val methodName = method.name.replaceFirstChar { it.lowercase(Locale.getDefault()) }
        return with(MethodSpec.methodBuilder(methodName).addModifiers(PUBLIC, ABSTRACT)) {
            returns(if (async) {
                TypeName.VOID
            } else {
                wrapStreaming(createType(method.outputType, messageNameToJavaPackage), method)
            })

            addParameter(createType(method.inputType, messageNameToJavaPackage), "input")

            if (withFilter) {
                addParameter(ParameterizedTypeName.get(ClassName.get(Map::class.java), ClassName.get(String::class.java), ClassName.get(String::class.java)), "properties")
            }

            if (async) {
                addParameter(ParameterizedTypeName.get(ClassName.get(StreamObserver::class.java), createType(method.outputType, messageNameToJavaPackage)), "observer")
            }

            build()
        }
    }

    private fun generateInterface(service: ServiceDescriptorProto, async: Boolean, javaPackage: String, messageNameToJavaPackage: Map<String, String>) : FileSpec {
        val javaFile = service.methodList.flatMap {
            listOf(
                generateMethod(it, messageNameToJavaPackage, async, withFilter = false),
                generateMethod(it, messageNameToJavaPackage, async, withFilter = true)
            )
        }.let {
            val javaName = (if (async) ::getAsyncServiceName else ::getBlockingServiceName)(service.name)
            TypeSpec.interfaceBuilder(javaName)
                .addModifiers(PUBLIC)
                .addMethods(it)
                .build()
        }.let {
            JavaFile.builder(javaPackage, it).build()
        }

        return JavaFileSpec(createPathToJavaFile(rootPath, javaPackage, javaFile.typeSpec.name), javaFile)
    }
}