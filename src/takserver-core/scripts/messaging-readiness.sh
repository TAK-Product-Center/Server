#!/bin/sh

set -e

# Check if the authentidation data has been created
cat 'UserAuthenticationFile.xml' | grep -q 'ROLE_ADMIN'

# Check if the postgres host is available
nc -zw3 $POSTGRES_HOST $POSTGRES_PORT