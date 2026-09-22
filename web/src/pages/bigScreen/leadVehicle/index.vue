<script setup lang="ts">
  import { computed, onMounted, ref, watch } from 'vue';
  import { useRoute } from 'vue-router';

  import { queryEquipmentDetailById } from '@/api/equipment';
  import MonitorContainer from '@/pages/notification/monitorContainer.vue';
  import { useVideoPollStore } from '@/store';
  import { delay } from '@/utils';

  import CurrentTrackMap from './currentTrackMap.vue';

  const route = useRoute();
  const videoPollStore = useVideoPollStore();
  const infoData = ref(null);
  const play = ref(false);
  const full = ref(false);

  const videoPollList = computed<any>(() => {
    if (route.path !== '/leadVehicle') {
      return [];
    }

    const arr = videoPollStore.videoPollDrawerData.filter((i) => !i.ahead);
    const ret = [...arr, ...Array.from({ length: 4 - arr.length }).fill(null)];
    return ret;
  });

  watch(
    () => route.name,
    async (val) => {
      if (val === 'leadVehicle') {
        await delay(2500);
        queryLeadVehicleData();
      } else {
        play.value = false;
        infoData.value = null;
      }
    },
  );

  onMounted(() => {
    queryLeadVehicleData();
  });

  async function queryLeadVehicleData() {
    const id = route.query.id;
    const { code, data } = await queryEquipmentDetailById({ id });
    if (code === 0 && data?.serviceAccounts?.length > 0) {
      const obj = {
        ...data,
        account: data.serviceAccounts[0].account,
      };
      infoData.value = obj;
      play.value = true;
    }
  }

  function fullScreen(data) {
    full.value = data;
  }
</script>

<template>
  <!-- 头车模式 -->
  <div class="lead-vehicle" :class="{ full }">
    <div class="content">
      <CurrentTrackMap class="box" />
      <div class="box right">
        <MonitorContainer
          v-if="play"
          :info="infoData"
          :show-close="false"
          @on-full-screen="fullScreen"
        />
      </div>
    </div>
    <div class="bottom">
      <div v-for="(item, index) in videoPollList" :key="item?.id || index" class="card">
        <MonitorContainer
          v-if="item"
          :info="item"
          :show-close="false"
          @on-full-screen="fullScreen"
        />
        <TdEmpty v-else class="back" />
      </div>
    </div>
  </div>
</template>

<style scoped lang="less">
  @import url('@/styles/mixin.less');

  .lead-vehicle {
    width: 100%;
    height: 100%;
    padding: 0 15px;
    margin-top: 30px;
    margin-left: -15px;

    &.full {
      transform: none;
    }

    .content {
      display: flex;
      height: calc(100% - 400px);

      .box {
        width: 50%;
        border: 1px solid transparent;
        border-image: linear-gradient(
          180deg,
          rgba(26 255 251 / 20%) 0%,
          rgba(26 255 251 / 100%) 100%
        );
        border-image-slice: 1;
      }

      .right {
        background: url('@/assets/images/camera/no_camera.png') no-repeat center;
        border-left: none;
      }
    }

    .bottom {
      display: flex;
      justify-content: space-between;
      margin-top: 25px;

      .card {
        width: 460px;
        height: 260px;
        border: 1px solid transparent;
        border-image: linear-gradient(
          180deg,
          rgba(26 255 251 / 20%) 0%,
          rgba(26 255 251 / 100%) 100%
        );
        border-image-slice: 1;
      }

      .back {
        backdrop-filter: blur(8px);
      }
    }

    :deep(.monitor-container) {
      width: 100%;
      height: 100%;
      margin: 0;

      .monitor-content {
        width: 100%;
        height: 100%;
      }
    }
  }
</style>
