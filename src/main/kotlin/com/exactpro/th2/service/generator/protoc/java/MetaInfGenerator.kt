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

import com.exactpro.th2.service.generator.protoc.FileSpec
import com.exactpro.th2.service.generator.protoc.FileSpecImpl
import com.exactpro.th2.service.generator.protoc.util.javaPackage
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto
import java.nio.file.Path
import java.util.Properties

class MetaInfGenerator : AbstractJavaServiceGenerator() {

    companion object {
        private const val ROOT_PATH_OPTION_NAME = "javaMetaInfPath"

        private val PATH_TO_SERVICE_FOLDER = Path.of("META-INF", "services")
    }

    private var rootPath: Path = PATH_TO_SERVICE_FOLDER

    override fun init(prop: Properties) {
        super.init(prop)
        prop.getProperty(ROOT_PATH_OPTION_NAME)?.also {
            rootPath = Path.of(it).resolve(PATH_TO_SERVICE_FOLDER)
        }
    }

    override fun generate(fileDescriptor: FileDescriptorProto, messageNameToJavaPackage: Map<String, String>): List<FileSpec> {
        val javaPackage = fileDescriptor.javaPackage()
        return fileDescriptor.serviceList.flatMap {
            listOf(
                generateMetaInfForBlockingService(it, javaPackage),
                generateMetaInfForAsyncService(it, javaPackage)
            )
        }
    }

    override fun generateForService(service: ServiceDescriptorProto, javaPackage: String, messageNameToJavaPackage: Map<String, String>): List<FileSpec> {
        return emptyList()
    }

    private fun generateMetaInfForAsyncService(service: ServiceDescriptorProto, javaPackage: String): FileSpec = FileSpecImpl(
        rootPath.resolve("${javaPackage}.${getBlockingServiceName(service.name)}").toString(),
        "${javaPackage}.${getBlockingDefaultImplName(service.name)}"
    )

    private fun generateMetaInfForBlockingService(service: ServiceDescriptorProto, javaPackage: String): FileSpec = FileSpecImpl(
        rootPath.resolve("${javaPackage}.${getAsyncServiceName(service.name)}").toString(),
        "${javaPackage}.${getAsyncDefaultImplName(service.name)}"
    )

}