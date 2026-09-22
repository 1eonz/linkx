<template>
  <el-input
    v-model="currentValue"
    type="password"
    :class="inputClass"
    :style="inputStyle"
    :placeholder="placeholder"
    :disabled="disabled"
    :clearable="clearable"
    :show-password="showPassword"
    :maxlength="maxlength"
    :minlength="minlength"
    :size="size"
    :prefix-icon="prefixIcon"
    :suffix-icon="suffixIcon"
    :autocomplete="autocomplete"
    :readonly="readonly"
    :tabindex="tabindex"
    :name="name"
    @copy.native.prevent
    @paste.native.prevent
    @cut.native.prevent
    @input="handleInput"
    @change="handleChange"
    @focus="handleFocus"
    @blur="handleBlur"
    @clear="handleClear"
  />
</template>

<script>
/**
 * 密码输入框组件
 * 自动禁止复制、粘贴、剪切操作
 */
export default {
  name: 'PasswordInput',
  props: {
    // v-model 绑定值
    value: {
      type: String,
      default: ''
    },
    // 自定义 class
    inputClass: {
      type: [String, Array, Object],
      default: ''
    },
    // 自定义 style
    inputStyle: {
      type: [String, Array, Object],
      default: ''
    },
    // 占位文本
    placeholder: {
      type: String,
      default: ''
    },
    // 是否禁用
    disabled: {
      type: Boolean,
      default: false
    },
    // 是否可清空
    clearable: {
      type: Boolean,
      default: false
    },
    // 是否显示密码切换按钮
    showPassword: {
      type: Boolean,
      default: false
    },
    // 最大输入长度
    maxlength: {
      type: [Number, String],
      default: undefined
    },
    // 最小输入长度
    minlength: {
      type: [Number, String],
      default: undefined
    },
    // 输入框尺寸
    size: {
      type: String,
      default: ''
    },
    // 输入框头部图标
    prefixIcon: {
      type: String,
      default: ''
    },
    // 输入框尾部图标
    suffixIcon: {
      type: String,
      default: ''
    },
    // 自动补全
    autocomplete: {
      type: String,
      default: 'off'
    },
    // 原生属性，是否只读
    readonly: {
      type: Boolean,
      default: false
    },
    // 输入框的 tabindex
    tabindex: {
      type: String,
      default: ''
    },
    // 原生属性
    name: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      currentValue: this.value
    }
  },
  watch: {
    value(val) {
      this.currentValue = val
    }
  },
  methods: {
    handleInput(value) {
      this.$emit('input', value)
    },
    handleChange(value) {
      this.$emit('change', value)
    },
    handleFocus(event) {
      this.$emit('focus', event)
    },
    handleBlur(event) {
      this.$emit('blur', event)
    },
    handleClear() {
      this.$emit('clear')
    },
    // 提供 focus 方法
    focus() {
      this.$refs.input && this.$refs.input.focus()
    },
    // 提供 blur 方法
    blur() {
      this.$refs.input && this.$refs.input.blur()
    },
    // 提供 select 方法
    select() {
      this.$refs.input && this.$refs.input.select()
    }
  }
}
</script>
