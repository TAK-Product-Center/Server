#!/bin/sh

# set up execution environment
export JDK_JAVA_OPTIONS="${JDK_JAVA_OPTIONS} -Dloader.path=WEB-INF/lib-provided,WEB-INF/lib,WEB-INF/classes,file:lib/ -Dio.netty.tmpdir=/opt/tak -Djava.io.tmpdir=/opt/tak -Dio.netty.native.workdir=/opt/tak -Djava.net.preferIPv4Stack=true -Djava.security.egd=file:/dev/./urandom -DIGNITE_UPDATE_NOTIFIER=false -DIGNITE_QUIET=true -Djdk.tls.client.protocols=TLSv1.2"
export IGNITE_HOME="/opt/tak"

# If a configmap with certs has been mounted copy it to the certificate directory
# Although you can mount a configmap file rw, you can't do so with a directory
# -L follows the relative symlinks since they break without it
if [ -d /certs-configmap ];then
	mkdir -p /certs/files
	cp -Lr /certs-configmap/* /certs/files/
fi

# get total RAM
TOTAL=`awk '/MemTotal/ {print $2}' /proc/meminfo`

# convert from KiB to MB"
TOTAL=$(($TOTAL * 1024 * 30 / 1000 / 1000 / 100))

export CONFIG_MAX_HEAP=7500
export API_MAX_HEAP=7500
export MESSAGING_MAX_HEAP=7500
export PLUGIN_MANAGER_MAX_HEAP=7500
export RETENTION_MAX_HEAP=7500
