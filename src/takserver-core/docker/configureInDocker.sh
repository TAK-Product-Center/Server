#!/bin/sh
if [ $# -eq 0 ]
  then
    ps -ef | grep takserver | grep -v grep | awk '{print $2}' | xargs kill
fi

cd /opt/tak
. ./setenv.sh
java -jar -Xmx${MESSAGING_MAX_HEAP}m -Dspring.profiles.active=messaging takserver.war &
java -jar -Xmx${API_MAX_HEAP}m -Dspring.profiles.active=api takserver.war &
java -jar -Xmx${PLUGIN_MANAGER_MAX_HEAP}m takserver-pm.jar &

if ! [ $# -eq 0 ]
  then
    tail -f /dev/null
fi
