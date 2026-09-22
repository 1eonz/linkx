#!/bin/bash

# 解析整个 INI 文件并导出变量
parse_ini() {
    local file=$1
    local prefix=$2
    local section=""

    while IFS= read -r line; do
        # 跳过空行和注释
        [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue

        # 检查是否为 section
        if [[ "$line" =~ ^\[(.*)\][[:space:]]*$ ]]; then
            section="${BASH_REMATCH[1]}"
            continue
        fi

        # 检查是否为键值对
        if [[ "$line" =~ ^([a-zA-Z0-9_]+)[[:space:]]*=[[:space:]]*(.*)$ ]]; then
            local key="${BASH_REMATCH[1]}"
            local value="${BASH_REMATCH[2]}"

            # 去除值前后的空格和引号
            value=$(echo "$value" | xargs | sed -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'$//")

            # 导出变量
            if [[ -n "$section" ]]; then
                echo "export ${prefix}_${section}_${key}=\"$value\""
            else
                echo "export ${prefix}_${key}=\"$value\""
            fi
        fi
    done < "$file"
}

# 使用示例
# eval $(parse_ini "config.ini" "CONFIG")
# echo $CONFIG_mysql_host
