#!/bin/bash

. aws_cfg.sh

echo "Getting instance IDs..."
instance_ids=$(aws ec2 describe-instances \
  --filter Name=ip-address,Values=`cat ${pool_name}.txt | tr " " ,` \
  --query 'Reservations[*].Instances[*].InstanceId' \
  --output text)

echo "Destroying instances..."
aws ec2 terminate-instances --instance-ids ${instance_ids}

echo "Done."
