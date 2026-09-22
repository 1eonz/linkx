<script lang="ts" setup>
  import { ref, unref } from 'vue';

  import { sendTextMsg } from '@/bridge/post.js';
  // import { sendMessage } from '@/api/pim';
  import { Message } from '@/components/Message';
  import { useEmitter } from '@/hooks';
  import EmojiView from '@/pages/coordination/emoji/emojiView.vue';
  import InputEditor from '@/pages/coordination/emoji/inputEditor.vue';
  import { usePIMStore } from '@/store';
  import dataUtil from '@/utils/dataUtil';

  import { throttle } from 'lodash-es';

  import EmojiSelect from '../emoji/emojiSelect.vue';

  const props = withDefaults(
    defineProps<{
      data: any[];
      title: string;
      replyDirectly?: boolean;
    }>(),
    {
      replyDirectly: false,
    },
  );
  const emit = defineEmits(['closeDialog', 'submit']);
  const chatValue = ref('');
  const chatTextAreaRef = ref();

  const PIMStore = usePIMStore();

  function emojiChange(data) {
    chatTextAreaRef.value.appendEmoji(data);
  }

  /**
   * 短信发送时间间隔大于1秒
   */
  const handleSend = throttle(async () => {
    const content = unref(chatValue);
    if (content === '') {
      return;
    }
    chatTextAreaRef.value?.clear();

    if (dataUtil.wordSizeOf(content) > 10_000) {
      Message('不超过10000字节（3333个汉字或10000个英文字母）');
      return;
    }

    // const cooperationUser = PIMStore.user?.cooperationUser;
    // if (!cooperationUser) return;

    try {
      // 根据 REPLY_DIRECTLY 决定发送数据
      // 如果 REPLY_DIRECTLY 为 true 且数据量大于1，则按 groupId 去重
      let sendData = props.data;
      if (props.replyDirectly && props.data.length > 1) {
        // 按 groupId 去重，保留每个 groupId 的第一条数据
        sendData = props.data.filter(
          (item, index, arr) => arr.findIndex((t) => t.groupId === item.groupId) === index,
        );
      }

      // 使用 Promise.all 确保所有消息都发送完成
      await Promise.all(
        sendData.map(async (item) => {
          const { fromUserId, fromUserName, groupId, icsMsgId } = item;
          // 文本消息基本参数
          const param: any = {
            // category: 2,
            // cMsgId: `${Date.now()}`,
            // from: cooperationUser.userId,
            // fromRealUserId: userId,
            // msg: {
            //   srcMsgId: icsMsgId,
            //   text: `[@${fromUserId}:@${fromUserName}]${content}`,
            // },
            // msgType: 1,
            // plaintext: 1,
            // to: groupId,
            groupId,
            srcMsgId: icsMsgId,
            text: `[@${fromUserId}:@${fromUserName}]${content}`,
          };
          await sendTextMsg(param);
        }),
      );

      // 检查发送结果
      // const failedCount = results.filter((result) => !result.success).length;
      // const successCount = results.length - failedCount;

      // if (failedCount > 0) {
      //   Message(`发送完成：成功 ${successCount} 条，失败 ${failedCount} 条`);
      // } else {
      //   Message('批量发送成功');
      // }

      // 所有发送操作完成后，执行后续操作
      chatValue.value = '';
      PIMStore.setQuoteMsg({});
      emit('submit');
      emit('closeDialog');
      useEmitter().emit('refreshTabsData');
      setTimeout(() => {
        useEmitter().emit('refreshTableData');
      }, 1000);
    } catch (error) {
      console.error('批量发送消息时发生错误:', error);
      Message('批量发送失败，请重试');
    }
  }, 1000);

  function closeWindow() {
    emit('closeDialog');
  }
</script>

<template>
  <TdFrameBox
    :dragger="true"
    :is-light="true"
    size="small"
    :title="title"
    @close-frame-box="closeWindow"
  >
    <div class="bulk-responses-content">
      <div class="responses-list">
        <div v-for="item in props.data" :key="item.id" class="item">
          <div>提问内容：</div>
          <EmojiView :is-list="true" :text="item.text" />
        </div>
      </div>
      <div class="chat-input">
        <div class="uploader">
          <EmojiSelect class="upload upload-img" @change="emojiChange" />
        </div>
        <div class="submit-content">
          <InputEditor
            ref="chatTextAreaRef"
            v-model.trim="chatValue"
            filter-at-member-key=""
            :show-at-list="false"
            @send="handleSend"
          />
        </div>
      </div>
      <div class="bottom-button">
        <TdButton class="btn" :is-light="true" text="取消" type="normal" @click="closeWindow" />
        <TdButton
          class="btn"
          :disable="!chatValue"
          :is-light="true"
          text="发送"
          type="normal"
          @click="handleSend"
        />
      </div>
    </div>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  .bulk-responses-content {
    .responses-list {
      width: 100%;
      height: 250px;
      padding: 8px 16px;
      margin-top: 10px;
      margin-bottom: 10px;
      overflow-y: scroll;
      background: var(--background-color);
      backdrop-filter: blur(27.18px);

      .item {
        display: flex;
        border-bottom: 1px dashed rgb(255 255 255/10%);

        div {
          min-width: 80px;
          margin: 2px 0;
          font-size: 14px;
          font-weight: 500;
          color: var(--item-text-color);
        }
      }
    }

    .chat-input {
      position: relative;
      width: 100%;
      height: 148px;
      background: var(--background-color);
      backdrop-filter: blur(27px);
      border: 1px solid rgb(255 255 255 / 20%);

      .uploader {
        position: absolute;
        top: 5px;
        left: 5px;
        z-index: 100;
        display: flex;

        .upload {
          margin-right: 5px;
          cursor: pointer;
        }

        .upload-img {
          width: 20px;
          height: 20px;
        }
      }

      .submit-content {
        position: relative;
        display: flex;
        flex-direction: column;
        height: 100%;
        padding: 22px 14px 14px;
      }
    }

    .bottom-button {
      display: flex;
      justify-content: flex-end;
      width: 100%;
      margin-top: 10px;
      margin-bottom: 10px;

      .btn {
        width: 60px;
        margin-right: 10px;
        background: var(--td-button-bg) !important;
        border: 1px solid var(--td-button-border);

        :deep(.button-text-light) {
          color: var(--text-color) !important;
        }
      }
    }
  }

  :deep(.name-light) {
    z-index: none !important;
  }
</style>
