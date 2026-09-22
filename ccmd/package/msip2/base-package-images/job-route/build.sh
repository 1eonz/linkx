#!/bin/sh
 logger() {
  echo "$(date +'%Y-%m-%d %H:%M:%S').$2.$0 [Build] $1"
}

 usage() {
  echo "==================usage====================="
  echo "sh build.sh /opt/conf.conf cloudcmd-admin"
  echo "============================================"
}

config_path=$1
dir_name=$2
target_dir=$3
manifests_dir=$4
namespace=$5

main(){
  script_path=$(cd "$(dirname "$0")" || exit 1;pwd)
  source ${config_path}
  mkdir -p "${target_dir}/conf/job"
  helm template "$dir_name" "${script_path}/../../helm/apisix" \
    --debug \
    -f "${script_path}/values.yaml" \
    --set "name=${dir_name}" \
    --namespace "${namespace}" \
    > "$target_dir/conf/job/$dir_name.yaml"
  if [ $? -ne 0 ]; then
    cat "$target_dir/conf/job/$dir_name.yaml"
    exit 1
  fi
}

if [ $# -lt 2 ]; then
  usage
  exit 1
fi

main $@