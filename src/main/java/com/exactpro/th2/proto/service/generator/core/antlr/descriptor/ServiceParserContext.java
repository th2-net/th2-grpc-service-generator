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
package com.exactpro.th2.proto.service.generator.core.antlr.descriptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class ServiceParserContext {

    private final Map<String, String> autoReplacedProtoPackageToJavaPackage = new HashMap<>();
    private final Map<String, Map<String, String[]>> protoMessageToJavaClass = new HashMap<>();
    private final Map<String, ServiceDescriptor> services = new HashMap<>();

    public void addAutoReplacedPackage(String protoPackage, String javaPackage) {
        String prevValue = autoReplacedProtoPackageToJavaPackage.get(protoPackage);
        if (prevValue == null) {
            autoReplacedProtoPackageToJavaPackage.put(protoPackage, javaPackage);
        } else if (!prevValue.equals(javaPackage)) {
            throw new IllegalArgumentException("Target for proto package already added");
        }
    }

    public void addService(String fileName, ServiceDescriptor serviceDescriptor) {
        services.put(fileName, serviceDescriptor);
    }

    public void addType(TypeDescriptor descriptor, String javaType, String javaTypeName) {
        var realJavaPackage = autoReplacedProtoPackageToJavaPackage.get(javaType);
        realJavaPackage = realJavaPackage == null ? javaType : realJavaPackage;
        protoMessageToJavaClass.computeIfAbsent(descriptor.getPackageName(), k -> new HashMap<>()).put(descriptor.getName(), new String[]{realJavaPackage, javaTypeName});
    }

    public List<ServiceDescriptor> getServiceForGeneration(List<String> fileNames) {
        return services
                .entrySet()
                .stream()
                .filter(it -> fileNames.contains(it.getKey()))
                .map(Entry::getValue)
                .map(it ->
                    ServiceDescriptor.builder()
                            .annotations(it.getAnnotations())
                            .comments(it.getComments())
                            .name(it.getName())
                            .packageName(it.getPackageName())
                            .methods(it.getMethods()
                                    .stream()
                                    .map(method -> MethodDescriptor.builder()
                                            .comments(method.getComments())
                                            .name(method.getName())
                                            .requestTypes(method.getRequestTypes().stream().map(requestType -> changeTypePackageName(requestType)).collect(Collectors.toList()))
                                            .responseType(changeTypePackageName(method.getResponseType())).build()
                                    )
                                    .collect(Collectors.toList())
                            ).build()
                    ).collect(Collectors.toList());
    }

    private TypeDescriptor changeTypePackageName(TypeDescriptor type) {

        if (type.getPackageName() == null || type.getPackageName().isEmpty()) {
            return type;
        }

        var protoPackage = protoMessageToJavaClass.get(type.getPackageName());

        var auto = autoReplacedProtoPackageToJavaPackage.get(type.getPackageName());
        if (auto != null) {
            return TypeDescriptor.builder().packageName(auto).name(type.getName()).build();
        }

        if (protoPackage != null) {
            var fullJavaName = protoPackage.get(type.getName());
            return TypeDescriptor.builder().packageName(fullJavaName[0]).name(fullJavaName[1]).build();
        } else {
            throw new IllegalStateException("Can not find java class for type = " + type.toString());
        }
    }
}
