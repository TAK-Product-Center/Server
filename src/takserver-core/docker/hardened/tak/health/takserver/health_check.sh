#!/usr/bin/env bash

LOG_FILE=/opt/tak/health/takserver/health_check.log
if [ ! -f $LOG_FILE ]; then
    touch $LOG_FILE
fi

# Add health tests here. You can have more than one.
# If a test failed, exit with 1. No need to continue.

takserver_https_response=$(curl https://localhost:8443/Marti/api/missions --cert-type P12 --cert /opt/tak/certs/files/"$ADMIN_CERT_NAME".p12:"$ADMIN_CERT_PASS" -Is -o /dev/null -w "%{http_code}" --head -k)

if [[ $takserver_https_response != 200 ]]; then
    echo -e "$(date) \tTAK Server listener HTTPS state: $takserver_https_response (waiting for 200)" >> $LOG_FILE
    exit 1
fi

# Do the file integrity check.
/opt/tak/health/takserver/check_file_integrity.sh >> $LOG_FILE
exit $?


