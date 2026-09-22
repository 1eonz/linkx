<script lang="ts" setup>
  import { computed } from 'vue';

  import { appConfig } from '@/config';

  import GroupRemove from './groupRemove.vue';
  import GroupSub from './groupSub.vue';

  const props = defineProps<{
    info: any;
  }>();

  const showRemove = computed(() => {
    const { info } = props;
    return info.executor_id === appConfig.isdn;
  });
</script>

<template>
  <!-- 操作按钮集合 -->
  <div class="group-operation">
    <!-- 删除 -->
    <GroupRemove v-if="showRemove" :info="info" />
    <!-- 订阅 -->
    <GroupSub btn-type="icon" :info="info" size="auto" />
  </div>
</template>

<style lang="less" scoped>
  .group-operation {
    display: flex;
    align-items: center;
    justify-content: space-between;

    & > div {
      min-width: 18px;
    }
  }
</style>
