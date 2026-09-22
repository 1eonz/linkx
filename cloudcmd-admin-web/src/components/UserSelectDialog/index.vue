<!--
  用户多选弹窗组件 (UserSelectDialog)

  功能说明：
  - 用于为角色设置用户
  - 支持用户搜索、分页
  - 支持多选，切换分页/搜索时保持选中状态
  - 显示已选人数提示

  Props:
  - visible: 弹窗是否显示
  - roleId: 角色ID（用于标题显示）
  - roleName: 角色名称（用于标题显示）
  - selectedUserIds: 已选用户ID数组（用于初始化选中状态）

  Events:
  - update:visible: 更新弹窗显示状态
  - confirm: 确认选择，返回选中的用户列表

  使用示例：
  <user-select-dialog
    :visible.sync="dialogVisible"
    :role-id="currentRole.id"
    :role-name="currentRole.name"
    :selected-user-ids="selectedIds"
    @confirm="handleConfirm"
  />
-->
<template>
  <el-dialog
    :visible.sync="dialogVisible"
    :title="dialogTitle"
    :close-on-click-modal="false"
    width="800px"
    custom-class="user-select-dialog"
    @close="handleClose"
  >
    <!-- 搜索栏 -->
    <search-bar
      ref="searchBar"
      search-key="name"
      :search-placeholder="$t('index.messageText.pleaseInputAccount')"
      :search-width="'250px'"
      @search="handleSearch"
      @reset="handleSearchReset"
    />

    <!-- 用户列表表格 -->
    <pro-table
      ref="userTable"
      :columns="columns"
      :data="userList"
      :loading="loading"
      :total="pagination.total"
      :page.sync="pagination.pageNum"
      :limit.sync="pagination.pageSize"
      row-key="id"
      show-selection
      selection-bar-unit="人"
      selection-bar-item-label="name"
      selection-bar-item-sub="departmentName"
      height="400px"
      @cross-selection-change="onCrossSelectionChange"
      @pagination="onPagination"
    >
      <!-- 状态列 -->
      <template #status="{ row }">
        <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
          {{ row.status === 0 ? $t('index.list.normal') : $t('index.list.forbidden') }}
        </el-tag>
      </template>
    </pro-table>

    <!-- 底部操作按钮 -->
    <div slot="footer" class="dialog-footer">
      <el-button @click="handleClose">
        {{ $t('index.cancel') }}
      </el-button>
      <el-button type="primary" @click="handleConfirm">
        {{ $t('index.determine') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import ProTable from '@/components/ProTable'
import SearchBar from '@/components/SearchBar'
import { getUserListByPage } from '@/api/resource/user'

export default {
  name: 'UserSelectDialog',

  components: { ProTable, SearchBar },

  props: {
    /**
     * 弹窗是否显示
     */
    visible: {
      type: Boolean,
      default: false
    },

    /**
     * 角色ID
     */
    roleId: {
      type: [String, Number],
      default: ''
    },

    /**
     * 角色名称
     */
    roleName: {
      type: String,
      default: ''
    },

    /**
     * 已选用户ID数组（用于初始化）
     */
    selectedUserIds: {
      type: Array,
      default: () => []
    }
  },

  data() {
    return {
      // 搜索关键词（由 SearchBar 驱动）
      searchName: '',
      // 加载状态
      loading: false,
      // 用户列表数据
      userList: [],
      // 分页参数
      pagination: {
        pageNum: 1,
        pageSize: 10,
        total: 0
      },
      // 跨页选中数量
      selectedCount: 0
    }
  },

  computed: {
    /** 列配置 */
    columns() {
      return [
        { title: this.$t('index.list.userName'), dataIndex: 'name', minWidth: 120, customTooltip: true },
        { title: this.$t('index.account'), dataIndex: 'account', minWidth: 120, customTooltip: true },
        { title: this.$t('index.list.organizationName'), dataIndex: 'departmentName', minWidth: 150, customTooltip: true },
        { title: this.$t('index.list.condition'), minWidth: 80, slot: 'status' }
      ]
    },

    /**
     * 弹窗标题
     */
    dialogTitle() {
      const count = this.selectedCount
      return `${this.$t('index.messageText.setUser')} - ${this.roleName || this.$t('index.list.role')} ${count > 0 ? `(${this.$t('index.proTable.selectedText')}${count}${this.$t('index.proTable.selectedUnit')})` : ''}`
    },

    /**
     * 双向绑定弹窗显示状态
     */
    dialogVisible: {
      get() {
        return this.visible
      },
      set(val) {
        this.$emit('update:visible', val)
      }
    }
  },

  watch: {
    /**
     * 监听弹窗显示状态
     * 打开时初始化数据
     */
    visible(val) {
      if (val) {
        this.initDialog()
      }
    }
  },

  methods: {
    /**
     * 初始化弹窗
     */
    async initDialog() {
      // 重置搜索条件
      this.searchName = ''
      this.pagination.pageNum = 1

      // 加载用户列表
      await this.loadUserList()
    },

    /**
     * 加载用户列表
     */
    async loadUserList() {
      this.loading = true

      try {
        const params = {
          name: this.searchName,
          pageNum: this.pagination.pageNum,
          pageSize: this.pagination.pageSize
        }

        // 调用API获取用户列表
        const { code, data } = await getUserListByPage(params)

        if (code === 0) {
          this.userList = data.records || []
          this.pagination.total = data.total || 0
        }
      } catch (error) {
        console.error('加载用户列表失败:', error)
        this.$message.error(this.$t('index.messageText.operationFailed'))
      } finally {
        this.loading = false
      }
    },

    /**
     * SearchBar 搜索事件
     */
    async handleSearch(params) {
      this.searchName = params.name || ''
      this.pagination.pageNum = 1
      await this.loadUserList()
    },

    /**
     * SearchBar 重置事件
     */
    async handleSearchReset() {
      this.searchName = ''
      this.pagination.pageNum = 1
      await this.loadUserList()
    },

    /**
     * ProTable 跨页选中变化
     */
    onCrossSelectionChange(selection) {
      this.selectedCount = selection.length
    },

    /**
     * ProTable 分页变化
     */
    onPagination() {
      this.loadUserList()
    },

    /**
     * 处理确认按钮
     */
    handleConfirm() {
      // 从 ProTable 获取全量选中数据
      const selectedList = this.$refs.userTable ? this.$refs.userTable.getMultipleSelection() : []

      // 触发确认事件
      this.$emit('confirm', selectedList)

      // 关闭弹窗
      this.handleClose()
    },

    /**
     * 处理关闭弹窗
     */
    handleClose() {
      this.dialogVisible = false
      this.searchName = ''
      this.userList = []
      this.selectedCount = 0
      // 清空 ProTable 选中状态
      this.$nextTick(() => {
        if (this.$refs.userTable) {
          this.$refs.userTable.clearAllSelection()
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>

<!-- 弹窗垂直居中（scoped 无法穿透 el-dialog 遮罩层） -->
<style lang="scss">
.user-select-dialog {
  margin-top: 0 !important;
  top: 50%;
  transform: translateY(-50%);
}
</style>
