import { onBeforeUnmount } from 'vue';

import { PIMHttpHeaders } from '@/enums/pim';

// 事件订阅集合
const subscriptions = {
  // 发布事件
  publish(eventName, data) {
    const callbacks = this.subscriptions[eventName];
    if (callbacks) {
      callbacks.forEach((callback) => callback(data));
    }
  },

  // 订阅事件
  subscribe(eventName, callback) {
    if (!this.subscriptions[eventName]) {
      this.subscriptions[eventName] = [];
    }
    this.subscriptions[eventName].push(callback);
  },

  subscriptions: {},

  // 取消订阅
  unsubscribe(eventName, callback) {
    if (this.subscriptions[eventName]) {
      this.subscriptions[eventName] = this.subscriptions[eventName].filter((cb) => cb !== callback);
    }
  },
};

class PIMWebSocket {
  heart: any;
  maxReconnectAttempts: number;
  reconnectAttempts: number;
  reconnectInterval: number;
  url: string;
  ws: null | WebSocket;

  constructor(url) {
    // WebSocket 实例
    this.ws = null;
    // WebSocket 服务器地址
    this.url = url;
    // 重连尝试次数
    this.reconnectAttempts = 0;
    // 最大重连次数
    this.maxReconnectAttempts = 5;
    // 重连间隔（毫秒）
    this.reconnectInterval = 3000;

    // 初始化连接
    this.connect();
  }

  // 关闭连接
  close() {
    this.ws?.close();
  }

  // 创建 WebSocket 连接
  connect() {
    this.ws = new WebSocket(this.url);

    // 连接成功
    this.ws.onopen = () => {
      console.log('WebSocket connected');
      this.reconnectAttempts = 0; // 重置重连计数器
      this.send('app', 'register');
      this.heartbeat();
    };

    // 接收消息
    this.ws.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        // 发布消息到对应的订阅事件
        subscriptions.publish(`${message.module}_${message.notifyType}`, message.data);
      } catch (error) {
        console.error('Error parsing message:', error);
      }
    };

    // 连接关闭
    this.ws.onclose = (event) => {
      console.log('WebSocket closed:', event.reason);
      // 自动重连逻辑
      if (this.reconnectAttempts < this.maxReconnectAttempts) {
        setTimeout(() => {
          this.reconnectAttempts++;
          console.log(`Reconnecting (attempt ${this.reconnectAttempts})...`);
          this.connect();
        }, this.reconnectInterval);
      }
    };

    // 连接错误
    this.ws.onerror = (error) => {
      console.error('WebSocket error:', error);
      this.ws?.close();
    };
  }

  // 心跳请求 服务端五分钟没收到心跳就主动断链
  heartbeat() {
    clearInterval(this.heart);

    this.heart = setInterval(
      () => {
        this.send('app', 'ping');
      },
      3 * 60 * 1000,
    );
  }

  // 发送消息
  send(module, notifyType) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      const config = {
        commId: localStorage.getItem(PIMHttpHeaders.COMMID),
        module,
        notifyType,
        token: localStorage.getItem(PIMHttpHeaders.ACCESSTOKEN),
        userId: localStorage.getItem(PIMHttpHeaders.USERID),
      };
      this.ws?.send(JSON.stringify({ ...config }));
    } else {
      console.error('WebSocket is not open');
    }
  }
}

let wsPubSub: null | PIMWebSocket = null;

export function usePIM() {
  // 初始化
  const init = (url: string) => {
    wsPubSub = new PIMWebSocket(url);
  };

  // 取消订阅事件
  const off = (module, notifyType, handler) => {
    subscriptions.unsubscribe(`${module}_${notifyType}`, handler);
  };

  // 订阅事件
  const on = (module, notifyType, handler) => {
    subscriptions.subscribe(`${module}_${notifyType}`, handler);

    onBeforeUnmount(() => {
      off(module, notifyType, handler);
    });
  };

  // 发送消息
  const emit = (module, notifyType) => {
    wsPubSub?.send(module, notifyType);
  };

  return {
    emit,
    init,
    off,
    on,
  };
}
