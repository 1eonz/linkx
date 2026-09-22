<script lang="ts" setup>
  import { computed, onMounted, ref, unref, watch } from 'vue';

  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { useEmitter, useI18n } from '@/hooks';
  import { queryResourceDetailsByInfo, videoPointCall } from '@/pages/resource/resourceHelper';
  import commOpt from '@/plugins/mspPlayer/commOpt';
  import { useCommunicationStore } from '@/store';
  import { isDef } from '@/utils/is';

  import CallWay from './callWay.vue';
  import { callWayDialogPosition } from './callWayPosition';

  const props = defineProps({
    account: {
      default: '',
      type: String,
    },
    btnType: {
      default: '',
      type: String,
    },
    info: {
      default: () => {},
      type: Object,
    },
    onlyCall: Boolean,
    size: {
      default: '',
      type: String,
    },
  });
  defineExpose({ trigger: monitorPointCall });
  const { t } = useI18n();
  const communicationStore = useCommunicationStore();

  const isActive = ref(false);
  const disable = ref(false);

  const iconName = computed(() => {
    if (props.onlyCall) {
      return 'video_call';
    }
    return unref(isActive) ? 'phone_hang_up' : 'video_call';
  });
  const btnTypeName = computed(() => {
    const { btnType } = props;
    if (btnType === 'icon') {
      return btnType;
    }
    return unref(isActive) && !props.onlyCall ? 'radioWarn' : btnType;
  });
  const serviceAccounts = computed(() => {
    return props.info?.serviceAccounts || [];
  });
  const isSelf = computed(() => {
    return appConfig.userData.id === props.info.id;
  });

  watch(
    () => props.info,
    () => {
      handlerComm();
    },
    { immediate: true },
  );

  onMounted(() => {
    handlerComm();
    useEmitter('commUpdate', handlerComm);
  });

  /**
   * 同一个人存在多个账号时
   * 视频点呼 与 语言点呼and视频查看 不可共存
   */
  function handlerComm() {
    if (unref(serviceAccounts).length > 1) {
      return;
    }

    let comm: any = {};
    unref(serviceAccounts).forEach((item) => {
      const { account } = item;
      const val = communicationStore.comm[account];
      if (val) comm = val;
    });

    const { monitor, video, voice } = comm;
    const onlyOne = unref(serviceAccounts).length === 1;
    isActive.value = isDef(video);
    disable.value = (isDef(voice) || isDef(monitor)) && onlyOne;
  }

  // 视频点呼
  async function monitorPointCall(e) {
    const details = await queryResourceDetailsByInfo(props.info);
    const { onlyCall } = props;
    const call = (account) => {
      if (unref(isActive)) {
        if (onlyCall) {
          return;
        }
        commOpt.hangUp('video', appConfig.isdn, account);
      } else {
        // 判断是否已经通话
        if (commOpt.isAlreadyCommForSend('video', account)) {
          return;
        }
        videoPointCall({ ...details, account });
      }
    };

    if (props.account) {
      call(props.account);
      return;
    }

    const { account, serviceAccounts } = details;
    if (!account) {
      Message(t('communication.communicationTips.communicationAccountNotAssociated'));
      return;
    }

    if (serviceAccounts?.length > 1) {
      if (unref(isActive)) {
        if (onlyCall) {
          return;
        }
        serviceAccounts.forEach((item) => {
          if (communicationStore.comm[item.account]) {
            commOpt.hangUp('video', appConfig.isdn, item.account);
          }
        });
      } else {
        // 判断是否已经通话
        if (commOpt.isAlreadyCommForSend('video', account)) {
          return;
        }
        Dialog({
          cid: 'CallWay',
          content: CallWay,
          data: {
            onSelect: (account) => {
              videoPointCall({ ...details, account });
            },
            serviceAccounts,
          },
          offset: callWayDialogPosition(e),
          zIndexDefault: 1300,
        });
      }
    } else {
      call(account);
    }
  }
</script>

<template>
  <!-- 视频点呼 -->
  <TdTooltip :content="t('communication.communicationFunction.videoPointCall')" placement="top">
    <TdButton
      :active="isActive"
      :class="{
        active: isActive,
      }"
      :disable="isSelf || disable"
      :icon-name="iconName"
      :size="size"
      :type="btnTypeName"
      @click.stop="monitorPointCall"
    />
  </TdTooltip>
</template>

<style lang="less" scoped>
  .video-call-btn {
    display: flex;
    align-items: center;
  }
</style>
