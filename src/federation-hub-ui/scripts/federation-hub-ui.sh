#!/bin/bash

export FEDERATION_HUB=/opt/tak/federation-hub
export JDK_JAVA_OPTIONS="-Dio.netty.tmpdir=/opt/tak -Djava.io.tmpdir=/opt/tak -Dio.netty.native.workdir=/opt/tak -DIGNITE_UPDATE_NOTIFIER=false"

# get total RAM
TOTALRAMBYTES=`awk '/MemTotal/ {print $2}' /proc/meminfo`

# set UI max if not set already
if [ -z "$UI_MAX_HEAP" ]; then
  export UI_MAX_HEAP=$(($TOTALRAMBYTES / 1000 / 100 * 25))
fi

java -Xmx${UI_MAX_HEAP}m -Dlogging.config=${FEDERATION_HUB}/configs/logback-ui.xml -jar ${FEDERATION_HUB}/jars/federation-hub-ui.war "$@"
