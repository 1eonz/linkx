<template>
  <van-popup v-model:show="visible" position="bottom" :style="{ height: '85%' }" round @closed="handleClosed">
    <view class="ai-assistant-page">
      <!-- 顶部区域 -->
      <view class="top-nav-bar">
        <view></view>
        <text class="title">公安智能体超市</text>
        <view></view>
      </view>

      <view class="input-wrapper" style="
          position: relative;
          width: 100%;
          margin-bottom: 12px;
          flex-shrink: 0;
        ">
        <van-search v-model="keywords" placeholder="请输入关键词" :clearable="false" @update:model-value="changeKeyword"
          style="width: 100%" />
        <view v-if="keywords" class="clear-btn" @click="clearKeywords" style="
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
          ">
          <van-icon name="close" size="18" color="#c0c4cc"></van-icon>
        </view>
      </view>

      <div class="tabs-box" v-if="!keywords">
        <div class="tab-item" :class="{ 'tab-item-active': item.id === activeTabId }" v-for="item in allTabs"
          :key="item.id" @click="clickTab(item)">
          {{ item.name }}
        </div>
      </div>

      <!-- 内容区域 -->
      <view class="agent-area">
        <view class="agent-row" v-for="(item, index) in agentsWithPermission" :key="index"
          @click="sessionModeAdded(item)">
          <AuthImg :picUrl="item.picUrl" class="agent-icon"></AuthImg>
          <view class="agent-text">
            <div class="agent-name" v-html="highlightText(item.name, keywords)"></div>
            <text class="agent-detail">{{ item.desc }}</text>
          </view>
          <template v-if="sessionMode">
            <view class="agent-btn" @click.stop="handleUse(item, $event)">使用</view>
          </template>
          <template v-else-if="hasAIAuth || true">
            <view v-if="item?.selectedTime" class="agent-btn" @click.stop="cancelAgentPhsh(item, $event)">取消</view>
            <view v-else class="agent-btn" @click.stop="handleUse(item, $event)">使用</view>
          </template>
          <template v-else>
            <view class="agent-btn apply" @click.stop="openApplyForm(item, $event)" v-if="
              item?.isRestricted === 1 &&
              (item?.applyPermission === 3 || item?.applyPermission === 2)
            ">申请</view>
            <view class="agent-btn apply" v-else-if="item?.isRestricted === 1 && item?.applyPermission === 0">审批中</view>
            <view v-else class="agent-btn" @click.stop="handleUse(item, $event)">使用</view>
          </template>
        </view>
        <!-- 加载提示 -->
        <view class="loading-more" v-if="loading">
          <text>加载中...</text>
        </view>
        <view class="no-more" v-if="agents.length === 0 && !loading">
          <text>没有更多了</text>
        </view>
      </view>
    </view>
  </van-popup>
</template>

<script setup>
import { ref, onMounted, computed } from "vue";
import { aiApi } from "@/common/api/index.js";
import AuthImg from "@/components/AuthImg/index.vue";
import { useAiStore } from "@/stores/ai.js";
import { useCommunicationStore } from "@/stores/communication.js";
import { usePageUrlStore } from "@/stores/pageUrl.js";
import { useDeviceAdapter } from "@/stores/useDeviceAdapter.js";
import { getGlobalsConfigByKey } from "@/common/utils";
import { showToast } from "vant";
import { createSessionId, linkxLog } from "@/utils/aiAssistantUtils.js";

const pageUrlStore = usePageUrlStore();
const { adaptationSize } = useDeviceAdapter();

const aiStore = useAiStore();
const communicationStore = useCommunicationStore();

const visible = ref(false);
const agents = ref([]);
const agentsAll = ref([]);
const allTabs = ref([{ id: "all", name: "全部" }]);
const keywords = ref("");
const activeTabId = ref("all");
const applyPermissionList = ref([]);
const sessionUserId = ref(null);
const loading = ref(false);

let resolvePromise = null;
let rejectPromise = null;

const sessionMode = ref(false);

function show(options = {}) {
  linkxLog('aiAssistant/AgentPopup.vue', 'show', "开始调用");
  visible.value = true;
  sessionMode.value = options.sessionMode || false;
  return new Promise((resolve, reject) => {
    resolvePromise = resolve;
    rejectPromise = reject;
  });
  linkxLog('aiAssistant/AgentPopup.vue', 'show', "调用结束");
}

function handleClose() {
  linkxLog('aiAssistant/AgentPopup.vue', 'handleClose', "开始调用");
  visible.value = false;
  linkxLog('aiAssistant/AgentPopup.vue', 'handleClose', "调用结束");
}

function handleClosed() {
  if (rejectPromise) {
    rejectPromise(new Error("用户取消了操作"));
    resolvePromise = null;
    rejectPromise = null;
  }
}

async function handleBack() {
  await getAiBaseUrl();
  handleClose();
}

async function initUser() {
  try {
    linkxLog('aiAssistant/AgentPopup.vue', 'initUser', "开始调用");
    const res = await communicationStore.getUserInfo();
    sessionUserId.value = res?.userid || null;
    linkxLog('aiAssistant/AgentPopup.vue', 'initUser', "调用结束");
  } catch (error) {
    console.log("获取用户信息失败", error);
    linkxLog('aiAssistant/AgentPopup.vue', 'initUser', "调用失败");
  }
}

const agentsWithPermission = computed(() => {
  const applyListInit = [];
  const applyListACCEPT = [];
  const applyListREFUSE = [];
  applyPermissionList.value?.forEach((item) => {
    if (+item.status === 0) {
      item?.resources.forEach((typeItem) => {
        applyListInit.push(JSON.parse(typeItem.ext)?.index);
      });
    } else if (+item.status === 1) {
      item?.resources.forEach((typeItem) => {
        applyListACCEPT.push(JSON.parse(typeItem.ext)?.index);
      });
    } else if (+item.status === 2) {
      item?.resources.forEach((typeItem) => {
        applyListREFUSE.push(JSON.parse(typeItem.ext)?.index);
      });
    }
  });
  return agents.value.map((item) => {
    if (applyListInit.includes(item.index)) {
      item.applyPermission = 0;
    } else if (applyListACCEPT.includes(item.index)) {
      item.applyPermission = 1;
    } else if (applyListREFUSE.includes(item.index)) {
      item.applyPermission = 2;
    } else {
      item.applyPermission = 3;
    }
    return item;
  });
});

async function getApplyPermission() {
}

onMounted(async () => {
  await initUser();
  getCappPrivJson();
  getAllTabs();
  getAgents(true);
  getApplyPermission();
  window.WeSpaceSDK.onVisibleChange(() => {
    getAiBaseUrl();
    getAgents(true);
    setTimeout(() => {
      getApplyPermission();
    }, 300);
  });
});

function clickTab(item) {
  activeTabId.value = item.id;
  getAgents();
}

const changeKeyword = async (val) => {
  console.log(`changeKeyword11111111: ${val}`);
  getAgents();
};

const clearKeywords = () => {
  keywords.value = "";
  getAgents();
};

async function getAllTabs() {
  linkxLog('aiAssistant/agent.vue', 'getAllTabs', "开始调用");
  const tabsData = await aiStore.getAllTabs();
  allTabs.value = [...allTabs.value, ...tabsData];
  linkxLog('aiAssistant/agent.vue', 'getAllTabs', "调用结束");
}

async function getAgents(init) {
  if (loading.value) return;
  linkxLog('aiAssistant/AgentPopup.vue', 'getAgents', "开始调用");
  const startTime = Date.now();
  const logPrefix = `[getAgents] ${init ? '初始化' : '刷新'} - 分类:${activeTabId.value} 关键词:"${keywords.value}"`;
  linkxLog('aiAssistant/AgentPopup.vue', 'getAgents:  logPrefix', logPrefix);

  if (!init && keywords.value) {
    agents.value = [];
  }

  loading.value = true;
  try {
    linkxLog('aiAssistant/AgentPopup.vue', 'getAgentsList', "接口开始调用");
    const allAgents = await aiStore.getAgentsList({
      name: keywords.value,
      categoryId: keywords.value
        ? null
        : activeTabId.value === "all"
          ? null
          : activeTabId.value,
    });
    linkxLog('aiAssistant/AgentPopup.vue', 'getAgentsList', "接口调用结束");
    agents.value = allAgents.filter(
      (item) => !item.name?.toLowerCase().includes("deepseek")
    );
    if (init) {
      agentsAll.value = [...agents.value];
    }

    const duration = Date.now() - startTime;
    linkxLog('aiAssistant/AgentPopup.vue', 'getAgents', `调用成功 ✅ 耗时: ${duration}ms, 返回数据: ${allAgents.length}条`);
  } catch (error) {
    const duration = Date.now() - startTime;
    linkxLog('aiAssistant/AgentPopup.vue', 'getAgents', `调用失败 ❌ 耗时: ${duration}ms`);
  } finally {
    loading.value = false;
  }
}

async function getAiBaseUrl() {
  try {
    linkxLog('aiAssistant/AgentPopup.vue', 'getAiBaseUrl', "开始调用");
    const aiBaseUrl = (await getGlobalsConfigByKey("ai"))?.replace(/\/$/, "");
    await communicationStore.setStorage(
      "aiBaseUrl",
      unescape(encodeURIComponent(JSON.stringify(aiBaseUrl)))
    );
    linkxLog('aiAssistant/AgentPopup.vue', 'getAiBaseUrl', "调用结束");
  } catch (e) {
    console.log(e);
  }
}

function highlightText(text, keyword) {
  if (!keyword || !text) {
    return text;
  }

  const regex = new RegExp(`(${keyword})`, "gi");

  return text.replace(
    regex,
    '<span style="color: rgba(255, 160, 92, 1); ">$1</span>'
  );
}

async function handleUse(currentAgent, event = null) {
  event?.stopPropagation();
  if (sessionMode.value) {
    const payload = {
      agentIndex: currentAgent?.index ?? null,
      sessionId: createSessionId(),
    };
    if (sessionUserId.value) {
      await window.WeSpaceSDK.setStorage(
        `sessionSelectedAgentIndex_${sessionUserId.value}`,
        unescape(encodeURIComponent(JSON.stringify(payload)))
      );
    }
    aiStore.changeAgentsList(agentsAll.value, currentAgent);
    aiStore.setCurrentAgent(currentAgent?.index);
    handleClose();
    if (resolvePromise) {
      resolvePromise(currentAgent);
      resolvePromise = null;
      rejectPromise = null;
    }
    return;
  }
  aiStore.changeAgentsList(agentsAll.value, currentAgent);
  aiStore.setCurrentAgent(currentAgent?.index);
  handleClose();
  if (resolvePromise) {
    resolvePromise(currentAgent);
    resolvePromise = null;
    rejectPromise = null;
  }
}

async function sessionModeAdded(currentAgent) {
  if (sessionMode.value) {
    await handleUse(currentAgent);
    return;
  }
}

async function navigateToUrl(path, title = null, titleStyle = null) {
  const url = pageUrlStore.getFullPageUrl(path);
  await communicationStore.openUrl(url, title, titleStyle);
}

function openApplyForm(item, event = null) {
  event?.stopPropagation();
  navigateToUrl(
    "/pages/aiAssistant/applyPermissionForm?formType=1&params=" +
    JSON.stringify(item),
    null,
    "noTitleStyle"
  );
}

const cappPrivJson = ref([]);

const hasAIAuth = computed(() => {
  return cappPrivJson.value.includes("1522392406668869999");
});

const getCappPrivJson = async () => {
  try {
    cappPrivJson.value = JSON.parse(
      await window.WeSpaceSDK.getStorage("cappPrivJson")
    );
  } catch (error) {
    console.log("cappPrivJson获取失败", error);
  }
};

function canUseAgent(agent) {
  if (hasAIAuth.value) return true;
  if (agent?.isRestricted === 1) {
    return agent?.applyPermission === 1;
  }
  return true;
}

function cancelAgentPhsh(val, event = null) {
  event?.stopPropagation();
  const agent = agents.value.find((item) => item?.index === val?.index);
  if (agent?.selectedTime) {
    agent.selectedTime = null;
    if (aiStore.currentAgentIndex === val.index) {
      aiStore.setCurrentAgent(null);
    }
    aiStore.getCurrentFunc(agents.value);
  }
}

defineExpose({ show });
</script>

<style scoped lang="scss">
.ai-assistant-page {
  height: 100%;
  padding: 0 20px 20px 20px;
  background: #f5f5f5;
  overflow: hidden;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.top-nav-bar {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  padding: 12px 0;

  .title {
    height: 44px;
    line-height: 44px;
    font-size: 18px;
    color: rgba(3, 8, 26, 1);
  }
}

.bg-area {
  width: 100%;
  position: relative;
  overflow: hidden;
  margin-bottom: 12px;
  height: auto;
  flex-shrink: 0;

  .banner {
    width: 100%;
    height: auto;
    display: block;

    img {
      width: 100%;
      height: auto;
      display: block;
    }
  }
}

.tabs-box {
  width: 100%;
  display: flex;
  align-items: center;
  font-size: 14px;
  font-weight: 400;
  letter-spacing: 0px;
  line-height: 20.27px;
  color: rgba(90, 99, 131, 1);
  margin-bottom: 12px;
  overflow-x: auto;
  overflow-y: hidden;
  white-space: nowrap;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
  -ms-overflow-style: none;
  flex-shrink: 0;

  &::-webkit-scrollbar {
    display: none;
  }

  .tab-item {
    margin-right: 16px;
    border-bottom: 1px solid #f5f5f5;
    padding-bottom: 3px;
    flex-shrink: 0;
    white-space: nowrap;
  }

  .tab-item-active {
    border-color: rgba(38, 78, 209, 1);
    color: rgba(38, 78, 209, 1);
  }
}

.agent-area {
  width: 100%;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
  -ms-overflow-style: none;

  &::-webkit-scrollbar {
    display: none;
  }

  .agent-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 12px;
    background: rgba(255, 255, 255, 1);
    border-radius: 16px;
    margin-bottom: 8px;
    flex-shrink: 0;
  }

  .agent-icon {
    width: 38px;
    height: 38px;
    border-radius: 14px;
    margin-right: 12px;
  }

  .agent-text {
    display: flex;
    flex-direction: column;
    flex: 1;
    overflow: hidden;
    justify-content: space-between;

    .agent-name {
      font-size: 14px;
      color: rgba(3, 8, 26, 1);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .agent-detail {
      font-size: 12px;
      color: rgba(134, 139, 152, 1);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      margin-top: 5px;
    }
  }

  .agent-btn {
    font-size: 14px;
    width: 80px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: rgba(38, 99, 255, 1);
    border-radius: 4px;
    background: rgba(30, 82, 242, 0.1);

    &.apply {
      color: #fc9221;
      background: rgba(252, 146, 33, 0.1);
    }

    &.added {
      color: #999;
      background: #f0f0f0;
      border: 1px solid #e0e0e0;
      pointer-events: none;
    }

    &.apply2 {
      display: flex;
      flex-direction: row;
      justify-content: center;
      align-items: center;
      gap: 6;
      padding: 4px 12px;
      border-radius: 4px;
      background: rgba(238, 242, 255, 1);
      color: rgba(38, 78, 209, 1);
      font-size: 12px;
      font-weight: 400;
      line-height: 14px;
    }
  }

  .agent-session-btn {
    width: 24px;
    height: 24px;
    padding: 0;
    background: transparent;
    border-radius: 50%;
  }

  .agent-status-icon {
    width: 24px;
    height: 24px;
    border-radius: 50%;
    display: block;
  }

  .loading-more {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 20px;
    color: rgba(38, 78, 209, 1);
    font-size: 14px;
  }

  .no-more {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 20px;
    color: #999;
    font-size: 14px;
  }
}

.input-wrapper {

  :deep(.van-search__content),
  :deep(.van-search) {
    background: transparent;
  }
}
</style>
