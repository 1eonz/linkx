<!-- index.vue -->
<template>
  <view class="agent-supermarket-wrap">
    <img class="agent-supermarket-trigger clickable" :width="adaptationSize.weightSm" :height="adaptationSize.weightSm"
      :src="AgentSupermarketIcon" @click="popupVisible = true" />
    <AgentsUsedList v-if="multiple" :recentList="recentList" :selectedAgent="selectedAgent"
      @change="onChangeUsedAgent" />
  </view>

  <van-popup v-model:show="popupVisible" position="bottom" :style="{ height: '80%' }" round @close="onClose">
    <view class="picker-header">
      <text class="picker-title">选择智能体</text>
    </view>
    <view class="picker-content">
      <AgentSupermarketList :multiple="multiple" :usedAgents="usedList" :selectedAgent="selectedAgent" :isSessionMode="isSessionMode" @use="onUseAgent"
        @cancel="onCancelAgent" />
    </view>
  </van-popup>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import AgentSupermarketList from './AgentSupermarketList.vue'
import AgentsUsedList from './AgentsUsedList.vue'
import AgentSupermarketIcon from '@/assets/svg/agent_supermarket.svg'
import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js'
import { useRouter } from 'vue-router'
import { getUsedAgents, saveUsedAgents, getSelectedAgent, saveSelectedAgent, clearAgentCache } from '@/pages/aiAssistantNew/composables/useAgentCache'

const { adaptationSize } = useDeviceAdapter()
const router = useRouter()

onMounted(async () => {
  // 标记初始化完成，开启缓存更新监听
  hydrated.value = true
})

const props = defineProps({
  currAgent: { 
    type: Object, 
    default: null 
  },
  isSessionMode: {
    type: Boolean,
    default: false
  }
})

// 根据 isSessionMode 计算 multiple
const multiple = computed(() => !props.isSessionMode)

// 根据 isSessionMode 计算缓存模式 session 或 general
const cacheMode = computed(() => props.isSessionMode ? 'session' : 'general')

// 监听模式变化，重新加载对应模式的缓存（包括初始化时）
watch(
  () => props.isSessionMode,
  async (newIsSessionMode) => {
    const mode = newIsSessionMode ? 'session' : 'general'
    
    // 重新加载对应模式的缓存
    const cachedUsedAgents = await getUsedAgents(mode)
    usedList.value = [...cachedUsedAgents]
    
    const cachedSelectedAgent = await getSelectedAgent(mode)
    
    // 初始化内部选中状态（仅用于UI回显，不emit，currPageAgent由initPage管理）
    if (newIsSessionMode) {
      // 会话模式：智能体列表由后端配置，无需校验是否在已用列表中
      if (cachedSelectedAgent) {
        selectedAgent.value = cachedSelectedAgent
      }
    }
    // 综合模式：缓存选中在已用列表中，直接恢复选中
    else if (cachedSelectedAgent && usedList.value.some(a => String(a.index) === String(cachedSelectedAgent.index))) {
      // selectedAgent.value = cachedSelectedAgent
      onChangeUsedAgent(cachedSelectedAgent)
    } 
    // 综合模式：缓存选中不在已用列表中（列表被清理或缓存过期），插入列表头部并恢复选中
    else if (cachedSelectedAgent && !usedList.value.some(a => String(a.index) === String(cachedSelectedAgent.index))) {
      usedList.value.unshift(cachedSelectedAgent)
      onChangeUsedAgent(cachedSelectedAgent)
      await saveUsedAgents(usedList.value, mode)
    }
    // 综合模式：无缓存选中，默认选已用列表第一个（最近使用的）
    else if (usedList.value.length > 0) {
      onChangeUsedAgent(usedList.value[0])
      await saveSelectedAgent(selectedAgent.value, mode)
    }
  },
  { immediate: true }
)

const emit = defineEmits(['select', 'confirm', 'beforeChange'])

const popupVisible = ref(false)

// 监听父组件传入的 currAgent 变化，同步回显选中状态
watch(
  () => props.currAgent,
  async (newAgent) => {
    if (newAgent) {
      selectedAgent.value = newAgent
    }
  },
  { immediate: true, deep: true }
)

// 已使用和最近使用的前3个 - 直接从 usedList 派生（界面逻辑，不保存）
const recentList = computed(() => usedList.value.slice(0, 3))

//选中的智能体(多选默认将最近使用作为选中)
const selectedAgent = ref(props.currAgent ?? null)
const hydrated = ref(false)  // 标记缓存初始化完成

// 已使用列表
const usedList = ref([])

// 监听 usedList 变化，自动更新本地缓存（仅 general 模式）
watch(
  usedList,
  async (newList, oldList) => {
    if (!hydrated.value || props.isSessionMode) return;
    await saveUsedAgents(newList, cacheMode.value)
  },
  { deep: true }
)

// 监听 selectedAgent 变化，自动更新本地缓存
watch(
  selectedAgent,
  async (newAgent) => {
    if (!hydrated.value) return
    await saveSelectedAgent(newAgent, cacheMode.value)
  },
  { deep: true }
)

async function onUseAgent(agent) {
  selectedAgent.value = agent
  emit('select', agent)
  if (multiple.value) {
    // 已存在则移除（保证最新使用的在前面）
    usedList.value = usedList.value.filter(item => String(item.index) !== String(agent.index))
    // 添加到开头
    usedList.value.unshift(agent)
    // 限制最多3个，超过则移除最早的（与 web 端规则一致）
    if (usedList.value.length > 3) {
      usedList.value.splice(3)
    }

    await saveUsedAgents(usedList.value, cacheMode.value)
    await saveSelectedAgent(agent, cacheMode.value)
  } else {
    await saveSelectedAgent(agent, cacheMode.value)
    popupVisible.value = false
    emit('beforeChange')
    router.replace({
      path: '/pages/aiAssistantNew/index',
      query: {
        init: 'true',
        index: agent.index,
        name: agent.name,
        picUrl: agent.picUrl
      }
    })
  }
}

async function onCancelAgent(agent) {
  if (multiple.value) {
    const agentIndex = String(agent.index)
    usedList.value = usedList.value.filter(item => String(item.index) !== agentIndex)
    
    // 直接保存缓存（不依赖watch）
    await saveUsedAgents(usedList.value, cacheMode.value)
    
    // 如果列表被清空，清空本地缓存
    if (usedList.value.length === 0) {
      await clearAgentCache(cacheMode.value)
    }
    
    if (String(selectedAgent.value?.index) === agentIndex) {
      const next = usedList.value[0] ?? null
      selectedAgent.value = next
      emit('select', next)
      await saveSelectedAgent(next, cacheMode.value)
    }
  } else {
    selectedAgent.value = null
    emit('select', null)
    await saveSelectedAgent(null, cacheMode.value)
    popupVisible.value = false
    emit('confirm', null)
  }
}

// 弹窗关闭
function onClose() {
  if (props.isSessionMode && selectedAgent.value) {
    saveSelectedAgent(selectedAgent.value, 'session');
  }
  if (multiple.value) {
    emit('confirm', {
      usedList: [...usedList.value],
      selectedAgent: selectedAgent.value
    })
  }
}

// 已使用列表切换选中
function onChangeUsedAgent(agent) {
  selectedAgent.value = agent
  emit('select', agent)
}
</script>

<style scoped lang="scss">
.agent-supermarket-wrap {
  padding: 0 12px;
  padding-top: 8px;
  // background: rgba(255, 255, 255, 1);
  display: flex;
  align-items: center;
  overflow: hidden;
  position: relative;
  z-index: 1;

  .agent-supermarket-trigger {
    width: 36px;
    height: 36px;
    border-radius: 4px;
    margin-right: 10px;
    cursor: pointer;
    flex-shrink: 0;
  }
}

.picker-header {
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;

  .picker-title {
    font-size: 16px;
    color: rgba(48, 48, 48, 1);
  }
}

.picker-content {
  height: calc(100% - 52px);
  overflow-y: auto;
  padding: 12px 0;
  background: #f5f5f5;
}
</style>
