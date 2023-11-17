#!/usr/bin/env bash
#
# This script is intended to help prepare takserver build artifacts for manual comparison when significant build 
# system changes are performed. Although it's tempting to compare locally built artifacts, I _STRONGLY_ recommend 
# against this since there are differences.
#
# Usage Instructions
# 1.  Obtain the bundle for master and your branch to be tested
#   a.  Navigate to the takserver gitlab
#   b.  Tap the rocket on the left bar and select "Pipelines"
#   c.  Locate the pipeline for the relevant branch
#.  d.  Wait for the first Stage "build" to finish
#   e.  If it is not master, once it is done successfully building, you'll have to locate the "Sign" Stage (The second orb as of writing) and execute the sign task.
#   f.  Tap the orb with the green check for the sign task and click the label to open it
#   g.  To the right, tap "Download" and hame the artifact bundle appropriately.
# 2.  Copy or symlink the reference bundle of signed artifacts from a master branch to 'artifacts-master.zip'
# 3.  Copy or symlink the bundle of signed artifacts to be tested from the modified branch to 'artifacts-modified.zip'
# 4.  Execute this script to deep-extract all the artifacts for comparison.
# 5.  Use your favorite directory comparison tool (such as Meld for Linux), to manually compare the "artifacts-master" and "artifacts-modified" directories

set -e

if [[ ! -f 'artifacts-master.zip' ]];then
	echo Please copy or symlink the reference artifacts.zip produced from the signing of a recent master CI build to artifacts-master.zip!
	exit 1
fi

if [[ ! -f 'artifacts-modified.zip' ]];then
	echo Please copy or symlink the artifacts.zip produced from the signing of a modified branch freom the CI to artifacts-modified.zip!
	exit 1
fi

if [[ "$(which unzip)" == "" ]];then
	echo Please install unzip!
	exit 1
fi

if [[ "$(which ar)" == "" ]];then
	echo Please install ar! This is in the package binutils in Ubuntu/Debian and apparently installed as part of XCode CLI tools in OS X.
	exit 1
fi

if [[ "$(which rpm2cpio)" == "" ]] || [[ "$(which cpio)" == "" ]];then
	echo Please install rpm2cpio and cpio! They are in the package rpm2cpio in Ubuntu/Debian or the package "rpm" using brew on OS X.
	exit 1
fi


extractArtifacts() {
	identifier=${1}
	targetDir="${PWD}/artifacts-${identifier}"
	mkdir "${targetDir}"

	unzip artifacts-${identifier}.zip -d artifacts-${identifier}-tmp
	pushd "artifacts-${identifier}-tmp/SIGNED/$(ls artifacts-${identifier}-tmp/SIGNED)"

	for filename in ./*;do
		if [[ "${filename}" == *.deb ]];then
			source="${PWD}/${filename}"
			target="${filename//-/}"
			target="${target//_/}"
			target="${target//[0-9]/}"
			target="${target//all.deb/}"
			target="${target//./}"
			target="${target//\//}"
			target="${target//DEV/}"
			target="${targetDir}/deb-${target}"
			mkdir -p "${target}"
			pushd "${target}"
			ar vx "${source}"
			mkdir data;tar xvzf data.tar.gz -C data;rm data.tar.gz
			mkdir control;tar xvzf control.tar.gz -C control;rm control.tar.gz
			popd
		elif [[ "${filename}" == *.rpm ]];then
			rpm2cpio ${filename} | cpio -idmv
			target="${filename//-/}"
			target="${target//[0-9]/}"
			target="${target//noarch.rpm/}"
			target="${target//./}"
			target="${target//\//}"
			target="${target//DEV/}"
			target="${targetDir}/rpm-${target}"
			mv opt ${target}

		elif [[ "${filename}" == *.zip ]];then
			target="${filename//-/}"
			target="${target//[0-9]/}"
			target="${target//.zip/}"
			target="${target//./}"
			target="${target//\//}"
			target="${target//DEV/}"
			if [[ "${filename}" == *docker* ]];then
				target="${targetDir}/zip-${target}"
				unzip $filename -d "${target}-tmp"

	            echo ISDOCKER
	            echo pwd=$PWD
	            dir=$(ls $target-tmp)
	            echo DIR=$dir
	            mv $target-tmp/$dir $target
				rm -r $target-tmp
			else
				target="${targetDir}/${target}"
				unzip $filename -d "${target}"
	        fi
		else
			cp "${filename}" "${targetDir}/${filename}"
		fi
	done
    popd
	rm -r artifacts-${identifier}-tmp
}

extractArtifacts master
extractArtifacts modified
