#!/bin/sh

# set up execution environment
export JDK_JAVA_OPTIONS="-Dloader.path=WEB-INF/lib-provided,WEB-INF/lib,WEB-INF/classes -Dio.netty.tmpdir=/opt/tak -Djava.io.tmpdir=/opt/tak -Dio.netty.native.workdir=/opt/tak -Djava.net.preferIPv4Stack=true -Djava.security.egd=file:/dev/./urandom -DIGNITE_UPDATE_NOTIFIER=false -DIGNITE_QUIET=true -Djdk.tls.client.protocols=TLSv1.2"
export IGNITE_HOME="/opt/tak"

# pull in defaults
[ -f /etc/default/takserver ] && . /etc/default/takserver

# get total RAM
TOTAL=`awk '/MemTotal/ {print $2}' /proc/meminfo`

# convert from KiB to MB"
TOTAL=$(($TOTAL * 1024 * 25 / 1000 / 1000 / 100))

# set API max if not set already
if [ -z "$API_MAX_HEAP" ]; then
  export API_MAX_HEAP=$(($TOTAL * 120 / 100))
fi

# set messaging max if not set already
if [ -z "$MESSAGING_MAX_HEAP" ]; then
  export MESSAGING_MAX_HEAP=$(($TOTAL * 50 / 100))
fi

# set plugin manager max if not set already
if [ -z "$PLUGIN_MANAGER_MAX_HEAP" ]; then
  export PLUGIN_MANAGER_MAX_HEAP=$(($TOTAL * 12 / 100))
fi

# set retention max if not set already
if [ -z "$RETENTION_MAX_HEAP" ]; then
  export RETENTION_MAX_HEAP=$(($TOTAL * 6 / 100))
fi

#echo "TOTAL : "$TOTAL
#echo "API_MAX_HEAP : "$API_MAX_HEAP
#echo "MESSAGING_MAX_HEAP : "$MESSAGING_MAX_HEAP
#echo "PLUGIN_MANAGER_MAX_HEAP : "$PLUGIN_MANAGER_MAX_HEAP
#echo "RETENTION_MAX_HEAP : "$RETENTION_MAX_HEAP
