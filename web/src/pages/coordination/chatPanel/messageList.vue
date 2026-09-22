<script setup lang="ts">
  import { computed, nextTick, ref, unref, watch } from 'vue';

  import { downloadFile, sendMessage } from '@/api/pim';
  import { Message } from '@/components/Message';
  import EmojiView from '@/pages/coordination/emoji/emojiView.vue';
  import MrsDialog from '@/pages/notification/components/mrsDialog.vue';
  import MsgVideoHistory from '@/pages/notification/components/msgVideoHistory.vue';
  import MsgVoiceHistory from '@/pages/notification/components/msgVoiceHistory.vue';
  import { usePIMStore } from '@/store';

  import { cloneDeep, throttle } from 'lodash-es';

  import { filterMessage, handleDeleteMessage, timeView } from '../common';
  import MemberDetailsPopover from "./memberDetailsPopover/index.vue"
  import GroupNotice from './groupNotice.vue';
  import MsgCard from './msgCard.vue';
  import MsgFile from './msgFile.vue';
  import MsgForward from './msgForward.vue';
  import QuoteMsg from './quoteMsg.vue';
  import QuoteMsg2 from './quoteMsg2.vue';

  const props = defineProps<{
    hiddenMenu?: boolean;
    hiddenMenuAll?: boolean;
    hiddenQuote?: boolean;
    isShowGroup?: boolean;
    messageList: any;
  }>();

  const emit = defineEmits([
    'transmit',
    'multiSelect',
    'modify',
    'reSend',
    'toChat',
    'changeCheck',
  ]);
  defineExpose({ closeMultiple, scrollToAnchor, scrollToBottom, toQuoteMsg });

  const PIMStore = usePIMStore();

  const attachList = ref<any[]>([]);
  const activeVideo = ref<any>({});
  const mrsDialogRef = ref();
  const failMsg = ref<any[]>([]);
  const dropDownMenuOption = ref<any[]>([
    {
      label: '复制',
      value: '1',
    },
  ]);
  const highlightId = ref(null);
  const showCheck = ref(false); // 是否展示勾选框
  const listRef = ref();
  const preDownloadList = ref<string[]>([]); // 预下载列表若下载失败则从列表中剔除
  const listScroll = ref(true);
  const pageNum = ref(1);
  const pageSize = 15;
  const loading = ref(false);

  const userId = computed(() => PIMStore.user?.id);
  const quoteMsg = computed(() => PIMStore.quoteMsg);
  const collaboration = computed(() => PIMStore.collaboration);
  const xietongId = computed(() => PIMStore.user?.cooperationUser?.userId);
  const groupIds = computed(() => PIMStore.user?.cooperationUser?.groupIds || []);

  const showList = computed(() => {
    const arr = cloneDeep(props.messageList).filter((i) => filterMessage(i));
    const ret = arr.reverse().slice(0, unref(pageNum) * pageSize);
    return ret.reverse();
  });

  watch(showList, handleDownload, { immediate: true });
  watch(
    () => PIMStore.chatListActive,
    () => {
      // 切换会话重置聊天页数
      pageNum.value = 1;
      listScroll.value = true;
      scrollToBottom();
    },
    { immediate: true },
  );
  // 列表消息发生变更后需要将列表滚动到底部
  watch(() => props.messageList, scrollToBottom, { immediate: true });

  function handleDownload() {
    unref(showList).forEach(async (item) => {
      const { fileKey } = item.msg || {};
      if (fileKey) {
        const isDownload = preDownloadList.value.includes(fileKey);
        // 下载过的附件不重复下载
        if (!isDownload) {
          // 下载附件
          await handleDownloadFile(fileKey, item.msg.fileType, item.seq);

          // 图片会把容器高度成达导致滚动条未在最底下
          if (unref(pageNum) === 1) {
            setTimeout(scrollToBottom, 500);
          }
        }
      }
      // 如果是视频封面则单独再下载一次
      const { videoThumb } = item.msg || {};
      if (videoThumb) {
        const isDownload = preDownloadList.value.includes(videoThumb);
        if (!isDownload) {
          await handleDownloadFile(videoThumb, item.msg.fileType, item.seq);
        }
      }
    });
  }

  // 文件下载
  async function handleDownloadFile(fileId, type, seqId) {
    if (!fileId) {
      return;
    }
    preDownloadList.value.push(fileId);
    const param = {
      forceAsync: false,
      isInline: false,
    };
    // 下载附件时存储附件预览列表
    const res = await downloadFile(fileId, '0', param);
    const attachBlob = new Blob([res]);
    const attachUrl = URL.createObjectURL(attachBlob);

    const index = unref(attachList).findIndex((item) => item.fileKey === fileId);
    if (index === -1) {
      attachList.value.push({
        fileKey: fileId,
        seqId,
        type,
        url: attachUrl,
      });
    }
  }

  function clickVideo(data: any) {
    showVideoDialog(data);
  }

  function showVideoDialog(item: any) {
    activeVideo.value.attach = getAttach(item);
    mrsDialogRef.value?.closeWindow();
    setTimeout(() => {
      mrsDialogRef.value?.clickVideo();
    }, 0);
  }

  function getAttach(item) {
    if (!item || !item.msg || !item.msg.fileKey) {
      return;
    }
    const imageInfo = attachList.value.filter((val) => item.msg.fileKey === val.fileKey);
    if (imageInfo.length > 0) {
      return imageInfo[0].url;
    }
    return '';
  }

  function getVideoThumb(videoThumb) {
    if (!videoThumb) {
      return;
    }
    const imageInfo = attachList.value.filter((val) => videoThumb === val.fileKey);
    if (imageInfo.length > 0) {
      return imageInfo[0].url;
    }
    return '';
  }

  // 查找预览index
  function getAttachIndex(fileKey) {
    const index = attachList.value
      .filter((val) => val.type === 1)
      .sort((pre, next) => {
        return pre.seqId - next.seqId;
      })
      .findIndex((item) => item.fileKey === fileKey);
    return index;
  }

  // 获取预览列表
  function getAttachList() {
    return attachList.value
      .filter((val) => val.type === 1)
      .sort((pre, next) => {
        return pre.seqId - next.seqId;
      })
      .map((item) => {
        return item.url;
      });
  }

  function reSend(item) {
    emit('reSend', item.msg.text);
  }

  function handleCheckItem(item) {
    console.log(item);
    if (unref(showCheck)) {
      emit('changeCheck', item);
    }
  }

  function isFailed(item) {
    const filters = failMsg.value.filter((val) => item.id === val.id);
    return filters.length > 0;
  }

  function editMessage(item) {
    emit('reSend', item.msg.text);
  }

  function isSelf(data) {
    return Number(data.from) === unref(userId) || Number(data.fromRealUserId) === unref(userId);
  }

  function getDropdownMenu(data) {
    if (props.hiddenMenuAll) return;
    if (data.msgType === 5) {
      dropDownMenuOption.value = [
        {
          label: '删除',
          value: '6',
        },
      ];
      return;
    }
    if (props.hiddenMenu) {
      dropDownMenuOption.value = [
        {
          label: '定位到聊天',
          value: '11',
        },
        {
          label: '删除',
          value: '6',
        },
      ];
      return;
    }
    const dropdownMenu = [
      {
        label: '复制',
        value: '1',
      },
      {
        label: '转发',
        value: '2',
      },
      {
        label: '引用',
        value: '3',
      },
      {
        label: '撤回',
        value: '4',
      },
      {
        label: '多选',
        value: '5',
      },
      {
        label: '删除',
        value: '6',
      },
    ];

    // 对端消息/非文本消息/超时2分钟情况不显示撤回下拉
    const curTime = Date.now();
    const diff = curTime - data.time;
    //  || data.msgType !== 1
    if (!isSelf(data) || diff > 2 * 60 * 1000) {
      dropdownMenu.splice(3, 1);
    }
    // 非文本消息不显示复制
    if (data.msgType !== 1) {
      dropdownMenu.splice(0, 1);
    }
    dropDownMenuOption.value = dropdownMenu;
  }

  async function dropdownMenuClick(val, item) {
    switch (val) {
      case '1': {
        // 复制
        copyText(item);
        break;
      }
      case '2': {
        // 转发
        emit('transmit', 3, item);
        break;
      }
      case '3': {
        // 引用
        handleQuote(item);
        break;
      }
      case '4': {
        // 撤回
        handleCancel(item);
        break;
      }
      case '5': {
        // 多选
        handleMultiple(item);
        break;
      }
      case '6': {
        // 删除
        handleDeleteMessage(item);
        break;
      }
      case '11': {
        // 定位到聊天
        toChatMenu(item);
        break;
      }
      // No default
    }
  }

  function copyText(item) {
    // 非文本消息不能复制成功
    if (!item || item.msgType !== 1 || !item.msg?.text) return;
    let content = item.msg.text;
    if (window.getSelection()) {
      content = window.getSelection()?.toString();
    }
    // 未获取到浏览器选中值时则拷贝整条消息
    if (content === '') {
      content = item.msg.text;
    }
    // 复制文本到剪贴板
    navigator.clipboard
      ?.writeText(content)
      .then(() => {
        // 可以在这里添加复制成功的提示
        Message({
          message: '内容已复制',
          type: 'success',
        });
      })
      .catch(() => {
        Message({ message: '复制失败', type: 'error' });
      });
  }

  function toChatMenu(item) {
    emit('toChat', item);
  }

  function handleMultiple(item) {
    showCheck.value = true;
    emit('multiSelect', true, item);
  }

  function closeMultiple() {
    showCheck.value = false;
  }

  /**
   * 判断是否滚动到顶部 上拉加载更多历史消息
   */
  const handleScroll = throttle(async (e) => {
    const el = e.target;
    const scrollTop = el.scrollTop; // 滚动高度
    const list = props.messageList;
    if (scrollTop === 0 && !unref(loading)) {
      listScroll.value = true;
      loading.value = true;
      await PIMStore.getOfflineMsg(PIMStore.currentChat, false);
      loading.value = false;

      if (unref(showList).length < list.length) {
        pageNum.value++;
      }

      nextTick(() => {
        el.scrollTop = 1;
        listScroll.value = false;
      });
    }
  }, 500);

  // 音频，视频，文件信息引用后，点击跳转到引用信息位置处，并高亮显示
  async function toQuoteMsg(item) {
    // 检查目标消息是否在当前显示列表中
    const targetMsg = unref(showList).find((msg) => msg.msgId === item.msgId);

    if (!targetMsg) {
      // 如果消息不在当前列表中，增加pageNum直到找到目标消息
      while (unref(pageNum) * pageSize < props.messageList.length) {
        pageNum.value++;
        await nextTick();
        const found = unref(showList).find((msg) => msg.msgId === item.msgId);
        if (found) {
          break;
        }
      }
    }

    highlightId.value = item.msgId; // 设置高亮消息ID
    nextTick(() => {
      // 确保DOM更新完成后再执行滚动操作
      const messageElement = listRef.value.querySelector(`.highlight`);

      if (messageElement) {
        listScroll.value = false;
        messageElement.scrollIntoView({ behavior: 'smooth', block: 'center' }); // 平滑滚动到元素位置
        setTimeout(() => {
          // 5秒后移除高亮样式
          listScroll.value = true;
          highlightId.value = null; // 重置高亮消息ID，移除高亮样式
        }, 5000);
      }
    });
  }

  // 消息撤回
  async function handleCancel(val) {
    // 消息撤回基本参数
    console.log('消息撤回', val);

    const param: any = {
      category: PIMStore.currentChat.sessionType,
      clientMsgId: `${Date.now()}`,
      from: unref(userId),
      msg: {
        srcMsgId: val.msgId,
      },
      msgType: 7,
      to: val.to,
      // plaintext: 1,
    };

    // 作为协同岗支撑的群聊才携带，作为普通用户在群无需携带
    if (unref(collaboration) && unref(groupIds).includes(Number(val.to))) {
      param.from = unref(xietongId);
      param.fromRealUserId = unref(userId);
    }
    await sendMessage(param);
  }

  // 消息引用
  function handleQuote(item) {
    PIMStore.setQuoteMsg(item);
  }

  /**
   * 滚动到底部
   */
  function scrollToBottom() {
    if (!listScroll.value) return;
    nextTick(() => {
      const container = listRef.value;
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

  function handleMessageItemClick(e, item) {
    if (unref(showCheck)) {
      e.preventDefault();
      emit('changeCheck', item);
    }
  }
</script>

<template>
  <div ref="listRef" class="message-list" @scroll="handleScroll">
    <img v-if="loading" class="loading" src="@/assets/images/common/earth_loading.gif" />
    <template v-for="item in showList" :key="item.msgId">
      <GroupNotice
        v-if="item.notifyType === 'GROUP_EVENT'"
        :message="item"
        @modify="emit('modify')"
      />
      <TdDropdownMenu
        v-else
        :hidden-menu="hiddenMenuAll"
        :is-light="true"
        :options="dropDownMenuOption"
        trigger="contextmenu"
        @click="(val) => dropdownMenuClick(val, item)"
        @drop-menu-open="getDropdownMenu(item)"
      >
        <div
          :id="`messageItem-${item.msgId}`"
          class="message-item"
          :class="{ highlight: item.msgId === highlightId }"
          @click="(e) => handleMessageItemClick(e, item)"
        >
          <div class="message" :class="{ myself: isSelf(item) }">
            <div v-if="item.msgType !== 7" class="avatar">
              <TdCheckbox
                v-show="showCheck"
                v-model="item.check"
                class="check-box"
                @change="handleCheckItem(item)"
                @click.stop
              />
              <MemberDetailsPopover :user-id="item.id">
                <TdChatHead
                  :avatar-id="item.avatar"
                  class="avatar"
                />
              </MemberDetailsPopover>
            </div>
            <div
              class="content"
              :class="{
                withdraw: item.msgType === 7,
              }"
            >
              <div v-if="item.msgType !== 7" class="info-time">
                <MemberDetailsPopover :user-id="item.id">
                  <span>{{ item.name }}</span>
                </MemberDetailsPopover>
                {{ timeView(item.time) }}
              </div>

              <!-- 视频回显 -->
              <MsgVideoHistory
                v-if="item.msgType === 2 && item.msg?.fileType && item.msg?.fileType === 3"
                :attach="getVideoThumb(item.msg?.videoThumb)"
                :loading-video="item.loadingVideo"
                :video-item="item"
                @click="clickVideo(item)"
              />

              <!-- 图片回显 -->
              <ElImage
                v-else-if="item.msgType === 2 && item.msg?.fileType && item.msg?.fileType === 1"
                fit="scale-down"
                :infinite="false"
                :initial-index="getAttachIndex(item.msg?.fileKey)"
                :preview-src-list="getAttachList()"
                :src="getAttach(item)"
                style="max-width: 350px; margin-bottom: 10px"
              />

              <!-- 音频回显 -->
              <MsgVoiceHistory
                v-else-if="item.msgType === 2 && item.msg?.fileType && item.msg?.fileType === 2"
                :audio-time="item.msg?.duration"
                class="text"
                :is-self="isSelf(item)"
                :self-id="item.msgId"
                :src="getAttach(item)"
              />

              <!-- 卡片回显 -->
              <MsgCard v-else-if="item.msgType === 5" :card-data="item" />

              <!-- 转发回显 -->
              <MsgForward v-else-if="item.msgType === 8" :card-data="item.msg" />

              <!-- 发送文件回显 -->
              <MsgFile
                v-else-if="
                  item.msgType === 2 &&
                  item.msg?.fileType &&
                  item.msg?.fileType !== 1 &&
                  item.msg?.fileType !== 2 &&
                  item.msg?.fileType !== 3
                "
                :card-data="item.msg"
              />

              <!-- 消息撤回回显 -->
              <p v-else-if="item.msgType === 7" class="msg-cancel">
                <span v-if="item.id === userId || item.id === xietongId" class="pre-text">
                  你撤回了一条消息
                </span>
                <span v-else class="pre-text">{{ item.name }}撤回了一条消息</span>
                <span
                  v-if="
                    (item.id === userId || item.id === xietongId) &&
                    item.revokeMsgType === 1 &&
                    item.msg?.text
                  "
                  class="btn-text"
                  @click="editMessage(item)"
                >
                  "重新编辑"
                </span>
              </p>

              <!-- 文字回显 -->
              <EmojiView v-else class="text" :is-list="false" :text="item.msg?.text || item.text" />

              <!-- 发送失败重新发送 -->
              <div v-if="isSelf(item) && isFailed(item)" class="send-failed">
                <Icon name="send_failed" @click="reSend(item)" />
              </div>
            </div>
          </div>
          <!-- 撤回的消息不显示引用 -->
          <div
            v-if="item.msg?.srcMsgId && item.msgType !== 7"
            class="qeote-box"
            :class="{ 'qeote-myself': isSelf(item) }"
          >
            <QuoteMsg2
              :attach-list="attachList"
              :quote-msg-id="item.msg?.srcMsgId"
              @click-video="clickVideo"
              @to-quote-msg="toQuoteMsg"
            />
          </div>
          <!-- 消息状态回显 -->
          <div
            v-if="
              isSelf(item) &&
              item.to !== item.from &&
              item.msgType !== 7 &&
              item.category !== 2 &&
              !hiddenMenu &&
              !hiddenMenuAll
            "
            class="read-tag"
          >
            <span v-if="item.read" class="read">已读</span>
            <span v-else class="not-read">未读</span>
          </div>
          <div v-if="showCheck" class="mask"></div>
        </div>
      </TdDropdownMenu>
    </template>
    <MrsDialog
      v-if="activeVideo.attach"
      ref="mrsDialogRef"
      :video-item="activeVideo"
      :video-url="activeVideo.attach"
    />
    <div v-if="quoteMsg.msgId && !hiddenQuote" class="quote-msg">
      <div class="close-btn" @click="PIMStore.setQuoteMsg({})">
        <img alt="" src="@/assets/images/message/icon_close.png" />
      </div>
      <QuoteMsg :attach-list="attachList" :quote-msg="quoteMsg" @to-quote-msg="toQuoteMsg" />
    </div>
  </div>
</template>

<style scoped lang="less">
  .message-list {
    flex: 1;
    width: 100%;
    height: calc(100% - 216px);
    padding: 10px;
    overflow-y: auto;
    background: var(--message-list-bg);
    border-right: 1px solid rgb(102 102 102 / 20%);

    .message-item {
      position: relative;

      .mask {
        position: absolute;
        top: 0;
        width: 100%;
        height: 100%;
      }
    }

    .withdraw {
      display: flex;
      align-items: center !important;
      justify-content: center;
      width: 100%;
    }

    .msg-cancel {
      display: flex;
      margin: 8px 0;

      .pre-text {
        font-size: 12px;
        font-weight: 400;
        color: var(--message-text-color);
      }

      .btn-text {
        font-size: 12px;
        font-weight: 400;
        color: var(--light-text-color);
        cursor: pointer;
      }
    }

    .time {
      margin-bottom: 10px;
      font-size: 12px;
      font-weight: 400;
      color: rgb(153 206 251 / 100%);
      text-align: center;
    }

    .highlight {
      background: rgb(29 116 214 / 16%);
      border: 0.0625rem solid rgb(29 116 214);
    }

    .message {
      display: flex;

      .avatar {
        display: flex;
        align-items: flex-start;
        cursor: pointer;
      }

      img {
        width: 32px;
        height: 32px;
        margin-right: 22px;
        border-radius: 50%;
      }

      .check-box {
        height: 40px;
        padding-right: 5px;
        padding-bottom: 10px;
      }

      .info-time {
        margin-bottom: 8px;
        font-size: 12px;
        font-weight: 400;
        color: var(--message-text-color);

        span {
          color: var(--message-text-color);
          cursor: pointer;
        }
      }

      .send-failed {
        padding-top: 8px;
        margin-right: 5px;
        text-align: center;
        cursor: pointer;
      }

      .text {
        position: relative;
        display: flex;
        width: fit-content;
        max-width: 900px;
        padding: 6px;
        margin-bottom: 10px;
        font-size: 14px;
        font-weight: 500;
        color: var(--text-color);
        word-break: break-all;
        background: var(--message-info-left-bg);
        border-radius: 3px;
        border-top-left-radius: 0;

        &::before {
          position: absolute;
          top: -5px;
          left: -5px;
          width: 0;
          height: 0;
          content: '';
          border-top: 6px solid transparent;
          border-right: 6px solid transparent;
          border-bottom: 6px solid var(--message-info-left-bg);
          border-left: 6px solid transparent;
          transform: rotate(45deg);
        }
      }
    }

    .qeote-box {
      display: flex;
      margin: -6px 53px 5px;

      .msg-content {
        width: fit-content;
        max-width: 40%;
        height: fit-content;
        padding: 4px 12px;
        background: var(--msg-content-bg);
      }
    }

    .qeote-myself {
      justify-content: flex-end;
    }

    .read-tag {
      padding-right: 70px;
      font-size: 12px;
      text-align: right;

      .read {
        color: rgb(125 155 189);
      }

      .not-read {
        color: var(--tabs-active-color);
      }
    }

    .myself {
      flex-direction: row-reverse;

      .avatar {
        flex-direction: row-reverse;
      }

      .check-box {
        padding-left: 5px;
      }

      .content {
        display: flex;
        flex-direction: column;
        align-items: flex-end;
      }

      img {
        margin: 0 0 0 22px;
      }

      .text {
        color: rgb(255 255 255);
        background-color: var(--message-info-right-bg);
        border-top-left-radius: 3px;
        border-top-right-radius: 0;

        &::before {
          right: -4px;
          left: auto;
          border-bottom: 6px solid var(--message-info-right-bg);
          transform: rotate(-45deg);
        }
      }
    }

    .quote-msg {
      position: absolute;
      bottom: 215px;
      left: 0;
      width: 100%;
      max-height: 90px;
      background: var(--quote-msg-bg);

      .close-btn {
        position: absolute;
        top: 4px;
        right: 4px;
        width: 14px;
        height: 14px;
        cursor: pointer;

        img {
          width: 100%;
          height: 100%;
        }
      }
    }

    .loading {
      width: 36px;
      height: 36px;
      margin: 0 auto;
    }
  }

  :deep(.el-image-viewer__prev.is-disabled) {
    display: none;
  }

  :deep(.el-image-viewer__next.is-disabled) {
    display: none;
  }
</style>
