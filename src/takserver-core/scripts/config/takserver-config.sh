#!/bin/sh

. ./setenv.sh

java -server -XX:+AlwaysPreTouch -XX:+UseG1GC -XX:+ScavengeBeforeFullGC -XX:+DisableExplicitGC -Xmx${CONFIG_MAX_HEAP}m -Dspring.profiles.active=config -Dkeystore.pkcs12.legacy -jar takserver.war $@

