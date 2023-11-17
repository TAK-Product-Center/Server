#!/bin/sh
if [ $# -eq 0 ]
  then
    ps -ef | grep takserver | grep -v grep | awk '{print $2}' | xargs kill
fi

cd /opt/tak
. ./setenv.sh
java -jar -Xmx${MESSAGING_MAX_HEAP}m -Dspring.profiles.active=messaging takserver.war &
java -jar -Xmx${API_MAX_HEAP}m -Dspring.profiles.active=api -Dkeystore.pkcs12.legacy takserver.war &
java -jar -Xmx${RETENTION_MAX_HEAP}m takserver-retention.jar &
java -jar -Xmx${PLUGIN_MANAGER_MAX_HEAP}m -Dloader.path=WEB-INF/lib-provided,WEB-INF/lib,WEB-INF/classes,file:lib/ takserver-pm.jar &

if ! [ $# -eq 0 ]
  then
    tail -f /dev/null
fi
