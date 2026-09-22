<script setup lang="ts">
  import { useRouter } from 'vue-router';

  import { useI18n } from '@/hooks';
  import { getCallTypeOptions } from '@/pages/mrs/common';
  import { getAccountByEquipmentData } from '@/pages/resource/resourceHelper';
  import { useCommunicateDispatchStore } from '@/store';

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
  defineExpose({ trigger });

  const { t } = useI18n();
  const { setCurActiveDeskTop } = useCommunicateDispatchStore();
  const router = useRouter();

  const menuOptions = getCallTypeOptions();

  function menuClick(callType) {
    trigger(null, callType);
  }

  function trigger(_, callType) {
    const account = getAccountByEquipmentData(props.info);
    setCurActiveDeskTop('MonitorDesktop');
    router.push({
      path: '/communicationCenter',
      query: {
        account,
        callType,
        time: Date.now(),
      },
    });
  }
</script>

<template>
  <TdDropdownMenu :options="menuOptions" @click="menuClick">
    <TdTooltip
      :content="t('communication.communicationFunction.audioAndVideoRecordings')"
      placement="top"
    >
      <TdButton icon-name="recordings" :size="size" :type="btnType" />
    </TdTooltip>
  </TdDropdownMenu>
</template>

<style scoped></style>
