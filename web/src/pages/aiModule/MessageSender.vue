<!-- web/src/pages/aiModule/components/MessageSender/index.vue -->

<template>
  <div class="message-sender">
    <!-- 已选文件展示区域 -->
    <div v-if="selectedFiles.length > 0" class="selected-files">
      <div v-for="(file, index) in selectedFiles" :key="file.uid" class="file-item">
        <!-- 图片预览 -->
        <div v-if="file.type === 'image'" class="file-preview image-preview">
          <img :src="file.url" alt="预览" />
        </div>

        <!-- 视频预览 -->
        <div v-else-if="file.type === 'video'" class="file-preview video-preview">
          <video :src="file.url" />
          <div class="play-icon">
            <el-icon><VideoPlay /></el-icon>
          </div>
        </div>

        <!-- 音频预览 -->
        <div v-else-if="file.type === 'audio'" class="file-preview audio-preview">
          <el-icon class="audio-icon"><Headset /></el-icon>
        </div>

        <!-- 文档预览 -->
        <div v-else class="file-preview document-preview">
          <el-icon class="doc-icon"><Document /></el-icon>
          <span class="file-ext">{{ file.ext }}</span>
        </div>

        <!-- 删除按钮 -->
        <div class="file-delete" @click="removeFile(index)">
          <el-icon><Close /></el-icon>
        </div>

        <!-- 文件名 -->
        <span class="file-name">{{ file.name }}</span>
        <!-- 上传进度 -->
        <div v-if="file.isUploading" class="upload-progress">
          <el-progress
            type="circle"
            :percentage="file.progress"
            :show-text="false"
            :stroke-width="15"
          />
        </div>
      </div>
    </div>
    <!-- 输入区域 -->
    <div class="input-area">
      <!-- 多行文本输入框 -->
      <textarea
        ref="textareaRef"
        v-model="inputValue"
        class="chat-textarea"
        :placeholder="isRecording ? '语音信息录入中...' : '输入消息，按 Enter 发送，Shift + Enter 换行...'"
        :rows="3"
        :maxlength="9999"
        @keydown="handleKeydown"
        :disabled="isRecording"
      />
      <div class="input-area-bottom">
        <!-- 文件上传按钮（左侧） -->
        <div v-if="isSupportFile && selectedFiles.length < 1" class="upload-btn" @click="selectFile" title="上传文件">
          <el-icon :size="24"><Paperclip /></el-icon>
        </div>
        <div class="input-area-bottom-right">
          <!-- 语音录制按钮（右侧）  -->
          <div v-if="isSupportAudio">
            <div v-if="isRecording" class="recording-status">
              <div class="recording-indicator">
                <span class="recording-dot"></span>
                <span class="recording-time">{{ formatRecordingTime(recordingTime) }}</span>
              </div>
              <div
                class="voice-btn cancel"
                @click="cancelRecording"
                title="取消录音"
              >
                <el-icon :size="20"><Close /></el-icon>
              </div>
              <div
                class="voice-btn confirm"
                @click="stopRecording"
                title="完成录音"
              >
                <el-icon :size="20"><Microphone /></el-icon>
              </div>
            </div>

            <div
              v-else
              class="voice-btn recording-def"
              @click="startRecording"
              title="点击录音"
            >
              <el-icon :size="24"><Microphone /></el-icon>
            </div>
          </div>

          <!-- 发送按钮 -->
          <div
            v-if="showSendBtn"
            class="send-btn"
            :class="{ disabled: isLoading }"
            @click="sendMessage"
          >
            <el-icon :size="24"><Promotion /></el-icon>
          </div>

          <!-- 暂停按钮 -->
          <div
            v-else-if="!isLoading && !isPaused && (isStreaming || isReceiving || isDisplaying)"
            class="pause-btn"
            @click="emit('pause')"
          >
            <el-icon :size="24"><VideoPause /></el-icon>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
  import { ElMessage } from 'element-plus';
  import {
    Paperclip,
    Microphone,
    Promotion,
    VideoPause,
    Close,
    Document,
    Headset,
    VideoPlay,
  } from '@element-plus/icons-vue';
  import { uploadFile, getFileCapabilities } from '@/api/ai';

  // ========== Props ==========
  const props = defineProps<{
    isLoading?: boolean;
    isPaused?: boolean;
    isStreaming?: boolean;
    isReceiving?: boolean;
    isDisplaying?: boolean;
    agentId?: string;
    userId?: string;
  }>();

  // ========== Emits ==========
  const emit = defineEmits<{
    (e: 'send', value: { content: string; fileIds: string, files: any[] }): void;
    (e: 'pause'): void;
    (e: 'sendFile', value: any): void;
  }>();

  // ========== 状态定义 ==========

  // 输入内容
  const inputValue = ref('');

  // 已选择的文件列表
  const selectedFiles = ref<any[]>([]);

  // 文件上传成功后的 sessionId 列表
  const sessionIdList = ref<string[]>([]);

  // 上传请求控制器映射
  const uploadControllers = new Map<string, AbortController>();

  // ========== 语音录制状态 ==========
  const isRecording = ref(false);
  const recordingTime = ref(0);
  const pendingRecording = ref(false); // 正在请求麦克风权限
  const shouldStopAfterStart = ref(false); // 是否在权限获取后立即停止

  let recordingTimer: number | null = null;
  let mediaRecorder: MediaRecorder | null = null;
  let audioChunks: Blob[] = [];
  let audioStream: MediaStream | null = null;
  let recordingCancelled = false;

  const formatRecordingTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  // ========== 文件能力配置 ==========
  const fileCapabilities = ref<any>({
    audio: false,
    audioType: [] as string[],
    video: false,
    videoType: [] as string[],
    image: false,
    imageType: [] as string[],
    document: false,
    documentType: [] as string[],
    fileInterfaceId: 0,
    hasFileInterface: false,
  });

  // ========== 计算属性 ==========

  // 是否支持音频
  const isSupportAudio = computed(() => {
    return fileCapabilities.value.audio;
  });

  // 是否支持文件
  const isSupportFile = computed(() => {
    return (
      fileCapabilities.value?.audio ||
      fileCapabilities.value?.document ||
      fileCapabilities.value?.image ||
      fileCapabilities.value?.video
    );
  });

  // 是否显示发送按钮
  const showSendBtn = computed(() => {
    return inputValue.value.trim() || selectedFiles.value.length > 0;
  });
  // 支持的文件类型
  const acceptList = ref<any>([]);
  // ========== 获取文件能力配置 ==========
  async function fetchFileCapabilities(agentId: string) {
    try {
      const { data, code } = await getFileCapabilities(agentId);
      if (code === 0) {
        const {
          audio,
          audioType,
          video,
          videoType,
          image,
          imageType,
          document,
          documentType,
        } = data as any;
        // 清空原有列表
        acceptList.value = [];
        const newAcceptTypes: string[] = [];
        // 只有audio为true时，才添加音频类型
        if (audio && audioType) {
          newAcceptTypes.push(...audioType);
        }

        // 只有video为true时，才添加视频类型
        if (video && videoType) {
          newAcceptTypes.push(...videoType);
        }

        // 只有image为true时，才添加图片类型
        if (image && imageType) {
          newAcceptTypes.push(...imageType);
        }

        // 只有document为true时，才添加文档类型
        if (document && documentType) {
          newAcceptTypes.push(...documentType);
        }
        acceptList.value = newAcceptTypes;

        fileCapabilities.value = data;
      }
    } catch (error) {
      console.error('获取文件能力配置失败:', error);
    }
  }

  // ========== 键盘事件处理 ==========
  function handleKeydown(e: KeyboardEvent) {
    // Enter 发送，Shift + Enter 换行
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  }

  // ========== 发送消息 ==========
  function sendMessage() {
    if (!inputValue.value.trim() && sessionIdList.value.length === 0) return;
    if (props.isLoading) return;

    emit('send', {
      content: inputValue.value.trim(),
      fileIds: sessionIdList.value.join(','),
      files: selectedFiles.value.map(_=>({
        attachement_path: _.attachement_path,
        sessionId: _.sessionId,
      })),
    });

    // 清空输入
    inputValue.value = '';
    selectedFiles.value = [];
    sessionIdList.value = [];
  }

  // ========== 文件选择 ==========
  async function selectFile() {
    if (selectedFiles.value.length >= 1) {
      ElMessage.warning('单次提问仅支持上传一个附件');
      return;
    }

    await fetchFileCapabilities(props.agentId as string)

    const input = document.createElement('input');
    input.type = 'file';
    input.multiple = false;

    input.accept = acceptList.value.join(',');

    input.onchange = async (e) => {
      const files = (e.target as HTMLInputElement).files;
      if (files && files.length > 0) {
        await handleFiles(Array.from(files));
      }
    };

    input.click();
  }

  // ========== 处理文件 ==========
  function getErrorInfo(file: File): string | null {
    const { ext, label, isExtUpload, isClassifyUpload } = getFileClassifyByFileInfo(file);
    
    // 如果当前分类不支持上传
    if(!isClassifyUpload) return `暂不支持上传${label}文件`;

    // 如果支持上传该类型文件 直接返回
    if(isExtUpload) return null;

    return `暂不支持该${label}文件格式${ext}`
  }

  // 通过文件信息及配置获取文件类型 后缀
  function getFileClassifyByFileInfo(file: File) {
    const fileType = file.type
    const ext = '.' + file.name.split('.').pop()?.toLowerCase();
    // 支持的文件分类
    const supportedClassify = {
      image: {
        value: 'image',
        label: '图片',
        types: fileCapabilities.value.imageType || [],
      },
      audio: {
        value: 'audio',
        label: '音频',
        types: fileCapabilities.value.audioType || [],
      },
      video: {
        value: 'video',
        label: '视频',
        types: fileCapabilities.value.videoType || [],
      },
      document: {
        value: 'document',
        label: '文档',
        types: fileCapabilities.value.documentType || [],
      },
    };

    const supportedClassifyList: {type: string, value: string}[]  = [] // 将 supportedClassify转化为 {type,value}[]
    for (const key in supportedClassify) {
      const item = supportedClassify[key];
      item.types.forEach(type => {
        supportedClassifyList.push({
          type,
          value: item.value,
        })
      })
    }

    // 查找是否有匹配的文件分类
    const classifyItem = ext ? supportedClassifyList.find(item => item.type === ext) : null;

    let classify

    // 如果文件类型存在 直接通过文件类型判断文件分类
    if (fileType) {
        // 不属于默认四种类型分类中的 都默认为文档类型
        classify = supportedClassify[fileType.split('/')[0]]?.value || supportedClassify.document.value;
    } else {
        // 如果文件类型不存在 则通过文件后缀判断文件分类
        if (ext) {
          classify = classifyItem?.value || supportedClassify.document.value;
        } else {
          classify = supportedClassify.document.value;
        }
    }

    // console.log({
    //   classify,// 文件分类
    //   ext,// 文件后缀
    //   label: supportedClassify[classify].label,// 文件分类标签
    //   isExtUpload: !!classifyItem, // 是否支持上传该类型文件后缀
    //   isClassifyUpload: fileCapabilities.value[classify], // 是否支持上传该类型文件分类
    //   fileCapabilities: fileCapabilities.value,
    // })

    return {
      classify,// 文件分类
      ext,// 文件后缀
      label: supportedClassify[classify].label,// 文件分类标签
      isExtUpload: !!classifyItem, // 是否支持上传该类型文件后缀
      isClassifyUpload: fileCapabilities.value[classify], // 是否支持上传该类型文件分类
      classifyItem, // 匹配的文件分类项
    }
  }

  async function handleFiles(files: File[]) {
    if (selectedFiles.value.length >= 1) {
      ElMessage.warning('单次提问仅支持上传一个附件');
      return;
    }

    for (const file of files) {
      const errorInfo = getErrorInfo(file);
      if (errorInfo) {
        ElMessage.error(errorInfo);
        continue;
      }

      const { classify: fileType} = getFileClassifyByFileInfo(file)
      const url = URL.createObjectURL(file);
      const uid = generateUID();
      const ext = '.' + file.name.split('.').pop()?.toLowerCase();
      const fileItem = {
        uid,
        file,
        name: file.name,
        size: file.size,
        type: fileType,
        ext,
        url,
        progress: 0,
        isUploading: true,
        sessionId: '',
      };

      selectedFiles.value.push(fileItem);

      // 开始上传
      uploadFileWithProgress(fileItem, uid);
    }
  }

  // ========== 带进度的文件上传 ==========
  async function uploadFileWithProgress(fileItem: any, uid: string) {
    const controller = new AbortController();
    uploadControllers.set(uid, controller);

    try {
      const formData = new FormData();
      formData.append('file', fileItem.file);
      formData.append('agentId', props.agentId || '');
      formData.append('sessionId', uid);
      formData.append('userId', props.userId || '');

      const response = await uploadFile(formData, {
        onProgress: (progress: number) => {
          const file = selectedFiles.value.find((f) => f.uid === uid);
          if (file) {
            file.progress = progress;
          }
        },
        signal: controller.signal,
      });
      const { data, code } = response as { data: any; code: number };

      if (code === 0) {
        const file = selectedFiles.value.find((f) => f.uid === uid);
        if (file) {
          file.isUploading = false;
          file.progress = 100;
          file.sessionId = data.sessionId;
          file.attachement_path = data.fileUrl;
          sessionIdList.value.push(data.sessionId);
        }
      } else {
        selectedFiles.value.splice(
          selectedFiles.value.findIndex((f) => f.uid === uid),
          1,
        );
        throw new Error(data?.message || '上传失败');
      }
    } catch (error: any) {
      if (error.name !== 'AbortError' && error.code !== 'ERR_CANCELED') {
        const file = selectedFiles.value.find((f) => f.uid === uid);
        if (file) {
          file.isUploading = false;
          file.uploadError = error.message;
        }
        const errorInfo = getErrorInfo(fileItem.file);
        ElMessage.error(String(errorInfo));
      }
      selectedFiles.value.splice(
        selectedFiles.value.findIndex((f) => f.uid === uid),
        1,
      );
    } finally {
      uploadControllers.delete(uid);
    }
  }

  // ========== 删除文件 ==========
  function removeFile(index: number) {
    const file = selectedFiles.value[index];
    if (!file) return;

    const controller = uploadControllers.get(file.uid);
    if (controller) {
      controller.abort();
      uploadControllers.delete(file.uid);
    }

    if (file.url?.startsWith('blob:')) {
      URL.revokeObjectURL(file.url);
    }

    if (file.sessionId) {
      const sidIndex = sessionIdList.value.indexOf(file.sessionId);
      if (sidIndex > -1) {
        sessionIdList.value.splice(sidIndex, 1);
      }
    }

    selectedFiles.value.splice(index, 1);
  }

  // ========== 语音录制 ==========
  async function startRecording() {
    console.log('mousedown: 开始录音');
    // 如果正在录制，不重复开始
    if (isRecording.value) return;

    // 标记正在请求权限
    pendingRecording.value = true;

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      audioStream = stream;

      // 权限获取成功后，检查是否应该停止（用户快速点击的情况）
      // if (shouldStopAfterStart.value) {
      //   // 用户已经松开鼠标，直接停止
      //   stream.getTracks().forEach((track) => track.stop());
      //   audioStream = null;
      //   pendingRecording.value = false;
      //   shouldStopAfterStart.value = false;
      //   return;
      // }

      // 开始录制
      mediaRecorder = new MediaRecorder(stream);
      audioChunks = [];

      mediaRecorder.ondataavailable = (e) => {
        if (e.data.size > 0) {
          audioChunks.push(e.data);
        }
      };

      mediaRecorder.onstop = () => {
        if (recordingCancelled) {
          recordingCancelled = false;
          audioChunks = [];
          if (audioStream) {
            audioStream.getTracks().forEach((track) => track.stop());
            audioStream = null;
          }
          return;
        }
        // 只有有数据时才发送
        if (audioChunks.length > 0) {
          const audioBlob = new Blob(audioChunks, { type: 'audio/webm' });
          sendAudioMessage(audioBlob);
        }
        // 清理资源
        if (audioStream) {
          audioStream.getTracks().forEach((track) => track.stop());
          audioStream = null;
        }
      };

      mediaRecorder.start();
      isRecording.value = true;
      pendingRecording.value = false;
      recordingTime.value = 0;

      // 开始计时
      recordingTimer = window.setInterval(() => {
        recordingTime.value++;
      }, 1000);
    } catch (error) {
      pendingRecording.value = false;
      shouldStopAfterStart.value = false;
      ElMessage.error('无法访问麦克风，请检查权限设置');
    }
  }

  function stopRecording() {

    console.log('mouseup: 停止录音');

    // 如果正在请求权限，标记需要在权限获取后停止
    if (pendingRecording.value) {
      shouldStopAfterStart.value = true;
      return;
    }

    // 如果没有在录制，直接返回
    if (!isRecording.value || !mediaRecorder) return;

    // 停止录制
    if (mediaRecorder.state !== 'inactive') {
      mediaRecorder.stop();
    }

    isRecording.value = false;

    // 停止计时
    if (recordingTimer) {
      clearInterval(recordingTimer);
      recordingTimer = null;
    }
  }

  function cancelRecording() {

    console.log('mouseleave: 取消录音');
    // 重置标志
    pendingRecording.value = false;
    shouldStopAfterStart.value = false;

    // 如果正在录制，停止并丢弃录音
    if (isRecording.value && mediaRecorder) {
      recordingCancelled = true;

      if (mediaRecorder.state !== 'inactive') {
        mediaRecorder.stop();
      }

      isRecording.value = false;
    }

    // 清理资源
    if (audioStream) {
      audioStream.getTracks().forEach((track) => track.stop());
      audioStream = null;
    }

    // 停止计时
    if (recordingTimer) {
      clearInterval(recordingTimer);
      recordingTimer = null;
    }
  }

  async function sendAudioMessage(audioBlob: Blob) {
    // 检查录音时长，太短的不发送
    if (recordingTime.value < 1) {
      ElMessage.warning('录音时间太短');
      return;
    }

    const file = new File([audioBlob], `语音消息_${Date.now()}.webm`, {
      type: 'audio/webm',
    });

    await handleFiles([file]);
  }

  // ========== 工具函数 ==========

  function generateUID(): string {
    return `file_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`;
  }

  // ========== 生命周期 ==========
  onMounted(() => {});

  watch(
    () => props.agentId,
    (newAgentId) => {
      const effectiveAgentId = newAgentId || '1';
      console.log('【MessageSender】------------newAgentId', newAgentId, '→ effectiveAgentId', effectiveAgentId);
      fetchFileCapabilities(effectiveAgentId);
    },
    {
      immediate: true,
    },
  );

  onUnmounted(() => {
    // 释放所有 URL
    selectedFiles.value.forEach((file) => {
      if (file.url?.startsWith('blob:')) {
        URL.revokeObjectURL(file.url);
      }
    });
  });
</script>

<style scoped lang="less">
  .message-sender {
    width: 100%;
    margin: 0;
    position: relative;
    padding: 12px;
    background: #fff;
    border-radius: 10px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    border: 3px solid #899fe3;
    box-sizing: border-box;
  }

  /* 已选文件区域 */
  .selected-files {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    margin-bottom: 12px;
    padding-bottom: 12px;
    border-bottom: 1px solid #eee;
  }

  .file-item {
    position: relative;
    width: 80px;
    height: 80px;
    border-radius: 8px;
    overflow: hidden;
    background: #f5f5f5;
  }

  .file-preview {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;

    img,
    video {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  .document-preview {
    flex-direction: column;
    gap: 4px;

    .doc-icon {
      font-size: 32px;
      color: #666;
    }

    .file-ext {
      font-size: 12px;
      color: #999;
    }
  }

  .audio-preview {
    .audio-icon {
      font-size: 40px;
      color: #409eff;
    }
  }

  .upload-progress {
    position: absolute;
    bottom: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    :deep(.el-progress-circle) {
      width: 20px !important;
      height: 20px !important;
    }
  }

  .file-delete {
    position: absolute;
    top: 4px;
    right: 4px;
    width: 20px;
    height: 20px;
    background: rgba(0, 0, 0, 0.6);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: #fff;

    &:hover {
      background: rgba(0, 0, 0, 0.8);
    }
  }

  .file-name {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 4px;
    background: rgba(0, 0, 0, 0.6);
    color: #fff;
    font-size: 10px;
    text-align: center;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  /* 输入区域 */
  .input-area {
    display: flex;
    flex-direction: column;
    gap: 8px;
    .input-area-bottom {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .input-area-bottom-right {
      display: flex;
      align-items: center;
      margin-left: auto;
    }
    :deep(.chat-textarea) {
      border: none;
    }
  }
  .upload-btn,
  .voice-btn,
  .send-btn,
  .pause-btn {
    width: 40px;
    height: 40px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.2s;
    flex-shrink: 0;
  }
  .send-btn,
  .pause-btn{
    margin-left: 8px;
  }
  .recording-def{
    margin-right: 0px !important;
  }

  .upload-btn {
    background: #f5f5f5;
    color: #666;

    &:hover {
      background: #e8e8e8;
      color: #333;
    }
  }

  .voice-btn {
    margin-right: 10px;
    background: #f5f5f5;
    color: #666;

    &:hover {
      background: #e8e8e8;
    }

    &.recording {
      background: #409eff;
      color: #fff;
    }

    .recording-time {
      position: absolute;
      bottom: -20px;
      font-size: 12px;
      color: #409eff;
    }
  }

  .recording-status {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .recording-indicator {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 0 12px;
    height: 36px;
    border-radius: 18px;
    background: rgba(64, 158, 255, 0.08);
  }

  .recording-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: #f56c6c;
    animation: recording-pulse 1.2s ease-in-out infinite;
    margin-bottom: 4px;
  }

  .recording-time {
    font-size: 14px;
    font-weight: 500;
    color: #409eff;
    font-variant-numeric: tabular-nums;
    min-width: 42px;
  }

  .voice-btn.cancel {
    background: #f5f5f5;
    color: #999;
    margin-right: 0;

    &:hover {
      background: #fde2e2;
      color: #f56c6c;
    }
  }

  .voice-btn.confirm {
    background: #409eff;
    color: #fff;
    margin-right: 0;

    &:hover {
      background: #66b1ff;
    }
  }

  @keyframes recording-pulse {
    0%, 100% {
      opacity: 1;
      transform: scale(1);
    }
    50% {
      opacity: 0.4;
      transform: scale(0.8);
    }
  }

  .send-btn {
    background: #409eff;
    color: #fff;
    justify-self: end;

    &:hover {
      background: #66b1ff;
    }

    &.disabled {
      background: #c0c4cc;
      cursor: not-allowed;
    }
  }

  .pause-btn {
    background: #e6a23c;
    color: #fff;

    &:hover {
      background: #ebb563;
    }
  }

  .chat-textarea {
    flex: 1;
    min-height: 40px;
    max-height: 200px;
    padding: 10px 12px;
    border: 1px solid #dcdfe6;
    border-radius: 8px;
    font-size: 14px;
    line-height: 1.5;
    resize: none;
    outline: none;
    transition: border-color 0.2s;

    &:focus {
      border-color: #409eff;
    }

    &::placeholder {
      color: #c0c4cc;
    }
  }

  /* 录音遮罩 */
  .recording-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    z-index: 1000;
  }

  .recording-indicator {
    position: relative;
    width: 100px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .pulse-ring {
    position: absolute;
    width: 100%;
    height: 100%;
    border: 3px solid #409eff;
    border-radius: 50%;
    animation: pulse 1.5s ease-out infinite;
  }

  @keyframes pulse {
    0% {
      transform: scale(1);
      opacity: 1;
    }
    100% {
      transform: scale(1.5);
      opacity: 0;
    }
  }

  .recording-text {
    margin-top: 20px;
    color: #fff;
    font-size: 16px;
  }
</style>
