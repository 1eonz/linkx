<script setup lang="ts">
  import { computed, nextTick, ref, unref, watch } from 'vue';
  import { useRoute } from 'vue-router';

  import VideoPollTime from '@/comm/video/components/videoPollTime.vue';
  import MessageBox from '@/components/MessageBox';
  import { useDraggable, useI18n } from '@/hooks';
  import { useVideoPollStore } from '@/store';
  import { guid } from '@/utils';

  import MonitorContainer from './monitorContainer.vue';

  const { t } = useI18n();
  const route = useRoute();
  const videoPollStore = useVideoPollStore();

  const full = ref(false);
  const draggable = ref(true);
  const uid = `dialog${guid()}`;
  const containerRef = ref();

  const videoPollList = computed<any>(() => videoPollStore.videoPollDrawerData);
  const title = computed(() => {
    const { playingPoll, videoList } = videoPollStore;
    return `${playingPoll.groupName}（${videoList?.length}）`;
  });
  const showPoll = computed(() => {
    return route.path !== '/leadVehicle' && unref(videoPollList).length > 0;
  });

  watch(
    videoPollList,
    (val) => {
      nextTick(() => {
        if (val.length === 0) {
          const el = document.getElementById(uid);
          if (el) {
            el.style.transform = '';
          }
        }
      });
    },
    { deep: true },
  );
  watch(showPoll, (val) => {
    if (val) {
      nextTick(() => {
        useDraggable({ draggable, uid });
      });
    }
  });

  function onFullScreen(data) {
    full.value = data;
  }

  async function closeWindow() {
    const res = await MessageBox({ text: t('desktop.other.sureClose') });
    if (res) {
      videoPollStore.clearVideoPollTimer();
    }
  }
</script>

<template>
  <div
    v-if="showPoll"
    :id="uid"
    class="video-poll-drawer"
    :class="{
      'video-poll-drawer-full': full,
    }"
  >
    <TdFrameBox :dragger="true" :tip-disabled="false" :title="title" @close-frame-box="closeWindow">
      <VideoPollTime class="time-control" />
      <MonitorContainer
        v-for="item in videoPollList"
        :key="item.id"
        ref="containerRef"
        class="video-poll"
        :class="{
          hide: item.ahead,
        }"
        :info="item"
        :show-close="false"
        @on-full-screen="onFullScreen"
      />
    </TdFrameBox>
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .video-poll-drawer {
    position: absolute;
    top: 140px;
    right: 76px;
    z-index: 100;
    display: flex;
    width: 360px;
    height: calc(100vh - 140px - 26px);
    overflow: hidden auto;

    :deep(.frame-box) {
      width: 100%;
      height: 100%;

      .header-name {
        display: inline-block;
        width: 180px;
        height: 18px;
        font-size: 16px;
        font-weight: 400;
        line-height: 18px;
        .ellipsis1();
      }

      .frame-box-container {
        padding: 0;
      }
    }

    .time-control {
      position: absolute;
      top: 0;
      right: 58px;
    }

    .video-poll {
      height: 24%;
      margin: 0 0 9px;

      &.hide {
        position: absolute;
        z-index: -1;
        opacity: 0;
      }

      &:nth-child(5) {
        margin-bottom: 0;
      }

      :deep(.monitor-content) {
        height: 100%;
      }
    }
  }

  .video-poll-drawer-full {
    z-index: 2000;
    transform: none !important;

    .ground-glass {
      backdrop-filter: none !important;
    }
  }
</style>
