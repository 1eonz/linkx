<script lang="ts" setup>
  import { computed, onBeforeUnmount, onMounted, ref, unref, watch } from 'vue';

  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { CategoryEnum } from '@/enums';
  import { useEmitter, useI18n } from '@/hooks';
  import { setVoiceHalfCallKeyUpFunction } from '@/pages/communicationCard/openResourcesCard';
  import { getAccountByEquipmentData, voiceHalfCall } from '@/pages/resource/resourceHelper';
  import { voiceFunc } from '@/plugins/mspPlayer';
  import commOpt from '@/plugins/mspPlayer/commOpt';
  import storeCidIsdnRelation from '@/plugins/mspPlayer/storeCidIsdnRelation';
  import { useCommunicationStore } from '@/store';
  import { isDef } from '@/utils/is';

  const props = defineProps<{
    btnType?: string;
    dropMenu?: boolean;
    info: any;
    size?: string;
  }>();
  defineExpose({ trigger: handleClick });
  const { t } = useI18n();
  const communicationStore = useCommunicationStore();

  const isActive = ref(false);
  const btnRef = ref();
  const mousedown = ref(false);

  const isSelf = computed(() => {
    return appConfig.userData.id === props.info.id;
  });
  const iconName = computed(() => {
    return unref(isActive) ? 'phone_hang_up' : 'phone_call';
  });
  const btnTypeName = computed(() => {
    const { btnType } = props;
    if (btnType === 'icon') {
      return btnType;
    }
    return unref(isActive) ? 'radioWarn' : btnType;
  });
  const account = computed(() => {
    return getAccountByEquipmentData(props.info);
  });

  watch(communicationStore.comm, () => {
    handlerComm();
  });

  watch(() => props.info, handlerComm, { immediate: true });

  useEmitter('closeMapPopup', () => {
    window.removeEventListener('keyup', handleKeyUp);
  });

  onMounted(() => {
    if (!props.dropMenu) {
      window.addEventListener('keyup', handleKeyUp);
      setVoiceHalfCallKeyUpFunction(handleKeyUp);
    }
  });

  onBeforeUnmount(() => {
    window.removeEventListener('keyup', handleKeyUp);
  });

  function handlerComm() {
    const comm = communicationStore.comm?.[unref(account)];

    if (!comm) {
      isActive.value = false;
      return;
    }

    const target = comm.halfdial;
    isActive.value = isDef(target);
  }

  // 拨打、挂断
  function handleClick() {
    if (isActive.value) {
      commOpt.hangUp('halfdial', appConfig.isdn, unref(account));
    } else {
      halfCall();
    }
  }

  // 放权
  function release() {
    const cid = storeCidIsdnRelation.getCidByIsdn(unref(account), 'halfdial');
    voiceFunc.voiceReleaseHalfDial(unref(account), cid);
  }

  // 发起半双工/抢权
  function halfCall() {
    if (!unref(account)) {
      Message(t('communication.communicationTips.communicationAccountNotAssociated'));
      return;
    }
    voiceHalfCall({ ...props.info, account: unref(account) });
  }

  function handleMousedown() {
    mousedown.value = true;
    halfCall();
  }

  function handleMouseup() {
    mousedown.value = false;
    release();
  }

  function handleKeyUp(event) {
    const { btnType, info } = props;
    const isNotPdt = Number(info.category) !== CategoryEnum.pdt;
    if (event.keyCode !== 32 || btnType === 'icon' || isNotPdt) {
      return;
    }
    event.preventDefault();
    handleClick();
  }
</script>

<template>
  <!-- 半双工点呼 -->
  <div class="half-call-btn">
    <TdTooltip :content="t('communication.communicationFunction.voiceHalfCall')" placement="top">
      <TdButton
        ref="btnRef"
        :active="isActive"
        :class="{
          active: isActive,
        }"
        :disable="isSelf"
        :icon-name="iconName"
        :size="size"
        :type="btnTypeName"
        @click.stop="handleClick"
      />
    </TdTooltip>

    <TdTooltip v-if="isActive" :content="t('resource.group.holdDownTheTalk')" placement="top">
      <TdButton
        :icon-name="mousedown ? 'commu_voice_speaking' : 'commu_voices'"
        icon-prefix="bigScreen"
        :size="size"
        :type="btnType"
        @mousedown="handleMousedown"
        @mouseup="handleMouseup"
      />
    </TdTooltip>
  </div>
</template>

<style lang="less" scoped></style>
