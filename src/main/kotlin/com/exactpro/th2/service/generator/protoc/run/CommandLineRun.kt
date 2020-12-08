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
import com.exactpro.th2.service.generator.protoc.util.javaPackage
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.Properties

class CommandLineRun(private val generators: List<Generator>) {

    fun parseDirectory(dirOrFile: File, files: MutableList<FileDescriptorProto>) {
        if (dirOrFile.exists()) {
            if (dirOrFile.isDirectory) {
                dirOrFile.listFiles()?.forEach {
                    parseDirectory(it, files)
                }
            } else {
                dirOrFile.inputStream().buffered().use { FileDescriptorSet.parseFrom(it) }.fileList.forEach {
                    files.add(it)
                }
            }
        }
    }

    fun start(args: Array<String>) {

        if (args.size < 2) {
            throw IllegalArgumentException("Count arguments is wrong. Minimum count is 2")
        }


        val protoDir: Path = Path.of(args[0])
        val outputDir: Path = Path.of(args[1])
        val prop = Properties()

        val output = outputDir.toFile()
        if (!output.exists()) {
            output.mkdirs()
        }

        var index = 2
        while (index < args.size) {
            args[index++].also {
                val str = it.substringAfter("--")
                val tmpIndex = str.indexOf('=')
                if (tmpIndex > 0) {
                    prop.setProperty(str.substring(0, tmpIndex), str.substring(tmpIndex + 1, str.length))
                }
            }
        }

        val map = HashMap<String, String>()

        val files = ArrayList<FileDescriptorProto>()

        parseDirectory(protoDir.toFile(), files)

        files.forEach {
            val javaPackage = it.javaPackage()
            it.messageTypeList.forEach {
                map.put(it.name, javaPackage)
            }
        }

        files.forEach { file ->
            generators.forEach {
                it.generate(file, map).forEach { fileSpec ->
                    outputDir.resolve(fileSpec.getFilePath()).toFile().also { out ->
                        if (!out.exists()) {
                            out.parentFile.mkdirs()
                            out.createNewFile()
                        }

                        out.outputStream().buffered().writer(StandardCharsets.UTF_8).use {
                            it.write(fileSpec.getContent())
                        }
                    }
                }
            }
        }

    }

}