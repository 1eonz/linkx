<template>
  <view class="anchor-wrap">
    <view v-if="shouldShow" class="message-anchor" @click="handleClick">
      <text class="anchor-text">问题审批通过</text>
      <img class="anchor-icon clickable" :src="MessageAnchorIcon" />
    </view>
  </view>
</template>

<script setup>
import { computed, onUnmounted, ref, watch } from 'vue'
import MessageAnchorIcon from '@/assets/svg/ai-msg-anchor.svg'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  target: {
    type: String,
    default: '',
  },
  scrollTo: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['click', 'scrollEnd'])

const isTargetOutOfView = ref(false)
let scrollHandler = null
let scrollEndHandler = null
let scrollPollTimer = null

const shouldShow = computed(() => props.visible && isTargetOutOfView.value)

function checkVisibility() {
  const container = document.querySelector(props.target)
  const targetEl = document.querySelector(props.scrollTo)
  if (!container || !targetEl) {
    isTargetOutOfView.value = false
    return
  }
  const containerRect = container.getBoundingClientRect()
  const targetRect = targetEl.getBoundingClientRect()
  isTargetOutOfView.value = targetRect.bottom < containerRect.top || targetRect.top > containerRect.bottom
}

function cleanup() {
  const container = document.querySelector(props.target)
  if (scrollHandler && container) {
    container.removeEventListener('scroll', scrollHandler)
  }
  scrollHandler = null
  if (scrollEndHandler && container) {
    container.removeEventListener('scrollend', scrollEndHandler)
  }
  scrollEndHandler = null
  if (scrollPollTimer) {
    clearInterval(scrollPollTimer)
    scrollPollTimer = null
  }
}

function startObserving() {
  cleanup()
  const container = document.querySelector(props.target)
  if (!container) return

  scrollHandler = () => {
    if (props.visible) {
      checkVisibility()
    }
  }
  container.addEventListener('scroll', scrollHandler, { passive: true })
}

function onScrollEnd() {
  cleanup()
  isTargetOutOfView.value = false
  emit('scrollEnd')
}

function handleClick() {
  const container = document.querySelector(props.target)
  const targetEl = document.querySelector(props.scrollTo)

  if (!container || !targetEl) return

  emit('click')

  cleanup()

  if ('onscrollend' in window) {
    scrollEndHandler = onScrollEnd
    container.addEventListener('scrollend', scrollEndHandler, { once: true })
  } else {
    let lastScrollTop = container.scrollTop
    let stableCount = 0
    scrollPollTimer = setInterval(() => {
      if (container.scrollTop === lastScrollTop) {
        stableCount++
        if (stableCount >= 3) {
          onScrollEnd()
        }
      } else {
        stableCount = 0
        lastScrollTop = container.scrollTop
      }
    }, 100)
  }

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

<style scoped lang="scss">
.anchor-wrap {
  position: absolute;
  bottom: 6px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  z-index: 100;
}

.message-anchor {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 14px;
  border-radius: 20px;
  border: 1px solid #F5F5F5;
  background-color: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow: 0 0 16px rgba(0, 0, 0, 0.08);
  cursor: pointer;

  &:active {
    opacity: 0.8;
  }
}

.anchor-text {
  font-size: 14px;
  margin-right: 6px;
  color: #264ED1;
}

.anchor-icon {
  width: 12px;
  height: 12px;
}
</style>
