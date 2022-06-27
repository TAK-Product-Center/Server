#!/bin/bash

export FEDERATION_HUB=/opt/tak/federation-hub
export JDK_JAVA_OPTIONS="-Dio.netty.tmpdir=/opt/tak -Djava.io.tmpdir=/opt/tak -Dio.netty.native.workdir=/opt/tak -DIGNITE_UPDATE_NOTIFIER=false"

java -Dlogging.config=${FEDERATION_HUB}/configs/logback-broker.xml --add-opens java.base/jdk.internal.misc=ALL-UNNAMED -Dio.netty.tryReflectionSetAccessible=true -jar ${FEDERATION_HUB}/jars/federation-hub-broker.jar "$@"
