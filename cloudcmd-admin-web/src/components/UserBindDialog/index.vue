<!--
  用户单选弹窗组件 (UserBindDialog)

  功能说明：
  - 用于绑定用户（单选模式）
  - 支持用户搜索、分页
  - 表格最后一列显示"绑定/解绑"按钮
  - 支持显示已绑定用户信息

  Props:
  - visible: 弹窗是否显示
  - bindUser: 当前绑定的用户信息

  Events:
  - update:visible: 更新弹窗显示状态
  - bind: 绑定用户成功
  - unbind: 解绑用户成功

  使用示例：
  <user-bind-dialog
    :visible.sync="dialogVisible"
    :bind-user="bindUserInfo"
    @bind="handleBindSuccess"
    @unbind="handleUnbindSuccess"
  />
-->
<template>
  <el-dialog
    :visible.sync="dialogVisible"
    title="绑定警员"
    :close-on-click-modal="false"
    width="800px"
    custom-class="user-bind-dialog"
    @close="handleClose"
  >
    <!-- 已绑定用户信息卡片 -->
    <div v-if="bindUser && bindUser.id && bindUser.imUserName" class="bind-user-card">
      <div class="card-header">
        <span class="card-title">当前绑定警员</span>
        <el-button
          type="text"
          icon="el-icon-delete"
          style="color: #F56C6C"
          :loading="unbindLoading"
          :disabled="unbindLoading"
          @click="handleUnbindConfirm"
        >
          解绑
        </el-button>
      </div>
      <div class="card-content">
        <div class="info-item">
          <span class="label">姓名：</span>
          <span class="value">{{ bindUser.imUserName || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="label">账号：</span>
          <span class="value">{{ bindUser.imUserId || bindUser.id || '-' }}</span>
        </div>
      </div>
    </div>

    <!-- 搜索栏 -->
    <search-bar
      ref="searchBar"
      search-key="name"
      search-placeholder="请输入用户姓名"
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
      height="400px"
      @pagination="onPagination"
    >
      <!-- 状态列 -->
      <template #status="{ row }">
        <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
          {{ row.status === 0 ? '正常' : '禁用' }}
        </el-tag>
      </template>

      <!-- 操作列 -->
      <template #action="{ row }">
        <!-- 已绑定且是当前用户 -->
        <template v-if="bindUser && bindUser.imUserId === row.id">
          <el-button
            type="text"
            icon="el-icon-delete"
            style="color: #F56C6C"
            :loading="unbindLoading"
            :disabled="unbindLoading"
            @click="handleUnbindConfirm"
          >
            解绑
          </el-button>
        </template>
        <!-- 未绑定或其他用户 -->
        <template v-else>
          <el-button
            type="text"
            icon="el-icon-link"
            style="color: #409EFF"
            :loading="bindLoading"
            :disabled="bindLoading"
            @click="handleBindConfirm(row)"
          >
            绑定
          </el-button>
        </template>
      </template>
    </pro-table>
  </el-dialog>
</template>

<script>
import ProTable from '@/components/ProTable'
import SearchBar from '@/components/SearchBar'
import { getAvailableUsers, bindUser, unbindUser } from '@/api/authority/customDepartment'

export default {
  name: 'UserBindDialog',

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
     * 当前绑定的用户信息
     */
    bindUser: {
      type: Object,
      default: () => null
    }
  },

  data() {
    return {
      // 搜索关键词
      searchName: '',
      // 加载状态
      loading: false,
      // 绑定/解绑加载状态
      bindLoading: false,
      unbindLoading: false,
      // 用户列表数据
      userList: [],
      // 分页参数
      pagination: {
        pageNum: 1,
        pageSize: 10,
        total: 0
      }
    }
  },

  computed: {
    /** 列配置 */
    columns() {
      return [
        { title: '姓名', dataIndex: 'name', minWidth: 120, customTooltip: true },
        { title: '身份证号', dataIndex: 'idCard', minWidth: 180, customTooltip: true },
        { title: '组织名称', dataIndex: 'departmentName', minWidth: 150, customTooltip: true },
        { title: '组织编码', dataIndex: 'departmentCode', minWidth: 120, customTooltip: true },
        { title: '操作', minWidth: 80, fixed: 'right', slot: 'action' }
      ]
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
    initDialog() {
      // 重置搜索条件
      this.searchName = ''
      this.pagination.pageNum = 1

      // 加载用户列表
      this.loadUserList()
    },

    /**
     * 加载用户列表
     */
    loadUserList() {
      this.loading = true

      const params = {
        name: this.searchName,
        pageNum: this.pagination.pageNum,
        pageSize: this.pagination.pageSize
      }

      // 调用API获取可绑定用户列表
      getAvailableUsers(params).then(({ code, data }) => {
        if (code === 0) {
          this.userList = data.records || []
          this.pagination.total = Number(data.total || 0)
        }
      }).catch(error => {
        console.error('加载用户列表失败:', error)
        this.$message.error('操作失败')
      }).finally(() => {
        this.loading = false
      })
    },

    /**
     * SearchBar 搜索事件
     */
    handleSearch(params) {
      this.searchName = params.name || ''
      this.pagination.pageNum = 1
      this.loadUserList()
    },

    /**
     * SearchBar 重置事件
     */
    handleSearchReset() {
      this.searchName = ''
      this.pagination.pageNum = 1
      this.loadUserList()
    },

    /**
     * ProTable 分页变化
     */
    onPagination() {
      this.loadUserList()
    },

    /**
     * 绑定确认
     */
    handleBindConfirm(user) {
      if (this.bindLoading) return
      this.$confirm(`确定绑定警员"${user.name}"吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.bindLoading = true
        bindUser({ imUserId: user.id }).then((res) => {
          if (res.code === 0) {
            this.$message.success('绑定成功')
            this.$emit('bindRefresh')
          } else if (res?.msg) {
            this.$message.error(res?.msg)
          }
        }).catch(error => {
          console.error('绑定失败:', error)
          this.$message.error('绑定失败')
        }).finally(() => {
          this.bindLoading = false
        })
      }).catch(() => {})
    },

    /**
     * 解绑确认
     */
    handleUnbindConfirm() {
      if (this.unbindLoading) return
      const userName = this.bindUser?.imUserName || '该警员'
      this.$confirm(`确定解绑警员"${userName}"吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.unbindLoading = true
        unbindUser({}).then((res) => {
          if (res?.code === 0) {
            this.$message.success('解绑成功')
            this.$emit('unbind')
            // 刷新列表
            this.loadUserList()
          } else if (res?.msg) {
            this.$message.error(res?.msg)
          }
        }).catch(error => {
          console.error('解绑失败:', error)
          this.$message.error('解绑失败')
        }).finally(() => {
          this.unbindLoading = false
        })
      }).catch(() => {})
    },

    /**
     * 处理关闭弹窗
     */
    handleClose() {
      this.dialogVisible = false
      this.searchName = ''
      this.userList = []
    }
  }
}
</script>

<style lang="scss" scoped>
// 已绑定用户卡片
.bind-user-card {
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #f0f9eb;
  border-radius: 4px;
  border: 1px solid #e1f3d8;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;

    .card-title {
      font-size: 14px;
      font-weight: 600;
      color: #67c23a;
    }
  }

  .card-content {
    display: flex;
    gap: 24px;

    .info-item {
      font-size: 13px;

      .label {
        color: #909399;
      }

      .value {
        color: #303133;
      }
    }
  }
}

// 操作按钮样式
::v-deep .el-button--text {
  padding: 4px 8px;
  font-size: 13px;
}
</style>

<!-- 弹窗垂直居中（scoped 无法穿透 el-dialog 遮罩层） -->
<style lang="scss">
.user-bind-dialog {
  margin-top: 0 !important;
  top: 50%;
  transform: translateY(-50%);
}
</style>
