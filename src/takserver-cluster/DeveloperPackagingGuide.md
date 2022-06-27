# Developer Guide for Packaging A Cluster Build

- Set the following environment variables to enable the generation of certificates of authority:
  - CA_NAME
  - STATE
  - CITY
  - ORGANIZATIONAL_UNIT

- Use gradle to clean and set up cluster directory
`cd takserver/src/ && ./gradlew clean buildcluster`

- Everything after this step has no further dependencies on the takserver source code. Navigate to takserver-cluster/build/distributions/ and move/extract the cluster zip to a safe location outside of the source code. This zip contains all the cluster artifacts and dependencies.

- Cluster build is maintained through takserver/src/takserver-cluster/build.gradle