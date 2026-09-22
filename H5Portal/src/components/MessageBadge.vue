<script setup lang="ts">
import { computed } from 'vue';
const props = defineProps({
  count: {
    type: Number,
    default: 0,
  },
});

// 计算显示的文本
const displayText = computed(() => {
  if (props.count <= 0) return '';
  if (props.count > 99) return '99+';
  return String(props.count);
});

// 是否显示
const show = computed(() => props.count > 0);

</script>

<template>
  <view v-if="show" class="message-badge">
    {{ displayText }}
  </view>
</template>

<style lang="scss" scoped>
.message-badge {
  width: 17px;
  height: 17px;
  padding: 0 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 500;
  color: #ffffff;
  background-color: rgba(245, 63, 62, 1);
  border-radius: 50%;
  box-sizing: border-box;
}
</style>