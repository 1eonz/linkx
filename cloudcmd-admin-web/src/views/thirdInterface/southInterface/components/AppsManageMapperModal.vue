<template>
  <el-dialog :visible.sync="dialogVisible" title="字段映射配置" width="800px" :close-on-click-modal="false"
    @close="handleClose">
    <div class="mapper-container">
      <div class="mapper-header">
        <el-button type="primary" size="small" @click="handleAdd">
          <i class="el-icon-plus"></i>
          添加映射
        </el-button>
        <el-button size="small" @click="handleReset" :disabled="mapperList.length === 0">
          <i class="el-icon-delete"></i>
          清空所有
        </el-button>
        <span class="mapper-tip">提示：字段名(key)用于展示字段，字段含义(value)用于展示字段的列标题</span>
      </div>

      <div class="mapper-list" v-if="mapperList.length > 0">
        <div v-for="(item, index) in mapperList" :key="index" class="mapper-item">
          <div class="mapper-item-fields">
            <el-input v-model="item.key" placeholder="请输入" size="medium" :maxlength="100" show-word-limit @blur="handleKeyBlur(index)">
              <template slot="prepend">字段名</template>
            </el-input>

            <i class="el-icon-arrow-right arrow-icon"></i>

            <el-input v-model="item.value" placeholder="请输入" size="medium" :maxlength="100" show-word-limit @blur="handleValueBlur(index)">
              <template slot="prepend">字段含义</template>
            </el-input>
          </div>

          <div class="mapper-item-actions">
            <el-button plain type="primary" size="small" icon="el-icon-top" circle @click="handleMoveUp(index)"
              :disabled="index === 0" />
            <el-button plain type="primary" size="small" icon="el-icon-bottom" circle @click="handleMoveDown(index)"
              :disabled="index === mapperList.length - 1" />
            <el-button plain type="danger" size="small" icon="el-icon-delete" circle @click="handleRemove(index)" />
          </div>
        </div>
      </div>

      <el-empty v-if="mapperList.length === 0" description="暂无映射配置，请点击添加映射" :image-size="80" class="mapper-empty" />
    </div>

    <div slot="footer" class="dialog-footer">
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </div>
  </el-dialog>
</template>

<script>
// 字段名校验 (字母/下划线开头，仅含字母、数字、下划线)
const KEY_REGEX = /^[a-zA-Z_][a-zA-Z0-9_]*$/
// 创建mapperItem
const createMapperItem = (key = '', value = '') => ({ key, value })
export default {
  name: 'AppsManageMapperModal',
  data() {
    return {
      dialogVisible: false,
      mapperList: [],
      currentRow: null,
      currentType: null
    }
  },
  mounted() {

  },
  methods: {
    // 根据当前行数据初始化映射列表，优先从currentRow中取mapper字段
    initMapperData() {
      let parsedMapper = []
      try { parsedMapper = JSON.parse(this.currentRow.mapper)} catch(e) {}
      this.mapperList = parsedMapper
    },

    handleAdd() {
      this.mapperList.push(createMapperItem())
    },

    handleRemove(index) {
      this.mapperList.splice(index, 1)
    },

    handleReset() {
      this.$confirm('确定要清空所有映射配置吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.mapperList = []
      }).catch(() => { })
    },

    // 交换两个位置的映射项（用于上移/下移）
    swapItems(indexA, indexB) {
      const temp = this.mapperList[indexA]
      this.$set(this.mapperList, indexA, this.mapperList[indexB])
      this.$set(this.mapperList, indexB, temp)
    },
    // 上移
    handleMoveUp(index) {
      if (index > 0) this.swapItems(index, index - 1)
    },
    // 下移
    handleMoveDown(index) {
      if (index < this.mapperList.length - 1) this.swapItems(index, index + 1)
    },

    // 字段名失焦：自动trim并校验唯一性和格式
    handleKeyBlur(index) {
      const item = this.mapperList[index]
      if (!item.key || !item.key.trim()) return
      const trimmedKey = item.key.trim()

      const isDuplicate = this.mapperList.some((other, idx) =>
        idx !== index && other.key === trimmedKey
      )
      if (isDuplicate) {
        this.$message.warning(`字段名 "${trimmedKey}" 已存在，请使用唯一的字段名`)
      }
      if (!KEY_REGEX.test(trimmedKey)) {
        this.$message.warning('字段名只能包含字母、数字、下划线，且不能以数字开头')
      }

      item.key = trimmedKey
    },

    // 字段含义失焦：自动trim
    handleValueBlur(index) {
      const item = this.mapperList[index]
      if (item.value) {
        item.value = item.value.trim()
      }
    },

    // 保存前全量校验：空值、重复、格式
    validateData() {
      if (this.mapperList.length === 0) {
        this.$message.warning('请至少添加一个字段映射')
        return false
      }

      const keys = new Set()
      for (let i = 0; i < this.mapperList.length; i++) {
        const item = this.mapperList[i]
        const trimmedKey = (item.key || '').trim()
        const trimmedValue = (item.value || '').trim()
        const rowNum = i + 1

        if (!trimmedKey) {
          this.$message.warning(`第 ${rowNum} 行的字段名不能为空`)
          return false
        }
        if (!trimmedValue) {
          this.$message.warning(`第 ${rowNum} 行的字段含义不能为空`)
          return false
        }
        if (keys.has(trimmedKey)) {
          this.$message.warning(`字段名 "${trimmedKey}" 重复，请使用唯一的字段名`)
          return false
        }
        if (!KEY_REGEX.test(trimmedKey)) {
          this.$message.warning(`字段名 "${trimmedKey}" 格式错误，只能包含字母、数字、下划线，且不能以数字开头`)
          return false
        }

        keys.add(trimmedKey)
      }

      return true
    },

    // 保存映射配置，将列表转为JSON对象并emit给父组件
    handleSave() {
      if (!this.validateData()) return
      const saveData = {
        mapper: this.mapperList ? JSON.stringify(this.mapperList) : '',
        mapperList: this.mapperList,
        type: this.currentType
      }
      if (this.currentRow) {
        saveData.id = this.currentRow.id
        saveData.row = this.currentRow
      }
      this.$emit('save', saveData)
      this.dialogVisible = false
    },

    // 关闭弹窗并重置内部状态
    handleClose() {
      this.dialogVisible = false
      this.resetData()
    },

    // 重置所有内部状态，防止弹窗间数据残留
    resetData() {
      this.mapperList = []
      this.currentRow = null
      this.currentType = null
    },
    // 供父组件通过ref调用，打开弹窗并回显当前行的映射数据
    open(type, row) {
      this.currentType = type
      this.currentRow = row
      this.dialogVisible = true
      this.initMapperData()
    }
  }
}
</script>

<style scoped>
.mapper-container {
  display: flex;
  flex-direction: column;
  height: 500px;
}

.mapper-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e4e7ed;
  flex-wrap: wrap;
}

.mapper-header .mapper-tip {
  font-size: 12px;
  color: #a9a9af;
  margin-left: auto;
  align-self: flex-end;
}

.mapper-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  gap: 4px;
  flex-direction: column;
  padding-right: 8px;
}

.mapper-list::-webkit-scrollbar {
  width: 6px;
}

.mapper-list::-webkit-scrollbar-thumb {
  background-color: #dcdfe6;
  border-radius: 3px;
}

.mapper-list::-webkit-scrollbar-thumb:hover {
  background-color: #c0c4cc;
}

.mapper-item {
  display: flex;
  align-items: center;
  padding: 8px;
  border-radius: 8px;
  transition: all 0.3s;
}

.mapper-item:hover {
  background-color: #f9fbff;
}

.mapper-item .mapper-item-fields {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
}

.mapper-item .mapper-item-fields :deep(.el-input-group__prepend) {
  width: 70px;
  justify-content: center;
}

.mapper-item .arrow-icon {
  color: #909399;
  font-size: 20px;
  flex-shrink: 0;
}

.mapper-item .mapper-item-actions {
  display: flex;
  flex-shrink: 0;
  margin-left: 16px;
}

.mapper-container :deep(.el-empty) {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.mapper-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
