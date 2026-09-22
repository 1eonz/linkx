<script lang="ts" setup></script>

<template>
  <RouterView v-slot="{ Component }">
    <KeepAlive>
      <component :is="Component" />
    </KeepAlive>
  </RouterView>
</template>

<style lang="less">
  @import '@/styles/reset.less';
  @import '@/styles/mixin.less';

  #app {
    width: 100vw;
    height: 100vh;
    color: var(--color-title);
    background: var(--background-default);
  }
</style>
