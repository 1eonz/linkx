<!--
 * @FileDescription: 群聊消息
 * @Author: 作者信息
 * @Date: 文件创建时间
 * @LastEditors: 最后更新作者
 * @LastEditTime: 最后更新时间
 -->
<script lang="ts" setup>
  import type { UploadFile } from 'element-plus';

  import { computed, onMounted, ref } from 'vue';

  import { queryChatIdByGroupId, uploadImageFile } from '@/api/sms';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { useI18n } from '@/hooks';
  import MsgHistory from '@/pages/notification/components/msgHistory.vue';
  import commOpt from '@/plugins/mspPlayer/commOpt';
  import { useCommunicationStore } from '@/store';
  import dataUtil from '@/utils/dataUtil';

  import { throttle } from 'lodash-es';

  import ChatBox from './chatBox.vue';

  const props = defineProps<{
    activeInfo: any;
    groupId: string;
    id: string;
    isMax?: boolean;
    isSimple?: boolean;
    type: string;
    userList: any[];
  }>();

  const { t } = useI18n();
  const communicationStore = useCommunicationStore();
  const showVoiceBtn = ref(false);
  const showVoiceStart = ref(true);
  const msgValue = ref('');
  const isShowSelect = ref(false);
  const selectedMsgItem = ref<any[]>([]);
  const chatTextAreaRef = ref();
  const chatId = ref('');
  const mediaRecorder = ref<any>(null);
  const recordedBlobs = ref<any[]>([]);
  // const videoRef = ref();
  // const canvasRef = ref();
  // const failMsg = ref<any[]>([]);
  let isFullScreen = false;

  const cardType = computed(() => {
    return props.type === 'group';
  });

  const videoShow = computed(() => {
    return communicationStore.videoCallId.includes(props.groupId);
  });

  onMounted(() => {
    const { groupId } = props;
    const dropFileArea = document.getElementById(`${groupId}dropArea`);
    if (dropFileArea) {
      dropFileArea.addEventListener('drop', fileDropped, false);
      dropFileArea.addEventListener('dragleave', (e) => {
        e.stopPropagation();
        e.preventDefault();
      });
      dropFileArea.addEventListener('dragenter', (e) => {
        e.stopPropagation();
        e.preventDefault();
      });
      dropFileArea.addEventListener('dragover', (e) => {
        e.stopPropagation();
        e.preventDefault();
      });
    }
  });

  /**
   * 发送消息，如果指定有内容，则使用指定内容；如果没有则使用文本框中的内容
   * 短信发送时间间隔大于1秒
   */
  const dealMsg = throttle(() => {
    if (validWordsLength()) {
      return;
    }

    const { communicationExDesc, hasComm } = communicationStore;
    if (!hasComm) {
      Message(communicationExDesc);
      return;
    }
    const { groupId } = props;
    commOpt.sendSMS(groupId, msgValue.value); // 群组短息
    msgValue.value = '';
    chatTextAreaRef.value.focus();
  }, 1000);

  function validWordsLength() {
    if (!msgValue.value) {
      return true;
    }
    if (dataUtil.wordSizeOf(msgValue.value) > 1000) {
      Message({ message: t('communication.communicationTips.msgWord'), type: 'error' });
      return true;
    }
    return false;
  }

  function getIsFullScreen() {
    // 可视区域的高度
    const clientHeight = document.documentElement.clientHeight || document.body.clientHeight;
    // screen是window的属性方法，window.screen可省略window，指的是窗口
    isFullScreen = screen.height === clientHeight;
  }

  function showVoice(val) {
    getIsFullScreen();
    showVoiceBtn.value = val;
    showVoiceStart.value = true;
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
          const { groupId, id } = props;
          const dest = [
            {
              isdn: id || groupId,
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

  async function showHistory(val) {
    showVoiceBtn.value = val;
    const cid = 'msgHistory';

    const { code, data } = await queryChatIdByGroupId({
      groupId: props.id,
    });
    if (code === 0) {
      Dialog({
        cid,
        content: MsgHistory,
        data: {
          cid,
          info: {
            chatId: data,
            title: t('homePage.noticeCenterData.history'),
          },
        },
        offset: ['25%', '20%'],
      });
    }
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
    // failMsg.value.push({
    //   index: showMsgList.value.length,
    //   fromIsdn: appConfig.isdn,
    //   type: 'video',
    //   sendType: 'fail',
    //   attach: '',
    // });
  }

  async function uploadImage(file: UploadFile) {
    if (isFullScreen) {
      setTimeout(() => {
        document.documentElement.requestFullscreen();
      }, 500);
    }

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

  function sendMMS(data, attachThumb?, audioContent?) {
    const { groupId, id } = props;
    const dest = [
      {
        isdn: id || groupId,
        msgid: data.msgid,
      },
    ];
    commOpt.sendMMS(dest, '', data.data, attachThumb || '', audioContent || '');
  }

  // 鼠标点击信息编辑框时触发的
  function preventDrag(event) {
    event.stopPropagation();
  }

  // 发送失败后重发
  function sendAgain() {
    dealMsg();
  }

  // 选择信息
  function selectItem(item, flag) {
    const index = selectedMsgItem.value.indexOf(item);
    if (flag) {
      if (index === -1) {
        selectedMsgItem.value.push(item);
      }
    } else {
      if (index !== -1) {
        selectedMsgItem.value.splice(index, 1);
      }
    }
  }

  // 显示多选后取消
  function cancelBtn() {
    isShowSelect.value = false;
    selectedMsgItem.value = [];
  }

  // 多选后上传时间轴
  function uploadTimeLineBtn() {}

  // 多选后转发
  function transmit() {}

  // 多选后批量删除
  function deleteMsg() {
    //
  }

  // 拖动文件上传
  function fileDropped(e) {
    e.stopPropagation();
    e.preventDefault(); // 必填字段
    const files = e.dataTransfer.files;
    const fileKeys = Object.keys(files);
    const { groupId } = props;
    const { communicationExDesc, hasComm } = communicationStore;
    fileKeys.forEach((key) => {
      // 一次拖动多个文件
      if (!hasComm) {
        Message(communicationExDesc);
        return;
      }
      let msgId: string = generateMsgId();
      const dest = [
        {
          isdn: groupId,
          msgid: msgId,
        },
      ];
      msgId = commOpt.sendMMS(dest, files[key].name, files[key].name) as string; // 群组短息

      const myTime = new Date();
      let hour: number | string = myTime.getHours();
      let minu: number | string = myTime.getMinutes();
      let sec: number | string = myTime.getSeconds();
      if (hour < 10) {
        hour = `0${hour}`;
      }
      if (minu < 10) {
        minu = `0${minu}`;
      }
      if (sec < 10) {
        sec = `0${sec}`;
      }
    });
  }

  // 改变消息中的字段
  function changeChatData() {
    //
  }

  // 回车和换行事件
  function listen(e) {
    if (validWordsLength()) {
      return;
    }

    if (e.keyCode === 13 && e.ctrlKey) {
      msgValue.value += '\n'; // 换行
    } else if (e.keyCode === 13) {
      dealMsg();
      e.preventDefault();
      return false;
    }
  }

  function generateMsgId() {
    const tmpDate = new Date();
    return String(tmpDate.getTime());
  }
</script>

<template>
  <div
    :id="`${groupId}dropArea`"
    :class="{
      'sms-comp': !isMax,
      'video-show': videoShow,
      group: cardType,
      max: isMax,
      'is-simple': isSimple,
    }"
  >
    <ChatBox
      :id="id"
      :active-info="activeInfo"
      :chat-id="chatId"
      :is-show-select="isShowSelect"
      :selected-msg-item="selectedMsgItem"
      :user-list="userList"
      @change-chat-data="changeChatData"
      @select-item="selectItem"
      @send-again="sendAgain"
    />
    <div class="part-bottom">
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
      <div v-if="showVoiceBtn" class="voiceBtn">
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
      <div v-else class="send_btn">
        <textarea
          ref="chatTextAreaRef"
          v-model.trim="msgValue"
          class="ground-glass td-textarea-inner"
          :placeholder="t('resource.communication.enter')"
          @keydown="listen($event)"
          @mousedown="preventDrag($event)"
        ></textarea>

        <TdButton
          v-if="!isShowSelect"
          class="words-count"
          :disable="msgValue === ''"
          :text="t('communication.msgFunction.msgSend')"
          type="normal"
          @click.stop="dealMsg"
        />

        <div class="handle-area">
          <div v-if="isShowSelect">
            <TdButton
              class="p-icon"
              :text="t('communication.msgFunction.uploadTimeline')"
              type="normal"
              @click="uploadTimeLineBtn"
            />
            <TdButton
              class="p-icon"
              :text="t('communication.msgFunction.transmit')"
              type="normal"
              @click="transmit"
            />
            <TdButton class="p-icon" :text="t('common.delete')" type="normal" @click="deleteMsg" />
            <TdButton class="p-icon" :text="t('common.cancel')" type="normal" @click="cancelBtn" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .sms-comp {
    display: flex;
    flex: 1;
    flex-direction: column;
    width: 100%;

    .part-bottom {
      position: relative;
      height: 116px;

      .uploader {
        position: absolute;
        top: 19px;
        left: 5px;
        z-index: 101;
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

      .voiceBtn {
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

      .td-textarea-inner {
        width: 100%;
        height: 100px;
        padding-top: 21px;
        padding-left: 5px;
        margin-top: 16px;
        font-size: 14px;
        color: var(--text-title-second);
        resize: none;

        &:focus {
          color: var(--text-default);
        }
      }

      /* stylelint-disable-next-line selector-pseudo-element-no-unknown */
      .td-textarea-inner::input-placeholder {
        color: #78a8de;
      }

      .words-count {
        position: absolute;
        right: 10px;
        bottom: 10px;
        width: 50px;
        height: 26px;
        line-height: 26px;
      }

      .handle-area {
        display: flex;
        align-items: center;
        justify-content: flex-end;
        width: 100%;
        margin-top: 0 !important;

        .p-icon {
          width: 30px;
          height: 30px;
          margin-top: 5px;
          font-size: var(--font-size-small);
          border: 0;
          fill: var(--icon-color-default);
        }
      }
    }
  }

  .video-show {
    height: 500px;
  }

  .group {
    min-height: 400px;
  }

  .is-simple {
    height: 500px;
  }

  .max {
    display: flex;
    flex: 1;
    flex-direction: column;
    width: 100%;
    overflow-y: scroll;

    .part-bottom {
      position: relative;

      .td-textarea-inner {
        width: 100%;
        height: 90px;
        padding: 0 10px;
        font-size: var(--font-size-small);
        color: var(--text-title-second);
        resize: none;
      }

      /* stylelint-disable-next-line selector-pseudo-element-no-unknown */
      .td-textarea-inner::input-placeholder {
        color: #78a8de;
      }

      .message {
        position: absolute;
        left: 10px;
        color: var(--text-title-second);
      }

      .td-textarea-inner:focus {
        color: var(--text-default) !important;
      }

      .words-count {
        position: absolute;
        top: 10px;
        right: 5px;
        display: inline-flex;
        align-items: center;
        width: 60px;
        margin-right: 5px;
      }

      .handle-area {
        display: flex;
        margin-top: 0 !important;
        background-color: var(--background-input);

        .p-icon {
          font-size: var(--font-size-small);
          background-color: var(--background-input);
          border: 0;
          fill: var(--icon-color-default);
        }
      }
    }
  }
</style>
