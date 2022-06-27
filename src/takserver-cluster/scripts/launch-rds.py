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
import argparse

parser = argparse.ArgumentParser()
parser.add_argument("--region", help="takserver region. eg. us-east-1, us-east-2, us-west-1", required=True)
parser.add_argument("--db-name", help="takserver db name (unique among other databases)", required=True)
parser.add_argument("--core-config", help="Absolute path to CoreConfig.xml. default is '/opt/tak/CoreConfig.xml", default="/opt/tak/CoreConfig.xml")
parser.add_argument("--storage-size", help="Initial size in GB of the database", required=True)
parser.add_argument("--storage-size-max", help="Max size in GB of the database", required=True)
parser.add_argument("--instance-type", help="RDS instance type. See: https://aws.amazon.com/rds/instance-types/", required=True)
parser.add_argument("--vpc-id", help="Id of the VPC the database should be launched into. The database will not have a public ip, so only instances in the VPC can reach the database.", required=True)
args = parser.parse_args()

TAK_REGION = args.region
TAK_DB_NAME = args.db_name
CORE_CONFIG = args.core_config
TAK_DB_ALLOCATED_STORAGE = args.storage_size
TAK_DB_ALLOCATED_STORAGE_MAX = args.storage_size_max
TAK_DB_INSTANCE = args.instance_type
TAK_VPC = args.vpc_id
TAK_DB_USERNAME = ''
TAK_DB_PASSWORD = ''



# Run A Command Line Argument, Logging Output As We Go
def runCmd(cmd):
	p = subprocess.Popen(cmd, shell=True, stderr=subprocess.PIPE)
	while True:
	    out = p.stderr.read(1)
	    
	    if out == '' and p.poll() != None:
	        break
	    
	    if out != '':
	        sys.stdout.write(out)
	        sys.stdout.flush()
	
	return p.poll()

# Print Out Formatted Json
def printJson(j) :
	print (json.dumps(j,sort_keys=True, indent=4, default=str))

# Setup A Subnet Group For RDS DB. Skip Creation If Exists
def setupSubnetGroups():
	try:
		find_db_subnet_group_res = boto3.client('rds', region_name=TAK_REGION).describe_db_subnet_groups(DBSubnetGroupName=TAK_DB_NAME + '-SG')
		printJson (find_db_subnet_group_res)
		print ('\nSubnet Security Group Already Exists')
	
	except botocore.exceptions.ClientError as e:
		
		try:
			find_db_subnets_res = boto3.client('ec2', region_name=TAK_REGION).describe_subnets(Filters=[{'Name': 'tag:Name', 'Values':['*']}])
			
			subnets = []
			for subnet in find_db_subnets_res['Subnets']:
				if (subnet['VpcId'] == TAK_VPC) :
					subnets.append(subnet['SubnetId'])
			
			create_db_subnet_group_res = boto3.client('rds', region_name=TAK_REGION).create_db_subnet_group(DBSubnetGroupName=TAK_DB_NAME + '-SG',DBSubnetGroupDescription=TAK_DB_NAME + '-SG',SubnetIds=subnets)
			
			printJson(create_db_subnet_group_res)
			print ('\nSubnet Security Group Created')
		
		except botocore.exceptions.ClientError as e:
			printJson(e.response['Error'])
			sys.exit(1)

# Find And Return Security Group
def describeSecurityGroups() :
	try:
		find_security_groups_res = boto3.client('ec2', region_name=TAK_REGION).describe_security_groups()
		
		for group in find_security_groups_res['SecurityGroups'] :
			if (group['GroupName'] == 'Takserver-RDS-SG') :
				print('\nVPC Security Group Found')
				printJson(group)
				return group['GroupId']

		print('\nCreating VPC Security Group')

		create_security_groups_res = boto3.client('ec2', region_name=TAK_REGION).create_security_group(GroupName='Takserver-RDS-SG',Description='Security Group for Taksever RDS',VpcId=TAK_VPC)
				
		printJson(create_security_groups_res)

		print('\nAdding Security Group Ingress')

		boto3.client('ec2', region_name=TAK_REGION).authorize_security_group_ingress(
		        GroupId=create_security_groups_res['GroupId'],
		        IpPermissions=[
		            {'IpProtocol': 'tcp',
		             'FromPort': 5432,
		             'ToPort': 5432,
		             'IpRanges': [{'CidrIp': '0.0.0.0/0'}]}
		        ])

		return create_security_groups_res['GroupId']
	
	except botocore.exceptions.ClientError as e:
		print ('\nEXITING: Could Not Find Or Create Security Group')
		sys.exit(1)

# Generate And Return An MD5 Hash
def generateDBHash():
	return hashlib.md5((TAK_DB_PASSWORD + TAK_DB_USERNAME).encode()).hexdigest()

def loadDatabaseCredentialsFromCoreConfig() :
	global TAK_DB_USERNAME;
	global TAK_DB_PASSWORD;
	# Read the core config to get the username and password for the databse
	try:
		core_config = xml.etree.ElementTree.parse(CORE_CONFIG)
		xml.etree.ElementTree.register_namespace('', 'http://bbn.com/marti/xml/config')
		xml.etree.ElementTree.register_namespace('xsi', 'http://www.w3.org/2001/XMLSchema-instance')
		for repository in core_config.getroot().findall('{http://bbn.com/marti/xml/config}repository'):
			for connection in repository.findall('{http://bbn.com/marti/xml/config}connection'):
				TAK_DB_USERNAME = connection.get('username')
				TAK_DB_PASSWORD = connection.get('password')
	except:
		print ('\nError reading CoreConfig.xml...')
		sys.exit(1)


# Create RDS DB Is It Does Not Exist. Modify Core Config With RDS Credentials
def setupRDS():
	loadDatabaseCredentialsFromCoreConfig()
	setupSubnetGroups()

	rds_dns = ''
	while not rds_dns :
		
		try:
			find_db_instances_res = boto3.client('rds', region_name=TAK_REGION).describe_db_instances(DBInstanceIdentifier=TAK_DB_NAME)
			rds_dns = find_db_instances_res['DBInstances'][0]['Endpoint']['Address']
			printJson(find_db_instances_res)
			print('\nDatabase Exists At ' + rds_dns)
		
		except botocore.exceptions.ClientError as e:
			
			try:
				create_db_instance_res = (boto3.client('rds', region_name=TAK_REGION)
				.create_db_instance(
				    DBName='cot',
				    DBInstanceIdentifier=TAK_DB_NAME,
				    AllocatedStorage=int(TAK_DB_ALLOCATED_STORAGE),
				    MaxAllocatedStorage=int(TAK_DB_ALLOCATED_STORAGE_MAX),
				    DBInstanceClass=TAK_DB_INSTANCE,
				    Engine='postgres',
				    MasterUsername=TAK_DB_USERNAME,
				    MasterUserPassword='md5' + generateDBHash(),
				    DBSubnetGroupName=TAK_DB_NAME + '-SG',
				    VpcSecurityGroupIds=[
				        describeSecurityGroups()
				    ],
				    EngineVersion='10.20',
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
		core_config = xml.etree.ElementTree.parse(CORE_CONFIG)
		xml.etree.ElementTree.register_namespace('', 'http://bbn.com/marti/xml/config')
		xml.etree.ElementTree.register_namespace('xsi', 'http://www.w3.org/2001/XMLSchema-instance')

		for repository in core_config.getroot().findall('{http://bbn.com/marti/xml/config}repository'):
			for connection in repository.findall('{http://bbn.com/marti/xml/config}connection'):
				connection.set('url', 'jdbc:postgresql://' + rds_dns + ':5432/cot')

		core_config.write(CORE_CONFIG)
		print('\nCoreConfig successfully modified to include RDS credentials')
	
	except:
		print ('\nError writing to CoreConfig.xml...')
		sys.exit(1)

	try:
		runCmd('java -jar /opt/tak/db-utils/SchemaManager.jar SetupRds')
		runCmd('java -jar /opt/tak/db-utils/SchemaManager.jar upgrade')
	except:
		print('\nCould not run schema manager')


setupRDS()
