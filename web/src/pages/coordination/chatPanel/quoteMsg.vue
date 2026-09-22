<script setup lang="ts">
  import { computed, ref, watch } from 'vue';

  import EmojiView from '@/pages/coordination/emoji/emojiView.vue';
  import { usePIMStore } from '@/store';

  const props = defineProps<{
    attachList: any[];
    quoteMsg?: any;
    quoteMsgId?: string;
  }>();
  const emit = defineEmits(['toQuoteMsg', 'getAttachIndex', 'getAttachList', 'getAttach']);
  const PimStore = usePIMStore();
  const personName = ref('');

  const msgDetail = computed(() => {
    if (props.quoteMsg?.msgId) {
      return props.quoteMsg;
    }
    if (props.quoteMsgId) {
      const index = PimStore.currentChatMessages.findIndex(
        (item) => item.msgId === props.quoteMsgId,
      );
      if (index !== -1) {
        return PimStore.currentChatMessages[index];
      }
    }
    return {};
  });

  watch(
    () => msgDetail.value,
    (item: any) => {
      getUserName(item.from);
    },
    { immediate: true },
  );

  async function getUserName(id) {
    const info = await PimStore.getUserInfo(id);
    personName.value = info?.name || '';
  }

  // 查找预览index
  function getAttachIndex(fileKey) {
    const index = props.attachList.findIndex((item) => item.fileKey === fileKey);
    return index;
  }

  // 获取预览列表
  function getAttachList() {
    return props.attachList.map((item) => {
      return item.url;
    });
  }

  function getAttach(item) {
    if (!item || !item.msg || !item.msg.fileKey) {
      return;
    }
    const imageInfo = props.attachList.filter((val) => item.msg.fileKey === val.fileKey);
    if (imageInfo.length > 0) {
      return imageInfo[0].url;
    }
    return '';
  }
  // 点击去引用信息的位置并高亮5s
  function toQuoteMsg() {
    emit('toQuoteMsg', msgDetail.value);
  }

  function getFileTypeShow(type) {
    return msgDetail.value?.msg?.fileType === type;
  }
</script>

<template>
  <div class="msg-content">
    <div class="msg-box">
      <!-- 视频引用 -->
      <div v-if="getFileTypeShow(3)" class="text" @click="toQuoteMsg">
        {{ personName }}：<span>【视频】</span>
      </div>
      <!-- 图片引用 -->
      <div v-else-if="getFileTypeShow(1)" class="text img-quote">
        {{ personName }}：
        <ElImage
          fit="contain"
          :initial-index="getAttachIndex(msgDetail.msg.fileKey)"
          :preview-src-list="getAttachList()"
          :src="getAttach(msgDetail)"
          style="max-width: 100%; max-height: 70px"
        />
      </div>
      <!-- 音频引用 -->
      <div v-else-if="getFileTypeShow(2)" class="text" @click="toQuoteMsg">
        {{ personName }}：<span>【音频】</span>
      </div>
      <!-- 卡片引用 -->
      <!-- <div v-else-if="getFileTypeShow(4)" class="text" @click="toQuoteMsg">
        {{ personName }}：<span>【卡片】</span>
      </div> -->
      <!-- 发送文件引用 -->
      <div
        v-else-if="
          getFileTypeShow(4) ||
          getFileTypeShow(5) ||
          getFileTypeShow(6) ||
          getFileTypeShow(7) ||
          getFileTypeShow(8) ||
          getFileTypeShow(9) ||
          getFileTypeShow(10)
        "
        class="text"
        @click="toQuoteMsg"
      >
        {{ personName }}：<span>【文件】{{ msgDetail.msg?.fileName }}</span>
      </div>
      <div v-else-if="msgDetail?.msg?.forwardMsgs" class="text" @click="toQuoteMsg">
        {{ personName }}：<span>【聊天记录】</span>
      </div>
      <!-- 文字引用 -->
      <div v-else class="text" @click="toQuoteMsg">
        {{ personName }}：
        <EmojiView :is-list="false" style="display: inline-block" :text="msgDetail.msg?.text" />
        <!-- {{ msgDetail.msg?.text }} -->
      </div>
    </div>
  </div>
</template>

<style scoped lang="less">
  .msg-content {
    box-sizing: border-box;
    display: flex;
    padding: 16px;

    .person-name {
      font-size: 10px;
      font-weight: 500;
      color: var(--item-text-color);
      text-align: left;
    }

    .msg-box {
      .text {
        display: -webkit-box; /* 必须设置，用于多行溢出 */
        width: 100%; /* 设置容器宽度 */
        overflow: hidden; /* 隐藏超出的内容 */
        font-size: 10px;
        font-weight: 500;
        color: var(--item-text-color);
        text-align: left;
        -webkit-line-clamp: 2; /* 限制显示的行数，例如显示 3 行 */
        -webkit-box-orient: vertical; /* 设置盒子排列方向为垂直 */
        cursor: pointer;

        span {
          color: #159aff;
        }
      }

      .img-quote {
        display: flex;
        align-items: flex-start;

        :deep(.el-image img) {
          max-height: 70px;
        }
      }
    }
  }

  :deep(.emoji-view) {
    display: inline !important;
    font-size: 10px;
    font-weight: 500;
    color: var(--item-text-color);
  }
</style>
