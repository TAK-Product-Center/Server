#!/usr/bin/env python

import argparse
import os
import re
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument('--good-hash', '-g', type=str, required=True, help='The last known good hash')
parser.add_argument('--failing-test', '-f', type=str, required=True, help='The failing test to execute')
parser.add_argument('--core-network-version', '-n', type=int, choices=[1, 2],
        help='The core network version to use. By default, no specific version is specified.')


SCRIPT_DIR = os.path.abspath(os.path.dirname(os.path.realpath(__file__)))
GIT_ROOT = os.path.realpath(os.path.join(SCRIPT_DIR, '../../../'))
BUILD_ROOT = os.path.realpath(os.path.join(SCRIPT_DIR, '../../'))

FINISHED_REGEX = r'(?P<hash>[a-z0-9]*) is the first bad commit.*'

RESULTS_DIR = 'BISECT_LOGS'


def _exec_rval(cmd, directory=None, file_label=None):
    if file_label is not None:
        stdout_out = open(os.path.join(RESULTS_DIR, 'bisect_cmd_stdout.txt'), 'w')
        stderr_out = open(os.path.join(RESULTS_DIR, 'bisect_cmd_stderr.txt'), 'w')
    else:
        stdout_out = open(os.path.join(RESULTS_DIR + file_label + '-stdout.txt'), 'w')
        stderr_out = open(os.path.join(RESULTS_DIR + file_label + '-stderr.txt'), 'w')

    stdout_out.write('CMD: [' + ' '.join(cmd) + ']\n')
    stderr_out.write('CMD: [' + ' '.join(cmd) + ']\n')

    return subprocess.call(cmd, cwd=directory, stdout=stdout_out, stderr=stderr_out)


def _exec_output(cmd, directory=None):
    try:
        result = subprocess.check_output(cmd, cwd=directory)
        return result
    except CalledProcessError as e:
        print('FAILED EXEC: [' + ' '.join(cmd) + ']')
        raise e


def _exec_test(test, core_network_version):
    hash = _exec_output(['git', 'rev-parse', 'HEAD']).strip()
    print(hash + ': Building...')
    cmd = ['./gradlew', 'clean', 'buildrpm']
    result = _exec_rval(cmd, directory=BUILD_ROOT, file_label=hash + '-buildrpm')
    if result != 0:
        print(hash + ': Failed Command [' + ' '.join(cmd) + ']!')
        return result

    print(hash + ': Testing...')
    if core_network_version is None:
        cmd = ['./gradlew', 'integrationTest', '--tests', test, '--fail-fast']
    else:
        cmd = ['./gradlew', '-Dcom.bbn.marti.takcl.network.version=' + str(core_network_version), 'integrationTest', '--tests', test, '--fail-fast']

    result = _exec_rval(cmd, directory=BUILD_ROOT, file_label=hash + '-test')
    if result != 0:
        print(hash + ': Failed Test ' + test)

    return result


def main():
    args = parser.parse_args()

    os.mkdir(RESULTS_DIR)

    _exec_output(['git', 'bisect', 'start'], directory=GIT_ROOT)
    _exec_output(['git', 'bisect', 'bad'], directory=GIT_ROOT)
    _exec_output(['git', 'bisect', 'good', args.good_hash], directory=GIT_ROOT)

    finished = False

    while not finished:
        result = _exec_test(args.failing_test, args.core_network_version)

        if result == 0:
            rv = _exec_output(['git', 'bisect', 'good'], directory=GIT_ROOT)
        else:
            rv = _exec_output(['git', 'bisect', 'bad'], directory=GIT_ROOT)

        match = re.match(FINISHED_REGEX, rv)
        if match is not None:
            print('{ "First Bad Hash" : "' + match.groupdict()['hash'] + '"}')
            finished = True


if __name__ == '__main__':
    main()
