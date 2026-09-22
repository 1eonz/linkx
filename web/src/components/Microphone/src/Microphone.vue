<script lang="ts" setup>
  import { computed, unref } from 'vue';

  import { useI18n } from '@/hooks';
  import { mediaFunc } from '@/plugins/mspPlayer';
  import { useCommunicationStore } from '@/store';

  defineOptions({
    name: 'TdMicrophone',
  });

  const props = defineProps<{
    cid: string;
    color?: string;
    disable?: boolean; // 禁用（不可点击）
    disableTips?: boolean; // 禁用tips
    inFlag?: boolean;
    mute?: boolean; // 传入的控制
    showLabel?: boolean;
  }>();

  const emit = defineEmits(['handlerMicSwitch']);

  defineExpose({ micClick });

  const { t } = useI18n();
  const communicationStore = useCommunicationStore();

  const muteMic = computed(() => {
    const { cid, inFlag, mute } = props;
    const { muteMic } = communicationStore.mediaData[cid] || {};
    if (!inFlag) {
      return Boolean(mute);
    }
    return Boolean(muteMic);
  });
  const iconColor = computed(() => {
    return props.disable ? '#a6a9ab' : props.color || '#fff';
  });

  function micClick() {
    const { cid, inFlag } = props;
    if (inFlag) {
      if (unref(muteMic)) {
        mediaFunc.unmuteMic(cid || -1);
      } else {
        mediaFunc.muteMic(cid || -1);
      }
    } else {
      emit('handlerMicSwitch', !unref(muteMic));
    }
  }
</script>

<template>
  <div class="mic-control" :class="{ 'mic-control-disable': disable }">
    <TdTooltip :content="t('videoConference.conferenceButton.micBtn')" :disabled="disableTips">
      <div class="icon-box">
        <Icon
          class="icon"
          :color="iconColor"
          :name="muteMic ? 'microphone_mute' : 'microphone'"
          @click.stop="micClick"
        />
        <span v-if="showLabel">{{ t('videoConference.conferenceButton.micBtn') }}</span>
      </div>
    </TdTooltip>
  </div>
</template>

<style lang="less" scoped>
  .mic-control {
    display: inline-flex;
    cursor: pointer;

    .icon-box {
      display: flex;
      flex-direction: column;
      align-items: center;
    }

    .icon {
      width: 20px;
      height: 20px;
      fill: var(--icon-color-normal);
    }

    span {
      font-size: 12px;
    }
  }

  .mic-control-disable {
    .icon {
      cursor: not-allowed;
    }

    span {
      color: #a6a9ab;
    }
  }
</style>
