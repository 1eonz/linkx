<template>
  <!-- 新增/修改三方应用 -->
  <el-dialog
    :title="
      !isAdd
        ? $t('index.thirdPartyApp.editThirdPartyApp')
        : $t('index.thirdPartyApp.addThirdPartyApp')
    "
    :visible.sync="dialogVisible"
    :close-on-click-modal="false"
    @close="closeDialog"
  >
    <el-form
      ref="thirdAppForm"
      :model="thirdAppForm"
      :rules="rules"
      label-width="170px"
      class="thirdAppForm"
      label-position="left"
    >
      <el-form-item :label="$t('index.list.thirdPartyApp')" prop="systemName">
        <el-input
          v-model="thirdAppForm.systemName"
          placeholder="请输入应用名称"
        />
      </el-form-item>
      <el-form-item :label="$t('index.list.thirdPartyAppID')" prop="clientId">
        <el-input v-model="thirdAppForm.clientId" placeholder="请输入应用ID" />
      </el-form-item>
      <el-form-item
        :label="$t('index.list.thirdPartyAppSecret')"
        prop="clientSecret"
      >
        <el-input
          v-model="thirdAppForm.clientSecret"
          type="password"
          show-password
          placeholder="请输入应用密钥"
        />
      </el-form-item>
      <el-form-item
         v-if="false"
        :label="$t('index.list.thirdPartyAppType')"
        prop="clientType"
      >
        <el-select
          v-model="thirdAppForm.clientType"
          placeholder="请选择应用类型"
          style="width: 100%;"
          :disabled="!isAdd"
        >
          <el-option
            v-if="!$store.state.user.licenseAuth.northboundDataInterface"
            label="协同统计"
            value="0"
          />
          <el-option
            v-if="!$store.state.user.licenseAuth.northboundDataInterface"
            label="三方应用"
            value="1"
          />
          <el-option
            v-if="!$store.state.user.licenseAuth.taskCollaborationAuth"
            label="三方任务"
            value="2"
          />
          <!-- <el-option label="在线统计" value="1" />
            <el-option label="支撑群组统计" value="2" />
            <el-option label="问题处理统计" value="3" />
            <el-option label="创群统计" value="4" />
            <el-option label="回复统计" value="5" />
            <el-option label="回复时长统计" value="6" />
            <el-option label="群组聊天记录" value="7" /> -->
        </el-select>
      </el-form-item>
      <el-form-item
        :label="$t('index.list.thirdPartyAppNameTokenValidation')"
        prop="tokenTime"
      >
        <el-input v-model="thirdAppForm.tokenTime" type="number">
          <template #append
            ><span>单位： 小时</span></template
          >
        </el-input>
      </el-form-item>
      <el-form-item
        :label="$t('index.list.thirdPartyAppNameRefreshTokenValidation')"
        prop="refreshTokenTime"
      >
        <el-input v-model="thirdAppForm.refreshTokenTime" type="number">
          <template #append
            ><span>单位： 天</span></template
          >
        </el-input>
      </el-form-item>
      <el-form-item :label="$t('index.list.thirdPartyAppStatus')" prop="status">
        <el-switch
          v-model="thirdAppForm.status"
          active-text="启用"
          inactive-text="停用"
          :active-value="1"
          :inactive-value="0"
        />
      </el-form-item>
      <el-form-item label="过期时间" prop="expired">
        <el-date-picker
          v-model="thirdAppForm.expired"
          type="datetime"
          placeholder="请选择过期时间"
          value-format="yyyy-MM-dd HH:mm:ss"
          style="width: 100%;"
        />
      </el-form-item>
      <el-form-item :label="$t('index.list.thirdPartyAppRemark')" prop="remark">
        <el-input
          v-model="thirdAppForm.remark"
          type="textarea"
          :rows="5"
          maxlength="255"
          show-word-limit
          placeholder="请输入"
        />
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="closeDialog">
        {{ $t('index.cancel') }}
      </el-button>
      <el-button type="primary" @click="handleConfirm">
        {{ $t('index.determine') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import {
  collaborationCreate,
  collaborationUpdate
} from '@/api/resource/thirdApp'
import { deepCopy } from '@/utils'
export default {
  name: 'EditThirdPartyApp',
  data() {
    const rules = {
      systemName: [
        { required: true, validator: this.validateName, trigger: 'blur' }
      ],
      clientId: [
        { required: true, validator: this.validateId, trigger: 'blur' }
      ],
      clientSecret: [
        { required: true, message: '请输入应用密钥', trigger: 'blur' },
        { min: 8, message: '应用密钥长度不能小于8位', trigger: 'blur' },
        { max: 16, message: '应用密钥长度不能超过16个字符', trigger: 'blur' }
      ],
      clientType: [
        { required: true, message: '请选择应用类型', trigger: 'blur' }
      ],
      tokenTime: [
        { required: true, message: '请输入token有效期', trigger: 'blur' },
        {
          pattern: /^-?\d+$/,
          message: 'token有效期必须是正整数',
          trigger: 'blur'
        }
      ],
      refreshTokenTime: [
        {
          required: true,
          message: '请输入refreshToken有效期',
          trigger: 'blur'
        },
        {
          pattern: /^-?\d+$/,
          message: 'refreshToken有效期必须是正整数',
          trigger: 'blur'
        }
      ]
    }
    return {
      treeProps: {
        children: 'children',
        label: 'name'
      },
      activeTab: 'ICC',
      dialogVisible: false,
      isAdd: true,
      thirdAppForm: {
        systemName: '',
        clientId: '',
        clientSecret: '',
        clientType: '',
        tokenTime: -1,
        refreshTokenTime: -1,
        status: 1,
        expired: '',
        remark: ''
      },
      menuTree: [],
      rules: Object.freeze(rules)
    }
  },
  created() {},
  mounted() {},
  methods: {
    // 初始化
    async init(row) {
      this.isAdd = !row
      if (row) {
        const {
          id,
          clientId,
          systemName,
          clientSecret,
          clientType,
          tokenTime,
          refreshTokenTime,
          status,
          expired,
          remark
        } = row
        this.thirdAppForm.systemName = systemName
        this.thirdAppForm.clientId = clientId
        this.thirdAppForm.clientSecret = clientSecret
        this.thirdAppForm.clientType = clientType
        this.thirdAppForm.tokenTime = tokenTime
        this.thirdAppForm.refreshTokenTime = refreshTokenTime
        this.thirdAppForm.status = status
        this.thirdAppForm.expired = expired
        this.thirdAppForm.remark = remark
        this.thirdAppForm['id'] = id
      }
      this.dialogVisible = true
    },
    validateName(rule, value, callback) {
      if (!value) {
        callback(new Error('请输入应用名称'))
      } else if (value == this.thirdAppForm.id) {
        callback(new Error('应用名称不能与应用ID相同'))
      } else {
        callback()
      }
    },

    validateId(rule, value, callback) {
      const hasLower = /[a-z]/.test(value)
      const hasUpper = /[A-Z]/.test(value)
      const hasNumber = /\d/.test(value)
      const hasSpecial = /[^a-zA-Z\d]/.test(value)
      if (!value) {
        callback(new Error('请输入应用ID'))
      } else if (hasSpecial) {
        callback(new Error('应用ID不能包含特殊字符'))
      }
      // } else if (!(hasLower && hasUpper && hasNumber)) {
      //   callback(new Error('ID必须同时包含大小写字母和数字'));
      // }
      else if (value.length < 8) {
        callback(new Error('应用ID长度不能小于8位'))
      } else if (value == this.thirdAppForm.name) {
        callback(new Error('应用ID不能与名称相同'))
      } else {
        callback()
      }
    },

    // 点击确认
    handleConfirm() {
      this.$refs['thirdAppForm'].validate(valid => {
        if (valid) {
          // this.editForm.systemName = this.thirdAppForm.systemName
          // this.editForm.clientId = this.thirdAppForm.clientId
          // this.editForm.clientSecret = this.thirdAppForm.clientSecret
          // this.editForm.clientType = this.thirdAppForm.clientType
          // this.editForm.tokenTime = this.thirdAppForm.tokenTime
          // this.editForm.refreshTokenTime = this.thirdAppForm.refreshTokenTime
          // this.editForm.status = this.thirdAppForm.status
          // this.editForm.remark = this.thirdAppForm.remark
          const param = deepCopy(this.thirdAppForm)
          const api = this.isAdd ? collaborationCreate : collaborationUpdate
          api(param).then(result => {
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
        }
      })
    },
    // 点击关闭
    closeDialog() {
      this.resetTemp()
      this.dialogVisible = false
    },
    // 重置
    resetTemp() {
      this.thirdAppForm.systemName = ''
      this.thirdAppForm.clientId = ''
      this.thirdAppForm.clientSecret = ''
      this.thirdAppForm.clientType = ''
      this.thirdAppForm.tokenTime = -1
      this.thirdAppForm.refreshTokenTime = -1
      this.thirdAppForm.status = 0
      this.thirdAppForm.expired = ''
      this.thirdAppForm.remark = ''
      delete this.thirdAppForm.id
    }
  }
}
</script>
<style lang="scss" scoped>
.tip-words {
  font-size: 16px;
  color: #303133;
}
.tip-size {
  font-size: 14px;
  color: #bfbdbc;
  margin-top: 6px;
}
.el-icon-folder {
  font-size: 50px;
  color: #409efe;
  margin: 40px auto 20px;
}
::v-deep .el-tree {
  .el-tree-node {
    .is-leaf + .el-checkbox .el-checkbox__inner {
      display: inline-block;
    }
    .el-checkbox .el-checkbox__inner {
      display: none;
    }
  }
}
</style>
