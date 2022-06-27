#!/bin/sh
# Script to clean out the data from Marti's data stores and (re) build the tables.
# Note that postgis_create_cot_table.sql clears data automatically when it builds 
# the cot database.
#

if [ $1 ]; then
    # Connect to DB 'postgres' because the archive script will be dropping and restoring the cot DB.
    psql -h localhost -U martiuser postgres < $1
    if [ $? -ne 0 ] ; then
	exit 1
    fi
    echo ""
    echo "Database restored from file $1"
else
	echo "Must supply .sql file that you want to restore as first argument"
	exit 1
fi


