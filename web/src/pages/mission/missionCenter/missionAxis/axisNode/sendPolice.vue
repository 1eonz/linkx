<script lang="ts" setup>
  import type { TaskData } from '@/pages/types/mission';

  import { reactive, ref, watch } from 'vue';

  import { flowMissionProcess } from '@/api/mission';
  import { queryEoResourcesByLike } from '@/api/resource';
  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { useEmitter, useI18n } from '@/hooks';
  import PoliceForceTree from '@/pages/tree/policeForceTree.vue';
  import dataUtil from '@/utils/dataUtil';

  import AssignTask from './sendPolice/assignTask.vue';

  const props = defineProps({
    info: {
      default: () => {},
      type: Object,
    },
    openType: {
      default: '',
      type: String,
    },
    position: {
      default: () => {},
      type: Object,
    },
  });
  const emit = defineEmits(['closeDialog']);

  const { t } = useI18n();

  const searchResult = ref([]);
  const isLoading = ref(false);
  const taskData = reactive<TaskData>({
    position: {
      address: '',
      latLon: '',
    },
    selectItem: [],
    type: 'list',
    typeText: t('mission.asignTask.dispose'),
  });
  const searchKey = ref('');
  const searchCount = ref(0);
  const assignTaskRef = ref();

  // 满足条件才能派发
  watch(
    taskData,
    (val) => {
      const { selectItem, typeText } = val;
      let disable = true;
      let buttonType = 'normal';

      if (selectItem.length > 0 && typeText !== '') {
        disable = false;
        buttonType = 'guide';
      }

      useEmitter().emit('changeButtonState', {
        buttonType,
        disable,
      });
    },
    { deep: true },
  );
  watch(searchKey, (val) => {
    taskData.type = val === '' ? 'list' : 'search';
  });
  watch(
    () => props.info,
    () => {
      taskData.selectItem = [];
      taskData.typeText = t('mission.asignTask.dispose');
      taskData.position = {
        address: '',
        latLon: '',
      };
      assignTaskRef.value?.resetData();
    },
  );

  // 获取任务数据
  function getTaskData(val) {
    Object.assign(taskData, val?.taskData);
  }

  // 删除列表中指定人
  function deleteSelectedItem(item) {
    const index = taskData.selectItem.indexOf(item);
    taskData.selectItem.splice(index, 1);
  }

  // 派警
  async function sendPolice() {
    const { flowId, payload } = props.info;
    const executorTargets: string[] = [];
    const executorNames: string[] = [];
    taskData.selectItem.forEach((item) => {
      executorTargets.push(item.id);
      executorNames.push(item.name);
    });
    const param = {
      action: 'DISPATCH',
      flowId,
      payload: {
        address: taskData.position.address,
        commandCenterId: appConfig.userData.commandCenterId,
        details: taskData.typeText,
        executorId: appConfig.userData.id,
        executorNames,
        executorTargets,
        importantLevel: payload.importantLevel,
        location: taskData.position.latLon,
        name: taskData.typeText,
        remark: '',
      },
    };
    const { code } = await flowMissionProcess(1, param); // 创建任务

    assignTaskRef.value?.onSendPoliceBack();

    // 结果校验
    if (code === 0) {
      Message(t('mission.asignTask.dispatchedTaskSuccessful'));
      useEmitter().emit('sendPoliceSuccess', flowId);
      // 关闭窗口
      closeCard();
    } else {
      Message(t('mission.timeline.createTaskFailed'));
    }
  }

  // 改变展示的类型
  function changeType(data) {
    taskData.type = data;
  }

  function closeCard() {
    if (props.openType === '') {
      emit('closeDialog');
    }
  }

  // 单击事件
  function singleClick(data) {
    const result = taskData.selectItem.some((item, index) => {
      if (item.id === data.nodes.id) {
        taskData.selectItem.splice(index, 1);
        return true;
      }
      return false;
    });
    if (!result) {
      taskData.selectItem.push(data.nodes);
    }
  }

  // 人员与组织模糊搜索
  async function searchPersonAndOrg(data) {
    searchResult.value = [];
    searchKey.value = data;
    if (!data) return;
    isLoading.value = true;

    const res = await queryEoResourcesByLike({
      cappOnly: true,
      keywords: data,
      pageSize: 100,
      start: 1,
    });

    if (dataUtil.checkReturnData(res)) {
      searchCount.value = res.data.total;
      let orgs = [];
      let individuals = [];
      let searchData = [];
      orgs = res.data.records.filter((item) => {
        return item.resourceType === 'Organization';
      });
      individuals = res.data.records.filter((item) => {
        return item.resourceType === 'Executor';
      });
      searchData = [...orgs, ...individuals];
      searchResult.value = searchData;
    }
    isLoading.value = false;
  }
</script>

<template>
  <div class="send-police" :class="{ 'send-police-plus': openType === '' }">
    <AssignTask
      ref="assignTaskRef"
      :item="taskData"
      @delete-selected-item="deleteSelectedItem"
      @get-task-data="getTaskData"
      @search-person-and-org="searchPersonAndOrg"
      @send-police="sendPolice"
    />

    <PoliceForceTree
      v-loading="isLoading"
      class="police-force-tree"
      :position="position"
      :search-count="searchCount"
      :search-key="searchKey"
      :search-result="searchResult"
      :selected-item="taskData.selectItem"
      :type="taskData.type"
      @change-type="changeType"
      @single-click="singleClick"
    />
  </div>
</template>

<style lang="less" scoped>
  .send-police {
    display: flex;
    flex-direction: column;
    height: 100%;
    padding-top: 5px;
    border-radius: 5px;

    .task-title {
      display: table-cell;
      flex-direction: row;
      margin-top: 20px;

      .title-content {
        width: fit-content;
        font-size: var(--font-size-large);
        color: var(--text-default);
      }

      .task-close {
        position: relative;
        top: 2px;
        left: 350px;
        z-index: 10;
        width: 30px;
        height: 30px;
        padding: 5px;
        cursor: pointer;
        fill: var(--icon-color-frame-control);
      }
    }

    .task-time {
      display: flex;
      margin-top: 10px;

      .type-icon {
        width: 16px;
        height: 16px;
        margin-top: 10px;
        margin-right: 10px;
        fill: var(--text-title-second);
      }
    }

    .task-type {
      display: flex;
      margin-top: 10px;

      .type-icon {
        width: 16px;
        height: 16px;
        margin-top: 10px;
        margin-right: 10px;
        fill: var(--text-title-second);
      }

      .type-content {
        width: 325px;
        height: auto;
        margin-left: 3px;
        border-radius: var(--border-radius);
        box-shadow: var(--shadow-default);
      }
    }

    .task-address {
      display: flex;
      margin-top: 10px;

      .address-icon {
        width: 16px;
        height: 16px;
        margin-top: 10px;
        margin-right: 10px;
        fill: var(--text-title-second);
      }

      .address-content {
        width: 325px;
        height: auto;
        margin-left: 3px;
        border-radius: var(--border-radius);
        box-shadow: var(--shadow-default);
      }

      .address-result {
        .search-droplist-box {
          position: absolute;
          top: 147px;
          left: 40px;
          z-index: 10;
          width: 387px;
          max-height: 353px;
          overflow-y: auto;
          background: var(--background-default);
          border-radius: 3px;

          .search-droplist-item {
            height: 2.5rem;
            padding: 0 1.25rem;
            clear: both;
            font-size: 1rem;
            line-height: 2.5rem;
            background: var(--background-default);

            .place {
              padding-top: 1px;
              color: var(--text-default);
            }

            .block {
              padding-top: 2px;
              padding-left: 5px;
              color: var(--text-default);
            }

            &:hover {
              background: var(--background-normal);
            }
          }
        }
      }
    }

    .task-police {
      display: flex;
      height: auto;
      margin-top: 10px;

      .police-icon {
        width: 16px;
        height: 16px;
        margin-top: 10px;
        margin-right: 13px;
        fill: var(--text-title-second);
      }

      .police-content {
        display: inline-flex;
        flex-wrap: wrap;
        width: 100%;
        height: auto;
        padding: 10px 0;
        background-color: var(--text-color-button);
        border-radius: 5px;
        box-shadow: var(--shadow-default);

        .select-man {
          display: inline-flex;
          flex-wrap: wrap;
          width: fit-content;

          .show-select-man {
            align-items: center;
            height: 30px;
            padding: 3px 8px 5px;
            margin-top: 4px;
            margin-bottom: 4px;
            margin-left: 10px;
            cursor: pointer;
            background-color: var(--button-color-guide-default);
            border-radius: 4px;

            span {
              max-width: 17rem;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
              vertical-align: middle;
            }
          }

          .search-key {
            display: inline-flex;
            flex-grow: 1;
            min-width: 80px;
            color: var(--text-title-second);
            background-color: var(--text-color-button);
          }
        }

        .search-content {
          width: 330px;
          height: auto;
          border-radius: var(--border-radius);
          box-shadow: var(--shadow-default);
        }
      }
    }

    .task-btn {
      padding: 5px;

      .left-btn {
        position: relative;
        left: 355px;
        width: 100px;
      }
    }

    .police-force-tree {
      flex: 1;
      overflow: hidden auto;
    }
  }

  .send-police-plus {
    max-height: 800px;
    overflow-y: auto;
  }
</style>
