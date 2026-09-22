<template>
  <div class="agent-interface">
    <el-card class="flex-card">
      <el-tabs v-model="activeTab" :before-leave="beforeLeave" @tab-click="tabClick">
        <el-tab-pane label="系统设置" name="AgentConfig" :key="tabKeys.AgentConfig">
          <AgentConfig @deploy-mode-change="handleDeployModeChange" @import-success="handleImportSuccess" />
        </el-tab-pane>
        <el-tab-pane label="AI智能体管理" name="AgentManage" :key="tabKeys.AgentManage">
          <AgentManage ref="agentManageRef" />
        </el-tab-pane>
        <el-tab-pane label="AI查询明细" name="AgentHistory" :key="tabKeys.AgentHistory">
          <AgentHistory />
        </el-tab-pane>
        <el-tab-pane label="AI智能体文件接口" name="AgentFile" :key="tabKeys.AgentFile">
           <AgentFile />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script>
import AgentConfig from './components/AgentConfig.vue'
import AgentManage from './components/AgentManage.vue'
import AgentHistory from './components/AgentHistory.vue'
import AgentFile from './components/AgentFile.vue'

const PATH_MAP = {
  AgentManage: '/layout/agent',
  AgentHistory: '/layout/queryStatistics',
  AgentFile: '/layout/agentFile',
}

export default {
  name: 'AgentInterface',
  components: {
    AgentConfig,
    AgentManage,
    AgentHistory,
    AgentFile
  },
  data() {
    return {
      activeTab: 'AgentConfig',
      tabKeys: {
        AgentConfig: `AgentConfig-${Date.now()}`,
        AgentManage: `AgentManage-${Date.now()}`,
        AgentHistory: `AgentHistory-${Date.now()}`,
        AgentFile: `AgentFile-${Date.now()}`,
      },
      separatedDeploy: false,
      frontendHost: '',
    }
  },
  methods: {
    beforeLeave(activeName) {
      console.log(11111111, this.separatedDeploy, activeName)
      if (this.separatedDeploy && activeName !== 'AgentConfig') {
        console.log(this.frontendHost, 'this.frontendHost')
        // 处理 baseUrl
        let baseUrl = this.frontendHost
        // 移除 baseUrl 末尾的斜杠
        baseUrl = baseUrl.replace(/\/$/, '')
        // 拼接完整路径
        const fullPath = baseUrl  + (PATH_MAP[activeName] || '')
        console.log('fullPath', fullPath)
        window.open(fullPath, '_blank')
        return false
      }
    },
    handleDeployModeChange(params) {
       console.log(22222222, params)
      this.separatedDeploy = params.separatedDeploy
      this.frontendHost = params.groupAiFrontendHost
      if (params.separatedDeploy) {
        this.activeTab = 'AgentConfig'
      }
    },
    handleImportSuccess() {
      this.$refs.agentManageRef?.fetchData()
    },
    tabClick(tab) {
      const tabName = tab.name
      this.tabKeys[tabName] = `${tabName}-${Date.now()}`
    },
  }
}
</script>

<style scoped>
.agent-interface {
  padding: 20px;
  max-height: calc(100% - 50px);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.flex-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.flex-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.flex-card .el-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.flex-card :deep(.el-tabs__content) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.flex-card :deep(.el-tab-pane) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
</style>
