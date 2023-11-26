#!/usr/bin/env bash

CHECKSUM_CMD=sha256sum
# A list of files which should be checked.
LIST_FILE=/opt/tak/health/takserver/critical_file_list.txt
# The file with the checksum outputs from sha256sum.
CHECKSUM_FILE=/opt/tak/health/takserver/critical_file_checksums.txt
EMPTY_FILE=/opt/tak/health/takserver/empty.txt

# If the checksum file already exist, then just verify the checkssums.
if [ -r $CHECKSUM_FILE ]; then
    $CHECKSUM_CMD -c --quiet $CHECKSUM_FILE
    if [ "$?" != "0" ]; then
        echo ERROR: Files integrity check failed.
        exit 1
    fi
    exit 0
# Make sure we have the list file, then create the checksum file.
elif [ -r $LIST_FILE ]; then
    count=0
    touch $CHECKSUM_FILE
    while read -r file;
    do
        # Trim all the spaces
        first=`echo $file | tr -d ' '`
        # If the line is for a comment which is leading with a #, then skip it.
        if [ ${first:0:1} != "#" ]; then
            if [ -r $file ]; then
                $CHECKSUM_CMD -b $file >> $CHECKSUM_FILE
                if [ "$?" != "0" ]; then
                    echo ERROR: command \"$CHECKSUM_CMD $file\" failed.
                    rm -f $CHECKSUM_FILE
                    exit 1
                else
                    count=$((count+1))
                fi
            else
                echo ERROR: $file is not readable.
                rm -f $CHECKSUM_FILE
                exit 1
            fi
        fi
    done < $LIST_FILE
    if [ $count -eq 0 ]; then
        if [ ! -f $EMPTY_FILE ]; then
            touch $EMPTY_FILE
            echo WARNING: No file checked for integrity. Please verify the list of files in /opt/tak/health/takserver/critical_file_list.txt.
        fi
        rm -f $CHECKSUM_FILE
    else
        rm -f $EMPTY_FILE
    fi
    exit 0
else
    if [ ! -f $EMPTY_FILE ]; then
        touch $EMPTY_FILE
        echo WARNING: $LIST_FILE does not exist, no files integrity check.
    fi
    exit 0
fi
