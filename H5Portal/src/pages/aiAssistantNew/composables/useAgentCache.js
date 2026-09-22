import { useCommunicationStore } from '@/stores/communication.js'

// 获取用户ID
async function getUserId() {
  try {
    const communicationStore = useCommunicationStore()
    const userInfo = await communicationStore.getUserInfo()
    return userInfo?.userid || null
  } catch (e) {
    return null
  }
}

// 获取存储key（带用户ID和模式）
async function getStorageKey(mode = 'general') {
  const userId = await getUserId()
  const prefix = userId ? `agentCache_${userId}` : 'ai_assistant_agent_cache'
  return `${prefix}_${mode}`
}

// 判断是否为 WeSpace 环境
function isWeSpaceEnv() {
  return typeof window !== 'undefined' && window.WeSpaceSDK !== undefined
}

// WeSpaceSDK 存储（简化版，直接调用）
async function weSpaceSetItem(key, value) {
  try {
    const encodedValue = value !== null 
      ? unescape(encodeURIComponent(JSON.stringify(value)))
      : unescape(encodeURIComponent(JSON.stringify(null)))
    
    await window.WeSpaceSDK.setStorage(key, encodedValue)
  } catch (e) {
    localStorage.setItem(key, JSON.stringify(value))
  }
}

// WeSpaceSDK 读取（简化版，直接调用）
async function weSpaceGetItem(key) {
  try {
    const data = await window.WeSpaceSDK.getStorage(key)
    return data ? JSON.parse(decodeURIComponent(escape(data))) : null
  } catch (e) {
    const localValue = localStorage.getItem(key)
    return localValue ? JSON.parse(localValue) : null
  }
}

// WeSpaceSDK 删除
async function weSpaceRemoveItem(key) {
  try {
    await window.WeSpaceSDK.setStorage(key, unescape(encodeURIComponent(JSON.stringify(null))))
  } catch (e) {
    localStorage.removeItem(key)
  }
}

// 保存缓存数据
export async function saveAgentCache(data, mode = 'general') {
  if (typeof window === 'undefined') return
  
  try {
    const key = await getStorageKey(mode)
    if (isWeSpaceEnv()) {
      await weSpaceSetItem(key, data)
    } else {
      localStorage.setItem(key, JSON.stringify(data))
    }
  } catch (e) {
  }
}

// 读取缓存数据
export async function loadAgentCache(mode = 'general') {
  if (typeof window === 'undefined') return null
  
  try {
    const key = await getStorageKey(mode)
    let data = null
    if (isWeSpaceEnv()) {
      data = await weSpaceGetItem(key)
    } else {
      const localValue = localStorage.getItem(key)
      data = localValue ? JSON.parse(localValue) : null
    }
    return data || {
      selectedAgent: null,
      usedAgents: [],
      lastUpdateTime: null
    }
  } catch (e) {
    console.error('Failed to load agent cache:', e)
    return {
      selectedAgent: null,
      usedAgents: [],
      lastUpdateTime: null
    }
  }
}

// 保存已使用列表到缓存
export async function saveUsedAgents(usedAgents, mode = 'general') {
  const cache = await loadAgentCache(mode)
  cache.usedAgents = usedAgents
  cache.lastUpdateTime = Date.now()
  await saveAgentCache(cache, mode)
  return usedAgents
}

// 获取已使用列表
export async function getUsedAgents(mode = 'general') {
  const cache = await loadAgentCache(mode)
  return cache.usedAgents || []
}

// 保存选中的智能体到缓存
export async function saveSelectedAgent(agent, mode = 'general') {
  const cache = await loadAgentCache(mode)
  cache.selectedAgent = agent
  cache.lastUpdateTime = Date.now()
  await saveAgentCache(cache, mode)
  return agent
}

// 从缓存获取选中的智能体
export async function getSelectedAgent(mode = 'general') {
  const cache = await loadAgentCache(mode)
  return cache.selectedAgent || null
}

// 清空指定模式的缓存
export async function clearAgentCache(mode = 'general') {
  if (typeof window === 'undefined') return
  
  try {
    const key = await getStorageKey(mode)
    if (isWeSpaceEnv()) {
      await weSpaceRemoveItem(key)
    } else {
      localStorage.removeItem(key)
    }
    console.log(`Agent cache cleared for mode: ${mode}`)
  } catch (e) {
    console.error('Failed to clear agent cache:', e)
  }
}

// 切换到会话模式时调用：清除综合模式的智能体缓存
export async function clearGeneralModeCache() {
  await clearAgentCache('general')
  console.log('Cleared general mode agent cache when entering session mode')
}

// 切换到综合模式时调用：可以选择是否清除会话模式缓存
export async function clearSessionModeCache() {
  await clearAgentCache('session')
  console.log('Cleared session mode agent cache')
}

// 预留：后端存储接口（未来扩展）
export async function syncToServer() {
  console.log('Sync to server reserved for future implementation')
}