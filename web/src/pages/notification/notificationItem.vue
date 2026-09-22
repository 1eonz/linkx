<script lang="ts" setup>
  import { computed, onBeforeUnmount, onMounted, ref, unref, watch, watchEffect } from 'vue';

  import { queryConference } from '@/api/conference';
  import { findSubscribeTalkingGroup } from '@/api/group';
  import { openGroupDetailsPopup } from '@/comm/group';
  import { Message } from '@/components/Message';
  import { appConfig, resourceMapConfig } from '@/config';
  import { useEmitter, useI18n, useSetInterval, useUtils } from '@/hooks';
  import VoiceHalfCallBtn from '@/pages/communicationCard/resourceAbilityButton/voiceHalfCallBtn.vue';
  import { openVideoPointCallPopup } from '@/pages/map/openVideoPopup';
  import { getInfoByAccount } from '@/pages/resource/resourceHelper';
  import { conferenceCardDialog } from '@/pages/videoConference/common';
  import { confFunc, groupFunc } from '@/plugins/mspPlayer';
  import commOpt from '@/plugins/mspPlayer/commOpt';
  import { communicationStatus } from '@/plugins/mspPlayer/commStatus';
  import {
    useCommunicationStore,
    useConferenceStore,
    useMonitorStore,
    useResourceStore,
  } from '@/store';
  import { delay } from '@/utils';
  import { isArray, isEmpty } from '@/utils/is';

  import { cloneDeep, throttle } from 'lodash-es';

  import QuickReply from '../communicationCard/sms/quickReply.vue';

  const props = defineProps({
    data: {
      default: () => {},
      type: Object,
    },
    holdList: {
      default: () => [],
      type: Array,
    },
  });
  const emit = defineEmits(['notificationItemLeave', 'notificationItemHover']);

  const { t } = useI18n();
  const conferenceStore = useConferenceStore();
  const resourceStore = useResourceStore();
  const communicationStore = useCommunicationStore();

  const microphoneStyle = ref({ 'box-shadow': ' 0 0 0 2px rgba(8,199,169, .4)' });
  const replyList = resourceMapConfig.quickReplyList;
  const userName = ref(t('common.unKnown'));
  const speakerName = ref('');
  const customReplyShow = ref(false);
  const itemData = ref<any>({});
  let clearTimer: any = null;
  const callerInfo = ref<any>();
  const isHold = ref(false);

  const isdn = computed(() => props.data.isdn);

  watchEffect(() => {
    const { comm } = communicationStore;
    const { isdn, opt, status, type } = comm.updateInfo;

    if (!isdn || isdn !== unref(itemData).isdn || type !== unref(itemData).type) {
      return;
    }

    const valStatus = status?.status.status;
    const { CALLING } = communicationStatus();

    if (opt === 'update') {
      itemData.value.status = status.status;
      if (valStatus === CALLING.status && itemData.value.value.isHold) {
        itemData.value.value.isHold = false;
      }
      // 群组的
      if (type === 'group' && opt === 'add') {
        const val = cloneDeep(comm);
        subCall(val);
      }
    }
  });

  watch(
    () => props.data,
    (val) => {
      itemData.value = Object.assign(val, { value: val.value || {} });
      getUserName(val.isdn);
      isHold.value = props.holdList.includes(itemData.value.isdn);
      if (val.status.status === 'selfSpeaking' && !clearTimer) {
        clearTimer = useSetInterval(() => {
          const flag = Math.floor(Math.random() * 5);
          microphoneStyle.value = {
            'box-shadow': ` 0 0 0 ${flag}px rgba(8,199,169, .4)`,
          };
        }, 200);
      }
    },
    { immediate: true },
  );

  watch(
    () => props.holdList,
    (val) => {
      isHold.value = val.includes(itemData.value.isdn);
    },
    { deep: true, immediate: true },
  );

  /**
   * 接听
   * 如果同时有多个需要接听时，接听一个后必须延时几秒才能接听下一个，否则会没有画面
   */
  const answer = throttle(async () => {
    const toId = appConfig.isdn;
    const { isdn, type } = unref(itemData);
    const { cid, status } = conferenceStore.confer;
    const isConf = ['audioConfer', 'videoConfer'].includes(type);

    if (status && status !== 'end' && status !== 'failed') {
      if (isConf) {
        useEmitter().emit('goConfTab');
        acceptConf(type, cid);
      } else {
        Message({
          message: t('videoConference.conferenceInfo.meeting'),
          type: 'warning',
        });
        hangUp();
      }
      return;
    }

    await commOpt.answer(type, isdn, toId);

    if (type !== 'voice') {
      await delay(200);
      openCard();
    }
  }, 500);

  onMounted(async () => {
    const { isdn, isGroup, speakerName, type, value } = unref(itemData);
    if (type === 'audioConfer' || type === 'videoConfer') {
      userName.value = unref(itemData).value.caller;
    } else if (type === 'group') {
      const res = await getGroupInfo(isdn);
      userName.value = res.name;
    } else {
      if (isGroup) {
        const res = await getGroupInfo(isdn);
        userName.value = res.name;
        resourceStore.unReadMessage({
          isdn,
          value: {
            isGroup,
            speakerName,
            type,
          },
        });
      } else {
        const res = await getResourceInfo(isdn);
        userName.value = res?.name || isdn;
      }
    }

    if (speakerName) {
      speakerName.value = speakerName;
    }

    // 视频回传是否需要接听
    if (
      appConfig.settingData.VIDEO_RETURN_CONFIRM === '0' &&
      type === 'monitor' &&
      value.dispatch
    ) {
      answer();
    }
  });

  onBeforeUnmount(() => {
    clearTimer?.();
  });

  function unHold() {
    const { value } = unref(itemData.value);
    commOpt.voiceUnhold(value.cid);
  }

  async function getUserName(isdn) {
    const res = await getResourceInfo(isdn);
    userName.value = res?.name || isdn;
  }

  async function subCall(val) {
    const { isdn, type } = unref(itemData);
    const param = {
      groupId: isdn,
      userId: appConfig.isdn,
    };
    const { code, data } = await findSubscribeTalkingGroup(param);
    if (Number(code) === 0 && isArray(data) && data.length > 0) {
      const speakerFixed = Number(data[0].speakerFixed);
      if (type === 'group' && speakerFixed === 1) {
        const groupStatus = val[isdn][type].status;
        const { GROUP_IDLE, GROUP_REJECTED, GROUP_SPEAKING } = communicationStatus();
        if (groupStatus === GROUP_SPEAKING.status) {
          speakerName.value = val[isdn][type].speakerName;
        } else if (groupStatus === GROUP_IDLE.status || groupStatus === GROUP_REJECTED.status) {
          speakerName.value = groupStatus.msg;
        }
      }
    }
  }

  function closeQuickReply() {
    if (customReplyShow.value === true) {
      customReplyShow.value = false;
    }
  }

  // 挂断当前通话并与接入通话
  const callReject = throttle(async () => {
    const toId = appConfig.isdn;
    const { isdn, type } = unref(itemData);

    setTimeout(() => {
      commOpt.answerAndReject(type, isdn, toId);
    }, 0);
  }, 3000);

  // 保持当前通话并与接入通话
  const callAndHold = throttle(async () => {
    if (props.holdList.length === 8) {
      Message(t('communication.communicationTips.holdFailure'));
      return;
    }
    const toId = appConfig.isdn;
    const { isdn, type } = unref(itemData);

    setTimeout(() => {
      commOpt.voiceHold(type, isdn, toId);
    }, 0);
  }, 3000);

  function showQuickReply() {
    customReplyShow.value = true;
  }

  function addReply(reply) {
    if (resourceMapConfig.quickReplyList.includes(reply)) {
      Message(t('homePage.noticeCenterData.quickReplyExists'));
    } else {
      resourceMapConfig.quickReplyList.push(reply);
    }
  }

  function deleteReply(reply) {
    const index = resourceMapConfig.quickReplyList.indexOf(reply);
    resourceMapConfig.quickReplyList.splice(index, 1);
  }

  function itemHover() {
    emit('notificationItemHover');
  }

  function itemLeave() {
    emit('notificationItemLeave');
  }

  function quickReply() {
    // 回复后缩小
    itemLeave();
    customReplyShow.value = false;
  }

  // 单击 打开群组卡片
  function showCommunicationCard() {
    const { addCommunicateCallId, comm, communicateCallId } = communicationStore;
    if (comm?.updateInfo?.type !== 'group') {
      return;
    }
    const { isdn } = itemData.value;
    if (itemData.value?.type === 'shortMessage') {
      const has = communicateCallId.includes(isdn);
      if (!has) {
        addCommunicateCallId(isdn);
      }
    }
    // 打开卡片
    openCard();
  }

  // 通信类型
  function getItemTypeName(data) {
    if (data.value?.dispatch) {
      return t('homePage.noticeCenterData.videoReturn');
    }
    switch (data.type) {
      case 'audioConfer': {
        return `${t('videoConference.conferenceButton.voiceConference')}-`;
      }
      case 'group': {
        return '';
      }
      case 'halfdial': {
        return t('homePage.noticeCenterData.halfDial');
      }
      case 'monitor': {
        return `${t('homePage.noticeCenterData.playVideo')}-`;
      }
      case 'shortMessage': {
        return '';
      }
      case 'video': {
        return `${t('homePage.noticeCenterData.P2Pvideo')}-`;
      }
      case 'videoConfer': {
        return `${t('videoConference.conferenceButton.videoConference')}-`;
      }
      case 'voice': {
        return `${t('homePage.noticeCenterData.P2Pvoice')}-`;
      }
      default: {
        return '';
      }
    }
  }

  // 获取状态的名称
  function getItemStatusName(status) {
    const { type } = itemData.value;
    if (type === 'group') {
      return '';
    } else if (type === 'videoConfer' || type === 'audioConfer') {
      return t('communication.communicationStatus.incoming');
    }
    return status.msg ? `${status.msg}` : '';
  }

  // 组呼按钮按下
  function groupCallMouseDown() {
    groupFunc.pttGroup(itemData.value.isdn);
  }

  function groupCallMouseUp() {
    microphoneStyle.value = { 'box-shadow': ' 0 0 0 2px rgba(8,199,169, .4)' };
    clearTimer?.();
    clearTimer = null;
    groupFunc.pttreleaseGroup(itemData.value.isdn);
  }

  function hangUp() {
    const { detMonitorCallId, detVideoCallId, monitorCallId, videoCallId } = communicationStore;
    const { isdn, type, value } = itemData.value;
    const isConf = ['audioConfer', 'videoConfer'].includes(type);
    if (isConf) {
      const { cid } = value;
      rejectConf(type, cid);
    } else {
      commOpt.hangUp(type, appConfig.isdn, isdn);
      const { id } = unref(callerInfo);
      if (videoCallId.includes(id)) {
        detVideoCallId(id);
      }
      if (monitorCallId.includes(id)) {
        detMonitorCallId(id);
      }
    }
  }

  async function openCard() {
    const { isdn, isGroup, type } = unref(itemData);
    const res = await getResourceInfo(isdn);
    const autoPlay = { type };
    let simple = true;

    if (type === 'shortMessage') {
      autoPlay.type = 'message';
      simple = false;
    }
    if (type === 'group' || isGroup) {
      openGroupDetailsPopup({
        id: isdn,
        simple,
      });
    } else if (type === 'monitor') {
      useMonitorStore().addMonitorDrawerData({ ...res, account: isdn, comm: unref(itemData) });
    } else {
      openVideoPointCallPopup({ ...res });
    }
  }

  async function getGroupInfo(groupId): Promise<any> {
    let ret = {};
    // 查看群组详情
    const result = await groupFunc.queryGroupInfo(groupId);
    if (result.rsp === '0') {
      ret = result.value;
    }
    return ret;
  }

  async function getResourceInfo(account) {
    const info = await getInfoByAccount(account);
    if (isEmpty(info)) {
      Object.assign(info, {
        account,
        id: account,
        name: account,
      });
    }
    callerInfo.value = info;
    return info;
  }

  // 拒接会议
  async function rejectConf(rsp, cid) {
    if (rsp === 'videoConfer') {
      confFunc.rejectVideoConf(cid);
    } else {
      confFunc.rejectAudioConf(cid);
    }
  }

  // 接通会议
  async function acceptConf(rsp, cid) {
    const { confer, setUniqueCode } = conferenceStore;
    const { code, data } = await queryConference({ confId: confer.conferenceId });
    if (code === 0) {
      setUniqueCode(data.uniqueCode);
    }
    const { isCommPanel } = useUtils();
    if (!isCommPanel) {
      conferenceCardDialog(true);
    }

    if (rsp === 'videoConfer') {
      confFunc.acceptVideoConf(cid);
    } else {
      confFunc.acceptAudioConf(cid);
    }
  }
</script>

<template>
  <!-- 保持列表框 -->
  <div v-if="isHold" class="hold-item">
    <div class="item-name">
      <div class="user-name">{{ userName }}</div>
    </div>
    <div class="item-icon">
      <TdTooltip :content="t('mrs.hold.holdCancel')" placement="top">
        <Icon class="btn-item" name="unhold" @click="unHold" />
      </TdTooltip>
      <TdTooltip :content="t('videoConference.confFunc.hangUp')" placement="top">
        <Icon class="btn-item" name="hold_reject" @click="hangUp" />
      </TdTooltip>
    </div>
  </div>
  <div
    v-else
    class="notification-item common-list-item"
    @click.stop="showCommunicationCard"
    @mouseleave="itemLeave"
    @mouseover="itemHover"
  >
    <div class="item-name">
      <div class="user-name">{{ userName }}</div>
      <!-- 通信类型+名称 -->
      <div
        v-if="itemData.status.status !== 'calling' && itemData.type !== 'group'"
        class="right-bottom type-name"
        :class="itemData.status.status === 'incoming' ? 'incoming-status' : ''"
      >
        {{ getItemTypeName(itemData) + getItemStatusName(itemData.status) }}
      </div>
      <div v-if="itemData.type === 'group' || itemData.isGroup" class="right-bottom type-name">
        {{ speakerName }}
      </div>
      <!-- 摄像头或视频点呼计时 -->
      <div
        v-if="
          ['monitor', 'video', 'videoConfer'].includes(itemData.type) &&
          itemData.status.status === 'calling'
        "
        class="right-bottom"
      >
        <Icon class="type-icon" name="video_call" />
        <TdCallTimer class="time" :isdn="isdn" :type="itemData.type" />
      </div>
      <!-- 语音计时 -->
      <div
        v-if="['voice', 'halfdial'].includes(itemData.type) && itemData.status.status === 'calling'"
        class="right-bottom"
      >
        <TdCallTimer class="time" :isdn="isdn" :type="itemData.type" />
      </div>
    </div>

    <div
      v-if="itemData.type === 'voice' && itemData.status.status === 'calling'"
      class="voice-point-call-operation"
    >
      <TdMicrophone :cid="itemData.value.cid" :in-flag="true" />
      <TdVolumeRange :cid="itemData.value.cid" />
    </div>

    <div class="operation">
      <!-- 话筒按钮 -->
      <div
        v-if="itemData.type === 'group'"
        class="btn-item"
        @mousedown="groupCallMouseDown"
        @mouseup="groupCallMouseUp"
      >
        <TdButton icon-name="microphone_voice" type="radioSuccess" />
      </div>
      <!-- 语音接听按钮 -->
      <TdButton
        v-if="
          ['voice', 'audioConfer'].includes(itemData.type) &&
          itemData.status.status === 'incoming' &&
          !itemData.value.isHold
        "
        class="btn-item answer-btn"
        icon-name="phone_call"
        type="radioSuccess"
        @click="answer"
      />
      <!-- 视频接听按钮 -->
      <TdButton
        v-if="
          ['monitor', 'video', 'videoConfer'].includes(itemData.type) &&
          itemData.status.status === 'incoming'
        "
        class="btn-item answer-btn"
        icon-name="video_call"
        type="radioSuccess"
        @click="answer"
      />
      <!-- 接听并挂断当前通话按钮 -->
      <TdTooltip
        v-if="itemData.value.isHold"
        :content="t('mrs.hold.rejectAndCall')"
        placement="top"
      >
        <TdButton class="hold-btn-item" icon-name="call_reject" type="icon" @click="callReject" />
      </TdTooltip>
      <!-- 挂断按钮 -->
      <TdTooltip
        v-if="
          ['voice', 'video', 'videoConfer', 'audioConfer'].includes(itemData.type) &&
          itemData.type !== 'group'
        "
        :content="t('videoConference.confFunc.hangUp')"
        placement="top"
      >
        <TdButton class="btn-item" icon-name="phone_hang_up" type="radioWarn" @click="hangUp" />
      </TdTooltip>
      <!-- 接听并保持当前通话按钮 -->
      <TdTooltip v-if="itemData.value.isHold" :content="t('mrs.hold.holdAndCall')" placement="top">
        <TdButton
          class="hold-btn-item"
          icon-name="call_and_hold"
          type="icon"
          @click="callAndHold"
        />
      </TdTooltip>
      <!-- 半双工点呼 -->
      <VoiceHalfCallBtn
        v-if="itemData.type === 'halfdial' && callerInfo"
        btn-type="radio"
        :info="callerInfo"
      />
      <!-- 叉叉按钮 -->
      <TdButton
        v-if="itemData.type === 'monitor'"
        class="btn-item"
        icon-name="reject"
        type="radioWarn"
        @click="hangUp"
      />
      <!-- 快速回复按钮 -->
      <TdButton
        v-if="itemData.type === 'shortMessage'"
        class="btn-reply"
        type="normal"
        @click.stop="showQuickReply"
      >
        {{ t('homePage.noticeCenterData.quickReply') }}
      </TdButton>
      <QuickReply
        v-if="customReplyShow"
        class="quick-reply"
        :reply-list="replyList"
        @add-reply="addReply"
        @close-quick-reply="closeQuickReply"
        @delete-reply="deleteReply"
        @quick-reply="quickReply"
      />
    </div>
  </div>
</template>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  .notification-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 290px;
    height: 60px;
    padding: 10px;
    margin-bottom: 10px;
    cursor: pointer;

    .item-name {
      display: flex;
      flex-direction: column;
      justify-content: space-around;
      width: 220px;
      height: 40px;

      .user-name {
        .ellipsis1();

        width: 100%;
        font-size: 16px;
        font-weight: bolder;
        line-height: 16px;
        color: var(--text-default);
      }

      .right-bottom {
        .ellipsis1();

        display: flex;
        align-items: center;
        width: 100%;
        height: 14px;
        font-size: 12px;
        line-height: 12px;

        .type-icon {
          width: 18px;
          height: 18px;
          fill: var(--text-color-minor);
        }

        .time {
          color: var(--text-color-minor);
        }
      }

      .type-name {
        display: block;
        color: var(--text-color-minor);
        text-align: left;
      }
    }

    .voice-point-call-operation {
      display: flex;
      margin: 12px 0;

      :deep(.td-button),
      :deep(.voice-control) {
        width: 22px;
        height: 22px;
        padding: 0;
        margin: 0 16px;

        svg {
          width: 16px !important;
          height: 16px !important;
        }
      }
    }

    .operation {
      position: relative;
      display: flex;
      align-items: center;

      .btn-item {
        margin-left: 10px;
      }

      .hold-btn-item {
        width: 50px;
        height: 50px;

        :deep(.icon) {
          width: 50px;
          height: 50px;
        }
      }

      .btn-reply {
        width: 80px;
        font-size: 14px;
      }

      :deep(.voice-single-call-card) {
        margin-right: 10px;
      }

      .quick-reply {
        position: absolute;
        right: 10px;
      }
    }
  }

  .hold-item {
    display: flex;
    align-items: center;
    width: 260px;
    height: 52px;
    padding: 10px;
    margin-bottom: 10px;
    background: linear-gradient(270deg, rgb(4 64 105 / 90%) 0%, rgb(6 41 74 / 90%) 100%);
    border: 2px solid rgb(0 194 255 / 100%);

    .item-name {
      display: flex;
      flex-direction: column;
      justify-content: space-around;
      min-width: 180px;
      height: 40px;

      .user-name {
        .ellipsis1();

        width: 100%;
        font-size: 14px;
        font-weight: 500;
        line-height: 22px;
        color: rgb(255 255 255 / 100%);
        letter-spacing: 0;
      }
    }

    .item-icon {
      display: flex;
      height: 25px;

      .btn-item {
        width: 25px;
        height: 24px;
        margin-left: 5px;
        cursor: pointer;
      }
    }
  }
</style>
