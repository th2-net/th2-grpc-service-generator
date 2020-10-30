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
package com.exactpro.th2.proto.service.generator.plugin;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.th2.proto.service.generator.core.antlr.ProtoServiceParser;
import com.exactpro.th2.proto.service.generator.core.antlr.ServiceClassGenerator;
import com.exactpro.th2.proto.service.generator.core.antlr.descriptor.AnnotationDescriptor;
import com.exactpro.th2.proto.service.generator.core.antlr.descriptor.ServiceDescriptor;
import com.exactpro.th2.proto.service.generator.core.antlr.descriptor.TypeDescriptor;
import com.exactpro.th2.proto.service.generator.plugin.ext.GenServiceExt;
import com.exactpro.th2.proto.service.generator.plugin.ext.GenServiceSettingsExt;

import lombok.SneakyThrows;

public class ServiceGeneratorPlugin implements Plugin<Project> {

    private static final String CONFIGURATION_APP_CLASSPATH = "compileClasspath";

    private static final Logger logger = LoggerFactory.getLogger(ServiceGeneratorPlugin.class);

    @Override
    public void apply(Project project) {

        var extension = project.getExtensions().create("serviceGeneration", GenServiceExt.class);

        var generateServiceTask = project.task("generateServices");

        generateServiceTask.doLast(task -> generateService(project, task, extension));

    }

    @SneakyThrows
    private void generateService(Project project, Task task, GenServiceExt extension) {
        logger.info("Test");

        var urls = Arrays.asList(project
                .getConfigurations()
                .getByName(CONFIGURATION_APP_CLASSPATH)
                .getAsPath().split(":"))
                .stream()
                .map(it -> {
                    try {
                        return Path.of(it).toUri().toURL();
                    } catch (MalformedURLException e) {
                        throw new IllegalStateException("Can not convert file to url = " + it.toString());
                    }
                }).toArray(URL[]::new);

        var settings = extension.getSettings();

        validateSettings(settings);

        var protoDir = project.file(settings.getProtoDir()).toPath();

        var genDir = project.file(settings.getOutDir()).toPath();

        ClassLoader artifactsLoader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());

        generate(protoDir, genDir, artifactsLoader);

    }

    @SneakyThrows
    protected void generate(Path protoDir, Path genDir, ClassLoader loader) {
        var serviceDescriptors = ProtoServiceParser.getServiceDescriptors(protoDir, loader);

        addSyncStubAnnotation(serviceDescriptors);

        var asyncServiceDescriptors = serviceDescriptors.stream()
                .map(this::toAsync)
                .collect(Collectors.toList());

        serviceDescriptors.addAll(asyncServiceDescriptors);

        ServiceClassGenerator.generate(genDir, serviceDescriptors);
    }


    protected ServiceDescriptor toAsync(ServiceDescriptor sd) {

        var asd = ServiceDescriptor.newInstance(sd);

        asd.getMethods().forEach(method -> {
            method.getRequestTypes().add(TypeDescriptor.builder()
                    .name("StreamObserver")
                    .packageName("io.grpc.stub")
                    .genericType(method.getResponseType())
                    .build());
            method.setResponseType(TypeDescriptor.builder()
                    .name("void")
                    .packageName(null)
                    .genericType(null)
                    .build());
        });

        asd.getAnnotations().forEach(ann -> {

            var parts = Arrays.asList(ann.getValue().split("\\."));

            //  package.to.stub.ActGrpc.ActBlockingStub.class ->
            //  package.to.stub.ActGrpc.ActStub.class
            parts.set(parts.size() - 2, asd.getName() + "Stub");

            ann.setValue(String.join(".", parts));

        });

        asd.setName("Async" + asd.getName());

        return asd;
    }

    protected void addSyncStubAnnotation(List<ServiceDescriptor> sds) {
        sds.forEach(sd -> sd.getAnnotations().add(
                AnnotationDescriptor.builder()
                        .name("GrpcStub")
                        .packageName("com.exactpro.th2.proto.service.generator.core.antlr.annotation")
                        .value(createStubClass(sd))
                        .build()
        ));
    }

    protected String createStubClass(ServiceDescriptor sd) {
        return String.join(".", List.of(
                sd.getPackageName(),
                sd.getName() + "Grpc",
                sd.getName() + "BlockingStub.class"
        ));
    }

    protected void validateSettings(GenServiceSettingsExt settings) {

        if (isEmpty(settings.getOutDir())) {
            throw new RuntimeException("Target directory path for generated source cannot be empty!");
        }

        if (isEmpty(settings.getProtoDir())) {
            throw new RuntimeException("Proto files directory path cannot be empty!");
        }

    }

    protected boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

}
