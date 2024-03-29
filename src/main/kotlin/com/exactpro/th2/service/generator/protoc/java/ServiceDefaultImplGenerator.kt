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

import com.exactpro.th2.service.AbstractGrpcService
import com.exactpro.th2.service.RetryPolicy
import com.exactpro.th2.service.StubStorage
import com.exactpro.th2.service.generator.protoc.FileSpec
import com.exactpro.th2.service.generator.protoc.Generator
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.stub.StreamObserver
import java.nio.file.Path
import java.util.Locale
import java.util.Properties
import javax.lang.model.element.Modifier.PROTECTED
import javax.lang.model.element.Modifier.PUBLIC

class ServiceDefaultImplGenerator : AbstractJavaServiceGenerator(), Generator {

    companion object {
        private const val ROOT_PATH_OPTION_NAME = "javaInterfacesImplPath"
    }

    private var rootPath: Path? = null

    override fun init(prop: Properties) {
        super.init(prop)
        rootPath = prop.getProperty(ROOT_PATH_OPTION_NAME)?.let { Path.of(it) }
    }

    override fun generateForService(service: ServiceDescriptorProto, javaPackage: String, messageNameToJavaPackage: Map<String, String>): List<FileSpec> {
        return listOf(
            generateDefaultImpl(service, false, javaPackage, messageNameToJavaPackage),
            generateDefaultImpl(service, true, javaPackage, messageNameToJavaPackage)
        )
    }

    private fun generateMethod(
        serviceName: String,
        javaPackage: String,
        method: DescriptorProtos.MethodDescriptorProto,
        messageNameToJavaPackage: Map<String, String>,
        async: Boolean,
        withFilter: Boolean
    ): MethodSpec {
        val methodName = method.name.replaceFirstChar { it.lowercase(Locale.getDefault()) }
        return with(MethodSpec.methodBuilder(methodName)) {
            addModifiers(PUBLIC)
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

            if (withFilter) {
                val stubClassName =
                    (if (async) ::getAsyncStubClassName else ::getBlockingStubClassName)(javaPackage, serviceName)
                addCode("$stubClassName stub = getStub(input, properties);\n")
                addCode(
                    if (async) {
                        "createAsyncRequest(observer, (newObserver) -> stub.$methodName(input, newObserver));"
                    } else {
                        if (method.serverStreaming) {
                            "return createBlockingServerStreamingRequest(() -> stub.$methodName(input));"
                        } else {
                            "return createBlockingRequest(() -> stub.$methodName(input));"
                        }
                    }
                )
            } else {
                if (!async) addCode("return ")
                addCode("$methodName(input, java.util.Collections.EMPTY_MAP${if (async) ", observer" else ""});")
            }
            build()
        }
    }

    private fun generateDefaultImpl(service: ServiceDescriptorProto, async: Boolean, javaPackage: String, messageNameToJavaPackage: Map<String, String>) : FileSpec {
        val javaFile = service.methodList.flatMap {
            listOf(
                generateMethod(service.name, javaPackage, it, messageNameToJavaPackage, async, true),
                generateMethod(service.name, javaPackage, it, messageNameToJavaPackage, async, false)
            )
        }.let {
            val className = (if (async) ::getAsyncDefaultImplName else ::getBlockingDefaultImplName)(service.name)
            val interfaceName = (if (async) ::getAsyncServiceName else ::getBlockingServiceName)(service.name)
            val name = service.name
            createClass(className, ClassName.get(javaPackage, interfaceName), javaPackage, name, !async, it)
        }.let {
            JavaFile.builder(javaPackage, it).build()
        }

        return JavaFileSpec(createPathToJavaFile(rootPath, javaPackage, javaFile.typeSpec.name), javaFile)
    }

    private fun createClass(javaClassName: String, interfaceClassName: ClassName, javaPackage: String, protoName: String, blocking: Boolean, methods: List<MethodSpec>) : TypeSpec {
        val stubClassName = if (blocking) getBlockingStubClassName(javaPackage, protoName) else getAsyncStubClassName(javaPackage, protoName)

        return TypeSpec
            .classBuilder(javaClassName)
            .superclass(ParameterizedTypeName.get(ClassName.get(AbstractGrpcService::class.java), stubClassName))
            .addSuperinterface(interfaceClassName)
            .addModifiers(PUBLIC)
            .addMethods(methods)
            .addMethod(createCreateStubMethod(javaPackage, protoName, blocking))
            //FIXME: Added default constructor for service loader
            .addMethod(MethodSpec.constructorBuilder().addModifiers(PUBLIC).addCode("super();\n").build())
            .addMethod(MethodSpec
                .constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(RetryPolicy::class.java, "retryConfiguration")
                .addParameter(ParameterizedTypeName.get(ClassName.get(StubStorage::class.java), stubClassName), "serviceConfiguration")
                .addCode("super(retryConfiguration, serviceConfiguration);")
                .build()
            )
            .build()
    }

    private fun createCreateStubMethod(javaPackage: String, protoName: String, blocking: Boolean) = MethodSpec
        .methodBuilder("createStub")
        .returns(if (blocking) getBlockingStubClassName(javaPackage, protoName) else getAsyncStubClassName(javaPackage, protoName))
        .addAnnotation(Override::class.java)
        .addParameter(Channel::class.java, "channel")
        .addParameter(CallOptions::class.java, "callOptions")
        .addModifiers(PROTECTED)
        .addCode(CodeBlock.of("return ${ClassName.get(javaPackage, "${protoName}Grpc")}.${if (blocking) "newBlockingStub" else "newStub"}(channel);"))
        .build()
}