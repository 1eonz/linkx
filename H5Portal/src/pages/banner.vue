<template>
  <view v-if="visible" class="my-swipe">
    <van-swipe indicator-color="white" :autoplay="interval" :height="swiperHeight">
      <van-swipe-item
        v-for="(item, index) in bannerList"
        :key="item.id"
        @click="handleClickSwiper(index)"
      >
        <van-image
          :src="transformImageUrl(item.pciUrl)"
          width="100%"
          height="100%"
          fit="cover"
          radius="20px"
        />
      </van-swipe-item>
    </van-swipe>
  </view>
</template>

<script setup>
  import { computed } from 'vue';

  import { useApplicationStore } from '@/stores/application.js';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';
  import { transformImageUrl } from '@/utils/imgUrlParse.js';

  const props = defineProps({
    // License 权限对象
    licensePermissions: {
      type: Object,
      default: () => ({}),
    },
  });
  // 使用设备适配
  const { adaptationSize } = useDeviceAdapter();
  const communicationStore = useCommunicationStore();
  const applicationStore = useApplicationStore();

  // Banner 图设计宽高比(宽:高)
  const BANNER_RATIO = 17 / 7;

  // 平板竖屏宽度比手机大很多,使用固定 swiperHeight 会因宽高比过扁导致 cover 裁剪
  // 平板按屏幕宽度比例计算高度;手机沿用 adaptationSize.swiperHeight
  const swiperHeight = computed(() => {
    const screenWidth = document.documentElement.clientWidth || window.screen.width;
    console.info(screenWidth, 'screenWidth');
    if (screenWidth >= 600) {
      const containerWidth = screenWidth - 32; // 减去左右 margin 16*2
      return Math.floor(containerWidth / BANNER_RATIO);
    }
    return parseInt(adaptationSize.value.swiperHeight, 10);
  });

  // 计算属性：是否显示
  const visible = computed(() => {
    return props.licensePermissions.LINKXBS === '1';
  });

  const interval = computed(() => applicationStore.time);
  const bannerList = computed(() => applicationStore.bannerList);

  // 轮播图跳转
  async function handleClickSwiper(index) {
    if (props.licensePermissions.LINKXBCF !== '1' || props.licensePermissions.LINKXBS !== '1')
      return;
    const data = bannerList.value[index];
    if (data.url) {
      await communicationStore.openUrl(data.url, data?.title, 'backStyle');
    }
  }
</script>

<style lang="scss" scoped>
  .my-swipe {
    display: block;
    box-sizing: border-box;
    margin: 8px 16px;
    border-radius: 20px;
    overflow: hidden;
    width: calc(100% - 32px);
  }
</style>
