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
              :key="item.id"
              :label="item.name"
              :value="item.peerId"
            >
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="目标组织" prop="orgName" style="width: 100%;">
          <select-tree-lazy
            v-if="formData.peerId"
            :key="formData.peerId"
            v-model="formData.orgName"
            class="col-select"
            style="width: 100%;"
            :is-init-value="true"
            :department-code="departmentCode"
            :peer-id="formData.peerId"
            placeholder="请选择目标组织"
            @clear-val="clearOrganizationType"
            @current-change="handleChangeOrg"
          />
          <el-input
            v-else
            v-model="formData.orgName"
            disabled
            placeholder="请先选择目标节点"
          />
        </el-form-item>
      </el-form>
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
import { shareCollaboration } from '@/api/h5/collaboration'
import { deepCopy } from '@/utils'
import selectTreeLazy from '@/components/SelectTreeLazy'
import { getServers } from '@/api/nodeManage/server'
const form = {
  coopUserId: undefined,
  postName: undefined,
  peerId: undefined,
  orgId: undefined,
  orgCode: undefined,
  orgName: undefined
}

export default {
  name: 'ShareForm',
  components: { selectTreeLazy },
  props: {
    isAdmin: {
      type: Boolean,
      default: false
    },
    departmentCode: {
      type: String,
      default: ''
    },
    version: {
      type: String,
      default: ''
    },
  },
  data() {
    return {
      dialogVisible: false,
      dialogTitle: '分享协同岗',
      formLoading: false,
      formData: deepCopy(form),
      formRules: {
        peerId: [
          { required: true, message: '目标节点不能为空', trigger: 'change' }
        ],
        orgName: [
          { required: true, message: '目标组织不能为空', trigger: 'change' }
        ]
      },
      peerOptions: []
    }
  },
  watch: {
    dialogVisible() {
      if (!this.dialogVisible) {
        this.$refs['formRef']?.resetFields()
      }
    }
  },
  mounted() {
    this.getPeerList()
  },
  methods: {
    // 获取服务器列表
    getPeerList() {
      getServers({
        pageNum: 1,
        pageSize: 100,
      }).then(res => {
        const records = res.data?.records || []
        this.peerOptions = records.filter(item => item.version === this.version)
      }).catch(() => {
        this.$message.error('获取服务器列表失败')
      })
    },
    // 打开弹窗
    open(row) {
      this.resetForm()
      this.formData.postName = row.postName
      this.formData.coopUserId = row.id
      this.dialogVisible = true
    },

    // 目标节点变化时，重置目标组织
    handlePeerChange() {
      this.formData.orgId = undefined
      this.formData.orgCode = undefined
      this.formData.orgName = undefined
      this.$refs.formRef.clearValidate('orgName')
    },

    // 选择目标组织
    handleChangeOrg(obj) {
      const { code, name, id } = obj
      this.formData = Object.assign({}, this.formData, {
        orgCode: code,
        orgId: id,
        orgName: name
      })
      this.$refs.formRef.clearValidate('orgName')
    },

    // 清空组织选择
    clearOrganizationType() {
      this.formData.orgId = undefined
      this.formData.orgCode = undefined
      this.formData.orgName = undefined
    },

    // 点击确定
    handleSubmit() {
      this.$refs['formRef'].validate(async valid => {
        if (!valid) return
        this.formLoading = true
        try {
          const { code, msg } = await shareCollaboration(
            this.formData.coopUserId,
            {
              peerId: this.formData.peerId,
              orgId: this.formData.orgId,
              orgName: this.formData.orgName
            }
          )
          this.$message({
            message: code === 0 ? '分享成功' : msg,
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
    }
  }
}
</script>
<style scoped lang="scss">
.col-select {
  margin-bottom: 0;
}
</style>
