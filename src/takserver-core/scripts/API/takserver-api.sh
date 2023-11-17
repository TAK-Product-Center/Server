#!/bin/sh

. ./setenv.sh

java -Xmx${API_MAX_HEAP}m -Dspring.profiles.active=api -Dkeystore.pkcs12.legacy -jar takserver.war $@

# To run with plugin support enable in CoreConfig and place plugin jars in /opt/tak/lib.
