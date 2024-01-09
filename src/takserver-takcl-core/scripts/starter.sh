#!/usr/bin/env bash

BUILD=false
DEPLOY=false
START=false
USE_COT_DB=false
KILL_EXISTING_TAKSERVER=false

START_RETENTION=false
START_PM=false

SERVER0_DOCKER_DB_IDENTIFIER=TakserverServer0DB
POSTGRES_USER=martiuser
POSTGRES_DB=cot
POSTGRES_PORT=5432

CONFIG_PID=null
MESSAGING_PID=null
API_PID=null
PM_PID=null
RETENTION_PID=null

DEFAULT_SERVER_SRC=takserver-package/build/takArtifacts

SRC=`realpath "$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"/../../`

cd "${SRC}"

if [[ "${TAKCL_SERVER_POSTGRES_PASSWORD}" == "" ]];then
  echo The environment variable TAKCL_SERVER_POSTGRES_PASSWORD must be set to a password!
  exit 1
fi

if [[ "${2}" == "" ]];then
  echo The second parameter should be the ABSOLUTE path of the desired test environment!
  exit 1
fi

#if [[ -d "${2}" ]];then
#  echo The target test environment path should not exist!
#  exit 1
#fi

DEPLOYMENT_DIR=${2}
mkdir -p ${DEPLOYMENT_DIR}

JARGS="-Dloader.path=WEB-INF/lib-provided,WEB-INF/lib,WEB-INF/classes,file:lib/ 
    -Dio.netty.tmpdir=${DEPLOYMENT_DIR}/ 
    -Djava.io.tmpdir=${DEPLOYMENT_DIR}/ 
    -Dio.netty.native.workdir=${DEPLOYMENT_DIR}/ 
    -Djava.net.preferIPv4Stack=true 
    -Djava.security.egd=file:/dev/./urandom 
    -DIGNITE_UPDATE_NOTIFIER=false 
    -Djdk.tls.client.protocols=TLSv1.2 
	  -Dlogging.level.com.bbn=DEBUG
    -Dlogging.level.tak=DEBUG 
    -Xmx2000m 
    -XX:+HeapDumpOnOutOfMemoryError"


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
	if [ $API_PID != null ];then
		kill -9 $API_PID
	fi

	if [ $PM_PID != null ];then
		kill -9 $PM_LID
	fi

	if [ $RETENTION_PID != null ];then
		kill -9 $RETENTION_PID
	fi

	sleep 2

	if [ $MESSAGING_PID != null ];then
		kill -9 $MESSAGING_PID
	fi

	sleep 2

	if [ $CONFIG_PID != null ];then
		kill -9 $CONFIG_PID
	fi
}

trap ctrl_c SIGINT

set -e

if [[ $1 == "" ]];then
	printf "This script is intended to simplify local server deployment.

Used Docker Container Name:
    TakserverServer0DB

Required Environment Variables:
	TAKCL_SERVER_POSTGRES_PASSWORD		The Postgres password that will be used
	
Optional Environment Variables:
    TAKSERVER_USER_AUTHENTICATION_FILE	Overrides the User Authentication File contents

Usage:

This script takes a single parameter, which can use a mix of the following characters to perform 
the indicated startup tasks:

\t(b)uild\t\tPerforms \`./gradlew clean buildRpm buildDocker\`
\t(d)eploy\tDeploys takserver to the provided deployment dir
\tsetup (c)ot databases\t\t Sets up DBs in docker and sets up the test script to use them
\t(s)tart\t\tStarts the server
\t(k)ill\t\tKills the currently running takserver, if one is running
"
  exit 1
fi

if [[ $1 == *"d"* ]]; then
	DEPLOY=true
fi

if [[ $1 == *"s"* ]]; then
	START=true
fi

if [[ $1 == *"c"* ]];then
	USE_COT_DB=true
fi

if [[ $1 == *"k"* ]];then
	KILL_EXISTING_TAKSERVER=true
fi

SERVER_SRC="${DEFAULT_SERVER_SRC}"

if [[ $1 == *"b"* ]]; then
	BUILD=true
fi

if [[ $USE_COT_DB == true ]];then
  if [[ "`docker ps | grep ${SERVER0_DOCKER_DB_IDENTIFIER}`" != "" ]];then
    docker stop ${SERVER0_DOCKER_DB_IDENTIFIER}
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
	./gradlew --parallel clean buildRpm buildDocker
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
        -p ${POSTGRES_PORT} postgis/postgis:15-3.3
    fi

    DOCKER0_IP=`docker inspect --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' ${SERVER0_DOCKER_DB_IDENTIFIER}`

    DB_JARGS="-Dcom.bbn.marti.takcl.server0DbHost=${DOCKER0_IP} \\"

  else
    DB_JARGS="-Dcom.bbn.marti.takcl.disableDb=true \\"
  fi

	if [[ "`ls ${DEPLOYMENT_DIR}/`" != "" ]];then
		rm -r ${DEPLOYMENT_DIR}/*
	fi

	cp -R ${SERVER_SRC}/* "${DEPLOYMENT_DIR}"/

	cp "${SRC}/takserver-takcl-core/src/rpm/resources/TAKCLConfig.xml" "${DEPLOYMENT_DIR}/utils/"
	sed -i "s|/opt/tak|${DEPLOYMENT_DIR}|g" "${DEPLOYMENT_DIR}/utils/TAKCLConfig.xml"


	echo "#!/usr/bin/env bash

java $DB_JARGS
	-Djava.net.preferIPv4Stack=true \\
	-Dlogging.level.com.bbn=DEBUG \\
	-Dlogging.level.tak=DEBUG \\
	-Dlogging.level.com.bbn.marti.takcl.connectivity.missions=TRACE \\
	-Dlogging.level.com.bbn.marti.tests.Assert=TRACE \\
	-jar ${DEPLOYMENT_DIR}/utils/takcl.jar tests \$@" > ${DEPLOYMENT_DIR}/utils/test.sh
	chmod +x ${DEPLOYMENT_DIR}/utils/test.sh

	if [[ "${SERVER_SRC}" == "${DEFAULT_SERVER_SRC}" ]];then
		cp "${SERVER_SRC}/CoreConfig.example.xml" "${DEPLOYMENT_DIR}/CoreConfig.xml"
		cp "${SERVER_SRC}/TAKIgniteConfig.example.xml" "${DEPLOYMENT_DIR}/TAKIgniteConfig.xml"
		if [[ "${USE_COT_DB}" == "true" ]];then
			sed -i "s/password=\"\"/password=\"atakatak\"/g" "${DEPLOYMENT_DIR}/CoreConfig.xml"
			sed -i "s/jdbc:postgresql:\/\/127.0.0.1:5432\/cot/jdbc:postgresql:\/\/${DOCKER0_IP}:5432\/cot/g" ${DEPLOYMENT_DIR}/CoreConfig.xml
		else
		  sed -i 's/<repository enable="true"/<repository enable="false"/g' "${DEPLOYMENT_DIR}/CoreConfig.xml"
		fi

		if [ -f "${TAKSERVER_USER_AUTHENTICATION_FILE}" ];then
			cp -R "${TAKSERVER_USER_AUTHENTICATION_FILE}" "${DEPLOYMENT_DIR}/"
		fi
	fi
	echo DONE
fi

if [[ "${TAKSERVER_CERT_SOURCE}" != "" ]] && [[ -d "${TAKSERVER_CERT_SOURCE}" ]];then
	cp -r ${TAKSERVER_CERT_SOURCE} ${DEPLOYMENT_DIR}/certs/files
fi

if [ $START == true ];then
	echo STARTING SERVER...

	if [[ $USE_COT_DB == true ]];then
		echo Waiting for DB to settle...
		sleep 20
		sed -i "s/<connection url=\"jdbc:postgresql:\/\/tak-database:5432\/cot\" username=\"martiuser\" password=\"\" \/>/<connection url=\"jdbc:postgresql:\/\/${DOCKER0_IP}:5432\/cot\" username=\"${POSTGRES_USER}\" password=\"${TAKCL_SERVER_POSTGRES_PASSWORD}\"\/>/g" "${DEPLOYMENT_DIR}/CoreConfig.xml"
		sync
		java -jar ${DEPLOYMENT_DIR}/db-utils/SchemaManager.jar -url jdbc:postgresql://${DOCKER0_IP}:5432/cot -user ${POSTGRES_USER} -password ${TAKCL_SERVER_POSTGRES_PASSWORD} upgrade
	fi

	{

	cd "${DEPLOYMENT_DIR}"
	. ./setenv.sh

	export IGNITE_HOME="${DEPLOYMENT_DIR}"
	export JDK_JAVA_OPTIONS=${JARGS}

	sync

	java -Dspring.profiles.active=config -jar takserver.war ${ARGS} &
	CONFIG_PID=$!
  java -Dspring.profiles.active=messaging -jar takserver.war ${ARGS} &
	MESSAGING_PID=$!
	java -Dspring.profiles.active=api -jar takserver.war ${ARGS} &
	API_PID=$!

	if [[ "${START_PM}" == "true" ]];then
		java -jar takserver-pm.jar ${ARGS} &
		PM_PID=$!
	fi

	if [[ "${START_RETENTION}" == "true" ]];then
		java -jar takserver-retention.jar &
		RETENTION_PID=$!
	fi


	if [[ "${TAKSERVER_CERT_SOURCE}" != "" ]] && [[ -d "${TAKSERVER_CERT_SOURCE}" ]] && [[ -f "${TAKSERVER_CERT_SOURCE}/admin.pem" ]]; then
		echo Sleeping 60 seconds before adding admin user...
		sleep 60
		java -jar ${DEPLOYMENT_DIR}/utils/UserManager.jar certmod -A "${TAKSERVER_CERT_SOURCE}/admin.pem"
	fi

	echo DONE

	sleep 1000000
	} || {
		echo EXITING!
		if [[ ${API_PID} != null ]];then
			echo API_PID=${API_PID}
			kill -9 ${API_PID}
		fi

		if [[ ${PM_PID} != null ]];then
			echo PM_PID=${PM_PID}
			kill -9 ${PM_PID}
		fi

		if [[ ${RETENTION_PID} != null ]];then
			echo RETENTION_PID=${RETENTION_PID}
			kill -9 ${RETENTION_PID}
		fi

		if [[ ${MESSAGING_PID} != null ]];then
			echo MESSAGING_PID=${MESSAGING_PID}
			kill -9 ${MESSAGING_PID}
		fi

		if [[ ${CONFIG_PID} != null ]];then
			echo CONFIG_PID=${CONFIG_PID}
			kill -9 ${CONFIG_PID}
		fi

	}
fi
