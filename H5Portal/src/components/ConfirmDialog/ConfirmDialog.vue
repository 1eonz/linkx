<template>
  <van-dialog
    v-model:show="visible"
    class="confirm-dialog"
    :show-confirm-button="false"
    :show-cancel-button="false"
  >
    <view class="confirm-dialog-message">{{ message }}</view>
    <view class="confirm-dialog-footer">
      <view class="confirm-dialog-cancel" @click="handleCancel">{{ cancelButtonText }}</view>
      <view class="confirm-dialog-confirm" @click="handleConfirm">{{ confirmButtonText }}</view>
    </view>
  </van-dialog>
</template>

<script setup>
import { computed } from 'vue';

defineOptions({ name: 'ConfirmDialog' });

const props = defineProps({
  show: { type: Boolean, default: false },
  message: { type: String, default: '' },
  confirmButtonText: { type: String, default: '保存' },
  cancelButtonText: { type: String, default: '取消' },
});

const emit = defineEmits(['update:show', 'confirm', 'cancel']);

const visible = computed({
  get: () => props.show,
  set: (val) => emit('update:show', val),
});

function handleConfirm() {
  visible.value = false;
  emit('confirm');
}

function handleCancel() {
  visible.value = false;
  emit('cancel');
}
</script>

<style scoped lang="scss">
.confirm-dialog {
  .confirm-dialog-message {
    padding: 36px 24px 32px;
    font-size: 16px;
    font-weight: 600;
    color: #1d2129;
    text-align: center;
    line-height: 1.5;
  }

  .confirm-dialog-footer {
    display: flex;
    gap: 16px;
    padding: 0 16px 16px;

    .confirm-dialog-cancel,
    .confirm-dialog-confirm {
      flex: 1;
      height: 40px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 8px;
      font-size: 15px;
    }

    .confirm-dialog-cancel {
      background-color: #f2f3f5;
      color: #969799;
    }

    .confirm-dialog-confirm {
      background-color: #007aff;
      color: #fff;
    }
  }
}
</style>
