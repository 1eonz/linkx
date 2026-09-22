<script lang="ts" setup>
  import { computed, nextTick, onMounted, ref, unref } from 'vue';

  import { queryChatIdByGroupId, querySmsHistoryList } from '@/api/sms';
  import { updateMsgStatus } from '@/comm/message';
  import { useEmitter } from '@/hooks';

  import { throttle } from 'lodash-es';

  import MessageItem from './messageItem.vue';

  const props = defineProps<{
    chatId: any;
    id: string;
    isShowSelect: boolean;
    userList: any[];
  }>();

  const emit = defineEmits(['sendAgain', 'selectItem', 'changeChatData']);

  const chatBoxRef = ref();
  const _chatId = ref('');
  const msgList = ref<any[]>([]);
  let total = 0;

  const showMsgList = computed(() => {
    let time = 0;
    const ret = unref(msgList).map((item, index) => {
      const { userList } = props;
      let speaker = '';
      userList.forEach((i) => {
        if (i.isdn === item.userId) {
          speaker = i.name;
        }
      });
      const curTime = Date.parse(item.sendTime);
      let showTime = false;
      if (unref(msgList).length - 1 === index) {
        time = curTime;
        showTime = true;
      } else {
        const diff = curTime - time;
        if (diff > 3 * 60 * 1000) {
          time = curTime;
          showTime = true;
        }
      }
      return { ...item, showTime, speaker };
    });
    return ret;
  });
  const attachList = computed(() => {
    return showMsgList.value
      .filter((item) => item.type === 'image')
      .map((item) => {
        return item.attach;
      });
  });

  useEmitter('updateMsgList', updateMsgEvent);

  onMounted(async () => {
    await getChatId();
    updateMsgStatus(unref(_chatId));
    queryMsgList(1);
  });

  /**
   * 消息通知事件
   */
  async function updateMsgEvent(data) {
    if (data.groupId !== props.id) {
      return;
    }
    if (!unref(_chatId)) {
      await getChatId();
    }
    await queryMsgList(1);
    updateMsgStatus(unref(_chatId));
  }

  /**
   * 获取会话id
   */
  async function getChatId() {
    const id = props.chatId;
    if (id) {
      _chatId.value = id;
      return;
    }

    const { code, data } = await queryChatIdByGroupId({
      groupId: props.id,
    });
    if (code === 0) {
      _chatId.value = data;
    }
  }

  /**
   * 查询消息列表
   * 刷新机制不应该每次都查全部，可以是每次查最新的10条，然后插入列表
   * @param pageNum
   */
  async function queryMsgList(pageNum: number) {
    const id = _chatId.value;
    const param = {
      chatId: id,
      pageNum,
      pageSize: 10,
    };
    const { code, data } = await querySmsHistoryList(param);
    if (code !== 0) {
      return;
    }

    total = Number(data.total);

    const arr = data.records || [];
    if (pageNum === 1) {
      arr.reverse().forEach((item) => {
        const index = unref(msgList).findIndex((i) => i.id === item.id);
        if (index === -1) {
          msgList.value.push(item);
        }
      });
    } else {
      msgList.value = [...arr.reverse(), ...unref(msgList)];
    }

    if (pageNum === 1) {
      scrollToBottom();
    } else {
      const item = unref(msgList)[arr.length - 1];
      const { id } = item;
      scrollToAnchor(id);
    }
  }

  /**
   * 滚动到底部
   */
  function scrollToBottom() {
    nextTick(() => {
      const container = chatBoxRef.value;
      container.scrollTop = container.scrollHeight;
    });
  }

  /**
   * 滚动到锚点位置
   * @param id
   */
  function scrollToAnchor(id: string) {
    nextTick(() => {
      const anchor = document.getElementById(`messageItem-${id}`);
      anchor?.scrollIntoView(false);
    });
  }

  /**
   * 判断是否滚动到顶部 上拉加载更多历史消息
   */
  const handleScroll = throttle((e) => {
    const el = e.srcElement;
    const scrollTop = el.scrollTop; // 滚动高度

    const len = unref(msgList).length;
    if (scrollTop === 0 && len < total) {
      queryMsgList(Number.parseInt(`${len / 10}`) + 1);
    }
  }, 200);

  function sendAgain(item) {
    emit('sendAgain', item);
  }

  function selectItem(item, flag) {
    emit('selectItem', item, flag);
  }

  // 改变信息数据
  function changeChatData(id, key, value) {
    emit('changeChatData', id, key, value);
  }
</script>

<template>
  <div ref="chatBoxRef" class="chat-box" @scroll="handleScroll">
    <MessageItem
      v-for="item in showMsgList"
      :key="item.id"
      :attach-list="attachList"
      :chat-id="_chatId"
      :is-show-select="isShowSelect"
      :item="item"
      @change-chat-data="changeChatData"
      @select-item="selectItem"
      @send-again="sendAgain"
    />
  </div>
</template>

<style lang="less" scoped>
  .chat-box {
    flex: 1;
    width: 100%;
    padding: 8px;
    overflow-y: auto;
    border-top: 1px solid #123364;

    .more-history {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 14px;
      margin: 10px 0;
    }
  }
</style>
