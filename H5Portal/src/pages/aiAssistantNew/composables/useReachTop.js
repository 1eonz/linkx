import { ref, onMounted, onUnmounted } from 'vue'

// 触发 reach-top 回调时携带的滚动状态
function useReachTop(containerSelector, onReachTop) {
  const hasReachedTop = ref(false)
  const loading = ref(false)
  const hasMore = ref(true)
  const threshold = 5
  let lastScrollTop = 0
  let scrollHandler = null

  // 滚动到顶部且向上滚动时触发回调
  function handleScroll() {
    const chatContainer = document.querySelector(containerSelector)
    if (!chatContainer) return

    const currentScrollTop = chatContainer.scrollTop

    if (currentScrollTop <= threshold && currentScrollTop <= lastScrollTop) {
      if (!hasReachedTop.value && !loading.value && hasMore.value && typeof onReachTop === 'function') {
        hasReachedTop.value = true
        const prevScrollHeight = chatContainer.scrollHeight
        const prevScrollTop = currentScrollTop
        Promise.resolve(onReachTop({ prevScrollHeight, prevScrollTop })).catch((e) => {
          console.error('[useReachTop] 加载失败:', e)
          hasReachedTop.value = false
          loading.value = false
        })
      }
    } else if (currentScrollTop > threshold) {
      // 离开顶部后重置标记
      hasReachedTop.value = false
    }

    lastScrollTop = currentScrollTop
  }

  onMounted(() => {
    const chatContainer = document.querySelector(containerSelector)
    if (chatContainer) {
      lastScrollTop = chatContainer.scrollTop
      scrollHandler = handleScroll
      chatContainer.addEventListener('scroll', scrollHandler)
    }
  })

  onUnmounted(() => {
    const chatContainer = document.querySelector(containerSelector)
    if (chatContainer && scrollHandler) {
      chatContainer.removeEventListener('scroll', scrollHandler)
    }
    scrollHandler = null
  })

  return {
    // 重置标记，允许下次触发
    resetReachTop: () => {
      hasReachedTop.value = false
      loading.value = false
    },
    // 标记加载中，期间不再触发
    setLoading: (v) => {
      loading.value = v
    },
    // 设置是否还有更早的历史可加载
    setHasMore: (v) => {
      hasMore.value = v
    },
    hasMoreHistory: hasMore,
    reachTopLoading: loading,
  }
}

export { useReachTop }
