<template>
  <view class="ai-assistant-page">
    <view class="left-area">
      <view class="title"> AI助手 </view>
      <view class="agents-entrance" @click="handelPage('agentsPage')">
        <text>全部智能体</text>
        <uv-icon name="arrow-right" size="18" color="#333"></uv-icon>
      </view>
    </view>
    <view class="right-area">
      <!-- 关闭按钮 -->
      <uv-image
        v-if="currentPage !== 'init'"
        width="28px"
        height="28px"
        class="close-btn"
        src="/static/ai/close.png"
        @click="handelPage('init')"
      ></uv-image>

      <!-- 首页 -->
      <view v-if="currentPage === 'init'" class="init-page">
        <view class="chat-tip">
          <view class="ai-desc">
            <text class="ai-desc1">我是AI助手!</text>
            <text class="ai-desc2">我可以帮你搜索、答疑、写作，请把你的任务交给我吧～</text>
          </view>
          <uv-image src="/static/ai/pc_bg.png"></uv-image>
        </view>
        <view class="init-function-btns">
          <view
            class="init-func-btn"
            v-for="func in agents.slice(0, 9)"
            :key="func.index"
            @click="handleUse(func)"
          >
            <!-- <uv-image
              width="48px"
              height="48px"
              class="agent-icon"
              :src="func.picUrl"
            ></uv-image> -->
            <AuthImg :picUrl="func.picUrl" class="agent-icon"></AuthImg>
            <view class="agent-text">
              <text class="agent-name">{{ func.name }}</text>
              <text class="agent-detail">{{ func.desc }}</text>
            </view>
          </view>
        </view>
        <uv-image width="100%" class="bg-line" src="/static/ai/pc_line.png"></uv-image>
      </view>
      <!-- 智能体页 -->
      <view v-else-if="currentPage === 'agentsPage'" class="agents-page">
        <view class="agentsPage-title">全部智能体</view>
        <view class="init-function-btns">
          <view
            class="init-func-btn"
            v-for="func in agents"
            :key="func.index"
            @click="handleUse(func)"
          >
            <AuthImg :picUrl="func.picUrl" class="agent-icon"></AuthImg>
            <view class="agent-text">
              <text class="agent-name">{{ func.name }}</text>
              <text class="agent-detail">{{ func.desc }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 对话页 -->
      <view class="chat-history" v-else-if="currentPage === 'messagesPage'">
        <!-- 对话 -->
        <view
          class="messages-area"
          v-for="(msg, index) in messages"
          :key="index"
          :class="msg.type === 'user' ? 'user-message' : 'ai-message'"
        >
          <uv-image
            width="50px"
            height="50px"
            shape="circle"
            :observeLazyLoad="true"
            :src="
              msg.type === 'user'
                ? userAvatar || '/static/ai/user_avatar.png'
                : '/static/ai/ai_avatar.png'
            "
          ></uv-image>
          <view
            class="message-content"
            @touchstart="(e) => handleTouchStart(e, msg.content)"
            @touchend="handleTouchEnd"
            @touchmove="handleTouchMove"
          >
            <text v-if="msg.type === 'user'">{{ msg.content }}</text>
            <view v-else v-html="msg.content"></view>
          </view>
          <view
            v-show="showMenu"
            class="action-menu"
            :style="{ top: menuTop + 'px', left: menuLeft + 'px', zIndex: 1 }"
            @click.stop="onMenuClick"
          >
            <view @click="handleAction">复制</view>
          </view>
        </view>

        <!-- 加载状态 -->
        <view v-if="isLoading && !isReceiving" class="messages-area ai-message">
          <uv-image
            width="50px"
            height="50px"
            shape="circle"
            src="/static/ai/ai_loading.png"
          ></uv-image>
          <view class="message-content">
            <text>正在查询...</text>
          </view>
        </view>
      </view>

      <!-- 功能按钮区 -->
      <view class="function-btns" v-if="currentPage === 'messagesPage'">
        <uv-image
          width="24px"
          height="24px"
          class="function-icon"
          src="/static/ai/magin_pen.png"
        ></uv-image>
        <view
          class="func-btn"
          :class="{ 'current-agent': currentAgentIndex === func.index }"
          v-for="func in functions"
          :key="func.index"
          @click="handleFunction(func)"
        >
          {{ func.name }}
        </view>
      </view>

      <!-- 输入区域 -->
      <view class="input-area" v-if="currentPage === 'messagesPage'">
        <uv-image
          width="14px"
          height="14px"
          class="input-icon"
          src="/static/ai/pen.png"
          :style="{ display: inputFocused ? 'none' : 'block' }"
        ></uv-image>
        <input
          class="chat-input"
          v-model="inputValue"
          placeholder="说点儿什么"
          @confirm="sendMessage"
        />
        <uv-image
          width="40px"
          height="40px"
          class="send-btn"
          :class="[{ 'send-btn-disabled': isLoading || !inputValue.trim() }]"
          src="/static/ai/send.png"
          @click="sendMessage"
        ></uv-image>
      </view>
    </view>
  </view>
</template>

<script setup>
  import { ref, computed, onMounted, watch, onBeforeUnmount } from 'vue';

  import AuthImg from '@/components/AuthImg/index.vue';
  import { useAiStore } from '@/stores/ai.js';

  const aiStore = useAiStore();

  // 接收父组件传递的props
  const props = defineProps({
    messages: {
      type: Array,
      required: true,
      default: () => [],
    },
    functions: {
      type: Array,
      required: true,
      default: () => [],
    },
    isLoading: {
      type: Boolean,
      required: true,
      default: false,
    },
    isReceiving: {
      type: Boolean,
      required: true,
      default: false,
    },
    currentAgentIndex: {
      type: [Number, null],
      required: true,
      default: null,
    },
    userAvatar: {
      type: [String, null],
      required: true,
      default: null,
    },
  });

  // 发送父组件定义的事件
  const emit = defineEmits(['sendMessage', 'handleFunction', 'copyText']);

  // 所有智能体
  const agents = computed(() => aiStore.agentsList);

  // 聊天相关状态
  const inputValue = ref('');

  const currentPage = ref('init'); // init 、messagesPage agentsPage
  const handelPage = (val) => {
    currentPage.value = val;
  };

  const inputFocused = ref(false);
  watch(
    () => inputValue.value,
    (newValue) => {
      if (newValue) {
        inputFocused.value = true;
      } else {
        inputFocused.value = false;
      }
    },
  );

  // 选择智能体
  function handleUse(currentAgent) {
    aiStore.changeAgentsList(agents.value, currentAgent);

    currentPage.value = 'messagesPage';
    emit('handleFunction', currentAgent);
  }

  // 选择智能体
  function handleFunction(func) {
    emit('handleFunction', func);
  }

  // 发送消息
  function sendMessage() {
    if (inputValue.value) {
      currentPage.value = 'messagesPage';
      emit('sendMessage', inputValue.value);
      inputValue.value = '';
    }
  }

  // 长按复制相关
  onMounted(() => {
    // 点击其他区域关闭菜单
    document.addEventListener('click', hideMenu);
  });
  onBeforeUnmount(() => {
    document.removeEventListener('click', hideMenu);
  });
  const onMenuClick = (e) => {
    e.stopPropagation();
  };
  const pressTimer = ref(null);
  const showMenu = ref(false);
  const menuTop = ref(0);
  const menuLeft = ref(0);
  const startX = ref(0);
  const startY = ref(0);
  const isMoving = ref(false);
  const transMsg = ref(null);

  function handleTouchStart(e, msg) {
    transMsg.value = msg;
    startX.value = e.touches[0].clientX;
    startY.value = e.touches[0].clientY;
    isMoving.value = false;

    // 500ms后如果没有松开或移动，显示菜单
    pressTimer.value = setTimeout(() => {
      if (!isMoving.value) {
        showMenu.value = true;
        // 计算菜单位置
        menuTop.value = e.touches[0].clientY + 10;
        menuLeft.value = e.touches[0].clientX - 30;
      }
    }, 500);
  }

  function handleTouchEnd() {
    clearTimeout(pressTimer.value);
  }

  function handleTouchMove(e) {
    const moveX = Math.abs(e.touches[0].clientX - startX.value);
    const moveY = Math.abs(e.touches[0].clientY - startY.value);

    // 判断是否移动超过阈值
    if (moveX > 5 || moveY > 5) {
      isMoving.value = true;
      showMenu.value = false;
      clearTimeout(pressTimer.value);
    }
  }

  function handleAction() {
    showMenu.value = false;
    emit('copyText', transMsg.value);
  }

  function hideMenu(e) {
    // 检查点击是否发生在消息内容区域
    const contentEl = e.target.closest('.message-content');
    if (!contentEl && showMenu.value) {
      showMenu.value = false;
    }
  }
</script>

<style scoped>
  /* 基础样式 */
  .ai-assistant-page {
    width: 100vw;
    height: 100vh;
    overflow: hidden;
    display: flex;
    justify-content: space-between;
  }

  .left-area {
    width: 18%;
    min-width: 225px;
    height: 100%;
    background: linear-gradient(90deg, rgba(246, 253, 255, 1) 0%, rgba(217, 229, 250, 1) 100%);
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 40px 30px 30px 25px;
    .title {
      width: 100%;
      margin-bottom: 149px;
      font-size: 24px;
      font-weight: 500;
      color: rgba(0, 0, 0, 1);
    }
    .agents-entrance {
      width: 100%;
      display: flex;
      justify-content: space-around;
      align-items: center;
    }
  }
  .right-area {
    flex: 1;
    padding: 40px 10% 60px 10%;
    height: 100%;
    align-self: center;
    display: flex;
    flex-direction: column;
    background: linear-gradient(90deg, rgba(246, 253, 255, 1) 0%, rgba(217, 229, 250, 1) 100%);
    position: relative;

    .close-btn {
      position: absolute;
      top: 45px;
      right: 45px;
    }
  }

  /* 聊天历史 */
  .chat-history {
    width: 100%;
    flex: 1;
    overflow-y: auto;
    align-self: center;
    display: flex;
    flex-direction: column;

    .messages-area {
      display: flex;
      margin-bottom: 10px;
      max-width: 100%;
    }

    .user-message {
      flex-direction: row-reverse; /* 反转用户消息的布局方向 */
    }

    .user-message .message-content {
      margin-right: 10px;
      background-color: rgb(73, 98, 255);
      color: #fff;
      border-radius: 12px 0px 12px 12px;
    }

    .ai-message .message-content {
      margin-left: 10px;
      border-radius: 0px 12px 12px 12px;
      background: rgba(255, 255, 255, 1);
    }

    .message-content {
      padding: 12px 16px;
      max-width: 80%;
      word-wrap: break-word;
    }
    .action-menu {
      position: fixed;
      z-index: 1000;
      background-color: #fff;
      border-radius: 4px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
      padding: 8px 12px;
      font-size: 14px;
    }
  }

  /* 输入区域样式 */
  .input-area {
    width: 100%;
    display: flex;
    align-items: center;
    margin-top: 26px;
    position: relative;
    height: 50px;
    border-radius: 219px;
    background: rgba(255, 255, 255, 1);
    border: 1px solid rgba(71, 185, 230, 0.61);
    box-shadow: 0px 4px 4px rgba(30, 82, 242, 0.07);

    .input-icon {
      padding-left: 17px;
    }

    .chat-input {
      flex: 1;
      height: 50px;
      padding-left: 17px;
    }

    .send-btn {
      margin-right: 10px;
    }

    .send-btn-disabled {
      opacity: 0.5 !important; /* 禁用时半透明 */
    }
  }

  /* 首页 */
  .init-page {
    display: flex;
    flex-direction: column;
    align-self: center;
    margin-top: 60px;
    width: 100%;
    height: 82%;
    min-height: 560px;
    border-radius: 24px;
    background: linear-gradient(270deg, rgba(191, 216, 255, 1) 0%, rgba(225, 232, 255, 1) 100%);
    position: relative;

    .chat-tip {
      padding: 0 29px;
      height: 37%;
      font-size: 18px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      .ai-desc {
        display: flex;
        flex-direction: column;
        .ai-desc1 {
          color: rgba(51, 51, 51, 1);
          font-size: 40px;
          font-weight: 700;
        }
        .ai-desc2 {
          margin-top: 22px;
          font-size: 24px;
          font-weight: 400;
          color: rgba(90, 99, 131, 1);
        }
      }
    }

    .bg-line {
      position: absolute;
      bottom: 0;
    }
  }

  /* 智能体页 */
  .agents-page {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding-top: 40px;
    .agentsPage-title {
      font-size: 32px;
      font-weight: 700;
      color: rgba(51, 51, 51, 1);
      padding-bottom: 40px;
    }
  }

  /* 首页功能按钮样式 */
  .init-function-btns {
    padding: 0 29px;
    display: grid;
    grid-template-columns: repeat(3, 1fr); /* 3列，每列宽度相等 */
    gap: 29px; /* 格子间距 */

    .init-func-btn {
      font-size: 16px;
      min-width: 325px;
      height: 111px;
      border-radius: 24px;
      background: rgba(255, 255, 255, 0.5);
      border: 2px solid rgba(255, 255, 255, 1);
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 6px;
      text-wrap: nowrap;
    }

    .agent-icon {
      width: 48px;
      height: 48px;
      padding-right: 20px;
    }
    .agent-text {
      display: flex;
      flex-direction: column;
      flex: 1;
      overflow: hidden;
      .agent-name {
        font-size: 24px;
        font-weight: 700;
        color: rgba(51, 51, 51, 1);
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
      .agent-detail {
        font-size: 16px;
        color: rgba(102, 102, 102, 1);
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }
  }

  /* 输入区前 功能按钮样式 */
  .function-btns {
    width: 100%;
    display: flex;
    justify-content: flex-start;
    align-items: center;
    flex-wrap: nowrap;
    padding: 10px 25px 10px 25px;
    border-radius: 309px;
    background: rgba(255, 255, 255, 1);
    box-shadow: 0px 2px 4px rgba(0, 0, 0, 0.05);

    /* 限制一行 */
    flex-wrap: wrap;
    max-height: 60px; /* 总体高度*/
    overflow: hidden; /* 超出隐藏 */

    .function-icon {
      padding-right: 15px;
    }

    .func-btn {
      height: 40px;
      min-width: 90px;
      border-radius: 221px;
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 8px 16px 8px 16px;
      margin-right: 16px;
      margin-bottom: 8px;
      border: 2px solid transparent;
      background: rgba(38, 99, 255, 0.07);
    }
    .current-agent {
      border: 2px solid rgba(3, 95, 255, 1);
      background: rgba(255, 255, 255, 1);
    }
  }
</style>
