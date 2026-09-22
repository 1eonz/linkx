#!/bin/bash

curr_dir=$(pwd)
export USER_INFO_PATH=$(cd $(dirname ${BASH_SOURCE[0]}); pwd)
chmod 755 -R ${USER_INFO_PATH}
cd ${curr_dir}

function Get_JSON_Value() {
  local file="$1"
  local key="$2"
  python3 - <<EOF
import json
with open("$file") as f:
    data = json.load(f)
for k in "$key".split('.'):
    data = data[k]
print(data, end="")
EOF
}

function Set_JSON_Value() {
  local file="$1"
  local key="$2"
  local value="$3"
  python3 - <<EOF
import json
with open("$file") as f:
    data = json.load(f)
keys = "$key".split('.')
obj = data
for k in keys[:-1]:
    obj = obj[k]
obj[keys[-1]] = "$value"
with open("$file", 'w') as f:
    json.dump(data, f, indent=4)
EOF
}

function Updata_Info()
{
    local remote_info=$1
    local local_info=$2
    local key=$3
    
    temp_info="$(Get_JSON_Value "$remote_info" "$key")"  # 注意双引号，防止值里有空格被截断
    Set_JSON_Value ${local_info} "${key}" "${temp_info}"
}

function main()
{
    cd ${USER_INFO_PATH}
    wget http://safeconf.rd.td-tech.com/CI-manifest/Linkx/global_user_domain.json
    # wget http://10.148.193.69/CI-manifest/Linkx/global_user_domain.json
    local remote_info="${USER_INFO_PATH}/global_user_domain.json"
    local local_info="${USER_INFO_PATH}/global_user_info.json"
    # 更新代码仓配置信息
    Updata_Info ${remote_info} ${local_info} "CodeHub.CodeUserName"
    Updata_Info ${remote_info} ${local_info} "CodeHub.CodeUserPass"
    Updata_Info ${remote_info} ${local_info} "CodeHub.CodeToken"
    Updata_Info ${remote_info} ${local_info} "CodeHub.CodeUser"
    Updata_Info ${remote_info} ${local_info} "CodeHub.CodeEmail"

    # 更新版本包上传信息
    Updata_Info ${remote_info} ${local_info} "VERSION_PACKAGE.Linkx.TYPE"
    Updata_Info ${remote_info} ${local_info} "VERSION_PACKAGE.Linkx.Gitee.GiteeBackUP"
    Updata_Info ${remote_info} ${local_info} "VERSION_PACKAGE.Linkx.Gitee.GiteeREPO"
    Updata_Info ${remote_info} ${local_info} "VERSION_PACKAGE.Linkx.Gitee.GiteeAPIToken"
    Updata_Info ${remote_info} ${local_info} "VERSION_PACKAGE.Linkx.CMC.CMCREPO"
    Updata_Info ${remote_info} ${local_info} "VERSION_PACKAGE.Linkx.CMC.CMCUserName"
    Updata_Info ${remote_info} ${local_info} "VERSION_PACKAGE.Linkx.CMC.CMCUserPasswd"

    # 更新Docker仓信息
    Updata_Info ${remote_info} ${local_info} "DockerHub.CloudDragon.DOCKER_REPO"
    Updata_Info ${remote_info} ${local_info} "DockerHub.CloudDragon.REPO_PASSWD"
    Updata_Info ${remote_info} ${local_info} "DockerHub.CloudDragon.REPO_USER"
    rm -rf ${remote_info}
}

main