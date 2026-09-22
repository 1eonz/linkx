<script setup lang="ts">
  import { computed } from 'vue';

  const props = defineProps<{
    color?: string; // 颜色值
    icon?: null | string | undefined;
    specialStyle: boolean; // 特殊样式
    title: string; // 标题文本
    type: string;
    url?: string;
    value: number | string; // 值文本
  }>();
  const backgroundColor = computed(() => {
    if (props.type === 'StatCards') return '';
    if (!props.color) return '';

    // 如果是rgba格式，修改透明度
    if (props.color.startsWith('rgba')) {
      return props.color.replace(/rgba\((\d+,\s*\d+,\s*\d+),\s*[\d.]+\)/, 'rgba($1, 0.1)');
    }

    // 如果是rgb格式，转换为rgba并设置透明度为0.3
    if (props.color.startsWith('rgb')) {
      return props.color.replace(/rgb\((\d+,\s*\d+,\s*\d+)\)/, 'rgba($1, 0.1)');
    }

    // 其他情况直接返回原颜色值
    return props.color;
  });
</script>

<template>
  <div class="card-content">
    <div
      class="card-icon"
      :class="{ 'stat-icon': type === 'StatCards' }"
      :style="{ backgroundColor }"
    >
      <img v-if="type === 'StatCards'" :src="url" />
      <div v-else>
        <i v-if="icon" :class="icon" :style="`color:${color}`"></i>
        <i v-else class="fas fa-user" :style="`color:${color}`"></i>
      </div>
    </div>
    <div class="card-info">
      <div :class="specialStyle ? 'card-value-special' : 'card-value'">{{ value }}</div>
      <div :class="specialStyle ? 'card-info-special' : 'card-title'">{{ title }}</div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .card-content {
    display: flex;
    align-items: center;
    width: 100%;
    height: 100%;
  }

  .card-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 50px;
    height: 50px;
    margin-right: 15px;
    font-size: 24px;
    color: white;
    // background-color:var(--card-icon-bg) !important;
    border: 1px solid var(--card-icon-border);
    border-radius: 50%;

    img {
      // filter: var(--svg-filter);
    }

    .el-icon {
      font-size: 24px;
    }
  }

  .stat-icon {
    background-color: var(--card-icon-bg) !important;
    border-radius: 8px;
  }

  .card-info {
    flex: 1;
  }

  .card-info-special {
    font-size: 14px;
    color: var(--card-value-special);
  }

  .card-value {
    font-size: 22px;
    font-weight: bold;
    color: var(--card-value-color);
  }

  .card-value-special {
    font-size: 22px;
    font-weight: bold;
    color: var(--card-value-special);
  }

  .card-title {
    font-size: 14px;
    color: var(--card-value-color);
  }

  @media (max-width: 768px) {
    .card-icon {
      width: 40px;
      height: 40px;

      .el-icon {
        font-size: 20px;
      }
    }

    .card-value {
      font-size: 18px;
    }
  }
</style>
