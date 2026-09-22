#!/bin/bash
version=$1
arch=$2
# INIT
cd "$(dirname "$0")" || exit 1
script_root="$(pwd)"
ccmd_root=$(cd "../../.."&&pwd)
echo "$ccmd_root"

# git update
cd "$ccmd_root"|| exit 1
git reset
git checkout .
git pull || exit 1

# mvn package
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
mvn clean install

# build images
cd "$script_root/../public/cloudcmd-cnx-public-wsserver" || exit 1
docker build -f "Dockerfile-${arch}" -t "127.0.0.1:5000/cloudcmd-cnx-public-wsserver:${version}" . || exit 1
cd "$script_root/../private/cloudcmd-cnx-private-queue" || exit 1
docker build -f "Dockerfile-${arch}" -t "127.0.0.1:5000/cloudcmd-cnx-private-queue:${version}" . || exit 1

# package
cd "$script_root/public" || exit 1
docker save -o cloudcmd-cnx-public-wsserver.s "127.0.0.1:5000/cloudcmd-cnx-public-wsserver:${version}" || exit 1
sed "s/{{ .Values.version }}/${version}/g" cloudcmd-cnx-public-wsserver.yaml.tpl > cloudcmd-cnx-public-wsserver.yaml
tar -zcvf "cnx.public.${version}.tar.gz" ./*.yaml ./*.sh cloudcmd-cnx-public-wsserver.s
rm -f cloudcmd-cnx-public-wsserver.s
rm -f cloudcmd-cnx-public-wsserver.yaml
docker rmi "127.0.0.1:5000/cloudcmd-cnx-public-wsserver:${version}"
mv "cnx.public.${version}.tar.gz" "${ccmd_root}/.."

cd "$script_root/private" || exit 1
docker save -o cloudcmd-cnx-private-queue.s "127.0.0.1:5000/cloudcmd-cnx-private-queue:${version}" || exit 1
sed "s/{{ .Values.version }}/${version}/g" cloudcmd-cnx-private-queue.yaml.tpl > cloudcmd-cnx-private-queue.yaml
tar -zcvf "cnx.private.${version}.tar.gz" ./*.yaml ./*.sh cloudcmd-cnx-private-queue.s
rm -f cloudcmd-cnx-private-queue.s
rm -f cloudcmd-cnx-private-queue.yaml
docker rmi "127.0.0.1:5000/cloudcmd-cnx-private-queue:${version}"
mv "cnx.private.${version}.tar.gz" "${ccmd_root}/.."

cd "${ccmd_root}" || exit 1
mvn clean

cd .. || exit 1
echo "package done for target:$(pwd)/cnx.private.tar.gz $(pwd)/cnx.public.tar.gz"
