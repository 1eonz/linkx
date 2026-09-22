<script setup lang="ts">
  import { reactive, ref } from 'vue';

  import { Delete } from '@element-plus/icons-vue';

  const tableData = ref([
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
  ]);
  const form = reactive({
    name: '',
    region: '',
    type: '',
  });

  function handleDelete(row: any) {
    console.log('Delete:', row);
  }

  function onAddItem() {
    tableData.value.push({
      address: 'No. 189, Grove St, Los Angeles',
      date: '2016-05-03',
      name: 'New Item',
    });
  }
</script>

<template>
  <el-card class="card">
    <el-table border :data="tableData" style="width: 100%">
      <el-table-column label="序号" type="index" width="80" />
      <el-table-column label="名称" prop="name" width="180" />
      <el-table-column align="center" label="">
        <template #default="scope">
          <el-form label-width="auto" :model="form">
            <el-form-item label="">
              <div class="flex items-center w-full">
                <el-image fit="cover" :src="scope.row.address" style="width: 172px; height: 74px" />
                <el-input v-model="form.name" style="width: 400px; margin: 0 8px" />
                <el-upload
                  action="https://jsonplaceholder.typicode.com/posts/"
                  :auto-upload="false"
                  class="upload-demo"
                  :show-file-list="false"
                >
                  <el-button class="upload-demo" size="small">选择图片</el-button>
                </el-upload>
              </div>
            </el-form-item>
            <el-form-item label="跳转链接：" label-width="172">
              <el-input v-model="form.region" style="width: 400px; margin: 0 8px" />
            </el-form-item>
            <el-form-item label="标题：" label-width="172">
              <el-input v-model="form.type" style="width: 400px; margin: 0 8px" />
            </el-form-item>
          </el-form>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <Delete class="w-16px cursor-pointer" @click="handleDelete(scope.row)" />
        </template>
      </el-table-column>
    </el-table>
    <div class="flex justify-center">
      <el-button class="mt-4" style="width: 310px; height: 44px" @click="onAddItem">
        添加轮播图
      </el-button>
    </div>
  </el-card>
</template>

<style scoped>
  .card {
    width: 100%;
    height: calc(100vh - 80px);
    overflow-y: scroll;

    .el-button {
      border-radius: 2px;
      border: 1px solid rgba(30, 82, 242, 1);
      color: rgba(30, 82, 242, 1);
    }
  }
</style>
