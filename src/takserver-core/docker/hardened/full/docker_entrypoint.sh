#!/bin/bash
set -e

TAKDIR=/opt/tak
CERTDIR=${TAKDIR}/certs
EXCONFIGFILE=${TAKDIR}/CoreConfig.example.xml
CONFIGFILE=${TAKDIR}/CoreConfig.xml

MESSAGING_PID=null
API_PID=null
PM_PID=null


check_env_var() {
    if [[ "${!1}" == "" ]];then
        echo The environment variable "${1}" must be set for ${2}!
        exit 1
    fi
}

killall() {
    echo Please wait a moment. It may take serveral seconds to fully shut down TAKServer.
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

trap killall SIGINT
trap killall SIGTERM

check_env_var CA_NAME " the Certificate Authority Name"
check_env_var CA_PASS " the Certificate Authority Password"
check_env_var STATE "the Certificate Authority generation"
check_env_var CITY "the Certificate Authority generation"
check_env_var ORGANIZATION "the Certificate Authority generation"
check_env_var ORGANIZATIONAL_UNIT "the Certificate Authority generation"
check_env_var ADMIN_CERT_NAME "the TAKServer management certificate"
check_env_var ADMIN_CERT_PASS "the TAKServer management certificate password"
check_env_var TAKSERVER_CERT_PASS "the TAKServer instance certificate password"

cd ${CERTDIR}

# If just regenerating the certs.  Variables will need to be updated beforehand.
if [[ -f "${CONFIGFILE}" ]];then
  EXCONFIGFILE=${CONFIGFILE}
fi

if [[ ! -d "${CERTDIR}/files" ]];then
    mkdir "${CERTDIR}/files"
fi

if [[ -z "$(ls -A "${CERTDIR}/files")" ]];then
  if [[ ! -f "${CERTDIR}/files/root-ca.pem" ]];then
     echo "Making new root certificate authority."
      export CAPASS=${CA_PASS}
      ${CERTDIR}/makeRootCa.sh --ca-name "${CA_NAME}"
  else
     echo "Using existing root CA."
  fi

  if [[ ! -f "${CERTDIR}/files/takserver.pem" ]];then
     echo "Making new takserver certificate."
      export CAPASS=${CA_PASS}
      export PASS="${TAKSERVER_CERT_PASS}"
      ${CERTDIR}/makeCert.sh server takserver
  else
      echo "Using existing takserver certificate."
  fi

  if [[ ! -f "${CERTDIR}/files/${ADMIN_CERT_NAME}.pem" ]];then
      echo "Making new ${ADMIN_CERT_NAME} certificate."
      export CAPASS=${CA_PASS}
      export PASS="${ADMIN_CERT_PASS}"
      ${CERTDIR}/makeCert.sh client "${ADMIN_CERT_NAME}"
  else
      echo "Using existing ${ADMIN_CERT_NAME} certificate."
  fi

  cd ${TAKDIR}

  cp ${EXCONFIGFILE} ${EXCONFIGFILE}.bak

  echo "Updating CoreConfig.example.xml values..."
  sed -i 's#keystore="JKS" keystoreFile="/opt/tak/certs/files/takserver.jks" keystorePass="atakatak"#keystore="JKS" keystoreFile="/opt/tak/certs/files/takserver.jks" keystorePass="'"$TAKSERVER_CERT_PASS"'"#' ${EXCONFIGFILE}
  sed -i 's#truststore="JKS" truststoreFile="/opt/tak/certs/files/truststore-root.jks" truststorePass="atakatak"#truststore="JKS" truststoreFile="/opt/tak/certs/files/truststore-root.jks" truststorePass="'"$CA_PASS"'"#' ${EXCONFIGFILE}

  # Wait for PGSQL init
  sleep 30

  cd ${TAKDIR}
  . ./setenv.sh

  echo "Starting the Server in order to add the ADMIN user."
  java -jar -Xmx${MESSAGING_MAX_HEAP}m -Dspring.profiles.active=messaging takserver.war &
  MESSAGING_PID=$!
  java -jar -Xmx${API_MAX_HEAP}m -Dspring.profiles.active=api -Dkeystore.pkcs12.legacy takserver.war &
  API_PID=$!

  sleep 8
  echo  -e "\033[33;5mWAITING FOR THE SERVER TO START UP BEFORE ADDING THE ADMIN USER...\033[0m"

  echo "Waiting to allow the server to start up"
  sleep 60
  TAKCL_CORECONFIG_PATH="${CONFIGFILE}"
  echo "Adding ADMIN certs"
  java -jar ${TAKDIR}/utils/UserManager.jar certmod -A "${CERTDIR}/files/${ADMIN_CERT_NAME}.pem"
  echo "ADMIN user added..."

  echo "Stopping the Server in preparation for loading the new ADMIN user."
  kill $MESSAGING_PID
  kill $API_PID

  echo "Waiting for the processes to stop..."
  sleep 60
else
    echo "${CERTDIR}/files directory already exists.  Using existing certificates."
fi

echo "Starting TAK Server ..."
cd ${TAKDIR}
. ./setenv.sh
java -jar -Xmx${MESSAGING_MAX_HEAP}m -Dspring.profiles.active=messaging takserver.war &
MESSAGING_PID=$!
java -jar -Xmx${API_MAX_HEAP}m -Dspring.profiles.active=api -Dkeystore.pkcs12.legacy takserver.war &
API_PID=$!
java -jar -Xmx${PLUGIN_MANAGER_MAX_HEAP}m -Dloader.path=WEB-INF/lib-provided,WEB-INF/lib,WEB-INF/classes,file:lib/ takserver-pm.jar &
PM_PID=$!

sleep infinity
