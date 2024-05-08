#!/bin/sh

# set up execution environment
export JDK_JAVA_OPTIONS="-Dloader.path=WEB-INF/lib-provided,WEB-INF/lib,WEB-INF/classes -Dio.netty.tmpdir=/opt/tak -Djava.io.tmpdir=/opt/tak -Dio.netty.native.workdir=/opt/tak -Djava.net.preferIPv4Stack=true -Djava.security.egd=file:/dev/./urandom -DIGNITE_UPDATE_NOTIFIER=false -DIGNITE_QUIET=true -Djdk.tls.client.protocols=TLSv1.2"
export IGNITE_HOME="/opt/tak"

# Set the java opens values. These must be managed in the root build file in addition to here!
export JDK_JAVA_OPTIONS="${JDK_JAVA_OPTIONS} 
--add-opens=java.base/sun.security.pkcs=ALL-UNNAMED
--add-opens=java.base/sun.security.pkcs10=ALL-UNNAMED
--add-opens=java.base/sun.security.util=ALL-UNNAMED
--add-opens=java.base/sun.security.x509=ALL-UNNAMED
--add-opens=java.base/sun.security.tools.keytool=ALL-UNNAMED
--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED
--add-opens=java.base/sun.nio.ch=ALL-UNNAMED
--add-opens=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED
--add-opens=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED
--add-opens=java.base/sun.reflect.generics.reflectiveObjects=ALL-UNNAMED
--add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED
--add-opens=java.base/java.io=ALL-UNNAMED
--add-opens=java.base/java.nio=ALL-UNNAMED
--add-opens=java.base/java.util=ALL-UNNAMED
--add-opens=java.base/java.util.concurrent=ALL-UNNAMED
--add-opens=java.base/java.util.concurrent.locks=ALL-UNNAMED
--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED
--add-opens=java.base/java.lang=ALL-UNNAMED
--add-opens=java.base/java.lang.invoke=ALL-UNNAMED
--add-opens=java.base/java.math=ALL-UNNAMED
--add-opens=java.sql/java.sql=ALL-UNNAMED
--add-opens=java.base/javax.net.ssl=ALL-UNNAMED
--add-opens=java.base/java.net=ALL-UNNAMED
--add-opens=jdk.unsupported/sun.misc=ALL-UNNAMED
--add-opens=java.base/java.lang.ref=ALL-UNNAMED
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED
--add-opens=java.base/java.security=ALL-UNNAMED
--add-opens=java.base/java.security.ssl=ALL-UNNAMED
--add-opens=java.base/java.security.cert=ALL-UNNAMED
--add-opens=java.base/sun.security.provider.certpath=ALL-UNNAMED
--add-opens=java.base/sun.security.rsa=ALL-UNNAMED
--add-opens=java.base/sun.security.ssl=ALL-UNNAMED
--add-opens=java.base/sun.security.validator=ALL-UNNAMED
--add-opens=java.base/sun.security.x500=ALL-UNNAMED
--add-opens=java.base/sun.security.pkcs12=ALL-UNNAMED
--add-opens=java.base/sun.security.provider=ALL-UNNAMED
--add-opens=java.base/javax.security.auth.x500=ALL-UNNAMED"

# pull in defaults
[ -f /etc/default/takserver ] && . /etc/default/takserver

# get total RAM
TOTALRAMBYTES=`awk '/MemTotal/ {print $2}' /proc/meminfo`

# convert from KiB to MB"
#TOTAL=$(($TOTARAMBYTES * 1024 * 25 / 1000 / 1000 / 100))

# set CONFIG max if not set already
if [ -z "$CONFIG_MAX_HEAP" ]; then
  export CONFIG_MAX_HEAP=$(($TOTALRAMBYTES / 35000))
fi

# set API max if not set already
if [ -z "$API_MAX_HEAP" ]; then
  export API_MAX_HEAP=$(($TOTALRAMBYTES / 6300))
fi

# set messaging max if not set already
if [ -z "$MESSAGING_MAX_HEAP" ]; then
  export MESSAGING_MAX_HEAP=$(($TOTALRAMBYTES / 6300))
fi

# set plugin manager max if not set already
if [ -z "$PLUGIN_MANAGER_MAX_HEAP" ]; then
  export PLUGIN_MANAGER_MAX_HEAP=$(($TOTALRAMBYTES / 30000))
fi

# set retention max if not set already
if [ -z "$RETENTION_MAX_HEAP" ]; then
  export RETENTION_MAX_HEAP=$(($TOTALRAMBYTES / 30000))
fi

echo "TOTALRAMBYTES : "$TOTALRAMBYTES
echo "CONFIG_MAX_HEAP (MB) : "$CONFIG_MAX_HEAP
echo "API_MAX_HEAP (MB) : "$API_MAX_HEAP
echo "MESSAGING_MAX_HEAP (MB) : "$MESSAGING_MAX_HEAP
echo "PLUGIN_MANAGER_MAX_HEAP (MB) : "$PLUGIN_MANAGER_MAX_HEAP
echo "RETENTION_MAX_HEAP (MB) : "$RETENTION_MAX_HEAP
