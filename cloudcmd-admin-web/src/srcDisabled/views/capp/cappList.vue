<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container">
        <el-button
          class="filter-item"
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
        style="width: 100%;margin-top: 20px;"
        @selection-change="handleSelection"
      >
        <el-table-column type="selection" align="center" width="40" />
        <el-table-column
          :label="$t('index.list.applicationName')"
          :show-overflow-tooltip="true"
          min-width="60"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.applicationType')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{
              scope.row.applicationType === 0
                ? 'apk'
                : scope.row.applicationType === 1
                ? 'H5'
                : 'local'
            }}</span>
          </template>
        </el-table-column>

        <el-table-column
          :label="$t('index.list.isSystemApplication')"
          :show-overflow-tooltip="true"
          min-width="40"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{
              scope.row.isSystemApplication
                ? $t('index.operations.no')
                : $t('index.operations.yes')
            }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.jumpLink')"
          :show-overflow-tooltip="true"
          min-width="60"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.url }}</span>
          </template>
        </el-table-column>

        <!-- <el-table-column
          :label="$t('index.list.favoriteStatus')"
          :show-overflow-tooltip="true"
          min-width="40"
          align="center"
        >
          <template slot-scope="scope">
            <el-tag v-if="scope.row.favorite" type="success">{{
              $t('index.list.collection')
            }}</el-tag>
            <el-tag v-else type="info">{{
              $t('index.operations.unCollect')
            }}</el-tag>
          </template>
        </el-table-column> -->
        <el-table-column
          :label="$t('index.list.enabledStatus')"
          :show-overflow-tooltip="true"
          min-width="40"
          align="center"
        >
          <template slot-scope="scope">
            <el-tag v-if="scope.row.status" type="success">{{
              $t('index.operations.enabled')
            }}</el-tag>
            <el-tag v-else type="danger">{{
              $t('index.list.forbidden')
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.face')"
          min-width="40"
          align="center"
        >
          <template slot-scope="scope">
            <authImg
              v-if="scope.row.imageUrl"
              class="head-shot"
              :auth-src="scope.row.imageUrl"
            />
          </template>
        </el-table-column>
        <el-table-column
          fixed="right"
          header-align="center"
          align="center"
          :label="$t('index.operations.operation')"
        >
          <template slot-scope="{ row }">
            <el-button
              v-show="row.isSystemApplication === 1"
              type="primary"
              icon="el-icon-edit"
              size="small"
              @click="handleUpdate(row)"
            >
              {{ $t('index.operations.change') }}
            </el-button>
            <el-button
              v-show="row.isSystemApplication === 1"
              type="danger"
              icon="el-icon-delete"
              size="small"
              @click="handleDelete(row)"
            >
              {{ $t('index.delete') }}
            </el-button>
            <!-- <el-button
              :type="row.favorite === 1 ? 'primary' : 'success'"
              icon="el-icon-star-on"
              size="small"
              @click="handleFavorite(row)"
            >
              {{
                row.favorite === 1
                  ? $t('index.operations.unCollect')
                  : $t('index.operations.collect')
              }}
            </el-button> -->
            <el-button
              :type="row.status === 1 ? 'danger' : 'success'"
              icon="el-icon-goods"
              size="small"
              @click="handleStatus(row)"
            >
              {{
                row.status === 1
                  ? $t('index.list.forbidden')
                  : $t('index.operations.enabled')
              }}
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
    <capp-set ref="editCapp" @success="handleGetList" />
  </div>
</template>

<script>
import CappSet from './cappSet.vue'
import {
  getCappList,
  deleteCapp,
  updateSystemApp,
  updateFavorite
} from '@/api/resource/capp'
import authImg from '@/components/AuthImg'

export default {
  name: 'CappList',
  components: { CappSet, authImg },
  data() {
    return {
      total: 0,
      pageNum: 1,
      pageSize: 10,
      listLoading: false,
      list: [],
      multipleSelection: []
    }
  },
  mounted() {
    this.getCappListFunc()
  },
  methods: {
    async getCappListFunc() {
      this.personListLoading = true
      await getCappList({ page: this.pageNum, limit: this.pageSize }).then(
        res => {
          const { data, code } = res
          if (code === 0) {
            this.total = parseInt(data.total)
            this.list = data.records
          }
        }
      )
    },
    // 收藏
    async handleFavorite(row) {
      const { id, favorite } = row
      const params = {
        id,
        favorite: favorite === 0 ? 1 : 0
      }
      const { code, msg } = await updateFavorite(params)
      this.$message({
        message: msg,
        type: code === 0 ? 'success' : 'error'
      })
      this.getCappListFunc()
    },

    // 禁用/启用
    async handleStatus(row) {
      const { id, status } = row
      const params = {
        id,
        status: status === 0 ? 1 : 0
      }
      const { code, msg } = await updateSystemApp(params)
      this.$message({
        message: msg,
        type: code === 0 ? 'success' : 'error'
      })
      this.getCappListFunc()
    },

    // 修改
    handleUpdate(row) {
      this.$refs.editCapp?.modify(row)
    },
    // 点击删除
    handleDelete(data) {
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          deleteCapp({ id: data.id }).then(result => {
            if (result.code === 0) {
              this.$message({
                message: this.$t('index.statusTitle.successfullyDelete'),
                type: 'success'
              })
              this.pageNum = 1
              this.getCappListFunc()
            } else {
              this.$message({
                message: result.msg,
                type: 'error'
              })
            }
          })
        })
        .catch(() => {})
    },
    getList({ page, limit }) {
      this.pageNum = page
      this.pageSize = limit
      this.getCappListFunc()
    },
    handleSelection(val) {
      this.multipleSelection = val
    },
    // 新增
    handleCreate() {
      this.$refs.editCapp?.add()
    },
    handleGetList() {
      this.getCappListFunc()
    }
  }
}
</script>

<style lang="scss" scoped>
.head-shot {
  display: inline-block;
  height: 70px;
  width: 70px;
}
</style>
