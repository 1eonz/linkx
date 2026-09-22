<!--
  虚拟滚动复选框列表组件

  功能说明：
  - 虚拟滚动渲染，只渲染可视区域内的 DOM 节点，支持大量数据
  - 支持复选框勾选
  - 配合 DataPermissionTree 右侧已选列表使用

  Props:
  - data: 数据列表 [{ id, name, icon, isPending }]
  - checkedIds: 已勾选的id列表
  - height: 容器高度（px）
  - itemSize: 每行高度（px）

  Events:
  - check-change: 勾选变化时触发，参数为 (item, val)

  Methods:
  - scrollToTop(): 滚动到顶部
-->
<template>
  <!-- 右侧已选列表虚拟滚动组件 -->
  <div class="virtual-checkbox-list" ref="container" :style="containerStyle">
    <!-- 占位元素，用于撑开滚动高度 -->
    <div class="virtual-checkbox-list__phantom" :style="{ height: totalHeight + 'px' }"></div>
    <!-- 可视区域内容，通过 transform 实现滚动偏移 -->
    <div class="virtual-checkbox-list__content" :style="{ transform: `translateY(${offset}px)` }">
      <div
        v-for="item in visibleData"
        :key="item.id"
        class="virtual-checkbox-list__item"
        :class="{ 'is-checked': isChecked(item.id) }"
      >
        <el-checkbox
          :value="isChecked(item.id)"
          :label="item.id"
          @change="(val) => handleCheck(item, val)"
        >
          <i v-if="item.icon" :class="item.icon" class="item-icon" />
          <span class="item-label" :title="item.name">{{ item.name }}</span>
        </el-checkbox>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'VirtualCheckboxList',

  props: {
    // 数据列表 [{ id, name, icon, isPending }]
    data: {
      type: Array,
      default: () => []
    },
    // 已勾选的id列表
    checkedIds: {
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
      default: 34
    }
  },

  data() {
    return {
      // 滚动偏移量
      offset: 0,
      // 当前滚动位置
      scrollTop: 0
    }
  },

  computed: {
    // 虚拟滚动总高度
    totalHeight() {
      return this.data.length * this.itemSize
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
      if (this.$refs.container) {
        return this.$refs.container.clientHeight
      }
      return 400
    },
    // 当前可视区域的数据
    visibleData() {
      if (!this.data.length) return []
      const currentHeight = this.pixelHeight
      const start = Math.floor(this.scrollTop / this.itemSize)
      // 多渲染10条作为缓冲
      const visibleCount = Math.ceil(currentHeight / this.itemSize) + 10
      const end = Math.min(start + visibleCount, this.data.length)
      return this.data.slice(start, end)
    }
  },

  mounted() {
    this.$refs.container.addEventListener('scroll', this.handleScroll, { passive: true })
    // 监听容器尺寸变化，更新可视区域
    this.resizeObserver = new ResizeObserver(() => {
      this.$forceUpdate()
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
    /**
     * 滚动事件处理
     */
    handleScroll() {
      const container = this.$refs.container
      this.scrollTop = container.scrollTop
      // 按 itemSize 对齐偏移量，避免滚动抖动
      this.offset = Math.floor(this.scrollTop / this.itemSize) * this.itemSize
    },

    /**
     * 判断节点是否勾选
     */
    isChecked(id) {
      return this.checkedIds.includes(id)
    },

    /**
     * 复选框勾选变化
     */
    handleCheck(item, val) {
      this.$emit('check-change', item, val)
    },

    /**
     * 滚动到顶部
     */
    scrollToTop() {
      if (this.$refs.container) {
        this.$refs.container.scrollTop = 0
        this.scrollTop = 0
        this.offset = 0
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.virtual-checkbox-list {
  position: relative;
  overflow-y: auto;

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

  &__item {
    display: flex;
    align-items: center;
    height: 34px;
    padding: 0 8px;
    box-sizing: border-box;
    border-radius: 4px;
    transition: background-color 0.2s ease;

    &:hover {
      background-color: #F5F7FA;
    }

    &.is-checked {
      background-color: #F5F7FA;
    }

    ::v-deep .el-checkbox {
      width: 100%;
      display: flex;
      align-items: center;
    }

    ::v-deep .el-checkbox__label {
      display: flex;
      align-items: center;
      overflow: hidden;
      flex: 1;
    }

    .item-icon {
      margin-right: 6px;
      color: #409EFF;
      font-size: 14px;
      flex-shrink: 0;
    }

    .item-label {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      color: #303133;
      font-size: 14px;
    }
  }
}
</style>
