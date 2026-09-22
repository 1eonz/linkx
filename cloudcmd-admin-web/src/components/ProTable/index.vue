<template>
  <div class="pro-table">
    <!-- 跨页选中提示条 -->
    <transition name="slide-fade">
      <div v-if="showSelectionBarVisible" class="selection-bar">
        <div class="selection-bar-left">
          <i class="el-icon-circle-check selection-icon"></i>
          <span class="selection-text">
            {{ computedSelectionBarText }} <strong class="selection-count">{{ selectedCount }}</strong> {{ computedSelectionBarUnit }}
          </span>
        </div>
        <div class="selection-bar-right" v-if="selectedCount > 0">
          <el-popover
            placement="bottom-end"
            width="320"
            trigger="hover"
            popper-class="selected-popover"
          >
            <div class="selected-list">
              <div
                v-for="(item, index) in displayedSelectedItems"
                :key="item[rowKey]"
                class="selected-item"
              >
                <!-- 自定义选中项插槽：完全控制每项渲染 -->
                <slot
                  name="selection-bar-item"
                  :item="item"
                  :index="index"
                  :remove="removeSelection"
                >
                  <!-- 自动渲染模式：主标签 + 副标签(tag) + 移除按钮 -->
                  <el-tooltip
                    v-if="selectionBarItemLabel"
                    :content="item[selectionBarItemLabel] || ''"
                    placement="top"
                    :disabled="!isTextOverflow(item[selectionBarItemLabel], 120)"
                  >
                    <span class="item-label">{{ item[selectionBarItemLabel] || '-' }}</span>
                  </el-tooltip>
                  <!-- 无 selectionBarItemLabel 时回退到 rowKey -->
                  <span v-if="!selectionBarItemLabel" class="item-label">{{ item[rowKey] }}</span>
                  <!-- 副标签（部门等），显示为 el-tag -->
                  <el-tooltip
                    v-if="selectionBarItemSub && item[selectionBarItemSub]"
                    :content="item[selectionBarItemSub]"
                    placement="top"
                    :disabled="!isTextOverflow(item[selectionBarItemSub], 80)"
                  >
                    <el-tag size="mini" type="info" class="item-sub">
                      {{ item[selectionBarItemSub] }}
                    </el-tag>
                  </el-tooltip>
                  <!-- 移除按钮 -->
                  <span class="item-remove" :title="$t('index.proTable.removeItem')" @click="removeSelection(item)">
                    <i class="el-icon-close"></i>
                  </span>
                </slot>
              </div>
              <div v-if="selectedCount > 20" class="more-tip">
                {{ $t('index.proTable.andMore') }} {{ selectedCount }} {{ computedSelectionBarUnit }}
              </div>
            </div>
            <span slot="reference" class="view-detail-link">
              <i class="el-icon-view"></i> {{ $t('index.proTable.viewDetail') }}
            </span>
          </el-popover>
          <el-button type="text" size="small" class="clear-btn" @click="clearAllSelection">
            <i class="el-icon-delete"></i> {{ $t('index.proTable.clearSelection') }}
          </el-button>
        </div>
      </div>
    </transition>

    <!-- 表格 -->
    <el-table
      ref="elTable"
      v-loading="loading"
      v-bind="$attrs"
      :data="data"
      :row-key="rowKey"
      :height="computedTableHeight"
      stripe
      border
      fit
      highlight-current-row
      style="width: 100%;"
      class="pro-table__inner"
      :header-cell-style="headerCellStyle"
      @select="handleSelect"
      @select-all="handleSelectAll"
      v-on="tableListeners"
    >
      <!-- 多选列 -->
      <el-table-column
        v-if="showSelection"
        type="selection"
        align="center"
        width="50"
        :selectable="selectable"
      />

      <!-- 序号列 -->
      <el-table-column
        v-if="showIndex"
        type="index"
        label="#"
        align="center"
        width="60"
      />

      <!-- 动态列 -->
      <template v-for="col in normalizedColumns">
        <el-table-column
          :key="col.dataIndex || col.slot"
          :prop="col.dataIndex"
          :label="col.title"
          :min-width="col.minWidth"
          :width="col.width"
          :fixed="col.fixed"
          :align="col.align || 'center'"
          :header-align="col.headerAlign || 'center'"
          :show-overflow-tooltip="col.ellipsis !== false"
        >
          <template slot-scope="scope">
            <!-- 自定义插槽 -->
            <slot
              v-if="col.slot"
              :name="col.slot"
              :row="scope.row"
              :column="col"
              :index="scope.$index"
            />
            <!-- customTooltip 模式：超长省略 + el-tooltip -->
            <!-- <el-tooltip
              v-else-if="col.customTooltip"
              :content="String(getCellValue(scope.row, col))"
              placement="top"
              v-bind="col.tooltipProps || {}"
            >
              <span class="cell-ellipsis">{{ getCellValue(scope.row, col) }}</span>
            </el-tooltip> -->
            <!-- 默认渲染：取 dataIndex 对应的值 -->
            <span v-else>{{ getCellValue(scope.row, col) }}</span>
          </template>
        </el-table-column>
      </template>

      <!-- 空数据插槽 -->
      <template slot="empty">
        <slot name="empty">
          <div class="pro-table__empty">
            <i class="el-icon-folder-opened"></i>
            <p>{{ $t('index.proTable.noData') }}</p>
            <!-- 空状态引导操作插槽（P3） -->
            <slot name="empty-action"></slot>
          </div>
        </slot>
      </template>
    </el-table>

    <!-- 分页 -->
    <pagination
      v-if="showPagination"
      v-show="total > 0"
      :total="total"
      :page.sync="currentPage"
      :limit.sync="currentLimit"
      @pagination="handlePagination"
    />
  </div>
</template>

<script>
/**
 * ProTable - 高级表格组件
 * 
 * 功能特性：
 * 1. 类似 antd Table 的 columns 配置方式
 * 2. 内置分页、loading、多选、序号列
 * 3. 跨页选中池（选中状态跨页保留）
 * 4. 跨页选中提示条（可配置是否显示、自定义选中项）
 * 5. 支持自定义列渲染（slot）
 * 6. 支持嵌套字段路径（dataIndex: 'org.name'）
 * 7. 表头样式可配置（headerCellStyle）
 * 8. 列超长文本支持 customTooltip + tooltipProps
 * 
 * @example 基础用法
 * <pro-table
 *   :columns="columns"
 *   :data="list"
 *   :loading="loading"
 *   :total="total"
 *   :page.sync="query.page"
 *   :limit.sync="query.limit"
 * />
 * 
 * @example 多选 + 跨页选中提示条（自动渲染名字+部门）
 * <pro-table
 *   show-selection
 *   selection-bar-unit="人"
 *   selection-bar-item-label="name"
 *   selection-bar-item-sub="departmentName"
 *   @cross-selection-change="onChange"
 * />
 * 
 * @example 多选 + 自定义选中项（完全控制渲染）
 * <pro-table show-selection selection-bar-unit="人">
 *   <template #selection-bar-item="{ item, remove }">
 *     <img :src="item.avatar" style="width:24px;height:24px;border-radius:50%" />
 *     <span style="flex:1;margin:0 8px">{{ item.name }}</span>
 *     <i class="el-icon-close" @click="remove(item)"></i>
 *   </template>
 * </pro-table>
 * 
 * @example 不显示跨页选中提示条
 * <pro-table show-selection :show-selection-bar="false" />
 * 
 * @example 列超长文本 tooltip
 * columns: [
 *   { title: '组织', dataIndex: 'departmentName', customTooltip: true },
 *   { title: '描述', dataIndex: 'desc', customTooltip: true, tooltipProps: { placement: 'top' } }
 * ]
 * 
 * @example 自定义表头样式
 * <pro-table :header-cell-style="{ background: '#fff', color: '#333' }" />
 * 
 * Props:
 * - columns: 列配置数组（详见 Column 配置）
 * - data: 数据源
 * - loading: 加载状态
 * - total: 数据总数
 * - page / limit: 分页（支持 .sync）
 * - showPagination: 是否显示分页
 * - rowKey: 行唯一标识字段名
 * - showSelection: 是否显示多选列
 * - showIndex: 是否显示序号列
 * - showSelectionBar: 是否显示跨页选中提示条
 * - selectionBarText: 提示条文本前缀（默认'已选择'）
 * - selectionBarUnit: 选中项单位（默认'项'）
 * - selectionBarItemLabel: 选中项主标签字段名（如'name'）
 * - selectionBarItemSub: 选中项副标签字段名（如'departmentName'）
 * - headerCellStyle: 表头单元格样式
 * 
 * Column 配置:
 * - title: 列标题
 * - dataIndex: 数据字段名，支持嵌套路径如 'org.name'
 * - slot: 自定义插槽名
 * - minWidth / width: 列宽
 * - fixed: 固定列，可选 'left' | 'right'
 * - align: 对齐方式，默认 'center'
 * - ellipsis: 是否溢出省略，默认 true
 * - customTooltip: 超长文本是否显示 el-tooltip，默认 false
 * - tooltipProps: el-tooltip 属性配置，如 { placement: 'top', effect: 'dark' }
 * 
 * Events:
 * - cross-selection-change: 跨页选中变化，参数为全量选中数组
 * - selection-change: 当前页选中变化，参数为当前页选中数组
 * - pagination: 分页变化，参数为 { page, limit }
 * 
 * Slots:
 * - [column.slot]: 列自定义渲染，作用域 { row, column, index }
 * - selection-bar-item: 选中提示条详情项，作用域 { item, index, remove }
 *   优先级：插槽 > selectionBarItemLabel prop > rowKey 默认渲染
 * - empty: 空数据自定义渲染
 * 
 * Methods:
 * - getSelectedMap(): 获取跨页选中池 Map
 * - getMultipleSelection(): 获取全量选中数组
 * - getSelectedCount(): 获取选中数量
 * - removeSelection(item): 从选中池移除单项
 * - clearAllSelection(): 清空所有选中（跨页+当前页）
 * - toggleRowSelection(row, selected): 切换某行选中状态
 * - toggleAllSelection(): 切换全选
 * - setCurrentRow(row): 设置当前行
 * - sort(prop, order): 手动排序
 * - doLayout(): 重新布局
 */
import Pagination from '@/components/Pagination'

export default {
  name: 'ProTable',

  components: { Pagination },

  inheritAttrs: false,

  props: {
    // ==================== 列配置 ====================
    /**
     * 列配置数组
     * @type {Array<{ title, dataIndex, slot, minWidth, width, fixed, align, ellipsis, customTooltip, tooltipProps }>}
     * @property {String} title - 列标题
     * @property {String} dataIndex - 数据字段名，支持嵌套路径如 'org.name'
     * @property {String} slot - 自定义插槽名
     * @property {Number} minWidth - 最小列宽
     * @property {Number} width - 列宽
     * @property {String} fixed - 固定列，可选 'left' | 'right'
     * @property {String} align - 对齐方式，默认 'center'
     * @property {Boolean} ellipsis - 是否溢出省略，默认 true
     * @property {Boolean} customTooltip - 超长文本是否显示 el-tooltip，默认 false
     * @property {Object} tooltipProps - el-tooltip 属性配置，如 { placement: 'top', effect: 'dark' }
     */
    columns: {
      type: Array,
      required: true,
      default: () => []
    },

    // ==================== 数据 ====================
    /** 数据源 */
    data: {
      type: Array,
      default: () => []
    },
    /** 加载状态 */
    loading: {
      type: Boolean,
      default: false
    },
    /** 数据总数（用于分页） */
    total: {
      type: Number,
      default: 0
    },

    // ==================== 分页 ====================
    /** 当前页码（支持 .sync） */
    page: {
      type: Number,
      default: 1
    },
    /** 每页条数（支持 .sync） */
    limit: {
      type: Number,
      default: 10
    },
    /** 是否显示分页 */
    showPagination: {
      type: Boolean,
      default: true
    },

    // ==================== 行配置 ====================
    /** 行数据的唯一标识字段名 */
    rowKey: {
      type: String,
      default: 'id'
    },
    /** 是否显示多选列 */
    showSelection: {
      type: Boolean,
      default: false
    },
    /** 是否显示序号列 */
    showIndex: {
      type: Boolean,
      default: false
    },
    /**
     * 控制复选框是否可选
     * @type {Function}
     * @param {Object} row - 行数据
     * @returns {Boolean} 是否可选
     */
    selectable: {
      type: Function,
      default: null
    },

    // ==================== 跨页选中提示条 ====================
    /**
     * 是否显示跨页选中提示条
     * 仅在 showSelection 为 true 时生效
     */
    showSelectionBar: {
      type: Boolean,
      default: true
    },
    /** 提示条文本前缀，如 '已选择'，默认走 i18n */
    selectionBarText: {
      type: String,
      default: ''
    },
    /** 选中项单位，如 '项'、'人'、'条'，默认走 i18n */
    selectionBarUnit: {
      type: String,
      default: ''
    },
    /**
     * 选中项主标签字段名
     * 设置后，popover 详情项自动显示 item[label] 作为主标签
     * 不设置时回退到 item[rowKey]
     * 示例：selection-bar-item-label="name"
     */
    selectionBarItemLabel: {
      type: String,
      default: ''
    },
    /**
     * 选中项副标签字段名
     * 设置后，popover 详情项自动显示 item[sub] 作为 el-tag
     * 不设置时不显示副标签
     * 示例：selection-bar-item-sub="departmentName"
     */
    selectionBarItemSub: {
      type: String,
      default: ''
    },

    // ==================== 表格样式 ====================
    /**
     * 表头单元格样式
     * 传入时覆盖默认样式，不传则使用默认值
     */
    headerCellStyle: {
      type: Object,
      default: () => ({ background: '#FAFBFC', color: '#1D2129', fontWeight: '600', fontSize: '14px' })
    },

    /**
     * 表格高度（固定高度）
     * 传入时使用固定高度，如 height="400px"
     */
    height: {
      type: [String, Number],
      default: null
    },

    /**
     * 是否启用自动高度计算
     * - true: 使用 ResizeObserver 自动计算表格高度（页面内嵌场景）
     * - false: 不自动计算，使用默认行为或传入的 height
     */
    autoHeight: {
      type: Boolean,
      default: false
    }
  },

  data() {
    return {
      currentPage: this.page,
      currentLimit: this.limit,
      // 跨页选中池
      // key = String(row[rowKey])（Object key 会被自动转为字符串）
      // value = row 完整数据
      // 注意：通过 getSelectedMap() 获取时，key 均为字符串类型，
      // 若 rowKey 值为数字（如 2），key 为 '2'，与数字 2 用 === 比较会失败
      selectedMap: {},
      // 表格高度（动态计算）
      autoTableHeight: null
    }
  },

  computed: {
    // ==================== 列配置 ====================
    /** 标准化列配置，过滤无效列 */
    normalizedColumns() {
      return this.columns.filter(col => col.title || col.slot)
    },

    // ==================== 表格高度 ====================
    /**
     * 计算后的表格高度
     * 1. 优先使用传入的 height prop（固定高度）
     * 2. 其次使用 autoHeight 自动计算的高度
     * 3. 否则返回 null（el-table 默认行为）
     */
    computedTableHeight() {
      if (this.height !== null) {
        // 传入固定高度
        return typeof this.height === 'number' ? this.height : parseInt(this.height, 10)
      }
      if (this.autoHeight) {
        // 自动计算高度
        return this.autoTableHeight
      }
      // 默认行为（不设置高度）
      return null
    },

    // ==================== 跨页选中 ====================
    /** 所有已选中的数据数组（跨页汇总） */
    multipleSelection() {
      return Object.values(this.selectedMap)
    },
    /** 已选中数量 */
    selectedCount() {
      return Object.keys(this.selectedMap).length
    },
    /** 用于 popover 展示的已选项（最多20条） */
    displayedSelectedItems() {
      return this.multipleSelection.slice(0, 20)
    },
    /** 是否显示跨页选中提示条 */
    showSelectionBarVisible() {
      return this.showSelection && this.showSelectionBar
    },
    /** 提示条文本前缀（props 优先，否则走 i18n） */
    computedSelectionBarText() {
      return this.selectionBarText || this.$t('index.proTable.selectedText')
    },
    /** 选中项单位（props 优先，否则走 i18n） */
    computedSelectionBarUnit() {
      return this.selectionBarUnit || this.$t('index.proTable.selectedUnit')
    },

    // ==================== 事件透传 ====================
    /** 透传 el-table 事件（排除由组件自身处理的事件） */
    tableListeners() {
      const { select: _s, 'select-all': _sa, 'selection-change': _sc, ...rest } = this.$listeners
      return rest
    }
  },

  watch: {
    page(val) {
      this.currentPage = val
    },
    limit(val) {
      this.currentLimit = val
    },
    // 监听数据变化，翻页后自动回显选中状态
    data() {
      this.$nextTick(() => {
        this.$nextTick(() => {
          this.restoreSelection()
        })
      })
    },
    // 监听分页显示状态变化，重新计算高度
    showPagination() {
      this.$nextTick(() => this.calcTableHeight())
    },
    // 监听选中提示条显示状态变化，重新计算高度
    showSelectionBarVisible() {
      this.$nextTick(() => this.calcTableHeight())
    }
  },

  mounted() {
    // 仅在启用 autoHeight 时初始化 ResizeObserver
    if (this.autoHeight) {
      this.initResizeObserver()
    }
  },

  beforeDestroy() {
    if (this._resizeObserver) {
      this._resizeObserver.disconnect()
      this._resizeObserver = null
    }
  },

  methods: {
    // ==================== 单元格渲染 ====================
    /**
     * 获取单元格显示值
     * 支持 dataIndex 为嵌套路径，如 'org.name'
     * @param {Object} row - 行数据
     * @param {Object} col - 列配置
     * @returns {String}
     */
    getCellValue(row, col) {
      if (!col.dataIndex) return ''
      const paths = col.dataIndex.split('.')
      let val = row
      for (const key of paths) {
        val = val?.[key]
      }
      if (val === null || val === undefined) return '-'
      return val
    },

    /**
     * 判断文本是否可能溢出（粗略估算）
     * 用于控制 tooltip 的 disabled 状态
     * @param {String} text - 文本内容
     * @param {Number} maxWidth - 最大宽度（px）
     * @returns {Boolean}
     */
    isTextOverflow(text, maxWidth = 120) {
      if (!text) return false
      // 按 14px 字号估算：每个字符约 7px 宽度（中文约 14px）
      const estimatedWidth = text.length * 8
      return estimatedWidth > maxWidth
    },

    // ==================== 分页 ====================
    /**
     * 分页变化处理
     * @param {Object} param - { page, limit }
     */
    handlePagination({ page, limit }) {
      this.$emit('update:page', page)
      this.$emit('update:limit', limit)
      this.$emit('pagination', { page, limit })
    },

    // ==================== 多选处理 ====================
    /**
     * 单行勾选变化（仅在用户交互时触发）
     * @param {Array} selection - 当前页选中的数据
     * @param {Object} row - 当前行数据
     */
    handleSelect(selection, row) {
      this.updateSelectedMap(selection)
    },

    /**
     * 全选/取消全选（仅在用户交互时触发）
     * @param {Array} selection - 当前页选中的数据
     */
    handleSelectAll(selection) {
      this.updateSelectedMap(selection)
    },

    /**
     * 更新跨页选中池
     * 先移除当前页所有行，再加入当前页新选中的行
     * @param {Array} selection - 当前页选中的数据
     */
    updateSelectedMap(selection) {
      // 移除当前页所有行
      this.data.forEach(row => {
        const key = row[this.rowKey]
        this.$delete(this.selectedMap, key)
      })
      // 加入当前页新选中的行
      selection.forEach(row => {
        const key = row[this.rowKey]
        this.$set(this.selectedMap, key, row)
      })
      // 触发跨页选中变化事件
      this.$emit('cross-selection-change', this.multipleSelection)
      // 同时触发 selection-change，方便不需要跨页选中的场景使用
      this.$emit('selection-change', selection)
    },

    /**
     * 翻页后回显 checkbox 选中状态
     */
    restoreSelection() {
      if (!this.$refs.elTable || !this.showSelection) return
      this.data.forEach(row => {
        const key = row[this.rowKey]
        if (this.selectedMap[key]) {
          this.$refs.elTable.toggleRowSelection(row, true)
        }
      })
    },

    // ==================== 暴露方法 ====================
    /**
     * 获取跨页选中池 Map
     * @returns {Object} selectedMap
     */
    getSelectedMap() {
      return this.selectedMap
    },

    /**
     * 获取全量选中数组
     * @returns {Array} selection
     */
    getMultipleSelection() {
      return this.multipleSelection
    },

    /**
     * 获取选中数量
     * @returns {Number} count
     */
    getSelectedCount() {
      return this.selectedCount
    },

    /**
     * 从选中池中移除单项
     * @param {Object} item - 要移除的数据项
     */
    removeSelection(item) {
      const key = item[this.rowKey]
      this.$delete(this.selectedMap, key)
      // 如果该行在当前页，取消 checkbox
      const currentRow = this.data.find(row => row[this.rowKey] === key)
      if (currentRow && this.$refs.elTable) {
        this.$refs.elTable.toggleRowSelection(currentRow, false)
      }
      this.$emit('cross-selection-change', this.multipleSelection)
    },

    /**
     * 清空所有选中（跨页选中池 + 当前页 checkbox）
     * 用于搜索/重置/手动清空等场景
     */
    clearAllSelection() {
      this.selectedMap = {}
      if (this.$refs.elTable) {
        this.$refs.elTable.clearSelection()
      }
      this.$emit('cross-selection-change', [])
      this.$emit('selection-change', [])
    },

    // ==================== el-table 方法透传 ====================

    /** 切换某行选中状态 */
    toggleRowSelection(row, selected) {
      this.$refs.elTable?.toggleRowSelection(row, selected)
    },

    /** 切换全选 */
    toggleAllSelection() {
      this.$refs.elTable?.toggleAllSelection()
    },

    /** 设置当前行 */
    setCurrentRow(row) {
      this.$refs.elTable?.setCurrentRow(row)
    },

    /** 手动排序 */
    sort(prop, order) {
      this.$refs.elTable?.sort(prop, order)
    },

    /** 重新布局 */
    doLayout() {
      this.$refs.elTable?.doLayout()
    },

    // ==================== 高度自适应 ====================
    /**
     * 初始化 ResizeObserver 监听容器尺寸变化
     */
    initResizeObserver() {
      if (typeof ResizeObserver === 'undefined') return

      this._resizeObserver = new ResizeObserver(() => {
        this.calcTableHeight()
      })
      this._resizeObserver.observe(this.$el)
      // 初始计算
      this.$nextTick(() => this.calcTableHeight())
    },

    /**
     * 计算表格高度
     * 表格高度 = 容器高度 - selectionBar高度 - 分页高度
     */
    calcTableHeight() {
      const el = this.$el
      if (!el) return

      const containerHeight = el.clientHeight
      if (!containerHeight) return

      let usedHeight = 0

      // selectionBar 高度
      const selectionBar = el.querySelector('.selection-bar')
      if (selectionBar) {
        usedHeight += selectionBar.offsetHeight
      }

      // 分页高度
      const pagination = el.querySelector('.pagination-container')
      if (pagination) {
        usedHeight += pagination.offsetHeight
      }

      // 计算表格高度（减去一些边距余量）
      const tableHeight = containerHeight - usedHeight - 2

      // 设置最小高度
      this.autoTableHeight = Math.max(tableHeight, 100)
    }
  }
}
</script>

<style lang="scss" scoped>
.pro-table {
  position: relative; // 为 loading 遮罩层提供定位基准

  // ==================== 表格样式 ====================
  .pro-table__inner {
    ::v-deep .el-table__row {
      transition: background-color 0.2s ease;
    }

    ::v-deep th {
      border-bottom: 2px solid #E4E7ED !important;
    }

    // 修复固定列高度计算问题
    ::v-deep .el-table__fixed,
    ::v-deep .el-table__fixed-right {
      // 确保固定列高度与主表格一致
      height: 100% !important;
      
      // // 修复固定列内部容器高度
      // .el-table__fixed-body-wrapper,
      // .el-table__fixed-footer-wrapper {
      //   height: auto !important;
      // }
      
      // // 修复 ::before 伪元素高度
      // &::before {
      //   height: 100% !important;
      // }
    }
  }

  .pro-table__empty {
    padding: 40px 0;
    color: #909399;
    font-size: 14px;
    text-align: center;

    i {
      font-size: 40px;
      margin-bottom: 8px;
      display: block;
    }

    p {
      margin: 0;
    }
  }

  // ==================== 跨页选中提示条 ====================
  .selection-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 16px;
    margin-bottom: 12px;
    background: linear-gradient(135deg, #e8f4fd, #f0f7ff);
    border-radius: 8px;
    border: 1px solid #d4e8fc;
    min-height: 44px; // 固定最小高度，防止抖动

    .selection-bar-left {
      display: flex;
      align-items: center;
      gap: 8px;
      height: 24px; // 固定高度
    }

    .selection-icon {
      font-size: 18px;
      color: #409EFF;
      line-height: 24px; // 固定行高
    }

    .selection-text {
      font-size: 13px;
      color: #606266;
      line-height: 24px; // 固定行高
    }

    .selection-count {
      font-size: 16px;
      color: #409EFF;
      font-weight: 700;
      margin: 0 2px;
      line-height: 24px; // 固定行高
    }

    .selection-bar-right {
      display: flex;
      align-items: center;
      gap: 12px;
      height: 24px; // 固定高度
    }

    .view-detail-link {
      font-size: 13px;
      color: #409EFF;
      cursor: pointer;
      transition: opacity 0.2s;
      line-height: 24px; // 固定行高

      &:hover {
        opacity: 0.8;
      }

      i {
        margin-right: 4px;
      }
    }

    .clear-btn {
      color: #909399;
      font-size: 13px;
      line-height: 24px; // 固定行高

      &:hover {
        color: #F56C6C;
      }
    }
  }

  // ==================== 表格单元格省略 ====================
  .cell-ellipsis {
    display: inline-block;
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  // ==================== 提示条过渡动画 ====================
  .slide-fade-enter-active {
    transition: all 0.3s ease-out;
  }

  .slide-fade-leave-active {
    transition: all 0.2s ease-in;
  }

  .slide-fade-enter,
  .slide-fade-leave-to {
    transform: translateY(-10px);
    opacity: 0;
  }
}
</style>

<!-- 全局样式：popover 渲染在 body 下，scoped 无法穿透 -->
<style lang="scss">
.selected-popover {
  .selected-list {
    max-height: 320px;
    overflow-y: auto;

    .selected-item {
      display: flex;
      align-items: center;
      padding: 8px 12px;
      border-bottom: 1px solid #F0F2F5;
      transition: background-color 0.15s;

      &:last-child {
        border-bottom: none;
      }

      &:hover {
        background-color: #F5F7FA;
      }

      // 主标签（名字）
      .item-label {
        flex: 1;
        font-size: 13px;
        color: #303133;
        font-weight: 500;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        margin-right: 8px;
      }

      // 副标签（部门等 tag）
      .item-sub {
        max-width: 80px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        border-radius: 10px;
        margin-right: 8px;
        flex-shrink: 0;
      }

      // 移除按钮
      .item-remove {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 24px;
        height: 24px;
        border-radius: 50%;
        cursor: pointer;
        color: #C0C4CC;
        font-size: 12px;
        transition: all 0.2s;
        flex-shrink: 0;

        &:hover {
          color: #F56C6C;
          background-color: #fef0f0;
        }

        &:focus {
          outline: 2px solid #409EFF;
          outline-offset: 1px;
        }
      }
    }

    .more-tip {
      padding: 8px 12px;
      font-size: 12px;
      color: #909399;
      text-align: center;
    }
  }
}
</style>
