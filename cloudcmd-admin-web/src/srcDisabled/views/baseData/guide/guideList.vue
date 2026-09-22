<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container">
        <!-- <el-input
          v-model="inputName"
          :placeholder="$t('index.operations.inputContent')"
          style="width: 200px;"
          class="filter-item"
          clearable
          @keyup.enter.native="handleFilter"
        />
        <el-button
          v-waves
          class="filter-item"
          type="primary"
          icon="el-icon-search"
          @click="handleFilter"
        >
          {{ $t('index.operations.search') }}
        </el-button> -->
        <el-button
          v-if="hasPerm('/admin/executor/create')"
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-circle-plus-outline"
          @click="handleCreate"
        >
          {{ $t('index.operations.Added') }}
        </el-button>
      </div>

      <el-table
        v-loading="listLoading"
        :data="list"
        stripe
        border
        fit
        row-key="id"
        highlight-current-row
        style="width: 100%;"
      >
        <el-table-column type="selection" align="center" width="40" />
        <el-table-column
          :label="$t('index.list.headline')"
          :show-overflow-tooltip="true"
          min-width="60"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.fileName')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            {{ scope.row.fileName }}
          </template>
        </el-table-column>
        <!-- <span>{{ getAddress(scope.row.address) }}</span> -->

        <el-table-column
          :label="$t('index.list.explain')"
          :show-overflow-tooltip="true"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.description }}</span>
          </template>
        </el-table-column>
        <el-table-column
          fixed="right"
          header-align="center"
          align="center"
          width="400"
          :label="$t('index.operations.operation')"
        >
          <template slot-scope="{ row }">
            <el-button
              type="primary"
              icon="el-icon-edit"
              size="small"
              @click="handleUpdate(row)"
            >
              {{ $t('index.operations.change') }}
            </el-button>
            <el-button
              type="danger"
              icon="el-icon-delete"
              size="small"
              @click="handleDelete(row)"
            >
              {{ $t('index.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="pageNum"
        :limit.sync="pageSize"
        @pagination="getList"
      />
    </el-card>

    <!-- 新增/修改 -->
    <guide-edit ref="editGuide" @success="handleGetList" />
  </div>
</template>

<script>
import pagination from '@/components/Pagination'
import GuideEdit from './guideEdit.vue'
import { getGuideList, deleteGuide } from '@/api/guide/guide'

export default {
  name: 'GuideList',
  components: {
    GuideEdit,
    pagination
  },
  data() {
    return {
      list: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      inputName: '',
      listLoading: false,
      showVehicle: true
    }
  },
  mounted() {
    this.getList()
  },
  methods: {
    getAddress(address) {
      const token = getToken()
      const url = process.env.VUE_APP_BASE_API + address
      const request = new XMLHttpRequest()

      request.responseType = 'blob'
      request.open('get', url, true)
      request.setRequestHeader('Authorization', 'token ' + token)
      request.onreadystatechange = e => {
        if (
          request.readyState === XMLHttpRequest.DONE &&
          request.status === 200
        ) {
          const binary = []
          binary.push(request.response)
          const res = window.URL.createObjectURL(new Blob(binary))
          console.log(res, '=======================res')
        }
      }
      request.send(null)
    },
    handleGetList() {
      this.getList()
    },
    // 点击删除
    handleDelete(data) {
      if (Array.isArray(data)) {
        if (data.length < 1) {
          this.$message({
            message: this.$t('index.messageText.pleaseCheckData'),
            type: 'error'
          })
          return
        } else {
          const array = []
          for (const person of data) {
            array.push(person.id)
          }
          this.delete(array)
        }
      } else {
        this.delete(Array.of(data.id))
      }
    },
    // 删除请求
    delete(array) {
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          deleteGuide(array).then(result => {
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
        .catch(() => {})
    },

    // 获取人员列表
    getList() {
      this.listLoading = true
      const params = {
        pageNum: this.pageNum,
        pageSize: this.pageSize
      }
      getGuideList(params).then(result => {
        const { data, code } = result
        if (code === 0) {
          this.list = data.records
          this.total = data.total
        }
        this.listLoading = false
      })
      if (localStorage.getItem('hiddenVehicle')) {
        this.showVehicle = false
      } else {
        this.showVehicle = true
      }
    },
    // 搜索
    handleFilter() {
      this.pageNum = 1
      this.getList()
    },
    // 新增
    handleCreate() {
      this.$refs.editGuide.add()
    },
    // 修改
    handleUpdate(row) {
      this.$refs.editGuide.modify(row)
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
  height: 70px;
  width: 70px;
}
.filter-container {
  padding-bottom: 10px;
  .filter-item {
    display: inline-block;
    vertical-align: middle;
    margin-bottom: 10px;
  }
}
.el-row {
  text-align: left;
  margin-left: 10px;
}
.el-col {
  margin: 8px;
}
.pointer {
  cursor: pointer;
}

.el-button--small {
  // width: 100px !important;
  margin-top: 5px;
  margin-left: 5px;
}
</style>
