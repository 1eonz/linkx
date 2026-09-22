<template>
  <!-- 连接状态指示组件 -->
  <span class="connection-status">
    <span :class="['status-dot', statusDotClass]"></span>
    <span v-if="showText">{{ displayText }}</span>
  </span>
</template>

<script>
/**
 * 连接状态指示组件
 * 
 * 状态值定义：
 * 0: 初始（default）
 * 1: 连接中
 * 2: JWT认证通过
 * 3: JWT认证失败
 * 4: 认证成功并心跳正常
 * 5: 心跳失活
 * 6: 关闭
 * 7: 等待认证
 */
export default {
  name: 'ConnectionStatusDot',
  props: {
    /** 连接状态值 */
    status: {
      type: Number,
      default: 0
    },
    /** 状态描述文本 */
    statusDesc: {
      type: String,
      default: ''
    },
    /** 是否显示文本 */
    showText: {
      type: Boolean,
      default: true
    }
  },
  computed: {
    /**
     * 根据状态值获取状态点类名
     */
    statusDotClass() {
      // 状态4: 认证成功并心跳正常 - 绿色
      if (this.status === 4) return 'status-dot--online'
      // 状态1, 2, 7: 处理中状态 - 蓝色
      if (this.status === 1 || this.status === 2 || this.status === 7) return 'status-dot--processing'
      // 状态0, 5: 初始/失活 - 橙色
      if (this.status === 0 || this.status === 5) return 'status-dot--warning'
      // 状态3, 6: 失败/关闭 - 红色
      if (this.status === 3 || this.status === 6) return 'status-dot--error'
      // 其他未知状态 - 灰色
      return 'status-dot--offline'
    },
    /**
     * 显示文本
     */
    displayText() {
      return this.statusDesc || '-'
    }
  }
}
</script>

<style lang="scss" scoped>
.connection-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;

  &--online {
    background-color: #67C23A;  // 绿色 - 认证成功并心跳正常
  }

  &--processing {
    background-color: #409EFF;  // 蓝色 - 处理中
  }

  &--warning {
    background-color: #E6A23C;  // 橙色 - 初始/失活
  }

  &--error {
    background-color: #F56C6C;  // 红色 - 失败/关闭
  }

  &--offline {
    background-color: #909399;  // 灰色 - 其他未知状态
  }
}
</style>
