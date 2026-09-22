<script setup lang="ts">
  import { computed, onMounted, reactive, ref, unref, watch } from 'vue';

  import { queryFenceAlarmById } from '@/api/alarms';
  import { fenceDetails } from '@/api/fence';
  import { queryTrackHistoryInfo } from '@/api/lbsLocation';
  import policeImg from '@/assets/images/marker/police_online.png';
  import { Dialog } from '@/components/Dialog';
  import { appConfig } from '@/config';
  import { useI18n } from '@/hooks';
  import ResourceAbility from '@/pages/communicationCard/resourceAbilityButton/resourceAbility.vue';
  import VoiceCallVolume from '@/pages/communicationCard/voiceCallVolume.vue';
  import BottomMap from '@/pages/home/bottomMap.vue';
  import { alarmImg, recoverImg } from '@/pages/map/marker/base64Marker';
  import { queryPersonDetailById } from '@/pages/resource/resourceHelper';
  import { formatColor, getAreaCenter, mapIsReady, mapManager } from '@/plugins/map';
  import { useCommunicationStore } from '@/store';
  import { isArray, isDef } from '@/utils/is';

  import dayjs from 'dayjs';
  import { cloneDeep, debounce } from 'lodash-es';

  import { drawFenceAlarmLayer } from './alarmCommon';
  import AlarmItem from './alarmItem.vue';

  const props = defineProps<{
    alarmId: string;
    alarmType: number;
    cid: string;
    userId: null | string;
  }>();

  const { t } = useI18n();
  const communicationStore = useCommunicationStore();

  const infoData = ref<any>({});
  const abilityValues = ref(['512001', '512006', '512005', '512007']);
  const showVoice = ref(false);
  const commId = ref('');
  const resourceName = ref('');
  const organization = ref('');
  const serviceAccountsData = ref([]);
  const trackPeriod = appConfig.settingData.TRACK_PERIOD ?? 12;
  const alarmData = reactive<any>({});
  const mapId = 'alarmMap';
  let trackData: any[] = [];
  const loading = ref(true);
  const title = t('alarm.alarmDetails');
  const showMap = computed(() => {
    return props.alarmType < 2;
  });
  const alarmTime = computed(() => {
    const { occurredTime, recoveredTime } = alarmData;
    const hms = 'YYYY-MM-DD HH:mm:ss';
    if (recoveredTime) {
      // 如果有恢复时间，取恢复时间前推配置小时数作为开始时间，取恢复时间作为结束时间
      const start = dayjs(recoveredTime).subtract(trackPeriod, 'hour').format(hms);
      return [start, recoveredTime];
    } else {
      // 如果没有恢复时间，取创建时间作为开始时间，取创建时间后配置小时数作为结束时间
      const end = dayjs(occurredTime).add(trackPeriod, 'hour').format(hms);
      return [occurredTime, end];
    }
  });

  watch(communicationStore.comm, () => {
    commHandler();
  });

  watch(infoData, () => {
    commHandler();
  });

  onMounted(() => {
    fetchData();
  });

  async function fetchData() {
    try {
      if (props.userId) {
        await queryExecutorDetail();
      }
      await queryFenceAlarmDetail();
      loading.value = false;
    } catch {
      loading.value = false;
    }
  }

  /**
   * 人员详情
   */
  async function queryExecutorDetail() {
    const data = await queryPersonDetailById(props.userId as string);
    if (data) {
      const { code, name, organizationName, serviceAccounts } = data;
      infoData.value = data;
      resourceName.value = name || code;
      organization.value = organizationName;
      serviceAccountsData.value = serviceAccounts;
    }
  }

  async function addPoliceMarker() {
    const { lat, lon } = unref(infoData).location?.coordinates?.[0] || {};
    if (lat && lon) {
      await mapIsReady(mapId);
      const mapObj = mapManager.get(mapId);
      const position = [lon, lat];

      mapObj.addLayer({
        data: [{ ...unref(infoData), position }],
        image: policeImg,
        isCluster: false,
        layerId: 'police',
      });
    }
  }

  /**
   * 预警详情
   */
  async function queryFenceAlarmDetail() {
    const param = { id: props.alarmId };
    const { code, data } = await queryFenceAlarmById(param);
    if (code === 0 && data) {
      const { fenceId, lonAndLat, occurredTime, recoveredTime } = data;
      Object.assign(alarmData, data);

      if (!unref(showMap)) return;

      if (fenceId) {
        await queryFence(fenceId);
      }

      queryTrack(1, {
        endTime: recoveredTime ? dayjs(recoveredTime).valueOf() : dayjs().valueOf(),
        startTime: dayjs(occurredTime).valueOf(),
      });

      if (lonAndLat) {
        await mapIsReady(mapId);
        const point = lonAndLat.split(',');

        drawFenceAlarmLayer({
          color: '#ff3b55',
          img: alarmImg,
          layerId: 'alarm',
          mapId,
          point,
          text: t('alarm.alarmLocation'),
        });
      }

      addPoliceMarker();
    }
  }

  /**
   * 区域绘制
   * @param fenceId
   */
  async function queryFence(fenceId) {
    const { code, data } = await fenceDetails(fenceId);
    if (code === 0 && data) {
      const { fenceColor, ogcGeometry } = data || {};
      const geo = ogcGeometry ? JSON.parse(ogcGeometry) : null;

      if (!geo) return;
      await mapIsReady(mapId);
      const mapObj = mapManager.get(mapId);
      const { center, path, radius, type } = geo;

      mapObj.setCenter(getAreaCenter(geo), 14);
      mapObj.draw({
        center,
        color: formatColor(fenceColor),
        handle: 'look',
        highlight: true,
        path,
        radius,
        type,
      });
    }
  }

  /**
   * 通信状态
   */
  function commHandler() {
    let comm: any = null;
    serviceAccountsData.value.forEach((item) => {
      const { account } = item;
      const val = communicationStore.comm[account];
      if (val) {
        comm = val;
        commId.value = account;
      }
    });
    if (!comm) {
      showVoice.value = false;
      return;
    }
    if (comm.voice) {
      showVoice.value = true;
    }
    showVoice.value = isDef(comm.voice);
  }

  function closeCard() {
    Dialog(props.cid as string)?.close();
  }

  /**
   * 重新渲染
   */
  const handleRefresh = debounce(() => {
    const mapObj = mapManager.get(mapId);

    loading.value = true;
    trackData = [];
    mapObj?.clearMap();
    fetchData();
  }, 500);

  /**
   * 动向查询
   * @param start
   * @param date
   */
  async function queryTrack(start, date) {
    const params: any = {
      ...date,
      executorId: unref(infoData)?.id,
      pageSize: 99_999,
      start,
      type: 1,
    };

    const { code, data } = await queryTrackHistoryInfo(params);

    if (code === 0 && isArray(data?.records)) {
      const { records, total } = data;

      records.forEach((item) => {
        const { lat, lon } = item.location.coordinates[0];
        trackData.push([lon, lat]);
      });

      const len = trackData.length;
      if (len < Number(total)) {
        queryTrack(start + 1, date);
      } else {
        createTrack();
        if (len > 0 && alarmData.state === 2) {
          drawFenceAlarmLayer({
            color: 'rgb(0, 199, 145)',
            img: recoverImg,
            layerId: 'recover',
            mapId,
            point: trackData[len - 1],
            text: t('alarm.recover'),
          });
        }
      }
    }
  }

  /**
   * 创建动向
   */
  async function createTrack() {
    await mapIsReady(mapId);
    const mapObj = mapManager.get(mapId);
    const path = cloneDeep(trackData);

    mapObj.draw({
      color: formatColor('rgb(255, 0, 0)'),
      handle: 'look',
      lineStyle: 'dashed',
      path,
      type: 'polyline',
    });
  }
</script>

<template>
  <div v-loading="loading" class="alarm-details ground-glass">
    <div class="header dragger">
      <TdTitle show-close @close="closeCard">{{ title }}</TdTitle>
    </div>
    <div class="section">
      <div class="box-left">
        <!-- info -->
        <div class="info-box">
          <TdAvatar class="img-person" :url="infoData.headShot || ''" />
          <div class="info">
            <div class="info-name">
              <TdTooltip :content="resourceName">
                <span class="subtitle-name"> {{ resourceName }} </span>
              </TdTooltip>

              <!-- voice-call -->
              <VoiceCallVolume
                v-show="showVoice"
                class="voice-call"
                :commu-id="commId"
                :show-btn="false"
              />
            </div>
            <TdTooltip :content="organization">
              <span class="organization-name">
                {{ organization }}
              </span>
            </TdTooltip>
          </div>
        </div>

        <!-- btn -->
        <ResourceAbility
          :ability-data="infoData"
          :ability-values="abilityValues"
          :alarm="true"
          :alarm-time="alarmTime"
          class="operation-btn"
          resource-type="person"
          :show-equipment-tabs="false"
        >
          <TdTooltip v-if="showMap" :content="t('alarm.refresh')" placement="top">
            <TdButton
              icon-name="refresh"
              size="default"
              type="radioSpecial"
              @click.stop="handleRefresh"
            />
          </TdTooltip>
        </ResourceAbility>
        <!-- alarm ext -->
        <AlarmItem :alarm-data="alarmData" />
      </div>

      <!-- map -->
      <div v-if="showMap" class="box-right">
        <BottomMap :map-id="mapId" />
      </div>
    </div>
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .alarm-details {
    min-height: 200px;

    .header {
      position: relative;
      display: flex;
      align-items: center;
      height: 34px;
    }

    .section {
      display: flex;

      .box-left {
        width: 310px;
        padding: 10px;

        .info-box {
          display: flex;
          align-items: center;

          .img-person {
            width: 50px;
            height: 50px;
            margin-right: 8px;
            border-radius: 25px;
          }

          .info {
            display: flex;
            flex-direction: column;
            width: calc(100% - 58px);
            font-size: 14px;

            .info-name {
              display: flex;
              align-items: center;

              .subtitle-name {
                .ellipsis1();

                max-width: 120px;
                margin-right: 8px;
                font-size: 16px;
                font-weight: bold;
                color: var(--text-title-first);
              }

              .voice-call {
                .ellipsis1();

                max-width: 110px;
              }
            }

            .organization-name {
              .ellipsis1();

              font-size: 12px;
              color: var(--text-title-second);
            }
          }
        }

        .operation-btn {
          display: flex;
          margin-top: 10px;
        }
      }

      .box-right {
        width: 500px;
        height: 400px;
      }
    }
  }
</style>
