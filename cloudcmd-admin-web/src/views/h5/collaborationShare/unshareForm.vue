<template>
  <div>
    <el-dialog
      :visible.sync="dialogVisible"
      :title="dialogTitle"
      width="400px"
      custom-class="adaptive-dialog"
      @close="closeDialog"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-position="left"
        label-width="100px"
        class="dialog-form"
      >
        <el-form-item label="协同岗名称" prop="postName">
          <el-input
            v-model="formData.postName"
            disabled
            placeholder="协同岗名称"
          />
        </el-form-item>
        <el-form-item label="目标节点" prop="peerId">
          <el-select
            v-model="formData.peerId"
            placeholder="请选择"
            @change="handlePeerChange"
          >
            <el-option
              v-for="item in peerOptions"
              :key="item.peerId"
              :label="item.peerName"
              :value="item.peerId"
            >
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog">取 消</el-button>
        <el-button
          type="primary"
          :disabled="formLoading"
          @click="handleSubmit"
        >
          确 定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { unshareCollaboration, getSharedNodes } from '@/api/h5/collaboration'
import { deepCopy } from '@/utils'

const form = {
  coopUserId: undefined,
  postName: undefined,
  peerId: undefined
}

export default {
  name: 'UnshareForm',
  data() {
    return {
      dialogVisible: false,
      dialogTitle: '取消分享协同岗',
      formLoading: false,
      formData: deepCopy(form),
      // 节点选项（来自分享记录）
      peerOptions: [],
      formRules: {
        peerId: [
          { required: true, message: '目标节点不能为空', trigger: 'change' }
        ]
      }
    }
  },
  watch: {
    dialogVisible() {
      if (!this.dialogVisible) {
        this.$refs['formRef']?.resetFields()
      }
    }
  },
  methods: {
    // 打开弹窗
    // row.id: 协同岗ID, row.postName/coopUserName: 协同岗名称
    async open(row) {
      this.resetForm()
      this.formData.postName = row.postName || row.coopUserName
      this.formData.coopUserId = row.id
      this.dialogVisible = true
      await this.loadSharedNodes(row.id)
    },

    // 加载分享记录中的节点信息
    async loadSharedNodes(coopUserId) {
      try {
        const res = await getSharedNodes(coopUserId)
        this.peerOptions = res.data || []
      } catch (error) {
        this.$message.error('获取分享记录失败')
      }
    },

    // 目标节点变化
    handlePeerChange() {
      this.$refs.formRef.clearValidate('peerId')
    },

    // 点击确定
    handleSubmit() {
      this.$refs['formRef'].validate(async valid => {
        if (!valid) return
        this.formLoading = true
        try {
          const { code, msg } = await unshareCollaboration(
            this.formData.coopUserId,
            { peerId: this.formData.peerId }
          )
          this.$message({
            message: code === 0 ? '取消分享成功' : msg,
            type: code === 0 ? 'success' : 'error'
          })
          if (code === 0) {
            this.dialogVisible = false
            this.$emit('success')
          }
        } finally {
          this.formLoading = false
        }
      })
    },

    closeDialog() {
      this.dialogVisible = false
    },

    resetForm() {
      this.formData = deepCopy(form)
      this.peerOptions = []
    }
  }
}
</script>
