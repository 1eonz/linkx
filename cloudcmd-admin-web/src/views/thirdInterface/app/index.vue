<template>
  <div class="unified-comm">
    <el-card class="flex-card">
      <el-tabs v-model="activeTab">
        <!-- 融合通信服务器配置 -->
        <el-tab-pane label="分类管理" name="appManage">
          <groupManage v-if="activeTab === 'appManage'" />
        </el-tab-pane>

        <!-- 融合通信访问授权 -->
        <el-tab-pane label="应用管理" name="groupManage">
          <appManage v-if="activeTab === 'groupManage'" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script>
import appManage from './appManage/index.vue'
import groupManage from './groupManage/index.vue'

export default {
  components: {
    appManage,
    groupManage
  },
  data() {
    return {
      activeTab: 'appManage',
      tabPaneKey: Date.now()
    }
  },
  methods: {
    tabClick() {
      this.tabPaneKey = Date.now()
    }
  }
}
</script>

<style scoped>
.unified-comm {
  padding: 20px;
  min-height: calc(100vh - 50px);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.flex-card {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.flex-card .el-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.flex-card .el-tabs__content {
  flex: 1;
  overflow: auto;
}
</style>
