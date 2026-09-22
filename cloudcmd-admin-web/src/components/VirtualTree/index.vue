<!--
  虚拟滚动树组件

  功能说明：
  - 虚拟滚动渲染，只渲染可视区域内的 DOM 节点，支持万级数据量
  - 支持复选框（父子联动/不联动）
  - 支持懒加载（lazy + load 回调）
  - 支持搜索过滤
  - 支持 scoped slot 自定义节点内容
  - 兼容 el-tree 常用 API（getCheckedKeys、setCheckedKeys、getNode 等）

  Props:
  - data: 树数据数组
  - props: 树属性配置 { label, children, isLeaf }
  - showCheckbox: 是否显示复选框
  - checkStrictly: 是否父子不联动
  - nodeKey: 节点唯一标识字段名
  - defaultCheckedKeys: 默认选中的key数组
  - defaultExpandAll: 是否默认展开所有节点
  - defaultExpandKeys: 默认展开的key数组
  - height: 容器高度（px）
  - itemSize: 每行高度（px）
  - filterText: 搜索关键词
  - lazy: 是否懒加载
  - load: 懒加载回调函数
  - currentKey: 当前高亮节点key
  - disabledFn: 禁用节点判断函数

  Events:
  - check: 勾选变化时触发
  - node-click: 节点点击时触发
  - node-expand: 节点展开/收起时触发
  - expand-change: 展开key变化时触发
  - lazy-load: 懒加载完成时触发

  Methods:
  - getNode(id): 获取节点信息
  - getCheckedKeys(): 获取选中key列表
  - getCheckedNodes(): 获取选中节点列表
  - setCheckedKeys(keys): 设置选中项
  - setChecked(key, checked): 设置单个节点选中状态
  - expandAll(): 展开所有节点
  - setExpandKeys(keys): 设置展开的节点
  - filter(val): 过滤树节点
  - reset(): 重置组件状态
-->
<template>
  <div class="virtual-tree" ref="container" :style="containerStyle">
    <!-- 占位元素，用于撑开滚动高度 -->
    <div class="virtual-tree__phantom" :style="{ height: totalHeight + 'px' }"></div>
    <!-- 可视区域内容，通过 transform 实现滚动偏移 -->
    <div class="virtual-tree__content" :style="{ transform: `translateY(${offset}px)` }">
      <template v-for="item in visibleData">
        <div :key="item._uid" class="virtual-tree__node"
          :class="{ 'is-checked': isChecked(item.id), 'is-current': currentKey === item.id, 'is-disabled': isNodeDisabled(item) }"
          @click="handleNodeClick(item)">
          <!-- 缩进占位 -->
          <span class="virtual-tree__indent" :style="{ width: item.level * 18 + 'px' }"></span>
          <!-- 展开/收起图标 -->
          <span class="virtual-tree__expand"
            :class="{ 'is-leaf': isLeafNode(item), 'is-expanded': item.expanded, 'is-loading': item.loading }"
            @click.stop="toggleExpand(item)">
            <i v-if="item.loading" class="el-icon-loading"></i>
            <i v-else-if="!isLeafNode(item)" class="el-icon-caret-right"></i>
          </span>

          <!-- 复选框 -->
          <el-checkbox
            v-if="showCheckbox"
            :value="isChecked(item.id)"
            :indeterminate="isIndeterminate(item.id)"
            :disabled="isNodeDisabled(item)"
            @change="(val) => handleCheck(item, val)"
            @click.native.stop
          />

          <!-- 节点内容：优先使用 scoped slot，否则使用默认 label -->
          <span class="virtual-tree__content-wrapper">
            <slot :node="item" :data="item.raw">
              <i v-if="item.raw && item.raw.icon" :class="item.raw.icon" class="virtual-tree__icon" />
              <span class="virtual-tree__label" :title="item.label">{{ item.label }}</span>
            </slot>
          </span>
        </div>
      </template>
    </div>
  </div>
</template>

<script>
let uid = 0

export default {
  name: 'VirtualTree',

  props: {
    // 树数据数组
    data: {
      type: Array,
      default: () => []
    },
    // 树属性配置
    props: {
      type: Object,
      default: () => ({
        label: 'label',
        children: 'children',
        isLeaf: 'isLeaf'
      })
    },
    // 是否显示复选框
    showCheckbox: {
      type: Boolean,
      default: false
    },
    // 是否父子不联动
    checkStrictly: {
      type: Boolean,
      default: false
    },
    // 节点唯一标识字段名
    nodeKey: {
      type: String,
      default: 'id'
    },
    // 默认选中的key数组
    defaultCheckedKeys: {
      type: Array,
      default: () => []
    },
    // 是否默认展开所有节点
    defaultExpandAll: {
      type: Boolean,
      default: false
    },
    // 默认展开的key数组
    defaultExpandKeys: {
      type: Array,
      default: () => []
    },
    // 容器高度（px数字或'100%'字符串）
    height: {
      type: [Number, String],
      default: 400
    },
    // 每行高度（px）
    itemSize: {
      type: Number,
      default: 26
    },
    // 搜索关键词
    filterText: {
      type: String,
      default: ''
    },
    // 是否懒加载
    lazy: {
      type: Boolean,
      default: false
    },
    // 懒加载回调
    load: {
      type: Function,
      default: null
    },
    // 当前高亮节点key
    currentKey: {
      type: [String, Number],
      default: null
    },
    // 禁用节点判断函数
    disabledFn: {
      type: Function,
      default: null
    }
  },

  data() {
    return {
      // 扁平化后的树数据（包含展开的子节点）
      flatData: [],
      // 当前可视区域的数据
      visibleData: [],
      // 已展开的节点key集合
      expandedKeys: new Set(),
      // 已勾选的节点key集合
      checkedKeys: new Set(),
      // 半选状态的节点key集合
      indeterminateKeys: new Set(),
      // 滚动偏移量
      offset: 0,
      // 当前滚动位置
      scrollTop: 0,
      // 是否初始化完成
      isReady: false,
      // 节点映射表 id → flatNode
      nodeMap: new Map(),
      // 是否已初始化展开状态
      hasInitializedExpand: false
    }
  },

  computed: {
    // 虚拟滚动总高度
    totalHeight() {
      return this.flatData.length * this.itemSize
    },
    // 容器样式
    containerStyle() {
      if (typeof this.height === 'string') {
        return { height: this.height }
      }
      return { height: this.height + 'px' }
    },
    // 获取实际像素高度
    pixelHeight() {
      if (typeof this.height === 'number') {
        return this.height
      }
      // 字符串模式，从 DOM 获取实际高度
      if (this.$refs.container) {
        return this.$refs.container.clientHeight
      }
      return 400
    }
  },

  watch: {
    // 监听数据变化，重新初始化
    data: {
      handler(newVal, oldVal) {
        if (newVal && newVal.length) {
          // 首次初始化（oldVal 为 undefined）或数据引用完全变化时，完整重建
          if (!oldVal || this.flatData.length === 0) {
            this.initData()
          } else {
            // 数据内容变化（如懒加载添加子节点），保留展开状态重建
            this.rebuildFlatData()
          }
        } else {
          this.flatData = []
          this.visibleData = []
          this.nodeMap.clear()
          this.expandedKeys.clear()
        }
      },
      immediate: true
    },
    // 监听默认选中key变化
    defaultCheckedKeys: {
      handler(keys) {
        if (keys && keys.length) {
          this.$nextTick(() => {
            this.setCheckedKeys(keys)
          })
        }
      },
      immediate: true
    },
    // 监听默认展开全部
    defaultExpandAll: {
      handler(val) {
        if (val && this.data.length && !this.hasInitializedExpand) {
          this.expandAll()
          this.hasInitializedExpand = true
        }
      }
    },
    // 监听默认展开key变化
    defaultExpandKeys: {
      handler(keys) {
        if (keys && keys.length && !this.defaultExpandAll && !this.hasInitializedExpand && this.data.length) {
          this.$nextTick(() => {
            this.applyExpandKeys(keys)
            this.hasInitializedExpand = true
          })
        }
      },
      immediate: true,
      deep: true
    },
    // 监听搜索关键词变化
    filterText: {
      handler() {
        this.handleFilter()
      }
    }
  },

  mounted() {
    this.$refs.container.addEventListener('scroll', this.handleScroll, { passive: true })
    // 监听容器尺寸变化，更新可视区域
    this.resizeObserver = new ResizeObserver(() => {
      this.updateVisibleData()
    })
    this.resizeObserver.observe(this.$refs.container)
  },

  beforeDestroy() {
    if (this.$refs.container) {
      this.$refs.container.removeEventListener('scroll', this.handleScroll)
    }
    if (this.resizeObserver) {
      this.resizeObserver.disconnect()
      this.resizeObserver = null
    }
  },

  methods: {
    // 判断节点是否为叶子节点
    isLeafNode(item) {
      const isLeafField = this.props.isLeaf || 'isLeaf'
      if (item.raw && item.raw[isLeafField] === true) return true
      return !item.hasChildren
    },

    // 判断节点是否禁用
    isNodeDisabled(item) {
      if (this.disabledFn && item.raw) {
        return this.disabledFn(item.raw)
      }
      return false
    },

    /**
     * 展开所有节点
     */
    expandAll() {
      this.expandedKeys.clear()
      const collectAllKeys = (nodes) => {
        nodes.forEach(node => {
          const id = node[this.nodeKey]
          const children = node[this.props.children] || []
          if (children.length > 0) {
            this.expandedKeys.add(id)
            collectAllKeys(children)
          }
        })
      }
      collectAllKeys(this.data)
      this.rebuildFlatData()
    },

    /**
     * 应用展开key列表，并确保父节点也展开
     */
    applyExpandKeys(keys) {
      if (!keys || keys.length === 0) return
      this.expandedKeys.clear()
      keys.forEach(key => {
        this.expandedKeys.add(key)
      })
      this.ensureParentExpanded(keys)
      this.rebuildFlatData()
    },

    /**
     * 设置展开的节点key（对外API）
     */
    setExpandKeys(keys) {
      this.applyExpandKeys(keys)
    },

    /**
     * 确保目标节点的所有父节点也展开
     */
    ensureParentExpanded(keys) {
      const findPathToNode = (nodes, targetId, path = []) => {
        for (const node of nodes) {
          const id = node[this.nodeKey]
          if (id === targetId) return [...path, id]
          const children = node[this.props.children] || []
          if (children.length > 0) {
            const result = findPathToNode(children, targetId, [...path, id])
            if (result) return result
          }
        }
        return null
      }

      keys.forEach(key => {
        const path = findPathToNode(this.data, key)
        if (path && path.length > 1) {
          // 展开所有父节点（不包含当前节点）
          path.slice(0, -1).forEach(pid => {
            this.expandedKeys.add(pid)
          })
        }
      })
    },

    /**
     * 初始化树数据
     */
    initData() {
      this.isReady = false
      this.nodeMap.clear()
      this.hasInitializedExpand = false

      // 构建基础扁平数据（不包含展开状态）
      this.buildBaseFlatData()

      // 处理默认展开
      if (this.defaultExpandAll) {
        this.expandAll()
        this.hasInitializedExpand = true
      } else if (this.defaultExpandKeys && this.defaultExpandKeys.length) {
        this.applyExpandKeys(this.defaultExpandKeys)
        this.hasInitializedExpand = true
      }

      this.isReady = true
      this.updateVisibleData()

      // 懒加载模式下，如果 data 为空，自动调用 load 获取根节点数据
      if (this.lazy && this.load && this.data.length === 0 && this.flatData.length === 0) {
        this.loadRootNodes()
      }
    },

    /**
     * 懒加载根节点数据
     */
    async loadRootNodes() {
      if (!this.load) return

      try {
        const children = await new Promise((resolve) => {
          this.load(null, (data) => resolve(data))
        })

        if (children && children.length > 0) {
          // 将根节点数据设置到 data 上
          this.$emit('lazy-load', null, children)
          // 手动构建根节点的 flatNode
          const labelField = this.props.label || 'label'
          const keyField = this.nodeKey
          const result = []
          children.forEach(node => {
            const id = node[keyField]
            const hasChildren = (node[this.props.children] || []).length > 0
            const flatNode = {
              _uid: uid++,
              id,
              label: node[labelField],
              level: 0,
              hasChildren: hasChildren || (this.lazy && !node[this.props.isLeaf || 'isLeaf']),
              expanded: false,
              loading: false,
              raw: node,
              parent: null
            }
            result.push(flatNode)
            this.nodeMap.set(id, flatNode)
          })
          this.flatData = result
          this.updateVisibleData()
        }
      } catch (e) {
        // 忽略异常
      }
    },

    /**
     * 构建基础扁平数据（仅顶层节点，不含展开的子节点）
     */
    buildBaseFlatData() {
      const labelField = this.props.label || 'label'
      const childrenField = this.props.children || 'children'
      const isLeafField = this.props.isLeaf || 'isLeaf'
      const keyField = this.nodeKey

      const result = []
      const processNode = (node, level = 0, parent = null) => {
        const id = node[keyField]
        const children = node[childrenField] || []
        // 懒加载模式下，除非明确标记为叶子节点，否则认为可能有子节点
        const hasChildren = this.lazy
          ? !(node[isLeafField] === true)
          : children.length > 0

        const flatNode = {
          _uid: uid++,
          id,
          label: node[labelField],
          level,
          hasChildren,
          expanded: false,
          loading: false,
          raw: node,
          parent
        }

        result.push(flatNode)
        this.nodeMap.set(id, flatNode)
      }

      this.data.forEach(node => processNode(node, 0, null))
      this.flatData = result
    },

    /**
     * 根据展开状态重建扁平数据（递归展开的子节点也会加入）
     */
    rebuildFlatData() {
      const labelField = this.props.label || 'label'
      const isLeafField = this.props.isLeaf || 'isLeaf'
      const keyField = this.nodeKey

      const result = []
      const processNode = (node, level = 0, parent = null) => {
        const id = node[keyField]
        const children = node[this.props.children] || []
        // 懒加载模式下，除非明确标记为叶子节点，否则认为可能有子节点
        const hasChildren = this.lazy
          ? !(node[isLeafField] === true)
          : children.length > 0
        const expanded = this.expandedKeys.has(id)

        const existingNode = this.nodeMap.get(id)
        const flatNode = {
          _uid: uid++,
          id,
          label: node[labelField],
          level,
          hasChildren,
          expanded,
          loading: existingNode ? existingNode.loading : false,
          raw: node,
          parent
        }

        result.push(flatNode)

        // 更新或添加节点映射
        if (this.nodeMap.has(id)) {
          this.nodeMap.get(id).expanded = expanded
        } else {
          this.nodeMap.set(id, flatNode)
        }

        // 如果节点展开且有子节点，递归处理子节点
        if (expanded && hasChildren) {
          children.forEach(child => processNode(child, level + 1, flatNode))
        }
      }

      this.data.forEach(node => processNode(node, 0, null))
      this.flatData = result
      this.updateVisibleData()
    },

    /**
     * 滚动事件处理
     */
    handleScroll() {
      if (!this.isReady) return
      const container = this.$refs.container
      this.scrollTop = container.scrollTop
      this.updateVisibleData()
    },

    /**
     * 更新可视区域数据
     */
    updateVisibleData() {
      if (!this.flatData.length) {
        this.visibleData = []
        return
      }
      const currentHeight = this.pixelHeight
      const start = Math.floor(this.scrollTop / this.itemSize)
      const visibleCount = Math.ceil(currentHeight / this.itemSize) + 20
      const end = Math.min(start + visibleCount, this.flatData.length)
      this.visibleData = this.flatData.slice(start, end)
      this.offset = start * this.itemSize
    },

    // 切换展开/收缩（支持懒加载）
    async toggleExpand(item) {
      if (this.isLeafNode(item)) return

      // 懒加载模式下，首次展开未加载子节点的节点时，调用 load 获取子节点
      const childrenField = this.props.children || 'children'
      const childrenLoaded = item.raw && item.raw[childrenField] && item.raw[childrenField].length > 0
      if (this.lazy && this.load && !item.expanded && !childrenLoaded) {
        item.loading = true
        this.$forceUpdate()

        try {
          const children = await new Promise((resolve) => {
            this.load(item.raw, (data) => resolve(data))
          })

          item.loading = false
          if (children && children.length > 0) {
            // 将子节点挂载到原始数据上
            this.$set(item.raw, this.props.children || 'children', children)
            item.hasChildren = true
            this.$emit('lazy-load', item.raw, children)
            // 懒加载完成后自动展开
            item.expanded = true
            this.expandedKeys.add(item.id)
            this.rebuildFlatData()
            this.$emit('node-expand', item.raw, true)
            this.$emit('expand-change', Array.from(this.expandedKeys))
            return
          } else {
            // 没有子节点，标记为叶子
            this.$set(item.raw, this.props.children || 'children', [])
            item.hasChildren = false
            this.$forceUpdate()
            return
          }
        } catch (e) {
          item.loading = false
          this.$forceUpdate()
          return
        }
      }

      item.expanded = !item.expanded

      if (item.expanded) {
        this.expandedKeys.add(item.id)
      } else {
        this.expandedKeys.delete(item.id)
      }

      this.rebuildFlatData()

      this.$emit('node-expand', item.raw, item.expanded)
      this.$emit('expand-change', Array.from(this.expandedKeys))
    },

    handleNodeClick(item) {
      this.$emit('node-click', item.raw, item)
    },

    /**
     * 获取节点所有子孙ID
     */
    getAllChildrenIds(node) {
      const ids = []
      const childrenField = this.props.children || 'children'

      const collect = (rawNode) => {
        const children = rawNode[childrenField] || []
        children.forEach(child => {
          const childId = child[this.nodeKey]
          ids.push(childId)
          collect(child)
        })
      }

      collect(node.raw)
      return ids
    },

    /**
     * 获取节点所有祖先ID
     */
    getAllParentIds(node) {
      const ids = []
      let currentNode = node
      while (currentNode.parent) {
        const parentId = currentNode.parent.id
        ids.push(parentId)
        currentNode = this.nodeMap.get(parentId)
      }
      return ids
    },

    /**
     * 向上更新父节点的选中/半选状态
     */
    updateParentState(node) {
      if (this.checkStrictly) return

      const parentIds = this.getAllParentIds(node)

      parentIds.forEach(parentId => {
        const parentNode = this.nodeMap.get(parentId)
        if (!parentNode) return

        const parentRaw = parentNode.raw
        const children = parentRaw[this.props.children] || []

        let checkedCount = 0
        let childIndeterminateCount = 0

        children.forEach(child => {
          const childId = child[this.nodeKey]
          if (this.checkedKeys.has(childId)) checkedCount++
          else if (this.indeterminateKeys.has(childId)) childIndeterminateCount++
        })

        const totalChildren = children.length

        if (checkedCount === totalChildren) {
          // 所有子节点都选中
          this.checkedKeys.add(parentId)
          this.indeterminateKeys.delete(parentId)
        } else if (checkedCount === 0 && childIndeterminateCount === 0) {
          // 没有子节点选中，也没有半选
          this.checkedKeys.delete(parentId)
          this.indeterminateKeys.delete(parentId)
        } else {
          // 部分选中
          this.checkedKeys.delete(parentId)
          this.indeterminateKeys.add(parentId)
        }
      })
    },

    /**
     * 向下更新子节点的选中状态
     */
    updateChildrenState(node, checked) {
      if (this.checkStrictly) return

      const childrenIds = this.getAllChildrenIds(node)
      childrenIds.forEach(childId => {
        if (checked) {
          this.checkedKeys.add(childId)
          this.indeterminateKeys.delete(childId)
        } else {
          this.checkedKeys.delete(childId)
          this.indeterminateKeys.delete(childId)
        }
      })
    },

    /**
     * 复选框勾选处理
     */
    handleCheck(item, value) {
      const isChecked = value
      if (this.isNodeDisabled(item)) return

      if (this.checkStrictly) {
        // 严格模式：只勾选当前节点
        if (isChecked) {
          this.checkedKeys.add(item.id)
          this.indeterminateKeys.delete(item.id)
        } else {
          this.checkedKeys.delete(item.id)
        }
      } else {
        // 非严格模式：父子联动
        if (isChecked) {
          this.checkedKeys.add(item.id)
          this.indeterminateKeys.delete(item.id)
        } else {
          this.checkedKeys.delete(item.id)
        }

        // 更新所有子节点
        this.updateChildrenState(item, isChecked)
        // 更新所有父节点
        this.updateParentState(item)
      }

      this.$emit('check', item.raw, {
        checkedKeys: Array.from(this.checkedKeys),
        checked: value,
        node: item
      })

      this.$forceUpdate()
    },

    // 判断节点是否为半选状态
    isIndeterminate(id) {
      return this.indeterminateKeys.has(id)
    },

    // 判断节点是否选中
    isChecked(id) {
      return this.checkedKeys.has(id)
    },

    /**
     * 搜索过滤处理
     */
    handleFilter() {
      if (!this.filterText) {
        this.rebuildFlatData()
        return
      }

      const keyword = this.filterText.toLowerCase()
      const matchedIds = new Set()

      // 查找匹配的节点及其父节点路径
      const findMatches = (nodes, parentPath = []) => {
        nodes.forEach(node => {
          const id = node[this.nodeKey]
          const label = node[this.props.label] || ''
          const isMatch = label.toLowerCase().includes(keyword)

          if (isMatch) {
            matchedIds.add(id)
            // 添加所有父节点ID，确保路径可见
            parentPath.forEach(pid => matchedIds.add(pid))
          }

          const children = node[this.props.children] || []
          if (children.length) {
            findMatches(children, [...parentPath, id])
          }
        })
      }

      findMatches(this.data)

      if (matchedIds.size === 0) {
        this.flatData = []
        this.visibleData = []
        return
      }

      // 构建过滤后的扁平数据
      const result = []
      const buildFilteredTree = (nodes, level = 0, parent = null) => {
        nodes.forEach(node => {
          const id = node[this.nodeKey]
          if (matchedIds.has(id)) {
            const children = node[this.props.children] || []
            const hasChildren = children.length > 0

            result.push({
              _uid: uid++,
              id,
              label: node[this.props.label],
              level,
              hasChildren,
              expanded: true, // 过滤模式下强制展开
              loading: false,
              raw: node,
              parent
            })

            if (hasChildren) {
              children.forEach(child => buildFilteredTree([child], level + 1, { id, raw: node }))
            }
          }
        })
      }

      buildFilteredTree(this.data)
      this.flatData = result
      this.updateVisibleData()
    },

    // ========== 对外 API ==========

    getNode(id) {
      const flatNode = this.nodeMap.get(id)
      if (!flatNode) return null
      return {
        data: flatNode.raw,
        id: flatNode.id,
        label: flatNode.label,
        level: flatNode.level,
        expanded: flatNode.expanded,
        parent: flatNode.parent
      }
    },

    getCheckedKeys() {
      return Array.from(this.checkedKeys)
    },

    getCheckedNodes() {
      const nodes = []
      this.checkedKeys.forEach(key => {
        const flatNode = this.nodeMap.get(key)
        if (flatNode && flatNode.raw) {
          nodes.push(flatNode.raw)
        }
      })
      return nodes
    },

    setCheckedKeys(keys) {
      this.checkedKeys.clear()
      this.indeterminateKeys.clear()
      if (!keys || keys.length === 0) {
        this.$forceUpdate()
        return
      }
      if (this.checkStrictly) {
        keys.forEach(key => this.checkedKeys.add(key))
      } else {
        keys.forEach(key => {
          this.checkedKeys.add(key)
          const node = this.nodeMap.get(key)
          if (node) {
            this.updateParentState(node)
          }
        })
      }
      this.$forceUpdate()
    },

    // 设置单个节点选中状态
    setChecked(key, checked) {
      const node = this.nodeMap.get(key)
      if (!node) return
      if (checked) {
        this.checkedKeys.add(key)
        this.indeterminateKeys.delete(key)
      } else {
        this.checkedKeys.delete(key)
        this.indeterminateKeys.delete(key)
      }
      if (!this.checkStrictly) {
        this.updateChildrenState(node, checked)
        this.updateParentState(node)
      }
      this.$forceUpdate()
    },

    setCurrentKey(key) {
      this.$emit('update:currentKey', key)
    },

    filter(val) {
      // 兼容 el-tree 的 filter 方法
      if (val !== undefined) {
        this.$emit('update:filterText', val)
      }
    },

    resetExpandInitialized() {
      this.hasInitializedExpand = false
    },

    getExpandKeys() {
      return Array.from(this.expandedKeys)
    },

    reset() {
      this.checkedKeys.clear()
      this.indeterminateKeys.clear()
      this.expandedKeys.clear()
      this.flatData = []
      this.visibleData = []
      this.nodeMap.clear()
      this.hasInitializedExpand = false
      this.$forceUpdate()
    }
  }
}
</script>

<style lang="scss" scoped>
.virtual-tree {
  position: relative;
  overflow-y: auto;
  overflow-x: auto;
  font-size: 14px;
  color: #606266;
  background: #fff;

  &__phantom {
    position: absolute;
    left: 0;
    top: 0;
    right: 0;
    z-index: -1;
    pointer-events: none;
  }

  &__content {
    position: absolute;
    left: 0;
    top: 0;
    min-width: 100%;
    width: max-content;
  }

  &__node {
    display: flex;
    align-items: center;
    height: 26px;
    padding: 0 8px;
    cursor: pointer;
    box-sizing: border-box;
    transition: background-color 0.15s;
    white-space: nowrap;
    min-width: 100%;
    width: max-content;

    &:hover {
      background-color: #f5f7fa;
    }

    &.is-checked {
      background-color: #f0f7ff;
    }

    &.is-current {
      background-color: #e6f1fc;
    }

    &.is-disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }

  // 缩进占位
  &__indent {
    flex-shrink: 0;
  }

  &__expand {
    width: 16px;
    height: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 4px;
    color: #c0c4cc;
    font-size: 12px;
    cursor: pointer;
    flex-shrink: 0;

    i {
      transition: transform 0.2s;
    }

    &.is-expanded i {
      transform: rotate(90deg);
    }

    &.is-leaf {
      visibility: hidden;
    }

    &.is-loading i {
      animation: rotating 1.5s linear infinite;
    }
  }

  // 节点内容包裹层，支持深层级内容不被截断
  &__content-wrapper {
    display: flex;
    align-items: center;
    min-width: 0;
    padding-right: 12px;
  }

  &__icon {
    margin-right: 4px;
    font-size: 14px;
    flex-shrink: 0;
  }

  &__label {
    overflow: visible;
    white-space: nowrap;
    margin-left: 4px;
  }
}

@keyframes rotating {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
