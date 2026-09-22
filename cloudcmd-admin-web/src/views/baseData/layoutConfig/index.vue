<template>
  <div class="layout-config">
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane
        v-for="tab in visibleTabs"
        :key="tab.name"
        :label="tab.label"
        :name="tab.name"
      >
        <component
          :is="tab.component"
          :key="tabKeys[tab.name]"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import CommonConfig from './components/CommonConfig.vue'
import AppH5Config from './components/AppH5Config.vue'
import PcConfig from './components/PcConfig.vue'
import OpsStatistic from './components/OpsStatistic.vue'
import { getIsAdmin } from '@/utils/auth'

// 全部一级 Tab 定义
const ALL_TABS = [
  { name: 'common-config', label: '公共设置', component: CommonConfig },
  { name: 'app-h5-config', label: 'App H5设置', component: AppH5Config },
  { name: 'pc-config', label: 'PC端设置', component: PcConfig },
  { name: 'ops-statistic', label: '运维统计', component: OpsStatistic }
]

export default {
  components: {
    CommonConfig,
    AppH5Config,
    PcConfig,
    OpsStatistic
  },
  data() {
    const isAdmin = getIsAdmin()
    const visibleTabs = isAdmin
      ? ALL_TABS
      : ALL_TABS.filter(tab => tab.name === 'app-h5-config')
    return {
      // 初始激活的 Tab：管理员默认"公共设置"，非管理员默认"App H5设置"
      activeTab: visibleTabs.length > 0 ? visibleTabs[0].name : 'common-config',
      // 各 Tab 的 key，用于切换时强制重建组件
      tabKeys: {
        'common-config': 'common-config',
        'app-h5-config': 'app-h5-config',
        'pc-config': 'pc-config',
        'ops-statistic': 'ops-statistic'
      }
    }
  },
  computed: {
    isAdmin() {
      return getIsAdmin()
    },
    // 根据是否管理员过滤可见 Tab
    // 非管理员只显示 "App H5设置"（其内部二级 Tab 由 AppH5Config.vue 根据菜单权限控制）
    visibleTabs() {
      if (this.isAdmin) {
        return ALL_TABS
      }
      return ALL_TABS.filter(tab => tab.name === 'app-h5-config')
    }
  },
  watch: {
    activeTab(newVal) {
      // 切换 Tab 时强制重建组件，确保数据为最新
      this.tabKeys[newVal] = newVal + '-' + Date.now()
    },
    // 可见 Tab 列表变化后，确保当前激活的 Tab 是可见的
    visibleTabs: {
      handler(newTabs) {
        if (newTabs.length === 0) return
        const stillVisible = newTabs.some(t => t.name === this.activeTab)
        if (!stillVisible) {
          this.activeTab = newTabs[0].name
        }
      },
      immediate: true
    }
  }
}
</script>
<style scoped>
.layout-config {
  padding: 20px;
}
</style>
