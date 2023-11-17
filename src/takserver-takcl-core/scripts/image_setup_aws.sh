#!/usr/bin/env bash

set -e

sudo yum install wget curl tmux htop vim patch git python python-pip python3 -y
sudo yum install epel-release -y
sudo yum install https://download.postgresql.org/pub/repos/yum/reporpms/EL-7-x86_64/pgdg-redhat-repo-latest.noarch.rpm -y
sudo yum update -y
sudo yum install java-11-openjdk-devel postgis33_15 postgis33_15-utils postgresql15 postgresql15-contrib  postgresql15-server  -y

sudo wget https://s3.amazonaws.com/cloudformation-examples/aws-cfn-bootstrap-latest.amzn1.noarch.rpm
sudo yum install -y aws-cfn-bootstrap-latest.amzn1.noarch.rpm
sudo ln -s /usr/local/lib/python2.7/site-packages/cfnbootstrap /usr/lib/python2.7/site-packages/cfnbootstrap
sudo yum install -y awscli
