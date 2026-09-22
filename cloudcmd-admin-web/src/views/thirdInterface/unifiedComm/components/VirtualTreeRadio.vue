<template>
  <div class="virtual-tree" ref="container" :style="{ height: height + 'px' }">
    <div class="virtual-tree__phantom" :style="{ height: totalHeight + 'px' }"></div>
    <div class="virtual-tree__content" :style="{ transform: `translateY(${offset}px)` }">
      <template v-for="item in visibleData">
        <div :key="item._uid" class="virtual-tree__node" :class="{ 'is-current': isCurrent(item.id), 'is-child-mark': isChildOfCurrent(item.id) && !isCurrent(item.id) }" :style="{ paddingLeft: item.level * 18 + 'px' }">
          <!-- 展开/收起图标 -->
          <span class="virtual-tree__expand" :class="{ 'is-leaf': !item.hasChildren, 'is-expanded': item.expanded }"
            @click.stop="toggleExpand(item)">
            <i v-if="item.hasChildren" class="el-icon-caret-right"></i>
          </span>

          <!-- 三种状态：单选 | 作为子元素标记被选中 | 未被选中 -->
          <span class="virtual-tree__radio" :class="{ 'is-checked': isCurrent(item.id), 'is-child-mark': isChildOfCurrent(item.id) && !isCurrent(item.id) }" @click.stop="handleRadioClick(item)">
            <span class="virtual-tree__radio-inner">
              <i v-if="isCurrent(item.id)" class="el-icon-check"></i>
              <i v-else-if="isChildOfCurrent(item.id) && !isCurrent(item.id)" class="el-icon-check"></i>
            </span>
          </span>

          <!-- 节点内容 -->
          <span class="virtual-tree__label" :title="item.label" @click="handleNodeClick(item)">{{ item.label }}</span>
        </div>
      </template>
    </div>
  </div>
</template>

<script>
let uid = 0

export default {
  name: 'VirtualTreeRadio',
  props: {
    data: {
      type: Array,
      default: () => []
    },
    props: {
      type: Object,
      default: () => ({
        label: 'label',
        children: 'children'
      })
    },
    nodeKey: {
      type: String,
      default: 'id'
    },
    defaultCurrentKey: {
      type: [String, Number],
      default: ''
    },
    defaultExpandAll: {
      type: Boolean,
      default: false
    },
    defaultExpandKeys: {
      type: Array,
      default: () => []
    },
    height: {
      type: Number,
      default: 400
    },
    itemSize: {
      type: Number,
      default: 26
    },
    filterText: {
      type: String,
      default: ''
    },
  },
  data() {
    return {
      flatData: [],
      visibleData: [],
      expandedKeys: new Set(),
      currentKey: '',
      offset: 0,
      scrollTop: 0,
      isReady: false,
      nodeMap: new Map(),
      hasInitializedExpand: false,
      // 存储当前选中节点的所有子节点ID（用于视觉标记）
      currentChildrenIds: new Set()
    }
  },
  computed: {
    totalHeight() {
      return this.flatData.length * this.itemSize
    }
  },
  watch: {
    data: {
      handler(newVal) {
        if (newVal && newVal.length) {
          this.initData()
        } else {
          this.flatData = []
          this.visibleData = []
          this.nodeMap.clear()
          this.expandedKeys.clear()
          this.currentChildrenIds.clear()
        }
      },
      immediate: true,
      deep: true
    },
    defaultCurrentKey: {
      handler(key) {
        if (key) {
          this.currentKey = key
          this.updateCurrentChildrenIds(key)
          this.$forceUpdate()
        }
      },
      immediate: true
    },
    defaultExpandAll: {
      handler(val) {
        if (val && this.data.length && !this.hasInitializedExpand) {
          this.expandAll()
          this.hasInitializedExpand = true
        }
      }
    },
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
    filterText: {
      handler(val) {
        this.handleFilter()
      }
    }
  },
  mounted() {
    this.$refs.container.addEventListener('scroll', this.handleScroll)
  },
  beforeDestroy() {
    if (this.$refs.container) {
      this.$refs.container.removeEventListener('scroll', this.handleScroll)
    }
  },
  methods: {
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

    applyExpandKeys(keys) {
      if (!keys || keys.length === 0) return

      this.expandedKeys.clear()

      keys.forEach(key => {
        this.expandedKeys.add(key)
      })

      this.ensureParentExpanded(keys)
      this.rebuildFlatData()
    },

    setExpandKeys(keys) {
      this.applyExpandKeys(keys)
    },

    ensureParentExpanded(keys) {
      const findPathToNode = (nodes, targetId, path = []) => {
        for (const node of nodes) {
          const id = node[this.nodeKey]
          if (id === targetId) {
            return [...path, id]
          }
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
          path.slice(0, -1).forEach(pid => {
            this.expandedKeys.add(pid)
          })
        }
      })
    },

    initData() {
      this.isReady = false
      this.nodeMap.clear()
      this.hasInitializedExpand = false

      this.buildBaseFlatData()

      if (this.defaultExpandAll) {
        this.expandAll()
        this.hasInitializedExpand = true
      } else if (this.defaultExpandKeys && this.defaultExpandKeys.length) {
        this.applyExpandKeys(this.defaultExpandKeys)
        this.hasInitializedExpand = true
      }

      // 设置默认当前选中节点
      if (this.defaultCurrentKey) {
        this.currentKey = this.defaultCurrentKey
        this.updateCurrentChildrenIds(this.defaultCurrentKey)
      }

      this.isReady = true
      this.updateVisibleData()
    },

    buildBaseFlatData() {
      const labelField = this.props.label || 'label'
      const childrenField = this.props.children || 'children'
      const keyField = this.nodeKey

      const result = []

      const processNode = (node, level = 0, parent = null) => {
        const id = node[keyField]
        const children = node[childrenField] || []
        const hasChildren = children.length > 0

        const flatNode = {
          _uid: uid++,
          id,
          label: node[labelField],
          level,
          hasChildren,
          expanded: false,
          raw: node,
          parent
        }

        result.push(flatNode)
        this.nodeMap.set(id, flatNode)
      }

      this.data.forEach(node => processNode(node, 0, null))
      this.flatData = result
    },

    rebuildFlatData() {
      const labelField = this.props.label || 'label'
      const childrenField = this.props.children || 'children'
      const keyField = this.nodeKey

      const result = []

      const processNode = (node, level = 0, parent = null) => {
        const id = node[keyField]
        const children = node[childrenField] || []
        const hasChildren = children.length > 0
        const expanded = this.expandedKeys.has(id)

        const flatNode = {
          _uid: uid++,
          id,
          label: node[labelField],
          level,
          hasChildren,
          expanded,
          raw: node,
          parent
        }

        result.push(flatNode)

        if (this.nodeMap.has(id)) {
          this.nodeMap.get(id).expanded = expanded
        } else {
          this.nodeMap.set(id, flatNode)
        }

        if (expanded && hasChildren) {
          children.forEach(child => processNode(child, level + 1, flatNode))
        }
      }

      this.data.forEach(node => processNode(node, 0, null))
      this.flatData = result
      this.updateVisibleData()
    },

    handleScroll() {
      if (!this.isReady) return

      const container = this.$refs.container
      this.scrollTop = container.scrollTop
      this.updateVisibleData()
    },

    updateVisibleData() {
      if (!this.flatData.length) {
        this.visibleData = []
        return
      }

      const start = Math.floor(this.scrollTop / this.itemSize)
      const visibleCount = Math.ceil(this.height / this.itemSize) + 20
      const end = Math.min(start + visibleCount, this.flatData.length)

      this.visibleData = this.flatData.slice(start, end)
      this.offset = start * this.itemSize
    },

    toggleExpand(item) {
      if (!item.hasChildren) return

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

    // 获取节点的所有子节点 ID（递归）
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

    // 更新当前选中节点的所有子节点ID集合（用于视觉标记）
    updateCurrentChildrenIds(key) {
      this.currentChildrenIds.clear()
      if (key) {
        const currentNode = this.nodeMap.get(key)
        if (currentNode) {
          const childrenIds = this.getAllChildrenIds(currentNode)
          childrenIds.forEach(id => this.currentChildrenIds.add(id))
        }
      }
    },

    // 判断节点是否为当前选中节点的子节点（用于视觉标记）
    isChildOfCurrent(id) {
      return this.currentChildrenIds.has(id)
    },

    handleRadioClick(item) {
      if (this.currentKey === item.id) return

      this.currentKey = item.id
      this.updateCurrentChildrenIds(item.id)

      this.$emit('node-click', item.raw)
      this.$emit('current-change', item.raw, {
        currentKey: this.currentKey
      })
      this.$forceUpdate()
    },

    handleNodeClick(item) {
      this.currentKey = item.id
      this.updateCurrentChildrenIds(item.id)
      this.$emit('node-click', item.raw)
      this.$emit('current-change', item.raw, {
        currentKey: this.currentKey
      })
      this.$forceUpdate()
    },

    isCurrent(id) {
      return this.currentKey === id
    },

    handleFilter() {
      if (!this.filterText) {
        this.rebuildFlatData()
        return
      }

      const keyword = this.filterText.toLowerCase()
      const matchedIds = new Set()

      const findMatches = (nodes, parentPath = []) => {
        nodes.forEach(node => {
          const id = node[this.nodeKey]
          const label = node[this.props.label] || ''
          const isMatch = label.toLowerCase().includes(keyword)

          if (isMatch) {
            matchedIds.add(id)
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
        this.visibleData = []
        return
      }

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
              expanded: true,
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

    resetExpandInitialized() {
      this.hasInitializedExpand = false
    },

    getCurrentKey() {
      return this.currentKey
    },

    setCurrentKey(key) {
      this.currentKey = key
      this.updateCurrentChildrenIds(key)
      this.$forceUpdate()
    },

    getExpandKeys() {
      return Array.from(this.expandedKeys)
    }
  }
}
</script>

<style lang="scss" scoped>
.virtual-tree {
  position: relative;
  overflow-y: auto;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
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
    right: 0;
    top: 0;
  }

  &__node {
    display: flex;
    align-items: center;
    height: 26px;
    padding: 0 8px;
    cursor: pointer;
    box-sizing: border-box;
    transition: background-color 0.15s;

    &:hover {
      background-color: #f5f7fa;
    }

    &.is-current, &.is-child-mark {
      background-color: #f4fafd;
    }
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
  }

  // 统一的自定义单选按钮
  &__radio {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 16px;
    height: 16px;
    margin-right: 8px;
    cursor: pointer;
    flex-shrink: 0;
  }

  &__radio-inner {
    display: inline-block;
    width: 16px;
    height: 16px;
    border-radius: 50%;
    border: 1px solid #dcdfe6;
    background-color: #fff;
    transition: all 0.2s;
    position: relative;
    cursor: pointer;

    i {
      font-size: 10px;
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
    }
  }

  // 状态1：未选中 - 灰色边框，无填充
  &__radio:not(.is-checked):not(.is-child-mark) &__radio-inner {
    border-color: #dcdfe6;
    background-color: #fff;

    i {
      display: none;
    }
  }

  // 状态2：选中状态 - 蓝色填充，白色对勾
  &__radio.is-checked &__radio-inner {
    border-color: #1890ff;
    background-color: #1890ff;

    i {
      display: block;
      color: #fff;
    }
  }

  // 状态3：子节点标记状态 - 灰色填充，灰色对勾
  &__radio.is-child-mark &__radio-inner {
    border-color: #b4bfca;
    background-color: #ffffff;
    cursor: default;

    i {
      display: block;
      color: #62788d;
    }
  }

  &__label {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    margin-left: 4px;
  }
}
</style>