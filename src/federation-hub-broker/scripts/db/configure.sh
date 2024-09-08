#!/bin/sh

echo "Configuring DB"

cd /opt/tak/federation-hub

# stop mongo
echo "Trying to setup Mongo."
systemctl stop mongod
sleep 1

# create data directory, set mongod permissions, remove lock file
echo "Creating Mongo data directory at /var/lib/mongodb"
mkdir -p /var/lib/mongodb

# Debian installs
if [ -f /etc/debian_version ]; then
    chown -R mongodb:mongodb /var/lib/mongodb
# Other OS
else
   chown -R mongod:mongod /var/lib/mongodb
   chcon -Rv --type=mongod_var_lib_t /var/lib/mongodb
fi

rm -f /tmp/mongodb-27017.sock
sleep 1
cp /opt/tak/federation-hub/scripts/db/mongod-noauth.conf /etc/mongod.conf

echo "Restarting Mongo..."
systemctl start mongod
sleep 10

# generate username + password and create user with mongosh
/opt/tak/federation-hub/scripts/db/setup-db.sh

sleep 1
rm -f /tmp/mongodb-27017.sock  
sleep 1

cp /opt/tak/federation-hub/scripts/db/mongod-auth.conf /etc/mongod.conf
sleep 1
systemctl restart mongod

echo "Mongo Setup!"