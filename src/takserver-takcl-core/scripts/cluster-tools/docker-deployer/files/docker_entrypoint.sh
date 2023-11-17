#!/usr/bin/env bash

set -e

echo Copying read-only source directories to writable directories, please wait...
cp -R /cluster-src /cluster
cp -R /aws-src /root/.aws
cp -R /certs-src /cluster/takserver-core/certs/files

cd /cluster
source cluster-properties

export CLUSTER_HOME_DIR=/cluster

python3 scripts/build-eks.py

bash
