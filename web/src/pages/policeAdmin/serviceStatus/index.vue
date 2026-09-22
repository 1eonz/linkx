<script setup lang="ts">
  import { nextTick, onActivated, onMounted, reactive, ref, unref } from 'vue';

  import { queryAttendanceStatusList } from '@/api/serviceStatus';
  import { Dialog } from '@/components/Dialog';
  import { useI18n } from '@/hooks';
  import OrganizationSelect from '@/pages/tree/organizationSelect.vue';

  import { debounce } from 'lodash-es';

  import { exportHandle, lookDetails } from './common';
  import History from './history.vue';

  const { t } = useI18n();
  const form = reactive({
    executorName: '',
    organizationId: '',
    pageSize: 10,
    start: 1,
  });
  const tableData = ref<any[]>([]);
  const total = ref(0);
  const checkedList = ref<any>([]);
  const contentRef = ref();
  const tableMaxHeight = ref(0);
  const tableRef = ref();

  const queryList = debounce(async () => {
    const param = {
      ...form,
      start: form.pageSize * (form.start - 1) + 1,
    };
    const { code, data } = await queryAttendanceStatusList(param);
    tableData.value = [];
    if (code === 0) {
      tableData.value = data.records;
      total.value = Number(data.total);

      nextTick(() => {
        const keys = unref(checkedList).map((i) => i.id);
        tableData.value.forEach((row) => {
          if (keys.includes(row.id)) {
            tableRef.value.toggleRowSelection(row, true);
          }
        });
      });
    }
  }, 500);

  onMounted(() => {
    tableMaxHeight.value = (contentRef.value?.offsetHeight || 400) - 72;
  });

  onActivated(() => {
    queryList();
  });

  function handleSelectionChange(data) {
    checkedList.value = data;
  }

  function history(data) {
    const cid = 'serviceStatusHistory';
    Dialog({
      cid,
      content: History,
      data: { cid, info: data },
      shade: true,
    });
  }

  function exportFn() {
    exportHandle({ ids: unref(checkedList).map((i) => i.id), ...form });
  }

  function handleSearch() {
    checkedList.value = [];
    queryList();
  }

  /**
   * 当前页改变
   */
  function pageNumChange(num) {
    form.start = num;
    // 每次翻页后将滚动条设置到顶部
    if (tableRef.value) {
      tableRef.value.$refs.scrollBarRef.setScrollTop(0);
    }
    queryList();
  }

  function sizeChange(size) {
    form.start = 1;
    form.pageSize = size;
    queryList();
  }
</script>

<template>
  <div class="table-layout service-status">
    <div class="filter ground-glass">
      <TdTitle>{{ t('policeAdmin.from.filter') }}</TdTitle>
      <ElForm class="filter-form" inline label-width="100px" :model="form" @submit.prevent>
        <ElFormItem :label="t('personCenter.belongOrgName')">
          <OrganizationSelect v-model="form.organizationId" style="width: 200px" />
        </ElFormItem>
        <ElFormItem :label="t('videoControl.warningInformation.policeOfficerName')">
          <TdInput
            v-model="form.executorName"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </ElFormItem>

        <ElFormItem>
          <TdButton class="query mr-10px" :text="t('alarm.btn.query')" @click="handleSearch" />
          <TdButton class="query" :text="t('alarm.btn.export')" @click="exportFn" />
        </ElFormItem>
      </ElForm>
    </div>

    <div class="table-box ground-glass">
      <TdTitle>{{ t('policeAdmin.service.attendance') }}</TdTitle>
      <div ref="contentRef" class="content">
        <ElTable
          ref="tableRef"
          border
          class="table-content"
          :data="tableData"
          :max-height="tableMaxHeight"
          style="width: 100%"
          @selection-change="handleSelectionChange"
        >
          <ElTableColumn align="center" type="selection" width="55" />
          <ElTableColumn
            align="center"
            :label="t('policeAdmin.service.executorName')"
            prop="date"
            width="300"
          >
            <template #default="{ row }">
              {{ row.executorName }} （{{ row.executorCode }}）
            </template>
          </ElTableColumn>
          <ElTableColumn
            align="center"
            :label="t('personCenter.belongOrgName')"
            prop="organizationName"
          />
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
          <ElTableColumn
            align="center"
            fixed="right"
            :label="t('policeAdmin.btn.operation')"
            width="250"
          >
            <template #default="{ row }">
              <TdButton
                border
                icon-name="category_track"
                radius
                size="auto"
                :text="t('policeAdmin.service.history')"
                @click="history(row)"
              />
            </template>
          </ElTableColumn>
        </ElTable>

        <ElPagination
          v-model:page-size="form.pageSize"
          background
          class="pagination"
          layout="total, sizes, prev, pager, next"
          size="small"
          :total="total"
          @current-change="pageNumChange"
          @size-change="sizeChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/formLayout.less';

  .service-status {
    width: calc(100% - 30px);
    padding: 20px 0 0 10px;
  }

  .filter {
    .filter-form {
      display: flex;
      align-items: center;
      height: 80px;

      .el-form-item {
        margin-right: 40px;
      }
    }
  }
</style>
