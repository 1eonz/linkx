download_dir=$1
curr_dir=$(pwd)
LINKX_CODE_PATH=$(cd $(dirname ${BASH_SOURCE[0]}); pwd)
download_dir=$(cd ${download_dir}; pwd)
cd ${curr_dir}

sha512_file=${download_dir}/ePack.sha512
set -xe

function create_sha512() {
  if [ -f ${sha512_file} ]; then
    rm -rf ${sha512_file}
  fi
  pushd ${download_dir}
  OLDIFS="$IFS"
  IFS=$'\n'
  for f_dir in $(find . -type f); do
    sha512sum $f_dir >> ${sha512_file}
  done
  IFS="$OLDIFS"
  create_uniquedir=$(md5sum $sha512_file | awk '{print $1}')
  filename=$(basename $sha512_file)
  popd
}

###  蓝区签名方式 ###
function blue()
{
  cd ${download_dir}
  wget http://safeconf.rd.td-tech.com/CI-manifest/signconf_cms/signconf.json
  wget http://safeconf.rd.td-tech.com/CI-manifest/signconf_cms/crlfile.crl
  Singed_ADDRESS=$(python3 -c "import sys, json; print(json.load(open('signconf.json'))['SingedConf']['Singed_Address'])")
  Singed_Port=$(python3 -c "import sys, json; print(json.load(open('signconf.json'))['SingedConf']['Singed_Port'])")
  Singed_Alias=$(python3 -c "import sys, json; print(json.load(open('signconf.json'))['SingedConf']['Singed_Alias'])")
  Singed_HashType=$(python3 -c "import sys, json; print(json.load(open('signconf.json'))['SingedConf']['Singed_HashType'])")
  signaturestandard=$(python3 -c "import sys, json; print(json.load(open('signconf.json'))['SingedConf']['signaturestandard'])")
  productlineid=$(python3 -c "import sys, json; print(json.load(open('signconf.json'))['SingedConf']['productlineid'])")
  versionid=$(python3 -c "import sys, json; print(json.load(open('signconf.json'))['SingedConf']['versionid'])")
  
  for ((i=3; i>0; i--)); do
    if ! curl --location --noproxy ${Singed_ADDRESS} --request POST "http://${Singed_ADDRESS}:${Singed_Port}/sign" \
      --form "alias=${Singed_Alias}" \
      --form "file=@${sha512_file}" \
      --form "hashtype=${Singed_HashType}" \
      --form "signaturestandard=${signaturestandard}" \
      --form "productlineid=${productlineid}" \
      --form "versionid=${versionid}" \
      -J -O; then
      sleep 3
      continue
    fi
    rm -f signconf.json
    exit 0
  done
  exit 1
}

create_sha512
blue