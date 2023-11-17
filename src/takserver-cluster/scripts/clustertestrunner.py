#!/usr/bin/env python3

import argparse
import os
import subprocess
import time

FILEPATH = os.path.abspath(os.path.join(os.path.dirname(os.path.realpath(__file__)), 'clustertestrunner.py'))
CLUSTER_ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.realpath(__file__)), os.path.pardir))
HELM_TAKSERVER_READINESS_FILEPATH = os.path.join(CLUSTER_ROOT, 'deployments/helm/templates/readiness-config.yaml')
HELM_TAKSERVER_CORE_FILEPATH = os.path.join(CLUSTER_ROOT, 'deployments/helm/templates/takserver-core-deployment.yaml')
HELM_TAKSERVER_INTEGRATIONTEST_FILEPATH = os.path.join(
    CLUSTER_ROOT, 'deployments/helm/templates/takserver-integrationtests-deployment.yaml')
HELM_TAKSERVER_PRODUCTIONVALUES_FILEPATH = os.path.join(CLUSTER_ROOT, 'deployments/helm/production-values.yaml')
HELM_INTEGRATIONTEST_DOCKERFILEPATH = os.path.join(CLUSTER_ROOT, 'docker-files/Dockerfile.takserver-integrationtests')
DEPLOYMENT_TAK_ROOT = '/'


class ReadinessProbe:
    def __init__(self, identifier, log_path, log_watch_values):
        self._identifier = identifier
        self._log_path = log_path
        self._log_watch_values = log_watch_values

    @property
    def identifier(self):
        return self._identifier

    def wait_for_ready(self, log_root, timeout):
        interval = 1
        duration = 0

        log_values = list(self._log_watch_values)

        log_path = os.path.join(log_root, self._log_path)

        if os.path.exists(log_path):
            log = open(log_path)
        else:
            log = None

        if timeout == 0:
            duration = -1

        while duration < timeout and len(log_values) > 0:
            if log is None:
                if os.path.exists(log_path):
                    log = open(log_path)
            else:
                line = log.readline()

                while line != '':
                    for candidate in list(log_values):
                        if candidate in line:
                            log_values.remove(candidate)

                            if len(log_values) == 0:
                                return

                    line = log.readline()

            time.sleep(interval)
            duration = duration + interval

        if len(log_values) > 0:
            for line in log_values:
                print(line)
            exit(1)


probe_dict = {
    'plugins': ReadinessProbe('plugins', 'logs/takserver-plugins.log', [
        't.s.p.s.DistributedPluginManager - execute method DistributedPluginManager',
        't.s.plugins.service.PluginService - Started PluginService'
    ]),
    'retention': ReadinessProbe('retention', 'logs/takserver-retention.log', [
        't.s.r.c.DistributedRetentionPolicyConfig -  execute method DistributedRetentionPolicyConfig',
        't.s.retention.RetentionApplication - Started RetentionApplication'
    ]),
    'messaging': ReadinessProbe('messaging', 'logs/takserver-messaging.log', [
        #        'c.b.m.s.DistributedSubscriptionManager - DistributedSubscriptionManager execute',
        'c.b.m.s.DistributedConfiguration - execute method DistributedConfiguration',
        't.s.f.DistributedFederationManager - execute method DistributedFederationManager',
        #        'c.b.m.g.DistributedPersistentGroupManager - execute method DistributedPersistentGroupManager',
        't.s.profile.DistributedServerInfo - execute method DistributedServerInfo',
        'c.b.m.s.DistributedContactManager - execute method DistributedContactManager',
        'c.b.m.r.DistributedRepeaterManager - execute method DistributedRepeaterManager',
        'c.b.m.groups.DistributedUserManager - DistributedUserManager execute',
        't.s.config.DistributedSystemInfoApi - execute method DistributedSystemInfoApi',
        #        't.s.cluster.DistributedInputManager - execute method DistributedInputManager',
        't.s.c.DistributedSecurityManager - execute method DistributedSecurityManager',
        'c.b.m.DistributedMetricsCollector - execute method DistributedMetricsCollector',
        't.s.c.DistributedInjectionService - execute method DistributedInjectionService',
        't.s.m.DistributedPluginDataFeedApi - execute method DistributedPluginDataFeedApi',
        't.s.messaging.DistributedPluginApi - execute method DistributedPluginApi',
        't.s.m.DistributedPluginSelfStopApi - execute method DistributedPluginSelfStopApi',
        'c.b.m.service.MessagingInitializer - takserver-core init complete'
    ]),
    'api': ReadinessProbe('api', 'logs/takserver-api.log', [
        #        'c.b.m.s.DistributedFederationHttpConnectorManager - execute method DistributedFederationHttpConnectorManager',
        #        'c.b.m.s.DistributedRetentionQueryManager - execute method DistributedRetentionQueryManager',
        #        't.s.api.DistributedPluginMissionApi - execute method DistributedPluginMissionApi',
        'o.s.b.w.e.tomcat.TomcatWebServer - Tomcat started'
    ])

}


class HelmModifier:

    @staticmethod
    def _update_takserver_core_template():
        import yaml

        def update_container(label, parent_container):

            def get_probe(process_label):
                return {
                    'exec': {
                        'command': [
                            'python',
                            '/clustertestrunner.py',
                            'readiness',
                            process_label,
                            '0'
                        ]
                    },
                    'initialDelaySeconds': 20,
                    'periodSeconds': 5,
                    'failureThreshold': 48
                }

            parent_container['readinessProbe'] = get_probe(label)
            parent_container['livenessProbe'] = get_probe(label)

            parent_container['volumeMounts'].append({
                'name': 'readiness-config',
                'mountPath': '/clustertestrunner.py',
                'subPath': 'clustertestrunner.py'
            })

        # The parser has problems reading the templated values, so change them for now. This could probably be done better.
        yaml_contents = open(HELM_TAKSERVER_CORE_FILEPATH).read()
        yaml_contents = yaml_contents.replace('{{ .Values.takserver.messaging.replicas }}', '1').replace(
            '{{ .Values.takserver.api.replicas }}', '1')
        open(HELM_TAKSERVER_CORE_FILEPATH, 'w').write(yaml_contents)

        with open(HELM_TAKSERVER_CORE_FILEPATH) as f:
            docs = yaml.load_all(f, Loader=yaml.FullLoader)

            core_docs = list()
            for doc in docs:
                core_docs.append(doc)
                doc['spec']['template']['spec']['volumes'].append({
                    'name': 'readiness-config',
                    'configMap': {
                        'name': '{{ .Values.readinessConfigMapName }}'
                    }
                })

                containers = doc['spec']['template']['spec']['containers']
                for container in containers:
                    if container['name'] == 'takserver-messaging':
                        update_container('messaging', container)
                        if 'env' not in container:
                            container['env'] = list()
                        container['env'].append({
                            'name': 'JDK_JAVA_OPTIONS',
                            'value': '-Dlogging.level.com.bbn=DEBUG -Dlogging.level.tak=DEBUG'
                        })

                    elif container['name'] == 'takserver-api':
                        update_container('api', container)
                        if 'env' not in container:
                            container['env'] = list()
                        container['env'].append({
                            'name': 'JDK_JAVA_OPTIONS',
                            'value': '-Dlogging.level.com.bbn=DEBUG -Dlogging.level.tak=DEBUG'
                        })

        yaml.dump_all(core_docs, open(HELM_TAKSERVER_CORE_FILEPATH, 'w'), default_flow_style=False)

    @staticmethod
    def _create_integrationtests_deployment():
        import yaml
        test_docs = list()

        # Add the service account configuration
        test_docs.append({
            'apiVersion': 'v1',
            'kind': 'ServiceAccount',
            'metadata': {
                'name': 'takcl',
                'namespace': 'takserver'
            }
        })

        # Add the cluster role binding
        test_docs.append({
            'apiVersion': 'rbac.authorization.k8s.io/v1',
            'kind': 'ClusterRoleBinding',
            'metadata': {
                'name': 'takcl',
                'namespace': 'takserver'
            },
            'roleRef': {
                'apiGroup': 'rbac.authorization.k8s.io',
                'kind': 'ClusterRole',
                'name': 'cluster-admin'
            },
            'subjects': [
                {
                    'kind': 'ServiceAccount',
                    'name': 'takcl',
                    'namespace': 'takserver'
                }
            ]
        })

        # Add the deployment configuration
        test_docs.append({
            'apiVersion': 'apps/v1',
            'kind': 'Deployment',
            'metadata': {
                'name': 'takserver-integrationtests',
            },
            'spec': {
                'selector': {
                    'matchLabels': {
                        'app': 'takserver-integrationtests'
                    }
                },
                'template': {
                    'metadata': {
                        'labels': {
                            'app': 'takserver-integrationtests'
                        }
                    },
                    'spec': {
                        'serviceAccountName': 'takcl',
                        'containers': [
                            {
                                'name': 'takserver-integrationtests',
                                'image': '{{ .Values.takserver.integrationtests.image.repository }}:{{ .Values.takserver.integrationtests.image.tag }}',
                                'resources': {
                                    'requests': {
                                        'cpu': 2,
                                        'memory': '2Gi'
                                    },
                                    'limits': {
                                        'cpu': 2,
                                        'memory': '2Gi'
                                    }
                                }
                            }
                        ],
                        'imagePullSecrets': [
                            {
                                'name': '{{ .Values.imagePullSecret }}'
                            }

                        ]
                    }
                }
            }
        })

        # Write it to disk
        yaml.dump_all(test_docs, open(HELM_TAKSERVER_INTEGRATIONTEST_FILEPATH, 'w'), default_flow_style=False)

    @staticmethod
    def _update_production_values():
        import yaml

        doc = yaml.load(open(HELM_TAKSERVER_PRODUCTIONVALUES_FILEPATH), Loader=yaml.FullLoader)
        doc['readinessConfigMapName'] = 'readiness-config'
        doc['takserver']['integrationtests'] = {
            'image': {
                'repository': 'docker-devtest-local.artifacts.tak.gov/takserver-cluster/takserver-integrationtests',
                'tag': 'integrationtests-provisioned'
            },
            'replicas': 1,
            'resources': {
                'requests': {
                    'cpu': 2,
                    'memory': '2Gi'
                },
                'limits': {
                    'cpu': 2,
                    'memory': '2Gi'
                }
            }
        }
        doc['takserver']['messaging']['replicas'] = 1
        doc['takserver']['api']['replicas'] = 1

        yaml.dump(doc, open(HELM_TAKSERVER_PRODUCTIONVALUES_FILEPATH, 'w'), default_flow_style=False)

    @staticmethod
    def _create_docker_file():
        open(HELM_INTEGRATIONTEST_DOCKERFILEPATH, 'w').write(
            '''FROM openjdk:11-jdk-stretch
RUN apt update && apt install -y vim python3
COPY CoreConfig.xml /opt/tak/CoreConfig.example.xml
COPY scripts/clustertestrunner.py /clustertestrunner.py
COPY takserver-core/certs/* /opt/tak/certs/
COPY takserver-takcl-core/takcl.jar takcl.jar
RUN chmod +x takcl.jar
CMD ["/bin/sleep", "28800"]'''
        )

    @staticmethod
    def _create_readiness_file():
        configmap_contents = subprocess.check_output(['kubectl', 'create', 'configmap', 'readiness-config',
                                                      '--from-file=' + FILEPATH, '--dry-run=client', '-o', 'yaml'],
                                                     stderr=subprocess.STDOUT).decode()
        open(HELM_TAKSERVER_READINESS_FILEPATH, 'w').write(configmap_contents)

    @staticmethod
    def enable_integration_testing():
        # Upgrade the takserver-core helm template
        HelmModifier._update_takserver_core_template()

        # Create the integration test runner helm template
        HelmModifier._create_integrationtests_deployment()

        # Update the deployment values configuration
        HelmModifier._update_production_values()

        # Create the Dockerfile
        HelmModifier._create_docker_file()

        # Create the readiness checking file (based on this file)
        HelmModifier._create_readiness_file()


parser = argparse.ArgumentParser()
subparsers = parser.add_subparsers(dest='mode')

readiness_parser = subparsers.add_parser('readiness')
readiness_parser.add_argument('process_identifier', metavar='PROCESS_IDENTIFIER', type=str,
                              choices=probe_dict.keys())
readiness_parser.add_argument('timeout', metavar='TIMEOUT', type=int)

prep_parser = subparsers.add_parser('prep')


def main():
    args = parser.parse_args()

    if args.mode == 'readiness':
        probe_dict[args.process_identifier].wait_for_ready(DEPLOYMENT_TAK_ROOT, args.timeout)

    elif args.mode == 'prep':
        HelmModifier.enable_integration_testing()

    else:
        parser.print_help()


if __name__ == '__main__':
    main()
