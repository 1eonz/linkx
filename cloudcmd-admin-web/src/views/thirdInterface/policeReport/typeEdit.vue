<template>
  <div class="type-edit">
    <el-dialog
      :visible.sync="dialogVisible"
      :title="dialogTitle"
      @close="closeDialog"
      class="edit-dock"
    >
      <div class="content-box">
        <el-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-position="left"
          label-width="200px"
          style="width: 100%; padding:0 30px;box-sizing:border-box"
          @submit.native.prevent
        >
          <el-form-item label="类型名称" prop="tag">
            <el-input
              v-model="formData.tag"
              maxlength="20"
              show-word-limit
              placeholder="请输入"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="closeDialog">取 消</el-button>
        <el-button type="primary" :disabled="formLoading" @click="handleSubmit">
          确 定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { deepCopy } from '@/utils'
import {
  savePolicetickettype,
  updatePolicetickettypes
} from '@/api/policeReport/dock'
const form = {
  tag: ''
}
export default {
  name: 'type-edit',
  data() {
    return {
      dialogVisible: false,
      formData: deepCopy(form),
      formType: '',
      formLoading: false,
      dialogTitle: '',
      formRules: {
        tag: [{ required: true, message: '类型名称不能为空', trigger: 'blur' }]
      }
    }
  },
  mounted() {},
  methods: {
    async open(type, row) {
      this.dialogTitle = type === 'create' ? '新增' : '修改'
      this.formType = type
      this.formLoading = true
      this.resetForm()
      if (row) {
        this.formData = { ...row }
        this.formData.id = row.id
      } else {
        this.formData = deepCopy(form)
      }
      this.formLoading = false
      this.dialogVisible = true
    },
    resetForm() {
      this.formData = deepCopy(form)
    },
    clearFormAndValidation() {
      this.$nextTick(() => {
        if (this.$refs.formRef) {
          this.$refs.formRef.resetFields()
          this.$refs.formRef.clearValidate()
        }
        this.resetForm()
        this.formType = ''
        this.formLoading = false
      })
    },
    closeDialog() {
      this.dialogVisible = false
      this.clearFormAndValidation()
    },
    handleSubmit() {
      this.$refs['formRef'].validate(valid => {
        if (valid) {
          this.handleRequest()
        } else {
          return false
        }
      })
    },
    async handleRequest() {
      let res
      if (this.formType === 'create') {
        res = await savePolicetickettype({ ...this.formData })
      } else {
        res = await updatePolicetickettypes({ ...this.formData })
      }
      const { code, msg } = res
      this.handleVisible(code)
      if (code === 0) {
        this.$message({
          message: this.formType === 'create' ? '新增成功' : '修改成功',
          type: 'success'
        })
        return
      }
      const title =
        this.formType === 'create'
          ? '新增失败，请稍后重试'
          : '修改失败，请稍后重试'
      this.$message({
        message: msg || title,
        type: 'error'
      })
    },
    handleVisible(code) {
      if (code === 0) {
        this.dialogVisible = false
        this.clearFormAndValidation()
        this.$emit('success')
      } else {
        this.dialogVisible = true
      }
      this.formLoading = false
    }
  }
}
</script>