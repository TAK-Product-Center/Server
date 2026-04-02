#!/bin/sh

. ./setenv.sh

exec java -server -XX:+AlwaysPreTouch -XX:+UseG1GC -XX:+ScavengeBeforeFullGC -XX:+DisableExplicitGC -XX:MaxRAMPercentage=${MAX_HEAP_PERCENT} -Dspring.profiles.active=config,consolelog -Dkeystore.pkcs12.legacy -jar takserver.war $@

