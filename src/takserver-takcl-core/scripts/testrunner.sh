#!/usr/bin/env bash

set -e

TAKCL_SERVER_LOG_LEVEL_OVERRIDES="com.bbn=TRACE tak=TRACE ${TAKCL_SERVER_LOG_LEVEL_OVERRIDES}"
DB0_EXTERNAL_PORT=32445
DB1_EXTERNAL_PORT=32446
DB2_EXTERNAL_PORT=32447
DOCKER_IMAGE='eclipse-temurin:17-jammy'

ping() {
	set -e
	nc ${1} ${2} -w 1 </dev/null >/dev/null 2>&1 && rval=$? || rval=$? && true
	return $rval
}

kill_db() {
	if [[ "`docker ps | grep ${SERVER0_DOCKER_DB_IDENTIFIER}`" != "" ]];then
		docker stop ${SERVER0_DOCKER_DB_IDENTIFIER}
	fi
	if [[ "`docker ps | grep ${SERVER1_DOCKER_DB_IDENTIFIER}`" != "" ]];then
		docker stop ${SERVER1_DOCKER_DB_IDENTIFIER}
	fi
	if [[ "`docker ps | grep ${SERVER2_DOCKER_DB_IDENTIFIER}`" != "" ]];then
		docker stop ${SERVER2_DOCKER_DB_IDENTIFIER}
	fi

}

takserver_running() {
	set -e
	ps aux | grep "^.*java.*takserver\.war.*$" > /dev/null 2>&1 
	return $?
}

postgresql_running() {
	return $(ping 127.0.0.1 5432)
}

if [[ ! -d "takserver-takcl-core" ]];then
	echo Please start the script from the src directory like './takserver-takcl-core/scripts/testrunner.sh'!
	exit 1
fi

LOG_TARGET=$(pwd)/TESTRUNNER_RESULTS
SERVER0_DOCKER_DB_IDENTIFIER=TakserverTestDB0
SERVER1_DOCKER_DB_IDENTIFIER=TakserverTestDB1
SERVER2_DOCKER_DB_IDENTIFIER=TakserverTestDB2
USE_DB=false
POSTGRES_USER=martiuser
POSTGRES_DB=cot
POSTGRES_PORT=5432

ARTIFACT_TEMPLATE_DIR="$(pwd)/takserver-package/build/takArtifacts"
ARTIFACT_SRC="$(pwd)/takserver-package/build/testrunnerArtifacts"

if [[ ! -f "${ARTIFACT_TEMPLATE_DIR}/takserver.war" ]] || [[ ! -d "takserver-package/federation-hub/build/artifacts/jars" ]] || [[ ! -d "takserver-package/federation-hub/build/artifacts/configs" ]];then
	echo Please build the project with './gradlew clean buildDocker buildRpm' to populate the tak root source! Future executions will automatically update takserver.war but require setting variables at the top of this script to true for everything else!
	exit 1
fi

if [[ -d "${ARTIFACT_SRC}" ]];then
  rm -r "${ARTIFACT_SRC}"
fi

mkdir "${ARTIFACT_SRC}"

if [[ ! -d "${ARTIFACT_SRC}/federation-hub" ]];then
  mkdir "${ARTIFACT_SRC}/federation-hub"
fi

if [[ ! -d "${ARTIFACT_SRC}/lib" ]];then
  mkdir "${ARTIFACT_SRC}/lib"
fi

cp -r ${ARTIFACT_TEMPLATE_DIR}/* ${ARTIFACT_SRC}/
cp takserver-package/federation-hub/build/artifacts/jars/* "${ARTIFACT_SRC}/federation-hub/"
cp -r takserver-package/federation-hub/build/artifacts/configs "${ARTIFACT_SRC}/federation-hub/configs"
cp federation-hub-broker/src/main/resources/federation-hub-broker.yml "${ARTIFACT_SRC}/federation-hub/configs/"
cp takserver-takcl-core/plugin-test-libs/* "${ARTIFACT_SRC}/lib/"




if [[ "${1}" == "run" ]];then
	if [[ "${2}" == "" ]];then
		echo Please provide a test name to execute!
		exit 1
	elif [[ "${2}" == *"ebsocket"* ]] || [[ "${2}" == *"ission"* ]] || [[ "${2}" == *"StartupTests"* ]];then

		# Kill existing database instance before checking if one is already running
		kill_db

		if [[ "${TAKCL_SERVER_POSTGRES_PASSWORD}" == "" ]];then
			echo The environment variable TAKCL_SERVER_POSTGRES_PASSWORD must be set to a password for the database!
			exit 1
		fi
		USE_DB=true
	fi

	if [[ -d "${LOG_TARGET}" ]] && [[ "${3}" != "--unsafe-mode" ]];then
		echo Please remove the existing "${LOG_TARGET}" directory to continue!
		exit 1
	fi

	if postgresql_running;then
		echo A local postgresql instance is already running! Please shut it down before proceeding!
		exit 1
	elif takserver_running;then
		echo A local takserver instance is already running! Please shut it down before proceeding!
		exit 1
	fi

	mkdir -p ${LOG_TARGET}

elif [[ "${1}" != "list" ]];then
	echo -c Please provide the parameter \'list\' to view the tests or \'run\' to run a test!
	exit 1
fi

# Start a java docker container with a main task of just sleeping
if [[ "${1}" == "list" ]];then
	docker run -it --rm --name temptaktest \
		--volume ${ARTIFACT_SRC}:/opt/takbase \
		"${DOCKER_IMAGE}" \
		bash -c "mkdir -p /opt/tak;cp -R /opt/takbase/* /opt/tak/; java -jar /opt/tak/utils/takcl.jar tests ${1} ${2}"

else
	JARGS=' -Djava.net.preferIPv4Stack=true'

	CMD="
	mkdir -p /opt/tak;
	cp -R /opt/takbase/* /opt/tak/;
	cp /opt/tak/CoreConfig.example.xml /opt/tak/CoreConfig.xml;
	cp /opt/tak/TAKIgniteConfig.example.xml /opt/tak/TAKIgniteConfig.xml; "

	if [[ "${USE_DB}" == "true" ]];then
		# Kill existing database instance
		kill_db

		# Create a new one, get the IP, and set it as a test parameter
		# The postgis/postgis docker repo on dockerhub is a community version unlike the ones used in our containers.
		# Update to point to a new version if using a new version for all the Docker containers.
		echo Setting up CoT Databases...
		if [[ "`docker ps | grep ${SERVER0_DOCKER_DB_IDENTIFIER}`" == "" ]];then
			docker run -it -d --rm --name ${SERVER0_DOCKER_DB_IDENTIFIER} \
				--env POSTGRES_PASSWORD=${TAKCL_SERVER_POSTGRES_PASSWORD} \
				--env POSTGRES_HOST_AUTH_METHOD=trust \
				--env POSTGRES_USER=${POSTGRES_USER} \
				--env POSTGRES_DB=${POSTGRES_DB} \
				-p ${DB0_EXTERNAL_PORT}:5432 \
				postgis/postgis:15-3.3
		fi

		if [[ "`docker ps | grep ${SERVER1_DOCKER_DB_IDENTIFIER}`" == "" ]];then
			docker run -it -d --rm --name ${SERVER1_DOCKER_DB_IDENTIFIER} \
				--env POSTGRES_PASSWORD=${TAKCL_SERVER_POSTGRES_PASSWORD} \
				--env POSTGRES_HOST_AUTH_METHOD=trust \
				--env POSTGRES_USER=${POSTGRES_USER} \
				--env POSTGRES_DB=${POSTGRES_DB} \
				-p ${DB1_EXTERNAL_PORT}:5432 \
				postgis/postgis:15-3.3
		fi

		if [[ "`docker ps | grep ${SERVER2_DOCKER_DB_IDENTIFIER}`" == "" ]];then
			docker run -it -d --rm --name ${SERVER2_DOCKER_DB_IDENTIFIER} \
				--env POSTGRES_PASSWORD=${TAKCL_SERVER_POSTGRES_PASSWORD} \
				--env POSTGRES_HOST_AUTH_METHOD=trust \
				--env POSTGRES_USER=${POSTGRES_USER} \
				--env POSTGRES_DB=${POSTGRES_DB} \
				-p ${DB2_EXTERNAL_PORT}:5432 \
				postgis/postgis:15-3.3
		fi


		# Get instance details and set them as necessary
		DOCKER0_IP=`docker inspect --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' ${SERVER0_DOCKER_DB_IDENTIFIER}`
		DOCKER1_IP=`docker inspect --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' ${SERVER1_DOCKER_DB_IDENTIFIER}`
		DOCKER2_IP=`docker inspect --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' ${SERVER2_DOCKER_DB_IDENTIFIER}`
		JARGS="${JARGS} -Dcom.bbn.marti.takcl.server0DbHost=${DOCKER0_IP} -Dcom.bbn.marti.takcl.server1DbHost=${DOCKER1_IP} -Dcom.bbn.marti.takcl.server2DbHost=${DOCKER2_IP}"

#		CMD="${CMD}
#		 sed -i 's|<connection url=\"jdbc:postgresql://127.0.0.1:5432/cot\" username=\"martiuser\" password=\"\" />|<connection url=\"jdbc:postgresql://${DOCKER0_IP}:5432/cot\" username=\"${POSTGRES_USER}\" password=\"${TAKCL_SERVER_POSTGRES_PASSWORD}\"/>|g' /opt/tak/CoreConfig.xml;"

		echo Waiting for DB to settle...
		sleep 20
		echo Locally executing "java -jar ${ARTIFACT_SRC}/db-utils/SchemaManager.jar -url jdbc:postgresql://127.0.0.1:${DB0_EXTERNAL_PORT}/cot -user ${POSTGRES_USER} -password ${TAKCL_SERVER_POSTGRES_PASSWORD} upgrade"
		java -jar ${ARTIFACT_SRC}/db-utils/SchemaManager.jar -url jdbc:postgresql://127.0.0.1:${DB0_EXTERNAL_PORT}/cot -user ${POSTGRES_USER} -password ${TAKCL_SERVER_POSTGRES_PASSWORD} upgrade

		echo Locally executing "java -jar ${ARTIFACT_SRC}/db-utils/SchemaManager.jar -url jdbc:postgresql://127.0.0.1:${DB1_EXTERNAL_PORT}/cot -user ${POSTGRES_USER} -password ${TAKCL_SERVER_POSTGRES_PASSWORD} upgrade"
		java -jar ${ARTIFACT_SRC}/db-utils/SchemaManager.jar -url jdbc:postgresql://127.0.0.1:${DB1_EXTERNAL_PORT}/cot -user ${POSTGRES_USER} -password ${TAKCL_SERVER_POSTGRES_PASSWORD} upgrade

		echo Locally executing "java -jar ${ARTIFACT_SRC}/db-utils/SchemaManager.jar -url jdbc:postgresql://127.0.0.1:${DB2_EXTERNAL_PORT}/cot -user ${POSTGRES_USER} -password ${TAKCL_SERVER_POSTGRES_PASSWORD} upgrade"
		java -jar ${ARTIFACT_SRC}/db-utils/SchemaManager.jar -url jdbc:postgresql://127.0.0.1:${DB2_EXTERNAL_PORT}/cot -user ${POSTGRES_USER} -password ${TAKCL_SERVER_POSTGRES_PASSWORD} upgrade


	else
		JARGS="${JARGS} -Dcom.bbn.marti.takcl.dbEnabled=false"
		CMD="${CMD}	sed -i 's/<repository enable=\"true\"/<repository enable=\"false\"/g' /opt/tak/CoreConfig.xml;"
	fi

	CMD="${CMD}
	java ${JARGS} -jar /opt/tak/utils/takcl.jar tests ${1} ${2};
	chmod -R 777 /opt/tak/TEST_RESULTS/TEST_ARTIFACTS/*"

	docker run -it --rm --name temptaktest \
		--volume ${ARTIFACT_SRC}:/opt/takbase \
		--volume ${LOG_TARGET}:/opt/tak/TEST_RESULTS/TEST_ARTIFACTS \
		--env TAKCL_SERVER_LOG_LEVEL_OVERRIDES="${TAKCL_SERVER_LOG_LEVEL_OVERRIDES}" \
		--env TAKCL_SERVER_POSTGRES_PASSWORD=${TAKCL_SERVER_POSTGRES_PASSWORD} \
		"${DOCKER_IMAGE}" \
		bash -c "${CMD}"
	kill_db
fi
