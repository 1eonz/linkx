<script setup lang="ts">
  import Aside from './aside.vue';
</script>

<template>
  <el-container class="common-layout">
    <el-header class="header" />
    <el-container>
      <Aside />
      <el-main>
        <RouterView v-slot="{ Component }">
          <component :is="Component" />
        </RouterView>
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped lang="less">
  .common-layout {
    height: 100%;
    width: 100%;

    .header {
      width: 100%;
      height: 56px;
      background: rgba(30, 82, 242, 1);
    }

    .el-container {
      height: calc(100vh - 56px);
    }

    .el-main {
      padding: 12px;
    }
  }
</style>
