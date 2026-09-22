<script lang="ts" setup>
  defineProps({
    headerObj: {
      default() {
        return {};
      },
      type: Object,
    },
    prohibit: {
      default: false,
      type: Boolean,
    },
  });
  const emit = defineEmits(['toOther']);
</script>

<template>
  <div class="module-header">
    <div class="header-title" :style="{ width: headerObj.backWidth || '28%' }">
      <span>{{ headerObj.title }}</span>
    </div>
    <div v-show="!prohibit" class="to-other" @click="emit('toOther', headerObj)">
      {{ headerObj.otherName }}
    </div>
  </div>
</template>

<style lang="less" scoped>
  .module-header {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;

    .header-title {
      display: flex;
      align-items: center;
      height: 100%;
      padding-left: 16px;
      margin-left: 8px;
      line-height: 32px;
      text-align: left;
      vertical-align: top;
      background: url('@/assets/images/popup/title_bg.png') no-repeat;
      background-size: 115px 32px;

      .screen-p-icon {
        width: 24px;
        height: 24px;
        margin-right: 3px;
      }

      span {
        font-size: 16px;
        font-weight: 400;
        color: rgb(255 255 255 / 100%);
      }
    }

    .to-other {
      position: absolute;
      right: 8px;
      margin-right: 15px;
      font-size: 12px;
      font-weight: 400;
      color: rgb(0 255 238 / 100%);
      cursor: pointer;
    }
  }
</style>
