<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="出局" name="outbound">
          <outbound-data v-if="activeTab === 'outbound'" />
        </el-tab-pane>
        <el-tab-pane label="入局" name="inbound">
          <inbound-data v-if="activeTab === 'inbound'" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script>
import OutboundData from '../outboundData'
import InboundData from '../inboundData'

export default {
  name: 'DataManage',
  components: {
    OutboundData,
    InboundData
  },
  data() {
    return {
      activeTab: 'outbound'
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
