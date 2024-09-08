#!/bin/bash
CERT_DIR="/tak/certs/files"
if  [ -d $CERT_DIR ] && [ -n "$(ls -A $CERT_DIR)" ]
then
  echo "Existing certificates found at $CERT_DIR. New certificates will not be generated."
  exit 1
else
  echo "Generating TakServer certificates..."
  ./generateClusterCerts.sh
fi

sleep infinity