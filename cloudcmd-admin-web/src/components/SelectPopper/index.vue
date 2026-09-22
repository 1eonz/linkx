<template>
  <div class="select-popper">
    <!-- 已选择的标签显示区域 -->
    <div class="selected-tags" :class="{ 'is-disabled': disabled }" @click="toggleDropdown">
      <div class="tags-container">
        <el-tag
          v-for="tag in selectedItems"
          :key="tag.value"
          :disable-transitions="true"
          :closable="!disabled"
          size="small"
          @close="removeTag(tag)"
        >
          {{ tag.label }}
        </el-tag>
        <span v-if="selectedItems.length === 0" class="placeholder">
            {{ placeholder }}
        </span>
      </div>
      <i
        class="el-icon-arrow-down dropdown-icon"
        :class="{ 'is-reverse': isOpen }"
      ></i>
    </div>

    <!-- 下拉面板 -->
    <transition name="fade">
      <div v-if="isOpen" class="dropdown-panel">
        <!-- 搜索框 -->
        <div class="search-container">
          <el-form-item class="search-form-item" prop="false">
            <el-input
              :error="false"
              v-model.trim="searchKeyword"
              placeholder="请输入"
              size="small"
              prefix-icon="el-icon-search"
              @input="handleInput"
            />
          </el-form-item>
        </div>

        <!-- 选项区域 -->
        <div class="options-container">
            <slot>
                <Options :options="filteredOptions" :selected="value" @select="handleSelect" />
            </slot>
        </div>

        <!-- 操作按钮 -->
        <div v-if="isShowFooter" class="action-buttons">
            <el-button size="small" @click="cancel">
                取消
            </el-button>
            <el-button type="primary" size="small" @click="confirm">
                确定
            </el-button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
import lodash from "lodash"
import Options from './Options.vue'

export default {
  name: 'SelectPopper',
  props: {
    value: {
      type: Array,
      default: () => []
    },
    placeholder: {
      type: String,
      default: '请选择'
    },
    options: {
        type: Array,
        default: () => []
    },
    selected: {
        type: Array,
        default: () => []
    },
    disabled: {
      type: Boolean,
      default: false
    },
    isShowFooter: {
        type: Boolean,
        default: true
    }
  },
  components: {
    Options
  },
  data() {
    return {
      isOpen: false,
      searchKeyword: '',
      filteredOptions: []
    }
  },
  computed: {
    selectedItems() {
        if (this.$slots.default) {
            return this.selected
        }
        return this.options.filter(item => this.value.includes(item.value))
    },
  },
  created() {
    // 创建一次防抖函数并缓存，保证多次输入复用同一个计时器
    this.debouncedSearch = lodash.debounce(this.handleSearch, 300)
  },
  mounted() {
    // 点击外部关闭下拉框
    document.addEventListener('click', this.handleClickOutside)
  },
  beforeDestroy() {
    // 移除点击外部关闭下拉框事件
    document.removeEventListener('click', this.handleClickOutside)
  },
  methods: {
    // 切换下拉框显示状态
    toggleDropdown() {
      if (this.disabled) {
        return
      }
      if (this.isOpen) {
        this.closeDropdown()
      } else {
        this.openDropdown()
      }
    },

    // 处理点击外部事件
    handleClickOutside(event) {
      if (!this.$el.contains(event.target)) {
        this.closeDropdown()
      }
    },

    handleInput() {
       this.debouncedSearch()
    },

    handleSearch(){
        // 判断是否传递了插槽
        if (this.$slots.default) {
          this.$emit('search', this.searchKeyword)
        } else {
            this.filterOptions()
        }
    },

    // 过滤选项
    filterOptions() {
      this.filteredOptions = this.options.filter(item => item.label.includes(this.searchKeyword))
    },

    // 处理选项选择
    handleSelect(item) {
      if (item.disabled) {
        return
      }

      if (this.value.includes(item.value)) {
        this.removeTag(item)
      } else {
        this.addTag(item)
      }
    },

    // 添加标签
    addTag(tag) {
        if (this.$slots.default) {
            this.$emit('select', tag)
        } else {
            this.$emit('update:value', [...this.value, tag.value])
        }
    },

    // 移除标签
    removeTag(tag) {
        if (this.$slots.default) {
            this.$emit('select', tag)
        } else {
            this.$emit('update:value', this.value.filter(v => v !== tag.value))
        }
    },

    // 确认选择
    confirm() {
      this.closeDropdown()
    },

    // 取消选择
    cancel() {
      this.closeDropdown()
    },

    openDropdown() {
      this.isOpen = true

      this.handleSearch()
    },

    closeDropdown() {
      this.isOpen = false
      // 关闭时清空搜索关键词，避免下次打开遗留上次输入
      this.searchKeyword = ''
      this.$emit('search', '')
    }
  }
}
</script>

<style lang="scss" scoped>
::v-deep .el-tag--small {
  height: 26px !important;
}

.select-popper {
  position: relative;
  width: 100%;

  .selected-tags {
    display: flex;
    align-items: center;
    justify-content: space-between;
    min-height: 30px;
    padding: 5px 8px 5px 15px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    cursor: pointer;
    background-color: #fff;
    transition: border-color 0.2s cubic-bezier(0.645, 0.045, 0.355, 1);
    box-sizing: border-box;
    line-height: 30px;

    &:hover {
      border-color: #c0c4cc;
    }

    &.is-disabled {
      background-color: #f5f7fa;
      border-color: #e4e7ed;
      color: #c0c4cc;
      cursor: not-allowed;

      &:hover {
        border-color: #e4e7ed;
      }
    }

    .tags-container {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      flex: 1;
      box-sizing: border-box;

      .el-tag {
        margin: 2px 4px 2px 0;
      }

      .placeholder {
        color: #c0c4cc;
        font-size: 14px;
      }
    }

    .dropdown-icon {
      margin-left: 8px;
      transition: transform 0.3s;
      color: #c0c4cc;

      &.is-reverse {
        transform: rotate(180deg);
      }
    }
  }

  .dropdown-panel {
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    z-index: 1000;
    margin-top: 4px;
    background: #fff;
    border: 1px solid #e4e7ed;
    border-radius: 4px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);

    .search-container {
      margin: 0 12px;
      padding: 5px 0;
      border-bottom: 1px solid #e4e7ed;
    }
    .options-container {
        margin: 0 12px;
        padding: 5px 0;
    }

    .action-buttons {
      display: flex;
      justify-content: flex-end;
      padding: 10px 0;
      border-top: 1px solid #e4e7ed;
      gap: 8px;
      margin: 0 10px;
    }
  }
}

// 过渡动画
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter,
.fade-leave-to {
  opacity: 0;
}

// 旋转动画
@keyframes rotating {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

::v-deep .el-tag--small {
  background-color: #f4f4f5 !important;
  border-color: #e9e9eb !important;
  color: #909399 !important;
  .el-tag__close {
    color: #909399 !important;
    &:hover {
      color: #fff !important;
      background: #909399 !important;
    }
  }
}
</style>
