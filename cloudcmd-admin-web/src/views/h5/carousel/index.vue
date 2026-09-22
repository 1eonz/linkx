<template>
  <div class="app-container">
    <!-- 搜索工作栏 -->
    <el-form
      ref="queryFormRef"
      class="-mb-15px"
      :model="queryParams"
      :inline="true"
      label-width="68px"
      @submit.native.prevent="handleQuery"
      >
      <el-form-item label="" prop="title">
        <el-input
        v-model="queryParams.title"
        placeholder="标题"
        clearable
        style="width: 300px;"
        class="filter-item"
        @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button
        v-waves
        class="filter-item"
        style="margin-left: 10px;"
        type="primary"
        icon="el-icon-search"
        @click="handleQuery"
        >
          {{ $t('index.operations.search') }}
        </el-button>
        <el-button
        class="filter-item"
        style="margin-left: 10px;"
        type="primary"
        icon="el-icon-refresh"
        @click="resetQuery"
        >
          重置
        </el-button>
        <el-button
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-circle-plus-outline"
          @click="openForm('create')"
          >
          {{ $t('index.operations.Added') }}
        </el-button>
      </el-form-item>
    </el-form>

    <el-card shadow="always" class="card">
      <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
        <el-table-column label="序号" align="center" width="100px" type="index" />
        <el-table-column label="轮播图" align="center" prop="pciUrl" width="300">
          <template #default="{ row }">
            <authImg v-if="row.pciUrl" class="head-shot" :auth-src="row.pciUrl" />
          </template>
        </el-table-column>
        <el-table-column label="标题" align="center" prop="title" />
        <el-table-column label="跳转链接" align="center" prop="url" />
        <el-table-column label="排序值" align="center" prop="sort" width="80px" />
        <el-table-column label="操作" align="center" width="200px">
          <template #default="scope">
            <el-button type="primary" icon="el-icon-edit" size="small" @click="openForm('update', scope.row.id)">
              {{ $t('index.operations.change') }}
            </el-button>
            <el-button type="danger" icon="el-icon-delete" size="small" @click="handleDelete(scope.row.id)">
              {{ $t('index.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <!-- 分页 -->
      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="getList"
      />
    </el-card>
    <!-- 表单弹窗：添加/修改 -->
    <carousel-form ref="formRef" @success="getList" />
  </div>
</template>

<script>
import { getCarouselPage, deleteCarousel } from '@/api/h5/carousel'
import carouselForm from './carouselForm'
import pagination from '@/components/Pagination'
import authImg from '@/components/AuthImg'

export default {
  name: 'Carousel',
  components: { pagination, carouselForm, authImg },
  data() {
    return {
      list: [],
      loading: true,
      total: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 5,
        title: undefined
      }
    }
  },
  mounted() {
    this.getList()
  },
  methods: {
    async getList() {
      this.loading = true
      try {
        const { data } = await getCarouselPage(this.queryParams)
        this.list = data.records
        this.total = data.total
      } finally {
        this.loading = false
      }
    },

    /** 添加/修改操作 */
    openForm(type, id) {
      this.$refs.formRef.open(type, id)
    },

    /** 删除按钮操作 */
    async handleDelete(id) {
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          deleteCarousel(id).then(result => {
            if (result.code === 0) {
              this.$message({
                message: this.$t('index.statusTitle.successfullyDelete'),
                type: 'success'
              })
            } else {
              this.$message({
                message: result.msg,
                type: 'error'
              })
            }
            this.getList()
          })
        })
        .catch(() => { })
    },

    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },

    /** 重置按钮操作 */
    resetQuery() {
      this.$refs['queryFormRef'].resetFields()
      this.handleQuery()
    }
  }
}
</script>

<style lang="scss" scoped>
.card {
  height: 100%;
  overflow-y: auto;
}

.head-shot {
  display: inline-block;
  height: 80px;
  width: 170px;
}
</style>
