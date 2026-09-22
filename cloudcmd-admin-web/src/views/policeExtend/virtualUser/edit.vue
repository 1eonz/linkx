<template>
  <el-dialog :visible.sync="visible" title="虚拟用户注册" width="724">
    <el-form ref="formRef" :model="form" :rules="rules">
      <el-form-item
        label="用户名称："
        :label-width="formLabelWidth"
        prop="userName"
      >
        <el-input
          v-model="form.userName"
          placeholder="请输入用户名称"
          maxlength="100"
          style="width: 492px"
        />
      </el-form-item>

      <el-form-item
        label="通讯号码："
        :label-width="formLabelWidth"
        prop="contactNumber"
      >
        <el-input
          v-model="form.contactNumber"
          placeholder="请输入通讯号码"
          maxlength="40"
          style="width: 492px"
        />
      </el-form-item>

      <el-form-item label="应用ID：" :label-width="formLabelWidth" prop="appId">
        <el-input
          v-model="form.appId"
          placeholder="请输入应用ID"
          maxlength="100"
          style="width: 492px"
        />
      </el-form-item>

      <el-form-item
        label="应用密钥："
        :label-width="formLabelWidth"
        prop="appSecret"
      >
        <el-input
          v-model="form.appSecret"
          placeholder="请输入应用密钥"
          maxlength="200"
          style="width: 492px"
          show-password
        />
      </el-form-item>
      <el-form-item
        label="是否自动入群："
        :label-width="formLabelWidth"
        prop="defaultUser"
      >
        <el-radio-group v-model="form.defaultUser">
          <el-radio :label="0">否</el-radio>
          <el-radio :label="1">是</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="备注：" :label-width="formLabelWidth" prop="remark">
        <el-input
          v-model="form.remark"
          type="textarea"
          placeholder="请输入备注"
          maxlength="255"
          show-word-limit
          :rows="4"
          style="width: 492px"
        />
      </el-form-item>
    </el-form>

    <div slot="footer" class="dialog-footer">
      <el-button :loading="loading" @click="handleCancel(false)">
        取消
      </el-button>
      <el-button :loading="loading" type="primary" @click="handleConfirm">
        确定
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import {
  addVirtualUser,
  updateVirtualUser
} from '@/api/policeExtend/virtualUser'
import { cloneDeep } from 'lodash'

export default {
  name: 'VirtualUserEdit',
  data() {
    return {
      formLabelWidth: '120px',
      baseForm: {
        userName: '', // 用户名称
        contactNumber: '', // 通讯号码
        appId: '', // 应用ID
        appSecret: '', // 应用密钥
        remark: '', // 备注
        defaultUser: 0, // 是否自动入群
        createdBy: ''
      },
      form: {},
      operate: 'add', // 操作类型：add/edit
      loading: false,
      visible: false,
      rules: {
        userName: [
          {
            required: true,
            message: '请输入用户名称',
            trigger: 'blur'
          },
          {
            validator: (rule, value, cb) => {
              const regex = /^(?!.*[~`!@#$%^&*()\-=+{[\]}\\|;:'",.<>/?]).{1,100}$/
              console.log(value, 'userNameuserNameuserName')
              if (!regex.test(value)) {
                cb(
                  '姓名长度不能超过100个字符，禁止输入以下特殊字符：`~!@#$%^&*()-_=+{[]}\\|;:\'",.<>/?"'
                )
              } else {
                cb()
              }
            }
          }
        ],
        contactNumber: [
          {
            // required: true,
            // message: '请输入用户名称',
            trigger: 'blur',
            validator: (rule, value, cb) => {
              const regex = /^[0-9]+$/
              if (!value) {
                cb()
              } else if (!regex.test(value)) {
                cb('通讯号码只能为数字')
              } else {
                cb()
              }
            }
          }
        ],
        appId: [
          {
            required: true,
            message: '请输入应用ID',
            trigger: 'blur'
          },
          {
            validator: (rule, value, cb) => {
              const appKeyRegexp = /.{8,100}/
              if (!appKeyRegexp.test(value)) {
                cb('8~100 位字符')
              } else {
                cb()
              }
            }
          }
        ],
        appSecret: [
          {
            required: true,
            message: '请输入应用密钥',
            trigger: 'blur'
          },
          {
            validator: (rule, value, cb) => {
              const appSecretRegexp = /.{8,200}/
              if (!appSecretRegexp.test(value)) {
                cb('8~200 位字符')
              } else {
                cb()
              }
            }
          }
        ]
      }
    }
  },
  created() {
    this.form = cloneDeep(this.baseForm)
    this.form.createdBy = localStorage.getItem('back_user_id')
  },
  methods: {
    // 打开弹窗
    open(type, data) {
      this.operate = type
      this.form = data ? cloneDeep(data) : cloneDeep(this.baseForm)
      this.form.createdBy = localStorage.getItem('back_user_id')
      this.$nextTick(() => {
        this.$refs.formRef?.clearValidate()
      })
      this.visible = true
    },

    // 确定提交
    handleConfirm() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) return

        this.loading = true
        try {
          const api =
            this.operate === 'edit' ? updateVirtualUser : addVirtualUser
          const res = await api(this.form)

          if (res.code === 0) {
            this.$message.success(
              res.msg || (this.operate === 'add' ? '添加成功' : '编辑成功')
            )
            this.handleCancel(true)
          } else {
            this.$message.error(res.msg || '操作失败')
          }
        } catch (error) {
          console.error('提交失败:', error)
        } finally {
          this.loading = false
        }
      })
    },

    // 取消
    handleCancel(shouldRefresh = false) {
      this.form = cloneDeep(this.baseForm)
      this.$refs.formRef?.resetFields()
      if (shouldRefresh) {
        this.$emit('update')
      }
      this.visible = false
    }
  }
}
</script>

<style scoped lang="scss">
.dialog-footer {
  text-align: right;
}

// 让 radio-group 与 form-item__content 上下垂直居中对齐
::v-deep .el-radio-group {
  display: flex;
  align-items: center;
  height: 40px;
  line-height: 40px;
}
</style>
