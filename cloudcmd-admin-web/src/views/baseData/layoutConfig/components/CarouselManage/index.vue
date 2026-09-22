<template>
  <div class="carousel-manage">
    <!-- 搜索栏 + 操作按钮 -->
    <search-bar
      search-key="title"
      search-placeholder="请输入标题"
      :actions="actions"
      @search="handleSearch"
      @reset="handleReset"
      @action="handleAction"
    />

    <el-table
      v-loading="loading"
      class="carousel-manage__table"
      :data="list"
      :show-overflow-tooltip="true"
      style="width: 100%;"
      :header-cell-style="{ background: '#fafbfc', color: '#303133', fontWeight: 600 }"
    >
      <el-table-column label="序号" align="center" width="80px" type="index" />
      <el-table-column label="轮播图" align="center" prop="pciUrl" width="200">
        <template #default="{ row }">
          <div v-if="row.pciUrl" class="carousel-manage__thumb">
            <auth-img class="head-shot" :auth-src="row.pciUrl" />
          </div>
          <div v-else class="carousel-manage__thumb-empty">
            <i class="el-icon-picture-outline"></i>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="标题" align="left" prop="title" min-width="180" show-overflow-tooltip />
      <el-table-column label="跳转链接" align="left" prop="url" min-width="240" show-overflow-tooltip />
      <el-table-column label="排序值" align="center" prop="sort" width="90px" />
      <el-table-column label="操作" align="center" width="160px" fixed="right">
        <template #default="scope">
          <el-button type="text" icon="el-icon-edit" @click="openForm('update', scope.row.id)">
            {{ $t('index.operations.change') }}
          </el-button>
          <el-button type="text" class="carousel-manage__btn-danger" icon="el-icon-delete" @click="handleDelete(scope.row.id)">
            {{ $t('index.delete') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 空状态 -->
    <div v-if="!loading && list.length === 0" class="carousel-manage__empty">
      <i class="el-icon-picture-outline carousel-manage__empty-icon"></i>
      <p class="carousel-manage__empty-text">暂无轮播图</p>
    </div>

    <!-- 分页 -->
    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 表单弹窗：添加/修改 -->
    <carousel-form ref="formRef" @success="getList" />
  </div>
</template>

<script>
import { getCarouselPage, deleteCarousel } from '@/api/h5/carousel'
import carouselForm from './carouselForm'
import pagination from '@/components/Pagination'
import authImg from '@/components/AuthImg'
import SearchBar from '@/components/SearchBar'

export default {
  name: 'CarouselManage',
  components: { pagination, carouselForm, authImg, SearchBar },
  data() {
    return {
      list: [],
      loading: false,
      total: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        title: undefined
      },
      actions: [
        { key: 'create', label: this.$t('index.operations.Added'), icon: 'el-icon-circle-plus-outline', type: 'primary' }
      ]
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

    /** 搜索 */
    handleSearch(params) {
      this.queryParams.title = params.title || undefined
      this.queryParams.pageNum = 1
      this.getList()
    },

    /** 重置 */
    handleReset() {
      this.queryParams.title = undefined
      this.queryParams.pageNum = 1
      this.getList()
    },

    /** 操作按钮 */
    handleAction(key) {
      if (key === 'create') {
        this.openForm('create')
      }
    },

    /** 添加/修改操作 */
    openForm(type, id) {
      this.$refs.formRef.open(type, id)
    },

    /** 删除 */
    handleDelete(id) {
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
    }
  }
}
</script>

<style lang="scss" scoped>
.carousel-manage {
  padding: 4px 0 16px;
}

// 表格
.carousel-manage__table {
  border-radius: 4px;
  overflow: hidden;
}

// 缩略图容器：固定比例，留白居中
.carousel-manage__thumb {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 140px;
  height: 70px;
  border-radius: 4px;
  overflow: hidden;
  background: #f7f9fc;
  border: 1px solid #ebeef5;

  .head-shot {
    display: block;
    height: 100%;
    width: 100%;
    object-fit: cover;
    border-radius: 3px;
  }
}

// 无图占位
.carousel-manage__thumb-empty {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 140px;
  height: 70px;
  border-radius: 4px;
  background: #f7f9fc;
  border: 1px dashed #dcdfe6;
  color: #c0c4cc;
  font-size: 24px;
}

// 操作列：危险按钮
.carousel-manage__btn-danger {
  color: #f56c6c;

  &:hover {
    color: #f78989;
  }
}

// 空状态
.carousel-manage__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 56px 0 32px;
  color: #909399;
  font-size: 14px;
}

.carousel-manage__empty-icon {
  font-size: 48px;
  color: #c0c4cc;
  margin-bottom: 12px;
}

.carousel-manage__empty-text {
  margin: 0;
  color: #606266;
}
</style>
