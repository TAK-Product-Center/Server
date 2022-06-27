#!/bin/bash

. aws_cfg.sh

if [ "$#" -lt 2 ] || [ "$#" -gt 3 ]; then
  echo "Usage: ./run_command.sh KEY_PATH COMMAND [SLEEP_BETWEEN_COMMANDS]"
  exit
fi

ip_addrs=`cat ${pool_name}.txt`

for i in ${ip_addrs}; do
  echo "Running command on instance ${i}..."
  ssh -i "$1" -o LogLevel=error -o StrictHostKeyChecking=no \
    centos@ec2-$(echo "${i}" | tr . -).compute-1.amazonaws.com \
    "$2"

  if [ "$#" -eq 3 ]; then
    echo "Waiting to run next command..."
    sleep "$3"
  fi
done

echo "Done."
