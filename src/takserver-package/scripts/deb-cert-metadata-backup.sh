#!/usr/bin/env bash

# This script is intended to back up the cert-metadata.sh and config.cfg files 
# until supported versions of the deb installers properly back them up as part 
# of their uninstallation step during the upgrade to a newer version.
# It should be executed prior to the upgrade to 5.4

set -e

if [ -f /opt/tak/certs/cert-metadata.sh ];then
	sudo cp /opt/tak/certs/cert-metadata.sh /opt/tak/certs/cert-metadata.sh.bak
fi

if [ -f /opt/tak/certs/config.cfg ];then
	sudo cp /opt/tak/certs/config.cfg /opt/tak/certs/config.cfg.bak
fi

if [ -f /opt/tak/db-utils/clear-old-data.sql ] ; then
	sudo cp /opt/tak/db-utils/clear-old-data.sql /opt/tak/db-utils/clear-old-data.sql.bak
fi
