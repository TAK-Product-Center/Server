#!/bin/bash

if [ $# -eq 0 ]
  then
    ps -ef | grep postgres | grep -v grep | awk '{print $2}' | xargs kill
fi

sleep 1

# Note, 1 variable to set prevents mistakes in the future.
PGVER=15

echo "Initializing the database..."
su - postgres -c "/usr/lib/postgresql/$PGVER/bin/pg_ctl initdb -D /var/lib/postgresql/$PGVER/data"

echo "Copying in our custom pg_hba.conf configuration file.."
cp /opt/tak/db-utils/pg_hba.conf /var/lib/postgresql/$PGVER/data/pg_hba.conf

echo "Starting the database..."
su - postgres -c "/usr/lib/postgresql/$PGVER/bin/pg_ctl -D /var/lib/postgresql/$PGVER/data -l /var/lib/postgresql/$PGVER/data/postgres.log start -o '-c max_connections=2100 -c shared_buffers=2560MB' --wait"

echo "Configuring the database..."
cd /opt/tak/db-utils
./configure.sh

echo "Performing any TAK Server specific upgrades to the schema..."
java -jar SchemaManager.jar upgrade

echo "Restarting the database..."
su - postgres -c "/usr/lib/postgresql/$PGVER/bin/pg_ctl -D /var/lib/postgresql/$PGVER/data -l /var/lib/postgresql/$PGVER/data/postgres.log restart -o '-c max_connections=2100 -c shared_buffers=2560MB' --wait"

tail -f /dev/null
