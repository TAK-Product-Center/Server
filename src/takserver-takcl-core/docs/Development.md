# TAKCL Developer Manual

The purpose of TAKCL, or the TAK Command Line, is two fold:

1.  Provide a means for orchestrating integration testing of TAKServer through simulated clients.
2.  Provide easy command line access to the methods and libraries created for orchestrating said tests.



## Architecture

### Overview, Packages, and Source Sets

Takcl is broken down into six source sets within takserver-takcl-core/src:
  * core - Core low-dependency files used throughout the framework
  * cursedtak - An alpha curses-like client to be used to simulate a server client
  * exe - Files necessary to create the executable takcl.jar
  * main - All other shared files
  * rpm - Customizations needed for the deployment with the takserver RPM
  * test - test files

The tests themselves are stored under the corresponding project:
  * takserver-core/src/integrationTest

The UserManager also uses TAKCL behind the scenes, and is located in takserver-usermanager


The architecture can be broken down into two major components: TACKL and the Test Framework: 

TAKCL contains the following major components:  
 * AppModules - These expose core server interaction functionality utilized to orchestrate tests in a way that also allows
   utilization of methods from the command line and are contained in the package com.bbn.marti.takcl.AppModules
   - The abstract classes for these are located in the core source set in the package com.bbn.marti.takcl.AppModules
   - The implementation of takcl.jar is located in the exe source set
   - The rest of the implementation is located in the main source set
 * CLI - This allows AppModules to be utilized from the command line with little effort.
   - The classes to utilize this are located in the com.bbn.marti.takcl.cli package in the core source set
   - They are used by the UserManager and the exe source set
 * Client - This wraps functionality for simulating a client that can be controlled and monitored by the framework.
   - These are located in the com.bbn.marti.takcl.connectivity package in the main source set
   - This package also contains the portion used to bring up and tear down server instances
 
The Test Framework contains the following components:  
 * Data - A set of structures used to represent objects and relations to TAKServer
   - They are located in the com.bbn.marti.test.shared.data package in the core source set
   - Although restrictive, they allow fast development of tests by using predefined servers, users, and connections
 * Engines - A set of Execution, Validation, And State Management abstractions to simplify interactions with the server.
   - They exist in the com.bbn.marti.test.shared.engines package of the main source set
 * Tests - The actual test files. Some of these are placed within the integrationTest source directories of TAKServer 
 components.
   - These are located in the integrationTest source set in takserver-core. Some tests exist in the main project, but they are largely old, incomplete, or redundant and not exeucted.
  
 
 ### CLI
 
 The CLI library provides two abstractions for CLI functionality: Simple and Advanced.
 
 #### Simple
 
 The simple CLI abstraction allows you to annotate methods with a Command annotation to turn them into executable 
 commands. It supports methods with parameters of the types **Enum**, **String**, **Boolean**, **Integer**, or in the 
 case of the last parameter, a variable length array of any of the previously mentioned types. The return value must be 
 **String** (which is the output of the command after successful execution) or void.
 
 ##### Class Requirements
 
 An AppModule using the simple CLI must implement one of the following classes:
 
 * [BaseAppModuleInterface.java](../src/core/java/com/bbn/marti/takcl/AppModules/generic/BaseAppModuleInterface.java)
 * [AppModuleInterface.java](../src/core/java/com/bbn/marti/takcl/AppModules/generic/AppModuleInterface.java)
 * [ServerAppModuleInterface.java](../src/core/java/com/bbn/marti/takcl/AppModules/generic/ServerAppModuleInterface.java)
 

 
 An example method signature is as follows:
 
 ```java
 @Command(description = "This connects a bunch of users to a server!", isDev = true)
public String connectUsersToServer(ServerProfileEnum server, String severUrl, Integer port, UserProfileEnum... users);
```

The _isDev_ flag is used to indicate a command is for developers only. This allows you to flag commands for internal 
use only so you can use the same AppModule even if some commands are still in development or not applicable to 
all end users.

#### Advanced

The advancec CLI abstraction allows significantly more complex command line annotations. This being the case, it requires quite a bit more work to implement properly. It is currently used by the UserManager.

#### Exceptions
 
 Exceptions inevitably happen when the user is providing input. Predefined exception types are defined in the core sorce set in the com.bbn.marti.takcl.cli package to enforce a clean end-user experience but allow the exceptions to be caught to aid debugging.

### Test Structure

Each piece of higher level functionality is captured in the [EngineInterface.java](../src/main/java/com/bbn/marti/test/shared/engines/EngineInterface.java). This is then implemented in [TestEngine.java](../src/main/java/com/bbn/marti/test/shared/engines/TestEngine.java), which serves one simple purpose: To execute the following classes, in order, which also implement the EngineInterface:
 - [ActionEngine](../src/main/java/com/bbn/marti/test/shared/engines/ActionEngine.java)
   * Executes the action against the server
 - [VerificationEngine](../src/main/java/com/bbn/marti/test/shared/engines/verification/VerificationEngine.java)
   * Verifies the expected behavior has impacted the rest of the system as expected
 - [StateEngine](../src/main/java/com/bbn/marti/test/shared/engines/state/StateEngine.java)
   * Updates the current state for the next TestEngine call

To keep things organized modification of engine data and operations relating to each phase should be kept within the engine at all costs. It has proven to be difficult to manage the integrity of the environment when elements creep outside of their designated area.

The tests themselves start, stop and execute the features within the test engine, isolated from the processes within the engine.

### Test Execution

The tests are executed through the AppModule [TestRunnerModule.java](../src/exe/java/com/bbn/marti/takcl/AppModules/TestRunnerModule.java). When a new test class is added, they should be added to this file. This provides a means for executing logging, and formatting the results for consumption by the CI.. This is called by the .gitlab-ci file in the root of the project. The test ordering in that file has been optimized to prevent conflicts in terms of multicast, CPU usage, and straggling resources.

### Test Development

The first thing that should be done when developing a test is to encapsulate individual pieces of functionality that should be tested, which each one added to [EngineInterface.java](../src/main/java/com/bbn/marti/test/shared/engines/EngineInterface.java). At this point, the corresponding ActionEngine method should be implemented and debugged, followed by the VerificationEngine, and then the StateEngine. 

At this point, a test can be made based on the functionality and combined with other functionality. Once a test class has been created, it should be added to the TestRunnerModule, and then added to the gitlab-ci.yaml file.

## Test Tags

Test tagging has been added to encapsulate higher level environment conditions and configurations that should be 
routinely tested on the CI. By default, the existing environment and default values will be used. However, if specific 
environment characteristics are specified via a Java argument or environment variable, they will be validated to ensure 
test integrity. The secondary and tertiary server are only used during federation tests. Currently, the following 
environment tags are supported:

| Environment Variable  | Java Arg                          | Description                   | Example Values            |
|-----------------------|-----------------------------------|-------------------------------|---------------------------|
| TAKCL_ENABLE_DB       | com.bbn.marti.takcl.dbEnabled     | CoreConfig.repository.enable  | true, false               |
| TAKCL_SERVER0_DB_HOST | com.bbn.marti.takcl.server0DbHost | Primary Server DB             | 10.221.8.123, dnsHostname |
| TAKCL_SERVER1_DB_HOST | com.bbn.marti.takcl.server1DbHost | Secondary Server DB           | 10.221.8.123, dnsHostname |
| TAKCL_SERVER2_DB_HOST | com.bbn.marti.takcl.server2DbHost | Tertiary Server DB            | 10.221.8.123, dnsHostname |
| TAKCL_JAVA_VERSION    | com.bbn.marti.takcl.javaVersion   | Java version                  | 11.0.12, 13.0.1, 8.0.302  |


## Notes

 - The UserManager test is currently executed in a legacy means through gradle. A prototype jar-based test that is executed on a deployed server using takcl.jar is partially developed but not fully debugged and ready for usage at this time.
 - The tests take quite a while since they provide enough time for not just expected activity to occur, but unexpected activity. The timings currently treat all elements the same. Ideally, a lot of work can be done here if knowledge of what has to occur (such as it being an initial connection or using a specific protocol) is considered, but that hasn't been prioritized.
 - Test stability is extremely tricky. Most of the conflicts due to simultaneous tests have been resolved, but an occasional LatestSaTest failure persists and hasn't been looked into yet.
