#!/bin/sh
export JAVA_HOME=/opt/jdk1.8.0_101
cur_dir=$(cd $(dirname $0);pwd)
code_path="${cur_dir}/.."
cd $code_path
mvn clean package -Dmaven.test.skip