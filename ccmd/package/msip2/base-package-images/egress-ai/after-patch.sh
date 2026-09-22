#!/bin/bash
conf_dir="/data/linkx/conf"
cm_name="egress-ai-config"
namespace="linkx"
deployment="egress-ai"

if [ ! -f "${conf_dir}/${cm_name}.cm.json" ]; then
    echo "backup file not exists: ${conf_dir}/${cm_name}.cm.json"
    exit 1
fi

kubectl patch cm "${cm_name}" --type='merge' --patch-file "${conf_dir}/${cm_name}.cm.json" -n "$namespace"
kubectl rollout restart "deployment/${deployment}" -n "${namespace}"
