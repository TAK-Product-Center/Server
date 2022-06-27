#/bin/bash

echo Provisioning local CA and certificates
docker run --name takserver-ca -e STATE=MA -e CITY=Cambridge -e ORGANIZATIONAL_UNIT=takorg -w /certs -t -d tak/server-ca:latest /bin/bash -c 'makeRootCa.sh --c\
a-name DOCKER1 && \
         makeCert.sh server takserver && \
         makeCert.sh client user && \
         makeCert.sh client admin && \
         bash' && \
  echo 'generating CA and certs for DOCKER1' && \
  sleep 5 && \
  docker cp takserver-ca:/certs/files/. target


