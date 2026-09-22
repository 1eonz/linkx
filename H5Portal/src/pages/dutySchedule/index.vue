<template>
  <div class="duty-schedule-page">
    <!-- 顶部导航栏 -->
    <TopNavBar
      ref="topNavBarRef"
      :bg-color="'#f5f5f5'"
      :title-color="'#333'"
      :title="'值班信息'"
    >
      <template #left>
        <van-icon name="arrow-left" color="#333" @click="handleGoBack" />
      </template>
      <template #right>
        <!-- 初始化加载中 -->
        <van-loading v-if="initLoading" size="20" />
        
        <!-- 组织切换按钮 -->
        <OrgSwitchButton
          v-else-if="userInfo"
          :mode="orgFilterMode"
          :selected-org-id="selectedOrgId"
          :organizations="organizations"
          :has-multiple-orgs="hasMultipleOrgs"
          :other-sub-mode="otherSubMode"
          @update:mode="handleOrgModeChange"
          @update:selected-org-id="handleOrgIdChange"
          @change="handleOrgFilterChange"
        />
      </template>
    </TopNavBar>
    
    <!-- 内容区域 -->
    <div 
      ref="pageContentRef"
      class="page-content" 
      :style="{ paddingTop: (statusBarHeight + 44) + 'px' }"
      @touchstart.passive="handleContentTouchStart"
      @touchmove="handleContentTouchMove"
      @touchend.passive="handleContentTouchEnd"
    >
      <!-- 初始化失败提示 -->
      <div v-if="initError" class="init-error">
        <van-empty description="加载失败">
          <van-button type="primary" size="small" @click="handleRetry">
            重新加载
          </van-button>
        </van-empty>
      </div>
      
      <template v-else>
        <!-- 搜索栏 -->
        <div class="search-section">
          <SearchBar 
            v-model="searchUserName"
            :loading="loading"
            @search="handleSearch"
            @clear="handleClearSearch"
          />
        </div>
        
        <!-- 日历组件 (sticky) -->
        <div 
          class="calendar-sticky" 
          :class="{ collapsed: isCalendarCollapsed }"
          :style="{ top: (statusBarHeight + 44) + 'px' }"
        >
          <Calendar
            :current-month="currentMonth"
            :selected-date="selectedDate"
            :loading="loading"
            :has-schedule-on-date="hasScheduleOnDate"
            :is-collapsed="isCalendarCollapsed"
            @prev-month="handlePrevMonth"
            @next-month="handleNextMonth"
            @select-date="handleSelectDate"
            @show-month-picker="handleShowMonthPicker"
            @collapse="handleCalendarCollapse"
            @expand="handleCalendarExpand"
          />
        </div>
        
        <!-- 排班列表 -->
        <div 
          ref="scheduleSectionRef"
          class="schedule-section" 
          :class="{ 'independent-scroll': isCalendarCollapsed }"
          @scroll.passive="handleScheduleScroll"
        >
          <ScheduleList
            :selected-date="selectedDate"
            :schedules="selectedDateSchedule"
          />
        </div>
      </template>
    </div>
    
    <!-- 年月选择器 -->
    <MonthPicker
      v-model:show="showMonthPicker"
      :current-month="currentMonth"
      @confirm="handleMonthConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { showToast } from 'vant';

import { useCommunicationStore } from '@/stores/communication.js';

import TopNavBar from '@/pages/components/TopNavBar.vue';

import Calendar from './components/Calendar.vue';
import MonthPicker from './components/MonthPicker.vue';
import OrgSwitchButton from './components/OrgSwitchButton.vue';
import ScheduleList from './components/ScheduleList.vue';
import SearchBar from './components/SearchBar.vue';
import { useDutySchedule } from './composables/useDutySchedule';
import type { OrgFilterMode, OtherSubMode } from './types';

const router = useRouter();
const communicationStore = useCommunicationStore();

// ─────────────────────────────────────────────────────────────
// 使用 composable
// ─────────────────────────────────────────────────────────────

const {
  // 状态
  selectedDate,
  currentMonth,
  searchUserName,
  userInfo,
  organizations,
  orgFilterMode,
  selectedOrgId,
  otherSubMode,
  selectedDateSchedule,
  loading,
  initLoading,
  hasMultipleOrgs,
  hasScheduleOnDate,
  
  // 方法
  init,
  changeMonth,
  goToMonth,
  selectDate,
  searchByUserName,
  clearSearch,
  changeOrgFilter,
} = useDutySchedule();

// ─────────────────────────────────────────────────────────────
// 页面状态
// ─────────────────────────────────────────────────────────────

// TopNavBar 引用
const topNavBarRef = ref();

// 页面内容区域引用
const pageContentRef = ref<HTMLElement | null>(null);

// 列表区域引用
const scheduleSectionRef = ref<HTMLElement | null>(null);

// 状态栏高度
const statusBarHeight = computed(() => topNavBarRef.value?.paddingTop || 0);

// 年月选择器显示状态
const showMonthPicker = ref(false);

// 日历折叠状态
const isCalendarCollapsed = ref(false);

// 初始化错误状态
const initError = ref(false);

// ─────────────────────────────────────────────────────────────
// 触摸滑动相关
// ─────────────────────────────────────────────────────────────

// 【控制参数】是否启用页面滑动切换日历折叠/展开
const ENABLE_PAGE_SWIPE_TOGGLE = false;

let touchStartY = 0;
let touchCurrentY = 0;
let touchStartTime = 0;
const swipeThreshold = 30; // 滑动距离阈值
const swipeTimeThreshold = 300; // 快速滑动时间阈值（ms）

// 搜索防抖定时器
let searchTimer: ReturnType<typeof setTimeout> | null = null;

// ─────────────────────────────────────────────────────────────
// 事件处理
// ─────────────────────────────────────────────────────────────

// 返回上一页
const handleGoBack = () => {
  communicationStore.close();
};

// 搜索（防抖）
const handleSearch = (value: string) => {
  if (searchTimer) {
    clearTimeout(searchTimer);
  }
  searchTimer = setTimeout(() => {
    searchByUserName(value);
  }, 300);
};

// 清除搜索
const handleClearSearch = () => {
  if (searchTimer) {
    clearTimeout(searchTimer);
  }
  clearSearch();
};

// 上一月
const handlePrevMonth = (preserveDate?: Date) => {
  changeMonth(-1, preserveDate);
};

// 下一月
const handleNextMonth = (preserveDate?: Date) => {
  changeMonth(1, preserveDate);
};

// 选择日期
const handleSelectDate = (date: Date) => {
  selectDate(date);
};

// 显示年月选择器
const handleShowMonthPicker = () => {
  showMonthPicker.value = true;
};

// 年月选择确认
const handleMonthConfirm = (year: number, month: number) => {
  goToMonth(year, month);
};

// 日历折叠
const handleCalendarCollapse = () => {
  isCalendarCollapsed.value = true;
};

// 日历展开
const handleCalendarExpand = () => {
  isCalendarCollapsed.value = false;
};

// ─────────────────────────────────────────────────────────────
// 统一触摸事件处理（在 page-content 上）
// ─────────────────────────────────────────────────────────────

// 触摸开始
const handleContentTouchStart = (e: TouchEvent) => {
  if (!ENABLE_PAGE_SWIPE_TOGGLE) return;
  touchStartY = e.touches[0].clientY;
  touchCurrentY = e.touches[0].clientY;
  touchStartTime = Date.now();
};

// 触摸移动
const handleContentTouchMove = (e: TouchEvent) => {
  if (!ENABLE_PAGE_SWIPE_TOGGLE) return;
  touchCurrentY = e.touches[0].clientY;
  const deltaY = touchCurrentY - touchStartY;
  
  if (!isCalendarCollapsed.value) {
    // 日历展开状态：向上滑动收起日历
    // 不需要在这里处理，在 touchend 时判断
  } else {
    // 日历收起状态：检测列表是否在顶部 + 向下滑动
    const el = scheduleSectionRef.value;
    if (el && el.scrollTop === 0 && deltaY > swipeThreshold) {
      // 展开日历
      isCalendarCollapsed.value = false;
      // 阻止列表滚动
      e.preventDefault();
    }
  }
};

// 触摸结束
const handleContentTouchEnd = () => {
  if (!ENABLE_PAGE_SWIPE_TOGGLE) return;
  const deltaY = touchCurrentY - touchStartY;
  const deltaTime = Date.now() - touchStartTime;
  
  if (!isCalendarCollapsed.value) {
    // 日历展开状态：检测向上滑动
    // deltaY < 0 表示向上滑动
    const isSwipeUp = deltaY < -swipeThreshold || 
      (deltaTime < swipeTimeThreshold && deltaY < -15);
    
    if (isSwipeUp) {
      console.log('[页面] 检测到向上滑动，收起日历', { deltaY, deltaTime });
      isCalendarCollapsed.value = true;
    }
  }
  
  // 重置
  touchStartY = 0;
  touchCurrentY = 0;
  touchStartTime = 0;
};

// 列表滚动处理
const handleScheduleScroll = () => {
  // 列表滚动时不需要特殊处理
  // 触摸事件已在 page-content 上统一处理
};

// ─────────────────────────────────────────────────────────────
// 组织筛选事件处理
// ─────────────────────────────────────────────────────────────

// 组织筛选模式变化
const handleOrgModeChange = (mode: OrgFilterMode) => {
  // 由 changeOrgFilter 统一处理
};

// 组织ID变化
const handleOrgIdChange = (orgId: string) => {
  // 由 changeOrgFilter 统一处理
};

// 组织筛选变化（统一处理，重新加载数据）
const handleOrgFilterChange = async (mode: OrgFilterMode, orgId?: string, subMode?: OtherSubMode) => {
  console.log('[页面] 组织切换:', { mode, orgId, subMode });
  await changeOrgFilter(mode, orgId, subMode);
};

// ─────────────────────────────────────────────────────────────
// 初始化
// ─────────────────────────────────────────────────────────────

// 重试初始化
const handleRetry = async () => {
  initError.value = false;
  const success = await init();
  if (!success) {
    initError.value = true;
    showToast('加载失败，请稍后重试');
  }
};

// 页面挂载
onMounted(async () => {
  const success = await init();
  if (!success) {
    initError.value = true;
    showToast('加载失败，请稍后重试');
  }
});

// 页面卸载
onUnmounted(() => {
  // 清理搜索定时器
  if (searchTimer) {
    clearTimeout(searchTimer);
    searchTimer = null;
  }
});
</script>

<style lang="scss" scoped>
.duty-schedule-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.page-content {
  padding: 12px;
  padding-bottom: 24px;
}

.init-error {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.search-section {
  margin-bottom: 12px;
  margin-top: 12px;
}

.calendar-sticky {
  position: sticky;
  z-index: 10;
  margin-bottom: 12px;
  background: #f5f5f5;
  
  // 添加过渡动画
  transition: all 0.3s ease;
}

.schedule-section {
  // 默认样式 - 占位
  min-height: 100px;
}

// 日历收起时，列表独立滚动
.schedule-section.independent-scroll {
  // 计算剩余高度：100vh - 导航栏 - 搜索栏 - 日历(收起状态)
  max-height: calc(100vh - 44px - 60px - 120px);
  overflow-y: auto;
  overflow-x: hidden;
  -webkit-overflow-scrolling: touch;
}
</style>