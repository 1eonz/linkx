<script setup lang="ts">
  import { computed } from 'vue';

  import { usePIMStore } from '@/store';

  import { timeView } from '../common';
  // import { getUserInfo as getUserInfoApi,getCollocation } from '@/api/statics';

  const props = defineProps<{
    disableYouAlias?: boolean; // 历史消息中不将姓名替换为“你”
    fromChatList?: boolean;
    hideTime?: boolean;
    memberList?: any[]; // 添加memberList属性
    message: any;
  }>();

  // const emit = defineEmits(['modify']);

  const PIMStore = usePIMStore();

  // 根据userId获取用户名称
  const getUserNameById = (userId, isNeedRealName = true) => {
    // 首先检查传入的memberList
    if (props.memberList && props.memberList.length > 0) {
      const member = props.memberList.find((m) => Number(m.id) === Number(userId));
      if (member)
        return isNeedRealName
          ? member?.realName || member?.name
          : member?.name || member?.postName || member?.realName;
    }
    // 如果memberList中没有，则使用store中的userMap
    const user = PIMStore.userMap.get(Number(userId));
    if (user) return user.name;
    // 数据库查询
    //  getUserInfoApi({userIds:[userId]}).then((userInfo:any)=>{
    //   if(userInfo?.data?.results?.length>0){
    //     return userInfo?.data?.results[0].name
    //   }
    //  })
    // const realInfos:any = await getCollocation({userIds:[userId]})
    //   if(realInfos?.data?.length>0){
    //     return realInfos.data[0].relatedUserNames
    //   }

    return '未知用户';
  };

  const transform = computed(() => {
    const ret = { ...props.message.msg.cmcontainer };
    const { newGroupProfile, operateId, operationType, coUserGroupRelations } = ret;
    const { user } = PIMStore;

    const disableYouAlias = !!props.disableYouAlias;

    ret.name = disableYouAlias
      ? getUserNameById(operateId)
      : Number(operateId) === Number(user.userid)
        ? '你'
        : getUserNameById(operateId);
    if (operationType === 4 && newGroupProfile) {
      ret.announcement = newGroupProfile?.name;
      ret.muteType = newGroupProfile?.muteType;
      ret.applyJoinPolicy = newGroupProfile?.applyJoinPolicy;

      // 只有当 undefinedNameIds 存在时才覆盖 name（用于群成员列表变更场景）
      // 修改群名称场景没有 undefinedNameIds，应使用 operateId
      if (newGroupProfile?.undefinedNameIds) {
        const master =
          typeof newGroupProfile?.undefinedNameIds === 'string'
            ? newGroupProfile.undefinedNameIds.split(',')?.[0]
            : newGroupProfile?.undefinedNameIds;
        ret.name = disableYouAlias
          ? getUserNameById(master)
          : Number(master) === Number(user.id)
            ? '你'
            : getUserNameById(master);
      }
    }

    if (operationType === 5) {
      const obj = ret.memberList?.[0] || {};
      ret.addAdmin = obj ? obj.role === 1 : false;
      // 是禁言操作还是管理员操作
      ret.childType = obj && obj.muteType ? 'mute' : 'admin';
      ret.muteFlag = obj ? obj.muteType === 1 : false;
      // 管理员操作要区分添加移除管理员，还是群主转移
      const qunzuIndex = ret.memberList?.findIndex((i) => i.role === 2);
      ret.qunzhuChange = qunzuIndex > -1;
      if (ret.qunzhuChange) {
        const qunzuObj = ret.memberList[qunzuIndex];
        if (qunzuObj) {
          qunzuObj.name = getUserNameById(qunzuObj.userId);
        }
        ret.qunzuName = qunzuObj.name;
      }
    }
    ret.memberList?.forEach((i) => {
      // 确保获取用户信息
      if (!i.name) {
        i.name = getUserNameById(i.userId);
      }
    });

    ret.memberName = ret.memberList
      ?.map((i) => {
        // 如果没有name字段，使用userId作为后备
        if (!i.name) {
          i.name = getUserNameById(i.userId);
        }
        if (disableYouAlias) return i.name;
        // 处理特殊情况：如果用户是当前用户
        if (i.name === user.name) {
          return '你';
        }
        return i.name;
      })
      .join(',');

    if (!disableYouAlias && ret.memberName === user.name) {
      ret.memberName = '你';
    }

    // 协同岗-上下岗
    if (operationType === 11 && coUserGroupRelations) {
      // coUserGroupRelations 可能是数组，需要取第一个元素
      const relation = Array.isArray(coUserGroupRelations) ? coUserGroupRelations[0] : coUserGroupRelations;
      const { realUserId, userId, operType, prompt } = relation || {};
      const userName = getUserNameById(userId, false);
      let realUserInfo = '';
      if (realUserId) {
        realUserInfo = getUserNameById(realUserId, false);
        if (realUserInfo === '未知用户' && props.memberList) {
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
        if (realUserInfo && realUserInfo !== '未知用户') {
          xietongNames += `(${realUserInfo})`;
        }
        if (dutyText) {
          xietongNames += `—${dutyText}`;
        }
        ret.xietongNames = xietongNames;
      }
    }
    return ret;
  });

  const type = computed(() => props.message.msg.cmcontainer.operationType);

  // function modify() {
  //   emit('modify');
  // }
</script>

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
        <template v-if="transform?.memberList[0]?.joinType === 3">
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
      <p v-else-if="type === 4 && (transform?.applyJoinPolicy || transform?.applyJoinPolicy === 0)">
        该群已{{ transform.applyJoinPolicy === 1 ? '开启' : '关闭' }}"入群验证"
      </p>
      <!-- 修改群名称 -->
      <p v-else-if="type === 4 && transform?.announcement">
        <span class="user">{{ transform.name }} </span>修改群名称为
        <span class="light">"{{ transform.announcement }}"</span>
      </p>
      <!-- 群公告 -->
      <p v-else-if="type === 4 && transform?.noticeDto">
        <span class="user">{{ transform.name }} </span>发布了
        <span class="light">群公告</span>
      </p>
      <!-- 禁言 -->
      <p v-else-if="type === 4 && transform?.muteType">
        <span class="user">{{ transform.name }}</span>
        {{ transform.muteType === 1 ? '已开启' : '已关闭' }}"全员禁言"
      </p>
      <!-- 群主 -->
      <p v-else-if="type === 5 && transform?.qunzhuChange">
        <span class="light">{{ transform.qunzuName }}</span>
        成为新群主
      </p>
      <p v-else-if="type === 5 && !transform?.qunzhuChange">
        {{ transform.name }}已将“<span class="light">{{ transform.memberName }}</span>
        <span v-if="transform.childType === 'admin'" class="user">
          ”{{ transform.addAdmin ? '添加为管理员' : '从管理员中移除' }}
        </span>
        <span v-else class="user">”{{ transform.muteFlag ? '禁言' : '解除禁言' }}</span>
      </p>
      <!-- 创建群 -->
      <p v-else-if="type === 6"> {{ transform.name }}创建了群聊 </p>
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

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .group-notice {
    margin-bottom: 16px;
    font-size: 12px;
    font-weight: 500;
    text-align: center;

    p {
      color: var(--message-text-color);
      text-align: center;
      white-space: normal;
    }

    .time {
      margin-bottom: 8px;
    }

    .user {
      color: var(--message-text-color);
    }

    .light {
      color: var(--light-text-color);
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
      .ellipsis1();
    }
  }
</style>
