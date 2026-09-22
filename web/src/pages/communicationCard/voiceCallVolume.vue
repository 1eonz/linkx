<script lang="ts" setup>
  import { onMounted, ref, watch } from 'vue';

  import { appConfig } from '@/config';
  import { useI18n } from '@/hooks';
  import commOpt from '@/plugins/mspPlayer/commOpt';
  import { communicationStatus } from '@/plugins/mspPlayer/commStatus';
  import storeCidIsdnRelation from '@/plugins/mspPlayer/storeCidIsdnRelation';
  import { useCommunicationStore } from '@/store';

  const props = defineProps({
    commuId: {
      default: '',
      type: String,
    },
    showBtn: {
      default: true,
      type: Boolean,
    },
    voiceCommuData: {
      default: () => {},
      type: Object,
    },
  });
  const { t } = useI18n();
  const communicationStore = useCommunicationStore();

  const showCallTimer = ref(false);
  const callStatus = ref(null);
  const cid = ref('');
  const voiceStatus = ref('');
  const showStatus = ref(true);

  watch(communicationStore.comm, (val) => {
    const { commuId } = props;
    const { isdn, opt, type } = val.updateInfo;
    showStatus.value = Boolean(val[commuId]?.voice);

    if (isdn !== commuId || type !== 'voice') {
      return;
    }

    if (opt === 'delete') {
      callStatus.value = null;
      showCallTimer.value = false;
      return;
    }

    const audioSta = val[isdn].voice;
    const { CALLING } = communicationStatus();
    voiceStatus.value = audioSta?.status?.status;
    if (audioSta.status.status === CALLING.status) {
      if (storeCidIsdnRelation.getCidByIsdn(commuId, 'voice')) {
        cid.value = storeCidIsdnRelation.getCidByIsdn(commuId, 'voice');
      }
      showCallTimer.value = true;
      callStatus.value = null;
    } else {
      callStatus.value = audioSta.status.msg;
      showCallTimer.value = false;
    }
  });
  watch(
    () => props.commuId,
    (val) => {
      showStatus.value = Boolean(communicationStore.comm[val]?.voice);
    },
  );

  onMounted(() => {
    const { commuId } = props;
    if (storeCidIsdnRelation.getCidByIsdn(commuId, 'voice')) {
      cid.value = storeCidIsdnRelation.getCidByIsdn(commuId, 'voice');
    }

    const _commu = communicationStore.comm[commuId];
    if (_commu?.voice) {
      const audioStatus = _commu;
      const { CALLING } = communicationStatus();
      if (!audioStatus || !audioStatus.voice) {
        return;
      }
      if (audioStatus.voice.status === CALLING.status) {
        showCallTimer.value = true;
      } else {
        callStatus.value = audioStatus.voice.status.msg;
      }
    }
  });

  async function answer() {
    const fromId = props.commuId;
    const toId = appConfig.isdn;
    commOpt.answer('voice', fromId, toId);
  }
  function hangUpClick() {
    const { commuId } = props;
    if (!commuId) {
      console.log('commuId is null');
      return;
    }
    commOpt.hangUp('voice', appConfig.isdn, commuId);
  }
</script>

<template>
  <div class="voice-call-volume">
    <span v-show="callStatus && showStatus" class="call-status">
      {{ callStatus }}
    </span>
    <TdCallTimer v-if="showCallTimer" class="call-timer" :isdn="props.commuId" type="voice" />
    <template v-if="showBtn">
      <!-- 本端 -->
      <div class="hang-up-div">
        <TdTooltip
          :content="t('communication.communicationFunction.hangUpPointCall')"
          placement="top"
        >
          <TdButton icon-name="phone_hang_up" type="radioWarn" @click="hangUpClick" />
        </TdTooltip>
      </div>
      <!-- 来电，同时isdn是登录人的 -->
      <div
        v-if="voiceCommuData && voiceCommuData.type === 'voice' && voiceStatus === 'incoming'"
        class="voice-answer"
      >
        <TdTooltip :content="t('communication.communicationFunction.answerCall')" placement="top">
          <TdButton icon-name="phone_hang_up" type="radioWarn" @click="answer" />
        </TdTooltip>
      </div>
    </template>
  </div>
</template>

<style lang="less" scoped>
  .voice-call-volume {
    display: flex;
    align-items: center;

    .voice-answer {
      margin-left: 7px;
    }

    .call-status {
      display: inline-block;
      margin-right: 10px;
      overflow: hidden;
      text-align: left;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .call-timer {
      margin-right: 15px;
    }
  }
</style>
