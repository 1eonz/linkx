import { socketUrl } from '@/common/config.js';
import { MyWebSocket, Message } from '@/uni_modules/x-web-socket/js_sdk/index.js'; // 路径改为 x-web-socket

// 单例 websocket 实例
let webSocket = null;

/**
 * 初始化 websocket 连接
 * @param {string} token 用户 token
 */
export function initWebSocket() {
  if (webSocket) {
    webSocket.close();
  }

  try {
    webSocket = new MyWebSocket((message) => {
      console.log('收到消息 ------ ', message);
      uni.$emit('newMessage', message);
    }, true);
    // 拼接带 token 的 url
    const url = `${socketUrl}`;
    webSocket.init({ url }); // 传对象
    console.log(webSocket, '========webSocket');
  } catch (error) {
    console.error('WebSocket 初始化失败', error);
  }
}

/**
 * 发送消息
 * @param {string} type 消息类型
 * @param {any} data 消息内容
 */
export function sendMessage(data) {
  if (webSocket) {
    webSocket.send(new Message(data));
  } else {
    console.warn('WebSocket 未初始化，请先调用 initWebSocket');
  }
}

/**
 * 关闭连接
 */
export function closeWebSocket() {
  if (webSocket) {
    webSocket.close();
    webSocket = null;
  }
}

// 监听消息（页面或组件中使用）
export function onMessage(callback) {
  uni.$on('newMessage', callback);
}

// 取消监听
export function offMessage(callback) {
  uni.$off('newMessage', callback);
}
