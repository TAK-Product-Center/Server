#!/usr/bin/env bash

set -e

EXTERNAL_IP=192.168.11.38
EXTERNAL_IP=128.33.66.208
MK_DRIVER=kvm2 # or docker, kvm2, virtualbox, qemu, etc. See https://minikube.sigs.k8s.io/docs/drivers/ 
MK_CPU_COUNT=16
MK_MEMORY=20g
DOCKER_REGISTRY_PORT=4000
ENABLE_INGRESS=true

if [[ "${TAKCL_SERVER_POSTGRES_PASSWORD}" == "" ]];then
	echo Please set the environment variable TAKCL_SERVER_POSTGRES_PASSWORD to a password to use this script!
	exit 1
fi

if [[ "${TAKSERVER_CERT_SOURCE}" == "" ]];then
	echo Please provide a TAKSERVER_CERT_SOURCE!
	exit 1
fi

if [[ ! -f "${TAKSERVER_CERT_SOURCE}/admin.pem" ]];then
	echo The TAKSERVER_CERT_SOURCE must contain an admin.pem file!
	exit 1
fi

DOCKER_REGISTRY=${EXTERNAL_IP}:${DOCKER_REGISTRY_PORT}

HELM_VERSION=v3.12.3
MINIKUBE_VERSION=v1.31.2
K8S_VERSION=v1.27.0

SCRIPT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]:-$0}"; )" > /dev/null 2>&1 && pwd 2> /dev/null; )"

DB_IDENTIFIER=takserver-cluster-db
POSTGRES_USER=postgres
POSTGRES_DB=cot
POSTGRES_PORT=5432

CORE_CONFIG_TARGET_PATH=${SCRIPT_DIR}/CoreConfig.xml
HELM_TEMPLATE=${SCRIPT_DIR}/minikube-cluster-template.yaml

HELM_CONF=${SCRIPT_DIR}/minikube-values.yaml
BINDIR=${SCRIPT_DIR}/setup-bins
PUBLISH_REPO=${EXTERNAL_IP}:${DOCKER_REGISTRY_PORT}/takserver-cluster

extract_zip() {
	if [[ ! -f ${1} ]];then
		echo The specified file "${1}" does not exist!
		exit 1
	fi

	TAG="${1#*-}"
	TAG="${TAG#*-}"
	TAG="${TAG%%.zip}"

	if [[ ! -d ${TAG} ]];then
		unzip ${1} -d ${TAG}
	fi   
	CLUSTER_DIR=${SCRIPT_DIR}/${TAG}/cluster
}

restart_db() {
	if [[ "`docker ps -a | grep ${DB_IDENTIFIER}`" != "" ]];then
		echo Killing current database...
		docker stop ${DB_IDENTIFIER} || true
		sleep 4
	fi

	echo Setting up database...
	docker run -it -d --rm --name ${DB_IDENTIFIER} \
		--env POSTGRES_PASSWORD=${TAKCL_SERVER_POSTGRES_PASSWORD} \
		--env POSTGRES_HOST_AUTH_METHOD=trust \
		--env POSTGRES_USER=${POSTGRES_USER} \
		--env POSTGRES_DB=${POSTGRES_DB} \
		-p 5432:5432 \
		postgis/postgis:15-3.3

	sleep 8

	DOCKER_IP=`docker inspect --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' ${DB_IDENTIFIER}`
	java -jar ${CLUSTER_DIR}/takserver-schemamanager/SchemaManager.jar -url jdbc:postgresql://${DOCKER_IP}:5432/cot -user ${POSTGRES_USER} -password ${TAKCL_SERVER_POSTGRES_PASSWORD} upgrade
}

get_dependencies() {
	# I should be able to just use "minikube kubectl", but loading configmaps with --from-file doesn't seem to work that way
	if [[ ! -d "${BINDIR}" ]];then
		mkdir "${BINDIR}"
	fi

	KUBECTL=${BINDIR}/kubectl-${K8S_VERSION}
	KUBECTL_CONVERT=${BINDIR}/kubectl-convert-${K8S_VERSION}
	HELM=${BINDIR}/helm-${HELM_VERSION}
	MINIKUBE=${BINDIR}/minikube-${MINIKUBE_VERSION}

	if [[ ! -f ${KUBECTL} ]];then
		wget https://storage.googleapis.com/kubernetes-release/release/${K8S_VERSION}/bin/linux/amd64/kubectl -O ${KUBECTL}
		chmod +x ${KUBECTL}
	fi

	if [[ ! -f ${KUBECTL_CONVERT} ]];then
		wget https://dl.k8s.io/release/${K8S_VERSION}/bin/linux/amd64/kubectl-convert -O ${KUBECTL_CONVERT}
		chmod +x ${KUBECTL_CONVERT}
	fi

	if [[ ! -f ${HELM} ]];then
		zip=helm-${HELM_VERSION}-linux-amd64.tar.gz
		wget https://get.helm.sh/${zip} -O ${BINDIR}/helm.tar.gz
		tar -xzvf ${BINDIR}/helm.tar.gz -C ${BINDIR}/
		mv ${BINDIR}/linux-amd64/helm ${HELM}
		chmod +x ${HELM}
		rm -r ${BINDIR}/linux-amd64 ${BINDIR}/helm.tar.gz
	fi

	if [[ ! -f ${MINIKUBE} ]];then
		wget https://github.com/kubernetes/minikube/releases/download/${MINIKUBE_VERSION}/minikube-linux-amd64 -O ${MINIKUBE}
		chmod +x ${MINIKUBE}
	fi

	rm -f ${BINDIR}/kubectl
	ln -s ${KUBECTL} ${BINDIR}/kubectl
	rm -f ${BINDIR}/kubectl-convert
	ln -s ${KUBECTL_CONVERT} ${BINDIR}/kubectl-convert
	rm -f ${BINDIR}/helm
	ln -s ${HELM} ${BINDIR}/helm
	rm -f ${BINDIR}/minikube
	ln -s ${MINIKUBE} ${BINDIR}/minikube
}

build_docker() {
	pushd ${CLUSTER_DIR}
	#### Make sure kubectl uses the local docker instance settings
	#eval $(minikube docker-env)

	docker build -t ${PUBLISH_REPO}/takserver-base:${TAG} -f docker-files/Dockerfile.takserver-base .
	docker push ${PUBLISH_REPO}/takserver-base:${TAG}

	docker build -t ${PUBLISH_REPO}/takserver-config:${TAG} -f docker-files/Dockerfile.takserver-config --build-arg TAKSERVER_IMAGE_REPO=${PUBLISH_REPO}/takserver-base --build-arg TAKSERVER_IMAGE_TAG=${TAG} .
	docker push ${PUBLISH_REPO}/takserver-config:${TAG}

	docker build -t ${PUBLISH_REPO}/takserver-messaging:${TAG} -f docker-files/Dockerfile.takserver-messaging --build-arg TAKSERVER_IMAGE_REPO=${PUBLISH_REPO}/takserver-base --build-arg TAKSERVER_IMAGE_TAG=${TAG} .
	docker push ${PUBLISH_REPO}/takserver-messaging:${TAG}

	docker build  -t ${PUBLISH_REPO}/takserver-api:${TAG} -f docker-files/Dockerfile.takserver-api --build-arg TAKSERVER_IMAGE_REPO=${PUBLISH_REPO}/takserver-base --build-arg TAKSERVER_IMAGE_TAG=${TAG} .
	docker push ${PUBLISH_REPO}/takserver-api:${TAG}

	docker build -t ${PUBLISH_REPO}/takserver-plugins:${TAG} -f docker-files/Dockerfile.takserver-plugins --build-arg TAKSERVER_IMAGE_REPO=${PUBLISH_REPO}/takserver-base --build-arg TAKSERVER_IMAGE_TAG=${TAG} .
	docker push ${PUBLISH_REPO}/takserver-plugins:${TAG}

	docker build -t ${PUBLISH_REPO}/takserver-database-setup:${TAG} -f docker-files/Dockerfile.database-setup .
	docker push ${PUBLISH_REPO}/takserver-database-setup:${TAG}

	#docker build  -t ${PUBLISH_REPO}/takserver-integrationtests:${TAG} -f docker-files/Dockerfile.takserver-integrationtests --build-arg TAKSERVER_IMAGE_REPO=${PUBLISH_REPO}/takserver-base --build-arg TAKSERVER_IMAGE_TAG=${TAG} .
	#docker push ${PUBLISH_REPO}/takserver-integrationtests:${TAG}
	popd
}

update_core_config() {
	pushd ${CLUSTER_DIR}
	if [[ ! -f CoreConfig.default.xml ]];then
		cp CoreConfig.xml CoreConfig.default.xml
		cp TAKIgniteConfig.xml TAKIgniteConfig.default.xml
	fi

	sed "s/DB_URL_PLACEHOLDER/jdbc:postgresql:\/\/${EXTERNAL_IP}:5432\/cot/g" CoreConfig.default.xml > CoreConfig.xml
	sed -i "s/DB_USERNAME_PLACEHOLDER/${POSTGRES_USER}/g" CoreConfig.xml
	sed -i "s/DB_PASSWORD_PLACEHOLDER/${TAKCL_SERVER_POSTGRES_PASSWORD}/g" CoreConfig.xml

	popd
}

deploy_local() {
	pushd ${CLUSTER_DIR}

	restart_db

	sed "s/LOCAL_TAG/${TAG}/g" ${HELM_TEMPLATE} > ${HELM_CONF}
	sed -i "s/LOCAL_REPO/${DOCKER_REGISTRY}/g" ${HELM_CONF}

	# Start up a new minikube container
	${MINIKUBE} stop || true
	${MINIKUBE} delete || true
	${MINIKUBE} start --memory=${MK_MEMORY} --cpus=${MK_CPU_COUNT} --kubernetes-version=${K8S_VERSION} --insecure-registry=${EXTERNAL_IP}:${DOCKER_REGISTRY_PORT} --driver=${MK_DRIVER} --apiserver-port 9210
	if [[ "${ENABLE_INGRESS}" == "true" ]];then
		${MINIKUBE} addons enable ingress
		sleep 5
		${KUBECTL} patch deployment -n ingress-nginx ingress-nginx-controller --type='json' -p='[{"op": "add", "path": "/spec/template/spec/containers/0/args/-", "value":"--enable-ssl-passthrough"}]'  
		sleep 5
	fi

	# Crate the custom certificate store. Make sure admin.pem exists to ensure the admin user is activated and startup completes successfully!
	${KUBECTL} create configmap cert-migration --from-file=${TAKSERVER_CERT_SOURCE} --dry-run=client -o yaml > deployments/helm/templates/cert-migration.yaml

	update_core_config

	${KUBECTL} create configmap tak-ignite-config --from-file=${CLUSTER_DIR}/TAKIgniteConfig.xml --dry-run=client -o yaml > deployments/helm/templates/tak-ignite-config.yaml
	${KUBECTL} create configmap core-config --from-file=${CLUSTER_DIR}/CoreConfig.xml --dry-run=client -o yaml > deployments/helm/templates/core-config.yaml
	if [[ -f readiness.py ]];then
		${KUBECTL} create configmap readiness-config --from-file=readiness.py --dry-run=client -o yaml > deployments/helm/templates/readiness-config.yaml
	fi

	cd deployments/helm
	${HELM} dep update

#	pushd ${CLUSTER_DIR}/deployments/ingress-infrastructure/
#	${KUBECTL} config set-context $(${KUBECTL} config current-context) --namespace=takserver && ${KUBECTL} apply --validate=false -f ingress-setup.yaml
#	popd

	${HELM} upgrade --install takserver -n=takserver --create-namespace ./ -f ${HELM_CONF}
	popd
}

delete_local_images() {
	if [[ "${1}" == "" ]];then
		echo Please provide the root IP/Host as a parameter!
		exit 1
	fi
	IP=${1}

	if [[ "${2}" == "" ]];then
		echo Please provide the tag to remove!
		exit 1
	fi
	TAG=${2}

	PORT=:${DOCKER_REGISTRY_PORT}

	IMAGE_SIGS='
	/takserver-cluster/takserver-database-setup
	/takserver-cluster/takserver-plugins
	/takserver-cluster/takserver-api
	/takserver-cluster/takserver-messaging
	/takserver-cluster/takserver-integrationtests
	/takserver-cluster/takserver-base
	'

	for image in ${IMAGE_SIGS};do
		echo Removing ${IP}${PORT}${image}:${TAG}
		docker rmi ${IP}${PORT}${image}:${TAG}
	done
}


setup_ingress() {
	if [[ "${ENABLE_INGRESS}" == "true" ]];then
		echo Waiting 10 seconds before enabling ingress...
		sleep 10
		${KUBECTL} apply -f minikube-ingress-loadbalancer.yaml
		${KUBECTL} patch deployment ingress-nginx-controller -n ingress-nginx --patch "$(cat minikube-ingress-patch.yaml)"
		${MINIKUBE} service list
	fi
}

if [[ "${1}" == "--remove-local-images" ]];then
	delete_local_images ${2} ${3}
	exit 0
elif [[ "${1}" == "--destroy" ]];then
	get_dependencies
	${MINIKUBE} stop || true
	${MINIKUBE} delete || true
	exit 0
fi

extract_zip ${1}
update_core_config
build_docker
get_dependencies
deploy_local
setup_ingress
