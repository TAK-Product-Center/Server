#!/usr/bin/env python3
# A convenience script to match with current deletion scripts

import os
import subprocess

if __name__ == '__main__':
    script_dir = os.path.join(os.path.dirname(os.path.realpath(__file__)), 'build-generic.py')
    subprocess.check_call(['python3', script_dir, 'uninstall'])
