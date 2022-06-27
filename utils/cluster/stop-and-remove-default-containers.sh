#/bin/bash

rm -r target
docker stop takserver-ca && docker rm takserver-ca
docker stop takserver-database && docker rm takserver-database
docker stop takserver-core && docker rm takserver-core
docker network rm takserver-net
echo Default docker containers and network deleted, if they existed.
