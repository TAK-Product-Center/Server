# Automated Cluster Quickstart

## Cluster Unimplemented Features
- Latest SA
- Port changes / Input definitions. We don't recommended trying this. If you really want to, you'll have to make sure all of the kubernetes services and deployment files have been updated beforehand.
- UserManager - It is recommended to use filter groups or LDAP for group assignment
- Certificate Revocation
- Federated Group Mapping (standard inbound/outbound groups work) 
- Client Dashboard
- Metrics Dashboard
- Contacts when working with data sync / mission packages from the TAK Server Admin UI will be inconsistent  
- Injectors
- Plugins - untested

## Prerequisites
- AWS:
  - Commercial AWS Access (GovCloud not supported yet)
  - __OPTIONAL:__ A DNS Hosted Zone registered in AWS Route 53 - https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/domain-register.html. This can be used to map a domain name to the Load Balancer, but is not required
- Software Installed on Workstation:
  - python3.7
  - pip3.7
  - run `pip3 install -r cluster/scripts/requirements.txt`
  - Kubernetes - https://kubernetes.io/docs/tasks/tools/install-kubectl/ (1.21)
  - Docker - Engine 19 (make sure its running)
  - Java 11
  - aws-cli 2.2.1 - https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html
  - [helm](https://helm.sh/)
  

## Software Check
- run `kubectl version` to ensure it is reachable globally and you have the right version
- run `docker ps` to ensure docker is running

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
- The Build Script will automatically generate certs at cluster/takserver-core/certs/files unless certs are already present. You can import your own certs into cluster/takserver-core/certs/files to use existing ones. However, the current cluster implementation follows a specific naming convention for certs. We currently recommend only resuing certs that have been generated through the clustering process.

## Set Up Required Environment Variables
- Edit __cluster/cluster-properties__
- Source __cluster/cluster-properties__
`source cluster/cluster-properties`

## Build Cluster
- Note if you installed python3.7, you may need to run the script with python3
- __EKS is recommended over Kops__
`python cluster/scripts/build-eks.py` 
- __KOPS with registered Route 53 domain__
  `python cluster/scripts/build-kops.py`
- __KOPS with gossip-based DNS (Route 53 not required)__
  `python cluster/scripts/build-kops-gossip.py`
  
## Delete Cluster
- Note if you installed python3.7, you may need to run the script with python3
- __Run the appropriate EKS or KOPS delete script:__
  - `python cluster/scripts/delete-eks.py`
  - `python cluster/scripts/delete-kops.py`
  - `python cluster/scripts/delete-kops-gossip.py`

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

1. Run ./gradlew buildDocker
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