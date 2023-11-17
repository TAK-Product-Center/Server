#!/bin/bash

# This script will wait until the final postgres (which allows connections) started in the /docker-entrypoint.sh.
# Then, create and initialize all the databases.

export PGDATA="/var/lib/postgresql/data"

echo "Initializing the database..."
su - postgres -c "/usr/pgsql-15/bin/pg_ctl initdb -D $PGDATA"

echo "Copying in our custom pg_hba.conf configuration file.."
cp /opt/tak/db-utils/full/pg_hba.conf $PGDATA/pg_hba.conf

cp /opt/tak/db-utils/full/postgresql.conf $PGDATA/postgresql.conf

echo "Starting the database..."
su - postgres -c "/usr/pgsql-15/bin/pg_ctl -D $PGDATA -l logfile start -o '-c max_connections=2100 -c shared_buffers=2560MB'"


cd /opt/tak/db-utils
./full/takserver-setup-db-full.sh

echo "Sleeping for 20 sec to allow postgres to start so pgpool doesn't think it is down before it actually starts"
sleep 20

# Not necessary to update primary and standby addresses in pgpool since we can assume a hostname
# being the same as the service in the compose file
echo "Copying pgpool conf and script files to container installation"
rm -f /etc/pgpool-II/*.sample
cp /opt/tak/db-utils/full/pgpool/*.conf /etc/pgpool-II
cp /opt/tak/db-utils/full/pgpool/*.sh /etc/pgpool-II

chown -R pgpool:pgpool /etc/pgpool-II

echo "Creating /var/run/pgpool and changing ownership to pgpool"
mkdir -p /var/run/pgpool
chown -R pgpool:pgpool /var/run/pgpool

#start pgpool
echo "Starting pgpool ..."
su - pgpool -c "/usr/bin/pgpool -n -f /etc/pgpool-II/pgpool.conf &"

echo "Sleeping to allow pgpool to startup"
sleep 20

echo "Running SchemaManager Upgrade..."
java -jar SchemaManager.jar upgrade
echo "SchemaManager Upgrade Complete."
tail -f /dev/null
