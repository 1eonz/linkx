<!--
  后台用户管理页面

  功能说明：
  - 用户列表展示（账号、角色、组织、状态）
  - 状态使用 Switch 组件
  - 操作：设置角色、重置密码、删除
  - 支持批量编辑、批量删除

  搜索条件：
  - 账号（姓名）
  - 组织
-->
<template>
  <div class="app-container">
    <!-- 主卡片区域 -->
    <el-card shadow="never" class="card">
      <!-- 搜索栏 -->
      <search-bar
        ref="searchBar"
        search-key="name"
        :search-placeholder="$t('index.messageText.pleaseInputAccount')"
        :show-org="true"
        :org-sync-sign="DEPARTMENT_SYNC_SIGN"
        :org-default-value="orgDefaultValue"
        :actions="searchBarActions"
        :selected-count="selectedCount"
        @search="handleSearch"
        @reset="handleReset"
        @action="handleSearchBarAction"
        @org-change="handleOrgChange"
      />
      
      <!-- 用户列表表格 -->
      <pro-table
        ref="userTable"
        :columns="columns"
        :data="list"
        :loading="listLoading"
        :total="total"
        :page.sync="listQuery.pageNum"
        :limit.sync="listQuery.pageSize"
        row-key="id"
        show-selection
        selection-bar-unit="人"
        selection-bar-item-label="name"
        selection-bar-item-sub="departmentName"
        @cross-selection-change="onCrossSelectionChange"
        @pagination="getList"
      >
        <!-- 账号列 -->
        <template #name="{ row }">
          <span class="user-name">{{ row.name }}</span>
        </template>

        <!-- 角色列 -->
        <template #role="{ row }">
          <template v-if="row.role && row.role.length > 0">
            <!-- <el-tag
              v-for="item in row.role"
              :key="item.id"
              size="small"
              type="info"
              style="margin: 2px 4px 2px 0"
            >
              {{ item.name }}
            </el-tag> -->
          </template>
          <span v-else class="no-data">-</span>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <status-switch
            :value="row.status"
            :disabled="!isGeneralAdmin(row)"
            :loading="row.statusLoading"
            @change="handleStatusChange(row, $event)"
          />
        </template>

        <!-- 操作列 -->
        <template #action="{ row }">
          <el-button
            type="text"
            icon="el-icon-user"
            @click="handleSetRole(row)"
          >
            {{ $t('index.operations.setRole') }}
          </el-button>
          <el-button
            v-if="isGeneralAdmin(row)"
            type="text"
            icon="el-icon-key"
            style="color: #E6A23C"
            @click="handleResetPwd(row)"
          >
            {{ $t('index.messageText.resetPassword') }}
          </el-button>
          <el-button
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
    
    <!-- 设置角色弹窗 -->
    <set-role ref="role" @success="getList" />
    
    <!-- 批量设置角色弹窗 -->
    <set-batch-role ref="batchRole" @success="getList" />
    
    <!-- 重置密码弹窗 -->
    <user-password ref="password" @success="getList" />
  </div>
</template>

<script>
// 引入 ProTable 组件
import ProTable from '@/components/ProTable'
// 引入状态切换组件
import StatusSwitch from '@/components/StatusSwitch'
// 引入 SearchBar 搜索栏组件
import SearchBar from '@/components/SearchBar'
// 引入设置角色弹窗
import setRole from './components/setRole'
// 引入批量设置角色弹窗
import setBatchRole from './components/setBatchRole'
// 引入重置密码弹窗
import userPassword from '@/views/permission/components/userPassword'
// 引入用户相关API
import {
  getPersonList,
  deletePerson,
  updatePersonStatus
} from '@/api/resource/personAdmin'
// 引入工具函数
import { getIsAdmin, getIdCardNum } from '@/utils/auth'
// 引入协同相关API
import { queryUserByIdCard } from '@/api/h5/collaboration'

export default {
  name: 'AdminPerson',
  
  components: {
    ProTable,
    StatusSwitch,
    SearchBar,
    setRole,
    setBatchRole,
    userPassword
  },
  
  data() {
    return {
      // 列配置
      columns: [
        { title: this.$t('index.account'), dataIndex: 'name', minWidth: 120, slot: 'name' },
        { title: this.$t('index.list.role'), dataIndex: 'role', minWidth: 120, slot: 'role' },
        { title: this.$t('index.list.organizationName'), dataIndex: 'departmentName', minWidth: 150, customTooltip: true },
        { title: this.$t('index.list.condition'), minWidth: 100, slot: 'status' },
        { title: this.$t('index.operations.operation'), minWidth: 280, fixed: 'right', slot: 'action' }
      ],
      // 列表数据
      list: [],
      // 总数
      total: 0,
      // 列表加载状态
      listLoading: false,
      // 选中数量（从 ProTable 同步）
      selectedCount: 0,
      
      // 查询参数
      listQuery: {
        pageNum: 1,
        pageSize: 10,
        name: '',           // 账号/姓名
        departmentName: '', // 组织名称
        departmentCode: '', // 组织编码
        privString: ''      // 组织ID
      },
      
      // 是否为管理员
      isAdmin: getIsAdmin(),
      // 身份证号
      idCardNum: getIdCardNum(),
      // 部门编码
      departmentCode: '',
      // 部门ID
      departmentId: '',
      
      // 部门同步标识
      // false: 使用懒加载
      // true: 一次性加载所有部门
      DEPARTMENT_SYNC_SIGN: false
    }
  },
  
  computed: {
    /** 组织默认值（传给 SearchBar） */
    orgDefaultValue() {
      return {
        departmentCode: this.departmentCode,
        departmentName: this.listQuery.departmentName,
        privString: ''
      }
    },
    
    /** 搜索栏右侧操作按钮配置 */
    searchBarActions() {
      return [
        {
          key: 'create',
          label: this.$t('index.operations.newUsers'),
          icon: 'el-icon-plus',
          type: 'primary'
        },
        {
          key: 'batchEdit',
          label: this.$t('index.operations.batchEdit'),
          icon: 'el-icon-edit-outline',
          type: 'warning'
        },
        {
          key: 'batchDelete',
          label: this.$t('index.operations.batchRemove'),
          icon: 'el-icon-delete',
          type: 'danger'
        }
      ]
    }
  },
  
  async mounted() {
    // 获取全局配置
    this.getGlobalConfig()
    
    // 非管理员需要获取用户所属组织
    if (!this.isAdmin) {
      await this.getUserOrgNameByIdCardNum()
    }
    
    // 获取用户列表
    this.getList()
  },
  
  methods: {
    /**
     * 获取全局配置
     */
    async getGlobalConfig() {
      const globalConfig = JSON.parse(localStorage.getItem('globalConfig'))
      if (globalConfig) {
        this.DEPARTMENT_SYNC_SIGN =
          globalConfig.DEPARTMENT_SYNC_SIGN === 'true' ||
          globalConfig.DEPARTMENT_SYNC_SIGN === true
      }
    },
    
    /**
     * 判断是否为普通管理员
     * 只有角色ID为2的用户才能禁用/启用/重置密码
     * @param {Object} row - 用户数据
     * @returns {Boolean}
     */
    isGeneralAdmin(row) {
      // return row?.role?.some(r => r.id === '2' || r.id === 2) || false
      return true
    },
    
    /**
     * 获取用户列表
     */
    getList() {
      this.listLoading = true
      
      getPersonList(this.listQuery)
        .then(({ data }) => {
          // 为每行数据添加状态加载标识
          this.list = data.records.map(item => ({
            ...item,
            statusLoading: false
          }))
          this.total = data.total
          this.listLoading = false
        })
        .catch(() => {
          this.listLoading = false
        })
    },
    
    /**
     * 搜索栏搜索事件
     * @param {Object} params - SearchBar 返回的搜索条件
     */
    handleSearch(params) {
      // 同步搜索条件到 listQuery
      if (params.name !== undefined) this.listQuery.name = params.name
      if (params.departmentCode !== undefined) this.listQuery.departmentCode = params.departmentCode
      if (params.departmentName !== undefined) this.listQuery.departmentName = params.departmentName
      if (params.privString !== undefined) this.listQuery.privString = params.privString
      this.handleFilter()
    },
    
    /**
     * 搜索栏重置事件
     * @param {Object} params - 重置后的空搜索条件
     */
    handleReset(params) {
      this.listQuery.name = ''
      this.listQuery.departmentName = ''
      this.listQuery.departmentCode = ''
      this.listQuery.privString = ''
      this.departmentCode = ''
      // 清空跨页选中
      this.$refs.userTable?.clearAllSelection()
      this.getList()
    },
    
    /**
     * 搜索栏操作按钮事件
     * @param {String} key - 按钮的 key
     */
    handleSearchBarAction(key) {
      switch (key) {
        case 'create':
          this.handleCreate()
          break
        case 'batchEdit':
          this.handleBatchEdit()
          break
        case 'batchDelete':
          this.handleBatchDelete()
          break
        default:
          break
      }
    },
    
    /**
     * 组织选择变化（来自 SearchBar）
     * @param {Object} data - { code, name, id }
     */
    handleOrgChange(data) {
      this.listQuery.departmentCode = data.code
      this.listQuery.departmentName = data.name
      this.listQuery.privString = data.id
    },
    
    /**
     * 搜索
     */
    handleFilter() {
      this.listQuery.pageNum = 1
      // 清空跨页选中
      this.$refs.userTable?.clearAllSelection()
      this.getList()
    },
    
    /**
     * 跨页选中变化回调
     * @param {Array} selection - 全量选中数组
     */
    onCrossSelectionChange(selection) {
      this.selectedCount = selection.length
    },
    
    /**
     * 状态切换处理
     * @param {Object} row - 用户数据
     * @param {Number} status - 新状态值
     */
    handleStatusChange(row, status) {
      // 设置加载状态
      this.$set(row, 'statusLoading', true)
      
      const params = {
        id: row.id,
        status
      }
      
      updatePersonStatus(params)
        .then(result => {
          if (result.code === 0) {
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
        })
        .catch(() => {
          this.$message.error(this.$t('index.messageText.operationFailed'))
        })
        .finally(() => {
          this.$set(row, 'statusLoading', false)
        })
    },
    
    /**
     * 设置角色
     * @param {Object} row - 用户数据
     */
    handleSetRole(row) {
      this.$refs.role.init(row, this.list)
    },
    
    /**
     * 新增用户
     */
    handleCreate() {
      // this.$refs.editPerson.init()
      this.$refs.role.init(null, this.list)
    },
    
    /**
     * 重置密码
     * @param {Object} row - 用户数据
     */
    handleResetPwd(row) {
      this.$refs.password.setData(row, false)
    },
    
    /**
     * 删除单个用户
     * @param {Object} row - 用户数据
     */
    handleDelete(row) {
      this.deleteUsers([row.id], false)
    },
    
    /**
     * 批量编辑
     */
    handleBatchEdit() {
      const selection = this.$refs.userTable?.getMultipleSelection() || []
      if (selection.length < 1) {
        this.$message({
          message: this.$t('index.messageText.pleaseCheckData'),
          type: 'warning'
        })
        return
      }
      
      const array = selection.map(person => person)
      this.$refs.batchRole.init(array, this.list)
    },
    
    /**
     * 批量删除
     */
    handleBatchDelete() {
      const selection = this.$refs.userTable?.getMultipleSelection() || []
      if (selection.length < 1) {
        this.$message({
          message: this.$t('index.messageText.pleaseCheckData'),
          type: 'warning'
        })
        return
      }
      
      const ids = selection.map(person => person.id)
      this.deleteUsers(ids, true)
    },
    
    /**
     * 删除用户
     * @param {Array} ids - 用户ID数组
     * @param {Boolean} isBatch - 是否批量删除
     */
    deleteUsers(ids, isBatch = false) {
      const confirmMsg = isBatch
        ? this.$t('index.operations.affirmPermanentlyDeleted')
        : this.$t('index.operations.affirmDeletedAuth')
      
      this.$confirm(confirmMsg, {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'warning'
      }).then(() => {
        deletePerson({ ids: ids.join(',') })
          .then(result => {
            if (result.code === 0) {
              // 从选中池中移除已删除的用户
              ids.forEach(id => {
                const selectedMap = this.$refs.userTable?.getSelectedMap() || {}
                const item = selectedMap[id]
                if (item) {
                  this.$refs.userTable?.removeSelection(item)
                }
              })
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
          })
      }).catch(() => {})
    },
    
    /**
     * 根据身份证号获取用户所属组织
     */
    async getUserOrgNameByIdCardNum() {
      try {
        const userRes = await queryUserByIdCard({ idCard: this.idCardNum })
        if (
          userRes &&
          userRes.data &&
          userRes.data.userDepartments &&
          userRes.data.userDepartments.length > 0
        ) {
          this.departmentCode = userRes.data.userDepartments[0].departmentCode
          this.departmentId = userRes.data.userDepartments[0].departmentId
        }
      } catch (error) {
        console.log(error)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
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

// 用户名称样式
.user-name {
  font-weight: 500;
  color: #303133;
}

// 无数据样式
.no-data {
  color: #C0C4CC;
  font-size: 13px;
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
