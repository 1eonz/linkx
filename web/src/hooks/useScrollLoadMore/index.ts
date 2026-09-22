/**
 * 滚动触底加载更多工具函数
 * 用于统一处理滚动加载更多的逻辑
 */

export interface ScrollLoadMoreOptions {
  /**
   * 获取加载状态的函数
   * @returns { loading: boolean; noMore: boolean }
   */
  getState: () => { loading: boolean; noMore: boolean };
  
  /**
   * 加载更多的回调函数
   */
  onLoadMore: () => void | Promise<void>;
  
  /**
   * 距离底部的阈值（像素），默认 10
   */
  threshold?: number;
}

/**
 * 检查是否滚动到底部并触发加载
 * @param event 滚动事件对象
 * @param options 配置选项
 */
export function checkScrollLoadMore(event: Event, options: ScrollLoadMoreOptions): void {
  const target = event.target as HTMLElement;
  if (!target) return;

  const { scrollTop, clientHeight, scrollHeight } = target;
  const { getState, onLoadMore, threshold = 10 } = options;
  const { loading, noMore } = getState();

  // 当滚动到底部且还有更多数据时加载更多
  if (!loading && !noMore && scrollTop + clientHeight >= scrollHeight - threshold) {
    onLoadMore();
  }
}

/**
 * 滚动触底加载更多 Hook
 * @param options 配置选项
 * @returns handleScroll 处理滚动事件的函数
 */
export function useScrollLoadMore(options: ScrollLoadMoreOptions) {
  const {
    getState,
    onLoadMore,
    threshold = 10,
  } = options;

  /**
   * 处理滚动事件
   * @param event 滚动事件对象
   */
  const handleScroll = async (event: Event) => {
    checkScrollLoadMore(event, {
      getState,
      onLoadMore,
      threshold,
    });
  };

  return {
    handleScroll,
  };
}