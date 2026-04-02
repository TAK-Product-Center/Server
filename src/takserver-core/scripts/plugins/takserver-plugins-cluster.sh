#!/bin/sh

. ./setenv.sh

exec java -Xms128m -Xmx${PLUGIN_MANAGER_MAX_HEAP}m -Dspring.profiles.active=k8cluster -jar takserver-pm.jar $@
