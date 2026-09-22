<script setup lang="ts">
  import { onActivated, onDeactivated, onMounted, reactive, ref } from 'vue';

  import { queryDeviceAlarmCount } from '@/api/alarms';
  import { Dialog } from '@/components/Dialog';
  import { useI18n } from '@/hooks';

  import SingleAlarmList from './singleAlarmList.vue';

  const emit = defineEmits(['check']);
  defineExpose({ queryData });

  const { t } = useI18n();
  const form = reactive<any>({ pageSize: 10, start: 1 });
  const tableData = ref<any[]>([]);
  const total = ref(0);
  const queryParam = ref({});
  const contentRef = ref();
  const tableMaxHeight = ref(0);
  const popCid = 'SingleAlarmList';

  onMounted(() => {
    tableMaxHeight.value = (contentRef.value?.offsetHeight || 400) - 72;
  });

  onActivated(() => {
    getAlarmCount();
  });

  onDeactivated(() => {
    closePop();
  });

  function queryData(param) {
    queryParam.value = param;
    getAlarmCount();
  }

  /**
   * 获取数据
   */
  async function getAlarmCount() {
    const param = {
      ...queryParam.value,
      ...form,
    };
    delete param.date;
    const { code, data } = await queryDeviceAlarmCount(param);
    if (code === 0) {
      const { records } = data;
      tableData.value = records;
      total.value = Number(data.total);
    }
  }

  /**
   * 分页切换
   * @param val
   */
  function currentChange(val: number) {
    form.start = val;
    getAlarmCount();
  }

  function sizeChange(val: number) {
    form.start = 1;
    form.pageSize = val;
    getAlarmCount();
  }

  /**
   * 详情
   * @param row
   */
  function detailsHandle(row) {
    const { executorName, userId } = row;
    closePop();
    Dialog({
      cid: popCid,
      content: SingleAlarmList,
      data: {
        cid: popCid,
        executorName,
        userId,
      },
      offset: ['30%', '20%'],
    });
  }

  function closePop() {
    Dialog(popCid)?.close();
  }

  /**
   * 获取勾选数据
   * @param val
   */
  const handleSelectionChange = (val: []) => {
    const idsData: any[] = [];
    if (!val) return;
    val.forEach((item: any) => {
      idsData.push(item.userId);
    });
    emit('check', idsData);
  };
</script>

<template>
  <div class="table-box ground-glass">
    <TdTitle>{{ t('alarm.statisticsList') }}</TdTitle>
    <div ref="contentRef" class="content">
      <ElTable
        border
        :data="tableData"
        :max-height="tableMaxHeight"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <ElTableColumn align="center" type="selection" width="55" />
        <ElTableColumn align="center" :label="t('resource.manDetail.name')" prop="executorName">
          <template #default="scoped">
            <TdLink color="yellow" @click="detailsHandle(scoped.row)">
              {{ scoped.row.executorName }}
            </TdLink>
          </template>
        </ElTableColumn>
        <ElTableColumn
          align="center"
          :label="t('personCenter.belongOrgName')"
          prop="organizationName"
          show-overflow-tooltip
        />
        <ElTableColumn
          align="center"
          :label="t('alarm.level.outofBoundTimes')"
          prop="outofBoundTimes"
        />
        <ElTableColumn
          align="center"
          :label="t('alarm.level.roadDriftTimes')"
          prop="roadDriftTimes"
        />
        <ElTableColumn align="center" :label="t('alarm.level.batteryTimes')" prop="batteryTimes" />
        <ElTableColumn
          align="center"
          :label="t('alarm.level.remainingSizeTimes')"
          prop="remainingSizeTimes"
        />
      </ElTable>

      <ElPagination
        v-model:page-size="form.pageSize"
        background
        class="pagination"
        layout="total, sizes, prev, pager, next"
        size="small"
        :total="total"
        @current-change="currentChange"
        @size-change="sizeChange"
      />
    </div>
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/formLayout.less';

  .light {
    color: var(--text-color-light);
  }
</style>
