<template>
  <div :class="{ 'has-logo': showLogo }">
    <logo v-if="showLogo" :collapse="isCollapse" />
    <el-scrollbar wrap-class="scrollbar-wrapper">
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :background-color="variables.menuBg"
        :text-color="variables.menuText"
        :unique-opened="false"
        :active-text-color="variables.menuActiveText"
        :collapse-transition="false"
        mode="vertical"
      >
        <sidebar-item
          v-for="route in routes"
          :key="route.path"
          :item="route"
          :base-path="route.path"
        />
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script>
import { mapState, mapGetters, mapActions } from 'vuex'
import Logo from './Logo'
import SidebarItem from './SidebarItem'
import variables from '@/styles/variables.scss'
import {
  importGetProgress,
  synchronizationGetProcess
} from '@/api/dataImport/dataImport'

export default {
  components: { SidebarItem, Logo },
  data() {
    return {
      firstRoutes: [],
      freeRoutes: [],
      lastRoute: [],
      flatMenuList: []
    }
  },
  computed: {
    ...mapGetters(['sidebar']),
    ...mapState('user', ['menu', 'permissions', 'permissionsMenu']),
    routes() {
      return [...this.firstRoutes, ...this.permissionsMenu, ...this.lastRoute]
    },
    activeMenu() {
      const route = this.$route
      const { meta, path } = route
      // if set path, the sidebar will highlight the path you set
      if (meta.activeMenu) {
        return meta.activeMenu
      }
      return path
    },
    showLogo() {
      return this.$store.state.settings.sidebarLogo
    },
    variables() {
      return variables
    },
    isCollapse() {
      return !this.sidebar.opened
    }
  },
  created() {
    this.getFixedRoutes()
    this.getMenuListData()
  },
  methods: {
    ...mapActions('user', ['setType', 'setPriv']),
    getFixedRoutes() {
      this.firstRoutes = this.$router.options.routes.slice(0, 3)
      this.lastRoute = this.$router.options.routes.slice(-1)
    },
    async getMenuListData() {
      const { type, hasChildOrgPriv } = this.permissions
      this.setType(type)
      this.setPriv(hasChildOrgPriv)

      // 有数据导入界面，当导入进步不等于100时，跳转
      this.freeRoutes.forEach(item => {
        const { name, children } = item
        if (name === 'Event') {
          children.forEach(child => {
            if (child.name === 'DataImport') {
              importGetProgress().then(({ data }) => {
                if (data.percent < 100) {
                  this.$router.push({ name: 'DataImport' })
                }
              })
              synchronizationGetProcess().then(({ data }) => {
                if (data.percent < 100) {
                  this.$router.push({ name: 'DataImport' })
                }
              })
            }
          })
        }
      })
    },
    getName(arr) {
      arr.forEach(item => {
        this.menuName.push(item.name)
      })
    }
  }
}
</script>
