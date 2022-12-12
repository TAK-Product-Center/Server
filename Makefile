ifndef REGISTRY
	REGISTRY := localhost:58030
endif

ifndef TAG
	TAG := latest
endif

cluster:
	-k3d registry create tak-registry --port 58030
	k3d cluster create tak \
		-p "30000-30010:30000-30010@server:0" \
		--k3s-arg '--disable=traefik@server:*' \
		--k3s-arg '--disable=metrics-server@server:*' \
		--registry-use k3d-tak-registry:58030 --wait

push-docker:
	docker push $(REGISTRY)/takserver-base:$(TAG)
	docker push $(REGISTRY)/takserver-messaging:$(TAG)
	docker push $(REGISTRY)/takserver-api:$(TAG)
	docker push $(REGISTRY)/takserver-plugins:$(TAG)
	docker push $(REGISTRY)/takserver-database-setup:$(TAG)

.ONESHELL:

compile:
	export STATE=MA
	export CITY=Cambridge
	export ORGANIZATIONAL_UNIT=takorg
	export CA_NAME=dummy
	cd src
	./gradlew clean buildHelmCluster

build-docker:
	cd src/takserver-cluster/build
	docker build -t $(REGISTRY)/takserver-base:$(TAG) -f docker-files/Dockerfile.takserver-base .
	docker build -t $(REGISTRY)/takserver-messaging:$(TAG) -f docker-files/Dockerfile.takserver-messaging --build-arg TAKSERVER_IMAGE_REPO=$(REGISTRY)/takserver-base --build-arg TAKSERVER_IMAGE_TAG=$(TAG) .
	docker build -t $(REGISTRY)/takserver-api:$(TAG) -f docker-files/Dockerfile.takserver-api --build-arg TAKSERVER_IMAGE_REPO=$(REGISTRY)/takserver-base --build-arg TAKSERVER_IMAGE_TAG=$(TAG) .
	docker build -t $(REGISTRY)/takserver-plugins:$(TAG) -f docker-files/Dockerfile.takserver-plugins --build-arg TAKSERVER_IMAGE_REPO=$(REGISTRY)/takserver-base --build-arg TAKSERVER_IMAGE_TAG=$(TAG) .
	docker build -t $(REGISTRY)/takserver-database-setup:$(TAG) -f docker-files/Dockerfile.database-setup .

build-helm:
	cd src/takserver-cluster/deployments/helm
	helm dependency build

cert:
	-kubectl create ns takserver
	kubectl apply -f ./config/cert.yaml -n takserver

deploy:
	cd src/takserver-cluster/deployments/helm
	helm upgrade takserver . --install -n=takserver --create-namespace \
		-f ./developer-values.yaml \
		--set takserver.messaging.image.repository=$(REGISTRY)/takserver-messaging \
		--set takserver.api.image.repository=$(REGISTRY)/takserver-api \
		--set takserver.plugins.image.repository=$(REGISTRY)/takserver-plugins \
		--set takserver.takserverDatabaseSetup.image.repository=$(REGISTRY)/takserver-database-setup \
		--set takserver.messaging.image.tag=$(TAG) \
		--set takserver.api.image.tag=$(TAG) \
		--set takserver.plugins.image.tag=$(TAG) \
		--set takserver.takserverDatabaseSetup.image.tag=$(TAG)

expose:
	kubectl apply -f ./config/svc.yaml -n takserver
