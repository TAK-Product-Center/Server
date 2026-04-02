#!/usr/bin/env sh

if [ "${TAK_KUBERNETES_NAMESPACE}" = "" ];then
  NAMESPACE=takserver
else
  NAMESPACE="${TAK_KUBERNETES_NAMESPACE}"
fi

POLLING_INTERVAL=2

API_REGEX='takserver\-api\-[0-9a-z]*\-[0-9a-z]*\s{1,}1/1'
MESSAGING_REGEX='takserver\-messaging\-[0-9a-z]*\-[0-9a-z]*\s{1,}1/1'
CONFIG_REGEX='takserver\-config\-[0-9a-z]*\-[0-9a-z]*\s{1,}1/1'
IGNITE_REGEX='takserver\-ignite\-[0-9]*\s{1,}1/1'
NATS_REGEX='takserver\-nats\-[0-9]*\s{1,}2/2'

DURATION=0

wait_for_shutdown() {
  shutdown_duration=0
	kubectl get pods -n "${NAMESPACE}" | grep -E "${1}" > /dev/null 2>&1
	result=$?
	while [ $result -eq 0 ];do
		echo -n '.'
		sleep ${POLLING_INTERVAL}
		shutdown_duration=`expr $shutdown_duration + $POLLING_INTERVAL`
		kubectl get pods -n "${NAMESPACE}" | grep -E "${1}" > /dev/null 2>&1
		result=$?
	done
	echo " Done (${shutdown_duration}s)"
	DURATION=`expr $DURATION + $shutdown_duration`
}

wait_for_startup() {
  startup_duration=0
	kubectl get pods -n "${NAMESPACE}" | grep -E "${1}" > /dev/null 2>&1
	result=$?
	while ! [ $result -eq 0 ];do
		echo -n '.'
		sleep ${POLLING_INTERVAL}
   	startup_duration=`expr $startup_duration + $POLLING_INTERVAL`
		kubectl get pods -n "${NAMESPACE}" | grep -E "${1}" > /dev/null 2>&1
		result=$?
	done
	echo " Done (${startup_duration}s)"
	DURATION=`expr $DURATION + $startup_duration`
}

kubectl -n ${NAMESPACE} scale --replicas=0 deployment/takserver-api
echo -n Waiting for takserver-api to shut down.
wait_for_shutdown "${API_REGEX}"
kubectl -n ${NAMESPACE} scale --replicas=0 deployment/takserver-messaging
echo -n Waiting for takserver-messaging to shut down.
wait_for_shutdown "${MESSAGING_REGEX}"
kubectl -n ${NAMESPACE} scale --replicas=0 deployment/takserver-config
echo -n Waiting for takserver-config to shut down.
wait_for_shutdown "${CONFIG_REGEX}"
kubectl -n ${NAMESPACE} scale --replicas=0 statefulset/takserver-ignite
echo -n Waiting for takserver-ignite to shut down.
wait_for_shutdown "${IGNITE_REGEX}"
kubectl -n ${NAMESPACE} scale --replicas=0 statefulset/takserver-nats
echo -n Waiting for takserver-nats to shut down.
wait_for_shutdown "${NATS_REGEX}"

kubectl -n ${NAMESPACE} scale --replicas=3 statefulset/takserver-nats
echo -n Waiting for takserver-nats to start up.
wait_for_startup "${NATS_REGEX}"
kubectl -n ${NAMESPACE} scale --replicas=2 statefulset/takserver-ignite
echo -n Waiting for takserver-ignite to start up.
wait_for_startup "${IGNITE_REGEX}"
kubectl -n ${NAMESPACE} scale --replicas=2 deployment/takserver-config
echo -n Waiting for takserver-config to start up.
wait_for_startup "${CONFIG_REGEX}"
kubectl -n ${NAMESPACE} scale --replicas=2 deployment/takserver-messaging
echo -n Waiting for takserver-messaging to start up.
wait_for_startup "${MESSAGING_REGEX}"
kubectl -n ${NAMESPACE} scale --replicas=2 deployment/takserver-api
echo -n Waiting for takserver-api to start up.
wait_for_startup "${API_REGEX}"

echo "Total restart duration: ${DURATION} seconds."
