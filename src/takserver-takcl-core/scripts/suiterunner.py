#!/usr/bin/env python3

import argparse
import glob
import os
import subprocess
from typing import Dict
from xml.etree import ElementTree
from xml.etree.ElementTree import Element

import sys

parser = argparse.ArgumentParser()
subparsers = parser.add_subparsers(dest='mode')
run_group = subparsers.add_parser('run')
validate_group = subparsers.add_parser('validate')
validate_group.add_argument('--validation-target', type=str, default=None)

if not os.path.exists('takserver-takcl-core'):
    print("Please start the script from the src directory like './takserver-takcl-core/scripts/testrunner.sh'!",
          file=sys.stderr)
    exit(1)

RESULTS_ROOT = os.path.abspath('TESTRUNNER_RESULTS')

ALL_TESTS = {
    # 'FedHubTests',
    'FedHubTests.advancedFedHubTest',
    'FedHubTests.basicFedHubTest',
    'FedHubTests.basicMultiInputFedHubTest',
    # 'FederationV1Tests',
    'FederationV1Tests.advancedFederationTest',
    'FederationV1Tests.basicFederationTest',
    'FederationV1Tests.basicMultiInputFederationTest',
    'FederationV1Tests.federateConnectionInitiatorWaitTest',
    # 'FederationV2Tests',
    'FederationV2Tests.advancedFederationTest',
    'FederationV2Tests.basicFederationTest',
    'FederationV2Tests.basicMultiInputFederationTest',
    'FederationV2Tests.federateConnectionInitiatorWaitTest',
    # 'GeneralTests',
    'GeneralTests.LatestSAFileAuth',
    'GeneralTests.LatestSAInputGroups',
    'GeneralTests.anonWithGroupInputTest',
    'GeneralTests.groupToNonGroup',
    'GeneralTests.latestSAAnon',
    'GeneralTests.latestSADisconnectTest',
    'GeneralTests.mcastSendTest',
    'GeneralTests.mcastTest',
    'GeneralTests.sslTest',
    'GeneralTests.streamTcpTest',
    'GeneralTests.tcpTest',
    'GeneralTests.udpTest',
    # 'InputTests',
    'InputTests.inputRemoveAddTest',
    # 'PluginStartupTests',
    'PluginStartupTests.pluginStartupValiationTest',
    # 'PointToPointTests',
    'PointToPointTests.basicPointToPointTest',
    'PointToPointTests.callsignIdentificationTest',
    'PointToPointTests.mixedIdentificationTest',
    'PointToPointTests.uidIdentificationTest',
    # 'StartupTests',
    'StartupTests.jarStartupValiationTest',
    # 'StreamingDataFeedsTests',
    'StreamingDataFeedsTests.dataFeedRemoveAddTest',
    # 'SubscriptionTests',
    'SubscriptionTests.clientSubscriptions',
    # 'UserManagementTests',
    'UserManagementTests.UserManagerTest',
    # 'WebsocketsFederationTests',
    'WebsocketsFederationTests.advancedWebsocketsFederationV1Test',
    'WebsocketsFederationTests.advancedWebsocketsFederationV2Test',
    # 'WebsocketsTests',
    'WebsocketsTests.basicSecureWebsocketTest',
    'WebsocketsTests.simpleWSS',
    'federationmissions.FederationEnterpriseFileSync',
    'missions.EnterpriseFileSync',
    'missions.MissionAddRetrieveRemove',
    'missions.MissionDataFlowTests',
    'missions.MissionFileSync',
    'missions.MissionUserCustomRolesTests',
    'missions.MissionUserDefaultRolesTests'
}

TEST_EXECUTION_LIST = [
    # 'FedHubTests',
    'FedHubTests.advancedFedHubTest',
    'FedHubTests.basicFedHubTest',
    'FedHubTests.basicMultiInputFedHubTest',
    # 'FederationV1Tests',
    # 'FederationV1Tests.advancedFederationTest',
    # 'FederationV1Tests.basicFederationTest',
    # 'FederationV1Tests.basicMultiInputFederationTest',
    # 'FederationV1Tests.federateConnectionInitiatorWaitTest',
    # 'FederationV2Tests',
    'FederationV2Tests.advancedFederationTest',
    'FederationV2Tests.basicFederationTest',
    'FederationV2Tests.basicMultiInputFederationTest',
    'FederationV2Tests.federateConnectionInitiatorWaitTest',
    'GeneralTests',
    'InputTests',
    'PluginStartupTests',
    'PointToPointTests.basicPointToPointTest',
    'PointToPointTests.callsignIdentificationTest',
    'PointToPointTests.mixedIdentificationTest',
    'PointToPointTests.uidIdentificationTest',
    'StreamingDataFeedsTests',
    'SubscriptionTests',
    'UserManagementTests',
    # 'WebsocketsFederationTests.advancedWebsocketsFederationV1Test',
    'WebsocketsFederationTests.advancedWebsocketsFederationV2Test',
    # 'WebsocketsTests',
    'WebsocketsTests.basicSecureWebsocketTest',
    'WebsocketsTests.simpleWSS',
    'federationmissions.FederationEnterpriseFileSync',
    'missions.EnterpriseFileSync',
    'missions.MissionAddRetrieveRemove',
    'missions.MissionDataFlowTests',
    'missions.MissionFileSync',
    'missions.MissionUserCustomRolesTests',
    'missions.MissionUserDefaultRolesTests',
    'StartupTests'
]

def red(val: str):
    return '\033[5;91m' + val + '\033[0m'
    pass

def green(val: str):
    return '\033[33;32m' + val + '\033[0m'

def display_results(pass_fail_dict: Dict[str, bool], duration_dict: Dict):
    pass_fail_dict = dict(pass_fail_dict)
    test_name_column_width = 0
    for value in ALL_TESTS:
        test_name_column_width = max(test_name_column_width, len(value))
    test_name_column_width = test_name_column_width + 4

    all_tests_sorted = sorted(ALL_TESTS)

    print("|=======================================TEST RESULTS=======================================|")
    print("| " + "Test Name".ljust(test_name_column_width) + '|  Result | Duration (s) |')

    for test_name in all_tests_sorted:
        if test_name in pass_fail_dict:
            if pass_fail_dict[test_name]:
                print('| ' + test_name.ljust(test_name_column_width) + '|    ' +
                      green('PASS') + ' | ' + str(int(duration_dict[test_name])).rjust(12) + ' |')
            else:
                print('| ' + test_name.ljust(test_name_column_width) + '|    ' +
                      red('FAIL') + ' | ' + str(int(duration_dict[test_name])).rjust(12) + ' |')
            pass_fail_dict.pop(test_name)
        else:
            print('| ' + test_name.ljust(test_name_column_width) + '| ' +
                  'NOT RUN' + ' | ' + 'n/a'.rjust(12) + ' |')

    if len(pass_fail_dict) > 0:
        print(red("|==================================UNACCOUNTED FOR TESTS===================================|"))
        for test_name in sorted(pass_fail_dict.keys()):
            if pass_fail_dict[test_name]:
                print('| ' + test_name.ljust(test_name_column_width) + '|    ' +
                      red('PASS') + ' | ' + str(int(duration_dict[test_name])).rjust(8) + ' |')
            else:
                print('| ' + test_name.ljust(test_name_column_width) + '|    ' +
                      red('FAIL') + ' | ' + str(int(duration_dict[test_name])).rjust(8) + ' |')


def validate_results(results_directory: str):
    pass_fail_dict = dict()
    duration_dict = dict()
    for filepath in glob.glob(os.path.join(results_directory, '*')):
        if filepath.endswith('.xml'):
            try:
                tree = ElementTree.parse(filepath)

                root = tree.getroot()  # type: Element

                testcase = root.find('testcase')

                if isinstance(testcase, Element):
                    if (testcase.get('classname').split('.')[-2] == 'missions' or
                        testcase.get('classname').split('.')[-2] == 'federationmissions'):
                        testname = testcase.get('classname').split('.')[-2] + '.' + \
                                   testcase.get('classname').split('.')[-1]
                        if testname in pass_fail_dict:
                            pass_fail_dict[testname] = pass_fail_dict[testname] and testcase.find('error') is None
                            duration_dict[testname] = duration_dict[testname] +  float(testcase.get('time'))
                        else:
                            pass_fail_dict[testname] = testcase.find('error') is None
                            duration_dict[testname] = float(testcase.get('time'))
                    else:
                        testname = testcase.get('classname').split('.')[-1] + '.' + testcase.get('name')
                        pass_fail_dict[testname] = testcase.find('error') is None
                        duration_dict[testname] = float(testcase.get('time'))
            except Exception as e:
                pass

    display_results(pass_fail_dict, duration_dict)


def main():
    args = parser.parse_args()

    if args.mode == 'run':

        for test in TEST_EXECUTION_LIST:
            print("Running " + test + "....")
            cmd = ['./takserver-takcl-core/scripts/testrunner.sh', 'run', test, '--unsafe-mode']
            # print("CMD=" + ' '.join(cmd))
            subprocess.call(cmd)
            print("Finished running " + test + "!")

        validate_results(RESULTS_ROOT)

    elif args.mode == 'validate':
        validation_target = RESULTS_ROOT if args.validation_target is None else args.validation_target

        if not os.path.exists(validation_target):
            print("The specified validation target directory '{d}' does not exist!".format(d=validation_target),
                  file=sys.stderr)
            exit(1)

        validate_results(validation_target)

    else:
        parser.print_help()


if __name__ == '__main__': \
        main()
