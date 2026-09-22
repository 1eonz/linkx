<script setup lang="ts">
  import { ref } from 'vue';

  import Edit from './edit.vue';

  const dialogFormVisible = ref(false);

  const tableData = [
    {
      address: 'No. 189, Grove St, Los Angeles',
      date: '2016-05-03',
      name: 'Tom',
    },
    {
      address: 'No. 189, Grove St, Los Angeles',
      date: '2016-05-02',
      name: 'Tom',
    },
  ];

  const currentPage = ref(1);
  const pageSize = ref(100);
  const background = ref(true);
  const disabled = ref(false);
  const size = ref<'' | 'default' | 'large' | 'small'>('default');

  function handleAdd() {
    dialogFormVisible.value = true;
  }

  function handleEdit(row: any) {
    console.log('Edit:', row);
  }

  function handleDelete(row: any) {
    console.log('Delete:', row);
  }

  function handleCurrentChange(page: number) {
    currentPage.value = page;
    // Add your logic for page change here
  }

  function handleSizeChange(newSize: number) {
    pageSize.value = newSize;
    // Add your logic for page size change here
  }
</script>

<template>
  <el-card class="card">
    <div class="header">
      <el-button type="primary" @click="handleAdd">新建</el-button>
    </div>
    <el-table border :data="tableData" style="width: 100%">
      <el-table-column label="序号" type="index" width="80" />
      <el-table-column label="名称" prop="name" width="180" />
      <el-table-column label="图标" prop="address" />
      <el-table-column label="URL" prop="address" />
      <el-table-column label="创建时间" prop="address" />
      <el-table-column label="操作">
        <template #default="scope">
          <el-button size="small" type="text" @click="handleEdit(scope.row)">修改</el-button>
          <el-button size="small" type="text" @click="handleEdit(scope.row)">上架</el-button>
          <el-button size="small" style="color: red" type="text" @click="handleDelete(scope.row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="bottom">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :background="background"
        :disabled="disabled"
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[100, 200, 300, 400]"
        :size="size"
        :total="400"
        @current-change="handleCurrentChange"
        @size-change="handleSizeChange"
      />
    </div>
  </el-card>
  <Edit v-if="dialogFormVisible" v-model:visible="dialogFormVisible" />
</template>

<style scoped>
  .card {
    width: 100%;
    height: 100%;

    .header {
      display: flex;
      justify-content: flex-end;
      padding: 24px 0;
    }

    .bottom {
      display: flex;
      justify-content: flex-end;
      padding: 24px 0;
    }
  }
</style>
