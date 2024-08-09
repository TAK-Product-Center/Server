
# TAK Server Federation Hub

*Requires Java 17.*

## Description

The federation of TAK data allows entities in separate administrative and trust domains to exchange information. Each TAK server has the ability to establish direct federated connections with other TAK servers, but this requires TAK servers to manage connections and authentication with each other.

The Federation Hub centralizes the management of federation capabilities in a TAK server deployment by using a hub and spoke topology. Instead of TAK server federates connecting to each other directly, they connect to a Federation Hub. The hub performs connection and trust management, and brokers federated data according to a policy defined by an administrator.

The Federation Hub can be built and run independently of TAK server.

## Developer Build

To build each microservice from source, navigate to `takserver/src` and run these commands:

```
./gradlew :federation-hub-policy:build
./gradlew :federation-hub-broker:build
./gradlew :federation-hub-ui:bootWar
```

To build the Federation Hub manager tool to enable admin access, run:

```
./gradlew :federation-hub-ui:shadowJar
```

To build the .rpm for the Federation Hub, run:

```
./gradlew takserver-package:federation-hub:buildRpm
```

### Local Microservice Start Order: 
1. policy manager (ignite server)
2. broker 
3. UI (optional)

## Install and Run RHEL7
Update yum

```
sudo yum update -y
```

Install Java 17
```
sudo yum install wget -y
sudo wget https://download.oracle.com/java/17/latest/jdk-17_linux-x64_bin.rpm
sudo yum install -y ./jdk-17_linux-x64_bin.rpm
```

To install from the .rpm, run:

```
sudo rpm -ivh takserver-fed-hub-*.noarch.rpm --nodeps
```

## Install and Run RHEL8
Update yum

```
sudo dnf update -y
```

Install Java 17
```
sudo dnf install java-17-openjdk-devel -y
```

To install from the .rpm, run:

```
sudo yum install takserver-fed-hub-*.noarch.rpm -y
```

Add and Apply SELinux
```
sudo dnf install checkpolicy
cd /opt/tak/federation-hub && sudo ./apply-selinux.sh && sudo semodule -l | grep takserver
```

## Install and Run Debian
Update apt

```
sudo apt update -y
```

To install from the .deb, run: (if you see the error: couldn't be accessed by user 'apt'. - pkgAcquire::Run (13: Permission denied), that is OK)

```
sudo apt install <absolute path>/takserver-fed-hub_*.deb -y
```



## Install Mongo
Make sure /opt/tak/federation-hub/configs/federation-hub-broker.yml has your database credentials defined. Defaults will be generated otherwise
```
dbUsername: martiuser
dbPassword: pass4marti
```

Mongo Setup RHEL
```
sudo cp /opt/tak/federation-hub/scripts/db/mongodb-org.repo /etc/yum.repos.d/mongodb-org.repo
sudo yum install -y mongodb-org
sudo systemctl daemon-reload
sudo systemctl enable mongod
sudo systemctl restart mongod
sudo /opt/tak/federation-hub/scripts/db/configure.sh
```

Mongo Setup Debian
```
sudo apt install curl software-properties-common gnupg apt-transport-https ca-certificates -y

curl -fsSL https://pgp.mongodb.com/server-7.0.asc |  sudo gpg -o /usr/share/keyrings/mongodb-server-7.0.gpg --dearmor

echo "deb [ arch=amd64,arm64 signed-by=/usr/share/keyrings/mongodb-server-7.0.gpg ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/7.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list

sudo apt update && sudo apt install mongodb-org -y
sudo systemctl enable mongod
sudo systemctl restart mongod

sudo /opt/tak/federation-hub/scripts/db/configure.sh
```

## Update Fedhub
Before updating the Federation Hub, you should back up the policy file and list of authorized users:

```
mv /opt/tak/federation-hub/ui_generated_policy.json /tmp
mv /opt/tak/federation-hub/authorized_users.yml /tmp
```

RHEL7
```
sudo rpm -Uvh takserver-fed-hub-*.noarch.rpm --nodeps
```

RHEL8
```
sudo yum upgrade takserver-fed-hub-*.noarch.rpm
```

Debian
```
sudo apt install <absolute path>/takserver-fed-hub_*.deb -y
```

The policy and authorized can then be replaced:
```
mv /tmp/ui_generated_policy.json /opt/tak/federation-hub/
mv /tmp/authorized_users.yml /opt/tak/federation-hub/
```

## Configuration
**The Federation Hub authenticates clients using TLS with X.509 client certificates. Scripts for generating a private security enclave, including a Certificate Authority (CA), and certs for use by the Federation Hub are in the TAK server documentation. See the TAK server configuration guide (docs/TAK_Server_Configuration_Guide.pdf) for additional information.**

The Federation Hub can then be started as a system service (and enabled to run on boot):

```
sudo systemctl restart federation-hub
sudo systemctl enable federation-hub
```

To run the microservices directly, the following scripts can be invoked (in this order):

```
sudo ./federation-hub-policy/scripts/federation-hub-policy.sh
sudo ./federation-hub-broker/scripts/federation-hub-broker.sh
sudo ./federation-hub-ui/scripts/federation-hub-ui.sh
```

The Federation Hub consists of three processes: a policy manager, an administrative UI, and a messaging broker. The system service runs all three simultaneously.

## Client Authentication and Authorization

To authorize clients to act as administrators and enable access to the admin UI, use `federation-hub-manager.jar`:

```
java -jar /opt/tak/federation-hub/jars/federation-hub-manager.jar path/to/cert.pem
```

By default, authorized users are written to `/opt/tak/federation-hub/authorized_users.yml`. Optionally, you can specify the location of the output file where the authorized users are written to:

```
java -jar /opt/tak/federation-hub/jars/federation-hub-manager.jar path/to/cert.pem ./authorized_users.yml
```

The location of the authorized users file should match the location configured for the admin UI microservice (in `/opt/tak/federation-hub/configs/federation-hub-ui.yml`).
