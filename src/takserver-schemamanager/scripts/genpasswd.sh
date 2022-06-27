#!/bin/sh
# Usage: pass as arguments the passwords you want to generate hashs for


for pass in "$@"
do
    htpasswd -nbB -C 10 username $pass | sed '0,/username:$2y/s/username:$2y/$2a/'
done
