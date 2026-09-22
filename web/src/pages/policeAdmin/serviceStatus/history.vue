<script setup lang="ts">
  import { onMounted, reactive, ref, unref } from 'vue';

  import { queryAttendanceStatusHistoryList } from '@/api/serviceStatus';
  import { Dialog } from '@/components/Dialog';
  import { useI18n } from '@/hooks';

  import { exportHandle, historyStatusOptions, lookDetails } from './common';

  const props = defineProps<{
    cid: string;
    info: any;
  }>();

  const { t } = useI18n();
  const tableData = ref([]);
  const checkeds = ref<any>([]);
  const form = reactive({
    pageSize: 10,
    start: 1,
  });
  const total = ref(0);

  onMounted(() => {
    queryList();
  });

  async function queryList() {
    const param = {
      ...form,
      executorCode: props.info.executorCode,
      start: form.pageSize * (form.start - 1) + 1,
    };
    const { code, data } = await queryAttendanceStatusHistoryList(param);

    if (code === 0) {
      const { records } = data;
      records.forEach((item) => {
        const { status } = item;
        item.statusName = historyStatusOptions[status - 1]?.label;
      });
      tableData.value = records;
      total.value = Number(data.total);
    }
  }

  function exportFn() {
    const ids = unref(checkeds).map((i) => i.id);
    const params: any = { type: '1' };

    if (ids.length === 0) {
      params.executorCode = props.info.executorCode;
    } else {
      params.ids = ids;
    }

    exportHandle(params);
  }

  function closeCard() {
    Dialog(props.cid as string)?.close();
  }

  function handleSelectionChange(data) {
    checkeds.value = data;
  }

  function pageNumChange(page) {
    form.start = page;
    queryList();
  }
</script>

<template>
  <div class="history-content ground-glass">
    <div class="header">
      <TdTitle show-close @close="closeCard">{{ t('policeAdmin.service.history') }}</TdTitle>
    </div>

    <div class="main">
      <div class="handle">
        <TdButton class="export" :text="t('alarm.btn.export')" @click="exportFn" />
      </div>

      <ElTable
        border
        class="table-content"
        :data="tableData"
        max-height="450px"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <ElTableColumn align="center" type="selection" width="55" />
        <ElTableColumn
          align="center"
          :label="t('policeAdmin.service.executorName')"
          prop="date"
          width="180"
        >
          <template #default="{ row }">
            {{ row.executorName }} （{{ row.executorCode }}）
          </template>
        </ElTableColumn>
        <ElTableColumn align="center" :label="t('resource.fillColor.duty')">
          <template #default="{ row }">
            <TdLink @click="lookDetails(row.onlineId, row)">
              {{ (row.onlineTime || '') + (row.onlineAddress || '') }}
            </TdLink>
          </template>
        </ElTableColumn>
        <ElTableColumn align="center" :label="t('resource.fillColor.leave')">
          <template #default="{ row }">
            <TdLink @click="lookDetails(row.offlineId, row)">
              {{ (row.offlineTime || '') + (row.offlineAddress || '') }}
            </TdLink>
          </template>
        </ElTableColumn>
      </ElTable>

      <ElPagination
        background
        class="pagination"
        layout="total, prev, pager, next"
        :page-size="form.pageSize"
        size="small"
        :total="total"
        @current-change="pageNumChange"
      />
    </div>
  </div>
</template>

<style scoped lang="less">
  .history-content {
    width: 928px;
    height: 586px;

    .header {
      position: relative;
      display: flex;
      align-items: center;
    }

    .main {
      width: 100%;
      height: 552px;
      padding: 10px;

      .handle {
        display: flex;
        justify-content: flex-end;
        width: 100%;
        height: 36px;
        margin-bottom: 10px;

        .export {
          width: 151px;
        }
      }
    }
  }

  .status {
    .circle {
      display: inline-block;
      width: 8px;
      height: 8px;
      margin-right: 5px;
      border-radius: 50%;

      &_1 {
        background-color: #7ec8ff;
      }

      &_2 {
        background-color: #ff6060;
      }

      &_3 {
        background-color: #fca701;
      }
    }
  }

  .pagination {
    display: flex;
    justify-content: center;
    margin-top: 10px;
  }
</style>
