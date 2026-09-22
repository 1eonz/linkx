<template>
  <div class="client-manage">
    <!-- 搜索栏 -->
    <!-- <search-bar
      search-key="keyword"
      search-placeholder="搜索IP或备注"
      :actions="actions"
      @search="handleSearch"
      @reset="handleReset"
      @action="handleAction"
    /> -->

    <!-- 表格 -->
    <pro-table
      ref="clientTable"
      :columns="clientColumns"
      :data="list"
      :loading="listLoading"
      :total="total"
      :page.sync="page"
      :limit.sync="limit"
      @pagination="handlePagination"
    >
      <!-- 授权状态列 -->
      <template #grantStatus="{ row }">
        <el-tag v-if="row.grant === 1 && !row.expired" type="success" size="medium">已授权</el-tag>
        <el-tag v-else-if="row.expired" type="danger" size="medium">已过期</el-tag>
        <el-tag v-else type="info" size="medium">未授权</el-tag>
      </template>
      <!-- 标签列 -->
      <template #tag="{ row }">
        <el-tag v-if="row.tag" size="small" type="info" class="tag-ellipsis">{{ row.tag }}</el-tag>
        <span v-else>-</span>
      </template>
      <!-- 连接状态列 -->
      <template #connectionStatus="{ row }">
        <connection-status-dot :status="row.status" :status-desc="row.statusDesc" />
      </template>
      <!-- 操作列 -->
      <template #operation="{ row }">
        <el-button type="primary" size="mini" @click="handleEdit(row)">
          编辑
        </el-button>
        <el-button type="success" size="mini" @click="handleAuthorize(row)">
          授权
        </el-button>
        <el-button type="danger" size="mini" @click="handleDelete(row)">
          删除
        </el-button>
        <el-button type="warning" size="mini" @click="handleReject(row)">
          拒绝
        </el-button>
      </template>
    </pro-table>

    <!-- 编辑弹窗组件 -->
    <edit-dialog
      :visible.sync="editDialogVisible"
      :client-data="currentClient"
      :loading="editDialogLoading"
      @save="handleSaveEdit"
    />

    <!-- 授权弹窗组件 -->
    <authorize-dialog
      :visible.sync="authorizeDialogVisible"
      :client-data="currentClient"
      :loading="authorizeDialogLoading"
      @save="handleSaveAuthorize"
    />

    <!-- 拒绝弹窗组件 -->
    <reject-dialog
      :visible.sync="rejectDialogVisible"
      :client-data="currentClient"
      :loading="rejectDialogLoading"
      @save="handleSaveReject"
    />
  </div>
</template>

<script>
import SearchBar from '@/components/SearchBar'
import ProTable from '@/components/ProTable'
import EditDialog from './components/EditDialog.vue'
import AuthorizeDialog from './components/AuthorizeDialog.vue'
import RejectDialog from './components/RejectDialog.vue'
import ConnectionStatusDot from '../components/ConnectionStatusDot.vue'
import { getClients, updateClient, deleteClient, rejectClient } from '@/api/nodeManage/client'

export default {
  name: 'ClientManage',
  components: { SearchBar, ProTable, EditDialog, AuthorizeDialog, RejectDialog, ConnectionStatusDot },
  data() {
    return {
      /** 客户端列表数据 */
      list: [],
      /** 数据总数 */
      total: 0,
      /** 当前页码 */
      page: 1,
      /** 每页条数 */
      limit: 10,
      /** 列表加载状态 */
      listLoading: true,
      /** 搜索参数 */
      searchParams: {},
      /** 当前操作的客户端数据 */
      currentClient: {},
      /** 编辑弹窗显示状态 */
      editDialogVisible: false,
      /** 编辑弹窗加载状态 */
      editDialogLoading: false,
      /** 授权弹窗显示状态 */
      authorizeDialogVisible: false,
      /** 授权弹窗加载状态 */
      authorizeDialogLoading: false,
      /** 拒绝弹窗显示状态 */
      rejectDialogVisible: false,
      /** 拒绝弹窗加载状态 */
      rejectDialogLoading: false,
      /** 定时刷新定时器 */
      refreshTimer: null
    }
  },
  computed: {
    /** 表格列配置 */
    clientColumns() {
      return [
        { title: 'IP地址', dataIndex: 'ip', minWidth: 130 },
        { title: '节点名称', dataIndex: 'name', minWidth: 150 },
        { title: '备注', dataIndex: 'remark', minWidth: 120 },
        { title: '标签', slot: 'tag', width: 120, align: 'center' },
        { title: '授权状态', slot: 'grantStatus', width: 120, align: 'center' },
        { title: '授权人', dataIndex: 'grantUserName', minWidth: 120 },
        { title: '授权时间', dataIndex: 'grantTime', minWidth: 120 },
        { title: '连接状态', slot: 'connectionStatus', width: 120, align: 'center' },
        { title: '最后活跃', dataIndex: 'lastSeen', width: 160, align: 'center' },
        { title: '创建时间', dataIndex: 'gmtCreated', width: 160, align: 'center' },
        { title: '操作', slot: 'operation', width: 300, align: 'center', fixed: 'right', ellipsis: false }
      ]
    },
    /** 操作按钮配置 */
    actions() {
      return []
    }
  },
  mounted() {
    this.getList()
    this.startRefreshTimer()
  },
  beforeDestroy() {
    this.stopRefreshTimer()
  },
  methods: {
    /**
     * 获取客户端列表
     * 调用API获取分页数据
     * @param {Boolean} isTimerRefresh - 是否为定时器刷新
     */
    getList(isTimerRefresh = false) {
      if (!isTimerRefresh) {
        this.listLoading = true
      }
      getClients({
        pageNum: this.page,
        pageSize: this.limit,
        keyword: this.searchParams.keyword || ''
      }).then(res => {
        this.list = res.data?.records || []
        this.total = Number(res.data?.total) || 0
      }).catch(() => {
        this.$message.error('获取客户端列表失败')
      }).finally(() => {
        this.listLoading = false
      })
    },

    /**
     * 分页变化事件
     * @param {Object} param - 分页参数
     * @param {Number} param.page - 页码
     * @param {Number} param.limit - 每页条数
     */
    handlePagination({ page, limit }) {
      this.page = page
      this.limit = limit
      this.stopRefreshTimer()
      this.getList()
      this.startRefreshTimer()
    },

    /**
     * 搜索事件
     * @param {Object} params - 搜索参数
     */
    handleSearch(params) {
      this.searchParams = params
      this.page = 1
      this.stopRefreshTimer()
      this.getList()
      this.startRefreshTimer()
    },

    /**
     * 重置事件
     * @param {Object} params - 重置后的参数
     */
    handleReset(params) {
      this.searchParams = params
      this.page = 1
      this.stopRefreshTimer()
      this.getList()
      this.startRefreshTimer()
    },

    /**
     * 操作按钮点击事件
     * @param {String} key - 操作标识
     */
    handleAction(key) {
      // 暂无操作
    },

    /**
     * 打开编辑弹窗
     * @param {Object} row - 当前行数据
     */
    handleEdit(row) {
      this.currentClient = row
      this.editDialogVisible = true
    },

    /**
     * 保存编辑数据
     * @param {Object} param - 保存参数
     * @param {String} param.peerId - 客户端标识
     * @param {Object} param.data - 编辑数据
     */
    handleSaveEdit({ peerId, data }) {
      this.editDialogLoading = true
      updateClient(peerId, data).then(res => {
        if (res.code !== 200 && res.code !== 0) {
          this.$message.error(res && res.msg || '编辑失败，请重试')
          return
        }
        this.$message.success('编辑成功')
        this.editDialogVisible = false
        this.getList()
      }).catch(() => {
        this.$message.error('编辑失败，请重试')
      }).finally(() => {
        this.editDialogLoading = false
      })
    },

    /**
     * 打开授权弹窗
     * @param {Object} row - 当前行数据
     */
    handleAuthorize(row) {
      this.currentClient = row
      this.authorizeDialogVisible = true
    },

    /**
     * 保存授权数据
     * @param {Object} param - 保存参数
     * @param {String} param.peerId - 客户端标识
     * @param {Object} param.data - 授权数据
     */
    handleSaveAuthorize({ peerId, data }) {
      this.authorizeDialogLoading = true
      updateClient(peerId, data).then(res => {
        if (res.code !== 200 && res.code !== 0) {
          this.$message.error(res && res.msg || '授权失败，请重试')
          return
        }
        this.$message.success('授权成功')
        this.authorizeDialogVisible = false
        this.getList()
      }).catch(() => {
        this.$message.error('授权失败，请重试')
      }).finally(() => {
        this.authorizeDialogLoading = false
      })
    },

    /**
     * 打开拒绝弹窗
     * @param {Object} row - 当前行数据
     */
    handleReject(row) {
      this.currentClient = row
      this.rejectDialogVisible = true
    },

    /**
     * 保存拒绝操作
     * @param {Object} param - 保存参数
     * @param {String} param.peerId - 客户端标识
     * @param {String} param.desc - 拒绝理由
     */
    handleSaveReject({ peerId, desc }) {
      this.rejectDialogLoading = true
      rejectClient(peerId, desc).then(res => {
        if (res.code !== 200 && res.code !== 0) {
          this.$message.error(res && res.msg || '拒绝失败，请重试')
          return
        }
        this.$message.success('已拒绝该客户端')
        this.rejectDialogVisible = false
        this.getList()
      }).catch(() => {
        this.$message.error('拒绝失败，请重试')
      }).finally(() => {
        this.rejectDialogLoading = false
      })
    },

    /**
     * 删除客户端
     * @param {Object} row - 当前行数据
     */
    handleDelete(row) {
      this.$confirm(
        '删除后客户端「' + (row.name || row.remark || row.ip) + '」将失去授权，确定删除？',
        '删除确认',
        {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--danger'
        }
      ).then(() => {
        return deleteClient(row.id)
      }).then(res => {
        if (res.code !== 200 && res.code !== 0) {
          this.$message.error(res && res.msg || '删除失败，请重试')
          return
        }
        this.$message.success('删除成功')
        this.getList()
      }).catch(e => {
        if (e !== 'cancel') {
          this.$message.error('删除失败，请重试')
        }
      })
    },

    /** 启动定时刷新 */
    startRefreshTimer() {
      this.refreshTimer = setInterval(() => {
        this.getList(true)
      }, 5000)
    },

    /** 停止定时刷新 */
    stopRefreshTimer() {
      if (this.refreshTimer) {
        clearInterval(this.refreshTimer)
        this.refreshTimer = null
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.tag-ellipsis {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
}
</style>
