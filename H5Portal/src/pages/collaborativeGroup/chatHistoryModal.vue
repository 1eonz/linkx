<template>
  <view class="modal-overlay" v-if="showEditModal" @click="closeModal">
    <view class="modal-content" @click.stop>
      <view class="modal-header">
        <div class="modal-left"></div>
        <text class="modal-title">{{ selectedGroup.groupName }}</text>
        <view class="close-btn" @click="closeModal">×</view>
      </view>
      <view class="modal-body">
        <ChatHistory :groupId="selectedGroup.groupId" :groupName="selectedGroup.groupName"></ChatHistory>
      </view>
    </view>
  </view>
</template>
<script lang="ts" setup>
  import { ref } from 'vue';

  import ChatHistory from './ChatHistory.vue';
  const selectedGroup = ref(null);
  const showEditModal = ref(false);
  // 查看历史聊天
  const onOpenHistoryChat = (item) => {
    console.log(item);
    selectedGroup.value = item;
    showEditModal.value = true;
  };
  // 关闭弹框
  function closeModal() {
    showEditModal.value = false;
  }
  defineExpose({
    onOpenHistoryChat,
  });
</script>
<style lang="scss" scoped>
  .modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: flex-end;
    justify-content: center;
    z-index: 100;

    .modal-content {
      width: 100%;
      background-color: #fff;
      border-radius: 16px 16px 0 0;
      height: 80vh;
      display: flex;
      flex-direction: column;
    }
  }

  .modal-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 20px 16px 16px;
    border-bottom: 1px solid #f0f0f0;

    .modal-left {
      // width: 40px;
      color: rgba(38, 78, 209, 1);
    }

    .modal-title {
      width: 80%;
      font-size: 18px;
      font-weight: 600;
      color: #333;
      text-align: center;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      word-break: break-all;
    }

    .close-btn {
      width: 24px;
      height: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
      color: #999;
      cursor: pointer;
    }
  }

  .modal-body {
    height: 70vh;
    padding: 16px;
  }
</style>
