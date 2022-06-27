#!/bin/bash

export FEDERATION_HUB=/opt/tak/federation-hub

java -Dlogging.config=${FEDERATION_HUB}/configs/logback-ui.xml -jar ${FEDERATION_HUB}/jars/federation-hub-ui.war "$@"
