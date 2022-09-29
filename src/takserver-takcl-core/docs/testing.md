
# CI Testing

## Getting Started

The easiest way to get started interacting with the integration tests is to navigate to your branch dashboard in gitlab 
([master](https://git.tak.gov/core/takserver/-/tree/master) for example). You will see a row above the file listing 
that contains some information on the last commit. To the left of the hash in this row, you will see an icon that 
displays the current status for this branch. Clicking it will take you to the pipeline page for your branch.

On the pipeline page, the two most useful tabs are the **Pipeline** tab and the **Tests** tab. The **Pipeline** tab 
currently contains the following stages:

 * **Build** - Builds Takserver. This must pass for tests to be executable and is executed on all commits
 * **Sign** - Signs the build artifacts for public use. This includes rpms, docker tarballs, and maven artifacts
 * **Scan** - This executes a fortify scan
 * **Publish dev** - Publishes a set of artifacts to the [tak.gov TAKServer dev repo](https://artifacts.tak.gov/ui/repos/tree/General/TAKServer/dev). This is useful for sharing one-off releases with non-developers
 * **Publish beta** - Publishes a set of artifacts to the [tak.gov TAKServer  beta repo](https://artifacts.tak.gov/ui/repos/tree/General/TAKServer/beta). DO NOT DO THIS UNLESS YOU HAVE BEEN TOLD TO DO SO BY SOMEONE COORDINATING RELEASES!
 * **Publish release** - Publishes a set of artifacts to the [tak.gov TAKServer release repo](https://artifacts.tak.gov/ui/repos/tree/General/TAKServer/release). DO NOT DO THIS UNLESS YOU HAVE BEEN TOLD TO DO SO BY SOMEONE COORDINATING RELEASES!
 * **Integration test** - The full integration test suite. This takes ~4 hours and cannot be run in parallel with other 
   full integration test executions

The current status displayed on the branch page and the status for each job in a pipeline column will have one of the
following values with the corresponding necessary user action:

| Value     	| Icon			| User Action																|  
|---------------|---------------|---------------------------------------------------------------------------|
| passed		| green check	| None. The default or manual action has passed								|  
| blocked		| black gear	| None. The test has not run (it is blocked by a lack of manual execution)	|  
| passed		| yellow "!"	| Manual Failure Investigation												|  
| failed		| red x			| Manual Failure Investigation												|  

## CI Test Execution

### Full suite execution (~4 hours) instructions:
1.  Push the branch to be tested to origin.  
2.  Navigate to your branch.
3.  Navigate to the Pipeline display for that branch.
4.  In the **Pipeline** tab, press the **play** button next to the **Integration Test** column heading.

### Single Test Execution

1.  Push the branch to be tested to origin.
2.  Navigate to your branch.
3.  Navigate to the Pipeline display for that branch.
4.  In the **Pipeline** tab, select the desired test below the **Integration Test** column heading.  

## Local Test Execution

In order to execute them locally, a script has been written that utilizes _openjdk_ and _postgis_ docker containers to 
relatively easily execute the tests on Linux and OS X. You would use it as follows:  

1.  Within the takserver `src` root, execute a build of the docker containers:  
    `./gradlew clean buildRpm buildDocker`  
2.  Execute the testrunner with the list argument to see the list of available tests:  
    `bash takserver-takcl-core/scripts/testrunner.sh list`  
3.  Execute one of the tests:  
    `bash takserver-takcl-core/scripts/testrunner.sh run <testname>`  
4.  Wait for the test to finish (This may take up to an hour depending on the test)  


## Test Failure Investigation Guide

There are generally three types of validations being done: transmission flow validation, API validation, and user 
authentication file validation. Transmission flow validation is done for all tests except the mission tests which use 
API validation, and the user manager tests (not to be confused with the user management tests), which use user 
authentication file validation.


### UserManagerTest Failure Investigation Guide

As of writing, this only applies to the UserManagerTests (not to be confused with the UserManagementTests). Proper 
failure investigation walkthrough is pending.  

### General Transmission Flow Validation Guide

1.  After navigating to the pipeline page for the failed commit, navigate to the Tests tab, select a job with failures 
    or errors, and make note of the parent job and the failed test name you would like to investigate.
    
2.  Click the status or name of the failed parent job (NOT THE ICON TO THE RIGHT WHICH WILL LIKELY BE A RETRY ICON)
4.  To the right, you will see a "Job artifacts" section. Tap the "Browse" button, and open the TEST_ARTIFACTS folder.
    Within this, each test has three artifacts listed: The JUnit XML output, the output from within the JUnit XML 
    output (the lack of newlines in the xml for this section makes it unreadable in the browser), and a folder. The 
    folder contains server artifacts related to the test.
5.  Open the xml and txt test execution file and determine where things went wrong (I have it output a '_#!#!@_' at every 
    failure to make it easy to search for them). As an example, here's an instance of a failure:

```
2021-05-27-05:20:22.347 [Time-limited test] INFO  c.b.m.t.c.i.ConnectibleClient - [s0_authstcp_authuser0_0f] - -- NETWORK CONNECT   Begin --
2021-05-27-05:20:22.348 [Time-limited test] INFO  c.b.m.t.c.i.ConnectibleClient - [s0_authstcp_authuser0_0f] - -- NETWORK AUTH   Begin --
2021-05-27-05:20:22.348 [Time-limited test] INFO  c.b.m.t.c.i.ConnectibleClient - [s0_authstcp_authuser0_0f] - -- NETWORK AUTH   Completed -- in 0ms.
2021-05-27-05:20:22.348 [Time-limited test] INFO  c.b.m.t.c.i.ConnectibleClient - [s0_authstcp_authuser0_0f] - -- NETWORK CONNECT   Completed -- in 1ms.
Connected and attempted authentication: s0_authstcp_authuser0_0f
2021-05-27-05:20:22.463 [Thread-23] INFO  c.b.m.t.c.i.ConnectibleClient - [s0_authstcp_authuser0_0f] - -- NETWORK RECEIVE STCP MESSAGE Begin --
2021-05-27-05:20:22.464 [Thread-23] INFO  c.b.m.t.c.i.ConnectibleClient - [s0_authstcp_authuser0_0f] - -- NETWORK RECEIVE STCP MESSAGE Completed -- in 1ms.
Authenticated: s0_authstcp_authuser0_0f
2021-05-27-05:20:25.549 [Time-limited test] INFO  c.b.m.t.s.e.v.UserExpectationValidator - #!#!@ s0_authstcp_authuser012_012f >-0!=1-> s0_authstcp_authuser0_0f - Justification: latestSA
2021-05-27-05:20:25.549 [Time-limited test] INFO  c.b.m.t.s.e.v.UserExpectationValidator - #!#!@ s0_authstcp_authuser12_012f >-0!=1-> s0_authstcp_authuser0_0f - Justification: latestSA
```

The key thing here is you'll see _s0_authstcp_authuser0_0f_ signed in and didn't receive the expected LatestSA messasges 
from _s0_authstcp_authuser012_012f_ or _s0_authstcp_authuser0_0f_, and the usere's connection activity began at 
`2021-05-27-05:20:22.347`. with this knowledge, navigate back to the TEST_ARTIFACTS folder and open the folder 
corresponding to the server artifacts from the test. Looking in the "SERVER_0_logs-takserver-api.log" file in that 
folder, I am not seeing anything shortly after the timestamp, so I check the "SERVER_0_logs-takserver-messaging.log" 
file next. I find the following two lines which seem to indicate a gap of activity and an event that correlates to the 
client connection:
```
2021-05-27-05:20:21.018 [Service:Repository] c.b.marti.service.RepositoryService - unable to obtain database connection
2021-05-27-05:20:22.350 [epollEventLoopGroup-7-4] c.b.m.n.c.b.AbstractBroadcastingChannelHandler - set input s0_authstcp for channel handler com.bbn.marti.nio.netty.handlers.NioNettyHandlerBase$2 Netty Dummy TCP Channel server on local port 17745 client: /127.0.0.1:34108 -851988906
```

From this point forward I can investigate further. If nothing seems to have gone wrong here, you may need to repeat the 
process, but from the points in the test execution where s0_authstcp_authuser012_012f_ and _s0_authstcp_authuser0_0f_ 
connected, sent, had their groups modified, or anything else that could ave resulted in the error.


### Mission Test Failure Investigation Guide

Unlike the General tests, the mission tests each walk through a sequence of events.  This can make it complicated, as 
if an API does not return the proper value, the error may due to the insertion of a value in earlier steps. The Mission 
tests or ordered. Within them, you will see a "aaa_setupEnvironment" at the beginning to set up the general scenario 
and a "zzz_teardown" at the end which brings everything down. Between that, you will see each class name begins with 
a letter indicating the order of execution, perhaps prefixed by a sub-identifier. For example, 
_missions.MissionFileSync_ has methods prefixed by "_a" to "_p". However, _missions.MissionUserCustomRolesTests_ are
prefixed by "admin", "owner", "readonlySubscriber", or "subscriber" followed by alphabetical ordering guides.

The debugging generally follows the same steps as the **General Transmission Flow Validation Guide**, but you'll have 
to start at the point of API failure, observe the API return value in the test runner log, compare it against what is 
expected (possibly by comparing to a recent passing sample of the test). If you don't see the cause in the read API,
you'll want to go back to the previous lettered steps to see if the failure was with insertion, not retrieval of data. 

## Tips
 - The test limit is currently set to 4. This is to prevent tests from causing resource starvation, causing expected timing failures.
 - If the first bubble in the _Stages_ column is a red X, it couldn't build. Ensure your branch can build locally.


## Historic Test Execution

In order to view historic test executions, you must navigate to the project-wide [Pipeline](https://git.takmaps.com/core/takserver/-/pipelines) 
dashboard. This displays the pipeline executed for every single commit to the server. Clicking the button to the left 
of each commit will take you to the commit's overall pipeline and test status page.

## Current Test List 

Note: Tests prefixed by an asterisk must be run with a database to pass and the asterisk is not part of the test name

Updated 08/09/2022

```
FederationV1Tests
FederationV1Tests.advancedFederationTest
FederationV1Tests.basicFederationTest
FederationV1Tests.basicMultiInputFederationTest
FederationV1Tests.federateConnectionInitiatorWaitTest
FederationV2Tests
FederationV2Tests.advancedFederationTest
FederationV2Tests.basicFederationTest
FederationV2Tests.basicMultiInputFederationTest
FederationV2Tests.federateConnectionInitiatorWaitTest
GeneralTests
GeneralTests.LatestSAFileAuth
GeneralTests.LatestSAInputGroups
GeneralTests.anonWithGroupInputTest
GeneralTests.groupToNonGroup
GeneralTests.latestSAAnon
GeneralTests.latestSADisconnectTest
GeneralTests.mcastSendTest
GeneralTests.mcastTest
GeneralTests.sslTest
GeneralTests.streamTcpTest
GeneralTests.tcpTest
GeneralTests.udpTest
InputTests
InputTests.inputRemoveAddTest
PointToPointTests
PointToPointTests.basicPointToPointTest
PointToPointTests.callsignIdentificationTest
PointToPointTests.mixedIdentificationTest
PointToPointTests.uidIdentificationTest
StartupTests
StartupTests.jarStartupValiationTest
StreamingDataFeedsTests
StreamingDataFeedsTests.dataFeedRemoveAddTest
SubscriptionTests
SubscriptionTests.clientSubscriptions
UserManagementTests
UserManagementTests.UserManagerTest
\*WebsocketsFederationTests
\*WebsocketsFederationTests.advancedWebsocketsFederationV1Test
\*WebsocketsFederationTests.advancedWebsocketsFederationV2Test
\*WebsocketsTests
\*WebsocketsTests.basicSecureWebsocketTest
\*WebsocketsTests.simpleWSS
\*missions.EnterpriseFileSync
\*missions.MissionAddRetrieveRemove
\*missions.MissionDataFlowTests
\*missions.MissionFileSync
\*missions.MissionUserCustomRolesTests
\*missions.MissionUserDefaultRolesTests
```

