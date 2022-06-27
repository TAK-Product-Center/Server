# Manual Cluster Quickstart

#### Prerequisties
- AWS:
  - Commerical AWS Access (GovCloud not supported yet)
  - A DNS Hosted Zone registered in AWS Route 53
  *A domain name is required for configuration of the Kubernetes cluster*
- Software Installed on Workstation:
  - AWS CLI
  - Kubernetes
  - Kops
  - Docker (make sure its running)
  - Java 11

#### Set Up Required Environment Variables 
- Edit __cluster/cluster-properties__
- Source __cluster/luster-properties__
`source cluster/cluster-properties`

#### Create a CA and X.509 certificates for TAK Server
- Generate New Certs <br>
`cd $CLUSTER_HOME_DIR && docker build -t ca-setup --build-arg ARG_CA_NAME=$TAK_CA_NAME --build-arg ARG_STATE=$TAK_STATE --build-arg ARG_CITY=$TAK_CITY --build-arg ARG_ORGANIZATIONAL_UNIT=$TAK_ORGANIZATIONAL_UNIT -f docker-files/Dockerfile.ca . && docker create -it --name ca-setup-container ca-setup && docker cp ca-setup-container:/files $CLUSTER_HOME_DIR/takserver-core/certs/ && docker rm ca-setup-container` <br>
`kubectl create configmap cert-migration --from-file="path to pre-generated cluster certs" --dry-run=client -o yaml >cert-migration-replacement.yaml` <br>
`kubectl create -f cert-migration-replacement.yaml -n takserver`
- Set the value of the CERT_CONFIGMAP_FILE variable in the __cluster/cluster-properties__ file to the path of the cert-migration-replacement.yaml


- __Note__
  - To Back up Cluster Certs, Copy __files__ Directory to Another Location After the Certs have been Generated
  - To Reuse Cluster Certs, Copy a __files__ Directory You have Backed up into `$CLUSTER_HOME_DIR/takserver-core/certs`


#### Create AWS Resources Needed for Cluster
- Create KOPS S3 State Store
`aws s3 mb $KOPS_STATE_STORE`
- Create an Elastic Container Repository (ECR) in AWS, for storing the TAK Server docker container images
`aws ecr create-repository --repository-name tak/server-$TAK_CLUSTER_NAME`
- Get the ECR __repositoryUri__
`aws ecr describe-repositories --repository-name tak/server-$TAK_CLUSTER_NAME | grep repositoryUri`
- Replace __repositoryUri__ with the value of __repositoryUri__ that was returned by the previous command.
`cd $CLUSTER_HOME_DIR/ && echo "AWS_ECR_URI=<repositoryUri>" >> cluster-properties && source cluster-properties`

#### Create Kubernetes Cluster using KOPS
- Authenticate AWS and Create Cluster Configuration. (replace __node count__)
`export KOPS_STATE_STORE=$KOPS_STATE_STORE && $(aws ecr get-login --no-include-email) && kops create cluster --zones $TAK_CLUSTER_ZONES --node-size=m4.xlarge $TAK_CLUSTER_NAME.$TAK_CLUSTER_DOMAIN_NAME --node-count=<node count>`
- Provision the Cluster in AWS
`kops update cluster --name $TAK_CLUSTER_NAME.$TAK_CLUSTER_DOMAIN_NAME --yes`
- Check Cluster Status
`kops validate cluster`
  - Note: The validation may return something like "Unable to validate Kubernetes cluster using Kops". This is an ip/DNS issue with AWS. This typically happens after you delete a cluster and try to create a new one with the same name shortly after. If you have this issue, you can wait until the DNS resolves itself, or you can tear it down and create a new one with a different name.

#### Create the TAK Server Postgres Database in RDS (AWS)
- Use this command to test whether the cluster has been created. Do not proceed until the cluster is valid.
`kops validate cluster`
- Create RDS Security Groups
  - Get Subnet IDs
  `aws ec2 describe-subnets --filters "Name=tag:KubernetesCluster,Values=$TAK_CLUSTER_NAME.$TAK_CLUSTER_DOMAIN_NAME" | grep SubnetId`
  - Create RDS Subnet Group inside Cluster VPC Using the Previous Subnet Ids (replace the __subnet-ids__)
  `aws rds create-db-subnet-group --db-subnet-group-name $TAK_CLUSTER_NAME-SG --db-subnet-group-description $TAK_CLUSTER_NAME-SG --subnet-ids <subnet-id> <subnet-id>`
- Create RDS Creds
  - Generate A Master Username and Password for Your Database using an MD5 Password/User Hash. This Will be Used in the Next Step. (the "-" at the end of the output is not part of the hash)
  `echo -n <password><user> | md5sum`
- Create RDS DB Inside Cluster VPC
  - Get Security GroupId
  `aws ec2 describe-security-groups --filters "Name=tag:Name,Values=nodes.$TAK_CLUSTER_NAME.$TAK_CLUSTER_DOMAIN_NAME" | grep GroupId | tail -1`
  - Create RDS DB with the __DB User__, __Hash__ and __GroupID__ from previous commands.  __Note__: The following command only includes the required options and creates the smallest instance possible (db.t2.micro with 20GB storage - fine for testing). If launching into proudction, consider looking at the [RDS Creation Options](https://docs.aws.amazon.com/cli/latest/reference/rds/create-db-instance.html) to enable other options such as larger instance size, encryption, storage allocation, monitoring, maintenance, backups and other options that fit your clustering needs.
  `aws rds create-db-instance --db-name cot --db-instance-identifier $TAK_CLUSTER_NAME --db-instance-class db.t2.micro --engine postgres --engine-version 10.6 --no-publicly-accessible --master-username <DB User> --master-user-password md5<Hash> --allocated-storage 20 --vpc-security-group-ids <GroupID> --db-subnet-group-name $TAK_CLUSTER_NAME-SG`
- Edit __CoreConfig.xml__ for cluster deployment
  - Wait Until the __DBInstanceStatus__ has Entered the __backing up__ or __available__ State. *It may take some time*
  `aws rds describe-db-instances --db-instance-identifier $TAK_CLUSTER_NAME | grep DBInstanceStatus`
  - Get RDS DNS Name
 `aws rds describe-db-instances --db-instance-identifier $TAK_CLUSTER_NAME | grep Address`
  - Edit __Repository__ Tag in __$CLUSTER_HOME_DIR/CoreConfig.xml__ and replace __{RDS DNS}__, __user__, and __password__ to match the outputted RDS DNS and Master Username/Password Credentials that were used to generate the Hash

#### Create or Update TAK Server Cluster

- Deploy Database Setup Pod
`cd $CLUSTER_HOME_DIR && docker build -t $AWS_ECR_URI:database-setup -f docker-files/Dockerfile.rds . &&  docker push $AWS_ECR_URI && kubectl run takserver-database-setup --image=$AWS_ECR_URI:database-setup --image-pull-policy Always --restart Never --env="AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY" --env="AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID" --env="region=$TAK_CLUSTER_REGION" --env="identifier=$TAK_CLUSTER_NAME"`
- Show Database Setup Logs. `kubectl get pods` Should Show A Completed Status for __takserver-database-setup__ Once the Setup has Completed.
`kubectl logs -f takserver-database-setup`
- Get AWS_ECR_URI
`cd $CLUSTER_HOME_DIR && echo -e "\n"$AWS_ECR_URI"\n"`
- Edit ____deployments/takserver-core/takserver-core-service-and-deployment.yaml____ and replace <AWS_ECR_URI>
- Edit ____docker-files/Dockerfile.takserver-core____ and replace <AWS_ECR_URI>
- Build TAK server container images
`cd $CLUSTER_HOME_DIR && docker build -t $AWS_ECR_URI:core-base -f docker-files/Dockerfile.takserver-base . && docker build -f docker-files/Dockerfile.takserver-core -t $AWS_ECR_URI:core-provisioned . && docker push $AWS_ECR_URI`
- Deploy API, Messaging, Ignite, NATS, and NATS Streaming services. Replace {REPLICA_COUNT} below with the number of replicas you'd like for each service. <br>
`cd $CLUSTER_HOME_DIR/deployments/helm/ helm upgrade --install -n takserver takserver src/takserver-cluster/deployments/helm -f src/takserver-cluster/deployments/helm/developer-values.yaml <br>
  --set takserver.plugins.enabled={REPLICA_COUNT} <br>
  --set takserver.messaging.replicas={REPLICA_COUNT} <br>
  --set takserver.api.replicas={REPLICA_COUNT} <br>
  --set takserver.messaging.image.repository=AWS_ECR_URI <br>
  --set takserver.api.image.repository=AWS_ECR_URI <br>
  --set takserver.plugins.image.repository=AWS_ECR_URI <br>
  --set ignite.replicaCount={REPLICA_COUNT} <br>
  --set nats.cluster.replicas={REPLICA_COUNT} <br>
  --set stan.cluster.replicas={REPLICA_COUNT})) <br>`


- __Optional__ : Create DNS entry for network load balancer. Go to AWS->EC2->Load Balancers->Your Load Balancer and copy the DNS name. Then go to AWS->Route53->Your Hosted Zone->Record Sets. Create record set: name = name of cluster, type = cname, value = load balancer DNS name.
- Wait for load balancer to be ready. This is usually the slowest step in the process. Watch EC2->Load Balancers->Your Load Balancer and wait until the state goes from provisioning to active. While waiting, ensure that `$CLUSTER_HOME_DIR/takserver-core/certs/files/admin.p12` has been added to your system so that you can access the takserver webpage. Once Active, open __https://DNS:8443__ in your browser


## Useful Commands
- Delete Cluster
`kops delete cluster --name $TAK_CLUSTER_NAME.$TAK_CLUSTER_DOMAIN_NAME --yes`
- Tail Logs
`kubectl logs -f <pod_id>`
- Get  Logs
`kubectl logs <pod_name>`
- Edit Cluster Options
`kops edit ig nodes` followed by `kops update cluster`
- delete service and deployment (will fail if the deployment does not exist yet)
`kubectl delete service takserver-core && kubectl delete deployment takserver-core`
- delete ignite deployments (if exists)
`kubectl config set-context $(kubectl config current-context) --namespace=ignite && kubectl delete service ignite && kubectl delete deployment ignite-cluster`
- delete NATS deployment (if exists)
`kubectl delete natsstreamingcluster takserver-nats-streaming && kubectl delete natscluster takserver-nats`
- Get service details
`kubectl describe service takserver-core`
- Get shell in running container
`kubectl exec -it <pod-name> -- /bin/bash`
- Send test traffic (1000 test clients)
`for i in {1..1000}; do while true; do cat ~/sa_anon1; sleep 5; done | openssl s_client -host service.tyr.takdata.net -port 8089 -cert user_no_pass.pem > /dev/null & sleep 0.1; done`
- Set current kubernetes namespace to default
`kubectl config set-context $(kubectl config current-context) --namespace=default`
- get all NATS instances
`kubectl get natsclusters`
`kubectl get natsstreamingclusters`
- Scale number of pods up or down
` kubectl scale --replicas=<num_pods> deployment.apps/takserver-core`
- Destroy all pods (scale to 0) but keep everything else running (load balancer, etc)
``` kubectl scale --replicas=0 deployment.apps/takserver-core ```
- clean up docker images and containers
`docker system prune`
`kubectl get services`
`kubectl get deployments`
- Uninstall all pods but keep all cluster infrastructure running
`helm uninstall takserver -n takserver`