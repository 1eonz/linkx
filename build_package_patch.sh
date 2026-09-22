#!/bin/bash
############
# USAGE: sh ci.sh /gitlab-ci/code/hebei_multicenter/config.conf
############

source /etc/profile
set -x
#nvm -v


echo "build ${Arch} package"
if [[ ! -e $1 ]]; then
    echo "config file not exists"
fi

common_sh="$(cd "$(dirname $1)" || exit 1;pwd)/$(basename build_package_common.sh)"
# BUILD_DOMAIN 支持环境变量 (CI 注入) 或 $2 参数, 默认 cloudcmd-admin-web
BUILD_DOMAIN=${BUILD_DOMAIN:-${2:-cloudcmd-admin-web}}
source ${common_sh}

modify_hook_py="${path_workspace}/HookModifier.py"

# 补丁模式: 覆盖 config 中的 action 和 patch_target, 使 package.sh 走补丁分支
#python3 "${modify_hook_py}" "$abs_conf" 'action=package' 'action=patch' replace
#python3 "${modify_hook_py}" "$abs_conf" 'patch_target=job-db' "patch_target=${BUILD_DOMAIN}" replace
# 先读取当前值再替换 (兼容多次构建: 上次改过的值不再是初始值)
cur_action=$(grep '^action=' "$abs_conf" | head -1 | sed 's/^action=//')
python3 "${modify_hook_py}" "$abs_conf" "action=${cur_action}" 'action=patch' replace
cur_patch_target=$(grep '^patch_target=' "$abs_conf" | head -1 | sed 's/^patch_target=//')
python3 "${modify_hook_py}" "$abs_conf" "patch_target=${cur_patch_target}" "patch_target=${BUILD_DOMAIN}" replace

# 更新 app-info 中的 path (多值时取首个作为主标识, 避免逗号污染下游解析)
app_info_file="${path_workspace}/meta/app-info-linkx.yaml"
lx_primary="${BUILD_DOMAIN%%,*}"
lx_path=$(grep 'path:' "${app_info_file}" | head -1 | sed 's/.*path:[[:space:]]*//' | tr -d '[:space:]')
python3 "${modify_hook_py}" "${app_info_file}" "path: ${lx_path}" "path: ${lx_primary}" replace

# 执行用户信息 (用于产物上传路径)
export EXECUTOR=${EXECUTOR:-unknown}
export BUILD_USER=${EXECUTOR}
export ENV_PIPELINE_STARTTIME=${ENV_PIPELINE_STARTTIME:-$(date +%Y%m%d%H%M%S)}
export LOCAL_TIME=${ENV_PIPELINE_STARTTIME}


function mv_pkg_patch_path(){
    codeBranch=${codeBranch:-$(git branch -a | grep -E '\*|/m/' | grep -v 'no' | sed -E 's#^\* ##;s#^origin##;s#.* ->##')}
    daily_path="Persion/${BUILD_USER}/Linkx/${codeBranch}/${Arch}/`date +%Y%m%d%H%M%S`"
    #mv ${path_workspace}/ci_package/target/*.zip $cur_target_path/${Arch}/
    logger "package success"
    echo "file path is ${cur_target_path}/${Arch}/"
    #curl --header "Authorization: Bearer ${gitee_api_token}" -T $cur_target_path/${Arch}/gitinfo.txt "${gitee_repo}/${daily_path}/"
    #curl --header "Authorization: Bearer ${gitee_api_token}" -T $cur_target_path/${Arch}/*.htm "${gitee_repo}/${daily_path}/"
    curl --header "Authorization: Bearer ${gitee_api_token}" -T ${path_workspace}/ci_package/target/*.zip "${gitee_repo}/${daily_path}/"
}

function main(){
    if [ $# -lt 1 ]; then
        exit 1
        echo "param error"
    fi
    cur_timer=$(date +'%Y%m%d%H%M%S')
    cur_target_path=${path_pkg}/${cur_timer}


    if [ "${Arch}" == "all" ]; then
      archs=("x86" "arm")
      for el in "${archs[@]}"; do
        Arch=${el}
	      echo "================================= build ${Arch} pkg start  ================================="
        mkdir -p $cur_target_path/${Arch}
        rm -rf "${path_workspace}/ci_package"
        cp -r "${path_workspace}/${dir_ccmd}/package/msip2" "${path_workspace}/ci_package" || exit 1
	      echo "================================= build ${Arch} pkg success ================================="
      done
    else
      mkdir -p $cur_target_path/${Arch}
      rm -rf "${path_workspace}/ci_package"
      cp -r "${path_workspace}/${dir_ccmd}/package/msip2" "${path_workspace}/ci_package" || exit 1
    fi

    # 按 BUILD_DOMAIN 多值聚合编译标志 (同原 hook Check_Linkx_Component 逻辑), 每个编译函数最多执行一次
    local compile_h5portal_flag=0
    local compile_admin_flag=0
    local compile_icc_flag=0
    local compile_ccmd_flag=0
    local service
    for service in ${BUILD_DOMAIN//,/ }; do
        case "${service}" in
            cloudcmd-h5portal)
                compile_h5portal_flag=1
                ;;
            cloudcmd-admin-web)
                compile_admin_flag=1
                ;;
            cloudcmd-web)
                compile_icc_flag=1
                ;;
            *)
                compile_ccmd_flag=1
                ;;
        esac
    done
    logger "compile flags: h5portal=${compile_h5portal_flag} admin=${compile_admin_flag} icc=${compile_icc_flag} ccmd=${compile_ccmd_flag}"

    [ ${compile_h5portal_flag} -eq 1 ] && compile_h5portal
    [ ${compile_admin_flag} -eq 1 ] && compile_admin
    [ ${compile_icc_flag} -eq 1 ] && compile_icc
    [ ${compile_ccmd_flag} -eq 1 ] && compile_ccmd
    # 直接执行 package.sh (补丁模式由 action=patch 和 patch_target 控制, build_images 原生支持逗号分隔多值)
    sh -x "$path_workspace/ci_package/package.sh" $abs_conf || exit 1
    mv_pkg_patch_path $@
}

main $@


