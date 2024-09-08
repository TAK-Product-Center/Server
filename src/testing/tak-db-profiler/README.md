## Setup

## Build

Exporter
```
./gradlew shadowjar buildExporter
```

Importer
```
./gradlew shadowjar buildImporter
```

## Run

### With default config options
```
java -jar build/libs/tak-db-profiler-exporter-*.jar
```
```
java -jar build/libs/tak-db-profiler-importer-*.jar
```


### With custom config options
```
java -jar build/libs/tak-db-profiler-exporter-*.jar -host <host> -port <port> -password <password> -username <username> -configDir <configDir>
```
```
java -jar build/libs/tak-db-profiler-importer-*.jar -host <host> -port <port> -password <password> -username <username> -configDir <configDir>
```

Example
```
java -jar build/libs/tak-db-profiler-exporter-*.jar -host localhost -port 5432 -password pass4marti -username martiuser -configDir /opt/tak/db-utils/db-profile
```
```
java -jar build/libs/tak-db-profiler-importer-*.jar -host localhost -port 5432 -password pass4marti -username martiuser -configDir /opt/tak/db-utils/db-profile
```