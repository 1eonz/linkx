#!/bin/bash
export JAVA_HOME=/opt/jdk1.8.0_101
project_name=ICP-X

#获取脚本的绝对路径
work_path=$(cd $(dirname $0);pwd)
cd ${work_path}/..
build_path=$(pwd)

#新建buildOut用于存放所有*.out,不能在这个目录有其他类型的文件，否则会报错
mkdir buildOut
cd buildOut
buildOut_path=$(pwd)

#生成*.out
cd ${build_path}
/home/uCI/agent/plugins/UADPGuarding/tool/klocwork/bin/kwmaven --output ${buildOut_path}/buildspec_${project_name}.out -f pom.xml -Dmaven.test.skip

#将build_path重定向到ini文件中，只能写一个目录，SAI的bug，暂时不解决
echo  "${buildOut_path}" > ${build_path}/buildOut.ini