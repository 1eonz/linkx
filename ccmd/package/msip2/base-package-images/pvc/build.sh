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
  mkdir -p "${target_dir}/conf/pv"
  helm template "$dir_name" "${script_path}/../../helm/pvc" \
    --debug \
    -f "${script_path}/values.yaml" \
    --set "version=${pkg_ver},arch=${arch}.${LOCAL_TIME},name=${dir_name},register_host=${register_host},namespace=${namespace}" \
    > "$target_dir/conf/pv/$dir_name.yaml"
    if [ $? -ne 0 ]; then
      cat "$target_dir/conf/pv/$dir_name.yaml"
      exit 1
    fi
  sed -i "s@${register_host}/${dir_name}:${pkg_ver}.${arch}.${LOCAL_TIME}@${register_host}/${dir_name}:${mirror_ver}.${arch}.${LOCAL_TIME}@g" $target_dir/conf/pv/$dir_name.yaml
  mkdir -p "${target_dir}/script/install/shell/pv" && cp "${script_path}/install-before.sh" "${target_dir}/script/install/shell/pv/before.sh"
  mkdir -p "${target_dir}/script/upgrade/shell/pv" && cp "${script_path}/install-before.sh" "${target_dir}/script/upgrade/shell/pv/before.sh"
  mkdir -p "${target_dir}/script/uninstall/shell/pv" && cp "${script_path}/uninstall-after.sh" "${target_dir}/script/uninstall/shell/pv/after.sh"
}

if [ $# -lt 2 ]; then
  usage
  exit 1
fi

main $@