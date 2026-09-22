<script lang="ts" setup>
  import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';

  import closeTrackImg from '@/assets/images/marker/track_close.png';
  import endTrackImg from '@/assets/images/marker/track_end.png';
  import endTrackEnImg from '@/assets/images/marker/track_end_en.png';
  import startTrackImg from '@/assets/images/marker/track_start.png';
  import startTrackEnImg from '@/assets/images/marker/track_start_en.png';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import { useBaseData, useI18n, useSetInterval } from '@/hooks';
  import { EMap, mapIsReady, mapManager } from '@/plugins/map';
  import { delay } from '@/utils';
  import dateUtil from '@/utils/dateUtil';

  const props = withDefaults(
    defineProps<{
      infoData: any;
      settingTime: number;
    }>(),
    {
      infoData: {},
    },
  );
  const emit = defineEmits(['closeDialog']);
  const { t } = useI18n();

  const text = ref(t('resource.policeTrackPlay.trackNowText'));
  let clearTimer: any = null; // 轨迹定时刷新
  let clearCountdownTimer: any = null; // 倒计时
  const mapId = 'trackMap';
  let trackObj: any = null; // 动向对象
  let mapObj: any = null; // 地图对象
  const isFull = ref(false);

  const trackData: any = [];
  let playTime = 0;
  let endTime = 0;
  const lastTime = ref('');

  const title = computed(() => {
    const { code, name } = props.infoData;
    return lastTime.value === t('resource.trackReplay.endTracking')
      ? `${t('resource.policeTrackPlay.track') + name}(${code})${text.value}`
      : `${t('resource.policeTrackPlay.trackNow') + name}(${code})${text.value}`;
  });

  watch(isFull, (val) => {
    Dialog('realTimeTrackCard').fullScreen(val);
  });

  onMounted(() => {
    EMap({
      mapId,
      onLoad: (context) => {
        mapObj = context;
        init();
      },
    });
  });

  onBeforeUnmount(() => {
    destroyTrack();
    mapManager.get(mapId)?.destroyMap();
    mapManager.delete(mapId);
    mapObj = null;
  });

  async function init() {
    await mapIsReady(mapId);

    playTime = Date.now();
    endTime = playTime + props.settingTime * 60 * 1000;

    // 点位查询
    clearTimer = useSetInterval(queryLocationFromStore, 3 * 1000, true);

    // 倒计时计算
    clearCountdownTimer = useSetInterval(handleTime, 1000);
  }

  // 直接从全局维护的数据查询实时经纬度
  function queryLocationFromStore() {
    const { category, id } = props.infoData;
    const { getResourceOrigin } = useBaseData();
    const source = getResourceOrigin[category] || [];
    for (const item of source) {
      const { position } = item;
      if (item.id === id) {
        if (!position && trackData.length === 0) {
          Message(t('mission.missionMap.nonPositionInfo'));
          break;
        }

        if (trackData.slice(-1).join('') !== position.join('')) {
          trackData.push(position);
        }

        // 首次进入创建地图对象与起点
        if (trackObj) {
          trackObj.updateData(trackData);
        } else {
          const lang = localStorage.getItem('localLanguage');
          trackObj = mapObj.trackRealTime({
            color: '#00FFF2',
            data: trackData,
            endImage: closeTrackImg,
            startImage: lang === 'en' ? startTrackEnImg : startTrackImg,
          });
        }
        break;
      }
    }
  }

  // 倒计时时间格式化
  function handleTime() {
    const endTimeDate = new Date(endTime).getTime();
    const currentTime = Date.now();
    const time = endTimeDate - currentTime;

    const { hour, minute, second } = dateUtil.getHMSByMsec(time);
    lastTime.value = `${hour}:${minute}:${second}`;
    if (time <= 1000) {
      clearTimer?.();
      clearCountdownTimer?.();
      const lang = localStorage.getItem('localLanguage');
      trackObj?.endData(lang === 'en' ? endTrackEnImg : endTrackImg);
      lastTime.value = t('resource.trackReplay.endTracking');
      text.value = t('resource.trackReplay.trackingComplete');
    }
  }

  function destroyTrack() {
    if (trackObj) {
      trackObj.destroyRealTimeTrack();
      trackObj = null;
    }
  }

  // 关闭窗口
  function closeWindow() {
    clearTimer?.();
    clearCountdownTimer?.();
    emit('closeDialog');
  }

  async function sizeWindow(mini) {
    isFull.value = !mini;
    await delay(300);
    mapObj?.resize?.();
  }
</script>

<template>
  <div class="real-track-history ground-glass">
    <TdTitle
      class="header dragger"
      :show-close="true"
      :show-size="true"
      :title="t('resource.policeTrackPlay.trackRealTime')"
      @close="closeWindow"
      @size="sizeWindow"
    />

    <div class="main">
      <div class="track-map">
        <div :id="mapId" class="map-container"></div>
      </div>

      <div class="warning ground-glass">
        <Icon class="icon" name="track_realtime" />
        <span class="text">
          {{ title }}
        </span>
        <div class="time">{{ lastTime }}</div>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .real-track-history {
    position: relative;
    width: 1334px;
    height: 750px;

    .header {
      height: 34px;
    }

    .main {
      display: flex;
      width: 100%;
      height: calc(100% - 34px);
    }

    .track-map {
      position: relative;
      display: flex;
      flex-direction: column;
      width: 100%;
      height: 100%;
      background-color: var(--background-dark-map);

      .map-container {
        flex: 1;
      }
    }

    .warning {
      position: absolute;
      top: 45px;
      left: 40%;
      display: flex;
      align-items: center;
      justify-content: flex-start;
      padding: 14px 10px;

      .text {
        font-size: 14px;
        font-weight: 400;
        line-height: 22px;
        color: rgb(171 216 255 / 100%);
        letter-spacing: 0;
      }

      .icon {
        width: 16px;
        height: 16px;
        margin-top: 2px;
        margin-right: 5px;
      }

      .cancel-btn {
        width: 60px;
        height: 32px;
        margin-left: 10px;
      }

      .time {
        margin-left: 10px;
        font-size: 14px;
        font-weight: 400;
        line-height: 22px;
        color: rgb(26 255 251 / 100%);
        letter-spacing: 0;
      }
    }
  }
</style>
