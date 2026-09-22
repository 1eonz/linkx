<template>
  <view class="nav-bar-container">
    <view :style="{ width: '100%', height: paddingTop + 'px' }"></view>
    <view class="top-nav-bar">
      <view class="nav-left">
        <van-icon name="arrow-left" :size="adaptationSize.iconSize" color="#333" @click="emit('back')" />
      </view>
      <text class="title">{{ title }}</text>
      <view class="nav-right">
        <slot name="right"></slot>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useDeviceAdapter } from "@/stores/useDeviceAdapter.js";
import { useCommunicationStore } from "@/stores/communication.js";

const emit = defineEmits(["back"]);
const props = defineProps({ title: "公安AI助手" });
// 获取设备尺寸信息
const { adaptationSize } = useDeviceAdapter();
const communicationStore = useCommunicationStore();
const paddingTop = ref(0);

onMounted(async () => {
  paddingTop.value = await communicationStore.fetchStatusBarHeight();
});
</script>

<style scoped lang="scss">
.nav-bar-container {
  width: 100%;
}
.top-nav-bar {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 2vw 2.5vh 1.5vw;

  .nav-left {
    width: 44px;
    text-align: center;
    display: flex;
    align-items: center;
    justify-content: flex-start;
    flex-shrink: 0;
  }

  .nav-right {
    min-width: 44px;
    display: flex;
    align-items: center;
    justify-content: flex-end;
    flex-shrink: 0;
  }

  .title {
    color: rgba(3, 8, 26, 1);
    font-size: 18px;
    height: 44px;
    line-height: 44px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    text-align: center;
    flex: 1;
    min-width: 0;
  }

  /* 平板适配 */
  @media screen and (min-height: 1200px) {
    .title {
      height: 88px;
      line-height: 88px;
      font-size: 0.8rem;
    }
  }

  @media screen and (min-height: 2001px) {
    .title {
      font-size: 0.6rem;
    }
  }
}
</style>
