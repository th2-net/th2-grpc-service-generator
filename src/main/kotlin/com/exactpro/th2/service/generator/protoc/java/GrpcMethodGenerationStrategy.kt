/*
 *  Copyright 2022 Exactpro (Exactpro Systems Limited)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.exactpro.th2.service.generator.protoc.java

import com.google.protobuf.Any
import com.google.protobuf.DescriptorProtos
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import io.grpc.stub.StreamObserver
import java.util.*
import javax.lang.model.element.Modifier.ABSTRACT
import javax.lang.model.element.Modifier.PUBLIC

@Suppress("unused")
internal enum class GrpcMethodGenerationStrategy(
    val sync: Boolean,
    val clientStreaming: Boolean,
    val serverStreaming: Boolean,
) {
    SYNC(true, false, false) {
        override fun MethodSpec.Builder.generateDeclaration(
            methodName: String,
            method: DescriptorProtos.MethodDescriptorProto,
            messageNameToJavaPackage: Map<String, String>,
            withFilter: Boolean
        ) {
            syncReturnType(method, messageNameToJavaPackage)
            inputParameter(method, messageNameToJavaPackage)
            propertyParameter(withFilter)
        }

        override fun MethodSpec.Builder.generateCode(
            serviceName: String,
            javaPackage: String,
            methodName: String,
            method: DescriptorProtos.MethodDescriptorProto,
            messageNameToJavaPackage: Map<String, String>,
            withFilter: Boolean
        ) {
            if (withFilter) {
                syncCodeWithFilters(javaPackage, serviceName, methodName)
            } else {
                syncCodeWithoutFilters(methodName)
            }
        }
    },
    SYNC_CLIENT_STREAMING(true, true, false) {
        override fun MethodSpec.Builder.generateDeclaration(
            methodName: String,
            method: DescriptorProtos.MethodDescriptorProto,
            messageNameToJavaPackage: Map<String, String>,
            withFilter: Boolean
        ) {
            syncReturnType(method, messageNameToJavaPackage)
            propertyParameter(withFilter)
        }

        override fun MethodSpec.Builder.generateCode(
            serviceName: String,
            javaPackage: String,
            methodName: String,
            method: DescriptorProtos.MethodDescriptorProto,
            messageNameToJavaPackage: Map<String, String>,
            withFilter: Boolean
        ) {
            if (withFilter) {
                syncCodeWithFilters(javaPackage, serviceName, methodName)
            } else {
                syncCodeWithoutFilters(methodName)
            }
        }
    },
    SYNC_SERVER_STREAMING(true, false, true) {
        override fun MethodSpec.Builder.generateDeclaration(
            methodName: String,
            method: DescriptorProtos.MethodDescriptorProto,
            messageNameToJavaPackage: Map<String, String>,
            withFilter: Boolean
        ) {
            syncStreamingReturnType(method, messageNameToJavaPackage)
            inputParameter(method, messageNameToJavaPackage)
            propertyParameter(withFilter)

        }

        override fun MethodSpec.Builder.generateCode(
            serviceName: String,
            javaPackage: String,
            methodName: String,
            method: DescriptorProtos.MethodDescriptorProto,
            messageNameToJavaPackage: Map<String, String>,
            withFilter: Boolean
        ) {
            if (withFilter) {
                syncCodeWithFilters(javaPackage, serviceName, methodName)
            } else {
                syncCodeWithoutFilters(methodName)
            }
        }
    },
    SYNC_BIDIRECTIONAL_STREAMING(true, true, true) {
        override fun MethodSpec.Builder.generateDeclaration(
            methodName: String,
            method: DescriptorProtos.MethodDescriptorProto,
            messageNameToJavaPackage: Map<String, String>,
            withFilter: Boolean
        ) {
            syncStreamingReturnType(method, messageNameToJavaPackage)
            propertyParameter(withFilter)
        }

        override fun MethodSpec.Builder.generateCode(
            serviceName: String,
            javaPackage: String,
            methodName: String,
            method: DescriptorProtos.MethodDescriptorProto,
            messageNameToJavaPackage: Map<String, String>,
            withFilter: Boolean
        ) {
            if (withFilter) {
                syncCodeWithFilters(javaPackage, serviceName, methodName)
            } else {
                syncCodeWithoutFilters(methodName)
            }
        }
    },
    ASYNC(false, false, false) {
        override fun MethodSpec.Builder.generateDeclaration(
            methodName: String,
            method: DescriptorProtos.MethodDescriptorProto,
            messageNameToJavaPackage: Map<String, String>,
            withFilter: Boolean
        ) {
            returns(TypeName.VOID)
            inputParameter(method, messageNameToJavaPackage)
            propertyParameter(withFilter)
            asyncParameter(method, messageNameToJavaPackage)
        }

        override fun MethodSpec.Builder.generateCode(
            serviceName: String,
            javaPackage: String,
            methodName: String,
            method: DescriptorProtos.MethodDescriptorProto,
            messageNameToJavaPackage: Map<String, String>,
            withFilter: Boolean
        ) {
            if (withFilter) {
                asyncCodeWithFilters(javaPackage, serviceName, methodName)
            } else {
                asyncCodeWithoutFilters(methodName)
            }
        }
    },
    ASYNC_CLIENT_STREAMING(false, true, false) {
        override fun MethodSpec.Builder.generateDeclaration(
            methodName: String,
            method: DescriptorProtos.MethodDescriptorProto,
            messageNameToJavaPackage: Map<String, String>,
            withFilter: Boolean
        ) {
            asyncStreamingReturnType(method, messageNameToJavaPackage)
            propertyParameter(withFilter)
            asyncParameter(method, messageNameToJavaPackage)
        }

        override fun MethodSpec.Builder.generateCode(
            serviceName: String,
            javaPackage: String,
            methodName: String,
            method: DescriptorProtos.MethodDescriptorProto,
            messageNameToJavaPackage: Map<String, String>,
            withFilter: Boolean
        ) {
            if (withFilter) {
                asyncClientStreamingCodeWithFilters(javaPackage, serviceName, methodName)
            } else {
                asyncClientStreamingCodeWithoutFilters(methodName)
            }
        }
    },
    ASYNC_SERVER_STREAMING(false, false, true) {
        override fun MethodSpec.Builder.generateDeclaration(
            methodName: String,
            method: DescriptorProtos.MethodDescriptorProto,
            messageNameToJavaPackage: Map<String, String>,
            withFilter: Boolean
        ) {
            returns(TypeName.VOID)
            inputParameter(method, messageNameToJavaPackage)
            propertyParameter(withFilter)
            asyncParameter(method, messageNameToJavaPackage)
        }

        override fun MethodSpec.Builder.generateCode(
            serviceName: String,
            javaPackage: String,
            methodName: String,
            method: DescriptorProtos.MethodDescriptorProto,
            messageNameToJavaPackage: Map<String, String>,
            withFilter: Boolean
        ) {
            if (withFilter) {
                asyncCodeWithFilters(javaPackage, serviceName, methodName)
            } else {
                asyncCodeWithoutFilters(methodName)
            }
        }
    },
    ASYNC_BIDIRECTIONAL_STREAMING(false, true, true) {
        override fun MethodSpec.Builder.generateDeclaration(
            methodName: String,
            method: DescriptorProtos.MethodDescriptorProto,
            messageNameToJavaPackage: Map<String, String>,
            withFilter: Boolean,
        ) {
            asyncStreamingReturnType(method, messageNameToJavaPackage)
            propertyParameter(withFilter)
            asyncParameter(method, messageNameToJavaPackage)
        }

        override fun MethodSpec.Builder.generateCode(
            serviceName: String,
            javaPackage: String,
            methodName: String,
            method: DescriptorProtos.MethodDescriptorProto,
            messageNameToJavaPackage: Map<String, String>,
            withFilter: Boolean
        ) {
            if (withFilter) {
                asyncClientStreamingCodeWithFilters(javaPackage, serviceName, methodName)
            } else {
                asyncClientStreamingCodeWithoutFilters(methodName)
            }
        }
    };

    fun generateImplMethod(
        serviceName: String,
        javaPackage: String,
        method: DescriptorProtos.MethodDescriptorProto,
        messageNameToJavaPackage: Map<String, String>,
        withFilter: Boolean
    ): MethodSpec {
        val methodName = method.name.replaceFirstChar { it.lowercase(Locale.getDefault()) }
        return with(MethodSpec.methodBuilder(methodName)) {
            addModifiers(PUBLIC)
            generateDeclaration(methodName, method, messageNameToJavaPackage, withFilter)
            generateCode(serviceName, javaPackage, methodName, method, messageNameToJavaPackage, withFilter)
            build()
        }
    }

    fun generateInterfaceMethod(
        method: DescriptorProtos.MethodDescriptorProto,
        messageNameToJavaPackage: Map<String, String>,
        withFilter: Boolean
    ): MethodSpec {
        val methodName = method.name.replaceFirstChar { it.lowercase(Locale.getDefault()) }
        return with(MethodSpec.methodBuilder(methodName)) {
            addModifiers(PUBLIC, ABSTRACT)

            generateDeclaration(methodName, method, messageNameToJavaPackage, withFilter)
            build()
        }
    }

    protected abstract fun MethodSpec.Builder.generateDeclaration(
        methodName: String,
        method: DescriptorProtos.MethodDescriptorProto,
        messageNameToJavaPackage: Map<String, String>,
        withFilter: Boolean,
    )

    protected abstract fun MethodSpec.Builder.generateCode(
        serviceName: String,
        javaPackage: String,
        methodName: String,
        method: DescriptorProtos.MethodDescriptorProto,
        messageNameToJavaPackage: Map<String, String>,
        withFilter: Boolean,
    )

    protected fun MethodSpec.Builder.syncReturnType(
        methodDescriptorProto: DescriptorProtos.MethodDescriptorProto,
        messageNameToJavaPackage: Map<String, String>
    ) {
        returns(
            createType(
                methodDescriptorProto.outputType,
                messageNameToJavaPackage
            )
        )
    }

    protected fun MethodSpec.Builder.syncStreamingReturnType(
        methodDescriptorProto: DescriptorProtos.MethodDescriptorProto,
        messageNameToJavaPackage: Map<String, String>
    ) {
        returns(
            ParameterizedTypeName.get(
                ClassName.get(Iterator::class.java), createType(
                    methodDescriptorProto.outputType,
                    messageNameToJavaPackage
                )
            )
        )
    }

    protected fun MethodSpec.Builder.asyncStreamingReturnType(methodDescriptorProto: DescriptorProtos.MethodDescriptorProto, messageNameToJavaPackage: Map<String, String>) {
        returns(
            ParameterizedTypeName.get(
                ClassName.get(StreamObserver::class.java), createType(
                    methodDescriptorProto.inputType,
                    messageNameToJavaPackage
                )
            )
        )
    }
    protected fun MethodSpec.Builder.inputParameter(
        methodDescriptorProto: DescriptorProtos.MethodDescriptorProto,
        messageNameToJavaPackage: Map<String, String>
    ) {
        addParameter(createType(methodDescriptorProto.inputType, messageNameToJavaPackage), INPUT_ARG)
    }

    protected fun MethodSpec.Builder.propertyParameter(
        withFilter: Boolean
    ) {
        if (withFilter) {
            addParameter(
                ParameterizedTypeName.get(
                    ClassName.get(Map::class.java),
                    ClassName.get(String::class.java),
                    ClassName.get(String::class.java)
                ), PROPERTIES_ARG)
        }
    }

    protected fun MethodSpec.Builder.asyncParameter(methodDescriptorProto: DescriptorProtos.MethodDescriptorProto, messageNameToJavaPackage: Map<String, String>) {
        addParameter(
            ParameterizedTypeName.get(
                ClassName.get(StreamObserver::class.java),
                createType(methodDescriptorProto.outputType, messageNameToJavaPackage)
            ), OBSERVER_ARG
        )
    }

    protected fun MethodSpec.Builder.asyncCodeWithoutFilters(methodName: String) {
        addCode("$methodName($INPUT_ARG, ${Collections::class.java.name}.emptyMap(), $OBSERVER_ARG);")
    }

    protected fun MethodSpec.Builder.asyncCodeWithFilters(
        javaPackage: String,
        serviceName: String,
        methodName: String
    ) {
        val stubClassName = getAsyncStubClassName(javaPackage, serviceName)
        addCode("$stubClassName stub = getStub($INPUT_ARG, $PROPERTIES_ARG);\n")
        addCode("createAsyncRequest($OBSERVER_ARG, (newObserver) -> stub.$methodName($INPUT_ARG, newObserver));")
    }

    protected fun MethodSpec.Builder.asyncClientStreamingCodeWithoutFilters(methodName: String) {
        addCode("return $methodName(${Collections::class.java.name}.emptyMap(), $OBSERVER_ARG);")
    }

    protected fun MethodSpec.Builder.asyncClientStreamingCodeWithFilters(
        javaPackage: String,
        serviceName: String,
        methodName: String
    ) {
        val stubClassName = getAsyncStubClassName(javaPackage, serviceName)
        addCode("$stubClassName stub = getStub(${Any::class.java.name}.getDefaultInstance(), $PROPERTIES_ARG);\n")
        addCode("return createAsyncStreamRequest($OBSERVER_ARG, stub::$methodName);")
    }

    protected fun MethodSpec.Builder.syncCodeWithoutFilters(methodName: String) {
        addCode("return $methodName($INPUT_ARG, ${Collections::class.java.name}.emptyMap());")
    }

    protected fun MethodSpec.Builder.syncCodeWithFilters(
        javaPackage: String,
        serviceName: String,
        methodName: String
    ) {
        val stubClassName = getBlockingStubClassName(javaPackage, serviceName)
        addCode("$stubClassName stub = getStub($INPUT_ARG, $PROPERTIES_ARG);\n")
        addCode("return createBlockingRequest(() -> stub.$methodName($INPUT_ARG));")
    }

    companion object {
        private const val OBSERVER_ARG = "observer"
        private const val PROPERTIES_ARG = "properties"
        private const val INPUT_ARG = "input"

        fun generateImplMethod(
            serviceName: String,
            javaPackage: String,
            method: DescriptorProtos.MethodDescriptorProto,
            messageNameToJavaPackage: Map<String, String>,
            async: Boolean,
            withFilter: Boolean
        ) = searchStrategy(method, async)
            .generateImplMethod(serviceName, javaPackage, method, messageNameToJavaPackage, withFilter)

        fun generateInterfaceMethod(
            method: DescriptorProtos.MethodDescriptorProto,
            messageNameToJavaPackage: Map<String, String>,
            async: Boolean,
            withFilter: Boolean
        ) = searchStrategy(method, async)
            .generateInterfaceMethod(method, messageNameToJavaPackage, withFilter)

        fun createType(protoMessage: String, messageNameToJavaPackage: Map<String, String>): TypeName {
            val name = protoMessage.substringAfterLast('.')
            val fullName = protoMessage.trimStart('.')
            return ClassName.get(messageNameToJavaPackage.getOrDefault(fullName, ""), name)
        }

        fun getBlockingStubClassName(javaPackage: String, protoName: String): ClassName =
            ClassName.get(javaPackage, "${protoName}Grpc", "${protoName}BlockingStub")

        fun getAsyncStubClassName(javaPackage: String, protoName: String): ClassName =
            ClassName.get(javaPackage, "${protoName}Grpc", "${protoName}Stub")

        private fun searchStrategy(method: DescriptorProtos.MethodDescriptorProto, async: Boolean): GrpcMethodGenerationStrategy = values().asSequence()
            .filter { it.sync == !async && it.clientStreaming == method.clientStreaming && it.serverStreaming == method.serverStreaming }
            .firstOrNull()
            ?: error("Strategy for sync ${!async}, client streaming ${method.clientStreaming}, server streaming ${method.serverStreaming}")
    }
}