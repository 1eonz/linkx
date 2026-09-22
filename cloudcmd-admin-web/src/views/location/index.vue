<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container">
        <!-- <el-input
            v-model="listQuery.deptName"
            :placeholder="$t('index.list.deptName')"
            style="width: 200px;"
            class="filter-item"
            clearable
            @keyup.enter.native="handleFilter"
          /> -->
        <!-- <el-select
          ref="selectRef"
          v-model="listQuery.deptName"
          :clearable="clearable"
          :placeholder="$t('index.list.deptName')"
        >
          <el-option
            :value="listQuery.deptName"
            style="height: auto;padding: 0;"
          >
            <el-tree
              ref="tree"
              :data="orgList"
              :props="defaultProps"
              :node-key="nodeKey"
              :default-expand-all="defaultExpandAll"
              :highlight-current="true"
              @node-click="handleNodeClick"
            >
              <template #default="{ node }">
                <span style="font-weight: normal">{{ node.label }}</span>
              </template>
            </el-tree>
          </el-option>
        </el-select> -->
        <select-tree-lazy
            v-model="listQuery.deptName"
            :placeholder="$t('index.list.deptName')"
             @clear-val="cleanOrg"
            @current-change="handleNodeClick"
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
          v-if="hasPerm('/admin/role/create')"
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
          :label="$t('index.list.deptName')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.departmentName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.deptLocation')"
          :show-overflow-tooltip="true"
          min-width="60"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.location }}</span>
            <!-- <el-tag :type="scope.row.status | statusFilter">
                {{
                  scope.row.status === 0
                    ? $t('index.list.normal')
                    : $t('index.list.forbidden')
                }}
              </el-tag> -->
          </template>
        </el-table-column>
        <el-table-column
          fixed="right"
          header-align="center"
          align="center"
          min-width="200"
          :label="$t('index.operations.operation')"
        >
          <template slot-scope="scope">
            <el-button
              v-if="hasPerm('/admin/role/update')"
              type="primary"
              icon="el-icon-edit"
              size="small"
              @click="handleUpdate(scope.row)"
            >
              {{ $t('index.operations.redact') }}
            </el-button>
            <el-button
              v-if="hasPerm('/admin/role/delete')"
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

    <!-- 新增/修改位置 -->
    <edit-location ref="editLocation" @success="getList" />
  </div>
</template>

<script>
import pagination from '@/components/Pagination'
import editLocation from './components/editLocation.vue'
import { getDepartmentList } from '@/api/resource/organization'
import { getLocationList, deleteLocation } from '@/api/resource/location'
import selectTreeLazy from '@/components/SelectTreeLazy'
export default {
  name: 'Location',
  filters: {
    statusFilter(status) {
      const statusMap = {
        0: 'success',
        1: 'danger'
      }
      return statusMap[status]
    }
  },
  components: { pagination, editLocation, selectTreeLazy },
  data() {
    return {
      listLoading: false,
      list: [],
      orgList: [],
      defaultProps: {
        children: 'children',
        label: 'name'
      },
      nodeKey: 'id',
      clearable: true,
      defaultExpandAll: false,
      total: 0,
      listQuery: {
        deptName: '',
        pageSize: 10,
        pageNum: 1
      }
    }
  },
  created() {
    this.getList()
    getDepartmentList().then(res => {
      if (res.code === 0) {
        this.orgList = res.data
      }
    })
  },
  methods: {
    // 获取
    getList() {
      this.listLoading = true
      getLocationList(this.listQuery).then(data => {
        this.list = data.list
        this.total = data.total
        this.listLoading = false
      })
    },
    handleNodeClick(data) {
      this.listQuery.deptName = data.name
      // this.$refs.selectRef.blur()
    },
    cleanOrg() {
      this.listQuery.deptName = ''
    },

    // 搜索
    handleFilter() {
      this.listQuery.pageNum = 1
      // const { deptName } = this.listQuery
      // if (deptName) {
      //   this.list = this.list.filter(item => {
      //     return item.departmentName.indexOf(deptName) !== -1
      //   })
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
        deptName: ''
      }
      // const nodesMap = this.$refs.tree.store.nodesMap
      // for (const key in nodesMap) {
      //   nodesMap[key].expanded = false
      // }
      this.getList()
    },
    // 新增位置
    handleCreate() {
      this.$refs.editLocation.init()
    },
    // 修改位置
    handleUpdate(row) {
      this.$refs.editLocation.init(row)
    },
    // 删除位置
    handleDelete({ id }) {
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          deleteLocation([id]).then(result => {
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
