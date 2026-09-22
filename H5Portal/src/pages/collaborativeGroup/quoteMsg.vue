<template>
  <div class="quote-message-container" @click="handleQuoteClick">
    <!-- 引用消息区域 -->
    <div class="quote-preview">
      <div class="quote-header">
        <text class="quote-sender">{{ quoteSenderName }}</text>
        <text class="quote-separator">：</text>
      </div>

      <!-- 文本消息 -->
      <div v-if="isTextMessage" class="quote-content text-quote">
        <EmojiDisplay :text="quoteContent" />
      </div>

      <!-- 图片消息 -->
      <div v-else-if="isImageMessage" class="quote-content image-quote">
        <van-image :src="getImageUrl()" class="quote-image" fit="cover" @click="previewImage" />
        <text class="quote-text-preview">[图片]</text>
      </div>

      <!-- 视频消息 -->
      <div v-else-if="isVideoMessage" class="quote-content video-quote">
        <view class="video-placeholder">
          <text class="video-icon">▶</text>
        </view>
        <text class="quote-text-preview">[视频]</text>
      </div>

      <!-- 文件消息 -->
      <div v-else-if="isFileMessage" class="quote-content file-quote">
        <text class="file-icon">📎</text>
        <text class="file-name">{{ quoteFileName }}</text>
      </div>

      <!-- 语音消息 -->
      <div v-else-if="isAudioMessage" class="quote-content audio-quote">
        <!-- <text class="audio-icon">🎵</text> -->
        <view class="voice-animation">
          <view class="wave-bar" v-for="n in 3" :key="n"> </view>
        </view>
        <text class="audio-duration">{{ quoteDuration }}″</text>
      </div>

      <!-- 转发消息 -->
      <div v-else-if="isForwardMessage" class="quote-content forward-quote">
        <text class="forward-icon">💬</text>
        <text class="forward-text">[合并转发]{{ quoteTitle }}</text>
      </div>

      <!-- 个人名片：点击打开详情弹窗 -->
      <div
        v-else-if="isUserCardMessage"
        class="quote-content card-quote"
        @click.stop="openMemberDetails"
      >
        <text class="card-text">[个人名片]{{ quoteUserName }}</text>
      </div>

      <!-- 群接龙 -->
      <div v-else-if="isGroupJoiningMessage" class="quote-content joining-quote">
        <text class="joining-icon">📝</text>
        <text class="joining-text">#接龙 {{ quoteJoiningText }}</text>
      </div>

      <!-- 未知类型 -->
      <div v-else class="quote-content unknown-quote">
        <text class="unknown-text">[未知消息]</text>
      </div>
    </div>

    <!-- 个人名片详情弹窗（挂在容器外，避免 v-else-if 链中无可见 DOM） -->
    <MemberDetailsPopup
      v-if="isUserCardMessage && quoteUserId"
      ref="memberDetailsPopupRef"
      :user-id="quoteUserId"
    />
  </div>
</template>

<script setup>
  import { computed, ref, watch, onMounted } from 'vue';

  import MemberDetailsPopup from './memberDetailsPopup/index.vue';

  import EmojiDisplay from '@/pages/collaborativeGroup/emojiDisplay.vue';
  import { transformImageUrl } from '@/utils/imgUrlParse.js';

  // 个人名片详情弹窗：与 msgCard.vue 保持一致

  const props = defineProps({
    // 被引用的消息
    quoteMsg: {
      type: Object,
      default: () => ({}),
    },
    // 引用消息ID（如果quoteMsg为空，则通过ID查找）
    quoteMsgId: {
      type: String,
      default: '',
    },
    // 群成员列表（用于获取用户名称）
    memberList: {
      type: Array,
      default: () => [],
    },
    // 回复的文本内容
    replyText: {
      type: String,
      default: '',
    },
  });
  onMounted(() => {
    console.log('props.quoteMsg', props.quoteMsg);
  });
  const emit = defineEmits(['quoteClick']);

  // 个人名片详情弹窗 ref
  const memberDetailsPopupRef = ref(null);

  // 解析消息内容
  const parseMessageContent = (message) => {
    if (!message) return {};

    try {
      // 文本消息
      if (message.msgType === 1) {
        if (message.msg.startsWith('{') && message.msg.endsWith('}')) {
          // 解析带有{text=...}格式的消息
          const msgData = {};
          const cleanMsg = message.msg.replace(/{|}/g, '');
          const pairs = cleanMsg.split(',');
          pairs.forEach((pair) => {
            const [key, value] = pair.split('=');
            if (key && value) {
              msgData[key.trim()] = value.trim();
            }
          });
          // 如果有text字段则返回它，否则返回原始msg
          return {
            text: msgData.text !== undefined ? msgData.text : message.msg,
          };
        }
        // 普通文本消息
        return {
          text: message.msg,
        };
      }
      // 文件消息
      else if (message.msgType === 2) {
        try {
          return JSON.parse(message.msg);
        } catch (e) {
          const msgData = {};
          const cleanMsg = message.msg.replace(/{|}/g, '');
          const pairs = cleanMsg.split(',');
          pairs.forEach((pair) => {
            const [key, value] = pair.split('=');
            if (key && value) {
              const trimmedValue = value.trim();
              msgData[key.trim()] = trimmedValue === 'null' ? null : trimmedValue;
            }
          });
          return msgData;
        }
      }
      // 其他消息类型
      else if ([5, 6, 8].includes(message.msgType)) {
        try {
          return JSON.parse(message.msg);
        } catch (e) {
          return message.msg;
        }
      }

      return {
        text: message.msg,
      };
    } catch (error) {
      console.error('解析消息失败:', error);
      return {
        text: message.msg,
      };
    }
  };

  // 获取被引用的消息详情
  const quoteDetail = computed(() => {
    if (props.quoteMsg?.msgId) {
      return {
        ...props.quoteMsg,
        msg: parseMessageContent(props.quoteMsg),
      };
    }
    return {
      msg: {},
    };
  });

  const quoteMsg = computed(() => quoteDetail.value.msg || {});

  // 获取发送者名称，支持协同岗拼接真实用户名
  const quoteSenderName = computed(() => {
    const senderId = quoteDetail.value.from;
    const fromRealUserId = quoteDetail.value.fromRealUserId;
    if (!senderId) return '未知用户';

    // 从群成员中查找
    const member = props.memberList.find((m) => m.id == senderId);
    if (member) {
      // 协同岗消息：优先使用已拼接好的realName，否则手动拼接协同岗名称和真实用户名
      if (fromRealUserId) {
        if (member?.realName && member.realName !== member?.name) {
          return member.realName;
        }
        const collabName = member?.name || `用户${senderId}`;
        const realMember = props.memberList.find((m) => m.id == fromRealUserId);
        const realName = realMember?.name || realMember?.realName;
        if (realName) {
          return `${collabName}(${realName})`;
        }
        return collabName;
      }
      return member?.name || member?.realName || `用户${senderId}`;
    }

    return `用户${senderId}`;
  });

  // 消息类型判断
  const isTextMessage = computed(() => quoteDetail.value.msgType === 1);
  const isImageMessage = computed(
    () => quoteDetail.value.msgType === 2 && quoteMsg.value.fileType === 1,
  );
  const isVideoMessage = computed(
    () => quoteDetail.value.msgType === 2 && quoteMsg.value.fileType === 3,
  );
  const isAudioMessage = computed(
    () => quoteDetail.value.msgType === 2 && quoteMsg.value.fileType === 2,
  );
  const isFileMessage = computed(
    () =>
      quoteDetail.value.msgType === 2 &&
      quoteMsg.value.fileType &&
      [4, 5, 6, 7, 8, 9, 10].includes(quoteMsg.value.fileType),
  );
  const isUserCardMessage = computed(() => quoteDetail.value.msgType === 5);
  const isForwardMessage = computed(
    () => quoteDetail.value.msgType === 8 || quoteMsg.value.forwardMsgs,
  );
  const isGroupJoiningMessage = computed(() => quoteDetail.value.msgType === 6);

  // 获取各种消息的内容
  const quoteContent = computed(() => quoteMsg.value.text || '');
  const quoteFileName = computed(() => quoteMsg.value.fileName || '未知文件');
  const quoteDuration = computed(() => quoteMsg.value.duration || 0);
  const quoteTitle = computed(() => quoteMsg.value.title || '');
  const quoteUserName = computed(() => quoteMsg.value.data?.userName || quoteMsg.value.userCardData?.userName || '未知用户');
  // 被引用名片的 userId：兼容 userCardData.userId 与 data.userId 两种数据结构
  const quoteUserId = computed(() => quoteMsg.value.userCardData?.userId || quoteMsg.value.data?.userId || '');
  const quoteJoiningText = computed(() => quoteMsg.value.text || '未命名接龙');

  // 获取图片URL
  const getImageUrl = () => {
    if (quoteMsg.value.filePath) {
      return transformImageUrl(`/admin-api${quoteMsg.value.filePath}`);
    }
    return '';
  };

  // 处理点击事件
  const handleQuoteClick = () => {
    emit('quoteClick', quoteDetail.value);
  };

  // 打开个人名片详情弹窗
  const openMemberDetails = () => {
    memberDetailsPopupRef.value?.open();
  };
</script>

<style lang="scss" scoped>
  .quote-message-container {
    background: #f8f9fa;
    border-radius: 4px;
    padding: 8px;
    margin: 4px 0;
    border-left: 2px solid #159aff;
    cursor: pointer;

    .quote-preview {
      display: flex;
      align-items: flex-start;

      .quote-header {
        display: flex;
        align-items: center;
        flex-shrink: 0;

        .quote-sender {
          font-size: 10px;
          color: #159aff;
          font-weight: 500;
        }

        .quote-separator {
          font-size: 10px;
          color: #666;
        }
      }

      .quote-content {
        display: flex;
        align-items: center;
        margin-left: 4px;
        flex: 1;
        min-width: 0;

        &.text-quote {
          color: #666;
          font-size: 10px;
        }

        &.image-quote {
          .quote-image {
            width: 20px;
            height: 20px;
            border-radius: 2px;
            margin-right: 4px;
          }

          .quote-text-preview {
            color: #159aff;
            font-size: 10px;
          }
        }

        &.video-quote {
          .video-placeholder {
            width: 20px;
            height: 20px;
            background: #e0e0e0;
            border-radius: 2px;
            display: flex;
            align-items: center;
            justify-content: center;
            margin-right: 4px;

            .video-icon {
              color: #666;
              font-size: 10px;
            }
          }

          .quote-text-preview {
            color: #159aff;
            font-size: 10px;
          }
        }

        &.file-quote {
          .file-icon {
            margin-right: 4px;
            font-size: 10px;
          }

          .file-name {
            color: #666;
            font-size: 10px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }

        &.audio-quote {
          .audio-icon {
            margin-right: 4px;
            font-size: 10px;
          }

          .audio-duration {
            color: #666;
            font-size: 10px;
          }
          .voice-animation {
            display: flex;
            align-items: center;
            gap: 2px;
            margin-right: 5px;

            .wave-bar {
              width: 2px;
              height: 5px;
              background: #999;
              border-radius: 1px;

              &:nth-child(1) {
                height: 6px;
              }
              &:nth-child(2) {
                height: 8px;
              }
              &:nth-child(3) {
                height: 10px;
              }
            }
          }
        }

        &.forward-quote {
          .forward-icon {
            margin-right: 4px;
            font-size: 10px;
          }

          .forward-text {
            color: #666;
            font-size: 10px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }

        &.card-quote {
          .card-icon {
            margin-right: 4px;
            font-size: 10px;
          }

          .card-text {
            color: #666;
            font-size: 10px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }

        &.joining-quote {
          .joining-icon {
            margin-right: 4px;
            font-size: 10px;
          }

          .joining-text {
            color: #666;
            font-size: 10px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }

        &.unknown-quote {
          .unknown-text {
            color: #999;
            font-size: 10px;
            font-style: italic;
          }
        }
      }
    }

    .reply-content {
      color: #333;
      font-size: 14px;
      line-height: 1.4;
      word-break: break-word;
    }
  }

  // 响应式设计
  @media (max-width: 768px) {
    .quote-message-container {
      padding: 6px;

      .quote-preview {
        .quote-content {
          font-size: 11px;
        }
      }

      .reply-content {
        font-size: 13px;
      }
    }
  }
</style>
