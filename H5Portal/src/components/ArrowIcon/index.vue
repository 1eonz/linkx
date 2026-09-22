<template>
  <text class="arrow-icon">
    <text class="arrow-icon-text">{{ text }}</text>
    <view class="arrow-right"></view>
  </text>
</template>

<script setup lang="ts">
    defineProps({
        text: {
            type: String,
            default: '更多'
        }
    })
</script>

<style scoped lang="scss">
.arrow-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: rgba(96, 98, 102, 0.1);
    border-radius: 20px;
    
    .arrow-icon-text {
        font-size: 14px;
        padding: 4px 2px 4px 16px;
    }
    .arrow-right {
        width: 7px;
        height: 7px;
        border-right: 1px solid rgb(96, 98, 102);
        border-bottom: 1px solid rgb(96, 98, 102);
        transform: rotate(-45deg);
        margin-right: 12px;
    }
}
</style>
