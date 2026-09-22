<script lang="ts" setup>
  import { computed, ref, unref } from 'vue';

  import { useI18n, usePermissions } from '@/hooks';

  import PoliceInfo from './policeDetails/policeInfo.vue';
  import PoliceTask from './policeDetails/policeTask.vue';

  const props = defineProps({
    infoData: {
      default: () => {},
      type: Object,
    },
    resourceId: {
      default: '',
      type: String,
    },
    type: {
      default: '',
      type: String,
    },
  });

  const { t } = useI18n();
  const activeTab = ref(1);
  const taskTitle = ref('');

  const showTask = computed(() => {
    return usePermissions('MISSION') && props.type === 'person';
  });
  const tabOptions = computed(() => {
    return [
      {
        id: 1,
        name: t('resource.detailType.basics'),
      },
      {
        id: 2,
        name: t('resource.detailType.currentTask') + unref(taskTitle),
      },
    ];
  });

  function tabChange(data) {
    activeTab.value = data.id;
  }

  function handleTaskTitle(data) {
    taskTitle.value = data;
  }
</script>

<template>
  <div class="card-list">
    <TdTab v-show="showTask" :data="tabOptions" @click="tabChange" />
    <PoliceInfo v-show="activeTab === 1" :context="infoData" />
    <PoliceTask
      v-show="showTask && activeTab === 2"
      :id="infoData.id"
      :context="infoData"
      :resource-id="resourceId"
      @title="handleTaskTitle"
    />
  </div>
</template>

<style lang="less" scoped>
  .card-list {
    min-height: 140px;
  }
</style>
