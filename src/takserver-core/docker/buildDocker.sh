#!/bin/bash

DIRNAME=`basename $PWD`
if [ "$DIRNAME"=docker ]; then
  echo "In docker directory.  Changing to  base directory."
  cd ..
fi

echo "Building and setting up takserver-db container"
docker build -t takserver-db:"$(cat tak/version.txt)" -f docker/Dockerfile.takserver-db .

echo "Building and setting up takserver container"
docker build -t takserver:"$(cat tak/version.txt)" -f docker/Dockerfile.takserver .
