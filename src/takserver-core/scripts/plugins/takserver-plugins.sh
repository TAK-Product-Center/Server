#!/bin/sh

. ./setenv.sh

# add the lib/ directory to the loader.path for the plugin manager
export JDK_JAVA_OPTIONS="-Dloader.path=WEB-INF/lib-provided,WEB-INF/lib,WEB-INF/classes,file:lib/ -Dio.netty.tmpdir=/opt/tak -Djava.io.tmpdir=/opt/tak -Dio.netty.native.workdir=/opt/tak -Djava.net.preferIPv4Stack=true -Djava.security.egd=file:/dev/./urandom -DIGNITE_UPDATE_NOTIFIER=false -DIGNITE_QUIET=true -Djdk.tls.client.protocols=TLSv1.2"

java -Xms128m -Xmx${PLUGIN_MANAGER_MAX_HEAP}m -jar takserver-pm.jar $@
