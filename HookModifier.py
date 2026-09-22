#!/usr/bin/env python3
"""HookModifier - 脚本文件修改工具，支持在保持缩进的前提下进行代码注入"""
import sys
import re
import logging
from pathlib import Path

logging.basicConfig(level=logging.INFO, format='%(levelname)s: %(message)s')
log = logging.getLogger(__name__)

VALID_MODES = {'replace', 'before', 'after'}

# 函数定义匹配：Python(def/async def) + Shell(function/name(){)
FUNCTION_PATTERN = re.compile(
    r'^\s*(async\s+)?def\s+\w+\s*\('      # Python: def name( / async def name(
    r'|^\s*function\s+\w+\s*[\({]'        # Shell: function name( / function name {
    r'|^\s*\w+\s*\(\s*\)\s*\{'            # Shell: name() {
)


def is_function_def(line: str) -> bool:
    """判断是否为函数定义行（排除注释）"""
    stripped = line.lstrip()
    return not stripped.startswith('#') and bool(FUNCTION_PATTERN.match(line))


def modify_file(filepath: str, target: str, modification: str, mode: str = 'replace') -> bool:
    """
    修改文件内容，保持缩进格式

    Args:
        filepath: 目标文件路径
        target: 要匹配的目标字符串（不能为空）
        modification: 要插入/替换的内容
        mode: 操作模式 - replace/before/after

    Returns:
        bool: True成功，False失败
    """
    # 参数校验
    if mode not in VALID_MODES:
        log.error(f"Invalid mode '{mode}'. Valid modes: {', '.join(VALID_MODES)}")
        return False
    if not target:
        log.error("Target string cannot be empty")
        return False

    path = Path(filepath)
    if not path.exists():
        log.error(f"File '{filepath}' not found")
        return False

    # 读取文件
    try:
        content = path.read_text(encoding='utf-8')
        lines = content.splitlines(keepends=True)
    except (PermissionError, UnicodeDecodeError, IOError) as e:
        log.error(f"Failed to read '{filepath}': {e}")
        return False

    # 检测换行符
    linesep = '\r\n' if '\r\n' in content else '\n'

    # 处理每一行
    new_lines = []
    modified = False

    for line in lines:
        line_content = line.rstrip('\r\n')
        line_ending = line[len(line_content):]

        # 跳过函数定义行或不匹配的行
        if is_function_def(line_content) or target not in line_content:
            new_lines.append(line)
            continue

        # 匹配成功，处理替换/插入
        modified = True
        indent = line_content[:len(line_content) - len(line_content.lstrip())]

        # 应用缩进到modification的每一行
        mod_lines = modification.rstrip('\r\n').splitlines()
        indented_mod = linesep.join(indent + m if m.strip() else m for m in mod_lines)

        if mode == 'replace':
            new_lines.append(indented_mod + line_ending)
        elif mode == 'before':
            new_lines.append(indented_mod + line_ending)
            new_lines.append(line)
        else:  # after
            new_lines.append(line)
            new_lines.append(indented_mod + line_ending)

    if not modified:
        log.warning(f"Target '{target}' not found in {filepath}")
        return False

    # 写入文件 (兼容 Python 3.9: write_text 不支持 newline 参数)
    try:
        # path.write_text(''.join(new_lines), newline='', encoding='utf-8')  # Python 3.10+ 才支持 newline, 3.9 会 TypeError
        content = ''.join(new_lines)
        with open(path, 'w', encoding='utf-8', newline='') as f:
            f.write(content)
        return True
    except (PermissionError, UnicodeEncodeError, IOError) as e:
        log.error(f"Failed to write '{filepath}': {e}")
        return False


if __name__ == "__main__":
    if len(sys.argv) < 4:
        print("Usage: python3 HookModifier.py <file> <target> <modification> [mode]")
        print("  mode: replace (default), before, after")
        sys.exit(1)

    script, target, modification = sys.argv[1], sys.argv[2], sys.argv[3]
    mode = sys.argv[4] if len(sys.argv) > 4 else 'replace'

    if modify_file(script, target, modification, mode):
        log.info(f"Successfully modified {script} (mode: {mode})")
        sys.exit(0)
    else:
        sys.exit(1)
