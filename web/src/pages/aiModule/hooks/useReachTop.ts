import { ref, onMounted, onUnmounted } from 'vue';

/** reach-top 回调接收的参数，包含触发时的滚动状态，便于加载后恢复滚动位置 */
export interface ReachTopPayload {
  prevScrollHeight: number;
  prevScrollTop: number;
}

/**
 * 检测滚动容器"上滑到顶部"的 hooks
 * 用于触发"加载更多更早的历史记录"
 *
 * 与 useAutoScrollToBottom 不同，本 hooks 仅关注 scrollTop 是否接近 0，
 * 不监听内容变化也不强制滚动，避免与自动滚动到底部的逻辑冲突。
 *
 * @param containerSelector 滚动容器的选择器
 * @param onReachTop 滚动到顶部时的回调函数
 * @returns 相关状态与方法
 */
export function useReachTop(
  containerSelector: string,
  onReachTop?: (payload: ReachTopPayload) => void | Promise<void>,
) {
  // 是否已经触发过"到顶部"回调，防止重复触发
  const hasReachedTop = ref(false);
  // 是否正在加载（外部可设置，加载期间阻止再次触发）
  const loading = ref(false);
  // 距离顶部的阈值（px）
  const threshold = 5;
  // 上一次滚动位置，用于判断滚动方向
  let lastScrollTop = 0;

  function handleScroll() {
    const chatContainer = document.querySelector(containerSelector) as HTMLElement | null;
    if (!chatContainer) return;

    const currentScrollTop = chatContainer.scrollTop;

    // 仅当向上滚动到接近顶部时触发
    if (currentScrollTop <= threshold && currentScrollTop <= lastScrollTop) {
      if (!hasReachedTop.value && !loading.value && onReachTop) {
        hasReachedTop.value = true;
        // 保留当前滚动高度，供外部"保持滚动位置"使用
        const prevScrollHeight = chatContainer.scrollHeight;
        const prevScrollTop = currentScrollTop;
        Promise.resolve(onReachTop({ prevScrollHeight, prevScrollTop })).finally(() => {
          // 由外部在加载完成后再调用 resetReachTop 重新允许触发
        });
      }
    } else if (currentScrollTop > threshold) {
      // 离开顶部后重置标记，允许下次再触发
      hasReachedTop.value = false;
    }

    lastScrollTop = currentScrollTop;
  }

  onMounted(() => {
    const chatContainer = document.querySelector(containerSelector) as HTMLElement | null;
    if (chatContainer) {
      lastScrollTop = chatContainer.scrollTop;
      chatContainer.addEventListener('scroll', handleScroll);
    }
  });

  onUnmounted(() => {
    const chatContainer = document.querySelector(containerSelector) as HTMLElement | null;
    if (chatContainer?.removeEventListener) {
      chatContainer.removeEventListener('scroll', handleScroll);
    }
  });

  return {
    /** 加载完成（或失败）后调用，重置标记，允许下次再触发 */
    resetReachTop: () => {
      hasReachedTop.value = false;
      loading.value = false;
    },
    /** 外部标记正在加载中，期间不再触发 */
    setLoading: (v: boolean) => {
      loading.value = v;
    },
  };
}
