<script setup>
  import { computed, ref, watch } from 'vue';

  import { getGlobalsList } from '@/api/dictionary';
  import {
    deletePoliceticket,
    deleteTask,
    getPoliceticket,
    getTasksList,
    relativePoliceticket,
    relativeTask,
  } from '@/api/policeTicket';
  import { Message } from '@/components/Message';
  import MessageBox from '@/components/MessageBox';
  import { useEmitter } from '@/hooks';
  import { getRecentDateRange } from '@/utils/index';

  import { Loading } from '@element-plus/icons-vue';

  const props = defineProps({
    license: {
      default: '0',
      type: String,
    },
    relativeGroupId: {
      default: '',
      required: true,
      type: String,
    },
    item: {
      default: null,
      type: Object,
    },
  });

  // 是否为只查看关联数据页面

  const emit = defineEmits(['archive', 'edit-tag', 'closeRelationDialog']);
  const tabTitles = ref([
    { id: 1, name: '一天' },
    { id: 3, name: '三天' },
    { id: 7, name: '七天' },
  ]);
  const searchText = ref('');
  const contentData = ref({
    orderList: [],
    taskList: [],
  });

  // 关联类型数据
  const associationTypes = ref([
    { id: 'orderList', name: '关联警单' },
    { id: 'taskList', name: '关联任务' },
  ]);
  const activeAssociationType = ref('orderList');
  const current = ref(1);
  const days = ref(3); // 当前点击标签的索引
  const queryParams = {
    current: 1,
    name: '',
    postId: '',
    size: 50,
  };

  const loading = ref(false);
  const hasMore = ref(true);
  const onlyRelative = ref(false);
  
  // 保存用户手动修改的勾选状态（跨日期/类型切换时保留）
  // key: 数据id, value: bindFlag (1=选中, 0=未选中)
  const userSelectionMap = ref(new Map());
  const selectedCount = computed(() => {
    console.log(contentData, activeAssociationType.value, 'activeAssociationType.value')
    return contentData.value[activeAssociationType.value].filter((item) => item.bindFlag === 1)
      .length;
  });

  watch(
    () => props.relativeGroupId,
    (newGroupId, oldGroupId) => {
      // 当群组ID变化时，重置所有状态并重新初始化
      if (newGroupId && newGroupId !== oldGroupId) {
        resetComponentState();
        init();
      }
    },
    { immediate: true },
  );
  watch(
    () => searchText.value,
    () => {
      init();
    },
  );
  watch(
    () => onlyRelative.value,
    () => {
      init();
    },
  );

  // 获取appConfig信息
  async function getAppConfig() {
    const { code, data } = await getGlobalsList();
    if (code === 0) {
      if (props.license === '0') {
        associationTypes.value = associationTypes.value.filter((item) => item.id !== 'taskList');
      } else {
        if (data.h5 !== '/') {
          associationTypes.value = associationTypes.value.filter((item) => item.id !== 'taskList');
        }
      }
    }
  }
  // 重置组件状态
  function resetComponentState() {
    // 清空数据
    contentData.value = {
      orderList: [],
      taskList: [],
    };
    // 重置搜索条件
    searchText.value = '';
    // 重置日期选择
    days.value = 3;
    // 重置关联类型
    activeAssociationType.value = 'orderList';
    // 重置当前页
    current.value = 1;
    queryParams.current = 1;
    queryParams.name = '';
    // 重置加载状态
    loading.value = false;
    hasMore.value = true;
    // 清空用户勾选状态
    userSelectionMap.value.clear();
  }

  function init() {
    queryParams.current = 1;
    hasMore.value = true;
    contentData.value = {
      orderList: [],
      taskList: [],
    };
    getPoliceTicketList();
    getTaskList();
    getAppConfig();
  }

  function getDateRange() {
    const day = days.value;
    if (day === 0 || day === null || day === undefined) return { endTime: '', startTime: '' };
    const res = getRecentDateRange(day);
    return res;
  }
  // 加载更多数据
  async function loadMore() {
    if (loading.value || !hasMore.value) return;

    loading.value = true;
    queryParams.current += 1;

    try {
      await refreshOne();
    } finally {
      loading.value = false;
    }
  }

  // 滚动事件处理
  function handleScroll(event) {
    const { target } = event;
    const { clientHeight, scrollHeight, scrollTop } = target;

    // 当滚动到距离底部50px时触发加载更多
    if (scrollTop + clientHeight >= scrollHeight - 50 && hasMore.value && !loading.value) {
      loadMore();
    }
  }
  async function relativeFunc() {
    if (onlyRelative.value) {
      emit('closeRelationDialog');
      return;
    }
    const selectedItems = contentData.value[activeAssociationType.value].filter(
      (item) => item.bindFlag === 1,
    );
    const ids = selectedItems.map((item) => item.id);
    
    // 检查是否选择了数据
    if (ids.length === 0) {
      Message({ message: '请至少选择一条数据', type: 'warning' });
      return;
    }
    
    const params = { data: ids, groupId: props.relativeGroupId };
    let res = {};
    res = await (activeAssociationType.value === 'orderList'
      ? relativePoliceticket(params)
      : relativeTask(params));
    const { code } = res;
    if (code === 0) {
      Message({ message: '关联成功', type: 'success' });
      queryParams.current = 1;
      // 关联成功后清空勾选状态
      userSelectionMap.value.clear();
      // 成功后刷新两类数据，计算最新的关联数量并回传给父组件
      await Promise.all([getPoliceTicketList(), getTaskList()]);
      updateRelationNumber();
      emit('closeRelationDialog');
      return;
    }
    Message({ message: '关联失败，请稍后重试', type: 'error' });
  }

  // 获取警单列表
  async function getPoliceTicketList() {
    const { endTime, startTime } = getDateRange();
    const params = {
      ...queryParams,
      endTime,
      groupId: props.relativeGroupId,
      name: searchText.value,
      startTime,
      groupType: props.item?.groupType,
    };
    if (onlyRelative.value) {
      params.bindFlag = 1;
      delete params.startTime;
      delete params.endTime;
    }
    const { code, data } = await getPoliceticket(params);

    if (code === 0) {
      const { records, total } = data;

      // 恢复用户的勾选状态
      records.forEach((item) => {
        if (userSelectionMap.value.has(item.id)) {
          item.bindFlag = userSelectionMap.value.get(item.id);
        }
      });

      if (queryParams.current === 1) {
        // 第一页，替换数据
        contentData.value.orderList = [...records];
      } else {
        // 后续页，追加数据
        contentData.value.orderList = [...contentData.value.orderList, ...records];
      }

      // 判断是否还有更多数据
      hasMore.value = contentData.value.orderList.length < total;
    }
  }
  // 获取任务列表
  async function getTaskList() {
    const { endTime, startTime } = getDateRange();
    const params = {
      ...queryParams,
      endTime,
      groupId: props.relativeGroupId,
      keywords: searchText.value,
      startTime,
    };
    if (onlyRelative.value) {
      params.bindFlag = 1;
      delete params.startTime;
      delete params.endTime;
    }
    const { code, data } = await getTasksList(params);
    if (code === 0) {
      const { records, total } = data;

      // 恢复用户的勾选状态
      records.forEach((item) => {
        if (userSelectionMap.value.has(item.id)) {
          item.bindFlag = userSelectionMap.value.get(item.id);
        }
      });

      if (queryParams.current === 1) {
        // 第一页，替换数据
        contentData.value.taskList = records;
      } else {
        // 后续页，追加数据
        contentData.value.taskList = [...contentData.value.taskList, ...records];
      }

      // 判断是否还有更多数据
      hasMore.value = contentData.value.taskList.length < total;
    }
  }
  // 切换日期
  const switchTitle = (index) => {
    days.value = index;
    refreshOne();
  };

  // 切换关联类型
  const switchAssociationType = async (type) => {
    activeAssociationType.value = type;
    refreshOne();
  };
  async function refreshOne() {
    await (activeAssociationType.value === 'orderList' ? getPoliceTicketList() : getTaskList());
  }
  function lookRelativeFunc() {
    onlyRelative.value = !onlyRelative.value;
  }
  // 取消
  const cancelSelection = (flag) => {
    emit('closeRelationDialog', flag);
  };

  function isChecked(item) {
    return item.bindFlag === 1;
  }
  function handleChange(item, checked) {
    item.bindFlag = checked ? 1 : 0; // 或者其他非1的值
    // 保存用户勾选状态
    userSelectionMap.value.set(item.id, item.bindFlag);
  }

  function updateRelationNumber() {
    const polTicketCnt = contentData.value.orderList.filter((item) => item.bindFlag === 1).length;
    const tasksCnt = contentData.value.taskList.filter((item) => item.bindFlag === 1).length;
    const updateData = {
      groupId: props.relativeGroupId,
      polTicketCnt,
      tasksCnt,
      updatedCounts: true,
    };
    useEmitter().emit('REFRESH_RELATION_NUMBER', updateData);
  }

  async function deleteFunc(item) {
    const flag = await MessageBox({
      offset: ['38%', '20%'],
      text: `删除后将不会关联协同群组，是否确认删除关联${activeAssociationType.value === 'orderList' ? '警单' : '任务'}？`,
      type: 'ok',
      zIndexDefault: 9999, // 设置一个很高的z-index值，确保在最上层
    });
    if (!flag) return;

    const params = { groupId: props.relativeGroupId, ticketId: item.id };
    let res;
    res = await (activeAssociationType.value === 'orderList'
      ? deletePoliceticket(params)
      : deleteTask(params));
    const { code } = res;
    if (code === 0) {
      item.bindFlag = 0;
      updateRelationNumber(); // 删除后更新其他页面关联数量
      Message({ message: '删除关联成功', type: 'success' });
      queryParams.current = 1;
      refreshOne();
      return;
    }
    Message({ message: '删除失败，请稍后重试', type: 'error' });
  }
</script>

<template>
  <div class="custom-component">
    <!-- 关联类型选择 -->
    <div class="association-tabs">
      <div class="tabs-container">
        <div
          v-for="(item, index) in associationTypes"
          :key="index"
          class="tab-item"
          :class="{ active: activeAssociationType === item.id }"
          @click="switchAssociationType(item.id)"
        >
          {{ item.name }}
        </div>
      </div>
      <div
        class="view-association"
        :class="{ 'only-relative-association': onlyRelative }"
        @click="lookRelativeFunc"
      >
        查看关联
      </div>
    </div>

    <!-- 搜索框 -->
    <div class="search-container">
      <el-input v-model="searchText" clearable placeholder="请输入关键词" />
    </div>

    <!-- 日期切换部分 -->
    <div v-if="!onlyRelative" class="title-container">
      <div
        v-for="(item, index) in tabTitles"
        :key="index"
        class="title-item"
        :class="{ active: days === item.id }"
        @click="switchTitle(item.id)"
      >
        {{ item.name }}
      </div>
    </div>

    <!-- 数据列表部分 -->
    <div
      v-if="contentData[activeAssociationType].length > 0"
      class="data-container"
      @scroll="handleScroll"
    >
      <el-collapse accordion>
        <el-collapse-item
          v-for="item in contentData[activeAssociationType]"
          :key="item.id"
          class="collapse-item"
          :name="item.id"
        >
          <template #title>
            <div class="title-item">
              <div class="title-left">
                <el-checkbox
                  v-if="!onlyRelative"
                  :checked="isChecked(item)"
                  @change="handleChange(item, $event)"
                  @click.stop
                />
                <TdTooltip :content="item.name">
                  <div class="tit">{{ item.name }}</div>
                </TdTooltip>
                <div class="type">{{ item.tag || item.level }}</div>
              </div>
              <div v-if="onlyRelative" class="title-right" @click="deleteFunc(item)" @click.stop>
                <Icon class="icon" name="delete-order" />
              </div>
            </div>
          </template>
          <!-- 详细内容 -->
          <div class="detail-content">
            <div class="detail-main">{{ item.content }}</div>
            <div class="detail-initiator">
              <div class="initiator">
                {{ activeAssociationType === 'orderList' ? '警单派发人' : '任务发起人' }}：<span>{{
                  item.dispatcher || item.creatorName
                }}</span>
              </div>
            </div>
            <div class="detail-footer">
              <div class="time">
                <Icon class="icon" name="time_group" />{{ item.operateTime || item.createTime }}
              </div>
              <div class="status"> <Icon class="icon" name="review" />{{ item.systemName }} </div>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-more">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>
      <div v-else-if="!hasMore && contentData[activeAssociationType].length > 0" class="no-more">
        没有更多数据了
      </div>
    </div>

    <div v-else class="empty-data">暂无数据...</div>

    <!-- 底部操作栏 -->
    <div class="footer-actions" :class="{ 'only-footer': onlyRelative }">
      <div v-if="!onlyRelative" class="selected-count">已选：{{ selectedCount }}</div>
      <div class="action-buttons">
        <el-button @click="cancelSelection">取消</el-button>
        <el-button type="primary" @click="relativeFunc">
          {{ onlyRelative ? '确定' : '关联' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .custom-component {
    position: relative;
    display: flex;
    flex-direction: column;
    width: 100%;
    height: 100%;
    padding: 0 15px 15px;

    .association-tabs {
      display: flex;
      align-items: flex-end;
      justify-content: space-between;
      margin-bottom: 15px;
      border-bottom: 1px solid var(--border-color2);

      .tabs-container {
        display: flex;
        gap: 20px;

        .tab-item {
          padding: 4px 8px;
          font-size: 14px;
          color: var(--tabs-color);
          cursor: pointer;

          &:hover {
            color: var(--tabs-active-color);
          }

          &.active {
            font-weight: 500;
            color: var(--tabs-active-color);
            border-bottom: 2px solid var(--tabs-active-color);
          }
        }
      }

      .view-association {
        padding: 2px 10px;
        font-size: 16px;
        color: var(--tabs-color);
        cursor: pointer;
        background: var(--button-bg);
        border: 1px solid var(--button-bg);
        border-radius: 4px;

        &:hover {
          color: var(--tabs-active-color);
          background-color: var(--button-hover-bg);
          border: 1px solid var(--tabs-active-color);
        }

        &.active {
          font-weight: 500;
          color: var(--tabs-active-color);
          background-color: var(--button-hover-bg);
          border: 1px solid var(--tabs-active-color);
        }
      }

      .only-relative-association {
        color: var(--tabs-active-color);
        background-color: var(--button-hover-bg);
        border: 1px solid var(--tabs-active-color);
      }
    }

    .search-container {
      margin-bottom: 5px;

      :deep(.el-input__inner) {
        color: var(--search-text-color);
        background: var(--search-bg);
        border: 1px solid var(--border-color2);

        &::placeholder {
          color: var(--tabs-color);
        }
      }
    }

    .title-container {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      justify-content: flex-start;
      padding: 6px 0;

      .title-item {
        padding: 3px 16px;
        font-size: 14px;
        color: var(--tabs-color2);
        cursor: pointer;
        background-color: var(--tabs-bg);
        border-radius: 16px;
        transition: all 0.3s;

        &:hover {
          color: var(--tabs-active-color2);
          background-color: var(--tabs-active-bg);
        }

        &.active {
          color: var(--tabs-active-color2);
          background-color: var(--tabs-active-bg);
        }
      }
    }

    .data-container {
      box-sizing: border-box;
      flex: 1;
      max-height: 50vh;
      padding-bottom: 40px;
      overflow: hidden auto;

      :deep(.el-collapse) {
        border: none;
      }

      .collapse-item {
        margin-bottom: 10px;

        :deep(.el-collapse-item__header) {
          height: 40px;
          padding: 0 10px;
          background-color: var(--background-other-color);
          border-bottom: 1px solid var(--border-color2);

          // 展开按钮箭头 - 默认朝下
          .el-collapse-item__arrow {
            color: var(--text-color);
            transition: transform 0.3s ease;
            transform: rotate(90deg);
          }
        }

        &.is-active {
          border-radius: 2px;

          :deep(.el-collapse-item__header) {
            background-color: var(--background-color) !important;
            border: none !important;

            // 展开后箭头朝上
            .el-collapse-item__arrow {
              transform: rotate(-90deg);
            }
          }
        }
      }

      .title-item {
        box-sizing: border-box;
        display: flex;
        align-items: center;
        justify-content: space-between;
        width: 100%;

        .title-left {
          box-sizing: border-box;
          display: flex;
          align-items: center;
          width: calc(100% - 100px);
        }

        .title-right {
          margin-right: 10px;
        }

        .tit {
          box-sizing: border-box;
          max-width: 80%;
          padding: 0 5px;
          overflow: hidden;
          font-weight: 500;
          color: var(--text-color);
          text-overflow: ellipsis;
          word-break: break-all;
          overflow-wrap: break-word;
          white-space: nowrap;
        }

        .type {
          height: 20px;
          padding: 0 3px;
          font-size: 12px;
          line-height: 20px;
          color: var(--type-color);
          background: var(--type-back);
          border-radius: 5px;
        }
      }
      // .title-item {
      //   display: flex;
      //   align-items: center;
      //   justify-content: flex-start;
      //   width: 100%;

      //   .tit {
      //     max-width: 80%;
      //     margin: 0 5px;
      //     overflow: hidden;
      //     font-size: 14px;
      //     font-weight: 500;
      //     line-height: 22px;
      //     color: #333;
      //     text-overflow: ellipsis;
      //     word-break: break-all;
      //     overflow-wrap: break-word;
      //     white-space: nowrap;
      //   }

      //   .type {
      //     height: 20px;
      //     padding: 0 3px;
      //     font-size: 12px;
      //     line-height: 20px;
      //     color: rgb(38 78 209 / 100%);
      //     background: #dde8ff;
      //     border-radius: 5px;
      //   }
      // }

      .detail-content {
        .detail-main {
          margin-bottom: 3px;
          color: var(--tabs-color);
        }

        .detail-initiator {
          margin-bottom: 3px;

          .initiator {
            color: var(--tabs-color);

            span {
              font-weight: 500;
              color: var(--hight-text-color);
            }
          }
        }

        .detail-footer {
          display: flex;
          justify-content: space-between;
          font-size: 12px;

          .time,
          .status {
            display: flex;
            align-items: center;
            color: var(--tabs-color);

            .icon {
              width: 16px;
              height: 16px;
              margin-top: 3px;
              margin-right: 5px;
            }
          }
        }
      }

      :deep(.el-collapse-item__wrap) {
        background: none !important;
        border-bottom: 1px solid var(--border-color2) !important;
      }

      :deep(.el-collapse-item__content) {
        padding: 0 10px 15px;
        margin-top: -3px;
        background-color: var(--background-color) !important;
      }

      .loading-more,
      .no-more {
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 15px;
        font-size: 14px;
        color: var(--tabs-color);

        .el-icon {
          margin-right: 8px;
        }
      }

      .loading-more {
        .el-icon.is-loading {
          animation: rotating 2s linear infinite;
        }
      }
    }

    .empty-data {
      height: 100px;
      line-height: 100px;
      color: var(--tabs-color);
      text-align: center;
    }

    .footer-actions {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 12px 0;
      margin-top: 10px;

      .selected-count {
        font-weight: 500;
        color: var(--tabs-active-color);
      }

      .action-buttons {
        display: flex;
        gap: 10px;
      }
    }

    .only-footer {
      justify-content: flex-end;
    }
  }

  @keyframes rotating {
    0% {
      transform: rotate(0deg);
    }

    100% {
      transform: rotate(360deg);
    }
  }

  :deep(.el-button) {
    color: var(--text-color);
    background: var(--button-bg);
    border: 1px solid var(--border-color);
    border-radius: 2px;

    span {
      font-size: 14px;
      font-weight: 400;
      line-height: 22px;
      color: var(--text-color);
      letter-spacing: 0;
    }
  }

  :deep(.el-button--primary) {
    background: var(--button-active-color);
    border-color: var(--button-active-color);

    span {
      color: #fff !important;
    }
  }

  :deep(.footer-actions .action-buttons .el-button--primary > span) {
    color: #fff !important;

    &:hover {
      color: #fff !important;
    }
  }

  :deep(.el-button--primary.is-disabled) {
    background: rgb(217 217 217 / 100%) !important;
    border-color: rgb(217 217 217 / 100%) !important;

    span {
      color: rgb(0 0 0 / 25%) !important;
    }

    &:hover {
      background: rgb(217 217 217 / 100%) !important;
      border-color: rgb(217 217 217 / 100%) !important;
    }
  }
</style>
