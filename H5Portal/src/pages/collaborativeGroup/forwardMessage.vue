<template>
  <view class="chat-history-container">
    <!-- <scroll-view class="forward-messages" scroll-y> -->
    <div class="forward-messages">
      <view v-for="(message, index) in messages" :key="index" class="message-item">
        <view class="message-left">
          <view class="avatar">
            <van-image
              v-if="getUserAvatar(message.from)"
              :src="getUserAvatar(message.from)"
              class="user-avatar"
              fit="cover"
            />
            <img v-else src="@/static/5110/groups.png" class="user-avatar" />
          </view>
          <view class="message-content">
            <view class="message-header">
              <text class="sender-name">{{ message.rebuildName }}</text>
              <text class="message-time">{{ formatTime(parseInt(message.time)) }}</text>
            </view>
            <view style="display: flex; justify-content: flex-start">
              <view class="message-bubble">
                <!-- 文本消息 -->
                <view v-if="message.msgType === 1" class="text-message text-message-left">
                  <EmojiDisplay :text="getTextContent(message)" />
                </view>

                <!-- 文件消息 -->
                <view v-else-if="message.msgType === 2" class="file-message">
                  <!-- 图片 -->
                  <img
                    v-if="message.fileType == 1"
                    :src="transformImageUrl(`/admin-api${message.filePath}`)"
                    class="message-image clickable"
                    @click="lookImage(message.filePath)"
                  />
                  <!-- 视频 -->
                  <view
                    v-else-if="message.fileType == 3"
                    class="video-container"
                    @click="lookVideo(message.filePath)"
                  >
                    <video
                      :src="transformImageUrl(`/admin-api${message.filePath}`)"
                      class="message-video"
                      :autoplay="false"
                      :initial-time="message.duration"
                    ></video>
                    <div class="play-button">
                      <van-icon name="play-circle-o" class="play-icon" />
                    </div>
                  </view>

                  <!-- 文件 -->
                  <view
                    v-else-if="[4, 5, 6, 7, 8, 9, 10, 11].includes(message.fileType)"
                    class="file-item"
                  >
                    <view class="file-icon" @click="lookFile(message.content)">📎</view>
                    <view class="file-info">
                      <text class="file-name">{{ message.fileName || '未知文件' }}</text>
                      <text class="file-size">{{ formatFileSize(message.fileSize) }}</text>
                    </view>
                  </view>

                  <!-- 语音 -->
                  <view v-else-if="message.fileType == 2" class="voice-message">
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
                        <text class="voice-duration">{{ message.duration || 0 }}″</text>
                      </view>
                    </view>
                  </view>
                </view>

                <!-- 转发消息 -->
                <view v-else-if="message.msgType === 8" class="forward-message">
                  <!-- 使用 MsgForwardCard 组件显示转发消息 -->
                  <MsgForwardCard
                    :card-data="getForwardCardData(message)"
                    :member-list="memberList"
                    @showDetail="handleShowForwardDetail"
                  />
                </view>

                <!-- 各种卡片消息 - 统一使用 MsgCard 组件 -->
                <view v-else-if="isCardMessage(message)" class="card-message">
                  <MsgCard :cardData="getCardData(message)" />
                </view>

                <!-- 群接龙 -->
                <view v-else-if="message.msgType === 6" class="group-joining-message">
                  <view class="group-joining-content">
                    <view class="joining-header">#接龙</view>
                    <view class="joining-text">{{ getGroupJoiningContent(message).text }}</view>
                    <view class="joining-info">
                      {{ getGroupJoiningContent(message).creatorName }}发起，参与共{{
                        getGroupJoiningContent(message).participantCount
                      }}人
                    </view>
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
                  <text>{{ getMessageContent(message) }}</text>
                </view>
              </view>
            </view>
          </view>
        </view>
      </view>
      <!-- </scroll-view> -->
    </div>

    <!-- 嵌套转发消息的弹框 -->
    <view class="modal-overlay" v-if="showNestedForwardModal" @click="closeNestedModal">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <div class="modal-left"></div>
          <text class="modal-title">[合并转发]群聊的聊天记录</text>
          <view class="close-btn" @click="closeNestedModal">×</view>
        </view>
        <view class="modal-body">
          <ForwardMessage1
            :messages="nestedForwardContent"
            :member-list="memberList"
            @lookImage="lookImage"
            @lookVideo="lookVideo"
          />
        </view>
      </view>
    </view>
    <van-action-sheet
      v-model:show="showActionSheetVisible"
      :actions="actionSheetActions"
      cancel-text="取消"
      @select="onActionSelect"
      @cancel="onActionCancel"
      title="选择操作"
    />
    <van-loading v-if="showLoading" size="24px" color="#1989fa" class="custom-loading">
      下载文件中...
    </van-loading>
  </view>
</template>

<script setup>
  import { showDialog, showSuccessToast, showToast } from 'vant';
  import { ref, watch, computed, onMounted } from 'vue';

  import { groupApi } from '@/common/api/index.js';
  import EmojiDisplay from '@/pages/collaborativeGroup/emojiDisplay.vue';
  import ForwardMessage1 from '@/pages/collaborativeGroup/forwardMessage1.vue';
  import MsgCard from '@/pages/collaborativeGroup/msgCard.vue';
  import MsgForwardCard from '@/pages/collaborativeGroup/msgForwardCard.vue';
  import defaultImg from '@/static/5110/im_person.svg';
  import { transformImageUrl } from '@/utils/imgUrlParse.js';
  import { createInnerAudioContext } from '@/utils/innerAudioContext.js';
  import { openDocument } from '@/utils/openDocument.js';
  import { setClipboardData } from '@/utils/setClipboardData.js';
  import { useCommunicationStore } from '@/stores/communication.js';

  const communicationStore = useCommunicationStore();

  const realUserInfos = new Map();
  // 存储远程获取的用户信息
  const remoteUserInfo = ref({});
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
  const props = defineProps({
    messages: {
      type: Array,
      default: () => [],
    },
    memberList: {
      type: Array,
      default: () => [],
    },
  });
  const emit = defineEmits(['lookImage', 'lookVideo']);

  // 获取需要查询的用户ID列表
  const getUserIdsToFetch = computed(() => {
    const userIds = new Set();
    props.messages.forEach((message) => {
      const userId = message.from || message.fromRealUserId;
      if (userId) {
        // 只有当用户不在本地成员列表中且不在远程缓存中时才需要获取
        const isLocalMember = props.memberList.find((m) => m.id == userId);
        const isRemoteCached = remoteUserInfo.value[userId];
        if (!isLocalMember && !isRemoteCached) {
          userIds.add(userId);
        }
      }
    });
    return Array.from(userIds);
  });

  // 获取用户信息（并发执行两个接口）
  const fetchUserInfo = async (userIds) => {
    if (!userIds || userIds.length === 0) return;

    try {
      // 并发调用获取用户信息和协同岗信息的接口
      const [userInfos, userInfos2] = await Promise.all([
        groupApi.getUserInfo({ userIds }),
        groupApi.getColloration({ userIds }),
      ]);

      // 处理普通用户信息
      if (userInfos?.results && Array.isArray(userInfos.results)) {
        userInfos.results.forEach((user) => {
          if (user && user.id) {
            remoteUserInfo.value[user.id] = user;
          }
        });
      }

      // 处理协同岗信息
      if (Array.isArray(userInfos2)) {
        userInfos2.forEach((item) => {
          if (item && item.id) {
            remoteUserInfo.value[item.id] = {
              id: item.id,
              name: item.postName,
              avatar: item.iconUrl,
            };
          }
        });
      }
    } catch (error) {
      console.error('获取用户信息失败:', error);
    }
  };

  watch(
    () => props.messages,
    async () => {
      // 先获取缺失的用户信息
      const userIdsToFetch = getUserIdsToFetch.value;
      if (userIdsToFetch.length > 0) {
        await fetchUserInfo(userIdsToFetch);
      }
      
      props.messages.forEach(async (item) => {
        item.rebuildName = await getUserNameTwo(item);
      });
    },
    { deep: true, immediate: true },
  );
  // 嵌套转发相关
  const showNestedForwardModal = ref(false);
  const nestedForwardContent = ref([]);

  // 判断是否为卡片消息
  const isCardMessage = (message) => {
    return (
      message.msgType === 5 ||
      message.userCardData ||
      message.officialAccountsData ||
      message.customCardData ||
      message.officialAccountsCodeData ||
      message.sharedMsgData
    );
  };

  // 语音播放相关
  const currentPlayingAudio = ref(null);
  const currentPlayingMsgId = ref('');

  // 播放语音消息
  const playVoiceMessage = async (message) => {
    console.log('播放语音消息:', message);

    // 使用更唯一的标识，结合 fileName 和 time
    const voiceId = `${message.fileName}_${message.time}`;
    const voiceUrl = transformImageUrl(`/admin-api${message.filePath}`);

    console.log('语音信息:', voiceUrl, voiceId);

    // 如果正在播放的是当前消息，则停止播放
    if (currentPlayingMsgId.value === voiceId && currentPlayingAudio.value) {
      console.log('停止当前播放的语音');
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
      currentPlayingMsgId.value = voiceId;

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

      // 监听等待事件（缓冲）
      innerAudioContext.onWaiting(() => {
        console.log('语音加载中...');
      });

      // 监听可以播放事件
      innerAudioContext.onCanplay(() => {
        console.log('语音可以播放了');
      });

      // 开始播放
      innerAudioContext.play();
    } catch (error) {
      console.error('播放语音失败:', error);
      showToast('播放失败');
    }
  };

  // 停止当前播放的语音
  const stopCurrentVoice = () => {
    if (currentPlayingAudio.value) {
      try {
        currentPlayingAudio.value.stop();
        currentPlayingAudio.value.destroy();
      } catch (e) {
        console.log('停止语音时发生错误:', e);
      }
      currentPlayingAudio.value = null;
    }
    currentPlayingMsgId.value = '';
  };

  // 判断是否正在播放
  const isVoicePlaying = (message) => {
    const voiceId = `${message.fileName}_${message.time}`;
    return currentPlayingMsgId.value === voiceId;
  };

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

  // 获取卡片数据
  const getCardData = (message) => {
    // 个人名片
    if (message.userCardData) {
      return {
        cardType: 'userCard',
        userCardData: message.userCardData,
      };
    }
    // 公众号名片
    else if (message.officialAccountsData) {
      return {
        cardType: 'officialAccounts',
        officialAccountsData: message.officialAccountsData,
      };
    }
    // 任务卡片
    else if (message.customCardData) {
      return {
        cardType: 'customCard',
        customCardData: message.customCardData,
      };
    }
    // 公众号分享
    else if (message.officialAccountsCodeData) {
      return {
        cardType: 'officialAccountCode',
        userCardData: message.officialAccountsCodeData,
      };
    }
    // 协同分享
    else if (message.sharedMsgData) {
      return {
        cardType: 'sharedMsg',
        sharedMsgData: message.sharedMsgData,
        sharedAppName: message.sharedAppName,
      };
    }
    // 设备
    else if (message.cardType === 'selfDefineCard') {
      const data = message?.data?.properties;
      const selfDefineCard = {};
      data.forEach((item) => {
        selfDefineCard[item.propertyName] = item.propertyValue;
      });
      return {
        cardType: 'selfDefineCard',
        selfDefineCard,
      };
    }
    // 默认返回空数据
    return {
      cardType: 'unknown',
    };
  };

  // 获取转发消息的卡片数据
  const getForwardCardData = (message) => {
    try {
      if (message.userTxt) {
        const forwardData = JSON.parse(message.userTxt);
        return {
          title: forwardData.title || '聊天记录',
          forwardMsgs: forwardData.forwardMsgs || [],
        };
      }
    } catch {
      return {
        title: '聊天记录',
        forwardMsgs: [],
      };
    }
    return {
      title: '聊天记录',
      forwardMsgs: [],
    };
  };

  // 处理显示转发详情
  const handleShowForwardDetail = (forwardData) => {
    nestedForwardContent.value = forwardData.forwardMsgs || [];
    console.log('99884', nestedForwardContent.value);
    showNestedForwardModal.value = true;
  };

  // 关闭嵌套转发弹框
  const closeNestedModal = () => {
    showNestedForwardModal.value = false;
  };

  // 获取用户头像
  const getUserAvatar = (userId) => {
    if (!userId) return '';
    const member = props.memberList.find((m) => m.id == userId);
    console.info('[群接龙调试] getUserAvatar - userId:', userId, '找到member:', member ? JSON.parse(JSON.stringify(member)) : null);
    if (!member) return '';
    // 优先使用协同岗头像，其次使用普通头像
    const avatar = member.collaborationAvatar || member.tumbAvatar || member.avatar || '';
    console.info('[群接龙调试] getUserAvatar - 最终avatar路径:', avatar);
    return avatar ? transformImageUrl(`/admin-api${avatar}`) : '';
  };

  async function getUserNameTwo(message) {
    if (!message || !message.from) return '未知用户';
    
    const userId = message.from;
    
    // 1. 先从本地群成员中查找
    const member = props.memberList.find((m) => m.id == userId);
    
    if (member) {
      // 协同岗消息：优先使用已拼接好的realName，否则查真实用户名拼接
      if (message.fromRealUserId) {
        if (member.realName && member.realName !== member.name) {
          return member.realName;
        }
        let name = member?.name || `用户${userId}`;
        if (realUserInfos.has(message.fromRealUserId)) {
          const userInfo = realUserInfos.get(message.fromRealUserId);
          if (userInfo && userInfo.name) {
            return `${name}(${userInfo.name})`;
          } else {
            return name;
          }
        } else {
          try {
            const userInfos = await groupApi.getUserInfo({
              userIds: [message.fromRealUserId],
            });
            if (userInfos.results && Array.isArray(userInfos.results)) {
              const userInfo = userInfos.results.find((m) => m.id == message.fromRealUserId);
              if (userInfo) {
                realUserInfos.set(message.fromRealUserId, userInfo);
                if (userInfo.name) {
                  return `${name}(${userInfo.name})`;
                }
              } else {
                return name;
              }
            }
          } catch (e) {
            // 获取真实用户信息失败
          }
        }
        return name;
      }
      return member?.realName || member?.name || `用户${userId}`;
    }
    
    // 2. 从远程获取的用户信息中查找
    const remoteUser = remoteUserInfo.value[userId];
    if (remoteUser) {
      let name = remoteUser.name || remoteUser.userName || `用户${userId}`;
      // 协同岗消息：还需要查真实用户名拼接
      if (message.fromRealUserId) {
        const remoteRealUser = remoteUserInfo.value[message.fromRealUserId];
        if (remoteRealUser && remoteRealUser.name) {
          return `${name}(${remoteRealUser.name})`;
        }
        // 远程缓存中没有真实用户信息，尝试查询
        try {
          const userInfos = await groupApi.getUserInfo({
            userIds: [message.fromRealUserId],
          });
          if (userInfos.results && Array.isArray(userInfos.results)) {
            const userInfo = userInfos.results.find((m) => m.id == message.fromRealUserId);
            if (userInfo && userInfo.name) {
              remoteUserInfo.value[message.fromRealUserId] = userInfo;
              return `${name}(${userInfo.name})`;
            }
          }
        } catch (e) {
          // 获取真实用户信息失败
        }
      }
      return name;
    }
    
    // 3. 尝试实时获取用户信息
    try {
      const userInfos = await groupApi.getUserInfo({ userIds: [userId] });
      if (userInfos?.results && Array.isArray(userInfos.results)) {
        const userInfo = userInfos.results.find((m) => m.id == userId);
        if (userInfo && userInfo.name) {
          remoteUserInfo.value[userId] = userInfo;
          let name = userInfo.name;
          // 协同岗消息：还需要查真实用户名拼接
          if (message.fromRealUserId) {
            try {
              const realUserInfos2 = await groupApi.getUserInfo({
                userIds: [message.fromRealUserId],
              });
              if (realUserInfos2?.results && Array.isArray(realUserInfos2.results)) {
                const realUserInfo = realUserInfos2.results.find((m) => m.id == message.fromRealUserId);
                if (realUserInfo && realUserInfo.name) {
                  remoteUserInfo.value[message.fromRealUserId] = realUserInfo;
                  name = `${name}(${realUserInfo.name})`;
                }
              }
            } catch (e) {
              // 获取真实用户信息失败
            }
          }
          return name;
        }
      }
      
      // 尝试获取协同岗信息
      const collorationRes = await groupApi.getColloration({ userIds: [userId] });
      if (Array.isArray(collorationRes) && collorationRes.length > 0) {
        const collabInfo = collorationRes[0];
        if (collabInfo && collabInfo.postName) {
          remoteUserInfo.value[userId] = {
            id: collabInfo.id,
            name: collabInfo.postName,
            avatar: collabInfo.iconUrl,
          };
          let name = collabInfo.postName;
          // 协同岗消息：还需要查真实用户名拼接
          if (message.fromRealUserId) {
            try {
              const realUserInfos3 = await groupApi.getUserInfo({
                userIds: [message.fromRealUserId],
              });
              if (realUserInfos3?.results && Array.isArray(realUserInfos3.results)) {
                const realUserInfo = realUserInfos3.results.find((m) => m.id == message.fromRealUserId);
                if (realUserInfo && realUserInfo.name) {
                  remoteUserInfo.value[message.fromRealUserId] = realUserInfo;
                  name = `${name}(${realUserInfo.name})`;
                }
              }
            } catch (e) {
              // 获取真实用户信息失败
            }
          }
          return name;
        }
      }
    } catch (e) {
      // 获取用户信息失败
    }
    
    return `用户${userId}`;
  }
  // 获取用户名称
  // const getUserName = (message) => {
  //   if (!message || !message.from) return "未知用户";
  //   const member = props.memberList.find((m) => m.id == message.from);
  //   let name = member?.realName || member?.name || `用户${message.from}`;
  //   return name
  // };

  // 格式化时间
  const formatTime = (timestamp) => {
    if (!timestamp) return '';
    const date = new Date(parseInt(timestamp));
    const year = date.getFullYear();
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const day = date.getDate().toString().padStart(2, '0');
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    const seconds = date.getSeconds().toString().padStart(2, '0');

    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
  };

  // 获取文本内容
  const getTextContent = (message) => {
    return message.text || '';
  };

  // 格式化文件大小
  const formatFileSize = (bytes) => {
    if (!bytes) return '';
    if (bytes < 1024) return bytes + 'B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB';
    return (bytes / (1024 * 1024)).toFixed(1) + 'MB';
  };

  // 获取转发消息标题
  const getForwardTitle = (message) => {
    try {
      if (message.userTxt) {
        const forwardData = JSON.parse(message.userTxt);
        return forwardData.title || '聊天记录';
      }
    } catch {
      return '聊天记录';
    }
    return '聊天记录';
  };

  // 获取转发消息数量
  const getForwardCount = (message) => {
    try {
      if (message.userTxt) {
        const forwardData = JSON.parse(message.userTxt);
        return forwardData.forwardMsgs?.length || 0;
      }
    } catch {
      return 0;
    }
    return 0;
  };

  // 获取群接龙内容
  const getGroupJoiningContent = (message) => {
    console.info('[群接龙调试] getGroupJoiningContent - message:', JSON.parse(JSON.stringify(message)));
    console.info('[群接龙调试] getGroupJoiningContent - props.memberList:', JSON.parse(JSON.stringify(props.memberList)));
    const content = parseGroupJoiningMessage(message);
    console.info('[群接龙调试] getGroupJoiningContent - 解析后content:', content);

    // 处理参与者数据
    let participants = [];
    if (content.userTxts && Array.isArray(content.userTxts)) {
      const sortedUserTxts = [...content.userTxts].sort((a, b) => {
        const timeA = a.timeStamp || a.clientTimeStamp || 0;
        const timeB = b.timeStamp || b.clientTimeStamp || 0;
        return timeA - timeB;
      });

      participants = sortedUserTxts.map((userTxt, index) => {
        const member = props.memberList.find((m) => m.id == userTxt.userId);
        console.info('[群接龙调试] 参与者 - userTxt:', JSON.parse(JSON.stringify(userTxt)), '匹配member:', member ? JSON.parse(JSON.stringify(member)) : null);
        if (member) {
          const postName = member.postName || '';
          const realName = member.realName || '';
          // 提取userTxt.txt中空格前的名称部分，用于判断协同岗身份
          let txt = (userTxt.txt || '').split(' ')[0];
          try {
            txt = txt.split(' ')[0];
          } catch {
            txt = userTxt.txt || '';
          }

          // 判断是否为协同岗身份：根据userTxt.txt内容而非仅凭postName是否存在
          // 1. txt 等于岗位名（以协同岗身份参与）
          // 2. txt 符合 "岗位名(真实姓名)" 格式
          // 3. txt 包含 "(真实姓名)" 但不以真实姓名开头
          const isCollaborationIdentity =
            (postName && txt === postName) ||
            (postName && realName && txt === `${postName}(${realName})`) ||
            (realName && !txt.startsWith(realName) && txt.includes(`(${realName})`));

          // 获取用户头像（支持 avatar 和 tumbAvatar 两种字段名）
          const userAvatar = member.tumbAvatar || member.avatar || '';
          // 如果是协同岗身份，优先使用协同岗头像；否则使用普通用户头像
          const avatar = isCollaborationIdentity
            ? (member.collaborationAvatar || userAvatar)
            : userAvatar;

          // 名称优先使用userTxt.txt（服务端已包含协同岗格式如"sf(30孙飞) asdsad"），与Web端保持一致
          const participantName = userTxt.txt || member.name || `用户${userTxt.userId}`;

          console.info('[群接龙调试] 参与者结果 - name:', participantName, 'avatar:', avatar, 'isCollaborationIdentity:', isCollaborationIdentity, 'postName:', postName, 'realName:', realName, 'txt:', txt);

          return {
            userId: userTxt.userId,
            name: participantName,
            avatar: avatar,
            timeStamp: userTxt.timeStamp || userTxt.clientTimeStamp,
            isCreator: index === 0,
          };
        }

        // 不在成员列表中的参与者
        return {
          userId: userTxt.userId,
          name: userTxt.txt || `用户${userTxt.userId}`,
          avatar: '',
          timeStamp: userTxt.timeStamp || userTxt.clientTimeStamp,
          isCreator: index === 0,
        };
      });
    }

    // 计算去重后的参与人数：协同岗账号和其对应的真实用户算作同一人
    const getRealUserIdForDedup = (userId) => {
      const member = props.memberList.find((m) => m.id == userId);
      if (!member || !member.postName) return userId.toString();
      if (member.name === member.postName) {
        const realUser = props.memberList.find(
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

    // 获取发起人名称 - 优先使用participants[0].name（来自userTxt.txt，含协同岗格式）
    let creatorName = '未知用户';
    if (participants.length > 0) {
      creatorName = participants[0].name;
    } else if (content.creator) {
      const creatorMember = props.memberList.find((m) => m.id == content.creator);
      if (creatorMember) {
        creatorName = creatorMember.realName || creatorMember.name || `用户${content.creator}`;
      } else {
        creatorName = `用户${content.creator}`;
      }
    }

    return {
      text: content.text || '未命名接龙',
      creatorName: creatorName,
      participantCount: participantCount,
      participants: participants,
    };
  };
  const parseGroupJoiningMessage = (message) => {
    console.log('***', message);
    if (message.msgType === 6) {
      try {
        // 直接解析 JSON 字符串，不需要 base64 解码
        const result = JSON.parse(message.userTxt);
        return result;
      } catch (jsonError) {
        console.error('解析接龙消息失败:', jsonError);
        // 如果解析失败，尝试返回默认结构
        return {
          text: '未命名接龙',
          creator: message.from,
          userTxts: [],
        };
      }
    }
    return message.userTxt;
  };
  // 获取消息内容
  const getMessageContent = (message) => {
    return `[消息类型: ${message.msgType}]`;
  };

  // 查看图片
  const lookImage = (url) => {
    emit('lookImage', url);
  };

  // 查看视频
  const lookVideo = (url) => {
    emit('lookVideo', url);
  };
</script>

<style lang="scss" scoped>
  .chat-history-container {
    display: flex;
    flex-direction: column;
    // height: 80vh;
    // border: 1px solid red;
    padding-bottom: 150px;
    background: #f5f5f5;
  }

  .forward-messages {
    height: 100%;
    padding: 10px;
    overflow-y: auto;
  }

  .message-item {
    display: flex;
    margin-bottom: 10px;
  }

  .message-left {
    display: flex;
    align-items: flex-start;
    width: 100%;
  }

  .avatar {
    width: 30px;
    height: 30px;
    border-radius: 50%;
    margin-right: 10px;
    flex-shrink: 0;
    overflow: hidden;
    padding: 3px;
    background: #ffffff;
    .user-avatar {
      width: 100%;
      height: 100%;
      border-radius: 50%;
    }
  }

  .message-content {
    flex: 1;
    // max-width: calc(100% - 50px);
  }

  .message-header {
    width: 100%;
    display: flex;
    align-items: center;
    margin-bottom: 4px;

    .sender-name {
      font-size: 10px;
      color: #666;
      margin-right: 8px;
      font-weight: 500;
    }

    .message-time {
      font-size: 10px;
      color: #999;
    }
  }

  .message-bubble {
    background: white;
    border-radius: 8px;
    padding: 8px 10px;
    position: relative;
    word-wrap: break-word;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
    max-width: 250px;

    .text-message {
      line-height: 1.4;
      white-space: pre-wrap;
      word-break: break-word;
      font-size: 14px;
    }

    .text-message-left {
      color: #333;
    }

    .message-image {
      max-width: 150px;
      border-radius: 4px;
    }

    .video-container {
      max-width: 150px;
      position: relative;

      .message-video {
        width: 150px;
        border-radius: 4px;
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
      background: #f8f9fa;
      padding: 10px;
      border-radius: 5px;

      .file-icon {
        font-size: 16px;
        margin-right: 10px;
      }

      .file-info {
        flex: 1;
        display: flex;
        flex-direction: column;

        .file-name {
          font-size: 13px;
          color: #333;
          margin-bottom: 4px;
          white-space: normal;
          overflow: hidden;
          text-overflow: ellipsis;
          line-height: 1;
          word-break: break-all;
        }

        .file-size {
          font-size: 11px;
          color: #999;
        }
      }
    }

    .voice-message {
      max-width: 100px;

      .voice-item {
        padding: 0 3px;
        border-radius: 4px;
        cursor: pointer;
        transition: all 0.2s;

        &:active {
          opacity: 0.7;
        }

        &.other-voice {
          background: #ffffff;
        }
      }

      .voice-content {
        display: flex;
        align-items: center;
        justify-content: space-between;

        &.other-voice-content {
          flex-direction: row;
        }
      }

      .voice-animation {
        display: flex;
        align-items: center;
        gap: 2px;

        .wave-bar {
          width: 2px;
          height: 5px;
          background: #999;
          border-radius: 1px;
          transition: all 0.3s ease;

          &:nth-child(1) {
            height: 6px;
          }
          &:nth-child(2) {
            height: 8px;
          }
          &:nth-child(3) {
            height: 10px;
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
        font-size: 12px;
        color: #666;
        min-width: 20px;
        text-align: center;
        margin-left: 6px;
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
      // 转发消息样式保持不变，MsgForwardCard 有自己的样式
    }

    .group-joining-message {
      .group-joining-content {
        background: #f8f9fa;
        border-radius: 4px;
        padding: 10px;
        max-width: 200px;

        .joining-header {
          font-size: 13px;
          font-weight: 600;
          color: #1b61f0;
          margin-bottom: 4px;
        }

        .joining-text {
          font-size: 13px;
          font-weight: 500;
          color: #333;
          margin-bottom: 4px;
          word-wrap: break-word;
        }

        .joining-info {
          font-size: 11px;
          color: #666;
          margin-bottom: 8px;
        }

        .joining-participants {
          border-top: 1px solid #eee;
          padding-top: 8px;

          .participant-item {
            display: flex;
            align-items: center;
            margin-bottom: 6px;

            &:last-child {
              margin-bottom: 0;
            }

            .participant-index {
              font-size: 10px;
              color: #666;
              margin-right: 3px;
              min-width: 10px;
            }
            .preview-image {
              width: 15px;
              height: 15px;
              border-radius: 50%;
              margin-right: 3px;
            }

            .participant-name {
              font-size: 11px;
              color: #333;
            }
          }
        }
      }
    }

    .system-message {
      color: #999;
      font-style: italic;
      font-size: 12px;
    }
  }

  /* 嵌套转发弹框样式 */
  .modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: flex-end;
    justify-content: center;
    z-index: 1000;
  }

  .modal-content {
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
      // width: 40px;
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
    height: 65vh;
    padding: 16px;
    overflow-y: auto;
  }
</style>
