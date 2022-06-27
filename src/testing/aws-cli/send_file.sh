#!/bin/bash

. aws_cfg.sh

if [ "$#" -ne 3 ]; then
  echo "Usage: ./send_file.sh KEY_PATH LOCAL_FILE REMOTE_FILE_DESTINATION"
  exit
fi

ip_addrs=`cat ${pool_name}.txt`

for i in ${ip_addrs}; do
  echo "Sending file to instance ${i}..."
  scp -i "$1" -o LogLevel=error -o StrictHostKeyChecking=no \
    "$2" centos@ec2-$(echo "${i}" | tr . -).compute-1.amazonaws.com:"$3"
done

echo "Done."
