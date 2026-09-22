import tarfile
from pathlib import Path

import paramiko

from common import log


class CommonError(Exception):
    pass

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
class Host:
    def __init__(self, _host: str):
        _h_s = _host.strip()
        _h_dict = parse_host(_h_s)
        self._ip = _h_dict["ip"]
        self._user = _h_dict["user"] or "root"
        self._port = _h_dict["port"] or 22
        self._passwd = None
        self._arch = None

    @property
    def ip(self):
        return self._ip

    @property
    def user(self):
        return self._user

    @property
    def port(self):
        return self._port

    @property
    def passwd(self):
        return self._passwd

    @property
    def arch(self):
        return self._arch

    def set_password(self, _passwd: str):
        self._passwd = _passwd

    def set_arch(self, _arch: str):
        self._arch = _arch


class SshSession:

    def __init__(self, _host, _user, _password, _port):
        self.__host = _host
        self.__user = _user
        self.__password = _password
        self.__port = _port
        self.__ssh = None
        self.__sftp = None

    def connect(self):
        self.__ssh = paramiko.SSHClient()
        self.__ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        self.__ssh.connect(self.__host, port=self.__port, username=self.__user, password=self.__password)
        self.__sftp = self.__ssh.open_sftp()

    def close(self):
        self.__sftp.close()
        self.__ssh.close()

    def exec_command(self, cmd):
        stdin, stdout, stderr = self.exec_command_return(cmd)
        for line in iter(stdout.readline, ""):
            log.info(f"Command output: {line.strip()}")

        exit_code = stdout.channel.recv_exit_status()
        if exit_code != 0:
            raise CommonError(f"Command failed with exit code {exit_code}: {stderr.read().decode() or 'Unknown error'}")

    def exec_command_return(self, cmd) -> tuple[
        paramiko.channel.ChannelStdinFile, paramiko.channel.ChannelFile, paramiko.channel.ChannelStderrFile]:
        return self.__ssh.exec_command(cmd)

    def remove(self, remote_addr):
        self.__sftp.remove(remote_addr)

    def put(self, local_path, remote_path):
        self.__sftp.put(local_path, remote_path)

    def put_dir(self, local_path: Path, remote_path: str):
        _tmp = local_path.parent / "tmp.tar.gz"
        _rmt = Path(remote_path) / "tmp.tar.gz"
        # do_subprocess(f"tar -zcvf {_tmp} {local_path.name} -C {local_path.parent}", f"tar {_tmp}")
        with tarfile.open(f"{_tmp}", "w:gz") as tar:
            tar.add(local_path, arcname=local_path.name)
        self.exec_command(f"mkdir -p {remote_path}")
        print(f"{_tmp} {_rmt.as_posix()}")
        self.__sftp.put(f"{_tmp}", f"{_rmt.as_posix()}")
        self.exec_command(f"cd {_rmt.parent.as_posix()} && tar -xvf {_rmt.as_posix()} && rm -f {_rmt.as_posix()}")
        _tmp.unlink()

    @property
    def host(self):
        return self.__host

    @property
    def user(self):
        return self.__user

    @property
    def password(self):
        return self.__password

    @property
    def port(self):
        return self.__port

    @property
    def ssh(self):
        return self.__ssh

    @property
    def sftp(self):
        return self.__sftp