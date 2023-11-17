# Testing Utility Scripts

This is a dumping ground for useful scripts I have written that are generally 'as-is' and are only maintained based on 
need. They have been developed on Linux and may or may not work with other base systems. Scripts known to not have been 
run in quite a while are annotated with an asterisk in the section title.

## General Setup

### Certificates
It is useful to have a directory of certificates to use for deployments that are re-used and already loaded into your 
browser for easy use. the environment variable `TAKSERVER_CERT_SOURCE` should be set to this directory and it should 
mirror the contents of a tak/certs/files directory after following the steps to create certificates. Don't forget to 
're-roll' them when they expire!
g
### Environment Variables

| Name                             | Description                                                   | Example                   |
|----------------------------------|---------------------------------------------------------------|---------------------------|
| TAKSERVER_CERT_SOURCE            | The contents of _/tak/certs/files/_ for reuse                 | '/my/cert/dir'            |
| TAKCL_SERVER_POSTGRES_PASSWORD   | The Postgres password to be used by Takcl tests               | 'badPassword'             |
| TAKCL_SERVER_LOG_LEVEL_OVERRIDES | Adds additional logging settings for TAKCL server deployments | 'com.bbn=TRACE tak=TRACE' |


## Scripts

### testrunner.sh

Used to execute integration tests locally
#### Environment Variables Used
 - TAKCL_SERVER_POSTGRES_PASSWORD
 - TAKCL_SERVER_LOG_LEVEL_OVERRIDES

#### Usage

1. Build the project with `./gradlew --parallel clean buildRpm buildDocker`
2. From the takserver src directory, view a list of test identifiers by executing 
   `./takserver-takcl-core/scripts/testrunner.sh list`
3. From the takserver src directory, execute a test with the 'run testIdentifier' parameters:
   `./takserver-takcl-core/scripts/testrunner.sh run GeneralTests.tcpTest`
4. When finished, the test results will be placed within the takserver src directory in the TESTRUNNER_RESULTS folder.

### start-local-cluster.sh

Used to start a local cluster with minikube. Minikube, Helm, and Kubectl will be downloaded automatically.

#### Environment Variables Used
- TAKCL_SERVER_POSTGRES_PASSWORD
- TAKSERVER_CERT_SOURCE

#### Preparation
1.  Start a local auto-start docker registry for local usage with the following command:  
    `docker run -d -p 5000:5000 --restart always --name registry registry:2`
2.  [Add](https://docs.docker.com/registry/insecure/) the registry using your external IP address to your docker configuration
3.  Ensure that registry is blocked from actual external access by your firewall.
4.  Restart the docker daemon by running `systemctl restart docker`
5.  Create a staging directory for local deployments and copy `start-local-cluster.sh` and `minikube-cluster-template.yaml` into it.
6.  Add values for the two environment variables to your _.bashrc_ for use for deployments.
7.  Modify `start-local-cluster.sh` so that the EXTERNAL_IP, MK_DRIVER, MK_CPU_COUNT, and MK_MEMORY are appropriate.

#### Usage
1.  Copy the cluster zip into the same directory as the `start-local-cluster.sh` script.  
2.  Start it by providing the zip as a parameter to the command. For example:  
    `./start-local-cluster.sh takserver-cluster-4.10-DEV-119.zip`

### bisect-helper.py *

Used to take the last known good commit hash and a first known bad commit hash and run specific tests against them to 
determine which commit broke the test.

### aws_runner.py and cloudformation.json

Used to run tests on AWS. WIP

### check_for_failures.py *

Used to look for discrepancies in terms of test executions

### collect_results.sh
!CI Gathers reults for CI consumption

### compare_results.py *
Compares workflows of two tests executions. Mainly used for significant upgrades to the testing infrastructure that 
could potentially result in broken useless tests

### log_helper.py, gen/logging.py *

Used to analyze test logs to determine bottlenecks

### image_setup_aws.sh *

Script to set up a CentOS 7 system for aws deployment which is useful if you don't want your kubectl on your host 
system tied up by a deployment to AWS

### start.sh *

Starts up a local server instance
