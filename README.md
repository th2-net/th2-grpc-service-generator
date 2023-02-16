# Overview
This project implements a **protoc** plugin to generate services for gRPC router.
It generates wrappers for Java's and Python's gRPC implementation.
You can plug it directly to protoc or run it from the command line itself.
# Usage
## Protoc plugin
If you want to use this generator with Gradle and the protobuf plugin, you should add the project's artifact to the protoc plugin's section and add the required options to the proto generated tasks
It is possible to set the following options:
1. javaInterfacesPath - the path for Java services interfaces
1. javaInterfacesImplPath - the path for TH2 services implementations
1. javaMetaInfPath - the path for ServiceLoader files
1. pythonPath - the path for Python services implementations
1. enableJava - the flag for turning on/off the generation of files for Java language (default: true)
1. enablePython - the flag for turning on/off the generation of files for Python language (default: true)

All these paths are relative to the directory in  ``generateProtoTasks.generatedFilesBaseDir`` parameter (Example: `javaInterfacesPath` = `path/to/base/dir/path/to/interfaces`)
```groovy
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${protobufVersion}"
    }
    plugins {
        grpc {
            artifact = "io.grpc:protoc-gen-grpc-java:${grpcVersion}"
        }
        generator {
            artifact = "com.exactpro.th2:service-generator-protoc:${generatorVersion}:all@jar"
        }
    }
    generateProtoTasks.generatedFilesBaseDir = 'path/to/base/dir'
    generateProtoTasks {
        all()*.plugins {
            grpc {}
            generator {
                option 'javaInterfacesPath=./path/to/interfaces'
                option 'javaInterfacesImplPath=./path/to/th2/impl'
                option 'javaMetaInfPath=./path/to/service/loader/files'
                option 'pythonPath=./path/to/python'
                option 'enableJava=true'
                option 'enablePython=true'
            }
        }
    }
}
```
## Command line
You can run this project using the command line. In order to this you can refer to the project's artifact which is used in Gradle protobuf's plugin. 

The first argument should be the directory or the file with ``FileDescriptorSet``. The second argument is the output directory. After those arguments you can set the options (Example: `--pythonPath=./path/to/python`)

``FileDescriptorSet`` is a special protobuf's object which describes the proto files. You can generate it with Gradle's protobuf plugin
```groovy
protobuf {
    //...
    generateProtoTasks {
        //...
        all()*.generateDescriptorSet = true
    }
}
```

## Release notes

### 3.3.0

+ BOM updated to `4.1.0`
+ Kotlin updated to `1.6.21`
+ Protobuf updated to `3.21.12`
+ Added GitHub workflow with vulnerabilities scanning

### 3.2.2

* Fixed backward compatibility to previous version. The problem related to AbstractGrpcService.getStub signature

### 3.2.1

* gRPC pins filters support on Python

### 3.2.0

* gRPC pins filters support

### 3.1.12

* Fix a problem with code generation for the protobuf `stream` modifier in returns.

### 3.1.11

* Fix a problem with retry grpc call when a server-side thrown an exception

### 3.1.10

* Fix a bug which uses an incorrect message, if a message with the same name exists in a different proto package;

### 3.1.7

* Removed the dependency to Bintray repository
