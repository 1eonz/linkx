#!/bin/bash
function logger() {
  echo "$(date +'%Y-%m-%d %H:%M:%S').${dir_name}.$0 [Build] "
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

function main() {
  source ${config_path}
  #cleanup
  docker images | grep ${dir_name} | awk '{print $3}' | xargs docker rmi > /dev/null 2>&1
  #buildimage
  script_path=$(cd "$(dirname "$0")" || exit 1;pwd)
  mirror_ver=$(echo  ${pkg_ver} | egrep -o "SPC[0-9]{3}B[0-9]*")
  docker build -t "${register_host}/${dir_name}:${mirror_ver}.${arch}.${LOCAL_TIME}" -f "Dockerfile-${arch}" . || exit 1
  #helm template "${dir_name}" "${script_path}/../../helm/web" \
  #  --debug \
  #  -f "${script_path}/values.yaml" \
  #  --set "version=${pkg_ver},arch=${arch}.${LOCAL_TIME},name=${dir_name},register_host=${register_host},namespace=${namespace}" \
  #  > "${manifests_dir}/${dir_name}.yaml"
  #   if [ $? -ne 0 ]; then
  #    cat "${manifests_dir}/${dir_name}.yaml"
  #    exit 1
  #  fi
  #sed -i "s@${register_host}/${dir_name}:${pkg_ver}.${arch}.${LOCAL_TIME}@${register_host}/${dir_name}:${mirror_ver}.${arch}.${LOCAL_TIME}@g" ${manifests_dir}/${dir_name}.yaml
  sed -i "s@{Arch}@${arch}@g" ${script_path}/${dir_name}.yaml
  sed -i "s@{Version}@${mirror_ver}@g" ${script_path}/${dir_name}.yaml
  sed -i "s@{BuildTime}@${LOCAL_TIME}@g" ${script_path}/${dir_name}.yaml
  cp -f ${script_path}/${dir_name}.yaml ${manifests_dir}/${dir_name}.yaml
  echo "${register_host}/${dir_name}:${mirror_ver}.${arch}.${LOCAL_TIME}" >> "${target_dir}/images.txt"

  mkdir -p "${target_dir}/script/upgrade/shell/egress-pim" && \
    cp "${script_path}/before-patch.sh" "${target_dir}/script/upgrade/shell/egress-pim/before.sh" && \
    cp "${script_path}/after-patch.sh" "${target_dir}/script/upgrade/shell/egress-pim/after.sh"
  mkdir -p "${target_dir}/script/rollback/shell/egress-pim" && \
    cp "${script_path}/before-patch.sh" "${target_dir}/script/rollback/shell/egress-pim/before.sh" && \
    cp "${script_path}/after-patch.sh" "${target_dir}/script/rollback/shell/egress-pim/after.sh"

}

if [ $# -lt 2 ]; then
  usage
  exit 1
fi

main $@
