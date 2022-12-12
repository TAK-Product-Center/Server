#!/bin/bash

set -u

kubectl create configmap core-config --from-file="../CoreConfig.xml" --dry-run=client -o yaml > core-config.yaml
