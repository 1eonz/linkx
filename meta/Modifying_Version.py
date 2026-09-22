import yaml
import argparse
import logging
import os
import sys
from logging import DEBUG, Formatter, StreamHandler
from subprocess import Popen
import xml.etree.ElementTree as ET
import xml.dom.minidom as xmldom

convertServerPath = lambda path: path.replace("\\", "/")
convertLocalPath = os.path.normpath

# 读取 YAML 文件
def load_yaml(file_path):
    with open(file_path, 'r', encoding='utf-8') as file:
        return yaml.safe_load(file)

# 保存 YAML 文件
def save_yaml(data, file_path):
    with open(file_path, 'w', encoding='utf-8') as file:
        yaml.dump(data, file, allow_unicode=True, default_flow_style=False, sort_keys=False)

    with open(file_path, 'r', encoding='utf-8') as file:
        content = file.read()
    content = content.replace('\'"', '"')
    content = content.replace('"\'', '"')
    with open(file_path, 'w', encoding='utf-8') as file:
        file.write(content)

def Usage():
    parse = argparse.ArgumentParser()
    #parse.add_argument("-t", "--downloadType", type=str, dest="downloadType", default="git", choices=["git", "svn"],
    #                   help="代码仓库管理类型")
    #parse.add_argument("-s", "--sourceUrl", type=str, dest="sourceUrl", required=True, default="abc",
    #                   help="代码仓库地址")
    #parse.add_argument("-l", "--localPath", type=str, dest="localPath", required=True, default="abc", help="下载到本地路径的地址")
    #parse.add_argument("-b", "--gitbranch", type=str, dest="gitbranch", default="master", help="git clone或 pull的分支名")
    #parse.add_argument("-u", "--username", type=str, dest="username", default='p_eRoot', help="下载svn 代码使用的用户名")
    #parse.add_argument("-p", "--password", type=str, dest="password", default="123456", help="下载svn 代码用户名的密码")
    
    parse.add_argument("-V", "--version_id", type=str, dest="version_id", required=True, help="软件版本号")
    parse.add_argument("-v", "--version_internal_id", type=str, dest="version_internal_id", required=True, help="适配层版本号")
    parse.add_argument("-m", "--match_plt", type=str, dest="match_plt", required=True, help="升级路径")
    parse.add_argument("-a", "--allow_his_plt", type=str, dest="allow_his_plt", required=True, help="升级路径")
    parse.add_argument("-F", "--file", type=str, dest="file", required=True, help="版本号文件")
    options = parse.parse_args()

    #options.sourceUrl = convertServerPath(options.sourceUrl)
    #options.localPath = convertLocalPath(options.localPath)
    mylog.info(str(options))
    return options

def getLog():
    log = logging.getLogger()
    log.setLevel(DEBUG)
    fmt = Formatter("[%(asctime)s]-[%(filename)s]-[%(lineno)d]-[%(levelno)s]-[%(message)s]")
    stdout = StreamHandler()
    stdout.setFormatter(fmt)
    log.addHandler(stdout)
    return log

# 将逗号分隔的字符串转换为列表
def parse_comma_separated(value):
    return [item.strip() for item in value.split(',')]


# 替换 YAML 中的变量
def replace_variables(data, replacements):
    if isinstance(data, dict):
        new_data = {}
        for k, v in data.items():
            if k == 'name' or k == 'name-cn':
                new_data[k] = f'"{replacements.get("VERSION_NAME", v)}"'
            elif k == 'version_id':
                new_data[k] = f'"{replacements.get("version_id", v)}"'
            elif k == 'version_internal_id':
                new_data[k] = f'"{replacements.get("version_internal_id", v)}"'

            elif k == 'match-plt':
                # 原本的 YAML 列表
                existing = v if isinstance(v, list) else []
                # 传入的增量值
                new_items = parse_comma_separated(replacements.get('match-plt', ''))
                # 合并 + 去重（保持顺序）
                merged = existing + [x for x in new_items if x not in existing]
                new_data[k] = merged

            elif k == 'allow-his':
                existing = v if isinstance(v, list) else []
                new_items = parse_comma_separated(replacements.get('allow-his', ''))
                merged = existing + [x for x in new_items if x not in existing]
                new_data[k] = merged

            else:
                new_data[k] = replace_variables(v, replacements)

        return new_data

    elif isinstance(data, list):
        return [replace_variables(item, replacements) for item in data]

    elif isinstance(data, str):
        # 替换占位符
        for var, val in replacements.items():
            placeholder = f"${{{var}}}"
            if placeholder in data:
                return data.replace(placeholder, val)
        return data

    else:
        return data


mylog = getLog()
Args = Usage()

if __name__ == '__main__':

    mylog.info(str(Args))
    yaml_file=Args.file

    # 替换内容
    replacements = {
        'version_id': f'{Args.version_id}',
        'version_internal_id': f'{Args.version_internal_id}',
        'match-plt': f'{Args.match_plt}',
        'allow-his': f'{Args.allow_his_plt}'
    }
    # 处理 YAML 文件
    yaml_data = load_yaml(yaml_file)
    replaced_data = replace_variables(yaml_data, replacements)
    # 保存替换后的 YAML 文件
    save_yaml(replaced_data, yaml_file)
