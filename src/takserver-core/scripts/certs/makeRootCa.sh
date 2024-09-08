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

CA_NAME=""
for (( i=1; i <= "$#"; i++ )); do
  if [ "${!i}" == "--ca-name" ];then
    NEXT_VAL=$(($i + 1))
    CA_NAME=${!NEXT_VAL}
  fi
done

if [ -z "${CA_NAME}" ]; then
  echo "Please give a name for your CA (no spaces).  It should be unique.  If you don't enter anything, or try something under 5 characters, I will make one for you"
  read CA_NAME
fi

canamelen=${#CA_NAME}
if [[ "$canamelen" -lt 5 ]]; then
  CA_NAME=`date +%N`
fi

CRYPTO_SETTINGS=""
FIPS_SETTINGS=""
openssl list -providers 2>&1 | grep "\(invalid command\|unknown option\)" >/dev/null
if [ $? -ne 0 ] ; then
  echo "Using legacy provider"
  CRYPTO_SETTINGS="-legacy"
fi

fips=false
for var in "$@"
do
    if [ "$var" == "-fips" ] || [ "$var" == "--fips" ];then
      fips=true
      FIPS_SETTINGS='-macalg SHA256 -aes256 -descert -keypbe AES-256-CBC -certpbe AES-256-CBC'
    fi
done

SUBJ=$SUBJBASE"CN=$CA_NAME"
echo "Making a CA for " $SUBJ
openssl req -new -sha256 -x509 -days 3652 -extensions v3_ca -keyout ca-do-not-share.key -out ca.pem -passout pass:${CAPASS} -config ../config.cfg -subj "$SUBJ"
openssl x509 -in ca.pem  -addtrust clientAuth -addtrust serverAuth -setalias "${CA_NAME}" -out ca-trusted.pem

if [ "$fips" = true ];then
  openssl pkcs12 ${CRYPTO_SETTINGS} -export -in ca-trusted.pem -out truststore-root-legacy.p12 -nokeys -caname "${CA_NAME}" -passout pass:${CAPASS}
fi
openssl pkcs12 ${CRYPTO_SETTINGS} ${FIPS_SETTINGS} -export -in ca-trusted.pem -out truststore-root.p12 -nokeys -caname "${CA_NAME}" -passout pass:${CAPASS}
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
if ! $(grep -q unique_subject crl_index.txt.attr); then
  echo "unique_subject = no" >> crl_index.txt.attr
fi

openssl ca -config ../config.cfg -gencrl -keyfile ca-do-not-share.key $KEYPASS -cert ca.pem -out ca.crl