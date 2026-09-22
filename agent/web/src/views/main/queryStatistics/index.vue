<script setup lang="ts">
  import { onMounted, ref } from 'vue';

  import { deleteAiagentRecord, exportAiagentRecord, getAiagentRecordPage } from '@/api';

  import dayjs from 'dayjs';
  import { ElMessage, ElMessageBox } from 'element-plus';
  import { throttle } from 'lodash-es';

  const tableData = ref([]);
  const query = ref({
    content: '',
    identityCardNumber: '',
    pageNo: 1,
    pageSize: 20,
    time: '',
    userName: '',
  });
  const time = ref([]);
  const total = ref(0);
  const selection = ref<any[]>([]);
  const fmt = 'YYYY-MM-DD HH:mm:ss';

  onMounted(() => {
    handleSearch();
  });

  function getParam() {
    const [start, end] = time.value || [];
    const param = {
      ...query.value,
      maxTime: end ? dayjs(end).format(fmt) : '',
      minTime: start ? dayjs(start).format(fmt) : '',
    };
    return param;
  }

  async function handleSearch() {
    const res = await getAiagentRecordPage(getParam());
    if (res.code === 0) {
      tableData.value = res.data.list;
      total.value = res.data.total;
    } else {
      ElMessage.error('查询失败');
    }
  }

  function handleDelete(row: any) {
    ElMessageBox.alert('确认删除该查询记录吗？', '请确认', {
      callback: async (action) => {
        if (action === 'confirm') {
          const res = await deleteAiagentRecord(row.id);
          if (res.code === 0) {
            ElMessage({
              message: res.msg || '删除成功',
              type: 'success',
            });
            handleSearch();
          } else {
            ElMessage.error(res.msg || '删除失败');
          }
        }
      },
      confirmButtonText: '确认',
    });
  }

  function handleCurrentChange(page: number) {
    query.value.pageNo = page;
    handleSearch();
  }

  function handleSizeChange(newSize: number) {
    query.value.pageSize = newSize;
    handleSearch();
  }

  const handleExport = throttle(async () => {
    try {
      const param = {
        ...getParam(),
        ids: selection.value.map((item) => item.id).join(',') || undefined,
      };
      const res = await exportAiagentRecord(param);
      const blob = new Blob([res as unknown as BlobPart], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = '查询统计.xlsx';
      link.click();
      URL.revokeObjectURL(link.href);
    } catch {
      ElMessage.error('导出失败');
    }
  }, 500);

  function handleSelectionChange(data) {
    selection.value = data;
  }

  function handleRest() {
    query.value = {
      content: '',
      identityCardNumber: '',
      pageNo: 1,
      pageSize: 20,
      time: '',
      userName: '',
    };
    time.value = [];
    handleSearch();
  }
</script>

<template>
  <el-card class="card">
    <div class="card-content">
      <div class="header">
        <el-input v-model="query.userName" clearable placeholder="请输入查询人" style="width: 200px" />
        <el-input v-model="query.identityCardNumber" clearable placeholder="请输入身份证" style="width: 200px" />
        <el-input v-model="query.content" clearable placeholder="请输入问题" style="width: 200px" />
        <div style="flex-shrink: 0">
          <el-date-picker
            v-model="time"
            :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 2, 1, 23, 59, 59)]"
            end-placeholder="结束时间"
            range-separator="-"
            start-placeholder="开始时间"
            type="datetimerange"
          />
        </div>
        <div class="btn-wrap">
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button type="primary" @click="handleExport">导出</el-button>
          <el-button type="primary" @click="handleRest">重置</el-button>
        </div>
      </div>
      <div class="table-wrapper">
        <el-table
          border
          :data="tableData"
          style="width: 100%"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column label="查询人" prop="userName" width="180" />
          <el-table-column label="查询人身份证号" prop="identityCardNumber" />
          <el-table-column label="查询智能体" prop="agentName" />
          <el-table-column label="查询内容" prop="queryContent" />
          <el-table-column label="查询时间" prop="time">
            <template #default="scope">
              {{ scope.row.time ? dayjs(scope.row.time).format('YYYY-MM-DD HH:mm') : '' }}
            </template>
          </el-table-column>
          <el-table-column label="操作">
            <template #default="scope">
              <el-button size="small" style="color: red" type="text" @click="handleDelete(scope.row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="bottom">
        <el-pagination
          v-model:current-page="query.pageNo"
          v-model:page-size="query.pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>
  </el-card>
</template>

<style scoped lang="less">
  .card {
    width: 100%;
    height: 100%;
    flex: 1;
    display: flex !important;
    flex-direction: column !important;

    :deep(.el-card__body) {
      height: 100%;
      flex: 1 !important;
      padding: 0 !important;
      margin: 0 !important;
      display: flex !important;
      flex-direction: column !important;
    }

    .card-content {
      flex: 1;
      display: flex;
      flex-direction: column;
      min-height: 0;
    }

    .header {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 16px 24px;
      flex-shrink: 0;

      .btn-wrap {
        display: flex;
        align-items: center;
      }
    }

    .table-wrapper {
      flex: 1;
      min-height: 0;
      padding: 0 24px;

      :deep(.el-table) {
        height: 100%;
      }
    }

    .bottom {
      display: flex;
      justify-content: flex-end;
      padding: 16px 24px;
      flex-shrink: 0;
    }
  }
</style>
