#!/bin/sh
# Script to clean out the data from Marti's data stores and (re) build the tables.
# Note that postgis_create_cot_table.sql clears data automatically when it builds 
# the cot database.
#

#export MARTI_ROOT="/home/martiuser/marti"

SQL_FILE=postgis_create_cot_table.sql

if [ $# -lt 1 ]; then
	SQL_PATH=`pwd`
else
	SQL_PATH=$1/
fi

FULLPATH="${SQL_PATH}/${SQL_FILE}"


if [ -e ${FULLPATH} ]; then
	psql -h localhost -U martiuser cot < ${FULLPATH}
else
	echo "Cannot find ${FULLPATH}."
	echo "Please run $0 from working directory where $SQL_FILE is located."
	echo "Alternatively, you can supply the directory containing $SQL_FILE as a command-line argument."
	exit 1
fi


