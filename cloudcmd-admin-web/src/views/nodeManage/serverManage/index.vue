<template>
  <div class="server-manage">
    <!-- 搜索栏 -->
    <!-- <search-bar
      search-key="keyword"
      search-placeholder="搜索IP或名称"
      :actions="actions"
      @search="handleSearch"
      @reset="handleReset"
      @action="handleAction"
    /> -->

    <!-- 操作栏 -->
    <div class="action-bar">
      <el-button type="primary" icon="el-icon-plus" @click="handleCreate">
        新增
      </el-button>
    </div>

    <!-- 表格 -->
    <pro-table
      ref="serverTable"
      :columns="serverColumns"
      :data="list"
      :loading="listLoading"
      :total="total"
      :page.sync="page"
      :limit.sync="limit"
      @pagination="handlePagination"
    >
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
        <el-button type="primary" size="mini" @click="handleUpdate(row)">
          编辑
        </el-button>
        <el-button type="danger" size="mini" @click="handleDelete(row)">
          删除
        </el-button>
      </template>
    </pro-table>

    <!-- 新增/编辑弹窗组件 -->
    <server-form-dialog
      ref="formDialog"
      :visible.sync="dialogFormVisible"
      :status="dialogStatus"
      :form-data="temp"
      :loading="dialogLoading"
      @submit="handleSubmit"
    />
  </div>
</template>

<script>
import SearchBar from '@/components/SearchBar'
import ProTable from '@/components/ProTable'
import ServerFormDialog from './components/ServerFormDialog.vue'
import ConnectionStatusDot from '../components/ConnectionStatusDot.vue'
import { getServers, createServer, updateServer, deleteServer } from '@/api/nodeManage/server'

export default {
  name: 'ServerManage',
  components: { SearchBar, ProTable, ServerFormDialog, ConnectionStatusDot },
  data() {
    return {
      /** 服务器列表数据 */
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
      /** 表单临时数据 */
      temp: {
        ip: '',
        name: '',
        tag: '',
        remark: ''
      },
      /** 弹窗显示状态 */
      dialogFormVisible: false,
      /** 弹窗状态：create-新增，update-编辑 */
      dialogStatus: 'create',
      /** 弹窗加载状态 */
      dialogLoading: false,
      /** 定时刷新定时器 */
      refreshTimer: null
    }
  },
  computed: {
    /** 表格列配置 */
    serverColumns() {
      return [
        { title: 'IP地址', dataIndex: 'ip', minWidth: 150 },
        { title: '节点名称', dataIndex: 'name', minWidth: 150 },
        { title: '连接状态', slot: 'connectionStatus', width: 120, align: 'center' },
        { title: '备注', dataIndex: 'remark', minWidth: 120 },
        { title: '标签', slot: 'tag', width: 120, align: 'center' },
        { title: '创建人', dataIndex: 'createUserName', width: 180, align: 'center' },
        { title: '创建时间', dataIndex: 'gmtCreated', width: 180, align: 'center' },
        { title: '操作', slot: 'operation', width: 200, align: 'center', fixed: 'right', ellipsis: false }
      ]
    },
    /** 操作按钮配置 */
    actions() {
      return [
        { key: 'create', label: '新增', icon: 'el-icon-plus', type: 'primary' }
      ]
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
     * 获取服务器列表
     * 调用API获取分页数据
     * @param {Boolean} isTimerRefresh - 是否为定时器刷新
     */
    getList(isTimerRefresh = false) {
      if (!isTimerRefresh) {
        this.listLoading = true
      }
      getServers({
        pageNum: this.page,
        pageSize: this.limit,
        keyword: this.searchParams.keyword || ''
      }).then(res => {
        this.list = res.data?.records || []
        this.total = Number(res.data?.total) || 0
      }).catch(() => {
        this.$message.error('获取服务器列表失败')
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
      if (key === 'create') {
        this.handleCreate()
      }
    },

    /** 重置表单数据 */
    resetTemp() {
      this.temp = {
        ip: '',
        name: '',
        tag: '',
        remark: ''
      }
    },

    /** 打开新增弹窗 */
    handleCreate() {
      this.resetTemp()
      this.dialogStatus = 'create'
      this.dialogFormVisible = true
      this.$nextTick(() => {
        this.$refs.formDialog?.clearValidate()
      })
    },

    /**
     * 打开编辑弹窗
     * @param {Object} row - 当前行数据
     */
    handleUpdate(row) {
      this.temp = {
        id: row.id,
        ip: row.ip,
        name: row.name || '',
        tag: row.tag || '',
        remark: row.remark || ''
      }
      this.dialogStatus = 'update'
      this.dialogFormVisible = true
      this.$nextTick(() => {
        this.$refs.formDialog?.clearValidate()
      })
    },

    /**
     * 提交表单
     * @param {Object} param - 提交参数
     * @param {String} param.status - 弹窗状态
     * @param {Object} param.data - 表单数据
     */
    handleSubmit({ status, data }) {
      if (status === 'create') {
        this.createData(data)
      } else {
        this.updateData(data)
      }
    },

    /**
     * 创建服务器
     * @param {Object} data - 表单数据
     */
    createData(data) {
      this.dialogLoading = true
      createServer(data).then(res => {
        if (res.code !== 200 && res.code !== 0) {
          this.$message.error(res && res.msg || '创建失败，请重试')
          return
        }
        this.$message.success('创建成功，要对端授权后，才能连接成功！')
        this.dialogFormVisible = false
        this.getList()
      }).catch(() => {
        this.$message.error('创建失败，请重试')
      }).finally(() => {
        this.dialogLoading = false
      })
    },

    /**
     * 更新服务器
     * @param {Object} data - 表单数据
     */
    updateData(data) {
      this.dialogLoading = true
      const { id, ...updateData } = data
      updateServer(id, updateData).then(res => {
        if (res.code !== 200 && res.code !== 0) {
          this.$message.error(res && res.msg || '更新失败，请重试')
          return
        }
        this.$message.success('更新成功')
        this.dialogFormVisible = false
        this.getList()
      }).catch(() => {
        this.$message.error('更新失败，请重试')
      }).finally(() => {
        this.dialogLoading = false
      })
    },

    /**
     * 删除服务器
     * @param {Object} row - 当前行数据
     */
    handleDelete(row) {
      this.$confirm(
        '删除后该服务器节点「' + (row.name || row.ip) + '」将无法连接，确定删除？',
        '删除确认',
        {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--danger'
        }
      ).then(() => {
        return deleteServer(row.id)
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
.action-bar {
  margin-bottom: 16px;
  display: flex;
  justify-content: end;
}

.tag-ellipsis {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
}
</style>
