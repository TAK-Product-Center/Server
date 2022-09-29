#!/bin/bash

# This script will wait until the final postgres (which allows connections) started in the /docker-entrypoint.sh.
# Then, create and initialize all the databases.
/usr/local/bin/docker-entrypoint.sh postgres &

while true; do
	sleep 2
		pg_isready -d postgres -h localhost -U postgres
		success=$?
		if [ $success -ne 0 ]; then
		 echo "postgres server is not ready"
		 continue;
		fi

		cp /opt/tak/db-utils/pg_hba.conf $PGDATA/pg_hba.conf
		chmod 600 $PGDATA/pg_hba.conf
		pg_ctl reload -D $PGDATA

		cd /opt/tak/db-utils
		./configure.sh

		java -jar SchemaManager.jar upgrade
		tail -f /dev/null

		break
done