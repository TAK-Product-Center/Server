#!/bin/sh

. ./setenv.sh

java -server -XX:+AlwaysPreTouch -XX:+UseG1GC -XX:+ScavengeBeforeFullGC -XX:+DisableExplicitGC -Xmx${RETENTION_MAX_HEAP}m -Dspring.main.banner-mode=off -DIGNITE_NO_ASCII=true -DIGNITE_PERFORMANCE_SUGGESTIONS_DISABLED=true -DIGNITE_UPDATE_NOTIFIER=false -jar takserver-retention.jar $@
