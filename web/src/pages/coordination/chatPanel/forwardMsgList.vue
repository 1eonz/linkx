<script setup lang="ts">
  import MessageList from './messageList.vue';

  defineProps<{
    msgList: any;
    title: string;
  }>();

  const emit = defineEmits(['closeDialog']);

  function closeWindow() {
    emit('closeDialog');
  }
</script>

<template>
  <TdFrameBox
    :dragger="true"
    :is-light="true"
    size="normal"
    :title="title"
    @close-frame-box="closeWindow"
  >
    <div class="msg-content">
      <div class="msg-box">
        <MessageList :hidden-menu-all="true" :message-list="msgList" />
      </div>
    </div>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  .frame-box {
    height: calc(100% - 250px) !important;
  }

  .frame-box-container {
    .msg-content {
      width: 100%;
      height: 100%;
      overflow: hidden;

      .msg-box {
        width: 100%;
        height: 100%;
        overflow-y: scroll;

        .message-list {
          height: 100%;
        }
      }
    }
  }
</style>
