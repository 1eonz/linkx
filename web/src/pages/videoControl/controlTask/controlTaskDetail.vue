<script lang="ts" setup>
  import { computed, onMounted, reactive, ref, watch } from 'vue';

  import {
    deleteDisableSuspectTask,
    deleteTrueTask,
    enableSuspectTask,
    queryVideoControlTaskDetail,
    stopSuspectTask,
  } from '@/api/videoControl';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import MessageBox from '@/components/MessageBox';
  import { useDC, useEmitter, useI18n } from '@/hooks';
  import { useMainStore } from '@/store';
  import { getIp } from '@/utils';

  import CreateControlTask from './createControlTask.vue';

  interface TaskInfo {
    cameraList: any[];
    creatorType: number;
    enable: number;
    endDateTime: string;
    logicType: number;
    name: string;
    remark: string;
    startDateTime: string;
    type: number;
    urls: string[];
  }

  const props = defineProps({
    isDbClick: {
      default: false,
      type: Boolean,
    },
    suspectTaskId: {
      default: null,
      type: [Number, String],
    },
  });
  const emit = defineEmits(['closeDialog']);

  const { t } = useI18n();
  const { updateVideoControlShowId } = useMainStore();
  const buttonsShow = ref(false);
  const taskInfo = reactive<TaskInfo>({
    cameraList: [],
    creatorType: 0,
    enable: 0,
    endDateTime: '',
    logicType: 0,
    name: '',
    remark: '',
    startDateTime: '',
    type: 0,
    urls: [],
  });

  const selectItem = computed(() => {
    return taskInfo.cameraList
      ? taskInfo.cameraList.map((item) => {
          return {
            category: item.category,
            code: item.cameraIsdn,
            id: item.cameraId,
            name: item.cameraName,
          };
        })
      : [];
  });
  const taskStatusColor = computed(() => {
    switch (taskInfo.enable) {
      case 1: {
        return 'yellow';
      }
      case 2: {
        return 'blue';
      }
      case 3: {
        return 'gray';
      }
      default: {
        return 'red';
      }
    }
  });
  const taskStatus = computed(() => {
    switch (taskInfo.enable) {
      case 1: {
        return t('videoControl.controlTask.processing');
      }
      case 2: {
        return t('videoControl.controlTask.paused');
      }
      case 3: {
        return t('videoControl.controlTask.over');
      }
      default: {
        return t('videoControl.controlTask.unstart');
      }
    }
  });
  const controlImageUrl = computed(() => {
    if (taskInfo.urls.length === 0) {
      return '';
    }
    return `${getIp()}/iap${taskInfo.urls[0]}`;
  });

  watch(
    () => props.suspectTaskId,
    (val) => {
      val && initTask();
    },
  );

  useDC('IAP', 'suspect_modify', (message) => {
    if (props.suspectTaskId === message.suspectTaskId) {
      initTask();
    }
  });
  useDC('IAP', 'suspect_enable', (message) => {
    if (props.suspectTaskId === message.suspectTaskId) {
      initTask();
    }
  });

  onMounted(() => {
    const { suspectTaskId } = props;
    initTask();

    useEmitter('closeLastControlCid', (id) => {
      if (suspectTaskId !== id) {
        closeCard();
      }
    });
    useEmitter('controlTaskUpdate', () => {
      initTask();
    });
  });

  async function initTask() {
    const { suspectTaskId } = props;
    if (!suspectTaskId) return;
    const param = { suspectTaskId };
    const { code, data } = await queryVideoControlTaskDetail(param);
    if (code === 0) {
      Object.keys(data).forEach((key) => {
        taskInfo[key] = data[key];
      });
      buttonsShow.value = taskInfo.creatorType !== 1;
    }
  }
  function updateControl() {
    Dialog({
      cid: 'createControlTask',
      content: CreateControlTask,
      data: {
        clickType: 'update',
        taskInfo,
      },
      offset: ['750px', '60px'],
    });
  }
  async function stopControl() {
    const param = {
      suspectTaskId: props.suspectTaskId,
    };
    const result = await stopSuspectTask(param);
    if (result.code === 0) {
      closeCard();
      useEmitter().emit('refresh');
      Message(t('videoControl.videoControlCommon.surveillanceStopped'));
    } else {
      Message(result.msg);
    }
  }
  async function startTask() {
    const param = {
      enable: true,
      suspectTaskId: props.suspectTaskId,
    };
    const result = await enableSuspectTask(param);
    if (result.code === 0) {
      closeCard();
      useEmitter().emit('refresh');
      Message(t('videoControl.videoControlCommon.surveillanceStart'));
    } else {
      Message(result.msg);
    }
  }
  async function deleteTask() {
    const param = {
      suspectTaskId: props.suspectTaskId,
    };
    const result = await deleteDisableSuspectTask(param);
    if (result.code === 0) {
      closeCard();
      useEmitter().emit('refresh');
      Message(t('videoControl.controlTask.successDeleted'));
    } else {
      Message(t('videoControl.controlTask.failedDeleted'));
    }
  }
  async function deleteTrueTaskBtn() {
    const affirm = await MessageBox({
      cancelText: t('planSafety.message.cancel'),
      confirmText: t('planSafety.message.delete'),
      offset: ['40%', '35%'],
      text: t('resource.jurisdiction.confirmDelete'),
    });
    if (!affirm) return;
    const param = {
      suspectTaskId: props.suspectTaskId,
    };
    const result = await deleteTrueTask(param);
    if (result.code === 0) {
      closeCard();
      useEmitter().emit('refresh');
      Message(t('videoControl.controlTask.successTrueDeleted'));
    } else {
      Message(t('videoControl.controlTask.failedDeleted'));
    }
  }
  async function timeOut() {
    const param = {
      enable: false,
      suspectTaskId: props.suspectTaskId,
    };
    const result = await enableSuspectTask(param);
    if (result.code === 0) {
      closeCard();
      useEmitter().emit('refresh');
      Message(t('videoControl.videoControlCommon.surveillanceTimeOut'));
    } else {
      Message(result.msg);
    }
  }
  function closeCard() {
    if (!props.isDbClick) {
      updateVideoControlShowId('');
      return;
    }
    emit('closeDialog');
  }
</script>

<template>
  <!-- 设防任务操作卡片 -->
  <TdFrameBox
    class="task-detail"
    :title="t('videoControl.controlTask.details')"
    @close-frame-box="closeCard"
  >
    <div class="control-detail-main">
      <div class="detail-item-title">
        <TdTag class="title-level" :label="taskStatus" :type="taskStatusColor" />
        <span class="title-text">{{ taskInfo.name }}</span>
      </div>
      <div class="detail-item-type blue-text">
        {{ t('videoControl.controlTask.fromType') }}
        {{
          taskInfo.type === 1
            ? t('videoControl.controlTask.carSurveillance')
            : taskInfo.type === 22
              ? t('videoControl.controlTask.temporaryFaceSurveillance')
              : t('videoControl.controlTask.personSurveillance')
        }}
      </div>
      <div class="detail-item-type">
        <Icon class="type-icon" name="category_time" />
        <div class="task-time">{{ taskInfo.startDateTime }}</div>
        &nbsp;-&nbsp;
        <div class="task-time">{{ taskInfo.endDateTime }}</div>
      </div>

      <div class="detail-item-type">
        <Icon class="type-icon" name="home_camera" />
        <div v-show="selectItem.length > 0" class="type-monitor">
          <div v-for="item in selectItem" :key="item.id" class="select-man">
            <span>{{ item.name }}</span>
          </div>
        </div>
      </div>

      <div v-if="taskInfo.logicType === 22" class="detail-item-type">
        <Icon class="type-icon" name="category_edit" />
        <div v-if="taskInfo.remark" class="type-remark">{{ taskInfo.remark }}</div>
        <span v-else>{{ t('videoControl.controlTask.NoRemarks') }}</span>
      </div>

      <div v-if="taskInfo.urls" class="detail-item-type">
        <Icon class="type-icon" name="category_image" />
        <img class="type-image" :src="controlImageUrl" />
      </div>

      <div v-if="buttonsShow" class="select-unit">
        <!--暂停设防-->
        <TdButton
          v-if="taskInfo.enable === 1 && taskInfo.logicType !== 22"
          class="btn"
          :text="t('videoControl.controlTask.timeOut')"
          type="normal"
          @click="timeOut"
        />
        <!--修改设防-->
        <TdButton
          v-if="taskInfo.logicType !== 22 && taskInfo.enable !== 3"
          class="btn"
          :text="t('videoControl.controlTask.modify')"
          type="normal"
          @click="updateControl"
        />
        <!--结束任务-->
        <TdButton
          v-if="taskInfo.enable === 1 || taskInfo.enable === 2"
          class="btn"
          :text="t('videoControl.controlTask.stop')"
          type="normal"
          @click="stopControl"
        />
        <!--开始设防-->
        <TdButton
          v-if="taskInfo.enable === 2"
          class="btn"
          :text="t('videoControl.controlTask.start')"
          type="normal"
          @click="startTask"
        />
        <!--未开始的任务--结束设防-->
        <TdButton
          v-if="taskInfo.enable === 0"
          class="btn"
          :text="t('videoControl.controlTask.stop')"
          type="normal"
          @click="deleteTask"
        />
        <TdButton
          class="btn"
          :text="t('videoControl.controlTask.deleteTask')"
          type="normal"
          @click="deleteTrueTaskBtn"
        />
      </div>
    </div>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  .task-detail {
    :deep(.frame-box-container) {
      background: linear-gradient(180deg, rgba(6 41 74 / 64%) 0%, rgba(6 41 74 / 26%) 100%);
      backdrop-filter: blur(8px);
    }
  }

  .control-detail-main {
    padding: 10px 0;

    .detail-item-title {
      .title-level {
        margin-right: 4px;
      }

      .title-text {
        font-size: 14px;
        font-weight: bold;
        line-height: 20px;
        color: var(--text-title-first);
        word-break: break-all;
      }
    }

    .detail-item-type {
      display: flex;
      align-items: center;
      max-height: 200px;
      overflow: auto;
      font-size: 14px;

      .type-icon {
        width: 26px;
        height: 26px;
        padding-right: 8px;
        fill: var(--icon-color-default);
      }

      .task-time {
        font-size: 12px;
      }

      .type-remark {
        width: calc(100% - 28px);
        max-height: 200px;
        overflow: auto;
      }

      .type-image {
        display: inline-block;
        width: 120px;
        height: 120px;
      }
    }

    .type-monitor {
      flex-wrap: wrap;
      width: 100%;
      max-height: 200px;
      overflow-y: scroll;

      .select-man {
        align-items: center;

        span {
          max-width: 17rem;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          vertical-align: middle;
        }
      }
    }

    .select-unit {
      display: flex;
      justify-content: center;
      margin-top: 10px;

      .btn {
        margin-right: 8px;
      }
    }

    .blue-text {
      color: var(--text-title-second);
    }
  }
</style>
