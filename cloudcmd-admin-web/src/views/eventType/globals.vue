<template>
  <div class="app-container globals-page">
    <!-- 顶部：搜索栏 + 操作按钮 -->
    <div class="globals-page__toolbar">
      <div class="globals-page__search">
        <el-input
          v-model="keyword"
          placeholder="请输入配置项 / 参数名 / 备注 进行搜索"
          clearable
          prefix-icon="el-icon-search"
          style="width: 360px;"
          @keyup.enter.native="handleSearch"
          @input="handleKeywordInput"
          @clear="handleSearch"
        />
        <el-button
          v-waves
          type="primary"
          icon="el-icon-search"
          @click="handleSearch"
        >
          {{ $t('index.operations.search') }}
        </el-button>
        <el-button
          v-waves
          icon="el-icon-refresh"
          @click="handleReset"
        >
          {{ $t('index.operations.reset') }}
        </el-button>
      </div>
      <el-button
        v-if="hasPerm('/admin/globals/create')"
        type="primary"
        icon="el-icon-circle-plus-outline"
        @click="handleCreate()"
      >
        {{ $t('index.operations.Added') }}
      </el-button>
    </div>

    <!-- 分类胶囊：前端过滤 -->
    <div v-if="classifyChips.length > 1" class="globals-page__chips">
      <button
        v-for="chip in classifyChips"
        :key="chip.key"
        type="button"
        :class="[
          'globals-chip',
          { 'is-active': activeClassify === chip.key }
        ]"
        @click="handleClassifyChange(chip.key)"
      >
        <span class="globals-chip__label">{{ chip.label }}</span>
        <span class="globals-chip__count">{{ chip.count }}</span>
      </button>
    </div>

    <!-- 表格 -->
    <el-table
      v-loading="listLoading"
      :data="tableData"
      :span-method="handleSpanMethod"
      border
      fit
      highlight-current-row
      style="width: 100%;"
      class="globals-table"
    >
      <el-table-column
        label="类别"
        prop="classify"
        width="180"
        align="center"
        class-name="classify-cell"
      >
        <template slot-scope="scope">
          <div class="classify-block">
            <span class="classify-block__name">{{ scope.row.classify || '未分类' }}</span>
            <el-tag
              class="classify-block__count"
              size="mini"
              type="info"
              effect="plain"
            >
              {{ classifyCountMap[scope.row.classify || '未分类'] }}
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column
        label="序号"
        width="80"
        align="center"
        class-name="index-cell"
      >
        <template slot-scope="scope">
          <span class="index-text">{{ getRowIndex(scope.row, scope.$index) }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('index.list.ConfigurationItem')"
        :show-overflow-tooltip="true"
        min-width="100"
        align="center"
      >
        <template slot-scope="scope">
          <span>{{ scope.row.remarkEn }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('index.list.ConfigurationParameters')"
        :show-overflow-tooltip="true"
        min-width="140"
        align="center"
      >
        <template slot-scope="scope">
          <span>{{ scope.row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('index.operations.configurationValue')"
        :show-overflow-tooltip="true"
        min-width="140"
        align="center"
      >
        <template slot-scope="scope">
          <span class="config-value">{{ scope.row.value }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('index.list.remarks')"
        :show-overflow-tooltip="true"
        min-width="120"
        align="center"
      >
        <template slot-scope="scope">
          <span>{{ scope.row.remark }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('index.list.condition')"
        class-name="status-col"
        min-width="80"
        align="center"
      >
        <template slot-scope="scope">
          <el-tag :type="scope.row.status | statusFilter">
            {{
              scope.row.status === 0
                ? $t('index.list.normal')
                : $t('index.list.forbidden')
            }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        fixed="right"
        header-align="center"
        align="center"
        min-width="160"
        :label="$t('index.operations.operation')"
      >
        <template slot-scope="scope">
          <el-button
            v-if="hasPerm('/admin/globals/update')"
            type="primary"
            icon="el-icon-edit"
            size="small"
            @click="handleUpdate(scope.row)"
          >
            {{ $t('index.operations.redact') }}
          </el-button>
          <el-button
            v-if="scope.row.status === 0 && hasPerm('/admin/globals/delete')"
            type="danger"
            icon="el-icon-delete"
            size="small"
            @click="handleDelete(scope.row)"
          >
            {{ $t('index.delete') }}
          </el-button>
          <el-button
            v-else-if="
              scope.row.status !== 0 && hasPerm('/admin/globals/update')
            "
            type="warning"
            icon="el-icon-star-on"
            size="small"
            @click="resuming(scope.row)"
          >
            {{ $t('index.operations.restore') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 空状态 -->
    <div v-if="!listLoading && tableData.length === 0" class="globals-page__empty">
      <i class="el-icon-warning-outline"></i>
      <span>未找到匹配的配置项</span>
    </div>

    <globals-config ref="config" @refresh="getList" />
  </div>
</template>

<script>
import {
  deleteGlobals,
  getGlobalsList,
  updateGlobals
} from '@/api/dictionary/globals'
import globalsConfig from './components/globalsConfig'

export default {
  name: 'Globals',
  filters: {
    statusFilter(status) {
      const statusMap = {
        '0': 'success',
        '1': 'danger'
      }
      return statusMap[status]
    }
  },
  components: { globalsConfig },
  data() {
    return {
      list: [],
      listLoading: false,
      dialogStatus: this.$t('index.operations.addedGlobalConfigurationItems'),
      keyword: '',
      // 防抖定时器
      debounceTimer: null,
      // 当前选中的分类：'__ALL__' 表示全部
      activeClassify: '__ALL__',
      // 不展示的项
      blackList: [
        'H5URL',
        'SHOW_331_FEATURE',
        'ICP_SDKSERVER_WS_URI',
        'ICP_ACCOUNT',
        'ICP_PASSWORD',
        'ICP_SDKSERVER_HTTP_HOST',
        'title',
        'FILE_STORAGE_IM',
        'GUANGTIE_430_FEATURE_SWITCH',
        'DBPATH',
        'AI_SEPARATED_DEPLOY',
        'GROUP_AI_FRONTEND_HOST'
      ]
    }
  },
  computed: {
    // 分类胶囊数据：第一个是"全部"，后面是各分类
    classifyChips() {
      const map = new Map()
      this.list.forEach(item => {
        const key = item.classify || '未分类'
        map.set(key, (map.get(key) || 0) + 1)
      })
      const allOption = { key: '__ALL__', label: '全部', count: this.list.length }
      const chips = [allOption]
      map.forEach((count, key) => {
        chips.push({ key, label: key, count })
      })
      return chips
    },
    // 表格展示数据：按 activeClassify 过滤
    tableData() {
      if (this.activeClassify === '__ALL__') {
        return this.list
      }
      return this.list.filter(item => {
        const key = item.classify || '未分类'
        return key === this.activeClassify
      })
    },
    // 每个类别下的项数统计（用于合并行内的徽标）
    classifyCountMap() {
      const map = {}
      this.list.forEach(item => {
        const key = item.classify || '未分类'
        map[key] = (map[key] || 0) + 1
      })
      return map
    },
    // 每个分类的编号映射（用于序号格式 X.Y 中的 X）
    // 仅统计当前 tableData 中可见的分类，保证切换胶囊过滤后编号连续
    classifyIndexMap() {
      const map = {}
      let classifyIdx = 0
      this.tableData.forEach(item => {
        const key = item.classify || '未分类'
        if (!(key in map)) {
          classifyIdx++
          map[key] = classifyIdx
        }
      })
      return map
    }
  },
  created() {
    this.getList()
  },
  beforeDestroy() {
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer)
      this.debounceTimer = null
    }
  },
  methods: {
    /**
     * 计算行的分类内序号（格式 X.Y）
     * X = 分类编号（从当前可见 tableData 中按出现顺序编号）
     * Y = 该分类内的项序号（从 1 开始）
     * 例：第 1 个分类的第 2 项 → "1.2"
     *
     * @param {Object} row - 当前行数据
     * @param {Number} rowIndex - 当前行在 tableData 中的索引
     * @returns {String} 形如 "1.2" 的序号
     */
    getRowIndex(row, rowIndex) {
      const classifyKey = row.classify || '未分类'
      const classifyIdx = this.classifyIndexMap[classifyKey] || 1
      // 计算当前行是该分类内的第几项（从 1 开始）
      let itemIdxInClassify = 1
      for (let i = 0; i < rowIndex; i++) {
        const prevRow = this.tableData[i]
        if (!prevRow) continue
        const prevKey = prevRow.classify || '未分类'
        if (prevKey === classifyKey) {
          itemIdxInClassify++
        }
      }
      return `${classifyIdx}.${itemIdxInClassify}`
    },

    async getList() {
      this.listLoading = true
      try {
        const params = {}
        if (this.keyword && this.keyword.trim()) {
          params.keyword = this.keyword.trim()
        }
        const { data } = await getGlobalsList(params)
        // 前端过滤黑名单项（兜底，后端应已过滤）
        this.list = (data || []).filter(item => !this.blackList.includes(item.name))
        // 搜索后回退：当前选中的分类下没有数据时，自动切回"全部"
        if (this.activeClassify !== '__ALL__') {
          const hasMatchInActiveClassify = this.list.some(item => {
            const key = item.classify || '未分类'
            return key === this.activeClassify
          })
          if (!hasMatchInActiveClassify) {
            this.activeClassify = '__ALL__'
          }
        }
      } finally {
        this.listLoading = false
      }
    },

    // 关键词输入防抖：300ms
    handleKeywordInput() {
      if (this.debounceTimer) {
        clearTimeout(this.debounceTimer)
      }
      this.debounceTimer = setTimeout(() => {
        this.handleSearch()
      }, 300)
    },

    handleSearch() {
      if (this.debounceTimer) {
        clearTimeout(this.debounceTimer)
        this.debounceTimer = null
      }
      this.getList()
    },

    handleReset() {
      this.keyword = ''
      this.activeClassify = '__ALL__'
      this.handleSearch()
    },

    // 切换分类胶囊：前端过滤
    handleClassifyChange(key) {
      if (this.activeClassify === key) return
      this.activeClassify = key
    },

    /**
     * el-table 合并行方法
     * 类别列（columnIndex === 0）按 classify 字段合并
     * 序号列（columnIndex === 1）不合并，每行独立显示 X.Y
     */
    handleSpanMethod({ row, column, rowIndex, columnIndex }) {
      // 仅对类别列（第一列，columnIndex === 0）做合并
      if (columnIndex !== 0) return
      const data = this.tableData
      if (!data || data.length === 0) return

      const currentClassify = row.classify || '未分类'

      // 当前是同类别第一行
      const isFirstOfGroup =
        rowIndex === 0 ||
        (data[rowIndex - 1].classify || '未分类') !== currentClassify

      if (isFirstOfGroup) {
        // 计算该类别后续连续多少行
        let spanCount = 1
        for (let i = rowIndex + 1; i < data.length; i++) {
          if ((data[i].classify || '未分类') === currentClassify) {
            spanCount++
          } else {
            break
          }
        }
        return {
          rowspan: spanCount,
          colspan: 1
        }
      } else {
        // 当前行被合并到上面，返回 0 0
        return {
          rowspan: 0,
          colspan: 0
        }
      }
    },

    handleCreate() {
      this.$refs['config'].open()
    },
    handleUpdate(row) {
      this.$refs['config'].open(row)
    },
    handleDelete(row) {
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          deleteGlobals(row.id).then(result => {
            if (result.data === 'success') {
              this.$message({
                message: result.msg,
                type: 'success'
              })
            } else {
              this.$message({
                message: result.msg,
                type: 'error'
              })
            }
            this.getList()
          })
        })
        .catch(() => {})
    },
    resuming(row) {
      const params = { ...row, status: 0 }
      updateGlobals(params).then(result => {
        if (result.code === 0) {
          this.getList()
        } else {
          this.$message({
            message: this.$t('index.statusTitle.enabledSucceed'),
            type: 'error'
          })
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.globals-page {
  padding: 20px;
}

.globals-page__toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.globals-page__search {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
}

// 分类胶囊：前端过滤
.globals-page__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 4px 0 16px;
  margin-bottom: 4px;
  border-bottom: 1px solid #f0f2f5;
}

.globals-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 16px;
  background: #ffffff;
  color: #606266;
  font-size: 13px;
  line-height: 1;
  cursor: pointer;
  transition: all 0.18s ease;

  &:hover {
    border-color: #409eff;
    color: #409eff;
  }

  &.is-active {
    background: #409eff;
    border-color: #409eff;
    color: #ffffff;

    .globals-chip__count {
      background: rgba(255, 255, 255, 0.25);
      color: #ffffff;
    }
  }

  &:focus-visible {
    outline: 2px solid #409eff;
    outline-offset: 2px;
  }
}

.globals-chip__label {
  font-weight: 500;
}

.globals-chip__count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  background: #f0f2f5;
  color: #909399;
  border-radius: 9px;
  font-size: 11px;
  font-weight: 600;
  transition: all 0.18s ease;
}

.globals-page__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 0;
  color: #909399;
  font-size: 14px;

  i {
    font-size: 40px;
    margin-bottom: 8px;
  }
}

// 序号列样式
.globals-table ::v-deep .index-cell {
  background-color: #fafbfc;
}

.index-text {
  display: inline-block;
  min-width: 28px;
  padding: 2px 8px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.02em;
}

// 表格类别列样式（合并单元格后第一列）
.globals-table ::v-deep .classify-cell {
  vertical-align: middle;
  background-color: #fafbfc;
}

.classify-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 4px 0;

  &__name {
    font-weight: 600;
    color: #303133;
    font-size: 14px;
  }

  &__count {
    font-weight: 400;
  }
}

.config-value {
  font-family: Menlo, Monaco, Consolas, 'Courier New', monospace;
  color: #409eff;
}
</style>
