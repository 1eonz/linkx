<template>
  <div class="police-report-container">
    <el-card class="flex-card">
      <el-tabs ref="policeReportTab" type="border-card" v-model="activeTab" @tab-click="tabClick">
        <el-tab-pane :key="tabPaneKey.Dock" label="警单对接" name="dock">
          <DockComponent />
        </el-tab-pane>
        <el-tab-pane :key="tabPaneKey.Manage" label="警单管理" name="manage">
          <ManageComponent />
        </el-tab-pane>
        <el-tab-pane :key="tabPaneKey.TypeManage" label="类型管理" name="typeManage">
          <TypeManageComponent />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script>
import DockComponent from './dock.vue'
import ManageComponent from './manage.vue'
import TypeManageComponent from './typeManage.vue'

export default {
  name: 'PoliceReport',
  components: {
    DockComponent,
    ManageComponent,
    TypeManageComponent
  },
  data() {
    return {
      activeTab: '',
      tabPaneKey: {
        Dock: `Dock-${Date.now()}`,
        Manage: `Manage-${Date.now()}`,
        TypeManage: `TypeManage-${Date.now()}`
      }
    }
  },
  mounted() {
    this.activeTab = this.$refs.policeReportTab?.panes?.[0]?.name
  },
  methods: {
    tabClick(tab) {
      const tabName = tab.paneName || tab.name
      this.tabPaneKey[tabName] = `${tabName}-${Date.now()}`
    }
  }
}
</script>

<style scoped>
.police-report-container {
  height: calc(100vh - 50px);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.flex-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.flex-card .el-tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.flex-card /deep/.el-card__body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.flex-card /deep/ .el-tabs__content {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: auto;
}

.flex-card /deep/ .el-tab-pane {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
</style>