<template>
  <div class="policer-select">
    <!-- 已选择的标签显示区域 -->
    <div class="selected-tags" @click="toggleDropdown">
      <div class="tags-container">
        <el-tag
          v-for="tag in selectedItems"
          :key="tag.id"
          :disable-transitions="true"
          closable
          size="small"
          @close="removeTag(tag)"
        >
          {{ tag.name }}
        </el-tag>
        <span v-if="selectedItems.length === 0" class="placeholder"
          >请选择警员</span
        >
      </div>
      <i
        class="el-icon-arrow-down dropdown-icon"
        :class="{ 'is-reverse': isOpen }"
      ></i>
    </div>

    <!-- 下拉面板 -->
    <transition name="fade">
      <div v-show="isOpen" class="dropdown-panel">
        <!-- 搜索框 -->
        <div class="search-container">
          <el-input
            v-model="searchKeyword"
            placeholder="请输入警员姓名"
            size="small"
            prefix-icon="el-icon-search"
            @input="handleSearch"
          />
        </div>

        <!-- 表格选项 -->
        <div class="options-container">
          <!-- 固定表头 -->
          <div class="table-header-fixed">
            <div class="table-header">
              <div class="checkbox-column">
                <el-checkbox
                  :value="isAllSelected"
                  :indeterminate="isIndeterminate"
                  @change="toggleSelectAll"
                />
              </div>
              <div class="name-column">警员姓名</div>
              <div class="org-column">所属部门</div>
              <div class="personnel-column">警号</div>
            </div>
          </div>

          <!-- 可滚动的表格内容 -->
          <div class="table-body-scrollable" @scroll="handleScroll">
            <div
              v-for="item in filteredOptions"
              :key="item.id"
              class="table-row"
              @click="toggleItem(item)"
            >
              <div class="checkbox-column">
                <el-checkbox
                  :value="isSelected(item.id)"
                  @change="toggleItem(item)"
                  @click.stop
                />
              </div>
              <div class="name-column" :title="item.name || ''">
                {{ item.name || '' }}
              </div>
              <div class="org-column" :title="item.primaryDepartment && item.primaryDepartment.departmentName || ''">
                {{ item.primaryDepartment && item.primaryDepartment.departmentName || '' }}
              </div>
              <div
                class="personnel-column"
                :title="item.idCardNum || ''"
              >
                {{ item.idCardNum || '' }}
              </div>
            </div>

            <!-- 加载状态 -->
            <div v-if="loading" class="loading-more">
              <i class="el-icon-loading"></i>
              <span>加载中...</span>
            </div>

            <!-- 没有更多数据提示 -->
            <div
              v-else-if="!hasMore && filteredOptions.length > 0"
              class="no-more-data"
            >
              没有更多数据了
            </div>

            <!-- 暂无数据 -->
            <div
              v-if="filteredOptions.length === 0 && !loading"
              class="no-data"
            >
              暂无数据
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <el-button size="small" @click="cancel">取消</el-button>
          <el-button type="primary" size="small" @click="confirm"
            >确定</el-button
          >
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
import { queryUserInfo } from '@/api/notification/alertPush'

export default {
  name: 'PolicerSelect',
  props: {
    value: {
      type: Array,
      default: () => []
    },
    orgId: {
      type: [String, Number],
      default: null
    },
    orgName: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      isOpen: false,
      searchKeyword: '',
      options: [],
      selectedItems: [],
      tempSelectedItems: [], // 临时选择项，用于确认前预览
      // 分页相关
      currentPage: 1,
      pageSize: 20,
      total: 0,
      loading: false,
      hasMore: true, // 是否还有更多数据
      searchTimer: null // 搜索防抖定时器
    }
  },
  computed: {
    filteredOptions() {
      return this.options
    },
    isAllSelected() {
      return (
        this.filteredOptions.length > 0 &&
        this.filteredOptions.every(item => this.isSelected(item.id))
      )
    },
    isIndeterminate() {
      const selectedCount = this.filteredOptions.filter(item =>
        this.isSelected(item.id)
      ).length
      return selectedCount > 0 && selectedCount < this.filteredOptions.length
    }
  },
  watch: {
    value: {
      handler(newVal) {
        this.selectedItems = [...newVal]
        this.tempSelectedItems = [...newVal]
      },
      immediate: true
    },
    orgId: {
      handler(newOrgId, oldOrgId) {
        if (newOrgId !== oldOrgId) {
          this.options = []
          this.currentPage = 1
          this.hasMore = true
          this.searchKeyword = ''
          if (newOrgId) {
            this.fetchOptions(true)
          }
        }
      },
      immediate: true
    }
  },
  mounted() {
    if (this.orgId) {
      this.fetchOptions()
    }
    document.addEventListener('click', this.handleClickOutside)
  },
  beforeDestroy() {
    document.removeEventListener('click', this.handleClickOutside)
    if (this.searchTimer) {
      clearTimeout(this.searchTimer)
    }
  },
  methods: {
    async fetchOptions(reset = false) {
      if (this.loading) return

      try {
        this.loading = true

        if (reset) {
          this.options = []
          this.currentPage = 1
          this.hasMore = true
        }
        
        const params = {
          deptId: this.orgId || undefined,
          includeChildren: 0, // 不包含子部门
          pageNum: this.currentPage,
          pageSize: this.pageSize,
          name: this.searchKeyword || undefined // 搜索关键词
        }

        const res = await queryUserInfo(params)
        const newRecords = res.data.records || []

        if (reset) {
          this.options = newRecords
        } else {
          this.options = [...this.options, ...newRecords]
        }

        this.total = res.data.total || 0
        this.hasMore = this.options.length < this.total

        if (newRecords.length > 0) {
          this.currentPage++
        }
      } catch (error) {
        console.error('获取警员数据失败:', error)
      } finally {
        this.loading = false
      }
    },

    toggleDropdown() {
      this.isOpen = !this.isOpen
      if (this.isOpen) {
        this.tempSelectedItems = [...this.selectedItems]
      }
    },

    handleClickOutside(event) {
      if (!this.$el.contains(event.target)) {
        this.isOpen = false
        this.tempSelectedItems = [...this.selectedItems]
      }
    },

    handleSearch() {
      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
      }

      this.searchTimer = setTimeout(() => {
        this.fetchOptions(true)
      }, 500)
    },

    handleScroll(event) {
      const { scrollTop, scrollHeight, clientHeight } = event.target
      if (
        scrollHeight - scrollTop - clientHeight <= 50 &&
        this.hasMore &&
        !this.loading
      ) {
        this.fetchOptions()
      }
    },

    toggleItem(item) {
      const index = this.tempSelectedItems.findIndex(
        selected => selected.id === item.id
      )
      if (index > -1) {
        this.tempSelectedItems.splice(index, 1)
      } else {
        this.tempSelectedItems.push(item)
      }
    },

    toggleSelectAll(checked) {
      if (checked) {
        this.filteredOptions.forEach(item => {
          if (
            !this.tempSelectedItems.find(selected => selected.id === item.id)
          ) {
            this.tempSelectedItems.push(item)
          }
        })
      } else {
        this.filteredOptions.forEach(item => {
          const index = this.tempSelectedItems.findIndex(
            selected => selected.id === item.id
          )
          if (index > -1) {
            this.tempSelectedItems.splice(index, 1)
          }
        })
      }
    },

    isSelected(id) {
      return this.tempSelectedItems.some(item => item.id === id)
    },

    removeTag(tag) {
      const index = this.selectedItems.findIndex(item => item.id === tag.id)
      if (index > -1) {
        this.selectedItems.splice(index, 1)
        this.emitChange()
      }
    },

    confirm() {
      this.selectedItems = [...this.tempSelectedItems]
      this.isOpen = false
      this.emitChange()
    },

    cancel() {
      this.tempSelectedItems = [...this.selectedItems]
      this.isOpen = false
    },

    emitChange() {
      this.$emit('input', this.selectedItems)
      this.$emit('change', this.selectedItems)
    }
  }
}
</script>

<style lang="scss" scoped>
::v-deep .el-tag--small {
  height: 26px !important;
}

.policer-select {
  position: relative;
  width: 100%;

  .selected-tags {
    display: flex;
    align-items: center;
    justify-content: space-between;
    min-height: 30px;
    padding: 5px 8px;
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
      max-height: 240px;
      display: flex;
      flex-direction: column;
      padding: 0 12px;
      box-sizing: border-box;

      .table-header-fixed {
        flex-shrink: 0;
        position: relative;
        z-index: 10;

        .table-header {
          display: flex;
          align-items: center;
          padding: 0 6px;
          background-color: #f5f7fa;
          border-bottom: 1px solid #e4e7ed;
          font-weight: 500;
          color: #606266;

          .checkbox-column {
            width: 25px;
            flex-shrink: 0;
          }

          .name-column {
            width: 150px;
            flex-shrink: 0;
          }

          .org-column {
            width: 120px;
            flex-shrink: 0;
          }

          .personnel-column {
            flex: 1;
          }
        }
      }

      .table-body-scrollable {
        flex: 1;
        overflow-y: auto;
        max-height: 240px;

        .table-row {
          display: flex;
          align-items: center;
          padding: 0 6px;
          border-bottom: 1px solid #f0f0f0;
          cursor: pointer;
          transition: background-color 0.2s;

          &:hover {
            background-color: #f5f7fa;
          }

          &:last-child {
            border-bottom: none;
          }

          .checkbox-column {
            width: 25px;
            flex-shrink: 0;
          }

          .name-column {
            width: 150px;
            flex-shrink: 0;
            font-weight: 500;
            color: #303133;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .org-column {
            width: 120px;
            flex-shrink: 0;
            color: #606266;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .personnel-column {
            flex: 1;
            color: #606266;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }

        .no-data {
          padding: 40px;
          text-align: center;
          color: #c0c4cc;
        }

        .loading-more {
          padding: 20px;
          text-align: center;
          color: #909399;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 8px;

          .el-icon-loading {
            animation: rotating 2s linear infinite;
          }
        }

        .no-more-data {
          padding: 10px;
          text-align: center;
          color: #c0c4cc;
          font-size: 12px;
        }
      }
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

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter,
.fade-leave-to {
  opacity: 0;
}

.table-body-scrollable::-webkit-scrollbar {
  width: 6px;
}

.table-body-scrollable::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.table-body-scrollable::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.table-body-scrollable::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

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