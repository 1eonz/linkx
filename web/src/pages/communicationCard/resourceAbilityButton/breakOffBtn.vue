<script lang="ts" setup>
  import { computed } from 'vue';

  import { Dialog } from '@/components/Dialog';
  import { appConfig } from '@/config';
  import { useI18n } from '@/hooks';
  import { getAccountByEquipmentData } from '@/pages/resource/resourceHelper';
  import { voiceFunc } from '@/plugins/mspPlayer';
  import commOpt from '@/plugins/mspPlayer/commOpt';

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
    size: {
      default: '',
      type: String,
    },
  });

  const { t } = useI18n();

  defineExpose({ trigger: handleClick });
  const isSelf = computed(() => {
    return appConfig.userData.id === props.info.id;
  });

  function handleClick(e) {
    const call = (account) => {
      if (account) {
        voiceFunc.voiceBreakOff(account);
      } else {
        commOpt.isCommReady(null);
      }
    };

    if (props.account) {
      call(props.account);
      return;
    }

    const { serviceAccounts } = props.info;
    if (serviceAccounts?.length > 1) {
      Dialog({
        cid: 'CallWay',
        content: CallWay,
        data: {
          onSelect: (account) => {
            voiceFunc.voiceBreakOff(account);
          },
          serviceAccounts,
        },
        offset: callWayDialogPosition(e),
        zIndexDefault: 1300,
      });
    } else {
      const account = getAccountByEquipmentData(props.info);
      call(account);
    }
  }
</script>

<template>
  <!-- 强拆 -->
  <div class="break-off-btn">
    <TdTooltip :content="t('communication.communicationFunction.forcedDemolition')" placement="top">
      <TdButton
        :disable="isSelf"
        icon-name="break_off"
        :size="size"
        :type="btnType"
        @click.stop="handleClick"
      />
    </TdTooltip>
  </div>
</template>

<style lang="less" scoped></style>
