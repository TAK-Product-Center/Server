#!/bin/sh
rm -rf ./tmp/

. ./setenv.sh

java -Xmx${MESSAGING_MAX_HEAP}m -Dspring.profiles.active=messaging -jar takserver.war $@

# To run with plugin support enable in CoreConfig and place plugin jars in /opt/tak/lib.
