/**
 * 发布订阅模式
 * WebSocket 订阅 与 发布
 */
function stringParseJson(data) {
  console.log('[DC] stringParseJson raw data:', typeof data, data);
  try {
    return JSON.parse(data);
  } catch (e) {
    try {
      const fixed = data
        .replace(/\r\n/g, '\\r\\n')
        .replace(/\r/g, '\\r')
        .replace(/\n/g, '\\n');
      return JSON.parse(fixed);
    } catch (error) {
      console.error(error, 'stringParseJsonErr');
    }
  }
}

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
  console.log('[DC] onmessage raw:', typeof message, message);
  try {
    const dataContent = stringParseJson(message.data);
    if (!dataContent) return;
    const module = dataContent.module;
    const notifyType = dataContent.notifyType;
    const dataValue = dataContent.data;
    const handlerKey = `${module}_${notifyType}`;
    if (messageHandlers[handlerKey] && messageHandlers[handlerKey].size > 0) {
      const handlers = messageHandlers[handlerKey];
      for (const item of handlers) {
        item(dataValue);
      }
    }
  } catch (error) {
    console.error(JSON.stringify(error), 'getMessageErr');
  }
}

function pushMessageToWeb(message) {
  console.log('[DC] pushMessageToWeb raw:', typeof message, message);
  try {
    const dataContent = stringParseJson(message);
    if (!dataContent) return;
    const module = dataContent.module;
    const notifyType = dataContent.notifyType;
    const dataValue = dataContent.data;
    const handlerKey = `${module}_${notifyType}`;
    if (messageHandlers[handlerKey] && messageHandlers[handlerKey].size > 0) {
      const handlers = messageHandlers[handlerKey];
      for (const item of handlers) {
        item(dataValue);
      }
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
