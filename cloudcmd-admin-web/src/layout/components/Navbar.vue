<template>
  <div class="navbar">
    <hamburger
      :is-active="sidebar.opened"
      class="hamburger-container"
      @toggleClick="toggleSideBar"
    />

    <breadcrumb class="breadcrumb-container" />

    <div class="right-menu">
      <el-dropdown class="avatar-container" trigger="click">
        <div class="avatar-wrapper">
          <svg
            data-v-53ff2da0=""
            data-v-ca3cd49c=""
            aria-hidden="true"
            class="user-avatar svg-icon"
          >
            <use data-v-53ff2da0="" xlink:href="#icon-user" />
          </svg>
          <div v-show="role" class="user-role">{{ role }}</div>
        </div>
        <el-dropdown-menu
          slot="dropdown"
          class="user-dropdown"
          style="width: 175px"
        >
          <el-row class="tac" style="overflow: hidden">
            <el-col :span="24">
              <el-menu
                default-active="2"
                class="el-menu-vertical-demo"
                style="border: none; width: 100%"
              >
                <router-link to="/">
                  <el-menu-item index="2">
                    <span slot="title" style="color: #000">
                      <i class="el-icon-house"></i>
                      {{ $t('index.homePage') }}
                    </span>
                  </el-menu-item>
                </router-link>
                <el-submenu v-if="false" index="1-4">
                  <template slot="title">
                    <i class="el-icon-chat-line-square"></i>
                    {{ $t('index.language') }}
                  </template>
                  <el-menu-item
                    index="1-4-1"
                    :class="[languageType === 'cn' ? 'language-class' : '']"
                    @click="language('cn')"
                  >
                    {{ $t('index.Chinese') }}
                    <i v-if="languageType === 'cn'" class="el-icon-check"></i>
                  </el-menu-item>
                  <el-menu-item
                    index="1-4-1"
                    :class="[languageType === 'en' ? 'language-class' : '']"
                    @click="language('en')"
                  >
                    {{ $t('index.English') }}
                    <i v-if="languageType === 'en'" class="el-icon-check"> </i>
                  </el-menu-item>
                </el-submenu>
                <el-menu-item index="2" @click.native="handleChangePwd">
                  <span slot="title" style="color: #000">
                    <i class="el-icon-view"></i>
                    {{ $t('index.pass.passModify') }}
                  </span>
                </el-menu-item>
                <el-menu-item index="bind-user" @click.native="handleBindUser">
                  <span slot="title" :style="{ color: isBindUser ? 'rgba(13, 81, 255, 1)' : '#000' }">
                    <img v-if="!isBindUser" :src="adminUserIcon" class="bind-user-icon" />
                    <img v-else :src="adminUserSuccessIcon" class="bind-user-icon" />
                    {{ isBindUser ? '已绑定警员' : '绑定警员' }}
                  </span>
                </el-menu-item>
                <el-menu-item index="3" @click.native="logout">
                  <span slot="title" style="color: #000">
                    <i class="el-icon-switch-button"></i>
                    {{ $t('index.Logout') }}
                  </span>
                </el-menu-item>
              </el-menu>
            </el-col>
          </el-row>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
    <!-- <div v-if="serviceVersion" class="version-info">警信版本：{{ serviceVersion }}</div> -->
    <div class="right-tip">{{ $t('index.tip') }}</div>

    <!-- 密码 -->
    <user-password ref="password" />
    
    <!-- 绑定用户弹窗 -->
    <user-bind-dialog
      :visible.sync="bindUserDialogVisible"
      :bind-user="bindUserInfo"
      @bindRefresh="loadBindUser"
      @unbind="handleUnbindSuccess"
    />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getRolePermissions } from '@/api/user'
import Breadcrumb from '@/components/Breadcrumb'
import Hamburger from '@/components/Hamburger'
import { getUserName } from '@/utils/auth'
import userPassword from '@/views/permission/components/userPassword'
import UserBindDialog from '@/components/UserBindDialog'
import { getBindUser } from '@/api/authority/customDepartment'
import adminUserIcon from '@/assets/images/adminuser.svg'
import adminUserSuccessIcon from '@/assets/images/adminusersuccess.svg'


export default {
  components: {
    Breadcrumb,
    Hamburger,
    userPassword,
    UserBindDialog
  },
  data() {
    return {
      role: '',
      userId: '',
      languageType: localStorage.getItem('localLanguage') || 'cn',
      // serviceVersion: localStorage.getItem('serviceVersion') || '',
      bindUserDialogVisible: false,
      bindUserInfo: null, // 绑定的用户信息
      adminUserIcon, // 绑定警员图标
      adminUserSuccessIcon, // 已绑定警员图标
      // serviceVersion: localStorage.getItem('serviceVersion') || ''
    }
  },
  computed: {
    ...mapGetters(['sidebar', 'avatar']),
    // 是否已绑定警员
    isBindUser() {
      return this.bindUserInfo && this.bindUserInfo.id && this.bindUserInfo.imUserName
    }
  },
  created() {
    // this.getUserRole()
    this.role = getUserName()
    // 查询绑定关系
    this.loadBindUser()
  },
  methods: {
    toggleSideBar() {
      this.$store.dispatch('app/toggleSideBar')
    },
    async logout() {
      clearInterval(global.tokenTimer)
      await this.$store.dispatch('user/logout')
      this.$router.push(`/login?redirect=${this.$route.fullPath}`)
    },
    getUserRole() {
      getRolePermissions().then(result => {
        this.role = result.data.role
      })
    },
    language(val) {
      this.languageType = val
      localStorage.setItem('language', val)
      this.$i18n.locale = val
      window.location.reload()
    },
    // 修改密码
    handleChangePwd() {
      const row = {
        username: this.role
      }
      this.$refs.password.setData(row, true)
    },
    // 查询绑定关系
    async loadBindUser() {
      try {
        const { code, data } = await getBindUser()
        if (code === 0 && data) {
          this.bindUserInfo = data
        } else {
          this.bindUserInfo = null
        }
      } catch (error) {
        console.error('查询绑定关系失败:', error)
        this.bindUserInfo = null
      }
    },
    // 绑定用户
    async handleBindUser() {
      // 每次打开弹窗前刷新绑定关系
      this.loadBindUser()
      this.bindUserDialogVisible = true
    },
    // 绑定成功
    handleBindSuccess(user) {
      this.bindUserInfo = user
    },
    // 解绑成功
    handleUnbindSuccess() {
      this.bindUserInfo = null
    }
  }
}
</script>

<style lang="scss" scoped>
.navbar {
  height: 50px;
  overflow: hidden;
  position: relative;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);

  .hamburger-container {
    line-height: 46px;
    height: 100%;
    float: left;
    cursor: pointer;
    transition: background 0.3s;
    -webkit-tap-highlight-color: transparent;

    &:hover {
      background: rgba(0, 0, 0, 0.025);
    }
  }

  .breadcrumb-container {
    float: left;
  }
  .right-tip {
    height: 100%;
    line-height: 50px;
    font-size: 12px;
    color: #5a5e66;
    float: right;
    margin-right: 10px;
  }
  .version-info {
    height: 100%;
    line-height: 50px;
    font-size: 12px;
    color: rgba(51, 51, 51, 1);
    float: right;
    margin: 0 10px;
  }
  .right-menu {
    float: right;
    height: 100%;
    line-height: 50px;
    &:focus {
      outline: none;
    }

    .right-menu-item {
      display: inline-block;
      padding: 0 8px;
      height: 100%;
      font-size: 18px;
      color: #5a5e66;
      vertical-align: text-bottom;

      &.hover-effect {
        cursor: pointer;
        transition: background 0.3s;

        &:hover {
          background: rgba(0, 0, 0, 0.025);
        }
      }
    }

    .avatar-container {
      margin-right: 30px;

      .avatar-wrapper {
        display: flex;
        justify-content: space-between;
        align-items: center;
        .user-role {
          cursor: pointer;
          font-size: 14px;
        }
        .user-avatar {
          cursor: pointer;
          width: 25px;
          height: 25px;
          border-radius: 10px;
          margin-right: 10px;
        }

        .el-icon-caret-bottom {
          cursor: pointer;
        }
      }
    }
  }
}
.el-menu-item.is-active {
  color: #000;
}
.el-submenu .el-menu-item {
  padding-left: 48px !important;
}
.language-class {
  background: #409eff !important;
  color: #fff !important;
}
.el-icon-check {
  color: #fff;
}
.bind-user-icon {
  width: 18px;
  height: 18px;
  vertical-align: middle;
  margin-right: 11px;
}
</style>
