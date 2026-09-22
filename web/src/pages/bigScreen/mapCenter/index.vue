<script lang="ts" setup>
  import { onBeforeUnmount, onMounted } from 'vue';

  import commOpt from '@/plugins/mspPlayer/commOpt';

  import FeatureBox from '../components/featureBox.vue';
  import ScreenChange from '../components/screenChange.vue';
  import AllResourceList from './allResourceList.vue';
  import MainMapBig from './mapBIg/mainMapBig.vue';

  onMounted(() => {
    window.addEventListener('beforeunload', () => {
      commOpt.hangUpAll();
    });
  });

  onBeforeUnmount(() => {
    window.removeEventListener('beforeunload', () => {
      commOpt.hangUpAll();
    });
  });
</script>

<template>
  <!-- 指挥调度中心 -->
  <div class="command-center">
    <MainMapBig class="main-map" />
    <AllResourceList class="resources" />
    <FeatureBox class="feature" />
    <ScreenChange class="screen-change" />
  </div>
</template>

<style lang="less" scoped>
  .command-center {
    position: relative;
    width: 100vw;
    height: 100vh;

    .main-map {
      width: 100vw;
      height: 100vh;
    }

    .resources {
      position: absolute;
      top: 0;
      left: 0;
    }

    .feature {
      position: absolute;
      top: 94px;
      right: 16px;
    }

    .screen-change {
      z-index: 99;
    }
  }
</style>
