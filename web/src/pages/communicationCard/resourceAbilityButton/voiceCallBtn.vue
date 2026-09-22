<script lang="ts" setup>
  import { computed, onMounted, ref, unref, watch } from 'vue';

  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { CategoryEnum } from '@/enums';
  import { useEmitter, useI18n } from '@/hooks';
  import { queryResourceDetailsByInfo, voicePointCall } from '@/pages/resource/resourceHelper';
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
  defineExpose({ trigger: handleClick });
  const { t } = useI18n();
  const communicationStore = useCommunicationStore();

  const isActive = ref(false);
  const disable = ref(false);

  const isSelf = computed(() => {
    return appConfig.userData.id === props.info.id;
  });
  const iconName = computed(() => {
    if (props.onlyCall) {
      return 'phone_call';
    }
    return unref(isActive) ? 'phone_hang_up' : 'phone_call';
  });
  const btnTypeName = computed(() => {
    const { btnType } = props;
    if (btnType === 'icon') {
      return btnType;
    }
    return unref(isActive) && !props.onlyCall ? 'radioWarn' : btnType;
  });

  watch(() => props.info, handlerComm, { immediate: true });

  useEmitter('commUpdate', handlerComm);

  onMounted(() => {
    handlerComm();
  });

  /**
   * 同一个人存在多个账号时
   * 语言点呼和视频点呼不可共存
   */
  function handlerComm() {
    const { serviceAccounts } = props.info || {};
    if (serviceAccounts?.length > 1) {
      return;
    }

    let comm: any = {};
    serviceAccounts?.forEach((item) => {
      const { account } = item;
      const val = communicationStore.comm?.[account];
      if (val) {
        comm = val;
      }
    });

    const { video, voice } = comm;
    isActive.value = isDef(voice);
    disable.value = isDef(video);
  }

  // 拨打、挂断
  async function handleClick(e) {
    const { DIALOUT_PREFIX = '' } = appConfig.settingData;
    const call = (account) => {
      if (unref(isActive)) {
        if (props.onlyCall) {
          return;
        }
        commOpt.hangUp('voice', appConfig.isdn, account);
      } else {
        // 判断是否已经通话
        if (commOpt.isAlreadyCommForSend('voice', account)) {
          return;
        }
        voicePointCall(account, account.startsWith(DIALOUT_PREFIX));
      }
    };

    if (props.account) {
      call(props.account);
      return;
    }

    const { info } = props;
    const details = await queryResourceDetailsByInfo(info);

    const { account, phoneNum, serviceAccounts = [] } = details;

    if (!account) {
      Message(t('communication.communicationTips.communicationAccountNotAssociated'));
      return;
    }

    if (info.category === CategoryEnum.person && phoneNum) {
      serviceAccounts.push({
        account: phoneNum,
        typeId: 'phone',
        typeName: t('resource.detailType.phone'),
      });
    }

    if (serviceAccounts.length > 1) {
      if (unref(isActive)) {
        if (props.onlyCall) {
          return;
        }
        serviceAccounts.forEach((item) => {
          if (communicationStore.comm[item.account]) {
            commOpt.hangUp('voice', appConfig.isdn, item.account);
          }
        });
      } else {
        // 判断是否已经通话
        if (commOpt.isAlreadyCommForSend('voice', account)) {
          return;
        }
        Dialog({
          cid: 'CallWay',
          content: CallWay,
          data: {
            onSelect: (account, item) => {
              if (item.typeId === 'phone') {
                voicePointCall(DIALOUT_PREFIX + account, true);
              } else {
                voicePointCall(account);
              }
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
  <!-- 语音点呼 -->
  <TdTooltip :content="t('communication.communicationFunction.pointCall')" placement="top">
    <TdButton
      :active="isActive"
      :class="{
        active: isActive,
      }"
      :disable="isSelf || disable"
      :icon-name="iconName"
      :size="size"
      :type="btnTypeName"
      @click.stop="handleClick"
    />
  </TdTooltip>
</template>

<style lang="less" scoped>
  .voice-call-btn {
    display: flex;
    align-items: center;
  }
</style>
