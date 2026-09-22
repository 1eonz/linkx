<template>
  <!-- 批量设置角色 -->
  <el-dialog
    v-if="dialogVisible"
    :visible.sync="dialogVisible"
    :close-on-click-modal="false"
    width="500px"
    custom-class="set-batch-role-dialog"
    :title="$t('index.operations.setRole')"
    @close="closeDialog"
  >
    <!-- 覆盖提示 -->
    <el-alert
      :title="$t('index.messageText.batchRoleOverrideWarning')"
      type="warning"
      show-icon
      :closable="false"
      style="margin-bottom: 16px; border-radius: 6px"
    />
    <el-form
      ref="tempForm"
      :model="form"
      label-position="left"
      :rules="rules"
      label-width="80px"
    >
      <!-- 已选用户（只读展示） -->
      <el-form-item
        :label="$t('index.list.compellation')"
        prop="relatedUserNames"
      >
        <el-input
          :value="form.relatedUserNames"
          disabled
          style="width: 100%"
        />
      </el-form-item>

      <!-- 角色（多选） -->
      <el-form-item :label="$t('index.list.role')" prop="roleIds">
        <el-tooltip
          :disabled="form.roleIds.length <= 1"
          placement="top"
          :content="selectedRoleNames"
        >
          <div>
            <el-select
              v-model="form.roleIds"
              multiple
              collapse-tags
              value-key="key"
              :placeholder="$t('index.operations.selects')"
              style="width: 100%"
            >
              <el-option
                v-for="item in roleList"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </div>
        </el-tooltip>
      </el-form-item>
    </el-form>

    <div slot="footer" class="dialog-footer">
      <el-button @click="dialogVisible = false">
        {{ $t('index.cancel') }}
      </el-button>
      <el-button type="primary" @click="handleConfirm()">
        {{ $t('index.operations.save') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import { getRoleList } from '@/api/resource/roleAdmin'
import { setBatchRole } from '@/api/resource/personAdmin'
import { APPLICATION_ID } from '@/appConfig'

export default {
  name: 'SetBatchRole',
  data() {
    const rules = {
      roleIds: [
        {
          required: true,
          type: 'array',
          min: 1,
          message: this.$t('index.messageText.roleCannotBeEmpty'),
          trigger: 'change'
        }
      ],
      relatedUserNames: [
        { required: true, message: this.$t('index.messageText.nameCannotBeEmpty'), trigger: 'blur' }
      ]
    }
    return {
      dialogVisible: false,
      rules,
      roleList: [],
      form: {
        relatedUserNames: '',
        relatedUserIds: [],
        roleIds: []
      }
    }
  },
  computed: {
    selectedRoleNames() {
      return this.form.roleIds
        .map(id => {
          const role = this.roleList.find(r => r.id === id)
          return role ? role.name : ''
        })
        .filter(Boolean)
        .join('、')
    }
  },
  methods: {
    async init(rowArr) {
      if (rowArr) {
        this.form.relatedUserNames = rowArr.map(item => item.name).join(',')
        this.form.relatedUserIds = rowArr.map(item => item.id)
      }
      await this.getList()
      this.dialogVisible = true
    },

    // 获取角色列表
    async getList() {
      const params = {
        applicationId: APPLICATION_ID.ADMIN,
        name: '',
        pageSize: 100,
        pageNo: 1
      }
      try {
        const roleList = await getRoleList(params)
        this.roleList = roleList.data?.records?.filter(item => item.status === 0)
      } catch (error) {
        console.error('获取角色列表失败:', error)
        this.$message.error(this.$t('index.messageText.operationFailed'))
      }
    },

    handleConfirm() {
      this.$refs['tempForm'].validate(valid => {
        if (!valid) return

        const params = {
          userIds: this.form.relatedUserIds,
          roleIds: this.form.roleIds
        }
        setBatchRole(params)
          .then(result => {
            if (result.code === 0) {
              this.$message({
                message: result.msg,
                type: 'success'
              })
              this.$emit('success')
              this.closeDialog()
            } else {
              this.$message({
                message: result.msg,
                type: 'error'
              })
            }
          })
          .catch(() => {
            this.$message.error(this.$t('index.messageText.operationFailed'))
          })
      })
    },

    closeDialog() {
      this.form.relatedUserNames = ''
      this.form.relatedUserIds = []
      this.form.roleIds = []
      this.$refs['tempForm'].resetFields()
      this.dialogVisible = false
    }
  }
}
</script>
<style lang="scss" scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 4px;
}
</style>

<style lang="scss">
.set-batch-role-dialog {
  border-radius: 10px;

  .el-dialog__header {
    padding: 20px 24px 16px;
    border-bottom: 1px solid #F0F2F5;
  }

  .el-dialog__body {
    padding: 20px 24px;
  }

  .el-dialog__footer {
    padding: 16px 24px 20px;
    border-top: 1px solid #F0F2F5;
  }
}
</style>
