<script setup lang="ts">
  import { computed, onMounted, ref } from 'vue';

  import { getChatHistoryCount, getCollocation, getUserInfo } from '@/api/statics';
  import { Dialog } from '@/components/Dialog';
  import { getCategoryNames, openForwardMsgs } from '@/pages/coordination/common';
  import EmojiView from '@/pages/coordination/emoji/emojiView.vue';
  import { usePIMStore } from '@/store';

  const props = defineProps<{
    cardData?: any;
    groupId?: string;
    memberList?: any;
  }>();

  const PIMStore = usePIMStore();

  interface GroupMember {
    id: number | string;
    name: string;
    tumbAvatar?: string;
  }

  interface UserInfo {
    avatar?: string;
    id: number | string;
    name: string;
    tumbAvatar?: string;
  }

  interface ChatHistoryResponse {
    records?: Array<{
      [key: string]: any;
      id: number | string;
      name: string;
      tumbAvatar?: string;
    }>;
  }

  const groupMembers = ref<GroupMember[]>([]);
  // 改为数组格式
  const externalUserInfos = ref<UserInfo[]>([]);

  // 获取群成员信息
  const fetchGroupMembers = async () => {
    if (!props.groupId) return;

    const res = await getChatHistoryCount({ groupId: props.groupId, pageSize: 100 });
    const data = res.data as ChatHistoryResponse;
    groupMembers.value =
      data && Array.isArray(data.records)
        ? data.records.map((record) => ({
            id: record.id,
            name: record.name,
            tumbAvatar: record.tumbAvatar,
          }))
        : [];
  };

  // 批量获取用户信息
  const fetchUserInfos = async (userIds: (number | string)[]) => {
    if (userIds.length === 0) return;

    try {
      // 过滤掉已经获取过的用户ID
      const newUserIds = userIds.filter(
        (id) =>
          !externalUserInfos.value.some((user) => user.id === id) &&
          !groupMembers.value.some((member) => member.id === id),
      );

      if (newUserIds.length === 0) return;

      const res = await getUserInfo({ userIds: newUserIds });
      if (
        (res as any).data &&
        (res as any).data.results &&
        Array.isArray((res as any).data.results)
      ) {
        // 直接赋值给数组，而不是push
        externalUserInfos.value = (res as any).data.results.map((user: any) => ({
          avatar: user.avatar || user.tumbAvatar || '',
          id: user.id,
          name: user.name || `用户${user.id}`,
        }));
      }

      // 获取协同岗信息
      const { code, data } = await getCollocation({ userIds: newUserIds });

      if (code === 0 && Array.isArray(data)) {
        data.map((item) => {
          externalUserInfos.value.push({
            avatar: item.iconUrl,
            id: item.id,
            name: item.postName,
          });
        });
      }
      console.log('externalUserInfos', externalUserInfos.value);
    } catch (error) {
      console.error('获取用户信息失败:', error);
    }
  };

  // 根据用户ID获取用户信息（优化版）
  const getUserInfoData = (userId: number | string) => {
    if (!userId) return { avatar: '', name: `用户${userId}` };

    // 优先从props.memberList中查找（包含协同岗realName等完整成员信息）
    if (props.memberList && props.memberList.length > 0) {
      const propMember = props.memberList.find((member) => member.id == userId);
      if (propMember) {
        return {
          avatar: propMember.tumbAvatar || '',
          name: propMember.name || `用户${userId}`,
          realName: propMember.realName,
        };
      }
    }
    // 其次从群成员中查找
    const groupMember = groupMembers.value.find((member) => member.id == userId);
    if (groupMember) {
      return {
        avatar: groupMember.tumbAvatar || '',
        name: groupMember.name || `用户${userId}`,
      };
    }
    // 其次从外部用户信息中查找
    const externalUser = externalUserInfos.value.find((user) => user.id == userId);
    if (externalUser) {
      return {
        avatar: externalUser.avatar || '',
        name: externalUser.name,
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
  };

  // 检查用户信息是否有效（不是默认值）
  const isValidUserInfo = (userInfo: any) => {
    return userInfo && userInfo.name && !userInfo.name.startsWith('用户');
  };

  // 获取用户ID列表：优先 fromRealUserId（真实用户ID），其次 from（协同岗ID）
  const getUserIds = (item: any) => {
    const ids: (number | string)[] = [];
    if (item.fromRealUserId) ids.push(item.fromRealUserId);
    if (item.from && !ids.includes(item.from)) ids.push(item.from);
    return ids;
  };

  // 提取所有需要获取信息的用户ID
  const extractUserIdsFromMsgData = (cardData: any) => {
    const userIds = new Set<number | string>();

    if (!cardData?.forwardMsgs) return [];
    cardData.forwardMsgs.forEach((item: any) => {
      getUserIds(item).forEach((userId) => userIds.add(userId));
    });

    return [...userIds];
  };

  const msgData = computed(() => {
    const { forwardMsgs } = props.cardData;
    if (!forwardMsgs) return [];

    return forwardMsgs.map((item) => {
      // 协同岗消息：同时查协同岗名称和真实用户名，拼接为"协同岗名称(真实用户名)"
      if (item.fromRealUserId) {
        const collabInfo = getUserInfoData(item.from);
        const realInfo = getUserInfoData(item.fromRealUserId);
        // 优先使用已拼接好的realName（如"lm测试协同岗(lm)"）
        if (collabInfo.realName && collabInfo.realName !== collabInfo.name) {
          return { ...item, name: collabInfo.realName };
        }
        const collabName = collabInfo.name;
        const realName = realInfo.name;
        // 两个都有效时拼接，否则取有效的那个
        if (isValidUserInfo(collabInfo) && isValidUserInfo(realInfo) && collabName !== realName) {
          return { ...item, name: `${collabName}(${realName})` };
        }
        if (isValidUserInfo(collabInfo)) {
          return { ...item, name: collabName };
        }
        if (isValidUserInfo(realInfo)) {
          return { ...item, name: realName };
        }
        return { ...item, name: `用户${item.from}` };
      }
      // 非协同岗消息：按优先级尝试每个用户ID
      for (const userId of getUserIds(item)) {
        const userInfo = getUserInfoData(userId);
        if (isValidUserInfo(userInfo)) {
          return { ...item, name: userInfo.name };
        }
      }
      // 都找不到，使用默认值
      const userIds = getUserIds(item);
      const userInfo = getUserInfoData(userIds[0] || '');
      return { ...item, name: userInfo.name };
    });
  });

  function showMsgList() {
    // 嵌套转发：不关闭父弹窗，使用不同的cid
    const isNested = !!Dialog('msgListForward');
    // 合并群成员和外部用户信息
    const groupMemberArr = props.memberList || groupMembers.value;
    const allMembers = [
      ...groupMemberArr.map((item) => {
        return {
          ...item,
          name: item?.name,
          realName: item?.realName,
        };
      }),
      ...externalUserInfos.value.map((user) => ({
        id: user.id,
        name: user.name,
        tumbAvatar: user.avatar,
      })),
    ];

    // 嵌套转发时关闭回调关闭嵌套弹窗自身，非嵌套时关闭父弹窗
    const closeCallback = isNested ? closeNestedDialog : closeGroupDialog;
    openForwardMsgs(props.cardData, props.groupId, allMembers, closeCallback, isNested);
  }

  function closeGroupDialog() {
    if (!Dialog('msgListForward')) return;
    Dialog('msgListForward')?.close();
  }

  function closeNestedDialog() {
    if (!Dialog('msgListForwardNested')) return;
    Dialog('msgListForwardNested')?.close();
  }

  function getName(txt) {
    let obj: any = {};
    if (txt) {
      try {
        // 尝试解析 JSON 字符串
        obj = JSON.parse(txt);
      } catch {
        // 如果解析失败，说明是普通字符串，直接使用
      }
    }
    if (obj.forwardMsgs) {
      return getCategoryNames(obj.forwardMsgs);
    }
    return '';
  }
  function formatText(text: string) {
   return text.replace('@all', '@所有人');
  }
  onMounted(async () => {
    if (props.groupId) {
      await fetchGroupMembers();
    }

    // 提取并获取所有需要的用户信息
    if (props.cardData) {
      const userIds = extractUserIdsFromMsgData(props.cardData);
      if (userIds.length > 0) {
        await fetchUserInfos(userIds);
      }
    }
  });
</script>

<template>
  <div v-if="cardData" class="msg-forward-card" @click="showMsgList">
    <div class="card-header">
      {{ cardData?.title || getCategoryNames(msgData) }}
    </div>
    <div class="card-body">
      <div v-for="item in msgData" :key="item.sessionSeqId" class="item">
        <span class="name">{{ item.name }}：</span>
        <span v-if="item.fileType === 3" class="overview">[视频]</span>
        <span v-else-if="item.fileType === 1" class="overview">[图片]</span>
        <span v-else-if="item.fileType === 2" class="overview">[音频]</span>
        <span v-else-if="item.msgType === 5" class="overview">[卡片]</span>
        <span v-else-if="item.fileType" class="overview">[文件]</span>
        <span v-else-if="item.msgType === 6" class="overview">[接龙]</span>
        <EmojiView
          v-else
          class="text"
          :is-list="true"
          style="display: inline"
          :text="formatText(item.text || getName(item.userTxt))"
        />
      </div>
    </div>
    <ElDivider class="divider" />
    <div class="card-bottom">聊天记录</div>
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .msg-forward-card {
    width: 240px;
    padding-top: 12px;
    margin: 5px 0;
    cursor: pointer;
    background: var(--msg-forward-card);
    border: 1px solid var(--msg-forward-card-border);
    border-radius: 7px;

    .card-header {
      height: 22px;
      padding: 0 12px;
      font-size: 14px;
      font-weight: 500;
      line-height: 22px;
      color: var(--text-color);
      .ellipsis1();
    }

    .card-body {
      padding: 4px 12px 8px;

      .item {
        // display: flex;
        font-size: 12px;
        font-weight: 400;
        line-height: 150%;
        color: var(--tabs-color);

        .name {
          color: var(--tabs-color);
        }

        .text {
          width: 160px;
          color: var(--tabs-color);
        }

        .overview {
          color: var(--tabs-color);
        }
      }
    }

    .divider {
      width: 100%;
      margin: 0;
      border-color: var(--divider-border-color);
    }

    .card-bottom {
      width: 100%;
      height: 24px;
      padding-left: 12px;
      font-size: 10px;
      font-weight: 500;
      line-height: 24px;
      color: var(--bottom-text-color);
    }
  }
</style>
