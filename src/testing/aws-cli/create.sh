#!/bin/bash

# Read in configuration.
. aws_cfg.sh

echo "Creating instances..."
instance_ids=$(aws ec2 run-instances \
  --image-id ${ami} \
  --count ${num_instances} \
  --instance-type ${instance_type} \
  --key-name ${key_name} \
  --security-group-ids ${security_group_id} \
  --associate-public-ip-address \
  --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=${pool_name}}]" \
  --output text \
  --query 'Instances[*].InstanceId')

echo "Waiting for instances to start..."
aws ec2 wait instance-running --instance-ids ${instance_ids}

echo "Getting instance IP addresses..."
ip_addrs=$(aws ec2 describe-instances \
  --instance-ids ${instance_ids} \
  --query 'Reservations[*].Instances[*].PublicIpAddress' \
  --output text)

echo ${ip_addrs} > ${pool_name}.txt

echo "Done."
