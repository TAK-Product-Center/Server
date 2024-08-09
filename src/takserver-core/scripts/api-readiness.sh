#!/bin/sh
cat '/logs/takserver-api.log' | grep -q 'Tomcat started on port(s)'

nc -zw3 $POSTGRES_HOST $POSTGRES_PORT
RVAL=$?

while [ $RVAL != 0 ];do
    nc -zw3 $POSTGRES_HOST $POSTGRES_PORT
    RVAL=$?
    sleep 1
#    init_time=true
done

#if [ "$init_time" = "true" ];then
#  sleep 60
#fi
