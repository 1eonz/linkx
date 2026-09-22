<script lang="ts" setup>
  import { computed, ref, unref, watchEffect } from 'vue';

  import { useI18n } from '@/hooks';
  import { mediaFunc } from '@/plugins/mspPlayer';
  import { useCommunicationStore } from '@/store';

  defineOptions({
    name: 'TdVolumeRange',
  });

  const props = withDefaults(
    defineProps<{
      cid?: string;
      color?: string;
      disable?: boolean; // 禁用（不可点击）
      disableTips?: boolean; // 禁用tips
      groupId?: string;
      showLabel?: boolean;
      visible?: boolean;
    }>(),
    {
      color: '#fff',
    },
  );

  defineExpose({
    silenceClick,
    voiceHoverChange,
  });

  const { t } = useI18n();
  const communicationStore = useCommunicationStore();

  const voiceHover = ref(false);
  const sliderVal = ref(50);

  const iconColor = computed(() => {
    return props.disable ? '#a6a9ab' : props.color;
  });
  const volume = computed<number>(() => {
    const { cid, groupId } = props;
    const media = communicationStore.mediaData[groupId || cid || ''];
    if (!media) {
      return unref(sliderVal);
    }
    const { muteSpeaker, volume } = media;
    return muteSpeaker ? 0 : Number(volume);
  });
  const iconName = computed(() => {
    let ret = '';
    if (unref(volume) <= 1) {
      ret = 'voice_quiet';
    } else if (unref(volume) > 1 && unref(volume) <= 100) {
      ret = 'voice_on';
    }
    return ret;
  });

  watchEffect(() => {
    sliderVal.value = unref(volume);
  });

  function voiceHoverHandle(data) {
    if (props.disable) {
      return;
    }
    voiceHover.value = data;
  }

  // 音量调节
  function voiceChange() {
    if (props.disable) {
      return;
    }

    const { cid, groupId } = props;
    const v = unref(sliderVal) || 1;
    if (groupId) {
      setGroupVolume(v);
      return;
    }

    if (cid) {
      mediaFunc.setVolume(cid, v); // 最小为1
    }
  }

  // 静音/取消静音
  function silenceClick() {
    if (props.disable) {
      return;
    }

    const { cid, groupId } = props;
    if (unref(volume) <= 1) {
      sliderVal.value = 50;
      if (groupId) {
        setGroupVolume(50);
        return;
      }
      if (cid) {
        mediaFunc.setVolume(cid, 50);
      }
    } else {
      sliderVal.value = 0;
      if (groupId) {
        setGroupVolume(1);
        return;
      }
      if (cid) {
        mediaFunc.setVolume(cid, 1);
      }
    }
  }

  // 群组音量
  function setGroupVolume(volume) {
    const { groupId } = props;
    if (groupId) {
      communicationStore.addCid2media({
        id: groupId,
        media: { volume },
      });

      mediaFunc.setGroupVolume(groupId, volume);
    }
  }

  function voiceHoverChange(data) {
    voiceHover.value = data;
  }
</script>

<template>
  <div
    class="volume-control"
    :class="{
      'volume-disabled': disable,
    }"
    @click.stop
    @mouseenter="voiceHoverHandle(true)"
    @mouseleave="voiceHoverHandle(false)"
  >
    <div v-show="voiceHover && !disable" class="slider-box">
      <ElSlider
        v-model="sliderVal"
        class="el-slider-class"
        tooltip-class="tooltip-class"
        vertical
        @change="voiceChange"
      />
    </div>

    <TdTooltip
      :content="t('communication.communicationVoice.volumeControl')"
      :disabled="disableTips"
      :visible="visible"
    >
      <div class="icon-box">
        <Icon class="icon" :color="iconColor" :name="iconName" @click.stop="silenceClick" />
        <span v-if="showLabel">{{ t('mrs.operation.volume') }}</span>
      </div>
    </TdTooltip>
  </div>
</template>

<style lang="less" scoped>
  .volume-control {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;

    .slider-box {
      position: absolute;
      bottom: 16px;
      z-index: 3;
      display: flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 136px;
      background-color: var(--background-frame);
      border-top-left-radius: 20px;
      border-top-right-radius: 20px;

      .el-slider-class {
        height: 100px;

        :deep(.el-slider__bar) {
          width: 4px;
          background: #00c2ff;
        }

        :deep(.el-slider__runway) {
          width: 4px;
          background: #daecf9;
        }

        :deep(.el-slider__button-wrapper) {
          left: -16px !important;
        }
      }
    }

    .icon-box {
      display: inline-flex;
      flex-direction: column;
      align-items: center;
      cursor: pointer;

      .icon {
        width: 20px;
        height: 20px;
        margin: auto;
      }

      span {
        font-size: 12px;
      }
    }
  }

  .volume-disabled {
    .p-icon {
      cursor: not-allowed;
    }

    span {
      color: #a6a9ab;
    }
  }
</style>
