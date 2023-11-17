#!/usr/bin/env bash

set -e

if [[ "${1}" == "" ]];then
	echo Please provide the location of the "cluster" dir as an argument!
	exit 1
fi

CLUSTER_SRC="$(pwd)/${1}"

docker build --file=files/Dockerfile --tag=tak-deployer:latest ./files
docker run -it --name=tak-cluster-deployer \
	--mount type=bind,source="${CLUSTER_SRC}",target=/cluster-src,readonly \
	--mount type=bind,source="${HOME}"/.aws,target=/aws-src,readonly \
	--mount type=bind,source=/home/awellman/Documents/TAKSERVER/test_certs-minimized,target=/certs-src,readonly \
	-v /var/run/docker.sock:/var/run/docker.sock \
	tak-deployer:latest
