<template>
  <div class="unified-comm">
    <el-card class="flex-card">    
      <el-tabs v-model="activeTab" @tab-click="tabClick">
        <!-- 融合通信服务器配置 -->
        <el-tab-pane label="通信服务服务器配置" name="IcpServerConfig" :key="tabPaneKey">
          <IcpServerConfig />
        </el-tab-pane>
        
        <!-- 融合通信访问授权 -->
        <el-tab-pane label="通信服务授权管理" name="IcpAuthManage" :key="tabPaneKey+1">
          <IcpAuthManage />
        </el-tab-pane>
        <!-- 设备类型管理 -->
        <el-tab-pane label="设备类型管理" name="DeviceTypeManage" :key="tabPaneKey+2">
          <DeviceTypeManage />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script>
import IcpServerConfig from './components/IcpServerConfig.vue'
import IcpAuthManage from './components/IcpAuthManage.vue'
import DeviceTypeManage from './components/DeviceTypeManage.vue'

export default {
  components: {
    IcpServerConfig,
    IcpAuthManage,
    DeviceTypeManage
  },
  data() {
    return {
      activeTab: 'IcpServerConfig',
      tabPaneKey: Date.now(),
    }
  },
  methods: {
    tabClick() {
      this.tabPaneKey = Date.now();
    },
  },
}
</script>

<style scoped>
.unified-comm {
  padding: 20px;
  height: calc(100vh - 50px);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.flex-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.flex-card ::v-deep .el-card__body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
}

.flex-card ::v-deep .el-tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.flex-card ::v-deep .el-tabs__header {
  flex-shrink: 0;
  margin: 0;
  padding: 0 16px;
}

.flex-card ::v-deep .el-tabs__content {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.flex-card ::v-deep .el-tab-pane {
  height: 100%;
  overflow: hidden;
}
</style>