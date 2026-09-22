<script setup lang="ts">
  defineOptions({
    name: 'TdEmpty',
  });

  defineProps({
    hasBg: {
      default: false,
      type: Boolean,
    },
    iconName: {
      default: '',
      type: String,
    },
    sizeType: {
      default: 'middle',
      type: String,
    },
    title: {
      default: '',
      type: String,
    },
  });
</script>

<template>
  <div class="td-empty" :class="hasBg ? 'empty-bg' : ''">
    <img alt="" :class="`img-${sizeType}`" src="@/assets/images/resource/empty_tips.png" />
    <span :class="`size-${sizeType}`">{{ title || $t('common.noData') }}</span>
    <span>
      <slot></slot>
    </span>
  </div>
</template>

<style lang="less" scoped>
  .td-empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;

    span {
      margin-top: 10px;
      font-weight: 400;
    }

    .size-small {
      font-size: 12px;
    }

    .size-middle {
      font-size: 14px;
    }

    .size-big {
      font-size: 16px;
    }

    .img-small {
      height: 40px;
    }
  }

  .empty-bg {
    height: 100%;
    margin-top: 0;
    background: url('@/assets/images/communicate/block_back.png') no-repeat;
    background-size: 100% 100%;
    border: 1px solid rgb(50 152 226 / 100%);
  }
</style>
