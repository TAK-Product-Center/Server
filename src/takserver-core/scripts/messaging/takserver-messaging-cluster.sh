#!/bin/sh

. ./setenv.sh

. ./enable_admin.sh &

java -Xmx${MESSAGING_MAX_HEAP}m -Dspring.profiles.active=messaging,consolelog -jar takserver.war $@

# To run with plugin support enable in CoreConfig and place plugin jars in /opt/tak/lib.
