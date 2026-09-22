<template>
  <div>
    <el-dialog
      :visible.sync="visible"
      :title="title"
      width="700px"
      custom-class="adaptive-dialog"
      :before-close="closeDialog"
    >
      <el-form
        ref="tempForm"
        :model="form"
        :rules="rules"
        label-position="left"
        label-width="200px"
        class="dialog-form"
      >
        <el-form-item :label="$t('index.list.userName')" prop="userName">
          <el-input
            v-model.trim="form.userName"
            :disabled="title === $t('index.operations.userToEdit')"
            class="edit-input"
          />
        </el-form-item>
        <el-form-item :label="$t('index.list.userAlias')" prop="alias">
          <el-input v-model.trim="form.alias" class="edit-input" />
        </el-form-item>
        <el-form-item
          v-if="title === $t('index.operations.newUsers')"
          :label="$t('index.pass.pass')"
          prop="password"
        >
          <el-input
            v-model.trim="form.password"
            type="password"
            class="edit-input"
          />
        </el-form-item>
        <el-form-item
          v-if="title === $t('index.operations.newUsers')"
          :label="$t('index.pass.newPassAgain')"
          prop="repeatPassword"
          :rules="rules.repeatPassword"
        >
          <el-input
            v-model.trim="form.repeatPassword"
            type="password"
            class="edit-input"
            @input="changeRepeatPassword"
          />
        </el-form-item>
        <el-form-item
          :label="$t('index.list.associatedPersons')"
          prop="personName"
        >
          <el-input
            v-model="form.personName"
            clearable
            class="edit-input"
            :placeholder="$t('index.operations.clickAssociate')"
            @focus="handleAssociatePerson"
          />
        </el-form-item>
        <el-form-item :label="$t('index.expirationDate')" prop="userPeriod">
          <el-date-picker
            v-model="form.userPeriod"
            style="width: 250px"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm:ss"
            :placeholder="$t('index.user.electUserValidityPeriod')"
          />
        </el-form-item>
        <el-form-item :label="this.$t('index.list.remarks')" prop="remark">
          <el-input v-model="form.remark" class="edit-input" />
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button @click="closeDialog"> {{ $t('index.cancel') }} </el-button>
        <el-button
          v-if="title === $t('index.operations.newUsers')"
          type="primary"
          @click="create()"
        >
          {{ $t('index.create') }}
        </el-button>
        <el-button v-else type="primary" @click="update()">
          {{ $t('index.operations.alter') }}
        </el-button>
      </div>
    </el-dialog>

    <!-- 关联人员 -->
    <relation-person ref="relation" @success="handleRelation" />
  </div>
</template>

<script>
import { createUser, updateUser, getUserById } from '@/api/permission/user'
import relationPerson from './relationPerson'
import { deepCopy } from '@/utils'

const form = {
  id: '',
  userName: '',
  alias: '',
  loginCount: '',
  password: '',
  repeatPassword: '',
  lastLoginTime: '',
  remark: '',
  status: '',
  roleId: '',
  roleName: '',
  organizationId: '',
  organizationName: '',
  isChangePassword: '',
  pwdErrorCount: '',
  pwdErrorTime: '',
  userPeriod: '',
  personId: '',
  personName: ''
}

export default {
  name: 'UserAdd',
  components: {
    relationPerson
  },
  data() {
    const pattern = new RegExp(
      "[`~!@#$^&*()=|{}':;',\\[\\].<>《》/?~！@#￥……&*（）——|{}【】‘；：”“'。，、？]"
    )
    const validateUserName = (rule, value, callback) => {
      if (value === '') {
        callback(new Error(this.$t('index.user.inputNameNoNull')))
      } else if (!/(^[0-9]*$)/.test(value)) {
        callback(new Error(this.$t('index.messageText.inputNumber')))
      } else {
        callback()
      }
    }
    const validateNewPwd = (rule, value, callback) => {
      if (value === '') {
        callback(new Error(this.$t('index.pass.enterNewPassTip')))
      } else {
        if (this.simplePassWord) {
          if (value.length < 6) {
            callback(new Error(this.$t('index.pass.passRuledLengthSimple')))
          }
        } else {
          if (!value.match(/([a-z])+/)) {
            callback(new Error(this.$t('index.pass.passRuleLowercase')))
          }
          if (!value.match(/([A-Z])+/)) {
            callback(new Error(this.$t('index.pass.passRuleMajuscule')))
          }
          if (!value.match(/([0-9])+/)) {
            callback(new Error(this.$t('index.pass.passRuleNumber')))
          }
          if (!pattern.test(value)) {
            callback(new Error(this.$t('index.pass.passRuledSpecialCharacter')))
          }
          if (value.length < 8) {
            callback(new Error(this.$t('index.pass.passRuledLength')))
          }
        }
        if (value.indexOf(' ') !== -1) {
          callback(
            new Error(this.$t('index.pass.passRuledCannotContainSpaces'))
          )
        }
        if (value.match(/([\u4E00-\u9FA5])+/)) {
          callback(
            new Error(
              this.$t('index.pass.passRuledCannotContainChineseCharacters')
            )
          )
        }
        if (value === this.form.userName) {
          callback(
            new Error(this.$t('index.user.userNameAndPasswordAreDifferent'))
          )
        }

        callback()
      }
    }
    const validateRepeatPwd = (rule, value, callback) => {
      if (value === '') {
        callback(new Error(this.$t('index.pass.enterNewPassAgainTip')))
      } else if (value !== this.form.password) {
        callback(new Error(this.$t('index.pass.passUnIdenticalTip')))
      } else {
        callback()
      }
    }
    const rules = {
      userName: [
        {
          required: true,
          validator: validateUserName,
          trigger: 'change'
        }
      ],
      password: [
        {
          required: true,
          message: this.$t('index.pass.addPassRuleTip'),
          trigger: 'change'
        },
        { validator: validateNewPwd, trigger: 'change' }
      ],
      repeatPassword: [
        {
          required: true,
          message: this.$t('index.pass.enterNewPassTip'),
          trigger: 'blur'
        },
        { validator: validateRepeatPwd, trigger: 'change' }
      ],
      alias: [
        {
          required: true,
          message: this.$t('index.user.inputUserAliasNoNull'),
          trigger: 'blur'
        }
      ]
    }
    return {
      visible: false,
      title: this.$t('index.operations.redact'),
      rules,
      form: deepCopy(form),
      simplePassWord: localStorage.getItem('simplePassWord')
    }
  },
  watch: {
    'form.personName': function(val) {
      if (val === '') {
        this.form.personId = ''
      }
    }
  },
  mounted() {
    this.newPwdTip()
  },
  methods: {
    init(row) {
      if (row) {
        getUserById(row.id).then(({ data }) => {
          this.form = data
        })
        this.title = this.$t('index.operations.userToEdit')
      } else {
        this.title = this.$t('index.operations.newUsers')
      }
      this.visible = true
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

    async create() {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          const param = deepCopy(this.form)
          createUser(param)
            .then(result => {
              if (result.code === 0) {
                this.$message({
                  message: this.$t('index.statusTitle.createSuccess'),
                  type: 'success'
                })
                this.closeDialog()
                this.$emit('success')
              } else {
                this.$message({
                  message: result.msg,
                  type: 'error'
                })
              }
            })
            .catch(() => {})
        } else {
          this.$message({
            message: this.$t('index.statusTitle.requiredFieldIsEmpty'),
            type: 'error'
          })
        }
      })
    },

    async update() {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          updateUser(this.form).then(result => {
            if (result.code === 0) {
              this.$message({
                message: this.$t('index.statusTitle.changeSuccess'),
                type: 'success'
              })
              this.closeDialog()
              this.$emit('success')
            } else if (result.code === 1) {
              this.$message({
                message: result.msg,
                type: 'error'
              })
            }
          })
        }
      })
    },

    handleAssociatePerson() {
      this.$refs.relation.init(this.form)
    },

    closeDialog() {
      this.form = deepCopy(form)
      this.$refs['tempForm'].resetFields()
      this.visible = false
    },

    changeRepeatPassword() {
      this.$forceUpdate()
    },

    handleRelation(row) {
      if (row) {
        Object.assign(this.form, {
          personId: row.id,
          personName: row.name
        })
      } else {
        this.form.personName = ''
      }
    }
  }
}
</script>

<style>
.el-form-item {
  margin-bottom: 30px !important;
}
.edit-input {
  width: 250px;
}
</style>
