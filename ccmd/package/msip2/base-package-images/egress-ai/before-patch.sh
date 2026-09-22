#!/bin/bash
conf_dir="/data/linkx/conf"
cm_name="egress-ai-config"
namespace="linkx"

mkdir -p "$conf_dir"

echo '{"data":' > "${conf_dir}/${cm_name}.cm.json"
kubectl get cm "$cm_name" -n "$namespace" -o jsonpath='{$.data}' >> "${conf_dir}/${cm_name}.cm.json"
echo '}' >> "${conf_dir}/${cm_name}.cm.json"