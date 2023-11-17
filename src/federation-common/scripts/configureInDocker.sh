!/bin/sh
if [ $# -eq 0 ]
  then
    ps -ef | grep 'federation-hub-policy' | grep -v grep | awk '{print $2}' | xargs kill
    ps -ef | grep 'federation-hub-ui' | grep -v grep | awk '{print $2}' | xargs kill
    ps -ef | grep 'federation-hub-broker' | grep -v grep | awk '{print $2}' | xargs kill
fi

cd /opt/tak/federation-hub/scripts/
sleep 1
sh federation-hub-policy.sh &
sleep 2
sh federation-hub-broker.sh &
sleep 3
sh federation-hub-ui.sh &

if ! [ $# -eq 0 ]
  then
    tail -f /dev/null
fi