<script lang="ts" setup>
  import { computed, onMounted, ref, unref, watch } from 'vue';
  import { useRoute } from 'vue-router';

  import { Message } from '@/components/Message';
  import { CategoryEnum } from '@/enums';
  import { useEmitter, useI18n } from '@/hooks';
  import MonitorCard from '@/pages/communicationCard/monitorCard/monitorCard.vue';
  import { removeDragElement } from '@/pages/communicationCenter/common';
  import MonitorOperate from '@/pages/communicationCenter/components/monitorOperate.vue';
  import { getAccountByEquipmentData, getInfoByAccount } from '@/pages/resource/resourceHelper';
  import {
    useCommunicateDispatchStore,
    useCommunicationStore,
    useTreeStore,
    useVideoPollStore,
  } from '@/store';
  import { delay } from '@/utils';
  import { isArray } from '@/utils/is';

  import Draggable from 'vuedraggable';

  import { Timeout } from '#/index';

  const props = defineProps<{
    loadingFlag?: boolean;
    monitorCenter?: boolean; // 是否为监控调阅中心
    synthesizeFlag?: boolean; // 是否为综合屏
  }>();
  const emit = defineEmits(['closeAll']);

  const { t } = useI18n();
  const route = useRoute();
  const communicateDispatchStore = useCommunicateDispatchStore();
  const communicationStore = useCommunicationStore();
  const treeStore = useTreeStore();
  const videoPollStore = useVideoPollStore();

  const monitorActive = ref<any>({}); // 单击选中摄像头或者记录仪
  const monitorRef = ref();
  const showFlag = ref(false); // 云台控制，语音按钮等要视频监控成功之后才能操作
  const distributeBtnActive = ref(false);
  const monitorActiveIndex = ref(-1); // 单击选中摄像头或者记录仪 -1都不选
  const monitorOperate = ref();
  const isFull = ref(false);
  let timer: Timeout;
  let beforeDragTime = 0;

  const pollTimer = computed(() => videoPollStore.clearPollTimer);
  const layoutActive = computed(() => communicateDispatchStore.layoutMonitor);
  const showData = computed(() => {
    const { monitorCenter, synthesizeFlag } = props;

    // 如果没有分屏，但是点开了投屏 就会导致摄像头重复渲染
    if (route.name === 'monitorCenter' && !monitorCenter) {
      return [];
    }

    // 同上
    if (route.name === 'communicateCenter' && monitorCenter) {
      return [];
    }

    const { monitorDesktopList, projectionCenter } = communicateDispatchStore;
    let ret = [...monitorDesktopList];
    if (projectionCenter && !synthesizeFlag && !monitorCenter) {
      ret = [];
    }

    // 轮巡时会有预播放
    if (videoPollStore.clearPollTimer) {
      const curr: any = [];
      const after: any = [];
      ret.forEach((item) => {
        if (item) {
          if (item.ahead) {
            after.push(item);
          } else {
            curr.push(item);
          }
        }
      });
      const _curr = curr.slice(0, 4);
      const _ret = [..._curr, ...Array.from({ length: 4 - _curr.length }).fill(null), ...after];
      return _ret;
    }

    return synthesizeFlag ? ret.slice(0, 3) : ret.slice(0, unref(layoutActive).id);
  });

  watch(
    () => videoPollStore.clearPollTimer,
    (val) => {
      if (val) {
        layoutChange({
          iconName: 'layout_2',
          id: 4,
          span: 12,
        });
      }
    },
  );
  watch(
    showData,
    (val) => {
      if (val.filter(Boolean).length === 0) {
        monitorActive.value = {};
        return;
      }
      // 默认选中最后一个
      let index = -1;
      val.forEach((j, k) => {
        if (j) index = k;
      });
      if (index !== -1) {
        monitorActive.value = val[index];
        monitorActiveIndex.value = index;
      }
    },
    { immediate: true },
  );

  useEmitter('monitorDesktopPlay', monitorDesktopPlay);

  onMounted(() => {
    communicateDispatchStore.initLayoutMonitor();
  });

  async function monitorDesktopPlay() {
    const { resourceOneKeyType } = treeStore;
    const { ballCamera, carPhoto, GBRecorder, monitor, person, recorder, terminal, uav } =
      treeStore.resourceCheckedList;
    if (resourceOneKeyType === 'VideoWatch') {
      const list = [
        ...terminal,
        ...recorder,
        ...monitor,
        ...GBRecorder,
        ...carPhoto,
        ...uav,
        ...ballCamera,
      ];
      if (isArray(person)) {
        for (const p of person) {
          for (const a of p.serviceAccounts) {
            const info = await getInfoByAccount(a.account);
            const index = list.findIndex((i) => i.id === info.id);
            if (index === -1) {
              list.push(info);
            }
          }
        }
      }

      if (list.length > 0) {
        const ret = list.filter((i) => {
          const account = getAccountByEquipmentData(i);
          return !!account;
        });

        if (ret.length === 0) {
          Message(t('communication.communicationTips.communicationAccountNotAssociated'));
          return;
        }
        playVideoWatch(ret);
      }
    }
  }

  function onFullScreen(data) {
    isFull.value = data;
  }

  function getColNum(index) {
    const { id, span } = unref(layoutActive);
    const num1 = index === 0 ? 16 : span;
    const num2 = id === 6 ? num1 : span;
    return props.synthesizeFlag ? 8 : num2;
  }

  async function dragAdd(e) {
    removeDragElement();

    if (e.pullMode !== 'clone') {
      return;
    }

    const now = Date.now();
    if (now - beforeDragTime < 500) {
      Message({
        message: t('common.frequentOperation'),
        type: 'warning',
      });
      return;
    }
    beforeDragTime = now;

    if (!communicationStore.hasComm) {
      Message(t('resource.resourceMsg.communicationAbnormal'));
      return;
    }

    const data = JSON.parse(e.from.dataset.info);
    const dragIndex = e.to.dataset.index;
    const relShowData = unref(showData).filter(Boolean);
    if (relShowData.length >= 16) {
      Message(`${t('monitor.monitorTips.maxPlay')} 16`);
      return;
    }

    const { category, id, name } = data || {};
    const account = getAccountByEquipmentData(data);
    if (!account) {
      Message(t('communication.communicationTips.communicationAccountNotAssociated'));
      return;
    }

    if (
      [
        CategoryEnum.ballCamera,
        CategoryEnum.carPhoto,
        CategoryEnum.GBRecorder,
        CategoryEnum.monitor,
        CategoryEnum.recorder,
        CategoryEnum.terminal,
        CategoryEnum.uav,
      ].includes(Number(category))
    ) {
      // 摄像头或记录仪
      const showDataArr = [...unref(showData)];
      if (relShowData.some((item) => item.id === id)) {
        // 列表已存在该资源
        Message(t('monitor.monitorTips.hasBeenOpenedOnList'));
      } else {
        if (communicationStore.comm[account]) {
          Message({
            message: name + t('monitor.monitorTips.theCardHasBeenOpened'),
            type: 'warning',
          });
          return;
        }
        closeMonitor(showDataArr[dragIndex]);
        setTimeout(() => {
          communicateDispatchStore.addMonitorList(data, dragIndex);
        }, 1000);
      }
    } else {
      Message(t('desktop.other.notSupported'));
    }
  }

  function dragEnd(e) {
    const fromIndex = e.from.dataset.index;
    const toIndex = e.to.dataset.index;
    if (fromIndex !== toIndex) {
      const { monitorDesktopList } = communicateDispatchStore;
      const arr = [...monitorDesktopList];
      [arr[fromIndex], arr[toIndex]] = [arr[toIndex], arr[fromIndex]];
      communicateDispatchStore.addMonitorList(arr);
    }
  }

  // 分发弹框关闭出发事件
  function closeDistributeVideo() {
    distributeBtnActive.value = false;
  }

  // 获取指定ref的index
  function getRefIndex(item, index?: number) {
    if (!item) return;

    if (!index && index !== 0) {
      index = monitorActiveIndex.value;
    }
    const newIndex = [...unref(showData)].slice(0, index).filter((child) => {
      return (
        child &&
        [
          CategoryEnum.carPhoto,
          CategoryEnum.GBRecorder,
          CategoryEnum.monitor,
          CategoryEnum.recorder,
        ].includes(child)
      );
    }).length;
    return newIndex;
  }

  function handleClick(item, index) {
    clearTimeout(timer);
    timer = setTimeout(() => {
      clickMonitor(item, index);
    }, 300);
  }

  // 单击选中
  function clickMonitor(item, index) {
    if (!item || unref(isFull)) return;
    if (item.id === unref(monitorActive).id) {
      return;
    }
    monitorActive.value = { ...item };
    monitorActiveIndex.value = index;
    const newIndex = getRefIndex(item, index) || 0;
    showFlag.value = monitorRef.value?.[newIndex]?.lineTimeShow;
  }

  function handleDbClick(item, index) {
    clearTimeout(timer);
    monitorActive.value = { ...item };
    monitorActiveIndex.value = index;
  }

  // 视频分发
  function distributeVideo(item, index?: number) {
    distributeBtnActive.value = true;
    const { account, code } = item;
    const isdn = account || code;
    const newIndex = getRefIndex(item, index) || 0;
    monitorRef.value[newIndex]?.distributeVideo(isdn);
  }

  function layoutChange(item) {
    communicateDispatchStore.setLayoutMonitor(item);
    communicateDispatchStore.addMonitorList(
      [...communicateDispatchStore.monitorDesktopList].filter(Boolean),
    );
  }

  function changeShowFlag(flag) {
    showFlag.value = flag;
  }

  function closeAll() {
    emit('closeAll');
  }

  function closeMonitor(data) {
    if (data) {
      communicateDispatchStore.deleteMonitor(data);
    }
  }

  // 一键调阅
  async function playVideoWatch(addData) {
    const { monitorDesktopList } = communicateDispatchStore;
    // 原来的
    const emptyIndex: number[] = [];
    const oldList = monitorDesktopList.filter((item, index) => {
      if (!item) {
        emptyIndex.push(index);
      }
      return item;
    });
    if (oldList.length >= 16) {
      Message(`${t('monitor.monitorTips.maxPlay')} 16`);
      return;
    }

    // 新加入
    const relAddData: any = [];
    addData.forEach((data) => {
      const { account, code, id, name } = data;
      const has = oldList.findIndex((item) => item.id === id);
      if (has === -1) {
        if (communicationStore.comm[account || code]) {
          Message({
            message: name + t('monitor.monitorTips.theCardHasBeenOpened'),
            type: 'warning',
          });
        } else {
          relAddData.push(data);
        }
      }
    });

    if (relAddData.length === 0) {
      Message(t('monitor.monitorTips.hasBeenOpenedOnList'));
      return;
    }

    // 同时拉多路视频会存在拉不起的现象，间隔拉会提升拉起成功率
    for (const [i, item] of relAddData.entries()) {
      if (oldList.length + i >= 16) break;
      // 新圈选的首先替换空白处
      if (emptyIndex.length > 0) {
        communicateDispatchStore.addMonitorList(item, emptyIndex.shift());
      } else {
        communicateDispatchStore.addMonitorList(item);
      }
      await delay(500);
    }
  }
</script>

<template>
  <!-- 监控桌面 -->
  <div
    v-loading="loadingFlag"
    class="monitor-desktop"
    :class="{ 'synthesize-monitor': synthesizeFlag }"
  >
    <ElRow
      class="monitor-desktop-box"
      :class="`monitor-desktop-box-${synthesizeFlag ? 0 : layoutActive.id}`"
      :gutter="0"
    >
      <ElCol
        v-for="(item, index) in showData"
        :key="item?.id || `${index}`"
        :class="`el-cols-${synthesizeFlag ? 0 : layoutActive.id} ${
          pollTimer && index > 3 ? 'hide-el-col' : ''
        }`"
        :span="getColNum(index)"
        @click="handleClick(item, index)"
        @dblclick="handleDbClick(item, index)"
      >
        <Draggable
          class="base-drag ground-glass"
          :data-index="index"
          group="bigDrag"
          item-key="id"
          :list="[item || {}]"
          @add="dragAdd"
          @end="dragEnd"
        >
          <template #item>
            <div
              v-if="item"
              class="monitor-item"
              :class="item.id === monitorActive.id ? 'monitor-item-active' : ''"
            >
              <MonitorCard
                ref="monitorRef"
                :class="synthesizeFlag ? 'drag-can-small' : `col-item-${layoutActive.span}`"
                :monitor-info="item"
                :mute="index !== unref(monitorActiveIndex)"
                @change-show-flag="changeShowFlag"
                @close-card="() => closeMonitor(item)"
                @close-distribute-video="closeDistributeVideo"
                @on-full-screen="onFullScreen"
              />
            </div>
            <div
              v-else
              class="col-item"
              :class="synthesizeFlag ? 'drag-can-small' : `col-item-${layoutActive.span}`"
            >
              <TdEmpty
                :has-bg="true"
                :icon-name="index === showData.length - 1 ? 'drag_icon' : ''"
                :title="monitorCenter ? '' : t('common.tdcomp.drag')"
              />
              <TdCorner />
            </div>
          </template>
        </Draggable>
      </ElCol>
    </ElRow>

    <div
      v-if="communicateDispatchStore.projectionCenter && !synthesizeFlag && !monitorCenter"
      class="show-ing"
    >
      <div>
        <Icon class="icon" name="projection_center" prefix="bigScreen" />
        <div class="showing-title">{{ t('monitor.playManagement.playing') }}</div>
      </div>
    </div>

    <MonitorOperate
      v-if="!synthesizeFlag"
      ref="monitorOperate"
      :distribute-flag="distributeBtnActive"
      :info="monitorActive"
      :layout-active="layoutActive"
      :monitor-center="monitorCenter"
      :synthesize-flag="synthesizeFlag"
      @click-monitor="clickMonitor"
      @close-all="closeAll"
      @distribute-video="distributeVideo"
      @layout-change="layoutChange"
    />
  </div>
</template>

<style lang="less" scoped>
  .monitor-desktop {
    position: relative;
    box-sizing: border-box;
    width: 100%;
    height: 100%;
    overflow: hidden;

    .monitor-desktop-box {
      height: calc(100% - 76px);
      padding-bottom: 9px;
      overflow: hidden;
    }

    .monitor-desktop-box-6 {
      display: block;
    }

    .show-ing {
      position: absolute;
      top: 0;
      z-index: 2;
      width: 100%;
      height: calc(100% - 76px);
      background: rgb(0 0 0 / 40%);
      backdrop-filter: blur(10px);
      border: 1px solid rgb(50 152 226 / 100%);

      & > div {
        position: absolute;
        top: 50%;
        left: 50%;
        width: 300px;
        text-align: center;
        transform: translate(-50%, -50%);

        .icon {
          width: 96px;
          height: 96px;
          margin: auto;
        }

        .showing-title {
          margin-top: 12px;
          font-size: 24px;
          font-weight: 500;
          line-height: 34.75px;
          color: rgb(255 255 255 / 100%);
          letter-spacing: 0;
        }
      }
    }

    .col-item {
      position: relative;
      font-size: 20px;
      color: black;
    }

    .base-drag {
      width: 100%;
      height: 100%;
    }

    .el-col {
      height: 100%;

      .monitor-item {
        position: relative;
        height: 100%;
      }

      .monitor-item-active {
        border: 1px solid rgb(26 255 251 / 100%);
        box-shadow: 0 1px 4px rgb(0 199 145 / 50%);
      }

      .col-item {
        height: 100%;
      }

      :deep(.empty-bg) {
        border: none;
      }
    }

    .el-col-12 {
      height: 50%;

      &:first-child .base-drag {
        border-right: none;
      }

      &:nth-child(3) .base-drag {
        border-right: none;
      }
    }

    .el-col-8 {
      height: 33.33%;

      &:nth-child(3n + 1) .base-drag {
        border-right: none;
      }

      &:nth-child(3n) .base-drag {
        border-left: none;
      }

      &:nth-child(n + 7) .base-drag {
        border-top: none;
      }
    }

    .el-col-6 {
      height: 25%;

      &:nth-child(4n + 1) .base-drag,
      &:nth-child(4n + 2) .base-drag {
        border-right: none;
      }

      &:nth-child(4n) .base-drag {
        border-left: none;
      }

      &:nth-child(n + 13) .base-drag {
        border-top: none;
      }
    }

    .el-cols-6 {
      float: left;
      width: 100%;

      &:first-child {
        height: 66.65%;
      }

      &:nth-child(2) .base-drag {
        border-left: none;
      }
    }

    .hide-el-col {
      opacity: 0;
    }

    .drag-can-small {
      height: 100%;

      :deep(.big-screen-video) {
        .section {
          height: calc(100% - 22px);
        }
      }
    }
  }

  .synthesize-monitor {
    box-sizing: border-box;
    height: calc(100% - 32px);
    padding: 15px 12px;
    border: none;

    .el-col {
      height: 100%;

      .base-drag {
        border: 1px solid rgb(50 152 226 / 100%);
      }
    }

    .monitor-desktop-box {
      width: 100%;
      height: 100%;
    }

    :deep(.video-popup-container) {
      min-width: 0;
      min-height: 0;
    }

    :deep(.video-box) {
      min-width: 0;
      min-height: 0 !important;
    }

    :deep(.video-call-volume) {
      min-width: 0;
      min-height: 0;
    }
  }
</style>
