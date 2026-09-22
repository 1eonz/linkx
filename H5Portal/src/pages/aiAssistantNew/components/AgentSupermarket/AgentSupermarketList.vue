<!-- AgentSupermarketList.vue -->
<template>
  <view class="ai-assistant-page">
    <view class="fixed-header">
      <view class="input-wrapper">
        <van-search
          v-model="keywords"
          placeholder="请输入关键词"
          :clearable="false"
          @update:model-value="onKeywordChange"
          style="width: 100%"
        />
        <view v-if="keywords" class="clear-btn" @click="clearKeywords">
          <van-icon name="close" size="18" color="#c0c4cc" />
        </view>
      </view>

      <view class="tabs-box" v-if="!keywords">
        <view
          v-for="tab in allTabs"
          :key="tab.id"
          class="tab-item"
          :class="{ 'tab-item-active': tab.id === activeTabId }"
          @click="onClickTab(tab)"
        >
          {{ tab.name }}
        </view>
      </view>
    </view>

    <view class="agent-area">
      <view
        v-for="(item, index) in agents"
        :key="item.index ?? index"
        class="agent-row"
      >
        <AuthImg :picUrl="item.picUrl" class="agent-icon" />
        <view class="agent-text">
          <view class="agent-name" v-html="highlightText(item.name, keywords)" />
          <text class="agent-detail">{{ item.desc }}</text>
        </view>
        <template v-if="isSessionMode">
          <view
          class="agent-btn"
          :class="usedIndexMap[String(item.index)] ? 'agent-btn-cancel' : 'agent-btn-use'"
          @click.stop="onUse(item , usedIndexMap[String(item.index)])"
          >
          {{ usedIndexMap[String(item.index)] ? '已使用' : '使用' }}
        </view>
        </template>
        <template v-else>
          <view
            class="agent-btn"
            :class="usedIndexMap[String(item.index)] ? 'agent-btn-cancel' : 'agent-btn-use'"
            @click.stop="usedIndexMap[String(item.index)] ? onCancel(item) : onUse(item)"
          >
            {{ usedIndexMap[String(item.index)] ? '取消' : '使用' }}
          </view>
        </template>
      </view>

      <view class="loading-more" v-if="loading">
        <text>加载中...</text>
      </view>
      <view class="no-more" v-if="!loading && agents.length === 0">
        <text>暂无数据</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import AuthImg from '@/components/AuthImg/index.vue'
import { getAgents, getAllTabs } from '@/common/api/ai.js'
import { getUsedAgents, saveUsedAgents } from '@/pages/aiAssistantNew/composables/useAgentCache'
import { getBaseUrl } from '@/common/config.js'

const props = defineProps({
  multiple: { 
    type: Boolean, 
    default: false 
  },
  // 选中的智能体
  selectedAgent: { 
    type: [Object, null], 
    default: null 
  },
  // 多选时候会收集已使用的智能体
  usedAgents: { 
    type: Array, 
    default: () => [] 
  },
  isSessionMode: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['use', 'cancel'])

// 使用 indexMap 的快速查找表
const usedIndexMap = computed(() => {
  const list = props.multiple ? props.usedAgents : (props.selectedAgent ? [props.selectedAgent] : [])
  return Object.fromEntries(list.map(item => [String(item.index), true]))
})

// 监听 selectedAgent 变化，强制重新渲染
watch(
  () => props.selectedAgent,
  (newAgent) => {
    console.log('AgentSupermarketList: selectedAgent changed:', newAgent)
  },
  { deep: true }
)

const agents = ref([])
const allTabs = ref([{ id: 'all', name: '全部', type: 'all' }])
const keywords = ref('')
const activeTabId = ref('all')
const loading = ref(false)
const recentAgents = ref([])

let searchTimer = null
let abortController = null

function getFullAvatarUrl(avatar) {
  if (!avatar) return '';
  if (avatar.startsWith('http://') || avatar.startsWith('https://')) {
    return avatar;
  }
  const baseUrl = getBaseUrl() + '/XA-ics-agent'
  const path = avatar.startsWith('/') ? avatar : `/${avatar}`;
  return `${baseUrl}${path}`;
}

onMounted(async () => {
  recentAgents.value = await getUsedAgents()
  await Promise.all([loadTabs(), loadAgents()])
})
onUnmounted(() => {
  clearTimeout(searchTimer)
  abortController?.abort()
})

async function loadTabs() {
  try {
    const data = await getAllTabs()
    const extra = (data || []).filter(t => t.id !== 'all' && t.id !== 0)
    allTabs.value = [{ id: 'all', name: '全部', type: 'all' }, ...extra]
  } catch (e) {
    console.error('获取分类失败:', e)
  }
}

function onClickTab(tab) {
  activeTabId.value = tab.id
  loadAgents()
}

function onKeywordChange() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(loadAgents, 300)
}

function clearKeywords() {
  keywords.value = ''
  loadAgents()
}

async function loadAgents() {
  abortController?.abort()
  abortController = new AbortController()
  const { signal } = abortController
  agents.value = []
  loading.value = true
  try {
    const params = {
      name: keywords.value || undefined,
      categoryId: (!keywords.value && activeTabId.value !== 'all') ? activeTabId.value : undefined
    }
    const res = await getAgents(params, signal)
    agents.value = (res || []).filter(item => !item.name?.toLowerCase().includes('deepseek')).map(item => ({
      ...item,
      picUrl: getFullAvatarUrl(item.picUrl)
    }))
  } catch (e) {
    if (e.name !== 'CanceledError' && e.name !== 'AbortError') {
      console.error('获取智能体列表失败:', e)
    }
  } finally {
    if (!signal.aborted) {
      loading.value = false
    }
  }
}

function highlightText(text, keyword) {
  if (!keyword || !text) return text
  const escaped = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp(`(${escaped})`, 'gi')
  return text.replace(regex, '<span style="color:rgba(255,160,92,1)">$1</span>')
}

function onUse(agent,isUsed) { 
  if(isUsed) return
  // 保存逻辑由父组件的 watch 自动处理
  emit('use', agent) 
}

function onCancel(agent) { 
  emit('cancel', agent) 
}
</script>

<style scoped lang="scss">
.ai-assistant-page {
  height: 100%;
  overflow: hidden;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.fixed-header {
  padding: 0 16px 0;
}

.tabs-box {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: rgba(90, 99, 131, 1);
  margin: 16px 0 12px;
  overflow-x: auto;
  white-space: nowrap;
  flex-shrink: 0;

  &::-webkit-scrollbar { display: none; }
  scrollbar-width: none;
  -ms-overflow-style: none;

  .tab-item {
    margin: 0 10px;
    padding-bottom: 3px;
    flex-shrink: 0;
    cursor: pointer;
  }

  .tab-item-active {
    color: rgba(38, 78, 209, 1);
    border-bottom: 2px solid rgba(38, 78, 209, 1);
  }
}

.agent-area {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
  -ms-overflow-style: none;
  padding-top: 12px;

  &::-webkit-scrollbar { display: none; }

  .agent-row {
    display: flex;
    align-items: center;
    background: #fff;
    border-radius: 8px;
    padding: 16px;
    margin: 0 16px 12px;
  }

  .agent-icon {
    width: 38px;
    height: 38px;
    border-radius: 14px;
    margin-right: 12px;
    flex-shrink: 0;
  }

  .agent-text {
    flex: 1;
    overflow: hidden;
    margin-right: 12px;

    .agent-name {
      font-size: 14px;
      color: rgba(3, 8, 26, 1);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .agent-detail {
      display: block;
      font-size: 12px;
      color: rgba(134, 139, 152, 1);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      margin-top: 5px;
    }
  }

  .agent-btn {
    width: 64px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    border-radius: 4px;
    flex-shrink: 0;
    cursor: pointer;

    &.agent-btn-use {
      color: rgba(38, 99, 255, 1);
      background: rgba(30, 82, 242, 0.1);
    }

    &.agent-btn-cancel {
      color: rgb(255, 135, 79);
      background: rgba(255, 135, 79, 0.1);
    }
  }

  .loading-more,
  .no-more {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 20px;
    font-size: 14px;
    color: #999;
  }

  .loading-more { color: rgba(38, 78, 209, 1); }
}

.input-wrapper {
  position: relative;

  :deep(.van-search__content),
  :deep(.van-search) {
    background: #ffffff;
  }

  :deep(.van-search) {
    padding: 0;
    border-radius: 6px;
    overflow: hidden;
  }
}

.clear-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 10;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
</style>
