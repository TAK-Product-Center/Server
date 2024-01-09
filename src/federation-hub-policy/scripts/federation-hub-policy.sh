#!/bin/bash

export FEDERATION_HUB=/opt/tak/federation-hub
export JDK_JAVA_OPTIONS="-Dio.netty.tmpdir=/opt/tak -Djava.io.tmpdir=/opt/tak -Dio.netty.native.workdir=/opt/tak -DIGNITE_UPDATE_NOTIFIER=false"

# get total RAM
TOTALRAMBYTES=`awk '/MemTotal/ {print $2}' /proc/meminfo`

# set POLICY max if not set already
if [ -z "$POLICY_MAX_HEAP" ]; then
  export POLICY_MAX_HEAP=$(($TOTALRAMBYTES / 1000 / 100 * 15))
fi

java -Xmx${POLICY_MAX_HEAP}m -Dlogging.config=${FEDERATION_HUB}/configs/logback-policy.xml -jar ${FEDERATION_HUB}/jars/federation-hub-policy.jar
