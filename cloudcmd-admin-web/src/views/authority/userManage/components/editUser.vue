<template>
  <!-- 新增/编辑管理员 -->
  <el-dialog
    :title="isAdd ? '新增管理员' : '编辑管理员'"
    :visible.sync="dialogVisible"
    :close-on-click-modal="false"
    width="900px"
    @close="closeDialog"
    @opened="initTreeCheckedState"
  >
    <div v-loading="detailLoading">
    <el-form
      ref="userForm"
      :model="userForm"
      :rules="rules"
      label-width="100px"
      class="userForm"
      label-position="left"
    >
      <el-form-item label="用户名称" prop="idCard">
        <el-input
          v-model="userForm.idCard"
          :disabled="!isAdd"
          placeholder="请输入用户名称"
          maxlength="20"
          @blur="handleIdCardBlur"
        />
      </el-form-item>
    </el-form>
    <div class="data-auth-section">
      <div class="data-auth-title">{{ $t('index.DataAuth') }}</div>
      <div class="data-auth-tree-wrapper">
        <!-- 数据权限穿梭树 -->
        <DataPermissionTree
          ref="dataAuthTree"
          :check-strictly="true"
          :default-checked-keys="userForm.dataAuthTreecheckedKeys"
          :show-node-select-all="true"
          :show-node-cancel-all="false"
          height="100%"
          left-title="部门列表"
          right-title="已选择"
          filter-placeholder="搜索部门"
          @change="changeDataAuthCheckKeys"
        />
      </div>
    </div>
    </div>
    <div slot="footer" class="dialog-footer">
      <el-button @click="closeDialog">
        {{ $t('index.cancel') }}
      </el-button>
      <el-button type="primary" :loading="confirmLoading" @click="handleConfirm">
        {{ $t('index.determine') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import { createUser, updateUser, getUserById } from '@/api/resource/person'
import DataPermissionTree from '../../components/DataPermissionTree.vue'
export default {
  name: 'EditUser',
  components: { DataPermissionTree },
  data() {
    const rules = {
      idCard: [
        {
          required: true,
          message: '用户名称不能为空',
          trigger: 'blur'
        },
        {
          pattern: /^[^\u4e00-\u9fa5\s]+$/,
          message: '不支持中文和空格，请输入英文、数字或特殊字符',
          trigger: 'blur'
        }
      ]
    }
    return {
      dialogVisible: false,
      isAdd: true,
      userForm: {
        idCard: '',
        orgIds: [],
        orgList: [],
        dataAuthTreecheckedKeys: []
      },
      rules: Object.freeze(rules),
      confirmLoading: false,
      detailLoading: false
    }
  },
  methods: {
    // 弹窗打开后恢复数据权限选中状态
    initTreeCheckedState() {
      // 数据获取完成后由 init 方法调用 syncFromDetail，此处不再处理
    },

    // 数据获取完成后同步选中状态到树组件
    syncTreeCheckedState() {
      if (this.$refs.dataAuthTree) {
        const normalized = this.$refs.dataAuthTree.syncFromDetail({
          orgList: this.userForm.orgList,
          orgIds: this.userForm.orgIds
        })
        if (normalized && normalized.length) {
          this.userForm.orgList = normalized
        }
      }
    },

    // 初始化
    async init(row) {
      this.isAdd = !row
      this.userForm.idCard = ''
      this.userForm.orgIds = []
      this.userForm.orgList = []
      this.userForm.dataAuthTreecheckedKeys = []
      this.dialogVisible = true
      if (row) {
        this.userForm['id'] = row.id
        this.detailLoading = true
        try {
          const res = await getUserById(row.id)
          if (res && res.code === 0 && res.data) {
            const { idCard, orgIds, orgList } = res.data
            this.userForm.idCard = idCard || ''
            if (orgList && orgList.length > 0) {
              // 新接口返回 orgList，直接使用新格式
              this.userForm.orgList = orgList
              this.userForm.orgIds = orgIds || []
              this.userForm.dataAuthTreecheckedKeys = []
            } else {
              // 旧接口只有 orgIds，由 initCheckedKeys 处理回显，不设置 default-checked-keys 避免冲突
              this.userForm.orgList = []
              this.userForm.orgIds = orgIds || []
              this.userForm.dataAuthTreecheckedKeys = []
            }
            // 数据获取完成后同步选中状态到树组件
            this.$nextTick(() => {
              this.syncTreeCheckedState()
            })
          }
        } catch (e) {
          // 忽略异常
        } finally {
          this.detailLoading = false
        }
      }
    },

    // 数据权限选中变化处理
    changeDataAuthCheckKeys(checkedDetail) {
      console.log(checkedDetail, 'checkedDetail')
      // change 事件返回 [{id, name, path}]
      this.userForm.orgList = checkedDetail
      this.userForm.orgIds = checkedDetail.map(item => item.id)
    },

    // 点击确认
    handleConfirm() {
      this.$refs['userForm'].validate(async valid => {
        if (valid) {
          // change 事件已正确处理 pendingKeys，直接使用 orgIds 和 orgList
          const finalOrgIds = this.userForm.orgIds
          const finalOrgList = this.userForm.orgList

          if (finalOrgIds.length === 0) {
            this.$message.warning('请至少选择一个数据权限')
            return
          }

          this.confirmLoading = true
          const param = {
            idCard: this.userForm.idCard,
            orgIds: finalOrgIds,
            orgList: finalOrgList
          }
          if (!this.isAdd && this.userForm.id) {
            param.id = this.userForm.id
          }
          const api = this.isAdd ? createUser : updateUser
          api(param).then(result => {
            if (result.code === 0) {
              this.$message({
                message: result.msg || '操作成功',
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
          }).finally(() => {
            this.confirmLoading = false
          })
        }
      })
    },

    // 新增时失焦去除前后空格
    handleIdCardBlur() {
      if (this.isAdd && typeof this.userForm.idCard === 'string') {
        this.userForm.idCard = this.userForm.idCard.trim()
      }
    },

    // 点击关闭
    closeDialog() {
      this.resetTemp()
      this.$refs.dataAuthTree && this.$refs.dataAuthTree.reset()
      this.dialogVisible = false
    },
    // 重置
    resetTemp() {
      this.userForm.idCard = ''
      this.userForm.orgIds = []
      this.userForm.orgList = []
      this.userForm.dataAuthTreecheckedKeys = []
      delete this.userForm.id
      this.$refs.userForm.resetFields()
    }
  }
}
</script>

<style lang="scss" scoped>
.userForm {
  ::v-deep .el-input.is-disabled .el-input__inner {
    background-color: transparent;
  }
  ::v-deep .el-form-item {
    margin-bottom: 18px;
  }
}
.data-auth-section {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px 16px;
  margin-bottom: 10px;
  .data-auth-title {
    font-size: 14px;
    font-weight: bold;
    color: #303133;
    margin-bottom: 10px;
    padding-bottom: 8px;
    border-bottom: 1px solid #ebeef5;
  }
  .data-auth-tree-wrapper {
    height: 400px;
    overflow: hidden;
  }
}
.dialog-footer {
  text-align: right;
}
</style>
