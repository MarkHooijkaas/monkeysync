#!/bin/sh
. ./project.properties
docker build -t ${PROJECT_NAME}:${PROJECT_VERSION} -t ${PROJECT_NAME}:latest .
