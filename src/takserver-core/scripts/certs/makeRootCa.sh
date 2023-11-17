#!/bin/bash
# EDIT cert-metadata.sh before running this script! 
#  Optionally, you may also edit config.cfg, although unless you know what
#  you are doing, you probably shouldn't.

. cert-metadata.sh

mkdir -p "$DIR"
cd "$DIR"

if [ -e ca.pem ]; then
  echo "ca.pem file already exists!  Please delete it before trying again"
  exit -1
fi

if [ ${#} == 2 ] && [ "${1}" == "--ca-name" ];then
  CA_NAME=${2}
else
  echo "Please give a name for your CA (no spaces).  It should be unique.  If you don't enter anything, or try something under 5 characters, I will make one for you"
  read CA_NAME
fi

canamelen=${#CA_NAME}
if [[ "$canamelen" -lt 5 ]]; then
  CA_NAME=`date +%N`
fi

openssl list -providers 2>&1 | grep "\(invalid command\|unknown option\)" >/dev/null
if [ $? -ne 0 ] ; then
  echo "Using legacy provider"
  LEGACY_PROVIDER="-legacy"
fi

SUBJ=$SUBJBASE"CN=$CA_NAME"
echo "Making a CA for " $SUBJ
openssl req -new -sha256 -x509 -days 3652 -extensions v3_ca -keyout ca-do-not-share.key -out ca.pem -passout pass:${CAPASS} -config ../config.cfg -subj "$SUBJ"
openssl x509 -in ca.pem  -addtrust clientAuth -addtrust serverAuth -setalias "${CA_NAME}" -out ca-trusted.pem

openssl pkcs12 ${LEGACY_PROVIDER} -export -in ca-trusted.pem -out truststore-root.p12 -nokeys -caname "${CA_NAME}" -passout pass:${CAPASS}
keytool -import -trustcacerts -file ca.pem -keystore truststore-root.jks -alias "${CA_NAME}" -storepass "${CAPASS}" -noprompt
cp truststore-root.jks fed-truststore.jks

## make copies for safety
cp ca.pem root-ca.pem
cp ca-trusted.pem root-ca-trusted.pem 
cp ca-do-not-share.key root-ca-do-not-share.key

## create empty crl 
KEYPASS="-key $CAPASS"

touch crl_index.txt
touch crl_index.txt.attr

openssl ca -config ../config.cfg -gencrl -keyfile ca-do-not-share.key $KEYPASS -cert ca.pem -out ca.crl



