<!-- AgentsUsedList.vue -->
<template>
  <view class="used-agent-btns">
    <view
      v-for="agent in recentList"
      :key="agent.index"
      class="agent-btn"
      :class="{ active: String(activeIndex) === String(agent.index) }"
      :style="{ height: adaptationSize.weightSm + 'px' }"
      @click="onSelect(agent)"
    >
      <AuthImg :picUrl="agent.picUrl" class="agent-icon" />
      {{ agent.name }}
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import AuthImg from '@/components/AuthImg/index.vue'
import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js'

const { adaptationSize } = useDeviceAdapter()

const props = defineProps({
  recentList: { type: Array, default: () => [] },
  selectedAgent: { type: Object, default: null }
})

const emit = defineEmits(['change'])

// 用 computed 派生，不需要本地 ref + watch
const activeIndex = computed(() => props.selectedAgent?.index ?? null)

function onSelect(agent) {
  emit('change', agent)
}
</script>

<style scoped lang="scss">
.used-agent-btns {
  display: flex;
  justify-content: flex-start;
  flex-wrap: nowrap;
  overflow-x: auto;
  overflow-y: hidden;
  -webkit-overflow-scrolling: touch;

  &::-webkit-scrollbar { display: none; }
  -ms-overflow-style: none;
  scrollbar-width: none;

  .agent-btn {
    min-height: 36px;
    font-size: 14px;
    border-radius: 2vw;
    background: rgba(255, 255, 255, 1);
    display: flex;
    align-items: center;
    white-space: nowrap;
    flex-shrink: 0;
    margin-right: 10px;
    padding: 0 12px;
    border: 1px solid rgba(239, 239, 239, 1);
    box-sizing: border-box;
    cursor: pointer;

    &.active {
      border-color: rgba(30, 82, 242, 1);
    }
  }

  .agent-icon {
    width: 20px;
    height: 20px;
    border-radius: 4px;
    margin-right: 8px;
  }

  @media screen and (min-height: 1200px) {
    .agent-btn { font-size: 0.5rem; }
    .agent-icon { width: 32px; height: 32px; }
  }

  @media screen and (min-height: 2001px) {
    .agent-icon { width: 42px; height: 42px; }
  }
}
</style>
