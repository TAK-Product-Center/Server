## Setup
You'll need a TAK Server with a grpc input similiar to the following:
<input auth="x509" _name="tls" protocol="tls" port="8089" coreVersion2TlsVersions="TLSv1.2,TLSv1.3"/>

## Build
./gradlew shadowjar

## Run
java -jar build/libs/nettytestclient-1.0-uber.jar 