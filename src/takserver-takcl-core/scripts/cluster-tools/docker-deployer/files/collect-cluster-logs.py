#!/usr/bin/env python3

import os
import subprocess

LOGDIR = 'takserver-cluster-logs'

if os.path.exists(LOGDIR):
    print('Please remove "takserver-cluster-logs" before running this command!')
    exit(1)

os.mkdir(LOGDIR)

cluster_state = subprocess.check_output(['kubectl', 'get', 'pods', '-n', 'takserver']).decode()
with open(os.path.join(LOGDIR, 'cluster-status.txt'), 'w') as fp:
    fp.write(cluster_state)
    fp.close()

pod_names = subprocess.check_output(['kubectl', 'get', 'pods', '--no-headers', '-o', 'custom-columns=:metadata.name', '-n', 'takserver']).decode().strip().split('\n')

for pod_name in pod_names:
    print('Collecting log for {pn}'.format(pn=pod_name))
    log_contents = subprocess.check_output(['kubectl', 'logs', '-n', 'takserver', pod_name]).decode()
    with open(os.path.join(LOGDIR, pod_name + '-log.txt'), 'w') as fp:
        fp.write(log_contents)
        fp.close()
