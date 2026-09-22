<script lang="ts" setup>
  import { computed, nextTick, ref, watch, watchEffect } from 'vue';

  import { useI18n } from '@/hooks';
  import { getAlarmType } from '@/pages/alarmRecord/alarmCommon';
  import dateUtil from '@/utils/dateUtil';

  import dayjs from 'dayjs';

  import { Timeout } from '#/index';

  const props = defineProps<{
    full?: Boolean;
    size?: any;
    trackData: any[];
    warnList?: any[];
  }>();
  const emit = defineEmits(['clickItem']);
  defineExpose({ updateTimeAxis });

  const { t } = useI18n();

  const activeId = ref('');
  const trackScrollRef = ref();
  let mouseOut = true;
  let mouseOutTimer: Timeout;
  let currentIndex = 0;

  const axisList = computed(() => {
    const { trackData, warnList = [] } = props;
    const list: any[] = [];
    const getTimeStr = (time) => {
      const ymd = dayjs(time).format('YYYY-MM-DD');
      const hms = dayjs(time).format('HH:mm:ss');
      return `${dateUtil.dataToYesterday(ymd)} ${hms}`;
    };

    if (trackData.length === 0) {
      return [];
    }

    warnList.forEach((item) => {
      const { occurredTime } = item;
      list.push({
        ...item,
        time: occurredTime,
        timeStr: getTimeStr(occurredTime),
        warning: true,
      });
    });

    trackData.forEach((item) => {
      const { gmtCreated } = item;
      list.push({
        ...item,
        time: dayjs(gmtCreated).valueOf(),
        timeStr: getTimeStr(gmtCreated),
        track: true,
      });
    });

    const sortArr = list.sort((a, b) => a.time - b.time);
    const ret: any[] = [];

    let i = 0;
    while (i < sortArr.length) {
      const current = sortArr[i];
      const after = sortArr[i + 1];

      if (after && current.time === after.time && current.track !== after.track) {
        ret.push({
          ...current,
          ...after,
        });
        i++;
      } else {
        ret.push(current);
      }
      i++;
    }

    return ret;
  });

  watch(
    () => props.full,
    () => {
      nextTick(() => {
        trackScrollRef.value?.getContainerHeight();
      });
    },
    { deep: true },
  );

  watchEffect(() => {
    const { trackData } = props;
    activeId.value = trackData?.[0]?.id;
  });

  function updateTimeAxis(index: number) {
    currentIndex = index;

    const { trackData } = props;
    if (trackData.length === 0) {
      return;
    }

    const { id } = trackData[index] || trackData[trackData.length - 1];

    activeId.value = id;

    if (mouseOut) {
      updateScroll();
    }
  }

  /**
   * 更新滚动条
   */
  function updateScroll() {
    trackScrollRef.value?.updateScroll?.(currentIndex);
  }

  function clickHandle(id: string) {
    const { trackData } = props;
    const index = trackData.findIndex((item) => item.id === id);
    const per = index / (trackData.length - 1);
    const val = per * 100;
    emit('clickItem', val);
  }

  function mouseleave() {
    mouseOutTimer = setTimeout(() => {
      mouseOut = true;
      updateScroll();
    }, 3000);
  }

  function mouseenter() {
    mouseOut = false;
    clearTimeout(mouseOutTimer);
  }
</script>

<template>
  <div
    class="track-time-axis"
    :class="full ? `${size}-full` : size"
    @mouseenter="mouseenter"
    @mouseleave="mouseleave"
  >
    <ul class="time-axis">
      <TdVirtualList
        v-if="axisList.length > 0"
        ref="trackScrollRef"
        :data="axisList"
        :total="axisList.length"
      >
        <template #item="{ item }">
          <li v-if="item.track" class="item" @click="clickHandle(item.id)">
            <div
              class="title"
              :class="{
                'warn-text': item.warning,
                'temp-text': !item.warning,
                'is-active': item.id === activeId,
              }"
            >
              <div v-if="item.warning" class="text">
                <span class="time">
                  {{ item.timeStr }}
                </span>

                <div class="warn">
                  <span>{{ t('policeAdmin.service.warningOccurs') }}：</span>
                  <TdTag :label="getAlarmType(item.alarmType)" type="red" />
                </div>
              </div>

              <div v-else class="text">{{ item.timeStr }}</div>
            </div>
          </li>

          <li v-else class="item">
            <div class="title warn-text">
              <div class="text">
                <span class="time">
                  {{ item.timeStr }}
                </span>

                <div class="warn">
                  <span>{{ t('policeAdmin.service.warningOccurs') }}：</span>
                  <TdTag :label="getAlarmType(item.alarmType)" type="red" />
                </div>
              </div>
            </div>
          </li>
        </template>
      </TdVirtualList>
      <TdEmpty v-else class="empty" />
    </ul>
  </div>
</template>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  .track-time-axis {
    flex-direction: row;
    padding: 4px 0 4px 10px;
    overflow: hidden auto;
    backdrop-filter: blur(20px);
    border: 1px solid rgb(255 255 255 / 20%);

    .time-axis {
      position: relative;
      height: 100%;

      .item {
        display: flex;
        flex: 1;
        padding: 6px;

        .text {
          font-size: var(--font-size-small);
          color: #daecf9;
        }

        .title {
          flex: 1;
          font-size: var(--font-size-small);
          color: #daecf9;
        }
      }
    }
  }

  .warn-text {
    position: relative;
    padding-left: 18px;
    font-size: 14px;

    &::before {
      position: absolute;
      top: 5px;
      left: -3px;
      z-index: 1;
      width: 12px;
      height: 12px;
      content: '';
      background: url('@/assets/svg/status_error.svg');
      background-size: contain;
      border-radius: 50%;
    }

    &::after {
      position: absolute;
      top: calc(50% - 3px);
      left: 2px;
      height: 100%;
      content: '';
      border-left: 1px solid var(--line-color);
    }

    .text {
      .warn {
        display: flex;
        align-items: center;

        span {
          line-height: 20px;
        }
      }

      .time {
        margin-right: 10px;
        color: var(--text-color-red);
      }
    }
  }

  .temp-text {
    position: relative;
    padding-left: 18px;
    font-size: 14px;

    &::before {
      position: absolute;
      top: 7px;
      left: -3px;
      z-index: 1;
      width: 10px;
      height: 10px;
      content: '';
      background-color: var(--line-color);
      border-radius: 50%;
    }

    &::after {
      position: absolute;
      top: calc(50% + 7px);
      left: 2px;
      height: 100%;
      content: '';
      border-left: 1px solid var(--line-color);
    }

    &.is-active {
      .text {
        color: #00c791 !important;
      }

      &::before {
        position: absolute;
        top: 3px;
        left: -4px;
        z-index: 1;
        width: 12px;
        height: 12px;
        content: '';
        background: url('@/assets/images/track-play/playing.png');
        background-size: contain;
        border-radius: 50%;
      }
    }
  }

  .time-axis > li:nth-last-child(1) > div:nth-last-child(1) {
    border-left: 2px solid transparent;
  }

  .small {
    height: 350px;

    &-full {
      height: calc(100vh - 400px);
    }
  }

  .medium {
    height: 390px;

    &-full {
      height: calc(100vh - 360px);
    }
  }

  .large {
    height: 450px;

    &-full {
      height: calc(100vh - 300px);
    }
  }
</style>
