<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import Edit from './edit.vue';
import { getGlobals } from '@/api';

// 编辑弹窗
const dialogVisible = ref(false);
const currentRow = ref<any>(null);

// 表格数据
const tableData = ref<any[]>([]);
const loading = ref(false);

// 搜索
const searchName = ref('');

// 获取全局变量列表
async function fetchGlobals() {
  loading.value = true;
  try {
    const params: any = {};
    if (searchName.value) {
      params.name = searchName.value;
    }
    const res = await getGlobals(params);
    // 根据接口返回格式处理数据
    if (res?.data) {
      tableData.value = Array.isArray(res.data) ? res.data : [res.data];
    } else if (Array.isArray(res)) {
      tableData.value = res;
    } else {
      tableData.value = [];
    }
  } catch (error: any) {
    console.error('获取全局变量失败:', error);
    ElMessage.error(error?.message || '获取全局变量失败');
    tableData.value = [];
  } finally {
    loading.value = false;
  }
}

// 搜索
function handleSearch() {
  fetchGlobals();
}

// 重置搜索
function handleReset() {
  searchName.value = '';
  fetchGlobals();
}


// 编辑
function handleEdit(row: any) {
  currentRow.value = { ...row };
  dialogVisible.value = true;
}

// 编辑成功回调
function handleSuccess() {
  dialogVisible.value = false;
  fetchGlobals();
}

// 状态标签颜色
function getStatusType(status: number) {
  return status === 0 ? 'success' : 'danger';
}

// 状态文本
function getStatusText(status: number) {
  return status === 0 ? '启用' : '禁用';
}

onMounted(() => {
  fetchGlobals();
});
</script>

<template>
  <el-card class="card">
    <!-- 头部搜索和操作区 -->
    <div class="header">
      <el-input
        v-model="searchName"
        placeholder="请输入变量名称"
        clearable
        style="width: 200px"
        @keyup.enter="handleSearch"
      />
      <div class="btn-wrap">
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button type="primary" @click="handleReset">重置</el-button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="table-wrapper">
      <el-table
        v-loading="loading"
        border
        :data="tableData"
        style="width: 100%"
      >
        <el-table-column label="序号" type="index" width="80" />
        <el-table-column label="变量名称" prop="name" width="180" />
        <el-table-column label="变量值" prop="value" min-width="200">
          <template #default="{ row }">
            <el-tooltip
              :content="row.value"
              placement="top"
              :disabled="!row.value || row.value.length < 30"
            >
              <span class="value-text">{{ row.value || '-' }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" width="200">
          <template #default="{ row }">
            <span>{{ row.remark || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="scope">
            <el-button
              size="small"
              type="primary"
              link
              @click="handleEdit(scope.row)"
            >
              编辑
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 空状态提示 -->
    <div v-if="!loading && tableData.length === 0" class="empty-tip">
      暂无数据
    </div>
  </el-card>

  <!-- 编辑弹窗 -->
  <Edit
    v-if="dialogVisible"
    v-model:visible="dialogVisible"
    :row="currentRow"
    @success="handleSuccess"
  />
</template>

<style scoped lang="less">
.card {
  width: 100%;
  height: 100%;
  flex: 1;
  display: flex !important;
  flex-direction: column !important;

  :deep(.el-card__body) {
    flex: 1 !important;
    padding: 0 !important;
    margin: 0 !important;
    display: flex !important;
    flex-direction: column !important;
    height: 100% !important;
    overflow: hidden;
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
    padding-bottom: 16px;

    :deep(.el-table) {
      height: 100%;
    }
  }

  .value-text {
    display: inline-block;
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .empty-tip {
    text-align: center;
    padding: 40px 0;
    color: #999;
    font-size: 14px;
  }
}
</style>
