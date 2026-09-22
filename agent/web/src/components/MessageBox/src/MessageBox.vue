<!-- 提示弹窗-->
<script lang="ts" setup>
  import { useI18n } from '@/hooks';

  const props = withDefaults(
    defineProps<{
      cancelText?: string;
      confirmText?: string;
      iconName?: string;
      onCancel?: () => void;
      onConfirm?: () => void;
      text?: string;
      title?: string;
    }>(),
    {
      iconName: 'message_box_info',
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
  <div class="message-box ground-glass">
    <!-- <div class="header">
      <div class="title">
        {{ title || t('common.promptContent.title') }}
      </div>
      <Icon name="delete" class="close-btn" @click="cancel" />
      <img src="@/assets/images/popup/popup_header_bg.png" alt="" class="header-bg" />
    </div> -->
    <div class="section">
      <div class="content">
        <div class="icon-box">
          <Icon v-show="iconName" class="icon" :name="iconName" />
        </div>
        <div class="tip-text">
          <div>{{ title }}</div>
          <div class="text">{{ text }}</div>
        </div>
      </div>
      <div class="btn-box">
        <TdButton
          :text="cancelText || t('common.promptContent.cancel')"
          type="normal"
          @click="cancel"
        />
        <TdButton
          :text="confirmText || t('common.promptContent.determine')"
          type="normal"
          @click="confirm"
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
    width: 310px;

    .header {
      position: relative;
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 100%;
      height: 34px;
      padding: 0 10px;
      background: var(--background-default);

      .title {
        z-index: 1;
        font-size: 16px;
        line-height: 16px;
      }

      .close-btn {
        z-index: 1;
        width: 20px;
        height: 20px;
        cursor: pointer;
      }

      .header-bg {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
      }
    }

    .section {
      padding: 0 24px 24px;

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
          }
        }

        .tip-text {
          .text {
            word-break: break-all;
            word-wrap: break-word;
          }
        }

        .tip-tip {
          margin-left: 24px;
          font-size: 13px;
          line-height: 24px;
          color: rgb(171 216 255 / 100%);
          word-break: break-all;
        }
      }

      .btn-box {
        display: flex;
        justify-content: flex-end;
        height: 36px;

        .td-button {
          width: 128px;
          margin-right: 8px;
        }
      }
    }
  }
</style>
