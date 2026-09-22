#!/bin/sh
export JAVA_HOME=/opt/jdk1.8.0_101
cur_dir=$(cd $(dirname $0);pwd)
export code_path=${cur_dir}/..
./CodeMars.sh -j -tool "SecFinder-J" -source $code_path/cloudcmd-common,$code_path/cloudcmd-framework,$code_path/cloudcmd-service -output $1 -uciInput $2