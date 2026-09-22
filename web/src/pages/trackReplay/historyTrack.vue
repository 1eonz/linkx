<script lang="ts" setup>
  import { computed, onBeforeUnmount, onMounted, reactive, ref, unref, watch } from 'vue';

  import { queryFenceAlarmList } from '@/api/alarms';
  import {
    queryPoliceCarTrace,
    queryPoliceCarTrackSummary,
    queryServerTime,
    queryTrackHistoryInfo,
    queryTrackHistorySummary,
  } from '@/api/lbsLocation';
  import sampleArrowImg from '@/assets/images/marker/sample_arrow.png';
  import sampleCircleImg from '@/assets/images/marker/sample_circle.png';
  import endTrackImg from '@/assets/images/marker/track_end.png';
  import endTrackEnImg from '@/assets/images/marker/track_end_en.png';
  import trackGoingImg1 from '@/assets/images/marker/track_going1.png';
  import trackGoingImg2 from '@/assets/images/marker/track_going2.png';
  import trackGoingImg3 from '@/assets/images/marker/track_going3.png';
  import trackGoingImg4 from '@/assets/images/marker/track_going4.png';
  import startTrackImg from '@/assets/images/marker/track_start.png';
  import startTrackEnImg from '@/assets/images/marker/track_start_en.png';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { useI18n } from '@/hooks';
  import { drawFenceAlarmLayer } from '@/pages/alarmRecord/alarmCommon';
  import { alarmClusterImg, alarmImg } from '@/pages/map/marker/base64Marker';
  import { queryPersonDetailById } from '@/pages/resource/resourceHelper';
  import { EMap, getAreaCenter, mapIsReady, mapManager } from '@/plugins/map';
  import { delay } from '@/utils';
  import { isArray } from '@/utils/is';

  import * as Turf from '@turf/turf';
  import dayjs from 'dayjs';
  import { debounce } from 'lodash-es';

  import TrackReplayTimeAxis from './trackReplayTimeAxis.vue';

  type DateParam = { endTime: number; startTime: number };

  const props = withDefaults(
    defineProps<{
      alarm?: boolean;
      infoData?: any;
      resourceType: string;
      showEquipmentTabs?: boolean;
      showSearch?: boolean;
      time?: any;
      title?: string;
      tracks?: any[];
    }>(),
    {
      alarm: false,
      infoData: {},
      showEquipmentTabs: true,
      showSearch: true,
    },
  );
  const emit = defineEmits(['maximize', 'close', 'closeDialog']);

  const { t } = useI18n();

  const loading = ref(false);
  const isFull = ref(false);
  const historySummary = ref({
    totalDistance: 0,
    totalDistanceStr: '',
    totalTime: 0,
    totalTimeStr: '',
  });
  const searchTime = ref<any>();
  const trackData = reactive<any>({});
  const progress = ref<number>(0);
  const activeBtnNum = ref<number>(0);
  const trackReplayTimeAxisRef = ref();
  const trackObj: any = {}; // 动向对象
  const eqTypeTabs = [
    {
      id: 1,
      name: t('homePage.mapToolData.recorder'),
    },
    {
      id: 2,
      name: t('homePage.mapToolData.terminal'),
    },
  ];
  let activeEqType = 1;
  let mapObj: any = null; // 地图对象
  const trackPeriod = appConfig.settingData.TRACK_PERIOD ?? 12;
  const activeTrackIndex = ref(0);
  const warnList = reactive<any>({});
  const timeFormat = 'YYYY-MM-DD HH:mm';
  const mapId = 'trackMap';
  const speed = ref(1);
  const playStatus = ref<'' | 'pause' | 'start'>('');
  const colors = ['#00FFF2', '#00FF1C', '#FF00CD', '#FFF500'];
  const avatarUrl = ref('');
  const trackRecord = ref<any[]>([]);
  const sampleForm = reactive({
    checkArrow: false,
    checkNum: false,
    sampleRate: 1,
  });

  const showTracks = computed(() => Boolean(props.tracks));
  const showTabs = computed(() => {
    const { resourceType, showEquipmentTabs } = props;
    return resourceType === 'person' && showEquipmentTabs && !unref(showTracks);
  });

  const size = computed(() => {
    let ret = 'medium';
    const { showSearch } = props;
    if (showTabs.value && showSearch) {
      ret = 'small';
    } else if (!showTabs.value && !showSearch) {
      ret = 'large';
    }
    return ret;
  });
  const info = computed(() => {
    const { infoData, tracks } = props;
    if (isArray(tracks)) {
      const target = tracks[unref(activeTrackIndex)];
      target.name = target.executorName;
      return target;
    } else {
      return infoData;
    }
  });
  const infoId = computed(() => {
    const { resourceType } = props;
    const { executorId, id, resourceId } = unref(info);
    if (resourceType === 'person') {
      return resourceId || executorId || id;
    }
    return resourceId || id;
  });
  const isPerson = computed(() => {
    const { resourceType } = props;
    return ['Executor', 'person'].includes(resourceType);
  });
  // 主干不显示采样率
  const showSample = computed(() => {
    return false;
  });

  watch(isFull, (val) => {
    Dialog('trackCard').fullScreen(val);
  });

  watch(
    infoId,
    async (id) => {
      if (unref(isPerson)) {
        const data = await queryPersonDetailById(id);
        if (data) {
          avatarUrl.value = data.headShot || '';
        }
      }
    },
    { immediate: true },
  );

  onMounted(() => {
    initTrack();
  });

  onBeforeUnmount(() => {
    destroyTrack();
    mapObj?.destroyMap();
    mapManager.delete(mapId);
    mapObj = null;
  });

  const searchHandle = debounce(async () => {
    // 规格限制,查询时间不得大于12小时
    const [start, end] = searchTime.value;
    const diffTime = dayjs(end).diff(start, 'minute');
    const outTime = trackPeriod * 60;
    if (diffTime > outTime) {
      Message(t('resource.trackReplay.selectTrackTimeRange'));
      loading.value = false;
      return;
    }
    reset();
    loading.value = true;
    searchTrackSummary();
    await searchTrack(1);
    loading.value = false;
    addFenceAlarmLayer();
    clickSample();
  }, 500);

  /**
   * 选择动向列表人员
   * @param index
   */
  const tracksItemClick = debounce(async (index: number) => {
    activeTrackIndex.value = index;
    loading.value = true;
    await searchTrackSummary();
    loading.value = false;
    await addFenceAlarmLayer();

    setTimeout(() => {
      Object.keys(trackObj).forEach((key) => {
        const target = trackObj[key];
        if (key === unref(infoId)) {
          target.setHighlight();
          const path: any[] = [];
          trackData[key]?.forEach((item) => {
            const { lat, lon } = item.location?.coordinates?.[0] || {};
            if (lat && lon) {
              path.push([lon, lat]);
            }
          });
          const center = getAreaCenter({ path });
          const zoom = mapObj.getZoom();
          center && mapObj.setCenter(center, zoom);
        } else {
          target.recover();
        }
      });
    }, 500);
  }, 500);

  // 初始化地图 + 轨迹 （地图加载和轨迹数据同步以便更快加载出轨迹）
  async function initTrack() {
    EMap({
      mapId,
      onLoad: (context) => {
        mapObj = context;
      },
    });

    const { time, tracks } = props;
    let end, start;
    if (isArray(time) && time.length > 0) {
      [start, end] = time;
    } else {
      start = dayjs().subtract(trackPeriod, 'hours');
      end = dayjs();
    }

    searchTime.value = [dayjs(start).format(timeFormat), dayjs(end).format(timeFormat)];
    // 多路动向回放逻辑不变
    if (tracks) {
      loading.value = true;
      for (const track of tracks) {
        await searchTrack(1, track.executorId);
      }
      tracksItemClick(0);
    }
  }

  function getActive(num) {
    return activeBtnNum.value === num;
  }

  // 重置
  function reset() {
    Object.keys(trackData).forEach((key) => {
      delete trackData[key];
    });
    destroyTrack();
    mapObj?.clearMap();
    progress.value = 0;
    playStatus.value = '';
    speed.value = 1;
    historySummary.value = {
      totalDistance: 0,
      totalDistanceStr: '',
      totalTime: 0,
      totalTimeStr: '',
    };
  }

  function destroyTrack() {
    deleteSample();
    Object.keys(trackObj).forEach((key) => {
      trackObj[key].destroyTrack();
      delete trackObj[key];
    });
  }

  // 查询动向统计
  async function searchTrackSummary() {
    const param: any = { ...(await getDateParam()) };
    getId(param);

    const { resourceType } = props;
    const api = resourceType === 'car' ? queryPoliceCarTrackSummary : queryTrackHistorySummary;

    const { code, data } = await api(param);

    if (code === 0 && isArray(data) && data.length > 0) {
      const { totalTime } = data[0];
      const hour = Number.parseInt(`${totalTime / (60 * 60)}`); // 计算整数小时数
      const afterHour = totalTime - hour * 60 * 60; // 取得算出小时数后剩余的秒数
      const min = Number.parseInt(`${afterHour / 60}`); // 计算整数分

      historySummary.value = {
        totalDistanceStr: `${data[0]?.totalDistance}km`,
        totalTimeStr: `${hour} ${t('common.dateDay.hour')} ${min} ${t('common.dateDay.minute')}`,
        ...data[0],
      };
      // 点击查询时的时间，防止多次点击数据重复
    } else {
      Message(t('resource.trackReplay.noCorrespondingTrackData'));
      if (!unref(showTracks)) {
        reset();
      }
    }
  }

  // 分页查询动向数据
  async function searchTrack(start, id?) {
    const { resourceType } = props;

    const p = await getDateParam();
    // queryFunc.queryGISTrack({
    //   begintime: dayjs(p.startTime).format('YYYY-MM-DD HH:mm:ss'),
    //   endtime: dayjs(p.endTime).format('YYYY-MM-DD HH:mm:ss'),
    //   offset: '0',
    //   ueid: infoData.account,
    // });

    const param: any = {
      ...p,
      pageSize: 9999,
      start,
    };
    const _id = getId(param, id);
    const api = resourceType === 'car' ? queryPoliceCarTrace : queryTrackHistoryInfo;

    const { code, data } = await api(param);
    const { records, total } = data;

    if (code === 0 && isArray(records) && records.length > 0) {
      if (!trackData[_id]) {
        trackData[_id] = [];
      }
      trackRecord.value = records;
      records.forEach((item, index) => {
        trackData[_id].push({ ...item, id: item.id || index });
      });
      if (trackData[_id].length < total) {
        await searchTrack(start + 1, id);
      } else {
        createTrack(_id);
        if (props.alarm && trackData[_id].length > 0) {
          await queryFenceAlarm(_id);
        }
      }
    }
  }

  // 获取时间参数
  async function getDateParam(): Promise<DateParam> {
    const serverTime = await queryServerTime();
    // 服务器时间和当前时间差
    const diff = serverTime.data - Date.now();

    const [start, end] = unref(searchTime);
    const date: DateParam = {
      endTime: dayjs(`${end}:59`).valueOf() + diff,
      startTime: dayjs(`${start}:00`).valueOf() + diff,
    };

    return date;
  }

  // 参数id
  function getId(param?, id?): string {
    const { resourceType } = props;
    const paramId = id || unref(infoId);

    if (unref(isPerson)) {
      param.executorId = paramId;
      param.type = activeEqType;
    } else if (resourceType === 'car') {
      param.id = paramId;
    } else {
      param.equipmentId = paramId;
    }

    return paramId;
  }

  // 创建动向
  async function createTrack(id) {
    await mapIsReady(mapId);
    const { tracks } = props;
    const lang = localStorage.getItem('localLanguage');
    const data: any[] = [];
    const index = tracks ? tracks.findIndex((item) => item.executorId === id) : 0;
    const color = colors[index];
    const imgs = [trackGoingImg1, trackGoingImg2, trackGoingImg3, trackGoingImg4];

    trackData[id].forEach((item) => {
      const { lat, lon } = item.location.coordinates[0];
      if (lon && lat) {
        data.push([lon, lat]);
      }
    });

    lineLength(data);

    trackObj[id] = mapObj.track({
      color,
      data,
      endImage: lang === 'en' ? endTrackEnImg : endTrackImg,
      image: imgs[index],
      move: (index) => {
        if (unref(infoId) !== id || !trackObj[id]) {
          return;
        }
        progress.value = (index / (data.length - 1)) * 100;
        trackReplayTimeAxisRef.value.updateTimeAxis(index);
      },
      startImage: lang === 'en' ? startTrackEnImg : startTrackImg,
      statusChange: (status) => {
        playStatus.value = status;
      },
    });
  }

  // 计算线长
  function lineLength(data) {
    // 创建一个线字符串（LineString）
    const line = Turf.lineString(data);
    // 计算线的长度，单位为米
    const length = Turf.length(line, { units: 'meters' });
    historySummary.value.totalDistanceStr = `${(length / 1000).toFixed(2)}km`;
  }

  /**
   * 获取围栏预警
   */
  async function queryFenceAlarm(id) {
    const { endTime, startTime } = await getDateParam();
    const _id = id || unref(infoId);
    const param = {
      endTime: dayjs(endTime).format('YYYY-MM-DD HH:mm:ss'),
      startTime: dayjs(startTime).format('YYYY-MM-DD HH:mm:ss'),
      userId: _id,
    };
    const { code, data } = await queryFenceAlarmList(param);

    if (code === 0) {
      const { list } = data;
      if (!isArray(list) || list.length === 0) {
        return;
      }

      list.forEach((item) => {
        const { lonAndLat } = item;
        if (lonAndLat) {
          const position = lonAndLat.split(',').map(Number);
          item.position = position;
        }
      });
      warnList[_id] = list;
    }
  }

  async function addFenceAlarmLayer() {
    await mapIsReady(mapId);
    const data = warnList[unref(infoId)];
    mapObj.deleteLayer(['alarm']);
    if (!props.alarm || !data) return;

    await drawFenceAlarmLayer({
      clusterImage: alarmClusterImg,
      color: '#ff3b55',
      data,
      img: alarmImg,
      layerId: 'alarm',
      mapId,
      text: t('policeAdmin.service.warningPosition'),
    });
  }

  /**
   * 拖动进度条
   * @param val 百分比
   */
  function sliderChange(val: number) {
    if (unref(showTracks)) return;
    const target = trackObj[unref(infoId)];
    if (!target) return;
    const percent = val / 100;
    const index = Math.round((trackData[unref(infoId)].length - 1) * percent);

    target.setPosition(index);

    if (target.status !== 'start') {
      target.start();
    }
    activeBtnNum.value = 0;
    playStatus.value = 'start';
  }

  // 开始
  function startTrack() {
    activeBtnNum.value = 0;
    const keys = Object.keys(trackObj);
    if (keys.length === 0) return;
    keys.forEach((key) => {
      trackObj[key]?.start();
    });
    playStatus.value = 'start';
  }

  // 暂停
  function pauseTrack() {
    activeBtnNum.value = 0;
    Object.keys(trackObj).forEach((key) => {
      trackObj[key]?.pause();
    });
    playStatus.value = 'pause';
  }

  // 从新播放
  function reStartTrack() {
    activeBtnNum.value = 1;
    const keys = Object.keys(trackObj);
    if (keys.length === 0) return;
    keys.forEach((key) => {
      trackObj[key]?.reStart();
    });
  }

  // 慢速
  function lowSpeed() {
    activeBtnNum.value = 2;
    const keys = Object.keys(trackObj);
    if (keys.length === 0) return;
    const _speed = unref(speed) / 2;
    speed.value = Math.max(_speed, 0.5);

    keys.forEach((key) => {
      const target = trackObj[key];
      target.setSpeed(unref(speed));
    });
  }

  // 快速
  function quickSpeed() {
    activeBtnNum.value = 3;
    const keys = Object.keys(trackObj);
    if (keys.length === 0) return;
    const _speed = unref(speed) * 2;
    speed.value = Math.min(_speed, 32);

    keys.forEach((key) => {
      const target = trackObj[key];
      target.setSpeed(unref(speed));
    });
  }

  // 关闭窗口
  function closeWindow() {
    emit('closeDialog');
    emit('close');
  }

  async function sizeWindow(mini) {
    isFull.value = !mini;
    await delay(300);
    mapObj?.resize?.();
  }

  // 切换设备类型
  function tabClickHandle(data) {
    activeEqType = data.id;
  }

  // 分页频率
  async function clickSample() {
    await mapIsReady(mapId);
    const { checkArrow, checkNum, sampleRate } = sampleForm;
    if (!showSample.value || trackRecord.value.length === 0) return;
    if (sampleRate > 10 || sampleRate < 1) {
      Message('采样率范围为1-10');
      return;
    }
    deleteSample();

    const sampleData: any = [];
    trackRecord.value.forEach((item, index) => {
      if (index % sampleRate === 0 && index !== 0 && index < trackRecord.value.length - 1) {
        const coordinates = item.location?.coordinates?.[0];
        if (coordinates) {
          item.position = [coordinates.lon, coordinates.lat];
        }
        sampleData.push(item);
      }
    });

    if (sampleData.length === 0) {
      Message('该采样率下无数据');
      return;
    }

    const keys = Object.keys(trackObj);
    if (keys.length === 0) return;

    // 箭头
    if (checkArrow) {
      keys.forEach((key) => {
        trackObj[key]?.drawSample({
          bothIcon: checkNum,
          data: sampleData,
          image: sampleArrowImg,
          key,
          layerId: 'sampleArrow',
        });
      });
    }

    // 数字
    if (checkNum) {
      keys.forEach((key) => {
        trackObj[key]?.drawSample({
          data: sampleData,
          image: sampleCircleImg,
          key,
          layerId: 'sampleCircle',
        });
      });
    }
  }

  // 删除采样速率
  function deleteSample() {
    if (!showSample.value) return;
    const map = mapManager.get(mapId);
    const keys = Object.keys(trackObj);
    if (keys.length === 0) return;
    keys.forEach((key) => {
      map.deleteLayer([`sampleCircle${key}`], false);
      map.deleteLayer([`sampleArrow${key}`], false);
    });
  }
</script>

<template>
  <div class="track-history ground-glass">
    <TdTitle
      class="header dragger"
      :show-close="true"
      :show-size="true"
      :title="title || t('communication.communicationFunction.trackHistory')"
      @close="closeWindow"
      @size="sizeWindow"
    />

    <div class="main">
      <!-- 地图 -->
      <div class="track-map">
        <div v-if="showTracks" class="tracks-box">
          <div
            v-for="(item, index) in tracks"
            :key="index"
            class="tracks-item"
            :class="[{ active: activeTrackIndex === index }]"
            @click="tracksItemClick(index)"
          >
            <div class="info">
              <div class="legend"></div>
              <TdTooltip :content="item.executorName || item.name">
                <span class="subtitle-name">
                  {{ item.executorName || item.name }}
                </span>
              </TdTooltip>
            </div>
          </div>
        </div>
        <div :id="mapId" class="map-container"></div>
      </div>

      <!-- 右侧操作 -->
      <div class="operation-board" :class="{ 'operation-board_untabs': !showTabs }">
        <div class="info-box">
          <TdAvatar class="img-box" :info="info" :url="avatarUrl" />

          <div class="info">
            <div class="info-name">
              <TdTooltip :content="info.name || info.code">
                <span class="subtitle-name"> {{ info.name || info.code }} </span>
              </TdTooltip>
            </div>
            <TdTooltip :content="info.organizationName">
              <span class="organization-name">
                {{ info.organizationName }}
              </span>
            </TdTooltip>
          </div>
        </div>

        <TdTab
          v-if="showTabs"
          class="equipment-type"
          :data="eqTypeTabs"
          tab-type="card"
          @click="tabClickHandle"
        />

        <div class="track-info-detail">
          <div class="distance-time">
            <span class="info-small-value"> {{ t('resource.policeTrackPlay.totalDis') }}： </span>
            <span class="distance-time-info">
              {{ historySummary.totalDistanceStr }}
            </span>
          </div>
          <div class="distance-time">
            <span class="info-small-value"> {{ t('resource.policeTrackPlay.totalTimes') }}： </span>
            <span class="distance-time-info">
              {{ historySummary.totalTimeStr }}
            </span>
          </div>
        </div>

        <div :class="{ 'track-search-time': showSearch, 'track-time': !showSearch }">
          <ElDatePicker
            v-model="searchTime"
            class="date-picker"
            :disabled="!showSearch"
            :end-placeholder="t('mission.missionList.endTime')"
            :format="timeFormat"
            popper-class="date-picker-popper"
            :start-placeholder="t('mission.missionList.startTime')"
            type="datetimerange"
            :value-format="timeFormat"
          />
          <TdButton
            v-if="showSearch"
            class="track-query"
            :text="t('resource.trackReplay.search')"
            type="normal"
            @click="searchHandle"
          />
        </div>

        <!-- 路线 -->
        <div class="track-info-axis">
          <p class="axis-title">{{ t('resource.trackReplay.totalRoute') }}：</p>
          <!--时间轴-->
          <TrackReplayTimeAxis
            ref="trackReplayTimeAxisRef"
            :full="isFull"
            :resource-type="resourceType"
            :size="size"
            :total-distance="historySummary.totalDistanceStr"
            :total-time="historySummary.totalTimeStr"
            :track-data="trackData[infoId] || []"
            :warn-list="warnList[infoId] || []"
            @click-item="sliderChange"
          />
        </div>

        <!-- 进度条 -->
        <div v-if="!showTracks" class="track-info-progress">
          <TdSlider v-model="progress" class="slider" @change="sliderChange" />
        </div>

        <!-- 功能按钮 -->
        <div class="track-info-button">
          <TdButton
            v-if="playStatus === 'start'"
            :active="getActive(0)"
            class="track-btn"
            :text="t('resource.trackReplay.timeOut')"
            type="normal"
            @click="pauseTrack"
          />
          <TdButton
            v-else
            :active="getActive(0)"
            class="track-btn"
            :text="t('resource.trackReplay.start')"
            type="normal"
            @click="startTrack"
          />
          <TdButton
            :active="getActive(1)"
            class="track-btn"
            :text="t('resource.policeTrackPlay.replay')"
            type="normal"
            @click="reStartTrack"
          />
          <TdButton
            :active="getActive(2)"
            class="track-btn"
            :text="t('resource.trackReplay.slow')"
            type="normal"
            @click="lowSpeed"
          />
          <TdButton
            :active="getActive(3)"
            class="track-btn"
            :text="t('resource.trackReplay.quickX') + speed"
            type="normal"
            @click="quickSpeed"
          />
        </div>

        <!-- 采样率 -->
        <div v-if="showSample" class="track-info-sample ground-glass">
          <div class="sample-item">
            <TdCheckbox v-model="sampleForm.checkNum" />
            <span>数字</span>
          </div>
          <div class="sample-item">
            <TdCheckbox v-model="sampleForm.checkArrow" />
            <span>箭头</span>
          </div>
          <div class="sample-item">
            <ElInput v-model="sampleForm.sampleRate" class="sample-item-input" />
            <TdButton class="sample-btn" text="更改采样率" type="normal" @click="clickSample" />
          </div>
        </div>
      </div>

      <div v-if="loading" class="loading">
        <ElIcon class="is-loading" style="font-size: 50px">
          <Loading />
        </ElIcon>
        <div class="loading-title">{{ t('common.loading') }}...</div>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .date-picker-popper {
    .el-picker-panel {
      position: absolute;
      left: -240px;
    }
  }
</style>

<style lang="less" scoped>
  .track-history {
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
      width: calc(100% - 310px);
      height: 100%;
      background-color: var(--background-dark-map);

      .tracks-box {
        position: absolute;
        top: 20px;
        z-index: 10;
        display: flex;
        justify-content: center;
        width: 100%;

        .tracks-item {
          display: flex;
          align-items: center;
          justify-content: center;
          min-width: 90px;
          height: 40px;
          padding: 0 10px;
          margin-right: 10px;
          cursor: pointer;
          background-color: rgb(25 41 60 / 85%);

          &:nth-of-type(1) .legend {
            background-color: #00fff2;
          }

          &:nth-of-type(2) .legend {
            background-color: #00ff1c;
          }

          &:nth-of-type(3) .legend {
            background-color: #ff00cd;
          }

          &:nth-of-type(4) .legend {
            background-color: #fff500;
          }

          .info {
            display: flex;
            flex: 1;
            align-items: center;
            justify-content: center;
            font-size: 14px;

            .legend {
              width: 14px;
              height: 14px;
              margin-right: 5px;
            }

            .subtitle-name {
              max-width: 200px;
              overflow: hidden;
              font-size: 16px;
              font-weight: bold;
              color: var(--text-title-first);
              text-align: center;
              text-overflow: ellipsis;
              white-space: nowrap;
            }
          }
        }

        .active {
          border: 2px solid #3299e3;
        }
      }

      .map-container {
        flex: 1;
      }
    }

    .operation-board {
      position: relative;
      display: flex;
      flex-direction: column;
      width: 310px;
      height: 100%;
      padding: 10px;
      border-left: 1px solid transparent;
      border-image: linear-gradient(
        180deg,
        rgba(26 255 251 / 20%) 0%,
        rgba(26 255 251 / 100%) 100%
      );
      border-image-slice: 1;

      .equipment-type {
        margin-top: 6px;
      }

      .track-info-detail {
        display: flex;
        flex-direction: column;
        padding: 6px 10px;
        margin: 6px 0;
        backdrop-filter: blur(20px);
        border: 1px solid rgb(255 255 255 / 20%);

        .distance-time {
          .info-small-value {
            width: 100px;
            height: 20px;
            font-size: 14px;
            color: var(--text-title-second);
            vertical-align: middle;
          }

          .distance-time-info {
            display: inline-block;
            height: 20px;
            font-size: 14px;
            color: #fff;
            vertical-align: middle;
          }
        }
      }

      .track-search-time {
        display: flex;
        flex-wrap: wrap;
        align-items: center;

        :deep(.el-date-editor) {
          width: 234px !important;
        }
      }

      .track-time {
        margin: 10px 0;

        :deep(.el-date-editor) {
          width: 290px !important;
          background: none;
        }
      }

      .track-info-axis {
        .tabs-box {
          margin-top: 10px;
          font-size: 18px;
          cursor: pointer;
        }

        .axis-title {
          font-size: 14px;
          color: var(--text-title-second);
        }
      }

      .track-info-progress {
        width: 100%;
        margin-top: 16px;
      }

      .track-info-sample {
        position: absolute;
        right: 320px;
        bottom: 20px;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        width: 170px;
        height: 120px;
        padding: 12px 24px;
        background: rgb(6 41 74 / 60%);
        backdrop-filter: blur(8px);
        opacity: 1;

        .sample-item {
          display: flex;
          align-items: center;
          height: 20px;
          font-size: 14px;

          span {
            margin-left: 20px;
          }

          .sample-item-input {
            background: linear-gradient(
              180deg,
              rgb(0 115 168 / 67%) 0%,
              rgb(105 255 215 / 67%) 100%
            );
            border: 1px solid rgb(26 255 251 / 100%);
            box-shadow: inset 0 0 4px rgb(26 255 251 / 100%);

            :deep(.el-input__inner) {
              background: transparent;
              border: none;
            }
          }

          .sample-btn {
            height: 26px;
            margin-left: 6px;
            line-height: 26px;
          }
        }
      }

      .track-info-button {
        display: flex;
        justify-content: space-between;
        margin-top: 16px;

        .track-btn {
          width: auto;
          height: auto;
          padding: 0 8px;
          margin: 0 2px;
          opacity: 1;
        }
      }

      .track-query {
        width: 100%;
        margin: 6px 0;
      }
    }
  }

  .info-box {
    display: flex;
    align-items: center;
    width: 100%;

    :deep(.img-box .icon) {
      width: 50px !important;
      height: 50px !important;
    }

    .info {
      display: flex;
      flex: 1;
      flex-direction: column;
      margin-left: 8px;
      font-size: 14px;

      .info-name {
        display: flex;
        align-items: center;

        .subtitle-name {
          width: 210px;
          overflow: hidden;
          font-size: 16px;
          font-weight: bold;
          color: var(--text-title-first);
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }

      .organization-name {
        width: 210px;
        overflow: hidden;
        color: var(--text-title-second);
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }

  .loading {
    position: absolute;
    top: 0;
    left: 0;
    z-index: 10;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    text-align: center;
    background-color: var(--background-default);
    opacity: 0.8;

    .loading-title {
      margin-top: 10px;
      font-size: var(--font-size-medium);
      color: var(--text-default);
    }
  }

  :deep(.el-input) {
    width: 30px;

    .el-input__inner {
      height: 22px !important;
      padding: 0;
      color: #fff;
      text-align: center;
      background: linear-gradient(to bottom, rgb(19 147 232 / 7%), rgb(19 147 232 / 100%));
      border: 1px solid #1afffb;
    }
  }
</style>
