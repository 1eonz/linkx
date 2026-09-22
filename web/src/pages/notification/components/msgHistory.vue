<script setup lang="ts">
  import { computed, onMounted, ref, unref, watch } from 'vue';

  import { getVideoFromApp, queryChatIdByGroupId, querySmsHistoryList } from '@/api/sms';
  import { appConfig } from '@/config';
  import { useI18n } from '@/hooks';
  import { getIp } from '@/utils';
  import dateUtil from '@/utils/dateUtil';

  import { showTime } from '../helper';
  import MrsDialog from './mrsDialog.vue';
  import MsgVideoHistory from './msgVideoHistory.vue';
  import MsgVoiceHistory from './msgVoiceHistory.vue';

  // 后端返回数据结构
  type MessageInfo = {
    chatId: string;
    content: string;
    isRead: number;
    msgBelongId: string;
    sendTime: string;
    title: string;
  };

  const props = defineProps<{
    info: MessageInfo;
  }>();

  const emit = defineEmits(['closeDialog']);

  const { t } = useI18n();

  const chatId = ref('');
  const keyword = ref('');
  const searchTime = ref('');
  const msgList = ref<any[]>([]);
  const activeVideo = ref<any>({});
  const mrsDialogRef = ref();
  const failMsg = ref<any[]>([]);

  watch(
    () => unref(keyword),
    () => {
      queryMsgList(1);
    },
  );

  const showMsgList = computed(() => {
    const arr = [...msgList.value];
    let time = 0;
    arr.forEach((item, index) => {
      const curTime = Date.parse(item.sendTime);
      if (index === 0) {
        time = curTime;
        item.showTime = true;
      } else {
        const diff = curTime - time;
        if (diff > 3 * 60 * 1000) {
          time = curTime;
          item.showTime = true;
        }
      }
    });
    failMsg.value.forEach((item: any) => {
      arr.splice(item.index, 0, item);
    });
    return arr;
  });
  const attachList = computed(() => {
    return showMsgList.value
      .filter((item) => item.type === 'image')
      .map((item) => {
        return item.attach;
      });
  });

  onMounted(async () => {
    await getChatId();
    queryMsgList(1);
  });

  async function clickVideo(data: any) {
    if (!data || !data.attach) return;
    if (data.attach.includes('https://')) {
      data.loadingVideo = true;
      const res: any = await getVideoFromApp({ attach: data.attach, chatId: unref(chatId) });
      data.loadingVideo = false;
      if (res.code === 0) {
        data.attach = res.data;
        showVideoDialog(data);
      }
      return;
    }
    showVideoDialog(data);
  }

  function showVideoDialog(item: any) {
    activeVideo.value = item;
    mrsDialogRef.value?.closeWindow();
    setTimeout(() => {
      mrsDialogRef.value?.clickVideo();
    }, 0);
  }
  function getAttach(name) {
    return `${getIp()}/imRecord${name}`;
  }

  function getAttachList(attachList) {
    return attachList.map((item) => {
      return `${getIp()}/imRecord${item}`;
    });
  }

  // 查找预览index
  function getAttachIndex(name) {
    const index = attachList.value.findIndex((item) => {
      return item === name;
    });
    return index;
  }

  /**
   * 获取会话id
   */
  async function getChatId() {
    const id = props.info.chatId;
    if (id) {
      chatId.value = id;
      return;
    }

    const { msgBelongId } = props.info;
    const { code, data } = await queryChatIdByGroupId({
      fromIsdn: appConfig.isdn,
      toIsdn: msgBelongId,
    });
    if (code === 0) {
      chatId.value = data;
    }
  }

  /**
   * 查询消息列表
   * 刷新机制不应该每次都查全部，可以是每次查最新的10条，然后插入列表
   * @param pageNum
   */
  async function queryMsgList(pageNum: number) {
    const id = chatId.value;
    const param = {
      chatId: id,
      endTime: unref(searchTime) ? dateUtil.format(searchTime.value[1]) : '',
      keyword: unref(keyword),
      pageNum,
      pageSize: 999_999,
      startTime: unref(searchTime) ? dateUtil.format(searchTime.value[0]) : '',
    };
    const { code, data } = await querySmsHistoryList(param);
    if (code !== 0) {
      return;
    }
    msgList.value = [];
    msgList.value = data.records.reverse();
  }

  function isSelf(data) {
    return data.fromIsdn === appConfig.isdn;
  }

  function closeWindow() {
    emit('closeDialog');
  }
</script>

<template>
  <!-- 用户短信 -->
  <TdFrameBox
    class="msg-history"
    :dragger="true"
    size="another"
    :title="info.title"
    @close-frame-box="closeWindow"
  >
    <TdInput
      v-model="keyword"
      class="message-search"
      :placeholder="t('common.search.searchPlaceholder')"
      type="searchInput"
    />
    <ElDatePicker
      v-model="searchTime"
      :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 2, 1, 23, 59, 59)]"
      :end-placeholder="t('resource.policeTrackPlay.endTime')"
      :start-placeholder="t('resource.policeTrackPlay.beginTime')"
      type="datetimerange"
      @change="queryMsgList(1)"
    />
    <div class="history">
      <template v-for="item in showMsgList" :key="item.sendTime">
        <p v-if="item.showTime" class="time">{{ showTime(item.sendTime) }}</p>
        <div :class="[isSelf(item) ? 'message-from-info-right' : 'message-from-info-left']">
          <p class="info-id">{{ item.fromName }} ({{ item.fromIsdn }})</p>
        </div>
        <div :id="`messageItem-${item.id}`" class="message" :class="{ myself: isSelf(item) }">
          <img alt="" src="@/assets/svg/tree/tree_person.svg" />
          <!-- 视频回显 -->
          <MsgVideoHistory
            v-if="item.type === 'video'"
            :attach="item.thumbnailUrl ? getAttach(item.thumbnailUrl) : ''"
            :loading-video="item.loadingVideo"
            :video-item="item"
            @click="clickVideo(item)"
          />
          <!-- 图片回显 -->
          <ElImage
            v-else-if="item.type === 'image'"
            fit="scale-down"
            :initial-index="getAttachIndex(item.attach)"
            :preview-src-list="getAttachList(attachList)"
            :src="getAttach(item.attach)"
            style="max-width: 350px; margin-bottom: 10px"
          />
          <!-- 音频回显 -->
          <MsgVoiceHistory
            v-else-if="item.type === 'voice'"
            :audio-time="Number(item.audioTime / 1000).toFixed(0)"
            class="text"
            :is-self="isSelf(item)"
            :src="getAttach(item.attach)"
          />
          <!-- 文字回显 -->
          <p v-else class="text">{{ item.content }}</p>
        </div>
      </template>
      <MrsDialog
        ref="mrsDialogRef"
        :video-item="activeVideo"
        :video-url="getAttach(activeVideo.attach)"
      />
    </div>
  </TdFrameBox>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .msg-history {
    height: 516px !important;

    :deep(.frame-box-container) {
      padding: 0 !important;
    }

    .history {
      width: 100%;
      height: calc(100% - 95px);
      padding: 10px;
      overflow-y: auto;

      .time {
        margin-bottom: 10px;
        font-size: 12px;
        font-weight: 400;
        color: rgb(153 206 251 / 100%);
        text-align: center;
      }

      .message {
        display: flex;
        padding: 0 32px 0 0;

        img {
          width: 32px;
          height: 32px;
          margin-right: 22px;
          border-radius: 50%;
        }

        .text {
          position: relative;
          padding: 6px;
          margin-bottom: 10px;
          font-size: 14px;
          font-weight: 400;
          word-break: break-all;
          background: rgb(50 152 226 / 100%);

          &::before {
            position: absolute;
            left: -20px;
            width: 0;
            height: 0;
            content: '';
            border-top: 10px solid transparent;
            border-right: 10px solid transparent;
            border-bottom: 14px solid rgb(50 152 226 / 100%);
            border-left: 10px solid transparent;
            transform: rotate(-90deg);
          }
        }
      }

      .myself {
        flex-direction: row-reverse;
        padding: 0 0 0 32px;

        img {
          margin: 0 0 0 22px;
        }

        .text {
          background-color: rgb(0 199 145 / 100%);

          &::before {
            left: 100%;
            border-bottom: 14px solid rgb(0 199 145 / 100%);
            transform: rotate(90deg);
          }
        }
      }
    }

    .message-search {
      width: 390px;
      margin-top: 10px;
      margin-bottom: 10px;
      margin-left: 10px;
    }

    .message-from-info-left {
      display: flex;
      align-items: center;
      height: 20px;
      padding: 0 0 0 56px;

      .info-id {
        font-size: 12px;
        line-height: 12px;
        color: var(--text-title-second);
      }
    }

    .message-from-info-right {
      display: flex;
      flex-direction: row-reverse;
      align-items: center;
      height: 20px;
      padding: 0 56px 0 0;

      .info-id {
        font-size: 12px;
        line-height: 12px;
        color: var(--text-title-second);
      }
    }

    :deep(.el-date-editor) {
      width: 390px;
      margin-bottom: 10px;
      margin-left: 10px;
    }
  }
</style>
