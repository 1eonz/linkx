#!/bin/bash

set -eux
set -o pipefail

tool_dir=$1
install_dir=${tool_dir}/jdk-21.0.1
download_jdk21_url="http://mirrors.huaweicloud.com/openjdk/21.0.1/openjdk-21.0.1_linux-x64_bin.tar.gz"

# download openjdk-21.0.1_linux-x64_bin.tar.gz
[ -f 'openjdk-21.0.1_linux-x64_bin.tar.gz' ] || wget ${download_jdk21_url}

# check install files
ls openjdk-21.0.1_linux-x64_bin.tar.gz

[ -d "${install_dir}" ] && rm -rf ${install_dir}
mkdir -p ${install_dir}

tar -xzf openjdk-21.0.1_linux-x64_bin.tar.gz
cp -fa jdk-21.0.1/*  ${install_dir}

chmod 755 -R ${install_dir}
# Environment Variable
ls ${tool_dir}/tools_profile.sh && sed -i '/JAVA_HOME/d' ${tool_dir}/tools_profile.sh
ls ${tool_dir}/tools_profile.sh && sed -i '/JRE_HOME/d' ${tool_dir}/tools_profile.sh
echo "export JAVA_HOME=${install_dir}" >> ${tool_dir}/tools_profile.sh
echo "export JRE_HOME=${install_dir}/jre" >> ${tool_dir}/tools_profile.sh
echo 'export CLASSPATH=$JAVA_HOME/lib:$JRE_HOME/lib:$CLASSPATH' >> ${tool_dir}/tools_profile.sh
echo 'export PATH=$JAVA_HOME/bin:$JRE_HOME/bin:$PATH' >> ${tool_dir}/tools_profile.sh

# delete
[ -d 'jdk-21.0.1' ] && rm -rf jdk-21.0.1
ls openjdk-21.0.1_linux-x64_bin.tar.gz && rm -f openjdk-21.0.1_linux-x64_bin.tar.gz

# echo install dir
echo "Install Path=${install_dir}"

# 安装华为和鼎桥JDK证书
# mkdir -p ${install_dir}/lib/security
# cd ${install_dir}/lib/security
# wget --no-check-certificate  https://devcloud.cn-north-4.huaweicloud.com/artifactory/CMC-Release/certificates/HuaweiITRootCA.cer
# wget --no-check-certificate  https://devcloud.cn-north-4.huaweicloud.com/artifactory/CMC-Release/certificates/HWITEnterpriseCA1.cer

# ${install_dir}/bin/keytool -keystore cacerts -importcert -alias HuaweiITRootCA -file HuaweiITRootCA.cer -storepass changeit -noprompt
# ${install_dir}/bin/keytool -keystore cacerts -importcert -alias HWITEnterpriseCA1 -file HWITEnterpriseCA1.cer -storepass changeit -noprompt
# chmod 755 -R ${install_dir}/lib/security/cacerts

# cd ${install_dir}/lib/security
# ${install_dir}/bin/keytool -list -V  -keystore cacerts -storepass changeit | grep -i HuaweiITRootCA
# ${install_dir}/bin/keytool -list -V  -keystore cacerts -storepass changeit | grep -i HWITEnterpriseCA1
