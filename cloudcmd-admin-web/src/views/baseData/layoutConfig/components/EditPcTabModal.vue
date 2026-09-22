<template>
  <div class="edit-pc-tab">
    <el-form :model="form" ref="form" :rules="rules" label-width="100px">
      <el-form-item label="页签名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入页签名称" />
      </el-form-item>
      <el-form-item label="页签URL" prop="url">
        <el-input v-model="form.url" placeholder="请输入页签URL" />
      </el-form-item>
      <el-form-item label="排列顺序" prop="order">
        <el-input-number v-model="form.order" :min="1" placeholder="请输入排列顺序" style="width: 100%;" />
      </el-form-item>
      <el-form-item label="打开方式" prop="openWay">
        <el-switch
          v-model="form.openWay"
          :active-value="1"
          :inactive-value="0"
          active-text="弹窗"
          inactive-text="iframe嵌入"
        />
      </el-form-item>
    </el-form>
    <div class="tab-footer">
      <el-button @click="closeDialog">取消</el-button>
      <el-button type="primary" @click="handleSubmit">确定</el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'EditPcTabModal',
  props: {
    tabData: {
      type: Object,
      default: null
    },
    existingNames: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      form: {
        name: '',
        url: '',
        order: 1,
        openWay: 0
      },
      rules: {
        name: [{ required: true, message: '请输入页签名称', trigger: 'blur' }],
        url: [{ required: true, message: '请输入页签URL', trigger: 'blur' }],
        order: [{ required: true, message: '请输入排列顺序', trigger: 'blur' }]
      }
    }
  },
  mounted() {
    if (this.tabData) {
      this.form.name = this.tabData.name || ''
      this.form.url = this.tabData.url || ''
      this.form.order = this.tabData.order || 1
      // 老数据无 openWay 字段时默认为嵌入（0）
      this.form.openWay = this.tabData.openWay != null ? this.tabData.openWay : 0
    }
  },
  methods: {
    // 提交表单
    async handleSubmit() {
      this.$refs.form.validate(async (valid) => {
        if (!valid) return
        // 名称重复校验
        if (this.existingNames.includes(this.form.name)) {
          return this.$message.warning('页签名称已存在，请使用其他名称')
        }
        this.$emit('ok', { ...this.form })
      })
    },
    // 关闭弹窗
    closeDialog() {
      this.$emit('cancel')
    }
  }
}
</script>

<style scoped>
.edit-pc-tab {
  padding: 10px;
}
.tab-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>