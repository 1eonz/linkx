<script lang="ts" setup>
  import { computed, onMounted, ref } from 'vue';

  import { appConfig } from '@/config';
  import { useI18n } from '@/hooks';
  import { createConferenceHandler } from '@/pages/videoConference/common';

  const props = defineProps({
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
  defineExpose({ trigger: createConference });
  const { t } = useI18n();
  const disable = ref(false);
  const btnRef = ref();

  const iconName = computed(() => {
    return 'bigScreen-conference';
  });
  const btnTypeName = computed(() => {
    const { btnType } = props;
    return btnType;
  });
  const isSelf = computed(() => {
    return appConfig.userData.id === props.info.id;
  });

  onMounted(() => {});

  // 视频点呼
  function createConference() {
    createConferenceHandler([props.info], true, true, true);
  }
</script>

<template>
  <!-- 组会 -->
  <TdTooltip :content="t('resource.operateBtn.videoConf')" placement="top">
    <TdButton
      ref="btnRef"
      :disable="isSelf || disable"
      :icon-name="iconName"
      :size="size"
      :type="btnTypeName"
      @click.stop="createConference"
    />
  </TdTooltip>
</template>

<style lang="less" scoped></style>
