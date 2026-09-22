<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="服务器管理" name="server">
          <server-manage v-if="activeTab === 'server'" />
        </el-tab-pane>
        <el-tab-pane label="客户端管理" name="client">
          <client-manage v-if="activeTab === 'client'" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script>
import ServerManage from '../serverManage'
import ClientManage from '../clientManage'

export default {
  name: 'NodeManagement',
  components: {
    ServerManage,
    ClientManage
  },
  data() {
    return {
      activeTab: 'server'
    }
  }
}
</script>

<style lang="scss" scoped>
.app-container {
  padding: 20px;
  height: calc(100vh - 84px);
  box-sizing: border-box;
}

.card {
  height: 100%;

  ::v-deep .el-card__body {
    padding: 16px;
    height: 100%;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
  }
}

::v-deep .el-tabs {
  height: 100%;
  display: flex;
  flex-direction: column;
}

::v-deep .el-tabs__header {
  margin-bottom: 16px;
  flex-shrink: 0;
}

::v-deep .el-tabs__content {
  flex: 1;
  overflow: auto;
}

::v-deep .el-tab-pane {
  height: 100%;
}
</style>
