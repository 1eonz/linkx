<script lang="ts" setup>
  import { computed, onMounted, ref, watch } from 'vue';

  import { getVideoFromApp } from '@/api/sms';
  import { appConfig, messageIconConfig } from '@/config';
  import { useI18n } from '@/hooks';
  import MrsDialog from '@/pages/notification/components/mrsDialog.vue';
  import MsgVideoHistory from '@/pages/notification/components/msgVideoHistory.vue';
  import MsgVoiceHistory from '@/pages/notification/components/msgVoiceHistory.vue';
  import { showTime } from '@/pages/notification/helper';
  import { getIp } from '@/utils';
  import dataUtil from '@/utils/dataUtil';

  const props = defineProps<{
    attachList: string[];
    chatId?: string;
    isShowSelect: boolean;
    item: any;
  }>();
  const emit = defineEmits(['selectItem', 'changeChatData', 'sendAgain']);

  const { t } = useI18n();
  const isSelect = ref(false);
  const imgPath = 'src/assets/images/message/';

  const isUser = computed(() => props.item.fromIsdn === appConfig.isdn);
  const activeVideo = ref<any>({});
  const mrsDialogRef = ref();

  watch(isSelect, (val) => {
    emit('selectItem', props.item, val);
  });

  // 关闭多选的按钮时，把已选择的取消
  watch(
    () => props.isShowSelect,
    (val) => {
      if (!val) {
        isSelect.value = false;
      }
    },
  );

  onMounted(() => {});

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
    const index = props.attachList.findIndex((item) => {
      return item === name;
    });
    return index;
  }

  async function clickVideo(data: any) {
    if (!data || !data.attach) return;
    if (data.attach.includes('https://')) {
      data.loadingVideo = true;
      const params = {
        attach: data.attach,
        chatId: props.chatId,
      };
      const res: any = await getVideoFromApp(params);
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

  // 改变信息数据
  function changeChatData(key, value) {
    emit('changeChatData', props.item.id, key, value);
  }
  // 文件图标
  function getFileIcon(fileName) {
    const fileIconName = messageIconConfig[dataUtil.getFileExtension(fileName)];
    return fileIconName ? `${imgPath + fileIconName}.png` : `${imgPath}UNKNOWN.png`;
  }
  // 发送状态文字描述
  function getStateDesc(status) {
    switch (status) {
      case 'fail': {
        return t('communication.communicationStatus.msgFail');
      }
      case 'sending': {
        return t('communication.communicationStatus.msgSending');
      }
      case 'success': {
        return t('communication.communicationStatus.msgSuccess');
      }
    }
  }
  // 发送状态(确定未读的图标)
  function getStateIcon(status) {
    switch (status) {
      case 'fail': {
        return `${imgPath}MSG_FAIL.png`;
      }
      case 'read': {
        return `${imgPath}MSG_SUCCESS.png`;
      }
      case 'sending': {
        return `${imgPath}LOADING.gif`;
      }
      case 'success': {
        return `${imgPath}MSG_SUCCESS.png`;
      }
      case 'unread': {
        return `${imgPath}MSG_UNREAD.png`;
      }
    }
  }

  // 显示类型
  function showType(item, type) {
    return item.type === type;
  }
  // 重新下载
  function reDownload() {}
  // 接收-1、写入本地文件；2、改变服务器上localPath字段
  function saveInDefaultPath() {
    changeChatData('alreadyDownloaded', true);
  }
  // 另存为-1、写入本地文件；2、改变服务器上localPath字段
  function saveInCertainPath() {}
  // 发送失败的信息可重新发送
  function sendAgain(item) {
    if (item.status === 'fail') {
      emit('sendAgain', item);
    }
  }
  // 打开文件
  function openFile() {}
  // 打开文件
  function openExplorer() {}
</script>

<template>
  <div
    :id="`messageItem-${item.id}`"
    class="message-item"
    :class="[isUser ? 'message-item-right' : 'message-item-left']"
  >
    <p v-if="item.showTime" class="info-time">
      {{ showTime(item.sendTime) }}
    </p>

    <div v-if="!isUser" class="message-from-info">
      <p class="info-id">{{ item.fromName }} ({{ item.fromIsdn }})</p>
    </div>

    <div class="message-box">
      <TdCheckbox v-if="isShowSelect" v-model="isSelect" />

      <div class="message-content">
        <!-- 头像 -->
        <TdAvatar class="avatar" :url="item.iconFromIsdn || ''" />
        <!-- 消息内容 -->
        <div
          class="message-content-inner"
          :class="[isUser ? 'right-background' : 'left-background']"
        >
          <!--文字-->
          <div v-if="showType(item, 'text')" class="message-content-text">
            <span>{{ item.content }}</span>
          </div>
          <!--文件-->
          <div v-if="showType(item, 'file')" class="message-content-file">
            <div class="file-name-icon">
              <div class="file-name">
                <p>{{ item.content }}</p>
                <p>{{ getStateDesc(item.status) }}</p>
              </div>
              <img class="file-icon" :src="getFileIcon(item.content)" />
            </div>
            <div v-if="isUser || item.alreadyDownloaded" class="file-btn">
              <TdButton
                class="btn btn-three-width"
                :class="[isUser ? 'right-btn' : 'left-btn']"
                :text="t('communication.msgFunction.openFile')"
                @click="openFile()"
              />
              <div style="width: 1px"></div>
              <TdButton
                class="btn btn-three-width"
                :class="[isUser ? 'right-btn' : 'left-btn']"
                :text="t('communication.msgFunction.openFiles')"
                @click="openExplorer()"
              />
              <div style="width: 1px"></div>
              <TdButton
                class="btn btn-three-width"
                :class="[isUser ? 'right-btn' : 'left-btn']"
                :text="t('communication.msgFunction.reload')"
                @click="reDownload()"
              />
            </div>
            <div v-if="!isUser && !item.alreadyDownloaded" class="file-btn">
              <TdButton
                class="btn btn-two-width"
                :class="[isUser ? 'right-btn' : 'left-btn']"
                :text="t('communication.msgFunction.receive')"
                @click="saveInDefaultPath()"
              />
              <div style="width: 1px"></div>
              <TdButton
                class="btn btn-two-width"
                :class="[isUser ? 'right-btn' : 'left-btn']"
                :text="t('communication.msgFunction.saveAs')"
                @click="saveInCertainPath()"
              />
            </div>
          </div>
          <!--图片-->
          <div v-if="showType(item, 'image')" class="message-content-image">
            <ElImage
              fit="scale-down"
              :initial-index="getAttachIndex(item.attach)"
              :preview-src-list="getAttachList(attachList)"
              :src="getAttach(item.attach)"
              style="max-width: 350px"
            />
          </div>
          <!--音频-->
          <MsgVoiceHistory
            v-if="showType(item, 'voice')"
            :audio-time="Number(item.audioTime / 1000).toFixed(0)"
            class="message-content-voice"
            :is-self="isUser"
            :src="getAttach(item.attach)"
          />
          <!--视频-->
          <MsgVideoHistory
            v-if="showType(item, 'video')"
            :attach="item.thumbnailUrl ? getAttach(item.thumbnailUrl) : ''"
            class="message-content-video"
            :loading-video="item.loadingVideo"
            :video-item="item"
            @click="clickVideo(item)"
          />
        </div>
        <!-- 消息发送状态 -->
        <img
          v-if="isUser && item.status !== 'success'"
          class="status-img"
          :src="getStateIcon(item.status)"
          @click="sendAgain(item)"
        />
      </div>
    </div>
  </div>
  <MrsDialog
    ref="mrsDialogRef"
    :video-item="activeVideo"
    :video-url="getAttach(activeVideo.attach)"
  />
</template>

<style lang="less" scoped>
  .message-item {
    display: flex;
    flex-direction: column;
    width: 100%;
    margin-bottom: 16px;
  }

  .info-time {
    width: 100%;
    margin-bottom: 8px;
    font-size: 12px;
    line-height: 12px;
    color: var(--text-title-second);
    text-align: center;
  }

  .message-from-info {
    display: flex;
    align-items: center;
    height: 20px;

    .info-id {
      font-size: 12px;
      line-height: 12px;
      color: var(--text-title-second);
    }
  }

  .message-box {
    padding: 0 32px 0 0;

    .message-content {
      display: flex;
      flex: 1;
      align-items: top;

      .status-img {
        width: 12px;
        height: 12px;
        margin: auto 6px;
      }

      .avatar {
        width: 32px;
        height: 32px;
        margin-right: 10px;
      }

      .message-content-inner {
        position: relative;
        font-size: 14px;
        font-weight: 400;
        border-radius: 2px;

        &::before {
          position: absolute;
          top: -1px;
          left: -10px;
          width: 0;
          height: 0;
          content: '';
          border-top: 5px solid transparent;
          border-right: 5px solid transparent;
          border-bottom: 7px solid rgb(50 152 226 / 100%);
          border-left: 5px solid transparent;
          transform: rotate(-90deg);
        }

        .message-content-voice {
          margin-top: 5px;
        }

        .message-content-text {
          max-width: 200px;
          padding: 5px 8px;

          span {
            color: #fff;
            word-break: break-all;
            word-wrap: break-word;
            white-space: pre-wrap;
          }
        }

        .message-content-image {
          width: auto;
          height: auto;
          padding: 6px;
        }

        .message-content-video {
          padding: 5px 8px;
        }

        .message-content-file {
          width: 300px;
          height: auto;

          .file-name-icon {
            display: flex;
            justify-content: center;

            .file-name {
              display: flex;
              flex-direction: column;
              margin-top: 18px;
            }

            .file-icon {
              width: 80px;
              height: 80px;
            }
          }

          .file-btn {
            display: flex;

            .btn {
              height: 30px;
              border-radius: 5px;
            }

            .btn-three-width {
              width: 33.3%;
            }

            .btn-two-width {
              width: 50%;
            }

            .right-btn {
              background-color: rgb(47 146 227);
            }

            .left-btn {
              background-color: #354c66;
            }
          }
        }
      }

      .left-background {
        background: rgb(50 152 226 / 100%);
      }

      .right-background {
        background-color: rgb(0 199 145 / 100%);

        &::before {
          left: 100%;
          border-bottom: 7px solid rgb(0 199 145 / 100%);
          transform: rotate(90deg);
        }
      }
    }
  }

  .message-item-left {
    .message-from-info {
      padding: 0 0 0 40px;
    }

    .message-box {
      display: flex;
    }
  }

  .message-item-right {
    .message-from-info {
      flex-direction: row-reverse;
      padding: 0 56px 0 0;

      .info-time {
        margin: 0 8px 0 0;
      }
    }

    .message-box {
      display: flex;
      padding: 0 0 0 32px;

      .avatar {
        margin: 0 0 0 10px;
      }

      .message-content {
        flex-direction: row-reverse;
      }
    }
  }
</style>
