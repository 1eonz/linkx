<script setup lang="ts">
  import { computed } from 'vue';

  import { Message } from '@/components/Message';

  import ids from 'virtual:svg-icons-names';

  const icons = computed(() => ids.map((i) => i.replace('icon-', '')));

  function handleCopy(name: string) {
    navigator.clipboard.writeText(name);
    Message({
      message: `copy ${name}`,
      type: 'success',
    });
  }
</script>

<template>
  <div class="content">
    <div v-for="iconName in icons" :key="iconName" class="icon-item" @click="handleCopy(iconName)">
      <Icon color="black" :name="iconName" />
      <ElTooltip
        :content="iconName"
        :open-delay="500"
        placement="bottom"
        popper-class="el-tooltip-style"
        :visible-arrow="false"
      >
        <span class="name-place">{{ iconName }}</span>
      </ElTooltip>
    </div>
  </div>
</template>

<style scoped lang="less">
  .content {
    box-sizing: border-box;
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
    width: 1400px;
    height: 100vh;
    padding: 20px;
    margin: 0 auto;
    overflow: hidden auto;
    background: #fff;

    .icon-item {
      box-sizing: border-box;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: space-between;
      width: 10%;
      height: 120px;
      padding: 15px;

      .td-icon {
        width: 40px;
        height: 40px;
      }

      .name-place {
        display: inline-block;
        width: 100%;
        overflow: hidden;
        color: #99a9bf;
        text-align: center;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }
</style>
