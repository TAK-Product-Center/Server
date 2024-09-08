#!/bin/bash
set -u
if [ -z ${CA_NAME+x} ];
then
  echo "Please set the following variables before running this script: CA_NAME. \n"
  exit -1
else
  ./cert-metadata.sh
  ./makeRootCa.sh --ca-name $CA_NAME
  ./makeCert.sh server takserver
  ./makeCert.sh client user
  ./makeCert.sh client admin
fi