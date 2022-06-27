#!/bin/sh

### TO RUN AS A CRONJOB, IT MUST BE RUN AS POSTGRES USER
## EXAMPLE: clear on a 1 minute interval
# sudo crontab -u postgres -e
# * * * * * /opt/tak/db-utils/clear-old-data.sh

## Script suitable for use as a cron-job to purge old data
cd /opt/tak/db-utils
if [ -x "/opt/tak/db-utils" ]; then
 DIR="/opt/tak/db-utils"
else
 DIR="."
fi

psql -d cot < $DIR/clear-old-data.sql
