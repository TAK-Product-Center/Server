#!/bin/sh
FILE=CoreConfig.xml
if [ "$1" ]; then
  FILE=$1
fi

xmllint --noout --schema CoreConfig.xsd $FILE
