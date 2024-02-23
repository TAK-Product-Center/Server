#!/usr/bin/env bash

SQL_LIB_PATH=/var/lib/pgsql/
SQL_DATA_DIR_NAME=takdata
SQL_DATA=${SQL_LIB_PATH}${SQL_DATA_DIR_NAME}
DATA_LISTING=`sudo -u postgres ls ${SQL_LIB_PATH} | grep ${SQL_DATA_DIR_NAME}`
SQL_RUNNING=`/usr/pgsql-10/bin/pg_isready | grep "accepting connections"`

USR_HOME=/home/jenkins

set -e

if [[ "${DATA_LISTING}" == "" ]];then
    sudo -u postgres /usr/pgsql-15/bin/initdb $SQL_DATA
fi

if [[ "${SQL_RUNNING}" == "" ]];then
    echo STARTING PSQL
    sudo -u postgres /usr/pgsql-15/bin/pg_ctl -D $SQL_DATA start
fi

cd /init_files

if [[ -f "takserver-base.rpm" ]];then
  rpm2cpio takserver-base.rpm | cpio -idm
  cp /init_files/opt/tak/CoreConfig.example.xml /init_files/opt/tak/CoreConfig.xml
  cp /init_files/opt/tak/TAKIgniteConfig.example.xml /init_files/opt/tak/TAKIgniteConfig.xml
fi

sudo -u postgres /usr/pgsql-15/bin/psql --host=127.0.0.1 --port=5432 -U postgres -c "CREATE ROLE martiuser LOGIN ENCRYPTED PASSWORD 'md564d5850dcafc6b4ddd03040ad1260bc2' SUPERUSER INHERIT CREATEDB NOCREATEROLE;"
sudo -u postgres /usr/pgsql-15/bin/createdb --host=127.0.0.1 --port=5432 -U postgres --owner=martiuser cot
java -Duser.dir=/init_files/opt/tak/ -jar /init_files/opt/tak/db-utils/SchemaManager.jar upgrade
