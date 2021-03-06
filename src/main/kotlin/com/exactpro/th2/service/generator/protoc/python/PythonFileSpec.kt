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
import com.google.common.base.CaseFormat
import com.google.common.base.CaseFormat.LOWER_UNDERSCORE
import java.nio.file.Path

class PythonFileSpec(private val roodDir: Path?, pathToFile: String?, serviceName: String, private val content: String) : FileSpec {

    private val filePath: String

    init {
        val fileName = CaseFormat.LOWER_CAMEL.to(LOWER_UNDERSCORE, serviceName) + "_service.py"
        filePath = if (pathToFile != null) Path.of(pathToFile, fileName).toString() else fileName
    }

    override fun getFilePath(): String = roodDir?.resolve(filePath)?.toString() ?: filePath

    override fun getContent(): String = content
}