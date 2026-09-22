<!--
  SearchBar 搜索栏组件

  功能说明：
  - 左侧：搜索输入框（可选）+ 组织选择器（可选）+ 时间段选择器（可选）+ 搜索按钮 + 重置按钮
  - 右侧：操作按钮（新增 / 批量编辑 / 批量删除 / 批量下载 / 自定义）
  - 响应式：宽度不足时自动换行（左侧一行，右侧第二行）

  Props:
    searchKey        - 搜索输入框绑定的 key，如 'name'，不传则不显示搜索框
    searchPlaceholder - 搜索输入框 placeholder
    searchDefaultValue - 搜索输入框初始值
    searchWidth      - 搜索输入框宽度，默认 200px

    showOrg          - 是否显示组织选择器，默认 false
    orgDefaultValue  - 组织初始值 { departmentCode, departmentName, privString }
    orgSyncSign      - 组织同步标识，true=一次性加载，false=懒加载，默认 false

    showDateRange    - 是否显示时间段选择器，默认 false
    dateRangeType    - 时间段类型：daterange / monthrange / daterange（默认 daterange）
    dateRangeFormat  - 时间段值格式，默认 yyyy-MM-dd
    dateRangeDefaultValue - 时间段初始值 [start, end]
    dateRangeStartPlaceholder - 开始时间 placeholder
    dateRangeEndPlaceholder   - 结束时间 placeholder

    actions          - 右侧操作按钮配置数组
      [{ key, label, icon, type, visible, disabled }]
      key: 'create' | 'batchEdit' | 'batchDelete' | 'batchDownload' | 自定义字符串
      label: 按钮文案（必填）
      icon: 按钮图标类名
      type: 按钮类型 primary/warning/danger 等
      visible: 是否显示，默认 true
      disabled: 是否禁用，默认 false

    selectedCount    - 当前选中条目数量，用于批量按钮 disabled 计算

  Events:
    @search   - 点击搜索按钮，参数: { [searchKey], departmentCode, departmentName, privString, dateRange }
    @reset    - 点击重置按钮，参数: 同 search（重置后的空值）
    @action   - 点击右侧操作按钮，参数: action.key
    @org-change - 组织选择变化，参数: { code, name, id }
-->
<template>
  <div ref="filterContainer" class="search-bar" :class="{ 'is-stacked': isStacked }">
    <!-- 左侧：搜索条件 + 搜索/重置按钮 -->
    <div ref="filterLeft" class="search-bar__left">
      <!-- 搜索输入框 -->
      <el-input
        v-if="searchKey"
        v-model="innerSearchValue"
        :placeholder="searchPlaceholder || $t('index.messageText.pleaseInputAccount')"
        :style="{ width: searchWidth || '200px' }"
        class="search-bar__item search-bar__search-input"
        prefix-icon="el-icon-search"
        clearable
        @keyup.enter.native="handleSearch"
      />

      <!-- 组织选择器 -->
      <template v-if="showOrg">
        <SelectTree
          v-if="orgSyncSign"
          v-model="innerOrgName"
          :is-init-value="true"
          :value="innerOrgCode"
          :placeholder="$t('index.messageText.pleaseSelectOrganization')"
          class="search-bar__item"
          @clear-val="handleOrgClear"
          @current-change="handleOrgChange"
        />
        <select-tree-lazy
          v-else
          v-model="innerOrgName"
          class="search-bar__item"
          style="width: 200px;"
          :is-init-value="true"
          :department-code="innerOrgCode"
          :placeholder="$t('index.messageText.pleaseSelectOrganization')"
          @clear-val="handleOrgClear"
          @current-change="handleOrgChange"
        />
      </template>

      <!-- 时间段选择器 -->
      <el-date-picker
        v-if="showDateRange"
        v-model="innerDateRange"
        :type="dateRangeType || 'daterange'"
        :value-format="dateRangeFormat || 'yyyy-MM-dd'"
        range-separator="-"
        :start-placeholder="dateRangeStartPlaceholder || $t('index.messageText.startDate')"
        :end-placeholder="dateRangeEndPlaceholder || $t('index.messageText.endDate')"
        class="search-bar__item search-bar__date-range"
        @change="handleSearch"
      />

      <!-- 搜索按钮 -->
      <el-button
        v-waves
        class="search-bar__item"
        type="primary"
        icon="el-icon-search"
        @click="handleSearch"
      >
        {{ $t('index.operations.search') }}
      </el-button>
      <!-- 重置按钮 -->
      <el-button
        v-waves
        class="search-bar__item"
        icon="el-icon-refresh"
        @click="handleReset"
      >
        {{ $t('index.operations.reset') }}
      </el-button>
    </div>

    <!-- 右侧：操作按钮 -->
    <div ref="filterRight" class="search-bar__right">
      <el-tooltip
        v-for="action in visibleActions"
        :key="action.key"
        :disabled="!isBatchAction(action) || selectedCount > 0"
        :content="$t('index.messageText.pleaseCheckData')"
        placement="top"
      >
        <el-button
          v-waves
          class="search-bar__item"
          :type="action.type || 'default'"
          :icon="action.icon || ''"
          :disabled="isActionDisabled(action)"
          @click="handleAction(action)"
        >
          {{ action.label }}
        </el-button>
      </el-tooltip>
    </div>
  </div>
</template>

<script>
import SelectTree from '@/components/SelectTree'
import selectTreeLazy from '@/components/SelectTreeLazy'

// 批量操作 key 列表
const BATCH_KEYS = ['batchEdit', 'batchDelete', 'batchDownload']

export default {
  name: 'SearchBar',

  components: {
    SelectTree,
    selectTreeLazy
  },

  props: {
    // 搜索输入框
    searchKey: {
      type: String,
      default: ''
    },
    searchPlaceholder: {
      type: String,
      default: ''
    },
    searchDefaultValue: {
      type: [String, Number],
      default: ''
    },
    searchWidth: {
      type: String,
      default: '200px'
    },

    // 组织选择器
    showOrg: {
      type: Boolean,
      default: false
    },
    orgDefaultValue: {
      type: Object,
      default: () => ({})
    },
    orgSyncSign: {
      type: Boolean,
      default: false
    },

    // 时间段选择器
    showDateRange: {
      type: Boolean,
      default: false
    },
    dateRangeType: {
      type: String,
      default: 'daterange'
    },
    dateRangeFormat: {
      type: String,
      default: 'yyyy-MM-dd'
    },
    dateRangeDefaultValue: {
      type: Array,
      default: () => []
    },
    dateRangeStartPlaceholder: {
      type: String,
      default: ''
    },
    dateRangeEndPlaceholder: {
      type: String,
      default: ''
    },

    // 右侧操作按钮
    actions: {
      type: Array,
      default: () => []
    },

    // 选中条目数量
    selectedCount: {
      type: Number,
      default: 0
    }
  },

  data() {
    return {
      // 内部搜索值
      innerSearchValue: this.searchDefaultValue,
      // 内部组织值
      innerOrgCode: this.orgDefaultValue.departmentCode || '',
      innerOrgName: this.orgDefaultValue.departmentName || '',
      innerOrgId: this.orgDefaultValue.privString || '',
      // 内部时间段值
      innerDateRange: this.dateRangeDefaultValue ? [...this.dateRangeDefaultValue] : [],
      // 是否换行（响应式）
      isStacked: false,
      // ResizeObserver 实例
      resizeObserver: null,
      // 防抖定时器
      resizeTimer: null
    }
  },

  computed: {
    /** 过滤可见的 action */
    visibleActions() {
      return this.actions.filter(a => a.visible !== false)
    }
  },

  mounted() {
    this.initResizeObserver()
  },

  beforeDestroy() {
    if (this.resizeObserver) {
      this.resizeObserver.disconnect()
      this.resizeObserver = null
    }
    if (this.resizeTimer) {
      clearTimeout(this.resizeTimer)
      this.resizeTimer = null
    }
  },

  methods: {
    /**
     * 初始化 ResizeObserver，检测宽度是否需要换行（带 100ms 防抖）
     */
    initResizeObserver() {
      if (typeof ResizeObserver === 'undefined') return

      this.resizeObserver = new ResizeObserver(() => {
        if (this.resizeTimer) clearTimeout(this.resizeTimer)
        this.resizeTimer = setTimeout(() => {
          this.checkStack()
        }, 100)
      })
      this.resizeObserver.observe(this.$refs.filterContainer)
    },

    /**
     * 检测是否需要换行
     * 当左右两侧内容总宽度超过容器宽度时，切换为上下两行
     */
    checkStack() {
      const container = this.$refs.filterContainer
      const left = this.$refs.filterLeft
      const right = this.$refs.filterRight
      if (!container || !left || !right) return

      const containerWidth = container.clientWidth
      const leftWidth = left.scrollWidth
      const rightWidth = right.scrollWidth
      // 间距 16px
      const gap = 16

      this.isStacked = (leftWidth + rightWidth + gap) > containerWidth
    },

    /**
     * 获取当前搜索条件对象
     */
    getSearchParams() {
      const params = {}

      // 搜索输入
      if (this.searchKey) {
        params[this.searchKey] = this.innerSearchValue
      }

      // 组织
      if (this.showOrg) {
        params.departmentCode = this.innerOrgCode
        params.departmentName = this.innerOrgName
        params.privString = this.innerOrgId
      }

      // 时间段
      if (this.showDateRange) {
        params.dateRange = this.innerDateRange || []
        params.startDate = (this.innerDateRange && this.innerDateRange[0]) || ''
        params.endDate = (this.innerDateRange && this.innerDateRange[1]) || ''
      }

      return params
    },

    /**
     * 搜索按钮点击
     */
    handleSearch() {
      this.$emit('search', this.getSearchParams())
    },

    /**
     * 重置按钮点击
     */
    handleReset() {
      // 重置搜索输入
      this.innerSearchValue = ''
      // 重置组织（同时清空 SelectTree 内部输入框显示）
      this.innerOrgCode = ''
      this.innerOrgName = ''
      this.innerOrgId = ''
      // 重置时间段
      this.innerDateRange = []

      // S8: 触发 SelectTree / select-tree-lazy 的清空事件，使其内部 inputText 视觉清空
      this.$nextTick(() => {
        // SelectTree 同步模式下清空内部状态
        const selectTreeRef = this.orgSyncSign
          ? this.$refs.filterLeft?.querySelector('.select-tree')
          : null
        if (selectTreeRef && selectTreeRef.__vue__) {
          selectTreeRef.__vue__.clearVal && selectTreeRef.__vue__.clearVal()
        }
      })

      this.$emit('reset', this.getSearchParams())
    },

    /**
     * 组织选择变化
     */
    handleOrgChange(data) {
      this.innerOrgCode = data.code
      this.innerOrgName = data.name
      this.innerOrgId = data.id
      this.$emit('org-change', { code: data.code, name: data.name, id: data.id })
    },

    /**
     * 组织清空
     */
    handleOrgClear() {
      this.innerOrgCode = ''
      this.innerOrgName = ''
      this.innerOrgId = ''
      this.$emit('org-change', { code: '', name: '', id: '' })
    },

    /**
     * 判断是否为批量操作
     */
    isBatchAction(action) {
      return BATCH_KEYS.includes(action.key)
    },

    /**
     * 计算按钮 disabled 状态
     * 批量操作：未选中时 disabled；非批量操作：由 action.disabled 控制
     * 逻辑：(isBatch && selectedCount===0) || action.disabled
     */
    isActionDisabled(action) {
      const batchDisabled = this.isBatchAction(action) && this.selectedCount === 0
      return batchDisabled || action.disabled
    },

    /**
     * 操作按钮点击
     */
    handleAction(action) {
      this.$emit('action', action.key)
    },

    /**
     * 外部重置组织（供父组件调用，如父组件有 departmentCode 需要同步）
     */
    setOrgValue({ departmentCode, departmentName, privString }) {
      this.innerOrgCode = departmentCode || ''
      this.innerOrgName = departmentName || ''
      this.innerOrgId = privString || ''
    }
  }
}
</script>

<style lang="scss" scoped>
.search-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding-bottom: 16px;
  margin-bottom: 4px;
  border-bottom: 1px solid #F0F2F5;

  // 换行模式
  &.is-stacked {
    flex-direction: column;
    gap: 12px;
  }

  &__left,
  &__right {
    display: flex;
    align-items: center;
    flex-wrap: nowrap;
    gap: 8px;
  }

  &__left {
    // 修复 SelectTree / SelectTreeLazy 内部 .select-tree 的对齐
    // class="search-bar__item" 会合并到组件根元素 .select-tree 上
    // 所以 .select-tree 和 .search-bar__item 是同一个元素，需要在父级用 ::v-deep 覆盖
    ::v-deep .select-tree {
      display: inline-flex;
      align-items: center;
      margin-bottom: 0;
    }
  }

  &__right {
    flex-shrink: 0;
  }

  &__item {
    display: inline-flex;
    align-items: center;
    flex-shrink: 0;
  }

  &__search-input {
    ::v-deep .el-input__inner {
      border-radius: 20px;
    }
  }

  &__date-range {
    // 日期范围选择器对齐
    display: inline-flex;
    align-items: center;
  }
}
</style>
