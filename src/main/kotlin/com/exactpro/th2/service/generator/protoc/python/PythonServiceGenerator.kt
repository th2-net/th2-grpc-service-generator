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
package com.exactpro.th2.service.generator.protoc.python

import com.exactpro.th2.service.generator.protoc.FileSpec
import com.exactpro.th2.service.generator.protoc.Generator
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import org.apache.commons.io.FilenameUtils
import java.nio.file.Path
import java.util.Properties

class PythonServiceGenerator : Generator {

    companion object {
        private const val ROOT_PATH_OPTION_NAME = "pythonPath"
        private const val ENABLE_PYTHON_GENERATION_OPTION_NAME = "enablePython"
    }

    private var enableGeneration = true

    private var rootPath: Path? = null

    override fun init(prop: Properties) {
        rootPath = prop.getProperty(ROOT_PATH_OPTION_NAME)?.let { Path.of(it) }
        prop.getProperty(ENABLE_PYTHON_GENERATION_OPTION_NAME)?.also {
            enableGeneration = it.toBoolean()
        }
    }

    override fun generate(fileDescriptor: FileDescriptorProto, messageNameToJavaPackage: Map<String, String>): List<FileSpec> {

        if (!enableGeneration) {
            return emptyList()
        }

        val fileName = FilenameUtils.removeExtension(fileDescriptor.name)
        val importFilePrefix = FilenameUtils.getBaseName(fileName)

        return fileDescriptor.serviceList.map { service ->
            val builder = StringBuilder()
            builder.append("""
            from . import ${importFilePrefix}_pb2_grpc as importStub
            
            class ${service.name}Service(object):
            
                def __init__(self, router):
                    self.connector = router.get_connection(${service.name}Service, importStub.${service.name}Stub)
            """.trimIndent())

            service.methodList.forEach { method ->
                builder.append("\n\n    def ${method.name}(self, request, timeout=None, properties=None):\n")
                builder.append("        return self.connector.create_request('${method.name}', request, timeout, properties)")
            }

            PythonFileSpec(rootPath, FilenameUtils.getPath(fileName), service.name, builder.toString())
        }

    }



}