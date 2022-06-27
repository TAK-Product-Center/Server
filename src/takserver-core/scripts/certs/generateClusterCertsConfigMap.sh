#!/bin/bash
set -u
./generateClusterCerts.sh
kubectl create configmap cert-migration --from-file="./files" --dry-run=client -o yaml >cert-migration.yaml