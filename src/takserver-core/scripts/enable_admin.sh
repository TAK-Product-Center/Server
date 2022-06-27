#!/bin/sh

while ! grep -q 'com.bbn.marti.nio.server.NioServer - Server started' '/logs/takserver-messaging.log'; do
    sleep 5
done

until java -jar UserManager.jar certmod -A certs/files/admin.pem
do
    sleep 5
done
