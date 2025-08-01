FROM gradle:8.11.1-jdk21 AS build
ARG release_version
ARG vcs_url

COPY ./ .
RUN gradle clean build bintrayUpload \
     -Prelease_version=${release_version} \
     -Pvcs_url=${vcs_url}