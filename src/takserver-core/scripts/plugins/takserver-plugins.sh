#!/bin/sh

. ./setenv.sh

java -Xms128m -Xmx${PLUGIN_MANAGER_MAX_HEAP}m -jar takserver-pm.jar $@
