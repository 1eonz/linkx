<!--
  数据权限穿梭树组件

  功能说明：
  - 左侧：权限树（支持搜索、父子联动/不联动、懒加载/一次性加载）
  - 右侧：已选列表（支持全选、批量移除）
  - 中间：移除按钮
  - 内聚部门数据加载逻辑（DEPARTMENT_SYNC_SIGN 判断）
  - 支持全选/取消按钮（可独立配置）
  - change 事件返回对象数组 [{ id, name, path }]

  Props:
  - value/v-model: 选中节点的ID数组
  - treeData: 外部传入树数据，不传则组件自行加载部门数据
  - treeProps: 树属性配置
  - checkStrictly: 是否父子不联动（默认false联动）
  - defaultExpandAll: 是否默认展开所有节点
  - lazy: 是否懒加载（null=自动判断）
  - load: 自定义懒加载回调（不传则使用默认queryDepartment逻辑）
  - defaultCheckedKeys: 默认选中的key数组
  - showFilter: 是否显示搜索框
  - filterPlaceholder: 搜索框占位文字
  - loading: 外部加载状态（treeData外部传入时使用）
  - height: 容器高度（数字=px，字符串如'100%'=百分比）
  - leftTitle: 左侧面板标题
  - rightTitle: 右侧面板标题
  - showNodeSelectAll: 是否显示全选按钮
  - showNodeCancelAll: 是否显示取消按钮
  - pathSeparator: path路径分隔符
  - departmentSyncSign: 同步标识（null=从localStorage读取）

  Events:
  - input: v-model双向绑定，返回ID数组
  - change: 选中项变化时触发，返回对象数组 [{ id, name, path }]

  Methods:
  - getCheckedKeys(): 获取选中key列表
  - getCheckedNodes(): 获取选中节点列表
  - getCheckedDetail(): 获取选中详情 [{ id, name, path }]
  - setCheckedKeys(keys): 设置选中项
  - filter(val): 过滤树节点
  - reset(): 重置组件状态
-->
<template>
  <div class="data-permission-tree" :style="{ height: computedHeight }">
    <!-- 左侧：权限树 -->
    <div class="panel left-panel">
      <div class="panel-header">
        <i class="el-icon-s-grid panel-icon"></i>
        <span class="panel-title">{{ leftTitle }}</span>
        <span v-if="selectedNodes.length > 0" class="panel-count">
          {{ selectedNodes.length }}
        </span>
        <el-input
          v-if="showFilter"
          v-model="filterText"
          :placeholder="filterPlaceholder"
          prefix-icon="el-icon-search"
          clearable
          size="mini"
          class="filter-input"
        />
      </div>
      <div v-loading="actualLoading" class="panel-content">
        <VirtualTree
          ref="treeRef"
          :data="actualTreeData"
          :props="treeProps"
          :node-key="'id'"
          :show-checkbox="true"
          :check-strictly="checkStrictly"
          :default-expand-all="defaultExpandAll || !actualLazy"
          :default-checked-keys="defaultCheckedKeys"
          :lazy="actualLazy"
          :load="actualLoad"
          height="100%"
          :filter-text="filterText"
          :item-size="34"
          @check="onTreeCheck"
          @lazy-load="onLazyLoad"
        >
          <template slot-scope="{ node, data }">
            <span class="custom-tree-node">
              <i v-if="data && data.icon" :class="data.icon" class="node-icon" />
              <span class="node-label" :title="node.label">{{ node.label }}</span>
              <el-tag
                v-if="data && data.url"
                size="mini"
                type="info"
                class="node-tag"
              >
                {{ data.url }}
              </el-tag>
              <!-- 全选/取消按钮 -->
              <span
                v-if="!actualLazy && (showNodeSelectAll || showNodeCancelAll) && data && data.children && data.children.length > 0"
                class="node-actions"
                @click.stop
                @mousedown.stop
              >
                <span
                  v-if="showNodeSelectAll"
                  class="action-btn"
                  @click.stop="selectAllDescendants(data)"
                >
                  全选
                </span>
                <span
                  v-if="showNodeCancelAll"
                  class="action-btn"
                  @click.stop="cancelAllDescendants(data)"
                >
                  取消
                </span>
              </span>
            </span>
          </template>
        </VirtualTree>
        <!-- 空状态 -->
        <div v-if="!actualLoading && actualTreeData.length === 0 && !actualLazy" class="empty-state">
          <div class="empty-icon-wrapper">
            <i class="el-icon-folder-opened"></i>
          </div>
          <p>暂无数据</p>
        </div>
      </div>
    </div>

    <!-- 中间操作按钮 -->
    <div class="transfer-buttons">
      <el-button
        circle
        icon="el-icon-arrow-left"
        :disabled="checkList.length === 0"
        @click="removeSelected"
      />
    </div>

    <!-- 右侧：已选列表 -->
    <div class="panel right-panel">
      <div class="panel-header">
        <el-checkbox
          v-model="checkAllFlag"
          :indeterminate="indeterminate"
          @change="handleCheckAll"
        />
        <span class="panel-title">{{ rightTitle }}</span>
        <span v-if="selectedNodes.length > 0" class="panel-count">
          {{ checkList.length }}/{{ selectedNodes.length }}
        </span>
      </div>
      <div class="panel-content">
        <VirtualCheckboxList
          v-if="selectedNodes.length > 0"
          :data="selectedNodes"
          :checked-ids="checkList"
          height="100%"
          :item-size="34"
          @check-change="onRightCheckChange"
        />
        <!-- 右侧空状态 -->
        <div v-if="selectedNodes.length === 0" class="empty-state">
          <div class="empty-icon-wrapper">
            <i class="el-icon-document-checked"></i>
          </div>
          <p>暂未选择</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { queryDepartment, queryDepartmentTree } from '@/api/h5/collaboration'
import VirtualTree from '@/components/VirtualTree'
import VirtualCheckboxList from '@/components/VirtualCheckboxList'

export default {
  name: 'DataPermissionTree',

  components: { VirtualTree, VirtualCheckboxList },

  model: {
    prop: 'value',
    event: 'input'
  },

  props: {
    // 选中节点的ID数组（v-model）
    value: {
      type: Array,
      default: () => []
    },
    // 外部传入树数据，不传则组件自行加载部门数据
    treeData: {
      type: Array,
      default: null
    },
    // 树属性配置
    treeProps: {
      type: Object,
      default: () => ({
        children: 'children',
        label: 'name'
      })
    },
    // 是否父子不联动
    checkStrictly: {
      type: Boolean,
      default: false
    },
    // 是否默认展开所有节点
    defaultExpandAll: {
      type: Boolean,
      default: false
    },
    // 是否懒加载（null=自动判断）
    lazy: {
      type: Boolean,
      default: null
    },
    // 自定义懒加载回调
    load: {
      type: Function,
      default: null
    },
    // 默认选中的key数组（仅对同步传入 treeData 场景有效，异步加载场景请使用 syncFromDetail/initCheckedKeys）
    defaultCheckedKeys: {
      type: Array,
      default: () => []
    },
    // 是否显示搜索框
    showFilter: {
      type: Boolean,
      default: false
    },
    // 搜索框占位文字
    filterPlaceholder: {
      type: String,
      default: '搜索权限'
    },
    // 外部加载状态（treeData外部传入时使用）
    loading: {
      type: Boolean,
      default: false
    },
    // 容器高度（数字表示px，字符串如'100%'表示百分比）
    height: {
      type: [Number, String],
      default: 500
    },
    // 左侧面板标题
    leftTitle: {
      type: String,
      default: '权限列表'
    },
    // 右侧面板标题
    rightTitle: {
      type: String,
      default: '已选择'
    },
    // 是否显示全选按钮
    showNodeSelectAll: {
      type: Boolean,
      default: false
    },
    // 是否显示取消按钮
    showNodeCancelAll: {
      type: Boolean,
      default: false
    },
    // path路径分隔符
    pathSeparator: {
      type: String,
      default: '/'
    },
    // 同步标识（null=从localStorage读取）
    departmentSyncSign: {
      type: Boolean,
      default: null
    }
  },

  data() {
    return {
      filterText: '',
      checkAllFlag: false,
      indeterminate: false,
      checkList: [],
      selectedNodes: [],
      // 解析后的同步标识
      resolvedSyncSign: false,
      // 组件内部树数据
      internalTreeData: [],
      // 组件内部加载状态
      internalLoading: false,
      // 懒加载模式下缓存的完整树
      allRoleNode: [],
      // 懒加载模式下未加载的占位节点详情 { id: { id, name, icon } }
      pendingDetails: {},
      // 是否跳过 handleCheck 处理（initCheckedDetail 中设置选中时使用）
      skipHandleCheck: false
    }
  },

  computed: {
    // 计算实际高度值
    computedHeight() {
      return typeof this.height === 'number' ? this.height + 'px' : this.height
    },
    // 实际使用的树数据
    actualTreeData() {
      return this.treeData !== null ? this.treeData : this.internalTreeData
    },
    // 实际使用的加载状态
    actualLoading() {
      return this.treeData !== null ? this.loading : this.internalLoading
    },
    // 实际是否懒加载
    actualLazy() {
      if (this.lazy !== null) return this.lazy
      return !this.resolvedSyncSign
    },
    // 实际懒加载回调
    actualLoad() {
      if (this.load !== null) return this.load
      return this.actualLazy ? this.loadDefaultNode : null
    }
  },

  created() {
    this.resolveSyncSign()
    // 外部未传入treeData时，组件自行加载
    if (this.treeData === null) {
      this.loadInternalTreeData()
    }
  },

  watch: {
    filterText() {
      // VirtualTree 通过 prop 监听 filterText，无需手动调用 filter
    },
    // 树数据变化时，刷新占位节点的名称
    actualTreeData() {
      if (Object.keys(this.pendingDetails).length > 0) {
        this.$nextTick(() => {
          this.refreshPendingNodes()
        })
      }
    }
  },

  methods: {
    /**
     * 解析同步标识
     */
    resolveSyncSign() {
      if (this.departmentSyncSign !== null) {
        this.resolvedSyncSign = this.departmentSyncSign
        return
      }
      // 从 localStorage 读取
      try {
        const globalConfig = JSON.parse(localStorage.getItem('globalConfig'))
        if (globalConfig) {
          this.resolvedSyncSign =
            globalConfig.DEPARTMENT_SYNC_SIGN === 'true' ||
            globalConfig.DEPARTMENT_SYNC_SIGN === true
        }
      } catch (e) {
        this.resolvedSyncSign = false
      }
    },

    /**
     * 组件内部加载部门树数据
     */
    async loadInternalTreeData() {
      if (this.resolvedSyncSign) {
        // 同步模式：一次性加载全部部门
        this.internalLoading = true
        try {
          const res = await queryDepartmentTree({})
          if (res && res.code === 0 && res.data) {
            const treeData = Array.isArray(res.data) ? res.data : [res.data]
            this.internalTreeData = this.normalizeTreeData(treeData)
            this.allRoleNode = this.internalTreeData
          }
        } catch (e) {
          // 忽略异常
        } finally {
          this.internalLoading = false
        }
      } else {
        // 懒加载模式：只加载根节点数据，子节点由 loadDefaultNode 逐级加载
        this.internalLoading = true
        try {
          const res = await queryDepartment({})
          if (res && res.code === 0 && res.data) {
            const rootData = Array.isArray(res.data) ? res.data : [res.data]
            // 根节点不设置 children，由展开时懒加载
            this.internalTreeData = rootData.map(node => ({
              ...node,
              children: []
            }))
            this.allRoleNode = this.saveAndBuildTree(this.allRoleNode, null, rootData)
          }
        } catch (e) {
          // 忽略异常
        } finally {
          this.internalLoading = false
        }
      }
    },

    /**
     * 规范化树数据，确保 children 为数组
     */
    normalizeTreeData(nodes) {
      return nodes.map(node => ({
        ...node,
        children:
          node.children !== null && Array.isArray(node.children)
            ? this.normalizeTreeData(node.children)
            : []
      }))
    },

    /**
     * 默认懒加载回调
     */
    async loadDefaultNode(rawData, resolve) {
      const parentCode = rawData?.code || ''
      try {
        const params = parentCode ? { parentCode } : {}
        const res = await queryDepartment(params)
        const data = res && res.code === 0 && res.data ? res.data : []
        // 缓存到 allRoleNode
        if (!rawData || !rawData.id) {
          this.allRoleNode = this.saveAndBuildTree(this.allRoleNode, null, data)
        } else if (data.length > 0) {
          this.allRoleNode = this.saveAndBuildTree(this.allRoleNode, rawData.code, data)
        }
        // 懒加载子节点渲染后，检查 pendingDetails 中是否有已加载的节点需要勾选
        this.$nextTick(() => {
          this.refreshPendingNodes()
        })
        return resolve(data)
      } catch (e) {
        return resolve([])
      }
    },

    /**
     * VirtualTree 懒加载完成事件
     */
    onLazyLoad(rawData, children) {
      // 懒加载子节点渲染后，检查 pendingDetails
      this.$nextTick(() => {
        this.refreshPendingNodes()
      })
    },

    /**
     * 保存并构建树形结构（懒加载模式缓存用）
     * 优化：仅对目标路径做浅拷贝，避免全量深拷贝
     */
    saveAndBuildTree(savedData, targetCode, newlyFetchedData) {
      const normalizedNewData = newlyFetchedData.map(node => ({
        ...node,
        children:
          node.children !== null && Array.isArray(node.children)
            ? node.children
            : []
      }))

      if (savedData.length === 0) {
        return normalizedNewData
      }

      // 浅拷贝顶层避免修改原数据
      const result = savedData.map(node => ({ ...node }))

      if (targetCode === null || targetCode === undefined) {
        // 根节点，直接追加
        result.push(...normalizedNewData)
        return result
      }

      // 定位目标节点并追加子节点
      const target = this.findNodeByCode(result, targetCode)
      if (target) {
        target.children = [...(target.children || []), ...normalizedNewData]
      }

      return result
    },

    /**
     * 根据 code 查找节点
     */
    findNodeByCode(treeData, code) {
      for (const node of treeData) {
        if (node.code === code) return node
        if (node.children && node.children.length > 0) {
          const found = this.findNodeByCode(node.children, code)
          if (found) return found
        }
      }
      return null
    },

    /**
     * VirtualTree check 事件
     * 合并已加载节点和未加载的占位节点
     */
    onTreeCheck(rawData, info) {
      if (this.skipHandleCheck) return

      this.checkList = []
      this.checkAllFlag = false
      this.indeterminate = false

      const tree = this.$refs.treeRef
      if (!tree) return

      // 获取已勾选的节点
      const checkedKeys = tree.getCheckedKeys()
      const checkedNodes = tree.getCheckedNodes()
      const loadedMap = {}
      const loadedNodes = checkedNodes.map(node => {
        loadedMap[node.id] = true
        return {
          id: node.id,
          name: node[this.treeProps.label || 'name'],
          icon: node.icon
        }
      })

      // 保留未加载的占位节点（从 pendingDetails 中移除已加载的）
      const newPending = {}
      Object.keys(this.pendingDetails).forEach(id => {
        if (!loadedMap[id]) {
          newPending[id] = this.pendingDetails[id]
        }
      })
      this.pendingDetails = newPending

      // 重新添加剩余的占位节点
      const pendingNodes = Object.values(this.pendingDetails).map(detail => ({
        id: detail.id,
        name: detail.name,
        icon: detail.icon || '',
        isPending: true
      }))

      // 合并：已加载在前，占位在后
      this.selectedNodes = [...loadedNodes, ...pendingNodes]

      const keys = this.selectedNodes.map(n => n.id)
      const detail = this.buildCheckedDetailWithPending(checkedNodes)

      this.$emit('input', keys)
      this.$emit('change', detail)
    },

    /**
     * 右侧 checkbox 变化
     */
    onRightCheckChange(item, val) {
      if (val) {
        if (!this.checkList.includes(item.id)) {
          this.checkList.push(item.id)
        }
      } else {
        this.checkList = this.checkList.filter(id => id !== item.id)
      }
      this.updateCheckAllState()
    },

    /**
     * 刷新占位节点（树数据加载完成后，将已加载的占位节点替换为真实名称）
     * 同时在树中勾选已加载的节点
     */
    refreshPendingNodes() {
      const tree = this.$refs.treeRef
      if (!tree || Object.keys(this.pendingDetails).length === 0) return

      // 检查哪些 pendingDetails 已在树中加载
      const stillPending = {}
      const newlyLoaded = []
      Object.keys(this.pendingDetails).forEach(id => {
        const node = tree.getNode(id)
        if (node && node.data) {
          newlyLoaded.push(node.data)
        } else {
          stillPending[id] = this.pendingDetails[id]
        }
      })

      if (newlyLoaded.length === 0) return

      // 更新 pendingDetails
      this.pendingDetails = stillPending

      // 在树中勾选已加载的节点
      // 暂停 handleCheck，避免 setCheckedKeys 触发的 check 事件覆盖 selectedNodes
      this.skipHandleCheck = true
      const currentCheckedKeys = tree.getCheckedKeys()
      const newlyLoadedIds = newlyLoaded.map(data => data.id)
      const allCheckedKeys = [...new Set([...currentCheckedKeys, ...newlyLoadedIds])]
      tree.setCheckedKeys(allCheckedKeys)
      // 恢复 handleCheck，并触发一次完整的状态同步
      this.skipHandleCheck = false

      // 重新构建 selectedNodes：移除旧的占位项，合并已加载的真实节点
      const existingIds = new Set()
      const loadedNodes = []

      // 保留已有的已加载节点
      this.selectedNodes.forEach(item => {
        if (!item.isPending) {
          existingIds.add(item.id)
          loadedNodes.push(item)
        }
      })

      // 添加新加载的节点
      newlyLoaded.forEach(data => {
        if (!existingIds.has(data.id)) {
          existingIds.add(data.id)
          loadedNodes.push({
            id: data.id,
            name: data[this.treeProps.label || 'name'],
            icon: data.icon
          })
        }
      })

      // 重新添加剩余的占位节点
      const pendingNodes = Object.values(this.pendingDetails).map(detail => ({
        id: detail.id,
        name: detail.name,
        icon: detail.icon || '',
        isPending: true
      }))

      this.selectedNodes = [...loadedNodes, ...pendingNodes]

      // 同步事件
      const keys = this.selectedNodes.map(n => n.id)
      const detail = this.buildCheckedDetailWithPending()
      this.$emit('input', keys)
      this.$emit('change', detail)
    },

    /**
     * 构建包含未加载节点的详情列表
     * 未加载的节点 name 为空字符串
     */
    buildCheckedDetailWithPending(cachedCheckedNodes) {
      const tree = this.$refs.treeRef
      const result = []

      // 已加载的节点，正常构建 { id, name, path }
      // 优先使用缓存的 checkedNodes，避免重复调用 getCheckedNodes
      const checkedNodes = cachedCheckedNodes || (tree ? tree.getCheckedNodes() : [])
      checkedNodes.forEach(node => {
        result.push({
          id: node.id,
          name: node[this.treeProps.label || 'name'],
          path: this.getNodePath(node.id)
        })
      })

      // 未加载的占位节点，使用 pendingDetails 中的名称
      const loadedIds = new Set(result.map(item => item.id))
      Object.keys(this.pendingDetails).forEach(id => {
        if (!loadedIds.has(id)) {
          const detail = this.pendingDetails[id]
          result.push({
            id: id,
            name: detail ? detail.name : '',
            path: ''
          })
        }
      })

      return result
    },

    /**
     * 获取节点的path（从根到父级的id路径）
     */
    getNodePath(nodeId) {
      const tree = this.$refs.treeRef
      if (!tree) return ''

      const node = tree.getNode(nodeId)
      if (!node || !node.parent || node.level <= 1) return ''

      const ancestorIds = []
      let current = node.parent
      while (current && current.level > 0) {
        // current 是 flatNode 对象，直接使用 id 属性
        ancestorIds.unshift(current.id)
        current = current.parent
      }

      return ancestorIds.join(this.pathSeparator)
    },

    /**
     * 递归获取节点所有子孙ID
     */
    getDescendantIds(data) {
      const result = []
      const traverse = (node) => {
        if (node.children && node.children.length > 0) {
          node.children.forEach(child => {
            result.push(child.id)
            traverse(child)
          })
        }
      }
      traverse(data)
      return result
    },

    /**
     * 全选当前节点及所有子孙节点
     */
    selectAllDescendants(data) {
      const tree = this.$refs.treeRef
      if (!tree) return

      const currentKeys = tree.getCheckedKeys()
      const descendantIds = this.getDescendantIds(data)
      const result = [...new Set([data.id, ...descendantIds, ...currentKeys])]
      tree.setCheckedKeys(result)
      this.onTreeCheck(data, { checkedKeys: result })
    },

    /**
     * 取消当前节点及所有子孙节点
     */
    cancelAllDescendants(data) {
      const tree = this.$refs.treeRef
      if (!tree) return

      const currentKeys = tree.getCheckedKeys()
      const descendantIds = this.getDescendantIds(data)
      const removeIds = [data.id, ...descendantIds]
      const result = currentKeys.filter(key => !removeIds.includes(key))
      tree.setCheckedKeys(result)
      this.onTreeCheck(data, { checkedKeys: result })
    },

    /**
     * 右侧全选/取消全选
     */
    handleCheckAll(val) {
      this.checkList = val ? this.selectedNodes.map(n => n.id) : []
      this.indeterminate = false
    },

    /**
     * 右侧勾选变化
     */
    updateCheckAllState() {
      const total = this.selectedNodes.length
      if (total === 0) {
        this.checkAllFlag = false
        this.indeterminate = false
      } else {
        this.checkAllFlag = this.checkList.length === total
        this.indeterminate = this.checkList.length > 0 && this.checkList.length < total
      }
    },

    /**
     * 移除右侧选中的节点
     */
    removeSelected() {
      const tree = this.$refs.treeRef
      if (!tree || this.checkList.length === 0) return

      // 从树中取消勾选
      const currentKeys = tree.getCheckedKeys()
      const result = currentKeys.filter(key => !this.checkList.includes(key))
      tree.setCheckedKeys(result)

      // 同时移除 pendingDetails 中被选中的占位节点
      const newPending = {}
      Object.keys(this.pendingDetails).forEach(id => {
        if (!this.checkList.includes(id)) {
          newPending[id] = this.pendingDetails[id]
        }
      })
      this.pendingDetails = newPending

      this.checkList = []
      this.checkAllFlag = false
      this.indeterminate = false
      this.onTreeCheck(null, { checkedKeys: result })
    },

    /**
     * 获取缓存的完整树数据（懒加载模式下使用）
     * @returns {Array} 树数据数组
     */
    getAllRoleNode() {
      return this.allRoleNode || []
    },

    /**
     * 高阶回显方法：根据数据自动判断格式并初始化选中状态
     * 优先使用 orgList（新格式 [{id, name, path}]），其次使用 orgIds（旧格式纯ID数组）
     * @param {Object} options - { orgList?: Array, orgIds?: Array }
     * @returns {Array} 转换后的新格式数据 [{ id, name, path }]
     */
    syncFromDetail({ orgList, orgIds }) {
      if (orgList && orgList.length > 0) {
        const normalized = this.initCheckedDetail(orgList)
        return normalized || []
      }
      if (orgIds && orgIds.length > 0) {
        this.initCheckedKeys(orgIds)
        return []
      }
      return []
    },

    /**
     * 获取选中的key列表
     */
    getCheckedKeys() {
      const tree = this.$refs.treeRef
      return tree ? tree.getCheckedKeys() : []
    },

    /**
     * 获取选中的节点列表
     */
    getCheckedNodes() {
      const tree = this.$refs.treeRef
      return tree ? tree.getCheckedNodes() : []
    },

    /**
     * 获取选中详情 [{ id, name, path }]
     * 包含未加载的占位节点（name 为空）
     */
    getCheckedDetail() {
      return this.buildCheckedDetailWithPending()
    },

    /**
     * 通过纯ID数组初始化选中状态（用于旧数据 orgIds 回显）
     * 懒加载模式下，未加载的节点显示为占位项（name 显示为 ID）
     * 展开后自动刷新为真实名称
     * @param {Array} keys - ID数组，如 ["id1", "id2"]
     */
    initCheckedKeys(keys) {
      if (!keys || !keys.length) return

      const tree = this.$refs.treeRef
      if (!tree) return

      // 暂停 handleCheck，避免 setCheckedKeys 触发的 check 事件用中间状态覆盖
      this.skipHandleCheck = true

      // 用 getNode 判断哪些 key 在树中真实存在（懒加载模式下节点可能未加载）
      const existingIds = new Set()
      const missingKeys = []
      keys.forEach(id => {
        const node = tree.getNode(id)
        if (node && node.data) {
          existingIds.add(id)
        } else {
          missingKeys.push(id)
        }
      })

      // 只对已存在的节点设置勾选
      tree.setCheckedKeys([...existingIds])

      // 未加载的节点加入 pendingDetails
      const newPending = {}
      missingKeys.forEach(key => {
        newPending[key] = { id: key, name: String(key), icon: '' }
      })
      this.pendingDetails = newPending

      // 恢复 handleCheck，并触发一次完整的状态同步
      this.skipHandleCheck = false
      this.$nextTick(() => {
        this.onTreeCheck(null, { checkedKeys: tree.getCheckedKeys() })
      })
    },

    /**
     * 设置选中的节点
     */
    setCheckedKeys(keys) {
      const tree = this.$refs.treeRef
      if (!tree) return
      tree.setCheckedKeys(keys)
      this.onTreeCheck(null, { checkedKeys: keys })
    },

    /**
     * 通过详情列表初始化选中状态（用于编辑回显）
     * 同时恢复树勾选和右侧已选列表
     * 兼容两种数据格式：
     *   新格式：[{ id, name, path }]
     *   旧格式：[{ id, name, children: [...] }]（树结构）
     * @param {Array} detail - 新格式或旧格式的数据
     * @returns {Array} 转换后的新格式数据 [{ id, name, path }]
     */
    initCheckedDetail(detail) {
      if (!detail || !detail.length) return []

      // 检测并转换数据格式
      const normalizedDetail = this.normalizeImOrgPrivJson(detail)

      // 暂停 handleCheck，避免 setCheckedKeys 触发的 check 事件用中间状态覆盖 selectedNodes
      this.skipHandleCheck = true

      // 重置状态
      this.checkList = []
      this.checkAllFlag = false
      this.indeterminate = false
      this.pendingDetails = {}

      // 设置右侧已选列表
      this.selectedNodes = normalizedDetail.map(item => ({
        id: item.id,
        name: item.name,
        icon: item.icon || ''
      }))

      // 设置树勾选
      const keys = normalizedDetail.map(item => item.id)
      const tree = this.$refs.treeRef
      if (tree) {
        // 用 getNode 判断哪些 key 在树中真实存在（懒加载模式下节点可能未加载）
        const existingIds = new Set()
        const missingKeys = []
        keys.forEach(id => {
          const node = tree.getNode(id)
          if (node && node.data) {
            existingIds.add(id)
          } else {
            missingKeys.push(id)
          }
        })

        // 只对已存在的节点设置勾选
        tree.setCheckedKeys([...existingIds])

        // 未加载的节点加入 pendingDetails
        const newPending = {}
        missingKeys.forEach(id => {
          const item = normalizedDetail.find(d => d.id === id)
          newPending[id] = {
            id: id,
            name: item ? item.name : String(id),
            icon: item ? (item.icon || '') : ''
          }
        })
        this.pendingDetails = newPending
      } else {
        // 树不存在，所有节点加入 pendingDetails
        const newPending = {}
        normalizedDetail.forEach(item => {
          newPending[item.id] = {
            id: item.id,
            name: item.name,
            icon: item.icon || ''
          }
        })
        this.pendingDetails = newPending
      }

      // 恢复 handleCheck，并触发一次完整的状态同步
      this.skipHandleCheck = false
      this.$nextTick(() => {
        this.onTreeCheck(null, { checkedKeys: tree ? tree.getCheckedKeys() : [] })
      })

      return normalizedDetail
    },

    /**
     * 检测并转换 imOrgPrivJson 数据格式
     * 旧格式（树结构）→ 新格式（扁平数组 [{id, name, path}]）
     * 新格式直接返回
     * @param {Array} data - imOrgPrivJson 数据
     * @returns {Array} 新格式数据 [{ id, name, path }]
     */
    normalizeImOrgPrivJson(data) {
      if (!data || !data.length) return []

      // 判断是否为旧格式（树结构）：第一项有 children 属性且是数组
      const isOldFormat = data.some(item =>
        item.children !== undefined && item.children !== null
      )

      if (!isOldFormat) {
        // 新格式，直接返回
        return data
      }

      // 旧格式，递归展平为 [{ id, name, path }]
      const result = []
      const flatten = (nodes, ancestorPath) => {
        nodes.forEach(node => {
          result.push({
            id: node.id,
            name: node.name,
            path: ancestorPath
          })
          if (node.children && Array.isArray(node.children) && node.children.length > 0) {
            const childPath = ancestorPath
              ? ancestorPath + this.pathSeparator + node.id
              : String(node.id)
            flatten(node.children, childPath)
          }
        })
      }
      flatten(data, '')

      return result
    },

    /**
     * 过滤树节点
     */
    filter(val) {
      this.filterText = val
    },

    /**
     * 重置组件状态
     */
    reset() {
      this.filterText = ''
      this.checkAllFlag = false
      this.indeterminate = false
      this.checkList = []
      this.selectedNodes = []
      this.pendingDetails = {}
      this.skipHandleCheck = false

      const tree = this.$refs.treeRef
      if (tree) {
        tree.setCheckedKeys([])
        tree.filter('')
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.data-permission-tree {
  display: flex;
  gap: 0;
}

// 面板通用样式
.panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #E8ECF1;
  border-radius: 6px;
  background: #FAFBFC;
  transition: border-color 0.2s ease;

  &:hover {
    border-color: #C0C4CC;
  }
}

// 面板头部
.panel-header {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  background: linear-gradient(135deg, #f5f7fa, #eef1f6);
  border-bottom: 1px solid #E8ECF1;
  gap: 8px;
  flex-shrink: 0;
}

.panel-icon {
  font-size: 14px;
  color: #409EFF;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.panel-count {
  font-size: 12px;
  color: #909399;
  background: #EBEEF5;
  padding: 1px 8px;
  border-radius: 10px;
  line-height: 18px;
}

.filter-input {
  flex: 1;
  margin-left: auto;

  ::v-deep .el-input__inner {
    border-radius: 14px;
  }
}

// 面板内容
.panel-content {
  flex: 1;
  overflow: auto;
  position: relative;
}

// 中间操作按钮
.transfer-buttons {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  gap: 8px;
}

// 树节点样式
.custom-tree-node {
  display: flex;
  align-items: center;
  font-size: 14px;
  white-space: nowrap;

  .node-icon {
    margin-right: 6px;
    color: #409EFF;
    font-size: 14px;
    flex-shrink: 0;
  }

  .node-label {
    overflow: visible;
    white-space: nowrap;
    color: #303133;
  }

  .node-tag {
    margin-left: 8px;
    max-width: 120px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    border-radius: 10px;
    flex-shrink: 0;
  }

  .node-actions {
    position: sticky;
    right: 0;
    flex-shrink: 0;
    margin-left: 8px;
    display: none;
    background: inherit;
    padding: 2px 0 2px 8px;

    .action-btn {
      display: inline-block;
      padding: 2px 4px;
      font-size: 12px;
      color: #409EFF;
      cursor: pointer;
      background: #fff;
      border-radius: 3px;

      &:hover {
        color: #66B1FF;
      }

      & + .action-btn {
        margin-left: 4px;
      }
    }
  }
}

// 整行 hover 时显示全选/取消按钮
::v-deep .virtual-tree__node {
  &:hover .node-actions {
    display: inline-flex;

    .action-btn {
      background: #f5f7fa;
    }
  }

  &.is-checked .node-actions {
    .action-btn {
      background: #f0f7ff;
    }
  }

  &.is-checked:hover .node-actions {
    .action-btn {
      background: #f0f7ff;
    }
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 50px 0;
  color: #909399;

  .empty-icon-wrapper {
    width: 52px;
    height: 52px;
    border-radius: 50%;
    background: linear-gradient(135deg, #e8f4fd, #f0f7ff);
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 12px;

    i {
      font-size: 24px;
      color: #409EFF;
    }
  }

  p {
    margin: 0;
    font-size: 13px;
    color: #909399;
  }
}

// VirtualTree 样式覆盖
::v-deep .virtual-tree {
  border: none;
  border-radius: 0;

  &__node {
    height: 34px;
    border-radius: 4px;
    margin-bottom: 1px;
    padding-right: 8px;

    &:hover {
      background-color: #F5F7FA;
    }
  }

  // 复选框间距调整
  .el-checkbox {
    margin-right: 6px;
  }
}
</style>
