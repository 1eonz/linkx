<script setup lang="ts">
  import { showTime } from './helper';

  defineProps<{
    activeId?: string;
    data: any;
    showCloseIcon?: boolean;
  }>();
  const emit = defineEmits(['click', 'close']);

  async function itemClick(data) {
    emit('click', data);
  }

  function handleClose() {
    emit('close');
  }
</script>

<template>
  <div class="message-item" :class="{ active: activeId === data.chatId }" @click="itemClick(data)">
    <div class="icon-box">
      <Icon class="icon" :name="data.groupId ? 'group_msg' : 'single_msg'" />
      <div v-if="data.isRead !== 1 && !showCloseIcon" class="unread"></div>
    </div>
    <div class="info-box">
      <p class="name">
        <span>{{ data.title }}</span>
        <Icon v-if="showCloseIcon" class="close" name="reject" @click="handleClose" />
        <span v-else>{{ showTime(data.sendTime) }}</span>
      </p>
      <TdTooltip :content="data.content">
        <p class="content">{{ data.content }}</p>
      </TdTooltip>
    </div>
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .message-item {
    display: flex;
    height: 60px;
    padding: 12px 8px;
    margin-bottom: 10px;
    cursor: pointer;
    background: rgb(173 204 240 / 7%);
    border: 1px solid rgb(255 255 255 / 20%);

    &:hover,
    &.active {
      padding: 13px 9px;
      background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);
      border: none;
    }

    .icon-box {
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;
      width: 36px;
      height: 36px;
      margin-right: 4px;
      border: 1px solid rgb(26 255 251 / 32%);
      border-radius: 3px;
      fill: #fff;

      .icon {
        width: 24px;
        height: 24px;
      }

      .unread {
        position: absolute;
        top: -4px;
        right: -4px;
        width: 8px;
        height: 8px;
        background: rgb(26 255 251 / 100%);
        border-radius: 50%;
        box-shadow: 0 1px 3px rgb(26 255 251 / 100%);
      }
    }

    .info-box {
      flex: 1;

      .name {
        display: flex;
        justify-content: space-between;

        span:nth-of-type(1) {
          width: 190px;
          overflow: hidden;
          font-size: 14px;
          font-weight: 500;
          color: rgb(255 255 255 / 100%);
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        span:nth-of-type(2) {
          font-size: 12px;
          font-weight: 400;
          color: rgb(171 216 255 / 100%);
        }

        .close {
          width: 16px;
          height: 16px;
          fill: #fff;
        }
      }

      .content {
        width: 228px;
        font-size: 12px;
        font-weight: 400;
        color: rgb(171 216 255 / 100%);
        .ellipsis1();
      }
    }
  }
</style>
