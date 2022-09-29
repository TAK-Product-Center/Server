# Using Hardened Docker Images

## Certificate Generation

To build the certificates required for TAKServer, update the build arguments in the command below, and in the root path of the unzipped hardened docker file contents:
### build the ca-setup-hardened image
```shell
docker build -t ca-setup-hardened --build-arg ARG_CA_NAME=<CA_NAME> --build-arg ARG_STATE=<ST> --build-arg ARG_CITY=<CITY> --build-arg ARG_ORGANIZATIONAL_UNIT=<UNIT> -f docker/Dockerfile.ca .
```
### run the ca-setup-hardened container
```shell
docker run --name ca-setup-hardened -it -d ca-setup-hardened
```
If certificates already exist in the `tak/cert/files` path when building the `ca-setup-hardened` image, certificate generation will be skipped at runtime.

To extract the generated certificates to a `files` folder in the root of the unzipped hardened docker contents on your host machine:

### copy the generated certificate files
```shell
docker cp ca-setup-hardened:/tak/certs/files files 
```
Run the following copy commands to ensure the required takserver certificates are available to the TAKServer container:
```shell
[ -d tak/certs/files ] || mkdir tak/certs/files \
&& docker cp ca-setup-hardened:/tak/certs/files/takserver.jks tak/certs/files/ \
&& docker cp ca-setup-hardened:/tak/certs/files/truststore-root.jks tak/certs/files/ \
&& docker cp ca-setup-hardened:/tak/certs/files/fed-truststore.jks tak/certs/files/ \
&& docker cp ca-setup-hardened:/tak/certs/files/admin.pem tak/certs/files/ \
&& docker cp ca-setup-hardened:/tak/certs/files/config-takserver.cfg tak/certs/files/
```

Note: The certificate generation container is only required to run once for TAKServer initialization.

## Building TAKServer
Building the hardened takserver and tak-database docker images requires creating an [Iron Bank/Repo1](https://repo1.dso.mil/dsop/dccscr#overview) account to access approved base images.
To create an account, follow the steps in the [IronBank Getting Started](https://repo1.dso.mil/dsop/dccscr#getting-started) instructions.

Note: To log in via the CLI, see the instructions in the [Registry Access](https://repo1.dso.mil/dsop/dccscr#registry-access) section.

To include the TAK Server version in the images tags, network, and container names, you can add the suffix `-"$(cat tak/version.txt)"` in the commands below.
### create docker bridge network to run takserver
```
docker network create takserver-net --subnet=172.28.0.0/16
```
Or to specify the subnet on network creation:
```shell
docker network create takserver-net-hardened -–subnet=<subnet>
```

### configure takserver and tak-database
This is a general guide of expected configurations required to successfully run the hardened containers:

- In `tak/CoreConfig.xml `, update the `connection url` with the hardened tak database container name and the postgres password.
    ```xml
    <connection url="jdbc:postgresql://tak-database-hardened:5432/cot" username="martiuser" password=<>/>
    ```
- Ensure in the `db-utils/pg_hba.conf` file that there is a host entry for the subnet of the hardened takserver network. For example:
  ```
  # TYPE  DATABASE        USER            ADDRESS                 METHOD
  host    all		all     	172.28.0.0/16		md5 
  ```
- To determine the subnet of the takserver network:
```shell
docker network inspect takserver-net-hardened
```

### build tak-database hardened image
```shell
docker build -t tak-database-hardened -f docker/Dockerfile.hardened-takserver-db . 
```
### build takserver hardened image
```shell
docker build -t takserver-hardened -f docker/Dockerfile.hardened-takserver .
```
## Running TAKServer

### run tak-database, connect to network
```shell
docker run --name tak-database-hardened --network takserver-net -d tak-database-hardened -p 5432:5432
```

### run takserver, connect to docker bridge network, and expose default takserver ports on docker host
```shell
docker run --name takserver-hardened --network takserver-net -p 8089:8089 -p 8443:8443 -p 8444:8444 -p 8446:8446 -t -d takserver-hardened
```

### get the admin cert fingerprint
```shell
docker exec -it ca-setup-hardened bash -c "openssl x509 -noout -fingerprint -md5 -inform pem -in files/admin.pem | grep -oP 'MD5 Fingerprint=\K.*'"
```

### Add the cert fingerprint as the admin after takserver has started up (about 60 seconds)
```shell
docker exec -it takserver-hardened bash -c 'java -jar /opt/tak/utils/UserManager.jar usermod -A -f <admin cert fingerprint> admin'
```

## Troubleshooting
- To view the takserver log:
```shell
docker exec -it takserver-hardened bash -c "cd /opt/tak/logs/ && tail -f takserver.log"
```
- In WSL, if there are errors on the shell scripts resembling: `/bin/bash^M: bad interpreter:`, ensure the line endings of the zip contents are in Unix.
