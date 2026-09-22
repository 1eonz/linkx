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

function main(){
  source ${config_path}
  script_path=$(cd "$(dirname $0)" || exit 1; pwd)
  cd "$script_path" || exit 1
  docker images | grep ${dir_name} | awk '{print $3}' | xargs docker rmi > /dev/null 2>&1
  mirror_ver=$(echo  ${pkg_ver} | egrep -o "SPC[0-9]{3}B[0-9]*")

  local db_list_file="${target_dir}/backup_config/database.txt"
  if [[ -f "$db_list_file" ]]; then
    logger "patch mode: generating dynamic Dockerfile from database.txt"
    local dynamic_dockerfile="Dockerfile-${arch}.dynamic"
    cp "Dockerfile-${arch}" "$dynamic_dockerfile"
    sed -i '/^COPY \.\//d' "$dynamic_dockerfile"
    while IFS= read -r db_name || [[ -n "$db_name" ]]; do
      db_name=$(echo "$db_name" | xargs)
      if [[ -n "$db_name" && -d "${script_path}/${db_name}" ]]; then
        echo "COPY ./${db_name}/ /flyway/db/migration/${db_name}/" >> "$dynamic_dockerfile"
      fi
    done < "$db_list_file"
    echo "COPY ./database.txt /flyway/db/migration/database.txt" >> "$dynamic_dockerfile"
    echo "COPY ./flyway.conf /flyway/conf/flyway.conf" >> "$dynamic_dockerfile"
    echo "COPY ./docker_run.sh /usr/bin/docker_run.sh" >> "$dynamic_dockerfile"
    cp -f "$db_list_file" "${script_path}/database.txt"
    local src_db_file_bak="${script_path}/database.txt.bak"
    if [[ -f "${script_path}/database.txt" ]]; then
      cp -f "${script_path}/database.txt" "$src_db_file_bak"
    fi
    docker build -t "${register_host}/${dir_name}:${mirror_ver}.${arch}.${LOCAL_TIME}" -f "$dynamic_dockerfile" . || { restore_rc=$?; rm -f "$dynamic_dockerfile"; if [[ -f "$src_db_file_bak" ]]; then cp -f "$src_db_file_bak" "${script_path}/database.txt"; rm -f "$src_db_file_bak"; fi; exit $restore_rc; }
    rm -f "$dynamic_dockerfile"
    if [[ -f "$src_db_file_bak" ]]; then
      cp -f "$src_db_file_bak" "${script_path}/database.txt"
      rm -f "$src_db_file_bak"
    fi
  else
    docker build -t "${register_host}/${dir_name}:${mirror_ver}.${arch}.${LOCAL_TIME}" -f "Dockerfile-${arch}" . || exit 1
  fi

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