<template>
  <!-- 绑定用户 -->
  <el-dialog
    title="绑定用户"
    :visible.sync="dialogVisible"
    :close-on-click-modal="false"
    width="1000px"
    @close="closeDialog"
  >
    <el-form ref="roleForm" label-width="100px" class="roleForm">
      <el-form-item label="角色名称" prop="name">
        <span>{{ roleName }}</span>
      </el-form-item>
      <el-form-item label="选择用户" prop="name" class="data-auth">
        <div class="filter-container">
          <el-input
            v-model="listQuery.name"
            placeholder="姓名"
            style="width: 150px;"
            class="filter-item"
            clearable
            @keyup.enter.native="handleFilter"
          />
          <el-input
            v-model="listQuery.idCard"
            placeholder="身份证号"
            style="width: 150px;"
            class="filter-item"
            clearable
            @keyup.enter.native="handleFilter"
          />
          <SelectTree
            v-if="DEPARTMENT_SYNC_SIGN"
            v-model="listQuery.departmentName"
            :is-init-value="true"
            :value="departmentCode"
            placeholder="组织名称"
            @clear-val="cleanOrganizationInput"
            @current-change="organizationCurrentChange"
          />
          <select-tree-lazy
            v-else
            v-model="listQuery.departmentName"
            class="filter-item"
            style="width: 150px;"
            :is-init-value="true"
            :department-code="departmentCode"
            placeholder="组织名称"
            @clear-val="cleanOrganizationInput"
            @current-change="organizationCurrentChange"
          />
          <el-button
            class="filter-item"
            type="primary"
            icon="el-icon-search"
            @click="handleFilter"
          >
            搜索
          </el-button>
          <el-button
            class="filter-item"
            style="margin-left: 10px;"
            type="primary"
            icon="el-icon-refresh"
            @click="handleReset"
          >
            重置
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
          height="370px"
          @selection-change="handleSelection"
        >
          <el-table-column type="selection" align="center" width="40" />
          <el-table-column
            label="姓名"
            :show-overflow-tooltip="true"
            min-width="60"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.name }}</span>
            </template>
          </el-table-column>

          <el-table-column
            label="身份证号"
            :show-overflow-tooltip="true"
            min-width="80"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.idCard }}</span>
            </template>
          </el-table-column>

          <el-table-column
            label="组织名称"
            :show-overflow-tooltip="true"
            min-width="80"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.departmentName }}</span>
            </template>
          </el-table-column>

          <el-table-column
            label="组织编码"
            :show-overflow-tooltip="true"
            min-width="80"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.departmentCode }}</span>
            </template>
          </el-table-column>

          <el-table-column
            label="直接上级"
            :show-overflow-tooltip="true"
            min-width="80"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.directLeaderName }}</span>
            </template>
          </el-table-column>

          <el-table-column
            label="上级ID"
            :show-overflow-tooltip="true"
            min-width="80"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.directLeaderId }}</span>
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
      </el-form-item>
    </el-form>

    <div slot="footer" class="dialog-footer">
      <el-button @click="closeDialog">
        取消
      </el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleConfirm">
        确定
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import { getPersonList } from '@/api/resource/person'
import { setBatchRole } from '@/api/resource/person'
import SelectTree from '@/components/SelectTree'
import selectTreeLazy from '@/components/SelectTreeLazy'
import pagination from '@/components/Pagination'

export default {
  name: 'BindUser',
  components: { SelectTree, selectTreeLazy, pagination },
  data() {
    return {
      dialogVisible: false,
      listLoading: false,
      submitLoading: false,
      list: [],
      total: 0,
      multipleSelection: [],
      roleId: '', // 当前角色ID
      roleName: '', // 当前角色名称
      departmentCode: '',
      listQuery: {
        pageNum: 1,
        pageSize: 10,
        name: '',
        idCard: '',
        departmentName: '',
        departmentCode: ''
      },
      DEPARTMENT_SYNC_SIGN: false
    }
  },
  created() {
    this.getGlobalConfig()
  },
  methods: {
    // 获取全局参数
    async getGlobalConfig() {
      const globalConfig = JSON.parse(localStorage.getItem('globalConfig'))
      if (globalConfig) {
        this.DEPARTMENT_SYNC_SIGN =
          globalConfig.DEPARTMENT_SYNC_SIGN === 'true' ||
          globalConfig.DEPARTMENT_SYNC_SIGN === true
      }
    },

    /**
     * 初始化弹窗
     * @param {Object} role - 角色信息 { id, name }
     */
    init(role) {
      this.roleId = role.id
      this.roleName = role.name
      this.resetQuery()
      this.getList()
      this.dialogVisible = true
    },

    // 获取用户列表
    getList() {
      this.listLoading = true
      const params = { ...this.listQuery }

      // 清理空参数
      Object.keys(params).forEach(key => {
        if (
          params[key] === '' ||
          params[key] === null ||
          params[key] === undefined
        ) {
          delete params[key]
        }
      })

      getPersonList(params)
        .then(res => {
          if (res.code === 0 && res.data) {
            this.list = res.data.records || []
            this.total = res.data.total || 0
          } else {
            this.$message.error(res.msg || '获取用户列表失败')
          }
        })
        .catch(error => {
          console.error('获取用户列表失败:', error)
          this.$message.error('获取用户列表失败')
        })
        .finally(() => {
          this.listLoading = false
        })
    },

    // 搜索
    handleFilter() {
      this.listQuery.pageNum = 1
      this.getList()
    },

    // 重置
    handleReset() {
      this.resetQuery()
      this.getList()
    },

    // 重置查询条件
    resetQuery() {
      this.listQuery = {
        pageNum: 1,
        pageSize: 10,
        name: '',
        idCard: '',
        departmentName: '',
        departmentCode: ''
      }
      this.departmentCode = ''
    },

    // 表格勾选变化
    handleSelection(selection) {
      this.multipleSelection = selection
    },

    // 获取角色名称
    getRoleName(row) {
      return row?.role?.name || ''
    },

    // 组织选择变化
    organizationCurrentChange(data) {
      this.listQuery.departmentCode = data.code
      this.listQuery.departmentName = data.name
      this.departmentCode = data.code
    },

    // 清空组织选择
    cleanOrganizationInput() {
      this.listQuery.departmentCode = ''
      this.listQuery.departmentName = ''
      this.departmentCode = ''
    },

    // 确认绑定
    async handleConfirm() {
      if (this.multipleSelection.length === 0) {
        this.$message.warning('请选择要绑定的用户')
        return
      }

      // 获取选中的用户ID列表
      const userIds = this.multipleSelection.map(item => item.id)

      this.$confirm(
        `确定将选中的 ${userIds.length} 个用户绑定到角色【${
          this.roleName
        }】吗？`,
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
        .then(() => {
          this.submitLoading = true

          const data = {
            roleId: this.roleId,
            userIds: userIds
          }

          setBatchRole(data)
            .then(res => {
              if (res.code === 0) {
                this.$message.success('绑定成功')
                this.$emit('success')
                this.closeDialog()
              } else {
                this.$message.error(res.msg || '绑定失败')
              }
            })
            .catch(error => {
              console.error('绑定用户失败:', error)
              this.$message.error('绑定用户失败')
            })
            .finally(() => {
              this.submitLoading = false
            })
        })
        .catch(() => {
          // 取消操作
        })
    },

    // 关闭弹窗
    closeDialog() {
      this.dialogVisible = false
      this.resetQuery()
      this.multipleSelection = []
    }
  }
}
</script>

<style lang="scss" scoped>
.filter-container {
  margin-bottom: 20px;
}

.filter-item {
  margin-right: 10px;
  margin-bottom: 0;
}

.dialog-footer {
  text-align: right;
}
.roleForm {
  .data-auth {
    display: flex;
    flex-direction: column;
    ::v-deep .el-form-item__content {
      margin-left: 30px !important;
    }
  }
}
::v-deep.pagination-container {
  padding: 10px 20px !important;
}
::v-deep.el-form-item {
  margin-bottom: 0 !important;
}
.pagination-container {
  padding: 16px 16px 0;
}
</style>
