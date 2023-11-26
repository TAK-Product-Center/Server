#!/bin/sh

. ./setenv.sh

java -jar -Dspring.profiles.active=monolith takserver.war $@

# To run with plugin support enable in CoreConfig and place plugin jars in /opt/tak/lib.
