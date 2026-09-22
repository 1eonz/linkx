#!/bin/bash
# USAGE:
#   sh agent_entry.sh              # 全量出包
#   BUILD_DOMAIN=svc1,svc2 sh agent_entry.sh patch  2>&1 | tee build_entry.log  # 聚合出包
set -x

path_workspace=$(cd $(dirname ${BASH_SOURCE[0]})/; pwd)
script_root="${path_workspace}/agent"

# ====== 公共逻辑: 环境初始化 + gitee/ci 信息读取 + snapshots + docker 清理 + venv ======

# Arch (CI 通过环境变量传入, 默认 amd64)
Arch=${Arch:-amd64}
arch=${Arch}

# version 从 app-info-agent.yaml 读取
version=$(grep "version_internal_id" ${path_workspace}/meta/app-info-agent.yaml | egrep -o "V[0-9]{3}+R[0-9]{3}C[0-9]{2}[SPCB0-9]*")

export http_proxy="http://proxy-rd.td-tech.com:8080"
export https_proxy="http://proxy-rd.td-tech.com:8080"
export no_proxy="localhost,127.0.0.1,::1,ftransshr.td-tech.com,rd.td-tech.com,172.25.0.0/16,172.26.0.0/16,172.24.0.0/16,172.24.3.12,172.24.3.14,192.168.*.*,td-tech.net,myhuaweicloud.com,cn-north-4.huaweicloud.com,repo.huaweicloud.com,mirrors.huaweicloud.com"

# 执行人信息 (用于产物上传路径)
export EXECUTOR=${EXECUTOR:-unknown}
export BUILD_USER=${EXECUTOR}
export ENV_PIPELINE_STARTTIME=${ENV_PIPELINE_STARTTIME:-$(date +%Y%m%d%H%M%S)}
export LOCAL_TIME=${ENV_PIPELINE_STARTTIME}

bash ${path_workspace}/global_user_init.sh
USER_INFO_FILE=${path_workspace}/global_user_info.json

gitee_repo=$(python3 -c "import sys, json; print(json.load(open('${USER_INFO_FILE}'))['VERSION_PACKAGE']['Linkx']['Gitee']['GiteeREPO'])")
gitee_api_token=$(python3 -c "import sys, json; print(json.load(open('${USER_INFO_FILE}'))['VERSION_PACKAGE']['Linkx']['Gitee']['GiteeAPIToken'])")
gitee_backup=$(python3 -c "import sys, json; print(json.load(open('${USER_INFO_FILE}'))['VERSION_PACKAGE']['Linkx']['Gitee']['GiteeBackUP'])")

cd $script_root
#git pull

commitID=$(git rev-parse HEAD)
codeUrl=$(git remote -v | grep fetch | awk '{print $2}')
codeUrl=$(echo ${codeUrl} | sed 's%git@\([^:]*\):\(.*\)%http://\1/\2%')
codeUrl=$(echo ${codeUrl} | sed 's%http.*@%http://%g')
codeUrl="http://****/$(echo ${codeUrl} | sed 's%.*/%%')"
codeBranch=$(git branch -a | grep -E '\*|/m/' | grep -v 'no' | sed -E 's#^\* ##;s#^origin##;s#.* ->##')

[ -f ${script_root}/ci/gitee_env.toml ] && rm -rf ${script_root}/ci/gitee_env.toml
cat > "${script_root}/ci/gitee_env.toml" << EOF
[commons]
gitee_repo="$gitee_repo"
gitee_api_token="$gitee_api_token"
gitee_backup="$gitee_backup"
EOF

[ -f ${script_root}/ci/ci_env.toml ] && rm -rf ${script_root}/ci/ci_env.toml
cat > "${script_root}/ci/ci_env.toml" << EOF
[commons]
version="$version"
arch="$arch"
codeRepo="$codeUrl"
CommitID="$commitID"
codeBranch="$codeBranch"
BuildTime=$(date +'%Y-%m-%d %H:%M:%S').$(printf "%03d\n" $((10#$(date +'%N')/1000000)))
EOF

cp -f ${path_workspace}/create_signconf_cms.sh ${script_root}/ci/create_signconf_cms.sh
cp -f ${path_workspace}/meta/app-info-agent.yaml ${script_root}/ci/app-info.yaml

cd $script_root/ci
wget http://safeconf.rd.td-tech.com/CI-manifest/snapshots/snapshots.zip
unzip snapshots.zip

if [ -n "docker ps -a | grep Exited | awk -F ' ' '{print $1}'" ]; then
  docker ps -a | grep Exited | awk -F ' ' '{print $1}' | xargs docker rm
fi

if [ -n "docker image ls | grep -E 'imagerepo.td-tech.com:5001|127.0.0.1|docker-virtual/linkx|td_build_env/linkx' | awk -F ' ' '{print $1":"$2}')" ]; then
  docker image ls | grep -E 'imagerepo.td-tech.com:5001|127.0.0.1|docker-virtual/linkx|td_build_env/linkx' | awk -F ' ' '{print $1":"$2}' | xargs docker rmi
fi

if [ -n "$(docker image ls | grep none | awk -F ' ' '{print $3}')" ]; then
  docker image ls | grep none | awk -F ' ' '{print $3}' | xargs docker rmi
fi

if [ -n "$(docker ps -a | grep mvn-build | awk -F ' ' '{print $1}')" ]; then
  docker ps -a | grep mvn-build | awk -F ' ' '{print $1}' | xargs docker stop
  docker ps -a | grep mvn-build | awk -F ' ' '{print $1}' | xargs docker rm
fi

# venv 依赖安装 (patch.py / package.py 共用)
source /etc/profile
cd $script_root/ci
poetry update
cd $script_root
${script_root}/ci/.venv/bin/pip3 install toml
${script_root}/ci/.venv/bin/pip3 install PyYAML
${script_root}/ci/.venv/bin/pip3 install paramiko

# ====== 聚合出包逻辑 (补丁模式) ======
function do_patch() {
    local modify_hook_py="${path_workspace}/HookModifier.py"

    # 1. 用 HookModifier 改 func.py 的 daily_path: Daily/... -> Persion/${BUILD_USER}/Agent/...
    local func_py="${script_root}/ci/func.py"
    python3 "${modify_hook_py}" "${func_py}" 'daily_path = f"Daily/' "daily_path = f\"Persion/${BUILD_USER}/Agent/{r_conf.arch}\"" replace

    # 2. 更新 app-info-agent.yaml 中的 path (多值时取首个作为主标识, 避免逗号污染下游解析)
    local app_info_file="${path_workspace}/meta/app-info-agent.yaml"
    if [ -z "${BUILD_DOMAIN:-}" ]; then
        echo "ERROR: BUILD_DOMAIN is not set"
        exit 1
    fi
    local ag_primary="${BUILD_DOMAIN%%,*}"
    local ag_path
    ag_path=$(grep 'path:' "${app_info_file}" | head -1 | sed 's/.*path:[[:space:]]*//' | tr -d '[:space:]')
    python3 "${modify_hook_py}" "${app_info_file}" "path: ${ag_path}" "path: ${ag_primary}" replace

    # 3. 读取 version_patch_id (fail-fast, 补丁包命名依赖)
    local patch_id
    patch_id=$(grep 'version_patch_id' "${app_info_file}" | head -1 | sed 's/.*version_patch_id:[[:space:]]*//' | tr -d '"' | tr -d "'" | tr -d '[:space:]')
    if [ -z "${patch_id}" ]; then
        echo "ERROR: version_patch_id not found in ${app_info_file}, abort"
        exit 1
    fi
    echo "read version_patch_id: ${patch_id}"

    # 4. 解析 BUILD_DOMAIN 多值, 聚合 patch.py 的 -n 参数 + -m/-w flags
    local mvn_flag=""
    local web_flag=""
    local name_args=()
    local domain
    for domain in ${BUILD_DOMAIN//,/ }; do
        case "${domain}" in
            dbjob)
                name_args+=("-n" "db-migrate")
                ;;
            agent-web)
                name_args+=("-n" "web")
                web_flag="-w"
                ;;
            agent-gateway)
                name_args+=("-n" "crs-gateway")
                mvn_flag="-m"
                ;;
            agent-infra)
                name_args+=("-n" "crs-module-infra")
                mvn_flag="-m"
                ;;
            agent-system)
                name_args+=("-n" "crs-module-system")
                mvn_flag="-m"
                ;;
            agent-ai-agent)
                name_args+=("-n" "crs-module-ai-agent")
                mvn_flag="-m"
                ;;
            mock-ai)
                name_args+=("-n" "mock-ai")
                mvn_flag="-m"
                ;;
            *)
                echo "ERROR: unknown BUILD_DOMAIN value: ${domain}, abort"
                exit 1
                ;;
        esac
    done
    echo "patch modules: [${name_args[*]}], mvn: ${mvn_flag:-no}, web: ${web_flag:-no}"

    # 5. 执行 patch.py (补丁打包流程, -n 多值聚合出一个包)
    ${script_root}/ci/.venv/bin/python ${script_root}/ci/patch.py ${name_args[*]} ${mvn_flag} ${web_flag} --package || exit 1
    echo "new package in ${script_root}/dist"
}

# ====== 入口分流 ======
case "$1" in
    patch)
        do_patch
        ;;
    ""|package)
        ${script_root}/ci/.venv/bin/python ${script_root}/ci/package.py || exit 1
        echo "new package in ${script_root}/dist"
        ;;
    *)
        echo "ERROR: unknown mode '$1', supported: (empty=package|patch)"
        exit 1
        ;;
esac
