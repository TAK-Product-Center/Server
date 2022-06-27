#!/bin/sh

cd certs
sed -i '/Please edit/d' ./cert-metadata.sh
sed -i '/delete this/d' ./cert-metadata.sh