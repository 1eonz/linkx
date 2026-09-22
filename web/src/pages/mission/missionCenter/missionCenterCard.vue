<script lang="ts" setup>
  import { computed, onBeforeUnmount, onMounted, ref, unref, watch, watchEffect } from 'vue';
  import { useRoute } from 'vue-router';

  import { flowMissionDetail, flowMissionProcessed } from '@/api/mission';
  import { Dialog } from '@/components/Dialog';
  import { useDC, useEmitter, useI18n } from '@/hooks';
  import eventAndMissionUtil from '@/pages/mission/eventAndMissionUtil';
  import { useMissionStore } from '@/store';

  import DisposalProcess from './disposalProcess.vue';
  import CurrentNode from './missionAxis/axisNode/currentNode.vue';
  import MissionDetails from './missionDetails.vue';

  const props = defineProps<{
    clickItem?: boolean;
    eventType?: string;
    flowId: string;
    title?: string;
  }>();
  const emit = defineEmits(['isShowCenterCardClose', 'closeDialog']);

  const { t } = useI18n();
  const route = useRoute();
  const missionStore = useMissionStore();

  const activeTab = ref('current');
  const componentList = ref([
    {
      id: 'current',
      isActive: true,
      name: t('mission.missionList.currentOperation'),
    },
    {
      id: 'disposalProcess',
      isActive: false,
      name: t('mission.missionList.disposalProcess'),
    },
  ]);
  const missionInfo = ref<any>({});
  const process = ref([]);

  const showOperation = computed(() => {
    const { status } = unref(missionInfo);
    const arr = ['ACCESSED', 'SIGNED', ...missionStore.getStatus.finish];
    return !arr.includes(status);
  });

  watch(route, () => {
    Dialog(`missionCard${props.flowId}`)?.close();
  });

  watchEffect(() => {
    if (props.flowId) {
      queryMissionDetail();
    }
  });

  // 更新任务
  useDC('MISSION', 'upsert', (data) => {
    if (data.flowId === props.flowId) {
      queryMissionDetail();
    }
  });

  onMounted(() => {
    // 任务派发成功
    useEmitter('sendPoliceSuccess', closeCard);
    // 任务忽略成功
    useEmitter('closeMissionCard', () => {
      closeCard();
      missionStore.queryMissionData();
    });
    useEmitter('closeMinListCard', (data) => {
      // 接收到最小化窗口列表的关闭消息 这里因为有些界面关闭的时候有相关操作 所以使用消息关闭
      if (data.id === props.flowId) {
        close();
      }
    });
  });

  onBeforeUnmount(() => {
    eventAndMissionUtil.closeCard();
  });

  function closeCard() {
    emit('isShowCenterCardClose');
    close();
  }

  function tabChange(data) {
    activeTab.value = data.id;
  }

  function close() {
    emit('closeDialog');
    Dialog('warningNoticeCenter')?.close();
  }

  // 获取任务详情
  async function queryMissionDetail() {
    const param = {
      id: props.flowId,
    };
    const { code, data } = await flowMissionDetail(param);
    if (code === 0) {
      const info = eventAndMissionUtil.evenAndMissionDataInit([data])[0];
      missionInfo.value = info;
      getFlowMissionProcessed();
    }
  }

  // 获取处置过程
  async function getFlowMissionProcessed() {
    const { flowId } = unref(missionInfo);
    const { code, data } = await flowMissionProcessed(flowId);
    if (code === 0) {
      process.value = data;
    }
  }
</script>

<template>
  <div class="mission-center-card">
    <!-- 任务详情 -->
    <TdFrameBox
      :dragger="false"
      :title="t('mission.missionList.missionDetail')"
      @close-frame-box="closeCard"
    >
      <div class="mission-center">
        <MissionDetails :info="missionInfo" />
      </div>
    </TdFrameBox>

    <!-- 操作处理框 -->
    <div class="mission-axis-tab ground-glass">
      <TdTab class="tabs-box" :data="componentList" tab-style="center" @click="tabChange" />

      <div class="mission-axis-box">
        <CurrentNode
          v-if="activeTab === 'current' && showOperation"
          class="mission-axis-current"
          :info="missionInfo"
        />
        <DisposalProcess
          v-if="activeTab === 'disposalProcess'"
          :flow-type="missionInfo.type"
          :process="process"
        />
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .mission-center-card {
    position: relative;
    display: flex;
    flex-direction: column;
    width: 310px;
    height: calc(100vh - 124px);
    overflow-y: auto; // 兼容-如果异常也能不影响使用
    font-size: 14px;

    .mission-center {
      padding-bottom: 2px;

      .task-new {
        padding: 10px 0;

        & > span:first-child {
          display: inline-block;
          min-width: 56px;
          margin: 0 4px 0 10px;
          color: var(--text-title-second);
        }
      }

      .event-part {
        box-sizing: border-box;
        display: flex;
        flex-direction: column;

        .event-object {
          height: auto;
        }
      }
    }

    .mission-axis-tab {
      display: flex;
      flex: 1;
      flex-direction: column;
      height: 0;
      margin-top: 10px;

      .tabs-box {
        font-size: var(--font-size-medium);
        cursor: pointer;
      }

      .mission-axis-box {
        flex: 1;
        overflow: hidden auto;

        .mission-axis-current {
          height: 100%;
        }
      }
    }
  }
</style>
