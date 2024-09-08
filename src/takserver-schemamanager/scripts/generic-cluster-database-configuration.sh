#!/bin/bash

DB_URL=jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB}

# Wait for the server to become available
RVAL=-1
while [ $RVAL != 0 ];do
    nc -zw3 ${POSTGRES_HOST} ${POSTGRES_PORT}
    RVAL=$?
    sleep 1
done

java -jar SchemaManager.jar -url ${DB_URL} -user ${POSTGRES_USER} -password ${POSTGRES_PASSWORD}  SetupGenericDatabase
java -jar SchemaManager.jar -url ${DB_URL} -user ${POSTGRES_USER} -password ${POSTGRES_PASSWORD}  upgrade

if curl -sL --fail http://localhost:15021/healthz/ready -o /dev/null; then
  # Shutdown Istio sidecar if it exists
  curl -fsI -X POST http://localhost:15020/quitquitquit
fi
