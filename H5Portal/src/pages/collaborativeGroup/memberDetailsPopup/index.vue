<template>
  <!-- 个人名片详情弹窗：有数据/无权限两种状态 -->
  <van-popup
    v-model:show="visible"
    position="center"
    :close-on-click-action="true"
    :close-on-click-overlay="true"
    round
    closeable
    class="member-details-popup"
  >
    <ContentBody :user-id="userId" :visible="visible" @close-dialog="handleClose" />
  </van-popup>
</template>

<script setup>
  import { ref } from 'vue';

  import ContentBody from './ContentBody.vue';

  const props = defineProps({
    userId: {
      type: [String, Number],
      required: true,
      // 校验：禁止空值
      validator: (value) => value !== undefined && value !== null && value !== '',
    },
  });

  // 弹窗显隐
  const visible = ref(false);

  // 打开弹窗
  const open = () => {
    visible.value = true;
  };

  // 子组件关闭回调
  const handleClose = () => {
    visible.value = false;
  };

  defineExpose({ open });
</script>

<style lang="scss">
  /* 全局覆盖：定制 van-popup 的圆角变量与样式
     不用 scoped 是因为 van-popup 会 teleport 到 body，
     scoped 的 data-attribute 不会作用于 teleport 后的 DOM */
  .member-details-popup.van-popup {
    width: 300px;
    max-width: calc(100vw - 32px);
    padding: 0;
    overflow: hidden;
    /* 覆盖 vant 的 --van-popup-round-radius 变量，统一圆角为 12px */
    --van-popup-round-radius: 12px;
    border-radius: 12px !important;
  }
</style>
