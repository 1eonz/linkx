<template>
  <div class="divider-bar" v-if="isVisible">
    <!-- 判断是否为图片 -->
    <img v-if="isImageUrl" class="divider-image" :src="imageUrl" />
    <span v-else class="divider-text">{{ displayContent }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { transformImageUrl } from '@/utils/imgUrlParse.js';

const props = defineProps({
  // 自定义配置对象（已解析的 JSON）
  custom: {
    type: Object,
    default: () => ({}),
  },
  // 配置对象
  section: {
    type: Object,
    default: () => ({}),
  },
  // License 权限对象
  licensePermissions: {
    type: Object,
    default: () => ({}),
  },
});

// 计算属性：是否显示整个组件
const isVisible = computed(() => {
  return props.licensePermissions.LINKXBS === '1';
});

// 获取显示内容
const displayContent = computed(() => {
  const custom = props.custom || {};
  
  // 兼容旧数据：无 contentType 时使用 lineContent
  if (!custom.contentType) {
    return custom.lineContent || '全局支撑一警，一警调动全局';
  }
  
  // 上传图片模式
  if (custom.contentType === 'upload') {
    return custom.uploadedImageUrl || '';
  }
  
  // 文字/图片URL 模式
  return custom.lineContent || '全局支撑一警，一警调动全局';
});

// 判断是否为图片
const isImageUrl = computed(() => {
  const custom = props.custom || {};
  
  // 上传图片模式：有 uploadedImageUrl 就是图片
  if (custom.contentType === 'upload') {
    return !!custom.uploadedImageUrl;
  }
  
  // 文字/图片URL 模式或旧数据：通过扩展名判断
  if (!displayContent.value) return false;
  const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.svg', '.bmp'];
  const lowerText = displayContent.value.toLowerCase();
  return imageExtensions.some((ext) => lowerText.includes(ext));
});

// 图片URL（经过 transformImageUrl 处理）
const imageUrl = computed(() => {
  if (!isImageUrl.value || !displayContent.value) return '';
  return transformImageUrl(displayContent.value);
});
</script>

<style lang="scss" scoped>
.divider-bar {
  text-align: center;
  padding: 12px 16px;
  margin-top: 32px;

  .divider-text {
    font-size: 12px;
    line-height: 18px;
    font-weight: 400;
    color: #5a6383;
    word-wrap: break-word;
  }

  .divider-image {
    max-width: 100%;
    height: auto;
  }

  /* 平板适配 */
  @media screen and (min-height: 1200px) {
    .divider-text {
      font-size: 0.6rem;
      margin-top: 40px;
    }
  }

  @media screen and (min-height: 2001px) {
    .divider-text {
      font-size: 0.4rem;
    }
  }
}
</style>