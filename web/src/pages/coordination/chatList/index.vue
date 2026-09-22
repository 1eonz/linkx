<script setup lang="ts">
  import { computed, ref, unref } from 'vue';

  import MessageBox from '@/components/MessageBox';
  import {
    chatListSort,
    checkUserInGroup,
    filterMessage,
    getGroupOperationType,
    timeView,
  } from '@/pages/coordination/common';
  import EmojiView from '@/pages/coordination/emoji/emojiView.vue';
  import { usePIMStore } from '@/store';

  import { cloneDeep } from 'lodash-es';

  import GroupNotice from '../chatPanel/groupNotice.vue';
  import PimSearchSlot from './pimSearchSlot.vue';

  withDefaults(
    defineProps<{
      hiddenHeader?: boolean;
    }>(),
    {
      hiddenHeader: false,
    },
  );

  const FILETYPE = {
    1: '图片',
    2: '语音',
    3: '视频',
    4: '其它类型文件',
    5: 'word',
    6: 'excel',
    7: 'pdf',
    8: 'txt',
    9: 'sms',
    10: 'ppt',
    11: 'location',
  };

  const PIMStore = usePIMStore();

  const activeTab = ref(0);
  const virtualRef = ref();

  const userId = computed(() => PIMStore.user.id);
  const xieTongId = computed(() => PIMStore.user.cooperationUser?.userId);
  const chatList = computed(() => {
    const chatProfiles = {}; // 获取置顶和免打扰信息
    PIMStore.user?.chatProfiles?.forEach((item) => {
      chatProfiles[item.targetId] = item;
    });

    const list = cloneDeep(PIMStore.chatList);

    // @我+@所有人
    list.forEach((item) => {
      // 过滤掉不需要展示的消息
      item.messages = item.messages?.filter((i) => filterMessage(i)) || [];
      const { messages } = item;
      if (item.sessionType !== 2 || !messages) {
        return;
      }

      item.atMe = false;
      if (item.latestMsg) {
        item.latestMsg.msgType = 1;
      }

      /**
       * 展示未读的@我自己，存在 `@我` 并且未读的消息
       */
      messages.forEach((child) => {
        if (child.msg?.text && child.from !== `${unref(userId)}`) {
          const atRegex = /(\[@.*?:)/g;
          const matches = child.msg.text.match(atRegex) || [];
          matches.forEach((match) => {
            const id = Number(match.substring(2, match.length - 1));
            item.latestMsg = child;
            const f = PIMStore.userMap.get(child.from * 1);
            if (f) {
              item.latestMsg.fromName = f.name;
            } else {
              PIMStore.getUserInfo(child.from);
            }

            const atMe = id === unref(userId);
            // @单人判断消息逻辑与@所有人收到判断逻辑需要区分开
            if (atMe) {
              item.atMe = atMe && !child.read;
              item.latestMsg.msgType = 1;
              item.latestMsgContent = atMe ? `@${PIMStore.user.name}` : '@所有人';
              return;
            }
            if (id === item.id || atMe || id === unref(xieTongId)) {
              item.atMe = atMe || !child.read;
              item.latestMsg.msgType = 1;
              item.latestMsgContent = atMe ? `@${PIMStore.user.name}` : '@所有人';
            }
          });
        }
      });
    });

    const filter = list.filter((i) => {
      // if (i.sessionType === 2) {
      //   return checkUserInGroup(i.id);
      // }

      switch (unref(activeTab)) {
        case 0: {
          return true;
        }
        case 1: {
          return false;
        }
        case 2: {
          return i.atMe;
        }
        case 3: {
          // 检查是否有未读消息
          const hasUnread = i.messages?.some(
            (msg) => !msg.read && Number(msg.from) !== unref(userId) && !msg.groupNotice,
          );
          return hasUnread;
        }
        case 4: {
          return i.sessionType === 1;
        }
        case 5: {
          return i.sessionType === 2;
        }
        default: {
          return false;
        }
      }
    });

    const arr = filter.map((i) => {
      const ret = {
        ...i,
        ...chatProfiles[i.sessionId],
        include: i.sessionType === 2 ? checkUserInGroup(i.id) : true,
        isCollaboration: false,
      };

      // 检查是否有草稿，且当前不是活跃会话时才显示草稿
      const draft = PIMStore.getDraft(i.sessionId);
      if (draft && i.sessionId !== PIMStore.chatListActive) {
        ret.latestMsgContent = `[draft]${draft}`;
        ret.latestMsgTime = timeView(Date.now());
        // 如果有草稿，不显示发送人信息
        ret.latestMsg = {
          ...ret.latestMsg,
          fromName: '',
        };
        return ret;
      }

      if (i.messages?.length > 0) {
        const effectMessages = i.messages.filter((item) => {
          if (item.msg?.cmcontainer) {
            return item.msg?.cmcontainer?.operationType !== 11;
          }
          return true;
        });
        if (effectMessages?.length > 0) {
          const latestMsg = effectMessages[effectMessages.length - 1];
          const { from, msg, msgType, newGroupProfile, operateId, operationType, time } = latestMsg;

          ret.latestMsg = latestMsg;
          ret.latestMsgTime = timeView(time);

          switch (msgType) {
            case 1: {
              ret.latestMsgContent = msg?.text;
              break;
            }
            case 2: {
              const { fileType } = msg || {};
              if (fileType) {
                ret.latestMsgContent = `[${FILETYPE[fileType]}]`;
              }
              break;
            }
            case 5: {
              ret.latestMsgContent = `[卡片]`;
              break;
            }
            default: {
              break;
            }
          }
          // 初始化群组成员，解决没有打开聊天窗列表获取不到群组成员问题
          latestMsg.memberList?.forEach((i) => {
            getInfo(i, i.userId);
          });

          // 群聊不显示已读未读
          if (i.sessionType === 2) {
            const fromId = operationType ? operateId : from;
            const f = PIMStore.userMap.get(fromId * 1);
            if (f) {
              ret.latestMsg.fromName = f.name;
            } else {
              PIMStore.getUserInfo(fromId);
            }

            if (f && operationType) {
              ret.latestMsgContent = getGroupOperationType(latestMsg, f);
            }

            switch (newGroupProfile?.muteType) {
              case 1: {
                ret.latestMsgContent = `${f?.name}已开启全员禁言`;
                break;
              }
              case 2: {
                ret.latestMsgContent = `${f?.name}已关闭全员禁言`;
                break;
              }
              default: {
                break;
              }
            }
          } else {
            ret.unread = i.messages.filter((i) => {
              return Number(i.from) !== unref(userId) && !i.read && !i.groupNotice;
            }).length;
          }
        } else {
          ret.latestMsgTime = '';
          ret.latestMsgContent = '';
        }
      } else {
        ret.latestMsgTime = '';
        ret.latestMsgContent = '';
      }
      return ret;
    });

    return chatListSort(arr);
  });

  function getInfo(target, id) {
    const f = PIMStore.userMap.get(id * 1);
    if (f) {
      Object.assign(target, f);
    } else {
      PIMStore.getUserInfo(id);
    }
  }

  const tabOptions = computed<any>(() => {
    let total = 0;
    unref(chatList).forEach((item) => {
      const { isMuteNotifications, unread } = item;
      if (unread && !isMuteNotifications) {
        total += unread;
      }
    });
    return [
      {
        count: total,
        label: '全部',
        value: 0,
      },
      // {
      //   // count: 9,
      //   label: '协同群组',
      //   value: 1,
      // },
      {
        // count: 2,
        label: '@我',
        value: 2,
      },
      {
        label: '未读',
        value: 3,
      },
      {
        label: '单聊',
        value: 4,
      },
      {
        label: '群聊',
        value: 5,
      },
    ];
  });

  function tabChange(item) {
    virtualRef.value?.reset();
    activeTab.value = item.value;
  }

  function chatChange(item) {
    console.log('chat', item);
    const { sessionId } = item;
    PIMStore.setChatListActive(sessionId);
  }

  // 会话右键
  async function chatContextmenu(val, data) {
    switch (val) {
      case '1': {
        PIMStore.setTopChat(data, true);
        break;
      }
      case '2': {
        PIMStore.setTopChat(data, false);
        break;
      }
      case '3': {
        PIMStore.setChatDoNotDisturb(data, true);
        break;
      }
      case '4': {
        PIMStore.setChatDoNotDisturb(data, false);
        break;
      }
      case '5': {
        const res = await MessageBox({
          isLight: true,
          offset: ['45%', '20%'],
          text: '确定删除吗？',
          type: 'ok',
        });
        if (res) {
          PIMStore.deleteChat(data);
        }
        break;
      }
    }
  }

  function getChatOptions(chat) {
    const ret: any = [];

    ret.push(
      chat.isStickyOnTop
        ? {
            label: '取消置顶',
            value: '2',
          }
        : {
            label: '置顶聊天',
            value: '1',
          },
    );

    if (chat.id !== unref(userId) && !PIMStore.collaboration) {
      ret.push(
        chat.isMuteNotifications
          ? {
              label: `取消 "消息免打扰"`,
              value: '4',
            }
          : {
              label: '消息免打扰',
              value: '3',
            },
      );
    }

    ret.push({
      label: '删除聊天',
      value: '5',
    });
    return ret;
  }
</script>

<template>
  <div class="chat-list">
    <div class="search">
      <TdSearch v-show="false" />
    </div>

    <div class="filter">
      <div class="tabs">
        <div
          v-for="item in tabOptions"
          :key="item.value"
          class="tab-item"
          :class="{ active: item.value === activeTab }"
          @click="tabChange(item)"
        >
          {{ item.label }} {{ item.count || '' }}
        </div>
        <div class="fil">
          <PimSearchSlot :is-light="true" />
        </div>
      </div>
    </div>

    <div class="list">
      <div v-if="chatList.length === 0" class="empty">
        <img alt="" src="@/assets/images/pim/main_empty.png" srcset="" />
        <div class="text">
          <span>高效工作，从沟通开始</span>
          <!-- <PimSearchSlot :is-light="true" :is-show-text="true" /> -->
        </div>
      </div>

      <TdVirtualList v-else ref="virtualRef" :data="chatList" :total="chatList.length">
        <template #item="{ item }">
          <TdDropdownMenu
            :is-light="true"
            :options="getChatOptions(item)"
            trigger="contextmenu"
            @click="(v) => chatContextmenu(v, item)"
          >
            <div
              class="list-item"
              :class="{
                active: item.sessionId === PIMStore.chatListActive,
                top: item.isStickyOnTop,
              }"
              @click="chatChange(item)"
            >
              <div class="avatar">
                <TdChatHead
                  :avatar-id="item.avatar"
                  :offline="item.isDel || !item.include"
                  :session-type="item.sessionType"
                />
              </div>
              <div class="wrapper">
                <div class="wra">
                  <div class="info">
                    <div class="name">{{ item.name || item.undefinedName }}</div>
                    <!-- <TdTag
                      v-if="collaborationMap.get(item.sessionId)"
                      label="协同岗"
                      type="station"
                    /> -->
                    <div v-if="item.cooperationUser" class="tags">
                      <TdTag
                        v-if="item?.cooperationUser?.serviceStatus === 1"
                        label="协同岗"
                        type="station"
                      />
                      <!-- <TdTag
                        v-for="i in item.userLabels"
                        :key="i.labelId"
                        :label="i.labelName"
                        type="station"
                      /> -->
                    </div>
                  </div>
                  <div class="time">{{ item.latestMsgTime }}</div>
                </div>
                <div class="wra">
                  <div v-if="item.latestMsg" class="content">
                    <span v-if="item.atMe" class="at">[有人@我]</span>
                    <GroupNotice
                      v-else-if="item.latestMsg.notifyType === 'GROUP_EVENT'"
                      :from-chat-list="true"
                      :hide-time="true"
                      :message="item.latestMsg"
                    />
                    <span v-else-if="item.latestMsg.msgType === 8" class="card">[聊天记录]</span>
                    <template v-else-if="item.sessionType === 2">
                      <span
                        v-if="item.latestMsg.operationType"
                        :class="item.latestMsg.noticeDto ? 'notice' : 'card'"
                      >
                        {{ item.latestMsgContent }}
                      </span>
                      <template v-if="!item.latestMsg.groupNotice">
                        <template v-if="item.latestMsgContent?.startsWith('[draft]')">
                          <span class="draft-tag">[草稿]</span>
                          <EmojiView :is-list="true" :text="item.latestMsgContent.substring(7)" />
                        </template>
                        <template v-else>
                          {{ item.latestMsg.fromName }}：
                          <EmojiView :is-list="true" :text="item.latestMsgContent" />
                        </template>
                      </template>
                    </template>
                    <template v-else>
                      <template v-if="item.latestMsgContent?.startsWith('[draft]')">
                        <span class="draft-tag">[草稿]</span>
                        <EmojiView :is-list="true" :text="item.latestMsgContent.substring(7)" />
                      </template>
                      <EmojiView v-else :is-list="true" :text="item.latestMsgContent" />
                    </template>
                  </div>
                  <div v-else></div>

                  <div class="tips">
                    <Icon v-if="item.isStickyOnTop" class="notify" name="top" prefix="im" />
                    <Icon
                      v-if="item.isMuteNotifications"
                      class="notify"
                      name="no_disturbing"
                      prefix="im"
                    />
                    <div
                      v-if="item.unread"
                      class="unread"
                      :class="{ 'unread-mute': item.isMuteNotifications }"
                    >
                      <span v-if="!item.isMuteNotifications">{{ item.unread }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </TdDropdownMenu>
        </template>
      </TdVirtualList>
    </div>
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .chat-list {
    width: 412px;
    height: 100%;
    padding: 0 10px;
    background: var(--chat-list-bg);

    .search {
      display: flex;
      height: 10px;
    }

    .filter {
      height: 42px;

      .tabs {
        display: flex;
        align-items: center;
        justify-content: space-between;
        height: 42px;
        padding-right: 5px;
        font-family: 'HarmonyOS Sans SC';
        font-size: 18px;
        font-weight: 500;
        line-height: 20px;
        text-align: left;
        background: var(--dark-back);

        .tab-item {
          display: flex;
          align-items: center;
          height: 34px;
          padding: 0 12px;
          color: var(--tabs-color);
          cursor: pointer;

          &.active {
            color: var(--item-text-color);
            background: var(--message-info-left-bg);
            border-radius: 2px;
          }
        }
      }

      .fil {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 32px;
        height: 32px;
        cursor: pointer;
      }
    }

    .list {
      width: 100%;
      height: calc(100% - 56px);
      overflow-y: auto;

      .list-item {
        display: flex;
        width: 100%;
        height: 67px;
        padding: 12px 16px;
        cursor: pointer;
        backdrop-filter: blur(27.18px);

        &.top {
          background: rgb(173 204 240 / 14%);
        }

        &.active {
          background: var(--list-item-active);
          border: 1px solid var(--list-item-active-border);
        }

        &:hover {
          background: var(--list-item-hover-bg);
        }

        :deep(.emoji-light) {
          color: var(--tabs-color) !important;
        }

        .avatar {
          width: 36px;
          height: 36px;
          margin-right: 12px;
        }

        .wrapper {
          width: calc(100% - 48px);

          .wra {
            display: flex;
            justify-content: space-between;

            &:first-of-type {
              margin-bottom: 4px;
            }

            .tips {
              display: flex;
              align-items: center;

              .notify {
                width: 10px;
                height: 10px;
                margin-left: 10px;
              }
            }
          }

          .info {
            display: flex;
            align-items: center;

            .name {
              max-width: 190px;
              margin-right: 4px;
              font-size: 14px;
              font-weight: 700;
              line-height: 150%;
              color: var(--text-color);
              .ellipsis1();

              white-space: pre;
            }

            .tags {
              margin-top: -4px;
            }
          }

          .time {
            font-size: 12px;
            font-weight: 500;
            line-height: 17px;
            color: rgb(125 155 189);
          }

          .content {
            display: flex;
            font-size: 12px;
            font-weight: 500;
            color: var(--text-other-color);
            .ellipsis1();

            :deep(.draft-tag) {
              display: inline-flex;
              align-items: center;
              font-weight: 600;
              color: #ed0f12;
            }

            :deep(.emoji-img) {
              display: inline-flex;
              align-items: center;
              vertical-align: middle;
            }
          }

          .at {
            font-weight: 600;
            color: rgb(240 7 7);
          }

          .notice {
            font-weight: 600;
            color: #ed0f12;
          }

          .card {
            color: var(--message-text-color);
          }

          .unread {
            width: 16px;
            height: 16px;
            margin-left: 10px;
            font-size: 12px;
            font-weight: 500;
            line-height: 14px;
            color: rgb(255 255 255);
            text-align: center;
            background: #ed0f12;
            border-radius: 8px;
          }

          .unread-mute {
            width: 10px !important;
            height: 10px !important;
          }
        }
      }

      .empty {
        display: flex;
        flex-direction: column;
        align-items: center;
        width: 100%;
        margin-top: 172px;

        img {
          width: 114px;
          height: 77px;
          margin-bottom: 12px;
        }

        .text {
          display: flex;

          span {
            font-size: 14px;
            font-weight: 500;
            color: var(--text-color);
          }
        }
      }
    }
  }

  :deep(.td-input-inner) {
    color: var(--item-text-color) !important;
  }

  :deep(.emoji-view) {
    white-space: nowrap !important;
  }

  .draft-tag {
    margin-right: 4px;
    color: #ed0f12;
  }
</style>
