<script lang="ts" setup>
  import { computed, onMounted, ref, unref, watch } from 'vue';

  import { Dialog } from '@/components/Dialog';
  import { useEmitter, useI18n } from '@/hooks';
  import { useControlTaskStore, useMainStore } from '@/store';

  import { throttle } from 'lodash-es';

  import ControlTaskDetail from './controlTaskDetail.vue';
  import ControlTaskSearch from './controlTaskSearch.vue';
  import ControlTaskSimple from './controlTaskSimple.vue';
  import CreateControlTask from './createControlTask.vue';

  type ActiveId = 1 | 2 | 999;

  const props = defineProps({
    showTab: {
      default: true,
      type: Boolean,
    },
  });

  const { t } = useI18n();
  const mainStore = useMainStore();
  const controlTask = useControlTaskStore();

  const query = ref('');
  const activeId = ref<ActiveId>(1); // 初始状态码
  const taskCardShowId = ref('');
  const listRef = ref();

  // 任务数据
  const currentTaskList = computed(() => {
    return controlTask.controlTaskData[unref(activeId)].data;
  });
  const showSearch = computed(() => query.value !== '');
  const showTaskList = computed(() => {
    return !controlTask.controlTaskData[999]?.total;
  });
  const controlTaskType = computed(() => {
    return [
      {
        count: controlTask.controlTaskData[1].total,
        label: t('videoControl.controlTask.processing'),
        value: 1,
      },
      {
        count: controlTask.controlTaskData[2].total,
        label: t('videoControl.controlTask.paused'),
        value: 2,
      },
      {
        count: controlTask.controlTaskData[999].total,
        label: t('videoControl.controlTask.all'),
        value: 999,
      },
    ];
  });

  watch(
    () => mainStore.videoControlShowId,
    (val) => {
      taskCardShowId.value = val;
    },
  );

  useEmitter('refresh', initTab);
  useEmitter('circleResource', circleEvent);

  onMounted(() => {
    initTab();
  });

  function initTab() {
    activeId.value = props.showTab ? 1 : 999;
  }

  // 滚动触发显示更多任务
  const scroll = throttle(() => {
    const { clientHeight, scrollHeight, scrollTop } = listRef.value; // 滚动高度;内容高度;可见高度
    // 触底
    if (scrollTop + clientHeight < scrollHeight) {
      controlTask.loadMoreControlTask(unref(activeId));
    }
  }, 500);

  function circleEvent(data) {
    // 这里是圈选建组，那么需要移动到设防任务组件部分，同时打开新建
    createTask(data);
  }

  function createTask(childCircleData = []) {
    Dialog({
      cid: 'createControlTask',
      content: CreateControlTask,
      data: {
        // 传入一个圈选来的数据进去
        childCircleData,
        clickType: 'create',
      },
      offset: ['600px', '80px'],
    });
  }

  function taskClickItem(taskId?: string) {
    if (taskCardShowId.value === taskId) {
      taskCardShowId.value = '';
      mainStore.updateVideoControlShowId('');
    } else {
      taskCardShowId.value = taskId as string;
      mainStore.updateVideoControlShowId(taskId);
    }
  }

  // 任务类型不同的数据过滤展示
  function showChildMessage(data) {
    activeId.value = data.value;
    listRef.value.scrollTo(0, 0);
  }
</script>

<template>
  <div class="control-task">
    <!-- 统计tab -->
    <TdTabCount
      v-show="showTab"
      :active="activeId"
      class="tab-box"
      :tabs="controlTaskType"
      @click="showChildMessage"
    />

    <!-- 搜索 -->
    <TdInput
      v-model="query"
      class="control-task-input"
      :placeholder="t('videoControl.createDeployment.controlName')"
      type="searchInput"
    />

    <!-- 搜索结果 -->
    <ControlTaskSearch
      v-show="showSearch"
      :class="showTab ? 'control-task-search' : 'all-list'"
      :click-id="taskCardShowId"
      :search-content="query"
      @search-mission-id="taskClickItem"
    />

    <!-- 设防列表 -->
    <div
      v-show="!showSearch"
      ref="listRef"
      :class="showTab ? 'control-task-list' : 'all-list'"
      @scroll="scroll"
    >
      <TdEmpty v-if="showTaskList" />
      <template v-else>
        <ControlTaskSimple
          v-for="item in currentTaskList"
          :key="item.suspectTaskId"
          :click-id="taskCardShowId"
          :task="item"
          @click-item="taskClickItem"
        />
        <TdEmpty v-if="currentTaskList.length === 0" />
      </template>
    </div>

    <!-- 详情 -->
    <ControlTaskDetail
      v-if="taskCardShowId"
      :class="showTab ? 'control-task-detail-right' : 'control-task-detail-left'"
      :suspect-task-id="taskCardShowId"
    />
  </div>
</template>

<style lang="less" scoped>
  .control-task {
    position: relative;
    display: flex;
    flex-direction: column;
    height: 100%;
    pointer-events: all;

    .tab-box {
      margin-top: 10px;
    }

    .control-task-input {
      margin: 10px 0;
    }

    .control-task-search {
      flex: 1;
      overflow-y: auto;
    }

    .control-task-list {
      flex: 1;
      overflow-y: auto;
    }

    .all-list {
      height: 760px;
      overflow: auto;
    }

    .control-task-detail {
      &-right {
        position: absolute;
        top: -36px;
        left: 310px;
      }

      &-left {
        position: absolute;
        top: -36px;
        right: 310px;
      }
    }
  }
</style>
