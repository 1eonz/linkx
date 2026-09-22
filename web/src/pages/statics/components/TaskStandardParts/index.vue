<template>
  <div class="task-standard-parts table-page table-page-content">
    <!-- 搜索表单 -->
    <div class="search-form">
      <el-input
        v-model="searchForm.name"
        placeholder="任务名称"
        clearable
        style="width: 240px"
        @clear="handleSearch"
        @keyup.enter="handleSearch"
      />
      <el-input
        v-model="searchForm.content"
        placeholder="任务内容"
        clearable
        style="width: 240px"
        @clear="handleSearch"
        @keyup.enter="handleSearch"
      />
      <el-button class="search-btn" :icon="RefreshLeft" @click="handleReset">重置</el-button>
      <el-button class="search-btn" :icon="Search" @click="handleSearch">搜索</el-button>
    </div>

    <!-- 表格 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      border
      style="width: 100%"
      :empty-text="loading ? '加载中...' : '暂无数据'"
    >
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="number" label="任务编号" min-width="140" show-overflow-tooltip />
      <el-table-column prop="name" label="任务名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
      <el-table-column
        prop="businessType"
        label="业务类型"
        width="120"
        align="center"
        :formatter="formatBusinessType"
      />
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <StatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="是否紧急" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.urgent ? 'danger' : 'info'">{{ row.urgent ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="startTime" label="任务开始时间" min-width="160" />
      <el-table-column prop="endTime" label="任务结束时间" min-width="160" />
      <el-table-column
        prop="creator"
        label="创建人"
        width="100"
        align="center"
        :formatter="formatCreator"
      />
      <el-table-column
        prop="executors"
        label="执行人姓名"
        width="140"
        align="center"
        :formatter="formatExecutors"
      />
      <el-table-column prop="operateTime" label="操作时间" min-width="160" />
      <!-- 操作列 -->
      <el-table-column fixed="right" label="操作" width="120" align="center">
        <template #default="{ row }">
          <span
            v-if="row.type === 1 || row.url"
            class="btn-item btn-detail"
            link
            @click="openDetail(row)"
          >
            查看详情
          </span>
          <span v-else class="na">N/A</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div v-if="pagination.total > 0" class="pagination">
      <el-pagination
        background
        layout="sizes, prev, pager, next, jumper, total"
        :total="pagination.total"
        :current-page="pagination.pageNum"
        :page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <!-- 详情弹窗（系统内部生成任务 type=1） -->
    <TaskDetailDialog v-model:visible="detailVisible" :data="detailData as TasksVO" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { Search, RefreshLeft } from '@element-plus/icons-vue';
import { getTaskList } from '@/api/taskManage';
import type { TasksVO } from './types';
import TaskDetailDialog from './components/TaskDetailDialog.vue';
import StatusTag from './components/StatusTag.vue';
import { openWindow } from '@/bridge/post';

const props = defineProps<{ task: any }>();

// 搜索：字段名对齐接口文档（name / content）
const searchForm = reactive<{ name: string; content: string }>({
  name: '',
  content: '',
});

// 表格
const tableData = ref<TasksVO[]>([]);
const loading = ref(false);
const pagination = reactive<{ pageNum: number; pageSize: number; total: number }>({
  pageNum: 1,
  pageSize: 10,
  total: 0,
});

// 详情弹窗（系统内部生成任务 type=1）
const detailVisible = ref(false);
const detailData = ref<TasksVO | null>(null);
const openDetail = (row: TasksVO) => {
  if (row.type === 1) {
    detailData.value = row;
    detailVisible.value = true;
  } else if (row.url) {
    openWindow(row.url);
  }
};

// 业务类型/状态前端常量映射（后端就绪后可改为接口字典）
const businessTypeMap: Record<string, string> = {
  police: '警情处置',
  case: '案事件办理',
  instruction: '指令流转',
  warning: '信号预警',
};
const formatBusinessType = (_row: any, _col: any, val: string) => businessTypeMap[val] ?? val;
// 创建人取 creator.name
const formatCreator = (_row: any, _col: any, val: TasksVO['creator']) => val?.name || '--';
// 执行人列表拼接为姓名串
const formatExecutors = (_row: any, _col: any, val: TasksVO['executors']) => {
  if (!Array.isArray(val) || val.length === 0) return 'N/A';
  return val.map((e) => e.name).join('、');
};

// 加载任务列表
const loadTaskList = async () => {
  if (!props.task?.module) return;
  loading.value = true;
  try {
    const res: any = await getTaskList({
      module: props.task.module,
      name: searchForm.name || undefined,
      content: searchForm.content || undefined,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      scope: 4, // 范围： 创建人+执行人
    });
    if (res.code === 0) {
      tableData.value = res.data.records;
      pagination.total = res.data.total;
    } else {
      ElMessage.error(res.msg || '任务列表加载失败');
      tableData.value = [];
      pagination.total = 0;
    }
  } catch (e) {
    ElMessage.error('任务列表加载失败');
    console.error('[loadTaskList] failed:', e);
    tableData.value = [];
    pagination.total = 0;
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadTaskList();
});

// 搜索
const handleSearch = () => {
  pagination.pageNum = 1;
  loadTaskList();
};

// 重置
const handleReset = () => {
  searchForm.name = '';
  searchForm.content = '';
  pagination.pageNum = 1;
  loadTaskList();
};

// 分页
const handlePageChange = (p: number) => {
  pagination.pageNum = p;
  loadTaskList();
};
const handleSizeChange = (s: number) => {
  pagination.pageSize = s;
  pagination.pageNum = 1;
  loadTaskList();
};
</script>

<style scoped lang="less">
@import '@/styles/tablePageStyle.less';

.task-standard-parts {
  .btn-item {
    cursor: pointer;
    margin: 0 4px;
    color: var(--tabs-active-color, #3b72ff);
  }

  .btn-detail {
    color: var(--tabs-active-color, #3b72ff);
  }

  .na {
    color: var(--el-text-color-placeholder);
  }
}
</style>
