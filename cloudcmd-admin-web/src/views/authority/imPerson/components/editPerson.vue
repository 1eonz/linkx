<template>
  <div>
    <el-dialog
      :visible.sync="visible"
      :title="dialogStatus"
      :before-close="handleClose"
      :close-on-click-modal="false"
      width="800px"
    >
      <el-form
        ref="tempForm"
        :rules="rules"
        :model="form"
        label-position="left"
        label-width="300px"
        style="height: 600px; margin-left: 30px; overflow-y: scroll"
      >
        <el-form-item :label="$t('index.list.uploadFace')" class="avatar-item">
          <el-upload
            ref="upload"
            class="avatar-uploader"
            action="#"
            :show-file-list="false"
            :auto-upload="false"
            :http-request="uploadFile"
            :on-change="handleChange"
            :before-upload="beforeUpload"
            accept=".jpg,.png,.gif"
          >
            <authImg v-if="showAuthImg" class="avatar" :auth-src="imageUrl" />
            <i
              v-else-if="!imageUrl"
              class="el-icon-plus avatar-uploader-icon"
            ></i>
            <img v-else :src="imageUrl" class="avatar" />
          </el-upload>
        </el-form-item>

        <el-form-item :label="$t('index.list.compellation')" prop="name">
          <el-input
            v-model.trim="form.name"
            class="edit-input"
            :placeholder="$t('index.operations.inputContent')"
          />
        </el-form-item>
        <el-form-item
          :label="$t('index.list.organization')"
          prop="organizationName"
        >
          <select-tree
            v-model="form.organizationName"
            style="width: 300px"
            :data="orgFilterList"
            :is-init-value="true"
            :placeholder="$t('index.list.addressType')"
            @clear-val="clearOrganizationType"
            @current-change="parentCurrentChange"
          />
        </el-form-item>
        <el-form-item
          :label="
            $t('index.list.policeNumber') + '(' + $t('index.list.only') + ')'
          "
          prop="code"
        >
          <el-input
            v-model.trim="form.code"
            class="edit-input"
            :placeholder="$t('index.operations.inputContent')"
          />
        </el-form-item>
        <el-form-item :label="$t('index.list.role')" prop="roleIds">
          <el-select
            v-model="form.roleIds[0]"
            class="filter-item"
            style="width: 300px"
            :placeholder="$t('index.operations.selects')"
          >
            <el-option
              v-for="item in roleList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          v-if="isAdd"
          :label="$t('index.pass.pass')"
          prop="password"
        >
          <el-input
            v-model="form.password"
            class="edit-input"
            :type="isShow ? 'text' : 'password'"
            :placeholder="$t('index.operations.inputContent')"
          >
            <svg
              slot="suffix"
              data-v-53ff2da0=""
              data-v-ca3cd49c=""
              class="user-avatar svg-icon"
              @click="changeShowState()"
            >
              <use
                data-v-53ff2da0=""
                :xlink:href="isShow ? '#icon-eye-open' : '#icon-eye'"
              />
            </svg>
          </el-input>
        </el-form-item>
        <el-form-item
          :label="$t('index.operations.hasChildOrgPriv')"
          prop="hasChildOrgPriv"
        >
          <el-radio v-model="form.hasChildOrgPriv" :label="0">
            {{ $t('index.operations.yes') }}
          </el-radio>
          <el-radio v-model="form.hasChildOrgPriv" :label="1">
            {{ $t('index.operations.no') }}
          </el-radio>
        </el-form-item>
        <el-form-item :label="$t('index.operations.leader')" prop="leader">
          <el-select
            v-model="form.leader"
            class="filter-item"
            style="width: 300px"
            :placeholder="$t('index.operations.selectLeader')"
          >
            <el-option
              v-for="item in leaderOptions"
              :key="item.value"
              :label="item.name"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('index.list.IDNumber')" prop="idCardNum">
          <el-input
            v-model="form.idCardNum"
            class="edit-input"
            :placeholder="$t('index.operations.inputContent')"
          />
        </el-form-item>
        <el-form-item :label="$t('index.list.phone')" prop="phoneNum">
          <el-input
            v-model.trim="form.phoneNum"
            maxlength="32"
            class="edit-input"
            :placeholder="$t('index.operations.inputContent')"
          />
        </el-form-item>
        <el-form-item :label="$t('index.list.email')" prop="emailAddr">
          <el-input
            v-model="form.emailAddr"
            class="edit-input"
            :placeholder="$t('index.operations.inputContent')"
          />
        </el-form-item>
        <el-form-item
          v-if="false"
          :label="$t('index.list.accountCode')"
          prop="account"
        >
          <el-input
            v-model="form.account"
            class="edit-input"
            :placeholder="
              `${$t('index.list.selectTheAccount')}(${$t(
                'index.communication.communicationRelated'
              )} )`
            "
            @focus="handleAssociateAccount"
          />
        </el-form-item>
        <el-form-item :label="$t('index.operations.gender')" prop="sex">
          <el-select
            v-model="form.sex"
            clearable
            class="filter-item"
            style="width: 300px"
            :placeholder="$t('index.operations.selectGender')"
          >
            <el-option
              v-for="item in sexOptions"
              :key="item.value"
              :label="item.name"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('index.operations.company')" prop="company">
          <el-input
            v-model="form.company"
            class="edit-input"
            :placeholder="$t('index.operations.inputContent')"
          />
        </el-form-item>
        <el-form-item :label="$t('index.operations.rank')" prop="rank">
          <el-input
            v-model="form.rank"
            class="edit-input"
            :placeholder="$t('index.operations.inputContent')"
          />
        </el-form-item>
        <el-form-item
          :label="$t('index.operations.softTerminal')"
          prop="softTerminal"
        >
          <el-input
            v-model="form.softTerminal"
            class="edit-input"
            :placeholder="$t('index.operations.inputContent')"
          />
        </el-form-item>
        <el-form-item
          :label="$t('index.operations.landingPhone')"
          prop="landingPhone"
        >
          <el-input
            v-model="form.landingPhone"
            class="edit-input"
            :placeholder="$t('index.operations.inputContent')"
          />
        </el-form-item>
        <el-form-item :label="$t('index.list.remarks')" prop="remark">
          <el-input
            v-model="form.remark"
            class="edit-input"
            :placeholder="$t('index.operations.inputContent')"
          />
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button @click="handleClose">
          {{ $t('index.cancel') }}
        </el-button>
        <el-button
          v-if="dialogStatus === $t('index.operations.newStaff')"
          type="primary"
          :loading="btnLoading"
          @click="handleSubmit(true)"
        >
          {{ $t('index.create') }}
        </el-button>
        <el-button
          v-else
          type="primary"
          :loading="btnLoading"
          @click="handleSubmit(false)"
        >
          {{ $t('index.operations.alter') }}
        </el-button>
      </div>
    </el-dialog>

    <!-- 关联账户 -->
    <connected-account
      :visible="accountVisible"
      :person-info="form"
      type-id="1"
      @close="accountClose"
    />
  </div>
</template>

<script>
import {
  createPerson,
  updatePerson,
  getPersonById,
  uploadTmp
} from '@/api/resource/person'
import authImg from '@/components/AuthImg'
import { deepCopy, filterOrgList } from '@/utils'
import { getRoleList } from '@/api/resource/role'
import connectedAccount from './connectedAccount'
import selectTree from '@/components/SelectTree'

const form = {
  headShot: '', // 头像
  id: 0,
  code: '',
  name: '',
  organizationId: 0,
  organizationName: '',
  roleIds: [], // 角色IDs
  account: null,
  accountId: null,
  hasChildOrgPriv: 0, // 是否具有下级组织权限
  sex: '',
  company: '', // 单位
  rank: '', // 职级
  softTerminal: '', // 软终端
  landingPhone: '', // 座机
  phoneNum: null,
  emailAddr: '',
  idCardNum: '', // 身份证号
  remark: '', // 备注
  password: '',
  leader: ''
}

export default {
  name: 'EditPerson',
  components: {
    connectedAccount,
    selectTree,
    authImg
  },
  props: {
    orgList: {
      type: Array,
      default: () => []
    }
  },
  data() {
    const validateIsNum = (rule, value, callback) => {
      const phoneRegExp = new RegExp('^[()\\d +-]*$')
      if (value) {
        if (!phoneRegExp.test(value)) {
          callback(new Error(this.$t('index.messageText.inputInvalid')))
        } else {
          callback()
        }
      } else {
        callback()
      }
    }
    const pattern = new RegExp(
      "[`~!@#$^&*()=|{}':;',\\[\\].<>《》/?~！@#￥……&*（）——|{}【】‘；：”“'。，、？]"
    )
    const validateNewPwd = (rule, value, callback) => {
      if (value === '') {
        callback(new Error(this.$t('index.pass.enterNewPassTip')))
      } else {
        if (this.simplePassWord) {
          if (value.length < 6) {
            callback(new Error(this.$t('index.pass.passRuledLengthSimple')))
          }
        } else {
          if (value.match(/(\w)*(\w)\2{2}(\w)*/g)) {
            callback(new Error(this.$t('index.pass.passRuleIdentical')))
          }
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
        callback()
      }
    }
    const validateName = (rule, value, callback) => {
      if (value.length === 0) {
        callback(new Error(this.$t('index.messageText.nameCannotBeEmpty')))
      } else if (pattern.test(value)) {
        callback(
          new Error(
            this.$t('index.messageText.nameCannotContainSpecialCharacters')
          )
        )
      } else {
        callback()
      }
    }
    const validateCode = (rule, value, callback) => {
      if (value.length === 0) {
        callback(new Error(this.$t('index.messageText.numberCannotBeEmpty')))
      } else if (value.length > 20) {
        callback(new Error(this.$t('index.messageText.numberNot20Characters')))
      } else if (!/^[\da-z]+$/i.test(value)) {
        callback(new Error(this.$t('index.messageText.numAndLetter')))
      } else {
        callback()
      }
    }
    const validateIdCard = (rule, value, callback) => {
      if (value.length === 0) {
        callback(new Error('身份证号不能为空'))
      } else {
        callback()
      }
    }
    const rules = {
      name: [
        {
          required: true,
          validator: validateName,
          trigger: 'change'
        }
      ],
      idCardNum: [
        {
          required: true,
          validator: validateIdCard,
          trigger: 'change'
        }
      ],
      code: [
        {
          required: true,
          validator: validateCode,
          trigger: 'change'
        }
      ],
      organizationName: [
        {
          required: true,
          message: this.$t('index.messageText.organizationCannotBeEmpty'),
          trigger: 'change'
        }
      ],
      hasChildOrgPriv: [
        {
          required: true,
          trigger: 'change'
        }
      ],
      roleIds: [
        {
          required: true,
          message: this.$t('index.messageText.roleCannotBeEmpty'),
          trigger: 'change'
        }
      ],
      password: [
        {
          required: true,
          message: this.$t('index.pass.enterNewPassTip'),
          trigger: 'blur'
        },
        { validator: validateNewPwd, trigger: 'change' }
      ],
      phoneNum: [
        {
          required: false,
          trigger: 'change',
          validator: validateIsNum
        }
      ]
    }

    return {
      visible: false,
      accountVisible: false,
      rules,
      isShow: false,
      dialogStatus: '',
      isAdd: true,
      roleList: [],
      sexOptions: [
        {
          name: this.$t('index.list.boy'),
          value: 'GENDER_MALE'
        },
        {
          name: this.$t('index.list.girl'),
          value: 'GENDER_FEMALE'
        }
      ],
      leaderOptions: [
        {
          name: this.$t('index.operations.no'),
          value: 0
        },
        {
          name: this.$t('index.operations.yes'),
          value: 1
        }
      ],
      extendProperties: [],
      btnLoading: false,
      imageUrl: '',
      imageValid: true,
      showAuthImg: false,
      form: deepCopy(form),
      simplePassWord: localStorage.getItem('simplePassWord')
    }
  },
  computed: {
    orgFilterList: function() {
      return filterOrgList(deepCopy(this.orgList))
    }
  },
  watch: {},
  created() {
    this.getRoleList()
  },
  methods: {
    // 新增
    async add() {
      this.dialogStatus = this.$t('index.operations.newStaff')
      this.visible = true
      this.isAdd = true
      await this.getRoleList()
      this.setDefaultRole()
    },

    // 修改
    async modify({ id }) {
      this.dialogStatus = this.$t('index.operations.editorialStaff')
      this.isAdd = false
      const { data } = await getPersonById(id)

      const isActive = this.roleList.find(ev => {
        return ev.id === data.roleIds[0]
      })
      if (!isActive) {
        data.roleIds = []
      }
      Object.assign(this.form, data)
      delete this.form.password
      delete this.form.sexName
      if (data.headShot) {
        this.showAuthImg = true
        this.imageUrl = data.headShot
      }

      this.visible = true
    },

    changeShowState() {
      this.isShow = !this.isShow
    },

    // 选择组织
    parentCurrentChange(data) {
      this.form.organizationId = data.id
      this.form.organizationName = data.name
    },
    // 清空组织
    clearOrganizationType() {
      this.form.organizationId = ''
      this.form.organizationName = ''
    },

    // 获取角色
    getRoleList() {
      const param = {
        name: '',
        pageSize: 100,
        pageNum: 1
      }
      return getRoleList(param).then(({ data }) => {
        this.roleList = data.records.filter(item => item.status === 0)
      })
    },

    setDefaultRole() {
      const index = this.roleList.findIndex(item => item.name === '警务协同')
      if (index < 0) return
      this.form.roleIds = [this.roleList[index].id]
    },

    // 打开 通信号弹窗
    handleAssociateAccount() {
      this.accountVisible = true
    },

    // 关闭 通信号弹窗
    accountClose(data) {
      if (data) {
        this.form.account = data.accounts
        this.form.accountId = data.accountIds
      }
      this.accountVisible = false
    },

    // 关闭
    handleClose() {
      this.form = deepCopy(form)
      this.$refs['tempForm'].resetFields()
      this.visible = false
      this.isAdd = true
      this.isShow = false
      this.showAuthImg = false
      this.imageUrl = ''
    },

    // 点击确定
    async handleSubmit(isAdd) {
      this.$refs.upload.submit()

      setTimeout(() => {
        this.handleEdit(isAdd)
      }, 500)
    },
    async handleEdit(isAdd) {
      this.$refs['tempForm'].validate(valid => {
        if (valid && this.imageValid) {
          if (isAdd) {
            delete this.form.id
          }
          delete this.form.accountId
          const param = JSON.parse(JSON.stringify(this.form))
          const api = isAdd ? createPerson : updatePerson
          this.btnLoading = true
          api(param).then(result => {
            if (result.code === 0) {
              this.$message({
                message: result.msg,
                type: 'success'
              })
              this.$emit('success', this.form?.id)
              this.handleClose()
            } else if (result.code) {
              this.$message({
                message: result.msg,
                type: 'error'
              })
            }
            this.btnLoading = false
          })
        }
      })
    },
    // 图片上传
    async uploadFile(file) {
      const formData = new FormData()
      formData.append('file', file.file)
      await uploadTmp(formData)
        .then(result => {
          const { code, data } = result
          if (code === 0) {
            this.form.headShot = data
          } else {
            this.$message({
              message: '头像上传失败',
              type: 'error'
            })
          }
        })
        .catch(() => {
          this.$message({
            message: '头像上传失败，图片资源错误',
            type: 'error'
          })
        })
    },
    handleChange(file) {
      if (file.raw) {
        this.showAuthImg = false
      }
      this.imageUrl = URL.createObjectURL(file.raw)
    },

    beforeUpload(file) {
      const { name, size } = file
      const index = name.lastIndexOf('.')
      const fileName = name.substring(0, index)
      const type = name.substring(index + 1).toLowerCase()
      const typeData = ['jpg', 'png', 'gif']

      const isJPG = typeData.includes(type)
      const isLt5KB = size / 1024 < 500
      const isLonger = fileName.length > 64
      const hasNull = fileName.includes(' ')
      if (!isJPG) {
        this.$message.error('图片只能是jpg/png/gif格式!')
      }
      if (!isLt5KB) {
        this.$message.error('图片大小不能超过500kb!')
      }
      if (isLonger) {
        this.$message.error('图片名称不得超出64个字符!')
      }
      if (hasNull) {
        this.$message.error('图片名称不得含有空字符!')
      }
      this.imageValid = isJPG && isLt5KB && !isLonger && !hasNull
      return this.imageValid
    }
  }
}
</script>

<style scoped lang="scss">
.user-avatar {
  cursor: pointer;
  width: 15px;
  height: 15px;
  margin-top: 10px;
}

.edit-input {
  padding-right: 50px;
  width: 350px;
}
.tree-style {
  max-height: 240px;
  overflow: auto;
}
.avatar-item {
  margin-top: 20px;
  ::v-deep.el-form-item__content {
    margin-left: 0 !important;
  }
}
::v-deep.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  height: 70px;
  width: 70px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  &:hover {
    border-color: #409eff;
  }
}
::v-deep.avatar {
  height: 70px;
  width: 70px;
  display: block;
}
</style>
