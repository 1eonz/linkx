#!/usr/bin/bash

curr_dir=$(pwd)
VERSION_NAME=$1
set -ex

GitRemoteUrl="https://codehub.devcloud.cn-north-4.huaweicloud.com/232b9a88535346dca0ed5dce209f0f96/Linkx.git"

function init_env()
{
    CODE_PATH=$(cd $(dirname ${BASH_SOURCE[0]})/../; pwd)
    USER_INFO_FILE=${CODE_PATH}/global_user_info.json
    
    bash ${CODE_PATH}/global_user_init.sh
    
    CODE_USER_NAME=$(python3 -c "import sys, json; print(json.load(open('${USER_INFO_FILE}'))['CodeHub']['CodeUserName'])")
    CODE_USER_PASS=$(python3 -c "import sys, json; print(json.load(open('${USER_INFO_FILE}'))['CodeHub']['CodeUserPass'])")
    CODE_TOKEN=$(python3 -c "import sys, json; print(json.load(open('${USER_INFO_FILE}'))['CodeHub']['CodeToken'])")
    CODE_USER=$(python3 -c "import sys, json; print(json.load(open('${USER_INFO_FILE}'))['CodeHub']['CodeUser'])")
    CODE_EMAIL=$(python3 -c "import sys, json; print(json.load(open('${USER_INFO_FILE}'))['CodeHub']['CodeEmail'])")
    
    git config --global user.name "${CODE_USER}"
    git config --global user.email "${CODE_EMAIL}"
    git config --global credential.helper store
    git config --global color.ui auto
    git config --global https.sslVerify false
    git config --global http.sslVerify false
    
    git config --global url."https://${CODE_USER_NAME}:${CODE_USER_PASS}@codehub.devcloud.cn-north-4.huaweicloud.com".insteadof https://codehub.devcloud.cn-north-4.huaweicloud.com

}

function modifying_plt_version()
{
	cd ${CODE_PATH}/meta
    if [ "X${VERSION_NAME}X" = "XLINKXX" ]; then
        info_file="${CODE_PATH}/meta/app-info-linkx.yaml"
    elif [ "X${VERSION_NAME}X" = "XAGENTX" ]; then
        info_file="${CODE_PATH}/meta/app-info-agent.yaml"
    else
        echo "请输入正确的模块名 LINKX/AGENT"
        exit 1
    fi
	python3 Modifying_Version.py -V "${version_id}" -v "${version_internal_id}" -m "${match_plt}" -a "${allow_his_plt}" -F "${info_file}"
}

function upload_plt()
{
    cd ${CODE_PATH}
    codeBranch=${codeBranch:-$(git branch -a | grep -E '\*|/m/' | grep -v 'no' | sed -E 's#^\* ##;s#^origin##;s#.* ->##')}
    git remote -v || git remote add origin ${GitRemoteUrl}
    git remote set-url origin ${GitRemoteUrl}
    git clean -dxf && git checkout . 
    git fetch --all && git remote prune origin
    git checkout ${codeBranch}

    rm .git/index.lock -rf
    git reset --hard origin/${codeBranch}
    git pull origin ${codeBranch}

    modifying_plt_version

	cd ${CODE_PATH}/meta
	git status
	git add *
	git commit -m "修改版本号为 ${VERSION_NAME} ${version_id}"
	git push origin ${codeBranch} && echo "git push successful"
	
	
}
function main()
{
	init_env
	upload_plt
}

main