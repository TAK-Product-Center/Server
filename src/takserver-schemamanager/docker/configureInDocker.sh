#!/bin/bash

# Note, 1 variable to set prevents mistakes in the future.
PGVER=15

echo "Initializing the database..."
su - postgres -c "/usr/lib/postgresql/$PGVER/bin/pg_ctl initdb -D /var/lib/postgresql/$PGVER/data"

echo "Copying in our custom pg_hba.conf configuration file.."
cp /opt/tak/db-utils/pg_hba.conf /var/lib/postgresql/$PGVER/data/pg_hba.conf

echo "Starting the database..."
su - postgres -c "/usr/lib/postgresql/$PGVER/bin/pg_ctl -D /var/lib/postgresql/$PGVER/data -l logfile start -o '-c max_connections=2100 -c shared_buffers=2560MB'"

echo "Configuring the database..."
cd /opt/tak/db-utils
./configure.sh

echo "Performing any TAK Server specific upgrades to the schema..."
java -jar SchemaManager.jar upgrade

tail -f /dev/null
