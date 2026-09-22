<template>
  <div class="placeholder-container">
    <div class="placeholder-content">
      <i class="el-icon-warning-outline placeholder-icon"></i>
      <h2>正在开发中</h2>
      <p>该功能模块暂未上线，敬请期待</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Placeholder'
}
</script>

<style lang="scss" scoped>
.placeholder-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  min-height: 400px;
}
.placeholder-content {
  text-align: center;
  color: #909399;
}
.placeholder-icon {
  font-size: 64px;
  margin-bottom: 16px;
}
h2 {
  font-size: 20px;
  margin: 0 0 8px 0;
  color: #606266;
}
p {
  font-size: 14px;
  margin: 0;
}
</style>
