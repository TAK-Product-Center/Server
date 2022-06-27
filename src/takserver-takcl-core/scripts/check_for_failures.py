#!/usr/bin/env python3

import argparse
import os
import re
import subprocess
from xml.etree import ElementTree

parser = argparse.ArgumentParser('Test Failure Calculator')

DEFAULT_TESTSUITE_NAME_MATCHER = '[a-zA-Z0-9]*'
NAME_MATCH_REGEX_FORMATTER = r'^[\s\t]*coreNetworkV%s-%s[.].*$'

parser.add_argument('artifact_directory', metavar='ARTIFACT_DIRECTORY', type=str,
                    help='The directory that contains JUnit artifacts that start with "TEST" and end with ".xml". ' +
                         'Child directories will be skipped.')
parser.add_argument('--core-network-version', '-n', type=str, choices=['1', '2'], default='[1-2]',
                    help='The core network version the test should match')
parser.add_argument('--test-suite-identifiers', '-t', nargs='*',
                    help='The tests to include in the calculation of expected test executions')

args = parser.parse_args()
artifact_directory = args.artifact_directory
core_network_version = args.core_network_version
test_suite_identifiers = args.test_suite_identifiers

if test_suite_identifiers is None:
    regex = NAME_MATCH_REGEX_FORMATTER % (core_network_version, DEFAULT_TESTSUITE_NAME_MATCHER)
else:
    ts_identifier_regex = '|'.join(test_suite_identifiers)
    regex = NAME_MATCH_REGEX_FORMATTER % (core_network_version, ts_identifier_regex)

print('Checking expected number of tests... ', end='')

output = subprocess.check_output(['java', '-jar', '/opt/tak/utils/takcl.jar', 'tests', 'list'],
                                 stderr=subprocess.STDOUT).decode().split('\n')
expected_test_count = len(list(filter(lambda x: re.match(regex, x) is not None, output)))

print(str(expected_test_count))

passed_test_count = 0
error_count = 0
failure_count = 0
skipped_count = 0

print("Checking " + artifact_directory + " for JUnit reports...")

for path, subdirectories, files in os.walk(artifact_directory):
    if path == artifact_directory:
        for filepath in files:
            if filepath.startswith('TEST-') and filepath.endswith('.xml'):
                full_path = os.path.join(path, filepath)
                et = ElementTree.fromstring('\n'.join(open(full_path, 'r')))
                passed_test_count = passed_test_count + int(et.get('tests'))
                error_count = error_count + int(et.get('errors'))
                failure_count = failure_count + int(et.get('failures'))
                skipped_count = skipped_count + int(et.get('skipped'))

print('Test Results: ' +
      '\n\tPassed:   ' + str(passed_test_count) + '/' + str(expected_test_count) +
      '\n\tErrors:   ' + str(error_count) + '/' + str(expected_test_count) +
      '\n\tFailures: ' + str(failure_count) + '/' + str(expected_test_count) +
      '\n\tSkipped:  ' + str(skipped_count) + '/' + str(expected_test_count))


if passed_test_count < expected_test_count:
    exit(expected_test_count - passed_test_count)

if passed_test_count > expected_test_count:
    raise Exception("The number of passing tests is greater than the number of expected passing tests!")
