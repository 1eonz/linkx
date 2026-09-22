<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container">
        <el-input
          v-model="listQuery.systemName"
          :placeholder="$t('index.list.thirdPartyApp')"
          style="width: 200px;"
          class="filter-item"
          clearable
          @keyup.enter.native="handleFilter"
        />
        <el-button
          v-waves
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-search"
          @click="handleFilter"
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
        highlight-current-row
        style="width: 100%;"
      >
        <el-table-column
          type="index"
          :label="$t('index.list.Index')"
          width="60"
          align="center"
        />
        <el-table-column
          :label="$t('index.list.thirdPartyApp')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.systemName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.thirdPartyAppID')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.clientId }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.thirdPartyAppSecret')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <el-input
              v-model="scope.row.clientSecret"
              type="password"
              show-password
              disabled
            />
          </template>
        </el-table-column>

        <el-table-column
           v-if="false"
          :label="$t('index.list.thirdPartyAppType')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <el-select
              v-model="scope.row.clientType"
              placeholder="请选择应用类型"
              style="width: 100%;"
              disabled
            >
              <el-option label="协同统计" value="0" />
              <el-option label="三方应用" value="1" />
              <el-option label="三方任务" value="2" />
              <!-- <el-option label="在线统计" value="1" />
            <el-option label="支撑群组统计" value="2" />
            <el-option label="问题处理统计" value="3" />
            <el-option label="创群统计" value="4" />
            <el-option label="回复统计" value="5" />
            <el-option label="回复时长统计" value="6" />
            <el-option label="群组聊天记录" value="7" /> -->
            </el-select>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.thirdPartyAppStatus')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <el-tag :type="scope.row.status | statusFilter">
              {{
                scope.row.status === 1
                  ? $t('index.list.AppOpen')
                  : $t('index.list.AppClose')
              }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="过期时间"
          :show-overflow-tooltip="true"
          min-width="160"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.expired }}</span>
          </template>
        </el-table-column>
        <el-table-column
          label="授权时间"
          :show-overflow-tooltip="true"
          min-width="160"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.grantTime }}</span>
          </template>
        </el-table-column>
        <el-table-column
          label="授权人"
          :show-overflow-tooltip="true"
          min-width="120"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.grantUserName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          label="创建时间"
          :show-overflow-tooltip="true"
          min-width="160"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.gmtCreated }}</span>
          </template>
        </el-table-column>
        <el-table-column
          label="修改时间"
          :show-overflow-tooltip="true"
          min-width="160"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.gmtModified }}</span>
          </template>
        </el-table-column>
        <el-table-column
          fixed="right"
          header-align="center"
          align="center"
          width="290"
          :label="$t('index.operations.operation')"
        >
          <template slot-scope="scope">
            <el-button
              type="primary"
              icon="el-icon-view"
              size="small"
              @click="handleView(scope.row)"
            >
              {{ $t('index.operations.particulars') }}
            </el-button>
            <el-button
              type="primary"
              icon="el-icon-edit"
              size="small"
              @click="handleUpdate(scope.row)"
            >
              {{ $t('index.operations.change') }}
            </el-button>
            <el-button
              type="danger"
              icon="el-icon-delete"
              size="small"
              @click="handleDelete(scope.row)"
            >
              {{ $t('index.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="listQuery.pageNum"
        :limit.sync="listQuery.pageSize"
        @pagination="getList"
      />
    </el-card>

    <!-- 新增/修改三方应用 -->
    <third-party-edit ref="thirdEdit" @success="getList" />
    <third-party-detail ref="thirdDetail" />
  </div>
</template>

<script>
import pagination from '@/components/Pagination'
import thirdPartyEdit from './components/thirdPartyEdit.vue'
import thirdPartyDetail from './components/thirdPartyDetail.vue'
import {
  collaborationList,
  collaborationCreate,
  collaborationUpdate,
  collaborationDelete,
  collaborationDetail
} from '@/api/resource/thirdApp'
export default {
  name: 'ThirdParty',
  filters: {
    statusFilter(status) {
      const statusMap = {
        0: 'danger',
        1: ''
      }
      return statusMap[status]
    }
  },
  components: { pagination, thirdPartyEdit, thirdPartyDetail },
  data() {
    return {
      listLoading: false,
      list: [],
      total: 0,
      listQuery: {
        pageNum: 1,
        pageSize: 10,
        systemName: ''
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    // 获取
    getList() {
      this.listLoading = true
      collaborationList(this.listQuery).then(({ data }) => {
        this.list = data.records
        this.total = data.total
        this.listLoading = false
      })
    },
    // 搜索
    handleFilter() {
      this.listQuery.pageNum = 1
      // const { systemName } = this.listQuery
      // if (systemName) {
      //   // this.list = this.list.filter(item => {
      //   //   return item.systemName.indexOf(systemName) !== -1
      //   // })

      // } else {
      //   this.getList()
      // }
      this.getList(this.listQuery)
    },
    // 重置
    resetQuery() {
      this.listQuery = {
        pageNum: 1,
        pageSize: 10,
        systemName: ''
      }
      this.getList()
    },

    // 新增应用
    handleCreate() {
      this.$refs.thirdEdit.init()
    },
    // 修改应用
    handleUpdate(row) {
      this.$refs.thirdEdit.init(row)
    },
    handleView(row) {
      this.$refs.thirdDetail.init(row)
    },
    // 删除三方应用
    handleDelete({ id }) {
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          collaborationDelete([id]).then(result => {
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
    }
  }
}
</script>

<style scoped lang="scss">
.card {
  height: 100%;
  overflow-y: auto;
}
.filter-container {
  padding-bottom: 10px;
  .filter-item {
    display: inline-block;
    vertical-align: middle;
    margin-bottom: 10px;
  }
}
.edit-input {
  padding-right: 50px;
  width: 360px;
}
.edit-select {
  padding-right: 50px;
  width: 360px;
}

.el-row {
  text-align: left;
  margin-left: 10px;
}
.el-col {
  margin: 8px;
}
</style>
