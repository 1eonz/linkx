work_path=$(cd $(dirname $0);pwd)
echo work_path
for service in `ls`; do
  serviceBase=${work_path}/${service}
  if [ -d ${serviceBase} ];then
    cd ${serviceBase}
    sh build.sh
    cd ..
  fi
done