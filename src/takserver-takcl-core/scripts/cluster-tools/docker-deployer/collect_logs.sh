#!/usr/bin/env bash

docker exec -it tak-cluster-deployer python3 /collect-cluster-logs.py
docker cp tak-cluster-deployer:/takserver-cluster-logs .
