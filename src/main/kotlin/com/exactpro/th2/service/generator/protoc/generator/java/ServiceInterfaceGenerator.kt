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
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import io.grpc.stub.StreamObserver
import java.nio.file.Path
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
            generateBlockingInterface(service, javaPackage, messageNameToJavaPackage),
            generateAsyncInterface(service, javaPackage, messageNameToJavaPackage)
        )
    }

    private fun generateBlockingInterface(service: ServiceDescriptorProto, javaPackage: String, messageNameToJavaPackage: Map<String, String>) : FileSpec {
        val javaFile = service.methodList.map {
            MethodSpec.methodBuilder(it.name)
                .addModifiers(PUBLIC, ABSTRACT)
                .returns(createType(it.outputType, messageNameToJavaPackage))
                .addParameter(createType(it.inputType, messageNameToJavaPackage), "input")
                .build()
        }.let {
            val name = service.name
            createInterface(getBlockingServiceName(name), it)
        }.let {
            JavaFile.builder(javaPackage, it).build()
        }

        return JavaFileSpec(createPathToJavaFile(rootPath, javaPackage, javaFile.typeSpec.name), javaFile)
    }

    private fun generateAsyncInterface(service: ServiceDescriptorProto, javaPackage: String, messageNameToJavaPackage: Map<String, String>) : FileSpec {
        val javaFile = service.methodList.map {
            MethodSpec.methodBuilder(it.name)
                .addModifiers(PUBLIC, ABSTRACT)
                .returns(TypeName.VOID)
                .addParameter(createType(it.inputType, messageNameToJavaPackage), "input")
                .addParameter(ParameterizedTypeName.get(ClassName.get(StreamObserver::class.java), createType(it.outputType, messageNameToJavaPackage)), "observer")
                .build()
        }.let {
            val name = service.name
            createInterface(getAsyncServiceName(name), it)
        }.let {
            JavaFile.builder(javaPackage, it).build()
        }

        return JavaFileSpec(createPathToJavaFile(rootPath, javaPackage, javaFile.typeSpec.name), javaFile)
    }

    private fun createInterface(javaName: String, methods: List<MethodSpec>) =
        TypeSpec
            .interfaceBuilder(javaName)
            .addModifiers(PUBLIC)
            .addMethods(methods)
            .build()
}