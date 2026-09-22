<!--
  通用状态切换组件 (StatusSwitch)

  功能说明：
  - 用于表格中的状态显示和切换
  - 支持禁用状态下显示标签
  - 统一的状态切换交互

  Props:
  - value: 当前状态值 (0=正常/启用, 1=禁用)
  - disabled: 是否禁用切换操作
  - loading: 是否处于加载状态

  Events:
  - change: 状态切换时触发，返回新状态值

  使用示例：
  <status-switch
    :value="row.status"
    :disabled="isSpecialRole(row)"
    @change="handleStatusChange(row, $event)"
  />
-->
<template>
  <div class="status-switch-wrapper">
    <!-- 禁用状态：根据实际值显示对应标签 -->
    <el-tag
      v-if="disabled"
      :type="value === 0 ? 'success' : 'danger'"
      size="medium"
      class="status-tag"
    >
      {{ value === 0 ? normalText : forbiddenText }}
    </el-tag>

    <!-- 可操作状态：显示开关 -->
    <div v-else class="switch-container">
      <el-switch
        :value="switchValue"
        :loading="loading"
        active-color="#67C23A"
        inactive-color="#F56C6C"
        @change="handleChange"
      />
      <span class="switch-text switch-text--right" v-show="!switchValue">{{ forbiddenText }}</span>
      <span class="switch-text switch-text--left" v-show="switchValue">{{ normalText }}</span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'StatusSwitch',

  props: {
    /**
     * 当前状态值
     * 0 = 正常/启用
     * 1 = 禁用
     */
    value: {
      type: Number,
      default: 0
    },

    /**
     * 是否禁用切换操作
     * 禁用时显示"正常"标签
     */
    disabled: {
      type: Boolean,
      default: false
    },

    /**
     * 是否处于加载状态
     */
    loading: {
      type: Boolean,
      default: false
    },

    /**
     * 启用状态文本
     */
    normalText: {
      type: String,
      default: '正常'
    },

    /**
     * 禁用状态文本
     */
    forbiddenText: {
      type: String,
      default: '禁用'
    }
  },

  computed: {
    /**
     * Switch 组件的值
     * el-switch: true = 启用, false = 禁用
     * 业务逻辑: 0 = 启用, 1 = 禁用
     * 需要进行转换
     */
    switchValue() {
      return this.value === 0
    }
  },

  methods: {
    /**
     * 处理状态切换
     * @param {Boolean} newValue - Switch组件的新值 (true=启用, false=禁用)
     */
    handleChange(newValue) {
      // 转换为业务状态值: true -> 0 (启用), false -> 1 (禁用)
      const statusValue = newValue ? 0 : 1
      this.$emit('change', statusValue)
    }
  }
}
</script>

<style lang="scss" scoped>
.status-switch-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;

  .status-tag {
    font-weight: 500;
  }

  .switch-container {
    position: relative;
    display: inline-flex;
    align-items: center;

    // Switch 组件样式优化
    ::v-deep .el-switch {
      .el-switch__core {
        height: 22px;
        min-width: 46px;
        border-radius: 11px;

        &::after {
          height: 18px;
          width: 18px;
          top: 1px;
          border-radius: 9px;
        }
      }

      // 隐藏自带 label（我们用自定义 span 替代）
      .el-switch__label {
        display: none;
      }
    }

    // 内嵌文字样式
    .switch-text {
      position: absolute;
      top: 50%;
      transform: translateY(-50%);
      font-size: 10px;
      font-weight: 600;
      color: #fff;
      line-height: 1;
      pointer-events: none;
      white-space: nowrap;
      z-index: 1;
    }

    // 左侧文字（inactive/禁用）：显示在滑块左侧区域
    .switch-text--left {
      left: 4px;
    }

    // 右侧文字（active/正常）：显示在滑块右侧区域
    .switch-text--right {
      right: 4px;
    }
  }
}
</style>
