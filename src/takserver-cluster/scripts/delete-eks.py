import boto3
import time
import subprocess
import sys
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
TAK_CLUSTER_REGION = os.environ['TAK_CLUSTER_REGION']
KOPS_STATE_STORE = os.environ['KOPS_STATE_STORE']
TAK_CLUSTER_NODE_COUNT = os.environ['TAK_CLUSTER_NODE_COUNT']
AWS_ECR_URI = ''


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

# Delete All Kubectl Resources
def deleteKubectl():
	runCmd('kubectl delete --all namespaces')

# Delete RDS DB If Exists
def deleteRDS():
	try:
		find_rds_db_res = boto3.client('rds', region_name=TAK_CLUSTER_REGION).describe_db_instances(DBInstanceIdentifier=TAK_CLUSTER_NAME)
		printJson(find_rds_db_res['DBInstances'][0])
		print('\nFound RDS Database to Delete')
		
		try:
			delete_rds_db_res = boto3.client('rds', region_name=TAK_CLUSTER_REGION).delete_db_instance(
			    DBInstanceIdentifier=find_rds_db_res['DBInstances'][0]['DBInstanceIdentifier'],
			    SkipFinalSnapshot=True,
			    DeleteAutomatedBackups=True
			)
			printJson(delete_rds_db_res)
			print('\nRDS Database Deletion Request Successful')
		
		except botocore.exceptions.ClientError as e:
			printJson(e.response['Error'])
			print('RDS Database could not be deleted.. attemping to continue')
	
	except botocore.exceptions.ClientError as e:
		printJson(e.response['Error'])
		print('\nRDS Database not Found.. Continuing')
		return;

	while True:
		try:
			delete_db_instances_res = boto3.client('rds', region_name=TAK_CLUSTER_REGION).describe_db_instances(DBInstanceIdentifier=TAK_CLUSTER_NAME)
			print ('Database is currently ' + delete_db_instances_res['DBInstances'][0]['DBInstanceStatus'] + ', checking again in 30 seconds')
			time.sleep(30)
		
		except botocore.exceptions.ClientError as e:
			printJson(e.response['Error'])
			print('\nRDS has been successfully deleted')
			break;

# Delete RDS DB Subnet Group If Exists
def deleteSubnetGroups():
	try:
		delete_db_subnet_groups_res = boto3.client('rds', region_name=TAK_CLUSTER_REGION).describe_db_subnet_groups(DBSubnetGroupName=TAK_CLUSTER_NAME + '-SG')
		printJson (delete_db_subnet_groups_res)
		print ('\nFound RDS Subnet Group to Delete')

		try:
			delete_db_subnet_group = boto3.client('rds', region_name=TAK_CLUSTER_REGION).delete_db_subnet_group(DBSubnetGroupName=TAK_CLUSTER_NAME + '-SG')
			printJson(delete_db_subnet_group)
			print ('\nRDS Subnet Group Deletion Request Successful')
		
		except botocore.exceptions.ClientError as e:
			printJson(e.response['Error'])
			print('\nRDS Subnet Group could not be deleted.. attemping to continue')
			
	except botocore.exceptions.ClientError as e:
		printJson(e.response['Error'])
		print('\nRDS Subnet Group not Found.. Continuing')
		return;

# Delete AWS ECR If Exists
def deleteECR():
	try:
	    find_ecr_repository_res = boto3.client('ecr', region_name=TAK_CLUSTER_REGION).describe_repositories(repositoryNames=['tak/server-' + TAK_CLUSTER_NAME])
	    printJson(find_ecr_repository_res)
	    print ('\nFound AWS ECR to Delete')

	    try:
	    	delete_ecr_repository_res = boto3.client('ecr', region_name=TAK_CLUSTER_REGION).delete_repository(
	    		registryId=find_ecr_repository_res['repositories'][0]['registryId'],
	    		repositoryName=find_ecr_repository_res['repositories'][0]['repositoryName'],
	    		force=True
	    	)
	    	printJson(delete_ecr_repository_res)
	    	print('\nECR Deletion Request Successful')
	    
	    except botocore.exceptions.ClientError as e:
	    	printJson(e.response['Error'])
	    	print('\nECR could not be deleted.. attemping to continue')
	
	except botocore.exceptions.ClientError as e:
		printJson(e.response['Error'])
		print('\nAWS ECR not Found.. Continuing')
		return;

# Delete Kops Cluster If Exists
def deleteCluster():
	runCmd('eksctl delete cluster --timeout 60m0s --wait -f ' + CLUSTER_HOME_DIR + '/eks-cluster.yaml')

# Delete Cluster CNAME If Exists
def deleteCNAME():
	record_to_delete = None;
	zone_id = None
	try:
		zones = boto3.client('route53', region_name=TAK_CLUSTER_REGION).list_hosted_zones()
		for zone in zones['HostedZones']:
			if zone['Name'] == TAK_CLUSTER_DOMAIN_NAME + '.':
				zone_id = zone['Id']
				printJson(zone)
				print('\nFound Hosted Zone\n')
				break;
	
	except botocore.exceptions.ClientError as e:
		printJson(e.response['Error'])
		print('\nCould not find a list of zones..')
		return;

	if not zone_id:
		print('\nCould not find hosted zone matching ' + TAK_CLUSTER_DOMAIN_NAME)
		return;

	try: 
		find_resource_record_sets_res = boto3.client('route53', region_name=TAK_CLUSTER_REGION).list_resource_record_sets(
		    HostedZoneId=zone_id,
		    StartRecordName=TAK_CLUSTER_NAME + '.' + TAK_CLUSTER_DOMAIN_NAME,
		    StartRecordType='CNAME'
		)

		for record in find_resource_record_sets_res['ResourceRecordSets']:
			if record['Name'] == TAK_CLUSTER_NAME + '.' + TAK_CLUSTER_DOMAIN_NAME + '.':		
				record_to_delete = record;
				printJson(record_to_delete)
				print('\nFound CNAME Record\n')
				break;

	except botocore.exceptions.ClientError as e:
		printJson(e.response['Error'])
		print('\nCNAME record could not be found.. attempting to continue')
		return;

	if not record_to_delete:
		print('\nCould not find the CNAME Record to Delete')
		return;

	try:
		delete_resource_record_sets_res = boto3.client('route53', region_name=TAK_CLUSTER_REGION).change_resource_record_sets(
		    HostedZoneId=zone_id,
		    ChangeBatch={
		    	'Changes': [
		            {
		                'Action': 'DELETE',
		                'ResourceRecordSet': {
		                    'Name': record_to_delete['Name'],
		                    'Type': record_to_delete['Type'],
		                    'TTL': record_to_delete['TTL'],
		                    'ResourceRecords': [{'Value': record_to_delete['ResourceRecords'][0]['Value']}]
		                }
		            },
		        ]
		    }
		)
		printJson(delete_resource_record_sets_res)
		print('\nCNAME Deletion Request Successful')
	
	except botocore.exceptions.ClientError as e:
		printJson(e.response['Error'])
		print('\nCNAME could not be deleted.. attemping to continue')

print("---------- Delete All Kubectl Resources ----------")
deleteKubectl()
print("---------- Running AWS RDS Deletion Commands ----------")
deleteRDS();
print("\n---------- Running AWS RDS Subnet Group Deletion Commands ----------")
deleteSubnetGroups()
print("\n---------- Running AWS ECR Deletion Commands ----------")
deleteECR()
print("\n---------- Running AWS CNAME Deletion Commands ----------")
deleteCNAME()
print("\n---------- Running Takserver Cluster Deletion Commands ----------")
deleteCluster()

