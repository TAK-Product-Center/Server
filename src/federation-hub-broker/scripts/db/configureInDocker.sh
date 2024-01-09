#!/bin/sh

echo "Running configureInDocker.sh"

cd /opt/tak/federation-hub

if [ ! -f "/etc/mongod.conf" ]; then
	echo "Mongo has not been setup set. Trying now."

	/usr/bin/mongod --bind_ip_all --config /opt/tak/federation-hub/scripts/db/mongod-noauth.conf &
	mongoPID=$!
	sleep 5
	/opt/tak/federation-hub/scripts/db/setup-db.sh
	
	sleep 1
    rm -f /tmp/mongodb-27017.sock
    sleep 1

	kill -9 $mongoPID
	sleep 5
	cp /opt/tak/federation-hub/scripts/db/mongod-auth.conf /etc/mongod.conf
fi

/usr/bin/mongod --bind_ip_all --config /etc/mongod.conf &
