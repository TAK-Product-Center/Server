#!/bin/sh

. ./setenv.sh

java -Xmx${API_MAX_HEAP}m -Dspring.profiles.active=config,consolelog -Dkeystore.pkcs12.legacy -jar takserver.war $@

