# TAK Server Federation Hub

*Requires Java 11.*

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

## Install and Run

To install from the .rpm, run:

```
sudo yum install federation-hub-*.noarch.rpm
```

The Federation Hub can then be started as a system service (and enabled to run on boot):

```
sudo systemctl start federation-hub
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

The Federation Hub authenticates clients using TLS with X.509 client certificates. Scripts for generating a private security enclave, including a Certificate Authority (CA), and certs for use by the Federation Hub are in the TAK server documentation. See the TAK server configuration guide (docs/TAK_Server_Configuration_Guide.pdf) for additional information.

To authorize clients to act as administrators and enable access to the admin UI, use `federation-hub-manager.jar`:

```
java -jar /opt/tak/federation-hub/jars/federation-hub-manager.jar path/to/cert.pem
```

By default, authorized users are written to `/opt/tak/federation-hub/authorized_users.yml`. Optionally, you can specify the location of the output file where the authorized users are written to:

```
java -jar /opt/tak/federation-hub/jars/federation-hub-manager.jar path/to/cert.pem ./authorized_users.yml
```

The location of the authorized users file should match the location configured for the admin UI microservice (in `/opt/tak/federation-hub/configs/federation-hub-ui.yml`).
