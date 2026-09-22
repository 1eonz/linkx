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
source "${config_path}"
script_path=$(cd "$(dirname $0)" || exit 1; pwd)
mirror_ver=$(echo  ${pkg_ver} | egrep -o "SPC[0-9]{3}B[0-9]*")

function build_script(){
  target_path=$1
  template_file=$2
  echo '#!/bin/bash' > "${target_path}"
  echo "version=${mirror_ver}" >> "${target_path}"
  echo "registry_host=${register_host}" >> "${target_path}"
  echo "name=${dir_name}" >> "${target_path}"
  echo "arch=${arch}.${LOCAL_TIME}" >> "${target_path}"
  echo "template_file=${template_file}" >> "${target_path}"
  cat "${script_path}/fragment.sh" >> "${target_path}"
}

function main(){
  cd "${path_workspace}/${dir_ccmd}/cloudcmd-heterogeneity/cloudcmd-sql-runner" || exit 1
  docker images | grep ${dir_name} | awk '{print $3}' | xargs docker rmi > /dev/null 2>&1
  docker build -t "${register_host}/${dir_name}:${mirror_ver}.${arch}.${LOCAL_TIME}" -f "Dockerfile-${arch}" . || exit 1
  echo "${register_host}/${dir_name}:${mirror_ver}.${arch}.${LOCAL_TIME}" >> "${target_dir}/images.txt"

  cd "$script_path" || exit 1
  mkdir -p "${target_dir}/script/upgrade/shell/job-sim" && \
    docker save -o "${target_dir}/script/upgrade/shell/job-sim/job-sim.tar" "${register_host}/${dir_name}:${mirror_ver}.${arch}.${LOCAL_TIME}" && \
    cp drop-job.yaml.tpl "${target_dir}/script/upgrade/shell/job-sim/" && \
    build_script "${target_dir}/script/upgrade/shell/job-sim/before.sh" "drop-job.yaml.tpl"
}

if [ $# -lt 2 ]; then
  usage
  exit 1
fi

main $@