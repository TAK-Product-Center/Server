#!/bin/bash

FILE=CoreConfig.xml

if [ ! -f "$FILE" ]; then
    FILE=opt/tak/CoreConfig.xml
fi

if [ -z "$DB_URL" ]; then
  DB_URL='jdbc:postgresql:\/\/takserver-postgresql:5432\/cot'
fi

if [ -z "$DB_USERNAME" ]; then
  DB_USERNAME=postgres
fi

if [ -z "$DB_PASSWORD" ]; then
  DB_PASSWORD=postgres
fi

sed -r -i $FILE -e 's/DB_URL_PLACEHOLDER/'$DB_URL'/g'
sed -r -i $FILE -e 's/DB_USERNAME_PLACEHOLDER/'$DB_USERNAME'/g'
sed -r -i $FILE -e 's/DB_PASSWORD_PLACEHOLDER/'$DB_PASSWORD'/g'