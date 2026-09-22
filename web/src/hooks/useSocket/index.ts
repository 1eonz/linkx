/* eslint-disable unicorn/number-literal-case */
import { Buffer } from 'buffer';

import { appConfig } from '@/config';
import { useSetInterval } from '@/hooks';
import { getToken } from '@/utils/auth';
import DC from '@/utils/DC';

interface Socket extends WebSocket {
  onSend?: (arg: any) => void;
  readyState: number;
}

/**
 * webSocket管理类
 * 初始化/断线重连等
 */
class SocketManage {
  headerLength: number;
  heartbeatInterval: number;
  heartbeatLosttimes: number;
  heartbeatType: number;
  heartInterval: any;
  hostPort: string;
  lockReconnect: boolean;
  reconnectionTimes: number;
  reConnectTimer: null | Timeout;
  registerType: number;
  sequence: number;
  socket: null | Socket;
  subscribeType: number;

  constructor(hostPort) {
    this.hostPort = hostPort;
    this.socket = null;
    this.headerLength = 16;
    this.registerType = 20;
    this.subscribeType = 22;
    this.heartbeatType = 32;
    this.reconnectionTimes = 0;
    this.heartbeatLosttimes = 5;
    this.heartbeatInterval = 20;
    this.sequence = 0;
    this.lockReconnect = false;
    this.reConnectTimer = null;
    this.heartInterval = null;
  }

  clearHeartInterval() {
    this.heartInterval?.();
  }

  heartCheck() {
    if (this.reconnectionTimes >= this.heartbeatLosttimes) {
      console.log('socket reconnectionTimes > heartbeatLosttimes');
      this.clearHeartInterval();
      this.reConnect();
    }
    this.reconnectionTimes++;

    const buf = this.parseCagentData(getToken(), '', this.heartbeatType);
    this.socket && this.socket.send(buf);
  }

  initWebSocket() {
    if ('WebSocket' in window) {
      const { DEV } = import.meta.env;
      let agreement = location.protocol === 'http:' ? 'ws' : 'wss';
      if (DEV) {
        agreement = 'wss';
      }
      const url = `${agreement}://${this.hostPort}/linkx/desktop/cagent`;
      try {
        // 实例化socket
        this.socket = new WebSocket(url);
        this.socket.addEventListener('open', this.onOpen.bind(this));
        this.socket.onmessage = this.onMessage.bind(this);
        this.socket.onSend = this.onSend.bind(this);
        this.socket.onerror = this.onError.bind(this);
        this.socket.addEventListener('close', this.onClose.bind(this));
      } catch (error) {
        console.log('socket exception', error);
        this.reConnect();
      }

      window.addEventListener('beforeunload', () => {
        console.log('socket onbeforeunload');
        this.socket?.close?.();
      });
    }
  }

  onClose() {
    console.log('socket onClose');
    this.reConnect();
  }

  onError() {
    console.log('socket onError');
    this.reConnect();
  }

  // 接收
  onMessage(message) {
    this.reconnectionTimes = 0;
    if (message.data === 'hearbeat:heartbeat') {
      return;
    }
    DC.onmessage(message);
  }

  onOpen() {
    if (this.socket?.readyState === 1) {
      this.onSend(
        JSON.stringify({
          jsonrpc: '3.0',
          method: 'cagent_login', // 登录调度台
          params: {
            isdn: appConfig.isdn,
            resourceId: appConfig.resourceId,
            serverip: appConfig.settingData.CAGENT_IP, // cagent ip地址
            serverport: appConfig.settingData.CAGENT_PORT, // cagent 端口号
            token: getToken(), // 由CUDC返回
          },
        }),
      );

      this.onSend(
        JSON.stringify({
          method: 'message_subscribe',
          params: {
            modules: [
              'CBM',
              'MS',
              'ACM',
              'RESOURCE_STATE',
              'VM',
              'LBS',
              'EVIDENCE',
              'GPS',
              'IM',
              'EVENT',
              'PASSPORT',
              'RESOURCE',
              'MISSION',
              'MONITOR',
              'IAP',
              'DUTY',
              'GROUP_CHANGE_MESSAGE',
              'GLOBAL',
              'LICENSE',
              'CLOUDCMD_IM_JINGXIN',
              'switch_status',
            ],
          },
        }),
      );
    }
  }

  onSend(arg) {
    const _token = getToken();
    const jsonData = JSON.parse(arg);
    const subMoudles = jsonData.params.modules;
    switch (jsonData.method) {
      case 'cagent_login': {
        const body = {
          heartbeatLosttimes: this.heartbeatLosttimes,
          heartbeatTimeout: this.heartbeatInterval,
        };
        const loginBuf = this.parseCagentData(_token, JSON.stringify(body), this.registerType);
        this.socket?.send(loginBuf);
        this.clearHeartInterval();
        this.heartInterval = useSetInterval(
          this.heartCheck.bind(this),
          this.heartbeatInterval * 1000,
        );
        console.log('socket connect success');

        break;
      }
      case 'closeWindow': {
        console.log('socket close mainWindow ...');

        break;
      }
      case 'message_send': {
        console.log(`socket message_send ${arg}`);

        const msgBuf = this.parseCagentData(_token, JSON.stringify(subMoudles), this.subscribeType);
        console.log(`socket message_send ${msgBuf}\n`);

        this.socket?.send(msgBuf);

        break;
      }
      case 'message_subscribe': {
        const subBuf = this.parseCagentData(_token, JSON.stringify(subMoudles), this.subscribeType);
        console.log('socket message_subscribe  subscribeType', this.subscribeType);

        this.socket?.send(subBuf);

        break;
      }
      default: {
        console.error('client has not connected or receive data format error...');
      }
    }
  }

  parseCagentData(token, body, type) {
    this.sequence++;
    let bodyLength = 0;
    if (body !== null) {
      bodyLength = body.length;
    }
    const length = this.headerLength + token.length + bodyLength;
    const buf = Buffer.alloc(length);
    buf.writeInt32BE(length, 0);
    buf.writeInt16BE(type, 4);
    buf.writeInt32BE(this.sequence, 6);
    buf.writeInt16BE(token.length, 12);

    const tokenBytes = this.stringToByte(token);
    for (const [i, tokenByte] of tokenBytes.entries()) {
      buf.writeInt8(tokenByte, i + this.headerLength);
    }

    const bodyOffset = this.headerLength + token.length;
    if (body !== null) {
      const bodyBytes = this.stringToByte(body);
      for (const [j, bodyByte] of bodyBytes.entries()) {
        buf.writeInt8(bodyByte, j + bodyOffset);
      }
    }

    return buf;
  }

  /**
   * 延迟和锁住重连,避免重复连接
   */
  reConnect() {
    if (this.lockReconnect) {
      console.log('socket is reConnecting');
      return;
    }
    this.lockReconnect = true;
    this.reconnectionTimes = 0;
    this.clearHeartInterval();

    console.log('socket is reConnecting clear reconnectionTimes', this.reconnectionTimes);

    this.reConnectTimer && clearTimeout(this.reConnectTimer);
    this.reConnectTimer = setTimeout(() => {
      console.log('socket start reConnect');
      this.lockReconnect = false;
      this.initWebSocket();
    }, 10_000);
  }

  stringToByte(str) {
    const bytes: number[] = [];
    const len = str.length;
    let c;

    for (let i = 0; i < len; i++) {
      c = str.charCodeAt(i);
      if (c >= 0x01_00_00 && c <= 0x10_ff_ff) {
        bytes.push(
          ((c >> 18) & 0x07) | 0xf0,
          ((c >> 12) & 0x3f) | 0x80,
          ((c >> 6) & 0x3f) | 0x80,
          (c & 0x3f) | 0x80,
        );
      } else if (c >= 0x00_08_00 && c <= 0x00_ff_ff) {
        bytes.push(((c >> 12) & 0x0f) | 0xe0, ((c >> 6) & 0x3f) | 0x80, (c & 0x3f) | 0x80);
      } else if (c >= 0x00_00_80 && c <= 0x00_07_ff) {
        bytes.push(((c >> 6) & 0x1f) | 0xc0, (c & 0x3f) | 0x80);
      } else {
        bytes.push(c & 0xff);
      }
    }
    return bytes;
  }
}

export function useSocketManage() {
  // 开发环境走本地 Vite 服务器，由 Vite 代理转发到后端；
  // 生产环境直接使用当前页面的 host
  const host = location.host;

  return new SocketManage(host);
}
