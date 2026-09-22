<template>
  <view class="empty-contain">
    <img width="120px" height="120px" src="@/static/task/indexTypeEmpty.png" />
    <view class="empty-text">空空如也~</view>
  </view>
</template>

<script></script>

<style lang="scss" scoped>
  .empty-contain {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    height: 100%;
    min-height: 200px;
    max-height: 400px;
    image {
      width: 120px;
      height: 120px;
    }
    .empty-text {
      font-size: 12px;
      font-weight: 400;
      letter-spacing: 0px;
      line-height: 17.38px;
      color: rgba(90, 99, 131, 1);
      text-align: center;
    }
  }
</style>
