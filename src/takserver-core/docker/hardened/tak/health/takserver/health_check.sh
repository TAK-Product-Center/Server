#!/usr/bin/env bash

LOG_FILE=/opt/tak/health/takserver/health_check.log
if [ ! -f $LOG_FILE ]; then
    touch $LOG_FILE
fi

# Add health tests here. You can have more than one.
# If a test failed, exit with 1. No need to continue.

# Do the file integrity check.
/opt/tak/health/takserver/check_file_integrity.sh >> $LOG_FILE
exit $?


