#!/bin/bash

# This script will wait until the final postgres (which allows connections) started in the /docker-entrypoint.sh.
# Then, create and initialize all the databases.

trap exit_script SIGINT SIGTERM SIGQUIT

exit_script() {
    echo "Shutting down pgpool gracefully."
    su - pgpool -c "/usr/bin/pgpool -f /etc/pgpool-II/pgpool.conf -F /etc/pgpool-II/pcp.conf -m fast stop"
    echo "Shutting down postgresql gracefully."
    su - postgres -c "/usr/pgsql-15/bin/pg_ctl -w -D $PGDATA -m fast stop "

    trap - SIGINT SIGTERM SIGQUIT # clear the trap
}

export PGDATA="/var/lib/postgresql/data"

echo "Clearing postgresql postmaster.pid if it exists..."
rm -f $PGDATA/postmaster.pid

# Check if the database is already initialized
if [ -z "$(ls -A -- "$PGDATA")" ]; then
    echo "Initializing the database..."
    su - postgres -c "/usr/pgsql-15/bin/pg_ctl initdb -D $PGDATA"

    echo "Copying in our custom pg_hba.conf configuration file.."
    PGHBA=$PGDATA/pg_hba.conf
    cp /opt/tak/db-utils/full/pg_hba.conf $PGHBA

    PGIDENT=$PGDATA/pg_ident.conf
    PGCONFIG=$PGDATA/postgresql.conf
    cp /opt/tak/db-utils/full/postgresql.conf $PGCONFIG

    # multi distro compatibility
    sed -i -e"s|^[# ]*data_directory[ ]*=.*$|data_directory = '$PGDATA'|" $PGCONFIG
    sed -i -e"s|^[# ]*hba_file[ ]*=.*$|hba_file = '$PGHBA'|" $PGCONFIG
    sed -i -e"s|^[# ]*ident_file[ ]*=.*$|ident_file = '$PGIDENT'|" $PGCONFIG
fi

echo "Starting the database..."
su - postgres -c "/usr/pgsql-15/bin/pg_ctl -w -D $PGDATA -l logfile start -o '-c max_connections=2100 -c shared_buffers=2560MB'"

cd /opt/tak/db-utils
./full/takserver-setup-db-full.sh

echo "Waiting to allow postgres to start so pgpool doesn't think it is down before it actually starts"
pg_isready -q -t 0

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

echo "Clearing pgpool pid if it exists..."
rm -f /var/run/pgpool/*.pid

#start pgpool
echo "Starting pgpool ..."
su - pgpool -c "/usr/bin/pgpool -n -f /etc/pgpool-II/pgpool.conf -F /etc/pgpool-II/pcp.conf &"

echo "Wait until pgpool starts"
while ! nc -z localhost 9999; do
    sleep 1
done
echo "PGPool started"

echo "Running SchemaManager Upgrade..."
java -jar SchemaManager.jar upgrade
echo "SchemaManager Upgrade Complete."

# Use pg_ctl status output to get the PID
DB_PID=`su - postgres -c "/usr/pgsql-15/bin/pg_ctl -D $PGDATA status" | grep "server is running" | tr -d ' ' | tr -d ')' | cut -f3 -d ':' `

# We can't use wait because the Postgres process is under a different user, etc.
# and we don't have "ps" installed. So, check if the PID directory exists under proc.
# Check every 8 seconds to allow a relatively fast shutdown but not use a lot of cycles
# on the check.
while [ -d "/proc/$DB_PID" ]; do
  sleep 8
done
