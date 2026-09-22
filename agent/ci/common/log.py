# log.py
import logging
from datetime import datetime

from common import utils

info, trace, error = None, None, None

# 全局 logger 变量
logger = None


def __file_log__():
    global logger
    log_dir = utils.get_workspace_root() / f"logs/{datetime.now().strftime("%Y%m%d%H")}"
    if not log_dir.exists():
        log_dir.mkdir(parents=True)

    # 日志格式（用于文件和控制台基础格式）
    formatter = logging.Formatter(
        '[%(levelname)s] [%(asctime)s] [%(filename)s:%(lineno)d] %(message)s'
    )

    # 初始化 logger
    logger = logging.getLogger("udc_tool")
    logger.setLevel(logging.DEBUG)

    if logger.handlers:
        return

    # 控制台 handler（带颜色）
    console_handler = logging.StreamHandler()
    console_handler.setLevel(logging.INFO)

    # 标准日志格式（去除颜色）
    standard_formatter = logging.Formatter(
        '[%(levelname)s] [%(asctime)s] [%(filename)s:%(lineno)d] %(message)s',
        datefmt=None
    )
    console_handler.setFormatter(standard_formatter)
    logger.addHandler(console_handler)

    # 文件 handlers
    def _add_file_handler(level, filename):
        file_handler = logging.FileHandler(log_dir / filename, encoding='utf-8')
        file_handler.setLevel(level)
        file_handler.setFormatter(formatter)  # 文件中不使用彩色
        logger.addHandler(file_handler)

    _add_file_handler(logging.INFO, "info.log")
    _add_file_handler(logging.DEBUG, "trace.log")
    _add_file_handler(logging.ERROR, "error.log")

    def _log_wrapper(level):
        def log_func(msg):
            if logger:
                logger.log(level, msg)

        return log_func

    # 设置全局函数
    global info, trace, error
    info = _log_wrapper(logging.INFO)
    trace = _log_wrapper(logging.DEBUG)
    error = _log_wrapper(logging.ERROR)


def __print_log__():
    global logger
    # 初始化 logger
    logger = logging.getLogger("udc_tool_console")
    logger.setLevel(logging.DEBUG)

    if logger.handlers:
        return

    # 创建彩色日志格式器
    standard_formatter = logging.Formatter(
        '[%(levelname)s] [%(asctime)s] [%(filename)s:%(lineno)d] %(message)s',
        datefmt=None
    )

    # 控制台 handler（带颜色）
    console_handler = logging.StreamHandler()
    console_handler.setLevel(logging.DEBUG)
    console_handler.setFormatter(standard_formatter)
    logger.addHandler(console_handler)

    def _log_wrapper(level):
        def log_func(msg):
            if logger:
                logger.log(level, msg)

        return log_func

    # 设置全局函数
    global info, trace, error
    info = _log_wrapper(logging.INFO)
    trace = _log_wrapper(logging.DEBUG)
    error = _log_wrapper(logging.ERROR)


def init_logger(mode: str = "file"):
    """
    初始化日志系统

    :param mode: "file" 使用文件+控制台日志；"print" 仅控制台输出并启用彩色日志
    """
    if mode == "file":
        __file_log__()
    elif mode == "print":
        __print_log__()
    else:
        raise ValueError(f"Unsupported log mode: {mode}")

    global info, trace, error


# 默认初始化为 file 模式
init_logger("print")
