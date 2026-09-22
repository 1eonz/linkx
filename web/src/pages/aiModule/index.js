const PAUSE_KEY_PREFIX = 'chat_pause_';

class AiAgentChatPausePosition {
  static savePosition(taskId, { replyPaused, replyPosition }) {
    if (!taskId) return;

    // 如果当前时手动暂停的状态，那么手动暂停的状态优先级更高 需要保存手动暂停的状态
    let isReplyPaused = this.getPosition(taskId)?.replyPaused;
    try {
      localStorage.setItem(
        `${PAUSE_KEY_PREFIX}${taskId}`,
        JSON.stringify({ replyPaused: isReplyPaused ? isReplyPaused : replyPaused, replyPosition }),
      );
    } catch (e) {
      console.error('保存暂停状态失败:', e);
    }
  }

  static clearPosition(taskId) {
    if (!taskId) return;
    try {
      localStorage.removeItem(`${PAUSE_KEY_PREFIX}${taskId}`);
    } catch (e) {
      console.error('清除暂停状态失败:', e);
    }
  }

  static getPosition(taskId) {
    if (!taskId) return null;
    try {
      const data = localStorage.getItem(`${PAUSE_KEY_PREFIX}${taskId}`);
      if (!data) return null;
      return JSON.parse(data);
    } catch (e) {
      console.error('获取暂停状态失败:', e);
      return null;
    }
  }
}

export default AiAgentChatPausePosition;
