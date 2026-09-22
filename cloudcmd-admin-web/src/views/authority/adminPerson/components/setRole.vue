<template>
  <!-- 新增/设置角色 -->
  <el-dialog
    v-if="dialogVisible"
    :visible.sync="dialogVisible"
    :close-on-click-modal="false"
    width="500px"
    custom-class="set-role-dialog"
    :title="
      isAdd ? $t('index.operations.Added') : $t('index.operations.setRole')
    "
    @close="closeDialog"
  >
    <el-form
      ref="tempForm"
      :model="form"
      label-position="left"
      :rules="rules"
      label-width="80px"
    >
      <!-- 组织名称 -->
      <el-form-item
        :label="$t('index.list.organizationName')"
        prop="departmentName"
      >
        <SelectTree
          v-if="DEPARTMENT_SYNC_SIGN"
          v-model="form.departmentName"
          :is-init-value="true"
          :is-disabled="!isAdd"
          :value="form.departmentCode"
          :placeholder="$t('index.operations.selects')"
          @clear-val="clearOrganizationType"
          @current-change="parentCurrentChange"
        />
        <select-tree-lazy
          v-else
          v-model="form.departmentName"
          style="width: 100%"
          :department-code="form.departmentCode"
          :is-disabled="!isAdd"
          :is-init-value="true"
          :placeholder="$t('index.operations.selects')"
          @clear-val="clearOrganizationType"
          @current-change="parentCurrentChange"
        />
      </el-form-item>

      <!-- 姓名 -->
      <el-form-item
        :label="$t('index.list.compellation')"
        prop="name"
      >
        <el-input
          v-model.trim="form.name"
          :placeholder="$t('index.operations.inputContent')"
          maxlength="50"
          :disabled="!isAdd"
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
import { getUserRoleByUserId, bindRole, setRole } from '@/api/resource/personAdmin'
import selectTreeLazy from '@/components/SelectTreeLazy'
import SelectTree from '@/components/SelectTree'
import { APPLICATION_ID } from '@/appConfig'

export default {
  name: 'SetRole',
  components: {
    selectTreeLazy,
    SelectTree
  },
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
      departmentName: [
        { required: true, message: this.$t('index.messageText.TheOrganizationNameCannotBeEmpty'), trigger: 'change' }
      ],
      name: [
        { required: true, message: this.$t('index.messageText.nameCannotBeEmpty'), trigger: 'blur' }
      ]
    }
    return {
      dialogVisible: false,
      rules,
      roleList: [],
      form: {
        name: '',
        roleIds: [],
        executorId: 0,
        departmentName: '',
        departmentCode: ''
      },
      isAdd: true,
      DEPARTMENT_SYNC_SIGN: false
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
  mounted() {
    this.getGlobalConfig()
  },
  methods: {
    // 获取全局参数
    async getGlobalConfig() {
      const globalConfig = JSON.parse(localStorage.getItem('globalConfig'))
      if (globalConfig) {
        this.DEPARTMENT_SYNC_SIGN =
          globalConfig.DEPARTMENT_SYNC_SIGN === 'true' ||
          globalConfig.DEPARTMENT_SYNC_SIGN === true
      }
    },

    async init(row) {
      this.isAdd = !row
      this.form.name = ''
      this.form.departmentName = ''
      this.form.departmentCode = ''
      this.form.roleIds = []

      if (row) {
        // 编辑模式：填充已有数据
        this.form.executorId = row.id
        this.form.name = row.name
        this.form.departmentCode = row.departmentCode
        this.form.departmentName = row.departmentName
      }

      await this.getList(row?.id)
      this.dialogVisible = true
    },

    // 获取角色列表和用户已有角色
    async getList(id = '') {
      const params = {
        applicationId: APPLICATION_ID.ADMIN,
        name: '',
        pageSize: 100,
        pageNo: 1
      }
      try {
        const roleList = await getRoleList(params)
        this.roleList = roleList.data?.records?.filter(item => item.status === 0)

        if (!this.isAdd && id) {
          // 编辑模式：获取用户已绑定的角色
          const userRole = await getUserRoleByUserId(id)
          // 兼容返回单个角色或角色数组
          const roleData = userRole.data
          if (Array.isArray(roleData)) {
            // 返回角色数组，过滤出仍在可用角色列表中的
            const activeRoleIds = roleData
              .filter(r => this.roleList?.some(ev => ev.id === r.id))
              .map(r => r.id)
            this.form.roleIds = activeRoleIds
          } else if (roleData) {
            // 返回单个角色对象
            const isActive = this.roleList?.some(ev => ev.id === roleData.id)
            if (isActive) {
              this.form.roleIds = [roleData.id]
            }
          }
        } else {
          this.form.roleIds = []
        }
      } catch (error) {
        console.error('获取角色数据失败:', error)
        this.$message.error(this.$t('index.messageText.operationFailed'))
      }
    },

    handleConfirm() {
      this.$refs['tempForm'].validate(valid => {
        if (!valid) return

        const api = this.isAdd ? bindRole : setRole
        let params = {}

        if (this.isAdd) {
          // 新增：传 name + departmentCode + roleIds
          params = {
            name: this.form.name,
            departmentCode: this.form.departmentCode,
            roleIds: this.form.roleIds
          }
        } else {
          // 编辑：传 userId + roleIds
          params = {
            userId: this.form.executorId,
            roleIds: this.form.roleIds
          }
        }

        api(params)
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
      this.form.name = ''
      this.form.roleIds = []
      this.form.executorId = 0
      this.form.departmentName = ''
      this.form.departmentCode = ''
      this.$refs['tempForm'].resetFields()
      this.dialogVisible = false
    },

    // 选择组织
    parentCurrentChange(data) {
      this.form.departmentCode = data.code
      this.form.departmentName = data.name
    },

    // 清空组织
    clearOrganizationType() {
      this.form.departmentCode = ''
      this.form.departmentName = ''
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

.form-text {
  font-size: 14px;
  color: #303133;
}
</style>

<style lang="scss">
.set-role-dialog {
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
