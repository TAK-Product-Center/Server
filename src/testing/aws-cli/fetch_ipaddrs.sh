#!/bin/bash

. aws_cfg.sh

ip_addrs=$(aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=${pool_name}" \
  --query 'Reservations[*].Instances[*].PublicIpAddress' \
  --output text)

echo ${ip_addrs} > ${pool_name}.txt
