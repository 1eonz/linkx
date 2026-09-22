<script setup lang="ts">
  import type { UploadFile } from 'element-plus';

  import { computed, nextTick, onMounted, ref, unref } from 'vue';

  import {
    getVideoFromApp,
    queryChatIdByGroupId,
    querySmsHistoryList,
    uploadImageFile,
  } from '@/api/sms';
  import { updateMsgStatus } from '@/comm/message';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { useDragResize, useEmitter, useI18n } from '@/hooks';
  import commOpt from '@/plugins/mspPlayer/commOpt';
  import { getIp } from '@/utils';
  import dataUtil from '@/utils/dataUtil';

  import { throttle } from 'lodash-es';

  import MrsDialog from './components/mrsDialog.vue';
  import MsgHistory from './components/msgHistory.vue';
  import MsgVideoHistory from './components/msgVideoHistory.vue';
  import MsgVoiceHistory from './components/msgVoiceHistory.vue';
  import { showTime } from './helper';

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
    uid: string;
  }>();
  const emit = defineEmits(['closeDialog']);
  const showVoiceBtn = ref(false);
  const showVoiceStart = ref(true);

  const { t } = useI18n();

  const chatValue = ref('');
  const historyRef = ref();
  const chatTextAreaRef = ref();
  const chatId = ref('');
  const msgList = ref<any[]>([]);
  const mediaRecorder = ref<any>(null);
  const recordedBlobs = ref<any[]>([]);
  const activeVideo = ref<any>({});
  const mrsDialogRef = ref();
  // const videoRef = ref();
  // const canvasRef = ref();
  const failMsg = ref<any[]>([]);
  let total = 0;
  const size = ref<any>('mini');

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

  useEmitter('updateMsgList', updateMsgEvent);

  useDragResize({
    callback() {
      const el = document.getElementById(props.uid) as HTMLElement;
      const width = el.offsetWidth;
      if (width < 412) {
        size.value = 'mini';
      } else if (width >= 412 && width < 698) {
        size.value = 'another';
      } else if (width >= 698 && width < 800) {
        size.value = 'small';
      } else {
        size.value = 'big';
      }
    },
    minH: 514,
    minW: 308,
    uid: props.uid,
  });

  onMounted(async () => {
    await getChatId();
    updateMsgStatus(unref(chatId));
    queryMsgList(1);
  });

  async function clickVideo(data: any) {
    if (!data || !data.attach) return;
    if (data.attach.includes('https://')) {
      data.loadingVideo = true;
      const params = {
        attach: data.attach,
        chatId: unref(chatId),
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

  function voiceStart() {
    showVoiceStart.value = false;
    navigator.mediaDevices
      .getUserMedia({ audio: true })
      .then((stream) => {
        mediaRecorder.value = new MediaRecorder(stream, {
          mimeType: 'audio/webm',
        });
        recordedBlobs.value = [];
        mediaRecorder.value.ondataavailable = (event) => {
          mediaRecorder.value = null;
          if (event.data && event.data.size > 0) {
            recordedBlobs.value.push(event.data);
          }
        };
        mediaRecorder.value.start();
        console.log('Recording started');
      })
      .catch((error) => console.error('Error:', error));
  }

  function voiceEnd() {
    showVoiceStart.value = true;
    mediaRecorder.value.stop();
    console.log('Recording stopped');
    const audio = document.createElement('audio');
    setTimeout(() => {
      const blob = new Blob(recordedBlobs.value, { type: 'audio/mp3' });
      // 上传附件大小限制20M以内
      let size = Math.max(blob.size, 0);
      size = size / 1024 / 1024;
      if (size > 20) {
        Message(t('communication.communicationTips.uploadFailure'));
        return;
      }
      audio.src = URL.createObjectURL(blob);
      // 计算音频时间
      audio.addEventListener(
        'canplay',
        async (e: any) => {
          if (e.target === null) {
            return;
          }
          const firstAudio: HTMLAudioElement = e.target;
          while (firstAudio.duration === Infinity) {
            await new Promise((r) => setTimeout(r, 200));
            firstAudio.currentTime = 10_000_000 * Math.random();
          }
          const reader = new FileReader();
          reader.readAsDataURL(blob);
          const { msgBelongId } = props.info;
          const dest = [
            {
              isdn: msgBelongId,
              msgid: Date.now(),
            },
          ];
          reader.addEventListener('load', (e) => {
            const audioContent: string = e.target?.result as string;
            commOpt.sendMMS(
              dest,
              '',
              '',
              '',
              audioContent.replace(/^data:audio\/\w+;base64,/, ''),
              firstAudio.duration,
            );
          });
        },
        { once: true },
      );
    }, 500);
  }

  function voiceCancel() {
    showVoiceStart.value = true;
    recordedBlobs.value = [];
    mediaRecorder.value.stop();
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
    const index = attachList.value.findIndex((item) => item === name);
    return index;
  }

  async function uploadImage(file: UploadFile) {
    // 上传附件大小限制20M以内
    let size = Math.max(file.size || 0, 0);
    size = size / 1024 / 1024;
    if (size > 20) {
      Message(t('communication.communicationTips.uploadFailure'));
      return;
    }
    const reader: any = new FileReader();
    reader.readAsDataURL(file.raw);
    reader.addEventListener('load', (res: any) => {
      if (!res.target.result) return;
      compressImg(file, res.target.result, size);
    });
  }

  async function uploadVideo(file: UploadFile) {
    showVoiceBtn.value = false;
    // 上传视频大小限制20M以内
    let size = Math.max(file.size || 0, 0);
    size = size / 1024 / 1024;
    if (size > 20) {
      Message(t('communication.communicationTips.uploadFailure'));
      return;
    }
    uploadVideoFunc(file);
  }

  // 压缩图片
  function compressImg(file: any, url: string, size: number) {
    const image = new Image();
    image.addEventListener('load', async () => {
      const canvas = document.createElement('canvas');
      let quality = 1;
      // 图片宽高大于10000或者图片大于2M时,缩略图缩小比例到0.1
      if (image.width > 10_000 || image.height > 10_000 || size > 2) {
        quality = 0.1;
      }
      canvas.width = image.width * quality;
      canvas.height = image.height * quality;
      canvas
        .getContext('2d')
        ?.drawImage(image, 0, 0, image.width * quality, image.height * quality);
      const canvasURL = canvas.toDataURL('image/jpeg', 0.1);
      const param = new FormData();
      param.append('file', file.raw as Blob);
      param.append('userId', appConfig.isdn);
      param.append('fileType', 'image');
      const { code, data } = await uploadImageFile(param);
      if (code === 0) {
        // 上传成功后才能发送彩信通知
        sendMMS(data, canvasURL.replace(/^data:image\/\w+;base64,/, ''));
      } else {
        Message({ message: t('communication.communicationStatus.msgFail'), type: 'error' });
      }
    });
    image.src = url;
  }

  // 上传video
  async function uploadVideoFunc(file: any) {
    const param = new FormData();
    param.append('file', file.raw as Blob);
    param.append('userId', appConfig.isdn);
    param.append('fileType', 'video');
    const { code, data } = await uploadImageFile(param);
    if (code === 0) {
      // 上传成功后才能发送彩信通知
      sendMMS(data, data.thumbnailBase64Str.replace(/^data:image\/\w+;base64,/, ''));
    } else {
      failUpload();
    }
  }

  // 视频上传失败回调
  function failUpload() {
    Message({ message: t('mrs.videosendingfailed'), type: 'error' });
    failMsg.value.push({
      attach: '',
      fromIsdn: appConfig.isdn,
      index: showMsgList.value.length,
      sendType: 'fail',
      type: 'video',
    });
  }

  function sendMMS(data, attachThumb?, audioContent?) {
    const { msgBelongId } = props.info;
    const dest = [
      {
        isdn: msgBelongId,
        msgid: data.msgid,
      },
    ];
    commOpt.sendMMS(dest, '', data.data, attachThumb || '', audioContent || '');
  }

  function showVoice(val) {
    showVoiceBtn.value = val;
    showVoiceStart.value = true;
  }

  function showHistory(val) {
    showVoiceBtn.value = val;
    const cid = 'msgHistory';
    Dialog({
      // shade: true,
      cid,
      content: MsgHistory,
      data: {
        cid,
        info: {
          chatId: unref(chatId),
          title: t('homePage.noticeCenterData.history'),
        },
      },
      offset: ['45%', '20%'],
    });
  }

  /**
   * 消息通知事件
   */
  async function updateMsgEvent(data) {
    if (![data.fromIsdn, data.toIsdn].includes(props.info.msgBelongId)) {
      return;
    }
    if (!unref(chatId)) {
      await getChatId();
    }
    await queryMsgList(1);
    updateMsgStatus(unref(chatId));
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

  function isSelf(data) {
    return data.fromIsdn === appConfig.isdn;
  }

  /**
   * 滚动到底部
   */
  function scrollToBottom() {
    nextTick(() => {
      const container = historyRef.value;
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
   * 短信发送时间间隔大于1秒
   */
  const handleSend = throttle(() => {
    const content = unref(chatValue);

    if (content === '') {
      return;
    }

    if (dataUtil.wordSizeOf(content) > 1000) {
      Message(t('communication.communicationTips.msgWord'));
      return;
    }

    const { msgBelongId } = props.info;
    commOpt.sendSMS(msgBelongId, content);
    chatValue.value = '';
  }, 1000);

  function handleKeyup(e) {
    if (e.keyCode === 13) {
      handleSend();
    }
  }

  function closeWindow() {
    emit('closeDialog');
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
</script>

<template>
  <!-- 用户短信 -->
  <TdFrameBox
    class="message-history"
    :dragger="true"
    :size="size"
    :title="info.title"
    @close-frame-box="closeWindow"
  >
    <div ref="historyRef" class="history" @scroll="handleScroll">
      <template v-for="item in showMsgList" :key="item.sendTime">
        <p v-if="item.showTime" class="time">{{ showTime(item.sendTime) }}</p>
        <div :id="`messageItem-${item.id}`" class="message" :class="{ myself: isSelf(item) }">
          <TdAvatar class="avatar" :url="item.iconFromIsdn || ''" />
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
    <div class="chat-input common-card-default">
      <div class="uploader">
        <ElUpload
          accept=".png,.jpg,.jpeg,.gif"
          action=""
          :auto-upload="false"
          class="upload"
          :on-change="uploadImage"
          :show-file-list="false"
        >
          <Icon class="upload-img" name="send_image" @click="showVoice(false)" />
        </ElUpload>
        <ElUpload
          accept=".mp4"
          action=""
          :auto-upload="false"
          class="upload"
          :on-change="uploadVideo"
          :show-file-list="false"
        >
          <Icon class="upload-img" name="send_video" @click="showVoice(false)" />
          <!-- <video ref="videoRef" style="display: none"></video>
          <canvas ref="canvasRef" style="display: none"></canvas> -->
        </ElUpload>
        <Icon class="upload upload-img" name="send_voice" @click="showVoice(true)" />
        <Icon class="upload upload-img" name="send_history" @click="showHistory(false)" />
      </div>
      <div v-if="showVoiceBtn" class="voice-btn">
        <div v-if="showVoiceStart" class="voice-start">
          <TdButton
            class="voice-btn-start"
            icon-name="voice_start"
            type="icon"
            @click="voiceStart"
          />
          <div class="voice-start-text">{{ t('common.tdcomp.callClick') }}</div>
        </div>
        <div v-if="!showVoiceStart" class="voice-end">
          <TdButton class="voice-btn-end" icon-name="voice_end" type="icon" @click="voiceEnd" />
          <div class="voice-end-text">{{ t('common.tdcomp.sendClick') }}</div>
        </div>
        <div v-if="!showVoiceStart" class="voice-cancel">
          <TdButton
            class="voice-btn-cancel"
            icon-name="voice_cancel"
            type="icon"
            @click="voiceCancel"
          />
          <div class="voice-cancel-text">{{ t('common.cancel') }}</div>
        </div>
      </div>
      <div v-else class="submit-content">
        <TdTextarea
          ref="chatTextAreaRef"
          v-model.trim="chatValue"
          :placeholder="t('resource.communication.enter')"
          @keyup="handleKeyup"
        />
        <div class="submit">
          <TdButton :disable="!chatValue" @click="handleSend">
            {{ t('communication.msgFunction.msgSend') }}
          </TdButton>
        </div>
      </div>
    </div>
  </TdFrameBox>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .message-history {
    height: 600px;

    :deep(.frame-box-container) {
      display: flex;
      flex-direction: column;
      padding: 0 !important;
    }

    .history {
      flex: 1;
      width: 100%;
      min-height: 320px;
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

    .chat-input {
      position: relative;
      width: 100%;
      height: 148px;

      .uploader {
        position: absolute;
        top: 5px;
        left: 5px;
        z-index: 100;
        display: flex;

        .upload {
          margin-right: 5px;
          cursor: pointer;
        }

        .upload-img {
          width: 20px;
          height: 20px;
        }
      }

      .voice-btn {
        position: absolute;
        top: 25px;
        left: 5px;
        z-index: 100;
        display: flex;
        width: 304px;
        height: 105px;

        .voice-start {
          position: relative;
          left: 110px;

          .voice-btn-start {
            width: 68px;
            height: 68px;

            :deep(.icon) {
              width: 68px;
              height: 68px;
            }
          }

          .voice-start-text {
            margin-left: 7px;
            font-size: 12px;
            font-weight: 400;
            line-height: 0px;
            color: rgb(255 255 255 / 100%);
            letter-spacing: 0;
          }
        }

        .voice-end {
          position: relative;
          left: 110px;

          .voice-btn-end {
            width: 68px;
            height: 68px;

            :deep(.icon) {
              width: 68px;
              height: 68px;
            }
          }

          .voice-end-text {
            margin-left: 7px;
            font-size: 12px;
            font-weight: 400;
            line-height: 0px;
            color: rgb(255 255 255 / 100%);
            letter-spacing: 0;
          }
        }

        .voice-cancel {
          position: relative;
          top: 15px;
          left: 160px;

          .voice-btn-cancel {
            width: 55px;
            height: 42px;

            :deep(.icon) {
              width: 55px;
              height: 42px;
            }
          }

          .voice-cancel-text {
            margin-top: 3px;
            margin-left: 15px;
            font-size: 12px;
            font-weight: 400;
            line-height: 0px;
            color: rgb(255 255 255 / 100%);
            letter-spacing: 0;
          }
        }
      }

      .submit-content {
        display: flex;
        flex-direction: column;
        height: 100%;
        padding: 22px 14px 14px;

        :deep(.td-textarea) {
          flex: 1;

          textarea {
            padding: 2px 0;
            font-size: 14px;
            font-weight: 400;
            background: inherit !important;
          }
        }

        .submit {
          display: flex;
          justify-content: flex-end;
          margin-top: 4px;

          :deep(.td-button) {
            width: 52px;
            height: 24px;
            font-size: 14px;
            font-weight: 400;
          }
        }
      }
    }
  }
</style>
