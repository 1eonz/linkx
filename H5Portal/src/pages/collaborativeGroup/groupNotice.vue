<template>
  <div>
    <div
      class="group-notice"
      :class="{
        'from-chat-list': fromChatList,
      }"
    >
      <p v-if="!hideTime" class="time">{{ timeView(props.message.time) }}</p>
      <!-- 进群（主动+邀请） -->
      <p v-if="type === 1">
        <template v-if="transform.memberList?.[0].joinType === 3">
          {{ transform.name }}{{ '邀请了' }}
        </template>
        <span class="light">{{ transform.memberName }} </span>进入了群聊
      </p>
      <!-- 主动退群 -->
      <p v-else-if="type === 2">
        <span class="light">{{ transform.memberName }} </span>退出了群聊
      </p>
      <!-- 被提出群 -->
      <p v-else-if="type === 3">
        {{ transform.name }}将<span class="light">{{ transform.memberName }} </span>移出了群聊
      </p>
      <!-- 加群策略 -->
      <p v-else-if="type === 4 && (transform.applyJoinPolicy || transform.applyJoinPolicy === 0)">
        该群已{{ transform.applyJoinPolicy === 1 ? '开启' : '关闭' }}"入群验证"
      </p>
      <!-- 修改群名称 -->
      <p v-else-if="type === 4 && transform.announcement">
        <span class="user">{{ transform.name }} </span>修改群名称为
        <span class="light">"{{ transform.announcement }}"</span>
      </p>
      <!-- 群公告 -->
      <p v-else-if="type === 4 && transform.noticeDto">
        <span class="user">{{ transform.name }} </span>发布了
        <span class="light">群公告</span>
      </p>
      <!-- 禁言 -->
      <p v-else-if="type === 4 && transform.muteType">
        <span class="user">{{ transform.name }}</span>
        {{ transform.muteType === 1 ? '已开启' : '已关闭' }}"全员禁言"
      </p>
      <!-- 群主 -->
      <p v-else-if="type === 5 && transform.qunzhuChange">
        <span class="light">{{ transform.qunzuName }}</span>
        成为新群主
      </p>
      <p v-else-if="type === 5 && !transform.qunzhuChange">
        {{ transform.name }}已将“<span class="light">{{ transform.memberName }}</span>
        <span v-if="transform.childType === 'admin'" class="user">
          ”{{ transform.addAdmin ? '添加为管理员' : '从管理员中移除' }}
        </span>
        <span v-else class="user">”{{ transform.muteFlag ? '禁言' : '解除禁言' }}</span>
      </p>
      <!-- 创建群 -->
      <p v-else-if="type === 6">{{ transform.name }}创建了群聊</p>
      <p v-else-if="type === 7">
        <span class="light">{{ transform.name }}</span>
        已解散该群
      </p>
      <p v-else-if="type === 11">
        <span class="user">{{ transform.xietongNames }}</span>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';

  import {
    getUserInfo as getUserInfoApi,
    getColloration,
  } from '@/common/api/collaborativeGroup.js';
  import { useCommunicationStore } from '@/stores/communication.js';
  //   import { usePIMStore } from '@/store';
  //   import { timeView } from '../common';

  const props = defineProps<{
    disableYouAlias?: boolean; // 历史消息中不将姓名替换为“你”
    fromChatList?: boolean;
    hideTime?: boolean;
    message: any;
    memberList?: any[]; // 添加memberList属性
  }>();
  const communicationStore = useCommunicationStore();
  //   const emit = defineEmits(['modify']);

  //   const PIMStore = usePIMStore();

  // 根据userId获取用户名称
  const getUserNameById = async (userId, isNeedRealName = true) => {
    // 首先检查userId是否为空
    if (!userId || userId === '' || userId < 0) {
      return `用户${userId}`;
    }
    // 首先检查传入的memberList
    if (props.memberList && props.memberList.length > 0) {
      const member = props.memberList.find((m) => Number(m.id) == Number(userId));
      if (member)
        return isNeedRealName
          ? member?.realName || member?.name
          : member?.name || member?.postName || member?.realName;
    }
    //如果不存在，数据库查询
    const userInfo = await getUserInfoApi({ userIds: [userId] });
    if (userInfo?.results?.length > 0) {
      return userInfo?.results?.[0]?.name;
    }
    const realInfos: any = await getColloration({ userIds: [userId] });
    if (realInfos?.length > 0) {
      return realInfos[0]?.relatedUserNames;
    }
    return `用户${userId}`;
  };

  const transform = ref<any>({});
  watch(
    () => props.message,
    async () => {
      const ret = JSON.parse(JSON.stringify(props.message.msg.cmcontainer));
      const { newGroupProfile, operateId, operationType, coUserGroupRelations } = ret;

      const user = await communicationStore.getUserInfo();
      const promises = [];

      const disableYouAlias = !!props.disableYouAlias;

      if (!disableYouAlias && operateId == user.userid) {
        ret.name = '你';
      } else {
        promises.push(
          getUserNameById(operateId, operationType !== 4).then((name) => {
            ret.name = name;
          }),
        );
      }
      if (operationType === 4 && newGroupProfile) {
        ret.announcement = newGroupProfile?.name;
        ret.muteType = newGroupProfile?.muteType;
        ret.applyJoinPolicy = newGroupProfile?.applyJoinPolicy;

        const master =
          typeof newGroupProfile?.undefinedNameIds === 'string'
            ? newGroupProfile.undefinedNameIds.split(',')?.[0]
            : newGroupProfile?.undefinedNameIds;
        if (master) {
          if (!disableYouAlias && master === user.userid) {
            ret.name = '你';
          } else {
            promises.push(
              getUserNameById(master).then((name) => {
                ret.name = name;
              }),
            );
          }
        }
      }
      if (operationType === 5) {
        const obj = ret?.memberList?.[0] || {};
        ret.addAdmin = obj ? obj.role === 1 : false;
        // 是禁言操作还是管理员操作
        ret.childType = obj && obj.muteType ? 'mute' : 'admin';
        ret.muteFlag = obj ? obj.muteType === 1 : false;
        // 管理员操作要区分添加移除管理员，还是群主转移
        const qunzuIndex = ret?.memberList?.findIndex((i) => i.role === 2);
        ret.qunzhuChange = qunzuIndex > -1;
        if (ret.qunzhuChange) {
          const qunzuObj = ret?.memberList[qunzuIndex];
          if (qunzuObj) {
            promises.push(
              getUserNameById(qunzuObj.userId).then((name) => {
                qunzuObj.name = name;
                ret.qunzuName = name;
              }),
            );
          }
        }
      }
      // 确保memberList是数组
      if (Array.isArray(ret?.memberList)) {
        const memberNamePromises = ret.memberList.map(async (i) => {
          // 确保获取用户信息
          if (!i.name) {
            i.name = await getUserNameById(i.userId);
          }
          return i;
        });
        promises.push(...memberNamePromises);

        // 等待所有memberList的用户信息获取完成
        await Promise.all(memberNamePromises);

        const memberName = [];
        ret.memberList.forEach((i) => {
          if (disableYouAlias) {
            memberName.push(i.name);
            return;
          }
          // 处理特殊情况：如果用户是当前用户
          if (i.name === user.username) {
            memberName.push('你');
          } else {
            memberName.push(i.name);
          }
        });
        ret.memberName = memberName.join(',');
        if (!disableYouAlias && ret.memberName === user.username) {
          ret.memberName = '你';
        }
      }
      // 协同岗-上下岗
      if (operationType === 11 && coUserGroupRelations) {
        // coUserGroupRelations 可能是数组，需要取第一个元素
        const relation = Array.isArray(coUserGroupRelations) ? coUserGroupRelations[0] : coUserGroupRelations;
        const { realUserId, userId, operType, prompt } = relation || {};
        const userName = await getUserNameById(userId, false);
        let realUserInfo = '';
        if (realUserId) {
          realUserInfo = await getUserNameById(realUserId, false);
          if (realUserInfo.startsWith('用户') && props.memberList) {
            const collabMember = props.memberList.find((m) => Number(m.id) === Number(userId));
            if (collabMember?.relatedUserNames) {
              realUserInfo = collabMember.relatedUserNames;
            }
          }
        }
        const dutyText = operType === 1 ? '上岗' : operType === 2 ? '下岗' : '';
        if (prompt) {
          ret.xietongNames = prompt
            .replace(/{COOPERATION_USER_NAME}/g, userName || '')
            .replace(/{GROUP_SUPPORT_USER_NAME}/g, realUserInfo || '')
            .replace(/{DUTY_TEXT}/g, dutyText || '');
          if (!realUserInfo) {
            ret.xietongNames = ret.xietongNames.replace(/\(\)/g, '');
          }
        } else {
          let xietongNames = userName;
          if (realUserInfo && !realUserInfo.startsWith('用户')) {
            xietongNames += `(${realUserInfo})`;
          }
          if (dutyText) {
            xietongNames += `—${dutyText}`;
          }
          ret.xietongNames = xietongNames;
        }
      }

      // 等待所有异步操作完成后再赋值
      await Promise.all(promises);
      transform.value = ret;
    },
    {
      deep: true,
      immediate: true,
    },
  );

  const type = computed(() => props.message.msg.cmcontainer.operationType);
  //   function modify() {
  //     emit('modify');
  //   }

  const timeView = (time) => {
    function isBeforeToday(targetDate) {
      const today = new Date();
      const todayStart = new Date(today.getFullYear(), today.getMonth(), today.getDate());

      // 处理传入的时间参数
      let targetTime;
      if (typeof targetDate === 'string') {
        targetTime = new Date(targetDate);
      } else if (targetDate instanceof Date) {
        targetTime = targetDate;
      } else {
        targetTime = new Date(targetDate);
      }

      // 清除目标时间的时分秒毫秒，只比较日期部分
      const targetDateOnly = new Date(
        targetTime.getFullYear(),
        targetTime.getMonth(),
        targetTime.getDate(),
      );

      return targetDateOnly < todayStart;
    }

    function formatDate(date, format) {
      const d = new Date(date);

      // 补零函数
      const padZero = (num) => num.toString().padStart(2, '0');

      const year = d.getFullYear();
      const month = padZero(d.getMonth() + 1);
      const day = padZero(d.getDate());
      const hours = padZero(d.getHours());
      const minutes = padZero(d.getMinutes());
      const seconds = padZero(d.getSeconds());

      // 简单的格式化逻辑
      switch (format) {
        case 'YYYY-MM-DD HH:mm':
          return `${year}-${month}-${day} ${hours}:${minutes}`;
        case 'HH:mm':
          return `${hours}:${minutes}`;
        default:
          return `${hours}:${minutes}`;
      }
    }
    return isBeforeToday(time) ? formatDate(time, 'YYYY-MM-DD HH:mm') : formatDate(time, 'HH:mm');
  };
</script>
<style scoped lang="scss">
  .group-notice {
    font-size: 12px;
    font-weight: 500;
    text-align: center;

    p {
      color: #666;
      text-align: center;
      white-space: normal;
    }

    .time {
      margin-bottom: 8px;
    }

    .user {
      color: #666;
    }

    .light {
      color: #159aff;
      cursor: pointer;
    }
  }

  .from-chat-list {
    p {
      display: flex;
      align-items: center;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .light {
      display: inline-block;
      max-width: 150px;
    }
  }
</style>
