<template>
  <view class="police-ticket-content">
    <!-- 搜索框 -->
    <view class="search-container">
      <van-search
        v-model="searchText"
        ref="SearchRef"
        shape="square"
        :show-action="false"
        placeholder="请输入关键词"
        class="my-custom-search"
        :clearable="true"
        @search="handleSearch"
        @input="handleChange"
      />
    </view>

    <!-- 日期切换部分 - 警单和任务都显示 -->
    <view class="title-container">
      <view
        v-for="(item, index) in tabTitles"
        :key="index"
        class="title-item"
        :class="{ active: activeDays === item.id }"
        @click="switchTitle(item.id)"
      >
        {{ item.name }}
      </view>
    </view>

    <!-- 自定义折叠面板 -->
    <!-- <scroll-view
      class="data-container"
      v-if="orderList.length > 0"
      scroll-y="true"
      @scrolltolower="onScrollToLower"
    > -->
    <view class="data-container" v-if="orderList.length > 0">
      <van-list
        v-model:loading="listLoading"
        v-model:finished="listFinished"
        finished-text="没有更多数据了"
        :immediate-check="false"
        :offset="10"
        @load="onScrollToLower"
      >
        <van-checkbox-group :value="selectedIds" shape="square" @change="handleCheckboxChange">
          <div class="custom-collapse-item" v-for="item in orderList" :key="item.id">
            <div
              class="custom-header"
              :class="{ expanded: item.expanded }"
              @click.stop="toggleItem(item)"
            >
              <div class="header-left">
                <van-checkbox
                  v-if="!onlyRelative"
                  :name="item.id"
                  :checked="selectedIds.includes(item.id)"
                  @click.stop="handleCheckboxClick(item)"
                  style="flex-shrink: 0"
                >
                </van-checkbox>

                <view class="tit">{{ item.name }}</view>
                <view class="type">{{ item.tag || item.level }}</view>
              </div>

              <div class="header-right">
                <!-- 使用 Vant Image 替代 image -->
                <van-image
                  v-if="onlyRelative"
                  :src="trashIcon"
                  @click.stop="onTrash(item)"
                  width="20px"
                  height="20px"
                  fit="contain"
                  class="trash"
                />

                <!-- 使用 Vant Icon 替代 uv-icon -->
                <van-icon
                  :name="item.expanded ? 'arrow-up' : 'arrow-down'"
                  size="16px"
                  color="#999"
                  @click.stop="toggleItem(item)"
                />
              </div>
            </div>

            <!-- 使用 Vant Collapse 的动画效果 -->
            <transition name="collapse">
              <div
                class="custom-content"
                :class="{ expanded: item.expanded }"
                v-show="item.expanded"
              >
                <div class="detail-content">
                  <div class="detail-main">{{ item.content }}</div>

                  <div class="detail-initiator">
                    <div class="initiator" v-if="currentTaskType === 'police'">
                      警单派发人：<span>{{ item.dispatcher }}</span>
                    </div>
                    <div class="initiator" v-else>
                      任务发起人：<span>{{ item.creatorName }}</span>
                    </div>
                  </div>

                  <div class="detail-footer">
                    <div class="time">
                      <van-icon name="clock" size="16px" color="#999" style="margin-right: 5px" />
                      {{ item.operateTime || item.createTime }}
                    </div>

                    <div class="status">
                      <van-icon
                        name="description"
                        size="16px"
                        color="#999"
                        style="margin-right: 5px"
                      />
                      {{ item.systemName }}
                    </div>
                  </div>
                </div>
              </div>
            </transition>
          </div>
        </van-checkbox-group>

        <!-- 加载状态 -->
        <!-- <view v-if="loading" class="loading-more">
        <uv-loading-icon text="加载中..."></uv-loading-icon>
      </view>
      <view v-else-if="!hasMore && orderList.length > 0" class="no-more">
        没有更多数据了
      </view>
    </scroll-view> -->
      </van-list>
    </view>
    <view v-else class="empty-data">暂无数据...</view>
  </view>
</template>

<script lang="ts" setup>
  import { showConfirmDialog, showSuccessToast, showToast } from 'vant';
  import { ref, computed, watch, onMounted } from 'vue';

  import { groupApi } from '@/common/api/index.js';
  import { getGlobalsConfigByKey } from '@/common/utils';
  import trashIcon from '@/static/5110/delete.png';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';
  // 使用设备适配
  const { adaptationSize } = useDeviceAdapter();
  const listLoading = ref(false);
  const listFinished = computed(() => !hasMore.value);
  // Props
  interface Props {
    currentGroup?: any;
    accessToken?: string;
    onlyRelative: Boolean;
    currentTaskType?: string; // 新增：当前任务类型
  }

  const props = withDefaults(defineProps<Props>(), {
    currentGroup: null,
    accessToken: '',
    onlyRelative: false,
    currentTaskType: 'police',
  });

  // Emits
  const emit = defineEmits<{
    'update:selectedCount': [count: number];
    confirm: [selectedItems: any[]];
    refresh: [];
  }>();

  // 响应式数据
  const searchText = ref('');
  const activeDays = ref(3);
  const orderList = ref<any[]>([]);
  const loading = ref(false);
  const hasMore = ref(true);
  const currentPage = ref(1);
  const totalPages = ref(0);
  const selectedIds = ref<string[]>([]);

  const tabTitles = ref([
    { name: '一天', id: 1 },
    { name: '三天', id: 3 },
    { name: '七天', id: 7 },
  ]);

  // 分别存储警单和任务的参数状态
  const policeState = ref({
    params: {
      current: 1,
      size: 10,
      groupId: '',
      name: '',
      keywords: '',
      postId: '',
    },
    selectedIds: [] as string[],
    hasMore: true,
  });

  const taskState = ref({
    params: {
      current: 1,
      size: 10,
      groupId: '',
      keywords: '',
    },
    selectedIds: [] as string[],
    hasMore: true,
  });

  // 计算属性
  const selectedCount = computed(() => {
    return selectedIds.value.length;
  });

  // 监听选中数量变化
  watch(selectedCount, (newCount) => {
    emit('update:selectedCount', newCount);
  });

  // 监听当前群组变化
  watch(
    () => props.currentGroup,
    (newGroup, oldGroup) => {
      if (newGroup && newGroup.groupId !== oldGroup?.groupId) {
        resetList();
        loadDataList();
      }
    },
    { deep: true },
  );

  // 监听任务类型变化 - 切换时恢复对应状态
  watch(
    () => props.currentTaskType,
    (newType, oldType) => {
      // 保存当前类型的状态
      if (oldType === 'police') {
        policeState.value.selectedIds = [...selectedIds.value];
        policeState.value.hasMore = hasMore.value;
      } else {
        taskState.value.selectedIds = [...selectedIds.value];
        taskState.value.hasMore = hasMore.value;
      }

      // 恢复新类型的状态
      if (newType === 'police') {
        selectedIds.value = [...policeState.value.selectedIds];
        hasMore.value = policeState.value.hasMore;
      } else {
        selectedIds.value = [...taskState.value.selectedIds];
        hasMore.value = taskState.value.hasMore;
      }

      // 重置列表数据和分页状态
      orderList.value = [];
      currentPage.value = 1;

      // 重置对应类型的参数页码
      if (newType === 'police') {
        policeState.value.params.current = 1;
      } else {
        taskState.value.params.current = 1;
      }

      loadDataList();
    },
  );

  watch(
    () => props.onlyRelative,
    async (newValue, oldValue) => {
      if (newValue !== oldValue) {
        // 重置列表但保持选中状态
        await resetList();
        await loadDataList();
      }
    },
    { immediate: false },
  );

  // 监听搜索文本变化
  watch(searchText, () => {
    resetList();
    loadDataList();
  });

  // 切换类型方法
  const switchType = (type: string) => {
    // 这个方法由父组件调用，watch已经处理了状态切换
  };

  // 获取日期范围（警单和任务都使用）
  function getDateRange() {
    const day = activeDays.value;
    if (day === 0 || day === null || day === undefined) return { startTime: '', endTime: '' };
    const res = getRecentDateRange(day);
    return res;
  }

  function getRecentDateRange(
    days: number,
    withSeparator = true,
  ): { startTime: string; endTime: string } {
    if (!Number(days) || days < 0) {
      throw new Error('days 参数必须是非负数');
    }

    const now = new Date();
    const oneDayMs = 24 * 60 * 60 * 1000;
    const startDate = new Date(now.getTime() - days * oneDayMs);

    const endTime = formatDateTimeString(now, withSeparator);
    const startTime = formatDateTimeString(startDate, withSeparator);

    return { startTime, endTime };
  }

  function formatDateTimeString(date: Date, withSeparator = true): string {
    const padZero = (num: number): string => num.toString().padStart(2, '0');

    const year = date.getFullYear();
    const month = padZero(date.getMonth() + 1);
    const day = padZero(date.getDate());
    const hour = padZero(date.getHours());
    const minute = padZero(date.getMinutes());
    const second = padZero(date.getSeconds());

    if (withSeparator) {
      return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
    }
    return `${year}${month}${day} ${hour}${minute}${second}`;
  }

  const handleChange = (val: string) => {
    if (!val) {
      resetList();
      loadDataList();
    }
  };

  const handleSearch = (val: string) => {
    loadDataList();
  };

  const switchTitle = (id: number) => {
    activeDays.value = id;
    resetList();
    loadDataList();
  };

  const toggleItem = (item: any) => {
    orderList.value.forEach((listItem) => {
      if (listItem.id !== item.id) {
        listItem.expanded = false;
      }
    });

    item.expanded = !item.expanded;
  };

  // 点击删除
  const onTrash = async (item: any) => {
    let text = '';
    if (props.currentTaskType === 'police') {
      text = '警单';
    } else {
      text = '任务';
    }
    // 显示确认对话框
    showConfirmDialog({
      title: '提示',
      message: `删除后将不会关联协同群组，是否确认删除关联${text}？`,
    })
      .then(async () => {
        let apiCall;
        if (props.currentTaskType === 'police') {
          apiCall = groupApi.deletePoliceticket({
            groupId: props.currentGroup.groupId,
            ticketId: item.id,
            token: props.accessToken,
          });
        } else {
          apiCall = groupApi.deleteTask({
            groupId: props.currentGroup.groupId,
            ticketId: item.id,
            token: props.accessToken,
          });
        }

        const result = await apiCall;
        // 删除成功后从列表中移除该项
        const index = orderList.value.findIndex((listItem) => listItem.id === item.id);
        if (index > -1) {
          orderList.value.splice(index, 1);
          // 如果该项目被选中，也需要从选中列表中移除
          const selectedIndex = selectedIds.value.indexOf(item.id);
          if (selectedIndex > -1) {
            selectedIds.value.splice(selectedIndex, 1);
          }
          // 更新选中数量
          emit('update:selectedCount', selectedIds.value.length);
          emit('refresh');
          showSuccessToast('删除成功');
        }
      })
      .catch(() => {
        console.log('点击取消');
      });
  };

  // 复选框变化处理
  const handleCheckboxChange = (ids: string[]) => {
    selectedIds.value = ids;
  };

  // 处理复选框点击
  const handleCheckboxClick = (item: any) => {
    const index = selectedIds.value.indexOf(item.id);
    if (index > -1) {
      selectedIds.value.splice(index, 1);
    } else {
      selectedIds.value.push(item.id);
    }
  };

  // 获取选中的项目
  const getSelectedItems = () => {
    return orderList.value.filter((item) => selectedIds.value.includes(item.id));
  };

  // 重置列表
  async function resetList() {
    orderList.value = [];
    currentPage.value = 1;
    hasMore.value = true;

    // 重置对应类型的参数，但不要重置选中状态
    if (props.currentTaskType === 'police') {
      policeState.value.params.current = 1;
    } else {
      taskState.value.params.current = 1;
    }
  }

  // 加载数据列表
  const loadDataList = async () => {
    if (!props.currentGroup) return;

    loading.value = true;
    listLoading.value = true;

    try {
      if (props.currentTaskType === 'police') {
        await loadPoliceTicketList();
      } else {
        await loadTaskList();
      }
    } catch (error) {
      showToast('获取数据失败');
    } finally {
      loading.value = false;
      listLoading.value = false;
    }
  };

  // 获取警单列表
  const loadPoliceTicketList = async () => {
    const { startTime, endTime } = getDateRange();
    policeState.value.params.groupId = props.currentGroup.groupId;
    policeState.value.params.name = searchText.value;

    const params = {
      ...policeState.value.params,
      startTime,
      endTime,
      token: props.accessToken,
      groupType: props.currentGroup?.groupType,
    };
    if (props.onlyRelative) {
      params.bindFlag = 1;
      delete params.startTime;
      delete params.endTime;
    }

    const res = await groupApi.getPoliceticket(params);
    if (res) {
      const newRecords = res.records.map((item: any) => ({
        ...item,
        expanded: false,
      }));

      const serverSelectedIds = newRecords
        .filter((item) => item.bindFlag === 1)
        .map((item) => item.id);

      if (policeState.value.params.current === 1) {
        // 第一页：直接使用服务器返回的选中状态
        selectedIds.value = serverSelectedIds;
        orderList.value = newRecords;
      } else {
        // 加载更多：合并之前页面的选中状态和新页面的选中状态
        const currentSelectedIds = [...selectedIds.value];
        const newSelectedIds = [...currentSelectedIds, ...serverSelectedIds];
        selectedIds.value = [...new Set(newSelectedIds)]; // 去重

        orderList.value = [...orderList.value, ...newRecords];
      }

      // 更新分页信息
      currentPage.value = res.current;
      totalPages.value = res.pages;
      hasMore.value = res.current < res.pages;

      if (hasMore.value) {
        policeState.value.params.current = Number(res.current) + 1;
      }
    }
  };

  // 获取任务列表
  const loadTaskList = async () => {
    const { startTime, endTime } = getDateRange();
    taskState.value.params.groupId = props.currentGroup.groupId;
    taskState.value.params.keywords = searchText.value;

    const params = {
      ...taskState.value.params,
      startTime,
      endTime,
      token: props.accessToken,
    };

    if (props.onlyRelative) {
      params.bindFlag = 1;
      delete params.startTime;
      delete params.endTime;
    }

    console.log('任务请求参数：', params, props.onlyRelative);

    const res = await groupApi.getTasksList(params);

    if (res) {
      const newRecords = res.records.map((item: any) => ({
        ...item,
        expanded: false,
      }));

      const serverSelectedIds = newRecords
        .filter((item) => item.bindFlag === 1)
        .map((item) => item.id);

      if (taskState.value.params.current === 1) {
        // 第一页：直接使用服务器返回的选中状态
        selectedIds.value = serverSelectedIds;
        orderList.value = newRecords;
      } else {
        // 加载更多：合并之前页面的选中状态和新页面的选中状态
        const currentSelectedIds = [...selectedIds.value];
        const newSelectedIds = [...currentSelectedIds, ...serverSelectedIds];
        selectedIds.value = [...new Set(newSelectedIds)]; // 去重

        orderList.value = [...orderList.value, ...newRecords];
      }

      // 更新分页信息
      currentPage.value = res.current;
      totalPages.value = res.pages;
      hasMore.value = res.current < res.pages;

      if (hasMore.value) {
        taskState.value.params.current = Number(res.current) + 1;
      }

      console.log('任务加载完成，选中ID:', selectedIds.value);
    }
  };

  // 加载更多数据
  const loadMore = () => {
    if (hasMore.value && !loading.value) {
      loadDataList();
    }
  };

  // 滚动到底部加载更多
  const onScrollToLower = () => {
    loadMore();
  };

  // 暴露方法给父组件
  defineExpose({
    getSelectedItems,
    switchType,
  });

  onMounted(async () => {
    if (props.currentGroup) {
      loadDataList();
    }
    // preloadImage();
  });
</script>

<style lang="scss" scoped>
  .police-ticket-content {
    // height: 35vh;
    height: 100%;
    display: flex;
    flex-direction: column;
    justify-content: space-between;

    .search-container {
      padding: 5px 15px;
      background: #fff;
    }

    .title-container {
      display: flex;
      padding: 10px 15px;
      background: #fff;
      border-bottom: 1px solid #f0f0f0;

      .title-item {
        padding: 5px 15px;
        margin-right: 10px;
        border-radius: 15px;
        background: #f5f5f5;
        color: #666;
        font-size: 14px;

        &.active {
          background: #264ed1;
          color: #fff;
        }
      }
    }

    .data-container {
      // height: 34vh;
      flex: 1;
      overflow-y: auto;
      background: #fff;
      padding: 10px 15px;

      .custom-collapse-item {
        width: 100%;
        // margin: 5px 0;
        border-radius: 8px;
        overflow: hidden;
        border-bottom: 1px solid rgba(245, 245, 245, 1);
        // box-shadow: 0 2px 8px rgba(0,0,0,0.1);

        .custom-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 12px;
          background: #fff;
          cursor: pointer;
          transition: background-color 0.3s ease;

          &.expanded {
            background: #f5f8fd;
          }

          .header-left {
            display: flex;
            align-items: center;
            justify-content: flex-start;
            // flex: 1;
            width: 70%;
            .tit {
              width: 80%;
              margin: 0 10px;
              font-size: 16px;
              font-weight: 500;
              color: #333;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }

            .type {
              padding: 2px 8px;
              background: #dde8ff;
              color: #264ed1;
              border-radius: 4px;
              font-size: 12px;
              white-space: nowrap;
            }
          }
          .header-right {
            width: 30%;
            display: flex;
            justify-content: flex-end;
          }
          .trash {
            width: 14px;
            height: 14px;
            margin-right: 10px;
          }
        }

        .custom-content {
          background: #f5f8fd;

          &.expanded {
            animation: fadeIn 0.3s ease-in-out;
          }

          .detail-content {
            padding: 15px;

            .detail-main {
              margin-bottom: 10px;
              color: #666;
              line-height: 1.5;
            }

            .detail-initiator {
              margin-bottom: 10px;

              .initiator {
                color: #666;

                span {
                  color: #333333;
                  font-weight: 500;
                }
              }
            }

            .detail-footer {
              display: flex;
              justify-content: space-between;
              font-size: 12px;
              color: #999;

              .time,
              .status {
                display: flex;
                align-items: center;
              }
            }
          }
        }
      }

      .loading-more,
      .no-more {
        text-align: center;
        padding: 5px 20px 0 20px;
        color: #999;
        font-size: 14px;
      }
    }

    .empty-data {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #999;
      font-size: 16px;
    }
  }

  @keyframes fadeIn {
    from {
      opacity: 0;
      transform: translateY(-10px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  .my-custom-search {
    width: 100%;
    background-color: #ffffff;
    border-radius: 8px;
    padding: 0px;

    :deep(.van-search__field) {
      background-color: #f2f2f2;
      border-radius: 6px;
      padding: 0px 10px;
    }
    :deep(.van-search__content) {
      padding-left: 0px;
    }
    :deep(.van-search__action) {
      color: rgba(38, 99, 255, 1);
      font-size: 16px;
    }

    :deep(.van-field__control) {
      font-size: 16px;
    }
  }
</style>
