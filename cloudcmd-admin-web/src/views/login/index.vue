<template>
  <div class="login-container">
    <div class="tool-btn">
      <el-dropdown @command="handleCommand">
        <el-button type="primary" size="medium">
          {{ language }}<i class="el-icon-arrow-down el-icon--right"></i>
        </el-button>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item command="cn">
            {{ $t('index.Chinese') }}
          </el-dropdown-item>
          <el-dropdown-item command="en">
            {{ $t('index.English') }}
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>

    <el-form
      ref="loginForm"
      :model="loginForm"
      :rules="loginRules"
      class="login-form"
      auto-complete="on"
      label-position="left"
    >
      <div class="title-container">
        <h3 class="title">{{  $t('index.list.mobileCommandName', { title: systemTitle }) }}</h3>
        <img src="@/assets/login_images/logo.png" />
      </div>
      <el-form-item prop="username" class="el-form-item-class">
        <span class="svg-container">
          <svg-icon icon-class="user" />
        </span>
        <el-input
          ref="username"
          v-model="loginForm.username"
          :placeholder="$t('index.user.inputName')"
          name="username"
          type="text"
          tabindex="1"
          auto-complete="on"
          class="el-input-class"
        />
      </el-form-item>

      <el-form-item prop="password" class="el-form-item-class">
        <span class="svg-container">
          <svg-icon icon-class="password" />
        </span>
        <el-input
          :key="passwordType"
          ref="password"
          v-model="loginForm.password"
          :type="passwordType"
          :placeholder="$t('index.pass.inputPassword')"
          name="password"
          tabindex="2"
          auto-complete="on"
          class="el-input-class"
          @keyup.enter.native="handleLogin"
          @copy.native.prevent
          @paste.native.prevent
          @cut.native.prevent
        />
      </el-form-item>
      <el-button
        :loading="loading"
        type="primary"
        style="width: 100%; margin-bottom: 10px"
        @click.native.prevent="handleLogin"
      >
        {{ $t('index.login.login') }}
      </el-button>
    </el-form>

    <el-dialog
      width="550px"
      :visible.sync="passwordFormVisible"
      :title="passwordTitle"
      :close-on-click-modal="false"
      style="caret-color: black"
      custom-class="adaptive-dialog"
      :before-close="handleClose"
    >
      <el-form
        ref="pwdForm"
        :rules="pwdRules"
        :model="pwdTemp"
        label-position="left"
        label-width="200px"
        class="dialog-form"
      >
        <el-form-item :label="$t('index.pass.oldPass')" prop="oldPassword">
          <password-input
            v-model="pwdTemp.oldPassword"
            :input-style="{ '-webkit-text-fill-color': '#000009' }"
          />
        </el-form-item>
        <el-form-item :label="$t('index.pass.newPass')" prop="newPassword">
          <password-input
            v-model="pwdTemp.newPassword"
            :input-style="{ '-webkit-text-fill-color': '#000009' }"
          />
        </el-form-item>
        <el-form-item
          :label="$t('index.pass.confirmNewPass')"
          prop="repeatNewPassword"
        >
          <password-input
            v-model="pwdTemp.repeatNewPassword"
            :input-style="{ '-webkit-text-fill-color': '#000009' }"
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="handleClose">
          {{ $t('index.cancel') }}
        </el-button>
        <el-button type="primary" @click="changePwd">
          {{ $t('index.operations.change') }}
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { changePwd, keepalive, getDeviceId, setDeviceId } from '@/api/user'
import { deepCopy } from '@/utils'
import { setToken } from '@/utils/auth'
import { queryGlobalsList } from '@/api/dictionary/globals'
import { getLicenseInfoUtil } from '@/utils/licenseUtils'
import { getGlobalsList } from '@/api/dictionary/globals'
import {
  createSimpleValidator,
  createComplexValidator,
  createRepeatValidator
} from '@/utils/passwordValidator'
import PasswordInput from '@/components/PasswordInput'

export default {
  name: 'Login',
  components: {
    PasswordInput
  },
  data() {
    const validateUserName = (rule, value, callback) => {
      if (value.length === 0) {
        callback(new Error(this.$t('index.user.inputNameNoNull')))
      } else {
        callback()
      }
    }
    const validatePassword = (rule, value, callback) => {
      if (value.length === 0) {
        callback(new Error(this.$t('index.pass.inputPasswordNoNull')))
      } else {
        callback()
      }
    }

    const loginRules = {
      username: [
        { required: true, trigger: 'blur', validator: validateUserName }
      ],
      password: [
        { required: true, trigger: 'blur', validator: validatePassword }
      ]
    }

    const pwdRules = {
      oldPassword: [
        {
          required: true,
          message: this.$t('index.pass.enterOldPassTip'),
          trigger: 'blur'
        }
      ],
      newPassword: [
        {
          required: true,
          message: this.$t('index.pass.enterNewPassTip'),
          trigger: 'blur'
        },
        {
          validator: (rule, value, callback) => {
            // 根据模式选择验证器
            const validator = this.simplePassWord
              ? createSimpleValidator(this)
              : createComplexValidator(this, () => this.pwdTemp.username)
            validator(rule, value, callback)
            // 触发确认密码验证
            if (this.pwdTemp.repeatNewPassword !== '') {
              this.$refs.pwdForm.validateField('repeatNewPassword')
            }
          },
          trigger: 'change'
        }
      ],
      repeatNewPassword: [
        {
          required: true,
          message: this.$t('index.pass.enterNewPassAgainTip'),
          trigger: 'blur'
        },
        {
          validator: createRepeatValidator(this, () => this.pwdTemp.newPassword),
          trigger: 'change'
        }
      ]
    }

    return {
      passwordFormVisible: false,
      passwordTitle: this.$t('index.firstLogin'),
      loginForm: {
        clientId: 'CDC-2000',
        clientSecret: 'CDC-2000',
        grantType: 'password',
        redirectUri: '',
        username: '',
        password: '',
        scope: 'all',
        state: '',
        deviceId: getDeviceId()
      },
      loginRules: Object.freeze(loginRules),
      pwdRules: Object.freeze(pwdRules),
      pwdTemp: {
        username: '',
        oldPassword: '',
        newPassword: '',
        repeatNewPassword: ''
      },
      loading: false,
      passwordType: 'password',
      redirect: '',
      loginPasswordFormVisible: false,
      language: '',
      simplePassWord: false
    }
  },
  computed: {
    systemTitle() {
      return this.$store.state.settings.systemName
    }
  },
  watch: {
    $route: {
      handler: function(route) {
        this.redirect = route.query && route.query.redirect
      },
      immediate: true
    }
  },
  mounted() {
    this.getSimplePassWord()

    const lang = localStorage.getItem('localLanguage')
    if (lang === 'cn') {
      this.language = '中文'
    } else {
      this.language = 'English'
    }
  },
  methods: {
    changePwd() {
      this.$refs['pwdForm'].validate(valid => {
        if (valid) {
          const param = deepCopy(this.pwdTemp)
          changePwd(param).then(result => {
            if (result.code === 0) {
              this.$message({
                message: this.$t('index.statusTitle.changeSuccess'),
                type: 'success'
              })
              this.handleClose()
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
    keepAlive() {
      keepalive()
        .then(result => {
          if (result.code !== 0) {
            clearInterval(global.tokenTimer)
            this.$store.dispatch('user/logout').then(() => {
              this.$router.push(`/login`)
            })
            this.$message({
              message: this.$t('index.loginTimeout'),
              type: 'warning'
            })
          }
        })
        .catch(() => {
          this.$message({
            message: this.$t('index.serverException'),
            type: 'error'
          })
        })
    },
    getDaysFromToday(dateStr) {
      if (!dateStr || typeof dateStr !== 'string') {
        console.log('请输入有效的日期字符串')
        return 0
      }
      // 解析日期字符串
      const inputDate = new Date(dateStr.replace(/-/g, '/')) // 兼容 Safari
      const today = new Date()

      // 计算时间差（毫秒）
      const timeDiff = inputDate.getTime() - today.getTime()

      // 转换为天数
      const daysDiff = Math.ceil(timeDiff / (1000 * 60 * 60 * 24))

      return daysDiff
    },

    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          if (this.loginForm.deviceId === null) {
            const currentTime = new Date().getTime()
            setDeviceId(currentTime)
            this.loginForm.deviceId = currentTime
          }
          this.loading = true
          this.$store
            .dispatch('user/login', this.loginForm)
            .then(async res => {
              // license 提示
              const licenseWarning = res.headers['x-cloudcmd-license-warning']
              if (licenseWarning) {
                const msg = Buffer.from(licenseWarning, 'base64')
                  .toString()
                  .toString()

                this.$alert(msg)
              }

              if (res.code === 121) {
                await this.$alert(
                  this.$t('index.pass.userPassword') +
                    res.msg +
                    this.$t('index.pass.howDayPastDueChange'),
                  {
                    confirmButtonText: this.$t('index.determine'),
                    type: 'info'
                  }
                ).catch(() => {})
              }

              setTimeout(async () => {
                // 获取licenseAuth并本地存储
                const licenseAuth = await getLicenseInfoUtil()
                const licenseState = +licenseAuth?.licenseState
                // 0 未激活，1 激活，2 即将过期，3 已过期，4 失效可试用，5 失效
                let licenseStateText = ''
                if (licenseState === 0) {
                  licenseStateText = 'license未激活'
                } else if (licenseState === 2) {
                  // licenseStateText =
                  //   '您的账户即将到期，请及时联系相关人员进行续费，以免影响您的业务'
                  const licenseExpirationTime = this.getDaysFromToday(
                    licenseAuth?.expireDate
                  )
                  const { data } = await getGlobalsList()
                  const systemNameItem = data?.find(item => item?.name === 'SYSTEM_NAME')
                  if (systemNameItem?.value) {
                    this.$store.dispatch('settings/setSystemName', systemNameItem.value)
                  }
                  const LICENSE_EXPIRED_TIME = data?.find(item => item?.name === 'MSIP_LICENSE_EXPIRED_TIME')?.value
                  if (licenseExpirationTime < Number(LICENSE_EXPIRED_TIME)) {
                    licenseStateText =
                      '您的账户还有' +
                      licenseExpirationTime +
                      '天到期，请及时联系相关人员进行续费，以免影响您的业务'
                  }
                } else if (licenseState === 3 || licenseState === 5) {
                  licenseStateText = 'license已过期，请重新导入'
                }
                if (licenseStateText !== '') {
                  this.$alert(licenseStateText)
                    .then(() => {
                      this.$router.push({ path: this.redirect || '/' })
                      window.location.reload(true)
                    })
                    .catch(() => {
                      this.$router.push({ path: this.redirect || '/' })
                      window.location.reload(true)
                    })
                } else {
                  this.$router.push({ path: this.redirect || '/' })
                  window.location.reload(true)
                }
              }, 500)
              this.loading = false
              // 设定定时任务，保持http心跳
              global.tokenTimer = setInterval(this.keepAlive, 5000)
            })
            .catch(e => {
              // 115密码过期、114首次登录、137重置密码后
              if (e.code === 114 || e.code === 115 || e.code === 137) {
                setToken(e.data.accessToken)
                this.passwordTitle =
                  e.msg || this.$t('index.pass.passwordAging')
                this.pwdTemp.username = e.data.userName
                this.passwordFormVisible = true
              } else if (e.code) {
                this.$message({
                  message: e.msg,
                  type: 'warning'
                })
              }
              this.loading = false
            })
        } else {
          const { username, password } = this.loginForm
          const emptyTips = this.$t(
            'index.pass.userNameOrPassUnStandardFillAgain'
          )
          const errTips = this.$t('index.pass.userNameOrPassUnStandard')
          const message =
            username === '' || password === '' ? emptyTips : errTips
          this.$message({
            message,
            type: 'error'
          })
          this.loading = false
        }
      })
    },
    handleClose() {
      this.$refs['pwdForm'].resetFields()
      this.passwordFormVisible = false
    },
    handleCommand(lang) {
      if (lang === 'cn') {
        this.language = '中文'
      } else if (lang === 'en') {
        this.language = 'English'
      }
      localStorage.setItem('localLanguage', lang)
      this.$i18n.locale = lang
      window.location.reload()
    },
    async getSimplePassWord() {
      localStorage.removeItem('simplePassWord')
      this.simplePassWord = false
      await queryGlobalsList().then(res => {
        // 系统名称
        console.log('res?.data?.title', res?.data?.title)
        this.$store.dispatch('settings/setSystemName', res?.data?.title || '警务协同')
        if (res?.data.ALLOW_SIMPLE_PASSWORD === '1') {
          localStorage.setItem('simplePassWord', true)
          localStorage.setItem('globalConfig', JSON.stringify(res.data))
          this.simplePassWord = true
          this.pwdRules.newPassword[0] = {
            required: true,
            message: this.$t('index.pass.passRuleTipSimple'),
            trigger: 'blur'
          }
        } else {
          this.pwdRules.newPassword[0] = {
            required: true,
            message: this.$t('index.pass.passRuleTip'),
            trigger: 'blur'
          }
        }
      })
    }
  }
}
</script>

<style lang="scss">
$bg: #283443;
$light_gray: #fff;
$cursor: #fff;

@supports (-webkit-mask: none) and (not (cater-color: $cursor)) {
  .login-container .el-input input {
    color: $cursor;
  }
}

/* reset element-ui css */
.login-container {
  .el-input-class {
    display: inline-block;
    height: 47px;
    width: 85%;

    input {
      background: transparent;
      border: 0;
      -webkit-appearance: none;
      border-radius: 0;
      padding: 12px 5px 12px 15px;
      color: $light_gray;
      height: 47px;
      caret-color: $cursor;
      &:-webkit-autofill {
        box-shadow: 0 0 0 1000px $bg inset !important;
        -webkit-text-fill-color: $cursor !important;
      }
    }
  }

  .el-form-item {
    margin-bottom: 32px;
  }

  .el-form-item-class {
    border: 1px solid rgba(255, 255, 255, 0.1);
    background: rgba(0, 0, 0, 0.1);
    border-radius: 5px;
    color: #454545;
  }
}
</style>

<style lang="scss">
$bg: #2d3a4b;
$dark_gray: #889aa4;
$light_gray: #eee;

.tool-btn {
  position: absolute;
  top: 15px;
  right: 15px;
}

.login-container {
  min-height: 100%;
  width: 100%;
  background-color: $bg;
  overflow: hidden;

  .login-form {
    position: relative;
    width: 520px;
    max-width: 100%;
    padding: 160px 35px 0;
    margin: 0 auto;
    overflow: hidden;
  }

  .tips {
    font-size: 14px;
    color: #fff;
    margin-bottom: 10px;

    span {
      &:first-of-type {
        margin-right: 16px;
      }
    }
  }

  .svg-container {
    padding: 6px 5px 6px 15px;
    color: $dark_gray;
    vertical-align: middle;
    width: 30px;
    display: inline-block;
  }

  .title-container {
    position: relative;
    text-align: center;

    .title {
      font-size: 26px;
      color: $light_gray;
      margin: 0px auto 40px auto;
      text-align: center;
      font-weight: bold;
    }
  }

  .show-pwd {
    position: absolute;
    right: 10px;
    top: 7px;
    font-size: 16px;
    color: $dark_gray;
    cursor: pointer;
    user-select: none;
  }
}
</style>
