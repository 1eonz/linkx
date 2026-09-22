<script lang="ts" setup>
  import { computed, onBeforeUnmount, ref } from 'vue';

  import { useI18n, usePermissions } from '@/hooks';
  import { messageStatusColor, messageStatusText } from '@/pages/messages/common';

  import { debounce } from 'lodash-es';

  const props = defineProps({
    clickId: {
      default: null,
      type: [Number, String],
    },
    data: {
      default: () => {},
      type: Object,
    },
    keyText: {
      default: '',
      type: String,
    },
  });
  const emit = defineEmits(['clickItem', 'handleIgnore']);
  const { t } = useI18n();
  const showButton = ref(false);

  const showValid = computed(() => {
    return usePermissions('MISSION') && usePermissions('DISPATCH') && usePermissions('eBC');
  });

  onBeforeUnmount(() => {
    showButton.value = false;
  });

  function click() {
    emit('clickItem', props.data);
  }
  function mouseover() {
    showButton.value = true;
  }
  function mouseout() {
    showButton.value = false;
  }
  const update = debounce(async (type) => {
    if (type === 2) {
      emit('handleIgnore', true, props.data);
    } else {
      emit('handleIgnore', false, props.data);
    }
  }, 500);
</script>

<template>
  <div
    class="common-list-item message-list-item"
    :class="{ 'common-list-item_active': data.id === clickId }"
    @click="click"
    @mouseout="mouseout"
    @mouseover="mouseover"
  >
    <div class="message-left">
      <div class="message-title">
        <TdTag
          class="message-status"
          :label="messageStatusText(data)"
          :type="messageStatusColor(data)"
        />
        <HighlightKeywords
          class="message-address"
          :content="data.title"
          font-color-class="address"
          :keyword="keyText"
        />
      </div>
      <div class="message-context">
        <span class="context-status">
          {{ t('message.status.name') }}
        </span>
        <HighlightKeywords
          :content="data.context"
          font-color-class="text-default"
          :keyword="keyText"
        />
      </div>
      <div class="message-time">
        <span class="source">
          {{ t('message.source') }}
        </span>
        <span class="equipment-name">{{ data.equipmentName }}</span>
        <HighlightKeywords :content="data.gmtCreated" font-color-class="light" :keyword="keyText" />
      </div>
    </div>

    <div v-show="showButton && data.status === 0" class="message-right">
      <TdButton
        v-if="showValid"
        class="confirm-btn"
        :text="t('videoControl.warningInformation.valid')"
        type="normal"
        @click.stop="update(1)"
      />
      <TdButton
        class="cancel-btn"
        :text="t('videoControl.warningInformation.ignore')"
        type="normal"
        @click.stop="update(2)"
      />
    </div>
  </div>
</template>

<style lang="less" scoped>
  .message-list-item {
    position: relative;
    display: flex;
    min-height: 110px;

    .message-left {
      box-sizing: border-box;
      display: flex;
      flex-direction: column;
      justify-content: center;
      width: 100%;

      .message-title {
        margin-bottom: 6px;
        word-break: break-all;

        .message-status {
          margin-right: 4px;
        }

        .message-address {
          width: 210px;
          font-size: 14px;
          line-height: 18px;
          word-break: break-all;
        }
      }

      .message-context {
        font-size: 12px;
        word-break: break-all;

        .context-status {
          color: var(--text-title-second);
        }
      }

      .message-time {
        font-size: 12px;
        color: var(--text-title-second);

        .source {
          color: var(--text-title-second);
        }

        .equipment-name {
          margin-right: 4px;
        }
      }
    }

    .message-right {
      position: absolute;
      top: 0;
      right: 0;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      width: 80px;
      height: 100%;
      font-size: 14px;
      color: var(--text-color-button);
      background-image: linear-gradient(to right, rgb(25 41 60 / 0%), rgb(25 41 60 / 100%));

      .button {
        padding: 0;
      }

      :deep(.guide) {
        width: auto;
      }

      .confirm-btn {
        width: 60px;
        height: 24px;
      }

      .cancel-btn {
        width: 60px;
        height: 24px;
        margin-top: 4px;
      }
    }
  }
</style>
