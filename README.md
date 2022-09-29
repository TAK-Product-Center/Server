# TAK Server Development
*Requires Java 11*

* Linux / MacOS is recommended for development. If using Windows, replace "gradlew" with "gradlew.bat" in commands below.

Links:
 * [Test Execution](src/takserver-takcl-core/docs/testing.md)
 * [Test Architecture and Development](src/takserver-takcl-core/docs/Development.md)
 * [Publishing](src/docs/publishing.md)

---
Clean and Build TAK Server, including war, retention service, plugin manager, user manager and schema manager.
```
cd src
./gradlew clean bootWar bootJar shadowJar
```

In Eclipse, choose File -> Import -> Gradle -> Existing Gradle Project

Navigate to `takserver/src`

Select Finish. The TAK Server parent project, and all subprojects, will be imported into Eclipse.

Install PostgreSQL + PostGIS extension locally on your workstation, or run the docker container as described below. If installing locally, use 

Start the Postres server.

To run a local PostgreSQL + PostGIS container, follow the commands below using the official PostGIS database docker container as follows, and changing the environment variables supplied to the container as necessary. Note the '--rm' means the container will be destroyed when it is stopped.

```
docker run -it -d --rm --name TakserverServer0DB \
    --env POSTGRES_PASSWORD=e815f795745e \
    --env POSTGRES_HOST_AUTH_METHOD=trust \
    --env POSTGRES_USER=martiuser \
    --env POSTGRES_DB=cot \
    -p 5432 postgis/postgis:10-3.1

echo SQL SERVER IP: `docker inspect --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' TakserverServer0DB`
```

Setup Local Database. If the postgis container was used, only the last two lines should be necessary.  
```
- cd src/takserver-schemamanager
- psql -d postgres  -c "CREATE ROLE martiuser LOGIN ENCRYPTED PASSWORD 'md564d5850dcafc6b4ddd03040ad1260bc2' SUPERUSER INHERIT CREATEDB NOCREATEROLE;"
- createdb --owner=martiuser cot
- ../gradlew shadowJar
- java -jar build/libs/schemamanager-<version>-uber.jar upgrade # Make sure that the CoreConfig.xml is in the current directory
```

Configure Local CoreConfig and Certs
```cd takserver-core/example```

This is the CoreConfig that takserver war will look for when running from the takserver-core/example directory. From this point, just follow the instructions at takserver/src/docs/TAK_Server_Configuration_Guide.pdf to set up the CoreConfig and Certs. Make sure that the CoreConfig now points to the directory where the certs were generated locally.

See appendix B in src/docs/TAK_Server_Configuration_Guide.pdf for cert generation instructions.

### Build and run TAK server locally for development

```
cd takserver-core
../gradlew clean bootWar bootJar
cd example
export JDK_JAVA_OPTIONS="-Dloader.path=WEB-INF/lib-provided,WEB-INF/lib,WEB-INF/classes,file:lib/ -Djava.net.preferIPv4Stack=true -Djava.security.egd=file:/dev/./urandom -DIGNITE_UPDATE_NOTIFIER=false -DIGNITE_QUIET=true"
```

TAK server consists of two processes: Messaging and API. The messaging process can run independently, but the API process needs to connect to the ignite server that runs as a part of the messaging process. For both processes, -Xmx should always be specified.

Run Messaging (note - this command and the following one to run api include the **duplicatelogs** profile. This turns off the filter that blocks duplicated log messages that cause log spam in operational deployments of TAK Server.
```
java -Xmx<value> -Dspring.profiles.active=messaging,duplicatelogs -jar ../build/libs/takserver-core-xyz.war
```

Run API
```
java -Xmx<value> -Dspring.profiles.active=api,duplicatelogs -jar ../build/libs/takserver-core-xyz.war
```

Run Plugin Manager (useful when working on plugin capability)
```
java -Xmx<value> -jar ../../takserver-plugin-manager/build/libs/takserver-plugin-manager-xyz.jar 
```

### RPM Generation
Separate RPMs are generated to install the following components of TAK server:

* api
* messaging
* database

To build all RPMs:
```
cd <repo-home>/src
./gradlew clean buildRpm
```

Subproject RPMs may be built individually using the following commands:
 
* takserver-package:api:buildRpm
* takserver-package:messaging:buildRpm
* takserver-package:database:buildRpm
* takserver-package:launcher:buildRpm
* takserver-package:takserver:buildRpm

## Certificates
TAK Server uses client and server certificates, TLS and X.509 mutual authentication and for channel encryption. Scripts for generating a private security enclave, including a Certificate Authority (CA), and certs for use by TAK Server and clients are located in /utils/misc/certs.

See the TAK Server configuration guide (docs/TAK_Server_Configuration_Guide.pdf) for additional information about TAK Server's capabilities.

## Logging
Logging levels for loggers at the class or package level can be set on startup:
```
java -Xmx<value> -Dspring.profiles.active=messaging -jar ../build/libs/takserver-core-1.3.13-DEV-xyz.war --logging.level.com.bbn.marti.sync=DEBUG --logging.level.marti_data_access_audit_log=OFF
```

turn down log level of all logs:
```java -jar takserver.war $@ --logging.level.root=ERROR```

turn down log level for subscriptions:
```java -jar takserver.war $@ --logging.level.com.bbn.marti.service.Subscription=ERROR```

turn off logs just for subscriptions:
```java -jar takserver.war $@ --logging.level.com.bbn.marti.service.Subscription=OFF```

entirely disable most logging:
```java -jar takserver.war $@ --logging.level.root=OFF```

The default log level for most things is INFO. Possible levels are INFO, WARN, ERROR, OFF (in order of decreasing log frequency)


These levels can be applied globally with this option 

```--logging.level.root=<level>```

i.e.

```--logging.level.root=ERROR```

## Swagger
https://localhost:8443/swagger-ui.html

## TAK Server CI

### Integration Tests

Integration tests are executed against master nightly. In addition to this, they can be executed on any branch as follows:  
1.  Navigate to the [TAKServer Dashboard](https://git.tak.gov/core/takserver).  
2.  On the sidebar, hover over 'CI/CD' and select 'Pipelines'.  
3.  Find your commit from the list and tap the Play button to the right, and select the test suite you would like to execute.  The Main suites are what is executed nightly and execute all the tests.  


Build takserver and plugin manager

```
cd <repo-home>/src
./gradlew clean build bootWar bootJar
```



