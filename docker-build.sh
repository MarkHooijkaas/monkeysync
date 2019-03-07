#!/bin/sh
. ./project.properties
./gradlew fatJar
docker build -t ${DOCKER_REPO}/${PROJECT_NAME} -t ${PROJECT_NAME}:latest .
