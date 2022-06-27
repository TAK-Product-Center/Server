#!/usr/bin/env bash

BUILD=false
DEPLOY=false
START=false
MONOLITH=false
DELETE_LOGS=false
USE_COT_DB=false
TEST_MODE=false
KILL_EXISTING_TAKSERVER=false
OVERRIDE_TEST_SETTINGS=false

SERVER0_DOCKER_DB_IDENTIFIER=TakserverServer0DB
SERVER1_DOCKER_DB_IDENTIFIER=TakserverServer1DB
SERVER2_DOCKER_DB_IDENTIFIER=TakserverServer2DB
POSTGRES_USER=martiuser
POSTGRES_DB=cot
POSTGRES_PORT=5432

MESSAGING_PID=null
API_PID=null

DEFAULT_SERVER_SRC=takserver-package/build/takArtifacts
DEPLOYMENT_DIR=/opt/tak

SRC=`realpath "$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"/../../`

cd "${SRC}"

JARGS="-Dio.netty.tmpdir=/opt/tak/ 
	-Dlogging.level.com.bbn=DEBUG 
    -Dlogging.level.tak=DEBUG 
    -Djava.io.tmpdir=/opt/tak/ 
    -Dio.netty.native.workdir=/opt/tak/ 
    -Dloader.path=WEB-INF/lib-provided,WEB-INF/lib,WEB-INF/classes,file:lib/ 
    -Djava.net.preferIPv4Stack=true 
    -Djava.security.egd=file:/dev/./urandom 
    -DIGNITE_UPDATE_NOTIFIER=false 
    -Djdk.tls.client.protocols=TLSv1.2 
    -Dspring.profiles.active=messaging 
    -Xmx2000m 
    -XX:+HeapDumpOnOutOfMemoryError "


ARGS="--logging.level.com.bbn=DEBUG 
	--logging.level.tak=DEBUG "
#ARGS="--logging.level.com.bbn.marti.nio.websockets.NioWebSocketHandler=TRACE \
#	--logging.level.com.bbn.marti.nio.websockets.TakProtoWebSocketHandler=TRACE \
#	--logging.level.tak.server.config=TRACE \
#	--logging.level.tak.server.messaging=TRACE \
#	--logging.level.com.bbn.marti.nio.netty.handlers.NioNettyTlsServerHandler=TRACE \
#	--logging.level.com.bbn.marti.nio.codec.impls.AbstractAuthCodec=TRACE \
#	--logging.level.tak.server.websocket=TRACE \
#	--logging.level.com.bbn.marti.service.SubmissionService=TRACE"


ctrl_c() {
	if [ $MESSAGING_PID != null ];then
		kill $MESSAGING_PID
	fi

	if [ $API_PID != null ];then
		kill $API_PID
	fi
}

trap ctrl_c SIGINT

set -e

if [[ $1 == "" ]];then
	printf "This script is intended to simplify local server deployment.

Used Docker Container Names:
    TakserverServer0DB
    TakserverServer1DB
    TakserverServer2DB
	
Optional Environment Variables:
    TAKSERVER_USER_AUTHENTICATION_FILE	Overrides the User Authentication File contents

Usage:

This script takes a single parameter, which can use a mix of the following characters to perform 
the indicated startup tasks:

\t(b)uild\t\tPerforms \`./gradlew clean buildRpm buildDocker\`
\t(d)eploy\tDeploys takserver to /opt/tak
\tsetup (c)ot databases\t\t Sets up DBs in docker and sets up the test script to use them
\t(l)og delete\tDeletes the logs from the previous execution if they exist
\t(s)tart\t\tStarts the server
\t(k)ill\t\tKills the currently running takserver, if one is running
\t(t)est\t\tif provided with 'd', deploys the server as a test would


Test Investigation Usage:
There are a few parameters that can be used to investigate failed test environments:
\t0,1,2,3	Clones and starts the server with the specified identifier from the last executed test
\to      	Overrides the server and client certificates with your certificates
"
  exit 1
fi

if [[ $1 == *"d"* ]]; then
	DEPLOY=true
fi

if [[ $1 == *"s"* ]]; then
	START=true
fi

if [[ $1 == *"m"* ]];then
	MONOLITH=true
fi

if [[ $1 == *"l"* ]];then
	DELETE_LOGS=true
fi

if [[ $1 == *"c"* ]];then
	USE_COT_DB=true
fi

if [[ $1 == *"k"* ]];then
	KILL_EXISTING_TAKSERVER=true
fi

if [[ $1 == *"o"* ]];then
	OVERRIDE_TEST_SETTINGS=true
fi

if [[ $1 == *"t"* ]];then
	TEST_MODE=true
fi

if [[ $1 == *"0"* ]];then
	TEST_SERVER=0
elif [[ $1 == *"1"* ]];then
	TEST_SERVER=1
elif [[ $1 == *"2"* ]];then
	TEST_SERVER=2
fi

if [[ ${TEST_SERVER} == "" ]];then
	SERVER_SRC="${DEFAULT_SERVER_SRC}"
	
	cp takserver-takcl-core/scripts/TAKCLConfig-pointsToManualTestDir.xml TAKCLConfig.xml
	if [[ $1 == *"b"* ]]; then
		BUILD=true
	fi

else
  SERVER_SRC="${SRC}/takserver-takcl-core/TEST_TMP/TEST_FARM/SERVER_${TEST_SERVER}"
  cp takserver-takcl-core/scripts/TAKCLConfig-pointsToTestCerts.xml TAKCLConfig.xml
fi


if [[ $USE_COT_DB == true ]];then
  if [[ "`docker ps | grep ${SERVER0_DOCKER_DB_IDENTIFIER}`" != "" ]];then
    docker stop ${SERVER0_DOCKER_DB_IDENTIFIER}
  fi

  if [[ "`docker ps | grep ${SERVER1_DOCKER_DB_IDENTIFIER}`" != "" ]];then
    docker stop ${SERVER1_DOCKER_DB_IDENTIFIER}
  fi

  if [[ "`docker ps | grep ${SERVER2_DOCKER_DB_IDENTIFIER}`" != "" ]];then
    docker stop ${SERVER2_DOCKER_DB_IDENTIFIER}
  fi
fi

if [[ $KILL_EXISTING_TAKSERVER == true ]];then
	echo KILLING EXISTING SERVER...
	ps -x --format pid,args  | grep java.*\.-jar.*takserver.war | grep -v grep |  awk '{print $1}' | while read pid ; do
		kill -9 $pid
	done
	echo DONE
fi

if [ $BUILD == true ];then
	echo BUILDING...
	./gradlew clean buildRpm buildDocker
	echo DONE
fi

if [ $DEPLOY == true ];then
	echo DEPLOYING...
  if [[ $USE_COT_DB == true ]];then
    echo Setting up CoT Databases...
    if [[ "`docker ps | grep ${SERVER0_DOCKER_DB_IDENTIFIER}`" == "" ]];then
        docker run -it -d --rm --name ${SERVER0_DOCKER_DB_IDENTIFIER} \
        --env POSTGRES_PASSWORD=${TAKCL_SERVER_POSTGRES_PASSWORD} \
        --env POSTGRES_HOST_AUTH_METHOD=trust \
        --env POSTGRES_USER=${POSTGRES_USER} \
        --env POSTGRES_DB=${POSTGRES_DB} \
        -p ${POSTGRES_PORT} postgis/postgis:10-3.1
    fi

    DOCKER0_IP=`docker inspect --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' ${SERVER0_DOCKER_DB_IDENTIFIER}`

    if [[ "`docker ps | grep ${SERVER1_DOCKER_DB_IDENTIFIER}`" == "" ]];then
        docker run -it -d --rm --name ${SERVER1_DOCKER_DB_IDENTIFIER} \
        --env POSTGRES_PASSWORD=${TAKCL_SERVER_POSTGRES_PASSWORD} \
        --env POSTGRES_HOST_AUTH_METHOD=trust \
        --env POSTGRES_USER=${POSTGRES_USER} \
        --env POSTGRES_DB=${POSTGRES_DB} \
        -p ${POSTGRES_PORT} postgis/postgis:10-3.1
    fi

    DOCKER1_IP=`docker inspect --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' ${SERVER1_DOCKER_DB_IDENTIFIER}`

    if [[ "`docker ps | grep ${SERVER2_DOCKER_DB_IDENTIFIER}`" == "" ]];then
        docker run -it -d --rm --name ${SERVER2_DOCKER_DB_IDENTIFIER} \
        --env POSTGRES_PASSWORD=${TAKCL_SERVER_POSTGRES_PASSWORD} \
        --env POSTGRES_HOST_AUTH_METHOD=trust \
        --env POSTGRES_USER=${POSTGRES_USER} \
        --env POSTGRES_DB=${POSTGRES_DB} \
        -p ${POSTGRES_PORT} postgis/postgis:10-3.1
    fi

    DOCKER2_IP=`docker inspect --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' ${SERVER2_DOCKER_DB_IDENTIFIER}`

    DB_JARGS="-Dcom.bbn.marti.takcl.server0DbHost=${DOCKER0_IP} \\
	-Dcom.bbn.marti.takcl.server1DbHost=${DOCKER1_IP} \\
	-Dcom.bbn.marti.takcl.server2DbHost=${DOCKER2_IP} \\"

  else
    DB_JARGS="-Dcom.bbn.marti.takcl.disableDb=true \\"
  fi

	if [[ "`ls ${DEPLOYMENT_DIR}/`" != "" ]];then
		rm -r ${DEPLOYMENT_DIR}/*
	fi

	cp -R ${SERVER_SRC}/* "${DEPLOYMENT_DIR}"/

	if [[ "${TEST_MODE}" == "true" ]];then
		DEPLOYMENT_DIR="/opt/tak/TEST_RESULTS/TEST_FARM/SERVER_0"
		mkdir -p "${DEPLOYMENT_DIR}"
		cp -R ${SERVER_SRC}/* "${DEPLOYMENT_DIR}"/

	fi

	cp "${SRC}/takserver-takcl-core/src/rpm/resources/TAKCLConfig.xml" "${DEPLOYMENT_DIR}/utils/"

	echo "#!/usr/bin/env bash

# alias runTest="java -Dcom.bbn.marti.takcl.takclIgniteConfig=true -Dcom.bbn.marti.takcl.disableDb=true -Djava.net.preferIPv4Stack=true -Dlogging.level.com.bbn=DEBUG -Dlogging.level.tak=DEBUG -Dlogging.level.com.bbn.marti.tests.Assert=TRACE -jar /opt/tak/utils/takcl.jar tests run"
# alias runUserTest="java -Dcom.bbn.marti.takcl.disableDb=true -Djava.net.preferIPv4Stack=true -Dlogging.level.com.bbn=DEBUG -Dlogging.level.tak=DEBUG -Dlogging.level.com.bbn.marti.tests.Assert=TRACE -jar /opt/tak/utils/takcl.jar tests run"
# alias runDbTest="java -Djava.net.preferIPv4Stack=true -Dlogging.level.com.bbn=DEBUG -Dlogging.level.tak=DEBUG -Dlogging.level.com.bbn.marti.tests.Assert=TRACE -jar /opt/tak/utils/takcl.jar tests run"

java $DB_JARGS
	-Djava.net.preferIPv4Stack=true \\
	-Dlogging.level.com.bbn=DEBUG \\
	-Dlogging.level.tak=DEBUG \\
	-Dlogging.level.com.bbn.marti.takcl.connectivity.missions=TRACE \\
	-Dlogging.level.com.bbn.marti.tests.Assert=TRACE \\
	-jar /opt/tak/utils/takcl.jar tests \$@" > /opt/tak/utils/test.sh
	chmod +x /opt/tak/utils/test.sh

	if [[ "${SERVER_SRC}" == "${DEFAULT_SERVER_SRC}" ]];then
		if [[ "${TEST_MODE}" != "true" ]];then
			cp "${SERVER_SRC}/CoreConfig.example.xml" "${DEPLOYMENT_DIR}/CoreConfig.xml"
			if [[ "${USE_COT_DB}" == "true" ]];then
				sed -i "s/password=\"\"/password=\"atakatak\"/g" "${DEPLOYMENT_DIR}/CoreConfig.xml"
				sed -i "s/jdbc:postgresql:\/\/127.0.0.1:5432\/cot/jdbc:postgresql:\/\/${DOCKER0_IP}:5432\/cot/g" /opt/tak/CoreConfig.xml
			else
			  sed -i 's/<repository enable="true"/<repository enable="false"/g' "${DEPLOYMENT_DIR}/CoreConfig.xml"
			fi
		fi

		if [ -f "${TAKSERVER_USER_AUTHENTICATION_FILE}" ];then
			cp -R "${TAKSERVER_USER_AUTHENTICATION_FILE}" "${DEPLOYMENT_DIR}/"
		fi
	fi
	echo DONE
fi

if [[ "${TAKCL_TEST_CERT_SRC_DIR}" != "" ]] && [[ -d "${TAKCL_TEST_CERT_SRC_DIR}" ]];then
	cp -r ${TAKCL_TEST_CERT_SRC_DIR} ${DEPLOYMENT_DIR}/certs/files
fi

if [ $START == true ];then
	echo STARTING SERVER...

	if [[ $USE_COT_DB == true ]];then
		echo Waiting for DB to settle...
		sleep 20
		sed -i "s/<connection url=\"jdbc:postgresql:\/\/tak-database:5432\/cot\" username=\"martiuser\" password=\"\" \/>/<connection url=\"jdbc:postgresql:\/\/${DOCKER0_IP}:5432\/cot\" username=\"${POSTGRES_USER}\" password=\"${TAKCL_SERVER_POSTGRES_PASSWORD}\"\/>/g" "${DEPLOYMENT_DIR}/CoreConfig.xml"
		java -jar /opt/tak/db-utils/SchemaManager.jar -url jdbc:postgresql://${DOCKER0_IP}:5432/cot -user ${POSTGRES_USER} -password ${TAKCL_SERVER_POSTGRES_PASSWORD} upgrade
	fi

	{
	if [[ "${DELETE_LOGS}" == "true" ]];then
		if [[ -d "${DEPLOYMENT_DIR}/logs" ]] && [[ `ls "${DEPLOYMENT_DIR}/logs"` != "" ]];then
			echo DELETING EXISTING LOGS
			rm ${DEPLOYMENT_DIR}/logs/*
		fi
	fi

	cd "${DEPLOYMENT_DIR}"
	. ./setenv.sh
	
	if [ $MONOLITH == true ];then
		java ${JARGS} -jar -Dspring.profiles.active=monolith takserver.war ${ARGS} &
		MESSAGING_PID=$!
	else
		java ${JARGS} -jar ${JARGS} -Dspring.profiles.active=messaging takserver.war ${ARGS} &
		MESSAGING_PID=$!
		java ${JARGS} -jar ${JARGS} -Dspring.profiles.active=api takserver.war ${ARGS} &
		API_PID=$!
	fi

	echo DONE

	sleep 1000000
	} || {
		echo EXITING!
		if [[ "${MESSAGING_PID}" != "" ]];then
			kill -9 ${MESSAGING_PID}
		fi

		if [[ "${API_PID}" != "" ]];then
			kill -9 ${API_PID}
		fi

	}
fi
