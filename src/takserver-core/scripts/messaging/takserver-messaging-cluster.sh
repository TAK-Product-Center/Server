#!/bin/sh

. ./setenv.sh

. ./enable_admin.sh &

exec java -server -XX:+AlwaysPreTouch -XX:+UseG1GC -XX:+ScavengeBeforeFullGC -XX:+DisableExplicitGC -XX:MaxRAMPercentage=${MAX_HEAP_PERCENT} -Dspring.profiles.active=messaging,consolelog -jar takserver.war $@


