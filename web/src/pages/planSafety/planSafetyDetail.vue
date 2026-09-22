<script setup lang="ts">
  import { onMounted, ref, shallowRef } from 'vue';

  import { queryPlanGroupById } from '@/api/plan';
  import { useEmitter, useI18n } from '@/hooks';

  import MapDetail from './detail/mapDetail.vue';
  import PlanSafetyDetail from './detail/planSafetyDetail.vue';
  import PlanSafetyGroup from './detail/planSafetyGroup.vue';
  import PollingGroup from './detail/pollingGroup.vue';

  const { t } = useI18n();
  const activeTab = ref('PlanSafetyDetail');
  const currentComponent = shallowRef<any>(PlanSafetyDetail);
  const tabData = ref([
    {
      id: 'PlanSafetyDetail',
      name: t('planSafety.detailTabs.detail'),
    },
    {
      id: 'PlanSafetyGroup',
      name: t('planSafety.detailTabs.group'),
    },
    {
      id: 'MapDetail',
      name: t('planSafety.detailTabs.mapDraw'),
    },
    {
      id: 'PollingGroup',
      name: t('planSafety.detailTabs.pollGroup'),
    },
  ]);
  const tableData = ref<any>({});

  onMounted(() => {
    useEmitter('queryPlanGroupDetail', tabListener);
  });

  function tabListener(params) {
    tableData.value = {};
    if (params.id) {
      queryDetail(params);
    }
  }

  /**
   * 获取数据
   */
  async function queryDetail(params) {
    const { code, data } = await queryPlanGroupById(params);
    if (code === 0) {
      tableData.value = data;
    }
  }

  function handleTabClick({ id }) {
    const comp = {
      MapDetail,
      PlanSafetyDetail,
      PlanSafetyGroup,
      PollingGroup,
    };
    currentComponent.value = comp[id];
    activeTab.value = id;
  }
</script>

<template>
  <div class="content">
    <TdTab :data="tabData" :default-value="activeTab" @click="handleTabClick" />
    <KeepAlive>
      <component :is="currentComponent" class="comp" :detail-data="tableData" />
    </KeepAlive>
  </div>
</template>

<style scoped lang="less">
  .content {
    width: 100%;
    height: 100%;

    .comp {
      padding: 20px;
    }
  }
</style>
