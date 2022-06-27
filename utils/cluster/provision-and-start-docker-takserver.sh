#/bin/bash

sh provision-ca.sh

echo Creating provisioned core image
docker build -f Dockerfile.takserver-core-provisioned -t tak/server-core-provisioned:latest .

echo Creating local bridge network
docker network create takserver-net

echo Starting database
docker run --name takserver-database --network takserver-net -d tak/server-database:latest

echo Starting provisioned core takserver
docker run --name takserver-core --network takserver-net -p 8089:8089 -p 8443:8443 -p 8444:8444 -p 8446:8446 -t -d tak/server-core-provisioned:latest
