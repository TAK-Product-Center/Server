#!/bin/bash

#
# AWS CLI configuration.
#

# Pool name.
# Example: pool_name=cdoucett-load-test
pool_name=

# Image.
# Example: ami=ami-0885c395b58b28015
ami=

# Private key name (no need to append ".pem").
# Example: key_name=cdoucett-keypair
key_name=

# Number of instances to create in pool.
# Example: num_instances=40
num_instances=

# Instance type.
# Example: instance_type=t3.xlarge
instance_type=

# Security group ID.
# Example: security_group_id=sg-0081a70dfc2709725
security_group_id=

if [ -z "$pool_name" ]; then
  echo "Set pool_name in aws_cfg.sh"
  exit
elif [ -z "$ami" ]; then
  echo "Set ami in aws_cfg.sh"
  exit
elif [ -z "$key_name" ]; then
  echo "Set key_name in aws_cfg.sh"
  exit
elif [ -z "$num_instances" ]; then
  echo "Set num_instances in aws_cfg.sh"
  exit
elif [ -z "$instance_type" ]; then
  echo "Set instance_type in aws_cfg.sh"
  exit
elif [ -z "$security_group_id" ]; then
  echo "Set security_group_id in aws_cfg.sh"
  exit
fi
