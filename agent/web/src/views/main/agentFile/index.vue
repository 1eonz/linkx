<script setup lang="ts">
  import { onMounted, ref } from 'vue';

  import {
    createAgentFile,
    deleteAgentFile,
    getAgentFileList,
    updateAgentFile,
  } from '@/api';

  import { ElMessage, ElMessageBox, FormInstance } from 'element-plus';

  const loading = ref(false);
  const tableData = ref<any[]>([]);
  const dialogVisible = ref(false);
  const operate = ref<'add' | 'edit'>('add');
  const submitLoading = ref(false);

  const baseForm = {
    body: '',
    desc: '',
    header: '',
    id: null as number | null,
    ip: '',
    method: 'GET',
    name: '',
    port: '',
    query: '',
    reponseFileFiled: '',
    uri: '',
  };

  const form = ref({ ...baseForm });
  const formRef = ref<FormInstance | null>(null);

  const rules = {
    name: [{ message: '请输入名称', required: true, trigger: 'blur' }],
    method: [{ message: '请选择请求方式', required: false, trigger: 'change' }],
    ip: [{ message: '请输入IP', required: false, trigger: 'blur' }],
    port: [{ message: '请输入端口', required: false, trigger: 'blur' }],
    uri: [{ message: '请输入路径', required: false, trigger: 'blur' }],
  };

  async function fetchData() {
    loading.value = true;
    try {
      const res = await getAgentFileList();
      if (res.code === 0) {
        tableData.value = res.data || [];
      }
    } catch (error) {
      console.error('获取列表失败:', error);
      tableData.value = [];
    } finally {
      loading.value = false;
    }
  }

  function handleAdd() {
    operate.value = 'add';
    resetForm();
    dialogVisible.value = true;
  }

  function handleEdit(row: any) {
    operate.value = 'edit';
    const { id, name, method, uri, ip, port, header, query, body, reponseFileFiled, desc } = row;
    form.value = { id, name, method, uri, ip, port, header, query, body, reponseFileFiled, desc };
    dialogVisible.value = true;
  }

  function handleDelete(row: any) {
    ElMessageBox.confirm('确认删除该配置吗？', '请确认', {
      cancelButtonText: '取消',
      confirmButtonText: '确认',
      type: 'warning',
    }).then(async () => {
      try {
        const res = await deleteAgentFile(row.id);
        if (res.code === 0) {
          ElMessage.success(res.msg || '删除成功');
          fetchData();
        } else {
          ElMessage.error(res.msg || '删除失败');
        }
      } catch (error) {
        console.error('删除失败:', error);
      }
    }).catch(() => {});
  }

  async function handleSubmit() {
    const valid = await formRef.value?.validate().catch(() => false);
    if (!valid) return;

    submitLoading.value = true;
    try {
      const api = operate.value === 'edit' ? updateAgentFile : createAgentFile;
      const res = await api(form.value);
      if (res.code === 0) {
        ElMessage.success(operate.value === 'edit' ? '编辑成功' : '新增成功');
        dialogVisible.value = false;
        fetchData();
      } else {
        ElMessage.error(res.msg || '操作失败');
      }
    } catch (error) {
      console.error('操作失败:', error);
    } finally {
      submitLoading.value = false;
    }
  }

  function resetForm() {
    form.value = { ...baseForm };
  }

  function handleDialogClose() {
    resetForm();
    formRef.value?.clearValidate();
  }

  function getMethodTagType(method: string): 'success' | 'warning' | 'info' | 'primary' | 'danger' {
    const typeMap: Record<string, 'success' | 'warning' | 'info' | 'primary' | 'danger'> = {
      DELETE: 'danger',
      GET: 'success',
      POST: 'primary',
      PUT: 'warning',
    };
    return typeMap[method] || 'info';
  }

  onMounted(() => {
    fetchData();
  });
</script>

<template>
  <div class="agent-file">
    <el-card class="card">
      <div class="header">
        <div class="btn-wrap">
          <el-button type="primary" @click="handleAdd">新增配置</el-button>
        </div>
      </div>

      <div class="table-wrapper">
        <el-table v-loading="loading" border :data="tableData" style="width: 100%" class="agent-file-table">
          <el-table-column label="序号" type="index" width="60" align="center" />
          <el-table-column label="名称" prop="name" width="150" />
          <el-table-column label="请求方式" prop="method" width="100" align="center">
            <template #default="scope">
              <el-tag :type="getMethodTagType(scope.row.method)" size="small">
                {{ scope.row.method }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="IP" prop="ip" />
          <el-table-column label="端口" prop="port" />
          <el-table-column label="路径" prop="uri" show-overflow-tooltip />
          <el-table-column label="Header参数" prop="header" width="150" align="center" />
          <el-table-column label="Query参数" prop="query" width="150" align="center" />
          <el-table-column label="Body参数" prop="body" width="150" align="center" />
          <el-table-column label="文件标识字段" prop="reponseFileFiled" width="120" />
          <el-table-column label="描述" prop="desc" width="150" show-overflow-tooltip />
          <el-table-column label="操作" width="150" align="center" fixed="right">
            <template #default="scope">
              <el-button size="small" type="primary" @click="handleEdit(scope.row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="operate === 'add' ? '新增配置' : '编辑配置'"
      width="724px"
      class="agent-file-dialog"
      :close-on-click-modal="false"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        style="width: 90%"
        :model="form"
        :rules="rules"
        label-width="120px"
        label-position="left"
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="请求方式" prop="method">
          <el-select v-model="form.method" placeholder="请选择请求方式" style="width: 100%">
            <el-option label="GET" value="GET" />
            <el-option label="POST" value="POST" />
            <el-option label="PUT" value="PUT" />
            <el-option label="DELETE" value="DELETE" />
          </el-select>
        </el-form-item>
        <el-form-item label="IP" prop="ip">
          <el-input v-model="form.ip" placeholder="请输入ip 如：192.168.1.100" />
        </el-form-item>
        <el-form-item label="端口" prop="port">
          <el-input v-model="form.port" placeholder="请输入端口" />
        </el-form-item>
        <el-form-item label="路径" prop="uri">
          <el-input v-model="form.uri" placeholder="请输入路径" />
        </el-form-item>
        <el-form-item label="Header参数">
          <el-input v-model="form.header" type="textarea" :rows="3" placeholder="JSON格式的Header参数" />
        </el-form-item>
        <el-form-item label="Query参数">
          <el-input v-model="form.query" type="textarea" :rows="3" placeholder="JSON格式的Query参数" />
        </el-form-item>
        <el-form-item label="Body参数">
          <el-input v-model="form.body" type="textarea" :rows="3" placeholder="JSON格式的Body参数" />
        </el-form-item>
        <el-form-item label="文件标识字段" prop="reponseFileFiled">
          <el-input v-model="form.reponseFileFiled" placeholder="文件标识字段" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.desc" placeholder="请输入描述" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="less">
  .agent-file-dialog {
    display: flex !important;
    flex-direction: column !important;
    max-height: 80vh !important;
    margin-top: 10vh !important;
    overflow: hidden !important;

    .el-dialog__header {
      flex-shrink: 0;
    }

    .el-dialog__body {
      flex: 1;
      overflow-y: auto;
      padding: 20px 0;
      margin: 0 20px;
      margin-left: 60px;
      min-height: 0;
    }

    .el-dialog__footer {
      flex-shrink: 0;
    }

    .el-form-item:not(.is-required) .el-form-item__label::before {
      content: '*';
      color: transparent;
      margin-right: 4px;
    }
  }
</style>

<style lang="less" scoped>
  .agent-file {
    width: 100%;
    height: 100%;
    flex: 1;
    display: flex;
    flex-direction: column;
  }

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

    .table-wrapper {
      flex: 1;
      min-height: 0;
      padding: 0 24px;
      padding-bottom: 16px;

      :deep(.agent-file-table) {
        height: 100%;
      }
    }
  }
</style>
