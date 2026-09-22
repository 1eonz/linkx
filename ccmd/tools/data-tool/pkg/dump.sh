#!/bin/bash
cd "$(dirname "$0")" || exit 1
#source ./tools.sh

docker load -i data-tool.s
mysql_ip=$(kubectl get svc -n linkx mysql -o jsonpath='{.spec.clusterIP}')
mkdir -p ./dump
docker run --name xxx \
  --security-opt seccomp=unconfined \
  -v "$(pwd)/dump:/app/dump" \
  -e MYSQL_HOST=${mysql_ip}:3306 \
  -e MYSQL_USER=root \
  -e MYSQL_PASS=password \
  data-tool \
  java \
  -XX:+UseZGC \
  -XX:+UseContainerSupport \
  -jar /app/app.jar dump

echo "restore done,then clean up"
docker rm xxx >/dev/null 2>&1
echo "done"
