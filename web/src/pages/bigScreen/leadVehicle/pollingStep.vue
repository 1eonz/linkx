<script setup lang="ts">
  import { computed, onBeforeUnmount, onMounted, ref, unref, watch } from 'vue';

  import { queryVideoPollingByParam } from '@/api/monitor';
  import { Message } from '@/components/Message';
  import appConfig from '@/config/appConfig';
  import { useI18n, useSetInterval, useUtils } from '@/hooks';
  import { startPlanSafety } from '@/pages/bigScreen/planSpecial/vehicle';
  import { distanceBePoints } from '@/plugins/map';
  import { usePlanStore, useVehicleStore, useVideoPollStore } from '@/store';

  import { debounce } from 'lodash-es';

  const { t } = useI18n();
  const planStore = usePlanStore();
  const vehicleStore = useVehicleStore();
  const videoPollStore = useVideoPollStore();

  const active = ref<number[]>([]);
  const auto = ref(true);
  const stepData = ref<any>([]);
  let clearTimer: any;
  const listRef = ref();

  const showStep = computed(() => vehicleStore.underProtection);

  watch(active, (val) => {
    if (val.length > 0) {
      videoPollStore.setPlayingPoll(unref(stepData)[val[0]]);
    }
  });

  onMounted(async () => {
    await init();
    if (unref(showStep) && useUtils().isSpecial) {
      startPlanSafety(true);
    }
  });

  onBeforeUnmount(() => {
    destroy();
  });

  const queryList = debounce(async () => {
    const { videoPollingGroup } = planStore.planData;
    if (!videoPollingGroup) {
      return;
    }

    const groupIds = videoPollingGroup?.split(',') || [];
    const param = {
      groupIds,
      isdn: appConfig.isdn,
      pageSize: 9999,
      start: 1,
    };

    const { code, data } = await queryVideoPollingByParam(param);
    if (code === 0) {
      data.records.forEach((item) => {
        const index = groupIds.indexOf(item.id);
        stepData.value[index] = item;
      });
      autoPolling();
    }
  }, 500);

  async function init() {
    auto.value = true;
    await queryList();
  }

  function destroy() {
    auto.value = false;
    active.value = [];
    stepData.value = [];
    clearTimer?.();
  }

  function handleTurn(direction) {
    const { scrollLeft } = listRef.value;
    if (direction === 'left') {
      if (scrollLeft > 0) {
        listRef.value.scrollLeft = scrollLeft - 10;
      }
    } else {
      listRef.value.scrollLeft = scrollLeft + 10;
    }
  }

  function handleClick(index) {
    if (unref(auto)) {
      auto.value = false;
      Message({
        message: t('resource.poll.pollModeSwitchedToManualMode'),
      });
    }

    active.value = [index];

    clearTimer?.();
    const { videoList } = unref(stepData)[index];
    videoPollStore.updateVideoList(videoList);
  }

  function handleAuto() {
    auto.value = !auto.value;
    if (unref(auto)) {
      autoPolling();
    } else {
      clearTimer?.();
    }
  }

  // 自动根据是否在头车的范围判断当前播放那个轮巡组（如果多个都在范围内则播放多个）
  function autoPolling() {
    clearTimer?.();
    clearTimer = useSetInterval(() => {
      const { headerCar, underProtection } = vehicleStore;
      if (!underProtection) {
        return;
      }

      const points = unref(stepData).map((i) => i.location.split(',').map(Number));
      const current: number[] = [];
      const videoList: any[] = [];

      points.forEach((item, index) => {
        // 连云港设备上传的坐标系和地图的一样
        // 家里不一样（需要wgs84togcj02转）
        // const dis = distanceBePoints(wgs84togcj02(headerCar.position), item);
        const { position } = headerCar || {};
        if (!position) {
          return;
        }

        const dis = distanceBePoints(position, item);
        if (dis <= 100) {
          current.push(index);
          unref(stepData)[index].videoList.forEach((v) => {
            const index = videoList.findIndex((i) => i.id === v.id);
            if (index === -1) {
              videoList.push(v);
            }
          });
        }
      });

      // 如果当前没有任何匹配的就放之前的
      // 如果结果和之前一样就保持不变
      if (current.length > 0 && JSON.stringify(unref(active)) !== JSON.stringify(current)) {
        active.value = current;
        videoPollStore.updateVideoList(videoList);
      }
    }, 1 * 1000);
  }
</script>

<template>
  <div v-show="showStep && stepData.length > 0" class="polling-step">
    <Icon class="turn left" name="drop_up" @click="handleTurn('left')" />
    <div ref="listRef" class="list">
      <div
        v-for="(item, index) in stepData"
        :key="index"
        class="step"
        :class="{ done: index < active[0], active: active.includes(index) }"
      >
        <div class="num-line">
          <div class="line" :class="{ space: index === 0 }"></div>
          <div class="num" @click.stop="handleClick(index)"> {{ item.count }}</div>
          <div class="line" :class="{ space: index + 1 === stepData.length }"></div>
        </div>
        <TdTooltip :content="item.address">
          <span class="address">{{ item.address }}</span>
        </TdTooltip>
      </div>
    </div>
    <Icon class="turn right" name="drop_down" @click="handleTurn('right')" />

    <TdButton
      class="btn"
      :text="auto ? t('resource.poll.turnOffAutoPoll') : t('resource.poll.startAutoPoll')"
      type="normal"
      @click="handleAuto"
    />
  </div>
</template>

<style scoped lang="less">
  @import url('@/styles/mixin.less');

  .polling-step {
    display: flex;
    align-items: center;
    max-width: 50%;
    padding: 0 10px;
    background: rgb(0 0 0 / 60%);

    .list {
      display: flex;
      align-items: center;
      overflow-x: auto;
    }

    .turn {
      width: 36px;
      height: 36px;
      margin-right: 10px;
      cursor: pointer;
      transform: rotate(-90deg);

      &.right {
        margin-left: 10px;
      }
    }

    .step {
      display: flex;
      flex-direction: column;
      padding: 8px 0;

      .num-line {
        display: flex;
        align-items: center;

        .num {
          width: 24px;
          height: 24px;
          font-size: 14px;
          line-height: 24px;
          color: #000;
          text-align: center;
          cursor: pointer;
          background: #fff;
          border-radius: 50%;
        }

        .line {
          width: 30px;
          height: 2px;
          background: #fff;
        }

        .space {
          opacity: 0;
        }
      }

      .address {
        display: inline-block;
        width: 84px;
        margin-top: 8px;
        font-size: 12px;
        line-height: 12px;
        text-align: center;

        .ellipsis1();
      }
    }

    .done {
      .num-line {
        .num {
          color: #fff;
          background: #00c791;
        }
      }

      .address {
        color: #00c791;
      }
    }

    .active {
      .num-line {
        .num {
          color: #fff;
          background: #fca701;
        }
      }

      .address {
        color: #fca701;
      }
    }

    .btn {
      position: absolute;
      top: 64px;
      left: 50%;
      transform: translateX(-50%);
    }
  }
</style>
