<script setup lang="ts">
  import type { UploadFile } from 'element-plus';

  import { computed, nextTick, onMounted, ref, unref, watch } from 'vue';

  import { checkStatusFile, sendMessage, uploadFile } from '@/api/pim';
  import { uploadImageFile } from '@/api/sms';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import { useEmitter, useI18n } from '@/hooks';
  import { checkUserInGroup } from '@/pages/coordination/common';
  import InputEditor from '@/pages/coordination/emoji/inputEditor.vue';
  import { usePIMStore } from '@/store';
  import dataUtil from '@/utils/dataUtil';

  import { debounce, throttle } from 'lodash-es';

  import GroupDetails from '../chatGroup/groupDetails.vue';
  import ChatGroup from '../chatGroup/index.vue';
  import { handleDeleteMessage, imChatHistory } from '../common';
  import EmojiSelect from '../emoji/emojiSelect.vue';
  import AtMember from './atMember.vue';
  import ChatDetails from './chatDetails.vue';
  import ChatOperation from './chatOperation.vue';
  import ForwardMsg from './forwardMsg.vue';
  import MessageList from './messageList.vue';

  withDefaults(
    defineProps<{
      ability?: any;
      chatId?: string;
      chatType?: string;
      taskAnswer?: boolean;
      titleBackType?: string;
    }>(),
    {
      ability: ['setting', 'groupMember', 'operateRight'],
      chatId: 'chatPanelContent',
      chatType: 'normal',
      taskAnswer: false,
      titleBackType: '',
    },
  );

  defineExpose({ addChatValue, clearInput, toQuoteMsg });

  const { t } = useI18n();
  const PIMStore = usePIMStore();
  const screenshotStatus = ref<boolean>(false);
  const showVoiceBtn = ref(false);
  const originalScrollTop = ref<number>(0); // 保存原始滚动位置

  const isShowGroup = ref(false);
  const chatValue = ref('');

  const chatTextAreaRef = ref();
  const mediaRecorder = ref<any>(null);
  const recordedBlobs = ref<any[]>([]);

  const failMsg = ref<any[]>([]);
  const atMemberRef = ref();
  const showAtList = ref(false); // 群组是否展示@成员列表
  const showCheck = ref(false); // 是否展示勾选框
  const noticeTip = ref('');
  const listRef = ref();
  const filterAtMemberKey = ref('');

  const showMsgList = computed(() => {
    const arr = [...PIMStore.currentChatMessages];
    let time = 0;
    arr.forEach(async (item, index) => {
      const curTime = Date.parse(item.time);
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

      const { from } = item;
      const f = PIMStore.userMap.get(from * 1);
      if (f) {
        Object.assign(item, f);
      } else {
        PIMStore.getUserInfo(from);
      }
      if (item.fromRealUserId) {
        const fromUser: any = await PIMStore.getUserInfo(item.fromRealUserId);
        if (!fromUser) return;

        const from = `(${fromUser?.name || ''})`;
        if (item.name !== fromUser?.name && !item.name?.includes(from)) {
          item.name = `${item.name}${from}`;
        }
      }
    });
    return arr;
  });
  const currentChat = computed(() => PIMStore.currentChat);
  const userId = computed(() => PIMStore.user.id);
  const isGroup = computed(() => currentChat.value.sessionType === 2);
  const quoteMsg = computed(() => PIMStore.quoteMsg);
  const collaboration = computed(() => PIMStore.collaboration);
  // 判断我是不是群主(或者管理员) + 协同岗(协同岗同管理员)
  const isLeader = computed(() => {
    const index = PIMStore.groupMemberActive.findIndex(
      (item) =>
        (item.id === PIMStore.user?.id && [1, 2].includes(item.role)) ||
        (item.id === PIMStore.user?.cooperationUser?.userId && item.type === 2),
    );
    return index !== -1;
  });
  const xietongId = computed(() => PIMStore.user?.cooperationUser?.userId);
  const groupIds = computed(() => PIMStore.user?.cooperationUser?.groupIds || []);
  const isMute = computed(() => {
    return currentChat.value.muteType === 1 && !isLeader.value;
  });
  const isInclude = computed(() => {
    if (currentChat.value.sessionType === 2) {
      return checkUserInGroup(currentChat.value.sessionId);
    }
    return true;
  });

  // 使用防抖处理草稿保存
  const saveDraft = debounce((content) => {
    if (currentChat.value?.sessionId) {
      PIMStore.setDraft(currentChat.value.sessionId, content);
    }
  }, 500);

  // 监听输入内容变化，保存草稿
  watch(chatValue, saveDraft);

  // 监听当前会话变化，恢复草稿
  watch(
    () => PIMStore.chatListActive,
    (newVal, oldVal) => {
      if (
        oldVal && // 保存旧会话的草稿
        chatValue.value
      ) {
        PIMStore.setDraft(oldVal, chatValue.value);
      }
      noticeTip.value = '';
      if (newVal) {
        // 恢复新会话的草稿
        const draft = PIMStore.getDraft(newVal);
        if (draft) {
          chatValue.value = draft;
          nextTick(() => {
            chatTextAreaRef.value?.setContent(draft);
          });
        } else {
          chatValue.value = '';
          nextTick(() => {
            chatTextAreaRef.value?.clear();
          });
        }
      } else {
        // 如果没有活跃会话，清空输入框
        chatValue.value = '';
        nextTick(() => {
          chatTextAreaRef.value?.clear();
        });
      }
    },
  );

  watch(
    () => PIMStore.currentChatMessages,
    (arr) => {
      const res = arr
        .filter((item) => !item.read && Number(item.from) !== userId.value)
        .map((item) => item.msgId);

      if (res.length > 0) {
        handleRead(res);
      }
    },
    { deep: true },
  );

  watch(
    () => PIMStore.currentChat,
    (newVal) => {
      if (newVal?.sessionType === 2) {
        // 当群组信息变化时，重新获取群组成员
        PIMStore.getGroupMembers(newVal.sessionId);
      }
    },
    { deep: true },
  );

  onMounted(() => {
    // 监听群公告事件
    useEmitter('groupNoticeChange', (noticeDto) => {
      handleShowGroupNotice(noticeDto);
    });
  });

  function handleShowGroupNotice(noticeDto) {
    noticeTip.value = currentChat.value?.sessionId === noticeDto.groupId ? noticeDto.content : '';
  }

  function clearInput() {
    chatTextAreaRef.value?.clear();
  }

  function toQuoteMsg(item) {
    listRef.value.toQuoteMsg(item);
  }

  function setFilterAtMemberKey(val) {
    filterAtMemberKey.value = val;
  }

  function addChatValue(person, isAutoAdd) {
    const { id, name } = person;
    const { sessionId } = currentChat.value;
    const resId = id === 'all' ? sessionId : id;
    const atPerson = id === 'all' ? `[@${sessionId}:@all]` : `[@${id}:@${name}]`;
    chatTextAreaRef.value.appendAt(name, atPerson, resId, isAutoAdd);
  }

  function setShowAtList(flag) {
    showAtList.value = flag;
  }

  function handleChange(val, flag) {
    atMemberRef.value?.handleChange(val, flag);
  }

  function handleBlur() {
    atMemberRef.value?.handleBlur();
  }

  function voiceStart() {
    showVoice(true);
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
      })
      .catch((error) => console.error('Error:', error));
  }

  function voiceEnd() {
    showVoice(false);
    if (!mediaRecorder.value) {
      return;
    }
    mediaRecorder.value.stop();
    const audio = document.createElement('audio');
    setTimeout(() => {
      const blob = new Blob(recordedBlobs.value, { type: 'audio/mp3' });
      // 上传附件大小限制100M以内
      let size = Math.max(blob.size, 0);
      size = size / 1024 / 1024;
      if (size > 100) {
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
          reader.addEventListener('load', () => {
            // 小于1秒的语音不发送
            if (firstAudio.duration && firstAudio.duration > 1) {
              transToFileAndSend(blob, `audio${Date.now()}`, 'audio/mp3', firstAudio.duration);
            } else {
              Message('发送语音消息必须大于1秒');
            }
          });
        },
        { once: true },
      );
    }, 500);
  }

  async function transToFileAndSend(blob, fileName, fileType, duration) {
    const file = new window.File([blob], fileName, { type: fileType });
    uploadFileFunc(file, duration);
  }

  // 文件大小限制
  function limitedSize(file: UploadFile) {
    let size = Math.max(file.size || 0, 0);
    size = size / 1024 / 1024;
    if (size > 100) {
      Message('上传附件大小限制100M以内');
      return false;
    }
    return true;
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
      uploadFileFunc(file);
      // compressImg(file, res.target.result, size);
    });
  }

  // 选择上传的文件
  async function handleUploadFile(file: UploadFile) {
    const limit = limitedSize(file);
    if (!limit) {
      return;
    }
    const reader: any = new FileReader();
    reader.readAsDataURL(file.raw);
    reader.addEventListener('load', async (res: any) => {
      if (!res.target.result) return;
      uploadFileFunc(file);
    });
  }

  // 文件上传
  async function uploadFileFunc(file, duration?, videoThumb?) {
    const { name, raw } = file;
    const param = new FormData();
    const index = name.lastIndexOf('.');
    // 有音频时间则直接取文件的类型否则通过文件名后缀获取
    const type = duration ? file.type : name.substr(index + 1, name.length);
    param.append('file', duration ? file : raw);
    param.append('fileName', name);
    param.append('category', '1');
    param.append('fileContentType', type);
    const { code, data } = await uploadFile(param);
    if (code === 0) {
      const size = data.fileSize.fileLength || 0;
      getStatusFile([data.fileId], name, type, size, duration, videoThumb);
    }
  }

  async function uploadScreenShotFunc(file) {
    const { name } = file;
    const param = new FormData();
    const type = file.type;
    param.append('file', file);
    param.append('fileName', name);
    param.append('category', '1');
    param.append('fileContentType', type);
    const { code, data } = await uploadFile(param);
    if (code === 0) {
      const size = data.fileSize.fileLength || 0;
      getStatusFile([data.fileId], name, type, size);
    }
  }

  // 获取缩略图截屏
  async function getVideoThumb(file) {
    const param = new FormData();
    param.append('file', file.raw as Blob);
    param.append('userId', '');
    param.append('fileType', 'video');
    const { code, data, msg } = await uploadImageFile(param);

    if (code === 0) {
      // 获取成功后返回base64再转化成jpg文件
      return base64ToFile(data.thumbnailBase64Str);
      // sendMMS(data, data.thumbnailBase64Str.replace(/^data:image\/\w+;base64,/, ''));
    } else {
      Message({
        message: msg,
        type: 'error',
      });
    }
  }

  // base64转File对象
  function base64ToFile(base64, isScreenShot?) {
    const arr = base64.split(',');
    const mime = isScreenShot ? 'png' : 'image/png';
    const bstr = atob(arr[0]);
    let n = bstr.length;
    const u8arr = new Uint8Array(n);

    while (n--) {
      u8arr[n] = bstr.charCodeAt(n);
    }
    const fileNameTimeStr = isScreenShot
      ? `screenShot${Date.now()}.png`
      : `videoThumb${Date.now()}.png`;
    return new File([u8arr], fileNameTimeStr, { type: mime });
  }

  // 视频封面上传
  async function uploadVideoThumb(file) {
    const result = await getVideoThumb(file);
    if (!result) {
      return;
    }
    const { name } = result;
    const param = new FormData();
    // 文件名后缀获取
    const type = 'image/png';
    param.append('file', result);
    param.append('fileName', name);
    param.append('category', '1');
    param.append('fileContentType', type);
    const { code, data } = await uploadFile(param);
    if (code === 0) {
      await getThumbStatusFile(data.fileId, file);
    }
  }

  async function getThumbStatusFile(fileId, file) {
    const param = {
      fileIds: [fileId],
    };
    const { code, data } = await checkStatusFile(param);
    if (code === 0) {
      data.forEach((item) => {
        const hasReady = ['0', '108', '109'].includes(item.fileStatus);
        // 已归档,可以正常发送彩信
        if (hasReady) {
          // 缩略图上传成功再上传视频
          uploadFileFunc(file, '', fileId);
        }
      });
    }
  }

  // 获取上传文件状态(批量)
  async function getStatusFile(fileIds, name, type, size, duration?, videoThumb?) {
    const param = {
      fileIds,
    };
    const { code, data } = await checkStatusFile(param);
    if (code === 0) {
      data.forEach((item) => {
        const hasReady = ['0', '108', '109'].includes(item.fileStatus);
        // 已归档,可以正常发送彩信
        if (hasReady) {
          sendMMS(fileIds[0], name, type, size, duration, videoThumb);
        }
      });
    }
  }

  async function uploadVideo(file: UploadFile) {
    showVoiceBtn.value = false;
    // 上传视频大小限制20M以内
    // 先上传视频缩略图
    await uploadVideoThumb(file);
  }

  async function sendMMS(fileKey, name?, type?, size?, audioDuration?, videoThumb?) {
    const { sessionId, sessionType } = currentChat.value;
    // 彩信消息基本参数
    const param: any = {
      category: sessionType,
      cMsgId: `${Date.now()}`,
      from: userId.value,
      msg: {
        duration: audioDuration || '', // 可选,音频时间
        fileKey,
        fileName: name,
        fileSize: size,
        fileType: getFileType(type, audioDuration),
        // imageSize: '102_182', // 可选,发送图片时携带w_h
        videoThumb, // 可选,视频封面
      },
      msgType: 2,
      // plaintext: 1,
      to: sessionId,
    };
    if (unref(collaboration) && unref(groupIds).includes(sessionId)) {
      param.fromRealUserId = unref(userId);
      param.from = unref(xietongId);
    }
    const { code, data } = await sendMessage(param);
    // 发送失败回显
    if (code !== 0) {
      failMsg.value.push({
        id: data.cMsgId,
      });
    }
  }

  // 与IM代码判断逻辑保持一致,禁止乱动逻辑
  function getFileType(val, audioDuration?) {
    // 1-image（图片）;2-audio（语音）;3-video（视频）;4- general（其它类型文件）;5-word;6-excel;7-pdf;8-txt;9-sms;10-ppt;11-location;
    // 如果是音频不判断后缀直接返回
    if (audioDuration) {
      return 2;
    }
    switch (val.toLowerCase()) {
      case 'doc':
      case 'docx': {
        return 5;
      } // word
      case 'jpeg':
      case 'jpg':
      case 'png':
      case 'webp': {
        return 1;
      } // image
      case 'mp4': {
        return 3;
      } // video
      case 'pdf': {
        return 7;
      } // pdf
      case 'ppt':
      case 'pptx': {
        return 10;
      } // ppt
      case 'txt': {
        return 8;
      } // txt
      case 'xls':
      case 'xlsx': {
        return 6;
      } // excel
      default: {
        return 4;
      } // general
    }
  }

  function showVoice(val) {
    showVoiceBtn.value = val;
  }

  /**
   * 短信发送时间间隔大于1秒
   */
  const handleSend = throttle(async () => {
    const content = unref(chatValue);

    if (content === '') {
      return;
    }
    chatTextAreaRef.value?.clear();

    if (dataUtil.wordSizeOf(content) > 10_000) {
      Message('不超过10000字节（3333个汉字或10000个英文字母）');
      return;
    }

    if (!currentChat.value.sessionId) {
      return;
    }

    if (showAtList.value) return;

    const { sessionId, sessionType } = currentChat.value;
    // 文本消息基本参数
    const param: any = {
      category: sessionType,
      cMsgId: `${Date.now()}`,
      from: userId.value,
      msg: {
        text: content,
      },
      msgType: 1,
      plaintext: 1,
      to: sessionId,
    };
    if (quoteMsg.value.msgId) {
      param.msg.srcMsgId = quoteMsg.value.msgId;
    }
    // 如果是协同岗的聊天回复
    if (unref(collaboration) && unref(groupIds).includes(sessionId)) {
      param.fromRealUserId = unref(userId);
      param.from = unref(xietongId);
    }
    const { code, data, msg } = await sendMessage(param);
    if (code === 0) {
      // 发送成功后调用successAfter方法
      successAfter();
    } else if (data) {
      // 发送失败添加失败记录
      failMsg.value.push({
        id: data.cMsgId,
      });
    } else {
      Message(msg);
    }
    chatValue.value = '';
    PIMStore.setQuoteMsg({});
  }, 1000);

  // 发送消息成功后的回调方法
  function successAfter() {
    if (currentChat.value?.sessionId) {
      PIMStore.clearDraft(currentChat.value.sessionId);
    }
  }

  // 已读回执
  async function handleRead(arrIds) {
    const { sessionType } = currentChat.value;
    const param = {
      category: sessionType,
      clientMsgId: `${Date.now()}`,
      from: userId.value,
      msg: {
        srcMsgIds: arrIds, // 回执消息id数组
      },
      msgType: 4,
      to: currentChat.value.id,
      // plaintext: 1,
    };
    await sendMessage(param);
  }

  function showConfigBox() {
    const { sessionId, sessionType } = currentChat.value;
    if (sessionType === 2) {
      // 群聊设置
      Dialog({
        cid: `GroupDetails${sessionId}`,
        content: GroupDetails,
        data: {
          onToChatMsg: (item) => {
            toQuoteMsg(item);
          },
        },
        offset: ['40%', '10%'],
      });
    } else {
      // 单聊设置
      Dialog({
        cid: `ChatDetails${sessionId}`,
        content: ChatDetails,
        data: {
          onToChatMsg: (item) => {
            toQuoteMsg(item);
          },
        },
        offset: ['40%', '10%'],
      });
    }
  }

  function showGroupList() {
    isShowGroup.value = !unref(isShowGroup);
  }

  // 转发
  function handleForward(type, data?) {
    const arr = showMsgList.value.filter((item) => {
      return item.check;
    });

    const list: any = data ? [data] : arr;
    if (list.length > 100) {
      Message({ message: '最多选择100条', type: 'error' });
      return;
    }
    Dialog({
      cid: `ForwardMsg`,
      content: ForwardMsg,
      data: {
        forwardMsgList: list,
        onSubmit: () => {
          handleMultiSelect(false);
        },
        type,
      },
      offset: ['40%', '10%'],
    });
  }

  function handleMultiSelect(check: boolean, item?) {
    showCheck.value = check;
    if (!check) {
      showMsgList.value.forEach((item) => {
        item.check = false;
      });
      listRef.value.closeMultiple();
    }
    if (item) {
      changeCheck(item);
    }
  }

  function emojiChange(data) {
    chatTextAreaRef.value.appendEmoji(data);
  }

  function handleNotice() {
    noticeTip.value = '';
    showConfigBox();
  }

  function handleReSend(text) {
    // 清空内容然后赋上重新编辑的内容
    chatTextAreaRef.value.clear();
    chatTextAreaRef.value.setContent(text);
  }

  function handleDel() {
    const arr = unref(showMsgList).filter((item) => item.check);
    handleDeleteMessage(arr).then(() => {
      handleMultiSelect(false);
    });
  }

  function showScreenShot() {
    showVoiceBtn.value = false;

    // 保存当前页面滚动位置
    originalScrollTop.value = window.pageYOffset || document.documentElement.scrollTop;

    // 检查浏览器是否支持 WebRTC
    if (!navigator.mediaDevices || !navigator.mediaDevices.getDisplayMedia) {
      Message({
        message: '当前浏览器不支持截图功能，请使用 Chrome 浏览器或更新浏览器版本',
        type: 'warning',
      });
      return;
    }

    // 检查是否在 HTTPS 环境下
    if (
      location.protocol !== 'https:' &&
      location.hostname !== 'localhost' &&
      !location.hostname.includes('127.0.0.1')
    ) {
      Message({
        message: '截图功能需要在 HTTPS 环境下使用，或使用 localhost 访问',
        type: 'warning',
      });
      return;
    }

    screenshotStatus.value = true;
  }

  function destroyComponent(status) {
    screenshotStatus.value = status;

    // 恢复页面滚动位置
    if (originalScrollTop.value > 0) {
      setTimeout(() => {
        window.scrollTo(0, originalScrollTop.value);
        originalScrollTop.value = 0;
      }, 100);
    }
  }

  function getImg(base64) {
    console.log('截图组件传递的图片信息', base64);
    const result = base64ToFile(base64.replace(/^data:image\/\w+;base64,/, ''), true);
    if (!result) {
      return;
    }
    const reader: any = new FileReader();
    reader.readAsDataURL(result);
    reader.addEventListener('load', (res: any) => {
      if (!res.target.result) return;
      uploadScreenShotFunc(result);
    });
  }

  function changeCheck(item) {
    const index = showMsgList.value.findIndex((i) => i.msgId === item.msgId);
    if (index !== -1) {
      showMsgList.value[index].check = !showMsgList.value[index].check;
    }
  }

  // 处理截图组件错误
  function handleScreenshotError(error) {
    console.error('截图组件错误:', error);
    Message({
      message: '截图功能初始化失败，请检查浏览器权限设置或刷新页面重试',
      type: 'error',
    });
    destroyComponent(false);
  }
</script>

<template>
  <div class="chat-panel">
    <div v-if="!PIMStore.chatListActive" class="glass-light empty">
      <img alt="" src="@/assets/images/pim/pancelEmpty.png" />
    </div>
    <TdFrameBox
      v-else
      class="list-wrapper"
      :dragger="false"
      :is-light="true"
      :show-arrow="true"
      :show-close-btn="false"
      size="big"
      :title="currentChat.name || currentChat.undefinedName || ''"
      :title-back-type="titleBackType"
    >
      <template #rightTop>
        <Icon
          v-if="ability.includes('setting') && isInclude"
          class="config-btn"
          name="chat-config"
          @click="showConfigBox"
        />
      </template>
      <div :id="chatId" class="content">
        <div class="chat-wrapper">
          <!-- 群公告tip -->
          <div v-if="isGroup && noticeTip" class="notice-tip" @click="handleNotice">
            <Icon class="notice-icon" name="notice" prefix="im" />
            <div class="text">{{ noticeTip }}</div>
            <Icon class="notice-icon" name="right_triangle_arrow" />
          </div>

          <!--群组显示/隐藏按钮-->
          <div
            v-if="isGroup && ability.includes('groupMember') && isInclude"
            class="chat-icon"
            :class="isShowGroup ? 'chat-icon-open' : 'chat-icon-hidden'"
            @click="showGroupList"
          ></div>

          <MessageList
            ref="listRef"
            :is-show-group="isShowGroup"
            :message-list="showMsgList"
            @change-check="changeCheck"
            @modify="showConfigBox"
            @multi-select="handleMultiSelect"
            @re-send="handleReSend"
            @transmit="handleForward"
          />

          <div v-if="!showCheck && !isMute && isInclude" class="chat-input">
            <div class="uploader">
              <Icon
                class="upload-img upload chat-pane-icon"
                name="send_screen_shot"
                @click="showScreenShot"
              />
              <EmojiSelect class="upload" @change="emojiChange" @click="showVoice(false)" />
              <ElUpload
                accept=".png,.jpg,.jpeg,.gif"
                action=""
                :auto-upload="false"
                class="upload"
                :on-change="uploadImage"
                :show-file-list="false"
              >
                <img
                  alt=""
                  class="chat-pane-icon"
                  src="@/assets/svg/im/chatPane_image.svg"
                  @click="showVoice(false)"
                />
              </ElUpload>
              <ElUpload
                accept=".mp4"
                action=""
                :auto-upload="false"
                class="upload"
                :on-change="uploadVideo"
                :show-file-list="false"
              >
                <img
                  alt=""
                  class="chat-pane-icon"
                  src="@/assets/svg/im/chatPane_video.svg"
                  @click="showVoice(false)"
                />
              </ElUpload>
              <ElUpload
                accept=".xls,.pdf,.dox,.xls,.rar,.zip,.word,"
                action=""
                :auto-upload="false"
                class="upload"
                :on-change="handleUploadFile"
                :show-file-list="false"
              >
                <img
                  alt=""
                  class="chat-pane-icon"
                  src="@/assets/svg/im/chatPane_file.svg"
                  @click="showVoice(false)"
                />
              </ElUpload>
              <img
                alt=""
                class="chat-pane-icon upload"
                src="@/assets/svg/im/chatPane_voice.svg"
                @click="showVoice(false)"
                @mousedown="voiceStart"
                @mouseout="voiceEnd"
                @mouseup="voiceEnd"
              />
              <img
                alt=""
                class="chat-pane-icon"
                src="@/assets/svg/im/chatPane_history.svg"
                @click="() => imChatHistory(toQuoteMsg)"
              />
            </div>
            <ChatOperation v-if="ability.includes('operateRight')" />
            <!-- 删除 -->
            <div v-if="showVoiceBtn" class="voice-btn"> 请讲话... </div>
            <div v-else class="submit-content">
              <InputEditor
                ref="chatTextAreaRef"
                v-model.trim="chatValue"
                :filter-at-member-key="filterAtMemberKey"
                :show-at-list="showAtList"
                @change="handleChange"
                @handle-blur="handleBlur"
                @send="handleSend"
              />
              <div class="submit">
                <TdButton
                  :active="true"
                  :disable="!chatValue"
                  :is-light="true"
                  :text="t('communication.msgFunction.msgSend')"
                  type="normal"
                  @click="handleSend"
                />
              </div>
            </div>
          </div>

          <div v-if="showCheck" class="chat-input bottom-btns">
            <Icon class="close" name="reject" @click="handleMultiSelect(false)" />
            <div class="btns" @click="handleForward(1)">
              <Icon class="btn" name="forward_one" prefix="im" />
              <span>逐条转发</span>
            </div>
            <div class="btns" @click="handleForward(2)">
              <Icon class="btn" name="forward_many" prefix="im" />
              <span>合并转发</span>
            </div>
            <div class="btns" @click="handleDel">
              <Icon class="btn" name="remove" prefix="im" />
              <span>删除</span>
            </div>
            <!-- <div class="btns">
              <Icon class="btn" name="save" prefix="im" />
              <span>保存</span>
            </div> -->
          </div>

          <div v-if="currentChat.isDel" class="mask"> 该群组已经被解散 </div>
          <div v-else-if="isMute" class="mask">禁言中</div>
          <div v-else-if="!isInclude" class="mask">你已退出群聊</div>
        </div>

        <ChatGroup
          v-show="isShowGroup && isGroup && (currentChat.muteType === 2 || isLeader)"
          :notice-tip="noticeTip"
        />

        <AtMember
          v-if="isGroup"
          ref="atMemberRef"
          :chat-id="chatId"
          :chat-type="chatType"
          @add-chat-value="addChatValue"
          @set-filter-at-member-key="setFilterAtMemberKey"
          @set-show-at-list="setShowAtList"
        />

        <!--截图组件-->
        <screen-short
          v-if="screenshotStatus"
          @destroy-component="destroyComponent"
          @get-image-data="getImg"
          @webrtc-error="handleScreenshotError"
        />
      </div>
    </TdFrameBox>
  </div>
</template>

<style scoped lang="less">
  .chat-panel {
    flex: 1;

    .config-btn {
      width: 18px;
      height: 18px;
      margin-bottom: 5px;
      cursor: pointer;
    }

    :deep(.frame-box-container) {
      display: flex;
      flex-direction: column;
      padding: 0 !important;
    }

    .list-wrapper {
      width: 100%;
      height: 100%;

      .content {
        position: relative;
        display: flex;
        width: 100%;
        height: 100%;
      }

      .chat-wrapper {
        position: relative;
        width: 100%;

        .notice-tip {
          position: absolute;
          top: 8px;
          left: 8px;
          z-index: 100;
          display: flex;
          align-items: center;
          width: calc(100% - 16px);
          height: 36px;
          padding: 0 8px;
          cursor: pointer;
          background: linear-gradient(
            90deg,
            rgb(255 255 255 / 60%) -14.933%,
            rgb(223 222 222 / 60%) -14.933%,
            rgb(226 225 225 / 60%) 100%
          );
          border: 1px solid rgb(77 147 201 / 60%);
          border-radius: 2px;

          .notice-icon {
            width: 14px;
            height: 14px;
            color: rgb(142 145 157);
          }

          .text {
            width: calc(100% - 30px);
            margin-left: 6px;
            overflow: hidden;
            font-size: 12px;
            font-weight: 400;
            color: rgb(26 26 26);
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }

        .chat-icon {
          position: absolute;
          top: 45%;
          right: 0;
          width: 20px;
          height: 40px;
          cursor: pointer;
        }

        .chat-icon-open {
          background: url('@/assets/images/pim/group_list_hidden.png') no-repeat center center;
          background-size: 20px, 40px;
        }

        .chat-icon-hidden {
          background: url('@/assets/images/pim/group_list_open.png') no-repeat center center;
          background-size: 20px, 40px;
        }
      }

      .chat-input {
        position: relative;
        width: 100%;
        height: 206px;
        background: var(--submit-content-bg);
        backdrop-filter: blur(27px);
        border: 1px solid var(--chat-input-color);
        border-left: none;

        .chat-pane-icon {
          width: 32px;
          height: 32px;
          cursor: pointer;
          filter: var(--svg-filter);
          transition: fill 0.3s ease;
        }

        .chat-pane-icon:hover {
          filter: brightness(0) saturate(100%) invert(39%) sepia(99%) saturate(2000%)
            hue-rotate(200deg) brightness(90%) contrast(90%);
        }

        .uploader {
          position: absolute;
          top: 5px;
          left: 5px;
          z-index: 100;
          display: flex;

          .upload {
            margin-right: 12px;
            cursor: pointer;
          }

          .upload-img {
            width: 32px;
            height: 32px;
            cursor: pointer;
          }
        }

        .voice-btn {
          position: absolute;
          top: 70px;
          left: 45%;
          z-index: 100;
          display: flex;
          width: 304px;
          height: 105px;
          font-size: 14px;
          font-weight: 600;
          color: #99cefb;
        }

        .submit-content {
          position: relative;
          display: flex;
          flex-direction: column;
          height: 100%;
          padding: 32px 14px 14px;
          background: var(--submit-content-bg);

          .submit {
            display: flex;
            justify-content: flex-end;
            margin-top: 4px;

            :deep(.td-button) {
              width: 52px;
              height: 24px;
              font-size: 14px;
              font-weight: 400;
              background: var(--text-switch-bg) !important;
            }
          }
        }
      }

      .bottom-btns {
        position: relative;
        display: flex;
        align-items: center;
        justify-content: space-around;

        .close {
          position: absolute;
          top: 15px;
          right: 15px;
          width: 12px;
          height: 12px;
          color: var(--item-text-color);
          cursor: pointer;
        }

        .btns {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          cursor: pointer;

          .btn {
            width: 23px;
            height: 23px;
            filter: var(--svg-filter);
          }

          span {
            margin-top: 5px;
            font-size: 12px;
            font-weight: 400;
            color: var(--item-text-color);
          }
        }
      }

      .mask {
        position: absolute;
        bottom: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        width: 100%;
        height: 220px;
        font-size: 14px;
        font-weight: 500;
        color: var(--item-text-color);
        cursor: not-allowed;
        background: var(--message-list-bg);
        backdrop-filter: blur(28px);
        border: 1px solid rgb(255 255 255 / 20%);
      }
    }

    .empty {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 100%;
      height: 100%;
      background: var(--dark-back);

      img {
        width: 106px;
        height: 92px;
      }
    }
  }

  :deep(.glass-light) {
    border: none;
  }

  // 截图时防止页面滚动位置变化
  :deep(.screen-shot-container) {
    position: fixed !important;
    top: 0 !important;
    left: 0 !important;
    z-index: 9999 !important;
  }

  // 截图时固定页面滚动位置
  :deep(.screen-shot-active) {
    position: fixed !important;
    width: 100% !important;
    overflow: hidden !important;
  }
</style>
