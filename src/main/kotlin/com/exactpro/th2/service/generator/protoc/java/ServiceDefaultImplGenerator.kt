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
package com.exactpro.th2.service.generator.protoc.java

import com.exactpro.th2.service.AbstractGrpcService
import com.exactpro.th2.service.RetryPolicy
import com.exactpro.th2.service.StubStorage
import com.exactpro.th2.service.generator.protoc.FileSpec
import com.exactpro.th2.service.generator.protoc.Generator
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
            generateBlockingDefaultImpl(service, javaPackage, messageNameToJavaPackage),
            generateAsyncDefaultImpl(service, javaPackage, messageNameToJavaPackage)
        )
    }

    private fun generateBlockingDefaultImpl(service: ServiceDescriptorProto, javaPackage: String, messageNameToJavaPackage: Map<String, String>): FileSpec {
        val javaFile = service.methodList.map {
            MethodSpec.methodBuilder(it.name.decapitalize())
                .addModifiers(PUBLIC)
                .returns(createType(it.outputType, messageNameToJavaPackage))
                .addParameter(createType(it.inputType, messageNameToJavaPackage), "input")
                .addCode("""
                    ${getBlockingStubClassName(javaPackage, service.name)} stub = getStub(input);
                    return createBlockingRequest(() -> stub.${it.name.decapitalize()}(input));
                """.trimIndent())
                .build()
        }.let {
            val name = service.name
            createClass(getBlockingDefaultImplName(name), ClassName.get(javaPackage, getBlockingServiceName(name)), javaPackage, name, true, it)
        }.let {
            JavaFile.builder(javaPackage, it).build()
        }

        return JavaFileSpec(createPathToJavaFile(rootPath, javaPackage, javaFile.typeSpec.name), javaFile)
    }

    private fun generateAsyncDefaultImpl(service: ServiceDescriptorProto, javaPackage: String, messageNameToJavaPackage: Map<String, String>) : FileSpec {
        val javaFile = service.methodList.map {
            MethodSpec.methodBuilder(it.name.decapitalize())
                .addModifiers(PUBLIC)
                .returns(TypeName.VOID)
                .addParameter(createType(it.inputType, messageNameToJavaPackage), "input")
                .addParameter(ParameterizedTypeName.get(ClassName.get(StreamObserver::class.java), createType(it.outputType, messageNameToJavaPackage)), "observer")
                .addCode("""
                    ${getAsyncStubClassName(javaPackage, service.name)} stub = getStub(input);
                    createAsyncRequest(observer, (newObserver) -> stub.${it.name.decapitalize()}(input, newObserver));
                """.trimIndent())
                .build()
        }.let {
            val name = service.name
            createClass(getAsyncDefaultImplName(name), ClassName.get(javaPackage, getAsyncServiceName(name)), javaPackage, name, false, it)
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
            .addMethod(MethodSpec
                .constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(RetryPolicy::class.java, "retryConfiguration")
                .addParameter(ParameterizedTypeName.get(ClassName.get(StubStorage::class.java), stubClassName), "serviceConfiguration")
                .addCode("""
                            super(retryConfiguration, serviceConfiguration);
                        """.trimIndent())
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