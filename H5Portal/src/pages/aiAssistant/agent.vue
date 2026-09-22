<template>
  <view class="ai-assistant-page">
    <!-- 顶部区域 -->
    <view :style="{ width: '100%', height: paddingTop + 'px' }"></view>
    <view class="top-nav-bar">
      <van-icon
          name="arrow-left"
          :size="adaptationSize.iconSize"
          color="#333"
          @click="handleBack()"
      />
      <text class="title">公安智能体超市</text>
      <view></view>
    </view>

    <!-- bg图 -->
    <view class="bg-area">
      <img
          src="@/static/ai/ai_ad.png"
          mode="widthFix"
          width="100%"
          class="banner"
      />
    </view>

    <view
        class="input-wrapper"
        style="
        position: relative;
        width: 100%;
        margin-bottom: 12px;
        flex-shrink: 0;
      "
    >
      <van-search
          v-model="keywords"
          placeholder="请输入关键词"
          :clearable="false"
          @update:model-value="changeKeyword"
          style="width: 100%"
      />
      <view
          v-if="keywords"
          class="clear-btn"
          @click="clearKeywords"
          style="
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
        "
      >
        <van-icon name="close" size="18" color="#c0c4cc"></van-icon>
      </view>
    </view>

    <div class="tabs-box" v-if="!keywords">
      <div
          class="tab-item"
          :class="{ 'tab-item-active': item.id === activeTabId }"
          v-for="item in allTabs"
          :key="item.id"
          @click="clickTab(item)"
      >
        {{ item.name }}
      </div>
    </div>

    <!-- 内容区域 -->
    <view class="agent-area">
      <view
          class="agent-row"
          v-for="(item, index) in agentsWithPermission"
          :key="index"
          @click="sessionModeAdded(item)"
      >
        <AuthImg :picUrl="item.picUrl" class="agent-icon"></AuthImg>
        <view class="agent-text">
          <div
              class="agent-name"
              v-html="highlightText(item.name, keywords)"
          ></div>
          <text class="agent-detail">{{ item.desc }}</text>
        </view>
        <template v-if="sessionMode">
          <view class="agent-btn" @click.stop="handleUse(item, $event)">使用</view>
        </template>
        <template v-else-if="hasAIAuth || true">
          <view v-if="item?.selectedTime" class="agent-btn" @click.stop="cancelAgentPhsh(item, $event)"
          >取消</view
          >
          <view v-else class="agent-btn" @click.stop="handleUse(item, $event)">使用</view>
        </template>
        <template v-else>
          <view
              class="agent-btn apply"
              @click.stop="openApplyForm(item, $event)"
              v-if="
              item?.isRestricted === 1 &&
              (item?.applyPermission === 3 || item?.applyPermission === 2)
            "
          >申请</view
          >
          <view
              class="agent-btn apply"
              v-else-if="item?.isRestricted === 1 && item?.applyPermission === 0"
          >审批中</view
          >
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
</template>

<script setup>
import { ref, onMounted, computed } from "vue";
import { useRoute } from "vue-router";
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
// 使用设备适配
const { adaptationSize } = useDeviceAdapter();

const aiStore = useAiStore();
const communicationStore = useCommunicationStore();
const route = useRoute();
const agents = ref([]);
const agentsAll = ref([]);
const paddingTop = ref(0);
const allTabs = ref([{ id: "all", name: "全部" }]);
const keywords = ref("");
const activeTabId = ref("all");
const applyPermissionList = ref([]);
const sessionUserId = ref(null);

// 加载状态
const loading = ref(false);

async function handleBack() {
  linkxLog('aiAssistant/agent.vue', 'handleBack', "开始调用");
  await getAiBaseUrl();
  await communicationStore.close();
  linkxLog('aiAssistant/agent.vue', 'handleBack', "调用结束");
}
async function initUser() {
  try {
    linkxLog('aiAssistant/agent.vue', 'initUser', "开始调用");
    const res = await communicationStore.getUserInfo();
    sessionUserId.value = res?.userid || null;
    linkxLog('aiAssistant/agent.vue', 'initUser', "调用结束");
  } catch (error) {
    linkxLog('aiAssistant/agent.vue', 'initUser', "调用失败", error);
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
      item.applyPermission = 0; //审批中
    } else if (applyListACCEPT.includes(item.index)) {
      item.applyPermission = 1; //同意
    } else if (applyListREFUSE.includes(item.index)) {
      item.applyPermission = 2; //拒绝
    } else {
      item.applyPermission = 3; //默认
    }
    return item;
  });
});
async function getApplyPermission() {
  // const userInfo = await communicationStore.getUserInfo();
  // aiApi
  //   .getAgentSubmissionPage({
  //     currentPage: 1,
  //     pageSize: 100,
  //     fromId: userInfo.userid,
  //     available: true,
  //   })
  //   .then((res) => {
  //     console.log(res);
  //     applyPermissionList.value = res?.records || [];
  //   })
  //   .catch((err) => {
  //     console.log(err);
  //   });
}
onMounted(async () => {
  await initUser();
  getCappPrivJson();
  getAllTabs();
  getAgents(true);
  getApplyPermission();
  paddingTop.value = await communicationStore.fetchStatusBarHeight();
  window.WeSpaceSDK.onVisibleChange(() => {
    getAiBaseUrl();
    getAgents(true);
    setTimeout(() => {
      //重新获取按钮权限
      getApplyPermission();
    }, 300);
  });
});

function clickTab(item) {
  activeTabId.value = item.id;
  getAgents();
}
const changeKeyword = async (val) => {
  getAgents();
};

// 清空关键字
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
// 智能体数据
async function getAgents(init) {
  linkxLog('aiAssistant/agent.vue', 'getAgents', "开始调用");
  if (loading.value) return;

  // 记录开始时间
  const startTime = Date.now();
  const logPrefix = `[getAgents] ${init ? '初始化' : '刷新'} - 分类:${activeTabId.value} 关键词:"${keywords.value}"`;
  console.log(`${logPrefix} 开始调用...`);

  // 搜索时先清空旧数据，避免显示过期内容
  if (!init && keywords.value) {
    agents.value = [];
  }

  loading.value = true;
  try {
    linkxLog('aiAssistant/agent.vue', 'getAgents', "接口调用开始");
    const allAgents = await aiStore.getAgentsList({
      name: keywords.value,
      // 当有关键词时搜索全部，没有关键词时按分类搜索
      categoryId: keywords.value
          ? null
          : activeTabId.value === "all"
              ? null
              : activeTabId.value,
    });
    linkxLog('aiAssistant/agent.vue', 'getAgents', "接口调用结束");
    agents.value = allAgents.filter(
        (item) => !item.name?.toLowerCase().includes("deepseek")
    );
    if (init) {
      agentsAll.value = [...agents.value];
    }
    
    // 计算耗时
    const duration = Date.now() - startTime;
    linkxLog('aiAssistant/agent.vue', 'getAgents', `调用成功 ✅ 耗时: ${duration}ms, 返回数据: ${allAgents.length}条`);
  } catch (error) {
    // 计算耗时（失败情况）
    const duration = Date.now() - startTime;
    linkxLog('aiAssistant/agent.vue', 'getAgents', `调用失败: ${duration}ms`, error);
  } finally {
    loading.value = false;
  }
}

// 获取最新的aiBaseUrl
async function getAiBaseUrl() {
  try {
    linkxLog('aiAssistant/agent.vue', 'getAiBaseUrl', "开始调用");
    const aiBaseUrl = (await getGlobalsConfigByKey("ai"))?.replace(/\/$/, "");
    await communicationStore.setStorage(
        "aiBaseUrl",
        unescape(encodeURIComponent(JSON.stringify(aiBaseUrl)))
    );
    linkxLog('aiAssistant/agent.vue', 'getAiBaseUrl', "调用结束");
  } catch (e) {
    linkxLog('aiAssistant/agent.vue', 'getAiBaseUrl', "调用失败");
  }
}

// 高亮关键词
function highlightText(text, keyword) {
  if (!keyword || !text) {
    return text;
  }

  // 创建正则表达式，不区分大小写
  const regex = new RegExp(`(${keyword})`, "gi");

  // 替换匹配的文本，添加黄色样式
  return text.replace(
      regex,
      '<span style="color: rgba(255, 160, 92, 1); ">$1</span>'
  );
}

// 会话模式标识
const sessionMode = computed(
    () => route.query.sessionMode === "true" || route.query.sessionMode === "1"
);

// 选择智能体
async function handleUse(currentAgent, event = null) {
  // 阻止事件冒泡，避免触发外层列表点击
  linkxLog('aiAssistant/agent.vue', 'handleUse', "开始调用");
  event?.stopPropagation();
  if (sessionMode.value) {
    // if (!canUseAgent(currentAgent)) {
    //   showToast("该智能体需提交申请，审批通过后可使用");
    //   return;
    // }
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
    await communicationStore.close();
    return;
  }
  linkxLog('aiAssistant/agent.vue', 'handleUse', "changeAgentsList开始调用");
  aiStore.changeAgentsList(agentsAll.value, currentAgent);
  linkxLog('aiAssistant/agent.vue', 'handleUse', "changeAgentsList调用结束, setCurrentAgent开始调用");
  aiStore.setCurrentAgent(currentAgent?.index);
  linkxLog('aiAssistant/agent.vue', 'handleUse', "setCurrentAgent调用结束, close开始调用");
  await communicationStore.close();
  linkxLog('aiAssistant/agent.vue', 'handleUse', "close调用结束");
}

async function sessionModeAdded(currentAgent) {
  if (sessionMode.value) {
    // 权限检查：无权限直接提示
    await handleUse(currentAgent);
    return;
  }
}
async function navigateToUrl(path, title = null, titleStyle = null) {
  linkxLog('aiAssistant/agent.vue', 'navigateToUrl', "开始调用");
  const url = pageUrlStore.getFullPageUrl(path);
  await communicationStore.openUrl(url, title, titleStyle);
  linkxLog('aiAssistant/agent.vue', 'navigateToUrl', "调用结束");
}

function openApplyForm(item, event = null) {
  // 阻止事件冒泡，避免触发外层列表点击
  event?.stopPropagation();
  navigateToUrl(
      "/pages/aiAssistant/applyPermissionForm?formType=1&params=" +
      JSON.stringify(item),
      null,
      "noTitleStyle"
  );
}
//获取勾选权限
const cappPrivJson = ref([]);
//判断智能体是否需要申请,是否有AI权限
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
//获取勾选权限
function canUseAgent(agent) {
  // 有AI权限直接放行
  if (hasAIAuth.value) return true;
  // 受限智能体需审批通过
  if (agent?.isRestricted === 1) {
    return agent?.applyPermission === 1;
  }
  // 非受限默认可用
  return true;
}
//取消使用智能体
function cancelAgentPhsh(val, event = null) {
  // 阻止事件冒泡，避免触发外层列表点击
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
//取消使用智能体
</script>

<style scoped lang="scss">
/* 基础样式 */
.ai-assistant-page {
  height: 100vh;
  padding: 0 20px 20px 20px;
  background: #f5f5f5;
  overflow: hidden;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

/* 头部样式 */
.top-nav-bar {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0; /* 防止被压缩 */

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
  /* 防止图片在平板上溢出遮挡后续内容 */
  overflow: hidden;
  /* 将间距放在容器上，保证容器与后续内容有间隔 */
  margin-bottom: 12px;
  /* 让容器高度自适应内容 */
  height: auto;
  flex-shrink: 0; /* 防止被压缩 */

  .banner {
    width: 100%;
    /* 移除固定高度，让图片根据宽度自动调整高度 */
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
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE and Edge */
  flex-shrink: 0; /* 防止被压缩 */

  &::-webkit-scrollbar {
    display: none; /* Chrome, Safari and Opera */
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
  flex: 1; /* 占据剩余空间 */
  overflow-y: auto;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0; /* 重要：允许flex子项缩小 */
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE and Edge */

  &::-webkit-scrollbar {
    display: none; /* Chrome, Safari and Opera */
  }

  .agent-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 12px;
    background: rgba(255, 255, 255, 1);
    border-radius: 16px;
    margin-bottom: 8px;
    flex-shrink: 0; // 防止被压缩
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

  // 加载状态样式
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
