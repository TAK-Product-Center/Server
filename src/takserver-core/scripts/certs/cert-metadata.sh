# Common configuration for all certificates 
#  Edit these fields to be appropriate for your organization
#  If they are left blank, they will not be included.  Do not leave COUNTRY
#  blank (you may set it to "XX" if you want to be obtuse).
# 
#  Values for each may be optionally set as environment variables.
#  Replace variables such as ${STATE} and ${CITY} as needed.
# 

COUNTRY=US
STATE=${STATE}
CITY=${CITY}
ORGANIZATION=${ORGANIZATION:-TAK}
ORGANIZATIONAL_UNIT=${ORGANIZATIONAL_UNIT}

CAPASS=${CAPASS:-atakatak}
PASS=${PASS:-$CAPASS}

## subdirectory to put all the actual certs and keys in
DIR=files

##### don't edit below this line #####

if [[ -z ${STATE} || -z ${CITY} || -z ${ORGANIZATIONAL_UNIT} ]]; then
  echo "Please set the following variables before running this script: STATE, CITY, ORGANIZATIONAL_UNIT. \n
  The following environment variables can also be set to further secure and customize your certificates: ORGANIZATION, ORGANIZATIONAL_UNIT, CAPASS, and PASS."
  exit -1
fi

SUBJBASE="/C=${COUNTRY}/"
if [ -n "$STATE" ]; then
 SUBJBASE+="ST=${STATE}/"
fi
if [ -n "$CITY" ]; then
 SUBJBASE+="L=${CITY}/"
fi
if [ -n "$ORGANIZATION" ]; then
 SUBJBASE+="O=${ORGANIZATION}/"
fi
if [ -n "$ORGANIZATIONAL_UNIT" ]; then
 SUBJBASE+="OU=${ORGANIZATIONAL_UNIT}/"
fi

