const PAUSE_KEY_PREFIX = 'chat_pause_';
const AGENT_PAUSE_KEY_PREFIX = 'chat_pause_agent_';

class AiAgentChatPausePosition {
  // 按 taskId 存储（保留原有逻辑）
  static savePosition(taskId, { replyPaused, replyPosition, agentId }) {
    if (!taskId) return;
    const data = { replyPaused, replyPosition, agentId };
    try {
      window.localStorage.setItem(
        `${PAUSE_KEY_PREFIX}${taskId}`,
        JSON.stringify(data),
      );
      // 同时按 agentId 存储一份，便于历史列表查询
      if (agentId !== undefined && agentId !== null && agentId !== '') {
        window.localStorage.setItem(
          `${AGENT_PAUSE_KEY_PREFIX}${agentId}`,
          JSON.stringify({ ...data, taskId }),
        );
      }
    } catch (e) {
      console.error('保存暂停状态失败:', e);
    }
  }

  static clearPosition(taskId) {
    if (!taskId) return;
    try {
      // 清理前先读出 agentId，以便同步清理按 agentId 存储的那份
      const data = window.localStorage.getItem(`${PAUSE_KEY_PREFIX}${taskId}`);
      let agentId;
      if (data) {
        try { agentId = JSON.parse(data).agentId; } catch (e) { agentId = undefined; }
      }
      window.localStorage.removeItem(`${PAUSE_KEY_PREFIX}${taskId}`);
      if (agentId !== undefined && agentId !== null && agentId !== '') {
        window.localStorage.removeItem(`${AGENT_PAUSE_KEY_PREFIX}${agentId}`);
      }
    } catch (e) {
      console.error('清除暂停状态失败:', e);
    }
  }

  static getPosition(taskId) {
    if (!taskId) return null;
    try {
      const data = window.localStorage.getItem(`${PAUSE_KEY_PREFIX}${taskId}`);
      if (!data) return null;
      return JSON.parse(data);
    } catch (e) {
      console.error('获取暂停状态失败:', e);
      return null;
    }
  }

  // 根据智能体id查询是否有缓存的暂停状态（O(1) 查询，不再轮询）
  static getPositionByAgent(agentId) {
    if (!agentId) return null;
    try {
      const data = window.localStorage.getItem(`${AGENT_PAUSE_KEY_PREFIX}${agentId}`);
      if (!data) return null;
      return JSON.parse(data);
    } catch (e) {
      console.error('根据智能体获取暂停状态失败:', e);
      return null;
    }
  }
}

export default AiAgentChatPausePosition;
