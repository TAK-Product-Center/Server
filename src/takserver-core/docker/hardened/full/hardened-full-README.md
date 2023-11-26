# Docker Hardened Full Setup and Execution

1. unzip the .zip file and then cd to the unzipped directory
2. Edit `tak/CoreConfig.example.xml` and update the **password** field to a non-empty value.  Then save the file. 
3. Edit the values in `docker/EDIT_ME.env` to non-empty values. (Make sure you use double quotes)  
4. From the current directory, type `docker compose -f docker/docker-compose.yml up` .    If you want to execute it in the background and get the command prompt back, add a ` -d` at the end of the command.
5. Once the TAKServer has started (after the Admin cert has been created), Copy / import the `takdata/certs/admin.pk12` file into your browser or Keychain.
6. Assuming steps 5 and 6 have both happened, navigate your browser to `https://{TAK Server Hostname}:8443` where the **TAK Server Hostname** is replaced with the proper hostname.


Note the following:
1. The docker compose command will:
   1. build both images and add to the local registry 
   2. setup the docker network 
   3. create 3 containers (2 for the TAK DB instances and 1 for the TAK Server)
   4. Lastly run the 3 containers 
      1. starting the DB containers first 
      2. starting the TAK Server after the DB containers have begun execution 
2. The docker compose will start all 3 containers on the current host / machine but each will think its on a different host. 
3. The first run of the DB containers will setup the DB and pgpool in those containers including the postgres and pgpool users / groups 
3. The first run of the TAK Server will automatically setup the TAK Server with a cert and the default cert password 
   1. It will also expose the certs/files and logs directories under `/takdata/certs` and `takdata/logs` respectively.

