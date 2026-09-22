#!/bin/bash
function logger() {
  echo "$(date +'%Y-%m-%d %H:%M:%S').$2.build.sh [Build] $1"
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
  docker images | grep ${dir_name} | awk '{print $3}' | xargs docker rmi > /dev/null 2>&1
  mirror_ver=$(echo  ${pkg_ver} | egrep -o "SPC[0-9]{3}B[0-9]*")
  docker build -t "${register_host}/${dir_name}:${mirror_ver}.${arch}.${LOCAL_TIME}" -f "Dockerfile-${arch}" . || exit 1
  helm template "${dir_name}" "${script_path}/../../helm/job" \
    --debug \
    -f "${script_path}/values.yaml" \
    --set "version=${pkg_ver},arch=${arch}.${LOCAL_TIME},register_host=${register_host}" \
    > "${target_dir}/conf/job/${dir_name}.yaml"
     if [ $? -ne 0 ]; then
      cat "${target_dir}/conf/job/${dir_name}.yaml"
      exit 1
    fi
  sed -i "s@${register_host}/${dir_name}:${pkg_ver}.${arch}.${LOCAL_TIME}@${register_host}/${dir_name}:${mirror_ver}.${arch}.${LOCAL_TIME}@g" ${target_dir}/conf/job/${dir_name}.yaml
  echo "${register_host}/${dir_name}:${mirror_ver}.${arch}.${LOCAL_TIME}" >> "${target_dir}/images.txt"
}

if [ $# -lt 2 ]; then
  usage
  exit 1
fi

main $@