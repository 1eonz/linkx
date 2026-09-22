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

if [ -n "docker ps -a | grep Exited | awk -F ' ' '{print $1}'" ]; then
  docker ps -a | grep Exited | awk -F ' ' '{print $1}' | xargs docker rm
fi

if [ -n "docker image ls | grep -E 'imagerepo.td-tech.com:5001|127.0.0.1|docker-virtual/linkx|td_build_env/linkx' | awk -F ' ' '{print $1":"$2}')" ]; then
  docker image ls | grep -E 'imagerepo.td-tech.com:5001|127.0.0.1|docker-virtual/linkx|td_build_env/linkx' | awk -F ' ' '{print $1":"$2}' | xargs docker rmi
fi

if [ -n "$(docker image ls | grep none | awk -F ' ' '{print $3}')" ]; then
  docker image ls | grep none | awk -F ' ' '{print $3}' | xargs docker rmi
fi



whoami
abs_conf="$(cd "$(dirname $1)" || exit 1;pwd)/$(basename $1)"
user_info_init="$(cd "$(dirname $1)" || exit 1;pwd)/$(basename global_user_init.sh)"

bash  "$user_info_init"
# shellcheck source=./config.conf
source $abs_conf
echo "source $abs_conf done"

export http_proxy="http://proxy-rd.td-tech.com:8080"
export https_proxy="http://proxy-rd.td-tech.com:8080"
export no_proxy="localhost,127.0.0.1,::1,ftransshr.td-tech.com,rd.td-tech.com,172.25.0.0/16,172.26.0.0/16,172.24.0.0/16,172.24.3.12,172.24.3.14,192.168.*.*,td-tech.net,myhuaweicloud.com,cn-north-4.huaweicloud.com,repo.huaweicloud.com,mirrors.huaweicloud.com"

export NODE16_HOME="/opt/buildtools/node-v16.15.1"
export NODE22_HOME="/opt/buildtools/node-v22.2.0"

bash $path_workspace/$dir_ccmd/CI/openjdk-21.0.1.sh $path_workspace/$dir_ccmd/CI/

function logger(){
    echo "$0 $1"
}

function compile_icc(){
    logger "compile icc"
    cd "$path_workspace/$dir_icc" || exit 1
    #nvm use 22.2.0
    export NODE_HOME="${NODE22_HOME}"
    export PATH="${NODE_HOME}/bin:$PATH"
    node -v
    #yarn cache clean
    npm install -g pnpm@10.33.4
    pnpm install
    pnpm i element-plus@2.4.3
    pnpm build:zjk
    if [ $? -gt 0 ]; then
        logger "build web error"
        exit 12
    fi
}

function compile_admin(){
    logger "compile admin"
    cd "$path_workspace/$dir_admin" || exit 1
    #nvm use 16.15.1
    export NODE_HOME="${NODE16_HOME}"
    export PATH="${NODE_HOME}/bin:$PATH"
    node -v
    #yarn cache clean
    yarn install
    yarn build:prod
    if [ $? -gt 0 ]; then
        logger "build admin web error"
        exit 12
    fi
}

function compile_h5portal(){
  logger "compile h5portal"
  rm -rf $path_workspace/$dir_h5portal/unpackage
  cd "$path_workspace/$dir_h5portal" || exit 1
  #npm install
  #npm run build
  export NODE_HOME="${NODE22_HOME}"
  export PATH="${NODE_HOME}/bin:$PATH"
  node -v
  npm install -g pnpm@10.33.4
  pnpm install --frozen-lockfile
  pnpm run build
  if [ $? -gt 0 ]; then
      logger "build h5 error"
      exit 12
  fi
}

function compile_ccmd(){
    logger "compile server"
    rm -f "$path_workspace/$dir_ccmd/ci_package/package/"*.tar.gz 2>/dev/null || true
    cd "$path_workspace/$dir_ccmd/cloudcmd-service/cloudcmd-base/cloudcmd-base-service/src/main/resources/" || exit 1
    cd "$path_workspace/$dir_ccmd" || exit 1
    #export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
    export JAVA_HOME=/opt/buildtools/openjdk-11.0.12
    export PATH=$JAVA_HOME/bin:$PATH
    mvn clean install -P ${Arch} -gs $path_workspace/$dir_ccmd/settings.xml
    if [ $? -gt 0 ]; then
        logger "maven install failed"
        exit 12
    fi
}

function mv_pkg_path(){
    codeBranch=${codeBranch:-$(git branch -a | grep -E '\*|/m/' | grep -v 'no' | sed -E 's#^\* ##;s#^origin##;s#.* ->##')}
    daily_path="Daily/`date +%Y%m%d`/Linkx/${codeBranch}/${Arch}/`date +%Y%m%d%H%M%S`"
    #mv ${path_workspace}/ci_package/target/*.zip $cur_target_path/${Arch}/
    logger "package success"
    echo "file path is ${cur_target_path}/${Arch}/"
    #curl --header "Authorization: Bearer ${gitee_api_token}" -T $cur_target_path/${Arch}/gitinfo.txt "${gitee_repo}/${daily_path}/"
    #curl --header "Authorization: Bearer ${gitee_api_token}" -T $cur_target_path/${Arch}/*.htm "${gitee_repo}/${daily_path}/"
    curl --header "Authorization: Bearer ${gitee_api_token}" -T ${path_workspace}/ci_package/target/*.zip "${gitee_repo}/${daily_path}/"
}
