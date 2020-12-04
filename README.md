# Overview
This project implemented protoc plugin for generation services for gRPC router.
It generates java and python code.
You can plug it to protoc or run from command line.
# Usage
## Protoc plugin
If you want to use this generator with gradle and protoc you should add artifact name to protoc plugins section and add to generated protoc tasks
You can set options:
1. javaInterfacesPath - generate services interfaces to this path
1. javaInterfacesImplPath - generate TH2 services interfaces implementation to this path
1. pythonPath - generate python services to this path

All these paths will be relative from  ``generateProtoTasks.generatedFilesBaseDir`` (Example: `javaInterfacesPath` = `path/to/base/dir/path/to/interfaces`)
```groovy
protobuf {
    plugins {
        generator {
            artifact = "com.exactpro.th2:service-generator-protoc:${version_generator}:all@jar"
        }
    }
    generateProtoTasks.generatedFilesBaseDir = 'path/to/base/dir'
    generateProtoTasks {
        all()*.plugins {
            generator {
                option 'javaInterfacesPath=./path/to/interfaces'
                option 'javaInterfacesImplPath=./path/to/th2/impl'
                option 'pythonPath=./path/to/python'
            }
        }
    }
}
```
## Command line
You can run this project with command line

First argument should be directory or file with FileDescriptorSet. Second argument is output directory. After that arguments you can set options (Example: `--pythonPath=./path/to/python`)
