#!/usr/bin/env bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )/"
RESULTS_DIR="integration_test_results"

mkdir "${RESULTS_DIR}"
TAKSERVER_ROOT="${SCRIPT_DIR}../../../"

TGT_CORE_RESULTS_DIR="${RESULTS_DIR}/takserver-core/build"
mkdir -p "${TGT_CORE_RESULTS_DIR}"

TGT_CORE_ARTIFACTS_DIR="${TGT_CORE_RESULTS_DIR}/tmp/integrationTest"
mkdir -p "${TGT_CORE_ARTIFACTS_DIR}"

SRC="${TAKSERVER_ROOT}src/takserver-core/build/reports"
if [[ -d "${SRC}" ]];then
  cp -R "${SRC}" "${TGT_CORE_RESULTS_DIR}/"
fi

SRC="${TAKSERVER_ROOT}src/takserver-core/build/test-results"
if [[ -d "${SRC}" ]];then
  cp -R "${SRC}" "${TGT_CORE_RESULTS_DIR}/"
fi

SRC="${TAKSERVER_ROOT}/src/takserver-core/build/tmp/integrationTest/TEST_ARTIFACTS"
if [[ -d "${SRC}" ]];then
  cp -R "${SRC}" "${TGT_CORE_ARTIFACTS_DIR}/"
fi

TGT_USERMANAGER_RESULTS_DIR="${RESULTS_DIR}/takserver-usermanager/build"
mkdir -p "${TGT_USERMANAGER_RESULTS_DIR}"

TGT_USERMANAGER_ARTIFACTS_DIR="${TGT_USERMANAGER_RESULTS_DIR}/tmp/integrationTest/TEST_ARTIFACTS"
mkdir -p "${TGT_USERMANAGER_ARTIFACTS_DIR}"

SRC="${TAKSERVER_ROOT}src/takserver-usermanager/build/reports"
if [[ -d "${SRC}" ]];then
  cp -R "${SRC}" "${TGT_USERMANAGER_RESULTS_DIR}/"
fi

SRC="${TAKSERVER_ROOT}src/takserver-usermanager/build/test-results"
if [[ -d "${SRC}" ]];then
  cp -R "${SRC}" "${TGT_USERMANAGER_RESULTS_DIR}/"
fi

SRC="${TAKSERVER_ROOT}src/takserver-usermanager/build/tmp/integrationTest/TEST_ARTIFACTS"
if [[ -d "${SRC}" ]];then
  cp -R "${SRC}" "${TGT_USERMANAGER_ARTIFACTS_DIR}/"
fi

tar cvzf "${RESULTS_DIR}.tar.gz" "${RESULTS_DIR}"
