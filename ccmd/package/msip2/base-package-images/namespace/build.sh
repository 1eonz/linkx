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
  mkdir -p "${target_dir}/init"
  helm template "$dir_name" "${script_path}/../../helm/platform" \
    --debug \
    --set "version=${pkg_ver},arch=${arch}.${LOCAL_TIME},name=${dir_name},register_host=${register_host},namespace=${namespace}" \
    > "${target_dir}/init/${dir_name}.yaml"
  if [ $? -ne 0 ]; then
    cat "${target_dir}/init/${dir_name}.yaml"
    exit 1
  fi
  sed -i "s@${register_host}/${dir_name}:${pkg_ver}.${arch}.${LOCAL_TIME}@${register_host}/${dir_name}:${mirror_ver}.${arch}.${LOCAL_TIME}@g" ${target_dir}/init/${dir_name}.yaml
  mkdir -p "${target_dir}/script/install/shell/log/"
  cp "${script_path}/before.sh" "${target_dir}/script/install/shell/log/"
  cp "${script_path}"/*.xml "${target_dir}/script/install/shell/log/"
  mkdir -p "${target_dir}/script/upgrade/shell/log/"
  cp "${script_path}/before.sh" "${target_dir}/script/upgrade/shell/log"
  cp "${script_path}"/*.xml "${target_dir}/script/upgrade/shell/log/"
}

if [ $# -lt 2 ]; then
  usage
  exit 1
fi

main $@