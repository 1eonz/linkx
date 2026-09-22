<template>
  <view class="container">
    <view class="mobileWrap">
      <ExamplePage />
    </view>
  </view>
</template>

<script setup>
  import ExamplePage from '@/pagesMain/sdk/ExamplePage.vue';
</script>
<style lang="scss">
  .container {
    width: 100%;
    min-height: 100vh;
  }

  .mobileWrap {
    width: 100%;
  }
</style>
