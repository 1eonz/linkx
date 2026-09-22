<!-- msgForwardCard.vue -->
<template>
  <view class="msg-forward-card" @click="showForwardDetail">
    <view class="card-header">
      <text class="forward-title">{{ cardData?.title || '聊天记录' }}</text>
    </view>
    <view class="card-body">
      <view v-for="(item, index) in displayMessages" :key="index" class="forward-item">
        <text class="sender-name">{{ getUserName(item) }}：</text>
        <view class="message-preview">
          <!-- 文本消息显示具体内容和表情 -->
          <view v-if="item.msgType === 1" class="text-message-preview">
            <EmojiDisplay :text="getTextContent(item)" />
          </view>

          <!-- 文件消息 -->
          <text v-else-if="item.msgType === 2" class="file-preview">
            {{ getFilePreview(item) }}
          </text>

          <!-- 个人名片 -->
          <text v-else-if="item.msgType === 5" class="card-preview">
            [个人名片]{{ item.data?.userName || item.userCardData?.userName || '' }}
          </text>

          <!-- 群接龙 -->
          <text v-else-if="item.msgType === 6" class="joining-preview"> [群接龙] </text>

          <!-- 转发消息 -->
          <text v-else-if="item.msgType === 8" class="forward-preview"> [转发消息] </text>

          <!-- 其他消息类型 -->
          <text v-else class="other-preview"> [消息类型{{ item.msgType }}] </text>
        </view>
      </view>
      <view v-if="showEllipsis" class="ellipsis-item">
        <text class="ellipsis-text">...</text>
      </view>
    </view>
    <view class="card-footer">
      <text class="footer-text">聊天记录</text>
    </view>
  </view>
</template>

<script setup>
  import { computed, ref, watch, onMounted } from 'vue';

  import { groupApi } from '@/common/api/index.js';
  import EmojiDisplay from '@/pages/collaborativeGroup/emojiDisplay.vue';

  const props = defineProps({
    cardData: {
      type: Object,
      default: () => ({}),
    },
    groupId: {
      type: String,
      default: '',
    },
    memberList: {
      type: Array,
      default: () => [],
    },
  });

  const emit = defineEmits(['showDetail', 'allMemberList']);

  // 存储远程获取的用户信息
  const remoteUserInfo = ref({});

  // 只显示前3条消息作为预览
  const displayMessages = computed(() => {
    return props.cardData?.forwardMsgs?.slice(0, 3) || [];
  });
  // 是否显示省略号
  const showEllipsis = computed(() => {
    return props.cardData?.forwardMsgs?.length > 3;
  });

  // 获取用户ID列表：优先 fromRealUserId（真实用户ID），其次 from（协同岗ID）
  const getUserIds = (message) => {
    if (!message) return [];
    const ids = [];
    if (message.fromRealUserId) ids.push(message.fromRealUserId);
    if (message.from && !ids.includes(message.from)) ids.push(message.from);
    return ids;
  };

  // 获取需要查询的用户ID列表
  const getUserIdsToFetch = computed(() => {
    if (!props.cardData?.forwardMsgs) return [];

    const userIds = new Set();
    props.cardData.forwardMsgs.forEach((message) => {
      getUserIds(message).forEach((userId) => {
        // 只有当用户不在本地成员列表中时才需要远程获取
        const isLocalMember = props.memberList.find((m) => m.id == userId);
        if (!isLocalMember) {
          userIds.add(userId);
        }
      });
    });
    return Array.from(userIds);
  });

  // 获取用户信息（并发执行两个接口）
  const fetchUserInfo = async (userIds) => {
    if (!userIds || userIds.length === 0) return;

    try {
      // 并发调用获取用户信息和协同岗信息的接口
      const [userInfos, userInfos2] = await Promise.all([
        groupApi.getUserInfo({ userIds }),
        groupApi.getColloration({ userIds }),
      ]);

      // 处理普通用户信息
      if (userInfos?.results && Array.isArray(userInfos.results)) {
        userInfos.results.forEach((user) => {
          if (user && user.id) {
            remoteUserInfo.value[user.id] = user;
          }
        });
      }

      // 处理协同岗信息
      if (Array.isArray(userInfos2)) {
        userInfos2.forEach((item) => {
          if (item && item.id) {
            remoteUserInfo.value[item.id] = {
              id: item.id,
              name: item.postName,
              avatar: item.iconUrl,
            };
          }
        });
      }
    } catch (error) {
      console.error('获取用户信息失败:', error);
    }
  };

  // 根据 userId 查找用户名称（先本地成员，再远程信息）
  const findUserNameById = (userId) => {
    if (!userId) return null;
    // 1. 从本地群成员中查找
    const localMember = props.memberList.find((m) => m.id == userId);
    if (localMember && localMember.name && !localMember.name.startsWith('用户')) {
      return localMember.name;
    }
    // 2. 从远程获取的用户信息中查找
    const remoteUser = remoteUserInfo.value[userId];
    if (remoteUser && (remoteUser.name || remoteUser.userName)) {
      return remoteUser.name || remoteUser.userName;
    }
    return null;
  };

  // 获取用户名称 - 协同岗消息同时查协同岗名称和真实用户名拼接
  const getUserName = (message) => {
    // 协同岗消息：优先使用已拼接好的realName，否则查真实用户名拼接
    if (message.fromRealUserId) {
      const member = props.memberList?.find((m) => m.id == message.from);
      if (member?.realName && member.realName !== member?.name) {
        return member.realName;
      }
      const collabName = findUserNameById(message.from);
      const realName = findUserNameById(message.fromRealUserId);
      if (collabName && realName && collabName !== realName) {
        return `${collabName}(${realName})`;
      }
      if (collabName) return collabName;
      if (realName) return realName;
      const displayId = message.from;
      return displayId ? `用户${displayId}` : '未知用户';
    }

    // 非协同岗消息：按优先级尝试每个用户ID
    for (const userId of getUserIds(message)) {
      const name = findUserNameById(userId);
      if (name) return name;
    }

    // 对于个人名片消息，尝试从消息数据中获取名称
    if (message.msgType === 5 && message.data) {
      return message.data.userName || `用户${getUserIds(message)[0]}`;
    }

    // 返回默认值
    const displayId = getUserIds(message)[0];
    return displayId ? `用户${displayId}` : '未知用户';
  };
  // 合并所有用户信息（本地成员 + 远程获取的成员）
  const allUserInfo = computed(() => {
    const mergedUsers = [];

    // 添加本地成员
    props.memberList.forEach((member) => {
      console.log(789, member);
      if (member && member.id) {
        mergedUsers.push({
          id: member.id,
          name: member.name,
          avatar: member.tumbAvatar,
          realName: member.realName,
          tumbAvatar: member.tumbAvatar,
          postName: member.postName,
          collaborationAvatar: member.collaborationAvatar,
        });
      }
    });

    // 添加远程获取的成员
    Object.keys(remoteUserInfo.value).forEach((userId) => {
      const remoteUser = remoteUserInfo.value[userId];
      if (remoteUser) {
        // 检查是否已存在该用户
        const existingUser = mergedUsers.find((user) => user.id === userId);
        if (!existingUser) {
          mergedUsers.push({
            id: userId,
            name: remoteUser.name || remoteUser.userName,
            avatar: remoteUser.avatar,
          });
        }
      }
    });
    return mergedUsers;
  });

  // 获取文本消息内容 - 直接使用 text 字段
  const getTextContent = (message) => {
    if (!message || message.msgType !== 1) return '';

    // 直接使用内层消息的 text 字段
    return message.text || '';
  };

  // 获取文件消息预览
  const getFilePreview = (message) => {
    if (!message || message.msgType !== 2) return '';

    // 直接使用内层消息的 fileType 字段
    switch (message.fileType) {
      case 1:
        return '[图片]';
      case 2:
        return '[语音]';
      case 3:
        return '[视频]';
      default:
        return '[文件]';
    }
  };

  // 显示转发详情
  const showForwardDetail = () => {
    console.info('[群接龙调试] msgForwardCard showForwardDetail - props.memberList:', JSON.parse(JSON.stringify(props.memberList)));
    console.info('[群接龙调试] msgForwardCard showForwardDetail - allUserInfo:', JSON.parse(JSON.stringify(allUserInfo.value)));
    emit('showDetail', props.cardData);
    emit('allMemberList', allUserInfo.value);
  };

  // 监听需要获取的用户ID变化
  watch(
    () => getUserIdsToFetch.value,
    (newUserIds) => {
      if (newUserIds && newUserIds.length > 0) {
        fetchUserInfo(newUserIds);
      }
    },
    {
      immediate: true,
    },
  );

  // 组件挂载时也执行一次获取
  onMounted(() => {
    if (getUserIdsToFetch.value.length > 0) {
      fetchUserInfo(getUserIdsToFetch.value);
    }
  });
</script>

<style lang="scss" scoped>
  .msg-forward-card {
    background: #f8f9fa;
    border-radius: 6px; /* 12rpx → 6px */
    padding: 5px 10px; /* 10rpx 20rpx → 5px 10px */
    margin: 5px 0; /* 10rpx 0 → 5px 0 */
    border: 1px solid #e0e0e0; /* 1rpx → 1px */
    max-width: 230px; /* 460rpx → 230px */

    .card-header {
      margin-bottom: 8px; /* 16rpx → 8px */

      .forward-title {
        font-size: 10px; /* 20rpx → 10px */
        font-weight: 600;
        color: #333;
      }
    }

    .card-body {
      .forward-item {
        margin-bottom: 6px; /* 12rpx → 6px */
        font-size: 10px; /* 20rpx → 10px */
        line-height: 1.2;

        .sender-name {
          color: #666;
          font-weight: 500;
          display: inline;
        }

        .message-preview {
          color: #333;
          display: inline;

          .text-message-preview {
            display: inline;
          }

          .file-preview,
          .card-preview,
          .joining-preview,
          .forward-preview,
          .other-preview {
            color: #666;
          }
        }

        &:last-child {
          margin-bottom: 0;
        }
      }

      .ellipsis-item {
        display: flex;
        justify-content: flex-start;
        align-items: center;
        margin-top: 4px; /* 8rpx → 4px */

        .ellipsis-text {
          font-size: 10px; /* 20rpx → 10px */
          color: #999;
          font-weight: bold;
        }
      }
    }

    .card-footer {
      margin-top: 5px; /* 10rpx → 5px */
      padding-top: 5px; /* 10rpx → 5px */
      border-top: 1px solid #e0e0e0; /* 1rpx → 1px */

      .footer-text {
        font-size: 10px; /* 20rpx → 10px */
        color: #999;
      }
    }
  }
</style>
