#!/bin/sh
cur_dir=`dirname S0`
cd $cur_dir
cur_dir=`pwd`
ICP_PACKAGE_PATH="${cur_dir}/../cloudcmd-pack/target"

VersionPath=$1
UploadDir=$2
if [ $# -lt 1 ];then
	echo "have no para"
	exit 1;
fi

version_mark=$(dirname $VersionPath)
type_mark=$(basename $VersionPath)

#. ./build_profile.sh
#[ -d ${EAPP_BUILD_ROOT_PATH} ] || { echo "build path is not exist"; exit 1; }
now_date=$(date +20%y-%m-%d)
#pushd ${ICP_PACKAGE_PATH}

LOC_BUILD_ROOT=$cur_dir
#popd

python pack.py

echo "VersionPath:${VersionPath}"
echo "VersionName:${VersionName}"
echo "version_mark:${version_mark}"
echo "VersionType:${VersionType}"
echo "LOC_BUILD_ROOT:${LOC_BUILD_ROOT}"
echo "FtpUserName:${FtpUserName}"
echo "FtpUserPw:${FtpUserPw}"


function put_file_to_server()
{
smbclient //10.120.225.162/write -U root%eLTE@com << EOF
prompt
lcd $LOC_BUILD_ROOT
mkdir ICP
cd ICP
mkdir ICP-X
cd ICP-x
mkdir $version_mark
cd $version_mark
mkdir $type_mark
cd $type_mark
mkdir $now_date
cd $now_date
mkdir $UploadDir
cd $UploadDir
mput cloudcmd.tar.gz
close
exit
EOF
}

for index in `seq 1 600`
do
if [ -n "`put_file_to_server |grep NT_STATUS_SHARING_VIOLATION`" ]
then
echo "put_file_to_server failed $index"
sleep 1
else
echo "put_file_to_server successfully $index"
exit 0
fi
done

exit 1




