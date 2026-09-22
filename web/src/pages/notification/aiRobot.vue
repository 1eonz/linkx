<script setup lang="ts">
  import { nextTick, ref, unref, watch } from 'vue';

  import { aiQuery } from '@/api/aiAssistant';
  import aiImg from '@/assets/images/ai/ai-assistant.png';
  import personImg from '@/assets/svg/tree/tree_person.svg';
  import { Message } from '@/components/Message';
  import { useI18n } from '@/hooks';
  import dataUtil from '@/utils/dataUtil';

  const emit = defineEmits(['close']);

  const { t } = useI18n();
  const chatValue = ref('');
  const historyRef = ref();
  const chatTextAreaRef = ref();
  const historyMessages = ref<any[]>([]);
  const showWelcome = ref(true);

  watch(
    historyMessages,
    () => {
      showWelcome.value = false;
      scrollBottom();
    },
    { deep: true },
  );

  function isSelf(data) {
    return data.role === 'user';
  }

  function getRoleImg(item) {
    return isSelf(item) ? personImg : aiImg;
  }

  function scrollBottom() {
    nextTick(() => {
      if (!unref(historyRef)) return;
      historyRef.value.scrollTop = unref(historyRef).scrollHeight;
    });
  }

  function handleKeyup(e) {
    if (e.keyCode === 13) {
      handleSend();
    }
  }

  async function handleSend() {
    const dest = unref(chatValue);

    if (dest === '') {
      return;
    }

    if (dataUtil.wordSizeOf(dest) > 1000) {
      Message(t('communication.communicationTips.msgWord'));
      return;
    }

    chatValue.value = '';
    const questionList = {
      content: dest,
      role: 'user',
    };

    const params = {
      messages: [questionList],
      model: 'AyenaSpring',
    };
    historyMessages.value.push(questionList);
    const aiResultList = await aiQuery(params);
    console.log('==aiResultList==ai交互返回值', aiResultList);
    if (aiResultList.id) {
      const tmpMessages: any[] = [];
      aiResultList.choices.forEach(({ message }) => {
        tmpMessages.push(message);
      });
      historyMessages.value.push(...tmpMessages);
    } else {
      historyMessages.value.push({
        content: t('aiAssistant.messageTips.serverError'),
        isServerError: true,
        role: 'assistant',
      });
    }
  }

  function closeWindow() {
    emit('close');
  }
</script>

<template>
  <!-- 用户短信 -->
  <TdFrameBox
    class="ai-wrapper"
    size="small"
    :title="t('homePage.menus.aIPoliceAssistant')"
    @close-frame-box="closeWindow"
  >
    <div ref="historyRef" class="history">
      <div v-if="showWelcome" class="init-robot">
        <img alt="" src="@/assets/images/ai/welcome-ai.png" />
        <div class="tips-bag">
          <div class="help-tips">{{ t('aiAssistant.welcome.help') }}</div>
          <span class="server-tips">{{ t('aiAssistant.welcome.server') }}</span>
        </div>
      </div>
      <div v-for="(item, index) in historyMessages" v-else :key="index + Math.random() * 100">
        <div class="message" :class="{ myself: isSelf(item) }">
          <img alt="" :src="getRoleImg(item)" />
          <p class="text" :class="{ serverError: item.isServerError }">{{ item.content }} </p>
        </div>
      </div>
    </div>
    <div class="chat-input">
      <TdTextarea
        ref="chatTextAreaRef"
        v-model="chatValue"
        class="chat-left"
        :placeholder="t('aiAssistant.search.enterProblems')"
        @keyup="handleKeyup"
      />
      <TdButton
        :disable="chatValue === ''"
        icon-name="ai_search"
        :text="t('aiAssistant.btns.searchBtn')"
        @click="handleSend"
      />
    </div>
  </TdFrameBox>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .ai-wrapper {
    position: absolute;
    top: 85px;
    right: 16px;
    z-index: 10;
    height: calc(100% - 115px);

    .history {
      width: 100%;
      height: calc(100% - 70px);
      padding: 20px 10px;
      overflow-y: auto;

      .init-robot {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;

        .tips-bag {
          width: 320px;
          height: 48px;
          padding: 4px 0;
          text-align: center;
          background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(41 233 194 / 28%) 100%);
          border: 1px solid rgb(41 233 194 / 100%);
          border-bottom: none;
          border-radius: 30px;

          .help-tips {
            height: 20px;
            font-size: 18px;
            font-weight: 500;
          }

          .server-tips {
            font-size: 14px;
            color: #a6cefd;
          }
        }
      }

      .time {
        margin-bottom: 10px;
        font-size: 12px;
        font-weight: 400;
        color: #a6cefd;
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

        .serverError {
          background: rgb(255 96 96 / 100%);

          &::before {
            border-bottom: 14px solid rgb(255 96 96 / 100%);
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
      display: flex;
      width: 100%;
      height: 48px;
      padding: 0 10px;

      :deep(.td-textarea) {
        height: 100%;

        textarea {
          width: 460px;
          padding: 2px 8px;
          font-size: 14px;
          font-weight: 400;
          background: inherit !important;
          border: 1px solid #00c2ff;
        }
      }

      :deep(.td-button) {
        width: 150px;
        height: 48px;
        margin-left: 10px;

        .button-text {
          font-size: 16px;
        }

        .icon {
          width: 18px;
          height: 18px;
          margin-right: 4px;
        }
      }
    }
  }
</style>
