/**
 * AI助手公共工具函数
 * 统一管理会话模式、综合模式的公共逻辑
 */

/**
 * 生成会话ID（uuid）
 * @returns {string} 会话ID
 */
export function createSessionId() {
  if (typeof crypto !== "undefined" && crypto?.randomUUID) {
    return crypto.randomUUID();
  }
  const ts = Date.now().toString(16);
  const rand = Math.random().toString(16).substring(2, 10);
  return `${ts}-${rand}`;
}

/**
 * 查找 DeepSeek 智能体的 index，作为兜底默认值
 * @param {Array} agents - 智能体列表
 * @returns {number|null} DeepSeek智能体的index，找不到返回null
 */
export function getDeepSeekAgentIndex(agents = []) {
  const targetItem = agents.find(
    (item) => item.name?.toLowerCase() === "deepseek"
  );
  return targetItem?.index ?? null;
}

/**
 * 获取会话模式下的选中智能体存储key
 * @param {string|number} userId - 用户ID
 * @returns {string|null} 存储key，userId为空时返回null
 */
export function getSessionSelectedAgentKey(userId) {
  if (!userId) return null;
  return `sessionSelectedAgentIndex_${userId}`;
}

/**
 * 获取摘要存储key
 * @param {string|number} userId - 用户ID
 * @param {string} modeName - 模式名称 ('session' | 'normal')
 * @returns {string} 存储key
 */
export function getSummaryKey(userId, modeName) {
  return `chatSummary_${userId}_${modeName}`;
}

/**
 * 获取聊天历史存储key
 * @param {string|number} userId - 用户ID
 * @param {boolean} isSessionMode - 是否会话模式
 * @param {string|null} sessionId - 会话ID（会话模式必需）
 * @param {number|null} agentIndex - 智能体index
 * @param {Array} agents - 智能体列表（用于获取DeepSeek默认值）
 * @returns {string|null} 存储key，会话模式下未选择会话时返回null
 */
export function getChatKey(userId, isSessionMode, sessionId, agentIndex, agents = []) {
  const keyBase = `chatHistory_${userId}`;
  if (!isSessionMode) {
    // 综合模式：保持原键名
    return keyBase;
  }

  // 会话模式：按会话ID + 智能体index分桶
  if (!sessionId) return null;
  const deepseekIndex = getDeepSeekAgentIndex(agents);
  const agentKey = agentIndex ?? deepseekIndex ?? "deepseek";
  return `${keyBase}_${sessionId}_${agentKey}`;
}

/**
 * 去除HTML标签，提取纯文本
 * @param {string} val - 可能包含HTML的字符串
 * @returns {string} 纯文本
 */
export function stripHtml(val) {
  if (!val || typeof val !== "string") return "";
  return val.replace(/<[^>]*>/g, "").trim();
}

export function linkxLog(filePath, funcName, ...args) {
  const timestamp = new Date().toLocaleString();
  const content = args.join('-');
  console.log(`[LINKX]-[${timestamp}]-[${filePath}]-[${funcName}]-${content}`);
}

// 格式化时间为 yyyy-MM-dd HH:mm:ss
export function formatMsgTime(val) {
  if (val === null || val === undefined || val === '') return '';
  // 已是目标格式直接返回，避免二次解析
  if (typeof val === 'string' && /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(val)) return val;
  const date = new Date(val);
  if (isNaN(date.getTime())) return String(val);
  const pad = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

