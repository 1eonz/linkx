#!/bin/sh
nacos_ip=$(kubectl get svc -n platform nacos -o jsonpath='{$.spec.clusterIP}')
while true; do
  curl -s "${nacos_ip}:8848/nacos/v1/core/cluster/health";
  if [ $? -eq 0 ]; then
    break;
  fi;
  sleep 2;
 done
curl -X POST "http://${nacos_ip}:8848/nacos/v1/console/namespaces" -d "customNamespaceId=agent&namespaceName=agent&namespaceDesc="

