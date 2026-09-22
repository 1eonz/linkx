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
  mkdir -p "${target_dir}/conf/midware"
  helm template mapbox "${script_path}/../../helm/mapbox" \
    --debug \
    --set "namespace=${namespace},image.registry=${register_host},image.tag=20260723.${arch}.${LOCAL_TIME}" \
    > "$target_dir/conf/midware/mapbox.yaml"
  if [ $? -ne 0 ]; then
    cat "$target_dir/conf/midware/mapbox.yaml"
    exit 1
  fi
  docker pull repo.rd.td-tech.com/docker-virtual/linkx/mapbox-service:20260723.${arch}
  docker tag repo.rd.td-tech.com/docker-virtual/linkx/mapbox-service:20260723.${arch} ${register_host}/mapbox:20260723.${arch}.${LOCAL_TIME}
  echo "${register_host}/mapbox:20260723.${arch}.${LOCAL_TIME}" >> "${target_dir}/images.txt"
}

if [ $# -lt 2 ]; then
  usage
  exit 1
fi

main $@