#!/bin/bash

export FEDERATION_HUB=/opt/tak/federation-hub
export JDK_JAVA_OPTIONS="-Dio.netty.tmpdir=/opt/tak -Djava.io.tmpdir=/opt/tak -Dio.netty.native.workdir=/opt/tak -DIGNITE_UPDATE_NOTIFIER=false"

# get total RAM
TOTALRAMBYTES=`awk '/MemTotal/ {print $2}' /proc/meminfo`

# set BROKER max if not set already
if [ -z "$BROKER_MAX_HEAP" ]; then
  export BROKER_MAX_HEAP=$(($TOTALRAMBYTES / 1000 / 100 * 50))
fi

java -Xmx${BROKER_MAX_HEAP}m -Dlogging.config=${FEDERATION_HUB}/configs/logback-broker.xml --add-opens java.base/jdk.internal.misc=ALL-UNNAMED -Dio.netty.tryReflectionSetAccessible=true -jar ${FEDERATION_HUB}/jars/federation-hub-broker.jar "$@"
