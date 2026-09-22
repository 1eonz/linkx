<template>
  <el-dialog
    :title="$t('index.pass.passModify')"
    :visible.sync="visible"
    :close-on-click-modal="false"
    width="800px"
    @close="closePwdDialog"
  >
    <el-form
      ref="pwdForm"
      :rules="rules"
      :model="form"
      label-position="left"
      label-width="160px"
      style="width: 700px"
    >
      <el-form-item
        v-if="isSelf"
        :label="$t('index.pass.oldPass')"
        prop="oldPwd"
      >
        <password-input
          v-model="form.oldPwd"
          input-class="edit-input"
        />
      </el-form-item>
      <el-form-item :label="$t('index.pass.newPass')" prop="newPwd">
        <password-input
          v-model="form.newPwd"
          input-class="edit-input"
        />
      </el-form-item>
      <el-form-item
        :label="$t('index.pass.confirmNewPass')"
        prop="repeatNewPwd"
      >
        <password-input
          v-model="form.repeatNewPwd"
          input-class="edit-input"
        />
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="closePwdDialog"> {{ $t('index.cancel') }} </el-button>
      <el-button type="primary" @click="changePwd">
        {{ $t('index.operations.change') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import { updatePersonPwd } from '@/api/resource/person'
import { changePwd } from '@/api/user'
import { queryGlobalsList } from '@/api/dictionary/globals'
import {
  createSimpleValidator,
  createComplexValidator,
  createRepeatValidator
} from '@/utils/passwordValidator'
import PasswordInput from '@/components/PasswordInput'

export default {
  name: 'UserPassword',
  components: {
    PasswordInput
  },
  data() {
    return {
      visible: false,
      simplePassWord: localStorage.getItem('simplePassWord'),
      rules: {
        oldPwd: [
          {
            required: true,
            message: this.$t('index.pass.enterOldPassTip'),
            trigger: ['change', 'blur']
          }
        ],
        newPwd: [
          {
            required: true,
            message: this.$t('index.pass.enterNewPassTip'),
            trigger: ['change', 'blur']
          },
          {
            validator: (rule, value, callback) => {
              // 根据模式选择验证器
              const validator = this.simplePassWord
                ? createSimpleValidator(this)
                : createComplexValidator(this, () => this.form.username)
              validator(rule, value, callback)
              // 触发确认密码验证
              if (this.form.repeatNewPwd !== '') {
                this.$refs.pwdForm.validateField('repeatNewPwd')
              }
            },
            trigger: ['change', 'blur']
          }
        ],
        repeatNewPwd: [
          {
            required: true,
            message: this.$t('index.pass.enterNewPassAgainTip'),
            trigger: ['change', 'blur']
          },
          {
            validator: createRepeatValidator(this, () => this.form.newPwd),
            trigger: ['change', 'blur']
          }
        ]
      },
      isSelf: false,
      form: {
        id: '',
        username: '',
        oldPwd: '',
        newPwd: '',
        repeatNewPwd: ''
      }
    }
  },
  mounted() {
    this.newPwdTip()
  },
  methods: {
    async getGlobal() {
      await queryGlobalsList().then(res => {
        if (res?.data) {
          if (res?.data.ALLOW_SIMPLE_PASSWORD === '1') {
            localStorage.setItem('simplePassWord', true)
            this.simplePassWord = true
          } else {
            localStorage.removeItem('simplePassWord')
            this.simplePassWord = null
          }
        }
      })
    },
    newPwdTip() {
      this.rules.newPwd[0] = {
        required: true,
        message: this.simplePassWord
          ? this.$t('index.pass.passRuleTipSimple')
          : this.$t('index.pass.passRuleTip'),
        trigger: 'blur'
      }
    },
    async setData({ id, username, name }, isSelf) {
      await this.getGlobal()
      this.form.id = id
      this.form.username = username || name
      this.isSelf = isSelf
      if (isSelf) {
        this.newPwdTip()
      } else {
        this.rules.newPwd[0].message = this.simplePassWord
          ? this.$t('index.pass.passRuleTipSimple')
          : this.$t('index.pass.addPassRuleTip')
      }
      this.visible = true
    },
    closePwdDialog() {
      this.visible = false
      this.$refs['pwdForm'].resetFields()
    },
    changePwd() {
      this.$refs['pwdForm'].validate(valid => {
        if (valid) {
          let param = {}
          const { id, username, oldPwd, newPwd, repeatNewPwd } = this.form
          if (this.isSelf) {
            param = {
              username: username,
              oldPassword: oldPwd,
              newPassword: newPwd,
              repeatNewPassword: repeatNewPwd
            }
          } else {
            param = {
              id: id,
              password: newPwd,
              repeatNewPwd: repeatNewPwd
            }
          }
          const api = this.isSelf ? changePwd : updatePersonPwd
          api(param)
            .then(result => {
              if (result.code === 0) {
                this.closePwdDialog()
                this.$message({
                  message:
                    this.$t('index.operations.change') +
                    this.$t('index.statusTitle.succeed'),
                  type: 'success'
                })
                this.$emit('success')
              } else {
                this.$message({
                  message: result.msg,
                  type: 'error'
                })
              }
            })
            .catch(() => {
              this.$message({
                type: 'error',
                message:
                  this.$t('index.operations.change') +
                  this.$t('index.statusTitle.fail')
              })
            })
        }
      })
    }
  }
}
</script>

<style scoped>
.edit-input {
  width: 500px;
}
::v-deep .el-form-item {
  margin-bottom: 30px;
}
</style>
