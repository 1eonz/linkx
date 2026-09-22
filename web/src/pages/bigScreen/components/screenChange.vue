<script lang="ts" setup>
  import { onMounted, ref } from 'vue';
  import { useRouter } from 'vue-router';

  import { multiScreenJump } from '@/pages/resource/resourceHelper';
  import { useCommunicateDispatchStore } from '@/store';

  const communicateDispatchStore = useCommunicateDispatchStore();
  const screenArr = ref<any>([]);
  const activeId = ref('communicateCenter');
  const router = useRouter();
  const curRouterName = ref<any>('');

  onMounted(() => {
    init();
  });

  function init() {
    curRouterName.value = router.currentRoute.value.name || activeId.value;
    // screenArr.value = [
    //   {
    //     title: '资源调度中心',
    //     pathName: 'communicateCenter',
    //     iconName: 'bigscreen_resource',
    //     openScreen: 1,
    //   },
    //   {
    //     title: '通信保障一张图',
    //     pathName: 'mapCenter',
    //     iconName: 'bigscreen_map',
    //     openScreen: 2,
    //   },
    //   {
    //     title: '视频监控中心',
    //     pathName: 'monitorCenter',
    //     iconName: 'bigscreen_monitor',
    //     openScreen: 3,
    //   },
    // ];
  }

  async function clickFunc(item) {
    activeId.value = item.pathName;
    const res = await multiScreenJump(item, router);
    communicateDispatchStore.setWindowOpen(res);
  }
</script>

<template>
  <div class="screen-change">
    <div
      v-for="(item, index) in screenArr"
      :key="index"
      class="screen-change-item"
      :class="{ 'screen-change-item-active': curRouterName === item.pathName }"
      @click="clickFunc(item)"
    >
      <Icon
        class="screen-p-icon"
        :color="curRouterName === item.pathName ? '#1AFFFB' : '#fff'"
        :name="item.iconName"
        prefix="bigScreen"
      />
      <span>{{ item.title }}</span>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .screen-change {
    position: absolute;
    bottom: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 22px;
    overflow: hidden;
    line-height: 24px;
    pointer-events: none;
    background: url('@/assets/images/communicate/footer_bg.png') no-repeat;
    background-size: 100% 100%;

    // &::after {
    //   content: '';
    //   width: 100%;
    //   height: 22px;
    //   background: url('@/assets/images/communicate/footer_bg.gif') no-repeat;
    //   background-size: 100% 100%;
    //   position: absolute;
    //   top: 0;
    //   left: 0;
    //   z-index: 100;
    // }

    .screen-change-item {
      display: flex;
      align-items: center;
      margin-top: 10px;
      font-size: 16px;
      font-weight: 500;
      color: rgb(255 255 255 / 100%);
      cursor: pointer;
      border-bottom: 4px solid rgb(255 255 255 / 0%);

      .screen-p-icon {
        width: 12px;
        height: 12px;
        padding: 0 !important;
        margin-right: 6px;
        fill: #fff !important;
      }

      &:not(:last-child) {
        margin-right: 14px;
      }
    }

    .screen-change-item-active {
      border-bottom: 4px solid rgb(26 255 251 / 100%);

      span {
        color: rgb(26 255 251 / 100%);
      }

      .screen-p-icon {
        fill: rgb(26 255 251 / 100%);
      }
    }
  }
</style>
