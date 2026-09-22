<!--
  后台角色管理页面

  功能说明：
  - 角色列表展示（角色名称、状态）
  - 状态使用 Switch 组件，支持直接切换
  - 操作：编辑、删除、设置用户

  特殊角色处理：
  - id=2 或 id=6 的角色为系统内置角色，不可删除/禁用
-->
<template>
  <div class="app-container">
    <!-- 主卡片区域 -->
    <el-card shadow="never" class="card">
      <!-- 搜索栏 -->
      <search-bar
        ref="searchBar"
        search-key="name"
        :search-placeholder="$t('index.list.roleName')"
        :actions="searchBarActions"
        @search="handleSearch"
        @reset="handleReset"
        @action="handleSearchBarAction"
      />

      <!-- 角色列表表格 -->
      <pro-table
        ref="roleTable"
        :columns="columns"
        :data="list"
        :loading="listLoading"
        :total="total"
        :page.sync="listQuery.pageNo"
        :limit.sync="listQuery.pageSize"
        row-key="id"
        @pagination="getList"
      >
        <!-- 角色名称列 -->
        <template #name="{ row }">
          <span class="role-name">{{ row.name }}</span>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <status-switch
            :value="row.status"
            :disabled="!isOperable(row)"
            :loading="row.statusLoading"
            @change="handleStatusChange(row, $event)"
          />
        </template>

        <!-- 操作列 -->
        <template #action="{ row }">
          <el-button
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(row)"
          >
            {{ $t('index.operations.change') }}
          </el-button>
          <el-button
            type="text"
            icon="el-icon-user"
            style="color: #67C23A"
            @click="handleSetUsers(row)"
          >
            {{ $t('index.messageText.setUser') }}
          </el-button>
          <el-button
            v-if="isOperable(row)"
            type="text"
            icon="el-icon-delete"
            style="color: #F56C6C"
            @click="handleDelete(row)"
          >
            {{ $t('index.delete') }}
          </el-button>
        </template>
      </pro-table>
    </el-card>

    <!-- 新增/修改角色弹窗 -->
    <edit-role ref="editRole" @success="getList" />

    <!-- 设置用户弹窗 -->
    <user-select-dialog
      :visible.sync="userDialogVisible"
      :role-id="currentRole.id"
      :role-name="currentRole.name"
      @confirm="handleUserConfirm"
    />
  </div>
</template>

<script>
// 引入 ProTable 组件
import ProTable from '@/components/ProTable'
// 引入 SearchBar 搜索栏组件
import SearchBar from '@/components/SearchBar'
// 引入角色编辑弹窗组件
import editRole from './components/editRole.vue'
// 引入状态切换组件
import StatusSwitch from '@/components/StatusSwitch'
// 引入用户选择弹窗组件
import UserSelectDialog from '@/components/UserSelectDialog'
// 引入角色相关API
import { getRoleList, updateRole, deleteRole } from '@/api/resource/roleAdmin'
// 引入用户绑定API
import { bindUsersToRole } from '@/api/resource/user'
// 引入应用配置
import { APPLICATION_ID } from '@/appConfig'

export default {
  name: 'AdminRole',

  components: {
    ProTable,
    SearchBar,
    editRole,
    StatusSwitch,
    UserSelectDialog
  },

  data() {
    return {
      // 列配置
      columns: [
        { title: this.$t('index.list.roleName'), dataIndex: 'name', minWidth: 120, slot: 'name' },
        { title: this.$t('index.list.condition'), minWidth: 100, slot: 'status' },
        { title: this.$t('index.operations.operation'), minWidth: 240, fixed: 'right', slot: 'action' }
      ],
      // 列表加载状态
      listLoading: false,
      // 角色列表数据
      list: [],
      // 总数
      total: 0,
      // 查询参数
      listQuery: {
        applicationId: APPLICATION_ID.ADMIN,
        name: '',
        pageSize: 10,
        pageNo: 1
      },
      // 用户选择弹窗显示状态
      userDialogVisible: false,
      // 当前操作的角色
      currentRole: {
        id: '',
        name: ''
      }
    }
  },

  computed: {
    /** SearchBar 操作按钮配置 */
    searchBarActions() {
      return [
        { key: 'create', label: this.$t('index.operations.Added'), icon: 'el-icon-plus', type: 'primary' }
      ]
    }
  },

  created() {
    // 页面创建时获取列表
    this.getList()
  },

  methods: {
    /**
     * 判断角色是否可操作
     * 系统内置角色（id=2 或 id=6）不可删除/禁用
     * @param {Object} row - 角色数据
     * @returns {Boolean} 是否可操作
     */
    isOperable(row) {
      return +row.id !== 2 && +row.id !== 6
    },

    /**
     * 获取角色列表
     */
    getList() {
      this.listLoading = true
      getRoleList(this.listQuery).then(({ data }) => {
        // 为每行数据添加状态加载标识
        this.list = data.records.map(item => ({
          ...item,
          statusLoading: false
        }))
        this.total = data.total
        this.listLoading = false
      }).catch(() => {
        this.listLoading = false
      })
    },

    /**
     * SearchBar 搜索事件
     */
    handleSearch(params) {
      this.listQuery.name = params.name || ''
      this.listQuery.pageNo = 1
      this.getList()
    },

    /**
     * SearchBar 重置事件
     */
    handleReset() {
      this.listQuery.name = ''
      this.listQuery.pageNo = 1
      this.getList()
    },

    /**
     * SearchBar 操作按钮事件
     */
    handleSearchBarAction(key) {
      if (key === 'create') {
        this.handleCreate()
      }
    },

    /**
     * 新增角色
     */
    handleCreate() {
      this.$refs.editRole.init()
    },

    /**
     * 编辑角色
     * @param {Object} row - 角色数据
     */
    handleUpdate(row) {
      this.$refs.editRole.init(row)
    },

    /**
     * 删除角色
     * @param {Object} row - 角色数据
     */
    handleDelete({ id, name }) {
      const confirmMsg = this.$t('index.operations.affirmDeleted') + `: "${name}"`

      this.$confirm(confirmMsg, {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'warning'
      }).then(() => {
        // 调用删除接口
        deleteRole([id]).then(result => {
          if (result.code === 0) {
            this.$message({
              message: this.$t('index.statusTitle.successfullyDelete'),
              type: 'success'
            })
            this.getList()
          } else {
            this.$message({
              message: result.msg,
              type: 'error'
            })
          }
        }).catch(() => {
          this.$message.error(this.$t('index.messageText.operationFailed'))
        })
      }).catch(() => {})
    },

    /**
     * 状态切换处理
     * @param {Object} row - 角色数据
     * @param {Number} status - 新状态值 (0=启用, 1=禁用)
     */
    handleStatusChange(row, status) {
      // 设置加载状态
      this.$set(row, 'statusLoading', true)

      const param = {
        id: row.id,
        status
      }

      updateRole(param).then(result => {
        if (result.code === 0) {
          // 更新成功，修改本地状态
          row.status = status
          this.$message({
            message: status === 0 ? this.$t('index.messageText.enableSuccess') : this.$t('index.messageText.disableSuccess'),
            type: 'success'
          })
        } else {
          this.$message({
            message: result.msg,
            type: 'error'
          })
        }
      }).catch(() => {
        this.$message({
          message: this.$t('index.messageText.operationFailed'),
          type: 'error'
        })
      }).finally(() => {
        // 取消加载状态
        this.$set(row, 'statusLoading', false)
      })
    },

    /**
     * 打开设置用户弹窗
     * @param {Object} row - 角色数据
     */
    handleSetUsers(row) {
      this.currentRole = {
        id: row.id,
        name: row.name
      }
      this.userDialogVisible = true
    },

    /**
     * 确认设置用户
     * @param {Array} users - 选中的用户列表
     */
    handleUserConfirm(users) {
      const userIds = users.map(user => user.id)

      bindUsersToRole({
        roleId: this.currentRole.id,
        userIds
      }).then(result => {
        if (result.code === 0) {
          this.$message({
            message: result.msg || this.$t('index.messageText.setUserSuccess'),
            type: 'success'
          })
          this.getList()
        } else {
          this.$message({
            message: result.msg,
            type: 'error'
          })
        }
      }).catch(() => {
        this.$message.error(this.$t('index.messageText.operationFailed'))
      })
    }
  }
}
</script>

<style scoped lang="scss">
.app-container {
  padding: 20px;
  background: #f0f2f5;
  min-height: calc(100vh - 84px);
}

// 卡片样式
.card {
  height: 100%;
  overflow-y: auto;
  border-radius: 8px;
  border: none;
  box-shadow: 0 2px 8px 0 rgba(0, 0, 0, 0.06);

  ::v-deep .el-card__body {
    padding: 20px 24px;
  }
}

// 角色名称样式
.role-name {
  font-weight: 500;
  color: #303133;
}

// 表格操作按钮样式优化
::v-deep .el-button--text {
  padding: 4px 8px;
  font-size: 13px;

  + .el-button--text {
    margin-left: 4px;
  }
}
</style>
