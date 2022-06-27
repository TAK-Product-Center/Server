#!/usr/bin/env python
import argparse
import json
import os
import shutil
import subprocess
import time
from enum import Enum

parser = argparse.ArgumentParser()
parser.add_argument('--key-name', type=str, required=True,
                    help="The key name to use to allow SCP access to the server. This should match the local key.")
parser.add_argument('--key-location', type=str, help="The location of the key matching the specified key name")
parser.add_argument('--keep-running', action='store_true',
                    help='Keeps the instance used for the tests running. ' +
                         'Make sure to terminate it manually if you use this option!')
parser.add_argument('--core-tests', '-c', type=str, action='append',
                    help='Specifies specific tests to be executed on the server for takserver-core')
parser.add_argument('--fail-fast', action='store_true',
                    help='Appends the "--fail-fast" command to the gradle tests, stopping on the first test failure.')

INSTANCE_PREFIX = 'takserver-integrationtests-'

BUCKET = 'takserver-integrationtest-config'

FINISH_CHECK_INTERVAL_SEC = 60

TEST_TIMEOUT_SEC = 21600
TEST_CHECK_INTERVAL_SEC = 240

TAKSERVER_GIT_ROOT_NAME = 'takserver-archive'
PUBLISH_ARCHIVE = TAKSERVER_GIT_ROOT_NAME + '.tar.gz'
RESULTS_NAME = 'integration_test_results'

ROOT_BUCKET = 'takserver-integrationtest'


def get_return_value(cmd, cwd=None):
    print('EXEC [' + ' '.join(cmd) + ']')
    return subprocess.call(cmd, cwd=cwd)


def get_output(cmd, cwd=None):
    print('EXEC [' + ' '.join(cmd) + ']')
    try:
        output = subprocess.check_output(cmd, cwd=cwd)
    except subprocess.CalledProcessError as e:
        raise e
    return output


class ArtifactHelper:
    def __init__(self):
        self.label = INSTANCE_PREFIX + get_output(['git', 'rev-parse', 'HEAD'])[:8]

        self._tmp_dir = self.label
        os.mkdir(self._tmp_dir)
        self._tmp_archive = os.path.join(self._tmp_dir, PUBLISH_ARCHIVE)
        self._tmp_git_dir = os.path.join(self._tmp_dir, TAKSERVER_GIT_ROOT_NAME)

        # remove_us = [
        #     self._tmp_archive,
        #     self._tmp_git_dir
        # ]
        # remove_us = [INTERMEDIATE_DIRECTORY,
        #              PUBLISH_ARCHIVE,
        #              'integration_test_results',
        #              'integration_test_results.tar.gz']
        # for file in remove_us:
        #     if os.path.exists(file):
        #         raise Exception("Please remove the following files from a previous run to continue:" +
        #                         '\n\t'.join(remove_us))

    def push_to_s3(self, local_file, target_file):
        get_output(
            ['aws', 's3', 'cp', local_file, 's3://' + ROOT_BUCKET + '/' + self.label + '/' + target_file])

    def pull_from_s3(self, remote_file, local_target):
        get_output(['aws', 's3', 'cp', 's3://' + ROOT_BUCKET + '/' + self.label + '/' + remote_file, local_target])

    def publish_input(self):
        tag = get_output(['git', 'describe', '--tags']).split('\n')[0]
        branch = get_output(['git', 'rev-parse', '--abbrev-ref', 'HEAD']).strip()
        get_output([
            'git', 'clone', '--depth', '1', '--branch', branch,
            'ssh://git@git.takmaps.com/core/takserver.git', self._tmp_git_dir])
        get_output(['git', 'tag', tag], cwd=self._tmp_git_dir)
        get_output(['tar', 'cvzf', PUBLISH_ARCHIVE, TAKSERVER_GIT_ROOT_NAME], cwd=self._tmp_dir)
        shutil.rmtree(self._tmp_git_dir)
        self.push_to_s3(self._tmp_archive, PUBLISH_ARCHIVE)
        os.remove(self._tmp_archive)

    def fetch_output(self):
        self.pull_from_s3(RESULTS_NAME + '.tar.gz', '.')

    # def cleanup_s3_bucket(self):
    #     # Hardcoding to be extra careful
    #     if not self.label.startswith('takserver-integrationtests-'):
    #         raise Exception("Could not delete S3 artifacts!")
    #     get_output(['aws', 's3', 'rm', 's3://takserver-integrationtest/' + self.label + '/' + PUBLISH_ARCHIVE])
    #     get_output(['aws', 's3', 'rm', 's3://takserver-integrationtest/' + self.label + '/' + RESULTS_NAME + '.tar.gz'])



class StackStatusMapping(Enum):
    PENDING = (['CREATE_IN_PROGRESS'])
    SUCCESSFUL = (['CREATE_COMPLETE'])
    ERROR = (['CREATE_FAILED', 'ROLLBACK_IN_PROGRESS', 'ROLLBACK_FAILED', 'ROLLBACK_COMPLETE'])
    UNKNOWN = ([
        'DELETE_COMPLETE',
        'DELETE_FAILED',
        'DELETE_IN_PROGRESS',
        'REVIEW_IN_PROGRESS',
        'UPDATE_COMPLETE',
        'UPDATE_COMPLETE_CLEANUP_IN_PROGRESS',
        'UPDATE_IN_PROGRESS',
        'UPDATE_ROLLBACK_COMPLETE',
        'UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS',
        'UPDATE_ROLLBACK_FAILED',
        'UPDATE_ROLLBACK_IN_PROGRESS',
        'IMPORT_IN_PROGRESS',
        'IMPORT_COMPLETE',
        'IMPORT_ROLLBACK_IN_PROGRESS',
        'IMPORT_ROLLBACK_FAILED',
        'IMPORT_ROLLBACK_COMPLETE'
    ])

    def __init__(self, aws_status_values):
        self._aws_status_values = aws_status_values

    @classmethod
    def get_status(cls, aws_status):
        for value in list(cls):
            if aws_status in value._aws_status_values:
                return value

        raise Exception('Unexpected AWS Stack Status "' + aws_status + '"!')


class AwsCloudInstanceState:
    def __init__(self, state, kill_instance=True):
        self.stack_id = state['StackId']
        self.stack_name = state['StackName']
        self.creation_time = state['CreationTime']
        self.stack_status = state['StackStatus']
        self._kill_instance = kill_instance

    @staticmethod
    def get_instance_names():
        return json.loads(get_output(['aws', 'cloudformation', 'list-stacks', '--query', 'StackSummaries[].StackName']))

    @staticmethod
    def from_stack_id(stack_id, kill_instance=True):
        return AwsCloudInstanceState(json.loads(get_output(
            ['aws', 'cloudformation', 'describe-stacks', '--stack-name', stack_id]))['Stacks'][0], kill_instance)

    @staticmethod
    def _create_modified_cloudformation(label, target_file, core_tests=None, fail_fast=False):
        cf = json.load(open(os.path.join(os.path.dirname(os.path.realpath(__file__)), 'cloudformation.json')))

        # Set the location of the published artifact
        cf['Resources']['IntegrationTestInstance']['Metadata']['AWS::CloudFormation::Init']['Configure']['files'][
            '/home/centos/takserver-archive.tar.gz']['source'] = (
                'http://' + ROOT_BUCKET + '.s3.amazonaws.com/' + label + '/' + PUBLISH_ARCHIVE)

        # Set the command to copy the results back to the s3 bucket
        cf['Resources']['IntegrationTestInstance']['Metadata']['AWS::CloudFormation::Init']['Configure']['commands'][
            '06_publish_results']['command'] = ['aws', 's3', 'cp', 'integration_test_results.tar.gz',
                                                's3://takserver-integrationtest/' + label + '/integration_test_results.tar.gz']

        if core_tests is not None:
            test_str = ''
            fail_fast_str = '' if not fail_fast else ' --fail-fast'
            for test in core_tests:
                test_str = test_str + ' --tests ' + test

            cf['Resources']['IntegrationTestInstance']['Metadata']['AWS::CloudFormation::Init']['Configure'][
                'commands']['03_run_core_tests'][
                'command'] = "su centos -c \"bash ../gradlew integrationTest" + test_str + fail_fast_str + "\""

        if fail_fast:
            cf['Resources']['IntegrationTestInstance']['Metadata']['AWS::CloudFormation::Init']['Configure'][
                'commands']['04_run_usermanager_tests'][
                'command'] = "su centos -c \"bash ../gradlew integrationTest --fail-fast\""


        # Save the modified file
        json.dump(cf, open(target_file, 'w'), indent=4)

    @staticmethod
    def create_new(keyname, label, kill_instance=True, core_tests=None, fail_fast=False):
        if label in AwsCloudInstanceState.get_instance_names():
            raise Exception('A staged or running test with the identifier "' + label + '" already exists!')

        target_file = label + '-cloudformation.json'

        AwsCloudInstanceState._create_modified_cloudformation(label, target_file, core_tests, fail_fast)

        # Upload the file to aws
        print(
            get_output(['aws', 's3', 'cp', target_file, 's3://' + ROOT_BUCKET + '/' + label + '/cloudformation.json']))

        # Delete the target file
        os.remove(target_file)

        cmd = ['--template-url', 'https://' + ROOT_BUCKET + '.s3.amazonaws.com/cloudformation.json']

        # Start the tests
        output_str = get_output(['aws', 'cloudformation', 'create-stack', '--stack-name', label,
                                 '--template-url',
                                 'https://' + ROOT_BUCKET + '.s3.amazonaws.com/' + label + '/cloudformation.json',
                                 '--parameters', 'ParameterKey=KeyName,ParameterValue=' + keyname,
                                 '--region', 'us-east-1', '--capabilities=CAPABILITY_IAM'])

        stackId = json.loads(output_str)['StackId']

        return AwsCloudInstanceState.from_stack_id(stackId, kill_instance)

    def _update(self):
        self.stack_status = json.loads(get_output(
            ['aws', 'cloudformation', 'describe-stacks', '--stack-name', self.stack_name]))['Stacks'][0]['StackStatus']

    def wait_for_completion(self, timeout_sec, check_interval):
        print('Waiting for the execution to finish within ' + str(timeout_sec) + ' seconds...')
        stack_status_mapping = StackStatusMapping.PENDING
        time_left = timeout_sec
        while stack_status_mapping == StackStatusMapping.PENDING and time_left > 0:
            time.sleep(check_interval)
            self._update()
            stack_status_mapping = StackStatusMapping.get_status(self.stack_status)
            time_left = time_left - check_interval

        if stack_status_mapping != StackStatusMapping.SUCCESSFUL:
            if time_left <= 0:
                raise Exception('The timeout of ' + str(timeout_sec) + ' seconds has been reached!')
            elif stack_status_mapping == StackStatusMapping.ERROR:
                raise Exception('Stack Error: "' + self.stack_status + '"!')
            elif stack_status_mapping == StackStatusMapping.UNKNOWN:
                raise Exception('Stack Unknown State: "' + self.stack_status + '"!')
            else:
                raise Exception('Unexpected StackStatusMapping value of "' + str(stack_status_mapping) + '"!')

    def delete(self):
        get_output(['aws', 'cloudformation', 'delete-stack', '--stack-name', self.stack_name])


def deploy(keyname, key_location=None, kill_instance=True, core_tests=None, fail_fast=False):
    ah = ArtifactHelper()
    ah.publish_input()
    label = ah.label
    instance_state = AwsCloudInstanceState.create_new(keyname, label, kill_instance, core_tests, fail_fast)
    instance_state.wait_for_completion(TEST_TIMEOUT_SEC, TEST_CHECK_INTERVAL_SEC)
    ah.fetch_output()
    if kill_instance:
        instance_state.delete()


def main():
    args = parser.parse_args()

    instance_state = deploy(keyname=args.key_name, key_location=args.key_location, kill_instance=not args.keep_running,
                            core_tests=args.core_tests, fail_fast=args.fail_fast)


if __name__ == '__main__':
    main()
