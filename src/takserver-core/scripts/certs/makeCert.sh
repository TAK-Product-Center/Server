#!/bin/bash
# EDIT cert-metadata.sh before running this script! 
#  Optionally, you may also edit config.cfg, although unless you know what
#  you are doing, you probably shouldn't.

. cert-metadata.sh

mkdir -p "$DIR"
cd "$DIR"

usage() {
  echo "Usage: ./makeCert.sh [server|client|ca] <common name>"
  echo "  If you do not provide a common name on the command line, you will be prompted for one"
  exit -1
}

if [ ! -e ca.pem ]; then
  echo "ca.pem does not exist!  Please make a CA before trying to make server certficiates"
  exit -1
fi

if [ "$1" ]; then
  if [ "$1" == "server" ]; then
    EXT=server
  elif [ "$1" == "client" ]; then
    EXT=client
  elif [ "$1" == "ca" ]; then
    EXT=v3_ca
  else
    usage
  fi
else
  usage
fi 


if [ "$2" ]; then
  SNAME=$2
else
  echo "Please give the common name for your certificate (no spaces).  It should be unique.  If you don't enter anything, or try something under 5 characters, I will make one for you"
  read SNAME
  canamelen=${#SNAME}
  if [[ "$canamelen" -lt 5 ]]; then
    SNAME=`date +%N`
  fi
fi

if [ "$1" == "server" ]; then
  if [[ $SNAME =~ ^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$ ]]; then
     ALTNAMEFIELD="IP.1" 
  else
     ALTNAMEFIELD="DNS.1"
  fi

  cp ../config.cfg config-$SNAME.cfg
  echo "
subjectAltName = @alt_names

[alt_names]
$ALTNAMEFIELD = $SNAME
" >> config-$SNAME.cfg
  CONFIG=config-$SNAME.cfg
else
  CONFIG=../config.cfg
fi

openssl list -providers 2>&1 | grep "\(invalid command\|unknown option\)" >/dev/null
if [ $? -ne 0 ] ; then
  echo "Using legacy provider"
  LEGACY_PROVIDER="-legacy"
fi

SUBJ=$SUBJBASE"CN=$SNAME"
echo "Making a $1 cert for " $SUBJ
openssl req -new -newkey rsa:2048 -sha256 -keyout "${SNAME}".key -passout pass:$PASS -out "${SNAME}".csr -subj "$SUBJ" 
openssl x509 -sha256 -req -days 730 -in "${SNAME}".csr -CA ca.pem -CAkey ca-do-not-share.key -out "${SNAME}".pem -set_serial ${RANDOM} -passin pass:${CAPASS} -extensions $EXT -extfile $CONFIG

if [[ "$1" == "ca" ]]; then
  openssl x509 -in "${SNAME}".pem  -addtrust clientAuth -addtrust serverAuth -setalias "${SNAME}" -out "${SNAME}"-trusted.pem
fi

# now add the chain
cat ca.pem >> "${SNAME}".pem
cat ca-trusted.pem >> "${SNAME}"-trusted.pem

# now make pkcs12 and jks keystore files
if [[ "$1" == "server" ||  "$1" == "client" ]]; then
  openssl pkcs12 ${LEGACY_PROVIDER} -export -in "${SNAME}".pem -inkey "${SNAME}".key -out "${SNAME}".p12 -name "${SNAME}" -CAfile ca.pem -passin pass:${PASS} -passout pass:${PASS}
  keytool -importkeystore -deststorepass "${PASS}" -destkeypass "${PASS}" -destkeystore "${SNAME}".jks -srckeystore "${SNAME}".p12 -srcstoretype PKCS12 -srcstorepass "${PASS}" -alias "${SNAME}"
else # a CA
  openssl pkcs12 ${LEGACY_PROVIDER} -export -in "${SNAME}"-trusted.pem -out truststore-"${SNAME}".p12 -nokeys -passout pass:${CAPASS}
  keytool -import -trustcacerts -file "${SNAME}".pem -keystore truststore-"${SNAME}".jks -storepass "${CAPASS}" -noprompt

  # include a CA signing keystore; NOT FOR DISTRIBUTION TO CLIENTS
  openssl pkcs12 ${LEGACY_PROVIDER} -export -in "${SNAME}".pem -inkey "${SNAME}".key -out "${SNAME}"-signing.p12 -name "${SNAME}" -passin pass:${PASS} -passout pass:${PASS}
  keytool -importkeystore -deststorepass "${PASS}" -destkeypass "${PASS}" -destkeystore "${SNAME}"-signing.jks -srckeystore "${SNAME}"-signing.p12 -srcstoretype PKCS12 -srcstorepass "${PASS}" -alias "${SNAME}"

  ## create empty crl 
  KEYPASS="-key $CAPASS"
  openssl ca -config ../config.cfg -gencrl -keyfile "${SNAME}".key $KEYPASS -cert "${SNAME}".pem -out "${SNAME}".crl
  
  echo "Do you want me to move files around so that future server and client certificates are signed by this new CA? [y/n]"
  read MVREQ
  if [[ "$MVREQ" == "y" || "$MVREQ" == "Y" ]]; then
    cp $SNAME.pem ca.pem
    cp $SNAME.key ca-do-not-share.key
    cp $SNAME-trusted.pem ca-trusted.pem
  else
    echo "Ok, not overwriting existing keys.  To manually change the CA later, execute these commands from the 'files' directory:"
    echo "  cp $SNAME.pem ca.pem"
    echo "  cp $SNAME.key ca-do-not-share.key"
    echo "  cp $SNAME-trusted.pem ca-trusted.pem"
  fi

fi
