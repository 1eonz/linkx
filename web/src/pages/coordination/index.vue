<script setup lang="ts">
  import { computed, ref } from 'vue';

  import { usePIMStore } from '@/store';

  import ChatList from './chatList/index.vue';
  import ChatPanel from './chatPanel/index.vue';
  import Collaboration from './collaboration/index.vue';
  // import ProblemSolve from './collaboration/problemSolve.vue';
  import { useEmitter } from '@/hooks';

  const PIMStore = usePIMStore();
  const showCount = computed(() => PIMStore.collaboration);
  const tabId = ref<number>(0);

  function handleTabClick(id) {
    tabId.value = 0;
    const params = { bigActiveName: 'third', tabId: id };
    useEmitter().emit('toTaskList', params);
    handleCloseList();
  }

  function handleCloseList() {
    tabId.value = 0;
  }
</script>

<template>
  <div v-show="tabId === 0" class="coordination-layout">
    <Collaboration v-show="showCount" @tab-click="handleTabClick" />
    <div
      class="layout-chat"
      :class="{
        'layout-short': showCount,
      }"
    >
      <ChatList :hidden-header="true" />
      <ChatPanel v-if="tabId === 0" title-back-type="dark-back" />
    </div>
  </div>
  <!-- <div v-if="tabId !== 0" class="coordination-list-layout">
    <ProblemSolve :target="tabId" @close-list="handleCloseList" />
  </div> -->
</template>

<style scoped lang="less">
  .coordination-layout {
    height: 100%;
    //padding: 10px 0;

    .layout-chat {
      display: flex;
      width: 100%;
      height: 100%;
    }

    .layout-short {
      height: calc(100% - 130px);
    }
  }

  .coordination-list-layout {
    height: 100%;
  }
</style>
