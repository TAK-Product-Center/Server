#!/usr/bin/env bash

LOG_FILE=/opt/tak/health/takserver-db/health_check.log
if [ ! -f $LOG_FILE ]; then
    touch $LOG_FILE
fi

# Add health tests here. You can have more than one.
# If a test failed, exit with 1. No need to continue.
pg_isready -U postgres
if [ "$?" != "0" ]; then
    echo "ERROR: Health check command \"pg_isready -U postgres\" failed." >> $LOG_FILE
    exit 1
fi

# Do the file integrity check.
/opt/tak/health/takserver-db/check_file_integrity.sh >> $LOG_FILE
exit $?


