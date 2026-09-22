<script lang="ts" setup>
  import { computed, onMounted, ref, watch } from 'vue';

  import { Dialog } from '@/components/Dialog';
  import { appConfig } from '@/config';
  import { useI18n } from '@/hooks';
  import { monitorCall, voicePointCall } from '@/pages/resource/resourceHelper';
  import commOpt from '@/plugins/mspPlayer/commOpt';
  import { useCommunicationStore } from '@/store';
  import { isDef } from '@/utils/is';

  import CallWay from './callWay.vue';
  import { callWayDialogPosition } from './callWayPosition';

  const props = defineProps({
    btnType: {
      default: '',
      type: String,
    },
    info: {
      default: () => {},
      type: Object,
    },
    size: {
      default: '',
      type: String,
    },
  });
  defineExpose({ trigger: handleClick });
  const { t } = useI18n();
  const communicationStore = useCommunicationStore();

  const isActive = ref(false);
  const disable = ref(false);

  const isSelf = computed(() => {
    return appConfig.userData.id === props.info.id;
  });
  const iconName = computed(() => {
    return 'voice_watch';
  });

  watch(communicationStore.comm, () => {
    handlerComm();
  });

  watch(
    () => props.info,
    () => {
      handlerComm();
    },
  );

  onMounted(() => {
    handlerComm();
  });

  function handlerComm() {
    // 同一个人存在多个账号时
    // 语言点呼和视频点呼不可共存
    let comm: any = null;
    props.info?.serviceAccounts?.forEach((item) => {
      const { account } = item;
      const val = communicationStore.comm?.[account];
      if (val) {
        comm = val;
      }
    });

    if (!comm) {
      isActive.value = false;
      disable.value = false;
      return;
    }
    if (isDef(comm.voice) && isDef(comm.monitor)) {
      isActive.value = true;
      disable.value = false;
      return;
    }
    isActive.value = false;
    disable.value = isDef(comm.video) || isDef(comm.voice) || isDef(comm.monitor);
  }

  function handleClick(e) {
    const { info } = props;
    const accounts = info.serviceAccounts;
    if (accounts.length > 1) {
      if (isActive.value) {
        accounts.forEach((item) => {
          if (communicationStore.comm[item.account]) {
            commOpt.hangUp('voice', appConfig.isdn, item.account);
          }
        });
      } else {
        Dialog({
          cid: 'CallWay',
          content: CallWay,
          data: {
            onSelect: (account) => {
              voiceWatch(account);
            },
            serviceAccounts: accounts,
          },
          offset: callWayDialogPosition(e),
          zIndexDefault: 1300,
        });
      }
    } else {
      const { account } = accounts[0] || {};
      if (isActive.value) {
        commOpt.hangUp('monitor', appConfig.isdn, account);

        setTimeout(() => {
          commOpt.hangUp('voice', appConfig.isdn, account);
        });
      } else {
        voiceWatch(account);
      }
    }
  }

  function voiceWatch(account) {
    const { info } = props;
    monitorCall({ ...info, account });
    setTimeout(() => {
      voicePointCall(account);
    }, 500);
  }
</script>

<template>
  <!-- 语音监控 -->
  <div class="voice-watch-btn">
    <TdTooltip
      :content="t('communication.communicationFunction.voiceWatch')"
      :open-delay="500"
      placement="top"
      popper-class="el-tooltip-style"
      :visible-arrow="false"
    >
      <TdButton
        :class="{
          active: isActive,
        }"
        :disable="isSelf || disable"
        :icon-name="iconName"
        :size="size"
        :type="btnType"
        @click.stop="handleClick"
      />
    </TdTooltip>
  </div>
</template>

<style lang="less" scoped>
  .voice-watch-btn {
    display: flex;
    align-items: center;
  }

  .active {
    background-color: var(--button-color-warn-default);
  }
</style>
