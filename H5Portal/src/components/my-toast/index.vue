<template>
  <Teleport to="body">
    <Transition name="toast">
      <div v-show="visible" class="custom-toast">
        {{ message }}
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
  import { ref, watch } from 'vue';

  // 接收 props：message 和 duration
  const props = defineProps({
    message: {
      type: String,
      required: true,
    },
    duration: {
      type: Number,
      default: 2000, // 默认 2 秒后自动关闭
    },
  });

  const visible = ref(false);
  let timer = null;

  // 显示 Toast
  const show = () => {
    visible.value = true;
    if (props.duration > 0) {
      timer = setTimeout(() => {
        hide();
      }, props.duration);
    }
  };

  // 隐藏 Toast
  const hide = () => {
    visible.value = false;
    if (timer) {
      clearTimeout(timer);
      timer = null;
    }
  };

  // 监听 message 变化，自动显示（由外部控制）
  watch(
    () => props.message,
    () => {
      show();
    },
    { immediate: true },
  );

  // 组件挂载时显示
  show();
</script>

<style scoped>
  .custom-toast {
    position: fixed;
    left: 50%;
    top: 50%;
    transform: translate(-50%, -50%);
    background: rgba(0, 0, 0, 0.8);
    color: white;
    padding: 12px 20px;
    border-radius: 6px;
    font-size: 14px;
    z-index: 9999;
    max-width: 80%;
    text-align: center;
    word-break: break-all;
    line-height: 1.5;
  }

  /* 动画效果 */
  .toast-enter-active,
  .toast-leave-active {
    transition:
      opacity 0.3s ease,
      transform 0.3s ease;
  }

  .toast-enter-from,
  .toast-leave-to {
    opacity: 0;
    transform: translate(-50%, -50%) scale(0.8);
  }
</style>
