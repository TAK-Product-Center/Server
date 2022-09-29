# Automated Cluster Quickstart

## Cluster Unimplemented Features
- Latest SA
- Port changes / Input definitions. It's possible to change these in the cluster (add / remove / modify inputs), but the cluster kubernetes YAML would need to be changed also to reflect the changes.
- The file-based user management UI is available in the cluster, but has not been tested. Using LDAP or AD for group assignment is recommended.
- Certificate Revocation
- Federated Group Mapping (standard inbound/outbound groups work) 
- Client Dashboard. The client dashboard will currently only show users who are connected to the messaging pod you are load-balanced to when accessing it.
- Metrics Dashboard. The metrics dashboard will currently show stats for the messaging pod you are load-balaced to when accessing it.
- Contacts when working with data sync / mission packages from the TAK Server Admin UI will be inconsistent.
- Injectors
- Plugins - untested

## Prerequisites
- AWS:
  - An AWS commerical account. AWS GovCloud has not been recently tested.
  - __OPTIONAL:__ A DNS Hosted Zone registered in AWS Route 53 - https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/domain-register.html. This can be used to map a domain name to the Load Balancer, but is not required
  
- The following software must be installed on the workstation you are using to create the cluster:
  - python 3.7
  - pip 3.7
  - Kubectl (kubernetes client version 1.21)
  -- Download version 1.21 here: https://kubernetes.io/docs/tasks/tools/install-kubectl/ and make sure it is on your command-line path
  - Docker Engine 20
  -- Either a command-line only install or a GUI Docker Desktop will work
  - Java 11
  - AWS CLI - Install as described here: https://aws.amazon.com/cli
  - helm - Install as described here: https://helm.sh
  - eksctl - Required to create and manage AWS EKS clusters. Install as described here: https://eksctl.io
  
## Install Dependencies and Check Software
- run `pip3 install -r cluster/scripts/requirements.txt` to install python dependencies
- run `kubectl version` to ensure it is available from your user account, and that it is version 1.21.
- run `docker run hello-world` to ensure docker is running

## Configuring AWS Credentials
- Set Region : `aws configure set region <region>` [(List of Regions)](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/Concepts.RegionsAndAvailabilityZones.html)
- Set Access Key : `aws configure set aws_access_key_id <aws_access_key_id>` [(Managing access keys (console))](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_credentials_access-keys.html)
- Set Secret Key : `aws configure set aws_secret_access_key <aws_secret_access_key>`
- Verify Credentials Work : `aws sts get-caller-identity`
- Example config :  `aws configure set region us-east-2`
                    `aws configure set aws_access_key_id AYERTV7MOWD6K3HECOKKK`
                    `aws configure set aws_secret_access_key 318860611555`
- View config :   `aws configure list`

## Create a CA and X.509 certificates for TAK Server
- The installer script will automatically generate certs at cluster/takserver-core/certs/files unless certs are already present. You can import your own certs into cluster/takserver-core/certs/files to use existing ones. However, a naming convention for certs is used so we recommend following the automatic generation process first.

## Set Up Environment Variables
- Edit __cluster/cluster-properties__
-- Set and review the options here including the cluster name, number of EC2 nodes, domain name to register (optional). The number of pods per service (messaging, API etc) is defined based on the total number of EC2 nodes.
- Source __cluster/cluster-properties__
`source cluster/cluster-properties`

## Build AWS EKS TAK Server Cluster (see note about alternate KOPS method at the bottom of this README)
- Depending on your environment, your python command may be `python` rather than `python3`.
`python3 cluster/scripts/build-eks.py` 
  
## Delete EKS TAK Server Cluster __this command will delete your cluster and all data__
- Depending on your environment, your python command may be `python` rather than `python3`.
- `python cluster/scripts/delete-eks.py`

## Notes
- AWS DNS propagation can take some time. When building the cluster and testing the load balancer, you may experience connection refused or host not found errors. Give them a reasonable amount of time to resolve, 20-30 minutes, before troubleshooting / debugging

## Useful Commands
- Tail Logs
`kubectl logs -f <pod_id>`
- Get  Logs
`kubectl logs <pod_name>`
- Get service details
`kubectl describe service <service>`
- Get deployment details
`kubectl describe deployment <deployment>`
- Get shell in running container
`kubectl exec -it <pod-name> -- /bin/bash`
- Scale messaging pods
`kubectl scale --replicas=<num_pods> deployment.apps/takserver-messaging`
- Scale api mods
`kubectl scale --replicas=<num_pods> deployment.apps/takserver-api`

## Developer steps for building docker images:

1. Run ./gradlew buildDocker or for hardened takserver and tak-database docker images run ./gradlew buildHardenedDocker
2. Set your working directory to `src/takserver-cluster/build/deployments`
3. Set the environment variables DB_URL, DB_USERNAME, and DB_PASSWORD. These values are used by default.
    1. For local development use the following values:
        1. DB_URL=`jdbc:postgresql://takserver-postgresql:5432/cot`
        2. DB_USERNAME=`postgres`
        3. DB_PASSWORD=`postgres`
    2. For deployment to AWS use the following values:
        1. DB_URL=`jdbc:postgresql://{RDS DNS}:5432/cot`
        2. DB_USERNAME=`martiuser`
        3. DB_PASSWORD - Assign whatever value you would like
4. Build the cluster's docker images

```
docker build -t docker-devtest-local.artifacts.tak.gov/takserver-cluster/takserver-base:latest -f docker-files/Dockerfile.takserver-base .
docker push docker-devtest-local.artifacts.tak.gov/takserver-cluster/takserver-base:latest

docker build --pull -t docker-devtest-local.artifacts.tak.gov/takserver-cluster/takserver-messaging:latest -f docker-files/Dockerfile.takserver-messaging --build-arg TAKSERVER_IMAGE_REPO=docker-devtest-local.artifacts.tak.gov/takserver-cluster/takserver-base --build-arg TAKSERVER_IMAGE_TAG=latest .
docker push docker-devtest-local.artifacts.tak.gov/takserver-cluster/takserver-messaging:latest

docker build --pull -t docker-devtest-local.artifacts.tak.gov/takserver-cluster/takserver-api:latest -f docker-files/Dockerfile.takserver-api --build-arg TAKSERVER_IMAGE_REPO=docker-devtest-local.artifacts.tak.gov/takserver-cluster/takserver-base --build-arg TAKSERVER_IMAGE_TAG=latest .
docker push docker-devtest-local.artifacts.tak.gov/takserver-cluster/takserver-api:latest

docker build --pull -t docker-devtest-local.artifacts.tak.gov/takserver-cluster/takserver-plugins:latest -f docker-files/Dockerfile.takserver-plugins --build-arg TAKSERVER_IMAGE_REPO=docker-devtest-local.artifacts.tak.gov/takserver-cluster/takserver-base --build-arg TAKSERVER_IMAGE_TAG=latest .
docker push docker-devtest-local.artifacts.tak.gov/takserver-cluster/takserver-plugins:latest

docker build -t docker-devtest-local.artifacts.tak.gov/takserver-cluster/takserver-database-setup:latest -f docker-files/Dockerfile.database-setup .
docker push docker-devtest-local.artifacts.tak.gov/takserver-cluster/takserver-database-setup:latest
```


## Steps to manually deploy with helm to a preconfigured cluster:

1.(Optional) Create a config map containing certs and deploy it to Kubernetes
- kubectl create configmap cert-migration-replacement --from-file="path to pre-generated cluster certs" --dry-run=client -o yaml >cert-migration-replacement.yaml
- kubectl create -f cert-migration-replacement.yaml -n takserver

2.Deploy TAK and dependencies.
- kubectl create secret -n takserver docker-registry reg-cred --docker-server=docker-devtest-local.artifacts.tak.gov --docker-username=${ARTIFACTORY_USERNAME} --docker-password=${ARTIFACTORY_PASSWORD}
- Set your working directory to `src/takserver-cluster/deployments/helm`
- helm dep update
- helm upgrade --install takserver -n=takserver --create-namespace ./ -f ./production-values.yaml

3. Uninstall deployment when work is complete.
- helm uninstall takserver -n takserver


## kOps is an alternate install mechanism that uses AWS resources directly, including EC2, rather that EKS. See https://kops.sigs.k8s.io for more information about kOps.
## Build Cluster with kOps
- Note if you installed python3.7, you may need to run the script with python3
- __kOps with registered Route 53 domain__
  `python3 cluster/scripts/build-kops.py`
- __kOpsS with gossip-based DNS (Route 53 not required)__
  `python3 cluster/scripts/build-kops-gossip.py`

## Delete kOps TAK Server Cluster
- Depending on your environment, your python command may be `python` rather than `python3`. Use the delete-kops-gossip.py script if you built a gossip-based cluster.
- `python cluster/scripts/delete-kops.py`
- `python cluster/scripts/delete-kops-gossip.py`
