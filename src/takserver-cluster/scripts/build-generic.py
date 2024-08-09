#!/usr/bin/env python3

import argparse
import datetime
import json
import os
import pathlib
import platform
import shutil
import stat
import subprocess
import sys
import time
import urllib.request
import xml.etree.ElementTree as ET
from enum import Enum
from typing import Optional, Union, List, Dict

import yaml

# URLs to fetch deployment dependencies
KUBCTL_URL = 'https://dl.k8s.io/release/{version}/bin/{os}/{arch}/kubectl'
MINIKUBE_URL = 'https://github.com/kubernetes/minikube/releases/download/{version}/minikube-{os}-{arch}'
HELM_URL = 'https://get.helm.sh/helm-{version}-{os}-{arch}.tar.gz'

# These values define how many of each node type will be instantiated for each api node with at least 1 being spun up
# TODO: Utilize ignite/nats scaling
TAK_MESSAGING_NODE_MULTIPLIER = 2
TAK_IGNITE_NODE_MULTIPLIER = 0.4
TAK_NATS_NODE_MULTIPLIER = 0.4
TAK_CONFIG_NODE_MULTIPLIER = 0.2
TAK_LOAD_BALANCER_NODE_MULTIPLIER = 0.2
TAK_API_NODE_MULTIPLIER = 1
TAK_PLUGIN_NODE_MULTIPLIER = 0
TAK_DATABASE_SETUP_MULTIPLIER = 0

# Required certificates. If the user provides their own certs and these aren't present deployment will halt
REQUIRED_CERTIFICATES = [
    'takserver.jks',
    'truststore-root.jks',
    'fed-truststore.jks',
    'admin.pem'
]

# If certs are not provided this container will be temporarily deployed locally to generate them
CA_CONTAINER_NAME = 'tak-ca-setup'

SCRIPT_DIR = os.path.dirname(os.path.realpath(__file__))
CLUSTER_ROOT = os.path.dirname(SCRIPT_DIR.rstrip('/'))

# The file where the command execution details will be logged to
COMMAND_LOG_FILE = os.path.join(CLUSTER_ROOT, os.path.realpath(__file__).split(os.sep)[-1]).rstrip('.py') + '.log'
COMMAND_LOG_FILE_RELATIVE_PATH = COMMAND_LOG_FILE.replace(CLUSTER_ROOT + '/', '')
DEFAULT_INGRESS_FILE = os.path.join(CLUSTER_ROOT, 'deployments/ingress-infrastructure/default-ingress-setup.yaml')

_parser = argparse.ArgumentParser('TAKServer AWS Cluster Deployer')


def halt_on_failure(msg: str, error_code: int = 1, silence_failure: bool = False):
    """
    Halts deployment with the provided message being made clearly visible
    :param msg: The message to display
    :param error_code: The error code to halt with. Defaults to 1.
    :param silence_failure: Do not display the error to the user.
    """
    if not silence_failure:
        print(f'\033[31;5mA FATAL ERROR OCCURRED!!\033[0m', file=sys.stderr)
        print(f'\033[31m{msg}\033[0m', file=sys.stderr)
    exit(error_code)


def parse_env_var(env_var: str, value_type: type, required: bool) -> Union[None, str, int]:
    """
    Convenience method for parsing env vars
    :param env_var: The env var tag
    :param value_type: The value's expected type
    :param required: Whether the env var is required
    """
    try:
        return value_type(os.environ[env_var])
    except KeyError:
        if required:
            halt_on_failure(f'Environment variable {env_var} must be set!')
        else:
            return None
    except ValueError:
        halt_on_failure(f'Environment variable {env_var} must be a {value_type.__name__}!')


def env_int_req(env_var: str) -> int:
    """
    Parses required integer env var
    :param env_var: The env var key
    :return: THe integer value
    """
    return parse_env_var(env_var, int, True)


def env_int_opt(env_var: str) -> Optional[int]:
    """
    Parses optional integer env var
    :param env_var: The env var key
    :return: The Integer value if present, or None if it is not
    """
    return parse_env_var(env_var, int, False)


def env_str_req(env_var: str) -> str:
    """
    Parses required str env var
    :param env_var: The env var key
    :return: The str value
    """
    return parse_env_var(env_var, str, True)


def env_bool_req(env_var: str) -> bool:
    """
    Parses required boolean env var
    :param env_var: The env var key
    :return: The boolean value
    """
    value_str = env_str_req(env_var).lower()
    if (value_str == 'true' or value_str == 'yes' or value_str == '1' or value_str == 't' or value_str == 'y'):
        return True
    if (value_str == 'false' or value_str == 'no' or value_str == '0' or value_str == 'f' or value_str == 'n'):
        return False
    halt_on_failure(f"Unexpected boolean value '{value_str}'! Expected true, false, yes, or no!")


def env_enum_req(env_var: str, enum_class: Enum):
    """
    Parses an enum value from an env var
    :param env_var: The env var key
    :param enum_class: The expected enum type
    :return: The enum value
    """
    value_str = env_str_req(env_var)
    label_dict = {target.value: target for target in list(enum_class)}
    if value_str in label_dict:
        return label_dict[value_str]
    else:
        halt_on_failure(
            f'Environment variable {env_var} must be a one of the following values: [' +
            ','.join(label_dict.keys()) + ']!')


def env_str_opt(env_var: str) -> Optional[str]:
    """
    Parses optional str env var
    :param env_var: The env var key
    :return: The str value if present, None if it is not
    """
    return parse_env_var(env_var, str, False)


def pretty_print_dict_list(dict_list: List[Dict], headers: List[str] = None):
    if headers is None:
        headers = sorted(dict_list[0].keys())

    column_lengths = dict()
    for header in headers:
        column_lengths[header] = len(header)

        for row in dict_list:
            column_lengths[header] = max(column_lengths[header], len(str(row[header])))

    header_columns = list()
    for header in headers:
        header_columns.append(header.ljust(column_lengths[header], ' '))
    rows = ['    '.join(header_columns)]

    for row_data in dict_list:
        row_columns = list()
        for header in headers:
            row_columns.append(str(row_data[header]).ljust(column_lengths[header], ' '))
        rows.append('    '.join(row_columns))

    for row in rows:
        print(row)


class DeploymentTarget(Enum):
    """
    The deployment targets
    """
    AWS = 'aws'
    RKE2 = 'generic-rke2'
    MINIKUBE = 'generic-minikube'

    @classmethod
    def from_label(cls, label: str) -> Union[None, 'DeploymentTarget']:
        label_dict = {target.value: target for target in set(DeploymentTarget)}
        return label_dict[label] if label in label_dict else None


class CertConfiguration:
    """
    An object to store the certificate configuration
    """

    def __init__(self):
        if 'TAK_CERT_SOURCE_DIR' in os.environ and os.environ['TAK_CERT_SOURCE_DIR'] != '':
            # If the cert dir is set use that
            self.src_dir = os.path.abspath(os.environ['TAK_CERT_SOURCE_DIR'])
            # Validate the directory exists
            if not os.path.exists(self.src_dir):
                halt_on_failure(f'Could not find TAK_CERT_SOURCE_DIR {self.src_dir}!')
            # Validate the admin cert exists
            if not os.path.exists(os.path.join(self.src_dir, 'admin.pem')):
                halt_on_failure(f'The TAK_CERT_SOURCE_DIR must contain an "admin.pem" cert for administration!')
            self.ca_name = None
            self.ca_state = None
            self.ca_city = None
            self.ca_organization = None
        else:
            # Otherwise we will need to generate the certificates
            self.src_dir = None
            self.ca_name = env_str_req('TAK_CA_NAME')
            self.ca_state = env_str_req('TAK_STATE')
            self.ca_city = env_str_req('TAK_CITY')
            self.ca_organization = env_str_req('TAK_ORGANIZATIONAL_UNIT')


class Configuration:
    """
    An object to store the configuration options
    """

    def __init__(self):
        self.deployment_target: DeploymentTarget = env_enum_req('TAK_DEPLOYMENT_TARGET', DeploymentTarget)
        self.tak_cluster_name = env_str_req('TAK_CLUSTER_NAME')
        self.tak_node_count = env_int_req('TAK_CLUSTER_NODE_COUNT')
        self.tak_enable_plugins = env_int_req('TAK_PLUGINS')
        self.tak_db_username = env_str_req('TAK_DB_USERNAME')
        self.tak_db_password = env_str_req('TAK_DB_PASSWORD')
        self.tak_cert_config = CertConfiguration()
        self.kubernetes_version = env_str_req('TAK_KUBERNETES_VERSION')
        self.kubernetes_namespace = env_str_req('TAK_KUBERNETES_NAMESPACE')
        self.helm_version = env_str_req('TAK_HELM_VERSION')
        self.minikube_version = env_str_req('TAK_MINIKUBE_VERSION')
        self.insecure_publish_repo = env_str_req('TAK_INSECURE_PUBLISH_REPO')
        self.kubeconfig = (env_str_req('TAK_KUBECONFIG_FILE') if self.deployment_target == DeploymentTarget.RKE2
                           else env_str_opt('TAK_KUBECONFIG_FILE'))
        self.use_port_exposure = env_bool_req('TAK_USE_PORT_EXPOSURE')
        self.certs = CertConfiguration()
        self.minikube_driver = env_str_req('TAK_MINIKUBE_DRIVER')
        self.minikube_memory = env_str_req('TAK_MINIKUBE_MEMORY')
        self.minikube_cpus = env_str_req('TAK_MINIKUBE_CPUS')
        self.minikube_delete_existing_instance = env_bool_req("TAK_MINIKUBE_DELETE_EXISTING_INSTANCE")


# Kubectl Releases: https://kubernetes.io/docs/tasks/tools/
class SoftwareLoadout(Enum):
    """
    An object used to configure the environment-specific deployment dependencies
    """
    LINUX_X86_64 = ('Linux x86_64', 'linux', 'x86_64')
    LINUX_ARM_64 = ('Linux ARM64', 'linux', 'arm64')
    LINUX_ARM = ('Linux ARM', 'linux', 'arm')
    DARWIN_X86_64 = ('Mac OS x86_64', 'darwin', 'x86_64')
    DARWIN_ARM_64 = ('Mac OS M2+', 'darwin', 'arm64')

    def __init__(self, pretty_label: str, python_platform: str, python_arch: str):
        self._pretty_label = pretty_label
        self._python_platform = python_platform
        self._python_arch = python_arch
        self._platform = python_platform  # Same as what is reported by python
        if self._python_arch == 'x86_64':
            self._arch = 'amd64'
        elif self._python_arch == 'arm64' or self._python_arch == 'arm':
            self._arch = self._python_arch
        else:
            halt_on_failure(f'Deployment from {python_platform} - {python_arch} is currently not supported!')
        self.bin_dir = os.path.join(SCRIPT_DIR, 'setup-bins')
        self._kubectl_path = None
        self._helm_path = None
        self._minikube_path = None

    @property
    def kubectl_path(self):
        return self._kubectl_path

    @property
    def helm_path(self):
        return self._helm_path

    @property
    def minikube_path(self):
        return self._minikube_path

    @classmethod
    def this_system(cls) -> 'SoftwareLoadout':
        """
        Gets the configuration for the host system
        :return: the SoftwareLoadout for this system
        """
        system_platform = sys.platform
        platform_machine = platform.machine()
        loadout_identifiers = list()
        for loadout in SoftwareLoadout:
            if system_platform == loadout._python_platform and platform_machine == loadout._python_arch:
                return loadout
            loadout_identifiers.append(loadout._pretty_label)

        err_msg = (f'Could not autodetect supported platform! Supported Platforms:{", ".join(loadout_identifiers)}')
        halt_on_failure(err_msg)

    def _download_binary(self, name: str, version: str, fetch_url: str) -> str:
        """
        Downloads a binary file
        :param name: The name of the binary
        :param version: The version of the binary
        :param fetch_url: The URL for the binary
        :return: The path to the downloaded binary
        """
        target = os.path.join(self.bin_dir, f'{name}-{version}-{self._platform}-{self._arch}')
        fetch_url = fetch_url.format(version=version, os=self._platform, arch=self._arch)
        if not os.path.exists(target):
            print(f'Fetching {name} {version} from {fetch_url}...')
            urllib.request.urlretrieve(fetch_url, target)
            os.chmod(target, os.stat(target).st_mode | stat.S_IEXEC)

        path = os.path.join(self.bin_dir, name)
        if os.path.exists(path):
            os.remove(path)
        os.symlink(target, path)
        return path

    def install_software(self, config: Configuration) -> 'SoftwareLoadout':
        """
        Installs the software needed to execute the provided deployment configuration
        :param config: The configuration to fetch necessary software for
        :return: This software loadout instance
        """
        if not os.path.exists(self.bin_dir):
            os.mkdir(self.bin_dir)

        self._kubectl_path = self._download_binary('kubectl', config.kubernetes_version, KUBCTL_URL)

        if config.deployment_target == DeploymentTarget.MINIKUBE:
            self._minikube_path = self._download_binary('minikube', config.minikube_version, MINIKUBE_URL)

        helm_target = os.path.join(self.bin_dir, f'helm-{config.helm_version}-{self._platform}-{self._arch}')
        fetch_url = HELM_URL.format(version=config.helm_version, os=self._platform, arch=self._arch)

        print(f'Fetching helm {config.helm_version} from {fetch_url}...')
        if not os.path.exists(helm_target):
            target_zip = os.path.join(self.bin_dir, f'helm-{config.helm_version}-{self._platform}-{self._arch}.tar.gz')
            urllib.request.urlretrieve(fetch_url, target_zip)
            shutil.unpack_archive(target_zip, self.bin_dir)
            shutil.move(os.path.join(self.bin_dir, f'{self._platform}-{self._arch}', 'helm'), helm_target)
            os.chmod(helm_target, os.stat(helm_target).st_mode | stat.S_IEXEC)
            os.remove(target_zip)
            shutil.rmtree(os.path.join(self.bin_dir, f'{self._platform}-{self._arch}'))

        self._helm_path = os.path.join(self.bin_dir, 'helm')

        if os.path.exists(self._helm_path):
            os.remove(self._helm_path)
        os.symlink(helm_target, self._helm_path)

        print()
        return self


class CommandRunner:
    """
    A helper class to run general, kubectl, minikube, or helm commands while logging the output to a shared log file
    and handling failure as specified
    """

    def __init__(self, configuration: Configuration, software_loadout: SoftwareLoadout):
        """
        :param configuration: The configuration to apply the commands to
        :param software_loadout: the SoftwareLoadout to use to execute the commands
        """
        self._configuration = configuration
        self._kubectl_path = software_loadout.kubectl_path
        self._minikube_path = software_loadout.minikube_path
        self._helm_path = software_loadout.helm_path

        self._log_file = open(COMMAND_LOG_FILE, 'a')

        timestamp = datetime.datetime.now().strftime('%Y-%m-%dT%H:%M:%S')
        self._log_file.write(f'Starting deployment at {timestamp}\n')
        self._log_file.flush()

    @property
    def configuration(self):
        """
        :return: The Configuration
        """
        return self._configuration

    def run_cmd(self, cmd, stdout_filepath: str = None, cwd: str = CLUSTER_ROOT, env: Dict[str, str] = None,
                keep_running_on_failure: bool = False, silence_failure: bool = False) -> str:
        """
        Runs a generic command
        :param cmd: The command to run
        :param stdout_filepath: An optional file to send the output to
        :param cwd: The directory to run the command in
        :param env: environment parameters to use with the command
        :param keep_running_on_failure: If true, do not halt execution
        :param silence_failure: If true, do not report the error as a fatal error
        :return: The output of the command as a str
        """

        self._log_file.write(f'Executing command [{" ".join(cmd)}]\n')
        process = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, cwd=cwd, env=env)
        output = process.stdout

        # Write to file if specified
        if stdout_filepath is not None:
            with open(stdout_filepath, 'wb') as target:
                target.write(output)

        # Write to standard log
        self._log_file.write(output.decode())
        self._log_file.flush()

        if process.returncode != 0:
            self._log_file.write(f'Command failed with error code {process.returncode}!\n')
            self._log_file.flush()

            if not keep_running_on_failure:
                if cmd[0].startswith(CLUSTER_ROOT):
                    cmd[0] = cmd[0].replace(CLUSTER_ROOT + '/', '')
                halt_on_failure(f'An error occurred while executing command \'{" ".join(cmd)}\'! ' +
                                f'See the end of {COMMAND_LOG_FILE_RELATIVE_PATH} for details.',
                                silence_failure=silence_failure)

        self._log_file.write(f'Command finished successfully\n')
        self._log_file.flush()

        return None if output is None else output.decode()

    def kubectl(self, cmd: List[str], stdout_filepath: str = None, cwd=None, keep_running_on_failure: bool = False,
                silence_failure: bool = False) -> str:
        """
        Runs a kubectl command
        :param cmd: The command to run
        :param stdout_filepath: The optional filepath to write the output to
        :param cwd: The optional directory to run the command in
        :param keep_running_on_failure: If true, do not halt execution
        :param silence_failure: If true, do not report the error as a fatal error
        :return: The output of the command as a str
        """
        if cmd[0] == 'kubectl':
            cmd.pop(0)
        cmd.insert(0, self._kubectl_path)
        if self.configuration.deployment_target == DeploymentTarget.RKE2:
            env = {'KUBECONFIG': self._configuration.kubeconfig}
        else:
            env = None
        return self.run_cmd(cmd, stdout_filepath, cwd, env, keep_running_on_failure=keep_running_on_failure,
                            silence_failure=silence_failure)

    def minikube(self, cmd: List[str], stdout_filepath: str = None, cwd=None, keep_running_on_failure: bool = False,
                 silence_failure: bool = False) -> str:
        """
        Runs a minikube command
        :param cmd: The command to run
        :param stdout_filepath: The optional filepath to write the output to
        :param cwd: The optional directory to run the command in
        :param keep_running_on_failure: If true, do not halt execution
        :param silence_failure: If true, do not report the error as a fatal error
        :return: The output of the command as a str
        """
        if cmd[0] == 'minikube':
            cmd.pop(0)
        cmd.insert(0, self._minikube_path)
        return self.run_cmd(cmd, stdout_filepath, cwd, None, keep_running_on_failure=keep_running_on_failure,
                            silence_failure=silence_failure)

    def helm(self, cmd: List[str], stdout_filepath: str = None, cwd=None, keep_running_on_failure: bool = False,
             silence_failure: bool = False) -> str:
        """
        Runs a helm command
        :param cmd: The command to run
        :param stdout_filepath: The optional filepath to write the output to
        :param cwd: The optional directory to run the command in
        :param keep_running_on_failure: If true, do not halt execution
        :param silence_failure: If true, do not report the error as a fatal error
        :return: The output of the command as a str
        """
        if cmd[0] == 'helm':
            cmd.pop(0)
        cmd.insert(0, self._helm_path)
        if self.configuration.deployment_target == DeploymentTarget.RKE2:
            env = {'KUBECONFIG': self._configuration.kubeconfig}
        else:
            env = None
        return self.run_cmd(cmd, stdout_filepath, cwd, env, keep_running_on_failure=keep_running_on_failure,
                            silence_failure=silence_failure)


class DockerImages(Enum):
    """
    The docker images that may be used as part of a deployment
    """
    Base = ('docker-files/Dockerfile.takserver-base', 'takserver-base')
    Config = ('docker-files/Dockerfile.takserver-config', 'takserver-config')
    Messaging = ('docker-files/Dockerfile.takserver-messaging', 'takserver-messaging')
    Api = ('docker-files/Dockerfile.takserver-api', 'takserver-api')
    DatabaseSetup = ('docker-files/Dockerfile.database-setup', 'takserver-database-setup')
    CaSetup = ('docker-files/Dockerfile.ca', 'takserver-ca-setup')
    Plugins = ('docker-files/Dockerfile.takserver-plugins', 'takserver-plugins')

    def __init__(self, dockerfile: str, base_image_name: str):
        """
        :param dockerfile: The local path to the dockerfile
        :param base_image_name: The name of the resulting image, without the tag or repository information
        """
        self._dockerfile = dockerfile
        self._base_image_name = base_image_name
        self._repository = None
        self._tag = None
        self._enabled = True

    @property
    def repository(self) -> str:
        """
        :return: The repository the image has been published to, if it has been published.
        """
        return self._repository

    @property
    def tag(self) -> str:
        """
        :return: The tag that has been applied to the image, if it has been published
        """
        return self._tag

    @property
    def enabled(self) -> bool:
        """
        :return: Whether the image is enabled
        """
        return self._enabled

    def disable(self):
        self._enabled = False

    def build(self, cmd_runner: CommandRunner, image_tag: str):
        """
        Builds the image and updates the repository and tag information
        :param cmd_runner: The command runner to use to build the image
        :param image_tag: The tag to apply to the image
        """
        if self._enabled:
            image_repo = cmd_runner.configuration.insecure_publish_repo
            full_tag = f'{image_repo}/{self._base_image_name}:{image_tag}'
            print(f'Building docker image {full_tag}...')

            cmd = ['docker', 'build', '-t', f'{full_tag}', '-f', self._dockerfile,
                   '--build-arg', f'TAKSERVER_IMAGE_REPO={image_repo}/takserver-base', '--build-arg',
                   f'TAKSERVER_IMAGE_TAG={image_tag}', '.']
            cmd_runner.run_cmd(cmd)
            self._repository = f'{image_repo}/{self._base_image_name}'
            self._tag = image_tag

        else:
            print(f'Skipping building of docker image for {self._base_image_name}')

    def publish(self, cmd_runner: CommandRunner):
        """
        Publishes the built image
        :param cmd_runner: The command runner to use to publish the image
        :return:
        """
        # TODO: Add support for self signed and authenticated repositories
        if self._enabled:
            if self._repository is None or self._tag is None:
                halt_on_failure("Images must be built before they can be published!")

            repository_and_tag = f'{self._repository}:{self._tag}'
            print(f'Publishing docker image {repository_and_tag}')
            cmd_runner.run_cmd(['docker', 'push', repository_and_tag])

        else:
            print(f'Skipping publishing of docker image for {self._base_image_name}')


class DeploymentContainers(Enum):
    """
    The deployed container types
    """
    Config = ('config', DockerImages.Config, TAK_CONFIG_NODE_MULTIPLIER)
    Messaging = ('messaging', DockerImages.Messaging, TAK_MESSAGING_NODE_MULTIPLIER)
    Api = ('api', DockerImages.Api, TAK_API_NODE_MULTIPLIER)
    Plugins = ('plugins', DockerImages.Plugins, TAK_PLUGIN_NODE_MULTIPLIER)
    DatabaseSetup = ('takserverDatabaseSetup', DockerImages.DatabaseSetup, TAK_DATABASE_SETUP_MULTIPLIER)

    def __init__(self, config_name: str, docker_image: DockerImages, replica_multiplier: int):
        """
        :param config_name: The name of the configuration in the yaml files
        :param docker_image: The docker image object used by the container
        :param replica_multiplier: The multiplier for now many instances should be deployed based on the takserver node
                                   count. At least one will always be deployed.
        """
        self._config_name = config_name
        self._docker_image = docker_image
        self._replica_multiplier = replica_multiplier

    def update_helm_config(self, deployment_values: Dict, tak_cluster_node_count: int):
        """
        Updates the helm configuration to have the desired resource allocations for this container
        :param deployment_values: The helm configuration
        :param tak_cluster_node_count: The defined TAK cluster node count. Standard multipliers for each node type will
                                       be multiplied by this to get the desired replica count.
        """
        container = deployment_values['takserver'][self._config_name]

        if self._docker_image.enabled:
            container['enabled'] = True

            container['image']['repository'] = self._docker_image.repository
            container['image']['tag'] = self._docker_image.tag

            if tak_cluster_node_count == 0:
                print(f'Setting {self.name} replicas to 1')
                container['replicas'] = 1
                if 'resources' not in container:
                    container['resources'] = dict()
                if 'requests' not in container['resources']:
                    container['resources']['requests'] = dict()
                if 'limits' not in container['resources']:
                    container['resources']['limits'] = dict()
                container['resources']['requests']['cpu'] = 2
                container['resources']['limits']['cpu'] = 2
                container['resources']['requests']['memory'] = '2Gi'
                container['resources']['limits']['memory'] = '2Gi'
            else:
                replicas = max(1, int(round(tak_cluster_node_count * self._replica_multiplier)))
                print(f'Setting {self.name} replicas to {replicas}')
                container['replicas'] = replicas

        else:
            container['enabled'] = False


class ClusterDeployer:
    """
    Used to deploy a generic cluster instance
    """

    def __init__(self, configuration: Configuration, software_loadout: SoftwareLoadout):
        """
        :param configuration: The configuration to deploy
        :param software_loadout: The local applications set up to be used for deployment
        """
        self._configuration = configuration
        self._cmd_runner = CommandRunner(configuration, software_loadout)

    def build_docker_images(self):
        """
        Builds all enabled docker images
        """

        if self._configuration.tak_enable_plugins <= 0:
            DockerImages.Plugins.disable()

        for image in DockerImages:
            image.build(self._cmd_runner, self._configuration.kubernetes_namespace)

    def publish_docker_images(self):
        """
        Publishes all enabled docker images. If they haven't been built this will fail!
        """
        for image in DockerImages:
            image.publish(self._cmd_runner)

    def prep_certs(self):
        """
        Prepares the certificates for this configuration. If none are provided a container will be brought up to create
        them
        """

        def validate_required_certs(cert_dir):
            for cert in REQUIRED_CERTIFICATES:
                if not os.path.isfile(os.path.join(cert_dir, cert)):
                    halt_on_failure(f'Although the certificate directory {cert_dir} exists it does not contain the ' +
                                    f'required certificate {cert}!')

        config = self._configuration.tak_cert_config
        default_cert_dir = os.path.join(CLUSTER_ROOT, 'takserver-core', 'certs', 'files')

        if config.src_dir is not None:
            print(f'Using certificates located in {config.src_dir}')
            source_dir = config.src_dir

        elif (os.path.isdir(default_cert_dir) and os.path.isfile(os.path.join(default_cert_dir, 'admin.pem'))):
            print(f'Using certificates prepopulated in {default_cert_dir}')
            source_dir = default_cert_dir

        else:
            print(f'Creating new certificates with default password "atakatak" in {default_cert_dir}')
            self._cmd_runner.run_cmd([
                'docker', 'run', '-it', f'--name={CA_CONTAINER_NAME}',
                f'--env=CA_NAME={config.ca_name}',
                f'--env=STATE={config.ca_state}',
                f'--env=CITY={config.ca_city}',
                f'--env=ORGANIZATIONAL_UNIT={config.ca_organization}',
                f"{DockerImages.CaSetup.repository}:{DockerImages.CaSetup.tag}"])
            self._cmd_runner.run_cmd(['docker', 'cp', f'{CA_CONTAINER_NAME}:/files',
                                      os.path.join(CLUSTER_ROOT, 'takserver-core', 'certs')])
            self._cmd_runner.run_cmd(['docker', 'rm', CA_CONTAINER_NAME])
            source_dir = default_cert_dir

        validate_required_certs(source_dir)
        self._cmd_runner.kubectl(['create', 'configmap', 'cert-migration',
                                  f'--from-file={source_dir}', '--dry-run=client', '--output=yaml'],
                                 stdout_filepath=os.path.join(CLUSTER_ROOT,
                                                              'deployments/helm/templates/cert-migration.yaml'))

    def update_configuration_files(self):
        """
        Updates all deployment configuration files with relevant configuration details
        """
        db_url = f'jdbc:postgresql://takserver-postgresql:5432/cot'
        db_username = self._configuration.tak_db_username
        db_password = self._configuration.tak_db_password
        namespace = self._configuration.kubernetes_namespace

        print('Updating Helm deployment values file...')
        # Update the helm deployment values
        config_filepath = os.path.join(CLUSTER_ROOT, 'deployments/helm/production-values.yaml')
        deployment_values = yaml.load(open(config_filepath), Loader=yaml.SafeLoader)
        deployment_values['takserver']['namespace'] = namespace
        deployment_values['postgresql']['enabled'] = True
        pg_auth = deployment_values['global']['postgresql']['auth']
        pg_auth['username'] = db_username
        pg_auth['password'] = db_password
        pg_auth['postgresPassword'] = db_password
        for container in DeploymentContainers:
            container.update_helm_config(deployment_values, self._configuration.tak_node_count)
        yaml.dump(deployment_values, open(config_filepath, 'w'), Dumper=yaml.SafeDumper)

        print('Updating CoreConfig.xml...')
        cc_path = os.path.join(CLUSTER_ROOT, 'CoreConfig.xml')
        core_config = open(cc_path).read()
        core_config = core_config.replace('DB_URL_PLACEHOLDER', db_url)
        core_config = core_config.replace('DB_USERNAME_PLACEHOLDER', db_username)
        core_config = core_config.replace('DB_PASSWORD_PLACEHOLDER', db_password)
        open(cc_path, 'w').write(core_config)

        print('Updating TAKIgniteConfig.xml...')
        tic_path = os.path.join(CLUSTER_ROOT, 'TAKIgniteConfig.xml')
        tic_tree = ET.parse(tic_path)
        tic_tree.getroot().attrib['igniteClusterNamespace'] = namespace
        tic_tree.write('TAKIgniteConfig.xml')

    def deploy(self):
        """
        Deploys the configuration to the k8s cluster
        """
        kubectl = self._cmd_runner.kubectl
        helm = self._cmd_runner.helm
        namespace = self._configuration.kubernetes_namespace

        if self._configuration.deployment_target == DeploymentTarget.MINIKUBE:
            minikube = self._cmd_runner.minikube
            # TODO: Add check for core count
            if self._configuration.minikube_delete_existing_instance:
                print("Halting existing Minikube instance...")
                minikube(['stop'], keep_running_on_failure=True, silence_failure=True)
                minikube(['delete'], keep_running_on_failure=True, silence_failure=True)
            print("Starting Minikube...")
            minikube(['start', '--apiserver-port', '9210',
                      f'--kubernetes-version={self._configuration.kubernetes_version}',
                      f'--driver={self._configuration.minikube_driver}',
                      f'--cpus={self._configuration.minikube_cpus}',
                      f'--memory={self._configuration.minikube_memory}',
                      f'--insecure-registry={self._configuration.insecure_publish_repo}',
                      '--extra-config=apiserver.service-node-port-range=8000-65535'])
            minikube(['addons', 'enable', 'ingress'])

        #    update_core_config
        print('Deploying...')
        kubectl(['create', 'configmap', 'tak-ignite-config',
                 f'--from-file={os.path.join(CLUSTER_ROOT, "TAKIgniteConfig.xml")}',
                 '--dry-run=client', '--output=yaml'],
                stdout_filepath=os.path.join(CLUSTER_ROOT,
                                             'deployments/helm/templates/tak-ignite-config.yaml'))

        kubectl(['create', 'configmap', 'core-config',
                 f'--from-file={os.path.join(CLUSTER_ROOT, "CoreConfig.xml")}',
                 '--dry-run=client', '--output=yaml'],
                stdout_filepath=os.path.join(CLUSTER_ROOT, 'deployments/helm/templates/core-config.yaml'))

        # DO NOT run helm commands within the deployments/helm directory as it may result in additional files being
        # placed in there and deployment failures!
        exec_dir = str(pathlib.Path(SCRIPT_DIR).parent)
        helm(['dep', 'update', 'deployments/helm'], cwd=exec_dir)

        helm(['upgrade', '--install',
              f'--namespace={namespace}', '--create-namespace',
              '--values=deployments/helm/production-values.yaml', 'takserver', './deployments/helm'], cwd=exec_dir)

    def try_get_address(self, service_name: str) -> str:
        # TODO: Add checks so that if multiple nodes are detected for a service it makes it known
        if self._configuration.deployment_target == DeploymentTarget.MINIKUBE:
            node_address = subprocess.check_output(['minikube', 'ip']).decode().strip()

        else:
            try:
                namespace = self._configuration.kubernetes_namespace

                node_name = json.loads(subprocess.check_output(
                    ['kubectl', '-n', namespace, 'get', 'pod', f'--selector=app={service_name}', '-o',
                     'json']).decode().strip()
                                       )['items'][0]['spec']['nodeName']
                node_details = json.loads(subprocess.check_output(
                    ['kubectl', 'get', 'node', '-A', '-o', 'json', f'--field-selector=metadata.name={node_name}'])
                                          .strip())
                node_address = list(filter(lambda x: x['type'] == 'InternalIP',
                                           node_details['items'][0]['status']['addresses']))[0]['address']
            except Exception as e:
                node_address = None

        return node_address

    def apply_port_exposures(self):
        """
        Deploys the configuration to the k8s cluster
        """
        kubectl = self._cmd_runner.kubectl
        namespace = self._configuration.kubernetes_namespace

        if self._configuration.deployment_target == DeploymentTarget.MINIKUBE:
            node_address = subprocess.check_output(['minikube', 'ip']).decode().strip()

        else:
            try:
                api_node_name = json.loads(subprocess.check_output(
                    ['kubectl', '-n', namespace, 'get', 'pod', '--selector=app=takserver-api', '-o', 'json']).strip()
                                           )['items'][0]['spec']['nodeName']
                node_details = json.loads(subprocess.check_output(
                    ['kubectl', 'get', 'node', '-A', '-o', 'json', f'--field-selector=metadata.name={api_node_name}'])
                                          .strip())
                node_address = list(filter(lambda x: x['type'] == 'InternalIP',
                                           node_details['items'][0]['status']['addresses']))[0]['address']
            except:
                node_address = None

        def expose_port(node_name: str, service_identifier, protocol: str, port: int, host: str = None) -> str:
            def try_patching(target_port: int):
                kubectl(['patch', 'service', service_identifier,
                         f'--namespace={namespace}', '--type=json',
                         '--patch=[{"op": "replace", "path": "/spec/ports/0/nodePort", "value":' + str(
                             target_port) + '}]'
                         ], silence_failure=True)

            kubectl(['expose', 'deployment', node_name,
                     f'--namespace={namespace}',
                     f'--protocol={protocol}', f'--port={str(port)}', f'--target-port={str(port)}',
                     '--type=LoadBalancer',
                     f'--name={service_identifier}'])

            try:
                try_patching(port)
            except:
                target_port = 30000 + port
                print(f'Failed to patch port to {str(port)}. Trying {str(target_port)}')
                try:
                    try_patching(target_port)
                except:
                    print('Cannot patch port. Using automatically assigned port.')

            external_port = kubectl([
                'get', 'service', f'--namespace={namespace}', '--output',
                'go-template={{range.spec.ports}}{{if .nodePort}}{{.nodePort}}{{end}}{{end}}', service_identifier])

            if host is None:
                return f"{service_identifier}-{protocol.lower()} -> {external_port}"
            else:
                return f"{service_identifier}-{protocol.lower()} -> {host}:{external_port}"

        urls = list()
        urls.append(expose_port('takserver-api', 'cert-https', 'TCP', 8443, node_address))
        urls.append(expose_port('takserver-api', 'federation-truststore', 'TCP', 8444, node_address))
        urls.append(expose_port('takserver-api', 'https', 'TCP', 8446, node_address))
        urls.append(expose_port('takserver-messaging', 'tls', 'TCP', 8089, node_address))
        urls.append(expose_port('takserver-messaging', 'federation-v1', 'TCP', 9000, node_address))
        urls.append(expose_port('takserver-messaging', 'federation-v2', 'TCP', 9001, node_address))

    def apply_ingress_rules(self):
        """
        Deploys the configuration to the k8s cluster
        """

        if self._configuration.use_port_exposure:
            print('Applying port exposures instead of direct ingress...')
            return self.apply_port_exposures()

        kubectl = self._cmd_runner.kubectl
        namespace = self._configuration.kubernetes_namespace

        try:
            api_node_name = json.loads(subprocess.check_output(
                ['kubectl', '-n', namespace, 'get', 'pod', '--selector=app=takserver-api', '-o', 'json']).strip()
                                       )['items'][0]['spec']['nodeName']
            node_details = json.loads(subprocess.check_output(
                ['kubectl', 'get', 'node', '-A', '-o', 'json', f'--field-selector=metadata.name={api_node_name}'])
                                      .strip())
            node_address = list(filter(lambda x: x['type'] == 'InternalIP',
                                       node_details['items'][0]['status']['addresses']))[0]['address']
        except:
            node_address = None

        # Update the namespace for the ingress rules
        data = yaml.load_all(open(DEFAULT_INGRESS_FILE), yaml.FullLoader)
        target = list()
        for section in data:
            section['metadata']['namespace'] = namespace
            target.append(section)

        yaml.dump_all(target, open(DEFAULT_INGRESS_FILE, 'w'))

        # Apply the ingress rules
        kubectl(['apply', '--namespace', namespace, '-f', DEFAULT_INGRESS_FILE])

    def display_external_access(self):
        # TODO: Validate no services have multiple IP addresses
        namespace = self._configuration.kubernetes_namespace

        service_json = json.loads(
            subprocess.check_output(['kubectl', 'get', 'services', '-n', namespace, '-o', 'json']).strip())

        service_list = list(filter(
            lambda x:
            x['metadata']['name'] == 'takserver-api-service' or
            x['metadata']['name'] == 'takserver-messaging-service'
            , service_json['items']))

        endpoint_details = list()

        for service in service_list:
            if ('status' in service and 'loadBalancer' in service['status'] and
                    'ingress' in service['status']['loadBalancer'] and
                    len(service['status']['loadBalancer']['ingress']) > 0):
                ip = service['status']['loadBalancer']['ingress'][0]['ip']
            else:
                try:
                    ip = self.try_get_address(service['spec']['selector']['app'])
                except:
                    ip = 'Unknown'

            if service['spec'] is not None and len(service['spec']['ports']) > 0:
                for port in service['spec']['ports']:
                    endpoint_details.append({
                        'SERVICE': service['metadata']['name'],
                        'ENDPOINT': port['name'],
                        'ADDRESS': ip,
                        'PORT': port['targetPort']
                    })

        print(f"Deployment Namespace: {namespace}")
        print('\nService Endpoints:')

        pretty_print_dict_list(endpoint_details, ['SERVICE', 'ENDPOINT', 'ADDRESS', 'PORT'])

        print()
        print('If no address is defined or the ports do not seem to work you may have to check with your system '
              'administrator to see how your ingress is configured. You can also try setting TAK_USE_PORT_EXPOSURE ' +
              'to true to see if that resolves your issues.')
        print()
        print('If this is a locally hosted system you may have to adjust your firewall rules to allow external access.')

    def start(self):
        """
        Starts deployment of the system
        """
        self.build_docker_images()
        print()
        self.prep_certs()
        print()
        self.update_configuration_files()
        print()
        self.publish_docker_images()
        print()
        self.deploy()
        print()
        self.apply_ingress_rules()
        print()
        self.display_external_access()

    def uninstall(self):
        """
        Removes the deployed artifacts from the cluster. The docker images are not removed, and a command to delete the
        database will be printed to the command line to remove it as well.
        """
        print('Uninstalling takserver...')
        ns = self._configuration.kubernetes_namespace

        # Chart hooks can't be deleted as part of the chart so delete the db setup hook manually
        # -- https://helm.sh/docs/topics/charts_hooks/#hook-deletion-policies
        self._cmd_runner.kubectl(['delete', 'pods', f"--selector=job-name=takserver-db-setup", f'--namespace={ns}'],
                                 keep_running_on_failure=True)
        self._cmd_runner.kubectl(['delete', 'job.batch/takserver-db-setup', '-n', ns], keep_running_on_failure=True)

        # Remove the helm chart
        self._cmd_runner.helm(['uninstall', f'--namespace={ns}', 'takserver'], keep_running_on_failure=True)

        print('Removing service definitions...')
        self._cmd_runner.kubectl(['delete', 'svc', f'{ns}-https-8443', '-n', ns], keep_running_on_failure=True)
        self._cmd_runner.kubectl(['delete', 'svc', f'{ns}-https-8444', '-n', ns], keep_running_on_failure=True)
        self._cmd_runner.kubectl(['delete', 'svc', f'{ns}-https-8446', '-n', ns], keep_running_on_failure=True)
        self._cmd_runner.kubectl(['delete', 'svc', f'{ns}-streaming-8089', '-n', ns], keep_running_on_failure=True)
        self._cmd_runner.kubectl(['delete', 'svc', f'{ns}-fed-9000', '-n', ns], keep_running_on_failure=True)
        self._cmd_runner.kubectl(['delete', 'svc', f'{ns}-fed-9001', '-n', ns], keep_running_on_failure=True)

        print('If you would like to delete the database please execute "kubectl delete pvc -n ' +
              ns + ' data-takserver-postgresql-0"')

        if self._configuration.deployment_target == DeploymentTarget.MINIKUBE:
            print('Run "scripts/setup-bins/minikube delete" if you would like to delete the host minikube instance ' +
                  ' including the database.')

        result = None
        while result != '':
            result = self._cmd_runner.kubectl([f'--namespace={ns}', 'get', 'pods', '--output=name'])
            time.sleep(2)


def main():
    print(f'Starting deployment of TAKServer. Execution logs will be saved to {COMMAND_LOG_FILE_RELATIVE_PATH}.\n')
    # Get the configuration
    configuration = Configuration()
    # Install the software necessary to set up the cluster
    software_loadout = SoftwareLoadout.this_system().install_software(configuration)
    # Deploy the cluster

    if len(sys.argv) >= 2:
        if sys.argv[1] == 'uninstall' or sys.argv[1] == '-u':
            ClusterDeployer(configuration, software_loadout).uninstall()
        elif sys.argv[1] == 'status' or sys.argv[1] == '-s':
            ClusterDeployer(configuration, software_loadout).display_external_access()
        else:
            print(f"Unexpected argument {sys.argv[1]}!")
            exit(1)
    else:
        ClusterDeployer(configuration, software_loadout).start()


if __name__ == '__main__':
    main()
