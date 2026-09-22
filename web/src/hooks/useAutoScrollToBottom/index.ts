import { ref, onMounted, onUnmounted, nextTick } from 'vue';

/**
 * 自动滚动到底部的 hooks
 * @param containerSelector 滚动容器的选择器
 * @param onReachBottom 滚动到底部时的回调函数（可选）
 * @returns 滚动相关的方法
 */
export function useAutoScrollToBottom(
  containerSelector: string,
  onReachBottom?: () => void
) {
  // 用户滚动状态
  const userScrolled = ref(false);
  // 是否已经触发过滚动到底部回调
  const hasReachedBottom = ref(false);
  // 上一次滚动位置
  let lastScrollTop = 0;
  // 是否正在自动滚动
  let isAutoScrolling = false;
  // MutationObserver 实例
  let observer: MutationObserver | null = null;

  // 滚动到底部
  async function scrollToBottom() {
    await nextTick();
    const chatContainer = document.querySelector(containerSelector);
    if (chatContainer && !userScrolled.value) {
      isAutoScrolling = true;
      chatContainer.scrollTop = chatContainer.scrollHeight;

      // 立即重置标志，因为滚动事件是同步的
      setTimeout(() => {
        isAutoScrolling = false;
      }, 1000);
    }
  }

  // 处理滚动事件
  function handleScroll() {
    const chatContainer = document.querySelector(containerSelector);
    if (chatContainer) {
      // 如果是自动滚动，直接返回，不处理任何逻辑
      if (isAutoScrolling) {
        return;
      }

      const currentScrollTop = chatContainer.scrollTop;
      const scrollDelta = Math.abs(currentScrollTop - lastScrollTop);
      lastScrollTop = currentScrollTop;

      // 计算滚动到底部的距离
      const distanceToBottom = chatContainer.scrollHeight - currentScrollTop - chatContainer.clientHeight;

      // 如果滚动变化很小（小于2px），可能是自动滚动或微小调整，不视为用户滚动
      const isUserScrolling = scrollDelta > 2;

      // 只要用户滚动离开底部（即使距离很小），就标记为用户已滚动
      // 只有当用户滚动到完全底部时，才重置标记
      if (distanceToBottom > 5) { // 5px 的阈值，确保用户有明显的滚动意图
        userScrolled.value = true;
        // 当用户离开底部时，重置hasReachedBottom标记，允许再次触发
        hasReachedBottom.value = false;
      } else {
        // 如果用户滚动到完全底部，则重置标记
        userScrolled.value = false;

        // 只有当用户主动滚动到底部且尚未触发过回调时，才触发回调
        // 避免内容变化后自动滚动到底部时触发
        if (onReachBottom && !hasReachedBottom.value && isUserScrolling) {
          console.log('触发滚动到底部回调');
          onReachBottom();
          hasReachedBottom.value = true; // 标记已触发，防止重复触发
        }
      }
    }
  }

  // 处理窗口/容器尺寸变化（resize）
  // 原理：放大窗口时 clientHeight 增大，但 scrollTop 不变，导致视觉上滚动条上移
  // 修复：如果原来在底部（非用户主动上滑），resize 后重新滚到底部
  // 复用 scrollToBottom 的 isAutoScrolling 保护，避免与 MutationObserver 触发的 scrollToBottom 竞态
  function handleResize() {
    if (!userScrolled.value) {
      scrollToBottom();
    }
    // 如果用户已主动上滑（userScrolled=true），保持原 scrollTop 不变，不动它
  }

  // 生命周期钩子
  onMounted(() => {
    const chatContainer = document.querySelector(containerSelector);
    if (chatContainer) {
      // 初始化滚动位置
      lastScrollTop = chatContainer.scrollTop;

      // 添加滚动事件监听器，检测用户是否主动滚动
      chatContainer.addEventListener('scroll', handleScroll);

      // 监听窗口尺寸变化，保持滚动位置稳定
      // resize 事件在浏览器布局完成后触发，此时 scrollHeight/clientHeight 已是最新值
      // 同步调用确保在 paint 前修正 scrollTop，避免视觉跳动
      window.addEventListener('resize', handleResize);

      // 使用 MutationObserver 监听子元素变化，自动触发滚动
      observer = new MutationObserver(() => {
        scrollToBottom();
      });

      // 配置观察选项
      const observerOptions: MutationObserverInit = {
        childList: true, // 观察子节点的变化
        subtree: true, // 观察所有后代节点
        characterData: true, // 观察文本内容的变化
        characterDataOldValue: false // 不需要旧值
      };

      // 开始观察
      observer.observe(chatContainer, observerOptions);
    }
  });

  onUnmounted(() => {
    const chatContainer = document.querySelector(containerSelector);
    if (chatContainer) {
      // 移除滚动事件监听器
      chatContainer.removeEventListener('scroll', handleScroll);
    }

    // 移除 resize 监听
    window.removeEventListener('resize', handleResize);

    // 停止观察
    if (observer) {
      observer.disconnect();
      observer = null;
    }
  });

  return {
    // 手动滚动到底部（强制滚动，忽略 userScrolled）
    forceScrollToBottom: async () => {
      await nextTick();
      const chatContainer = document.querySelector(containerSelector);
      if (chatContainer) {
        chatContainer.scrollTop = chatContainer.scrollHeight;
      }
    },
    // 重置滚动到底部状态，允许再次触发回调
    resetReachBottom: () => {
      hasReachedBottom.value = false;
    }
  };
}
