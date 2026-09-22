<script lang="ts" setup>
  import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';

  import { operateWarningDetail, queryVideoControlWarningDetail } from '@/api/videoControl';
  import defaultImg from '@/assets/images/event/photos.png';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { useDC, useEmitter, useI18n, usePermissions } from '@/hooks';
  import { levelColor, levelText, statusColor, statusText } from '@/pages/videoControl/common';
  import { useMainStore } from '@/store';
  import { getIp } from '@/utils';

  import AddEventCard from './addEventCard.vue';
  import ControlResourceItem from './warningNoticeCenter/controlResourceItem.vue';

  interface WarningNotice {
    address: string;
    alarmLevel: string;
    alarmTime: string;
    cameraIsdn: string;
    cameraName: string;
    frMetaData: any;
    frObjectInfo: any;
    isTemp: boolean;
    operationId: number;
    operationType: string;
    picture: any;
    remark: string;
    source: string;
    status: number;
    suspectTaskName: string;
    title: string;
    type: number;
    vehicleObjectInfo: any;
  }

  const props = defineProps({
    isDbClick: {
      default: false,
      type: Boolean,
    },
    isMissionCard: {
      default: false,
      type: Boolean,
    },
    warningData: {
      default() {
        return null;
      },
      type: Object,
    },
    warningNoticeId: {
      default: 0,
      type: [Number, String],
    },
  });
  const emit = defineEmits(['closeDialog']);

  const { t } = useI18n();
  const mainStore = useMainStore();

  const warningNoticeInfo = reactive<WarningNotice>({
    address: '',
    alarmLevel: '',
    alarmTime: '',
    cameraIsdn: '',
    cameraName: '',
    frMetaData: {},
    frObjectInfo: {},
    isTemp: false,
    operationId: 0,
    operationType: '',
    picture: {},
    remark: '',
    source: '',
    status: 0,
    suspectTaskName: '',
    title: '',
    type: 0,
    vehicleObjectInfo: {},
  });
  const contents = ref('');
  const baseImgUrl = ref('');
  const sceneImgUrl = ref('');

  const controlPersonInfo = computed(() => {
    return warningNoticeInfo.frObjectInfo;
  });
  const showBtn = computed(() => {
    return usePermissions('MISSION') && usePermissions('eBC');
  });
  const sourceInfo = computed(() => {
    const localLanguage = localStorage.getItem('localLanguage');
    const { cameraName, source } = warningNoticeInfo;
    let str = '';
    str =
      localLanguage === 'en'
        ? `Identified by the ${cameraName} ${source}`
        : `由${cameraName}${source}识别`;
    return str;
  });

  watch(
    () => props.warningNoticeId,
    (val) => {
      if (val) {
        queryWarningDetail();
      }
    },
  );

  useDC('IAP', 'alarm_create', async (message) => {
    if (props.warningNoticeId === message.alarmId) {
      queryWarningDetail();
    }
  });
  useDC('IAP', 'alarm_operate', async (message) => {
    if (props.warningNoticeId === Number(message.alarmId)) {
      queryWarningDetail();
    }
  });

  useEmitter('mapAlarmCardDetailsChange', initWarningNotice);

  onMounted(() => {
    const { isMissionCard, warningNoticeId } = props;
    if (isMissionCard) {
      nextTick(() => {
        initWarningNoticeData();
      });
    } else {
      queryWarningDetail();
    }

    useEmitter('closeLastWarningCid', (id) => {
      if (warningNoticeId !== id) {
        closeCard();
      }
    });
  });

  async function queryWarningDetail() {
    const { warningNoticeId } = props;
    if (!warningNoticeId) return;
    const param = {
      alarmId: warningNoticeId,
    };
    const { code, data } = await queryVideoControlWarningDetail(param);
    // 重置
    if (code === 0) {
      initWarningNotice(data);
    }
  }

  function initWarningNotice(data) {
    const ip = `${getIp()}/iap`;
    Object.keys(data).forEach((key) => {
      warningNoticeInfo[key] = data[key];
    });
    const { address, alarmTime, cameraName, frObjectInfo, picture } = warningNoticeInfo;
    contents.value =
      `${frObjectInfo.name} ( ${t('videoControl.createEvent.licenseNumber')}:${
        frObjectInfo.credentialNumber
      }) ` +
      `，${t('videoControl.createEvent.personTag')}【${frObjectInfo.tag}】，${t(
        'videoControl.createEvent.in',
      )} ${alarmTime} ${t('videoControl.createEvent.monitoredCamera')}（${cameraName}）${t(
        'videoControl.createEvent.at',
      )}${address}${t('videoControl.createEvent.Identify')}`;

    baseImgUrl.value = ip + picture.thumbImageUrl;
    sceneImgUrl.value = picture.sceneImageUrl ? ip + picture.sceneImageUrl : defaultImg;
  }

  function initWarningNoticeData() {
    const ip = `${getIp()}/iap`;
    const { warningData } = props;
    Object.keys(warningData).forEach((key) => {
      warningNoticeInfo[key] = warningData[key];
    });

    const { sceneImageUrl, thumbImageUrl } = warningNoticeInfo.picture;
    baseImgUrl.value = ip + thumbImageUrl;
    sceneImgUrl.value = sceneImageUrl ? ip + sceneImageUrl : defaultImg;
  }

  function effectWarningNotice() {
    Dialog('addEventCard')?.close();
    Dialog({
      cid: 'addEventCard',
      content: AddEventCard,
      data: {
        warningData: warningNoticeInfo,
      },
      offset: ['750px', '60px'],
    });
  }

  async function ignore() {
    const param = {
      alarmId: props.warningNoticeId,
      eventId: 0,
      operationId: appConfig.userData.id,
      operationType: 1,
    };
    const result = await operateWarningDetail(param);
    if (result.code === 0) {
      queryWarningDetail();
      useEmitter().emit('alarmIgnore');
      Message(t('videoControl.warningInformation.reportedIncidentIgnored'));
      mainStore.updateWarningNoticeShowId('');
    } else {
      Message(t('videoControl.warningInformation.ignoreInvalidTips'));
    }
  }

  function closeCard() {
    Dialog('addEventCard')?.close();

    if (!props.isDbClick) {
      mainStore.updateWarningNoticeShowId('');
      return;
    }
    emit('closeDialog');
  }
</script>

<template>
  <TdFrameBox :title="t('alarm.alarmDetails')" @close-frame-box="closeCard">
    <div class="warning-detail-main">
      <div v-if="!isMissionCard" class="warning-notice-title">
        <TdTag :label="statusText(warningNoticeInfo)" :type="statusColor(warningNoticeInfo)" />
        <TdTag
          class="level-tag"
          :label="levelText(warningNoticeInfo)"
          :type="levelColor(warningNoticeInfo)"
        />
        <span class="title-text">
          {{ warningNoticeInfo.title }}
        </span>
      </div>

      <div
        v-if="!isMissionCard && warningNoticeInfo.status === 0"
        class="warning-notice-type blue-text"
      >
        {{ t('videoControl.warningInformation.fromTask') }}
        {{ warningNoticeInfo.suspectTaskName }}
      </div>

      <div v-if="!isMissionCard" class="warning-notice-type">
        <Icon class="type-icon" name="category_time" />
        <div class="type-info">{{ warningNoticeInfo.alarmTime }}</div>
      </div>

      <div v-if="!isMissionCard" class="warning-notice-type">
        <Icon class="type-icon" name="category_position" />
        <div class="type-info">
          {{ warningNoticeInfo.address }}
        </div>
      </div>

      <div v-if="!isMissionCard" class="warning-notice-type">
        <Icon class="type-icon" name="control_icon" />
        <div class="type-info">
          {{ sourceInfo }}
        </div>
      </div>

      <div v-if="!isMissionCard && warningNoticeInfo.isTemp" class="warning-notice-type">
        <Icon class="type-icon" name="category_edit" />
        <div v-if="warningNoticeInfo.remark" class="type-info">
          {{ warningNoticeInfo.remark }}
        </div>
        <div v-else>{{ t('videoControl.controlTask.NoRemarks') }}</div>
      </div>

      <div
        v-if="!isMissionCard && !warningNoticeInfo.isTemp"
        v-show="false"
        class="warning-notice-type"
      >
        {{ contents }}
      </div>
      <!-- 信息卡 -->
      <ControlResourceItem
        :control-person-info="controlPersonInfo"
        :is-identify-obj="false"
        :meta-data="warningNoticeInfo.frMetaData"
        :misson-card="isMissionCard"
        :type="warningNoticeInfo.type"
        :vehicle-object-info="warningNoticeInfo.vehicleObjectInfo"
      />
      <!-- 图片预览 -->
      <div class="warning-notice-type">
        <div class="container-imgs left-img">
          <img :src="sceneImgUrl" />
        </div>
        <div class="container-imgs">
          <img :src="baseImgUrl" />
        </div>
      </div>

      <div v-if="!isMissionCard" v-show="warningNoticeInfo.status === 0" class="select-unit">
        <TdButton
          :class="showBtn ? 'btn' : 'big'"
          :text="t('videoControl.warningInformation.ignore')"
          type="normal"
          @click="ignore"
        />
        <TdButton
          v-if="showBtn"
          class="btn"
          :text="t('videoControl.warningInformation.transferValidCase')"
          type="normal"
          @click="effectWarningNotice"
        />
      </div>
    </div>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  .warning-detail-main {
    padding: 10px 0;
  }

  .warning-notice-title {
    .level-tag {
      margin: 0 4px;
    }

    .title-text {
      font-size: 14px;
      font-weight: bold;
      line-height: 20px;
      color: var(--text-title-first);
      word-break: break-all;
    }
  }

  .warning-notice-type {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    font-size: 14px;
    line-height: 18px;

    .type-icon {
      width: 26px;
      height: 26px;
      padding-right: 8px;
      fill: var(--icon-color-default);
    }

    .type-info {
      display: inline;
      width: 100%;
      word-break: break-all;
    }

    .container-imgs {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 140px;
      height: 140px;
      background: var(--background-secondary);

      img {
        max-width: 140px;
        max-height: 140px;
      }
    }

    .left-img {
      margin-right: 10px;
    }
  }

  .blue-text {
    margin: 5px 0;
    color: var(--text-title-second);
    word-break: break-all;
  }

  .select-unit {
    display: flex;
    justify-content: space-between;
    margin-top: 10px;

    .btn {
      width: 140px;
    }

    .big {
      width: 100%;
    }
  }
</style>
