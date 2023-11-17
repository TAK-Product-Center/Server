#!/usr/bin/env bash

SQL_LIB_PATH=/var/lib/pgsql/
SQL_DATA_DIR_NAME=takdata
SQL_DATA=${SQL_LIB_PATH}${SQL_DATA_DIR_NAME}
DATA_EXISTS=`sudo -u postgres ls ${SQL_LIB_PATH} | grep ${SQL_DATA_DIR_NAME}`
SQL_RUNNING=`/usr/pgsql-10/bin/pg_isready | grep "accepting connections"`
SSHD_PATH=`which /usr/sbin/sshd`

set -e

if [[ "${DATA_EXISTS}" == "" ]];then
	sudo -u postgres /usr/pgsql-15/bin/initdb $SQL_DATA
fi

if [[ "${SQL_RUNNING}" == "" ]];then
	echo STARTING PSQL
	sudo -u postgres /usr/pgsql-15/bin/pg_ctl -D $SQL_DATA start
fi

echo STARTING SSHD
sudo /usr/sbin/sshd -D
