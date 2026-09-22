<script setup lang="ts">
  import { useI18n } from '@/hooks';

  defineOptions({
    name: 'TdLoading',
  });

  defineProps<{
    cancel?: boolean; // 取消加载
  }>();

  const emit = defineEmits(['onCancel']);

  const { t } = useI18n();
</script>

<template>
  <div class="td-loading">
    <div class="loading-img"></div>
    <div class="loading-text">{{ `${t('common.loading')}...` }}</div>
    <div v-if="cancel" class="loading-btn">
      <TdButton @click="emit('onCancel')">{{ t('common.cancel') }}</TdButton>
    </div>
  </div>
</template>

<style scoped lang="less">
  .td-loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;

    .loading-img {
      width: 100px;
      height: 100px;
      background: url('@/assets/images/common/reactangle.gif') no-repeat;
      background-size: contain;
    }

    .loading-text {
      margin: 10px 0;
    }
  }
</style>
