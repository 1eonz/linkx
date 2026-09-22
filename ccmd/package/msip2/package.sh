#!/bin/bash
set -o errexit
set -x
conf_path=$1
namespace=linkx
export LOCAL_TIME=${LOCAL_TIME:-$(date +%Y%m%d%H%M%S)}
# shellcheck source=../config.conf.example
source "$1"
cd "$(dirname "$0")" || exit 1
current_path=$(pwd)
packages_path="${current_path}/target"
mkdir -p "${packages_path}/yaml"
function usage(){
  echo "
    sh package.sh /opt/config.conf
  "
}

function logger(){
	echo "$(date +'%Y-%m-%d %H:%M:%S').package.sh $1"
}

function pre_package(){
  # shellcheck disable=SC2086
  cp -r ${current_path}/template/* ${packages_path}/
}

function copy_backup_config(){
  logger "start copy backup_config files"
  local src_dir="${current_path}/base-package-images/backup_config"
  if [[ ! -d "$src_dir" ]]; then
    logger "warning: backup_config source directory not found: ${src_dir}"
    return 0
  fi
  mkdir -p "${packages_path}/backup_config"
  cp -f "${src_dir}"/* "${packages_path}/backup_config/"

  local job_db_dir="${current_path}/base-package-images/job-db"
  if [[ -f "${job_db_dir}/backupdb_config.yml" ]]; then
    cp -f "${job_db_dir}/backupdb_config.yml" "${packages_path}/backup_config/"
  fi
  if [[ -f "${job_db_dir}/database.txt" ]]; then
    cp -f "${job_db_dir}/database.txt" "${packages_path}/backup_config/"
  fi
  logger "backup_config files copied (full database config, may be overridden by gen_patch_database_config in patch mode)"
}

function helm_dependency(){
  cd "${current_path}/helm" || exit 1
  for d in *; do
    if [[ -d $d ]]; then
      cd "$d"
      helm dependency update
      cd ..
    fi
  done
}

function create_sha512() {
  signconf_cms_shell="${path_workspace}/create_signconf_cms.sh"
  ${signconf_cms_shell} $packages_path || {
    echo "create_signconf_cms is failed"
    exit 1
  }
}

function gen_patch_database_config() {
  logger "start generate patch database config (before build_images)"

  if [[ "$action" == "package" ]]; then
    return 0
  fi

  if [[ -z "$patch_target" ]]; then
    logger "warning: patch_target is empty, skip patch database config generation"
    return 0
  fi

  local mapping_file="${current_path}/service_auxiliary_mapping.conf"
  if [[ ! -f "$mapping_file" ]]; then
    logger "warning: mapping file not found: ${mapping_file}, skip"
    return 0
  fi

  local required_databases=""
  local has_flyway_job=0
  local matched_count=0
  local unmatched_services=""

  local -A svc_map
  while IFS= read -r line; do
    line="${line%$'\r'}"
    [[ "$line" =~ ^[[:space:]]*# ]] && continue
    [[ -z "$line" ]] && continue
    local key="${line%%:*}"
    key="$(echo "$key" | xargs)"
    [[ -n "$key" ]] && svc_map["$key"]="$line"
  done < "$mapping_file"
  logger "loaded ${#svc_map[@]} entries from mapping file: ${!svc_map[*]}"

  IFS=',' read -ra PATCH_LIST <<< "$patch_target"

  for svc in "${PATCH_LIST[@]}"; do
    local svc_trimmed=$(echo "$svc" | xargs)
    if [[ -z "$svc_trimmed" ]]; then
      continue
    fi
    if [[ -n "${svc_map[$svc_trimmed]+x}" ]]; then
      local aux_line="${svc_map[$svc_trimmed]}"
      matched_count=$((matched_count + 1))
      local aux_part=$(echo "$aux_line" | cut -d'|' -f1 | cut -d':' -f2)
      if echo "$aux_part" | grep -qw "flyway_job"; then
        has_flyway_job=1
      fi
      local db_part=""
      if [[ "$aux_line" == *"|"* ]]; then
        db_part="${aux_line##*|}"
      fi
      if [[ -n "$db_part" ]]; then
        required_databases="${required_databases},${db_part}"
      fi
      logger "matched service [${svc_trimmed}]: aux_part=[${aux_part}], db_part=[${db_part}]"
    else
      unmatched_services="${unmatched_services},${svc_trimmed}"
      logger "warning: service [${svc_trimmed}] not found in mapping file"
    fi
  done

  if [[ -n "$unmatched_services" ]]; then
    unmatched_services=$(echo "$unmatched_services" | sed 's/^,//')
    logger "unmatched services: ${unmatched_services}"
  fi

  if [[ $matched_count -eq 0 ]]; then
    logger "warning: no services in patch_target [${patch_target}] matched mapping file, keeping full backup config"
    return 0
  fi

  if [[ -n "$required_databases" ]]; then
    required_databases=$(echo "$required_databases" | sed 's/^,//' | tr ',' '\n' | sort -u | tr '\n' ',' | sed 's/,$//')
    logger "required databases: $required_databases"
  fi

  mkdir -p "${packages_path}/backup_config"

  if [[ -n "$required_databases" ]]; then
    logger "generating database.txt for flyway: $required_databases"
    echo "$required_databases" | tr ',' '\n' > "${packages_path}/backup_config/database.txt"

    logger "generating backupdb_config.yml for databases: $required_databases"
    cat > "${packages_path}/backup_config/backupdb_config.yml" <<EOF
db_backup:
  databases:
EOF
    IFS=',' read -ra DB_LIST <<< "$required_databases"
    for db in "${DB_LIST[@]}"; do
      cat >> "${packages_path}/backup_config/backupdb_config.yml" <<EOF
    - name: "${db}"
      blacklist_tables: []
      whitelist_tables: []
EOF
    done
  elif [[ "$has_flyway_job" -eq 0 ]]; then
    logger "no databases for this patch, generating empty backupdb_config.yml and database.txt"
    cat > "${packages_path}/backup_config/backupdb_config.yml" <<EOF
db_backup:
  enabled: false
  databases: []
  mongodb: []
EOF
    : > "${packages_path}/backup_config/database.txt"
  fi

  if [[ "$has_flyway_job" -eq 1 ]]; then
    local services_path="${current_path}/base-package-images"
    if [[ ! -d "${services_path}/job-db" ]]; then
      logger "ERROR: job-db service directory not found"
      exit 1
    fi
    local already_in_target=0
    IFS=',' read -ra CHECK_LIST <<< "$patch_target"
    for svc in "${CHECK_LIST[@]}"; do
      if [[ "$svc" == "job-db" ]]; then
        already_in_target=1
        break
      fi
    done
    if [[ "$already_in_target" -eq 0 ]]; then
      logger "auto-appending job-db to patch_target (flyway_job required)"
      patch_target="${patch_target},job-db"
    fi
  fi

  logger "patch database config generation complete"
}

function gen_auxiliary_content() {
  logger "start generate auxiliary content"

  if [[ "$action" == "package" ]]; then
    logger "full package mode, auxiliary content already generated by build.sh"
    return 0
  fi

  local mapping_file="${current_path}/service_auxiliary_mapping.conf"
  if [[ ! -f "$mapping_file" ]]; then
    logger "warning: mapping file not found: ${mapping_file}, skip auxiliary content generation"
    return 0
  fi

  local services_path="${current_path}/base-package-images"
  local required_auxiliaries=""

  local -A aux_svc_map
  while IFS= read -r line; do
    line="${line%$'\r'}"
    [[ "$line" =~ ^[[:space:]]*# ]] && continue
    [[ -z "$line" ]] && continue
    local key="${line%%:*}"
    key="$(echo "$key" | xargs)"
    [[ -n "$key" ]] && aux_svc_map["$key"]="$line"
  done < "$mapping_file"

  IFS=',' read -ra PATCH_LIST <<< "$patch_target"

  for svc in "${PATCH_LIST[@]}"; do
    local svc_trimmed=$(echo "$svc" | xargs)
    if [[ -z "$svc_trimmed" ]]; then
      continue
    fi
    if [[ -n "${aux_svc_map[$svc_trimmed]+x}" ]]; then
      local aux_line="${aux_svc_map[$svc_trimmed]}"
      local aux_part=$(echo "$aux_line" | cut -d'|' -f1 | cut -d':' -f2)
      required_auxiliaries="${required_auxiliaries},${aux_part}"
    fi
  done

  if [[ -z "$required_auxiliaries" ]]; then
    logger "no auxiliary content required for patch_target: $patch_target"
    return 0
  fi

  required_auxiliaries=$(echo "$required_auxiliaries" | sed 's/^,//' | tr ',' '\n' | sort -u | tr '\n' ',' | sed 's/,$//')
  logger "required auxiliary types: $required_auxiliaries"

  local mirror_ver=$(echo ${pkg_ver} | egrep -o "SPC[0-9]{3}B[0-9]*")

  IFS=',' read -ra AUX_LIST <<< "$required_auxiliaries"
  for aux_type in "${AUX_LIST[@]}"; do
    case "$aux_type" in
      file_backup_config|upgrade_scripts)
        ;;
      flyway_job)
        if [[ ! -f "${packages_path}/conf/job/job-db.yaml" ]]; then
          logger "generating flyway_job yaml"
          helm template "job-db" "${current_path}/helm/job" \
            --debug \
            -f "${services_path}/job-db/values.yaml" \
            --set "version=${pkg_ver},arch=${arch}.${LOCAL_TIME},register_host=${register_host}" \
            > "${packages_path}/conf/job/job-db.yaml"
          sed -i "s@${register_host}/job-db:${pkg_ver}.${arch}.${LOCAL_TIME}@${register_host}/job-db:${mirror_ver}.${arch}.${LOCAL_TIME}@g" "${packages_path}/conf/job/job-db.yaml"
        fi
        ;;
      file_init_job)
        if [[ ! -f "${packages_path}/conf/job/job-files.yaml" ]]; then
          logger "generating file_init_job yaml"
          helm template "job-files" "${current_path}/helm/job" \
            --debug \
            -f "${services_path}/job-files/values.yaml" \
            --set "version=${pkg_ver},arch=${arch}.${LOCAL_TIME},register_host=${register_host}" \
            > "${packages_path}/conf/job/job-files.yaml"
          sed -i "s@${register_host}/job-files:${pkg_ver}.${arch}.${LOCAL_TIME}@${register_host}/job-files:${mirror_ver}.${arch}.${LOCAL_TIME}@g" "${packages_path}/conf/job/job-files.yaml"
        fi
        ;;
      egress_ai_scripts)
        logger "generating egress_ai_scripts"
        mkdir -p "${packages_path}/script/upgrade/shell/egress-ai"
        mkdir -p "${packages_path}/script/rollback/shell/egress-ai"
        cp "${services_path}/egress-ai/before-patch.sh" "${packages_path}/script/upgrade/shell/egress-ai/before.sh"
        cp "${services_path}/egress-ai/after-patch.sh" "${packages_path}/script/upgrade/shell/egress-ai/after.sh"
        cp "${services_path}/egress-ai/before-patch.sh" "${packages_path}/script/rollback/shell/egress-ai/before.sh"
        cp "${services_path}/egress-ai/after-patch.sh" "${packages_path}/script/rollback/shell/egress-ai/after.sh"
        ;;
      egress_pim_scripts)
        logger "generating egress_pim_scripts"
        mkdir -p "${packages_path}/script/upgrade/shell/egress-pim"
        mkdir -p "${packages_path}/script/rollback/shell/egress-pim"
        cp "${services_path}/egress-pim/before-patch.sh" "${packages_path}/script/upgrade/shell/egress-pim/before.sh"
        cp "${services_path}/egress-pim/after-patch.sh" "${packages_path}/script/upgrade/shell/egress-pim/after.sh"
        cp "${services_path}/egress-pim/before-patch.sh" "${packages_path}/script/rollback/shell/egress-pim/before.sh"
        cp "${services_path}/egress-pim/after-patch.sh" "${packages_path}/script/rollback/shell/egress-pim/after.sh"
        ;;
      appinfo_job)
        if [[ ! -f "${packages_path}/conf/job/job-appinfo.yaml" ]]; then
          logger "generating appinfo_job yaml"
          helm template "job-appinfo" "${current_path}/helm/job" \
            --debug \
            -f "${services_path}/job-appinfo/values.yaml" \
            --set "arch=${arch}.${LOCAL_TIME},register_host=${register_host}" \
            > "${packages_path}/conf/job/job-appinfo.yaml"
        fi
        ;;
      *)
        logger "warning: unknown auxiliary type: $aux_type"
        ;;
    esac
  done

  # appinfo_job 是版本信息刷新 + 菜单 reload, 每次补丁升级都该跑 (跟 agent 实现对齐).
  # mapping 里 job-appinfo 是独立条目, 业务服务补丁不会触发它, 这里无条件兜底生成,
  # 避免补丁安装后 /opt/version/linkx/app-info.yaml 不刷新、菜单不 reload.
  if [[ ! -f "${packages_path}/conf/job/job-appinfo.yaml" ]]; then
    logger "force generating appinfo_job yaml (always required for patch)"
    helm template "job-appinfo" "${current_path}/helm/job" \
      --debug \
      -f "${services_path}/job-appinfo/values.yaml" \
      --set "arch=${arch}.${LOCAL_TIME},register_host=${register_host}" \
      > "${packages_path}/conf/job/job-appinfo.yaml"
  fi

  logger "auxiliary content generation complete"
}

function build_images() {
  logger "start build service docker image"
  services_path="${current_path}/base-package-images"
  cd "${services_path}" || exit 1
  mkdir -p "${packages_path}/conf/manifests"
  mkdir -p "${packages_path}/conf/job"
  mkdir -p "${packages_path}/conf/vip"
  if [[ "$action" == "package" ]]; then
    for service in *; do
      serviceBase="${services_path}/${service}"
      if [[ -d "${serviceBase}" && -f "${serviceBase}/build.sh" ]]; then
        cd "${serviceBase}" || exit 1
        logger "start build ${service} docker image"
        sh build.sh "${conf_path}" "${service}" "${packages_path}" "${packages_path}/conf/manifests" ${namespace} || exit 1
        logger "build ${service} docker image success"
        cd ..
      fi
    done
  else
    IFS=',' read -ra VERSIONS <<< "$patch_target"
    for service in "${VERSIONS[@]}"; do
      serviceBase="${services_path}/${service}"
      if [[ -d "${serviceBase}" && -f "${serviceBase}/build.sh" ]]; then
        cd "${serviceBase}" || exit 1
        logger "start build ${service} docker image"
        sh build.sh "${conf_path}" "${service}" "${packages_path}" "${packages_path}/conf/manifests" ${namespace} || exit 1
        logger "build ${service} docker image success"
        cd ..
      else
        echo "$service not exists"
        exit 1
      fi
    done
  fi
  cp -f ${current_path}/vip/* ${packages_path}/conf/vip/
  logger "build service docker image success"
}

function export_images() {
  cd "${packages_path}" || exit 1
  mkdir -p "${packages_path}/images"
  all_images=""
  while IFS= read -r image; do
    all_images="${all_images} ${image}"
  done < "${packages_path}/images.txt"
  # 判空处理
  if [[ -n "${all_images}" ]]; then
    docker save -o "${packages_path}/images/${namespace}.tar" ${all_images} || exit 1
    echo "save images done"
  else
    echo "no images to save"
  fi
}

function gen_app_info() {
  cd "$packages_path" || exit 1
  rm -rf app-info.yaml
  cp -f $path_workspace/meta/app-info-linkx.yaml app-info.yaml
cat <<EOF >> app-info.yaml
  build_time: "$(date +'%Y-%m-%d %H:%M:%S').$(printf "%03d\n" $((10#$(date +'%N')/1000000)))"
EOF
}

function read_patch_id() {
  local app_info_file="${path_workspace}/meta/app-info-linkx.yaml"
  if [[ ! -f "$app_info_file" ]]; then
    echo "ERROR: app-info file not found: ${app_info_file}" >&2
    return 1
  fi
  local patch_id
  patch_id=$(grep 'version_patch_id' "$app_info_file" | head -1 | sed 's/.*version_patch_id:[[:space:]]*//' | tr -d '"' | tr -d "'" | tr -d '[:space:]')
  if [[ -z "$patch_id" ]]; then
    echo "ERROR: version_patch_id not found in ${app_info_file}" >&2
    return 1
  fi
  echo "$patch_id"
}

function deploy_version() {
  cd ${packages_path} || exit 1
  commitID=$(git rev-parse HEAD)
  codeUrl=$(git remote -v | grep fetch | awk '{print $2}')
  codeUrl=$(echo ${codeUrl} | sed 's%git@\([^:]*\):\(.*\)%http://\1/\2%')  # 处理SSH格式: git@host:path -> https://host/path
  codeUrl=$(echo ${codeUrl} | sed 's%http.*@%http://%g')    # 处理HTTP认证信息
  codeUrl="http://****/$(echo ${codeUrl} | sed 's%.*/%%')"
  codeBranch=$(git branch -a | grep -E '\*|/m/' | grep -v 'no' | sed -E 's#^\* ##;s#^origin##;s#.* ->##')
  rm -rf version.ini
  cat > version.ini <<EOF
[version]
Product=$prd_name
Version=${pkg_ver}
Arch=${Arch}
GitRepo='${codeUrl}'
CommitID=${commitID}
Branch=${codeBranch}
BuildTime=$(date +'%Y-%m-%d %H:%M:%S').$(printf "%03d\n" $((10#$(date +'%N')/1000000)))
EOF

  cp -prf version.ini ${current_path}/base-package-images/cloudcmd-base-service/
  cp -prf version.ini ${current_path}/base-package-images/linkx-node-service/
}

function create_snapshot() {
  pushd $path_workspace/ccmd/CI/
    wget http://safeconf.rd.td-tech.com/CI-manifest/snapshots/snapshots.zip
    unzip snapshots.zip
  popd
  pushd $path_workspace/ccmd/CI/snapshots/linux
    unset password
    unset gitee_repo
    unset gitee_api_token
    unset gitee_backup
    ant -f build.xml
    cp -rf ./result/snapshot.htm ${packages_path}/snapshot.htm
  popd
}

function pkg_tar_gz() {
  cd "${packages_path}" || exit 1
  if [[ "$action" == "package" ]]; then
    zip -5 -r "${prd_name}_${pkg_ver}_${Arch}.zip" .
  else
    zip -5 -r "${prd_name}_patch_${PATCH_ID}_${Arch}.zip" .
  fi
}

function main() {
  if [[ "$action" != "package" ]]; then
    PATCH_ID=$(read_patch_id)
    if [[ $? -ne 0 || -z "$PATCH_ID" ]]; then
      echo "ERROR: cannot build patch package without version_patch_id, abort" >&2
      exit 1
    fi
    export PATCH_ID
    logger "patch_id: ${PATCH_ID}"
  fi
  # shellcheck disable=SC2068
  pre_package $@
  # shellcheck disable=SC2068
  gen_app_info $@
  # shellcheck disable=SC2068
  deploy_version $@
  # shellcheck disable=SC2068
  helm_dependency $@
  copy_backup_config
  gen_patch_database_config
  build_images $@
  gen_auxiliary_content
  # shellcheck disable=SC2068
  export_images $@
  # shellcheck disable=SC2068
  create_snapshot
  create_sha512
  # shellcheck disable=SC2068
  pkg_tar_gz $@
}

if [ $# -lt 1 ]; then
  usage
  exit 1
fi

# shellcheck disable=SC2068
main $@