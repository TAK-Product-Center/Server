#!/bin/bash

# Script to set up the TAKServer database.
# This is meant to be run as root.
# Since it asks the user for confirmation before obliterating his database,
# it cannot be run by the RPM installer and must be a manual post-install step.
#
# Usage: takserver-setup-db-full.sh [db-name]
#

# Note:  This script assumes we are running in a hardened docker container

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

export PGDATA=/var/lib/postgresql/data
export PGBIN=/usr/pgsql-15/bin
# Ensure PostgreSQL is initialized.

DB_NAME=cot
if [ $# -eq 1 ] ; then
    DB_NAME=$1
fi

# Create the user "martiuser" if it does not exist.
echo "Creating user \"martiuser\" ..."
su - postgres -c "$PGBIN/psql -U postgres -c \"CREATE ROLE martiuser LOGIN ENCRYPTED PASSWORD '$md5pass' SUPERUSER INHERIT CREATEDB NOCREATEROLE;\""
if [ $? -ne 0 ]; then
    echo "Something went wrong with creating user 'martiuser'.  Exiting..."
    exit 1
fi

# create the database
echo "Creating database $DB_NAME"
su - postgres -c "$PGBIN/createdb -U  postgres --owner=martiuser $DB_NAME"
if [ $? -ne 0 ]; then
    echo "Something went wrong with creating the database $DB_NAME.  Exiting..."
    exit 1
fi

echo "Database $DB_NAME created."

# Setup pgpool accounts, etc.
echo "Adding pgpool service account to system"
useradd -U -r -m -p $password pgpool
#needed for traversing /home directory which is 750 perms
usermod -a -G root pgpool

if [ ! $? -eq 0 ]; then
   echo "Unable to add pgpool user!  Exiting..."
   exit 1;
fi

echo "Setting up pgpool related roles and accounts in DB"
su - postgres -c "$PGBIN/psql -U postgres -c \"CREATE ROLE pgpool WITH LOGIN ENCRYPTED PASSWORD '$md5pass'; GRANT pg_monitor TO pgpool; CREATE ROLE repl WITH REPLICATION LOGIN ENCRYPTED PASSWORD '$md5pass'; \""

echo "Setting up pcp.conf"
echo "pgpool:$md5pass" >> /opt/tak/db-utils/full/pgpool/pcp.conf

# setting up the pool_password file
echo "pgpool:$md5pass" > /opt/tak/db-utils/full/pgpool/pgpwd.conf
echo "$username:$md5pass" >> /opt/tak/db-utils/full/pgpool/pgpwd.conf
echo "postgres:$md5pass" >> /opt/tak/db-utils/full/pgpool/pgpwd.conf
