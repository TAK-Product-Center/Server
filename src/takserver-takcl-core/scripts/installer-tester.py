#!/usr/bin/env python3
import argparse
import csv
import glob
import os
import re
import subprocess
import time
import uuid
from enum import Enum

from subprocess import CalledProcessError
from typing import List

#################################################### Custom Values ####################################################

# The tag for the initial build. A copy of takserver-package after a `./gradlew clean buildRpm buildDeb` for the given
# build is expected to be located at takserver-package-{INITIAL_TAG}
INITIAL_TAG = '5.2-RELEASE58'

# The tag for a recent unpatched upgrade version. A copy of takserver-package after a
# `./gradlew clean buildRpm buildDeb` for the given build is expected to be located at takserver-package-{INITIAL_TAG}
LATEST_MASTER_TAG = '5.3-RELEASE24'

# The tag for a patched version. A copy of takserver-package after a
# `./gradlew clean buildRpm buildDeb` for the given build is expected to be located at takserver-package-{INITIAL_TAG}
LATEST_MODIFIED_TAG = '5.4-DEV29'

# The path to a directory of usable certs that will be copied to /opt/tak/certs/files
CERTS_PATH = os.environ['TAKSERVER_CERT_SOURCE']

# The deb VM name
DEB_VM_NAME = 'takserver-ubuntu-22.04'
# The deb VM ssh identifier, usually set in ~/.ssh/config along with a certificate. It must authenticate with a cert
# and have passwordless sudo in order to automate the testing.
DEB_VM_SSH_ALIAS = 'tak-u'
# The RPM host VM name
RPM_VM_NAME = 'takserver-rocky-8'
# The rpm VM ssh identifier, usually set in ~/.ssh/config along with a certificate. It must authenticate with a cert
# and have passwordless sudo in order to automate the testing.
RPM_VM_SSH_ALIAS = 'tak-r'


def rollback_vm(vm_identifier: str):
    '''
    The method used to tear down and roll back a vm.
    :param vm_identifier: The identifier for the vim
    '''
    # If it is not shut down, kill it using virsh on Linux
    result = subprocess.check_output(['virsh', 'list', '--state-shutoff']).decode()
    if vm_identifier not in result:
        subprocess.check_output(['virsh', 'destroy', vm_identifier])

    # Then roll it back. In this case I am touching a file another script running as root is monitoring for which
    # will do another "virsh destroy" sequence, rollback to a clean snapshot using zfs, and then start it back up.
    # Afterwards ssh attempts will be made within this script to connect until successful and then a scenario will be tested.

    with open(f'/tmp/reset-vm-{vm_identifier}', 'w') as file:
        file.write('trigger')


#######################################################################################################################

SCRIPT_DIRECTORY = os.path.dirname(os.path.realpath(__file__))
TAK_ROOT = os.path.abspath(f'{SCRIPT_DIRECTORY}/../../')

TRACKING_TAG = str(uuid.uuid4()).replace('-', '')

TOUCH_TEMPLATE = 'if [[ -f "{filepath}" ]];then echo "{tag_line}" | sudo tee -a "{filepath}";fi'
VERSION_TAG_REGEX = r'(?P<major_version>^[0-9]*\.[0-9]*-[a-zA-Z]*)(?P<minor_version>[0-9]*$)'

DEB_CERT_BACKUP_SCRIPT = os.path.join(TAK_ROOT, 'takserver-package/scripts/deb-cert-metadata-backup.sh')

def extract_version_backup_tag(version_tag: str):
    values = re.match(VERSION_TAG_REGEX, version_tag).groupdict()
    return f'{values["major_version"]}-{values["minor_version"]}'


LOG_TARGET = 'execution-log.txt'
LOG_FILE = open(LOG_TARGET, 'a')

SAVED_LATEST_MASTER_BACKUP = extract_version_backup_tag(LATEST_MASTER_TAG)
SAVED_LATEST_MODIFIED_BACKUP = extract_version_backup_tag(LATEST_MODIFIED_TAG)


def exec_cmd(cmd: List[str], cwd: str = None, dry_run: bool = False):
    if dry_run:
        if cwd is None:
            print(f'EXEC [{" ".join(cmd)}] in default directory\n')
        else:
            print(f'EXEC [{" ".join(cmd)}] in directory {cwd}\n')

    else:
        try:
            if cwd is None:
                LOG_FILE.write(f'EXEC [{" ".join(cmd)}] in default directory\n')
            else:
                LOG_FILE.write(f'EXEC [{" ".join(cmd)}] in directory {cwd}\n')

            LOG_FILE.flush()

            subprocess.check_call(cmd, stderr=LOG_FILE, stdout=LOG_FILE, cwd=cwd)
            LOG_FILE.write('\n')
            LOG_FILE.flush()
        except CalledProcessError as e:
            LOG_FILE.write('')
            LOG_FILE.flush()
            raise e


def display_chart(headers: List[str], rows: List[List[str]]):
    result = list()
    column_widths = dict()

    header_idx = 0
    for header in headers:
        column_widths[header_idx] = len(header)
        header_idx += 1

    for row in rows:
        if len(row) != len(headers):
            raise Exception('Row column count doesn\'t match header count!')

        column_idx = 0
        for column_value in list(row):
            if column_value is None:
                column_value = ''
                row[column_idx] = column_value
            else:
                column_value = str(column_value)
                row[column_idx] = column_value

            column_widths[column_idx] = max(column_widths[column_idx], len(column_value))
            column_idx += 1

    idx = 0
    frame_row = ''
    header_row = ''
    separator_row = ''

    while idx < len(headers):
        frame_row = frame_row + '--' + '-'.ljust(column_widths[idx], '-') + '-'
        header_row = header_row + '| ' + headers[idx].ljust(column_widths[idx], ' ') + ' '
        separator_row = separator_row + '|-' + '-'.ljust(column_widths[idx], '-') + '-'
        idx += 1
    header_row += '|'
    frame_row = '|' + frame_row[1:] + '|'
    separator_row += '|'
    result.append(frame_row)
    result.append(header_row)
    result.append(separator_row)

    for row in rows:
        line = ''
        column_idx = 0
        while column_idx < len(row):
            line = line + '| ' + row[column_idx].ljust(column_widths[column_idx], ' ') + ' '
            column_idx += 1
        line = line + '|'
        result.append(line)

    result.append(frame_row)

    for line in result:
        print(line)


class VirtualMachine(Enum):
    ROCKY_8 = (RPM_VM_NAME, RPM_VM_SSH_ALIAS)
    UBUNTU_2204 = (DEB_VM_NAME, DEB_VM_SSH_ALIAS)

    def __init__(self, vm_identifier: str, vm_ssh_alias: str):
        self.vm_identifier = vm_identifier
        self.vm_ssh_alias = vm_ssh_alias

    def test_connection(self, timeout: int = 90):
        success = False
        duration = 0
        while not success and duration < timeout:
            try:
                exec_cmd(['ssh', self.vm_ssh_alias, 'ls'])
                success = True
            except subprocess.CalledProcessError:
                time.sleep(1)
                duration = duration + 1

        if success:
            print('Connection to VM verified')
        else:
            raise Exception('The VM never became reachable!')

    def restore_base_snapshot(self):
        rollback_vm(self.vm_identifier)
        self.test_connection()

    def execute_command(self, cmd: List[str]):
        exec_cmd(['ssh', self.vm_ssh_alias] + cmd)

    def copy_file(self, filepath: str):
        print(f'Copying {filepath} to the VM...')
        exec_cmd(['scp', '-r', filepath, f'{self.vm_ssh_alias}:~/'])


class UpgradeScenario(Enum):
    LATEST_MASTER = SAVED_LATEST_MASTER_BACKUP
    LATEST_MODIFIED = SAVED_LATEST_MODIFIED_BACKUP

    @classmethod
    def from_version_value(cls, version: str):
        result = None
        for scenario in list(cls):
            if scenario.value == version:
                result = scenario

        if result is None:
            raise Exception(f'Unexpected upgrade version "{version}"!')
        return result


class TestFile(Enum):
    DB_CLEAR_OLD_DATA = ('/opt/tak/db-utils/clear-old-data.sql', ' --', '', True)
    CERT_METADATA = ('/opt/tak/certs/cert-metadata.sh', '# ', '')
    CERT_CONFIG = ('/opt/tak/certs/config.cfg', '# ', '')
    RETENTION_POLICY = ('/opt/tak/conf/retention/retention-policy.yml', '# ', '')
    RETENTION_SERVICE = ('/opt/tak/conf/retention/retention-service.yml', '# ', '')
    MISSION_ARCHIVING_CONFIG = ('/opt/tak/conf/retention/mission-archiving-config.yml', '# ', '')
    MISSION_STORE = ('/opt/tak/mission-archive/mission-store.yml', '# ', '')

    #    FEDHUB_CERT_METADATA = ('/opt/tak/federation-hub/certs/cert-metadata.sh', '# ', '')
    #    FEDHUB_CERT_CONFIG = ('/opt/tak/federation-hub/certs/config.cfg', '# ', '')
    #    FEDHUB_BROKER_DOCKER = ('/opt/tak/federation-hub/configs/federation-hub-broker-docker.yml', '# ', '')
    #    FEDHUB_BROKER = ('/opt/tak/federation-hub/configs/federation-hub-broker.yml', '# ', '')
    #    FEDHUB_UI = ('/opt/tak/federation-hub/configs/federation-hub-ui.yml', '# ', '')
    #    FEDHUB_LOGBACK_BROKER = ('/opt/tak/federation-hub/configs/logback-broker.xml', '<!--', '-->')
    #    FEDHUB_LOGBACK_POLICY = ('/opt/tak/federation-hub/configs/logback-policy.xml', '<!--', '-->')
    #    FEDHUB_LOGBACK_SPRING = ('/opt/tak/federation-hub/configs/logback-spring.xml', '<!--', '-->')
    #    FEDHUB_LOGBACK_TEST = ('/opt/tak/federation-hub/configs/logback-test.xml', '<!--', '-->')
    #    FEDHUB_LOGBACK_UI = ('/opt/tak/federation-hub/configs/logback-ui.xml', '<!--', '-->')

    def __init__(self, path: str, comment_prefix: str, comment_postfix: str, backup_new_version: bool = False):
        self.path = path
        self.comment_prefix = comment_prefix
        self.comment_postfix = comment_postfix
        self.backup_new_version = backup_new_version

    @classmethod
    def from_file(cls, path: str):
        result = None
        for file in list(cls):
            if file.path == path:
                result = file

        if result is None:
            raise Exception(f'Unexpected file path "{path}"!')
        return result


class InstallationPackage(Enum):
    RPM_API = (
        VirtualMachine.ROCKY_8, f'takserver-api',
        f'{TAK_ROOT}/takserver-package-{INITIAL_TAG}/API/build/distributions/takserver-api-{INITIAL_TAG}.noarch.rpm',
        f'{TAK_ROOT}/takserver-package-{LATEST_MASTER_TAG}/API/build/distributions/takserver-api-{LATEST_MASTER_TAG}.noarch.rpm',
        f'{TAK_ROOT}/takserver-package-{LATEST_MODIFIED_TAG}/API/build/distributions/takserver-api-{LATEST_MODIFIED_TAG}.noarch.rpm')

    # RPM_COMBINED = (
    #     VirtualMachine.ROCKY_8, None,
    #     f'{TAK_ROOT}/takserver-package-{INITIAL_TAG}/build/distributions/takserver-combined-{INITIAL_TAG}.noarch.rpm',
    #     f'{TAK_ROOT}/takserver-package-{LATEST_MASTER_TAG}/build/distributions/takserver-combined-{LATEST_MASTER_TAG}.noarch.rpm',
    #     f'{TAK_ROOT}/takserver-package-{LATEST_MODIFIED_TAG}/build/distributions/takserver-combined-{LATEST_MODIFIED_TAG}.noarch.rpm')

    RPM_MESSAGING = (
        VirtualMachine.ROCKY_8, f'takserver-messaging',
        f'{TAK_ROOT}/takserver-package-{INITIAL_TAG}/messaging/build/distributions/takserver-messaging-{INITIAL_TAG}.noarch.rpm',
        f'{TAK_ROOT}/takserver-package-{LATEST_MASTER_TAG}/messaging/build/distributions/takserver-messaging-{LATEST_MASTER_TAG}.noarch.rpm',
        f'{TAK_ROOT}/takserver-package-{LATEST_MODIFIED_TAG}/messaging/build/distributions/takserver-messaging-{LATEST_MODIFIED_TAG}.noarch.rpm')

    RPM_DB = (
        VirtualMachine.ROCKY_8, None,
        f'{TAK_ROOT}/takserver-package-{INITIAL_TAG}/database/build/distributions/takserver-database-{INITIAL_TAG}.noarch.rpm',
        f'{TAK_ROOT}/takserver-package-{LATEST_MASTER_TAG}/database/build/distributions/takserver-database-{LATEST_MASTER_TAG}.noarch.rpm',
        f'{TAK_ROOT}/takserver-package-{LATEST_MODIFIED_TAG}/database/build/distributions/takserver-database-{LATEST_MODIFIED_TAG}.noarch.rpm')

    RPM_FEDHUB = (
        VirtualMachine.ROCKY_8, 'federation-hub',
        f'{TAK_ROOT}/takserver-package-{INITIAL_TAG}/federation-hub/build/distributions/takserver-fed-hub-{INITIAL_TAG}.noarch.rpm',
        f'{TAK_ROOT}/takserver-package-{LATEST_MASTER_TAG}/federation-hub/build/distributions/takserver-fed-hub-{LATEST_MASTER_TAG}.noarch.rpm',
        f'{TAK_ROOT}/takserver-package-{LATEST_MODIFIED_TAG}/federation-hub/build/distributions/takserver-fed-hub-{LATEST_MODIFIED_TAG}.noarch.rpm')

    RPM_FULL = (
        VirtualMachine.ROCKY_8, f'takserver',
        f'{TAK_ROOT}/takserver-package-{INITIAL_TAG}/takserver/build/distributions/takserver-{INITIAL_TAG}.noarch.rpm',
        f'{TAK_ROOT}/takserver-package-{LATEST_MASTER_TAG}/takserver/build/distributions/takserver-{LATEST_MASTER_TAG}.noarch.rpm',
        f'{TAK_ROOT}/takserver-package-{LATEST_MODIFIED_TAG}/takserver/build/distributions/takserver-{LATEST_MODIFIED_TAG}.noarch.rpm')

    RPM_CORE = (
        VirtualMachine.ROCKY_8, f'takserver',
        f'{TAK_ROOT}/takserver-package-{INITIAL_TAG}/launcher/build/distributions/takserver-core-{INITIAL_TAG}.noarch.rpm',
        f'{TAK_ROOT}/takserver-package-{LATEST_MASTER_TAG}/launcher/build/distributions/takserver-core-{LATEST_MASTER_TAG}.noarch.rpm',
        f'{TAK_ROOT}/takserver-package-{LATEST_MODIFIED_TAG}/launcher/build/distributions/takserver-core-{LATEST_MODIFIED_TAG}.noarch.rpm')

    DEB_API = (
        VirtualMachine.UBUNTU_2204, f'takserver-api',
        f'{TAK_ROOT}/takserver-package-{INITIAL_TAG}/API/build/distributions/takserver-api_{INITIAL_TAG}_all.deb',
        f'{TAK_ROOT}/takserver-package-{LATEST_MASTER_TAG}/API/build/distributions/takserver-api_{LATEST_MASTER_TAG}_all.deb',
        f'{TAK_ROOT}/takserver-package-{LATEST_MODIFIED_TAG}/API/build/distributions/takserver-api_{LATEST_MODIFIED_TAG}_all.deb')

    # DEB_COMBINED = (
    #     VirtualMachine.UBUNTU_2204, None,
    #     f'{TAK_ROOT}/takserver-package-{INITIAL_TAG}/build/distributions/takserver-combined_{INITIAL_TAG}_all.deb',
    #     f'{TAK_ROOT}/takserver-package-{LATEST_MASTER_TAG}/build/distributions/takserver-combined_{LATEST_MASTER_TAG}_all.deb',
    #     f'{TAK_ROOT}/takserver-package-{LATEST_MODIFIED_TAG}/build/distributions/takserver-combined_{LATEST_MODIFIED_TAG}_all.deb')

    DEB_MESSAGING = (
        VirtualMachine.UBUNTU_2204, f'takserver-messaging',
        f'{TAK_ROOT}/takserver-package-{INITIAL_TAG}/messaging/build/distributions/takserver-messaging_{INITIAL_TAG}_all.deb',
        f'{TAK_ROOT}/takserver-package-{LATEST_MASTER_TAG}/messaging/build/distributions/takserver-messaging_{LATEST_MASTER_TAG}_all.deb',
        f'{TAK_ROOT}/takserver-package-{LATEST_MODIFIED_TAG}/messaging/build/distributions/takserver-messaging_{LATEST_MODIFIED_TAG}_all.deb')

    DEB_DB = (
        VirtualMachine.UBUNTU_2204, None,
        f'{TAK_ROOT}/takserver-package-{INITIAL_TAG}/database/build/distributions/takserver-database_{INITIAL_TAG}_all.deb',
        f'{TAK_ROOT}/takserver-package-{LATEST_MASTER_TAG}/database/build/distributions/takserver-database_{LATEST_MASTER_TAG}_all.deb',
        f'{TAK_ROOT}/takserver-package-{LATEST_MODIFIED_TAG}/database/build/distributions/takserver-database_{LATEST_MODIFIED_TAG}_all.deb')

    DEB_FEDHUB = (
        VirtualMachine.UBUNTU_2204, 'federation-hub',
        f'{TAK_ROOT}/takserver-package-{INITIAL_TAG}/federation-hub/build/distributions/takserver-fed-hub_{INITIAL_TAG}_all.deb',
        f'{TAK_ROOT}/takserver-package-{LATEST_MASTER_TAG}/federation-hub/build/distributions/takserver-fed-hub_{LATEST_MASTER_TAG}_all.deb',
        f'{TAK_ROOT}/takserver-package-{LATEST_MODIFIED_TAG}/federation-hub/build/distributions/takserver-fed-hub_{LATEST_MODIFIED_TAG}_all.deb')

    DEB_FULL = (
        VirtualMachine.UBUNTU_2204, f'takserver',
        f'{TAK_ROOT}/takserver-package-{INITIAL_TAG}/takserver/build/distributions/takserver_{INITIAL_TAG}_all.deb',
        f'{TAK_ROOT}/takserver-package-{LATEST_MASTER_TAG}/takserver/build/distributions/takserver_{LATEST_MASTER_TAG}_all.deb',
        f'{TAK_ROOT}/takserver-package-{LATEST_MODIFIED_TAG}/takserver/build/distributions/takserver_{LATEST_MODIFIED_TAG}_all.deb')

    DEB_CORE = (
        VirtualMachine.UBUNTU_2204, f'takserver',
        f'{TAK_ROOT}/takserver-package-{INITIAL_TAG}/launcher/build/distributions/takserver-core_{INITIAL_TAG}_all.deb',
        f'{TAK_ROOT}/takserver-package-{LATEST_MASTER_TAG}/launcher/build/distributions/takserver-core_{LATEST_MASTER_TAG}_all.deb',
        f'{TAK_ROOT}/takserver-package-{LATEST_MODIFIED_TAG}/launcher/build/distributions/takserver-core_{LATEST_MODIFIED_TAG}_all.deb')

    def __init__(self, vm: VirtualMachine, service_identifier: str, initial_package_path: str,
                 unpatched_upgrade_package_path: str,
                 patched_upgrade_package_path: str):
        self.vm = vm
        self.service_identifier = service_identifier
        self.initial_package_path = initial_package_path
        self.unpatched_upgrade_package_path = unpatched_upgrade_package_path
        self.patched_upgrade_package_path = patched_upgrade_package_path

    def initialize_vm(self):
        vm = self.vm
        vm.restore_base_snapshot()
        vm.copy_file(self.initial_package_path)
        vm.copy_file(self.unpatched_upgrade_package_path)
        vm.copy_file(self.patched_upgrade_package_path)
        vm.copy_file(DEB_CERT_BACKUP_SCRIPT)
        vm.copy_file(CERTS_PATH)

    def install_initial_package(self):
        # Install it
        if self.initial_package_path.endswith('.rpm'):
            self.vm.execute_command(
                ['sudo', 'dnf', 'install', '-y', f'${{HOME}}/{os.path.basename(self.initial_package_path)}'])
            self.vm.execute_command(
                ['if [ -f /opt/tak/apply-selinux.sh ];then cd /opt/tak ; sudo ./apply-selinux.sh ; fi'])
            self.vm.execute_command([
                'if [ -f /opt/tak/federation-hub/apply-selinux.sh ];then cd /opt/tak/federation-hub ; sudo ./apply-selinux.sh ; fi'])
        elif self.initial_package_path.endswith('.deb'):
            self.vm.execute_command(
                ['sudo', 'dpkg', '-i', f'${{HOME}}/{os.path.basename(self.initial_package_path)}'])
        else:
            raise Exception('Unexpected package type!')

        self.vm.execute_command(
            ['if [ -d /opt/tak/certs ];then sudo cp -R ${HOME}/test_certs /opt/tak/certs/files ; fi'])
        self.vm.execute_command([
            'if [ -d /opt/tak/federation-hub/certs ];then sudo cp -R ${HOME}/test_certs /opt/tak/federation-hub/certs/files ; fi'])

        if self.service_identifier is not None:
            self.vm.execute_command(['sudo', 'systemctl', 'daemon-reload'])
            self.vm.execute_command(['sudo', 'systemctl', 'enable', self.service_identifier])
            self.vm.execute_command(['sudo', 'systemctl', 'restart', self.service_identifier])
            time.sleep(10)

    def install_patched_package(self):
        if self.initial_package_path.endswith('.rpm'):
            self.vm.execute_command(
                ['sudo', 'dnf', 'install', '-y', f'${{HOME}}/{os.path.basename(self.patched_upgrade_package_path)}'])
        elif self.initial_package_path.endswith('.deb'):
            self.vm.execute_command(
                ['sudo', 'dpkg', '-i', f'${{HOME}}/{os.path.basename(self.patched_upgrade_package_path)}'])
        else:
            raise Exception('Unexpected package type!')

    def install_unpatched_package(self):
        if self.initial_package_path.endswith('.rpm'):
            self.vm.execute_command(
                ['sudo', 'dnf', 'install', '-y', f'${{HOME}}/{os.path.basename(self.unpatched_upgrade_package_path)}'])

        elif self.initial_package_path.endswith('.deb'):
            self.vm.execute_command(
                ['sudo', 'dpkg', '-i', f'${{HOME}}/{os.path.basename(self.unpatched_upgrade_package_path)}'])
        else:
            raise Exception('Unexpected package type!')


class FileState:
    def __init__(self, test_file: 'TestFile', installation_package: InstallationPackage,
                 upgrade_scenario: UpgradeScenario, initially_exists: bool):
        self.test_file = test_file
        self._installation_package = installation_package
        self._initially_exists = initially_exists
        self._upgrade_scenario = upgrade_scenario
        self._subsequently_exists = None
        self._changes_preserved = None
        self._new_version_saved = None

    @property
    def initially_exists(self):
        return self._initially_exists

    @property
    def subsequently_exists(self):
        return self._subsequently_exists

    @property
    def changes_preserved(self):
        return self._changes_preserved

    @property
    def new_version_saved(self):
        return self._new_version_saved

    @classmethod
    def check_and_tag_initial_files(cls, vm: VirtualMachine, upgrade_scenario: UpgradeScenario,
                                    installation_package: InstallationPackage) -> List['FileState']:
        results = list()
        for file in list(TestFile):
            cmd = TOUCH_TEMPLATE.format(filepath=file.path,
                                        tag_line=f'{file.comment_prefix}{TRACKING_TAG}{file.comment_postfix}')
            vm.execute_command([cmd])
            try:
                vm.execute_command(['sudo', 'ls', file.path])
                initially_exists = True
            except CalledProcessError:
                initially_exists = False
            results.append(FileState(file, installation_package, upgrade_scenario, initially_exists))
        return results

    def validate_changes_preserved(self, vm: VirtualMachine, upgrade_scenario: UpgradeScenario):
        try:
            vm.execute_command(['sudo', 'ls', self.test_file.path])
            self._subsequently_exists = True
        except CalledProcessError:
            self._subsequently_exists = False

        if self._subsequently_exists:
            try:
                vm.execute_command(['sudo', 'grep', TRACKING_TAG, self.test_file.path])
                self._changes_preserved = True
            except CalledProcessError:
                self._changes_preserved = False

        if self.test_file.backup_new_version:
            try:
                vm.execute_command(['sudo', 'ls', f'{self.test_file.path}.dist.{upgrade_scenario.value}'])
                self._new_version_saved = True
            except CalledProcessError:
                self._new_version_saved = False
        else:
            self._new_version_saved = False

    @classmethod
    def _produce_headers(cls) -> List[str]:
        return ['Package', 'Upgrade', 'File', 'Initially Exists', 'Exists After Upgrade',
                'Changes Preserved', 'New Version Saved', 'Passing']

    @classmethod
    def _produce_rows(cls, states: List['FileState']) -> List:
        rows = list()
        upgrade_scenario = None
        installation_package = None
        for state in states:
            if upgrade_scenario is None:
                upgrade_scenario = state._upgrade_scenario
            else:
                if not upgrade_scenario == state._upgrade_scenario:
                    raise Exception('Upgrade scenarios do not match!')

            if installation_package is None:
                installation_package = state._installation_package
            else:
                if not installation_package == state._installation_package:
                    raise Exception('Installation packages do not match!')

            if state.initially_exists:
                if state.subsequently_exists and state.changes_preserved:
                    if state.test_file.backup_new_version:
                        if state.new_version_saved:
                            result = True
                        else:
                            result = False
                    else:
                        result = True
                else:
                    result = False
            else:
                result = True

            row = [
                state._installation_package.name,
                state._upgrade_scenario.name,
                state.test_file.name,
                str(state.initially_exists),
                str(state.subsequently_exists),
                str(state.changes_preserved),
                str(state.new_version_saved),
                result
            ]
            rows.append(row)
        return rows

    @classmethod
    def display_state(cls, states: List['FileState']):
        headers = cls._produce_headers()
        rows = cls._produce_rows(states)
        display_chart(headers, rows)

    @classmethod
    def save_state(cls, states: List['FileState'], output_directory: str):
        headers = cls._produce_headers()
        rows = cls._produce_rows(states)
        installation_package = states[0]._installation_package.name
        upgrade_scenario = states[0]._upgrade_scenario.name

        save_path = os.path.join(output_directory, f'{installation_package}-{upgrade_scenario}.csv')
        with open(save_path, 'w', newline='') as csvfile:
            writer = csv.writer(csvfile)
            writer.writerow(headers)
            for row in rows:
                writer.writerow(row)

    @classmethod
    def from_csv_row(cls, row: List):
        result = cls(test_file=TestFile[row[2]],
                     installation_package=InstallationPackage[row[0]],
                     upgrade_scenario=UpgradeScenario[row[1]],
                     initially_exists=row[3] == 'True')
        result._subsequently_exists = row[4] == 'True'
        result._changes_preserved = row[5] == 'True'
        result._new_version_saved = row[6] == 'True'
        return result

    @classmethod
    def load_results(cls, csv_filepath: str) -> List['FileState']:
        reader = csv.reader(open(csv_filepath, 'r'))
        headers = None
        file_states = list()
        for row in reader:
            if headers is None:
                headers = row
            else:
                file_states.append(cls.from_csv_row(row))
        return file_states

    @classmethod
    def analyze_results(cls, result_directory: str):
        results = dict()
        headers = ['File', 'Package', 'Changes Preserved (master)', 'Changes Preserved (modified)',
                   'New Version Saved (master)', 'New Version Saved (modified)', 'Change']

        for file in glob.glob(f'{result_directory}/*.csv'):
            execution_result = cls.load_results(file)

            for file_state in execution_result:
                if file_state.test_file not in results:
                    results[file_state.test_file] = dict()
                test_file = results[file_state.test_file]

                if file_state._installation_package not in test_file:
                    test_file[file_state._installation_package] = dict()
                package = test_file[file_state._installation_package]

                if file_state._upgrade_scenario in package:
                    raise Exception('Duplicate Entry detected!')

                package[file_state._upgrade_scenario] = file_state

        rows = list()
        for test_file in sorted(results.keys(), key=lambda x: x.name):
            for package in sorted(results[test_file].keys(), key=lambda x: x.name):
                master_changes_preserved = \
                    results[test_file][package][UpgradeScenario.LATEST_MASTER]._changes_preserved
                modified_changes_preserved = \
                    results[test_file][package][UpgradeScenario.LATEST_MODIFIED]._changes_preserved
                master_new_version_saved = \
                    results[test_file][package][UpgradeScenario.LATEST_MASTER]._new_version_saved
                modified_new_version_saved = \
                    results[test_file][package][UpgradeScenario.LATEST_MODIFIED]._new_version_saved

                if master_changes_preserved:
                    if modified_changes_preserved:
                        change_delta = 'Preserved'
                    else:
                        change_delta = 'Broken'
                else:
                    if modified_changes_preserved:
                        change_delta = 'Fixed'
                    else:
                        change_delta = 'Untouched'

                if master_new_version_saved:
                    if modified_new_version_saved:
                        modify_delta = 'Preserved'
                    else:
                        modify_delta = 'Broken'
                else:
                    if modified_new_version_saved:
                        modify_delta = 'Fixed'
                    else:
                        modify_delta = 'Untouched'

                if change_delta == 'Preserved':
                    if modify_delta == 'Preserved':
                        overall_delta = 'Preserved'
                    elif modify_delta == 'Broken':
                        overall_delta = 'Broken'
                    elif modify_delta == 'Untouched':
                        overall_delta = 'Preserved'
                    elif modify_delta == 'Fixed':
                        overall_delta = 'Fixed'
                    else:
                        raise Exception(f'Unexpected modify delta {modify_delta}')
                elif change_delta == 'Broken':
                    overall_delta = 'Broken'

                elif change_delta == 'Untouched':
                    if modify_delta == 'Preserved':
                        overall_delta = 'Preserved'
                    elif modify_delta == 'Broken':
                        overall_delta = 'Broken'
                    elif modify_delta == 'Untouched':
                        overall_delta = 'Untouched'
                    elif modify_delta == 'Fixed':
                        overall_delta = 'Fixed'
                    else:
                        raise Exception(f'Unexpected modify delta {modify_delta}')

                elif change_delta == 'Fixed':
                    if modify_delta == 'Preserved':
                        overall_delta = 'Fixed'
                    elif modify_delta == 'Broken':
                        overall_delta = 'Broken'
                    elif modify_delta == 'Untouched':
                        overall_delta = 'Fixed'
                    elif modify_delta == 'Fixed':
                        overall_delta = 'Fixed'
                    else:
                        raise Exception(f'Unexpected modify delta {modify_delta}')

                else:
                    raise Exception(f'Unexpected change delta {change_delta}')

                if overall_delta != 'Untouched':
                    row = [
                        test_file.name,
                        package.name,
                        master_changes_preserved,
                        modified_changes_preserved,
                        master_new_version_saved,
                        modified_new_version_saved,
                        overall_delta
                    ]
                    rows.append(row)

        display_chart(headers, rows)


parser = argparse.ArgumentParser()
subparsers = parser.add_subparsers(dest='command')

single_test_parser = subparsers.add_parser('run-single-test')
single_test_parser.add_argument('installer', metavar='INSTALLER',
                                choices=list(map(lambda x: x.name, list(InstallationPackage))))
single_test_parser.add_argument('test_scenario', metavar='TEST_SCENARIO',
                                choices=list(map(lambda x: x.name, list(UpgradeScenario))))

all_tests_parser = subparsers.add_parser('run-all-tests')
all_tests_parser.add_argument('output_directory', metavar='OUTPUT_DIRECTORY', type=str)

analysis_parser = subparsers.add_parser('analyze-results')
analysis_parser.add_argument('results_directory', metavar='RESULTS_DIRECTORY', type=str)


class TestRunner:
    def __init__(self, package: InstallationPackage, scenario: UpgradeScenario):
        self.package = package
        self.scenario = scenario

    def execute_scenario(self, results_directory: str = None):
        package = self.package
        vm = package.vm

        print(f'Rolling back VM {vm.vm_identifier} to a clean state...')
        package.initialize_vm()

        print(f'Installing initial package {os.path.basename(package.initial_package_path)}...')
        package.install_initial_package()

        print('Checking initially installed files and tagging them...')
        file_states = FileState.check_and_tag_initial_files(vm, self.scenario, self.package)

        vm.execute_command(['bash', f'${{HOME}}/deb-cert-metadata-backup.sh'])
        # if vm == VirtualMachine.UBUNTU_2204:
        #     vm.execute_command([
        #         'if [ -f  /opt/tak/certs/cert-metadata.sh ] ; then sudo cp /opt/tak/certs/cert-metadata.sh /opt/tak/certs/cert-metadata.sh.bak ; fi'])
        #     vm.execute_command([
        #         'if [ -f /opt/tak/certs/config.cfg ] ; then sudo cp /opt/tak/certs/config.cfg /opt/tak/certs/config.cfg.bak ; fi'])

        if self.scenario == UpgradeScenario.LATEST_MASTER:
            print(f'Installing unpatched package {os.path.basename(package.unpatched_upgrade_package_path)}...')
            package.install_unpatched_package()
        elif self.scenario == UpgradeScenario.LATEST_MODIFIED:
            print(f'Installing modified package {os.path.basename(package.patched_upgrade_package_path)}...')
            package.install_patched_package()
        else:
            raise Exception(f'Unexpected scenario {self.scenario}!')

        print('Validating preservation and presence after upgrade...')
        for file in file_states:
            file.validate_changes_preserved(vm, self.scenario)

        FileState.display_state(file_states)
        if results_directory is not None:
            FileState.save_state(file_states, results_directory)


def main():
    args = parser.parse_args()

    if args.command == 'run-single-test':
        package = InstallationPackage[args.installer]
        scenario = UpgradeScenario[args.test_scenario]
        runner = TestRunner(package, scenario)
        runner.execute_scenario()

    elif args.command == 'run-all-tests':
        output_directory = args.output_directory
        if os.path.exists(output_directory):
            print(f'The output directory {output_directory} already exists!')
            exit(1)
        os.mkdir(output_directory)
        for package in list(InstallationPackage):
            for scenario in list(UpgradeScenario):
                runner = TestRunner(package, scenario)
                runner.execute_scenario(output_directory)

    elif args.command == 'analyze-results':
        results_directory = args.results_directory
        if not os.path.exists(results_directory):
            print(f'The results directory {results_directory} does not exist!')
            exit(1)
        FileState.analyze_results(args.results_directory)

    else:
        parser.print_usage()


if __name__ == '__main__':
    main()
