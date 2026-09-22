<script setup lang="ts">
  import { onMounted, ref } from 'vue';

  import { deleteAiagent, getAiagentPage, queryCategory } from '@/api';
  import defaultImg from '@/assets/images/default.png';
  import { displayImage } from '@/utils';

  import { ElMessage, ElMessageBox } from 'element-plus';

  import Edit from './edit.vue';
  import TypeEdit from './typeEdit.vue';

  const tableData = ref([]);
  const currentPage = ref(1);
  const pageSize = ref(20);
  const total = ref(0);
  const loading = ref(false);
  const editRef = ref<any>(null);
  const priorityLabels = ['高', '中', '低'];

  const searchForm = ref({
    name: '',
    categoryId: '',
  });

  const typeList = ref<any>([]);
  const showTypeDialog = ref(false);

  onMounted(() => {
    fetchData();
    queryType();
  });

  async function fetchData() {
    loading.value = true;
    try {
      const params: any = {
        pageNo: currentPage.value,
        pageSize: pageSize.value,
      };
      if (searchForm.value.name) params.name = searchForm.value.name;
      if (searchForm.value.categoryId) params.categoryId = searchForm.value.categoryId;
      const { code, data } = await getAiagentPage(params);
      if (code === 0) {
        tableData.value = data.list || [];
        total.value = data.total || 0;
      }
    } catch (error) {
      tableData.value = [];
      total.value = 0;
    } finally {
      loading.value = false;
    }
  }

  async function queryType() {
    try {
      const res = await queryCategory();
      if (res?.code === 0) {
        typeList.value = res.data || [];
      }
    } catch (error) {
      console.error('获取分类失败:', error);
      typeList.value = [];
    }
  }

  function handleAdd() {
    editRef.value?.open('add');
  }

  function handleEdit(row: any) {
    editRef.value?.open('edit', {
      audio: row.audio,
      audioTypeList: row.audioTypeList,
      avatar: row.avatar,
      body: row.body,
      bodyType: row.bodyType,
      categoryIds: row.categoryIds,
      categoryName: row.categoryName,
      desc: row.desc,
      document: row.document,
      documentTypeList: row.documentTypeList,
      endFlag: row.endFlag,
      header: row.header,
      httpMethod: row.httpMethod,
      id: row.id,
      image: row.image,
      imageTypeList: row.imageTypeList,
      isRestricted: row.isRestricted,
      receiveIm: row.receiveIm,
      scope: row.scope,
      name: row.name,
      paramScript: row.paramScript,
      priority: row.priority,
      query: row.query,
      respScript: row.respScript,
      token: row.token,
      url: row.url,
      video: row.video,
      videoTypeList: row.videoTypeList,
      fileInterfaceId: row.fileInterfaceId,
      virtualUserId: row.virtualUserId,
    });
  }

  function handleDelete(row: any) {
    ElMessageBox.confirm('确认删除该智能体吗？', '请确认', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    }).then(async () => {
      try {
        const res = await deleteAiagent(row.id);
        if (res.code === 0) {
          ElMessage.success(res.msg || '删除成功');
          fetchData();
        } else {
          ElMessage.error(res.msg || '删除失败');
        }
      } catch (error) {
        ElMessage.error('删除失败');
      }
    }).catch(() => {});
  }

  function handleSearch() {
    currentPage.value = 1;
    fetchData();
  }

  function handleReset() {
    searchForm.value = { name: '', categoryId: '' };
    currentPage.value = 1;
    fetchData();
  }

  function handleCurrentChange(page: number) {
    currentPage.value = page;
    fetchData();
  }

  function handleSizeChange(newSize: number) {
    pageSize.value = newSize;
    fetchData();
  }
</script>

<template>
  <el-card class="card">
    <div class="header">
      <el-input
        v-model="searchForm.name"
        clearable
        placeholder="请输入名称"
        style="width: 200px"
        @keyup.enter="handleSearch"
      />
      <el-select
        v-model="searchForm.categoryId"
        clearable
        filterable
        placeholder="请选择分类"
        style="width: 200px"
      >
        <el-option
          v-for="item in typeList"
          :key="item.id"
          :label="item.name"
          :value="item.id"
        />
      </el-select>
      <div class="btn-wrap">
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button type="primary" @click="handleReset">重置</el-button>
        <el-button type="primary" @click="showTypeDialog = true">创建分类</el-button>
        <el-button type="primary" @click="handleAdd">创建智能体</el-button>
      </div>
    </div>
    <div class="table-wrapper">
      <el-table v-loading="loading" border :data="tableData" style="width: 100%" class="agent-table">
        <el-table-column label="序号" type="index" width="60" align="center" />
        <el-table-column label="名称" prop="name" width="180" />
        <el-table-column label="说明" prop="desc" show-overflow-tooltip />
        <el-table-column label="图标" prop="avatar" width="80" align="center">
          <template #default="scope">
            <el-image
              fit="cover"
              :src="scope.row.avatar ? displayImage(scope.row.avatar) : defaultImg"
              style="width: 48px; height: 50px"
            />
          </template>
        </el-table-column>
        <el-table-column label="三方智能体对接地址" prop="url" show-overflow-tooltip />
        <el-table-column label="Token" prop="token" show-overflow-tooltip />
        <el-table-column label="分类" prop="categoryName" />
        <el-table-column label="优先级" prop="priority" width="80" align="center">
          <template #default="scope">
            {{ priorityLabels[scope.row.priority] || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="是否涉密" prop="isRestricted" width="80" align="center">
          <template #default="scope">
            {{ scope.row.isRestricted === 1 ? '是' : '否' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" align="center">
          <template #default="scope">
            <el-button size="small" type="primary" @click="handleEdit(scope.row)">修改</el-button>
            <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="bottom">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        @current-change="handleCurrentChange"
        @size-change="handleSizeChange"
      />
    </div>
  </el-card>

  <Edit ref="editRef" :type-list="typeList" @update="fetchData" @refresh-category="queryType" />

  <TypeEdit
    v-if="showTypeDialog"
    v-model="showTypeDialog"
    :props-type-list="typeList"
    @query-type="queryType"
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
      flex-shrink: 0;
      padding: 16px 24px;
      display: flex;
      align-items: center;
      gap: 8px;

      .btn-wrap {
        display: flex;
        align-items: center;
      }
    }

    .bottom {
      display: flex;
      justify-content: flex-end;
      padding: 16px 24px;
      flex-shrink: 0;
    }

    .table-wrapper {
      flex: 1;
      min-height: 0;
      padding: 0 24px;

      :deep(.agent-table) {
        height: 100%;
      }
    }
  }
</style>
