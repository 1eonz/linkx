import { getFullPageUrl } from '@/utils'

export function useApprovalWebSocket() {
  let approvalWebSocket = null
  let cachedWsUrl = null
  let pingTimer = null

  // 事件管理
  const events = {}

  function on(type, event, cb) {
    const key = cb ? `${type}:${event}` : type
    const fn = cb || event;
    (events[key] ??= new Set()).add(fn)
  }

  function off(type, event, cb) {
    const key = cb ? `${type}:${event}` : type
    const fn = cb || event
    events[key]?.delete(fn)
  }

  function emit(key, ...args) {
    events[key]?.forEach(fn => fn(...args))
  }

  
  // 发送心跳
  function sendPing() {
    approvalWebSocket?.send('ping')
  }
  
  // 清理心跳
  function clearPingTimer() {
    if (pingTimer) {
      clearInterval(pingTimer)
      pingTimer = null
    }
  }

  // 构建 WebSocket URL
  function buildWsUrl(httpUrl, idCard) {
    // 这里 idCard 是用户的身份证号，用于唯一标识用户
    // /transfer/ws?idCard=xxxxxxx
    const { protocol, host, pathname } = new URL(httpUrl)
    const wsProtocol = protocol === 'https:' ? 'wss:' : 'ws:'
    const wsPath = pathname.replace(/\/$/, '') + '/transfer/ws'
    const query = `?idCard=${encodeURIComponent(idCard)}`
    return `${wsProtocol}//${host}${wsPath}${query}`
  }
  
  // 解析 WebSocket JSON 字符串消息
  function parseJson(str) {
    if (typeof str === 'object') return str
    try {
      return JSON.parse(str)
    } catch {
      return null
    }
  }
  
  // 处理 WebSocket 消息
  function processWebSocketMessage({ data: rawData } = {}) {
    if (!rawData || rawData === 'pong') return

    const data = parseJson(rawData)
    const content = data && parseJson(data.content)
    if (!content) return

    emit(data.type, content)
    if (content.event) {
      emit(`${data.type}:${content.event}`, content)
    }
  }
  
  // 连接 WebSocket 服务器
  function connectWebSocket(wsUrl) {
    approvalWebSocket = new WebSocket(wsUrl)

    approvalWebSocket.onopen = () => {
      clearPingTimer()
      pingTimer = setInterval(sendPing, 10000)
      emit('open')
    }

    approvalWebSocket.onmessage = processWebSocketMessage

    approvalWebSocket.onerror = (error) => {
      clearPingTimer()
      emit('error', error)
    }

    approvalWebSocket.onclose = (event) => {
      approvalWebSocket = null
      clearPingTimer()
      emit('close', event)
    }
  }

  function isConnected() {
    return approvalWebSocket && approvalWebSocket.readyState === WebSocket.OPEN
  }

  async function reconnect() {
    if (isConnected()) return
    close()
    await connect()
  }

  async function connect(idCard) {
    try {
      if (!cachedWsUrl) {
        const groupAiHost = getFullPageUrl() + '/XA-ics-agent'
        console.log('【useApprovalWebSocket】---------------groupAiHost', groupAiHost);
        cachedWsUrl = buildWsUrl(groupAiHost, idCard)
        console.log('【useApprovalWebSocket】---------------cachedWsUrl', cachedWsUrl);
      }
      connectWebSocket(cachedWsUrl)
    } catch (error) {
      console.error('[ApprovalWS] 初始化失败:', error)
    }
  }
  
  // 关闭 WebSocket 连接
  function close() {
    approvalWebSocket?.close()
    approvalWebSocket = null
  }
  
  // 清理 WebSocket 资源
  function cleanup() {
    clearPingTimer()
    close()
    Object.keys(events).forEach(key => delete events[key])
  }
  
  return { 
    connect, 
    reconnect,
    isConnected,
    close, 
    cleanup, 
    on, 
    off 
  }
}
