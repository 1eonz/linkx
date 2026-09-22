<!-- 提示弹窗-->
<script lang="ts" setup>
  import { useI18n } from '@/hooks';

  const props = withDefaults(
    defineProps<{
      cancelText?: string;
      confirmText?: string;
      iconName?: string;
      isLight?: boolean;
      onCancel?: () => void;
      onConfirm?: () => void;
      text?: string;
      title?: string;
    }>(),
    {
      iconName: 'message_box_error',
      isLight: false,
      title: '',
    },
  );
  const emit = defineEmits(['closeDialog']);
  const { t } = useI18n();

  function confirm() {
    props.onConfirm?.();
    emit('closeDialog');
  }
  function cancel() {
    props.onCancel?.();
    emit('closeDialog');
  }
</script>

<template>
  <div class="message-box" :class="isLight ? 'glass-light' : 'ground-glass'">
    <div class="header">
      <div class="title">
        {{ title || t('common.promptContent.title') }}
      </div>
      <Icon class="close-btn" name="close" @click="cancel" />
    </div>
    <div class="section">
      <div class="content">
        <div class="icon-box">
          <Icon v-show="iconName" class="icon" :name="iconName" />
        </div>
        <div class="tip-text" :class="{ 'tip-light': isLight }">
          <div class="text" :class="{ 'text-light': isLight }">{{ text }}</div>
        </div>
      </div>
      <div class="btn-box">
        <TdButton
          :active="true"
          :is-light="isLight"
          :text="confirmText || t('common.promptContent.determine')"
          type="normal"
          @click="confirm"
        />
        <TdButton
          :is-light="isLight"
          :text="cancelText || t('common.promptContent.cancel')"
          type="normal"
          @click="cancel"
        />
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .position {
    position: absolute;
    top: 25vh;
    left: 25vw;
  }

  .message-box {
    z-index: 1;
    width: 500px;
    background: var(--background-color);

    .header {
      position: relative;
      display: flex;
      align-items: center;
      justify-content: flex-end;
      width: 100%;
      height: 34px;
      padding: 0 10px;
      background: none;

      .line {
        width: 4px;
        height: 14px;
        margin-right: 8px;
      }

      .title {
        z-index: 1;
        width: 100%;
        font-size: 16px;
        line-height: 16px;
        color: var(--section-text-light) !important;
      }

      .close-btn {
        z-index: 1;
        width: 10px;
        height: 10px;
        cursor: pointer;
      }
    }

    .section {
      padding: 0 15px 15px;

      .content {
        display: flex;
        justify-content: left;
        min-height: 76px;
        padding: 16px 0;
        overflow: hidden;

        .icon-box {
          margin: 4px 8px 0 0;

          .icon {
            width: 16px;
            height: 16px;
            filter: var(--svg-filter);
          }
        }

        .tip-text {
          .text {
            color: var(--text-color);
            word-break: break-all;
            word-wrap: break-word;
          }

          .text-light {
            color: var(--section-text-light);
          }
        }

        .tip-tip {
          margin-left: 24px;
          font-size: 13px;
          line-height: 24px;
          color: var(--section-text-light);
          word-break: break-all;
        }
      }

      .btn-box {
        display: flex;
        justify-content: flex-end;
        height: 30px;

        .td-button {
          box-sizing: border-box;
          width: 45px;
          margin-left: 10px;
          background: none;
          border: 1px solid var(--border-color);

          :deep(.button-text) {
            color: var(--text-color);
          }

          &:hover {
            background: var(--button-active-color);
            border: none;

            :deep(.button-text) {
              color: #fff;
            }
          }
        }

        .normal_active {
          background: var(--button-active-color);
          border: none;

          :deep(.button-text) {
            color: #fff;
          }
        }
      }
    }
  }
</style>
