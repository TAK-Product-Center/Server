#!/bin/bash
# Script to apply SELinux policy if SELinux is enforcing

is_enforcing=`getenforce`

if [ "$is_enforcing" == "Enforcing" ]
then
    echo "SELinux Enabled. Applying policy."
    checkmodule -M -m -o takserver-policy.mod takserver-policy.te
    semodule_package -o takserver-policy.pp -m takserver-policy.mod
    echo "Enforcing policy"
    sudo semodule -i takserver-policy.pp
    rm -f takserver-policy.pp  takserver-policy.mod
else
    echo "SELinux is not enforced. Ignoring policy."
fi

exit

