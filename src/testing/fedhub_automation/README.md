
# Introduction

This is an ansible playbook for the purposes of automatically setting up federation hub, a number of TAK server instances, and a number of PyTAK instances on AWS. This allows for automated load testing of federation hub.

# Prerequisites

Ansible and python should be installed to run the playbook. The playbook was last tested with these versions:

```
ansible [core 2.13.13]
  python version = 3.8.10 (default, Nov 22 2023, 10:22:35) [GCC 9.4.0]
  jinja version = 3.1.3
  libyaml = True
```

# Configuration Before Running Playbook

Before running the playbook, you should populate the variables.yml with the appropriate variable values and place the appropriate files in the same directory as setup_instances.yml.

## Files Needed by Playbook

There are three files you need to place in the same directory as setup_instances.yml to run the playbook: the key file to use for access to the various AWS instances, the rpm file for the version of federation hub to be tested, and the zip file containing the docker files of the takserver to be tested.

For example, one might have the following files in the directory:
`tak-bbn-admin.pem, takserver-fed-hub-5.0-RELEASE96.noarch.rpm, takserver-docker-5.1-BETA-13.zip`

## Generating AWS Security Credentials

You should generate AWS security credentials before running the ansible script so that it has permission to provision AWS instances. To do so, you can download the AWS CLI and run the following command with a token from your MFA device:

`aws sts get-session-token --serial-number <your MFA device serial number> --token-code XXXXXX`

You can look at this page for more details: https://repost.aws/knowledge-center/authenticate-mfa-cli

## Variables to Populate in variables.yml

### aws_secret_key

This is the AWS secret key used to access the AWS account that will be used for provisioning AWS instances.

### aws_access_key

This is the AWS access key used to access the AWS account that will be used for provisioning AWS instances.

### aws_session_token

This is the AWS session token used to access the AWS account that will be used for provisioning AWS instances.

### key_name

This is the name of the key file that will be used to access the AWS instances with the .pem extension omitted. It is assumed that all AWS instances will be accessible via the same key.

### fedhub_version

This is the name of the fedhub rpm that you will be using. This file should be in the same directory as the playbook when the playbook is run.

### takserver_version

This is the takserver version that you will be using. This should be the name of the takserver docker files zip with the .zip extension omitted. The takserver docker files zip should be in the same directory as the playbook when the playbook is run.

### takserver_num_instances

This is the number of takserver instances to provision.

### pytak_num_instances

This is the number of pytak instances to provision per takserver.

### clients

This is the number of clients that each pytak instance will simulate.

### self_sa_delta

This is the position reporting frequency in seconds that each pytak client will simulate.

### fedhub_instance_type

This is the AWS instance type that will be used for the fed hub instance.

### takserver_instance_type

This is the AWS instance type that will be used for tak server instances.

### pytak_instance_type

This is the AWS instance type that will be used for pytak client instances.

### fedhub_ami_id

This is the AMI id that will be used for the fed hub instance. The AMI id should be for a machine running Rocky Linux 8.

### takserver_ami_id

This is the AMI id that will be used for tak server instances. The AMI id should be for a machine running Amazon Linux.

### pytak_ami_id

This is the AMI id that will be used for pytak client instances. The AMI id should be for a machine running CentOS 7.

### connection_index

This is how many federates each federate will be connected to during the generation of the fed hub policy. If set to 0, there will be no connections. If it is greater than or equal to the total number of tak servers minus one, every federate will be connected to every other federate. The TAK servers are numbered starting at 0 and the connections will be made in increasing order starting from the next TAK server (so if there are 3 TAK servers and a connection index of 1 is configured, 0 will be connected to 1, 1 will be connected to 2, 2 will be connected to 3, and 3 will be connected to 0).

### instance_name_prefix

This is what the instances created by the ansible script will have their names prefixed with. The script will automatically append a "_" after this prefix.

## An example variables.yml

```
aws_secret_key: VKJ!1k4jvk1lvlk1jV!lk24jvk1vjl1!V1jk
aws_access_key: @#JK$J^FK@J#FKJ@FK@
aws_session_token: IQoJb3JpZ2luX2VjEMD//////////fjafjkjk4DfkjakdkDdkfjk126j1k4KfjakskKtSoudHxuBasZTEZYDtGzzlBt0F9bU5aukUNv5FNxVoq7wEIeRAEGgwzMTc3NjA2MTE2NjYiDJ6OckxFh82Alxpm7CrMAWqZ8E6kVqYXN6aEuiSwgJUP5MQI9W2EXnemxaoLjnOEl/o8sMvXKKNqi/PqkggD+hK6/z/MiJQRm37zsVnR1O3KyuZiQpcU96p8d7wWG5MIheK7NHMWFkaviqm6kVGayowJdepyQWGus3ppcs1OmVVttoKEc8NCMyQmTY4KrhEUl3QlDjCzQMl9AAwVdBJt/X9uRWWrx8tHAsgq+FmJhXHK0Mp+yXEhXUbAkS7scue75LALasrN/zMrljtKOgHWgpHj07kN5Wh5QhZPujDmupC0BjqYAas8Mogndeg9YIYElj+t+rRmRHBBfjcgQnxhO/0rjuFrsHhrzGiM2qe6yxlvEG+CkRPqUPxHn0L9IHRZLyBIY/MXCdaTbBgxIK/3Xdohk68OOn9nLnTAYrisulNEf8uGZvzVEfvV2f2ifjtwRK3nP2dxCXI5lcP402P0q8onvR84Buk26D2xIXYMDR08dYgiduC+Yukt1gE0
key_name: tak-bbn-admin
fedhub_version: takserver-fed-hub-5.0-RELEASE96.noarch.rpm
takserver_version: takserver-docker-5.1-BETA-13
takserver_num_instances: 4 # number of takserver instances to provision
pytak_num_instances: 2 # number of pytak instances to provision per takserver instance
clients: 5 # number of clients for each pytak client to simulate
self_sa_delta: .5 # frequency in seconds for position reporting by pytak simulated clients
fedhub_instance_type: c5.4xlarge
takserver_instance_type: c5.xlarge
pytak_instance_type: t2.medium
fedhub_ami_id: ami-027bcdee875d9ac1a
takserver_ami_id: ami-00fd0e043ae988157
pytak_ami_id: ami-0f8604d85567e0aba
connection_index: 2 # this will connect every federate to two other federates
instance_name_prefix: automation_test_elu
```

# Running the Playbook

To run the playbook, simply run the following command:

ansible-playbook setup_instances.yml

# Accessing Fed Hub After Playbook is Run

To access the federation hub UI once the playbook is run, find the AWS instance with the name containing "fedhub_automation_test_fedhub_", and get its IP address.

You also need to make sure to load the credentials for the federation hub into your browser. You can find these credentials in fedhub_config_files/fedhub_files. You will need to load admin.p12 into your browser to access the federation hub instance's UI. The password for the file is "atakatak".

Once you have the credentials loaded, you can visit this url to view the federation hub UI and see traffic flowing:

`https://<IP address of fed hub aws instance>:9100`