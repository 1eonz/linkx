<!--
  操作按钮集合
  注意列表渲染时只能当鼠标hover时才能渲染组件，否则过多渲染会导致页面卡顿甚至崩溃！！！（watch过多造成内存占用过多）
-->

<script lang="ts" setup>
  import { computed, ref, unref } from 'vue';

  import { CategoryEnum } from '@/enums';
  import { useI18n, usePermissions } from '@/hooks';
  import { getCallTypeOptions } from '@/pages/mrs/common';
  import { useFavoriteStore } from '@/store';

  import BreakOffBtn from './breakOffBtn.vue';
  import ConferenceBtn from './conferenceBtn.vue';
  import FavoriteSourceBtn from './favoriteSourceBtn.vue';
  import Recordings from './recordings.vue';
  import SmsSendBtn from './smsSendBtn.vue';
  import TrackHistoryBtn from './trackHistoryBtn.vue';
  import TrackRealTimeBtn from './trackRealTimeBtn.vue';
  import VehicleBtn from './vehicleBtn.vue';
  import VideoCallBtn from './videoCallBtn.vue';
  import VideoWatchBtn from './videoWatchBtn.vue';
  import VoiceCallBtn from './voiceCallBtn.vue';
  import VoiceHalfCallBtn from './voiceHalfCallBtn.vue';
  import VoiceWatchBtn from './voiceWatchBtn.vue';

  const props = withDefaults(
    defineProps<{
      abilityData: any;
      abilityValues: any[];
      account?: string;
      alarm?: boolean;
      alarmTime?: any[];
      btnType?: string;
      defaultDropMenu?: any[];
      dropMenu?: boolean;
      isMain?: boolean;
      onlyCall?: boolean;
      resourceType?: string;
      showEquipmentTabs?: boolean;
      size?: string;
      voiceHalf?: boolean;
    }>(),
    {
      btnType: 'radioSpecial',
      showEquipmentTabs: true,
    },
  );
  const emit = defineEmits(['callClick', 'dropMenuSelect', 'dropMenuOpen']);

  const { t } = useI18n();
  const favoriteStore = useFavoriteStore();

  const halfCall = ref();
  const voiceCall = ref();
  const videoCall = ref();
  const trackHistory = ref();
  const trackRealTime = ref();
  const videoWatch = ref();
  const voiceWatch = ref();
  const smsSend = ref();
  const favorite = ref();
  const breakOff = ref();
  const recordings = ref();

  const showVoiceHalf = computed(() => {
    const { abilityData } = props;
    const isPdt = Number(abilityData.category) === CategoryEnum.pdt;
    const ret = props.voiceHalf || isPdt;
    return ret;
  });
  const info = computed(() => {
    const { abilityData } = props;
    const { account } = abilityData;
    const ret = { ...abilityData };
    if (!ret.serviceAccounts) {
      // 保持数据结构一致 serviceAccounts
      ret.serviceAccounts = [{ account }];
    }
    return ret;
  });
  const isPerson = computed(() => {
    const { category } = props.abilityData;
    return (
      props.resourceType?.toLowerCase() === 'person' || Number(category) === CategoryEnum.person
    );
  });
  const isNotSeats = computed(() => {
    const { category } = props.abilityData;
    // 不是坐席也不是人员才能轨迹跟踪
    return Number(category) !== CategoryEnum.seat && category !== CategoryEnum.person;
  });
  const hasBreakOff = computed(() => {
    return (
      !props.onlyCall &&
      (showAbilityBtn('512001') || showAbilityBtn('512007') || showAbilityBtn('512006'))
    );
  });
  const hasRecording = computed(() => {
    return (
      !props.onlyCall && !unref(isPerson) && usePermissions('eBC') && usePermissions('DISPATCH')
    );
  });
  const showSmsSend = computed(() => {
    const { category, serviceAccounts } = props.abilityData;
    let ret = false;
    const canSendSms = new Set([CategoryEnum.seat, CategoryEnum.terminal]);
    if (serviceAccounts) {
      serviceAccounts.forEach((item) => {
        if (canSendSms.has(Number(item.category))) {
          ret = true;
        }
      });
    }
    return ret || canSendSms.has(Number(category)) || unref(isPerson);
  });
  const isFavorite = computed(() => {
    const { category, id, resourceId } = unref(info);
    const categoryStr = (category || CategoryEnum.person).toString();
    const infoId = resourceId || id;
    const arr = favoriteStore.favoriteResources?.[categoryStr] || [];
    let isFavorite = false;
    arr.forEach((item) => {
      const itemId = item.resourceId || item.id;
      if (itemId === infoId) {
        isFavorite = true;
      }
    });
    return isFavorite;
  });
  const menuOptions = computed(() => {
    const arr = [
      ...(props.defaultDropMenu || []),
      {
        icon: 'phone_call',
        label: t('communication.communicationFunction.pointCall'),
        show: showAbilityBtn('512001') && !unref(showVoiceHalf),
        value: 'voiceCall',
      },
      {
        icon: 'video_call',
        label: t('communication.communicationFunction.videoPointCall'),
        show: showAbilityBtn('512006'),
        value: 'videoCall',
      },
      {
        icon: 'track_play',
        label: t('resource.policeTrackPlay.trackPlay'),
        show: showAbilityBtn('512005'),
        value: 'trackHistory',
      },
      {
        icon: 'track_realtime',
        label: t('resource.policeTrackPlay.trackRealTime'),
        show: showAbilityBtn('512005') && unref(isNotSeats),
        value: 'trackRealTime',
      },
      {
        icon: 'monitor_call',
        label: t('communication.communicationFunction.videoWatch'),
        show: showAbilityBtn('512007'),
        value: 'videoWatch',
      },
      {
        icon: 'voice_watch',
        label: t('communication.communicationFunction.voiceWatch'),
        show: showVoiceWatch('512001'),
        value: 'voiceWatch',
      },
      {
        icon: 'comm_msg',
        iconPrefix: 'bigScreen',
        label: t('communication.communicationFunction.shortMessage'),
        show: unref(showSmsSend),
        value: 'smsSend',
      },
      {
        icon: 'break_off',
        label: t('communication.communicationFunction.forcedDemolition'),
        show: unref(hasBreakOff),
        value: 'breakOff',
      },
      {
        icon: 'recordings',
        label: t('communication.communicationFunction.audioAndVideoRecordings'),
        show: unref(hasRecording),
        value: 'recordings',
        children: getCallTypeOptions(),
      },
      {
        icon: unref(isFavorite) ? 'btn_uncollected' : 'btn_collected',
        label: unref(isFavorite)
          ? t('monitor.monitorFunction.cancelCollection')
          : t('monitor.monitorFunction.cameraCollection'),
        show: showAbilityBtn('favorite'),
        value: 'favorite',
      },
    ];
    return arr.filter((i) => i.show);
  });

  function showAbilityBtn(value) {
    const { abilityValues } = props;
    const ret = unref(isPerson) || abilityValues.includes(value);
    return ret;
  }

  function showVoiceWatch(value) {
    const { abilityData } = props;
    const isRecorder = Number(abilityData.category) === CategoryEnum.recorder;
    const ret = isRecorder && showAbilityBtn(value);
    return ret;
  }

  function callClick(config) {
    emit('callClick', config);
  }

  function menuClick(val, childVal, event) {
    const refs = {
      breakOff,
      favorite,
      halfCall,
      recordings,
      smsSend,
      trackHistory,
      trackRealTime,
      videoCall,
      videoWatch,
      voiceCall,
      voiceWatch,
    };
    refs[val]?.value.trigger(event, childVal);
    emit('dropMenuSelect', val);
  }

  function dropMenuOpen(open: boolean) {
    emit('dropMenuOpen', open);
  }
</script>

<template>
  <div class="resource-ability">
    <!-- 半双工点呼 -->
    <VoiceHalfCallBtn
      v-if="showAbilityBtn('512001') && showVoiceHalf && dropMenu"
      ref="halfCall"
      :btn-type="btnType"
      :drop-menu="dropMenu"
      :info="info"
      :size="size"
    />

    <TdDropdownMenu
      v-if="dropMenu"
      :options="menuOptions"
      @click="menuClick"
      @drop-menu-open="dropMenuOpen"
    >
      <Icon name="drop_down_plan" />
    </TdDropdownMenu>

    <div v-show="!dropMenu" class="operations">
      <!-- 半双工点呼 -->
      <VoiceHalfCallBtn
        v-if="showAbilityBtn('512001') && showVoiceHalf && !dropMenu"
        ref="halfCall"
        :btn-type="btnType"
        :drop-menu="dropMenu"
        :info="info"
        :size="size"
      />

      <!-- 语音点呼 -->
      <VoiceCallBtn
        v-if="showAbilityBtn('512001') && !showVoiceHalf"
        ref="voiceCall"
        :account="account"
        :btn-type="btnType"
        :info="info"
        :only-call="onlyCall"
        :size="size"
        @call-click="callClick"
      />

      <!-- 视频点呼 -->
      <VideoCallBtn
        v-if="showAbilityBtn('512006')"
        ref="videoCall"
        :account="account"
        :btn-type="btnType"
        :info="info"
        :only-call="onlyCall"
        :size="size"
        @call-click="callClick"
      />

      <!-- 动向回放 -->
      <TrackHistoryBtn
        v-if="showAbilityBtn('512005')"
        ref="trackHistory"
        :alarm="alarm"
        :alarm-time="alarmTime"
        :btn-type="btnType"
        :info-data="info"
        :resource-type="resourceType"
        :show-equipment-tabs="showEquipmentTabs"
        :size="size"
      />

      <!-- 轨迹跟踪 -->
      <TrackRealTimeBtn
        v-if="showAbilityBtn('512005') && isNotSeats"
        ref="trackRealTime"
        :btn-type="btnType"
        :info-data="info"
        :size="size"
      />

      <!-- 视频查看/监控 -->
      <VideoWatchBtn
        v-if="showAbilityBtn('512007')"
        ref="videoWatch"
        :account="account"
        :btn-type="btnType"
        :info="info"
        :only-call="onlyCall"
        :resource-type="resourceType"
        :size="size"
      />

      <!-- 语音伴听 -->
      <VoiceWatchBtn
        v-if="showVoiceWatch('512001')"
        ref="voiceWatch"
        :btn-type="btnType"
        :info="info"
        :size="size"
        @call-click="callClick"
      />

      <!-- 短消息 -->
      <SmsSendBtn
        v-if="showSmsSend"
        ref="smsSend"
        :account="account"
        :btn-type="btnType"
        :info="info"
        :resource-type="resourceType"
        :size="size"
      />

      <!-- 强拆 -->
      <BreakOffBtn
        v-if="hasBreakOff"
        ref="breakOff"
        :account="account"
        :btn-type="btnType"
        :info="info"
        :size="size"
        @call-click="callClick"
      />

      <!-- 录音录像 -->
      <Recordings
        v-if="hasRecording"
        ref="recordings"
        :btn-type="btnType"
        :info="info"
        :size="size"
      />

      <!-- 收藏 -->
      <FavoriteSourceBtn
        v-if="showAbilityBtn('favorite')"
        ref="favorite"
        :btn-type="btnType"
        :info="info"
        :size="size"
        @call-click="callClick"
      />

      <!-- 组会 -->
      <ConferenceBtn v-if="isMain" :btn-type="btnType" :info="info" :size="size" />

      <VehicleBtn v-if="usePermissions('eBC')" :info="info" />

      <slot></slot>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .resource-ability {
    display: flex;
    align-items: center;
  }

  .operations {
    display: flex;
    flex-wrap: wrap;
    align-items: center;

    :deep(& > div) {
      min-width: 18px;
      margin: 8px;
    }
  }
</style>
