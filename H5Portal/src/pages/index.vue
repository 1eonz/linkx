<template>
  <view v-if="systemLimit || !effectLicense" class="license-limit">系统功能受限，请联系管理员</view>
  <view class="container" v-else>
    <!-- 顶部导航栏 -->
    <TopNavBar
      ref="topNavBarRef"
      :licensePermissions="licensePermissions"
    >
      <template #left>
        <div class="ai-img" v-if="showAiIcon" @click="handleToAi">
          <img width="24px" height="24px" src="@/static/tabIcon/home_ai.png" />
        </div>
      </template>

      <template #title="{ title }">
        <NodeSelector :title="title" />
      </template>

      <template #right>
        <div class="msg-notify" @click="handlerMsgNotify">
          <img width="24px" height="24px" src="@/static/tabIcon/msgNotify.svg" />
        </div>
        <XietongSwitch ref="xietongRef" v-if="showXietongSwitch" />
      </template>
    </TopNavBar>

    <!-- 内容区域 -->
    <view :style="{ 'padding-top': paddingTop + 50 + 'px' }">
      <van-pull-refresh v-model="loading" @refresh="onRefresh">
        <view v-if="!effectLicense || licensePermissions.LINKXBS !== '1'" class="license-limit">
          系统功能受限，请联系管理员
        </view>

        <!-- 动态渲染布局组件 -->
        <template v-for="section in layoutSections" :key="section.id">
          <component
            :is="getSectionComponent(section.type)"
            v-bind="getSectionProps(section)"
            @page-disappear="handlePageDisappear"
          />
        </template>
      </van-pull-refresh>
    </view>
  </view>
</template>

<script setup>
  import { marked } from 'marked';
import qs from 'qs';
import { showToast } from 'vant';
import { ref, computed, onMounted, onBeforeUnmount, watch, onUnmounted, nextTick } from 'vue';

// 组件引入
import Banner from './banner.vue';
import CollaborativeGroup from './components/CollaborativeGroup.vue';
import CommonApps from './components/CommonApps.vue';
import DividerBar from './components/DividerBar.vue';
import MessageList from './components/MessageList.vue';
import ThirdPartyOrTask from './components/ThirdPartyOrTask.vue';
import TopNavBar from './components/TopNavBar.vue';
import XietongSwitch from './components/xietongSwitch.vue';
import NodeSelector from './components/NodeSelector.vue';

import { taskSave, getTaskByMsgId, responsesTask, responsesTaskAll, getLayoutSections, getSystemConfig } from '@/common/api/h5.js';
import { h5Api, groupApi } from '@/common/api/index.js';
import DC from '@/common/network/DC.js';
import { useSocketManage } from '@/common/network/ws.js';
import { getGlobalsConfigByKey, getCachedGlobalsConfig } from '@/common/utils';
import { useCommon } from '@/hooks/useCommon.js';
import { useEmitter } from '@/hooks/useEmitter.js';
  import { useIntentExecutor } from '@/hooks/useIntentExecutor.js';
import { useApplicationStore } from '@/stores/application.js';
import { useCommunicationStore } from '@/stores/communication.js';
import { usePageUrlStore } from '@/stores/pageUrl.js';
import { useUserStore } from '@/stores/user.js';
import { debounce } from '@/utils';
import { sendNotification } from '@/utils/glassUtils.js';
import { clearTimerLocation, checkLicenseStatus, heartbeatAddress } from '@/utils/totalFunc.js';
import { handleNotificationClick } from '@/utils/notification.js';
import {createNotifyMessage} from "@/common/api/h5.js";
  import { stripHtml } from '@/utils/aiAssistantUtils.js';
  // 使用小乔推送处理 Hook
  const { initIntentExecutor } = useIntentExecutor();
const { userStore: userStoreCommon } = useCommon();
const { setUserInfo } = userStoreCommon;

const emitter = useEmitter();
const communicationStore = useCommunicationStore();
const applicationStore = useApplicationStore();
const userStore = useUserStore();
const pageUrlStore = usePageUrlStore();

// 组件引用
const topNavBarRef = ref();
const xietongRef = ref();

// 状态
const loading = ref(false);
const systemLimit = ref(false);
const userInfo = ref(null);
const accessToken = ref('');
const licensePermissions = ref({ LINKXBS: '1', LINKXBCF: '1' });
const cappPrivJson = ref([]);
const layoutSections = ref([]);
const systemConfigMap = ref({}); // 系统配置

let socketManage = null;
let closeFlag = false;



// 布局组件映射表
const sectionComponentMap = {
  1: Banner,
  2: CommonApps,
  3: CollaborativeGroup,
  4: ThirdPartyOrTask,
  5: DividerBar,
  6: MessageList,
};

// 获取布局组件
const getSectionComponent = (type) => sectionComponentMap[type];

// 获取布局组件 props
const getSectionProps = (section) => {
  const baseProps = {
    licensePermissions: licensePermissions.value,
    permissionsArr: permissionsArr.value,
    accessToken: accessToken.value,
    userInfo: userInfo.value,
    onGetUserInfo: getUserInfo,
    custom: parseCustom(section.custom),
    section,
    systemConfig: systemConfigMap.value, // 传递系统配置
  };

  return baseProps;
};

// 计算属性
const permissionsArr = computed(() => communicationStore.permissionsArr);

const effectLicense = computed(() => {
  const obj = licensePermissions.value || {};
  if (obj.LINKXBS !== '1') return false;
  if (obj.status === '0' || obj.status === '3' || obj.status === '5') return false;
  for (let i in obj) {
    if (i !== 'status' && i !== 'LINKXBS' && i !== 'expireDate') {
      if (obj[i] === '1') return true;
    }
  }
  return false;
});

// 是否显示 AI 图标
const showAiIcon = computed(
  () =>
    permissionsArr.value.includes('ai') &&
    licensePermissions.value.LINKXACF === '1' &&
    licensePermissions.value.LINKXBS === '1',
);

// 是否显示协同开关
const showXietongSwitch = computed(
  () =>
    licensePermissions.value.LINKXBS === '1' && licensePermissions.value.LINKXGCF === '1',
);

const paddingTop = computed(() => topNavBarRef.value?.paddingTop || 0);

// 获取用户信息
const getUserInfo = async () => {
  try {
    const res = await communicationStore.getUserInfo();
    if (res) {
      userInfo.value = res;
      console.log('打印用户信息', JSON.stringify(userInfo.value, null, 2));
      // 缓存用户信息，供子页面（如createGroup）跨页面复用
      communicationStore.setStorage('cachedUserInfo', unescape(encodeURIComponent(JSON.stringify(res))));
    } else {
      console.error('获取用户信息失败或WeSpaceSDK不可用');
    }
  } catch (error) {
    console.error(`获取用户信息错误: ${error.message}`);
  }
};

// 获取 License 权限
const getLicensePermissions = async () => {
  const res = await h5Api.getLicensePermissions({});
  licensePermissions.value = res || {};
  console.log('licensePermissions', res);
  checkLicenseStatus(res);
  // 缓存license权限，供子页面（如createGroup）跨页面复用
  communicationStore.setStorage('cachedLicensePermissions', unescape(encodeURIComponent(JSON.stringify(res || {}))));
};

// 获取首页布局配置
const fetchLayoutSections = async () => {
  try {
    const res = await getLayoutSections({show: 1});
    console.log('本地调试---首页布局配置', res)
    // 过滤掉 show !== 1 和 deleted === 1 的项，并按 sort 排序
    layoutSections.value = (res || [])
      .filter((item) => item.show === 1 && item.deleted !== 1)
      .sort((a, b) => a.sort - b.sort);
    console.log('layoutSections', layoutSections.value);
  } catch (error) {
    console.error('获取布局配置失败:', error);
    layoutSections.value = [];
  }
};

// 获取系统配置
const fetchSystemConfig = async () => {
  try {
    const res = await getSystemConfig();
    const configList = res || [];

    configList.forEach(item => {
      try {
        // CREAT_GROUP_CONFIG 是数组格式，其他是对象格式
        if (item.key === 'CREAT_GROUP_CONFIG') {
          systemConfigMap.value[item.key] = JSON.parse(item.value);
        } else {
          systemConfigMap.value[item.key] = JSON.parse(item.value);
        }
      } catch (e) {
        systemConfigMap.value[item.key] = item.key === 'CREAT_GROUP_CONFIG' ? [] : {};
      }
    });

    console.log('系统配置:', systemConfigMap.value);
  } catch (error) {
    console.error('获取系统配置失败:', error);
  }
};

// 解析 custom JSON
const parseCustom = (customStr) => {
  if (!customStr) return {};
  try {
    return JSON.parse(customStr);
  } catch {
    return {};
  }
};

// 获取登录信息
const getLoginInfo = async () => {
  const data = await userStore.getStoreUserInfo();
  systemLimit.value = data?.code === 182;
  if (!systemLimit.value) {
    accessToken.value = data.accessToken;
    cappPrivJson.value = data.roles?.[0]?.cappPrivJson || [];
    window.WeSpaceSDK?.setStorage('cappPrivJson', JSON.stringify(cappPrivJson.value));
  }
  return data;
};

// Token 登录
const tokenLogin = async (params) => {
  await userStore.setUserInfo(params);
};

// 登录后处理
const afterLoginProcess = async () => {
  const data = await getLoginInfo();
  if (systemLimit.value) return;

  handlePageDisappear();
  accessToken.value = data.accessToken;
  await window.WeSpaceSDK?.setStorage('wsAccessToken', data.accessToken);

  socketManage = await useSocketManage();
  await socketManage.initWebSocket();

  return data;
};

// 初始化（优化：并行请求，移除重复调用）
const initData = async () => {
  // 并行请求互不依赖的数据
  const [, , , , loginData] = await Promise.all([
    getUserInfo(),
    getLicensePermissions(),
    fetchLayoutSections(),
    fetchSystemConfig(),
    getLoginInfo(),
  ]);

  if (!systemLimit.value) {
    // 非关键操作并行执行（不阻塞首屏渲染）
    heartbeatAddress();

    // 缓存全局配置，供子页面（如createGroup）跨页面复用
    const cachedGlobals = getCachedGlobalsConfig();
    if (cachedGlobals) {
      communicationStore.setStorage('cachedGlobalsConfig', unescape(encodeURIComponent(JSON.stringify(cachedGlobals))));
    }

    // 并行执行非阻塞操作
    Promise.all([
      applicationStore.getBannerData(),
      communicationStore.h5permissions(),
    ]).then(() => {
      // 缓存权限数组，供子页面（如createGroup）跨页面复用
      communicationStore.setStorage('cachedPermissionsArr', unescape(encodeURIComponent(JSON.stringify(communicationStore.permissionsArr))));
    });

    // 等待子组件挂载完成后再触发事件
    await nextTick();
    emitter.emit('INDEX_INIT');
    // 获取协同开关状态
    xietongRef.value?.getSwitchStatusFunc();
  }
};

// 下拉刷新
const onRefresh = async () => {
  try {
    await setUserInfo();
    await afterLoginProcess();
    await getLicensePermissions();
    await fetchLayoutSections();
    await fetchSystemConfig();
    // 获取轮播图数据
    applicationStore.getBannerData();
    // 等待子组件更新完成后再触发事件
    await nextTick();
    emitter.emit('INDEX_REFRESH');
    xietongRef.value?.getSwitchStatusFunc();
    communicationStore.h5permissions();
    setTimeout(() => {
      showToast('刷新成功');
      loading.value = false;
    }, 1000);
  } catch (error) {
    console.log('刷新失败：', error);
    setTimeout(() => {
      showToast('刷新失败，请重试！');
      loading.value = false;
    }, 1000);
  }
};

// 页面消失处理
const handlePageDisappear = async () => {
  if (socketManage) {
    await socketManage.closeWebSocket();
    socketManage = null;
  }
};

// Kickout 处理
const kickoutMethod = async () => {
  await getUserInfo();
  const aastokenNew = userInfo.value?.aastoken;
  if (!aastokenNew) {
    initData();
    return;
  }
  await setUserInfo();
  await afterLoginProcess();
};

// Watch token
watch(
  () => userInfo.value?.aastoken,
  async (aastokenNew, aastokenOld) => {
    if (!aastokenNew || aastokenNew === aastokenOld) return;
    await tokenLogin({ clientId: 'CAPP-1000', token: aastokenNew });
    emitter.emit('TASKS_UPDATE');
  },
  { immediate: true },
);

// Watch accessToken
watch(
  () => userStore.userInfo?.accessToken,
  async () => {
    if (userStore.userInfo?.accessToken) {
      await getLicensePermissions();
      try {
        await afterLoginProcess();
        xietongRef.value?.getSwitchStatusFunc();
        communicationStore.h5permissions();
      } catch (error) {
        console.log('登录改变', error);
      }
    }
  },
);

// AI 跳转
const handleToAi = async () => {
  const env = ''
  if (env === 'xiongan') {
    try {
      const aiBaseUrl = (await getGlobalsConfigByKey('ai'))?.replace(/\/$/, '');
      await communicationStore.setStorage('aiBaseUrl', unescape(encodeURIComponent(JSON.stringify(aiBaseUrl))));
      const interactionMode = (await getGlobalsConfigByKey('AI_AGENT_INTERACTION')) || '';
      if (interactionMode === 'session') {
        navigateToUrl('/pages/aiAssistant/index?sessionMode=true', null, 'noTitleStyle');
      } else {
        navigateToUrl('/pages/aiAssistant/index', null, 'noTitleStyle');
      }
    } catch (e) {
      console.log(e);
    }
  } else {
    try {
      const aiBaseUrl = (await getGlobalsConfigByKey('ai'))?.replace(/\/$/, '');
      await communicationStore.setStorage('aiBaseUrl', unescape(encodeURIComponent(JSON.stringify(aiBaseUrl))));
      navigateToUrl('/pages/aiAssistantNew/index', null, 'noTitleStyle');
    } catch (e) {
      console.log(e);
    }
  }
};

const handlerMsgNotify = async () => {
  // const a = {"contactId":"39628891270150","text":"zhouwh用户发送的通知消息","url":"https://www.baidu.com","showInNotification":"1","notificationId":"2074687813406289921"}
  // communicationStore.showNotification(a);
  // createNotifyMessage(a);
  const testUrl = `/pagesMain/notifyList`;
  navigateToUrl(testUrl,'', 'noTitleStyle');
};

// 页面跳转
const navigateToUrl = async (path, title = null, titleStyle = null) => {
  if (path.includes('application')) {
    applicationStore.getCurrentApp();
  }
  const url = pageUrlStore.getFullPageUrl(path);
  console.log(url, '=====url页面跳转路径=====');
  await communicationStore.openUrl(url, title, titleStyle);
};

// 监听任务
const listenTask = async (val) => {
  const BACKEND_GENERATION_TASK = await getGlobalsConfigByKey('BACKEND_GENERATION_TASK', true);
  if (BACKEND_GENERATION_TASK === 'true' || BACKEND_GENERATION_TASK === true) {
    console.log('新建任务直接走后台');
    return;
  }
  if (licensePermissions.value.LINKXBS !== '1' || licensePermissions.value.LINKXGCF !== '1') {
    console.log('license权限不足');
    return;
  }

  // 检查任务是否过期
  if (isExpiredTaskMessage(val.time)) {
    console.log('@协同岗时间大于当前时间超过1分钟，不创建待办任务。');
    return;
  }

  const deptInfo = await window.WeSpaceSDK?.getUserInfoByUserId({
    userId: val.sender,
    forceRemote: false,
  });
  const deptments = deptInfo?.userDepartments?.filter((item) => item.isPrimary) || [];
  if (deptments.length === 0) {
    console.log('任务创建者没有部门，无法创建任务');
    return;
  }

  const deptment = deptments[0];
  await taskSave({
    fromUserDepartmentId: deptment.departmentId,
    fromUserDepartmentName: deptment.departmentName,
    fromUserId: deptInfo.userId,
    fromUserName: deptInfo.userName,
    fromUserNick: deptInfo.userName,
    groupId: val.groupid,
    groupName: val.groupName,
    icsMsgId: val.msgid,
    isDeleted: 0,
    msgFileId: '',
    seqid: val.msgid,
    status: 1,
    text: val.text,
    toExecutorId: userInfo.value.userid,
    userId: userInfo.value.userid,
    postId: val.receiver,
  });
};

// 判断任务消息是否过期
const isExpiredTaskMessage = (time) => {
  if (!time) return true;
  const currentTime = Date.now();
  return currentTime - Number(time) > 60 * 1000;
};

// 发送眼镜消息
const sendNotificationToUser = debounce(async (val) => {
  const deptInfo = await window.WeSpaceSDK?.getUserInfoByUserId({
    userId: val.sender,
    forceRemote: false,
  });
  if (deptInfo) {
    const glassMessage = `${deptInfo.userName}在${val.groupName}群里@了我:${removeAtFormat(val.text)}`;
    console.log('[协同岗@我的消息]通知：上app通知栏，消息内容为：', glassMessage);
    sendNotification({ text: glassMessage, msgid: val.msgid });
  }
}, 500);

// 移除 @ 格式
const removeAtFormat = (text) => {
  return text.replace(/\[@\d+:@[^@]+ \]/g, '');
};

// 引用消息处理
const quotationMssage = async (val) => {
  const deptInfo = await window.WeSpaceSDK?.getUserInfoByUserId({
    userId: val.sender,
    forceRemote: false,
  });
  const deptments = deptInfo?.userDepartments?.filter((item) => item.isPrimary) || [];
  if (deptments.length === 0) {
    console.log('回复操作人没有部门，无法创建任务');
    return;
  }
  const deptment = deptments[0];

  const replyDirectly = await getGlobalsConfigByKey('REPLY_DIRECTLY', true);
  const replyDirectlyFlag = replyDirectly === 'true' || replyDirectly === true;
  console.log('REPLY_DIRECTLY', replyDirectlyFlag);
  if (!replyDirectlyFlag) {
    await hasQuotationMessage(val, deptInfo, deptment);
  } else {
    await noQuotationMessage(val, deptInfo, deptment);
  }
};

// 有引用消息的处理
const hasQuotationMessage = async (val, deptInfo, deptment) => {
  if (!val.refmsgid) {
    console.log('回复内容没有引用消息，不予回复操作');
    return;
  }
  const taskInfo = await getTaskByMsgId({
    icsMsgId: val.refmsgid,
    postId: val.receiver,
  });
  console.info(taskInfo, '============taskInfo');
  if (!taskInfo?.taskId) {
    console.log('该引用消息没有对应任务，不予回复操作');
    return;
  }

  const userInfos = await groupApi.getColloration({ userIds: [val.receiver] });
  let collorationInfo = {};
  if (userInfos && Array.isArray(userInfos)) {
    collorationInfo = userInfos[0] || {};
  }
  // 判断是否@发起人
      const atRegex = /(\[@.*?\])/g;
    const atMatches = val.text.match(atRegex) || [];
    let atPerson = false;
    for (const at of atMatches) {
      const regex = /\[@(.*?):@/;
      const id2 = at.match(regex)[1];
      if (Number(id2) === Number(taskInfo.fromUserId)) {
        atPerson = true;
      }
    }
    console.info(atPerson, '该消息是否@发起人，不予回复操作');
    if (!atPerson) return;
  if (deptInfo) {
    const msgArr = [
      {
        content: val.text,
        departmentId: deptment.departmentId,
        departmentName: deptment.departmentName,
        fromExecutorId: val.sender,
        msgFileId: val.msgid,
        seqid: val.msgid,
        taskId: taskInfo.taskId,
        userId: userInfo.value.userid,
        userName: `${collorationInfo.postName || ''}(${deptInfo.userName})`,
        userNick: deptInfo.userName,
        postId: val.receiver,
      },
    ];
    await responsesTask(msgArr);
  }
};

// 无引用消息的处理
const noQuotationMessage = async (val, deptInfo, deptment) => {
  // if (val.filetype !== 9) return; //filetype === 9 代表存文本和表情,不包含@和图片等。雄安现场需求回复的所有消息都可以回复任务
  const userInfos = await groupApi.getColloration({ userIds: [val.receiver] });
  const collorationInfo = userInfos?.[0] || {};
  console.log(collorationInfo, '============collorationInfo');

  if (deptInfo) {
    const msgArr = {
      content:val.filetype === 9 || val.filetype === 5998 ? val.text : '', //文字、表情和@消息
      departmentId: deptment.departmentId,
      departmentName: deptment.departmentName,
      fromExecutorId: val.sender,
      msgFileId: val.msgid,
      seqid: val.msgid,
      userId: userInfo.value.userid,
      userName: `${collorationInfo.postName || ''}(${deptInfo.userName})`,
      userNick: `${collorationInfo.postName || ''}(${deptInfo.userName})`,
      postId: val.receiver,
      groupId: val.groupid,
    };
    await responsesTaskAll(msgArr);
  }
};

// 任务眼镜处理
const taskGlassProcess = (val, type) => {
  const idCard = userInfo.value?.idCard;
  const executors = val?.executors;
  if (executors?.length > 0 && idCard) {
    const index = executors.findIndex((item) => item.idCard === idCard);
    if (index !== -1) {
      setTimeout(() => {
        const text = `任务${type},标题为${val?.name},状态为${val?.status}`;
        console.log('[任务推送]通知：上app通知栏，消息内容为：', text);
        sendNotification({ text: `任务${type},标题为${val?.name},状态为${val?.status}` });
      });
    }
  }
};

// 推送消息
const pushMessage = (message, type) => {
  const data = {
    url: message?.url,
    text: message?.title || type || '',
    contactId: userInfo.value?.userid,
  };
  applicationStore.getBannerData();
  sendNotification(data);
};

// 设置调试参数
const setDebugQueryString = () => {
  try {
    const queryString = window?.location?.search;
    const urlParams = new URLSearchParams(queryString || '');
    window?.WeSpaceSDK?.setStorage(
      'debugQueryString',
      urlParams.get('debug') === 'true' ? 'debug=true' : '',
    );
  } catch (error) {
    console.log(error);
  }
};

// iframe 消息处理
const convertJsonKeyQuotes = (jsonFragment) => {
  let cleanStr = jsonFragment.trim();
  if (cleanStr.startsWith('"') && cleanStr.endsWith('"')) {
    cleanStr = cleanStr.slice(1, -1);
  }
  const keyValuePattern = /"([^"]+)":\s*("([^"]*)"|([^,"]+))/g;
  return cleanStr.replace(keyValuePattern, (match, key, valueFull, valueStr, valueNonStr) => {
    const formattedKey = `'${key}'`;
    const formattedValue = valueStr !== undefined ? `'${valueStr}'` : valueNonStr;
    return `${formattedKey} : ${formattedValue}`;
  });
};

const handleMessage = (event) => {
  console.log('全部消息', event);
  const data = event.data;
  console.log('收到消息：', data);
  if (!data) return;
  if (data.type === 'tab') {
    const str = JSON.stringify(data);
    const pureStr = str.slice(1, -1);
    const dealStr = convertJsonKeyQuotes(pureStr);
    console.log('dealStr---------', dealStr);
    const params = {
      appId: 'ITEM_TASK_PAGE',
      data: dealStr,
    };
    communicationStore.switchTab(params);
  } else if (data.type === 'xcx' && data.typeUrl) {
    jumpToFun(data.typeUrl);
  }
};

const jumpToFun = (data) => {
  const urlData = new URL(data);
  const obj = qs.parse(urlData.search.replace(/\?/g, ''));
  obj.pathname = urlData.pathname;
  const url = urlData.protocol + '//' + urlData.host;
  console.log(url, JSON.stringify(obj), '$$$$$$$$$$$$$$$$$$$$$$$$$');
  communicationStore.openUrlApp(url, obj);
};

// 生命周期
onMounted(async () => {
  setDebugQueryString();
  initData();

  // 初始化小乔推送监听
  initIntentExecutor();
  // SDK 事件监听
  window.WeSpaceSDK?.onIcpUserStatusChange((val) => {
    if (val === 'online') initData();
  });

  window.WeSpaceSDK?.onStorageChange('currentAppSet', () => {
    applicationStore.getCurrentApp();
  });

  window.WeSpaceSDK?.onVisibleChange(async (val) => {
    console.log('页面可见性变化', val);
     emitter.emit('SDK_VISIBLE_CHANGE', val);
    if (val === true || val === 'true') {
      initData();
      setDebugQueryString();
      handlePageDisappear();
      socketManage = await useSocketManage();
      socketManage.initWebSocket();
      emitter.emit('INDEX_REFRESH'); // 刷新页面
    }
  });

  window.WeSpaceSDK?.onClose(() => {
    if (closeFlag) return;
    clearTimerLocation();
    closeFlag = true;
  });

  window.WeSpaceSDK?.onAtCooperationUser((val) => {
    listenTask(val);
    console.log('[协同岗@我的消息]通知：上app通知栏，消息内容为：', val);
    sendNotificationToUser(val);
  });
  // 回复的所有消息，用于处理REPLY_DIRECTLY=true的消息
  window.WeSpaceSDK?.onCooperationUserSendMsg((param) => {
    console.log(JSON.stringify(param), '引用消息');
    quotationMssage(param);
  });
  window.WeSpaceSDK?.onClickNotification((param) => {
    console.log(JSON.stringify(param), '=============onClickNotification监听');
    // 使用公共方法处理通知点击（已读接口+url跳转），无url不作操作
    handleNotificationClick(param);
  });

  // 监听 iframe 的 postMessage
  window.addEventListener('message', handleMessage, false);

  // DC 事件监听
  DC.on('PASSPORT', 'kickout', () => {
    console.log('====用户权限变更，重新登录！');
    kickoutMethod();
  });

  DC.on('CAROUSEL_UPDATE', 'UPDATE', (val) => {
    pushMessage(val, '轮播图更新');
    // 轮播图更新上App通知栏
  });

  DC.on('CAROUSEL_UPDATE', 'CREATE', (val) => {
    pushMessage(val, '轮播图新增');
    // 轮播图新增上App通知栏
  });

  DC.on('CAROUSEL_UPDATE', 'DEL', (val) => {
    pushMessage(val, '轮播图删除');
  });

  DC.on('TASKS_UPDATE', 'CREATE', (val) => {
    // 任务新增上App通知栏
    taskGlassProcess(val, '新增');
  });

  DC.on('TASKS_UPDATE', 'STATUS_CHANGE', (val) => {
    // 任务更新上App通知栏
    taskGlassProcess(val, '更新');
  });

  DC.on('AT_GROUP_AI', 'RESPONSE', (val) => {
      if (val) {
        console.info('群AI助手响应数据', val);
        const htmlContent = marked.parse(val);
         console.info('处理后的html', val);
        const plainText = stripHtml(htmlContent);
        console.info('去除html标签的数据', plainText);

        console.log('[群AI助手响应数据]通知：上app通知栏，消息内容为：', plainText);
        sendNotification({ text: plainText });
      } else {
        console.log('群AI助手响应数据获取失败', val);
      }
  });

  //保存推送消息通知 TODO暂时不知三方推送的字段
  window.WeSpaceSDK?.onRemotePushMessage((param) => {
    console.log(JSON.stringify(param), '收到-消息通知，入库');
    let reqJson = {
      ...param,
      text: param.text,
      contactId: param.contactId,
      data: param.data,
      receivedAt: param.receivedAt,
      url: param.url,
    }
    if (String(param?.showInNotification) === '1') {
      communicationStore.showNotification(reqJson);
    }
    createNotifyMessage(reqJson);
  });

  window.addEventListener('pagehide', handlePageDisappear);
  window.addEventListener('beforeunload', handlePageDisappear);
  document.addEventListener('visibilitychange', handlePageDisappear);
});

onBeforeUnmount(() => {
  handlePageDisappear();
  communicationStore.removeStorageChange('currentAppSet');
  window.removeEventListener('message', handleMessage, false);
});

onUnmounted(() => {
  handlePageDisappear();
  window.removeEventListener('pagehide', handlePageDisappear);
  window.removeEventListener('beforeunload', handlePageDisappear);
  document.removeEventListener('visibilitychange', handlePageDisappear);
});
</script>

<style lang="scss" scoped>
.container {
  background-color: #f7f7f7;
  box-sizing: border-box;
  width: 100%;
  min-height: 100vh;
}

.license-limit {
  width: 100%;
  height: 200px;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 16px;
  font-weight: 500;
  color: #333333;
}

.msg-notify{
  padding-top: 4px;
  padding-right: 10px;
  cursor: pointer;
}

.ai-img {
  width: 24px;
  height: 24px;
  cursor: pointer;
}
</style>
