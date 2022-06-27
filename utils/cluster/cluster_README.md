# Cluster Quickstart

#### --- Prerequisties ---
* AWS:
* AWS Access
* Route 53 DNS with Hosted Zone
* Software Installed on Workstation:
* AWS CLI
* Kubernetes
* Kops
* Docker (make sure its running)
* Java 11

#### --- Configuring AWS Creds  ---
- Set Region : `aws configure set region <region>`
- Set Access Key : `aws configure set aws_access_key_id <aws_access_key_id>`
- Set Secret Key : `aws configure set aws_secret_access_key <aws_secret_access_key>`
- Verify Credentials Work : `aws sts get-caller-identity`

#### --- Clean Takserver  ---
- `cd takserver/src/ && ./gradlew clean`

#### --- Setup Environment Variables  ---
- Run Gradle Task to Clear and Setup Cluster Directory
`cd takserver-cluster/ && ../gradlew clearcluster buildcluster`
- Edit __build/cluster-properties__ and then Source it
`source build/cluster-properties`

#### --- Create Takserver Certs  ---
- `cd $CLUSTER_HOME_DIR/build/takserver-core/certs/`
- Generate Certs by Editing __cert-metadata.sh__, then Run
`./makeRootCa.sh && ./makeCert.sh server takserver && ./makeCert.sh client user && ./makeCert.sh client admin`
- __Note__
- To Backup Cluster Certs, Copy __files__ Directory to Another Location After the Certs have been Generated
- To Reuse Cluster Certs, Copy a __files__ Directory You have Backed up into `$CLUSTER_HOME_DIR/build/takserver-core/certs`

#### --- Create AWS Cluster Resources  ---
- Create KOPS S3 State Store
`aws s3 mb $KOPS_STATE_STORE`
- Create Elastic Container Repository (ECR) for Storing the Various Takserver Images
`aws ecr create-repository --repository-name tak/server-$TAK_CLUSTER_NAME`
- Get the ECR __repositoryUri__
`aws ecr describe-repositories --repository-name tak/server-$TAK_CLUSTER_NAME | grep repositoryUri`
- Replace  __<repositoryUri>__ with the Value of __repositoryUri__ that was Returned by the Previous Command.
`cd $CLUSTER_HOME_DIR/build/ && echo "AWS_ECR_URI=<repositoryUri>" >> cluster-properties && source cluster-properties`

#### --- Create Kubernetes Cluster using KOPS ---
- Authenticate AWS and Create Cluster Configuration
`$(aws ecr get-login --no-include-email) && kops create cluster --zones $TAK_CLUSTER_ZONES --node-size=m4.xlarge $TAK_CLUSTER_NAME.$TAK_CLUSTER_DOMAIN_NAME --node-count=<node count>`
- Provision the Cluster in AWS
`kops update cluster --name $TAK_CLUSTER_NAME.$TAK_CLUSTER_DOMAIN_NAME --yes`
- Check Cluster Status
`kops validate cluster`
- Check Node Status
`kubectl get nodes --show-labels`

#### --- Create RDS Postgres Cot Database---
- __Wait Here Until the Cluster is Valid
`kops validate cluster`
- Setup RDS Security Groups
- Get Subnet Ids
`aws ec2 describe-subnets --filters "Name=tag:KubernetesCluster,Values=$TAK_CLUSTER_NAME.$TAK_CLUSTER_DOMAIN_NAME" | grep SubnetId`
- Create RDS Subnet Group inside Cluster VPC Using the Previous Subnet Id(s)
`aws rds create-db-subnet-group --db-subnet-group-name $TAK_CLUSTER_NAME-SG --db-subnet-group-description $TAK_CLUSTER_NAME-SG --subnet-ids <subnet-id> <subnet-id>`
- RDS Creds
- Generate an MD5 User/Password Hash, This Will be Used in the Next Step
`echo -n <password><user> | md5sum`
- Create RDS DB Inside Cluster VPC
- Get Security GroupId
`aws ec2 describe-security-groups --filters "Name=tag:Name,Values=nodes.$TAK_CLUSTER_NAME.$TAK_CLUSTER_DOMAIN_NAME" | grep GroupId | tail -1`
- Create RDS DB with the __DB User__, __Hash__ and __GroupID__
`aws rds create-db-instance --db-name cot --db-instance-identifier $TAK_CLUSTER_NAME --db-instance-class db.t2.micro --engine postgres --engine-version 10.6 --no-publicly-accessible --master-username <DB User> --master-user-password md5<Hash> --allocated-storage 20 --vpc-security-group-ids <GroupID> --db-subnet-group-name $TAK_CLUSTER_NAME-SG`
- Edit __CoreConfig__
- Wait Until the __DBInstanceStatus__ has Entered the __backing up__ or __available__ State.
`aws rds describe-db-instances --db-instance-identifier $TAK_CLUSTER_NAME | grep DBInstanceStatus`
- Get RDS DNS Name
`aws rds describe-db-instances --db-instance-identifier $TAK_CLUSTER_NAME | grep Address`
- Edit __Repository__ Tag in __CoreConfig.xml__ so that __<connection url="jdbc:postgresql://<RDS DNS>:5432/cot" username="<user>" password="<password>"/>__ matches the new RDS DNS and Credentials
`cd $CLUSTER_HOME_DIR/build/`

#### --- Create or Update TAK Server Cluster ---
- Setup Namesapce and Service Account for Apache ignite (distributed cache cluster)
`cd $CLUSTER_HOME_DIR/ignite-deployment && kubectl config set-context $(kubectl config current-context) --namespace=ignite && kubectl create -f ignite-namespace.yaml && kubectl create -f ignite-service-account.yaml && kubectl create -f ignite-account-role.yaml && kubectl create -f ignite-role-binding.yaml`
- Install NATS Operator and NATS Streaming Operator (distributed message queue)
`cd $CLUSTER_HOME_DIR/nats_ignite-deployment && kubectl apply -f 00-prereqs.yaml && kubectl apply -f 10-deployment.yaml && kubectl apply -f default-rbac.yaml && kubectl apply -f deployment.yaml`
- Deploy Database Setup Pod
`cd $CLUSTER_HOME_DIR/build && docker build -t $AWS_ECR_URI:database-setup -f docker-files/Dockerfile.rds . &&  docker push $AWS_ECR_URI && kubectl run takserver-database-setup --image=$AWS_ECR_URI:database-setup --image-pull-policy Always --restart Never --env="AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY" --env="AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID" --env="region=$TAK_CLUSTER_REGION" --env="identifier=$TAK_CLUSTER_NAME"`
- Show Database Setup Logs
`kubectl logs -f takserver-database-setup`
- Deploy Ignite
`cd $CLUSTER_HOME_DIR/ignite-deployment && kubectl create -f ignite-service.yaml && kubectl create -f ignite-deployment.yaml`
- Deploy NATS
`cd $CLUSTER_HOME_DIR/nats_ignite-deployment && kubectl apply -f takserver-nats.yaml`
- Wait for NATS Pods to Start (`kubectl get pods`), then run
`kubectl apply -f takserver-nats-streaming.yaml`
- Check if NATS and Ignite are Up
`kubectl get pods`
- Build tak server, build takserver-core docker container image, build provisioned core takserver image (including certs and truststore), deploy takserver-core
- Edit ____takserver-core-service-and-deployment.yaml____ and replace <AWS_ECR_URI>
`cd $CLUSTER_HOME_DIR/build && echo $AWS_ECR_URI`
- Edit ____Dockerfile.takserver-core____ and replace <AWS_ECR_URI>
`cd $CLUSTER_HOME_DIR/build/docker-files && echo $AWS_ECR_URI`
- Build and Deploy Takserver Images
`cd $CLUSTER_HOME_DIR/build && docker build -t $AWS_ECR_URI:core-base -f docker-files/Dockerfile.takserver-base . && docker build -f docker-files/Dockerfile.takserver-core -t $AWS_ECR_URI:core-provisioned . && docker push $AWS_ECR_URI && kubectl create -f takserver-core-service-and-deployment.yaml`
- __Optional__ : Create DNS entry for network load balancer. Go to AWS->EC2->Load Balancers->Your Load Balancer and copy the DNS name. Then go to AWS->Route53->Your Hosted Zone->Record Sets. Create record set: name = name of cluster, type = cname, value = load balancer DNS name.
- Wait for load balancer to be ready. This is usually the slowest step in the process. Watch EC2->Load Balancers->Your Load Balancer and wait until the state goes from provisioning to active. While waiting, ensure that the certs in `$TAK_HOME_DIR/utils/cluster/target` have been added to your system so that you can access the takserver webpage. Once Active, open __https://<DNS>:8443__ in your browser

## --- Useful Commands ---
- Tail Logs
`kubectl logs -f <pod_id>`
- Get  Logs
`kubectl logs <pod_name>`
- Edit Cluster Options
`kops edit` followed by `kops update cluster`
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
- delete cluster
`kops delete cluster --name $TAK_CLUSTER_NAME.$TAK_CLUSTER_DOMAIN_NAME --yes`
- Scale number of pods up or down
` kubectl scale --replicas=<num_pods> deployment.apps/takserver-core`
- Destroy all pods (scale to 0) but keep everything else running (load balancer, etc)
``` kubectl scale --replicas=0 deployment.apps/takserver-core ```
- clean up docker images and containers
`docker system prune`
`kubectl get services`
`kubectl get deployments`

