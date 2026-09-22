<script lang="ts" setup>
  import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
  import { useRoute } from 'vue-router';

  import { getRateLimiting } from '@/api/speed';
  import { CategoryEnum } from '@/enums';
  import { useBaseData, useI18n, useSetInterval, useUtils } from '@/hooks';
  import SpeedAlarmList from '@/pages/alarmRecord/speedAlarmList.vue';
  import { useMonitorStore, useResourceStore } from '@/store';

  const { t } = useI18n();
  const route = useRoute();
  const monitorStore = useMonitorStore();
  const resourceStore = useResourceStore();

  const listData = ref<any[]>([]);
  let clearTimer: any;

  const isAlarm = computed(() => {
    return listData.value.length > 0;
  });
  const isShow = computed(() => {
    return monitorStore.allBtnShowType === 'speedType';
  });

  watch(
    () => resourceStore.speedAlarmTime,
    (newVal, oldVal) => {
      if (newVal !== oldVal) {
        refreshSpeedAlarmList();
      }
    },
  );

  watch(route, () => {
    const { isCommand, isTask } = useUtils();
    if (isCommand || isTask) {
      return;
    }
    monitorStore.setAllBtnShowType('');
  });

  onMounted(() => {
    refreshSpeedAlarmList();
  });

  onBeforeUnmount(() => {
    clearTimer?.();
  });

  async function refreshSpeedAlarmList() {
    clearTimer?.();

    // 超速列表默认15秒定时刷新
    const { code, data } = await getRateLimiting();
    let t = 15;
    let speed = 1;
    if (code === 0 && data) {
      const { refreshCycle, triggerThreshold } = JSON.parse(data);
      t = refreshCycle;
      speed = triggerThreshold;
    }
    clearTimer = useSetInterval(() => {
      getSpeedAlarmList(speed);
    }, t * 1000);
  }

  function getSpeedAlarmList(speed) {
    const arr: any = [];
    const { getResourceOrigin } = useBaseData();
    const origin = getResourceOrigin;
    Object.keys(origin).forEach((key) => {
      if ([CategoryEnum.monitor, CategoryEnum.person, CategoryEnum.seat].includes(Number(key))) {
        return;
      }
      origin[key].forEach((item) => {
        if (Number(item.scoped) > Number(speed)) {
          arr.push(item);
        }
      });
    });
    listData.value = arr;
  }

  function clickSpeedTool() {
    monitorStore.setAllBtnShowType('speedType');
  }

  function closeFrame() {
    monitorStore.setAllBtnShowType('');
  }
</script>

<template>
  <div class="speed-manage-tools">
    <div :class="isAlarm ? 'tip-icon' : ''">
      <TdTooltip :content="t('homePage.mapToolData.speed')" placement="left">
        <div class="tool-btn" :class="isShow ? 'active' : ''" @click="clickSpeedTool">
          <Icon class="icon-tool" name="speed" />
        </div>
      </TdTooltip>
    </div>

    <ElIcon v-if="isShow" class="caret-right">
      <CaretRight />
    </ElIcon>

    <teleport to="body">
      <TdFrameBox
        v-if="isShow"
        class="frame-box"
        :title="t('homePage.menus.speedAlarmList')"
        @close-frame-box="closeFrame"
      >
        <SpeedAlarmList :data="listData" />
      </TdFrameBox>
    </teleport>
  </div>
</template>

<style lang="less" scoped>
  .frame-box {
    position: fixed;
    right: 98px;
    bottom: 50px;
    z-index: 101;
    height: calc(100vh - 124px);
  }

  .frame-box-bigScreen {
    position: fixed;
    top: 94px;
    right: 94px;
    z-index: 100;
    height: calc(100vh - 124px);
  }

  .speed-manage-tools {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: flex-end;
    margin-top: 4px;
    font-size: var(--font-size-small);
    border-radius: 50px/50px;

    .tip-icon {
      &::after {
        position: absolute;
        top: -5px;
        right: -5px;
        width: 12px;
        height: 12px;
        content: '';
        background: rgb(255 96 96);
        border-radius: 50%;
      }
    }

    .tool-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      cursor: pointer;
      background: rgb(6 41 74 / 80%);
      border: 1px solid rgb(50 152 226 / 100%);
      box-shadow: inset 0 0 16px 1px rgb(14 111 179 / 100%);

      .icon-tool {
        width: 20px;
        height: 20px;
        fill: var(--icon-color-default);
      }

      &:hover {
        .icon-tool {
          fill: #fff;
        }
      }
    }

    .active {
      background: var(--button-color-special-active);
      border: none;
      box-shadow: none;

      .icon-tool {
        fill: var(--icon-color-click) !important;
      }
    }

    .caret-right {
      position: absolute;
      left: -15px;
      color: var(--button-color-normal-default);
    }
  }
</style>
