<script setup lang="ts">
  import { inject, onMounted, reactive, ref, unref, watch } from 'vue';

  import { queryPersonGroupListByParam } from '@/api/plan';
  import { useEmitter, useI18n } from '@/hooks';

  import PlanSafetyCard from './planSafetyCard.vue';

  const props = defineProps({
    activeTab: {
      default: 0,
      type: Number,
    },
  });
  const emit = defineEmits(['operation', 'detailClick']);

  const { t } = useI18n();
  const tableData = ref<any[]>([]);
  const form = reactive<any>({ pageSize: 8, start: 1 });
  const total = ref<number>(0);
  const queryParams = inject<any>('queryParams');

  watch(
    () => props.activeTab,
    () => {
      form.start = 1;
      queryData();
    },
  );

  onMounted(() => {
    queryData();
    useEmitter('queryPlanSafety', tabListener);
  });

  function tabListener() {
    form.start = 1;
    queryData();
  }

  /**
   * 获取数据
   */
  async function queryData() {
    const params = {
      ...unref(queryParams),
      ...form,
      state: props.activeTab,
    };
    const { code, data } = await queryPersonGroupListByParam(params);
    tableData.value = [];
    if (code === 0) {
      tableData.value = data.records;
      total.value = Number(data.total);
    }
    useEmitter().emit('queryPlanGroupDetail', { id: null });
  }

  function handleClick() {
    queryData();
  }

  function handleOperation(type, data) {
    emit('operation', type, data);
  }

  function cardClick(val) {
    tableData.value.forEach((item) => {
      item.active = item.id === val;
    });
    useEmitter().emit('queryPlanGroupDetail', { id: val });
  }

  function detailClick(data) {
    emit('detailClick', data);
  }

  function currentChange(val: number) {
    form.start = form.pageSize * (val - 1) + 1;
    queryData();
  }
</script>

<template>
  <div v-if="tableData.length > 0" class="list-box">
    <PlanSafetyCard
      v-for="data in tableData"
      :key="data.id"
      :card-data="data"
      @card-click="cardClick"
      @click="handleClick"
      @detail-click="detailClick"
      @operation="(type) => handleOperation(type, data)"
    />
  </div>
  <div v-else class="empty">
    <img alt="" class="image" src="@/assets/images/resource/empty_tips.png" />
    <span class="text">{{ t('planSafety.noData') }}</span>
  </div>
  <ElPagination
    v-if="total > form.pageSize"
    v-model:page-size="form.pageSize"
    background
    class="pagination"
    layout="prev, pager, next"
    size="small"
    :total="total"
    @current-change="currentChange"
  />
</template>

<style scoped lang="less">
  .list-box {
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
    padding: 0 24px;
  }

  .pagination {
    display: flex;
    justify-content: center;
    margin-top: 10px;
  }

  .empty {
    display: inline-grid;
    align-items: center;
    justify-content: center;
    width: 100%;
    margin-top: 20px;
    text-align: center;

    .image {
      width: 128px;
      height: 76px;
      margin-top: 70px;
    }

    .text {
      margin-top: 19px;
      color: rgb(171 216 255);
    }
  }
</style>
