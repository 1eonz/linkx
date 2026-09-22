<script setup lang="ts">
  import { computed, ref, unref } from 'vue';

  import { Dialog } from '@/components/Dialog';
  import { usePIMStore } from '@/store';

  import SelectMember from '../chatGroup/selectMember.vue';
  import MessageList from './messageList.vue';

  const emit = defineEmits(['closeDialog', 'toChat']);

  const PIMStore = usePIMStore();

  const keyword = ref('');
  const filterOptions = [
    {
      id: 1,
      name: '全部',
    },
    {
      id: 2,
      name: '文件',
    },
    {
      id: 3,
      name: '图片和视频',
    },
    {
      id: 4,
      name: '链接',
    },
  ];
  const member = ref<any>([]);
  const tabActive = ref(1);

  const chat = computed(() => PIMStore.currentChat);
  const title = computed(() => `${unref(chat).name}的聊天记录`);

  const messageList = computed(() => {
    const arr = [...PIMStore.currentChatMessages];
    const k = unref(keyword);
    const mk = unref(member).map((i) => i.id);
    const type = unref(tabActive);
    return arr.filter((i) => {
      if (mk.length > 0 && !mk.includes(i.id)) {
        return false;
      }

      if (type !== 1 && i.msgType === 1) {
        return false;
      }

      if (type === 2 && (!i.msg?.fileType || i.msg.fileType < 4)) {
        return false;
      }

      if (type === 3 && ![1, 3].includes(i.msg?.fileType)) {
        return false;
      }

      // TODO：链接类型是多少？
      if (type === 4) {
        return false;
      }

      if (k !== '') {
        switch (i.msgType) {
          case 1: {
            // 文本
            return i.msg?.text.includes(k);
          }
          case 5: {
            // 卡片搜索不显示
            return false;
          }
          case 8: {
            // 合并转发
            const index = i.msg?.forwardMsgs.findIndex((i) => i.text?.includes(k));
            return index !== -1 || i.msg?.title.includes(k);
          }
          default: {
            return i.msg?.fileName?.includes(k);
          }
        }
      }

      return true;
    });
  });

  function closeWindow() {
    emit('closeDialog');
  }

  function choiceMember() {
    const cid = `SelectMember_${unref(chat).id}`;
    Dialog({
      cid,
      content: SelectMember,
      data: {
        defaultKeys: unref(member).map((i) => i.id),
        memberList: PIMStore.groupMemberActive,
        onSubmit: (list) => {
          member.value = list;
          Dialog(cid)?.close();
        },
        title: '选择群成员',
      },
    });
  }

  function removeMember(data) {
    member.value = unref(member).filter((i) => i.id !== data.id);
  }

  function reset() {
    member.value = [];
  }

  function tabClick(data) {
    tabActive.value = data.id;
  }

  // 处理定位到聊天的功能
  function handleToChat(item) {
    emit('toChat', item);
    closeWindow();
  }
</script>

<template>
  <TdFrameBox
    class="chat-history"
    :dragger="true"
    :is-light="true"
    :show-line="true"
    size="normal"
    :title="title"
    @close-frame-box="closeWindow"
  >
    <div class="container">
      <TdInput v-model="keyword" class="message-search" placeholder="搜索" type="searchInput" />

      <TdTab :data="filterOptions" @click="tabClick" />

      <div class="swapper">
        <MessageList
          :hidden-menu="true"
          :hidden-quote="true"
          :message-list="messageList"
          @to-chat="handleToChat"
        />

        <div v-if="chat.sessionType === 2" class="filter">
          <div class="title">
            <span>筛选条件</span>
            <span @click="reset">重置</span>
          </div>
          <div class="member-title">群成员</div>
          <div class="member" placeholder="请选择群成员" @click="choiceMember">
            <div v-for="item in member" :key="item.id" class="item">
              <span>{{ item.name }}</span>
              <Icon name="reject_light" @click.stop="removeMember(item)" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </TdFrameBox>
</template>

<style scoped lang="less">
  .chat-history {
    left: calc(30% - 13vh) !important;
    width: 800px;
    height: 640px;

    .container {
      display: flex;
      flex-direction: column;
      width: 100%;
      height: 100%;
      padding-top: 16px;

      :deep(.td-input-inner) {
        color: var(--item-text-color) !important;
        background: var(--td-input-inner-bg) !important;
      }

      :deep(.is-blur) {
        background: var(--td-input-inner-bg) !important;
      }
    }

    :deep(.td-tab) {
      margin-top: 16px;

      .td-tabs-items {
        width: auto;
        padding: 0 16px;
      }
    }

    .swapper {
      display: flex;
      flex: 1;
      height: 0;
      border-top: 1px solid rgb(153 206 251 / 40%);

      .message-list {
        width: calc(100% - 240px);
        height: 100%;
        overflow: auto;
        border-right: 1px solid rgb(153 206 251 / 40%);
      }

      .filter {
        width: 240px;
        padding: 0 16px;
        border-left: 1px solid rgb(255 255 255 / 20%);

        .title {
          display: flex;
          justify-content: space-between;
          margin: 20px 0 24px;

          span:nth-of-type(1) {
            font-size: 14px;
            font-weight: 400;
            color: var(--item-text-color);
          }

          span:nth-of-type(2) {
            font-size: 14px;
            font-weight: 400;
            color: rgb(33 150 254);
            cursor: pointer;
          }
        }

        .member-title {
          color: var(--item-text-color);
        }

        .member:empty::before {
          font-size: 12px;
          color: var(--item-text-color);
          content: attr(placeholder);
        }

        .member {
          display: flex;
          flex-flow: row wrap;
          align-items: center;
          min-height: 44px;
          max-height: 300px;
          padding: 4px 8px;
          margin: 8px 0;
          overflow: auto;
          background: var(--td-input-inner-bg) !important;
          border-radius: 2px;

          .item {
            box-sizing: border-box;
            display: flex;
            align-items: center;
            padding: 4px 8px;
            margin: 0 4px 4px 0;
            background: var(--message-info-right-bg);
            border: 1px solid;
            border-radius: 2px;
            border-image: linear-gradient(
              180deg,
              rgb(77 147 201 / 34%) 0%,
              rgb(77 147 201 / 60%) 100%
            );
            border-image-slice: 1;

            span {
              color: rgb(255 255 255);
            }

            .td-icon {
              margin-left: 6px;
              cursor: pointer;
            }
          }
        }
      }
    }
  }

  :deep(.td-tab-default) {
    &::after {
      border-bottom: none !important;
    }
  }

  :deep(.td-tabs__active-bar) {
    background-color: rgb(21 154 255) !important;
  }

  :deep(.td-tabs__item) {
    color: var(--item-text-color) !important;
  }

  :deep(.td-tabs__item.is-active) {
    color: rgb(21 154 255) !important;
  }

  :deep(.td-input-inner) {
    color: rgb(102 102 102) !important;
  }
</style>
