import os
import platform
import subprocess
import sys
from pathlib import Path
from typing import List, Any, Callable

from common import log


class CommonError(Exception):
    pass


def get_workspace_root() -> Path:
    if getattr(sys, 'frozen', False):
        # 打包后的路径
        return Path(os.path.dirname(sys.executable))
    else:
        # 开发环境下的路径
        return Path(__file__).resolve().parent.parent


def check_admin():
    system = platform.system()
    release = platform.release()
    if system == 'Linux' or system == 'Darwin':
        log.info(f"now running at Linux:{release}")
        if os.geteuid() != 0:
            raise CommonError("Please run as administrator")
    else:
        log.error(f"System not supported: {system}-{release}")
        raise CommonError("System not supported yet.")


def parse_host(host_str: str) -> dict[str, str | None]:
    try:
        host_str = host_str.strip()
        if not host_str:
            raise CommonError(f"parse host error {host_str}")

        # 拆分用户名和主机部分
        user_host = host_str.split("@", 1)
        if len(user_host) == 1:
            username, host_part = None, user_host[0]
        else:
            username, host_part = user_host

        # 拆分 IP 和端口
        ip_port = host_part.split(":", 1)
        if len(ip_port) == 1:
            ip, port = ip_port[0], None
        else:
            ip, port = ip_port

        if not ip:
            raise CommonError(f"parse host error {host_str}")

        return {"user": username, "ip": ip, "port": port}
    except Exception as e:
        print(f"parse host error: {host_str}, 错误: {e}")
        raise CommonError(f"parse host error {host_str}")


def do_subprocess(cmd, msg, env=None, timeout=None):
    try:
        system = platform.system()
        encoding = 'gbk' if system == 'Windows' else 'utf-8'

        # 使用 stdout=PIPE 并实时读取输出
        with subprocess.Popen(
                cmd,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                shell=True,
                env=env,
                text=True,
                encoding=encoding
        ) as proc:
            output = []
            for line in proc.stdout:
                log.info(line.strip())  # 实时打印到控制台
                output.append(line.strip())

            # 等待进程结束并获取返回码
            returncode = proc.wait(timeout=timeout)
            output_str = "\n".join(output)

            if returncode != 0:
                log.error(f"{msg} failed: {output_str}")
                raise CommonError(f"{msg} failed: {output_str}")
    except subprocess.TimeoutExpired:
        error_msg = f"{msg} failed: Timeout expired"
        log.error(error_msg)
        raise CommonError(error_msg)


def do_popen(cmd, env=None, stdout=subprocess.PIPE, stderr=subprocess.STDOUT):
    return subprocess.Popen(cmd, shell=True, stdout=stdout, stderr=stderr, env=env)


def depth_first_sort(items: List[Any], is_parent_func: Callable[[Any, Any], bool],
                     set_level_func: Callable[[Any, int], None]) -> List[Any]:
    # 构建父子关系映射
    children_map = {}
    roots = []

    # 初始化每个节点的子节点列表
    for item in items:
        children_map[item] = []

    # 填充父子关系
    for child in items:
        for parent in items:
            if is_parent_func(parent, child) and parent != child:
                children_map[parent].append(child)
                break
        else:
            # 如果没有找到父节点，则作为根节点
            roots.append(child)

    # 深度优先遍历
    result = []

    def dfs(node, depth=1):
        set_level_func(node, depth)
        result.append(node)
        for c in children_map.get(node, []):
            dfs(c, depth + 1)

    # 从所有根节点开始 DFS
    for root in roots:
        dfs(root)

    return result
