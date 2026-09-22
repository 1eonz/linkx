#!/bin/bash
function logger() {
  echo "$(date +'%Y-%m-%d %H:%M:%S').${dir_name}.build.sh [Build] ${config_path}"
}

function usage() {
  echo "==================usage====================="
  echo "sh build.sh /opt/conf.conf cloudcmd-admin"
  echo "============================================"
}

config_path=$1
dir_name=$2
target_dir=$3
manifests_dir=$4
namespace=$5

function main(){
  source ${config_path}
  script_path=$(cd "$(dirname $0)" || exit 1; pwd)
  cd "$script_path" || exit 1
  mkdir -p "${target_dir}/conf/job"
  helm template "${dir_name}" "${script_path}/../../helm/intermediary" \
    --debug \
    -f "${script_path}/values.yaml" \
    --namespace linkx \
    > "${target_dir}/conf/job/${dir_name}.yaml"
  if [ $? -ne 0 ]; then
    cat "${target_dir}/conf/job/${dir_name}.yaml"
    exit 1
  fi
}

if [ $# -lt 2 ]; then
  usage
  exit 1
fi

main $@