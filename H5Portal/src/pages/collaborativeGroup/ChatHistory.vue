<template>
  <view class="chat-history-container">
    <!-- <scroll-view
      class="chat-messages"
      scroll-y
      :scroll-top="scrollTop"
      @scroll="handleScroll"
      @scrolltolower="loadMoreMessages"
      :scroll-with-animation="true"
    > -->
    <SearchInput
      ref="searchInputRef"
      :show-member-filter="true"
      :member-list="memberList"
      :selected-member-id="selectedMemberId"
      :group-name="groupName"
      @handleChange="handleSearch"
      @handleSearch="handleSearch"
      @handleSwitch="handleSearch"
      @memberChange="handleMemberChange"
    />

    <view class="chat-messages" ref="chatMessagesRef" @scroll="handleScroll">
      <!-- 顶部加载提示 -->
      <view v-if="isLoadingMore" class="loading-more">
        <view class="loading-spinner"></view>
        <text>加载历史消息...</text>
      </view>
      <view v-else-if="!hasMore && chatHistory.length > 0" class="loading-more">
        <text>没有更多数据了</text>
      </view>

      <!-- 消息列表 -->
      <template v-for="message in processedMessages" :key="message.msgId">
          <!-- 群组通知消息 -->
          <view v-if="isValidGroupEvent(message)" class="system-message-item">
            <GroupNotice
              :message="message"
              :member-list="groupMembers"
              :hide-time="true"
              :disable-you-alias="props.disableYouAlias"
            />
          </view>

          <!-- 普通消息 -->
          <view
            v-else-if="message.msgType !== 10"
            :class="['message-item', isSelf(message) ? 'own-message' : 'other-message']"
          >
            <!-- 对方消息（左侧） -->
            <view v-if="!isSelf(message)" class="message-left">
              <view class="avatar">
                <van-image
                  v-if="message.avatar"
                  class="user-avatar"
                  :src="transformImageUrl(`/admin-api${message.avatar}`)"
                />
                <img v-else src="@/static/5110/groups.png" class="user-avatar" />
              </view>
              <view class="message-content">
                <view class="message-header">
                  <text class="sender-name">{{ message.name }}</text>
                  <text class="message-time">{{ formatTime(parseInt(message.time)) }}</text>
                </view>
                <view class="message-bubble-box" style="display: flex; justify-content: flex-start">
                  <view class="message-bubble">
                    <!-- 引用消息 -->
                    <view v-if="getMessageContent(message).type === 'quote'" class="quote-message">
                      <view class="quote-text">
                        <EmojiDisplay :text="getMessageContent(message).content" :highlight-keyword="searchParams.keyword" />
                      </view>
                      <QuoteMsg
                        :quote-msg="getMessageContent(message).quotedMsg"
                        :reply-text="getMessageContent(message).content"
                        :member-list="groupMembers"
                      />
                    </view>

                    <!-- 文本消息 -->
                    <view
                      v-else-if="getMessageContent(message).type === 'text'"
                      class="text-message"
                    >
                      <EmojiDisplay :text="getMessageContent(message).content" :highlight-keyword="searchParams.keyword" />
                    </view>

                    <!-- 文件消息 -->
                    <view
                      v-else-if="getMessageContent(message).type === 'file'"
                      class="file-message"
                    >
                      <!-- 图片 -->
                      <van-image
                        v-if="getMessageContent(message).content.fileType == 1"
                        :src="
                          transformImageUrl(
                            `/admin-api${getMessageContent(message).content.filePath}`,
                          )
                        "
                        class="message-image"
                        fit="contain"
                        @click="lookImage(getMessageContent(message).content.filePath)"
                      />

                      <!-- 视频 -->
                      <view
                        v-else-if="getMessageContent(message).content.fileType == 3"
                        class="video-container"
                        @click="lookVideo(getMessageContent(message).content.filePath)"
                      >
                        <video
                          :src="
                            transformImageUrl(
                              `/admin-api${getMessageContent(message).content.filePath}`,
                            )
                          "
                          :autoplay="false"
                          class="message-video"
                        ></video>
                        <div class="play-button">
                          <van-icon name="play-circle-o" class="play-icon" />
                        </div>
                      </view>
                      <!-- 封面： :poster="" -->

                      <!-- 文件 -->
                      <view
                        v-else-if="
                          [4, 5, 6, 7, 8, 9, 10, 11].includes(
                            getMessageContent(message).content.fileType,
                          )
                        "
                        class="file-item"
                        @click="lookFile(getMessageContent(message).content)"
                      >
                        <view class="file-icon">📎</view>
                        <view class="file-info">
                          <text class="file-name">{{
                            getMessageContent(message).content.fileName || '未知文件'
                          }}</text>
                          <text class="file-size">{{
                            formatFileSize(getMessageContent(message).content.fileSize)
                          }}</text>
                        </view>
                      </view>

                      <!-- 语音 -->
                      <view
                        v-else-if="getMessageContent(message).content.fileType == 2"
                        class="voice-message"
                      >
                        <view class="voice-item other-voice" @click="playVoiceMessage(message)">
                          <view class="voice-content other-voice-content">
                            <!-- 播放动画 -->
                            <view class="voice-animation">
                              <view
                                class="wave-bar"
                                v-for="n in 3"
                                :key="n"
                                :class="{ active: isVoicePlaying(message) }"
                              >
                              </view>
                            </view>
                            <!-- 语音时长 -->
                            <text class="voice-duration"
                              >{{ getMessageContent(message).content.duration || 0 }}″</text
                            >
                          </view>
                        </view>
                      </view>
                    </view>

                    <!-- 转发消息 -->
                    <view
                      v-else-if="getMessageContent(message).type === 'forward'"
                      class="forward-message"
                    >
                      <MsgForwardCard
                        :card-data="getMessageContent(message).content"
                        :groupId="groupId"
                        :member-list="groupMembers"
                        @allMemberList="getAllMemberList"
                        @showDetail="showForwardDetail"
                      />
                      <!-- <view class="forward-content">
											<text class="forward-title">[合并转发]
												{{ getMessageContent(message).content.title }}</text>
											<text
												class="forward-count">{{ getMessageContent(message).content.forwardMsgs?.length || 0 }}条消息</text>
										</view> -->
                    </view>

                    <!-- 个人名片 -->
                    <view
                      v-else-if="getMessageContent(message).type === 'userCard'"
                      class="userCard-message"
                    >
                      <MsgCard :card-data="getMessageContent(message).content" />
                    </view>

                    <!-- 群接龙 -->
                    <view
                      v-else-if="getMessageContent(message).type === 'groupJoining'"
                      class="group-joining-message"
                    >
                      <view class="group-joining-content">
                        <!-- 接龙标题 -->
                        <view class="joining-header">#接龙</view>

                        <!-- 接龙内容 -->
                        <view class="joining-text">{{ getGroupJoiningContent(message).text }}</view>

                        <!-- 发起人和参与人数 -->
                        <view class="joining-info">
                          {{ getGroupJoiningContent(message).creatorName }}发起，参与共{{
                            getGroupJoiningContent(message).participantCount
                          }}人
                        </view>

                        <!-- 参与人员列表 -->
                        <view class="joining-participants">
                          <view
                            v-for="(participant, pIndex) in getGroupJoiningContent(message)
                              .participants"
                            :key="participant.userId"
                            class="participant-item"
                          >
                            <text class="participant-index">{{ pIndex + 1 }}</text>
                            <van-image
                              v-if="participant.avatar"
                              :src="transformImageUrl(`/admin-api${participant.avatar}`)"
                              alt="用户头像"
                              class="preview-image"
                              fit="contain"
                            />
                            <van-image
                              v-else
                              :src="defaultImg"
                              alt="默认头像"
                              class="preview-image"
                              fit="contain"
                            />
                            <text class="participant-name">{{ participant.name }}</text>
                          </view>
                        </view>
                      </view>
                    </view>

                    <!-- 系统消息 -->
                    <view v-else class="system-message">
                      <text>{{ getMessageContent(message).content }}</text>
                    </view>
                  </view>
                </view>
              </view>
            </view>

            <!-- 当前用户消息（右侧） -->
            <view v-else class="message-right">
              <view class="avatar">
                <van-image
                  v-if="message.avatar"
                  :src="transformImageUrl(`/admin-api${message.avatar}`)"
                  class="user-avatar"
                />
                <img v-else src="@/static/5110/groups.png" class="user-avatar" />
              </view>
              <view class="message-content">
                <view class="message-header own-header">
                  <text class="sender-name">{{ message.name }}</text>
                  <text class="message-time">{{ formatTime(parseInt(message.time)) }}</text>
                </view>
                <view class="message-bubble-box" style="display: flex; justify-content: flex-end">
                  <view class="message-bubble own-bubble">
                    <!-- 引用消息 -->
                    <view v-if="getMessageContent(message).type === 'quote'" class="quote-message">
                      <view class="quote-text">
                        <EmojiDisplay :text="getMessageContent(message).content" :highlight-keyword="searchParams.keyword" />
                      </view>
                      <QuoteMsg
                        :quote-msg="getMessageContent(message).quotedMsg"
                        :reply-text="getMessageContent(message).content"
                        :member-list="groupMembers"
                      />
                    </view>

                    <!-- 文本消息 -->
                    <view
                      v-else-if="getMessageContent(message).type === 'text'"
                      class="text-message"
                    >
                      <EmojiDisplay :text="getMessageContent(message).content" :highlight-keyword="searchParams.keyword" />
                    </view>

                    <!-- 文件消息 -->
                    <view
                      v-else-if="getMessageContent(message).type === 'file'"
                      class="file-message"
                    >
                      <!-- 图片 -->
                      <van-image
                        v-if="getMessageContent(message).content.fileType == 1"
                        :src="
                          transformImageUrl(
                            `/admin-api${getMessageContent(message).content.filePath}`,
                          )
                        "
                        class="message-image"
                        fit="contain"
                        @click="lookImage(getMessageContent(message).content.filePath)"
                      />

                      <!-- 视频 -->
                      <view
                        v-else-if="getMessageContent(message).content.fileType == 3"
                        class="video-container"
                        @click="lookVideo(getMessageContent(message).content.filePath)"
                      >
                        <video
                          :src="
                            transformImageUrl(
                              `/admin-api${getMessageContent(message).content.filePath}`,
                            )
                          "
                          class="message-video"
                          :autoplay="false"
                          :initial-time="getMessageContent(message).content.duration"
                        ></video>
                        <div class="play-button">
                          <van-icon name="play-circle-o" class="play-icon" />
                        </div>
                      </view>
                      <!-- 封面： :poster="" -->

                      <!-- 文件 -->
                      <view
                        v-else-if="
                          [4, 5, 6, 7, 8, 9, 10, 11].includes(
                            getMessageContent(message).content.fileType,
                          )
                        "
                        class="file-item"
                        @click="lookFile(getMessageContent(message).content)"
                      >
                        <view class="file-icon">📎</view>
                        <view class="file-info">
                          <text class="file-name">{{
                            getMessageContent(message).content.fileName || '未知文件'
                          }}</text>
                          <text class="file-size">{{
                            formatFileSize(getMessageContent(message).content.fileSize)
                          }}</text>
                        </view>
                      </view>
                      <!-- 语音 -->
                      <view
                        v-else-if="getMessageContent(message).content.fileType == 2"
                        class="voice-message"
                      >
                        <view class="voice-item own-voice" @click="playVoiceMessage(message)">
                          <view class="voice-content own-voice-content">
                            <!-- 语音时长 -->
                            <text class="voice-duration"
                              >{{ getMessageContent(message).content.duration || 0 }}″</text
                            >
                            <!-- 播放动画 -->
                            <view class="voice-animation">
                              <view
                                class="wave-bar"
                                v-for="n in 3"
                                :key="n"
                                :class="{ active: isVoicePlaying(message) }"
                              >
                              </view>
                            </view>
                          </view>
                        </view>
                      </view>
                    </view>

                    <!-- 转发消息 -->
                    <view
                      v-else-if="getMessageContent(message).type === 'forward'"
                      class="forward-message"
                    >
                      <MsgForwardCard
                        :card-data="getMessageContent(message).content"
                        :groupId="groupId"
                        :member-list="groupMembers"
                        @allMemberList="getAllMemberList"
                        @showDetail="showForwardDetail"
                      />
                      <!-- <view class="forward-content">
											<text class="forward-title">[合并转发]
												{{ getMessageContent(message).content.title }}</text>
											<text
												class="forward-count">{{ getMessageContent(message).content.forwardMsgs?.length || 0 }}条消息</text>
										</view> -->
                    </view>

                    <!-- 个人名片 -->
                    <view
                      v-else-if="getMessageContent(message).type === 'userCard'"
                      class="userCard-message"
                    >
                      <MsgCard :card-data="getMessageContent(message).content" />
                    </view>

                    <!-- 群接龙 -->
                    <view
                      v-else-if="getMessageContent(message).type === 'groupJoining'"
                      class="group-joining-message"
                    >
                      <view class="group-joining-content">
                        <!-- 接龙标题 -->
                        <view class="joining-header">#接龙</view>

                        <!-- 接龙内容 -->
                        <view class="joining-text">{{ getGroupJoiningContent(message).text }}</view>

                        <!-- 发起人和参与人数 -->
                        <view class="joining-info">
                          {{ getGroupJoiningContent(message).creatorName }}发起，参与共{{
                            getGroupJoiningContent(message).participantCount
                          }}人
                        </view>

                        <!-- 参与人员列表 -->
                        <view class="joining-participants">
                          <view
                            v-for="(participant, pIndex) in getGroupJoiningContent(message)
                              .participants"
                            :key="participant.userId"
                            class="participant-item"
                          >
                            <text class="participant-index">{{ pIndex + 1 }}</text>
                            <van-image
                              v-if="participant.avatar"
                              :src="transformImageUrl(`/admin-api${participant.avatar}`)"
                              alt="用户头像"
                              class="preview-image"
                              fit="contain"
                            />
                            <van-image
                              v-else
                              :src="defaultImg"
                              alt="默认头像"
                              class="preview-image"
                              fit="contain"
                            />
                            <text class="participant-name">{{ participant.name }}</text>
                          </view>
                        </view>
                      </view>
                    </view>

                    <!-- 系统消息 -->
                    <view v-else class="system-message">
                      <text>{{ getMessageContent(message).content }}</text>
                    </view>
                  </view>
                </view>
              </view>
            </view>
          </view>

          <!-- 无法解析的群组消息 -->
          <view v-else-if="message.msgType === 10" class="system-message-item">
            <view class="system-content"> [系统消息] </view>
          </view>
        </template>
    </view>
    <!-- </scroll-view> -->

    <!-- 图片预览 -->
    <view v-if="showDialogFlag" class="image-preview" @click="showDialogFlag = false">
      <van-image
        :src="transformImageUrl(`/admin-api${imagePreviewUrl}`)"
        class="preview-image"
        fit="contain"
      />
    </view>

    <!-- 视频预览 -->
    <view v-if="showVideoDialog" class="video-preview" @click="showVideoDialog = false">
      <video
        :src="transformImageUrl(`/admin-api${videoPreviewUrl}`)"
        class="preview-video"
        controls
        autoplay
        controlslist="nodownload nofullscreen noremoteplayback"
        :show-fullscreen-btn="false"
      ></video>
      <view class="video-close">×</view>
    </view>
    <!-- 合并转发的弹框 -->
    <view class="modal-overlay" v-if="showForwardModal" @click="closeModal">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <div class="modal-left"></div>
          <text class="modal-title">[合并转发]群聊的聊天记录</text>
          <view class="close-btn" @click="closeModal">×</view>
        </view>
        <view class="modal-body">
          <ForwardMessages
            :messages="forwardModalContent"
            :member-list="allMemberList"
            @lookImage="lookImage"
            @lookVideo="lookVideo"
          />
        </view>
      </view>
    </view>
    <van-loading v-if="showLoading" size="24px" color="#1989fa" class="custom-loading">
      下载文件中...
    </van-loading>
    <van-action-sheet
      v-model:show="showActionSheetVisible"
      :actions="actionSheetActions"
      cancel-text="取消"
      @select="onActionSelect"
      @cancel="onActionCancel"
      title="选择操作"
    />
  </view>
</template>

<script setup>
  import { showDialog, showSuccessToast, showToast } from 'vant';
  import { ref, computed, onMounted, watch, nextTick, onUnmounted } from 'vue';

  import SearchInput from './searchInput.vue';

  import { groupApi } from '@/common/api/index.js';
  import EmojiDisplay from '@/pages/collaborativeGroup/emojiDisplay.vue';
  import ForwardMessages from '@/pages/collaborativeGroup/forwardMessage.vue';
  import GroupNotice from '@/pages/collaborativeGroup/groupNotice.vue';
  import MsgCard from '@/pages/collaborativeGroup/msgCard.vue';
  import MsgForwardCard from '@/pages/collaborativeGroup/msgForwardCard.vue';
  import QuoteMsg from '@/pages/collaborativeGroup/quoteMsg.vue';
  import { useCommunicationStore } from '@/stores/communication.js';
  // import defaultImg from "@/static/5110/im_person.svg";
  import { transformImageUrl } from '@/utils/imgUrlParse.js';
  import { createInnerAudioContext } from '@/utils/innerAudioContext.js';
  import { openDocument } from '@/utils/openDocument.js';
  import { setClipboardData } from '@/utils/setClipboardData.js';

  // import EmojiView from './emojiView.vue'
  const isProcessing = ref(false);
  const showLoading = ref(false);
  const showActionSheetVisible = ref(false);
  const myFileName = ref('');
  const myFileUrl = ref('');
  const myFileSize = ref('');
  const actionSheetActions = ref([
    // { name: '在浏览器中打开', value: 'browser' },
    { name: '下载', value: 'download' },
    { name: '复制链接', value: 'copy' },
  ]);
  // Props
  const props = defineProps({
    groupId: {
      type: String,
      required: true,
    },
    groupName: {
      type: String,
      required: false,
    },
    // 历史消息中：群组通知不将姓名替换为“你”
    disableYouAlias: {
      type: Boolean,
      default: false,
    },
  });
  const communicationStore = useCommunicationStore();

  // 响应式数据
  const showDialogFlag = ref(false);
  const showVideoDialog = ref(false);
  const imagePreviewUrl = ref('');
  const videoPreviewUrl = ref('');

  // 聊天历史数据
  const chatHistory = ref([]);
  const isLoadingMore = ref(false);
  const hasMore = ref(true);
  const currentPage = ref(1);
  const pageSize = ref(40);
  const isInitialLoad = ref(true);
  const searchVersion = ref(0);

  const groupMembers = ref([]);

  const chatMessagesRef = ref(null);

  // 用户信息
  const userInfo = ref({});
  const userId = computed(() => userInfo.value?.userid);
  const xietongId = computed(() => userInfo.value?.cooperationUser?.userId);

  // 滚动相关
  const scrollTop = ref(0);
  const oldScrollHeight = ref(0);

  // 合并转发相关
  const showForwardModal = ref(false);
  const forwardModalContent = ref([]);

  const showForwardDetail = (value) => {
    forwardModalContent.value = value.forwardMsgs?.sort(
      (a, b) => parseInt(a.time) - parseInt(b.time),
    );
    console.log(forwardModalContent.value);
    showForwardModal.value = true;
  };
  const allMemberList = ref([]);
  const getAllMemberList = (value) => {
    allMemberList.value = value;
  };
  // 关闭弹框
  function closeModal() {
    showForwardModal.value = false;
  }

  // 获取用户信息
  async function getUserInfo() {
    try {
      const res = await communicationStore.getUserInfo();
      if (res) {
        userInfo.value = res;
      } else {
        console.error('获取用户信息失败或WeSpaceSDK不可用');
      }
    } catch (error) {
      console.error(`获取用户信息错误: ${error.message}`);
    }
  }
  // 语音播放相关
  const currentPlayingAudio = ref(null);
  const currentPlayingMsgId = ref('');

  // 播放语音消息
  const playVoiceMessage = async (message) => {
    const voiceContent = getMessageContent(message).content;
    const voiceUrl = transformImageUrl(`/admin-api${voiceContent.filePath}`);

    // 如果正在播放的是当前消息，则停止播放
    if (currentPlayingMsgId.value === message.msgId && currentPlayingAudio.value) {
      stopCurrentVoice();
      return;
    }

    // 停止当前播放的语音
    stopCurrentVoice();

    try {
      // 创建新的音频上下文
      const innerAudioContext = createInnerAudioContext();
      innerAudioContext.src = voiceUrl;

      // 设置播放状态
      currentPlayingAudio.value = innerAudioContext;
      currentPlayingMsgId.value = message.msgId;

      // 监听播放事件
      innerAudioContext.onPlay(() => {
        console.log('开始播放语音');
      });

      // 监听结束事件
      innerAudioContext.onEnded(() => {
        console.log('语音播放结束');
        stopCurrentVoice();
      });

      // 监听错误事件
      innerAudioContext.onError((err) => {
        console.error('语音播放错误:', err);
        stopCurrentVoice();
        showToast('播放失败');
      });

      // 开始播放
      await innerAudioContext.play();
    } catch (error) {
      console.error('播放语音失败:', error);
      showToast('播放失败');
    }
  };

  // 停止当前播放的语音
  const stopCurrentVoice = () => {
    if (currentPlayingAudio.value) {
      currentPlayingAudio.value.stop();
      currentPlayingAudio.value.destroy();
      currentPlayingAudio.value = null;
    }
    currentPlayingMsgId.value = '';
  };

  // 判断是否正在播放
  const isVoicePlaying = (message) => {
    return currentPlayingMsgId.value === message.msgId;
  };

  // 获取完整的图片URL
  const getFullImageUrl = (path) => {
    console.log('url', path);
    if (!path) return '';
    // 这里需要根据实际服务器地址配置

    return path.startsWith('http') ? path : `http://10.28.64.83:30021${path}`;
  };

  // 自定义过滤函数，排除operationType=4的已读回执
  const customFilterMessage = (message) => {
    // 排除operationType=4的已读回执
    if (message.msgType === 4) {
      return false;
    }

    // 排除msgType=10且operationType=4的群组事件
    if (message.msg && message.msg.includes('operationType=4')) {
      return false;
    }

    return true;
  };

  // 解析群组事件消息
  const parseGroupEventMessage = (message) => {
    try {
      if (message.msgType === 10 && message.msg.includes('cmcontainer')) {
        // 处理 {cmcontainer={...}} 格式的消息
        const cmcontainerMatch = message.msg.match(/{cmcontainer=({.*})}/);
        if (cmcontainerMatch && cmcontainerMatch[1]) {
          let cmcontainerStr = cmcontainerMatch[1];

          // 转换为标准JSON格式
          let jsonStr = cmcontainerStr
            .replace(/([a-zA-Z_][a-zA-Z0-9_]*)=/g, '"$1":') // 将 key= 转换为 "key":
            .replace(/'/g, '"'); // 将单引号转换为双引号

          // 处理数组中的对象
          jsonStr = jsonStr.replace(/\[([^\]]+)\]/g, (match, arrayContent) => {
            if (arrayContent.includes('=')) {
              const arrayItems = arrayContent
                .split(/(?<=}),?\s*/)
                .map((item) => {
                  if (item.trim() && item.includes('=')) {
                    return `{${item.replace(/([a-zA-Z_][a-zA-Z0-9_]*)=/g, '"$1":').replace(/'/g, '"')}}`;
                  }
                  return item;
                })
                .filter((item) => item.trim());
              return `[${arrayItems.join(',')}]`;
            } else {
              // 处理空数组或简单数组
              return match;
            }
          });

          // 处理嵌套的对象（如newGroupProfile）
          // 找到所有key={...}格式的字符串并正确处理
          const nestedObjectRegex = /"([a-zA-Z_][a-zA-Z0-9_]*)":\{([^}]*)\}/g;
          jsonStr = jsonStr.replace(nestedObjectRegex, (match, key, content) => {
            // 处理嵌套对象内部的key=value格式
            const processedContent = content
              .replace(/([a-zA-Z_][a-zA-Z0-9_]*)=/g, '"$1":')
              .replace(/'/g, '"');
            return `"${key}":{${processedContent}}`;
          });

          try {
            const cmcontainer = JSON.parse(jsonStr);

            // 确保operationType存在
            if (cmcontainer.operationType === undefined) {
              // 尝试从字符串中提取operationType
              const opTypeMatch = cmcontainerStr.match(/operationType=(\d+)/);
              if (opTypeMatch) {
                cmcontainer.operationType = parseInt(opTypeMatch[1]);
              } else {
                cmcontainer.operationType = 0;
              }
            }

            // 确保memberList是数组
            if (cmcontainer.memberList === '[]') {
              cmcontainer.memberList = [];
            }

            return {
              ...message,
              msg: {
                cmcontainer: cmcontainer,
              },
            };
          } catch (parseError) {
            // 备用解析：直接提取关键字段
            const cmcontainer = {};
            const fields = ['operationType', 'operateId', 'groupId', 'operateRealUserId', 'tag'];

            fields.forEach((field) => {
              const regex = new RegExp(`${field}=([^,}]+)`);
              const match = cmcontainerStr.match(regex);
              if (match) {
                let value = match[1];
                // 尝试转换为数字
                if (/^\d+$/.test(value)) {
                  value = parseInt(value);
                }
                cmcontainer[field] = value;
              }
            });

            // 处理memberList
            const memberListMatch = cmcontainerStr.match(/memberList=\[([^\]]*)\]/);
            if (memberListMatch && memberListMatch[1]) {
              try {
                const members = memberListMatch[1]
                  .split(/(?<=}),?\s*/)
                  .filter((m) => m.trim())
                  .map((memberStr) => {
                    const member = {};
                    const memberFields = memberStr.match(/([a-zA-Z_][a-zA-Z0-9_]*)=([^,}]+)/g);
                    if (memberFields) {
                      memberFields.forEach((field) => {
                        const [key, value] = field.split('=');
                        member[key] = /^\d+$/.test(value) ? parseInt(value) : value;
                      });
                    }
                    return member;
                  });
                cmcontainer.memberList = members;
              } catch (e) {
                cmcontainer.memberList = [];
              }
            } else {
              cmcontainer.memberList = [];
            }

            // 处理newGroupProfile
            const newGroupProfileMatch = cmcontainerStr.match(/newGroupProfile=\{([^}]+)\}/);
            if (newGroupProfileMatch && newGroupProfileMatch[1]) {
              try {
                const profileStr = newGroupProfileMatch[1];
                const profile = {};

                // 解析newGroupProfile中的字段
                const profileFields = profileStr.split(',');
                profileFields.forEach((field) => {
                  const [key, value] = field.split('=');
                  if (key && value) {
                    profile[key.trim()] = /^\d+$/.test(value) ? parseInt(value) : value.trim();
                  }
                });

                cmcontainer.newGroupProfile = profile;
              } catch (e) {
                cmcontainer.newGroupProfile = {};
              }
            }

            // 处理coUserGroupRelations - 解析为数组格式
            const coUserGroupMatch = cmcontainerStr.match(/coUserGroupRelations=\[([^\]]*)\]/);
            if (coUserGroupMatch && coUserGroupMatch[1]) {
              try {
                const relationsStr = coUserGroupMatch[1].trim();
                const relations = {};

                // 解析花括号内的字段
                const fieldStr = relationsStr.replace(/^\{|\}$/g, '').trim();
                const relationsArr = fieldStr.split(',');
                relationsArr.forEach((field) => {
                  const [key, value] = field.split('=');
                  if (key && value) {
                    relations[key.trim()] = /^\d+$/.test(value) ? parseInt(value) : value.trim();
                  }
                });
                // 存储为数组格式（与PC版本保持一致）
                cmcontainer.coUserGroupRelations = [relations];
              } catch (e) {
                cmcontainer.coUserGroupRelations = [];
              }
            }

            return {
              ...message,
              msg: {
                cmcontainer: cmcontainer,
              },
            };
          }
        }
      }
      return message;
    } catch (error) {
      return message;
    }
  };

  // 处理后的消息列表
  const processedMessages = ref([]);
  const allChatHistorySet = ref(new Set());
  // 异步处理消息列表
  const processMessages = async () => {
    isProcessing.value = true;
    try {
      const members = groupMembers.value;
      //将所有消息存入allChatHistorySet,防止查询时获取不到引用消息
      chatHistory.value.forEach((item) => allChatHistorySet.value.add(item));
      // 先过滤消息
      const filteredMessages = chatHistory.value.filter((item) => {
        // 使用PC版本的过滤逻辑
        // 排除operationType=4的已读回执
        if (item.msgType === 4) {
          return false;
        }

        // 排除msgType=10且operationType=4的群组事件
        // if (item.msg && item.msg.includes("operationType=4")) {
        //   return false;
        // }

        return true;
      });

      // 收集所有需要获取用户信息的 fromRealUserId
      const needFetchUserIds = new Set();
      let userInfoMap = new Map();
      filteredMessages.forEach((item) => {
        if (item.fromRealUserId && item.fromRealUserId !== '' && item.msgType !== 10) {
          // 检查是否已经在群成员中
          const existsInMembers = members.find((member) => member.id == item.fromRealUserId);
          if (!existsInMembers) {
            needFetchUserIds.add(item.fromRealUserId);
          } else {
            userInfoMap.set(item.fromRealUserId, existsInMembers);
          }
        }
        if (item.from && item.fromRealUserId === '' && item.msgType !== 10) {
          // 检查是否已经在群成员中
          const existsInMembers = members.find((member) => member.from == item.from);
          if (!needFetchUserIds.has(item.from)) {
            if (!existsInMembers) {
              needFetchUserIds.add(item.from);
            } else {
              userInfoMap.set(item.from, existsInMembers);
            }
          }
        }
      });

      // 批量获取用户信息
      if (needFetchUserIds.size > 0) {
        try {
          const userIdsArray = Array.from(needFetchUserIds).filter((id) => id && id !== '');
          if (userIdsArray.length > 0) {
            const userInfos = await groupApi.getUserInfo({
              userIds: userIdsArray,
            });
            if (userInfos.results && Array.isArray(userInfos.results)) {
              userInfos.results.forEach((user) => {
                userInfoMap.set(user.id, user);
              });
            }
          }
        } catch (error) {
          console.error('批量获取用户信息失败:', error);
        }
      }

      const needFetchRealUserIds = new Set();
      const needFetchRealUsesInfosMap = new Map();
      // 处理每条消息
      const processed = filteredMessages.map((item) => {
        // 处理群组事件消息
        if (item.msgType === 10) {
          const parsedMessage = parseGroupEventMessage(item);
          return parsedMessage;
        }

        // 处理用户信息 - 优先使用群成员信息
        let fromUser = {};

        // 如果群成员信息存在，优先使用群成员中的姓名和头像
        const groupMember = members.find((member) => member.id == item.from);
        if (groupMember) {
          fromUser = {
            ...fromUser,
            name: groupMember.name,
            avatar: groupMember.tumbAvatar, // 使用 tumbAvatar 字段作为头像
          };
        }

        let name = fromUser.name || `用户${item.from}`;
        let realName = '';
        // 如果有 fromRealUserId，从批量获取的用户信息中查找
        if (item.fromRealUserId) {
          const userInfo = userInfoMap.get(item.fromRealUserId);
          if (userInfo && userInfo.name) {
            if (fromUser.name) {
              name += `(${userInfo.name})`;
            } else {
              name = userInfo.name;
            }
            realName = userInfo.name;
            if (groupMember) {
              groupMember.realName = name;
            }
          }
        }

        if (item.fromRealUserId === '' && item.from && !groupMember) {
          const userInfo = userInfoMap.get(item.from);
          if (userInfo && userInfo.name) {
            name = userInfo.name;
            realName = userInfo.name;
            fromUser.avatar = userInfo.avatar;
          }
        }
        //如果没有名字且item.from不为空，添加到set重新查询
        if (item.fromRealUserId && !fromUser.name && item.from) {
          name = '';
          needFetchRealUserIds.add(item.from);
        }
        return {
          ...item,
          name,
          realName,
          avatar: fromUser.avatar,
          showTime: false,
        };
      });

      // 批量获取用户信息
      if (needFetchRealUserIds.size > 0) {
        try {
          const realUserIdsArray = Array.from(needFetchRealUserIds).filter((id) => id && id !== '');
          const realUserInfos = await groupApi.getColloration({
            userIds: realUserIdsArray,
          });
          if (realUserInfos && Array.isArray(realUserInfos)) {
            realUserInfos.forEach((user) => {
              needFetchRealUsesInfosMap.set(user.id, user);
            });
          }
        } catch (error) {
          console.error('批量获取用户信息失败:', error);
        }
      }
      if (needFetchRealUsesInfosMap.size > 0) {
        processed.forEach((item) => {
          if (item.fromRealUserId && !item.name) {
            const realUserInfo = needFetchRealUsesInfosMap.get(item.from);
            if (realUserInfo && realUserInfo.postName) {
              item.name = `${realUserInfo.postName}(${item?.realName})`;
              const findItem = groupMembers.value.find((groupItem) => groupItem.id === item.from);
              if (!findItem) {
                groupMembers.value.push({
                  ...realUserInfo,
                  realName: `${realUserInfo.postName}(${item?.realName})`,
                });
              } else {
                findItem.realName = `${realUserInfo.postName}(${item?.realName})`;
              }
            }
          }
          if (!item.name) {
            item.name = `用户${item.from}(${item?.realName})`;
          }
        });
      }

      // 按时间排序
      processedMessages.value = processed.sort((a, b) => parseInt(a.time) - parseInt(b.time));
      // 非加载更多时（首次加载/搜索），消息处理完成并渲染后滚动到底部
      if (!isLoadingMore.value) {
        nextTick(() => {
          scrollToBottom();
        });
      }
    } finally {
      isProcessing.value = false;
    }
  };

  // 判断是否为有效的群组事件消息
  const isValidGroupEvent = (message) => {
    if (message.msgType !== 10) return false;
    if (!message.msg || !message.msg.cmcontainer) return false;

    const cmcontainer = message.msg.cmcontainer;
    const validOperationTypes = [1, 2, 3, 4, 5, 6, 7, 11]; // 有效的操作类型

    return validOperationTypes.includes(cmcontainer.operationType); //&&
    // cmcontainer.operationType !== 4 // 排除operationType=4
  };

  // 判断消息是否为自己发送
  const isSelf = (message) => {
    const currentUserId = userId.value;
    const xietongUserId = xietongId.value;
    return (
      Number(message.from) === Number(currentUserId) ||
      Number(message.from) === Number(xietongUserId) ||
      Number(message.fromRealUserId) === Number(currentUserId)
    );
  };

  // 解析普通消息内容
  const parseMessage = (message) => {
    try {
      if (message.msgType === 1) {
        // 文本消息 {text=内容} 或 {srcMsgId=xxx, text=内容}
        if (message.msg.startsWith('{') && message.msg.endsWith('}')) {
          const msgData = {};
          const cleanMsg = message.msg.replace(/{|}/g, '');

          const pairs = [];
          let currentPair = '';
          let inText = false;

          for (let i = 0; i < cleanMsg.length; i++) {
            const char = cleanMsg[i];

            if (char === ',' && !inText) {
              pairs.push(currentPair);
              currentPair = '';
            } else {
              currentPair += char;
            }

            // 检测到 text= 开始，标记进入文本内容
            if (currentPair.endsWith('text=')) {
              inText = true;
            }

            // 如果是最后一个字符且处于文本模式
            if (inText && i === cleanMsg.length - 1) {
              pairs.push(currentPair);
            }
          }

          // 处理最后一个pair
          if (currentPair && !pairs.includes(currentPair)) {
            pairs.push(currentPair);
          }

          pairs.forEach((pair) => {
            if (pair.startsWith('text=')) {
              const key = 'text';
              const value = pair.substring(5);
              msgData[key] = value === 'null' ? null : value;
            } else {
              const equalIndex = pair.indexOf('=');
              if (equalIndex > -1) {
                const key = pair.substring(0, equalIndex);
                let value = pair.substring(equalIndex + 1);
                msgData[key.trim()] = value === 'null' ? null : value;
              }
            }
          });
          return msgData;
        }
        return {
          text: message.msg,
        };
      } else if (message.msgType === 2) {
        // 文件消息
        try {
          const msgData = JSON.parse(message.msg);
          return msgData;
        } catch (jsonError) {
          // 备用解析逻辑
          const msgData = {};
          const cleanMsg = message.msg.replace(/{|}/g, '');
          const pairs = cleanMsg.split(',');
          pairs.forEach((pair) => {
            const [key, value] = pair.split('=');
            if (key && value) {
              const trimmedValue = value.trim();
              msgData[key.trim()] = trimmedValue === 'null' ? null : trimmedValue;
            }
          });
          return msgData;
        }
      } else if (message.msgType === 5) {
        // 个人名片消息
        try {
          return JSON.parse(message.msg);
        } catch (e) {
          return message.msg;
        }
      } else if (message.msgType === 6) {
        return parseGroupJoiningMessage(message);
      } else if (message.msgType === 8) {
        // 转发消息
        try {
          return JSON.parse(message.msg);
        } catch (e) {
          return message.msg;
        }
      }

      return message.msg;
    } catch (e) {
      console.error('解析消息失败:', e);
      return message.msg;
    }
  };

  // 解析群接龙消息
  const parseGroupJoiningMessage = (message) => {
    if (message.msgType === 6) {
      try {
        const result = JSON.parse(message.msg);
        return result;
      } catch (jsonError) {
        return message.msg;
      }
    }
    return message.msg;
  };

  const getGroupJoiningContent = (message) => {
    const content = getMessageContent(message).content;

    // 处理参与者数据
    let participants = [];
    let missingUserIds = [];
    if (content.userTxts && Array.isArray(content.userTxts)) {
      // 按时间戳排序，时间最早的排在最前面
      const sortedUserTxts = [...content.userTxts].sort((a, b) => {
        const timeA = a.timeStamp || a.clientTimeStamp || 0;
        const timeB = b.timeStamp || b.clientTimeStamp || 0;
        return timeA - timeB;
      });
      participants = sortedUserTxts.map((userTxt, index) => {
        const memberInfo = groupMembers.value.find((member) => member.id == userTxt.userId);
        if (memberInfo) {
          // 判断是否为协同岗身份
          const realName = memberInfo.realName || '';
          const postName = memberInfo.postName || '';
          let txt = (userTxt.txt || '').split(' ')[0];
          try {
            txt = txt.split(' ')[0];
          } catch {
            txt = userTxt.txt || '';
          }
          
          // 三种情况判断为协同岗身份：
          // 1. txt 等于岗位名
          // 2. txt 符合 "岗位名(真实姓名)" 格式
          // 3. txt 包含 "(真实姓名)" 但不以真实姓名开头
          const isCollaborationIdentity = 
            (postName && txt === postName) ||
            (postName && realName && txt === `${postName}(${realName})`) ||
            (realName && !txt.startsWith(realName) && txt.includes(`(${realName})`));
          // 获取用户头像（支持 avatar 和 tumbAvatar 两种字段名）
          const userAvatar = memberInfo.tumbAvatar || memberInfo.avatar || '';
          
          // 如果是协同岗身份，优先使用协同岗头像；否则使用普通用户头像
          const avatar = isCollaborationIdentity 
            ? (memberInfo.collaborationAvatar || userAvatar)
            : userAvatar;
          
          return {
            userId: userTxt.userId,
            name: userTxt.txt || `用户${userTxt.userId}`,
            avatar: avatar,
            timeStamp: userTxt.timeStamp || userTxt.clientTimeStamp,
            isCreator: index === 0, // 第一个参与者就是发起人
          };
        } else {
          // 将不在groupMembers里面的成员id收集起来
          missingUserIds.push(userTxt.userId);
          return {
            userId: userTxt.userId,
            name: userTxt.txt || `用户${userTxt.userId}`,
            avatar: '', // 暂时留空
            timeStamp: userTxt.timeStamp || userTxt.clientTimeStamp,
            isCreator: index === 0,
            isMissing: true, // 标记为需要获取信息的用户
          };
        }
      });
    }

    // 计算去重后的参与人数：协同岗账号和其对应的真实用户算作同一人
    const getRealUserIdForDedup = (userId) => {
      const member = groupMembers.value.find((m) => m.id == userId);
      if (!member || !member.postName) return userId.toString();
      if (member.name === member.postName) {
        const realUser = groupMembers.value.find(
          (m) => m.postName === member.postName && m.name !== member.postName,
        );
        return realUser ? realUser.id.toString() : userId.toString();
      }
      return userId.toString();
    };

    // 按真实用户去重计算人数，但参与者列表保留全部条目
    const uniqueRealUserIds = new Set();
    participants.forEach((p) => {
      uniqueRealUserIds.add(getRealUserIdForDedup(p.userId));
    });
    const participantCount = uniqueRealUserIds.size;

    // 获取发起人名称 - 第一个参与者就是发起人
    let creatorName = '未知用户';
    if (participants.length > 0) {
      creatorName = participants[0].name;
    } else if (content.creator) {
      // 如果没有参与者但有creator字段，使用creator
      const creatorUser = PIMStore.userMap.get(Number(content.creator)) || {};
      creatorName = creatorUser.name || `用户${content.creator}`;
    }

    return {
      text: content.text || '未命名接龙',
      creatorName: creatorName,
      participantCount: participantCount,
      participants: participants,
    };
  };

  // 获取消息显示内容
  const getMessageContent = (message) => {
    const parsedMsg = parseMessage(message);
    switch (message.msgType) {
      case 1: // 文本消息
        // 检查是否是引用消息
        if (parsedMsg.srcMsgId) {
          const quotedMsg = getQuotedMessage(parsedMsg.srcMsgId);
          return {
            type: 'quote',
            content: parsedMsg.text || message.msg,
            srcMsgId: parsedMsg.srcMsgId,
            quotedMsg: quotedMsg,
          };
        }
        return {
          type: 'text',
          content: parsedMsg.text || message.msg,
        };

      case 2: // 文件消息
        return {
          type: 'file',
          content: parsedMsg,
        };
      case 5: // 个人名片
        return {
          type: 'userCard',
          content: parsedMsg,
        };
      case 6: // 群接龙
        return {
          type: 'groupJoining',
          content: parsedMsg,
        };

      case 7: // 撤回消息
        return {
          type: 'system',
          content: '[消息已撤回]',
        };

      case 8: // 转发消息
        return {
          type: 'forward',
          content: parsedMsg,
        };
      case 10: // 群组变更事件
        return {
          type: 'groupEvent',
          content: parsedMsg,
        };

      default:
        return {
          type: 'unknown',
          content: `[未知消息类型: ${message.msgType}]`,
        };
    }
  };

  // 获取被引用的消息
  const getQuotedMessage = (srcMsgId) => {
    if (!srcMsgId || srcMsgId === 'null') return null;
    const allChatList = Array.from(allChatHistorySet.value);
    const foundMsg = allChatList.find((msg) => {
      return msg.msgId === srcMsgId;
    });
    return foundMsg || null;
  };

  // 处理@消息显示
  const formatTextMessage = (text) => {
    if (!text) return '';
    return text.replace(/\[@(\d+):@([^\]]+)\]/g, (match, userId, userName) => {
      if (userName === 'all') {
        return '@所有人 ';
      }
      return '@' + userName + ' ';
    });
  };

  // 格式化文件大小
  const formatFileSize = (bytes) => {
    if (!bytes) return '';
    if (bytes < 1024) return bytes + 'B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB';
    return (bytes / (1024 * 1024)).toFixed(1) + 'MB';
  };

  // 格式化日期
  const formatDate = (timestamp) => {
    const date = new Date(timestamp);
    const now = new Date();
    const isToday = date.toDateString() === now.toDateString();
    const isYesterday = new Date(now - 86400000).toDateString() === date.toDateString();

    if (isToday) {
      return `今天 ${date.getHours().toString().padStart(2, '0')}:${date
        .getMinutes()
        .toString()
        .padStart(2, '0')}`;
    } else if (isYesterday) {
      return `昨天 ${date.getHours().toString().padStart(2, '0')}:${date
        .getMinutes()
        .toString()
        .padStart(2, '0')}`;
    } else {
      return `${date.getMonth() + 1}月${date.getDate()}日 ${date
        .getHours()
        .toString()
        .padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
    }
  };

  // 格式化时间（仅时间部分）
  const formatTime = (timestamp) => {
    const date = new Date(timestamp);
    const year = date.getFullYear();
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const day = date.getDate().toString().padStart(2, '0');
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    const seconds = date.getSeconds().toString().padStart(2, '0');

    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
  };
  // 查看图片
  const lookImage = (url) => {
    imagePreviewUrl.value = url;
    showDialogFlag.value = true;
  };

  // 查看视频
  const lookVideo = (url) => {
    videoPreviewUrl.value = url;
    showVideoDialog.value = true;
  };

  // 格式化时间参数（开始时间末尾加 :00，结束时间末尾加 :59）
  const formatTimeParam = (time, isEnd = false) => {
    if (!time) return '';
    // 如果已经包含秒，直接返回
    if (time.includes(':') && time.split(':').length === 3) {
      return time;
    }
    // 否则添加秒
    return isEnd ? `${time}:59` : `${time}:00`;
  };

  // 获取聊天历史记录
  const fetchChatHistory = async (page = 1, isLoadMore = false, version = undefined) => {
    if (!props.groupId || (isLoadMore && !hasMore.value) || isLoadingMore.value) return;
    isLoadingMore.value = true;
    try {
      const res = await groupApi.getChatHistory({
        groupId: props.groupId,
        pageNum: page,
        pageSize: pageSize.value,
        keywords: searchParams.value.keyword,
        startTime: formatTimeParam(searchParams.value.startTime, false),
        endTime: formatTimeParam(searchParams.value.endTime, true),
        from: selectedMemberId.value, // 新增：群成员筛选
      });

      // 非加载更多时校验版本号，版本号不一致说明已有新搜索，丢弃本次响应
      if (!isLoadMore && version !== undefined && version !== searchVersion.value) {
        return;
      }

      if (res && res.records) {
        if (isLoadMore) {
          chatHistory.value = [...res.records, ...chatHistory.value];
          nextTick(() => {
            // 获取聊天记录后，补充缺失成员
            fetchAllMissingMembersFromChatHistory();
          });
        } else {
          chatHistory.value = res.records;
          nextTick(() => {
            // 获取聊天记录后，补充缺失成员
            fetchAllMissingMembersFromChatHistory();
          });
        }

        hasMore.value = res.records.length === pageSize.value;
        currentPage.value = page;
      } else {
        // 没有数据时设置hasMore为false
        hasMore.value = false;
      }
    } catch (error) {
      console.error('获取聊天记录失败:', error);
      // 错误时也设置hasMore为false，避免一直加载
      hasMore.value = false;
    } finally {
      isLoadingMore.value = false;
      isInitialLoad.value = false;
    }
  };

  // 获取群成员信息

  const getGroupMembers = async () => {
    const res = await groupApi.getChatHistoryCount({
      groupId: props.groupId,
      pageSize: 100,
    });
    if (res) {
      groupMembers.value = res.records || [];
      // 补充协同岗信息（头像优先使用协同岗头像）
      await supplementCollaborationInfo();
      await fetchAllMissingMembersFromChatHistory();
    } else {
      groupMembers.value = [];
    }
  };

  // 补充协同岗信息（头像优先使用协同岗头像）
  const supplementCollaborationInfo = async () => {
    try {
      const memberIds = groupMembers.value.map((m) => m.id.toString()).filter((id) => id);
      if (memberIds.length === 0) return;

      // 获取协同岗信息
      const collorationRes = await groupApi.getColloration({ userIds: memberIds });
      if (collorationRes && Array.isArray(collorationRes)) {
        // 构建两个映射：
        // 1. 协同岗ID -> 协同岗信息（协同岗本身）
        // 2. 关联用户ID -> 协同岗信息（关联的真实用户）
        const collorationMap = new Map();
        const relatedUserMap = new Map();
        
        collorationRes.forEach((item) => {
          if (item && item.id) {
            // 映射1：协同岗ID -> 协同岗信息
            collorationMap.set(item.id.toString(), {
              postName: item.postName,
              iconUrl: item.iconUrl, // 协同岗头像
            });
            
            // 映射2：关联用户ID -> 协同岗信息
            if (item.relatedUserIds) {
              item.relatedUserIds.split(',').forEach((userId) => {
                relatedUserMap.set(userId.trim(), {
                  postName: item.postName,
                  iconUrl: item.iconUrl, // 协同岗头像
                  realName: item.relatedUserNames, // 关联用户的真实名称
                });
              });
            }
          }
        });
        
        // 更新群成员信息，添加协同岗头像字段
        groupMembers.value = groupMembers.value.map((member) => {
          // 先确保所有成员都有 realName
          const baseMember = {
            ...member,
            realName: member.realName || member.name,
          };
          
          // 先检查是否是协同岗本身
          const collorationInfo = collorationMap.get(member.id.toString());
          if (collorationInfo) {
            return {
              ...baseMember,
              // 保留协同岗头像字段，用于群接龙中判断身份后使用
              collaborationAvatar: collorationInfo.iconUrl,
              // 保留岗位名称，用于群接龙身份判断
              postName: collorationInfo.postName,
            };
          }
          
          // 再检查是否是关联用户
          const relatedInfo = relatedUserMap.get(member.id.toString());
          if (relatedInfo) {
            return {
              ...baseMember,
              // 保留协同岗头像字段，用于群接龙中判断身份后使用
              collaborationAvatar: relatedInfo.iconUrl,
              // 保留岗位名称，用于群接龙身份判断
              postName: relatedInfo.postName,
            };
          }
          return baseMember;
        });
      }
    } catch (error) {
      console.error('补充协同岗信息失败:', error);
    }
  };

  // 获取聊天记录中涉及的所有非本群成员
  const fetchAllMissingMembersFromChatHistory = async () => {
    try {
      // 从聊天记录中提取所有用户ID
      const allUserIds = new Set();

      chatHistory.value.forEach((message) => {
        // 消息发送者
        if (message.from) {
          allUserIds.add(message.from.toString());
        }

        // 群接龙参与者
        if (message.msgType === 6) {
          try {
            const content = JSON.parse(message.msg);
            if (content.userTxts && Array.isArray(content.userTxts)) {
              content.userTxts.forEach((userTxt) => {
                if (userTxt.userId) {
                  allUserIds.add(userTxt.userId.toString());
                }
              });
            }
          } catch (e) {}
        }

        // 协同岗上下岗消息 (operationType=11) - 收集 realUserId 和 userId
        if (message.msgType === 10) {
          try {
            // 尝试从原始消息字符串中提取 coUserGroupRelations
            const msgStr = message.msg || '';
            if (msgStr.includes('operationType=11') && msgStr.includes('coUserGroupRelations')) {
              // 提取 realUserId
              const realUserIdMatch = msgStr.match(/realUserId=(\d+)/);
              if (realUserIdMatch && realUserIdMatch[1]) {
                allUserIds.add(realUserIdMatch[1]);
              }
              // 提取 userId（协同岗ID）
              const userIdMatch = msgStr.match(/userId=(\d+)/);
              if (userIdMatch && userIdMatch[1]) {
                allUserIds.add(userIdMatch[1]);
              }
            }
          } catch (e) {
            console.error('[ChatHistory] 解析协同岗消息失败:', e);
          }
        }
      });

      // 从聊天记录中提取所有协同岗ID（用于查询协同岗信息，获取关联用户信息）
      const collabUserIds = new Set();
      chatHistory.value.forEach((message) => {
        if (message.msgType === 10) {
          try {
            const msgStr = message.msg || '';
            if (msgStr.includes('operationType=11') && msgStr.includes('coUserGroupRelations')) {
              const userIdMatch = msgStr.match(/userId=(\d+)/);
              if (userIdMatch && userIdMatch[1]) {
                collabUserIds.add(userIdMatch[1]);
              }
            }
          } catch {}
        }
      });
      // 过滤掉已经在群成员中的用户
      const existingIds = groupMembers.value.map((m) => m.id.toString());
      const missingIds = Array.from(allUserIds).filter((id) => !existingIds.includes(id));
      if (missingIds.length > 0) {
        // 先从已有的群成员中提取协同岗信息（用于匹配关联用户）
        const existingRelatedUserMap = new Map();
        groupMembers.value.forEach((member) => {
          if (member.postName && member.collaborationAvatar) {
            // 该成员有协同岗信息，可能是协同岗本身或关联用户
            existingRelatedUserMap.set(member.id.toString(), {
              postName: member.postName,
              iconUrl: member.collaborationAvatar,
              realName: member.realName,
            });
          }
        });
        
        // 并发获取警信用户信息和协同岗信息
        // 注意：getColloration 需要用协同岗ID查询，而不是关联用户ID
        const collabIdsToQuery = [...collabUserIds].filter((id) => !existingIds.includes(id));
        const [userRes, collorationRes] = await Promise.all([
          groupApi.getUserInfo({ userIds: missingIds }),
          groupApi.getColloration({ userIds: collabIdsToQuery.length > 0 ? collabIdsToQuery : missingIds }),
        ]);

        // 构建两个映射：
        // 1. 协同岗ID -> 协同岗信息（协同岗本身）
        // 2. 关联用户ID -> 协同岗信息（关联的真实用户）
        const collorationMap = new Map();
        const relatedUserMap = new Map(existingRelatedUserMap); // 初始化为已有的协同岗信息
        if (collorationRes && Array.isArray(collorationRes)) {
          collorationRes.forEach((item) => {
            if (item && item.id) {
              // 映射1：协同岗ID -> 协同岗信息
              collorationMap.set(item.id.toString(), {
                postName: item.postName,
                iconUrl: item.iconUrl, // 协同岗头像
              });
              
              // 映射2：关联用户ID -> 协同岗信息
              if (item.relatedUserIds) {
                item.relatedUserIds.split(',').forEach((userId) => {
                  relatedUserMap.set(userId.trim(), {
                    postName: item.postName,
                    iconUrl: item.iconUrl, // 协同岗头像
                    realName: item.relatedUserNames, // 关联用户的真实名称
                  });
                });
              }
            }
          });
        }
        
        // 从已有群成员中查找协同岗成员，提取其关联用户信息
        // 这样即使关联用户不在群成员列表中，也能获取到协同岗信息
        const collaborationMembers = groupMembers.value.filter((m) => m.postName && m.collaborationAvatar);
        
        for (const collabMember of collaborationMembers) {
          // 查询该协同岗的详细信息，获取关联用户ID
          try {
            const collabDetailRes = await groupApi.getColloration({ userIds: [collabMember.id.toString()] });
            if (collabDetailRes && Array.isArray(collabDetailRes)) {
              collabDetailRes.forEach((item) => {
                if (item && item.relatedUserIds) {
                  item.relatedUserIds.split(',').forEach((userId) => {
                    const trimmedUserId = userId.trim();
                    // 为所有关联用户添加映射（无论是否在 missingIds 中）
                    relatedUserMap.set(trimmedUserId, {
                      postName: item.postName,
                      iconUrl: item.iconUrl,
                      realName: item.relatedUserNames,
                    });
                  });
                }
              });
            }
          } catch (e) {
            console.error('获取协同岗详细信息失败:', e);
          }
        }
        // 为已有的群成员补充协同岗信息（如果他们之前没有获取到）
        groupMembers.value = groupMembers.value.map((member) => {
          const memberIdStr = member.id.toString();
          // 如果该成员没有协同岗信息，检查是否在 relatedUserMap 中
          if (!member.collaborationAvatar && !member.postName) {
            const relatedInfo = relatedUserMap.get(memberIdStr);
            if (relatedInfo) {
              return {
                ...member,
                collaborationAvatar: relatedInfo.iconUrl,
                postName: relatedInfo.postName,
              };
            }
          }
          return member;
        });

        // 构建要添加的成员列表
        const additionalMembers = [];

        // 1. 从 getUserInfo 结果中添加用户
        if (userRes?.results) {
          additionalMembers.push(...userRes.results.map((user) => {
            // 先检查是否是协同岗本身
            const collorationInfo = collorationMap.get(user.id.toString());
            if (collorationInfo) {
              return {
                id: user.id,
                name: user.name || `用户${user.userId}`,
                tumbAvatar: user.avatar || '',
                realName: user.name || '',
                collaborationAvatar: collorationInfo.iconUrl,
                postName: collorationInfo.postName,
                isExternal: true, // 标记为非本群成员
              };
            }
            
            // 再检查是否是关联用户
            const relatedInfo = relatedUserMap.get(user.id.toString());
            if (relatedInfo) {
              return {
                id: user.id,
                name: user.name || `用户${user.userId}`,
                tumbAvatar: user.avatar || '',
                realName: user.name || '',
                collaborationAvatar: relatedInfo.iconUrl,
                postName: relatedInfo.postName,
                isExternal: true, // 标记为非本群成员
              };
            }
            
            // 普通用户
            return {
              id: user.id,
              name: user.name || `用户${user.userId}`,
              tumbAvatar: user.avatar || '',
              realName: user.name || '',
              isExternal: true, // 标记为非本群成员
            };
          }));
        }

        // 2. 对于 missingIds 中还没有被处理的用户，从 relatedUserMap 中添加
        const processedIds = new Set(additionalMembers.map((m) => m.id.toString()));
        for (const missingId of missingIds) {
          if (!processedIds.has(missingId)) {
            const relatedInfo = relatedUserMap.get(missingId);
            if (relatedInfo) {
              additionalMembers.push({
                id: missingId,
                name: relatedInfo.realName || `用户${missingId}`,
                tumbAvatar: '',
                realName: relatedInfo.realName || '',
                collaborationAvatar: relatedInfo.iconUrl,
                postName: relatedInfo.postName,
                isExternal: true,
              });
            }
          }
        }

        if (additionalMembers.length > 0) {
          groupMembers.value = [...groupMembers.value, ...additionalMembers];
        }
      }
    } catch (error) {
      console.error('获取非本群成员信息失败:', error);
    }
  };

  // 滚动到底部
  const scrollToBottom = () => {
    nextTick(() => {
      const container = chatMessagesRef.value;
      if (container) {
        container.scrollTop = container.scrollHeight;
      }
    });
  };

  // 加载更多历史消息
  const loadMoreMessages = async () => {
    if (!hasMore.value || isLoadingMore.value) return;
    // 记录加载前的滚动高度，用于加载后恢复滚动位置
    const container = chatMessagesRef.value;
    const prevScrollHeight = container ? container.scrollHeight : 0;
    await fetchChatHistory(currentPage.value + 1, true);
    // 加载更多后恢复滚动位置，防止新数据插入顶部导致视图跳动
    if (container) {
      nextTick(() => {
        const newScrollHeight = container.scrollHeight;
        container.scrollTop = newScrollHeight - prevScrollHeight;
      });
    }
  };

  // 滚动事件处理：滚动到顶部时加载更多历史消息
  const handleScroll = (event) => {
    const { scrollTop } = event.target;
    // 滚动到顶部附近（阈值10px）时触发加载
    if (scrollTop <= 10 && hasMore.value && !isLoadingMore.value) {
      loadMoreMessages();
    }
  };

  // 监听groupId变化
  watch(
    () => props.groupId,
    (newVal) => {
      if (newVal) {
        currentPage.value = 1;
        hasMore.value = true;
        isInitialLoad.value = true;
        chatHistory.value = [];
        groupMembers.value = [];
        processedMessages.value = [];
        const currentVersion = ++searchVersion.value;
        getGroupMembers();
        fetchChatHistory(1, false, currentVersion);
      }
    },
  );

  // 监听 chatHistory 和 groupMembers 变化，自动处理消息 //()=>groupMembers.value
  watch(
    [() => chatHistory.value],
    () => {
      if (chatHistory.value.length > 0 && groupMembers.value.length > 0) {
        processMessages();
      }
    },
    { deep: true },
  );
  const lookFile = (fileContent) => {
    console.log('=== 修复版文件打开 ===');

    if (!fileContent.filePath) {
      showToast('文件路径为空');
      return;
    }
    // 构建完整URL
    let fileUrl = fileContent.filePath;
    if (!fileUrl.startsWith('http')) {
      fileUrl = transformImageUrl(`/admin-api${fileContent.filePath}`);
      console.log('文件路径：', fileUrl);
    }

    const fileName = fileContent.fileName || '文件';

    console.log('文件信息:', { fileUrl, fileName });

    // 直接使用URL打开，避免blob问题
    openFileDirectly(fileUrl, fileName, fileContent.fileSize);
  };

  // 直接使用URL打开文件
  const openFileDirectly = (fileUrl, fileName, fileSize) => {
    console.log('直接打开文件:', fileUrl);
    // 显示选项让用户选择打开方式
    myFileUrl.value = fileUrl;
    myFileName.value = fileName;
    myFileSize.value = String(fileSize);
    showActionSheetVisible.value = true;
  };
  const onActionSelect = (action, index) => {
    console.log('选择了:', action, '索引:', index);
    const actionValue = action.value;
    // 根据选择的选项执行相应操作
    switch (actionValue) {
      case 'browser':
        // 在浏览器中打开
        openInBrowser(myFileUrl.value);
        break;
      case 'download':
        // 下载
        handleDownload(myFileUrl.value, myFileName.value, myFileSize.value);
        break;
      case 'copy':
        // 复制链接
        copyFileLink(myFileUrl.value, myFileName.value);
        break;
    }

    // 关闭 ActionSheet
    showActionSheetVisible.value = false;
  };

  const onActionCancel = () => {
    console.log('用户取消操作');
    showActionSheetVisible.value = false;
  };

  // 在浏览器中打开
  const openInBrowser = (fileUrl) => {
    const fullUrl = `${window.location.origin}${fileUrl}` 
    console.log('在浏览器中打开:', fullUrl);
    communicationStore.openUrl(fullUrl, null, 'noTitleStyle')
  };

  const handleDownload = async (fileUrl, fileName, fileSize) => {
    try {
      const fullUrl = `${window.location.origin}${fileUrl}` 
      showLoading.value = true;
      await communicationStore.downloadFile({ url: fullUrl, fileName, fileSize });
    } finally {
      showLoading.value = false;
    }
  }

  // 下载并打开文件（修复blob问题）
  const downloadAndOpen = async (fileUrl, fileName) => {
    await handleDownload(fileUrl, fileName);
    openInBrowser(fileUrl);
  };

  // 打开blob文件
  const openBlobFile = (fileURL, fileName, blob) => {
    console.log('打开blob文件:', fileURL);

    // 在App环境中，我们需要将blob保存为临时文件
    if (typeof plus !== 'undefined') {
      saveBlobToTempFile(blob, fileName)
        .then((tempFilePath) => {
          if (tempFilePath) {
            openSavedFile(tempFilePath, fileName);
          } else {
            openInBrowser(fileURL);
          }
        })
        .catch((error) => {
          console.error('保存blob失败:', error);
          openInBrowser(fileURL);
        });
    } else {
      // H5环境直接使用blob URL
      window.open(fileURL, '_blank');
    }
  };

  // 将blob保存为临时文件（App环境）
  const saveBlobToTempFile = (blob, fileName) => {
    return new Promise((resolve, reject) => {
      if (typeof plus === 'undefined') {
        reject(new Error('非App环境'));
        return;
      }

      // 读取blob数据
      const reader = new FileReader();
      reader.onload = function (e) {
        const arrayBuffer = e.target.result;

        // 生成临时文件路径
        const tempDir = plus.io.convertLocalFileSystemURL('_doc/') + 'temp/';
        const tempFilePath = tempDir + fileName;

        // 确保目录存在
        plus.io.resolveLocalFileSystemURL(
          tempDir,
          (entry) => {
            writeFile(tempFilePath, arrayBuffer, resolve, reject);
          },
          (error) => {
            // 目录不存在，创建目录
            plus.io.resolveLocalFileSystemURL(
              '_doc/',
              (rootEntry) => {
                rootEntry.getDirectory(
                  'temp',
                  { create: true },
                  (dirEntry) => {
                    writeFile(tempFilePath, arrayBuffer, resolve, reject);
                  },
                  reject,
                );
              },
              reject,
            );
          },
        );
      };

      reader.onerror = reject;
      reader.readAsArrayBuffer(blob);
    });
  };

  // 写入文件
  const writeFile = (filePath, arrayBuffer, resolve, reject) => {
    plus.io.resolveLocalFileSystemURL(
      filePath,
      (entry) => {
        // 文件已存在，直接使用
        resolve(filePath);
      },
      (error) => {
        // 文件不存在，创建文件
        plus.io.resolveLocalFileSystemURL(
          '_doc/temp/',
          (dirEntry) => {
            dirEntry.getFile(
              filePath.split('/').pop(),
              { create: true },
              (fileEntry) => {
                fileEntry.createWriter((writer) => {
                  writer.onwrite = function () {
                    resolve(filePath);
                  };
                  writer.onerror = reject;
                  const blob = new Blob([arrayBuffer]);
                  writer.write(blob);
                }, reject);
              },
              reject,
            );
          },
          reject,
        );
      },
    );
  };

  // 打开已保存的文件
  const openSavedFile = (filePath, fileName) => {
    console.log('打开已保存的文件:', filePath);

    const fileType = getFileType(fileName);

    openDocument({
      filePath: filePath,
      fileType: fileType,
      success: () => {
        console.log('文件打开成功');
        showSuccessToast('文件已打开');
      },
      fail: (err) => {
        console.error('文件打开失败:', err);
        showToast('打开失败，请安装相关应用');
      },
    });
  };

  // 复制文件链接
  const copyFileLink = (fileUrl, fileName) => {

    const fullUrl = `${window.location.origin}${fileUrl}` 

    setClipboardData({
      data: fullUrl,
      success: () => {
        showSuccessToast('链接已复制');
        // 提示用户
        setTimeout(() => {
          showDialog({
            title: '操作提示',
            message: `文件链接已复制到剪贴板\n\n文件: ${fileName}\n\n您可以:\n1. 在浏览器中粘贴打开\n2. 分享给其他人`,
            showCancelButton: false,
            confirmButtonText: '知道了',
            messageAlign: 'left', // 左对齐，更适合多行文本
          }).then(() => {
            console.log('用户点击了确定');
          });
        }, 1000);
      },
    });
  };

  // 文件类型判断
  const getFileType = (fileName) => {
    const extension = fileName.split('.').pop()?.toLowerCase() || '';
    console.log('文件扩展名:', extension);

    const typeMap = {
      pdf: 'pdf',
      doc: 'doc',
      docx: 'doc',
      xls: 'xls',
      xlsx: 'xls',
      ppt: 'ppt',
      pptx: 'ppt',
      txt: 'txt',
      rtf: 'rtf',
      csv: 'csv',
      jpg: 'jpg',
      jpeg: 'jpg',
      png: 'png',
      gif: 'gif',
    };

    return typeMap[extension] || '';
  };

  // 组件挂载时获取数据
  onMounted(async () => {
    await getUserInfo();
    if (props.groupId) {
      nextTick(async () => {
        searchParams.value = searchInputRef.value.getSearchParams();
        searchParams.value.keyword = searchParams.value.keyword.trim(); // 去除两端空格
        const currentVersion = ++searchVersion.value;
        await getGroupMembers();
        await fetchChatHistory(1, false, currentVersion);
        await fetchMemberList(); // 获取群成员列表
      });
    }
  });
  onUnmounted(() => {
    stopCurrentVoice();
  });
  //搜索框
  const searchInputRef = ref();
  const searchParams = ref({
    keyword: '',
    startTime: '',
    endTime: '',
  });
  
  // 群成员筛选
  const selectedMemberId = ref('');
  const memberList = ref([]);
  const memberLoading = ref(false);
  const memberPage = ref(1);
  const memberPageSize = ref(20);
  const memberHasMore = ref(true);
  
  // 获取群成员列表（分页）
  const fetchMemberList = async (isLoadMore = false) => {
    if (!props.groupId || memberLoading.value) return;
    if (isLoadMore && !memberHasMore.value) return;
    
    memberLoading.value = true;
    try {
      const res = await groupApi.getChatHistoryCount({
        groupId: props.groupId,
        pageSize: memberPageSize.value,
        pageNum: isLoadMore ? memberPage.value + 1 : 1,
      });
      
      if (res && res.records) {
        if (isLoadMore) {
          memberList.value = [...memberList.value, ...res.records];
        } else {
          memberList.value = res.records;
        }
        memberHasMore.value = res.records.length === memberPageSize.value;
        if (isLoadMore) {
          memberPage.value++;
        } else {
          memberPage.value = 1;
        }
      }
    } catch (error) {
      console.error('获取群成员列表失败:', error);
    } finally {
      memberLoading.value = false;
    }
  };
  
  // 群成员选择变化
  const handleMemberChange = async (memberId) => {
    selectedMemberId.value = memberId;
    currentPage.value = 1;
    hasMore.value = true;
    chatHistory.value = [];
    processedMessages.value = []; // 同时清空处理后的消息，避免显示旧数据
    const currentVersion = ++searchVersion.value;
    // 只需要调用 fetchChatHistory，watch 会自动触发 processMessages
    await fetchChatHistory(1, false, currentVersion);
    // processMessages(); //获取后处理消息
  };
  
  const handleSearch = async (val) => {
    searchParams.value = val;
    searchParams.value.keyword = searchParams.value.keyword.trim(); // 去除两端空格
    currentPage.value = 1;
    hasMore.value = true;
    chatHistory.value = []; //清空消息
    processedMessages.value = []; // 同时清空处理后的消息，避免显示旧数据
    const currentVersion = ++searchVersion.value;
    // 只需要调用 fetchChatHistory，watch 会自动触发 processMessages
    await fetchChatHistory(1, false, currentVersion);
    // processMessages(); //获取后处理消息
  };
  //搜索框end
</script>

<style lang="scss" scoped>
  .chat-history-container {
    display: flex;
    flex-direction: column;
    height: 70vh;
    // padding-bottom: 250px; /* 保持原样，根据实际布局调整 */
    background: #f5f5f5;
  }

  .chat-messages {
    flex: 1;
    padding: 10px;
    margin-top: 10px;
    display: flex;
    flex-direction: column;
    gap: 10px;
    height: 100%;
    /* 消息超出当前区域时内部滚动 */
    overflow-y: auto;
    box-sizing: border-box;
  }

  .loading-more {
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 10px;
    color: #999;
    font-size: 12px;

    .loading-spinner {
      width: 16px; /* 32rpx = 16px */
      height: 16px; /* 32rpx = 16px */
      border: 2px solid #f3f3f3; /* 4rpx = 2px */
      border-top: 2px solid #007aff; /* 4rpx = 2px */
      border-radius: 50%;
      animation: spin 1s linear infinite;
      margin-right: 8px; /* 16rpx = 8px */
    }
  }

  @keyframes spin {
    0% {
      transform: rotate(0deg);
    }
    100% {
      transform: rotate(360deg);
    }
  }

  .time-divider {
    text-align: center;
    margin: 10px 0; /* 20rpx = 10px */
    font-size: 12px; /* 24rpx = 12px */
    color: #999;
  }

  .system-message-item {
    display: flex;
    justify-content: center;
    margin-bottom: 10px; /* 20rpx = 10px */

    .system-content {
      background: rgba(255, 255, 255, 0.1);
      color: #999;
      padding: 4px 8px; /* 8rpx=4px, 16rpx=8px */
      border-radius: 8px; /* 16rpx = 8px */
      font-size: 12px; /* 24rpx = 12px */
      max-width: 80%;
      text-align: center;
    }
  }

  .message-item {
    display: flex;
    margin-bottom: 10px; /* 20rpx = 10px */

    &.own-message {
      justify-content: flex-end;
    }
  }

  .message-left,
  .message-right {
    display: flex;
    align-items: flex-start;
    width: 100%;
  }

  .message-right {
    flex-direction: row-reverse;
  }

  .avatar {
    width: 30px; /* 60rpx = 30px */
    height: 30px; /* 60rpx = 30px */
    border-radius: 15px; /* 50% of 30px */
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #ffffff;
    margin: 0 10px; /* 20rpx = 10px */
    overflow: hidden;
    padding: 2.5px; /* 5rpx ≈ 2.5px */

    .default-avatar {
      color: white;
      font-size: 14px; /* 28rpx = 14px */
    }

    .user-avatar {
      width: 100%;
      height: 100%;
      border-radius: 50%;
      object-fit: cover; /* 对应 UniApp 的 mode="aspectFill" */
    }
  }

  .message-content {
    flex: 1;
    width: calc(100% - 60px); /* 60rpx = 30px × 2 = 60px */
  }

  .message-header {
    width: 100%;
    display: flex;
    align-items: center;
    margin-bottom: 4px; /* 8rpx = 4px */

    &.own-header {
      justify-content: flex-end;
    }

    .sender-name {
      font-size: 10px; /* 20rpx = 10px */
      color: #666;
      margin: 0 4px; /* 8rpx = 4px */
    }

    .message-time {
      font-size: 10px; /* 20rpx = 10px */
      color: #999;
    }
  }

  .message-bubble {
    width: fit-content;
    background: white;
    border-radius: 8px; /* 16rpx = 8px */
    padding: 5px 10px; /* 10rpx=5px, 20rpx=10px */
    position: relative;
    word-wrap: break-word;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1); /* 2rpx=1px, 8rpx=4px */
    max-width: 250px; /* 500rpx = 250px */

    &.own-bubble {
      background: #95ec69; /* 保持微信绿色 */
    }

    .message-text {
      font-size: 10px; /* 20rpx = 10px */
      line-height: 1.4; /* 修正为合理的行高 */
      word-break: break-word;
    }

    .text-message-left .message-text {
      color: #333;
    }

    .quote-message {
      .quote-text {
        margin-bottom: 8px; /* 16rpx = 8px */
        padding-bottom: 4px; /* 8rpx = 4px */
        border-bottom: 1px solid rgba(255, 255, 255, 0.2);
      }
    }

    .message-image {
      width: 150px; /* 300rpx = 150px */
      border-radius: 4px; /* 8rpx = 4px */
      cursor: pointer;
      transition: transform 0.2s ease;

      &:hover {
        transform: scale(1.02);
      }
    }

    .video-container {
      width: 150px; /* 300rpx = 150px */
      position: relative;
      max-width: 200px; /* 400rpx = 200px */

      .message-video {
        width: 100%;
        border-radius: 4px; /* 8rpx = 4px */
      }

      .play-button {
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        background: rgba(0, 0, 0, 0.5);
        border-radius: 50%;
        width: 16px; /* 32rpx = 16px */
        height: 16px; /* 32rpx = 16px */
        display: flex;
        align-items: center;
        justify-content: center;

        .play-icon {
          color: white;
          font-size: 16px; /* 32rpx = 16px */
          margin-left: 2px; /* 4rpx = 2px */
        }
      }
    }

    .file-item {
      display: flex;
      align-items: center;
      background: #ffffff;
      padding: 5px; /* 10rpx = 5px */
      border-radius: 5px; /* 10rpx = 5px */

      .file-icon {
        font-size: 10px; /* 20rpx = 10px */
        margin-right: 10px; /* 20rpx = 10px */
      }

      .file-info {
        width: 100%;
        flex: 1;
        display: flex;
        flex-direction: column;

        .file-name {
          font-size: 10px; /* 20rpx = 10px */
          color: #333;
          margin-bottom: 4px; /* 8rpx = 4px */
          white-space: normal;
          overflow: hidden;
          text-overflow: ellipsis;
          line-height: 1.2; /* 修正行高 */
          word-break: break-all;
        }

        .file-size {
          font-size: 9px; /* 18rpx = 9px */
          color: #999;
        }
      }
    }

    .voice-message {
      max-width: 100px; /* 200rpx = 100px */

      .voice-item {
        border-radius: 4px; /* 8rpx = 4px */
        cursor: pointer;
        transition: all 0.2s;

        &:active {
          opacity: 0.7;
        }

        &.other-voice {
          background: #ffffff;
        }

        &.own-voice {
          background: #95ec69;
        }
      }

      .voice-content {
        display: flex;
        align-items: center;
        justify-content: space-between;

        &.other-voice-content {
          flex-direction: row;
        }

        &.own-voice-content {
          flex-direction: row-reverse;
        }
      }

      .voice-animation {
        display: flex;
        align-items: center;
        gap: 2px; /* 4rpx = 2px */

        .wave-bar {
          width: 2px; /* 4rpx = 2px */
          height: 5px; /* 10rpx = 5px */
          background: #999;
          border-radius: 1px; /* 2rpx = 1px */
          transition: all 0.3s ease;

          &:nth-child(1) {
            height: 6px; /* 12rpx = 6px */
          }
          &:nth-child(2) {
            height: 8px; /* 16rpx = 8px */
          }
          &:nth-child(3) {
            height: 10px; /* 20rpx = 10px */
          }

          &.active {
            background: #07c160;

            &:nth-child(1) {
              animation: wave 1s infinite 0.1s;
            }
            &:nth-child(2) {
              animation: wave 1s infinite 0.2s;
            }
            &:nth-child(3) {
              animation: wave 1s infinite 0.3s;
            }
          }
        }
      }

      .voice-duration {
        font-size: 12px; /* 24rpx = 12px */
        color: #666;
        min-width: 20px; /* 40rpx = 20px */
        text-align: center;
      }
    }

    @keyframes wave {
      0%,
      100% {
        transform: scaleY(1);
      }
      50% {
        transform: scaleY(0.3);
      }
    }

    .forward-message {
      max-width: 230px; /* 460rpx = 230px */

      .forward-content {
        display: flex;
        flex-direction: column;

        .forward-title {
          font-size: 14px; /* 28rpx = 14px */
          color: #333;
          margin-bottom: 4px; /* 8rpx = 4px */
          font-weight: 500;
        }

        .forward-count {
          font-size: 12px; /* 24rpx = 12px */
          color: #999;
        }
      }
    }

    .userCard-message {
      width: 175px; /* 350rpx = 175px */
      max-width: 175px; /* 350rpx = 175px */

      .card-content {
        background: #ffffff;
        padding: 2.5px 5px; /* 5rpx=2.5px, 10rpx=5px */

        .card-top {
          display: flex;
          justify-content: space-between;
          padding-bottom: 5px; /* 10rpx = 5px */
          border-bottom: 1px solid #eee;
        }

        .card-icon {
          width: 25px; /* 50rpx = 25px */
          height: 25px; /* 50rpx = 25px */
          border-radius: 25px; /* 50rpx = 25px */
          font-size: 14px; /* 28rpx = 14px */
          margin-right: 10px; /* 20rpx = 10px */
        }

        .card-info {
          flex: 1;
          display: flex;
          flex-direction: column;

          .card-name {
            font-size: 10px; /* 20rpx = 10px */
            color: #333;
            margin-bottom: 2px; /* 4rpx = 2px */
            font-weight: 500;
          }
        }

        .card-desc {
          font-size: 9px; /* 18rpx = 9px */
          color: #999;
        }
      }
    }

    .group-joining-message {
      max-width: 200px; /* 400rpx = 200px */

      .group-joining-content {
        background: #f8f9fa;
        border-radius: 4px; /* 8rpx = 4px */
        padding: 10px; /* 20rpx = 10px */

        .joining-header {
          font-size: 10px; /* 20rpx = 10px */
          font-weight: 600;
          color: #1b61f0;
          margin-bottom: 4px; /* 8rpx = 4px */
        }

        .joining-text {
          font-size: 10px; /* 20rpx = 10px */
          font-weight: 500;
          color: #333;
          margin-bottom: 4px; /* 8rpx = 4px */
          word-wrap: break-word;
        }

        .joining-info {
          font-size: 10px; /* 20rpx = 10px */
          color: #666;
          margin-bottom: 10px; /* 20rpx = 10px */
        }

        .joining-participants {
          border-top: 1px solid #eee; /* 1rpx ≈ 1px */
          padding-top: 8px; /* 16rpx = 8px */

          .participant-item {
            display: flex;
            align-items: center;
            margin-bottom: 6px; /* 12rpx = 6px */

            &:last-child {
              margin-bottom: 0;
            }

            .participant-index {
              font-size: 10px; /* 20rpx = 10px */
              color: #666;
              margin-right: 3px; /* 16rpx = 8px */
              min-width: 10px; /* 40rpx = 20px */
            }
            .preview-image {
              width: 15px;
              height: 15px;
              border-radius: 50%;
              margin-right: 3px;
            }

            .participant-name {
              font-size: 10px; /* 20rpx = 10px */
              color: #333;
            }
          }
        }
      }
    }

    .system-message {
      color: #999;
      font-style: italic;
      font-size: 10px; /* 20rpx = 10px */
      text-align: center;
    }
  }

  /* 图片预览样式 - 使用 Vant ImagePreview */
  .image-preview,
  .video-preview {
    /* 这些样式将由 Vant ImagePreview 组件处理 */
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background: rgba(0, 0, 0, 0.8);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 9999;
    overflow: auto; /* 允许滚动 */
    .preview-wrapper {
      display: flex;
      align-items: center;
      justify-content: center;
      /* 不设置宽高限制，让图片自然显示 */
    }

    .preview-image {
      /* 不设置宽高，保持原始尺寸 */
      object-fit: none; /* 不缩放图片 */
      /* 可选：添加一些边距避免贴边 */
      margin: 20px;
    }
    .preview-video{
      width: 100%;
      height: 80%;

      // 隐藏视频原生控件的溢出菜单按钮（三个点）
      &::-webkit-media-controls-overflow-button {
        display: none !important;
      }

      &::-internal-media-controls-overflow-button {
        display: none !important;
      }
    }
  }

  /* 自定义图片预览覆盖层 */
  .custom-image-preview {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.9);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 9999;

    .preview-image {
      width: 100%;
      height: 80%;
      object-fit: contain;
    }

    .preview-video {
      width: 100%;
      height: 80%;

      // 隐藏视频原生控件的溢出菜单按钮（三个点）
      &::-webkit-media-controls-overflow-button {
        display:none !important;
      }

      &::-internal-media-controls-overflow-button {
        display: none !important;
      }
    }
  }

  .video-preview {
    .video-close {
      position: absolute;
      top: 20px; /* 40rpx = 20px */
      right: 20px; /* 40rpx = 20px */
      color: white;
      font-size: 30px; /* 60rpx = 30px */
      width: 40px; /* 80rpx = 40px */
      height: 40px; /* 80rpx = 40px */
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(0, 0, 0, 0.5);
      border-radius: 50%;
      cursor: pointer;
    }
  }

  /* 响应式设计 - 转换为 px 断点 */
  @media (max-width: 768px) {
    .chat-messages {
      padding: 6px; /* 12rpx ≈ 6px */

      .message-left,
      .message-right {
        max-width: 85%;
      }

      .avatar {
        width: 32px; /* 64rpx ≈ 32px */
        height: 32px; /* 64rpx ≈ 32px */
        margin: 0 8px; /* 16rpx = 8px */
      }

      .message-content {
        max-width: calc(100% - 48px); /* 96rpx ≈ 48px */
      }

      .message-bubble {
        max-width: 200px; /* 400rpx = 200px */
        padding: 8px; /* 16rpx = 8px */

        .message-image,
        .video-container {
          max-width: 150px; /* 300rpx = 150px */
        }
      }
    }
  }

  /* 模态框样式 - 使用 Vant 组件 */
  .modal-overlay {
    /* 建议使用 Vant Popup 组件替代 */
  }

  .modal-content {
    /* 建议使用 Vant Popup 组件替代 */
    width: 100%;
    background-color: #fff;
    border-radius: 16px 16px 0 0;
    height: 80vh;
    display: flex;
    flex-direction: column;
  }

  .modal-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 20px 16px 16px;
    border-bottom: 1px solid #f0f0f0;

    .modal-left {
      color: rgba(38, 78, 209, 1);
    }

    .modal-title {
      width: 80%;
      font-size: 18px;
      font-weight: 600;
      color: #333;
      text-align: center;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      word-break: break-all;
    }

    .close-btn {
      width: 24px;
      height: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
      color: #999;
      cursor: pointer;
    }
  }

  .modal-body {
    height: 57vh;
    padding: 16px;
    overflow-y: auto;
  }

  .custom-loading {
    display: flex;
    align-items: center;
    gap: 4px; /* 8px → 4px */
    padding: 6px 10px; /* 12px=6px, 20px=10px */
    background: rgba(255, 255, 255, 0.9);
    border-radius: 3px; /* 6px → 3px */
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1); /* 2px=1px, 8px=4px */
  }

  /* Vant 组件样式覆盖 */
  :deep(.van-loading) {
    .van-loading__spinner {
      width: 16px;
      height: 16px;
    }
  }

  :deep(.van-image-preview) {
    .van-image-preview__image {
      max-width: 100%;
      max-height: 100%;
    }
  }

  :deep(.van-popup) {
    border-radius: 16px 16px 0 0;
  }

  :deep(.van-nav-bar) {
    background: #fff;

    .van-nav-bar__title {
      color: #333;
      font-weight: 600;
    }

    .van-nav-bar__left,
    .van-nav-bar__right {
      color: #2762fd;
    }
  }
</style>
