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
package com.exactpro.th2.service.generator.protoc

import com.exactpro.th2.service.generator.protoc.java.MetaInfGenerator
import com.exactpro.th2.service.generator.protoc.java.ServiceDefaultImplGenerator
import com.exactpro.th2.service.generator.protoc.java.ServiceInterfaceGenerator
import com.exactpro.th2.service.generator.protoc.python.PythonServiceGenerator
import com.exactpro.th2.service.generator.protoc.run.CommandLineRun
import com.exactpro.th2.service.generator.protoc.run.PluginRun

object Main {

    private val generators: List<Generator> = listOf(
        ServiceInterfaceGenerator(),
        ServiceDefaultImplGenerator(),
        PythonServiceGenerator(),
        MetaInfGenerator()
    )

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            PluginRun(generators).generateResponse(System.`in`, System.out)
        } else {
            CommandLineRun(generators).start(args)
        }
    }

}