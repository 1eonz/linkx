/**
 * 发布订阅模式
 * WebSocket 订阅 与 发布
 */
import { stringParseJson } from '@/utils';

const messageHandlers = {};

// 订阅
function on(module, type, handler) {
  if (module && type && handler) {
    const handlerKey = `${module}_${type}`;
    if (!messageHandlers[handlerKey]) {
      messageHandlers[handlerKey] = new Set();
    }
    messageHandlers[handlerKey].add(handler);
  } else {
    console.error(`handler error, handler is:${handler}`);
  }
  return handler;
}

// 取消订阅
function off(module, type, handler) {
  if (module && type && handler) {
    const handlerKey = `${module}_${type}`;
    if (!messageHandlers[handlerKey]) {
      console.error(`unregist error: no handler handlerKey:${handlerKey}handler:${handler}`);
    }
    messageHandlers[handlerKey].delete(handler);
  } else {
    console.error(`unregist handler error, handler is:${handler}`);
  }
}

// 发布 注册全局的消息通道
function onmessage(message) {
  message = message.data.replaceAll(new RegExp('\\\\', 'g'), ''); // 对斜线的转义
  try {
    const dataContent = stringParseJson(message);
    const module = dataContent.module; // 模块名称
    const notifyType = dataContent.notifyType; // 消息类型
    const dataValue = dataContent.data; // 消息内容
    const handlerKey = `${module}_${notifyType}`;
    if (messageHandlers[handlerKey] && messageHandlers[handlerKey].size > 0) {
      const handlers = messageHandlers[handlerKey];
      for (const item of handlers) {
        item(dataValue);
      }
    } else {
      // console.log('no handler process message,message:' + message)
    }
  } catch (error) {
    console.error(JSON.stringify(error));
  }
}

// 没用到
function pushMessageToWeb(message) {
  message = message.replaceAll(new RegExp('\\\\', 'g'), ''); // 对斜线的转义
  // console.log('getMessage:' + message)
  try {
    const dataContent = stringParseJson(message);
    const module = dataContent.module; // 模块名称
    const notifyType = dataContent.notifyType; // 消息类型
    const dataValue = dataContent.data; // 消息内容
    const handlerKey = `${module}_${notifyType}`;
    if (messageHandlers[handlerKey] && messageHandlers[handlerKey].size > 0) {
      const handlers = messageHandlers[handlerKey];
      for (const item of handlers) {
        item(dataValue);
      }
    } else {
      // console.log('no handler process message,message:' + message)
    }
  } catch (error) {
    console.error(JSON.stringify(error));
  }
}

export default {
  off,
  on,
  onmessage,
  pushMessageToWeb,
};
