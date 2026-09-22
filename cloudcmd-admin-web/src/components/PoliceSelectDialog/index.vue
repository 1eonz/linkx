<!--
  警员多选弹窗组件 (PoliceSelectDialog)

  功能说明：
  - 用于为自定义组织绑定警员
  - 支持警员搜索、分页
  - 支持多选，切换分页/搜索时保持选中状态
  - 显示已选人数提示

  Props:
  - visible: 弹窗是否显示
  - nodeId: 组织节点ID
  - nodeName: 组织节点名称（用于标题显示）

  Events:
  - update:visible: 更新弹窗显示状态
  - confirm: 确认选择，返回选中的警员列表

  使用示例：
  <police-select-dialog
    :visible.sync="dialogVisible"
    :node-id="currentNode.id"
    :node-name="currentNode.name"
    @confirm="handleConfirm"
  />
-->
<template>
  <el-dialog
    :visible.sync="dialogVisible"
    :title="dialogTitle"
    :close-on-click-modal="false"
    width="800px"
    custom-class="police-select-dialog"
    @close="handleClose"
    destroy-on-close
  >
    <!-- 搜索栏 -->
    <search-bar
      ref="searchBar"
      search-key="keyword"
      :search-placeholder="搜索警员姓名"
      :search-width="'300px'"
      @search="handleSearch"
      @reset="handleSearchReset"
    />

    <!-- 警员列表表格 -->
    <pro-table
      ref="policeTable"
      :columns="columns"
      :data="policeList"
      :loading="loading"
      :total="pagination.total"
      :page.sync="pagination.pageNum"
      :limit.sync="pagination.pageSize"
      row-key="id"
      show-selection
      selection-bar-unit="人"
      height="400px"
      :selectable="checkSelectable"
      @cross-selection-change="onCrossSelectionChange"
      @pagination="onPagination"
      selectionBarItemLabel="name"
      selectionBarItemSub="departmentName"
    ></pro-table>

    <!-- 底部操作按钮 -->
    <div slot="footer" class="dialog-footer">
      <el-button @click="handleClose">
        取消
      </el-button>
      <el-button type="primary" @click="handleConfirm">
        确定
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import ProTable from '@/components/ProTable'
import SearchBar from '@/components/SearchBar'
import { getAvailableUsers } from '@/api/authority/customDepartment'

export default {
  name: 'PoliceSelectDialog',

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
     * 组织节点ID
     */
    nodeId: {
      type: [String, Number],
      default: ''
    },

    /**
     * 组织节点名称
     */
    nodeName: {
      type: String,
      default: ''
    }
  },

  data() {
    return {
      // 搜索关键词（由 SearchBar 驱动）
      searchKeyword: '',
      // 加载状态
      loading: false,
      // 警员列表数据
      policeList: [],
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
        { title: '姓名', dataIndex: 'name', minWidth: 120 },
        { title: '手机号', dataIndex: 'mobile', minWidth: 180 },
        { title: '组织名称', dataIndex: 'departmentName', minWidth: 150 },
        { title: '组织编码', dataIndex: 'departmentCode', minWidth: 120 }
      ]
    },

    /**
     * 弹窗标题
     */
    dialogTitle() {
      const count = this.selectedCount
      const nodeName = this.nodeName || '组织节点'
      return `绑定警员 - 「${nodeName}」`
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
      this.searchKeyword = ''
      this.pagination.pageNum = 1

      // 加载警员列表
      await this.loadPoliceList()
    },

    /**
     * 加载警员列表
     */
    async loadPoliceList() {
      this.loading = true

      try {
        const params = {
          name: this.searchKeyword,
          pageNum: this.pagination.pageNum,
          pageSize: this.pagination.pageSize
        }

        // 调用API获取可绑定警员列表
        const { code, data } = await getAvailableUsers(params)

        if (code === 0) {
          const records = data?.records || []
          
          // 处理数据，添加 isDisabled 标识
          this.policeList = records.map(user => ({
            ...user,
            // 判断是否已在当前组织
            isDisabled: user.boundCustomDepartments?.some(
              dept => String(dept.id) === String(this.nodeId)
            ) || false
          }))
          this.pagination.total = data?.total || 0
        }
      } catch (error) {
        console.error('加载警员列表失败:', error)
        this.$message.error('获取可绑定警员列表失败')
      } finally {
        this.loading = false
      }
    },

    /**
     * 控制行是否可选
     * @param {Object} row - 行数据
     * @returns {Boolean} 是否可选
     */
    checkSelectable(row) {
      console.log(row, !row.isDisabled, 'checkSelectable')
      return !row.isDisabled
    },

    /**
     * SearchBar 搜索事件
     */
    async handleSearch(params) {
      this.searchKeyword = params.keyword || ''
      this.pagination.pageNum = 1
      await this.loadPoliceList()
    },

    /**
     * SearchBar 重置事件
     */
    async handleSearchReset() {
      this.searchKeyword = ''
      this.pagination.pageNum = 1
      await this.loadPoliceList()
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
      this.loadPoliceList()
    },

    /**
     * 处理确认按钮
     */
    handleConfirm() {
      // 从 ProTable 获取全量选中数据
      const selectedList = this.$refs.policeTable ? this.$refs.policeTable.getMultipleSelection() : []

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
      this.searchKeyword = ''
      this.policeList = []
      this.selectedCount = 0
      // 清空 ProTable 选中状态
      this.$nextTick(() => {
        if (this.$refs.policeTable) {
          this.$refs.policeTable.clearAllSelection()
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
.police-select-dialog {
  margin-top: 0 !important;
  top: 50%;
  transform: translateY(-50%);
}
</style>
