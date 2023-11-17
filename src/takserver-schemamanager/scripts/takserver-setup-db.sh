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

run_schema_manager () {
  echo "Applying the TAKServer specific schema changes for the TAK Server version..."
  if [ "$IS_DOCKER" = true ]; then
     java -jar SchemaManager.jar upgrade
  elif [ -e /opt/tak/db-utils/SchemaManager.jar ]; then
     java -jar /opt/tak/db-utils/SchemaManager.jar upgrade
  else
     echo "ERROR: Unable to find SchemaManager.jar!"
     exit 1
  fi
  echo "Database updated with SchemaManager.jar"
}

echo "TAK Server DB Setup"

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

# If the envpass environment variable is set, set the password for the example. 
# This allows for prebuilt docker containers (such as ironbank) to have configuration passed to them
if [ ! -z "$envpass" ]; then
  password=$envpass
  sed -i "s/password=\"\"/password=\"$envpass\"/g" /opt/tak/CoreConfig.example.xml
fi

# cant find password - request one from user
if [ -z "$password" ]; then
  read -p 'Could not find a password in /opt/tak/CoreConfig.xml or /opt/tak/CoreConfig.example.xml. Please supply a plaintext database password: ' userPasswordInput
  password=$userPasswordInput
fi

# cant read password from user input, exit and warn
if [ -z "$password" ]; then
  echo "WARNING: Skipping schema changes, no DB password could be found! These can be manually applied by running sudo /opt/tak/db-utils/takserver-setup-db.sh"
  exit 1
fi

# switch CWD to the location where this script resides
cd `dirname $0`

DB_NAME=$1
if [ $# -lt 1 ]; then
  DB_NAME=cot
fi

DB_INIT=""
IS_DOCKER=false
SVC_ENABLE=""
PGBIN_NEW=""
PGBIN_OLD=""
PGDATA_NEW=""
PGDATA_OLD=""
PRE_POSTGRES_10=true
OLD_SVC_FILENAME=""
SVC_FILENAME=""
POSTGRES_POST_CMD=""
IS_DEBIAN=false

echo "Checking if running in docker container..."
if [ -f /.dockerenv ]; then
    echo "Docker installation detected.";
    IS_DOCKER=true
else
    echo "Not running in a container.";
fi

# Default to RHEL/Rocky/CentOS install. Otherwise, do Debian specific logic
if [ -f /etc/debian_version ]; then
   echo "Debian installation detected."
   IS_DEBIAN=true
fi

echo "Checking that postgresql 15 is installed..."
if [ -x /usr/pgsql-15/bin/postgresql-15-setup ]; then
   PGBIN_NEW=/usr/pgsql-15/bin
   PGDATA_NEW=/var/lib/pgsql/15/data
elif [ -x /usr/lib/postgresql/15/bin/pg_ctl ]; then
   PGBIN_NEW=/usr/lib/postgresql/15/bin
   if [ "$IS_DEBIAN" = true ]; then
      if [ "$IS_DOCKER" = true ]; then
         PGDATA_NEW=/var/lib/postgresql/15/data
      else
         PGDATA_NEW=/var/lib/postgresql/15/main
      fi
   else
     PGDATA_NEW=/var/lib/postgresql/15/data
   fi
else
   echo "You need to install Postgresql 15 before running this script"
   exit 1
fi

if [ "$IS_DOCKER" = false ]; then
   echo "Determining previous postgres version if there was one..."
   # Assume (uh-oh.. wish there was a better determination) latest version is the new one and the version just below
   # is the previous version
   if [ -x /usr/pgsql-10/bin/postgresql-10-setup ]; then
      PGBIN_OLD=/usr/pgsql-10/bin
      PGDATA_OLD=/var/lib/pgsql/10/data
      OLD_SVC_FILENAME="postgresql-10.service"
      PRE_POSTGRES_10=false
   # Cover debian previous version for 10
   elif [ -x /usr/lib/postgresql/10/bin/pg_ctl ]; then
      PGBIN_OLD=/usr/lib/postgresql/10/bin
      if [ "$IS_DEBIAN" = true ]; then
        PGDATA_OLD=/var/lib/postgresql/10/main
      else
        PGDATA_OLD=/var/lib/postgresql/10/data
      fi
      PRE_POSTGRES_10=false
   elif [ -x /usr/pgsql-9.6/bin/initdb ]; then
      PGBIN_OLD=/usr/pgsql-9.6/bin
      PGDATA_OLD=/var/lib/pgsql/9.6/data
      OLD_SVC_FILENAME="postgresql-9.6.service"
   elif [ -x /usr/pgsql-9.5/bin/initdb ]; then
      PGBIN_OLD=/usr/pgsql-9.5/bin
      PGDATA_OLD=/var/lib/pgsql/9.5/data
      OLD_SVC_FILENAME="postgresql-9.5.service"
   elif [ -x /usr/pgsql-9.4/bin/initdb ]; then
      PGBIN_OLD=/usr/pgsql-9.4/bin
      PGDATA_OLD=/var/lib/pgsql/9.4/data
      OLD_SVC_FILENAME="postgresql-9.4.service"
   elif [ -x /usr/pgsql-9.3/bin/initdb ]; then
      PGBIN_OLD=/usr/pgsql-9.3/bin
      PGDATA_OLD=/var/lib/pgsql/9.3/data
      OLD_SVC_FILENAME="postgresql-9.3.service"
   elif [ -x /usr/pgsql-9.2/bin/initdb ]; then
      PGBIN_OLD=/usr/pgsql-9.2/bin
      PGDATA_OLD=/var/lib/pgsql/9.2/data
      OLD_SVC_FILENAME="postgresql-9.2.service"
   else
       echo "Previous version of compatible version (9.2 or later) of Postgresql not found.  Upgrade will be skipped."
   fi
fi

if [ ! -z "$PGDATA_NEW" ]; then
  echo "exporting PGDATA variable"
  export PGDATA=$PGDATA_NEW
else
  # we should NEVER reach this but its good bulletproofing
  echo "PGDATA_NEW somehow not set.  Exiting..."
  exit 1
fi

if [ ! -z "$PGDATA_OLD" ]; then
  echo "Checking disk space for upgrade..."
  PGDATA_OLD_SIZE=`du -s -m $PGDATA_OLD | cut -f 1 `
  # The division by one is to convert to an integer for comparison
  PGDATA_REQ_SIZE=`echo "$PGDATA_OLD_SIZE * 1.5/1" | bc`
  echo "Upgrade will require $PGDATA_REQ_SIZE MB free in partition with new Postgresql version"
  PGDATA_NEW_AVAIL=`df -m $PGDATA_NEW | grep -v Available | tr -s ' ' | cut -f 4 -d ' '`
  echo "There are $PGDATA_NEW_AVAIL MB available in partition with new Postgresql version"
  if [ "$PGDATA_NEW_AVAIL" -gt "$PGDATA_REQ_SIZE" ]; then
    echo "Disk space available is greater than the required space. Upgrade of the database can proceed."
  else
    echo "Disk space available is less than the required space for upgrade of the database.  Exiting..."
    exit 1
  fi
fi

# Ensure PostgreSQL is initialized.  Note: It should already be initialized for containers
if [ "$IS_DOCKER" = false ]; then
  if [ "$IS_DEBIAN" = true ]; then
      echo "Debian installation detected. Checking on cluster ..."
      clusters=`pg_lsclusters --no-header 2>/dev/null | wc --lines`
      if [ $? -ne 0 ] && [ $clusters -lt 1 ]; then
          echo "WARNING: Postgres does not appear to have been initialized."
      else
          echo "Postgres cluster check successful"
      fi
      SVC_FILENAME="postgresql.service"
  elif [ -x $PGBIN_NEW/postgresql-15-setup ]; then
       echo -n "Database initialization: "
       $PGBIN_NEW/postgresql-15-setup initdb
       if [ $? -eq 0 ]; then
          echo "PostgreSQL database initialized."
       else
         echo "WARNING: Failed to initialize PostgreSQL database."
         echo "This could simply mean your database has already been initialized."
       fi

      SVC_FILENAME="postgresql-15.service"
  else
    echo "WARNING: Unable to automatically initialize PostgreSQL database."
  fi

  if [ -x /usr/bin/systemctl ]; then
       # If systemctl is available, we need to call systemctl to make a link from /etc/systemd/system/<service file> to
       # /usr/lib/systemd/system/<service file>
      is_enabled=`systemctl is-enabled $SVC_FILENAME`
      if [ $is_enabled == "disabled" ]; then
          echo "Enabling Postgresql service"
          /usr/bin/systemctl enable $SVC_FILENAME
      fi
      POSTGRES_POST_CMD="/usr/bin/systemctl restart $SVC_FILENAME"
  else
     echo "Expected systemctl to be available for postgresql 15 install"
     exit 1
  fi
fi

if [ ! -z "$PGBIN_OLD" ]; then
  martiuser_exists=`su postgres -c "psql postgres -AXqtc \"SELECT 1 FROM pg_roles WHERE rolname='martiuser'\""`
  if [ $? -eq 0 ] && [ "$martiuser_exists" = "1" ]; then
       echo "Postgres user \"martiuser\" exists."
       if $(su - postgres -c "psql -U postgres cot -c ''"); then
            echo "pg15 db initalized, refusing to migrate pg10 db. Running schemamanager"
            run_schema_manager
            exit 0
       fi
  else
       echo "martiuser does not exist, pg15 db not initalized"
  fi

  echo "***** Upgrade needed *****"
  if [ "$PRE_POSTGRES_10" = true ]; then
     echo "Upgrading from a version prior to TAK 1.3.11 (Postgresql 10) is not tested and guaranteed to work!"
     # default to yes for non-interative script executions
     REPLY="y"
     read -p "Are you sure you want to proceed? (y or n)" -n 1 -r
     if [ "$REPLY" =~ "^[Nn]$" ]; then
       echo "\nExiting..."
       exit 1
     fi
  fi
  echo "Stopping old and new database servers in preparation for upgrade..."
  su - postgres -c "$PGBIN_OLD/pg_ctl -D $PGDATA_OLD stop"
  su - postgres -c "$PGBIN_NEW/pg_ctl -D $PGDATA_NEW stop"
  echo "Performing upgrade of data from $PGDATA_OLD directory to $PDDATA_NEW directory..."
  echo "NOTE: If you are upgrading a large DB, this could take a significant amount of time.  Make sure your invoke "
  echo "via a tty session so the script can output to the console or set the remote session timeout very large to ensure"
  echo "the upgrade completes before reaching the timeout to avoid losing the session in the middle of the upgrade. "
  su - postgres -c "$PGBIN_NEW/pg_upgrade --old-bindir=$PGBIN_OLD --new-bindir=$PGBIN_NEW --old-datadir=$PGDATA_OLD --new-datadir=$PGDATA_NEW"
  if [ $? -ne 0 ]; then
       echo "Database upgrade failed. Stopping to prevent data loss"
       exit 1
    fi
fi

if [ ! -d $PGDATA ]; then
  echo "ERROR: Cannot find PostgreSQL data directory. Please set PGDATA manually and re-run."
  exit 1
fi

# Verify and add en_US.UTF-8 locale on Debian based installations
if ! $(locale -a | grep -iq en_US.UTF8); then 
  if [ -e /etc/locale.gen ]; then
    echo "Adding en_US.UTF-8 locale"
    sudo sed -i -e"s/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/g" /etc/locale.gen
    sudo locale-gen
  fi
fi

# Start PG services to allow identification of data directory and config files
echo "(Re)starting PostgreSQL service."
$POSTGRES_POST_CMD

# Install our version of pg_hba.conf
echo "Installing TAKServer's version of PostgreSQL access-control policy."
PGHBA=$(su - postgres -c "psql -AXqtc 'SHOW hba_file'")
# Back up pg_hba.conf
BACKUP_SUFFIX=`date --rfc-3339='seconds' | sed 's/ /-/'`
HBA_BACKUP=$PGHBA.backup-$BACKUP_SUFFIX
if [ -e /opt/tak/db-utils/pg_hba.conf ] || [ -e pg_hba.conf ]; then
  if [ -e $PGHBA ]; then
    mv $PGHBA $HBA_BACKUP
    echo "Copied existing PostgreSQL access-control policy to $HBA_BACKUP."
  fi

   # for docker install
  if [ "$IS_DOCKER" = true ]; then
    echo "Docker db install"
    cp pg_hba.conf $PGHBA
  else
    # for RPM install
    echo "RPM/DEB db install"
    cp /opt/tak/db-utils/pg_hba.conf $PGHBA
  fi

  chown postgres:postgres $PGHBA
  chmod 600 $PGHBA
  echo "Installed TAKServer's PostgreSQL access-control policy to $PGHBA."
  echo "Restarting PostgreSQL service."
  $POSTGRES_POST_CMD
else
  echo "ERROR: Unable to find pg_hba.conf!"
  exit 1
fi

PGCONFIG=$(su - postgres -c "psql -AXqtc 'SHOW config_file'")
CONF_BACKUP=$PGCONFIG.backup-$BACKUP_SUFFIX
if [ -e /opt/tak/db-utils/postgresql.conf ] || [ -e postgresql.conf ];  then
  PGIDENT=$(su - postgres -c "psql -AXqtc 'SHOW ident_file'")

  if [ -e $PGCONFIG ]; then
    mv $PGCONFIG $CONF_BACKUP
    echo "Copied existing PostgreSQL configuration to $CONF_BACKUP."
  fi

   # for docker install
  if [ "$IS_DOCKER" = true ]; then
    cp postgresql.conf $PGCONFIG
  else
    # for RPM install
    echo "RPM/DEB db install"
    cp /opt/tak/db-utils/postgresql.conf $PGCONFIG
  fi

  # multi distro compatibility
  sed -i -e"s|^[# ]*data_directory[ ]*=.*$|data_directory = '$PGDATA'|" $PGCONFIG
  sed -i -e"s|^[# ]*hba_file[ ]*=.*$|hba_file = '$PGHBA'|" $PGCONFIG
  sed -i -e"s|^[# ]*ident_file[ ]*=.*$|ident_file = '$PGIDENT'|" $PGCONFIG

  chown postgres:postgres $PGCONFIG
  chmod 600 $PGCONFIG
  echo "Installed TAKServer's PostgreSQL configuration to $PGCONFIG."
  echo "Restarting PostgreSQL service."
  $POSTGRES_POST_CMD
fi

DB_NAME=cot
if [ $# -eq 1 ] ; then
    DB_NAME=$1
fi

# Only setup Database or reinitialize data if we didn't upgrade
if [ -z "$PGDATA_OLD" ]; then
    # Create the user "martiuser" if it does not exist.
    martiuser_exists=`su postgres -c "psql postgres -AXqtc \"SELECT 1 FROM pg_roles WHERE rolname='martiuser'\""`
    if [ $? -eq 0 ] && [ "$martiuser_exists" = "1" ]; then
       echo "Postgres use \"martiuser\" exists."
    else
       echo "Creating user \"martiuser\" ..."
       PASSWORD_ENCRYPTION=`su postgres -c "psql -AXqtc 'SHOW password_encryption'"`
       if [ "$PASSWORD_ENCRYPTION" = "scram-sha-256" ]; then
          echo "with SCRAM-SHA-256 password encryption"
          su - postgres -c "psql -U postgres -c \"CREATE ROLE martiuser LOGIN ENCRYPTED PASSWORD '$password' SUPERUSER INHERIT CREATEDB NOCREATEROLE;\""
       else
          echo "with MD5 password encryption"
          md5pass=$(echo -n "md5" && echo -n "$password$username" | md5sum | tr -dc "a-zA-Z0-9\n")
          su - postgres -c "psql -U postgres -c \"CREATE ROLE martiuser LOGIN ENCRYPTED PASSWORD '$md5pass' SUPERUSER INHERIT CREATEDB NOCREATEROLE;\""
       fi
    fi

    # create the database
    echo "Creating database $DB_NAME"
    su - postgres -c "createdb -U postgres --owner=martiuser $DB_NAME"

    if [ $? -ne 0 ]; then
       echo "Database $DB_NAME exists. Running SchemaManager."
       run_schema_manager
       exit 0
    fi
    echo "Database $DB_NAME created."
else
  # We can assume the we have installed all of our configuration files at this point and started the database
  # Now we need to apply any extensions updates before we do the TAKServer schema specific updates.
  # Note that pg_upgrade intentionally drops off the extension file to the root of both versions.
  echo "Checking to see if we need to update the extensions for the database..."
  if [ -x $PGDATA_NEW/../update_extensions.sql ]; then
    su - postgres -c "$PGBIN_NEW/psql --username=postgres --file=$PGDATA_NEW/../update_extensions.sql postgres"
    if [ $? -ne 0 ]; then
       echo "Unable to update extensions"
    else
       echo "Updated extensions successfully.  Removing update_extensions.sql file."
       rm -f $PGDATA_NEW/../update_extensions.sql
    fi
  fi

  echo "Disabling service for old database version..."
  /usr/bin/systemctl disable $OLD_SVC_FILENAME
  echo "Removing old service files"
  rm /etc/systemd/system/$OLD_SVC_FILENAME
  rm /usr/lib/systemd/system/$OLD_SVC_FILENAME
  echo "Reload service units..."
  /usr/bin/systemctl daemon-reload

  echo "**** NOTE:  Your old database data is still around in $PGDATA_OLD  .  "
  echo "Once you are satisfied that the new database installation is correct, you can delete the old data with:  "
  echo "$PGDATA_NEW/../delete_old_cluster.sh"
  echo ""
  echo "After running the delete_old_cluster.sh script, you should also do a 'yum erase' of the RPMs for the previous database version."
fi

run_schema_manager