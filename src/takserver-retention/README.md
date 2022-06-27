# TAK Server Retention Tool

Add a desription here

### Build and Run 
Steps:
1. build takserver
2. build takserver-retention

```
cd takserver-retention/
../gradlew bootJar
```

3. Start takserver first
4. start the retention application
```
cd takserver/src/takserver-data-retention
java -jar build/libs/takserver-retention*.jar
```
Expected output:

```
 [main] t.s.retention.RetentionApplication - Started RetentionApplication in 5.814 seconds (JVM running for 7.048)
 [main] t.s.retention.RetentionApplication -  Retention Application started  
 [main] t.s.retention.RetentionApplication -  Data Reaper Service is scheduled at 0/10 * * * * *
 [scheduling-1] t.s.r.service.RetentionReaperService -  running based on this schedule 0/10 * * * * *
```
