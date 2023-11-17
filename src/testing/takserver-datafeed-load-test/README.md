## Build
From this directory (**src/testing/takserver-datafeed-load-test/**), type:
```
./gradlew clean shadowjar
```
## Add Plugin to TAK Server
1. Setup the TAK Server **CoreConfig.xml** file
2. Create a **lib** subdirectory under the execution directory (typically **src/takserver-core/example**)
2. Drop the .jar file from **src/testing/takserver-datafeed-load-test/build/libs** into the **lib** subdirectory of the execution directory 
3. Create a a **conf/plugins** subdirectory under the execution directory.
4. Copy the **src/testing/takserver-datafeed-load-test/tak.server.plugins.DataFeedMessageSenderPluginLoadTest.yaml** file from into a **conf/plugins** subdirectory under the execution directory.
5. Edit the copy of **tak.server.plugins.DataFeedMessageSenderPluginLoadTest.yaml** 
6. Start the Messaging, API and Plugin TAK Server processes (make sure the **lib* directory is in the classpath for the plugin process)
7. The Plugin TAK Server process will automatically start the plugin which will start sending data-feed messages based on the yaml configuration.
