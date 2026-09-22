#!/bin/bash
cd "$(dirname "$0")" || exit 1
script_path=$(pwd)
log_config_pim_platform_path=/data/install/IaaS/msiptool/java/conf/log_conf
master1=$(kubectl get node -o jsonpath='{$.items[0].status.addresses[?(@.type=="InternalIP")].address}')

cp -rf "$script_path"/*.xml $log_config_pim_platform_path/. 2>&1
curl -k -X PUT "https://$master1:7777/reloadServiceConf"