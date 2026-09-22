<script lang="ts" setup>
  import { onMounted } from 'vue';
  import { useWebview2SelectFix } from '@/composables/useWebview2SelectFix'

  // 修复 WebView2 环境下 el-select 选项选不中问题
  useWebview2SelectFix()
  onMounted(() => {
    // 禁用右键
    window.document.addEventListener('contextmenu', () => {
      return false;
    });
  });
</script>

<template>
  <RouterView v-slot="{ Component }">
    <KeepAlive>
      <component :is="Component" />
    </KeepAlive>
  </RouterView>
</template>

<style lang="less">
  @import '@/styles/font-awesome.css';
  @import '@/styles/reset.less';
  @import '@/styles/mixin.less';
  @import '@/styles/map.less';
  @import '@/styles/common.less';
  @import '@/styles/themes.less';
  @import '@/styles/markdown.less';

  #app {
    width: 100vw;
    height: 100vh;
    color: var(--color-title);
    background: var(--background-default);
  }
</style>
