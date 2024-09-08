#!/usr/bin/env bash

set -e

echo Copying read-only source directories to writable directories, please wait...
unzip takserver-cluster.zip
cp -R /aws-src /root/.aws
cp -R /cluster-properties /cluster/cluster-properties

cd /cluster
source cluster-properties

export CLUSTER_HOME_DIR=/cluster

if [[ "${TAK_CERT_SOURCE_DIR}" != "" ]];then
	cp -R "${TAK_CERT_SOURCE_DIR}" /tak-cert-source-dir
	export TAK_CERT_SOURCE_DIR=/tak-cert-source-dir
fi

echo Please execute \`python3 scripts/build-eks.py\` to build the aws cluster ${TAK_CLUSTER_NAME}

bash
