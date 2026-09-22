<script setup lang="ts">
  import { onMounted, onBeforeUnmount, ref, computed, watch } from 'vue';

  import { ArrowLeft, ArrowRight, Loading } from '@element-plus/icons-vue';
  import { ElMessage } from 'element-plus';

  import calendarIcon from '@/assets/svg/calendar.svg';
  import CalendarView from './components/CalendarView.vue';
  import listViewIcon from '@/assets/svg/list_view.svg';
  import { themeService } from '../../data/useTheme';
  import ListView from './components/ListView.vue';
  import OrgSwitchButton from './components/OrgSwitchButton.vue';
  import SearchForm from './components/SearchForm.vue';
  import { useCalendar } from './composables/useCalendar';
  import { useDutySchedule } from './composables/useDutySchedule';

  import type { OrgFilterMode, OtherSubMode } from './types';

  // 定义 emit
  const emit = defineEmits<{
    (e: 'back'): void;
  }>();

  // ─────────────────────────────────────────────────────────────
  // 状态管理
  // ─────────────────────────────────────────────────────────────

  // 视图模式：calendar | list
  const viewMode = ref<'calendar' | 'list'>('calendar');

  // 使用hooks
  const {
    monthStr,
    monthText,
    days,
    weekDayNames,
    prevMonth,
    nextMonth,
    updateDuties,
  } = useCalendar();

  const {
    searchParams,
    listData,
    total,
    pagination,
    calendarLoading,
    listLoading,
    deleteLoading,
    fetchCalendarData,
    fetchListData,
    deleteBatch,
    resetSearchParams,
    cancelAllRequests,
    // 组织筛选
    organizations,
    orgFilterMode,
    selectedOrgId,
    otherSubMode,
    hasMultipleOrgs,
    // initLoading,
    init,
    changeOrgFilter,
    // 数据权限
    // imOrgPrivsArr,
    // userRoleAuth,
    setupUserWatch,
    // 用户就绪状态
    userReady,
  } = useDutySchedule();

  const theme = computed(() => {
    return themeService.getCurrentTheme();
  });

  // 组件refs
  const searchFormRef = ref();
  const listViewRef = ref();

  // 导入loading
  const importLoading = ref(false);

  // ─────────────────────────────────────────────────────────────
  // 数据加载
  // ─────────────────────────────────────────────────────────────

  /**
   * 加载日历数据
   */
  async function loadCalendarData() {
    console.log('[日历] 开始加载，月份:', monthStr.value);
    const data = await fetchCalendarData(monthStr.value);
    console.log('[日历] 获取到的数据:', data);
    console.log('[日历] 数据条数:', Object.keys(data).length);
    updateDuties(data);
  }

  /**
   * 加载列表数据
   */
  async function loadListData() {
    await fetchListData();
  }

  /**
   * 刷新数据
   */
  function refresh() {
    if (viewMode.value === 'calendar') {
      loadCalendarData();
    } else {
      loadListData();
    }
  }

  // ─────────────────────────────────────────────────────────────
  // 事件处理
  // ─────────────────────────────────────────────────────────────

  /**
   * 搜索
   */
  function handleSearch() {
    const params = searchFormRef.value?.getSearchParams();
    if (params) {
      Object.assign(searchParams, params);
    }
    refresh();
  }

  /**
   * 重置
   */
  function handleReset() {
    resetSearchParams();
    searchFormRef.value?.getSearchParams && Object.assign(searchParams, searchFormRef.value.getSearchParams());
    refresh();
  }

  /**
   * 视图切换
   */
  function handleViewModeChange(mode: 'calendar' | 'list') {
    viewMode.value = mode;
    refresh();
  }

  /**
   * 返回常用应用列表
   */
  function handleBack() {
    emit('back');
  }

  /**
   * 组织筛选变化
   */
  function handleOrgFilterChange(mode: OrgFilterMode, orgId?: string, subMode?: OtherSubMode) {
    console.log(orgId, 'mode', subMode)
    changeOrgFilter(mode, orgId, subMode);
    refresh();
  }

  /**
   * 删除处理
   */
  async function handleDelete(ids: string[]) {
    const success = await deleteBatch(ids);
    if (success) {
      ElMessage.success('删除成功');
      loadListData();
    } else {
      ElMessage.error('删除失败');
    }
  }

  /**
   * 批量删除（已注释：去掉选择列功能）
   */
  // function handleBatchDelete() {
  //   listViewRef.value?.handleDelete();
  // }

  // ─────────────────────────────────────────────────────────────
  // 生命周期
  // ─────────────────────────────────────────────────────────────

  // 监听月份变化
  watch(monthStr, () => {
    if (viewMode.value === 'calendar') {
      loadCalendarData();
    }
  });

  // 监听分页变化
  watch(
    () => pagination.pageNum,
    () => {
      if (viewMode.value === 'list') {
        loadListData();
      }
    },
  );

  watch(
    () => pagination.pageSize,
    () => {
      if (viewMode.value === 'list') {
        pagination.pageNum = 1;
        loadListData();
      }
    },
  );

  // 页面挂载时加载数据
  onMounted(async () => {
    // 设置用户信息响应式监听
    setupUserWatch();
    // 初始化用户信息和组织列表
    const success = await init();
    // 只有初始化成功才刷新数据
    if (success) {
      refresh();
    }
  });

  // 监听用户就绪状态变化，就绪后自动刷新数据
  watch(userReady, async (ready, wasReady) => {
    // 从未就绪变为就绪时，刷新数据
    if (ready && !wasReady) {
      console.log('[值班信息] 用户信息就绪，开始刷新数据');
      const success = await init();
      if (success) {
        refresh();
      }
    }
  });

  // 页面卸载时取消请求
  onBeforeUnmount(() => {
    cancelAllRequests();
  });
</script>

<template>
  <div class="duty-information-wrap">
    <!-- 加载状态：等待用户信息就绪 -->
    <div v-if="!userReady" class="loading-container">
      <ElIcon class="loading-icon" :size="32">
        <Loading />
      </ElIcon>
      <span class="loading-text">正在加载...</span>
    </div>

    <!-- 正常内容：用户信息已就绪 -->
    <div v-else :class="theme === 'light' ?'duty-information-inner' : 'duty-information-inner duty-information-inner-dark'">
      <div class="duty-information-inner-box">
        <!-- 第一排：搜索表单 -->
        <div class="filter-section">
          <SearchForm
            ref="searchFormRef"
            :import-loading="importLoading"
            :view-mode="viewMode"
            @search="handleSearch"
            @reset="handleReset"
            @back="handleBack"
          />
        </div>
  
        <!-- 第二排：控制栏 -->
        <div class="control-bar">
          <!-- 左侧：月份切换（仅在日历视图显示） -->
          <div class="control-left">
            <template v-if="viewMode === 'calendar'">
              <ElIcon
                class="arrow-icon"
                :class="{ disabled: calendarLoading }"
                @click="prevMonth"
              >
                <ArrowLeft />
              </ElIcon>
              <span class="month-text">{{ monthText }}</span>
              <ElIcon
                class="arrow-icon"
                :class="{ disabled: calendarLoading }"
                @click="nextMonth"
              >
                <ArrowRight />
              </ElIcon>
            </template>
          </div>
  
          <!-- 右侧：视图切换 -->
          <div class="control-right">
            <OrgSwitchButton
              :mode="orgFilterMode"
              :selected-org-id="selectedOrgId"
              :organizations="organizations"
              :has-multiple-orgs="hasMultipleOrgs"
              :other-sub-mode="otherSubMode"
              @change="handleOrgFilterChange"
            />
            <ElButtonGroup class="ml-17px">
              <ElButton
                :type="viewMode === 'calendar' ? 'primary' : ''"
                size="small"
                @click="handleViewModeChange('calendar')"
              >
                <img class="view-icon" :src="calendarIcon" alt="" />
                <div>日历查看</div>
              </ElButton>
              <ElButton
                :type="viewMode === 'list' ? 'primary' : ''"
                size="small"
                @click="handleViewModeChange('list')"
              >
                <img class="view-icon" :src="listViewIcon" alt="" />
                <div>列表</div>
              </ElButton>
            </ElButtonGroup>
          </div>
        </div>
  
        <!-- 第三部分：内容区域 -->
        <div class="content-section">
          <!-- 日历视图 -->
          <CalendarView
            v-if="viewMode === 'calendar'"
            :days="days"
            :week-day-names="weekDayNames"
            :loading="calendarLoading"
            :duty-type-filter="searchParams.dutyType"
            @prev="prevMonth"
            @next="nextMonth"
          />
  
          <!-- 列表视图 -->
          <ListView
            v-else
            ref="listViewRef"
            :data="listData"
            :total="total"
            :loading="listLoading"
            :delete-loading="deleteLoading"
            :page-num="pagination.pageNum"
            :page-size="pagination.pageSize"
            @update:page-num="pagination.pageNum = $event"
            @update:page-size="pagination.pageSize = $event"
            @delete="handleDelete"
          />
        </div>

      </div>
    </div>
  </div>
</template>

<style scoped lang="less">
  .duty-information-wrap {
    box-sizing: border-box;
    width: 100%;
    height: 100%;
    overflow-x: hidden;
    background: var(--background-color);
    // box-shadow: inset 0 6px 10px -6px var(--box-shadow-color);
  }

  // 加载状态样式
  .loading-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    background: var(--background-color);

    .loading-icon {
      color: var(--color-primary);
      animation: spin 1s linear infinite;
    }

    .loading-text {
      margin-top: 12px;
      font-size: 14px;
      color: var(--text-color);
    }
  }

  @keyframes spin {
    from {
      transform: rotate(0deg);
    }
    to {
      transform: rotate(360deg);
    }
  }

  .duty-information-inner {
    width: 100%;
    height: 100%;
    // padding: 20px;
    display: flex;
    flex-direction: column;
    // background: #f6f8ff;
    // gap: 16px;
    .duty-information-inner-box {
      flex-grow: 1;
      display: flex;
      flex-direction: column;
      background: var(--background-white-color);
      box-shadow: inset 0 6px 10px -6px var(--box-shadow-color);
      border-radius: 12px;
    }
  }
  .duty-information-inner-dark {
    background: var(--background-color);
  }

  .filter-section {
    // background: var(--background-white-color);
    // border: 1px solid var(--border-color);
    border-radius: 4px;
  }

  .control-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 24px 24px 20px;
    // background: var(--background-white-color);
    // border: 1px solid var(--border-color);
    border-radius: 4px;

    .control-left {
      display: flex;
      align-items: center;
      gap: 12px;

      .arrow-icon {
        font-size: 20px;
        cursor: pointer;
        color: var(--text-color);

        &:hover {
          color: var(--color-primary);
        }

        &.disabled {
          cursor: not-allowed;
          opacity: 0.5;
        }
      }

      .month-text {
        font-size: 16px;
        font-weight: 600;
        color: var(--text-color);
        min-width: 100px;
        text-align: center;
        user-select: none;
      }
    }

    .control-right {
      display: flex;
      align-items: center;

      // 默认按钮样式 - 参考刷新按钮样式，支持 Dark 主题
      :deep(.el-button:not(.el-button--primary)) {
        background: var(--button-bg);
        border: 1px solid var(--border-color);
        color: var(--text-color);

        &:hover,
        &:focus {
          background: var(--button-hover-bg);
          border-color: var(--button-active-color);
          color: var(--tabs-active-color);
        }
      }

      // 视图切换按钮组样式
      :deep(.el-button-group) {
        .el-button {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          height: 32px;
          font-size: 14px;
          font-weight: 400;
          line-height: 22px;
          padding: 0 12px;
          span{
            display: inline-flex;
            align-items: center;
            justify-content: center;
          }

          // active 状态（primary）：主题色背景，白色文字
          &.el-button--primary {
            background: var(--button-active-color) !important;
            border-color: var(--button-active-color) !important;
            color: #fff !important;

            span {
              color: #fff !important;
            }

            .view-icon {
              filter: brightness(0) invert(1);
            }
          }
        }
      }
    }
  }

  .view-icon {
    width: 16px;
    height: 16px;
    margin-right: 4px;
    vertical-align: middle;
  }

  .content-section {
    flex: 1;
    // background: var(--background-white-color);
    // border: 1px solid var(--border-color);
    border-radius: 4px;
    padding: 0 24px 16px;
    overflow: hidden;
  }

  .ml-10px {
    margin-left: 10px;
  }
</style>