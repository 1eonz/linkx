<script setup>
  import { onMounted, ref, watch } from 'vue';

  import { getChatHistoryCount, getCollocation, getUserInfo as getUserInfoApi } from '@/api/statics'; // 引入获取群成员的接口
  import defaultImg from '@/assets/images/pim/im_person.svg';
  import MsgCard from '@/pages/coordination/chatPanel/msgCard.vue';
  import MsgFile from '@/pages/coordination/chatPanel/msgFile.vue';
  import MsgForward from '@/pages/coordination/chatPanel/msgForward.vue';
  import EmojiView from '@/pages/coordination/emoji/emojiView.vue';
  import msgVoiceHistory from '@/pages/notification/components/msgVoiceHistory.vue';
  import { usePIMStore } from '@/store';
  import { getIp } from '@/utils';

  const props = defineProps({
    closeDialog: {
      default: () => {},
      type: Function,
    },
    groupId: {
      default: '',
      type: String,
    },
    groupMembers: {
      default: () => [],
      type: Array,
    },
    msgList: {
      default: () => [],
      type: Array,
    },
    title: {
      default: '聊天记录',
      type: String,
    },
  });

  const emit = defineEmits(['close']);

  const PIMStore = usePIMStore();
  const showDialog = ref(false);
  const showVideoDialog = ref(false);
  const imagePreviewUrl = ref('');
  const videoPreviewUrl = ref('');
  const groupMembers = ref([]);
  // 存储远程获取的用户信息
  const externalUserInfos = ref([]);

  // 获取群成员信息
  const fetchGroupMembers = async () => {
    if (!props.groupId) return;

    try {
      const res = await getChatHistoryCount({ groupId: props.groupId, pageSize: 100 });
      if (res.data) {
        groupMembers.value = res.data.records || [];
      }
    } catch (error) {
      console.error('获取群成员失败:', error);
      groupMembers.value = [];
    }
  };

  // 批量获取用户信息
  const fetchUserInfos = async (userIds) => {
    if (!userIds || userIds.length === 0) return;

    try {
      // 过滤掉已经获取过的用户ID
      const newUserIds = userIds.filter(
        (id) =>
          !externalUserInfos.value.some((user) => user.id === id) &&
          !props.groupMembers.some((member) => member.id === id) &&
          !groupMembers.value.some((member) => member.id === id),
      );

      if (newUserIds.length === 0) return;

      // 并发获取用户信息和协同岗信息
      const [userRes, collabRes] = await Promise.all([
        getUserInfoApi({ userIds: newUserIds }),
        getCollocation({ userIds: newUserIds }),
      ]);

      // 处理普通用户信息
      if (userRes?.data?.results && Array.isArray(userRes.data.results)) {
        userRes.data.results.forEach((user) => {
          if (user && user.id) {
            externalUserInfos.value.push({
              id: user.id,
              avatar: user.avatar || user.tumbAvatar || '',
              name: user.name || `用户${user.id}`,
            });
          }
        });
      }

      // 处理协同岗信息
      if (collabRes?.code === 0 && Array.isArray(collabRes.data)) {
        collabRes.data.forEach((item) => {
          if (item && item.id) {
            externalUserInfos.value.push({
              id: item.id,
              avatar: item.iconUrl || '',
              name: item.postName || `用户${item.id}`,
            });
          }
        });
      }
    } catch (error) {
      console.error('获取用户信息失败:', error);
    }
  };

  // 根据用户ID获取用户信息
  function getUserInfo(userId) {
    if (!userId) return { avatar: '', name: '未知用户' };

    // 优先从群成员中查找
    const groupMember = props.groupMembers.find((member) => member.id == userId);
    if (groupMember) {
      return {
        avatar: groupMember.tumbAvatar || groupMember.avatar || '',
        name: groupMember.name || `用户${userId}`,
        realName: groupMember?.realName,
      };
    }

    // 从内部群成员中查找
    const internalMember = groupMembers.value.find((member) => member.id == userId);
    if (internalMember) {
      return {
        avatar: internalMember.tumbAvatar || internalMember.avatar || '',
        name: internalMember.name || `用户${userId}`,
        realName: internalMember?.realName,
      };
    }

    // 从远程获取的用户信息中查找
    const externalUser = externalUserInfos.value.find((user) => user.id == userId);
    if (externalUser) {
      return {
        avatar: externalUser.avatar || '',
        name: externalUser.name || `用户${userId}`,
      };
    }

    // 备用：从userMap中查找
    const user = PIMStore.userMap.get(Number(userId));
    if (user) {
      return {
        avatar: user.avatar || user.tumbAvatar || '',
        name: user.name || `用户${userId}`,
      };
    }

    // 最后返回默认值
    return {
      avatar: '',
      name: `用户${userId}`,
    };
  }

  const realUserInfos = new Map();
  const processedMessages = ref([]);
  watch(
    () => props.msgList,
    async () => {
      processedMessages.value = [];
      
      // 先提取所有需要获取的用户ID
      const userIdsToFetch = new Set();
      props.msgList.forEach((item) => {
        const displayUserId = item.from || item.fromRealUserId;
        if (displayUserId) {
          // 检查是否已经在各个数据源中
          const inGroupMembers = props.groupMembers.some((member) => member.id === displayUserId);
          const inInternalMembers = groupMembers.value.some((member) => member.id === displayUserId);
          const inExternalUsers = externalUserInfos.value.some((user) => user.id === displayUserId);
          const inUserMap = PIMStore.userMap.get(Number(displayUserId));
          
          if (!inGroupMembers && !inInternalMembers && !inExternalUsers && !inUserMap) {
            userIdsToFetch.add(displayUserId);
          }
        }
      });
      
      // 先获取缺失的用户信息
      if (userIdsToFetch.size > 0) {
        await fetchUserInfos(Array.from(userIdsToFetch));
      }
      
      const promises = props.msgList.map(async (item) => {
        let name = '';
        // 协同岗消息：同时查协同岗名称和真实用户名，拼接为"协同岗名称(真实用户名)"
        if (item.fromRealUserId) {
          const collabInfo = getUserInfo(item.from);
          const realInfo = getUserInfo(item.fromRealUserId);
          // 优先使用已拼接好的realName（如"lm测试协同岗(lm)"）
          if (collabInfo.realName && collabInfo.realName !== collabInfo.name) {
            name = collabInfo.realName;
          } else {
            const collabName = collabInfo.name;
            const realName = realInfo.name;
            // 两个都有效且名称不同时拼接
            if (collabName && !collabName.startsWith('用户') && realName && !realName.startsWith('用户') && collabName !== realName) {
              name = `${collabName}(${realName})`;
            } else if (collabName && !collabName.startsWith('用户')) {
              name = collabName;
            } else if (realName && !realName.startsWith('用户')) {
              name = realName;
            } else {
              name = collabName || realName || `用户${item.from}`;
            }
          }
          // 补充查询：如果名称是默认值，尝试从realUserInfos或API获取
          if (name.startsWith('用户')) {
            if (realUserInfos.has(item.fromRealUserId)) {
              const cachedRealInfo = realUserInfos.get(item.fromRealUserId);
              if (cachedRealInfo && cachedRealInfo.name) {
                const collabNameFromMember = collabInfo?.realName || collabInfo.name;
                name = `${collabNameFromMember}(${cachedRealInfo.name})`;
              }
            } else {
              try {
                const userInfos = await getUserInfoApi({ userIds: [item.fromRealUserId] });
                if (userInfos?.data?.results && Array.isArray(userInfos.data?.results)) {
                  const userInfo2 = userInfos?.data?.results.find((m) => m.id === item.fromRealUserId);
                  realUserInfos.set(item.fromRealUserId, userInfo2);
                  if (userInfo2 && userInfo2.name) {
                    const collabNameFromMember = collabInfo?.realName || collabInfo.name;
                    name = `${collabNameFromMember}(${userInfo2.name})`;
                  }
                }
              } catch (e) {
                // 获取真实用户信息失败
              }
            }
          }
        } else {
          const displayUserId = item.from || item.fromRealUserId;
          const userInfo = getUserInfo(displayUserId);
          name = userInfo?.realName || userInfo.name;
        }
        // 获取头像信息
        const avatarInfo = getUserInfo(item.from);
        // 处理消息内容
        let msgContent = {};
        if (item.msg) {
          msgContent = typeof item.msg === 'string' ? tryParseJson(item.msg) : item.msg;
        }
        return {
          ...item,
          avatar: avatarInfo.avatar,
          duration: item.duration || msgContent.duration,
          fileKey: item.fileKey || msgContent.fileKey,
          fileName: item.fileName || msgContent.fileName,
          filePath: item.filePath || msgContent.filePath,
          fileType: item.fileType || msgContent.fileType,
          msg: msgContent,
          name,
          // 确保必要的字段存在
          text: item.text || msgContent.text,
          videoThumb: item.videoThumb || msgContent.videoThumb,
        };
      });

      const processedItems = await Promise.all(promises);
      // 按照time字段从小到大排序
      processedItems.sort((a, b) => {
        const timeA = Number.parseInt(a.time || '0');
        const timeB = Number.parseInt(b.time || '0');
        return timeA - timeB;
      });

      processedMessages.value = processedItems;
    },
    {
      immediate: true,
    },
  );

  // 尝试解析JSON
  function tryParseJson(str) {
    try {
      return JSON.parse(str);
    } catch {
      return { text: str };
    }
  }

  // 解析引用消息
  function parseQuoteMessage(message) {
    try {
      if (
        message.msgType === 1 &&
        (message.msg.startsWith('{text=') || message.msg.startsWith('{srcMsgId='))
      ) {
        const msgData = {};
        const cleanMsg = message.msg.replaceAll(/{|}/g, '');

        // 处理包含逗号的文本内容
        const pairs = [];
        let currentPair = '';
        let inText = false;

        for (let i = 0; i < cleanMsg.length; i++) {
          const char = cleanMsg[i];

          if (char === ',' && !inText) {
            pairs.push(currentPair);
            currentPair = '';
          } else {
            currentPair += char;
          }

          // 检测是否进入text字段
          if (currentPair.endsWith('text=')) {
            inText = true;
          }

          // 如果在text字段中，只有到达字符串末尾才结束
          if (inText && i === cleanMsg.length - 1) {
            pairs.push(currentPair);
          }
        }

        // 如果循环结束后还有剩余内容，添加到pairs中
        if (currentPair && !pairs.includes(currentPair)) {
          pairs.push(currentPair);
        }

        pairs.forEach((pair) => {
          // 特殊处理text字段，它可能包含逗号
          if (pair.startsWith('text=')) {
            const key = 'text';
            const value = pair.substring(5); // 跳过"text="部分
            msgData[key] = value === 'null' ? null : value;
          } else {
            const equalIndex = pair.indexOf('=');
            if (equalIndex !== -1) {
              const key = pair.substring(0, equalIndex);
              const value = pair.substring(equalIndex + 1);
              msgData[key.trim()] = value === 'null' ? null : value;
            }
          }
        });
        return msgData;
      }
      return { text: message.msg };
    } catch {
      return { text: message.msg };
    }
  }

  // 解析群接龙消息
  function parseGroupJoiningMessage(message) {
    if (message.msgType === 6) {
      try {
        const result = JSON.parse(message.userTxt);
        return result;
      } catch {
        return message.userTxt;
      }
    }
    return message.userTxt;
  }

  // 获取被引用的消息
  function getQuotedMessage(srcMsgId) {
    if (!srcMsgId) return null;
    return props.msgList.find((msg) => msg.msgId === srcMsgId);
  }

  // 获取引用消息的发送者信息
  function getQuoteSenderInfo(quotedMsg) {
    if (!quotedMsg) return '未知用户';
    return getUserInfo(quotedMsg.from).name;
  }

  // 获取群接龙内容
  function getGroupJoiningContent(message) {
    const content = getMessageContent(message).content;

    // 处理参与者数据
    let participants = [];
    if (content.userTxts && Array.isArray(content.userTxts)) {
      // 按时间戳排序，时间最早的排在最前面
      const sortedUserTxts = [...content.userTxts].sort((a, b) => {
        const timeA = a.timeStamp || a.clientTimeStamp || 0;
        const timeB = b.timeStamp || b.clientTimeStamp || 0;
        return timeA - timeB;
      });

      participants = sortedUserTxts.map((userTxt, index) => {
        const memberInfo = props.groupMembers.find((member) => member.id === userTxt.userId);
        const userInfo = getUserInfo(userTxt.userId);

        let avatar = userInfo.avatar;
        if (memberInfo) {
          const postName = memberInfo.postName || '';
          const realName = memberInfo.realName || '';
          let txt = (userTxt.txt || '').split(' ')[0];
          try {
            txt = txt.split(' ')[0];
          } catch {
            txt = userTxt.txt || '';
          }

          const isCollaborationIdentity =
            (postName && txt === postName) ||
            (postName && realName && txt === `${postName}(${realName})`) ||
            (realName && !txt.startsWith(realName) && txt.includes(`(${realName})`));

          const userAvatar = memberInfo.tumbAvatar || memberInfo.avatar || '';
          avatar = isCollaborationIdentity
            ? (memberInfo.collaborationAvatar || userAvatar)
            : userAvatar;
        }

        return {
          avatar,
          isCreator: index === 0,
          name: userTxt.txt || (memberInfo ? memberInfo.name : userInfo.name),
          timeStamp: userTxt.timeStamp || userTxt.clientTimeStamp,
          userId: userTxt.userId,
        };
      });
    }

    // 计算去重后的参与人数：协同岗账号和其对应的真实用户算作同一人
    const getRealUserIdForDedup = (userId) => {
      const member = props.groupMembers.find((m) => m.id == userId);
      if (!member || !member.postName) return userId.toString();
      if (member.name === member.postName) {
        const realUser = props.groupMembers.find(
          (m) => m.postName === member.postName && m.name !== member.postName,
        );
        return realUser ? realUser.id.toString() : userId.toString();
      }
      return userId.toString();
    };

    // 按真实用户去重计算人数，但参与者列表保留全部条目
    const uniqueRealUserIds = new Set();
    participants.forEach((p) => {
      uniqueRealUserIds.add(getRealUserIdForDedup(p.userId));
    });

    // 获取发起人名称
    let creatorName = '未知用户';
    if (participants.length > 0) {
      creatorName = participants[0].name;
    } else if (content.creator) {
      const creatorInfo = props.groupMembers.find((member) => member.id === content.creator);
      creatorName = creatorInfo ? creatorInfo.name : getUserInfo(content.creator).name;
    }

    return {
      creatorName,
      participantCount: uniqueRealUserIds.size,
      participants,
      text: content.text || '未命名接龙',
    };
  }

  // 获取消息显示内容
  function getMessageContent(message) {
    // 检查是否是引用消息
    const parsedMsg = parseQuoteMessage(message);
    if (parsedMsg.srcMsgId) {
      const quotedMsg = getQuotedMessage(parsedMsg.srcMsgId);
      return {
        content: parsedMsg.text || message.msg,
        quotedMsg,
        srcMsgId: parsedMsg.srcMsgId,
        type: 'quote',
      };
    }

    switch (message.msgType) {
      case 1: {
        // 文本消息
        return {
          content: message.text || message.msg?.text || '',
          type: 'text',
        };
      }

      case 2: {
        // 文件消息
        return {
          content: {
            duration: message.duration,
            fileKey: message.fileKey,
            fileName: message.fileName,
            filePath: message.filePath,
            fileSize: message.fileSize,
            fileType: message.fileType,
            videoThumb: message.videoThumb,
          },
          type: 'file',
        };
      }

      case 5: {
        // 个人名片
        return {
          content: message,
          type: 'userCard',
        };
      }

      case 6: {
        // 群接龙
        return {
          content: parseGroupJoiningMessage(message),
          type: 'groupJoining',
        };
      }

      case 7: {
        // 撤回消息
        return {
          content: '[消息已撤回]',
          type: 'system',
        };
      }

      case 8: {
        // 转发消息
        return {
          content: message.msg || message,
          type: 'forward',
        };
      }

      case 10: {
        // 群组事件
        return {
          content: '[群组事件]',
          type: 'groupEvent',
        };
      }

      default: {
        return {
          content: `[未知消息类型: ${message.msgType}]`,
          type: 'unknown',
        };
      }
    }
  }

  // 获取引用消息的显示内容
  function getQuoteContent(quotedMsg) {
    if (!quotedMsg) return '[引用消息不存在]';

    const content = getMessageContent(quotedMsg);
    switch (content.type) {
      case 'file': {
        if (content.content.fileType === 1) return '[图片]';
        if (content.content.fileType === 2) return '[语音]';
        if (content.content.fileType === 3) return '[视频]';
        return '[文件]';
      }
      case 'forward': {
        return '[转发消息]';
      }
      case 'groupJoining': {
        return '[群接龙]';
      }
      case 'text': {
        return content.content;
      }
      case 'userCard': {
        return '[个人名片]';
      }
      default: {
        return '[引用消息]';
      }
    }
  }

  // 处理@消息显示
  function formatTextMessage(text) {
    if (!text) return '';
    return text.replaceAll(/\[@(\d+):@([^\]]+)\]/g, (match, userId, userName) => {
      if (userName === 'all') {
        return '@所有人 ';
      }
      return `@${userName} `;
    });
  }

  // 格式化日期
  function formatDate(timestamp) {
    if (!timestamp) return '';
    return new Date(Number.parseInt(timestamp)).toLocaleString();
  }

  function lookImage(url) {
    imagePreviewUrl.value = url;
    showDialog.value = true;
  }

  function lookVideo(url) {
    videoPreviewUrl.value = url;
    showVideoDialog.value = true;
  }

  // 处理关闭
  function handleClose() {
    props.closeDialog?.();
    emit('close');
  }

  // 监听群成员变化，重新处理消息
  watch(
    groupMembers,
    () => {
      // console.log('群成员信息已更新，重新处理消息')
    },
    { deep: true },
  );

  onMounted(() => {
    if (props.groupId) {
      fetchGroupMembers();
    }
  });
</script>

<template>
  <div class="forward-message-container">
    <!-- 自定义标题栏和关闭按钮 -->
    <div class="dialog-header">
      <div class="header-title">{{ title }}</div>
      <div class="header-close" @click="handleClose">
        <el-icon><Close /></el-icon>
      </div>
    </div>

    <div class="forward-messages">
      <template v-for="(message, index) in processedMessages" :key="index">
        <!-- 消息项 - 全部显示在左侧 -->
        <div class="message-item">
          <div class="message-left">
            <div class="avatar">
              <img
                v-if="message.avatar"
                alt=""
                class="user-avatar"
                :src="`${getIp()}/linkx/desktop${message.avatar}`"
              />
              <el-icon v-else class="item-icon">
                <Avatar :src="message.avatar" />
              </el-icon>
            </div>
            <div class="message-content">
              <div style="display: flex; justify-content: flex-start">
                <div class="sender-name">{{ message.name }}</div>
                <div class="message-time">
                  {{ message.time ? formatDate(parseInt(message.time)) : '' }}
                </div>
              </div>
              <div style="display: flex; justify-content: flex-start">
                <div class="message-bubble">
                  <!-- 引用消息 -->
                  <div v-if="getMessageContent(message).type === 'quote'" class="quote-message">
                    <div class="quote-text">
                      <EmojiView
                        :is-list="false"
                        style="color: var(--text-color)"
                        :text="formatTextMessage(getMessageContent(message).content)"
                      />
                    </div>
                    <div class="quote-content">
                      <div class="quote-sender">
                        {{ getQuoteSenderInfo(getMessageContent(message).quotedMsg) }}
                      </div>
                      <div class="quote-preview">
                        {{ getQuoteContent(getMessageContent(message).quotedMsg) }}
                      </div>
                    </div>
                  </div>

                  <!-- 文本消息 -->
                  <div
                    v-else-if="getMessageContent(message).type === 'text'"
                    class="text-message text-message-left"
                  >
                    <EmojiView
                      :is-list="false"
                      :text="formatTextMessage(getMessageContent(message).content)"
                    />
                  </div>

                  <!-- 文件消息 -->
                  <div v-else-if="getMessageContent(message).type === 'file'" class="file-message">
                    <!-- 图片 -->
                    <img
                      v-if="getMessageContent(message).content.fileType === 1"
                      alt=""
                      :src="`${getIp()}/linkx/desktop${getMessageContent(message).content.filePath}`"
                      style="
                        max-width: 300px;
                        max-height: 200px;
                        cursor: pointer;
                        border-radius: 4px;
                      "
                      @click="
                        lookImage(
                          `${getIp()}/linkx/desktop${getMessageContent(message).content.filePath}`,
                        )
                      "
                    />

                    <!-- 视频 -->
                    <div
                      v-else-if="getMessageContent(message).content.fileType === 3"
                      class="video-container"
                      style="position: relative; display: inline-block; cursor: pointer"
                      @click="
                        lookVideo(
                          `${getIp()}/linkx/desktop${getMessageContent(message).content.filePath}`,
                        )
                      "
                    >
                      <video
                        playsinline
                        :src="`${getIp()}/linkx/desktop${getMessageContent(message).content.filePath}`"
                        style="max-width: 300px; max-height: 200px; border-radius: 4px"
                      ></video>
                      <div
                        class="play-button"
                        style="
                          position: absolute;
                          top: 50%;
                          left: 50%;
                          transform: translate(-50%, -50%);
                        "
                      >
                        <svg height="48" viewBox="0 0 64 64" width="48">
                          <circle cx="32" cy="32" fill="rgba(0,0,0,0.5)" r="32" />
                          <polygon fill="white" points="26,22 26,42 42,32" />
                        </svg>
                      </div>
                    </div>

                    <!-- 文件 -->
                    <MsgFile
                      v-if="
                        [4, 5, 6, 7, 8, 9, 10, 11].includes(
                          getMessageContent(message).content.fileType,
                        )
                      "
                      :card-data="getMessageContent(message).content"
                    />

                    <!-- 语音 -->
                    <msgVoiceHistory
                      v-if="getMessageContent(message).content.fileType === 2"
                      :audio-time="getMessageContent(message).content.duration || 0"
                      :is-self="false"
                      :self-id="message.msgId || index"
                      :src="`${getIp()}/linkx/desktop${getMessageContent(message).content.filePath}`"
                    />
                  </div>

                  <!-- 转发消息 -->
                  <div
                    v-else-if="getMessageContent(message).type === 'forward'"
                    class="forward-message"
                  >
                    <MsgForward
                      :card-data="getMessageContent(message).content"
                      :group-id="groupId"
                      :member-list="props.groupMembers"
                    />
                  </div>

                  <!-- 个人名片 -->
                  <div
                    v-else-if="getMessageContent(message).type === 'userCard'"
                    class="userCard-message"
                  >
                    <MsgCard :card-data="getMessageContent(message).content" />
                  </div>

                  <!-- 群接龙 -->
                  <div
                    v-else-if="getMessageContent(message).type === 'groupJoining'"
                    class="group-joining-message"
                  >
                    <div class="group-joining-content">
                      <!-- 接龙标题 -->
                      <div class="joining-header">#接龙</div>

                      <!-- 接龙内容 -->
                      <div class="joining-text">{{ getGroupJoiningContent(message).text }}</div>

                      <!-- 发起人和参与人数 -->
                      <div class="joining-info">
                        {{ getGroupJoiningContent(message).creatorName }}发起，参与共{{
                          getGroupJoiningContent(message).participantCount
                        }}人
                      </div>

                      <!-- 参与人员列表 -->
                      <div class="joining-participants">
                        <div
                          v-for="(participant, idx) in getGroupJoiningContent(message).participants"
                          :key="participant.userId"
                          class="participant-item"
                        >
                          <span class="participant-index">{{ idx + 1 }}</span>
                          <img
                            v-if="participant.avatar"
                            alt=""
                            class="participant-avatar"
                            :src="`${getIp()}/linkx/desktop${participant.avatar}`"
                          />
                          <img v-else alt="" class="participant-avatar" :src="defaultImg" />
                          <span class="participant-name">{{ participant.name }}</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- 群组事件 -->
                  <div
                    v-else-if="getMessageContent(message).type === 'groupEvent'"
                    class="system-message"
                  >
                    <!-- [群组事件消息] -->
                  </div>

                  <!-- 系统消息 -->
                  <div v-else class="text-message">
                    <!-- {{ getMessageContent(message).content }} -->
                    <EmojiView :is-list="false" :text="getMessageContent(message).content" />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- 图片预览弹框 -->
    <el-dialog v-model="showDialog" title="预览" width="1000px">
      <div
        style="
          display: flex;
          align-items: center;
          justify-content: center;
          width: 100%;
          height: 600px;
        "
      >
        <img alt="预览图片" :src="imagePreviewUrl" style="max-width: 100%; max-height: 600px" />
      </div>
    </el-dialog>

    <!-- 视频预览弹框 -->
    <el-dialog v-model="showVideoDialog" title="预览" width="1000px">
      <div
        style="
          display: flex;
          align-items: center;
          justify-content: center;
          width: 1000px;
          height: 600px;
        "
      >
        <video
          controls
          playsinline
          :src="videoPreviewUrl"
          style="max-width: 95%; max-height: 600px"
        ></video>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped lang="less">
  .forward-message-container {
    display: flex;
    flex-direction: column;
    width: 800px;
    height: 700px;
    padding-bottom: 100px;
    overflow: hidden;
    background: var(--message-list-bg);
    border-radius: 8px;
  }

  /* 对话框标题栏样式 */
  .dialog-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 20px;
    background: var(--td-bg-color-container);
    border-bottom: 1px solid var(--divider-border-color);

    .header-title {
      font-size: 16px;
      font-weight: 600;
      color: var(--text-color);
    }

    .header-close {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      font-size: 18px;
      color: var(--text-color);
      cursor: pointer;
      border-radius: 4px;
      transition: all 0.2s;

      &:hover {
        color: var(--tabs-active-color);
        background: var(--td-bg-color-container-hover);
      }
    }
  }

  .forward-messages {
    flex: 1;
    padding: 16px;
    overflow-y: auto;

    .time-divider {
      display: inline-block;
      padding: 4px 12px;
      margin: 16px 0;
      margin-left: 50%;
      font-size: 12px;
      color: var(--message-text-color);
      text-align: center;
      background: var(--td-bg-color-container);
      border-radius: 12px;
      transform: translateX(-50%);
    }

    .message-item {
      display: flex;
      margin-bottom: 16px;
    }

    .message-left {
      display: flex;
      align-items: flex-start;
      width: 100%;
      max-width: 100%;
    }

    .avatar {
      flex-shrink: 0;
      width: 36px;
      height: 36px;
      padding: 2px;
      overflow: hidden;
      border: 1px solid #2e94e5;
      border-radius: 50%;

      .user-avatar {
        width: 100%;
        height: 100%;
        object-fit: cover;
        border-radius: 50%;
      }

      .item-icon {
        width: 36px;
        height: 36px;

        svg {
          width: 36px;
          height: 36px;
          color: #2e94e5;
          border-radius: 50%;
        }
      }
    }

    .message-content {
      width: 100%;
      max-width: calc(100% - 60px);
      margin: 0 12px;
    }

    .sender-name {
      margin-bottom: 4px;
      margin-left: 8px;
      font-size: 12px;
      font-weight: 500;
      color: var(--message-text-color);
    }

    .message-time {
      margin-bottom: 8px;
      margin-left: 8px;
      font-size: 11px;
      color: var(--message-text-color);
      opacity: 0.7;
    }

    .message-bubble {
      position: relative;
      max-width: 600px;
      padding: 8px 12px;
      word-wrap: break-word;
      background: var(--message-info-left-bg);
      border-radius: 8px;
      box-shadow: 0 1px 3px rgb(0 0 0 / 10%);
    }

    .text-message {
      font-size: 14px;
      line-height: 1.4;
      word-break: break-word;
      white-space: pre-wrap;
    }

    .text-message-left {
      :deep(.emoji-view) {
        color: #333;
      }
    }

    .system-message {
      font-size: 12px;
      font-style: italic;
      color: var(--message-text-color);
      opacity: 0.8;
    }

    .file-message,
    .forward-message,
    .card-message {
      max-width: 500px;
    }

    /* 引用消息样式 */
    .quote-message {
      .quote-text {
        padding-bottom: 4px;
        margin-bottom: 8px;
        border-bottom: 1px solid rgb(255 255 255 / 20%);
      }

      .quote-content {
        padding: 8px;
        background: rgb(255 255 255 / 10%);
        border-left: 3px solid #2e94e5;
        border-radius: 4px;

        .quote-sender {
          margin-bottom: 4px;
          font-size: 12px;
          font-weight: 500;
          color: #2e94e5;
        }

        .quote-preview {
          font-size: 12px;
          color: var(--text-color);
          word-break: break-word;
          opacity: 0.8;
        }
      }
    }

    /* 群接龙样式 */
    .group-joining-message {
      .group-joining-content {
        width: 240px;
        padding: 12px;
        background: var(--message-info-left-bg);
        border-radius: 8px;

        .joining-header {
          margin-bottom: 8px;
          font-size: 14px;
          font-weight: 600;
          color: #1b61f0;
        }

        .joining-text {
          margin-bottom: 8px;
          font-size: 14px;
          font-weight: 500;
          color: var(--text-color);
          word-wrap: break-word;
        }

        .joining-info {
          margin-bottom: 12px;
          font-size: 12px;
          color: var(--text-color);
        }

        .joining-participants {
          .participant-item {
            display: flex;
            align-items: center;
            margin-bottom: 8px;

            &:last-child {
              margin-bottom: 0;
            }

            .participant-index {
              min-width: 20px;
              margin-right: 8px;
              font-size: 12px;
              color: var(--text-color);
            }

            .participant-avatar {
              width: 24px;
              height: 24px;
              margin-right: 8px;
              border-radius: 50%;
            }

            .participant-name {
              font-size: 12px;
              color: var(--text-color);
            }
          }
        }
      }
    }

    .video-container {
      position: relative;
      display: inline-block;
      cursor: pointer;
    }

    .play-button {
      position: absolute;
      top: 50%;
      left: 50%;
      z-index: 10;
      cursor: pointer;
      transition: opacity 0.3s;
      transform: translate(-50%, -50%);
    }

    .video-container:hover .play-button {
      opacity: 0.8;
    }
  }

  .forward-messages::-webkit-scrollbar {
    width: 6px;
  }

  .forward-messages::-webkit-scrollbar-track {
    background: var(--td-input-inner-bg);
    border-radius: 3px;
  }

  .forward-messages::-webkit-scrollbar-thumb {
    background: var(--light-text-color);
    border-radius: 3px;
  }

  .forward-messages::-webkit-scrollbar-thumb:hover {
    background: var(--tabs-active-color);
  }

  /* 确保弹框样式正确 */
  :deep(.el-dialog__title) {
    color: var(--text-color) !important;
  }

  :deep(.el-dialog__headerbtn .el-dialog__close) {
    color: var(--text-color) !important;
  }
</style>
