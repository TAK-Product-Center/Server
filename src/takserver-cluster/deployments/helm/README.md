# TAK Server

## Introduction

This chart bootstraps a [TAK Server](https://git.tak.gov/core/takserver/-/blob/master/README.md) deployment on a [Kubernetes](http://kubernetes.io) cluster using the [Helm](https://helm.sh) package manager. The deployment also pulls in the following helm charts as dependencies:
- [NATS] https://nats-io.github.io/k8s/
- [STAN] https://nats-io.github.io/k8s/
- [Postgres] https://github.com/bitnami/charts/tree/master/bitnami/postgresql/#installing-the-chart
- [Ignite] https://github.com/helm/charts/tree/master/stable/ignite

## Prerequisites

- Kubernetes 1.12+
- Helm 3.1.0
- PV provisioner support in the underlying infrastructure

## Installing the Chart
To install the chart or upgrade an existing deployment with the release name `takserver` and the recommended default values:

```console
$ cd src/takserver-cluster/deployments/helm
$ helm upgrade --install takserver -n=takserver --create-namespace ./ -f ./production-values.yaml
```
> Note: Replace the production-values.yaml in the above command with `developer-values.yaml` when running in a local cluster
> to limit resource allocations.

> Note: you need to substitute the values _[POSTGRESQL_PASSWORD]_, and _[REPLICATION_PASSWORD]_ if you
> do not use one of the included default `values.yaml` files to upgrade an existing deployment.


The [Parameters](#parameters) section lists the parameters that can be configured during installation. This deployment **must** be placed into a Kubernetes namespace with the identifier "takserver". 

> **Tip**: List all releases using `helm list`

## Uninstalling the Chart

To uninstall/delete the `takserver` deployment:

```console
$ helm uninstall -n takserver takserver 
```

The command removes all the Kubernetes components but PVC's associated with the chart and deletes the release.

To delete the PVC's associated with `takserver`:

```console
$ kubectl delete pvc -n takserver -l release=takserver
```

> **Note**: Deleting the PVC's will delete postgresql data as well. Please be cautious before doing it.

## Parameters

### TAKServer default parameters

| Name 	| Description 	| Value 	|
|---	|---	|---	|
| imagePullSecret 	| Identifies the Kubernetes authentication secret that can be used to pull docker  images from an external private registry. 	| reg-cred 	|
| certConfigMapName 	| Identifies the Kubernetes ConfigMap that can be used to replace the default certs  for authenticating with TAKServer.  To create a replacement configmap, generate new certifactes of authority by running  the script `src/takserver-core/scripts/certs/generateClusterCerts.sh` and then  running the following commands: <br> • kubectl create configmap cert-migration --from-file="path to pre-generated cluster  certs" --dry-run=client -o yaml >cert-migration-replacement.yaml <br> • kubectl create -f cert-migration-replacement.yaml -n takserver 	| cert-migration 	|
| takserver.ingress.enabled 	| Configures whether the ingress will be included in or excluded from the deployment.  This ingress is primarily intended to support developers and should not be used  for production. 	| false 	|
| takserver.ingress.api.cert.annotations 	| Declares annotations that will be appended to the ingress. 	|  	|
| takserver.ingress.api.cert.host 	| Declares an Ingress rule that specifies which host's traffic should be redirected into the cluster. 	|  	|
| takserver.ingress.api.federationTruststore.annotations 	| Declares annotations that will be appended to the ingress. 	|  	|
| takserver.ingress.api.federationTruststore.host 	| Declares an Ingress rule that specifies which host's traffic should be redirected into the cluster. 	|  	|
| takserver.ingress.api.https.annotations 	| Declares annotations that will be appended to the ingress. 	|  	|
| takserver.ingress.api.https.host 	| Declares an Ingress rule that specifies which host's traffic should be redirected into the cluster. 	|  	|
| takserver.ingress.messaging.streaming.annotations 	| Declares annotations that will be appended to the ingress. 	|  	|
| takserver.ingress.messaging.streaming.host 	| Declares an Ingress rule that specifies which host's traffic should be redirected into the cluster. 	|  	|
| takserver.ingress.messaging.fedv1.annotations 	| Declares annotations that will be appended to the ingress. 	|  	|
| takserver.ingress.messaging.fedv1.host 	| Declares an Ingress rule that specifies which host's traffic should be redirected into the cluster. 	|  	|
| takserver.ingress.messaging.fedv2.annotations 	| Declares annotations that will be appended to the ingress. 	|  	|
| takserver.ingress.messaging.fedv2.host 	| Declares an Ingress rule that specifies which host's traffic should be redirected into the cluster. 	|  	|
| takserver.messaging.image.repository 	| The docker registry identifier that the messaging container image can be pulled from. 	| docker-devtest-local.artifacts.tak.gov/takserver-cluster/takserver-messaging 	|
| takserver.messaging.image.tag 	| The unique identifier of the docker image to pull. 	| messaging-provisioned 	|
| takserver.api.image.repository 	| The docker registry identifier that the api container image can be pulled from. 	| docker-devtest-local.artifacts.tak.gov/takserver-cluster/takserver-api 	|
| takserver.api.image.tag 	| The unique identifier of the docker image to pull. 	| api-provisioned 	|
| takserver.plugins.image.repository 	| The docker registry identifier that the plugins container image can be pulled from. 	| docker-devtest-local.artifacts.tak.gov/takserver-cluster/takserver-plugins 	|
| takserver.plugins.image.tag 	| The unique identifier of the docker image to pull. 	| plugins-provisioned 	|
| takserver.plugins.enabled 	| Flag to indicate whether plugins should be deployed to the cluster. 	| true 	|
| takserver.takserverDatabaseSetup.image.repository 	| The docker registry identifier that the databasesetup container image can be pulled from. 	| docker-devtest-local.artifacts.tak.gov/takserver-database-setup 	|
| takserver.takserverDatabaseSetup.image.tag 	| The unique identifier of the docker image to pull. 	| takserver-database-setup-provisioned 	|
| takserver.limits.persistentVolumeClaims 	|  The maximum number of persistent volume claims that can be allocated | 5
| takserver.limits.requests.storage | The size of the allocation for storing requests | 100Gi
| takserver.limits.storage.max | The maximum size of persistent volume claims | 50Gi
| takserver.limits.storage.min | The minimum size of persistent volume claims | 1Gi
| postgresql.image.registry 	| PostgreSQL image registry. The default value provided by Postgres must be overriden to  enable installation of postgis. 	| docker.io 	|
| postgresql.image.repository 	| PostgreSQL image repository. The default value provided by Postgres must be overriden to  enable installation of postgis. 	| postgis/postgis 	|
| postgresql.image.tag 	| PostgreSQL image tag (immutable tags are recommended). The default value provided by  Postgres must be overriden to enable installation of postgis. 	| 10-3.1 	|
| postgresql.postgresqlDatabase 	| PostgreSQL database that will be automatically created during the deployment process. 	| cot 	|
| postgresql.postgresql.postgresqlUsername 	| PostgreSQL user (has superuser privileges if username is postgres). 	| postgres 	|
| global.postgresql.postgresqlPassword 	| PostgreSQL user password. This must be a non-empty value. 	| postgres 	|
| ignite.image.tag 	| Image tag to declare which version of ignite to pull. 	| 2.11.0 	|
| ignite.image.serviceAccount.name 	| If ignite.serviceAccount.create is enabled, what should the serviceAccount name be  - otherwise randomly generated. 	| takserver-ignite 	|

To override a default value, specify the parameter using the `--set key=value[,key=value]` argument to `helm install`. For example,

```console
$ helm upgrade --install takserver -n takserver --create-namespace -f ./production-values.yaml \
  --set takserver.ingress.enabled=true \
    ./
```

### Dependency parameters
The following subcharts also provide their own set of parameters that can be overriden. Visit each link to see what parameters are available to override.
- [NATS] https://nats-io.github.io/k8s/
- [STAN] https://nats-io.github.io/k8s/
- [Postgres] https://github.com/bitnami/charts/tree/master/bitnami/postgresql/#installing-the-chart
- [Ignite] https://github.com/helm/charts/tree/master/stable/ignite

When overriding a parameter from a subchart specify the parameter using the `--set key=value[,key=value]` argument with the chart's name as a prefix on the key to `helm install`. For example, 

```console
$ helm upgrade --install takserver -n=takserver --create-namespace -f ./production-values.yaml \
  --set postgresql.shmVolume.enabled=false, postgresql.postgresqlDatabase=my-database \
    ./
```

## Troubleshooting

Find more information about how to deal with common errors related to Bitnami’s Helm charts in [this troubleshooting guide](https://docs.bitnami.com/general/how-to/troubleshoot-helm-chart-issues).
