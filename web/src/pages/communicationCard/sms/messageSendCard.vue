<script lang="ts" setup>
  import type { UploadFile } from 'element-plus';

  import { ref, unref } from 'vue';

  import { uploadImageFile } from '@/api/sms';
  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { useI18n } from '@/hooks';
  import { sendDispatchMMS, sendDispatchSMS } from '@/pages/communicationCard/sms/messageHelper';
  import dataUtil from '@/utils/dataUtil';

  import { throttle } from 'lodash-es';

  const props = defineProps({
    checkedData: {
      default: () => [],
      type: Array,
    },
  });

  const emit = defineEmits(['closeDialog']);

  const { t } = useI18n();
  const chatValue = ref('');
  const mediaRecorder = ref<any>(null);
  const recordedBlobs = ref<any[]>([]);
  const showVoiceBtn = ref(false);
  const showVoiceStart = ref(true);

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

    sendDispatchSMS(props.checkedData, content);
    handleClear();
  }, 1000);

  function handleKeyup(e) {
    if (e.keyCode === 13) {
      handleSend();
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

  async function uploadImage(file: UploadFile) {
    // 上传图片大小限制5M以内
    let size = Math.max(file.size ?? 0, 0);
    size = size / 1024 / 1024;
    if (size > 5) {
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

  // 上传video
  async function uploadVideoFunc(file: any) {
    const param = new FormData();
    param.append('file', file.raw as Blob);
    param.append('userId', appConfig.isdn);
    param.append('fileType', 'video');
    const { code, data } = await uploadImageFile(param);
    if (code === 0) {
      // 上传成功后才能发送彩信通知
      sendDispatchMMS(
        props.checkedData,
        data.data,
        data.msgid,
        data.thumbnailBase64Str.replace(/^data:image\/\w+;base64,/, ''),
      );
    } else {
      Message({ message: t('communication.communicationStatus.msgFail'), type: 'error' });
    }
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
        sendDispatchMMS(
          props.checkedData,
          data.data,
          data.msgid,
          canvasURL.replace(/^data:image\/\w+;base64,/, ''),
        );
      } else {
        Message({ message: t('communication.communicationStatus.msgFail'), type: 'error' });
      }
    });
    image.src = url;
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
          reader.addEventListener('load', (e) => {
            const msgId = Date.now();
            const audioContent: string = e.target?.result as string;
            sendDispatchMMS(
              props.checkedData,
              '',
              msgId,
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

  function showVoice(val) {
    showVoiceBtn.value = val;
    showVoiceStart.value = true;
  }

  function handleClear() {
    chatValue.value = '';
  }

  function handleClose() {
    chatValue.value = '';
    emit('closeDialog');
  }
</script>

<template>
  <TdFrameBox
    class="message-send"
    :dragger="true"
    :title="t('communication.msgFunction.send')"
    @close-frame-box="handleClose"
  >
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
      </ElUpload>
      <Icon class="upload upload-img" name="send_voice" @click="showVoice(true)" />
    </div>
    <div v-if="showVoiceBtn" class="voiceBtn">
      <div v-if="showVoiceStart" class="voice-start">
        <TdButton class="voice-btn-start" icon-name="voice_start" type="icon" @click="voiceStart" />
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
    <div v-else class="send-btn">
      <TdTextarea
        v-model.trim="chatValue"
        :placeholder="t('resource.communication.enter')"
        @keyup="handleKeyup"
      />

      <div class="buttons">
        <TdButton
          class="btn"
          :text="t('communication.msgFunction.clear')"
          type="normal"
          @click.stop="handleClear"
        />
        <TdButton
          class="btn"
          :disable="chatValue === ''"
          :text="t('communication.msgFunction.msgSend')"
          type="normal"
          @click.stop="handleSend"
        />
      </div>
    </div>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  .message-send {
    position: relative;
    width: 320px;
    height: 200px;
    padding-bottom: 52px;

    :deep(.frame-box-container) {
      display: flex;
      flex-direction: column;
    }

    :deep(.td-textarea) {
      flex: 1;

      textarea {
        padding: 0;
        background: inherit !important;
      }
    }

    .buttons {
      position: absolute;
      right: 16px;
      bottom: 6px;
      display: flex;
      align-items: center;
      justify-content: flex-end;
      padding: 10px 0;

      .btn {
        width: 52px;
        height: 24px;
        margin-left: 8px;
      }
    }

    .uploader {
      position: absolute;
      top: 38px;
      left: 10px;
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

    .voiceBtn {
      position: absolute;
      top: 75px;
      left: 10px;
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

    .send-btn {
      :deep(.td-textarea) {
        height: 100%;

        textarea {
          padding: 2px 0;
          margin-top: 18px;
          font-size: 14px;
          font-weight: 400;
          background: inherit !important;
        }
      }
    }
  }
</style>
