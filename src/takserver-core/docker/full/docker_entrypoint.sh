#!/usr/bin/env bash

set -e

TR=/opt/tak
CR=${TR}/certs
CONFIG=${TR}/data/CoreConfig.xml
TAKIGNITECONFIG=${TR}/data/TAKIgniteConfig.xml
CONFIG_PID=null
MESSAGING_PID=null
API_PID=null
PM_PID=null


check_env_var() {
	if [[ "${!1}" == "" ]];then
		echo The environment variable "${1}" must be set for ${2}!
		exit 1
	fi
}

kill() {
	echo Please wait a moment. It may take serveral seconds to fully shut down TAKServer.

    if [ $CONFIG_PID != null ];then
        kill $CONFIG_PID
    fi

    if [ $MESSAGING_PID != null ];then
        kill $MESSAGING_PID
    fi

    if [ $API_PID != null ];then
        kill $API_PID
    fi

    if [ $PM_PID != null ];then
        kill $PM_PID
    fi

}

trap kill SIGINT
trap kill SIGTERM

check_env_var POSTGRES_DB "the database connection if TAKSERVER_NO_DB is not set to true!"
check_env_var POSTGRES_USER "the database connection if TAKSERVER_NO_DB is not set to true!"
check_env_var POSTGRES_PASSWORD "the database connection if TAKSERVER_NO_DB is not set to true!"
check_env_var CA_NAME " the Certificate Authority Name"
check_env_var CA_PASS " the Certificate Authority Password"
check_env_var STATE "the Certificate Authority generation"
check_env_var CITY "the Certificate Authority generation"
check_env_var ORGANIZATION "the Certificate Authority generation"
check_env_var ORGANIZATIONAL_UNIT "the Certificate Authority generation"
check_env_var ADMIN_CERT_NAME "the TAKServer management certificate"
check_env_var ADMIN_CERT_PASS "the TAKServer management certificate password"
check_env_var TAKSERVER_CERT_PASS "the TAKServer instance certificate password"

# Seed initial certificate data if necessary
if [[ ! -d "${TR}/data/certs" ]];then
	mkdir "${TR}/data/certs"
fi
if [[ -z "$(ls -A "${TR}/data/certs")" ]];then
	echo Copying initial certificate configuration
	cp -R ${TR}/certs/* ${TR}/data/certs/
else
	echo Using existing certificates.
fi

# Move original certificate data and symlink to certificate data in data dir
mv ${TR}/certs ${TR}/certs.orig
ln -s "${TR}/data/certs" "${TR}/certs"

# Seed initial CoreConfig.xml if necessary
if [[ ! -f "${CONFIG}" ]];then
	echo Copying initial CoreConfig.xml
	if [[ -f "${TR}/CoreConfig.xml" ]];then
		cp ${TR}/CoreConfig.xml ${CONFIG}
		mv ${TR}/CoreConfig.xml ${TR}/CoreConfig.xml.orig
	else
		cp ${TR}/CoreConfig.example.xml ${CONFIG}
	fi
else
	echo Using existing CoreConfig.xml.
fi

# Seed initial TAKIgniteConfig.xml if necessary
if [[ ! -f "${TAKIGNITECONFIG}" ]];then
	echo Copying initial TAKIgniteConfig.xml
	if [[ -f "${TR}/TAKIgniteConfig.xml" ]];then
		cp ${TR}/TAKIgniteConfig.xml ${TAKIGNITECONFIG}
		mv ${TR}/TAKIgniteConfig.xml ${TR}/CoreConfig.xml.orig
	else
		cp ${TR}/TAKIgniteConfig.example.xml ${TAKIGNITECONFIG}
	fi
else
	echo Using existing TAKIgniteConfig.xml.
fi

# Symlink the log directory
ln -s "${TR}/data/logs" "${TR}/logs"

cd ${CR}

if [[ ! -f "${CR}/files/root-ca.pem" ]];then
	CAPASS=${CA_PASS} bash /opt/tak/certs/makeRootCa.sh --ca-name "${CA_NAME}"
else
	echo Using existing root CA.
fi

if [[ ! -f "${CR}/files/intermediate-signing.jks" ]];then
  echo "Making new signing certificate."
  export CAPASS=${CA_PASS}
  yes | /opt/tak/certs/makeCert.sh ca intermediate
else
  echo "Using existing intermediate CA certificate."
fi

if [[ ! -f "${CR}/files/takserver.pem" ]];then
	CAPASS=${CA_PASS} PASS="${TAKSERVER_CERT_PASS}" bash /opt/tak/certs/makeCert.sh server takserver
else
	echo Using existing takserver certificate.
fi

if [[ ! -f "${CR}/files/${ADMIN_CERT_NAME}.pem" ]];then
	CAPASS=${CA_PASS} PASS="${ADMIN_CERT_PASS}" bash /opt/tak/certs/makeCert.sh client "${ADMIN_CERT_NAME}"
else
	echo Using existing ${ADMIN_CERT_NAME} certificate.
fi

chmod -R 777 ${TR}/data/

python3 ${TR}/coreConfigEnvHelper.py "${CONFIG}" "${CONFIG}"

# Wait for PGSQL init
sleep 8

# Init PGSQL
java -jar ${TR}/db-utils/SchemaManager.jar -url jdbc:postgresql://takdb:5432/${POSTGRES_DB} -user ${POSTGRES_USER} -password ${POSTGRES_PASSWORD} upgrade
sleep 4

cd ${TR}

. ./setenv.sh

java -jar -Xmx${CONFIG_MAX_HEAP}m -Dspring.profiles.active=config takserver.war &
CONFIG_PID=$!
java -jar -Xmx${MESSAGING_MAX_HEAP}m -Dspring.profiles.active=messaging takserver.war &
MESSAGING_PID=$!
java -jar -Xmx${API_MAX_HEAP}m -Dspring.profiles.active=api -Dkeystore.pkcs12.legacy takserver.war &
API_PID=$!
java -jar -Xmx${PLUGIN_MANAGER_MAX_HEAP}m -Dloader.path=WEB-INF/lib-provided,WEB-INF/lib,WEB-INF/classes,file:lib/ takserver-pm.jar &
PM_PID=$!

sleep 16
echo  -e "\033[33;5mWAITING FOR THE SERVER TO START UP BEFORE ADDING THE ADMIN USER...\033[0m"

# Give some time for the server to start up
sleep 44
TAKCL_CORECONFIG_PATH="${CONFIG}"
TAKCL_TAKIGNITECONFIG_PATH="${TAKIGNITECONFIG}"
java -jar /opt/tak/utils/UserManager.jar certmod -A "/opt/tak/certs/files/${ADMIN_CERT_NAME}.pem"

echo ADMIN USER ADDED

# Doing a wait is cleaner than using sleep and will simply exit as soon the PluginManager
# process completes.   No need to "kill" the process.
wait $PM_PID
