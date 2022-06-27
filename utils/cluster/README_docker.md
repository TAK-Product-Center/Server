# Docker Quickstart

## Prerequisties
OpenJDK 11
Docker 18

## build the base docker image.
The docker image for takserver-core depends on this.

```
cd utils/cluster
docker build -f Dockerfile.postgres10_postgis24 -t tak/postgres10_postgis24:latest .
```

```
./provision-and-start-docker-takserver.sh
```

OR

## or build the base image, skipping cache
```  
cd utils/cluster
docker build --no-cache -f Dockerfile.postgres10_postgis24 -t tak/postgres10_postgis24:latest .
```

## build images from source
```
cd ../../src
./gradlew clean buildDocker buildDockerCA
```

## run ca container, generate certs and copy all files to current directory (on docker host)
```
cd ../utils/cluster
docker run --name takserver-ca -e STATE=MA -e CITY=Cambridge -e ORGANIZATIONAL_UNIT=takorg -w /certs -t -d tak/server-ca:latest /bin/bash -c 'makeRootCa.sh --ca-name DOCKER1 && \
         makeCert.sh server takserver && \ 
         makeCert.sh client user && \ 
         makeCert.sh client admin && \
         bash' && \
  echo 'generating CA and certs for DOCKER1' && \
  sleep 5 && \
  docker cp takserver-ca:/certs/files/. target
```

## create docker bridge network to run takserver
```
docker network create takserver-net
```

## run database, connect to network
```
docker run --name takserver-database --network takserver-net -d tak/server-database:latest
```

## build the provisioned core image, including trustores and server cert.
```
docker build -f Dockerfile.takserver-core-provisioned -t tak/server-core-provisioned:latest .
```

## run provisioned core, connect to docker bridge network, and expose default takserver ports on docker host.
```
docker run --name takserver-core --network takserver-net -p 8089:8089 -p 8443:8443 -p 8444:8444 -p 8446:8446 -t -d tak/server-core-provisioned:latest
```

## connect to provisioned core container
```
docker exec -it takserver-core bash
```

## verify listening on correct ports
```
root@5b9c72873922:/## netstat -tulpn
Active Internet connections (only servers)
Proto Recv-Q Send-Q Local Address           Foreign Address         State       PID/Program name    
tcp        0      0 0.0.0.0:8089            0.0.0.0:*               LISTEN      7/java              
tcp        0      0 0.0.0.0:8443            0.0.0.0:*               LISTEN      7/java              
tcp        0      0 0.0.0.0:8444            0.0.0.0:*               LISTEN      7/java              
tcp        0      0 0.0.0.0:8446            0.0.0.0:*               LISTEN      7/java              
tcp        0      0 0.0.0.0:36481           0.0.0.0:*               LISTEN      7/java              
tcp        0      0 127.0.0.11:46309        0.0.0.0:*               LISTEN      -                   
tcp        0      0 0.0.0.0:3334            0.0.0.0:*               LISTEN      7/java              
udp        0      0 127.0.0.11:52369        0.0.0.0:*                           -
```



## To delete and stop everything created by this procedure (*CAREFUL*)
```
rm -r target
docker stop takserver-ca && docker rm takserver-ca
docker stop takserver-database && docker rm takserver-database
docker stop takserver-core && docker rm takserver-core
docker network rm takserver-net
```
