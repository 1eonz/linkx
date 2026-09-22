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
source ${common_sh}

function main(){
    if [ $# -lt 1 ]; then
        exit 1
        echo "param error"
    fi
    cur_timer=$(date +'%Y%m%d%H%M%S')
    cur_target_path=${path_pkg}/${cur_timer}
    compile_h5portal
    compile_icc
    compile_admin

    if [ "${Arch}" == "all" ]; then
      archs=("x86" "arm")
      for el in "${archs[@]}"; do
        Arch=${el}
	      echo "================================= build ${Arch} pkg start  ================================="
        mkdir -p $cur_target_path/${Arch}
        compile_ccmd
        rm -rf "${path_workspace}/ci_package"
        cp -r "${path_workspace}/${dir_ccmd}/package/msip2" "${path_workspace}/ci_package" || exit 1
        sh -x "$path_workspace/ci_package/package.sh" $abs_conf || exit 1

        mv_pkg_path $@
        rm -rf "${path_workspace}/ci_package"
	      echo "================================= build ${Arch} pkg success ================================="
      done
    else
      mkdir -p $cur_target_path/${Arch}
      compile_ccmd
      rm -rf "${path_workspace}/ci_package"
      cp -r "${path_workspace}/${dir_ccmd}/package/msip2" "${path_workspace}/ci_package" || exit 1
      sh -x "$path_workspace/ci_package/package.sh" $abs_conf || exit 1
      
      mv_pkg_path $@
    fi
}

main $@


