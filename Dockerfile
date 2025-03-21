FROM gradle:8.11.1-jdk11 AS build
ARG release_version
ARG bintray_user
ARG bintray_key
ARG vcs_url

COPY ./ .
RUN gradle clean build bintrayUpload \
     -Prelease_version=${release_version} \
     -Pbintray_user=${bintray_user} \
     -Pbintray_key=${bintray_key} \
     -Pvcs_url=${vcs_url}