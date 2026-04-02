#!/bin/sh

tc qdisc del dev enp0s3 root
tc qdisc add dev enp0s3 root handle 1: htb default 1
tc class add dev enp0s3 parent 1: classid 1:1 htb rate 10mbit
tc class add dev enp0s3 parent 1: classid 1:2 htb rate 2mbit
tc class add dev enp0s3 parent 1: classid 1:3 htb rate 5mbit
tc filter add dev enp0s3 protocol ip parent 1: prio 1 u32 match ip sport 8443 0xffff flowid 1:2
tc filter add dev enp0s3 protocol ip parent 1: prio 1 u32 match ip sport 8090 0xffff flowid 1:3

# show tc stats
#tc -s -d qdisc show dev eth1

#tc qdisc add dev enp0s3 root tbf rate 12kbit burst 12kbit latency 5000ms