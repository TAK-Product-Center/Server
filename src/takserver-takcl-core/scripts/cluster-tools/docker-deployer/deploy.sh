#!/usr/bin/env bash

set -e

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" > /dev/null 2>&1 && pwd )
CANDIDATE_COUNT=$(ls -1 ${SCRIPT_DIR}/../../../../takserver-cluster/build/distributions/takserver-cluster-*.zip | wc -l)

if [[ "${1}" == "" ]];then
	echo Please provide the location of the desired cluster-properties file!
	exit 1
fi

if [[ ! -f "${1}" ]];then
	echo The specified configuration file "${1}" does not exist!
fi

if [[ "${TAKSERVER_CERT_SOURCE}" == "" ]];then
	echo Please set the environment variable TAKSERVER_CERT_SOURCE to the directory of your takserver certs!
	exit 1
fi

if [[ "${CANDIDATE_COUNT}" != "1" ]];then
	echo Please make sure one and only one cluster deploy zip exists in takserver-cluster/build/distributions!
	exit 1
fi

CLUSTER_PROPERTIES=$(realpath ${1})

DEPLOYMENT_ZIP=$(ls -1 ${SCRIPT_DIR}/../../../../takserver-cluster/build/distributions/takserver-cluster-*.zip)
DEPLOYMENT_ZIP=$(realpath ${DEPLOYMENT_ZIP})
CONTAINER_NAME=$(basename ${DEPLOYMENT_ZIP})
CONTAINER_NAME="tak-deployer-${CONTAINER_NAME%.*}"

CLUSTER_SRC="$(realpath ${1})"

DOCKERFILE="${SCRIPT_DIR}/files/Dockerfile"
DOCKERCONTEXT="${SCRIPT_DIR}/files"

docker build --file="${DOCKERFILE}" --tag=tak-deployer:latest "${DOCKERCONTEXT}"
docker run -it --name="${CONTAINER_NAME}-nocerts" \
	--mount type=bind,source="${CLUSTER_PROPERTIES}",target=/cluster-properties,readonly \
	--mount type=bind,source="${HOME}"/.aws,target=/aws-src,readonly \
	--mount type=bind,source="${TAKSERVER_CERT_SOURCE}",target=/certs-src,readonly \
	--mount type=bind,source="${DEPLOYMENT_ZIP}",target=/takserver-cluster.zip,readonly \
	-v /var/run/docker.sock:/var/run/docker.sock \
	tak-deployer:latest
