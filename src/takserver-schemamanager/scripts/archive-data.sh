#!/bin/sh
# Script to dump data from Tak Server's data stores to a file.
#

FILENAME=takserver-archive-`date +"%Y-%m-%d-%H.%M"`.sql
#pg_dump -h localhost -U martiuser -W -c -t "cot_*" -t "subscription*" -t resource cot > takserver-dump.sql
#zip $FILENAME takserver-dump.sql

USER=martiuser
DBNAME=cot
echo "Please provide the password for $USER."
pg_dump -h localhost -U $USER -W --clean --create $DBNAME -f $FILENAME
echo "Database $DBNAME archived to $FILENAME."
echo "Making a compressed copy for you ..."
zip --quiet $FILENAME.zip $FILENAME
echo "Compressed copy is $FILENAME.zip."
echo "Done."
