#!/usr/bin/env python3

import argparse
import os
import shutil
import subprocess
import time
from typing import Optional, List
import uuid

SCRIPT_DIR = os.path.abspath(os.path.dirname(os.path.realpath(__file__)))

ROOT_DIR = os.path.abspath(os.path.join(SCRIPT_DIR, '../../'))

DEFAULT_IMAGE_VERSION = 'SNAPSHOT'
DEFAULT_TARGET_CONTAINER = 'takcl'

parser = argparse.ArgumentParser(description='TAKCL Docker Container Creator')

parser.add_argument('--ssh-public-key-filepath', '-k', type=str,
                    help='The public key used to allow SSH access via the "tak" user')

parser.add_argument('--target-container', '-t', type=str, default=DEFAULT_TARGET_CONTAINER,
                    help='The target container to deploy to. Default: ' + DEFAULT_TARGET_CONTAINER)

parser.add_argument('--tag-version', '-v', type=str, default=DEFAULT_IMAGE_VERSION,
                    help='The label to apply to the created image. Default: "' + DEFAULT_IMAGE_VERSION)

parser.add_argument('--tag-latest', '-l', action='store_true', default=False,
                    help='Tag the deployed container with the "latest" tag')

parser.add_argument('--build-rpm', action='store_true', default=False,
                    help='If provided, the parent project will be built and used to initialize the base system')
# parser.add_argument('--no-build', action='store_true', default=False,
#                     help='Skips "./gradlew clean buildRpm"')

parser.add_argument('--use-rpm', '-r', type=str,
                    help='If provided, the contents of this RPM will be used to set up the base system')


def _exec(command: List[str], cwd: str = None, return_output: bool = False):
    if return_output:
        rval = subprocess.run(command, cwd=cwd, stdout=subprocess.PIPE)
    else:
        rval = subprocess.run(command, cwd=cwd)

    if rval.returncode != 0:
        print('Command failed with return code ' + str(rval.returncode) + '! Command: ' + str(command))
        exit(1)

    if return_output:
        return rval.stdout


def prep_rpm(execute_build: bool, base_rpm: Optional[str] = None):
    if base_rpm is not None:
        if not os.path.exists(base_rpm):
            print('The provided RPM "' + base_rpm + '" does not exist!')
            exit(1)
        shutil.copy(base_rpm, os.path.join(SCRIPT_DIR, 'takserver-base.rpm'))

    else:
        if execute_build:
            _exec(['./gradlew', 'clean', 'buildRpm'], cwd=ROOT_DIR)

            dist_dir = os.path.join(ROOT_DIR, 'takserver-package', 'build', 'distributions')
            if not os.path.exists(dist_dir):
                print('The path "' + dist_dir + ' could not be found!"!')
                exit(1)

            tak_rpms = list(filter(lambda x: x.startswith('takserver') and x.endswith('.rpm'), os.listdir(dist_dir)))
            tak_rpms.sort(reverse=True)

            if len(tak_rpms) == 0:
                print('No "takserver-*.rpm" files could be found in "' + dist_dir + '"!')
                exit(1)

            shutil.copy(os.path.join(dist_dir, tak_rpms[0]), os.path.join(SCRIPT_DIR, 'takserver-base.rpm'))


if __name__ == '__main__':
    args = parser.parse_args()
    print(args)
    cmd = ['docker', 'build']

    if args.ssh_public_key_filepath is not None:
        pub_key = open(args.ssh_public_key_filepath, 'r').read()
        print("key: " + pub_key)
        cmd.append('--build-arg')
        cmd.append('PUBLIC_KEY=' + pub_key)

    prep_rpm(args.build_rpm, args.use_rpm)

    cmd.append('--tag')
    cmd.append(args.target_container + ':' + args.tag_version)

    if args.tag_latest:
        cmd.append('--tag')
        cmd.append(args.target_container + ':latest')

    cmd.append('.')

    _exec(cmd)

    if args.build_rpm or args.use_rpm is not None:
        container_name = args.target_container + '-tmp-' + str(uuid.uuid4())[-12:]
        _exec(['docker', 'run', '--name', container_name, '-d', args.target_container + ':' + args.tag_version])
        time.sleep(10)
        _exec(['docker', 'exec', '-it', container_name, 'bash', '/init_files/init-db.sh'])
        _exec(['docker', 'stop', container_name])
        _exec(['docker', 'commit', container_name, args.target_container + ':' + args.tag_version])
        if args.tag_latest:
            _exec(['docker', 'tag', args.target_container + ':' + args.tag_version, args.target_container + ':latest'])
        _exec(['docker', 'rm', container_name])
