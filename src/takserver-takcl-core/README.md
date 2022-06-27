# TAKCL User Guide

See the [Development](docs/Development.md) documentation for architecture details.

## Usage Guide

### Test Execution

The tests can be executed on the CI infrastructure. In order to do so, navigate to the [TAKServer Pipeline](https://git.takmaps.com/core/takserver/-/pipelines) view. Find your commit, and if it has not completed building (Indicated by a green check for the first stage), continue waiting. If it failed to build, attempt to build it locally as there is likely a build failure.

For the second stage, you will see an indicator of test status. Generally, unless it is a scheduled build or a build on the maintenance branch, this will be a gear. Click it, and you will see a list of applicable tests.

The primary integration test suite run against master nightly and maintenance branches automatically is made up of itest_cnv1, itest_cnv2, itest_usermgr, and itest_cnv2_mission. The manual tests are a mirror of these tests, but broken up for a narrower scope of execution, and a faster return of results.

#### Custom Test

The test **Manual Custom** will allow you to specify the test identifier you would like to execute. If you tap a test name instead of the play button from the second stage selector list mentioned above, it will take you to a section where you can specify the variable. For the **Manual Custom** test, you can set the **TEST_IDENTIFIER** variable to any of the following tests to execute them quickly and individually while debugging failures seen in the primary test suite:

	coreNetworkV1-FederationV1Tests
	coreNetworkV1-FederationV1Tests.advancedFederationTest
	coreNetworkV1-FederationV1Tests.basicFederationTest
	coreNetworkV1-FederationV1Tests.basicMultiInputFederationTest
	coreNetworkV1-FederationV1Tests.federateConnectionInitiatorWaitTest
	coreNetworkV1-FederationV2Tests
	coreNetworkV1-FederationV2Tests.advancedFederationTest
	coreNetworkV1-FederationV2Tests.basicFederationTest
	coreNetworkV1-FederationV2Tests.basicMultiInputFederationTest
	coreNetworkV1-FederationV2Tests.federateConnectionInitiatorWaitTest
	coreNetworkV1-GeneralTests
	coreNetworkV1-GeneralTests.LatestSAFileAuth
	coreNetworkV1-GeneralTests.LatestSAInputGroups
	coreNetworkV1-GeneralTests.anonWithGroupInputTest
	coreNetworkV1-GeneralTests.groupToNonGroup
	coreNetworkV1-GeneralTests.latestSAAnon
	coreNetworkV1-GeneralTests.latestSADisconnectTest
	coreNetworkV1-GeneralTests.mcastSendTest
	coreNetworkV1-GeneralTests.mcastTest
	coreNetworkV1-GeneralTests.sslTest
	coreNetworkV1-GeneralTests.streamTcpTest
	coreNetworkV1-GeneralTests.tcpTest
	coreNetworkV1-GeneralTests.udpTest
	coreNetworkV1-InputTests
	coreNetworkV1-InputTests.inputRemoveAddTest
	coreNetworkV1-PointToPointTests
	coreNetworkV1-PointToPointTests.basicPointToPointTest
	coreNetworkV1-PointToPointTests.callsignIdentificationTest
	coreNetworkV1-PointToPointTests.mixedIdentificationTest
	coreNetworkV1-PointToPointTests.uidIdentificationTest
	coreNetworkV1-SubscriptionTests
	coreNetworkV1-SubscriptionTests.clientSubscriptions
	coreNetworkV1-UserManagementTests
	coreNetworkV1-UserManagementTests.UserManagerTest
	coreNetworkV1-WebsocketsTests
	coreNetworkV1-WebsocketsTests.advancedWebsocketsFederationV1Test
	coreNetworkV1-WebsocketsTests.advancedWebsocketsFederationV2Test
	coreNetworkV1-WebsocketsTests.basicSecureWebsocketTest
	coreNetworkV1-WebsocketsTests.simpleWSS
	coreNetworkV2-FederationV1Tests
	coreNetworkV2-FederationV1Tests.advancedFederationTest
	coreNetworkV2-FederationV1Tests.basicFederationTest
	coreNetworkV2-FederationV1Tests.basicMultiInputFederationTest
	coreNetworkV2-FederationV1Tests.federateConnectionInitiatorWaitTest
	coreNetworkV2-FederationV2Tests
	coreNetworkV2-FederationV2Tests.advancedFederationTest
	coreNetworkV2-FederationV2Tests.basicFederationTest
	coreNetworkV2-FederationV2Tests.basicMultiInputFederationTest
	coreNetworkV2-FederationV2Tests.federateConnectionInitiatorWaitTest
	coreNetworkV2-GeneralTests
	coreNetworkV2-GeneralTests.LatestSAFileAuth
	coreNetworkV2-GeneralTests.LatestSAInputGroups
	coreNetworkV2-GeneralTests.anonWithGroupInputTest
	coreNetworkV2-GeneralTests.groupToNonGroup
	coreNetworkV2-GeneralTests.latestSAAnon
	coreNetworkV2-GeneralTests.latestSADisconnectTest
	coreNetworkV2-GeneralTests.mcastSendTest
	coreNetworkV2-GeneralTests.mcastTest
	coreNetworkV2-GeneralTests.sslTest
	coreNetworkV2-GeneralTests.streamTcpTest
	coreNetworkV2-GeneralTests.tcpTest
	coreNetworkV2-GeneralTests.udpTest
	coreNetworkV2-InputTests
	coreNetworkV2-InputTests.inputRemoveAddTest
	coreNetworkV2-PointToPointTests
	coreNetworkV2-PointToPointTests.basicPointToPointTest
	coreNetworkV2-PointToPointTests.callsignIdentificationTest
	coreNetworkV2-PointToPointTests.mixedIdentificationTest
	coreNetworkV2-PointToPointTests.uidIdentificationTest
	coreNetworkV2-SubscriptionTests
	coreNetworkV2-SubscriptionTests.clientSubscriptions
	coreNetworkV2-UserManagementTests
	coreNetworkV2-UserManagementTests.UserManagerTest
	coreNetworkV2-WebsocketsTests
	coreNetworkV2-WebsocketsTests.advancedWebsocketsFederationV1Test
	coreNetworkV2-WebsocketsTests.advancedWebsocketsFederationV2Test
	coreNetworkV2-WebsocketsTests.basicSecureWebsocketTest
	coreNetworkV2-WebsocketsTests.simpleWSS


### Test Execution (Deprecated):

1. Ensure "/opt/tak/" is writable by the current user and empty
2. Build the project from the src root:
   `./gradlew clean buildRpm`
3. Use the starter script to deploy the server and associated docker postgresql databases (Examine it to understand what it is doing):
   `./takserver-takcl-core/scripts/starter.sh cd` ("cd" is the most common argument pairing I use which  to sets up a Cot database in docker  and deploys the server).
4. Navigate to /opt/tak/utils, and execute the following command to view tests:
   `./test.sh list`
5. Navigate to /opt/tak/utils, and execute the following command to execute a test:
   `./test.sh run <testName>`

See /opt/tak/TEST_RESULTS/TEST_ARTIFACTS for the results.

NOTE: The test will fail if TEST_TMP already exists, other conflicting takserver.war processes are running, or the 
wrong java version is used!

### Debug Artifacts

There are a few debug artifacts generated in `/opt/tak/TEST_RESULTS/` that may be useful:
`TEST_CERTS` - The certificates generated for use by the environment.  
`TEST_FARM` - The set of servers used for the most recently executed test.  
`TEST_ARTIFACTS` - This contains the initial CoreConfig, stderr, and stdout of each server used for each test. It also contains a JSON audit of the test activity that can be used for validating significant TAKCL upgrades have not broken expected validation behavior. If the build isn't cleaned between tests you may find multiple instances of the executions in this directory.

## Ports

The following port ranges are used for testing:

| Port Range    | Description                                   |
|---------------|-----------------------------------------------|
| 17600-17650   | Ignite Ports                                  |
| 17651-17000   | Misc Server Ports                             |
| 17700-17999   | Deployment specific server inputs and outputs |
