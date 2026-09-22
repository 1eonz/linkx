<script setup>
  defineProps({
    text: {
      type: String,
      default: '协同',
    },
  });
</script>

<template>
  <div class="cooperation-tag">
    <slot>{{ text }}</slot>
  </div>
</template>

<style lang="less" scoped>
  .cooperation-tag {
    box-sizing: border-box;
    padding: 2px 6px;
    margin-right: 4px;
    font-size: 10px;
    font-weight: 500;
    line-height: 14px;
    color: rgba(250, 120, 27, 1);
    background: rgba(255, 205, 169, 0.2);
    border: 1px solid rgba(250, 120, 27, 1);
    border-radius: 3px;
    flex: none;
  }
</style>


