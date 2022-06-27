#!/bin/bash
chown postgres:postgres /var/lib/postgresql/data
su - postgres -c '/usr/lib/postgresql/10/bin/pg_ctl initdb -D /var/lib/postgresql/data'

cp /opt/tak/db-utils/pg_hba.conf /var/lib/postgresql/data/pg_hba.conf
su - postgres -c "/usr/lib/postgresql/10/bin/pg_ctl -D /var/lib/postgresql/data -l logfile start -o '-c max_connections=2100 -c shared_buffers=2560MB'"
cd /opt/tak/db-utils
./configure.sh
java -jar SchemaManager.jar upgrade
tail -f /dev/null
