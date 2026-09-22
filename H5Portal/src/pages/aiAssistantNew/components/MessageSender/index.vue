<template>
  <view class="message-sender">
    <!-- 已选文件展示区域 -->
    <view v-if="selectedFiles.length > 0" class="selected-files">
      <view v-for="(file, index) in selectedFiles" :key="file.uid" class="file-item">
        <!-- 图片预览 -->
        <img v-if="file.type === 'image'" :src="file.url" class="file-preview" mode="aspectFill" />

        <!-- 视频预览 -->
        <view v-else-if="file.type === 'video'" class="file-preview video-preview">
          <video :src="file.url" class="video-thumb" />
          <view class="video-play-icon">
            <van-icon name="play-circle" />
          </view>
        </view>

        <!-- 音频预览 -->
        <view v-else-if="file.type === 'audio'" class="file-preview audio-preview">
          <view class="video-play-icon"><van-icon name="music" /></view>
        </view>

        <!-- 文档预览 -->
        <view v-else class="file-preview file-doc">
          <text class="file-ext">{{ file.ext }}</text>
        </view>

        <!-- 上传进度遮罩 -->
        <view class="upload-progress-mask" v-if="file.isUploading">
          <van-circle
            :current-rate="file.progress"
            :rate="100"
            :speed="100"
            stroke-width="100"
            layer-color="#747a7f"
            color="#fff"
            size="20px"
          />
        </view>

        <!-- 删除按钮 -->
        <view class="file-delete" @click="removeFile(index)">×</view>
        <text class="file-name">{{ file.name }}</text>
      </view>
    </view>
    <!-- 输入区域 -->
    <view class="input-area">
      <!-- 相机按钮 -->
      <img
        v-if="isSupportMedia && !isInputFocused && !isVoiceMode && selectedFiles.length === 0"
        class="action-btn clickable"
        src="@/static/ai/camera.svg"
        :width="adaptationSize.groupIconWidth"
        :height="adaptationSize.groupIconHeight"
        @click="openCamera"
      />

      <!-- 文字输入框 -->
      <van-field
        v-if="!isVoiceMode"
        rows="1"
        class="chat-input"
        :autosize="{ maxHeight: 60 }"
        type="textarea"
        v-model="inputValue"
        placeholder="发消息..."
        ref="inputRef"
        :maxlength="9999"
        @focus="handleInputFocus"
        @blur="handleInputBlur"
        @input="handleInputChange"
      />

      <!-- 语音输入 -->
      <view
        v-else-if="selectedFiles.length === 0"
        class="voice-btn"
        :class="{ 'voice-btn-active': isRecording }"
        @touchstart="startRecording"
        @touchend="stopRecording"
        @touchmove="handleTouchMove"
        @touchcancel="cancelRecording"
      >
        <text>{{ isRecording ? '松手发送' : '按住说话' }}</text>
      </view>

      <!-- 文件选择按钮 -->
      <img
        v-if="isSupportFile && !isInputFocused && !isVoiceMode && selectedFiles.length === 0"
        class="action-btn clickable"
        src="@/static/ai/file.svg"
        :width="adaptationSize.groupIconWidth"
        :height="adaptationSize.groupIconHeight"
        @click="selectFile"
      />

      <!-- 语音/键盘切换按钮 -->
      <img
        v-if="isSupportAudio && !isInputFocused && selectedFiles.length === 0"
        class="action-btn clickable"
        :src="isVoiceMode ? keyboards : voice"
        :width="adaptationSize.groupIconWidth"
        :height="adaptationSize.groupIconHeight"
        @click="toggleVoiceMode"
      />

      <!-- 暂停按钮 -->
      <img
        v-if="(isStreaming || isReceiving || isDisplaying) && !isPaused"
        class="send-btn clickable"
        src="@/static/ai/pause.png"
        :width="adaptationSize.groupIconWidth"
        :height="adaptationSize.groupIconHeight"
        @click="emit('pause')"
      />

      <!-- 发送按钮 -->
      <img
        v-else-if="isInputFocused || selectedFiles.length > 0 || inputValue.trim()"
        class="send-btn clickable"
        :class="[{ 'send-btn-disabled': isLoading }]"
        src="@/static/ai/send_app.png"
        :width="adaptationSize.groupIconWidth"
        :height="adaptationSize.groupIconHeight"
        @click="sendMessage"
      />
    </view>

    <!-- 语音录制浮层 -->
    <view
      v-if="isRecording"
      class="voice-recording-overlay"
      :class="{ 'cancel-mode': isCancelMode }"
      @touchmove.prevent
    >
      <view class="recording-container">
        <!-- 提示文字 -->
        <view class="recording-tip" :class="{ 'cancel-tip': isCancelMode }">
          <text v-if="!isCancelMode">松手发送，上移取消</text>
          <text v-else>松手取消</text>
        </view>

        <!-- 白色音轨区域 -->
        <view class="waveform-container">
          <view
            v-for="(level, index) in audioWaveform"
            :key="index"
            class="wave-bar"
            :style="{ height: `${Math.max(4, level * 0.8)}px` }"
          />
        </view>
      </view>
    </view>

    <!-- 操作选择弹窗 -->
    <van-action-sheet
      v-model:show="showActionSheet"
      :actions="currentActions"
      @select="onActionSelect"
    />
  </view>
</template>

<script setup>
  import { ref, onUnmounted, watch } from 'vue';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';
  import { showCustomToast } from '@/utils/toast';
  import keyboards from '@/static/ai/keyboards.svg';
  import voice from '@/static/ai/voice.svg';
  import { uploadFile, getFileCapabilities } from '@/common/api/ai.js';
  import{ getAlbumFileUpload, getTakePhotoFileUpload, getFileUpload } from "@/utils/file.js"

  // Props
  const props = defineProps({
    isLoading: { type: Boolean, default: false },
    isPaused: { type: Boolean, default: false },
    isStreaming: { type: Boolean, default: false },
    isReceiving: { type: Boolean, default: false },
    isDisplaying: { type: Boolean, default: false },
    agentId: { type: String, default: '' },
    userId: { type: String, default: '' },
  });
  const emit = defineEmits(['send', 'pause', 'sendFile','focus']);
  const { adaptationSize } = useDeviceAdapter();

  // ========== 状态定义 ==========
  const inputRef = ref(null);
  const inputValue = ref('');
  // 输入框是否聚焦
  const isInputFocused = ref(false);
  // 是否处于语音模式
  const isVoiceMode = ref(false);
  // 是否正在录音
  const isRecording = ref(false);
  // 是否处于取消录音模式（手指上移时）
  const isCancelMode = ref(false);
  const selectedFiles = ref([]);
  const sessionIdList = ref([]);

  // 上传请求控制器映射（用于取消请求）
  const uploadControllers = new Map();

  /**
   * 选择相册文件
   */
  async function selectAlbumFile() {
    let file = await getAlbumFileUpload()

    console.log('file', file)

    await handleFiles([file]);
  }

  /**
   * 选择拍照文件
   */
  async function selectTakePhotoFile() {
    let file = await getTakePhotoFileUpload()

    console.log('file', file)

    await handleFiles([file]);
  }

  /**
   * 选择普通文件
   */
  async function selectFile() {
    let file = await getFileUpload()

    console.log('file', file)

    await handleFiles([file]);
  }

  // 弹窗控制
  const showActionSheet = ref(false);
  const currentActions = ref([
    { name: '拍照', value: 'camero' },
    { name: '从相册中选择', value: 'photo' },
  ]);

  // 录音相关
  let mediaRecorder = null;
  let audioChunks = [];
  let recordingStartTime = 0;
  let mediaStream = null;

  // 音频可视化相关
  let audioContext = null;
  let analyser = null;
  let animationFrameId = null;
  let durationTimer = null;

  const audioWaveform = ref(new Array(30).fill(4)); // 30个波形条
  const recordingDuration = ref(0);
  // 支持的文件类型
  const acceptList = ref([]);
  // 是否支持语音
  const isSupportAudio = ref(true);
  // 是否支持图片、视频
  const isSupportMedia = ref(true);
  // 是否支持文件
  const isSupportFile = ref(true);
  const fileCapabilities = ref({
    audio: false,
    audioType: [],
    video: false,
    videoType: [],
    image: false,
    imageType: [],
    document: false,
    documentType: [],
  });
  watch(
    () => props.agentId,
    (val) => {
      updateAgentCapabilities()
    },
    { immediate: true },
  );

  // 更新智能体能力
  async function updateAgentCapabilities() {
    let res = await getFileCapabilities(props.agentId)

    const {
      audio,
      audioType,
      video,
      videoType,
      image,
      imageType,
      document,
      documentType,
    } = res;

    // 清空原有列表
    acceptList.value = [];
    const newAcceptTypes = /** @type {string[]} */ ([]);
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
    fileCapabilities.value = { audio, audioType, video, videoType, image, imageType, document, documentType };
    isSupportAudio.value = audio;
    isSupportMedia.value = image || video;
    isSupportFile.value = document || audio;
  }
   
  // ========== 输入框事件 ==========
  function handleInputFocus() {
    isInputFocused.value = true;
    emit('focus')
  }

  function handleInputBlur() {
    isInputFocused.value = false;
  }

  function handleInputChange() {
    if (inputValue.value.trim()) {
      isInputFocused.value = true;
    }
  }
  // ========== 相机功能 ==========
  function openCamera() {
    showActionSheet.value = true;
  }


  // ========== 操作选择回调 ==========
  async function onActionSelect(action) {
    showActionSheet.value = false;
    await new Promise((resolve) => setTimeout(resolve, 150));

    const type = action.value;
    if (type === 'camero') {
      await selectTakePhotoFile();
    } else {
      await selectAlbumFile();
    }
  }

  // ========== 生成唯一ID ==========
  function generateUID() {
    return `file_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`;
  }
  function getErrorInfo(file) {
    const MAX_FILE_SIZE = 2 * 1024 * 1024 * 1024; // 2G 最大文件大小
    if (file.size > MAX_FILE_SIZE) {
      return '超过最大上传2G文件大小';
    }
    const fileType = getFileType(file);
    const ext = '.' + file.name.split('.').pop()?.toLowerCase();

    const categoryMap = {
      audio: { enabled: !!fileCapabilities.value.audio, types: fileCapabilities.value.audioType || [], label: '音频' },
      video: { enabled: !!fileCapabilities.value.video, types: fileCapabilities.value.videoType || [], label: '视频' },
      image: { enabled: !!fileCapabilities.value.image, types: fileCapabilities.value.imageType || [], label: '图片' },
      document: { enabled: !!fileCapabilities.value.document, types: fileCapabilities.value.documentType || [], label: '文档' },
    };

    const category = categoryMap[fileType];
    if (!category) return  `无法识别的文件类型${fileType}`;
    if (!category.enabled) return `暂不支持上传${category.label}`;
    if (!category.types.includes(ext)) {
      const labelMap = { file: '文档', video: '视频', audio: '音频', image: '图片' };
      const label = labelMap[fileType] || '';
      return label ? `暂不支持该${label}文件格式${ext}` : `不支持该类型文件：${ext}`;
    }
    return null;
  }

  // ========== 处理文件 ==========
  async function handleFiles(files) {
    if (selectedFiles.value.length > 0) return
    const file = files[0]
    const errorInfo = getErrorInfo(file);
    if (errorInfo) {
      showCustomToast(errorInfo);
      return;
    }

    const fileType = getFileType(file);
    const url = URL.createObjectURL(file);
    const uid = generateUID();
    const ext = '.' + file.name.split('.').pop()?.toLowerCase();

    const fileItem = {
      uid: uid,
      type: fileType,
      url: url,
      name: file.name,
      ext: ext,
      file: file,
      size: file.size,
      isUploading: true,
      progress: 10,
      sessionId: null,
    };

    selectedFiles.value.push(fileItem);
    uploadFileWithProgress(fileItem, uid);
  }
  // ========== 带进度的文件上传 ==========
  async function uploadFileWithProgress(fileItem, uid) {
    // 创建 AbortController 用于取消请求
    const controller = new AbortController();
    uploadControllers.set(uid, controller);

    try {
      const formData = new FormData();
      formData.append('file', fileItem.file);
      formData.append('agentId', props.agentId);
      formData.append('sessionId', uid);
      formData.append('userId', props.userId);
      const res = await uploadFile(formData, {
        // 进度回调
        onProgress: (progress) => {
          const file = selectedFiles.value.find((f) => f.uid === uid);
          if (file) {
            file.progress = progress;
          }
        },
        // 取消信号
        signal: controller.signal,
      });
      // 上传成功
      const file = selectedFiles.value.find((f) => f.uid === uid);
      if (file) {
        file.isUploading = false;
        file.progress = 100;
        file.sessionId = res.sessionId;
        file.attachement_path = res.fileUrl;
        sessionIdList.value.push(res.sessionId);
      }
    } catch (error) {
      console.error('文件上传失败:', error);

      // 如果不是取消导致的错误
      if (error.name !== 'AbortError' && error.message !== 'canceled') {
        const file = selectedFiles.value.find((f) => f.uid === uid);
        if (file) {
          file.isUploading = false;
          file.uploadError = error.message;
        }
        const errorInfo = fileItem.file ? getErrorInfo(fileItem.file) : null;
        showCustomToast(errorInfo);
        selectedFiles.value = selectedFiles.value.filter((f) => f.uid !== uid);
      }
    } finally {
      // 清理控制器
      uploadControllers.delete(uid);
    }
  }

  function getFileType(file) {
    if (file.type.startsWith('image/')) return 'image';
    if (file.type.startsWith('video/')) return 'video';
    if (file.type.startsWith('audio/')) return 'audio';
    return 'document';
  }
  // ========== 文件管理 ==========
  function removeFile(index) {
    const file = selectedFiles.value[index];

    if (!file) return;

    // 文件正在上传中
    if (file.isUploading) {
      // 取消上传请求
      const controller = uploadControllers.get(file.uid);
      if (controller) {
        controller.abort();
        uploadControllers.delete(file.uid);
      }
      showCustomToast('已取消上传');
    }
    //文件已上传完成
    if (!file.isUploading && file.sessionId) {
      // 从 sessionIdList 中移除对应的 sessionId
      sessionIdList.value = sessionIdList.value.filter((sessionId) => sessionId !== file.sessionId);
    }
    // 释放 blob URL
    if (file.url?.startsWith('blob:')) {
      URL.revokeObjectURL(file.url);
    }

    // 从文件列表中移除
    selectedFiles.value.splice(index, 1);
  }
  // ========== 语音功能 ==========
  function toggleVoiceMode() {
    isVoiceMode.value = !isVoiceMode.value;
    if (!isVoiceMode.value) {
      setTimeout(() => inputRef.value?.focus(), 100);
    }
  }

  // ========== 处理触摸移动（判断是否移出录音区域） ==========
  function handleTouchMove(event) {
    if (!isRecording.value) return;

    const touch = event.touches[0];
    const screenHeight = window.innerHeight;
    const clientY = touch.clientY;

    // voice-recording-overlay 区域高度为 160px，固定在底部
    const overlayHeight = 160;
    const overlayTop = screenHeight - overlayHeight;

    // 手指移出录音区域（向上移出）进入取消模式
    const shouldCancel = clientY < overlayTop;

    isCancelMode.value = shouldCancel;
  }

  // ========== 开始录音 ==========
  async function startRecording(event) {
    if (isRecording.value) return;
    if (selectedFiles.value.length > 0) return;

    // 重置状态
    isCancelMode.value = false;
    recordingDuration.value = 0;
    audioWaveform.value = new Array(30).fill(4);

    // 开始时长计时
    durationTimer = setInterval(() => {
      if (isRecording.value) {
        recordingDuration.value = Math.floor((Date.now() - recordingStartTime) / 1000);
        // 限制最大时长60秒
        if (recordingDuration.value >= 60) {
          stopRecording();
        }
      }
    }, 100);

    try {
      // 获取麦克风流
      mediaStream = await navigator.mediaDevices.getUserMedia({
        audio: { echoCancellation: true, noiseSuppression: true },
      });

      // 录音逻辑
      const mimeTypes = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4'];
      let mimeType = mimeTypes.find((type) => MediaRecorder.isTypeSupported(type));

      if (!mimeType) {
        showCustomToast('不支持录音格式');
        cleanupRecording();
        return;
      }

      mediaRecorder = new MediaRecorder(mediaStream, { mimeType });
      audioChunks = [];
      recordingStartTime = Date.now();

      mediaRecorder.ondataavailable = (e) => {
        if (e.data.size > 0) audioChunks.push(e.data);
      };

      mediaRecorder.onstop = () => {
        // 录音结束后的处理在 stopRecording 中完成
      };

      // 音频可视化
      audioContext = new (window.AudioContext || window.webkitAudioContext)();
      analyser = audioContext.createAnalyser();
      analyser.fftSize = 256;
      analyser.smoothingTimeConstant = 0.3;

      const source = audioContext.createMediaStreamSource(mediaStream);
      source.connect(analyser);
      await audioContext.resume();

      mediaRecorder.start(100);
      isRecording.value = true;

      // 启动可视化
      startAudioVisualization();
    } catch (error) {
      console.error('录音失败:', error);
      cleanupRecording();
      clearInterval(durationTimer);

      if (error.name === 'NotAllowedError') {
        showCustomToast('请允许使用麦克风');
      } else if (error.name === 'NotFoundError') {
        showCustomToast('未找到麦克风');
      } else {
        showCustomToast('录音失败');
      }
    }
  }

  // ========== 音量可视化 ==========
  function startAudioVisualization() {
    if (!analyser || !audioContext) return;

    const bufferLength = analyser.frequencyBinCount;
    const dataArray = new Uint8Array(bufferLength);

    function updateWaveform() {
      if (!analyser || !isRecording.value) return;

      analyser.getByteFrequencyData(dataArray);

      // 更新波形数据（30个波形条）
      const step = Math.max(1, Math.floor(dataArray.length / 30));
      const waveform = [];
      for (let i = 0; i < 30; i++) {
        const value = dataArray[Math.min(i * step, dataArray.length - 1)];
        // 转换为高度值 4-40px
        let height = Math.min(40, Math.max(4, Math.floor((value / 255) * 40)));
        // 增加小音量的敏感度
        if (value > 0 && height < 8) {
          height = 8;
        }
        waveform.push(height);
      }
      audioWaveform.value = waveform;

      animationFrameId = requestAnimationFrame(updateWaveform);
    }

    updateWaveform();
  }

  // ========== 停止录音 ==========
  async function stopRecording() {
    if (!isRecording.value || !mediaRecorder) {
      cleanupRecording();
      return;
    }

    const isCancel = isCancelMode.value;

    if (isCancel) {
      mediaRecorder.onstop = () => {
        cleanupRecording();
        showCustomToast('已取消录音');
      };
      mediaRecorder.stop();
    } else {
      mediaRecorder.onstop = () => {
        const duration = Math.floor((Date.now() - recordingStartTime) / 1000);

        if (selectedFiles.value.length > 0) {
          cleanupRecording();
          return;
        }

        if (duration < 1) {
          showCustomToast('录音时间太短');
          cleanupRecording();
          return;
        }

        const mimeType = mediaRecorder?.mimeType || 'audio/webm';
        const blob = new Blob(audioChunks, { type: mimeType });
        const url = URL.createObjectURL(blob);
        const uid = generateUID();
        const audioFile = new File([blob], `audio_${Date.now()}.webm`, { type: mimeType });
        
        const MAX_FILE_SIZE = 2 * 1024 * 1024 * 1024; // 2G 最大文件大小
        if (audioFile.size > MAX_FILE_SIZE) {
          showCustomToast('超过最大上传2G文件大小');
          cleanupRecording();
          return;
        }

        const fileItem = {
          uid: uid,
          type: 'audio',
          url: url,
          duration: duration,
          name: `语音_${duration}s.webm`,
          file: audioFile,
          isUploading: true,
          progress: 0,
          sessionId: null,
        };

        selectedFiles.value.push(fileItem);
        uploadFileWithProgress(fileItem, uid);
        cleanupRecording();
        // 录音成功后切回文字输入模式，完成交互闭环
        isVoiceMode.value = false;
      };
      mediaRecorder.stop();
    }

    isRecording.value = false;
    clearInterval(durationTimer);
  }

  // ========== 取消录音 ==========
  function cancelRecording() {
    if (!isRecording.value) return;

    mediaRecorder?.stop();
    cleanupRecording();
    clearInterval(durationTimer);
    showCustomToast('已取消录音');
  }

  // ========== 清理资源 ==========
  function cleanupRecording() {
    if (animationFrameId) {
      cancelAnimationFrame(animationFrameId);
      animationFrameId = null;
    }

    if (audioContext) {
      audioContext.close().catch(console.error);
      audioContext = null;
      analyser = null;
    }

    if (mediaStream) {
      mediaStream.getTracks().forEach((track) => {
        if (track.readyState === 'live') track.stop();
      });
      mediaStream = null;
    }

    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
      try {
        mediaRecorder.stop();
      } catch (e) {}
    }
    mediaRecorder = null;

    isRecording.value = false;
    isCancelMode.value = false;
    audioWaveform.value = new Array(30).fill(4);
  }

  // ========== 发送消息 ==========
  function sendMessage() {
    const uploadingFiles = selectedFiles.value.filter((f) => f.isUploading);
    if (uploadingFiles.length > 0) {
      showCustomToast('请等待文件上传完成');
      return;
    }
    const files = selectedFiles.value.map(_=>({
        attachement_path: _.attachement_path,
        sessionId: _.sessionId,
      }))

    if (inputValue.value.trim() || sessionIdList.value.length > 0) {
      console.log('发送消息', inputValue.value, sessionIdList.value, files, selectedFiles.value);
      emit('send', inputValue.value, sessionIdList.value, files);
      inputValue.value = '';
      sessionIdList.value = [];
      selectedFiles.value = [];
    }
  }

  onUnmounted(() => {
    // 取消所有正在上传的请求
    uploadControllers.forEach((controller) => {
      controller.abort();
    });
    uploadControllers.clear();

    cleanupRecording();

    selectedFiles.value.forEach((file) => {
      if (file.url?.startsWith('blob:')) URL.revokeObjectURL(file.url);
    });
  });
</script>

<style scoped lang="scss">
  .message-sender {
    width: 100%;
    background: #fff;
  }

  .selected-files {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    padding: 10px 12px;
    border-top: 1px solid #f0f0f0;
    margin-top: 10px;

    .file-item {
      position: relative;
      width: 60px;

      .file-preview {
        width: 60px;
        height: 60px;
        border-radius: 8px;
        background: #f5f5f5;
        display: flex;
        align-items: center;
        justify-content: center;
        overflow: hidden;
      }
      .upload-progress-mask {
        position: absolute;
        top: 0;
        right: 0;
        width: 60px;
        height: 60px;
        border-radius: 8px;
        background: rgba(0, 0, 0, 0.35);
        display: flex;
        align-items: center;
        justify-content: center;
      }
      .video-preview {
        position: relative;
        .video-thumb {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }
        .video-play-icon {
          position: absolute;
          width: 24px;
          height: 24px;
          background: rgba(0, 0, 0, 0.5);
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          color: #fff;
          font-size: 10px;
        }
      }

      .audio-preview {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: #fff;
        .audio-duration {
          font-size: 14px;
          font-weight: 500;
        }
      }

      .file-doc {
        background: #e8f4ff;
        .file-ext {
          font-size: 12px;
          color: #1890ff;
          font-weight: 500;
        }
      }

      .file-delete {
        position: absolute;
        top: -6px;
        right: -6px;
        width: 18px;
        height: 18px;
        background: #ff4d4f;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;
        font-size: 12px;
        z-index: 10;
      }

      .file-name {
        display: block;
        font-size: 10px;
        color: #666;
        text-align: center;
        margin-top: 4px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }

  .input-area {
    width: 100%;
    display: flex;
    align-items: center;
    padding: 10px 12px 12px;
    gap: 8px;

    .chat-input {
      flex: 1; // 自动填充剩余空间
      min-width: 0; // 允许压缩到最小
      border-radius: 10px;
      background-color: #f5f5f5;
      font-size: 14px;
    }

    .voice-btn {
      flex: 1;
      min-width: 0;
      height: 40px;
      border-radius: 20px;
      background-color: #f5f5f5;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      color: #333;

      &.voice-btn-active {
        background-color: #e8f4ff;
        color: #1890ff;
      }
    }

    .action-btn {
      width: 24px;
      height: 24px;
      flex-shrink: 0; // 不压缩，保持固定宽度
      opacity: 0.8;
      &:active {
        opacity: 0.5;
      }
    }

    .send-btn {
      width: 30px;
      height: 30px;
      flex-shrink: 0; // 不压缩，保持固定宽度
    }

    .send-btn-disabled {
      opacity: 0.5 !important;
      pointer-events: none;
    }
  }
  /* 平板适配 */
  @media screen and (min-height: 1200px) {
    :deep(.input-placeholder) {
      font-size: 0.6rem;
      height: 2vh;
      line-height: 2vh;
    }
  }
  // ========== 语音录制浮层样式 ==========
  .voice-recording-overlay {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    height: 160px;
    background: linear-gradient(to top, rgb(135, 163, 255), rgb(178, 214, 255));
    box-shadow: rgb(178, 214, 255) 0 0 50px 50px;
    z-index: 10000;

    &.cancel-mode {
      background: linear-gradient(to top, rgb(255, 131, 122), rgb(255, 175, 193));
      box-shadow: rgb(255, 175, 193) 0 0 50px 50px;
    }

    @keyframes slideUp {
      from {
        transform: translateY(100%);
      }
      to {
        transform: translateY(0);
      }
    }

    .recording-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100%;
      padding: 20px;
    }

    .recording-tip {
      font-size: 14px;
      color: rgba(255, 255, 255, 0.9);
      margin-bottom: 20px;
      text-align: center;

      &.cancel-tip {
        color: rgba(255, 255, 255, 0.9);
      }
    }

    .waveform-container {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 4px;
      height: 40px;
      padding: 0 20px;

      .wave-bar {
        width: 2px;
        background: #ffffff;
        border-radius: 2px;
        transition: height 0.05s ease;
        box-shadow: 0 0 4px rgba(255, 255, 255, 0.5);
      }
    }

    .recording-duration {
      margin-top: 20px;
      font-size: 16px;
      color: rgba(255, 255, 255, 0.9);
      font-weight: 500;
    }
  }
</style>
