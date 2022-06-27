#!/bin/bash

java -jar SchemaManager.jar SetupGenericDatabase
java -jar SchemaManager.jar upgrade

if curl -sL --fail http://localhost:15021/healthz/ready -o /dev/null; then
  # Shutdown Istio sidecar if it exists
  curl -fsI -X POST http://localhost:15020/quitquitquit
fi