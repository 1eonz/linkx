<template>
  <div class="reply-content markdown-body-render">
    <div v-html="content"></div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps({
  model: {
    type: String,
    default: '',
  },
})
// 将末尾的[end]替换为空字符串
const content = computed(() => props.model.replace('[end]', ''))   
</script>

<style scoped lang="less">
@import '@/styles/markdown.less';

.reply-content {
  font-size: 14px;
  line-height: 1.5;
}
</style>
