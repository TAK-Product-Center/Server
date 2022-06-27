The core fig functionality, without any dependency on roger. Intended to be dropped into TAK Server.

Build uber jar:

../gradlew clean shadowJar

To produce a fig-core.jar for TAK Server which is compatible with the TAK Server V1 federation, the protobuf generated code needs a patch. This is to work around a compatibility issue between protobuf 3 and 2. If a protobuf 3 message is sent to a protobuf 2 peer that contains a value of 0 for a required field in the protobuf 2 schema, parsing will fail on the protobuf 2 peer, which thinks that the required field is missing.

Steps:

1. ../gradlew clean test publish shadowJar
2. patch -p0 < proto2compatFix.patch
3. Hack build.gradle to disable protobuf code generation
4. ../gradlew shadowJar (again)
