#!/bin/bash
### Script to revoke a certificate and update the CRL.  

. cert-metadata.sh

if [ ! "$1" ]; then
   echo "Provide the filename of the certificate to revoke (.pem)."
   exit 1
fi

if [ ! "$2" ]; then
   echo "Provide the filename of the CA key (.key)."
   exit 1
fi

if [ ! "$3" ]; then
   echo "Provide the filename of the CA certificate (.pem)."
   exit 1
fi

mkdir -p "$DIR"
cd "$DIR"

touch crl_index.txt
touch crl_index.txt.attr

## if you have a custom password  for your CA key, edit this, or comment it
## out to have the openssl commands below prompt you for the password:
KEYPASS="-key $PASS"
CONFIG=../config.cfg

# make a copy of the pem that we can modify, remove all blank lines to account for bug in Client Certificates
# interface that was saving out pem files with blank line
cp "$1.pem" "$1-revoke.pem"
sed -i '/^$/d' "$1-revoke.pem"

openssl ca -config $CONFIG -revoke "$1-revoke.pem" -keyfile "$2".key $KEYPASS -cert "$3".pem
openssl ca -config $CONFIG -gencrl -keyfile "$2".key $KEYPASS -cert "$3".pem -out "$3".crl

rm -f "$1-revoke.pem"

## the command below will print the CRL in a readable form
# openssl crl -text -in *.crl
