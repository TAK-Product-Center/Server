import boto3
import time
import subprocess
import sys
from sys import platform
import os
import botocore
import json
import ast
import fileinput
import hashlib
import xml.etree.ElementTree

# AWS CREDS
AWS_ACCESS_KEY_ID = os.environ['AWS_ACCESS_KEY_ID']
AWS_SECRET_ACCESS_KEY = os.environ['AWS_SECRET_ACCESS_KEY']

# Cluster Config
CLUSTER_HOME_DIR = os.environ['CLUSTER_HOME_DIR']
TAK_CLUSTER_NAME = os.environ['TAK_CLUSTER_NAME']
TAK_CLUSTER_ZONES = os.environ['TAK_CLUSTER_ZONES']
TAK_CLUSTER_DOMAIN_NAME = os.environ['TAK_CLUSTER_DOMAIN_NAME']
AWS_S3_KOPS_STORE_NAME = os.environ['AWS_S3_KOPS_STORE_NAME']
TAK_CLUSTER_REGION = os.environ['TAK_CLUSTER_REGION']
KOPS_STATE_STORE = os.environ['KOPS_STATE_STORE']
TAK_CLUSTER_NODE_COUNT = os.environ['TAK_CLUSTER_NODE_COUNT']
AWS_ECR_URI = ''
TAK_PLUGINS = os.environ['TAK_PLUGINS']


# RDS DB Config
TAK_DB_USERNAME = os.environ['TAK_DB_USERNAME']
TAK_DB_PASSWORD = os.environ['TAK_DB_PASSWORD']
TAK_DB_SIZE = os.environ['TAK_DB_SIZE']
TAK_DB_ALLOCATED_STORAGE = os.environ['TAK_DB_ALLOCATED_STORAGE']

# TAK Certificate Config
TAK_CA_NAME = os.environ['TAK_CA_NAME']
TAK_CITY = os.environ['TAK_CITY']
TAK_STATE = os.environ['TAK_STATE']
TAK_ORGANIZATIONAL_UNIT = os.environ['TAK_ORGANIZATIONAL_UNIT']

CERT_CONFIGMAP_FILE = ''

# Run A Command Line Argument, Logging Output As We Go
def runCmd(cmd):
	p = subprocess.Popen(cmd, shell=True, stderr=subprocess.PIPE)
	while True:
	    out = p.stderr.read(1)
	    
	    if out == b'' and p.poll() != None:
	        break
	    
	    if out != b'':
	    	sys.stdout.write(out.decode('utf-8'))
	    	sys.stdout.flush()
	
	return p.poll()

# Print Out Formatted Json
def printJson(j) :
	print (json.dumps(j,sort_keys=True, indent=4, default=str))

# Setup AWS S3. If Exists, Skip. Otherwise, Create.
def setupS3():	
	try:
	    find_s3_res = boto3.client('s3', region_name=TAK_CLUSTER_REGION).head_bucket(Bucket='tak.server-' + TAK_CLUSTER_NAME)
	    printJson(find_s3_res)
	    print('\nS3 Store Already Exists At ' + 's3://tak.server-' + TAK_CLUSTER_NAME + ' , Skipping Creation..')
	
	except botocore.exceptions.ClientError as e:
	    error_code = e.response['Error']['Code']
	    if error_code == '404':
	    	try:
	    		if TAK_CLUSTER_REGION == 'us-east-1':
	    			create_s3_res = boto3.client('s3').create_bucket(Bucket='tak.server-' + TAK_CLUSTER_NAME)
	    		else:
	    			create_s3_res = boto3.client('s3').create_bucket(Bucket='tak.server-' + TAK_CLUSTER_NAME,CreateBucketConfiguration={'LocationConstraint': TAK_CLUSTER_REGION})

	    		printJson(create_s3_res)
	    		print('\nCreated S3 Store')
	    	
	    	except botocore.exceptions.ClientError as e:
	    		printJson(e.response['Error'])
	    		print("\nPlease ensure you have a valid region and your cluster name follows the naming convention (no capitals or underscores)")
	    		sys.exit(1)
	    
	    else :
	    	printJson(e.response['Error'])
	    	sys.exit(1)

# Setup AWS ECR. If Exists, Skip. Otherwise, Create. 
# Once ECR Is Available, Write URI To The Cluster Properties File (Will Override Any Existing AWS_ECR_URI)
def setupECR():
	global AWS_ECR_URI
	try:
	    find_ecr_res = boto3.client('ecr', region_name=TAK_CLUSTER_REGION).describe_repositories(repositoryNames=['tak/server-' + TAK_CLUSTER_NAME])
	    printJson(find_ecr_res)
	    AWS_ECR_URI = find_ecr_res['repositories'][0]['repositoryUri']
	    print('\nECR Already Exists At ' + AWS_ECR_URI + ' , Skipping Creation..')
	
	except botocore.exceptions.ClientError as e:
		create_ecr_res = boto3.client('ecr').create_repository(repositoryName='tak/server-' + TAK_CLUSTER_NAME)
		AWS_ECR_URI = create_ecr_res['repository']['repositoryUri']
		print('ECR created at ' + AWS_ECR_URI)

	while True:
		try:
		    boto3.client('ecr', region_name=TAK_CLUSTER_REGION).describe_repositories(repositoryNames=['tak/server-' + TAK_CLUSTER_NAME])
		    print('ECR is Available')
		    break
		
		except botocore.exceptions.ClientError as e:
			print('Waiting for ECR to become available. Checking again in 15 seconds')
			time.sleep(15)

	try:
		ecr_uri_exists = False
		
		properties_file = fileinput.input(files=CLUSTER_HOME_DIR +'/cluster-properties', inplace=1)
		for line in properties_file:
			if 'export AWS_ECR_URI=' in line:
				line = 'export AWS_ECR_URI=' + AWS_ECR_URI + "\n"
				ecr_uri_exists = True
			print (line, end ="")
		properties_file.close()

		if not ecr_uri_exists:
			with open(CLUSTER_HOME_DIR +'/cluster-properties', "a") as properties_file:
				properties_file.write('export AWS_ECR_URI=' + AWS_ECR_URI + "\n")
	
	except:
		print('\nEXITING: Could not open ' + CLUSTER_HOME_DIR +'/cluster-properties')
		sys.exit(1)

# Setup The Cluster Using Kops. Skip Setup If Cluster Exists And Is Valid.
def setupCluster():
	if runCmd('kops get cluster') == 0 and runCmd('kops validate cluster --name ' + TAK_CLUSTER_NAME + '.' + TAK_CLUSTER_DOMAIN_NAME) == 0:
		print('Cluster Exists, Skipping Setup..\n')
	
	else:
		setup_cluster_cmd = 'export KOPS_STATE_STORE=' + KOPS_STATE_STORE + ' && aws ecr get-login-password && kops create cluster --zones ' + TAK_CLUSTER_ZONES + ' --node-size=c5.4xlarge ' + TAK_CLUSTER_NAME + '.' + TAK_CLUSTER_DOMAIN_NAME + ' --node-count=' + TAK_CLUSTER_NODE_COUNT
		setup_cluster_cmd_status = runCmd(setup_cluster_cmd)
		
		update_cluster_cmd = 'kops update cluster --name ' + TAK_CLUSTER_NAME + '.' + TAK_CLUSTER_DOMAIN_NAME + ' --yes --admin'
		update_cluster_cmd_status = runCmd(update_cluster_cmd)
		

# Validate Kops API Is Ready
def validateKops():
	kops_valid = False
	while not kops_valid :
		
		if runCmd('kops validate cluster --name ' + TAK_CLUSTER_NAME + '.' + TAK_CLUSTER_DOMAIN_NAME) == 0:
			print ('Cluster Is Now Valid!')
			kops_valid = True
		
		else:
			print ("Cluster Not Ready. Trying Again In 60 Seconds\n")
			time.sleep(60)

# Validate KubeCtl API Is Ready
def validateKubeCtl():
	kubectl_valid = False
	while not kubectl_valid :
		
		if runCmd('kubectl cluster-info') == 0:
			print ('Kubectl Is Now Valid!')
			kubectl_valid = True
		
		else:
			print ("\nKubeCtl Not Ready. Trying Again In 15 Seconds\n")
			time.sleep(15)
	
# Generate And Return An MD5 Hash
def generateDBHash():
	return hashlib.md5((TAK_DB_PASSWORD + TAK_DB_USERNAME).encode('utf-8')).hexdigest()

# Setup RDS DB Parameter group with 50000 max connections
def setupDBParameterGroups():
	try:
		find_db_param_group_res = boto3.client('rds', region_name=TAK_CLUSTER_REGION).describe_db_parameter_groups(DBParameterGroupName='takserver-rds-pg')
		printJson(find_db_param_group_res)
		print('\nParameter Group Exists')
	except botocore.exceptions.ClientError as e:
		create_db_param_group_res = boto3.client('rds', region_name=TAK_CLUSTER_REGION).create_db_parameter_group(
		    DBParameterGroupName='takserver-rds-pg',
		    DBParameterGroupFamily='postgres10',
		    Description='Takserver RDS parameter group for postgres10'
		)

		print('\nCreating RDS Parameter Group')
		printJson(create_db_param_group_res)

		modify_db_param_group_res = boto3.client('rds', region_name=TAK_CLUSTER_REGION).modify_db_parameter_group(
		    DBParameterGroupName='takserver-rds-pg',
		    Parameters=[
		        {
		            'ParameterName': 'max_connections',
		            'ParameterValue': '50000',
		            'ApplyMethod': 'pending-reboot'
		        },
		    ]
		)

		print('\nModifying RDS Parameter Group')
		printJson(modify_db_param_group_res)

# Setup A Subnet Group For RDS DB. Skip Creation If Exists
def setupSubnetGroups():
	try:
		find_db_subnet_group_res = boto3.client('rds', region_name=TAK_CLUSTER_REGION).describe_db_subnet_groups(DBSubnetGroupName=TAK_CLUSTER_NAME + '-SG')
		printJson (find_db_subnet_group_res)
		print ('\nSubnet Security Group Already Exists')
	
	except botocore.exceptions.ClientError as e:
		
		try:
			find_db_subnets_res = boto3.client('ec2', region_name=TAK_CLUSTER_REGION).describe_subnets(Filters=[{'Name': 'tag:KubernetesCluster','Values': [TAK_CLUSTER_NAME + '.' + TAK_CLUSTER_DOMAIN_NAME]}])
			
			subnets = []
			for subnet in find_db_subnets_res['Subnets']:
				subnets.append(subnet['SubnetId'])
			
			create_db_subnet_group_res = boto3.client('rds', region_name=TAK_CLUSTER_REGION).create_db_subnet_group(DBSubnetGroupName=TAK_CLUSTER_NAME + '-SG',DBSubnetGroupDescription=TAK_CLUSTER_NAME + '-SG',SubnetIds=subnets)
			
			printJson(create_db_subnet_group_res)
			print ('\nSubnet Security Group Created')
		
		except botocore.exceptions.ClientError as e:
			printJson(e.response['Error'])
			sys.exit(1)

# Find And Return Cluster Security Group
def describeSecurityGroups() :
	try:
		find_security_groups_res = boto3.client('ec2', region_name=TAK_CLUSTER_REGION).describe_security_groups(Filters=[{'Name': 'tag:Name','Values': ['nodes.' + TAK_CLUSTER_NAME + '.' + TAK_CLUSTER_DOMAIN_NAME]}])
		return find_security_groups_res['SecurityGroups'][0]['GroupId']
	
	except botocore.exceptions.ClientError as e:
		print ('\nEXITING: Could Not Find Cluster Security Group')
		sys.exit(1)

# Create RDS DB Is It Does Not Exist. Modify Core Config With RDS Credentials
def setupRDS():
	setupDBParameterGroups()
	setupSubnetGroups()

	rds_dns = ''
	while not rds_dns :
		
		try:
			find_db_instances_res = boto3.client('rds', region_name=TAK_CLUSTER_REGION).describe_db_instances(DBInstanceIdentifier=TAK_CLUSTER_NAME)
			rds_dns = find_db_instances_res['DBInstances'][0]['Endpoint']['Address']
			printJson(find_db_instances_res)
			print('\nDatabase Exists At ' + rds_dns)
		
		except botocore.exceptions.ClientError as e:
			
			try:
				create_db_instance_res = (boto3.client('rds', region_name=TAK_CLUSTER_REGION)
				.create_db_instance(
				    DBName='cot',
				    DBInstanceIdentifier=TAK_CLUSTER_NAME,
				    AllocatedStorage=int(TAK_DB_ALLOCATED_STORAGE),
				    DBInstanceClass=TAK_DB_SIZE,
				    Engine='postgres',
				    MasterUsername=TAK_DB_USERNAME,
				    MasterUserPassword='md5' + generateDBHash(),
				    VpcSecurityGroupIds=[
				        describeSecurityGroups()
				    ],
				    DBSubnetGroupName=TAK_CLUSTER_NAME + '-SG',
				    EngineVersion='10.20',
				    DBParameterGroupName='takserver-rds-pg',
				    PubliclyAccessible=False
				))
				printJson(create_db_instance_res)
				print('\nDatabase Is Now Creating... ')
			
			except botocore.exceptions.ClientError as e:
				printJson(e.response['Error'])
				sys.exit(1)
		
		except KeyError as e:
			print ('\nDatabase Does Not Have An Endpoint Yet.. Trying again in 60 Seconds')
			time.sleep(60)

	try:
		core_config = xml.etree.ElementTree.parse(CLUSTER_HOME_DIR +'/CoreConfig.xml')
		xml.etree.ElementTree.register_namespace('', 'http://bbn.com/marti/xml/config')
		xml.etree.ElementTree.register_namespace('xsi', 'http://www.w3.org/2001/XMLSchema-instance')

		for repository in core_config.getroot().findall('{http://bbn.com/marti/xml/config}repository'):
			for connection in repository.findall('{http://bbn.com/marti/xml/config}connection'):
				connection.set('url', 'jdbc:postgresql://' + rds_dns + ':5432/cot')
				connection.set('username', TAK_DB_USERNAME)
				connection.set('password', TAK_DB_PASSWORD)

		core_config.write(CLUSTER_HOME_DIR +'/CoreConfig.xml')
		print('\nCoreConfig successfully modified to include RDS credentials')
	
	except:
		print ('\nError writing to CoreConfig.xml...')
		sys.exit(1)

# Check The Status Of The RDS Setup Pod
def checkDBSetupDeploymentStatus():
	deployment_succeeded_code = runCmd("kubectl get pod --field-selector=status.phase=Succeeded | grep takserver-database-setup")
	deployment_failed_code = runCmd("kubectl get pod --field-selector=status.phase=Failed | grep takserver-database-setup")
	
	if deployment_succeeded_code == 0:
		print ('\nDatabase Setup Pod Already Exists And Succeeded.. Skipping Setup (delete this pod if you want to run/rerun RDS schema setup)\n')
		return 0
	
	elif deployment_failed_code == 0:
		print ('\nDatabase Setup Pod Already Exists But Failed.. Deleting and Deploying..\n')
		return 1
	
	else:
		print ('\nNo Databse Setup Exists.. Creating\n')
		return -1

def checkDocker():
	check_docker_cmd = 'docker ps'
	check_docker_cmd_status = runCmd(check_docker_cmd)

	if check_docker_cmd_status == 0:
		print("\nDocker is running.. continuing")
		return
	else :
		if platform == "linux" or platform == "linux2":
			check_docker_cmd = 'systemctl restart docker'
			runCmd(check_docker_cmd)
			
			check_docker_cmd = 'docker-machine restart && eval "$(docker-machine env default)"'
			runCmd(check_docker_cmd)
		elif platform == "darwin":
			check_docker_cmd = 'docker-machine restart && eval "$(docker-machine env default)"'
			runCmd(check_docker_cmd)
		elif platform == "win32" or platform == "cygwin":
			print('Windows Host')

	time.sleep(5)

	check_docker_cmd_status = runCmd(check_docker_cmd)

	if check_docker_cmd_status == 0:
		print("\nDocker is running.. continuing")
	else:
		print("\n** Could not programatically start docker. Please do it manually and rerun the script. Run 'docker ps' to verify status")
		sys.exit(1)

# Run The Databse Setup Pod And Wait For It To Finish
def deployDatabaseSetupPod():
	db_pod_status_code = checkDBSetupDeploymentStatus()
	if db_pod_status_code == 0:
		return
	
	elif db_pod_status_code == 1:
		runCmd("kubectl delete pod takserver-database-setup")
	
	init_DB_cmd = 'cd ' + CLUSTER_HOME_DIR + ' && docker build -t ' + AWS_ECR_URI + ':database-setup -f docker-files/Dockerfile.rds . && docker push ' + AWS_ECR_URI + ':database-setup && kubectl run takserver-database-setup --image=' + AWS_ECR_URI + ':database-setup --image-pull-policy Always --restart Never --env="AWS_SECRET_ACCESS_KEY=' + AWS_SECRET_ACCESS_KEY + '" --env="AWS_ACCESS_KEY_ID=' + AWS_ACCESS_KEY_ID + '" --env="region=' + TAK_CLUSTER_REGION + '" --env="identifier=' + TAK_CLUSTER_NAME + '"'
	if runCmd(init_DB_cmd) == 0:
		print ('\nDeployed Database Setup Pod\n')
	
	else:
		print('\nUnable to deploy database pod...')
		sys.exit(1)

	while True:
		db_deployment_failed_code = runCmd("kubectl get pod --field-selector=status.phase=Failed | grep takserver-database-setup")
		db_deployment_succeeded_code = runCmd("kubectl get pod --field-selector=status.phase=Succeeded | grep takserver-database-setup")
		if db_deployment_succeeded_code == 0:
			print ('\nSchemaManager has successfully setup Cot Database\n')
			break
		
		if db_deployment_failed_code == 0:
			print ('\nSomething went wrong. Schema manager failed to setup db.\n')
			sys.exit(1)
		
		print ('Schema Manager Still Setting Up DB.. checking again in 30 seconds\n')
		time.sleep(30)

# Deploy Ingress
def deployIngress():
	deploy_ingress_cmd = 'cd ' + CLUSTER_HOME_DIR + '/deployments/ingress-infrastructure && kubectl config set-context $(kubectl config current-context) --namespace=takserver && kubectl apply -f ingress-setup.yaml'
	if runCmd(deploy_ingress_cmd) == 0:
		print('Ingress Setup Deployed Successfully\n')
	
	else:
		print('Ingress Setup Already Deployed.. Skipping\n')

# Deploy Load Balancer
def deployLoadBalancer():
	cmd = 'cd ' + CLUSTER_HOME_DIR + ' && kubectl create -f ' + CLUSTER_HOME_DIR + '/deployments/ingress-infrastructure/load-balancer-deployment.yaml'
	cmd_status = runCmd(cmd)

	if cmd_status == 0:
		print("\nLoad balancer deployed")
	else:
		print("\nLoad balancer was not deployed")

# Generate Cluster Certificaties If They Dont Exist
def generateTakseverCertificates():
	if not os.path.isdir(CLUSTER_HOME_DIR + '/takserver-core/certs/files') or not os.listdir(CLUSTER_HOME_DIR + '/takserver-core/certs/files'):
		if not TAK_CA_NAME or not TAK_CITY or not TAK_STATE or not TAK_ORGANIZATIONAL_UNIT:
			printJson('No certs found and cert metadata not defined in cluster-properties. please update cluster-properties, resource the file, and try rebuilding')
			sys.exit(1)

		runCmd('cd ' + CLUSTER_HOME_DIR + ' && docker build -t ca-setup --build-arg ARG_CA_NAME=' + TAK_CA_NAME + ' --build-arg ARG_STATE=' + TAK_STATE + ' --build-arg ARG_CITY=' + TAK_CITY + ' --build-arg ARG_ORGANIZATIONAL_UNIT=' + TAK_ORGANIZATIONAL_UNIT + ' -f docker-files/Dockerfile.ca . && docker create -it --name ca-setup-container ca-setup && docker cp ca-setup-container:/files ' + CLUSTER_HOME_DIR + '/takserver-core/certs/ && docker rm ca-setup-container')
		print('\nNew certificates with default password: "atakatak" generated at: \n\n' + CLUSTER_HOME_DIR + '/takserver-core/certs/files.')  
	
	else:    
		print('\nEXISTING CERTIFICATES FOUND AT \n\n' + CLUSTER_HOME_DIR + '/takserver-core/certs/files\n\nNOT GENERATING NEW ONES...')  

	config_map_cmd = 'kubectl create configmap cert-migration --from-file="' + CLUSTER_HOME_DIR + '/takserver-core/certs/files' + '" --dry-run=client -o yaml >' + CLUSTER_HOME_DIR +  '/deployments/helm/templates/cert-migration.yaml'
	runCmd(config_map_cmd)


def addCoreConfigMap():
	fp = os.path.join(CLUSTER_HOME_DIR, 'CoreConfig.xml')
	if not os.path.isfile(fp):
		printJson('No CoreConfig.source.xml found. It should be located in the root of the cluster directory!')
		sys.exit(1)

	print('Loading CoreConfig.source.xml found in cluster root into the cluster ConfigMap.')
	config_map_cmd = 'kubectl create configmap core-config --from-file="' + fp + '" --dry-run=client -o yaml >' + CLUSTER_HOME_DIR +  '/deployments/helm/templates/core-config.yaml'
	runCmd(config_map_cmd)


def publishTakserverCoreImages():
	base_cmd = ('cd ' + CLUSTER_HOME_DIR + ' && docker build -t ' + AWS_ECR_URI + ':core-base -f docker-files/Dockerfile.takserver-base . && docker push ' + AWS_ECR_URI + ':core-base')
	cmd_status = runCmd(base_cmd)

	if cmd_status == 0:
		build_dependent_dockerfiles_cmd = ('cd ' + CLUSTER_HOME_DIR + ' && docker build -t ' + AWS_ECR_URI + ':messaging-provisioned -f docker-files/Dockerfile.takserver-messaging --build-arg TAKSERVER_IMAGE_REPO=' + AWS_ECR_URI + ' .'
						 + ' && docker build -t ' + AWS_ECR_URI + ':api-provisioned -f docker-files/Dockerfile.takserver-api --build-arg TAKSERVER_IMAGE_REPO=' + AWS_ECR_URI + ' .'
						 + ' && docker push ' + AWS_ECR_URI + ' --all-tags')

		build_dependent_dockerfiles_cmd_status = runCmd(build_dependent_dockerfiles_cmd)

		if build_dependent_dockerfiles_cmd_status == 0:
			print("\nTakserver Base, API, & Messaging images were published")
		else:
			print("\nTakserver Base, API, & Messaging images were NOT published")
	else:
		print("\nTakserver Base was image was not published")


# Deploy Takserver Plugins
def publishTakserverPluginImages():
	cmd = 'cd ' + CLUSTER_HOME_DIR + ' && docker build -t ' + AWS_ECR_URI + ':plugins-provisioned -f docker-files/Dockerfile.takserver-plugins --build-arg TAKSERVER_IMAGE_REPO=' + AWS_ECR_URI + ' . && docker push ' + AWS_ECR_URI + ' --all-tags'
	cmd_status = runCmd(cmd)

	if cmd_status == 0:
		print("\nTakserver Plugins deployed")
	else:
		print("\nTakserver Services was not deployed")


# Find Cluster Load Balancer.. Wait Until It's Found AND Active. Return Its DNS
def getLoadBalancerDNS():
	dns = ''
	load_balancer_active = False

	while not load_balancer_active:
		try:
			load_balancers = boto3.client('elbv2', region_name=TAK_CLUSTER_REGION).describe_load_balancers()
		
		except botocore.exceptions.ClientError as e:
			printJson(e.response['Error'])
			sys.exit(1)
		
		for load_balancer in load_balancers['LoadBalancers']:
			try:
				cluster_load_balancer = boto3.client('elbv2', region_name=TAK_CLUSTER_REGION).describe_tags(ResourceArns=[load_balancer['LoadBalancerArn']])
			
			except botocore.exceptions.ClientError as e:
				printJson(e.response['Error'])
				sys.exit(1)
			
			for tag_description in cluster_load_balancer['TagDescriptions']:
				for tag in tag_description['Tags']:
					tag_value = tag['Value']
					if tag_value == TAK_CLUSTER_NAME + '.' + TAK_CLUSTER_DOMAIN_NAME:
						dns = load_balancer['DNSName']
						if load_balancer['State']['Code'] == 'active':
							load_balancer_active = True
							printJson(load_balancer)
							print('\nLoad Balancer is now Active at ' + dns)

		if not load_balancer_active:
			print('Load Balancer not Active yet. Checking again in 60 seconds')
			time.sleep(60)

	return dns

# Get The Cluster Hosted Zone - Return Its ID
def getHostedZoneId():
	zone_id = ''
	try:
		zones = boto3.client('route53', region_name=TAK_CLUSTER_REGION).list_hosted_zones()
	
	except botocore.exceptions.ClientError as e:
		printJson(e.response['Error'])
		sys.exit(1)

	for zone in zones['HostedZones']:
		if zone['Name'] == TAK_CLUSTER_DOMAIN_NAME + '.':
			zone_id = zone['Id']
			printJson(zone)
			print('\nFound Hosted Zone')
			break
	
	return zone_id

# Create a CNAME For the Load Balancers DNS
def createCNAMEForLoadBalancerDNS(zone, dns):
	if not dns:
		print('\nCould not find load balancer DNS')
		sys.exit(1)

	if not zone:
		print('\nCould not find hosted zone matching ' + TAK_CLUSTER_DOMAIN_NAME)
		sys.exit(1)

	try:
		create_resource_record_sets_res = boto3.client('route53', region_name=TAK_CLUSTER_REGION).change_resource_record_sets(
		    HostedZoneId=zone,
		    ChangeBatch={
		    	'Changes': [
		            {
		                'Action': 'UPSERT',
		                'ResourceRecordSet': {
		                    'Name': TAK_CLUSTER_NAME + '.' + TAK_CLUSTER_DOMAIN_NAME,
		                    'Type': 'CNAME',
		                    'TTL': 5,
		                    'ResourceRecords': [{'Value': dns}]
		                }
		            },
		        ]
		    }
		)
	
	except botocore.exceptions.ClientError as e:
		printJson(e.response['Error'])
		sys.exit(1)

	printJson(create_resource_record_sets_res)
	print('\nUpserted CNAME Record Set')

# Test That CNAME Resolves
def testTakseverCNAME(zone):
	dns_is_ready = False
	domain_name = ''
	while not dns_is_ready:
		try: 
			test_dns_answer_res = boto3.client('route53', region_name=TAK_CLUSTER_REGION).test_dns_answer(
			    HostedZoneId=zone,
			    RecordName=TAK_CLUSTER_NAME + '.' + TAK_CLUSTER_DOMAIN_NAME,
			    RecordType='CNAME'
			)
			if test_dns_answer_res['ResponseCode'] != 'NOERROR':
				print('\nCNAME status is ' + test_dns_answer_res['ResponseCode'] + ', checking again in 30 seconds')
				time.sleep(30)
			
			else:
				dns_is_ready = True
				printJson(test_dns_answer_res)
				print('\nTakserver DNS is now active ' + test_dns_answer_res['RecordName'] )
				domain_name = test_dns_answer_res['RecordName']
		
		except botocore.exceptions.ClientError as e:
			printJson(e.response['Error'])
			sys.exit(1)

	return domain_name

# Print Takserver Entrypoints Defined In CoreConfig
def printTakserverEntryPoints(domain_name):
	try:
		core_config = xml.etree.ElementTree.parse(CLUSTER_HOME_DIR + '/CoreConfig.xml')
		xml.etree.ElementTree.register_namespace('', 'http://bbn.com/marti/xml/config')
		xml.etree.ElementTree.register_namespace('xsi', 'http://www.w3.org/2001/XMLSchema-instance')

		for network in core_config.getroot().findall('{http://bbn.com/marti/xml/config}network'):
			for input in network.findall('{http://bbn.com/marti/xml/config}input'):
				print ('\nClients can now connect to Taksever at ' + domain_name + ':' + input.get('port') + ' using protocol ' 
					+ input.get('protocol') + ' for ' + input.get('_name'))

			for connector in network.findall('{http://bbn.com/marti/xml/config}connector'):
				http = 'https://' if "https" in connector.get('_name') else 'http://'
				print ('\nTakserver can now be reached at ' + http
					+ domain_name + ':' + connector.get('port') + ' for ' + connector.get('_name'))
	
	except:
		print ('\nError reading entrypoints from CoreConfig.xml...')
		sys.exit(1)

def installHelmChart():
	max_tak_pods = int(TAK_CLUSTER_NODE_COUNT) * 3

	total_msg = math.floor(max_tak_pods * .667)
	total_api = max_tak_pods - total_msg
	total_ignite = max(1, int(round(total_msg / 5, 0)))
	total_nats = max(1, int(round(total_msg / 5, 0)))
	password = runCmd("aws ecr get-login-password")
	configmap = 'certConfigMapName'

	if CERT_CONFIGMAP_FILE != '':
		runCmd('kubectl create -f cert-migration-replacement.yaml -n takserver')
		configmap = 'cert-migration-replacement'

	runCmd('kubectl delete secret -n takserver reg-cred --ignore-not-found')
	runCmd('kubectl create secret -n takserver docker-registry reg-cred \
			--docker-server=$AWS_ACCOUNT_ID.dkr.ecr.$TAK_CLUSTER_REGION.amazonaws.com \
			--docker-username=AWS \
			--docker-password=' + str(password) + '\
			--namespace=takserver')
	runCmd('helm dependency update')
	runCmd(('helm upgrade --install -n takserver takserver src/takserver-cluster/deployments/helm -f src/takserver-cluster/deployments/helm/developer-values.yaml'
			+ ' --set certConfigMapName=' + configmap
			+ ' --set takserver.plugins.enabled=' + str(TAK_PLUGINS == '1')
			+ ' --set takserver.messaging.replicas=' + str(total_msg)
			+ ' --set takserver.api.replicas=' + str(total_api)
			+ ' --set takserver.messaging.image.repository=' + AWS_ECR_URI
			+ ' --set takserver.api.image.repository=' + AWS_ECR_URI
			+ ' --set takserver.plugins.image.repository=' + AWS_ECR_URI
			+ ' --set ignite.replicaCount=' + str(total_ignite)
			+ ' --set nats.cluster.replicas=' + str(total_nats)
			+ ' --set stan.cluster.replicas=' + str(total_nats)))

print("---------- Running AWS S3 Setup Commands ----------")
setupS3()
print("\n---------- Running AWS ECR Setup Commands ----------")
setupECR()
print("\n---------- Running AWS Cluster Initialization Commands ----------")
setupCluster()
print("\n---------- Running Kops Validation Commands ----------")
validateKops()
print("\n---------- Running KubeCtl Validation Commands ----------")
validateKubeCtl()
setupRDS()
print("\n---------- Running Docker Check ----------")
checkDocker()
print("\n---------- Running AWS RDS Deployment Commands ----------")
deployDatabaseSetupPod()
deployIngress()
print("\n---------- Running Docker Check ----------")
checkDocker()
print("\n---------- Running AWS Load Balancer Service Commands ----------")
deployLoadBalancer()
print("\n---------- Running Takserver Certificate Generation Commands ----------")
generateTakseverCertificates()
print("\n---------- Adding CoreConfigMap ----------")
addCoreConfigMap()
print("\n---------- Publishing Takserver Core Docker Images ----------")
publishTakserverCoreImages()
if TAK_PLUGINS == '1':
	print("\n---------- Publishing Takserver Plugin Docker Images ----------")
	publishTakserverPluginImages()
else:
	print("\n---------- Plugins Disabled, Plugin Docker Image Publications ----------")

print("\n---------- Deploying services to cluster ----------")
installHelmChart()
print("\n---------- Running AWS Get Load Balancer DNS Commands ----------")
dns = getLoadBalancerDNS()
print("\n---------- Running AWS Get Hosted Zone ID Commands ----------")
zone = getHostedZoneId()
print("\n---------- Running AWS Create CNAME Record Set Commands ----------")
createCNAMEForLoadBalancerDNS(zone, dns)
print("\n---------- Running AWS Test CNAME DNS Commands ----------")
domain_name = testTakseverCNAME(zone)
print("\n---------- Running TAK Entrypoint Commands ----------")
printTakserverEntryPoints(domain_name)

