<script lang="ts" setup>
  import { computed, ref } from 'vue';

  import MessageDrawer from '@/pages/notification/messageDrawer.vue';
  import { useMainStore } from '@/store';

  import CommunicateBox from './communicateBox.vue';

  defineProps<{
    synthesizeFlag?: boolean; // 是否为综合屏
  }>();

  const mainStore = useMainStore();
  const emptyData = ref(false);

  const showMsg = computed(() => mainStore.showMessageDrawer);

  function closeMessage() {
    mainStore.setShowMessageDrawer(false);
  }
</script>

<template>
  <!-- 通讯桌面 -->
  <div
    class="communicate-desktop"
    :class="{
      'synthesize-comm-desk': synthesizeFlag,
      'empty-comm': emptyData,
    }"
  >
    <CommunicateBox :synthesize-flag="synthesizeFlag" />
    <MessageDrawer v-show="!synthesizeFlag && showMsg" @close="closeMessage" />
  </div>
</template>

<style lang="less" scoped>
  .communicate-desktop {
    display: flex;
    width: 100%;
    height: 100%;
  }

  .empty-comm {
    background: rgb(14 111 179 / 40%);
  }

  .synthesize-comm-desk {
    height: auto;
    padding-top: 0;
    margin-top: 0;
    background-color: unset;
    border: none;

    .resources-footer {
      position: relative;
      margin-top: 10px;
    }

    :deep(.el-col) {
      height: calc(50% - 5px) !important;
    }
  }

  .message-drawer {
    position: relative;
    top: unset;
    right: unset;
    margin-left: 8px;
  }
</style>
