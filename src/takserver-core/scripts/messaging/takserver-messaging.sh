#!/bin/sh
rm -rf ./tmp/

. ./setenv.sh

java -server -XX:+AlwaysPreTouch -XX:+UseG1GC -XX:+ScavengeBeforeFullGC -XX:+DisableExplicitGC -Xmx${MESSAGING_MAX_HEAP}m -Dspring.profiles.active=messaging -jar takserver.war $@
