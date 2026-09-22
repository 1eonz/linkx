<template>
  <div class="anchor-wrap">
    <div v-if="shouldShow" class="message-anchor" @click="handleClick">
      <span class="anchor-text">问题审批通过</span>
      <img class="anchor-icon" :src="MessageAnchorIcon" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import MessageAnchorIcon from '@/assets/svg/ai-msg-anchor.svg'

const props = defineProps<{
  visible?: boolean
  target?: string
  scrollTo?: string
}>()

const emit = defineEmits<{
  (e: 'click'): void
  (e: 'scrollEnd'): void
}>()

const isTargetOutOfView = ref(false)
let scrollHandler: (() => void) | null = null

const shouldShow = computed(() => props.visible && isTargetOutOfView.value)

function checkVisibility() {
  const container = props.target ? document.querySelector(props.target) : null
  const targetEl = props.scrollTo ? document.querySelector(props.scrollTo) : null
  if (!container || !targetEl) {
    isTargetOutOfView.value = false
    return
  }
  const containerRect = container.getBoundingClientRect()
  const targetRect = targetEl.getBoundingClientRect()
  isTargetOutOfView.value = targetRect.bottom < containerRect.top || targetRect.top > containerRect.bottom
}

function cleanup() {
  const container = props.target ? document.querySelector(props.target) : null
  if (scrollHandler && container) {
    container.removeEventListener('scroll', scrollHandler)
  }
  scrollHandler = null
}

function startObserving() {
  cleanup()
  const container = props.target ? document.querySelector(props.target) : null
  if (!container) return

  scrollHandler = () => {
    if (props.visible) {
      checkVisibility()
    }
  }
  container.addEventListener('scroll', scrollHandler, { passive: true })
}

function handleClick() {
  const container = props.target ? document.querySelector(props.target) : null
  const targetEl = props.scrollTo ? document.querySelector(props.scrollTo) : null

  if (!container || !targetEl) return

  emit('click')

  targetEl.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

watch(
  () => [props.visible, props.scrollTo],
  () => {
    if (props.visible && props.scrollTo) {
      checkVisibility()
      startObserving()
      if (!isTargetOutOfView.value) {
        emit('scrollEnd')
      }
    } else {
      isTargetOutOfView.value = false
      cleanup()
    }
  },
  { immediate: true }
)

watch(isTargetOutOfView, (newVal, oldVal) => {
  if (oldVal && !newVal && props.visible) {
    emit('scrollEnd')
  }
})

onUnmounted(() => {
  cleanup()
})
</script>

<style scoped lang="less">
.anchor-wrap {
  position: absolute;
  bottom: 6px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.message-anchor {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 14px;
  border-radius: 20px;
  border: 1px solid #f5f5f5;
  background-color: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow: 0 0 16px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: opacity 0.2s;

  &:hover {
    opacity: 0.85;
  }

  &:active {
    opacity: 0.8;
  }
}

.anchor-text {
  font-size: 14px;
  margin-right: 6px;
  color: #264ed1;
}

.anchor-icon {
  width: 12px;
  height: 12px;
}
</style>
