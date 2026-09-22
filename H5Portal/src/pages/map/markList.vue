<template>
  <div class="mark-list">
    <img
      src="@/assets/images/map/close_layer.png"
      alt=""
      class="list-close clickable"
      @click="closeList"
    />
    <div class="mark-list__search">
      <i class="iconfont icon-search" />
      <input
        v-model.trim="keyword"
        class="mark-list__input"
        placeholder="请输入搜索内容"
        type="text"
        @input="handleSearchInput"
      />
    </div>
    <div ref="scrollContainer" class="mark-list__scroll" @scroll="handleScroll">
      <!--  新增：列表Loading状态 -->
      <div v-if="filteredList.length === 0 && !searchKeyword" class="mark-list__loading">
        <van-loading size="20px" />
      </div>
      
      <template v-else-if="filteredList.length > 0">
        <div
          v-if="enableVirtualScroll"
          class="mark-list__virtual-wrapper"
          :style="{
            height: typeof totalHeight === 'string' ? totalHeight : totalHeight + 'px',
          }"
        >
          <div
            class="mark-list__virtual-content"
            :style="{ transform: `translateY(${offsetY}px)` }"
          >
            <div v-for="item in visibleItems" :key="item.id || item.__id" class="mark-list__item">
              <div class="mark-list__item-left">
                <div class="mark-list__item-icon">
                  <img :src="getItemAvatar(item)" alt="" />
                  <span
                    class="mark-list__item-status"
                    :class="{ online: item.status === 'online' }"
                  />
                </div>
              </div>
              <div class="mark-list__item-right">
                <div class="mark-list__item-title">
                  {{ item.name || item.alias || item.id || '未知点位' }}
                </div>
                <button class="mark-list__item-btn" @click="handleView(item)">查看</button>
              </div>
            </div>
          </div>
        </div>
        <template v-else>
          <div v-for="item in visibleItems" :key="item.id || item.__id" class="mark-list__item">
            <div class="mark-list__item-left">
              <div class="mark-list__item-icon">
                <img :src="getItemAvatar(item)" alt="" />
                <span
                  class="mark-list__item-status"
                  :class="{ online: item.status === 'online' }"
                />
              </div>
            </div>
            <div class="mark-list__item-right">
              <div class="mark-list__item-title">
                {{ item.name || item.alias || item.id || '未知点位' }}
              </div>
              <button class="mark-list__item-btn" @click="handleView(item)">查看</button>
            </div>
          </div>
        </template>
      </template>
      
      <!--  空状态文案 -->
      <div v-else class="mark-list__empty">
        <div class="empty-text">没有找到匹配的搜索内容</div>
        <div class="empty-tip">请尝试其他搜索内容</div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { computed, ref, shallowRef, watch, onMounted, onUnmounted, nextTick, triggerRef } from 'vue';
  import type { PropType } from 'vue';

  import person_header from '@/assets/images/map/person_header.png';
  import { useUserCacheStore } from '@/stores/userCache.js';

  const props = defineProps({
    list: {
      type: Array as PropType<Record<string, any>[]>,
      default: () => [],
    },
    layers: {
      type: Array as PropType<Record<string, any>[]>,
      default: () => [],
    },
  });

  const emit = defineEmits<{
    (e: 'view', item: Record<string, any>): void;
    (e: 'close'): void;
  }>();
  
  /**
   * 搜索防抖延迟时间
   */
  const SEARCH_DEBOUNCE_DELAY = 500;
  
  /**
   * 虚拟滚动阈值
   */
  const VIRTUAL_SCROLL_THRESHOLD = 10;
  
  // ==================== 状态管理 ====================
  
  const keyword = ref('');
  const searchKeyword = ref(''); // 防抖后的搜索关键词
  const userCacheStore = useUserCacheStore();

  // 建立头像缓存，避免重复请求
  const avatarCache = new Map<string, string>();

  /**
   * 获取点位头像
   * @param {Object} item - 点位数据
   * @returns {string} - 头像URL
   */
  function getItemAvatar(item) {
    // 人员类型：优先使用用户缓存的头像
    if (item.type === 'person' && item.id) {
      const cached = avatarCache.get(item.id);
      if (cached) return cached;
      const storeAvatar = userCacheStore.getAvatar(item.id);
      if (storeAvatar) {
        avatarCache.set(item.id, storeAvatar);
        return storeAvatar;
      }
      return person_header;
    }
    // 其他类型：通过 layerId 反查图层配置的 header 图标
    const layer = props.layers?.find((l) => l.key === item.layerId);
    if (layer?.headerImage) return layer.headerImage;
    return person_header;
  }

  // ==================== 虚拟滚动相关 ====================
  
  const scrollContainer = ref<HTMLElement | null>(null);
  const itemHeight = ref(60); // 每个列表项的高度（动态计算）
  const containerHeight = ref(200); // 容器高度（动态获取）
  const scrollTop = ref(0);

  // 计算可见项数量
  const visibleCount = computed(() => {
    return Math.ceil(containerHeight.value / itemHeight.value) + 3; // 可见项数量 + 缓冲区
  });

  // 防抖定时器
  let debounceTimer: ReturnType<typeof setTimeout> | null = null;

  /**
   * 在线点位排序
   * - 在线点位排在前面
   * @param {Array} list - 待排序列表
   * @returns {Array} - 排序后的列表
   */
  function sortOnlineFirst<T extends Record<string, any>>(list: T[]) {
    const online: T[] = [];
    const others: T[] = [];
    for (const item of list) {
      if (item?.status === 'online') online.push(item);
      else others.push(item);
    }
    return online.concat(others);
  }

  // 基础排序：先排序在线点位，再排序其他点位
  // computed 本身有缓存机制，只有 props.list 变化时才会重新计算
  const baseSortedList = computed(() => {
    return sortOnlineFirst(props.list);
  });

  // 过滤列表（使用防抖后的关键词）
  const filteredList = computed(() => {
    const value = searchKeyword.value.trim();
    if (!value) return baseSortedList.value;
    // 使用更高效的搜索方式
    const lowerValue = value.toLowerCase();
    const result = baseSortedList.value.filter((item) => {
      const text = `${item.title || ''}${item.name || ''}${item.alias || ''}`.toLowerCase();
      return text.includes(lowerValue);
    });
    return result;
  });

  // 启用虚拟滚动：当列表项超过10个时启用
  const enableVirtualScroll = computed(() => {
    return filteredList.value.length > VIRTUAL_SCROLL_THRESHOLD;
  });

  // 虚拟滚动计算
  const totalHeight = computed(() => {
    if (!enableVirtualScroll.value) {
      return 'auto';
    }
    return filteredList.value.length * itemHeight.value;
  });

  const startIndex = computed(() => {
    if (!enableVirtualScroll.value) {
      return 0;
    }
    return Math.max(0, Math.floor(scrollTop.value / itemHeight.value) - 1);
  });

  const endIndex = computed(() => {
    if (!enableVirtualScroll.value) {
      return filteredList.value.length;
    }
    return Math.min(filteredList.value.length, startIndex.value + visibleCount.value);
  });

  const visibleItems = computed(() => {
    if (!enableVirtualScroll.value) {
      return filteredList.value;
    }
    return filteredList.value.slice(startIndex.value, endIndex.value);
  });

  const offsetY = computed(() => {
    if (!enableVirtualScroll.value) {
      return 0;
    }
    return startIndex.value * itemHeight.value;
  });

  /**
   * 搜索防抖处理
   */
  function handleSearchInput() {
    if (debounceTimer) {
      clearTimeout(debounceTimer);
    }
    debounceTimer = setTimeout(() => {
      searchKeyword.value = keyword.value;
      // 搜索后重置滚动位置
      if (scrollContainer.value) {
        scrollContainer.value.scrollTop = 0;
        scrollTop.value = 0;
      }
    }, SEARCH_DEBOUNCE_DELAY);
  }

  /**
   * 滚动处理
   * @param {Event} event - 滚动事件
   */
  function handleScroll(event: Event) {
    const target = event.target as HTMLElement;
    scrollTop.value = target.scrollTop;
  }

  // 监听列表变化，清空头像缓存并重置滚动位置
  watch(
    () => props.list.length,
    () => {
      // 列表变化时清空头像缓存
      avatarCache.clear();
      nextTick(() => {
        if (scrollContainer.value) {
          scrollContainer.value.scrollTop = 0;
          scrollTop.value = 0;
        }
      });
    },
  );

  onMounted(() => {
    // 初始化时动态获取容器高度和item高度
    nextTick(() => {
      if (scrollContainer.value) {
        // 获取实际容器高度
        const rect = scrollContainer.value.getBoundingClientRect();
        if (rect.height > 0) {
          containerHeight.value = rect.height;
        }

        // 尝试获取第一个item的实际高度
        const firstItem = scrollContainer.value.querySelector('.mark-list__item') as HTMLElement;
        if (firstItem) {
          const itemRect = firstItem.getBoundingClientRect();
          if (itemRect.height > 0) {
            itemHeight.value = itemRect.height;
          }
        }
      }
    });
  });

  onUnmounted(() => {
    if (debounceTimer) {
      clearTimeout(debounceTimer);
    }
    avatarCache.clear();
  });

  /**
   * 查看点位详情
   * @param {Object} item - 点位数据
   */
  function handleView(item: Record<string, any>) {
    emit('view', item);
  }
  
  /**
   * 关闭列表
   */
  function closeList() {
    emit('close');
  }
</script>

<style lang="scss" scoped>
  .mark-list {
    width: 100%;
    position: absolute;
    padding: 24px 0 16px;
    border-radius: 16px 16px 0 0;
    background: rgba(255, 255, 255, 1);
    bottom: 0;
    width: 100%;
    background: #fff;
    display: flex;
    flex-direction: column;
    gap: 12px;
    z-index: 3;
    .list-close {
      width: 16px;
      height: 16px;
      position: absolute;
      right: 10px;
      top: 8px;
    }

    &__search {
      position: relative;
      height: 40px;
      border-radius: 10px;
      background: rgba(245, 245, 245, 1);
      display: flex;
      align-items: center;
      padding: 0 16px;
      color: #b5b8c0;
      margin: 0 16px;

      .iconfont {
        font-size: 18px;
        margin-right: 8px;
      }
    }

    &__input {
      flex: 1;
      border: none;
      background: transparent;
      font-size: 14px;
      color: #03081a;
      outline: none;

      &::placeholder {
        color: #b5b8c0;
      }
    }

    &__scroll {
      max-height: 200px;
      overflow-y: auto;
      padding-right: 4px;
      padding: 0 16px;
      position: relative;
    }

    //  新增：Loading样式
    &__loading {
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 40px 0;
    }

    &__virtual-wrapper {
      position: relative;
      width: 100%;
    }

    &__virtual-content {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      width: 100%;
    }

    &__item {
      display: flex;
      align-items: center;
      transition: background 0.2s;
      box-sizing: border-box;
      min-height: 60px;

      &:hover {
        background: rgba(74, 141, 255, 0.08);
      }
    }

    &__item-left {
      margin-right: 8px;
      padding: 10px 0;
    }

    &__item-right {
      padding: 10px 0;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
      flex: 1;
      min-width: 0;
      border-bottom: 1px solid #efefef;
    }

    &__item-icon {
      position: relative;
      width: 32px;
      height: 32px;
      border-radius: 16px;
      background: rgba(74, 141, 255, 0.08);
      display: flex;
      align-items: center;
      justify-content: center;

      img {
        width: 32px;
        height: 32px;
        object-fit: contain;
      }
    }

    &__item-status {
      position: absolute;
      bottom: 2px;
      right: 2px;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      border: 1px solid #fff;
      background: #a6a9ab;

      &.online {
        background: #43cf7c;
      }
    }

    &__item-title {
      font-size: 14px;
      color: #03081a;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    &__item-btn {
      border: none;
      background: #4a8dff;
      color: #fff;
      border-radius: 999px;
      padding: 0 16px;
      font-size: 12px;
      cursor: pointer;
      flex-shrink: 0;
      height: 24px;

      &:hover {
        background: #3575ea;
      }
    }

    //  优化：空状态样式
    &__empty {
      text-align: center;
      padding: 40px 0;
      
      .empty-text {
        font-size: 14px;
        color: #03081a;
        margin-bottom: 8px;
      }
      
      .empty-tip {
        font-size: 12px;
        color: #8a8f9c;
      }
    }
  }
</style>
