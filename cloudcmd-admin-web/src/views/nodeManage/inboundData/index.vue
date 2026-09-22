<template>
  <div class="inbound-data">
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
      <!-- <template #grantStatus="{ row }">
        <el-tag v-if="row.grant === 1 && !row.expired" type="success" size="small">已授权</el-tag>
        <el-tag v-else-if="row.expired" type="danger" size="small">已过期</el-tag>
        <el-tag v-else type="info" size="small">未授权</el-tag>
      </template> -->
      <!-- 标签列 -->
      <!-- <template #tag="{ row }">
        <el-tag v-if="row.tag" size="small" type="info" class="tag-ellipsis">{{ row.tag }}</el-tag>
        <span v-else>-</span>
      </template> -->
      <!-- 连接状态列 -->
      <!-- <template #connectionStatus="{ row }">
        <connection-status-dot :status="row.status" :status-desc="row.statusDesc" />
      </template> -->
       <template #org="{ row }">
        <el-tag :type="row.org == 1 ? '' : 'info'">{{
          row.org == 1 ? '已授权' : '未授权'
        }}</el-tag>
      </template>
      <!-- 看板 -->
      <template #dashboard="{ row }">
        <el-tag :type="row.dashboard == 1 ? '' : 'info'">{{
          row.dashboard == 1 ? '已授权' : '未授权'
        }}</el-tag>
      </template>
      <!-- 协同岗 -->
      <template #coopUser="{ row }">
        <el-tag :type="row.coopUser == 1 ? '' : 'info'">{{
          row.coopUser == 1 ? '已授权' : '未授权'
        }}</el-tag>
      </template>
      <!-- H5 -->
      <template #h5="{ row }">
        <el-tag :type="row.h5 == 1 ? '' : 'info'">{{
          row.h5 == 1 ? '已授权' : '未授权'
        }}</el-tag>
      </template>
      <!-- 操作列 -->
      <template #operation="{ row }">
        <el-button type="primary" size="mini" @click="handleDetail(row)">
          详情
        </el-button>
      </template>
    </pro-table>

    <!-- 客户端详情抽屉组件 -->
    <client-detail-drawer
      :visible.sync="drawerVisible"
      :client-data="currentClient"
    />
  </div>
</template>

<script>
import SearchBar from '@/components/SearchBar'
import ProTable from '@/components/ProTable'
import ClientDetailDrawer from './components/ClientDetailDrawer.vue'
import ConnectionStatusDot from '../components/ConnectionStatusDot.vue'
import { getClients } from '@/api/nodeManage/client'

export default {
  name: 'InboundData',
  components: { SearchBar, ProTable, ClientDetailDrawer, ConnectionStatusDot },
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
      /** 详情抽屉显示状态 */
      drawerVisible: false,
      /** 当前查看的客户端数据 */
      currentClient: {},
      /** 定时刷新定时器 */
      refreshTimer: null
    }
  },
  computed: {
    /** 表格列配置 */
    clientColumns() {
      return [
        { title: 'IP地址', dataIndex: 'ip', minWidth: 130 },
        // { title: '端口', dataIndex: 'port', width: 80, align: 'center' },
        // { title: '节点名称', dataIndex: 'name', minWidth: 150 },
        // { title: '备注', dataIndex: 'remark', minWidth: 120 },
        // { title: '标签', slot: 'tag', width: 120, align: 'center' },
        // { title: '授权状态', slot: 'grantStatus', width: 100, align: 'center' },
        // { title: '授权人', dataIndex: 'grantUserName', minWidth: 120 },
        // { title: '授权时间', dataIndex: 'grantTime', minWidth: 120 },
        // { title: '连接状态', slot: 'connectionStatus', width: 100, align: 'center' },
        // { title: '最后活跃', dataIndex: 'lastSeen', width: 160, align: 'center' },
        //  { title: '标识', dataIndex: 'peerId', width: 100, align: 'center' },
        { title: '组织部门', slot: 'org', minWidth: 150 },
        { title: '看板', slot: 'dashboard', width: 120, align: 'center' },
        { title: '协同岗', slot: 'coopUser', minWidth: 150 },
        { title: 'H5', slot: 'h5', width: 120, align: 'center' },
        { title: '操作', slot: 'operation', width: 80, align: 'center', fixed: 'right', ellipsis: false }
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
     * 打开详情抽屉
     * @param {Object} row - 当前行数据
     */
    handleDetail(row) {
      this.currentClient = row
      this.drawerVisible = true
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
