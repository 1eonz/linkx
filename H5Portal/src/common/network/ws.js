import { Buffer } from 'buffer';

import DC from './DC';

import { getGlobalsConfigByKey } from '@/common/utils';

let token = '';

/**
 * webSocket管理类
 * 初始化/断线重连等
 */
class SocketManage {
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
    this.heartTimer = null;
    this.shouldReconnect = true; // 是否应该自动重连的标志
    this.boundOnOpen = null; // 保存绑定后的 onOpen 函数引用
    this.boundOnClose = null; // 保存绑定后的 onClose 函数引用
    this.watchdogTimer = null; // 看门狗定时器
    this.beforeUnloadBound = false; // 是否已绑定 beforeunload 事件
  }

  clearHeartInterval() {
    clearInterval(this.heartTimer);
    this.heartTimer = null;
  }

  heartCheck() {
    if (this.reconnectionTimes >= this.heartbeatLosttimes) {
      console.log('socket reconnectionTimes > heartbeatLosttimes');
      this.clearHeartInterval();
      this.reConnect();
    }
    this.reconnectionTimes++;
    console.log('socket heartCheck', this.reconnectionTimes);

    const buf = this.parseCagentData('', this.heartbeatType);
    this.socket && this.socket.send(buf);
  }

  async initWebSocket(data) {
    try {
      token = data || (await window.WeSpaceSDK.getStorage('wsAccessToken'));
    } catch (error) {
      console.warn('Failed to get wsAccessToken from storage:', error);
      token = data || '';
    }
    console.log(token, '======token');

    if ('WebSocket' in window) {
      const url = `${this.hostPort}`;
      try {
        // 实例化socket
        this.socket = new WebSocket(url);
        console.log(this.socket, '====WebSocket====');

        // 重置重连标志，允许自动重连
        this.shouldReconnect = true;

        // 保存绑定后的函数引用，以便后续可以正确移除监听器
        this.boundOnOpen = this.onOpen.bind(this);
        this.boundOnClose = this.onClose.bind(this);

        this.socket.addEventListener('open', this.boundOnOpen);
        this.socket.onmessage = this.onMessage.bind(this);
        this.socket.onSend = this.onSend.bind(this);
        this.socket.onerror = this.onError.bind(this);
        this.socket.addEventListener('close', this.boundOnClose);
      } catch (error) {
        console.log('socket exception', error);
        this.reConnect();
      }

      // 只绑定一次 beforeunload 事件，避免重连时重复添加
      if (!this.beforeUnloadBound) {
        window.addEventListener('beforeunload', () => {
          console.log('socket onbeforeunload');
          this.socket?.close?.();
        });
        this.beforeUnloadBound = true;
      }
      // 启动看门狗
      this.startWatchdog();
    }
  }

  onClose() {
    console.log('socket onClose');
    // 只有在允许重连的情况下才自动重连
    if (this.shouldReconnect) {
      this.reConnect();
    }
  }

  onError(error) {
    console.log('socket onError', error);
    this.reConnect();
  }
  // 关闭websocket连接
  closeWebSocket() {
    // 设置标志，阻止自动重连
    this.shouldReconnect = false;
    this.clearHeartInterval();
    this.stopWatchdog(); // 停止看门狗
    this.reConnectTimer && clearTimeout(this.reConnectTimer);
    this.reConnectTimer = null;
    // 移除事件监听器，避免触发 onClose 导致重连
    if (this.socket) {
      if (this.boundOnClose) {
        this.socket.removeEventListener('close', this.boundOnClose);
      }
      if (this.boundOnOpen) {
        this.socket.removeEventListener('open', this.boundOnOpen);
      }
      // 清空其他事件处理器
      this.socket.onmessage = null;
      this.socket.onerror = null;
      this.socket.onSend = null;
      this.socket.close();
    }
    this.socket = null;
    this.boundOnOpen = null;
    this.boundOnClose = null;
  }

  // 接收
  onMessage(message) {
    this.reconnectionTimes = 0;
    if (message.data === 'hearbeat:heartbeat') {
      return;
    }
    console.log('[WS][DEBUG] onMessage 收到原始消息, message.data 类型:', typeof message.data, '长度:', message.data?.length);
    DC.onmessage(message);
  }

  onOpen() {
    console.log(this.socket, '====onOpen socket====');
    if (this.socket?.readyState === 1) {
      this.onSend(
        JSON.stringify({
          jsonrpc: '3.0',
          method: 'cagent_login', // 登录调度台
          params: {
            token: token, // 由CUDC返回
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
              'ALERT ',
            ],
          },
        }),
      );
    }
  }

  onSend(arg) {
    const jsonData = JSON.parse(arg);
    const subMoudles = jsonData.params.modules;
    switch (jsonData.method) {
      case 'cagent_login': {
        const body = {
          heartbeatLosttimes: this.heartbeatLosttimes,
          heartbeatTimeout: this.heartbeatInterval,
        };
        const loginBuf = this.parseCagentData(JSON.stringify(body), this.registerType);
        this.socket?.send(loginBuf);
        this.clearHeartInterval();
        this.heartTimer = setInterval(this.heartCheck.bind(this), this.heartbeatInterval * 1000);
        console.log('socket connect success');

        break;
      }
      case 'closeWindow': {
        console.log('socket close mainWindow ...');

        break;
      }
      case 'message_send': {
        console.log(`socket message_send ${arg}`);

        const msgBuf = this.parseCagentData(JSON.stringify(subMoudles), this.subscribeType);
        console.log(`socket message_send ${msgBuf}\n`);

        this.socket?.send(msgBuf);

        break;
      }
      case 'message_subscribe': {
        const subBuf = this.parseCagentData(JSON.stringify(subMoudles), this.subscribeType);
        console.log('socket message_subscribe  subscribeType', this.subscribeType);

        this.socket?.send(subBuf);

        break;
      }
      default: {
        console.error('client has not connected or receive data format error...');
      }
    }
  }

  parseCagentData(body, type) {
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

  /**
   * 启动看门狗，定期检查连接状态
   */
  startWatchdog() {
    this.stopWatchdog(); // 避免重复创建
    if (!this.shouldReconnect) return;

    console.log('Watchdog started');
    // 5秒检查一次
    this.watchdogTimer = setInterval(() => {
      // 如果不应该重连，则停止看门狗
      if (!this.shouldReconnect) {
        this.stopWatchdog();
        return;
      }

      // 检查socket状态
      // readyState: 0 (CONNECTING), 1 (OPEN), 2 (CLOSING), 3 (CLOSED)
      // 如果不是连接中(0)且不是已打开(1)，说明连接断开了
      if (!this.socket || (this.socket.readyState !== 0 && this.socket.readyState !== 1)) {
        console.log(
          'Watchdog: Socket connection lost (readyState: ' +
            (this.socket ? this.socket.readyState : 'null') +
            '), forcing reconnect...',
        );
        this.reConnect();
      }
    }, 5000);
  }

  /**
   * 停止看门狗
   */
  stopWatchdog() {
    if (this.watchdogTimer) {
      clearInterval(this.watchdogTimer);
      this.watchdogTimer = null;
      console.log('Watchdog stopped');
    }
  }

  stringToByte(str) {
    const bytes = [];
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

export async function useSocketManage() {
  const wsUrl = await getGlobalsConfigByKey('H5_WS_ADDR');
  if (!wsUrl) {
    console.log('未配置websocket地址，请先配置');
  }
  console.log('===websocket连接地址===', wsUrl);
  return new SocketManage(wsUrl);
}
