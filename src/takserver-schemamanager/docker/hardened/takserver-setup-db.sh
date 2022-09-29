#!/bin/bash

# Script to set up the TAKServer database.
# This is meant to be run as root.
# Since it asks the user for confirmation before obliterating his database,
# it cannot be run by the RPM installer and must be a manual post-install step.
#
# Usage: takserver-db-setup.sh [db-name]
#

#if [ "$EUID" -ne 0 ]
#  then echo "$0 must be run as root."
#  exit 1
#fi

username='martiuser'
password=""
# try to get password from /opt/tak/CoreConfig.xml
if [ -f "/opt/tak/CoreConfig.xml" ]; then
  password=$(echo $(grep -m 1 "<connection" /opt/tak/CoreConfig.xml)  | sed 's/.*password="//; s/".*//')
fi
# try to get password from /opt/tak/CoreConfig.example.xml
if [ -z "$password" ]; then
  if [ -f "/opt/tak/CoreConfig.example.xml" ]; then
    password=$(echo $(grep -m 1 "<connection" /opt/tak/CoreConfig.example.xml)  | sed 's/.*password="//; s/".*//')
  fi
fi
# cant find password - request one from user
if [ -z "$password" ]; then
  : ${1?' Could not find a password in /opt/tak/CoreConfig.xml or /opt/tak/CoreConfig.example.xml. Please supply a plaintext database password as the first parameter'}
  password=$1
fi

md5pass=$(echo -n "md5" && echo -n "$password$username" | md5sum | tr -dc "a-zA-Z0-9\n")

# switch CWD to the location where this script resides
cd `dirname $0`

DB_NAME=$1
if [ $# -lt 1 ]; then
  DB_NAME=cot
fi

DB_INIT=""
# Ensure PostgreSQL is initialized.

if [ -x /usr/pgsql-10/bin/postgresql-10-setup ]; then
    DB_INIT="/usr/pgsql-10/bin/postgresql-10-setup initdb"
elif [ -x /usr/bin/postgresql-setup ]; then
    DB_INIT="/usr/bin/postgresql-setup initdb"
elif [ -x /usr/pgsql-9.4/bin/initdb ]; then
    DB_INIT="service postgresql-9.4 initdb"
elif [ -x /usr/pgsql-9.5/bin/initdb ]; then
    DB_INIT="service postgresql-9.5 initdb"
elif [ -x /usr/pgsql-9.6/bin/initdb ]; then
    DB_INIT="service postgresql-9.6 initdb"
else
    echo "WARNING: Unable to automatically initialize PostgreSQL database."
fi

echo -n "Database initialization: " 
$DB_INIT 
if [ $? -eq 0 ]; then
    echo "PostgreSQL database initialized."
else
    echo "WARNING: Failed to initialize PostgreSQL database."
    echo "This could simply mean your database has already been initialized."
fi
      
# Figure out where the system keeps the PostgreSQL data

if [ -z ${PGDATA+x} ]; then
   if [ -d /var/lib/pgsql/10/data ]; then
        export PGDATA=/var/lib/pgsql/10/data
        POSTGRES_CMD="service postgresql-10 restart"
   elif [ -x /usr/bin/systemctl ]; then
       export PGDATA=`/usr/bin/systemctl show postgresql.service -p Environment | sed 's/.*PGDATA\=\([a-zA-Z0-9]*\)/\1/'`
       POSTGRES_CMD="/bin/systemctl restart postgresql.service"
   elif [ -d /var/lib/pgsql/10/data ]; then
        export PGDATA=/var/lib/pgsql/10/data
        POSTGRES_CMD="service postgresql-10 restart"
   elif [ -d /var/lib/pgsql/9.6/data ]; then
       export PGDATA=/var/lib/pgsql/9.6/data
       POSTGRES_CMD="service postgresql-9.6 restart"
   elif [ -d /var/lib/pgsql/9.5/data ]; then
       export PGDATA=/var/lib/pgsql/9.5/data
       POSTGRES_CMD="service postgresql-9.5 restart"
   elif [ -d /var/lib/pgsql/9.4/data ]; then
       export PGDATA=/var/lib/pgsql/9.4/data
       POSTGRES_CMD="service postgresql-9.4 restart"
   elif [ -d /var/lib/pgsql/9.3/data ]; then
       export PGDATA=/var/lib/pgdsql/9.3/data
       POSTGRES_CMD="service postgresql-9.3 restart"
   elif [ -d /var/lib/pgsql/9.2/data ]; then
       export PGDATA=/var/lib/pgsql/9.2/data
       POSTGRES_CMD="service postgresql-9.2 restart"
   elif [ -d /var/lib/pgsql/data ]; then
       export PGDATA=/var/lib/pgdata/data
       POSTGRES_CMD="service postgresql restart"
   else
       echo "PGDATA not set and unable to find PostgreSQL data directory automatically."
       echo "Please set PGDATA and re-run this script."
       exit 1
   fi
fi

if [ ! -d $PGDATA ]; then
  echo "ERROR: Cannot find PostgreSQL data directory. Please set PGDATA manually and re-run."
  exit 1
fi

# Get user's permission before obliterating the database
DB_EXISTS=`psql -l 2>/dev/null | grep ^[[:blank:]]*$DB_NAME`
if [ "x$DB_EXISTS" != "x" ]; then
   echo "WARNING: Database '$DB_NAME' already exists!"
   echo "Proceeding will DESTROY your existing data!"
   echo "You can back up your data using the pg_dump command. (See 'man pg_dump' for details.)"
   read -p "Type 'erase' (without quotes) to erase the '$DB_NAME' database now:" kickme
   if [ "$kickme" != "erase" ]; then
       echo "User didn't say 'erase'. Aborting."
       exit 1
   fi
   #su postgres -c "psql --command='drop database if exists $DB_NAME;'"
   psql --command='drop database if exists $DB_NAME;'
fi

if [ -e pg_hba.conf ]; then
  IS_DOCKER='true'
fi

# Install our version of pg_hba.conf
echo "Installing TAKServer's version of PostgreSQL access-control policy."
# Back up pg_hba.conf
BACKUP_SUFFIX=`date --rfc-3339='seconds' | sed 's/ /-/'`
HBA_BACKUP=$PGDATA/pg_hba.conf.backup-$BACKUP_SUFFIX
if [ -e /opt/tak/db-utils/pg_hba.conf ] || [ -e pg_hba.conf ]; then
  if [ -e $PGDATA/pg_hba.conf ]; then
    mv $PGDATA/pg_hba.conf $HBA_BACKUP
    echo "Copied existing PostgreSQL access-control policy to $HBA_BACKUP."
  fi

   # for docker install
  if [ IS_DOCKER ]; then
    cp pg_hba.conf $PGDATA
  else
    # for RPM install
    echo "RPM db install"
      cp /opt/tak/db-utils/pg_hba.conf $PGDATA
  fi

  echo "Installed TAKServer's PostgreSQL access-control policy to $PGDATA/pg_hba.conf."
  echo "Restarting PostgreSQL service."
  $POSTGRES_CMD
else
  echo "ERROR: Unable to find pg_hba.conf!"
  exit 1
fi

CONF_BACKUP=$PGDATA/postgresql.conf.backup-$BACKUP_SUFFIX
if [ -e /opt/tak/db-utils/postgresql.conf ] || [ -e postgresql.conf ];  then
  if [ -e $PGDATA/postgresql.conf ]; then
    mv $PGDATA/postgresql.conf $CONF_BACKUP
    echo "Copied existing PostgreSQL configuration to $CONF_BACKUP."
  fi

   # for docker install
  if [ IS_DOCKER ]; then
    cp postgresql.conf $PGDATA
  else
    # for RPM install
    echo "RPM db install"
      cp /opt/tak/db-utils/postgresql.conf $PGDATA
  fi

  echo "Installed TAKServer's PostgreSQL configuration to $PGDATA/postgresql.conf."
  echo "Restarting PostgreSQL service."
  $POSTGRES_CMD
fi

DB_NAME=cot
if [ $# -eq 1 ] ; then
    DB_NAME=$1
fi

# Create the user "martiuser" if it does not exist.
echo "Creating user \"martiuser\" ..."
psql -U postgres -c "CREATE ROLE martiuser LOGIN ENCRYPTED PASSWORD '$md5pass' SUPERUSER INHERIT CREATEDB NOCREATEROLE;"

# create the database
echo "Creating database $DB_NAME"
createdb -U postgres --owner=martiuser $DB_NAME
if [ $? -ne 0 ]; then
    exit 1
fi

echo "Database $DB_NAME created."

if [ IS_DOCKER ]; then
   java -jar SchemaManager.jar upgrade
elif [ -e /opt/tak/db-utils/SchemaManager.jar ]; then
   java -jar /opt/tak/db-utils/SchemaManager.jar upgrade
else
   echo "ERROR: Unable to find SchemaManager.jar!"
   exit 1
fi

echo "Database updated with SchemaManager.jar"

if [ ! -x /usr/bin/systemctl ]; then
  echo "Systemctl was not found. Skipping Systemd configuration."
  exit 1
fi

# Set PostgreSQL to run automatically at boot time
if [ -d /var/lib/pgsql/10/data ]; then
    START_INIT="chkconfig --level 345 postgresql-10 on"
elif [ -x /usr/bin/systemctl ]; then
    /usr/bin/systemctl enable postgresql.service
elif [ -d /var/lib/pgsql/9.4/data ]; then
    START_INIT="chkconfig --level 345 postgresql-9.4 on"
elif [ -d /var/lib/pgsql/9.5/data ]; then
    START_INIT="chkconfig --level 345 postgresql-9.5 on"
elif [ -d /var/lib/pgsql/9.6/data ]; then
    START_INIT="chkconfig --level 345 postgresql-9.6 on"
else
  echo "ERROR: unable to detect postgres version to start on boot"
  exit 1
fi
    
$START_INIT
echo "set postgres to start on boot"