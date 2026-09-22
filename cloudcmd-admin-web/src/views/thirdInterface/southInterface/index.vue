<template>
  <div class="south-interface">
    <el-card class="flex-card">    
      <SouthAppsManage />
    </el-card>
  </div>
</template>

<script>
import SouthAppsManage from './components/SouthAppsManage.vue'

export default {
  name: 'SouthInterface',
  components: {
    SouthAppsManage
  }
}
</script>

<style scoped>
.south-interface {
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

.flex-card /deep/.el-card__body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
</style>