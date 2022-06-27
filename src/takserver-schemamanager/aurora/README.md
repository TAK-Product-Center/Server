# Setting Up AWS Aurora With TAK Server

## Prerequisties
- CentOS or Docker

## AWS Aurora Setup (any config options not explicitly stated here can be changed to meet specific needs / requirements)

- Create Database
	1. Go To AWS
	2. Go To RDS
	3. Select Databases
	4. Create Database
		- standard create
	5. Engine Options
		- Amazon Aurora
		- Amazon Aurora with PostgreSQL compatibility
		- PostgreSQL 10.6
	6. Templetes 
		- production
	7. Settings
		- Setup a username and password and make sure they match the CoreConfig Repositiory settings
	8. Connectivity
		- Publicly accessible (No Recommended unless PG-Pool will be running outside of the cloud, in which case you should setup ip restrictions to the database)
		- Database port: 5432
	9. Database authentication
		- Password authentication
	10. Additional Configuration
		- Initial database name: cot

## PG-Pool
- AWS Aurora uses read replicas behind a load balancer to scale the database. This means there will be one endpoint for the master (write database) and one endpoint for the read replica load balancer. In order to utilize these features, you'll need a tool called PG-Pool. PG-Pool will intercept database calls and determine if they should hit the read or write database.

- It is up to you where you want to run PG-Pool. Things to consider are:
	- running an EC2 instance inside the same VPC as AWS Auroa Database with PG-Pool will give you the most network security control
	- running PG-Pool on a separate non aws server will require the AWS Auroa Database to have a public ip
	- running PG-Pool on the same server as TAK Server may starve TAK Server of resources

- Below are instructions for setting up PG-Pool on CentOs or with a Docker container


## PG-Pool Setup On CentOS 
- download and install PG-Pool
`yum install http://www.pgpool.net/yum/rpms/4.1/redhat/rhel-7-x86_64/pgpool-II-release-4.1-1.noarch.rpm`
`yum install pgpool-II-pg11-* -y`

- move into the PG-Pool directory
`cd /etc/pgpool-II`

- copy the following files from the TAK distribution into pgpool-II
	- pgpool.conf to /etc/pgpool-II
	- pool_hba.conf to /etc/pgpool-II

- fill in <DB_USER>, <DB_PASS> and then run
`pg_md5 -m -u <DB_USER> <DB_PASS>`

- fill in all the <WRITE_URL>, <READ_URL>, <DB_USER>, <DB_PASS> and then run
`sed -i "/backend_hostname0/c\backend_hostname0 = <WRITE_URL>" pgpool.conf && \
sed -i "/backend_hostname1/c\backend_hostname1 = <READ_URL>" pgpool.conf && \
sed -i "/health_check_user/c\health_check_user = <DB_USER>" pgpool.conf  && \
sed -i "/health_check_password/c\health_check_password = <DB_PASS>" pgpool.conf && \
sed -i "/sr_check_user/c\sr_check_user = <DB_USER>" pgpool.conf && \
sed -i "/sr_check_password/c\sr_check_password = <DB_PASS>" pgpool.conf`

- run pgpool
`pgpool -f pgpool.conf`

- Switch over to the TAK Server machine:
	- Make sure your CoreConfig database settings are configured with the ip of the PG-Pool service (port will still be 5432)
	- Make sure your CoreConfig database settings are configured with the Aurora DB username and password
	- move into /opt/tak/db-utils, and run 
	`java -jar SchemaManager.jar SetupRds && java -jar SchemaManager.jar upgrade`


## PG-Pool Setup With Docker
- From the pg-pool directory of the TAK distribution folder, fill in the command parameters and run:
`docker build --build-arg DB_USER=<DB_USER> --build-arg DB_PASS=<DB_PASS> --build-arg URL_WRITER=<WRITE_URL>  --build-arg URL_READER=<READ_URL> -t takserver-pg-pool -f Dockerfile.pg-pool .`

- Once the docker image is built, run:
`docker run -d -p 5432:5432 takserver-pg-pool`

- Switch over to the TAK Server machine:
	- Make sure your CoreConfig database settings are configured with the ip of the PG-Pool service (port will still be 5432)
	- Make sure your CoreConfig database settings are configured with the Aurora DB username and password
	- move into /opt/tak/db-utils, and run 
	`java -jar SchemaManager.jar SetupRds && java -jar SchemaManager.jar upgrade`




