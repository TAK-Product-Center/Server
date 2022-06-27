import boto3
import time
import subprocess
import sys
import os

region = os.environ['region']
identifier = os.environ['identifier']
source = boto3.client('rds', region_name=region)
retryInterval = 5;

try:
  	instance = source.describe_db_instances(DBInstanceIdentifier=identifier).get('DBInstances')[0]
except :
  	print("An Error occured. Ensure the identifier and region are valid\n")
  	sys.exit(1)

while not instance.get('Endpoint') :
    print(identifier + ' is not ready. DB is currently ' + instance.get('DBInstanceStatus') + '. Trying again in ' + str(retryInterval) + ' seconds.')
    time.sleep(retryInterval)
    instance = source.describe_db_instances(DBInstanceIdentifier=identifier).get('DBInstances')[0]

subprocess.call("java -jar SchemaManager.jar SetupRds", shell=True)
subprocess.call("java -jar SchemaManager.jar upgrade", shell=True)

