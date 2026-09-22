<template>
  <el-dialog title="新建分类" :visible.sync="dialogVisible" width="320px" @close="handleClose">
    <el-form label-width="60px">
      <div ref="scrollBox" class="form-item-contain">
        <el-form-item v-for="(item, index) in editTypeList" :key="index" :label="`分类${index + 1}`">
          <div class="type-item">
            <el-input v-model="item.name" maxlength="8" />
            <i v-if="editTypeList.length > 1" class="el-icon-delete type-delete" @click="removeType(index)" />
          </div>
        </el-form-item>
      </div>
    </el-form>
    <el-button style="width: 100%" @click="addType">+添加</el-button>
    <span slot="footer" class="dialog-footer">
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="confirmAddType">确定</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { createCategory, deleteCategory } from '@/api/thirdInterface/agentInterface.js'

export default {
  name: 'AgentTypeEditModal',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    typeList: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      editTypeList: []
    }
  },
  computed: {
    dialogVisible: {
      get() {
        return this.visible
      },
      set(val) {
        this.$emit('update:visible', val)
      }
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.editTypeList = this.typeList.map(item => ({ id: item.id, name: item.name }))
      }
    }
  },
  methods: {
    addType() {
      this.editTypeList.push({ name: '' })
      this.$nextTick(() => {
        if (this.$refs.scrollBox) {
          this.$refs.scrollBox.scrollTop = this.$refs.scrollBox.scrollHeight
        }
      })
    },
    removeType(index) {
      const item = this.editTypeList[index]
      if (item.id) {
        this.$confirm('确认删除该分类吗？', '请确认', {
          confirmButtonText: '确认',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(async () => {
          try {
            const res = await deleteCategory(item.id)
            if (res.code === 0) {
              this.$message.success('删除成功')
              this.editTypeList.splice(index, 1)
              this.$emit('success')
            } else {
              this.$message.error(res.msg || '删除失败')
            }
          } catch (error) {
            this.$message.error('删除失败')
          }
        }).catch(() => {})
      } else {
        this.editTypeList.splice(index, 1)
      }
    },
    async confirmAddType() {
      const data = this.editTypeList.filter(item => item.name || item.id)
      if (data.some(item => item.name === '')) {
        this.$message.warning('分类名称不能为空')
        return
      }
      const nameArr = data.map(item => item.name)
      if (new Set(nameArr).size < nameArr.length) {
        this.$message.warning('分类名称不能重复')
        return
      }
      if (data.length === 0) {
        this.$message.warning('没有新增分类')
        return
      }
      try {
        const res = await createCategory(data)
        if (res.code === 0) {
          this.$message.success('保存成功')
          this.dialogVisible = false
          this.$emit('success')
        } else {
          this.$message.error(res.msg || '保存失败')
        }
      } catch (error) {
        this.$message.error('保存失败')
      }
    },
    handleClose() {
      this.editTypeList = []
    }
  }
}
</script>

<style scoped>
.form-item-contain {
  max-height: 300px;
  overflow-y: auto;
}

.type-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.type-delete {
  cursor: pointer;
  color: #F56C6C;
  font-size: 16px;
  flex-shrink: 0;
}
</style>
