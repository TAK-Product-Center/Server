# Overview

Amazon provides a command line interface (CLI) for managing AWS resources. Using this interface, you can write shell scripts to configure and create instances, run commands and programs on them, destroy them, etc. This is particularly useful for testing at scale, since you can quickly bootstrap many instances, run some programs, and then destroy them to minimize resource utilization (and cost).

This directory contains scripts for this purpose:

    * `create.sh`: create a pool of instances
    * `send_file.sh`: send a file to each instance in the pool
    * `run_command.sh`: run a command on each instance in the pool
    * `destroy.sh`: destroy the pool of instances

The scripts depend on two files:

    * A configuration file, `aws_cfg.sh`
    * A text file that contains all of the IP addresses of instances in the pool

If for some reason the text file containing the pool IP addresses is lost or corrupted, it can be reset using the `fetch_ipaddrs.sh` script (as long as `aws_cfg.sh` still holds the correct pool name).

# Setting Up

The scripts depend on the AWS CLI tool. To obtain the AWS CLI executable and configure it, follow the steps here:

    https://docs.aws.amazon.com/polly/latest/dg/setup-aws-cli.html

Included in those steps are instructions for configuring the AWS CLI to be connected to your AWS account using your access keys. More information for how to set this up using `aws configure` is here:

    https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-quickstart.html

# Sample Usage

The scripts can be used to orchestrate a load test by:

    1. Creating many client instances.
    2. Sending each client instance a configuration file.
    3. Running the load test program from each client.
    4. Stopping the load test.
    5. Destroying the client instances.

To create the client instances, use the `create.sh` script:

    ./create.sh

This will require configuring the pool in `aws_cfg.sh`. After the command completes, a text file containing the IP addresses of all instances in the pool will be created.

To send a file to each instance, use the `send_file.sh` script. For example, to send the same local configuration file (e.g., `config.yml`) to the `load_test` directory on each instance, run:

    ./send_file.sh ~/cdoucett-keypair.pem config.yml ~/takserver/src/testing/load_test

To run a command on each instance (such as to start a load test), use the `run_command.sh` script:

    ./run_command.sh ~/cdoucett-keypair.pem "bash -c 'cd takserver/src/testing/load_test; (python3 tak_tester.py --test-pytak-proto --config config.yml > ~/pytak.log) &'" 20

The above command runs a load test using the configuration file we previously sent to each instance, pipes the output to a log file, and waits 20 seconds between sending the command to the next instance. This delay can be useful when using a large testbed to avoid opening too many simultaneous connections at once to a server.

Similarly, to stop the load test on each instance, you could run:

    ./run_command.sh ~/cdoucett-keypair.pem "pkill python3"

To destroy all instances in the pool, use the `destroy.sh` script:

    ./destroy.sh

Note: make sure that all of the volumes associated with your instances are set to automatically delete when the instances are deleted, or delete them separately via the AWS dashboard.
