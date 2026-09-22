<template>
  <div class="outbound-data">
    <!-- 搜索栏 -->
    <!-- <search-bar
      search-key="keyword"
      search-placeholder="搜索IP或名称"
      :actions="actions"
      @search="handleSearch"
      @reset="handleReset"
      @action="handleAction"
    /> -->

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
      <!-- <template #tag="{ row }">
        <el-tag v-if="row.tag" size="small" type="info" class="tag-ellipsis">{{
          row.tag
        }}</el-tag>
        <span v-else>-</span>
      </template> -->
      <!-- 组织部门 -->
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
        <el-button type="primary" size="mini" @click="handleAuthorize(row)">
          授权
        </el-button>
      </template>
    </pro-table>

    <!-- 授权配置弹窗组件 -->
    <grant-config-dialog
      :visible.sync="dialogVisible"
      :server-data="currentServer"
      :loading="saveLoading"
      @save="handleSaveGrant"
    />
  </div>
</template>

<script>
import SearchBar from '@/components/SearchBar'
import ProTable from '@/components/ProTable'
import GrantConfigDialog from './components/GrantConfigDialog.vue'
import ConnectionStatusDot from '../components/ConnectionStatusDot.vue'
import { getServers } from '@/api/nodeManage/server'
import { updateServerOpenDataGrant } from '@/api/nodeManage/openData'

export default {
  name: 'OutboundData',
  components: { SearchBar, ProTable, GrantConfigDialog, ConnectionStatusDot },
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
      /** 授权弹窗显示状态 */
      dialogVisible: false,
      /** 保存加载状态 */
      saveLoading: false,
      /** 当前操作的服务器数据 */
      currentServer: {},
      /** 定时刷新定时器 */
      refreshTimer: null
    }
  },
  computed: {
    /** 表格列配置 */
    serverColumns() {
      return [
        { title: 'IP地址', dataIndex: 'ip', minWidth: 150 },
        // { title: '端口', dataIndex: 'port', width: 100, align: 'center' },
        // { title: '节点名称', dataIndex: 'name', minWidth: 150 },
        // { title: '连接状态', slot: 'dashboard', width: 120, align: 'center' },
        // { title: '备注', dataIndex: 'remark', minWidth: 150 },
        // { title: '标签', slot: 'tag', width: 120, align: 'center' },
        // {
        //   title: '创建时间',
        //   dataIndex: 'gmtCreated',
        //   width: 180,
        //   align: 'center'
        // },
        { title: '标识', dataIndex: 'peerId', width: 100, align: 'center' },
        { title: '组织部门', slot: 'org', minWidth: 150 },
        { title: '看板', slot: 'dashboard', width: 120, align: 'center' },
        { title: '协同岗', slot: 'coopUser', minWidth: 150 },
        { title: 'H5', slot: 'h5', minWidth: 150 },

        {
          title: '操作',
          slot: 'operation',
          width: 100,
          align: 'center',
          fixed: 'right',
          ellipsis: false
        }
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
      })
        .then(res => {
          this.list = res.data?.records || []
          this.total = Number(res.data?.total) || 0
        })
        .catch(() => {
          this.$message.error('获取服务器列表失败')
        })
        .finally(() => {
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
     * 打开授权弹窗
     * @param {Object} row - 当前行数据
     */
    handleAuthorize(row) {
      this.currentServer = row
      this.dialogVisible = true
    },

    /**
     * 保存授权配置
     * @param {Object} param - 保存参数
     * @param {String} param.peerId - 服务器标识
     * @param {Object} param.data - 授权配置数据
     */
    handleSaveGrant({ peerId, data }) {
      this.saveLoading = true
      updateServerOpenDataGrant(peerId, data)
        .then(res => {
          if (res.code !== 200 && res.code !== 0) {
            this.$message.error((res && res.msg) || '保存失败，请重试')
            return
          }
          this.$message.success('授权配置已保存')
          this.dialogVisible = false
        })
        .catch(() => {
          this.$message.error('保存失败，请重试')
        })
        .finally(() => {
          this.saveLoading = false
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
