<script setup lang="ts">
  import { computed, ref, unref, watch } from 'vue';

  import EmojiView from '@/pages/coordination/emoji/emojiView.vue';
  import { getIp } from '@/utils';
  // import MsgVideoHistory from '@/pages/notification/components/msgVideoHistory.vue';
  import MsgVoiceHistory from '@/pages/notification/components/msgVoiceHistory.vue';
  import { usePIMStore } from '@/store';

  import { openForwardMsgs } from '../common';
  import MemberDetailsPopover from './memberDetailsPopover/index.vue';
  import MsgFile from './msgFile.vue';
  import MsgForward from './msgForward.vue';

  const props = defineProps<{
    attachList: any[];
    memberList?: any[];
    quoteMsg?: any;
    quoteMsgId?: string;
  }>();
  const emit = defineEmits([
    'toQuoteMsg',
    'getAttachIndex',
    'getAttachList',
    'getAttach',
    // 'clickVideo',
  ]);

  const PimStore = usePIMStore();
  const personName = ref('');

  const userId = computed(() => PimStore.user.id);

  // 解析消息内容 - 修改后的版本
  const parseMessageContent = (message) => {
    if (!message) return {};

    try {
      // 文本消息类型1
      if (message.msgType === 1) {
        const msgContent = message.msg;

        // 处理 {key1=value1, key2=value2} 格式
        if (
          typeof msgContent === 'string' &&
          msgContent.startsWith('{') &&
          msgContent.endsWith('}')
        ) {
          const msgData = {};
          // 去除首尾的大括号
          const content = msgContent.slice(1, -1);
          // 按逗号分割键值对
          const pairs = content.split(',');

          pairs.forEach((pair) => {
            const [key, ...valueParts] = pair.split('=');
            if (key && valueParts.length > 0) {
              const value = valueParts.join('=').trim();
              const trimmedKey = key.trim();
              // 处理布尔值和数字
              if (value === 'true') {
                msgData[trimmedKey] = true;
              } else if (value === 'false') {
                msgData[trimmedKey] = false;
              } else if (!isNaN(Number(value)) && value !== '') {
                msgData[trimmedKey] = Number(value);
              } else {
                msgData[trimmedKey] = value;
              }
            }
          });
          return msgData;
        }
        // 普通文本
        return { text: msgContent };
      }
      // 文件消息类型2
      else if (message.msgType === 2) {
        try {
          // 尝试解析JSON格式
          return JSON.parse(message.msg);
        } catch {
          // 备用解析：处理键值对格式
          if (
            typeof message.msg === 'string' &&
            message.msg.startsWith('{') &&
            message.msg.endsWith('}')
          ) {
            const msgData = {};
            const content = message.msg.slice(1, -1);
            const pairs = content.split(',');

            pairs.forEach((pair) => {
              const [key, ...valueParts] = pair.split('=');
              if (key && valueParts.length > 0) {
                const value = valueParts.join('=').trim();
                const trimmedKey = key.trim();
                // 处理特殊值
                switch (value) {
                  case 'false': {
                    msgData[trimmedKey] = false;

                    break;
                  }
                  case 'null': {
                    msgData[trimmedKey] = null;

                    break;
                  }
                  case 'true': {
                    msgData[trimmedKey] = true;

                    break;
                  }
                  default: {
                    msgData[trimmedKey] = !isNaN(value) && value !== '' ? Number(value) : value;
                  }
                }
              }
            });
            return msgData;
          }
          return { text: message.msg };
        }
      }
      // 其他消息类型 (5,6,8)
      else if ([5, 6, 8].includes(message.msgType)) {
        try {
          return JSON.parse(message.msg);
        } catch {
          // 尝试键值对解析
          if (
            typeof message.msg === 'string' &&
            message.msg.startsWith('{') &&
            message.msg.endsWith('}')
          ) {
            const msgData = {};
            const content = message.msg.slice(1, -1);
            const pairs = content.split(',');

            pairs.forEach((pair) => {
              const [key, ...valueParts] = pair.split('=');
              if (key && valueParts.length > 0) {
                const value = valueParts.join('=').trim();
                const trimmedKey = key.trim();
                switch (value) {
                  case 'false': {
                    msgData[trimmedKey] = false;

                    break;
                  }
                  case 'null': {
                    msgData[trimmedKey] = null;

                    break;
                  }
                  case 'true': {
                    msgData[trimmedKey] = true;

                    break;
                  }
                  default: {
                    msgData[trimmedKey] = !isNaN(value) && value !== '' ? Number(value) : value;
                  }
                }
              }
            });
            return msgData;
          }
          return { text: message.msg };
        }
      }

      // 默认返回原始消息
      return { text: message.msg };
    } catch (error) {
      console.error('解析消息失败:', error, message);
      return { text: message.msg };
    }
  };

  const msgDetail = computed(() => {
    console.log('quoteMsg', props.quoteMsg, props.quoteMsgId);
    if (props.quoteMsg?.msgId) {
      return {
        ...props.quoteMsg,
        msg: parseMessageContent(props.quoteMsg),
      };
    }
    if (props.quoteMsgId) {
      const index = PimStore.currentChatMessages.findIndex(
        (item) => item.msgId === props.quoteMsgId,
      );
      if (index !== -1) {
        const message = PimStore.currentChatMessages[index];
        return {
          ...message,
          msg: parseMessageContent(message),
        };
      }
    }
    return { msg: {} };
  });

  const msg = computed(() => msgDetail.value.msg || {});

  // 从群组成员中获取用户名称，支持协同岗拼接真实用户名
  const getUserNameFromMembers = (userId, fromRealUserId?) => {
    if (!props.memberList || !userId) return `用户${userId}`;
    const member = props.memberList.find((m) => m.id === userId);

    if (fromRealUserId) {
      // 协同岗消息：优先使用已拼接好的realName，否则手动拼接协同岗名称和真实用户名
      if (member?.realName && member.realName !== member?.name) {
        return member.realName;
      }
      const collabName = member?.name || `用户${userId}`;
      const realMember = props.memberList.find((m) => m.id == fromRealUserId);
      const realName = realMember?.name || realMember?.realName;
      if (realName) {
        return `${collabName}(${realName})`;
      }
      return collabName;
    }

    return member?.name || member?.realName || `用户${userId}`;
  };

  watch(
    () => msgDetail.value,
    (item: any) => {
      if (item?.from) {
        // 优先从群组成员中获取名称，协同岗消息传入fromRealUserId以拼接真实用户名
        const nameFromMembers = getUserNameFromMembers(item.from, item.fromRealUserId);
        if (nameFromMembers && nameFromMembers !== `用户${item.from}`) {
          personName.value = nameFromMembers;
        } else {
          // 如果群组成员中没有，再从用户信息获取
          getUserName(item.from, item.fromRealUserId);
        }
      }
    },
    { immediate: true },
  );

  async function getUserName(id, fromRealUserId?) {
    if (!id) {
      personName.value = '';
      return;
    }
    try {
      // 协同岗消息：同时获取协同岗名称和真实用户名
      if (fromRealUserId) {
        const [collabInfo, realInfo] = await Promise.all([
          PimStore.getUserInfo(id),
          PimStore.getUserInfo(fromRealUserId),
        ]);
        const collabName = collabInfo?.name || `用户${id}`;
        const realName = realInfo?.name;
        personName.value = realName ? `${collabName}(${realName})` : collabName;
      } else {
        const info = await PimStore.getUserInfo(id);
        personName.value = info?.name || `用户${id}`;
      }
    } catch (error) {
      console.error('获取用户信息失败:', error);
      personName.value = `用户${id}`;
    }
  }

  // 获取文件URL
  function getFileUrl(fileKey, filePath) {
    if (!fileKey && !filePath) return '';

    // 首先从attachList中查找
    if (props.attachList && fileKey) {
      const attachment = props.attachList.find((item) => item.fileKey === fileKey);
      if (attachment && attachment.url) {
        return attachment.url;
      }
    }

    // 如果attachList中没有，使用filePath构建URL
    if (filePath) {
      return `${getIp()}/linkx/desktop${filePath}`;
    }

    return '';
  }

  // 获取图片URL
  function getImageUrl() {
    if (msg.value.fileType === 1) {
      return getFileUrl(msg.value.fileKey, msg.value.filePath);
    }
    return '';
  }

  // 获取视频URL
  function getVideoUrl() {
    console.log('视频：', msg.value);
    if (msg.value.fileType === 3) {
      console.log(msg.value.fileKey, msg.value.filePath);
      return getFileUrl(msg.value.fileKey, msg.value.filePath);
    }
    return '';
  }

  // 获取音频URL
  function getAudioUrl() {
    if (msg.value.fileType === 2) {
      return getFileUrl(msg.value.fileKey, msg.value.filePath);
    }
    return '';
  }

  // 点击去引用信息的位置并高亮5s
  function toQuoteMsg() {
    console.log(msgDetail.value,"msgDetail.value")
    if (!msgDetail.value.msgId) {
      console.error('引用消息ID为空')
      return;
    }

    const { msg } = msgDetail.value;
    if (msg.cardType === 'userCard') {
      // openMemberDetails({ id: msg.data?.userId || msg.userCardData?.userId });
    } else if (msg.forwardMsgs) {
      openForwardMsgs(msg, '', '');
    }
    emit('toQuoteMsg', msgDetail.value);
  }

  function isImageMessage() {
    return msgDetail.value.msgType === 2 && msg.value.fileType === 1;
  }

  function isVideoMessage() {
    return msgDetail.value.msgType === 2 && msg.value.fileType === 3;
  }

  function isAudioMessage() {
    return msgDetail.value.msgType === 2 && msg.value.fileType === 2;
  }

  function isFileMessage() {
    return (
      msgDetail.value.msgType === 2 &&
      msg.value.fileType &&
      [4, 5, 6, 7, 8, 9, 10].includes(msg.value.fileType)
    );
  }

  function isUserCardMessage() {
    return msgDetail.value.msgType === 5;
  }

  function isForwardMessage() {
    return msgDetail.value.msgType === 8 || msg.value.forwardMsgs;
  }

  function isGroupJoiningMessage() {
    return msgDetail.value.msgType === 6;
  }

  function isTextMessage() {
    return msgDetail.value.msgType === 1;
  }

  function isSelf(data) {
    if (!data || !data.from) return false;
    return Number(data.from) === unref(userId);
  }

  // 获取群接龙内容
  const getGroupJoiningContent = () => {
    const content = msg.value;
    if (!content) return null;

    let participants = [];
    if (content.userTxts && Array.isArray(content.userTxts)) {
      participants = content.userTxts.map((userTxt, index) => {
        return {
          isCreator: index === 0,
          name: userTxt.txt || `用户${userTxt.userId}`,
          userId: userTxt.userId,
        };
      });
    }

    let creatorName = '未知用户';
    if (participants.length > 0) {
      creatorName = (participants[0] as { name: string }).name;
    } else if (content.creator) {
      creatorName = getUserNameFromMembers(content.creator) || `用户${content.creator}`;
    }

    return {
      creatorName,
      participantCount: participants.length,
      participants,
      text: content.text || '未命名接龙',
    };
  };
</script>

<template>
  <div class="msg-content" @click="toQuoteMsg">
    <div class="msg-box">
      <!-- 个人名片引用 -->
      <MemberDetailsPopover @click.stop v-if="isUserCardMessage()" :user-id="msg.data?.userId || msg.userCardData?.userId">
        <div class="text2">
          {{ personName }}：<span>[个人名片]{{ msg.data?.userName || msg.userCardData?.userName || '未知用户' }}</span>
        </div>
      </MemberDetailsPopover>

      <!-- 视频引用 -->
      <div v-else-if="isVideoMessage()" class="text2 video-quote">
        {{ personName }}：
        <div class="video-preview">
          <video
            controls
            preload="metadata"
            :src="getVideoUrl()"
            style="width: 200px; height: 200px"
          ></video>
        </div>
      </div>

      <!-- 图片引用 -->
      <div v-else-if="isImageMessage()" class="text2 img-quote">
        {{ personName }}：
        <div class="image-preview">
          <ElImage
            fit="cover"
            :preview-src-list="[getImageUrl()]"
            :src="getImageUrl()"
            style="width: 60px; height: 60px; border-radius: 4px"
          />
        </div>
      </div>

      <!-- 音频引用 -->
      <div v-else-if="isAudioMessage()" class="text2">
        {{ personName }}：
        <MsgVoiceHistory
          :audio-time="msg.duration || 0"
          class="text"
          :is-self="isSelf(msgDetail)"
          :self-id="msgDetail.msgId"
          :src="getAudioUrl()"
        />
      </div>

      <!-- 文件引用 -->
      <div v-else-if="isFileMessage()" class="text2">
        {{ personName }}：
        <MsgFile :card-data="msg" />
      </div>

      <!-- 转发消息引用 -->
      <div v-else-if="isForwardMessage()" class="text2">
        {{ personName }}：
        <MsgForward :card-data="msg" />
      </div>

      <!-- 群接龙引用 -->
      <div v-else-if="isGroupJoiningMessage()" class="text2">
        {{ personName }}：
        <div class="group-joining-preview">
          <span>#接龙 {{ getGroupJoiningContent()?.text || '未命名接龙' }}</span>
        </div>
      </div>

      <!-- 文本消息引用 -->
      <div v-else-if="isTextMessage()" class="text2">
        {{ personName }}：
        <EmojiView
          :is-list="false"
          style="display: inline-block"
          :text="msg?.text || msgDetail.msg"
        />
      </div>

      <!-- 未知类型消息 -->
      <div v-else class="text2">
        {{ personName }}：
        <span>[未知消息类型: {{ msgDetail.msgType }}]</span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="less">
  .msg-content {
    box-sizing: border-box;
    display: flex;
    width: 100%;
    padding: 8px 12px;
    cursor: pointer;
    background: rgb(173 204 240 / 40%);
    border-left: 3px solid #159aff;
    border-radius: 4px;
    transition: background-color 0.2s;

    &:hover {
      background: rgb(173 204 240 / 20%);
    }

    .msg-box {
      width: 100%;

      .text2 {
        display: flex;
        gap: 4px;
        align-items: flex-start;
        width: 100%;
        font-size: 12px;
        font-weight: 400;
        color: var(--text-color);
        text-align: left;
        background: none !important;

        span {
          color: var(--text-color);
        }
      }

      .img-quote {
        .image-preview {
          margin-left: 4px;
        }
      }

      .video-quote {
        .video-preview {
          position: relative;
          margin-left: 4px;

          :deep(.video-history) {
            width: 80px;
            height: 60px;
          }

          .show-icon {
            position: absolute;
            top: 40%;
            left: 35%;
          }
        }
      }

      .group-joining-preview {
        color: #159aff;
      }
    }
  }

  :deep(.emoji-view) {
    display: inline !important;
    font-size: 12px;
    font-weight: 500;
    color: var(--item-text-color);
  }
</style>
