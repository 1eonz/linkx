<script setup lang="ts">
  import { ref } from 'vue';

  import { useI18n } from '@/hooks';
  import { getResourceType, singleClick } from '@/pages/resource/resourceHelper';
  import SupportTree from '@/pages/tree/supportTree/index.vue';

  defineProps<{
    autoUpdate?: boolean;
    canDrag?: boolean;
    operation?: boolean;
  }>();

  const emit = defineEmits(['checkChange']);

  const { t } = useI18n();

  const keywords = ref('');

  function checkChange(checkData) {
    emit('checkChange', checkData);
  }

  function handleNodeClick(data) {
    const type = getResourceType(data);
    singleClick({ data, type }, true);
  }
</script>

<template>
  <div class="support-group-list">
    <div class="title">
      <TdInput
        v-model="keywords"
        :placeholder="t('videoControl.createEvent.enterName')"
        type="searchInput"
      />
    </div>
    <SupportTree
      :keywords="keywords"
      :operation="operation"
      @check-change="checkChange"
      @click="handleNodeClick"
    />
  </div>
</template>

<style scoped lang="less">
  .support-group-list {
    display: flex;
    flex-direction: column;
    height: calc(100% - 96px);

    .title {
      padding: 10px;
    }
  }
</style>
