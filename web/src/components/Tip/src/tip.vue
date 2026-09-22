<script setup lang="ts">
  defineOptions({
    name: 'TdTip',
  });

  defineProps<{
    disable?: boolean;
    msg?: string;
  }>();

  const emit = defineEmits(['handelCancel', 'handleConfirm']);

  function handelCancel() {
    emit('handelCancel');
  }

  function handleConfirm() {
    emit('handleConfirm');
  }
</script>

<template>
  <div class="edit-tip">
    <div class="header">
      <div class="left"> <Icon class="line" name="line" /> <span>群名称</span></div>
      <Icon class="close-btn" name="close" @click="handelCancel" />
    </div>
    <div v-if="msg" class="content content1">
      <img alt="" src="@/assets/images/message/icon_tip.png" />{{ msg }}
    </div>
    <div v-else class="content">
      <slot></slot>
    </div>
    <div class="bottom-btns">
      <TdButton :is-light="true" size="auto" text="取消" type="normal" @click="handelCancel" />
      <TdButton
        :active="true"
        :disable="disable"
        :is-light="true"
        size="auto"
        text="确定"
        type="normal"
        @click="handleConfirm"
      />
    </div>
  </div>
</template>

<style lang="less" scoped>
  .edit-tip {
    position: absolute;
    top: 50%;
    left: 50%;
    z-index: 102;
    width: 360px;
    padding: 16px 0;
    background: var(--edit-tip);
    border: 1px solid var(--glass-light-border);
    border-radius: 2px;
    // border: 0.0625rem solid transparent;
    // border-image: linear-gradient(180deg, rgba(26 255 251 / 20%) 0%, rgba(26 255 251 / 100%) 100%);
    // border-image-slice: 1;
    transform: translate(-50%, -50%);

    .header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 16px 8px;
      // border-bottom: 1px solid #eee;

      .left {
        display: flex;
        align-items: center;

        .line {
          width: 4px;
          height: 14px;
          margin-right: 8px;
        }

        span {
          font-family: 'HarmonyOS Sans SC';
          font-size: 16px;
          font-weight: 400;
          line-height: 24px;
          color: var(--item-text-color);
          text-align: center;
          letter-spacing: 0;
        }
      }

      .close-btn {
        width: 12px;
        height: 12px;
        cursor: pointer;
        filter: var(--svg-filter);
      }
    }

    .content {
      padding: 6px 0 16px;
      color: var(--item-text-color);
    }

    .content1 {
      display: flex;
      align-items: center;
      padding: 24px 16px;

      img {
        margin-right: 8px;
      }
    }

    .bottom-btns {
      display: flex;
      align-items: center;
      justify-content: flex-end;
      padding: 0 16px;

      .td-button {
        padding: 2px 12px;
        font-size: 14px;
        font-weight: 500;
        background: var(--button-text-inner) !important;

        &:first-child {
          margin-right: 16px;
        }

        :deep(.button-text-light) {
          color: var(--item-text-color) !important;
        }
      }
    }
  }

  :deep(.el-textarea__inner) {
    color: var(--item-text-color) !important;

    &:focus-within {
      color: var(--item-text-color);
      background: var(--edit-tip-textarea);
      border: 1px solid #00c2ff;
    }
  }
</style>
