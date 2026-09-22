<script lang="ts" setup>
  import { ref, watch } from 'vue';

  import { useI18n } from '@/hooks';
  import { getIp } from '@/utils';
  import { getToken } from '@/utils/auth';

  const props = defineProps<{
    activeGuide: any;
  }>();

  const emit = defineEmits(['closeDialog']);

  const { t } = useI18n();
  const reviewFile = ref<any>();
  const scrollContainer = ref();

  watch(
    () => props.activeGuide,
    () => {
      const address = props.activeGuide?.address;
      if (!address) return;
      getAddress(address);
    },
    { deep: true, immediate: true },
  );

  function closeFrame() {
    emit('closeDialog');
  }
  function getAddress(address: String) {
    if (scrollContainer.value) {
      scrollContainer.value.scrollTop = 0;
    }
    const url = `${getIp()}/linkx/desktop${address}`;
    const http = new XMLHttpRequest();
    http.open('get', url, true);
    http.setRequestHeader('Authorization', `token ${getToken()}` as string);
    http.addEventListener('readystatechange', () => {
      if (http.readyState === XMLHttpRequest.DONE) {
        reviewFile.value = http.status === 200 ? http.response : '';
      }
    });
    http.send();
  }
</script>

<template>
  <div class="guide-detail">
    <TdFrameBox
      class="frame-box"
      size="normal"
      :title="t('homePage.menus.lawEnforcementEncyclopediaDetail')"
      @close-frame-box="closeFrame"
    >
      <div ref="scrollContainer" class="detail-content" v-html="reviewFile"> </div>
    </TdFrameBox>
  </div>
</template>

<style lang="less" scoped>
  .guide-detail {
    position: fixed;
    right: 100%;
    display: flex;
    flex-direction: column;
    width: 756px;
    height: calc(100vh - 300px);
    margin-right: 100px;
    font-size: 14px;
  }

  :deep(.frame-box) {
    position: fixed;
    width: 756px !important;
    height: calc(100vh - 300px) !important;

    :deep(.frame-box-container) {
      padding: 0 !important;
    }

    .detail-content {
      box-sizing: border-box;
      width: 100%;
      height: 100%;
      padding: 20px;
      overflow-y: auto;
      backdrop-filter: blur(0.7rem);

      span,
      div,
      p {
        color: #fff !important;
        background: none !important;
      }
    }
  }
</style>
