<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, provide, ref, watch, defineAsyncComponent } from 'vue';
import { useRoute } from 'vue-router';
import { debounce } from 'lodash-es';

import { getAppPage } from '@/api/app';
import { getGlobalsList } from '@/api/dictionary';
import { tasksStatistics } from '@/api/pim';
import {
  getCurrentOrganization,
  getLicenseInfo,
  getOverdueReplyList,
  getSystemConfig,
  listZeroOnDutyPosts,
  queryDepartment,
  queryDepartmentTree,
  getTaskByMsgId,
  getUserInfo,
  responsesTask,
  responsesTaskAll,
  getCollocation,
  getVersion,
  getNotifyList,
} from '@/api/statics';
import {getCallebleApps, getTaskStandardModules} from '@/api/taskManage'
import { getLastNum, switchStatus } from '@/api/xietong';
import { checkConfigSwitch } from '@/utils';
import { useWebView2DatePicker } from '@/composables/useWebView2DatePicker';
import ai from '@/assets/images/ai/ai.png';
import aiDark from '@/assets/images/ai/ai_dark.png';
import aiSelectedDark from '@/assets/images/ai/ai_dark_selected.png';
import aiSelected from '@/assets/images/ai/ai_selected.png';
import app from '@/assets/images/ai/app_icon.png';
import appDark from '@/assets/images/ai/app_dark_icon.png';
import appSelectedDark from '@/assets/images/ai/app_dark_selected_icon.png';
import appSelected from '@/assets/images/ai/app_selected_icon.png';
import collection from '@/assets/images/ai/collection.png';
import collectionDark from '@/assets/images/ai/collection_dark.png';
import collectionHover from '@/assets/images/ai/collection_hover.png';
import create from '@/assets/images/ai/create.png';
import createDark from '@/assets/images/ai/create_dark.png';
import createHover from '@/assets/images/ai/create_hover.png';
import dispatch from '@/assets/images/ai/dispatch.svg';
import dispatchDark from '@/assets/images/ai/dispatch_dark.svg';
import dispatchHover from '@/assets/images/ai/dispatch_hover.svg';
import coop from '@/assets/images/ai/coop.png';
import coopDark from '@/assets/images/ai/coop_dark.png';
import coopHover from '@/assets/images/ai/coop_hover.png';
import customize from '@/assets/images/ai/customize.png';
import customizeDark from '@/assets/images/ai/customize_dark.png';
import customizeHover from '@/assets/images/ai/customize_hover.png';
import cooperation from '@/assets/images/ai/xt.png';
import cooperationDark from '@/assets/images/ai/xt_dark.png';
import cooperationSelectedDark from '@/assets/images/ai/xt_dark_selected.png';
import cooperationSelected from '@/assets/images/ai/xt_selected.png';
import {
  closeChatUI,
  createGroup,
  onSelectedMembers,
  openChat,
  openSelectMemberUI,
  setBadge,
  downloadFile,
} from '@/bridge/post.js';
import { Dialog } from '@/components/Dialog';
import MessageBox from '@/components/MessageBox';
import { appConfig } from '@/config';
import { useDC, useEmitter, useSetInterval } from '@/hooks';
// 动态导入 AI 模块组件

import ProblemSolve from '@/pages/coordination/collaboration/problemSolve.vue';
import ArchivedTable from '@/pages/statics/components/ArchivedTable.vue';
import { usePIMStore, useStaticsState } from '@/store'; // useRouterStore
import { getRecentSomeDays, getIp } from '@/utils/index';
// import ThemeSwitcher from '@/components/ThemeSwitcher/index.vue';
import ArchiveTable from '@/pages/statics/components/ArchiveTable.vue';
import { getToken } from '@/utils/auth';
import { checkIsSessionMode } from '@/utils';

import { ElMessage } from 'element-plus';

import { themeService } from '../../data/useTheme';
import ArchiveManagement from './components/ArchiveManagement.vue';
import ChartArea from './components/ChartArea.vue';
import SecondCards from './components/SecondCards.vue';
import SelectTree from './components/selectTree.vue';
import SelectTreeLazy from './components/selectTreeLazy.vue';
import NodeSelect from './components/NodeSelect.vue';
import StatCards from './components/StatCards.vue';
import TableArea from './components/TableArea.vue';
import CreateGroup from './createGroup.vue';
import CreateGroupByCoop from './components/CreateGroupByCoop/index.vue';
import CreateGroupByDispatch from './components/CreateGroupByDispatch/index.vue';
// import createGroupByCostom from './createGroupByCostom.vue'
import createGroupByCostom from './components/CreateGroupByCustom/index.vue';
import DutyInformation from '@/pages/dutyInformation/index.vue';
import DynamicTaskManage from './components/DynamicTaskManage/index.vue'
import ScrollableTabs from '@/components/ScrollableTabs/index.vue';

import { isWebView2 } from '@/utils/env';
import { openUrl } from '@/bridge/post';
// import { useSocketManage } from '@/hooks/useSocket';
// import DC from '@/utils/DC';
import TaskStandardParts from './components/TaskStandardParts/index.vue'

interface OrgData {
  userDepartments?: any[];
}
// const routerStore = useRouterStore();
const PIMStore = usePIMStore();
const StaticsStore = useStaticsState();
const bigActiveName = ref('');
const switchContainer = ref(false);
const showXieTong = ref(false);
const currentOrganization = ref('');
const currentOrganizationLabel = ref('');
const timeRange = ref<string[]>([]);
const tabId = ref<number>(1);
const deparmentArr = ref<any>([]);
const showTabs = ref(true);
const switchLoading = ref(false); // 添加开关加载状态
const exportLoading = ref(false); // 协同监测统计导出加载状态
const isSwitchCooldown = ref(false); // 开关点击后的冷却状态
let switchCooldownTimer: any = null; // 冷却计时器
let clearTimer: any = null; // 定时器
const isSelectTreeDefaultGetComplate = ref(false); // tree组件数据是否获取成功
const isShowOldTree = ref(false);
const showArchiveDialog = ref(false);
const showNotifyDialog = ref(false);
const badgeNumber = ref(0);
const badgeNumberComputed = computed(() => {
  const storeBadgeNumber = StaticsStore.zeroUserOnlineTotal + StaticsStore.answerTotal;
  return storeBadgeNumber || badgeNumber.value;
});

// 根据环境动态加载组件
const env = ref('')

const moduleMap = {
  default: defineAsyncComponent(() => import('@/pages/aiModule/index.vue')),
  xiongan: defineAsyncComponent(() => import('@/pages/aiModule_XiongAn/index.vue')),
}

const AsyncAiModule = computed(() => {
  return moduleMap[env.value] ?? moduleMap.default
})


const cycle = ref('');
// 自定义页签是否追加 URL 参数开关
const customTabAppendSwitch = ref(true);
// PC端自定义页签数据
const CustomNavTabs = ref<any[]>([]);
// iframe 刷新 key
const iframeKey = ref(0);
// 用于 iframe URL 的组织ID（跟随 store.departmentCode 变化）
const currentOrganizationForIframe = ref('');
// ScrollableTabs 实例引用
const scrollableTabsRef = ref();
const refreshTagStatistics = ref(0);
// const socketManage = useSocketManage();
// const originalOnMessage = DC.onmessage;
const processedArchiveMessages = ref(new Set());
const tabImgs = {
  ai,
  aiDark,
  aiSelected,
  aiSelectedDark,
  cooperation,
  cooperationDark,
  cooperationSelected,
  cooperationSelectedDark,
  app:app,
  appDark:appDark,
  appSelected:appSelected,
  appSelectedDark:appSelectedDark,
};
// 任务页签数据
const TaskNavTabs = ref<any[]>([]);
// 合并自定义页签和任务页签
const DynamicNavTabs = computed(() => [...TaskNavTabs.value, ...CustomNavTabs.value])

// 任务标准件模块 tab 数据源（从接口动态拉取）
const taskModuleTabs = ref<any[]>([]);
const loadTaskModuleTabs = async () => {
  try {
    const res: any = await getTaskStandardModules();
    if (res.code === 0) {
      // 接口字段：id / systemCode / module / showInPc / gmtLastModified / gmtCreated
      // tab 显示名暂用 module 字段（接口文档未提供独立显示名，后续后端若返回中文名直接生效）
      taskModuleTabs.value = (res.data || []).map((m: any) => ({
        id: m.module,
        name: m.module,
        module: m.module,
        navSubType: 'taskStandardParts',
      }));
    }
  } catch (e) {
    console.error('[loadTaskModuleTabs] failed:', e);
  }
};
interface LicenseInfo {
  expireDate?: string;
  HADR?: string;
  LINKXACF?: string;
  LINKXBCF?: string;
  LINKXBS?: string;
  LINKXCCF?: string;
  LINKXGCF?: string;
  LINKXNDI?: string;
  LINKXNum?: string;
  LINKXSDF?: string;
  LINKXTCF?: string;
  status?: string;
}
const licenseInfo = ref<LicenseInfo>({ LINKXBCF: '1', LINKXBS: '1' });
const isShowCustomCreateGroupConfig = ref(true);

// 建群按钮配置
const groupButtonConfig = ref({
  notifyMessage: {title: '推送消息', show: true},   // 默认显示
  customGroup: { title: '自定义建群', show: true },   // 默认显示
  quickGroup: { title: '一键建群', show: true },       // 默认显示
  functionGroup: { title: '职能建群', show: false },   // 默认不显示
  quickDispatch: { title: '一键调度', show: false }  // 预留
});

// 默认配置（用于无配置时）
const defaultGroupButtonConfig = [
  {type: '0', name: '推送消息', enable: 'true'},
  { type: '1', name: '自定义建群', enable: 'true' },
  { type: '2', name: '一键建群', enable: 'true' },
  { type: '3', name: '职能建群', enable: 'false' },
  { type: '4', name: '一键调度', enable: 'false' }
];

// 待办问题数量
const problemNumber = ref(0);
// 警信版本号
const appVersion = ref('');
// 是否显示节点选择器：仅在存在多节点时显示
// TODO 设置为false 主线版本暂不需要 功能暂不开启 开启时间待定
const showNodeSelect = ref(false);
const tdTabsBase = computed(() => [
  {
    id: 'third',
    name: '协同处置',
  },
  {
    id: 'second',
    name: '群组总计',
  },
  {
    id: 'first',
    name: '协同监测统计',
  },
]);
const activeTab = ref('cooperation');
const activeIndex = ref(0); // 当前点击标签的索引
// const titles = ref(['未归档', '已归档']);
const userInfo = computed(() => {
  return PIMStore.user;
});

const imOrgPrivsArr = ref([]);
const hasXtqzAuth = ref(false); // 协同群组权限标识
const hasQzgdAuth = ref(false); // 群组归档权限标识
const hasXttjAuth = ref(false); // 协同统计权限标识
const hasXtjctjjAuth = ref(false); // 协同检测统计权限标识
const hasAI = ref(false); // AI权限标识
const isRoleAuthLoaded = ref(false); // roleAuth 数据是否已加载完成
const currentTheme = ref('light');
const isSessionMode = ref(false); // 会话模式标识
const isCreateHover = ref(false); // 一键建群 hover 状态
const isNotifyHover = ref(false); // 推送消息 hover 状态
const isCustomizeHover = ref(false); // 自定义建群 hover 状态
const isCoopHover = ref(false); // 职能建群 hover 状态
const isDispatchHover = ref(false); // 一键调度 hover 状态
const isCollectionHover = ref(false); // 我的收藏 hover 状态
const SHOW_331_FEATURE = ref(false);
const ONE_KEY_CREATE_GROUP_SIGN = ref(false); //是否展示自定义建群入口
const isDispatchUserAllowed = ref(false); // 一键调度用户权限标识
const dispatchUsersConfig = ref(''); // 一键调度用户配置

// 常用应用相关
const appList = ref<any[]>([]);
const currentAppId = ref('');
// 是否显示值班信息（根据 H5 常用应用 id=25 判断）
const hasDutyInformation = ref(false);

const notifyList = ref<any>([]);
// 分页相关状态
const requestParams = ref({
  keywords: '', //搜索关键字
  page: 1,
  pageSize: 1000,
  start: '',
  end: '',
  userId: '',
});
/**
 * 获取全部应用并判断是否显示值班信息
 * 与 H5 项目逻辑一致：检查是否存在 id=25 且 status=0 的应用
 */
const fetchAppList = async () => {
  try {
    let scope = 0;
    if (!isWebView2()) {
      scope = 4;
    } else {
      scope = 8;
    }
    const res = await getAppPage({ scope: scope, pageNum: 1, pageSize: 100 });
    const records = res?.data?.records || [];
    // 过滤启用的应用（status === 0）
    const enabledApps = records.filter((item) => item.status === 0);
    appList.value = enabledApps;
    // // 判断是否存在 id=25 的应用
    // const dutyApp = enabledApps.find((item) => Number(item.id) === DUTY_INFORMATION_APP_ID);
    // hasDutyInformation.value = !!dutyApp;
    // // 如果存在值班信息应用，添加到 appList
    // if (dutyApp) {
    //   appList.value = [{ id: 'dutyInformation', name: dutyApp.name || '值班信息' }];
    // } else {
    //   appList.value = [];
    // }
    
  } catch (error) {
    console.error('获取应用列表失败:', error);
    hasDutyInformation.value = false;
    appList.value = [];
  }
};

provide('hasXtqzAuth', hasXtqzAuth);
provide('hasQzgdAuth', hasQzgdAuth);
provide('imOrgPrivJson', imOrgPrivsArr);
provide('hasAI', hasAI);
provide('licenseInfo', licenseInfo); // 提供 license 信息给子组件，避免子组件重复请求
provide('bigActiveName', bigActiveName); // 提供当前 tab 名称给子组件，用于监听 tab 切换
const getRoleAuth = async () => {
  const roleAuth = PIMStore.userRoleAuth || {};
  imOrgPrivsArr.value = roleAuth.imOrgPrivJson || [];
  const iccPrivJson = roleAuth.iccPrivJson || [];
  hasXtqzAuth.value = iccPrivJson.includes('1522392406668870001'); // 协同群组
  hasQzgdAuth.value = iccPrivJson.includes('1522392406668870002'); // 群组归档
  hasXttjAuth.value = iccPrivJson.includes('1522392406668869997'); // 协同统计
  hasXtjctjjAuth.value = iccPrivJson.includes('1522392406668870011'); // 协同检测统计
  hasAI.value = iccPrivJson.includes('1522392406668870008'); // AI权限
  // 只有当 roleAuth 有实际数据时才标记为已加载
  if (roleAuth && Object.keys(roleAuth).length > 0) {
    isRoleAuthLoaded.value = true;
  }
};
const titleFilter = computed(() => {
  let titles = ['未归档', '已归档'];
  if (!hasQzgdAuth.value) {
    titles = ['群组列表'];
  }
  return titles;
});
const reLoginIsClickCancel = ref(false); // 重新登录时是否点击了否
const systemLimit = computed(() => PIMStore.systemLimit);
const tdTabs = computed(() => {
  if (reLoginIsClickCancel.value) return []; // 如果点击了否，则把tdtabs返回空

  // 确保 licenseInfo 已加载完成，检查关键字段是否存在
  // 如果 LINKXGCF 和 LINKXSDF 还是 undefined，说明数据还没加载完成，返回空数组等待
  // 使用 hasOwnProperty 或 in 操作符检查字段是否存在，避免在值为 '0' 时误判
  const hasLinkXgcf = 'LINKXGCF' in licenseInfo.value;
  const hasLinkXsdf = 'LINKXSDF' in licenseInfo.value;

  if (!hasLinkXgcf || !hasLinkXsdf) {
    return [];
  }

  const arr: any = [...tdTabsBase.value];
  const newArr: any = [];
  if (switchContainer.value && licenseInfo.value.LINKXGCF === '1') {
    newArr.push(arr[0]);
  }
  // const index = routerStore.menuList.findIndex((item) => item.url === 'copilotStatistics');
  // if (index !== -1) {
  if (licenseInfo.value.LINKXGCF === '1') {
    newArr.push(arr[1]);
  }
  // }
  if (hasXttjAuth.value && licenseInfo.value.LINKXSDF === '1') {
    newArr.push(arr[2]);
  }
  if (!hasXtqzAuth.value) {
    // 如果没有协同群组权限
    const xtqzIndex = newArr.findIndex((item) => item.id === 'second');
    if (xtqzIndex !== -1) {
      newArr.splice(xtqzIndex, 1); // 如果没有权限，需要删除协同群组tab不展示
    }
  }
  // 添加PC端自定义页签（在协同监测统计后面）
  if (DynamicNavTabs.value.length > 0 && hasXtjctjjAuth.value) {
    // 插入到协同监测统计后面
    const firstIndex = newArr.findIndex((item) => item.id === 'first');
    if (firstIndex !== -1) {
      newArr.splice(firstIndex + 1, 0, ...DynamicNavTabs.value);
    } else {
      newArr.push(...DynamicNavTabs.value);
    }
  }

  newArr.push(...taskModuleTabs.value);

  return newArr;
});

// 获取当前选中的 tab
const currentTab = computed(() => {
  return tdTabs.value.find((t) => t.id === bigActiveName.value);
});
const totalTabs = computed(() => {
  let arr: any = [];
  if (hasXtqzAuth.value || hasXttjAuth.value || switchContainer.value || hasXtjctjjAuth.value) {
    arr.unshift({
      id: 'cooperation',
      name: '协同警务',
    });
  }
  if (licenseInfo.value.LINKXACF === '1' && hasAI.value) {
    arr.push({
      id: 'ai',
      name: 'AI助手',
    });
  }
  // 只有在 roleAuth 数据加载完成后才判断是否显示 AI 助手和常用应用
  if (isRoleAuthLoaded.value) {
    arr.push({
      id: 'app',
      name: '常用应用',
    });
  }
  if (env.value === 'xiongan') {
    arr = arr.filter((item) => item.id !== 'app');
  } 
  return arr;
});
const defaultActiveTab = computed(() => {
  const tabs = tdTabs.value;
  if (tabs.length === 0) return '';

  // 优先级：协同处置 > 协同群组 > 协同监测统计
  // 按照在数组中的顺序查找
  return tabs[0].id;
});
const xietongIds = computed(() => {
  const { cooperationUser, cooperationUsers } = PIMStore.user || {};
  let postIds = '';
  const isMultipleCollaboration =
    appConfig.settingData?.MULTIPLE_COLLABORATION === 'true' ||
    appConfig.settingData?.MULTIPLE_COLLABORATION === true;

  if (isMultipleCollaboration) {
    cooperationUsers?.forEach((item, index) => {
      if (index !== 0) {
        postIds += ',';
      }
      postIds += item.userId;
    });
  } else {
    postIds = cooperationUser?.userId || '';
  }
  return postIds;
});
// 有效license。LINKXBS=1、其余全部至少一个不为0
const effectLicense = computed(() => {
  const obj = licenseInfo.value || {};
  if (obj.LINKXBS !== '1') return false;
  // 检查过期状态
  if (obj.status === '0' || obj.status === '3' || obj.status === '5') {
    return false; // license已过期或者未激活
  }

  let flag = false;
  for (const i in obj) {
    if (i !== 'status' && i !== 'LINKXBS' && obj[i] === '1') {
      flag = true;
    }
  }
  return flag;
});

const route = useRoute();

const shortcuts = [
  {
    text: '当日',
    value: () => {
      const today = new Date();
      return [today, today];
    },
  },
  {
    text: '最近一周',
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setDate(end.getDate() - 6);
      return [start, end];
    },
  },
  {
    text: '最近一年',
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setFullYear(end.getFullYear() - 1);
      return [start, end];
    },
  },
];

// 获取待办问题数量
async function getStatistics() {
  // 直接获取最新数据，不依赖 computed（因为 appConfig.settingData 不是响应式的）
  const { cooperationUser, cooperationUsers } = PIMStore.user || {};
  const isMultipleCollaboration =
    appConfig.settingData?.MULTIPLE_COLLABORATION === 'true' ||
    appConfig.settingData?.MULTIPLE_COLLABORATION === true;

  let postIds = '';
  if (isMultipleCollaboration) {
    cooperationUsers?.forEach((item, index) => {
      if (index !== 0) {
        postIds += ',';
      }
      postIds += item.userId;
    });
  } else {
    postIds = cooperationUser?.userId || '';
  }

  // 如果 postIds 为空，不发送请求，直接返回
  if (!postIds) {
    problemNumber.value = 0;
    return;
  }
  const { code, data } = await tasksStatistics({
    postIds,
    userId: null,
  });

  if (code === 0) {
    problemNumber.value = Number(data[1]) + Number(data[2]);
  }
}
// 防抖版本的 getStatistics（300ms 防抖，避免多次快速调用）
const debouncedGetStatistics = debounce(getStatistics, 300);

// 监听协同岗 ID 变化（不使用 immediate，避免数据不完整时调用）
watch(
  () => xietongIds.value,
  (newPostIds, oldPostIds) => {
    // 仅在 postIds 有值且发生变化时触发
    if (newPostIds && newPostIds !== oldPostIds) {
      debouncedGetStatistics();
    }
  },
);
watch(
  () => PIMStore.user?.idCard,
  (newIdCard) => {
    if (newIdCard) {
      getCurrentOrganizationFunc();
    }
  },
  { immediate: true },
);
watch(
  () => PIMStore.user?.userid,
  (newId) => {
    if (newId) {
      // 重新判断一键调度权限
      if (dispatchUsersConfig.value) {
        isDispatchUserAllowed.value = dispatchUsersConfig.value.split(';').includes(newId);
      } else {
        // 配置为空时，不允许任何人使用
        isDispatchUserAllowed.value = false;
      }
    }
  },
  { immediate: true },
);
// 监听用户协同岗信息加载完成，触发待办统计刷新
watch(
  () => PIMStore.user?.cooperationUsers,
  (cooperationUsers) => {
    // 当 cooperationUsers 有数据时触发刷新
    if (cooperationUsers && cooperationUsers.length > 0) {
      debouncedGetStatistics();
    }
  },
  { immediate: true },
);
// 监听单协同岗模式下的 cooperationUser 变化
watch(
  () => PIMStore.user?.cooperationUser,
  (cooperationUser) => {
    // 当 cooperationUser 有数据时触发刷新
    if (cooperationUser?.userId) {
      debouncedGetStatistics();
    }
  },
  { immediate: true },
);
// 监听多协同岗配置变化，配置加载完成后触发刷新
watch(
  () => appConfig.settingData?.MULTIPLE_COLLABORATION,
  (newVal, oldVal) => {
    // 当配置从 undefined 变为有值时，触发刷新
    if (oldVal === undefined && newVal !== undefined && PIMStore.user?.cooperationUser) {
      debouncedGetStatistics();
    }
  },
);
watch(
  [() => StaticsStore.departmentCode, () => StaticsStore.dateRang],
  ([code]) => {
    getBadgeNumber(code);
  },
  { immediate: true },
);
// 监听 store 中的组织变化，同步更新 iframe 使用的组织ID
watch(
  () => StaticsStore.departmentCode,
  (code) => {
    currentOrganizationForIframe.value = code;
  },
  { immediate: true },
);
watch(
  () => tdTabs.value,
  (arr: any) => {
    showTabs.value = false;
    if (arr.length === 0) {
      return;
    }
    setTimeout(() => {
      showTabs.value = true;
    }, 0);
  },
);
watch(
  () => totalTabs.value,
  (arr: any) => {
    if (arr.length === 0) return;
    activeTab.value = arr[0].id;
  },
);
// 监听 params 变化
watch(
  [() => route, () => tdTabs.value],
  ([route, tabs]: any) => {
    if (route.name !== 'statics') return;

    const newParams: any = route.params;
    if (newParams && newParams.bigActiveName) {
      tabId.value = Number(newParams.tabId) || 1;
      bigActiveName.value = newParams.bigActiveName || 'third';
      return;
    }

    if (tabs.length === 0) {
      bigActiveName.value = '';
      return;
    }

    // 根据 tdTabs 的内容设置默认的 active tab
    bigActiveName.value = defaultActiveTab.value;

    // 当切换到协同监测统计时，确保重新获取组织信息
    updateCurrentDepart();
  },
  { deep: true, immediate: true },
);

watch(
  () => PIMStore.collaboration,
  (switchStatus: any) => {
    switchContainer.value = switchStatus;
  },
  { immediate: true },
);
watch(
  () => PIMStore.userRoleAuth,
  (newRoleAuth) => {
    if (newRoleAuth) {
      getRoleAuth();
    }
  },
  { deep: true, immediate: true },
);

watch(
  () => PIMStore.bondedStatus,
  (bondedStatus) => {
    showXieTong.value = bondedStatus;
  },
  { immediate: true },
);
watch(
  () => timeRange.value,
  (data: any) => {
    let date = { endTime: '', startTime: '' };
    if (!data || data?.length === 0) {
      StaticsStore.setDateRang(date);
    }
    date = { endTime: data[1], startTime: data[0] };
    StaticsStore.setDateRang(date);
  },
);
// watch(
//   () => appConfig.settingData.cycle,
//   (cycle: any) => {
//     setTimeout(() => {
//       timeRange.value = getRecentSomeDays(Number(cycle) || 7);
//     }, 500);
//   },
//   { immediate: true, deep: true },
// );
watch(
  () => cycle.value,
  (item: any) => {
    timeRange.value = getRecentSomeDays(Number(item) || 7);
  },
  { deep: true, immediate: true },
);
watch(
  () => [badgeNumberComputed.value, problemNumber.value, PIMStore.collaboration],
  () => {
    console.log('===触发协同页签刷新===');
    // 同源时可直接访问 parent
    // window.parent?.LinkXEvents?.pushData(user);
    let count = Number(badgeNumberComputed.value);
    if (PIMStore.collaboration) {
      count += Number(problemNumber.value);
    }
    // cspc 环境下调用 SDK 设置角标
    setBadge({ count, appId: 'ITEM_POLICE_COOPERATION' });
    window.parent.postMessage(
      { type: 'LINKX_PUSH_DATA', payload: { count } },
      '*', // 生产环境请改成父页面的明确 origin
    );
  },
);
// 首次数据就绪时触发刷新（修复首次进入页面数据为空的问题）
watch(
  [() => currentOrganization.value, () => timeRange.value, () => bigActiveName.value],
  ([org, time, tab], [oldOrg, oldTime]) => {
    // 仅在首次数据就绪时触发（oldOrg 和 oldTime 为空或未定义）
    const isFirstTimeReady = (!oldOrg || !oldTime?.length) && org && time?.length === 2;
    if (tab === 'first' && isFirstTimeReady) {
      nextTick(() => {
        handleRefresh();
      });
    }
  },
);
// 监听组织、时间、主题变化，刷新自定义页签 iframe
watch(
  [() => currentOrganizationForIframe.value, () => timeRange.value, () => currentTheme.value, () => bigActiveName.value],
  () => {
    // 当前选中的是自定义页签时，刷新 iframe
    if (currentTab.value?.isCustomNav) {
      iframeKey.value++;
    }
  },
);
// 获取appConfig信息
async function getAppConfig() {
  const { code, data } = await getGlobalsList();
  if (code === 0) {
    appConfig.settingData = data;
    cycle.value = data.cycle;
    isShowCustomCreateGroupConfig.value = data?.ONE_KEY_CREATE_GROUP_SIGN === 'true';
    // 判断是否为会话模式
    isSessionMode.value = checkIsSessionMode(data);
    // 获取自定义页签 URL 参数追加开关
    customTabAppendSwitch.value = checkConfigSwitch({
      configData: data,
      configKey: 'CUSTOM_TAB_APPEND_SWITCH',
      defaultValue: true,
    });
    judgeLicenseExpire();

    // 配置加载完成后，触发待办统计刷新（因为 xietongIds 依赖此配置）
    debouncedGetStatistics();
  }
}
// 获取任务页签数据
async function fetchTaskNavConfig() {
  // 查询所有南向应用遍历获取
  const res = await getCallebleApps({page: 1, pageSize: -1});
  console.log(res.data.records, 'res.data.records')
  const list = res?.data?.records || []
  // 根据scope字段展示: scope: 0-all, 1-pc, 2-mobile
  list.filter(item => item.scope === 0 || item.scope === 1)
  TaskNavTabs.value = list.map(item => {
    return {
      ...item,
      id: item.id,
      _name: item.name,
      name: item.systemName,
      navType: 'dynamic',
      navSubType: 'task',
    }
  })
}

// 获取PC端自定义页签配置
async function getPcNavConfig() {
  try {
    const res = await getSystemConfig();
    if (res.code === 0 && res.data) {
      const item = res.data.find((d: any) => d.key === 'PC_NAV_CUSTOM');
      if (item && item.value) {
        const parsed = JSON.parse(item.value) || [];
        const mapParsed = parsed.map((item, index) => ({
          id: `pc_nav_${index}`,
          name: item.name,
          url: item.url,
          order: item.order,
          openWay: item.openWay ?? 0,
          navType: 'dynamic',
          navSubType: 'custom',
          isCustomNav: true,
        }));
        CustomNavTabs.value = mapParsed.sort((a: any, b: any) => a.order - b.order);
      }
    }
  } catch (error) {
    console.error('获取PC端自定义页签配置失败:', error);
  }
}

// 构建iframe URL（携带参数）
function buildIframeUrl(baseUrl: string) {
  // 如果开关为 false，直接返回原始 URL，不添加参数
  if (!customTabAppendSwitch.value) {
    return baseUrl;
  }
  const theme = currentTheme.value;
  const orgId = currentOrganizationForIframe.value;
  const startTime = timeRange.value?.[0] || '';
  const endTime = timeRange.value?.[1] || '';
  const separator = baseUrl.includes('?') ? '&' : '?';
  return `${baseUrl}${separator}theme=${theme}&orgId=${orgId}&startTime=${startTime}&endTime=${endTime}`;
}

async function init() {
  await getAppConfig();
  getPcNavConfig();
  // 531-广铁特性
  fetchTaskNavConfig();
  // 初始时没有注册事件，登录时无法触发mitt，保持原样
  setTimeout(async () => {
    getCurrentOrganizationFunc();
  }, 500);
  // 确保在组件挂载后检查组织信息是否已获取
  setTimeout(() => {
    updateCurrentDepart();
  }, 100);
}

function useDebounceImmediate<T extends (...args: any[]) => any>(
  fn: T,
  delay: number,
): (...args: Parameters<T>) => void {
  let timeoutId: null | ReturnType<typeof setTimeout> = null;

  return function (this: any, ...args: Parameters<T>): void {
    if (timeoutId === null) {
      fn(...args);
      timeoutId = setTimeout(() => {
        timeoutId = null;
      }, delay);
    }
  };
}
const debounceInit = useDebounceImmediate(init, 1000);
const getLicense = async () => {
  try {
    const res = await getLicenseInfo();
    if (res.code === 0) {
      licenseInfo.value = res.data as LicenseInfo;
      // let title = '';
      switch (licenseInfo.value.status) {
        case '0': {
          // title = 'license未激活';
          // ElMessage.error(title);

          break;
        }
        case '2': {
          judgeLicenseExpire();

          break;
        }
        case '3':
        case '5': {
          // title = 'license已过期，请重新导入';
          // ElMessage.error(title);

          break;
        }
        // No default
      }
    }
  } catch (error) {
    console.error('getLicenseInfo error:', error);
  }
};
function judgeLicenseExpire() {
  // 暂时不需要提示，过期直接系统受限就行
  return
  // if (licenseInfo.value.status !== '2') return;
  // const expireDate: any = licenseInfo.value.expireDate;
  // const now = new Date();
  // const expireDateObj = new Date(expireDate);
  // const diffTime = expireDateObj.getTime() - now.getTime();
  // const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  // const MSIP_LICENSE_EXPIRED_TIME = appConfig.settingData.MSIP_LICENSE_EXPIRED_TIME;
  // if (!MSIP_LICENSE_EXPIRED_TIME) return;
  // if (diffDays >= Number(MSIP_LICENSE_EXPIRED_TIME)) return;
  // const title = `您的账户还有${diffDays}天到期，请及时联系相关人员进行续费，以免影响您的业务`;
  // ElMessage.warning(title);
}
// 获取建群按钮配置
const fetchGroupButtonConfig = async () => {
  try {
    const res = await getSystemConfig();
    const configList = Array.isArray(res?.data) ? res.data : [];
    const item = configList.find((c: any) => c.key === 'CREAT_GROUP_CONFIG');

    const configs = item ? JSON.parse(item.value) : defaultGroupButtonConfig;

    // 自定义建群
    const customConfig = configs.find((c: any) => c.type === '1');
    if (customConfig) {
      groupButtonConfig.value.customGroup = {
        title: customConfig.name || '自定义建群',
        show: customConfig.enable === 'true'
      };
    }

    // 一键建群
    const quickConfig = configs.find((c: any) => c.type === '2');
    if (quickConfig) {
      groupButtonConfig.value.quickGroup = {
        title: quickConfig.name || '一键建群',
        show: quickConfig.enable === 'true'
      };
    }

    // 职能建群
    const functionConfig = configs.find((c: any) => c.type === '3');
    if (functionConfig) {
      groupButtonConfig.value.functionGroup = {
        title: functionConfig.name || '职能建群',
        show: functionConfig.enable === 'true'
      };
    }

    // 一键调度（预留）

    const dispatchConfig = configs.find((c: any) => c.type === '4');
    if (dispatchConfig) {
      groupButtonConfig.value.quickDispatch = {
        title: dispatchConfig.name || '一键调度',
        show: dispatchConfig.enable === 'true'
      };
    }

  } catch (error) {
    console.error('获取建群按钮配置失败:', error);
  }
};

onMounted(async () => {
  await getLicense();
  await fetchGroupButtonConfig(); // 获取建群按钮配置
  await fetchAppList(); // 获取应用列表并判断是否显示值班信息
  getTheme();
  debounceInit();
  loadTaskModuleTabs(); // 拉取任务标准件模块列表，用于动态生成顶部 tab
  // 移除直接调用 getStatistics()，由 watch 监听数据变化触发
  setTimeout(() => {
    refreshAllBadges();
  }, 1000);

  // 获取版本信息
  try {
    const { data: versionData } = await getVersion();
    console.error('获取版本信息数据:', versionData, '警信版本:', versionData?.linkx?.serviceVersion || '-');
    if (versionData?.linkx?.serviceVersion) {
      appVersion.value = versionData.linkx.serviceVersion;
    }
  } catch (error) {
    console.error('获取版本信息失败:', error);
  }

  useEmitter('REFRESH_INDEX_PROBLEMNUMBER', debouncedGetStatistics);
  const { data, code} = await getGlobalsList();
  if (code === 0) {
    SHOW_331_FEATURE.value = data?.SHOW_331_FEATURE === 'true' || data?.SHOW_331_FEATURE === true;
    ONE_KEY_CREATE_GROUP_SIGN.value = data?.ONE_KEY_CREATE_GROUP_SIGN === 'true' || data?.ONE_KEY_CREATE_GROUP_SIGN === true;
    // 一键调度用户权限判断
    const dispatchUsers = data?.GROUP_CREATE_ONE_KEY_DISPATCH_USERS || '';
    const currentUserId = PIMStore.user?.userid || '';
    // 配置为空时，不允许任何人使用
    isDispatchUserAllowed.value = dispatchUsers ? dispatchUsers.split(';').includes(currentUserId) : false;
    // 存储配置，供 watch 使用
    dispatchUsersConfig.value = dispatchUsers;
  }

  // 回复的所有消息，用于处理REPLY_DIRECTLY=true的消息
  window.WeSpaceSDK?.onCooperationUserSendMsg((param) => {
    console.log(JSON.stringify(param), '引用消息');
    quotationMssage(param);
  });

  // WebView2环境下加入群聊成功后的回调监听
  window.WeSpaceSDK?.onJoinGroup((data) => {
    const pendingGroupId = window.__pendingJoinGroupId;
    console.log('onJoinGroup 回调:', data, 'pendingGroupId', pendingGroupId);
    if (pendingGroupId && data?.groupId === pendingGroupId) {
      // 收到回调，清除兜底定时器，避免重复跳转
      if (window.__pendingJoinGroupTimer) {
        clearTimeout(window.__pendingJoinGroupTimer);
        window.__pendingJoinGroupTimer = null;
      }
      openChat({ groupId: pendingGroupId });
      window.__pendingJoinGroupId = '';
    }
  });
});
onBeforeUnmount(() => {
  closeChatUI();
  if (clearTimer) {
    clearTimer();
    clearTimer = null;
  }
  if (switchCooldownTimer) {
    clearTimeout(switchCooldownTimer);
    switchCooldownTimer = null;
  }
});
useDC('UPDATE_TASK', 'task', () => {
  debouncedGetStatistics();
  getBadgeNumber(currentOrganization.value);
});
useDC('CLOUDCMD_IM_JINGXIN', 'switch_status', () => {
  debouncedGetStatistics();
  getBadgeNumber(currentOrganization.value);
});
useDC('GROUP_ARCHIVING', 'GROUP_ARCHIVING', (data) => {
  if (!data?.groupId) return;

  const groupId = data?.groupId;

  // 检查该消息是否已经处理过
  if (processedArchiveMessages.value.has(groupId)) {
    return;
  }

  // 标记为已处理
  processedArchiveMessages.value.add(groupId);

  // 通过事件总线通知所有相关组件，携带归档是否成功的状态
  let isArchiveSuccess = false;
  if (data?.token) {
    if (data?.token === getToken()) {
      // isArchive 0 表示归档失败，1 表示归档成功， 无值表示归档成功
      if(data?.isArchive !== '0') {
        isArchiveSuccess = true;
        ElMessage({ message: `群组归档完成`, type: 'success' });
      } else {
        ElMessage({ message: `群组归档失败`, type: 'error', });
      }
    }
  } else {
    isArchiveSuccess = true;
    ElMessage({
      message: `群组归档完成`,
      type: 'success',
    });
  }
  useEmitter().emit('GROUP_ARCHIVING_COMPLETE', { groupId, isArchiveSuccess });

  // 清理标记（5秒后清理，避免内存泄漏）
  setTimeout(() => {
    processedArchiveMessages.value.delete(groupId);
  }, 5000);
});
useEmitter('GROUP_ARCHIVING_COMPLETE', () => {
  // 刷新所有相关数据
  handleRefresh();
  handleRefreshTagStatistics();

  // 如果弹窗打开，也需要刷新
  if (showArchiveDialog.value) {
    // 这里可以触发弹窗内组件的刷新
    useEmitter().emit('REFRESH_ARCHIVE_DIALOG');
  }
});
// 引用消息处理
const quotationMssage = async (val) => {
  const deptInfo = await window.WeSpaceSDK?.getUserInfoByUserId({
    userId: val.sender,
    forceRemote: false,
  });
  console.info(deptInfo, 'deptInfo');
  const deptments = deptInfo?.userDepartments?.filter((item) => item.isPrimary) || [];
  if (deptments.length === 0) {
    console.log('回复操作人没有部门，无法创建任务');
    return;
  }
  const deptment = deptments[0];
  console.info(deptment, 'deptment');
  const res:any = await getGlobalsList();
  const replyDirectlyFlag = res.data && String(res.data?.REPLY_DIRECTLY) === 'true';
  console.info(replyDirectlyFlag, 'REPLY_DIRECTLY');

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
  const {code, data} = await getTaskByMsgId({
    icsMsgId: val.refmsgid,
    postId: val.receiver,
  });
  if(code !== 0)return
  const taskInfo:any = data
  console.info(taskInfo, 'taskInfo');
  if (!taskInfo?.taskId) {
    console.log('该引用消息没有对应任务，不予回复操作');
    return;
  }

  const userInfosData = await getUserInfo({ userIds: [val.receiver] });
  if(userInfosData.code !== 0) return
  const userInfos = userInfosData.data;
  console.info(userInfos, 'userInfos');
  let collorationInfo:any = {};
  if (userInfos && Array.isArray(userInfos)) {
    collorationInfo = userInfos[0] || {};
  }
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
  console.info(deptInfo, 'deptInfo');
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
  const userInfosData = await getCollocation({ userIds: [val.receiver] });
  if(userInfosData.code !== 0) return
  const userInfos = userInfosData.data;
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
function getTheme() {
  currentTheme.value = themeService.getCurrentTheme();
  themeService.onThemeChange((theme) => {
    currentTheme.value = theme;
  });
}
// 获取0人员在线协同岗个数和逾期数量
async function getBadgeNumber(code) {
  if (!code || !timeRange.value || timeRange.value.length !== 2) {
    return;
  }
  const params = {
    peerId: StaticsStore.peerId,
    departmentCode: code,
    endTime: timeRange.value[1],
    pageNum: 1,
    pageSize: 10,
    startTime: timeRange.value[0],
  };
  const params1 = {
    peerId: StaticsStore.peerId,
    departmentCode: code,
    endTime: timeRange.value[1],
    keywords: '',
    pageNum: 1,
    pageSize: 10,
    startTime: timeRange.value[0],
  };
  const res = await listZeroOnDutyPosts(params);
  const questionRes = await getOverdueReplyList(params1);
  // 响应结构为 { code, msg, data: { records, total } }，total 在 data 层级
  const zeroTotal = Number((res as any)?.data?.total) || 0;
  const answerTotal = Number((questionRes as any)?.data?.total) || 0;
  const total = zeroTotal + answerTotal;
  badgeNumber.value = total;
  // 同步更新 store，保证 badgeNumberComputed 数据一致性
  StaticsStore.setZeroUserOnlineTotal(zeroTotal);
  StaticsStore.setAnswerTotal(answerTotal);
}

// 开启10秒冷却
function startSwitchCooldown() {
  try {
    isSwitchCooldown.value = true;
    if (switchCooldownTimer) clearTimeout(switchCooldownTimer);
    switchCooldownTimer = setTimeout(() => {
      isSwitchCooldown.value = false;
      switchCooldownTimer = null;
    }, 10_000);
  } catch {
    isSwitchCooldown.value = false;
  }
}
function updateCurrentDepart() {
  if (
    bigActiveName.value === 'first' &&
    (!currentOrganization.value || !currentOrganizationLabel.value)
  ) {
    getCurrentOrganizationFunc();
  }
}
function updateTiming() {
  const time = Number(appConfig.settingData.STATISTICS_REFRESH_TIME) || 30;
  clearTimer?.();
  clearTimer = useSetInterval(() => {
    if (bigActiveName.value === 'first') {
      handleRefresh();
    }
  }, time * 1000);
}
function handleRefresh() {
  // 如果当前是自定义页签，刷新 iframe
  if (currentTab.value?.isCustomNav) {
    iframeKey.value++;
  }
  useEmitter().emit('refreshAllData');
}

// 协同监测统计导出
// 1. 时间范围必填校验
// 2. 时间范围不超过半年（183 天）校验
// 3. 调用接口获取 blob，通过 downloadFile（cspc 走 SDK，bspc 走 <a download>）触发下载
const HALF_YEAR_DAYS = 183;
async function handleExport() {
  if (exportLoading.value) return;

  const startTime = timeRange.value?.[0] || '';
  const endTime = timeRange.value?.[1] || '';
  if (!startTime || !endTime) {
    ElMessage.warning('请先选择时间范围');
    return;
  }

  // 校验时间范围不超过半年
  const start = new Date(startTime);
  const end = new Date(endTime);
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
    ElMessage.warning('时间范围格式错误');
    return;
  }
  const diffDays = Math.floor((end.getTime() - start.getTime()) / (24 * 60 * 60 * 1000));
  if (diffDays < 0) {
    ElMessage.warning('开始时间不能晚于结束时间');
    return;
  }
  if (diffDays > HALF_YEAR_DAYS) {
    ElMessage.warning(`最大导出半年的数据，当前选择 ${diffDays + 1} 天，请缩小时间范围`);
    return;
  }

  exportLoading.value = true;
  try {
    const departmentCode = StaticsStore.departmentCode || '';
    let query = `departmentCode=${encodeURIComponent(departmentCode)}&startTime=${encodeURIComponent(startTime)}&endTime=${encodeURIComponent(endTime)}&includeChildren=1`;
    const peerId = (StaticsStore as any).peerId;
    if (peerId) {
      query += `&peerId=${encodeURIComponent(peerId)}`;
    }
    const url = `${getIp()}/linkx/desktop/dashboard/v1/static/export?${query}`;

    if (isWebView2()) {
      await openUrl(url);
    } else {
      await downloadFile({ url, fileName: `协同监测明细.xlsx` });
      ElMessage.success('导出成功');
    }
  } catch (error) {
    console.error('协同监测统计导出失败:', error);
    ElMessage.error('导出失败');
  } finally {
    exportLoading.value = false;
  }
}
// 获取组织信息
async function getCurrentOrganizationFunc() {
  const idCard = PIMStore.user?.idCard;
  if (!idCard) return;
  const { code, data } = await getCurrentOrganization({ idCard });
  if (code === 0 && data) {
    console.log('getCurrentOrganization data:', data);
    const arr = (data as OrgData)?.userDepartments || [];
    if (arr.length === 0) return;
    const index = arr.findIndex((item) => {
      return item.isPrimary;
    });
    if (index !== -1) {
      currentOrganization.value = arr[index].departmentCode;
      currentOrganizationLabel.value = arr[index].departmentName;
      if (!currentOrganization.value) {
        // 如果执行到这里则不再向下执行，需要将是否获取数据状态设置为true，确保tree组件间正常展示
        isSelectTreeDefaultGetComplate.value = true;
        return;
      }
      queryDepartmentFunc(currentOrganization.value);
    }
  } else {
    isSelectTreeDefaultGetComplate.value = true;
  }
}

useEmitter('getRoleAuthMitt', () => {
  debounceInit();
});
useEmitter('getReloginClickCancelState', (flag: boolean) => {
  reLoginIsClickCancel.value = flag;
});
// 监听全局 appConfig 更新事件，刷新当前页面的定时任务周期
useEmitter('APP_CONFIG_UPDATED', () => {
  updateTiming();
});
function findCodeInDepartment(data, targetCode) {
  // 遍历数组中的每个节点
  for (const node of data) {
    // 如果当前节点的 id 等于目标 id，返回 true
    if (node.code === targetCode) {
      return true;
    }
    // 如果当前节点有子节点，递归检查子节点
    if (node.children && node.children.length > 0) {
      const foundInChildren = findCodeInDepartment(node.children, targetCode);
      if (foundInChildren) {
        return true;
      }
    }
  }
  // 遍历完所有节点都没有找到，返回 false
  return false;
}
// 获取下级组织列表
async function queryDepartmentFunc(parentCode, peerId: string | null = StaticsStore.peerId) {
  if (imOrgPrivsArr.value?.length > 0) {
    console.log('queryDepartmentFunc imOrgPrivsArr:', parentCode);
    deparmentArr.value = imOrgPrivsArr.value;
    isShowOldTree.value = true;
  } else {
    console.log('queryDepartmentFunc parentCode:', parentCode);
    try {
      let res: any;
      if (
        appConfig.settingData.DEPARTMENT_SYNC_SIGN === 'true' ||
        appConfig.settingData.DEPARTMENT_SYNC_SIGN === true
      ) {
        res = await queryDepartmentTree({ parentCode, peerId });
        isShowOldTree.value = true;
      } else {
        res = await queryDepartment({ parentCode, peerId });
        isShowOldTree.value = false;
      }
      const { code, data } = res;
      if (code === 0 && data) {
        deparmentArr.value = data.children || [];
      }
    } catch { }
  }

  // 如果不是数据权限返回,获取不到本级,把本级添加到数组中默认展示
  if (
    imOrgPrivsArr.value?.length === 0 &&
    currentOrganization.value &&
    currentOrganizationLabel.value
  ) {
    deparmentArr.value = [
      {
        code: currentOrganization.value,
        name: currentOrganizationLabel.value,
        children: deparmentArr.value,
      },
    ];
  }
  // 如果当前code不包含再筛选的数据中，取筛选出的第一条数据作为默认值
  if (
    deparmentArr.value.length > 0 &&
    !findCodeInDepartment(deparmentArr.value, currentOrganization.value)
  ) {
    currentOrganization.value = deparmentArr.value[0]?.code;
    currentOrganizationLabel.value = deparmentArr.value[0]?.name;
  }
  if (currentOrganization.value) {
    getBadgeNumber(currentOrganization.value);
  }

  isSelectTreeDefaultGetComplate.value = true;
}

// 节点列表加载完成：节点数大于 1（含"当前节点"）才显示选择器
function handleNodesLoaded(count: number) {
  showNodeSelect.value = count > 1;

  // 如果节点数只有1个（当前节点），则不显示选择器 然后默认加载当前节点数据
  if(!showNodeSelect.value) handleNodeChange(null);
}

// 节点切换处理：peerId 为空表示当前节点，走原逻辑；非空表示其他节点，直接查询全部组织树
async function handleNodeChange(peerId: string | null) {
  // 当前节点：走原逻辑（getCurrentOrganizationFunc + queryDepartmentFunc）
  if (!peerId) {
    StaticsStore.setPeerId(peerId);
    // 重新获取当前用户组织并拉取下级组织
    currentOrganization.value = '';
    currentOrganizationLabel.value = '';
    isSelectTreeDefaultGetComplate.value = false;
    await getCurrentOrganizationFunc();
    return;
  }
  // 切换到其他节点：直接查询该节点全部组织树（isQueryAll=true，仅传 peerId）
  try {
    let res: any;

    if (
      appConfig.settingData.DEPARTMENT_SYNC_SIGN === 'true' ||
      appConfig.settingData.DEPARTMENT_SYNC_SIGN === true
    ) {
      res = await queryDepartmentTree({ peerId }, true);
      isShowOldTree.value = true;
    } else {
      res = await queryDepartment({ peerId }, true);
      isShowOldTree.value = false;
    }
    const { code, data } = res;
    if (code === 0 && data) {
      deparmentArr.value = Array.isArray(data) ? data : [data];
    } else {
      deparmentArr.value = [];
    }
  } catch {
    deparmentArr.value = [];
  }
  // 重置当前组织为新树首个节点
  currentOrganization.value = deparmentArr.value[0]?.code || '';
  currentOrganizationLabel.value = deparmentArr.value[0]?.name || '';
  isSelectTreeDefaultGetComplate.value = true;
  // 批量更新 store（同一 tick 内，watch 仅触发一次）
  StaticsStore.setPeerId(peerId);
  if (currentOrganization.value) {
    StaticsStore.setDepartmentCode(currentOrganization.value);
    getBadgeNumber(currentOrganization.value);
  }
}

function refreshAllBadges() {
  nextTick(() => {
    if (currentOrganization.value) {
      getBadgeNumber(currentOrganization.value);
    }
    debouncedGetStatistics();
  });
}
// 设置协同岗开关状态
const handleRefreshFunc = async (snapshotStatus?: boolean): Promise<{ success: boolean; data?: any; errorMsg?: string }> => {
  try {
    const { userid, username } = userInfo.value;
    if (!userid) {
      console.error('用户ID不存在');
      return { success: false, errorMsg: '用户ID不存在' };
    }
    // 使用快照值计算目标状态，若未传入快照则使用当前值
    const currentStatus = snapshotStatus !== undefined ? snapshotStatus : switchContainer.value;

    const { code, data, msg } = await switchStatus({ userId: userid, userName: username, switchStatus: !currentStatus});

    if (code === 0) {
      PIMStore.updateXietongGroup();
      return { success: true, data };
    } else {
      // 返回后端错误信息，由调用方决定是否显示
      return { success: false, errorMsg: msg || '状态切换失败' };
    }
  } catch {
    return { success: false, errorMsg: '' };
  }
};
// 统计该协同岗当前剩余人数
const getLastNumFunc = async () => {
  try {
    const { userid } = userInfo.value;
    if (!userid) {
      throw new Error('用户ID不存在');
    }

    const { code, data, msg } = await getLastNum({ userId: userid });

    if (code === 0) {
      return data;
    } else {
      throw new Error(msg || '获取人数信息失败');
    }
  } catch (error) {
    console.error('获取人数信息失败:', error);
    throw error;
  }
};
const handleClick = (tab) => {
  console.log(tab, 'tab', bigActiveName.value)
  // 如果点击的是当前已经激活的 tab，不执行任何操作
  if (tab.paneName === bigActiveName.value) {
    return;
  }
  bigActiveName.value = tab.paneName;
  if (tab.paneName === 'first') {
    timeRange.value = getRecentSomeDays(Number(cycle.value) || 7);
    // 强制刷新协同监测统计数据
    nextTick(async () => {
      // 使用 store.departmentCode 作为数据源（用户在其他页签切换部门会更新 store）
      const orgCode = StaticsStore.departmentCode || currentOrganization.value;
      // 只有在组织信息为空时才重新获取
      if (!orgCode) {
        await getCurrentOrganizationFunc();
        await queryDepartmentFunc(currentOrganization.value);
      }
      // 直接用当前组织刷新数据
      if (orgCode) {
        getBadgeNumber(orgCode);
      }
      handleRefresh();
    });
  }
};
// 自定义 tab 点击处理（用于 ScrollableTabs 组件）
const handleCustomTabClick = (tab: any) => {
  const clickedTab = tdTabs.value.find((t) => t.id === tab.id);
  // 弹窗模式：不切换 tab，直接调用 openUrl 打开
  if (clickedTab?.isCustomNav && clickedTab?.openWay === 1) {
    openUrl(buildIframeUrl(clickedTab.url));
    return;
  }
  // 嵌入模式及普通 tab：手动更新 bigActiveName 后执行切换逻辑
  bigActiveName.value = tab.id;
  handleClick({ ...tab, paneName: tab.id });
};
// 点击我的收藏
const myCollect = () => {
  showArchiveDialog.value = true;
  // 每次打开对话框时重置activeIndex，确保组件重新创建
  activeIndex.value = -1;
  nextTick(() => {
    activeIndex.value = 0;
  });
};
// 点击推送消息
const handleCreateNotify = () => {
  showNotifyDialog.value = true;
  getNotifyData();
};
// 切换标题
const switchTitle = (_, index) => {
  activeIndex.value = index;
};
async function handleXietongBefore() {
  // 冷却期内直接拦截
  if (isSwitchCooldown.value) {
    ElMessage.warning('操作过于频繁，请稍后再试');
    return false;
  }
  // 防止重复点击
  if (switchLoading.value) {
    return false;
  }

  // 检查网络连接
  if (!checkNetworkConnection()) {
    ElMessage.error('网络连接异常，请检查网络后重试');
    return false;
  }

  // 保存弹窗显示时的状态快照
  const snapshotStatus = switchContainer.value;

  try {
    switchLoading.value = true;

    // 弹窗确认
    let text: string = '确定要开启协同岗吗？';

    if (switchContainer.value) {
      try {
        // 添加超时控制
        const timeoutPromise = new Promise((_, reject) => {
          setTimeout(() => reject(new Error('获取人数信息超时')), 5000);
        });

        const { lastPeopleNum } = (await Promise.race([getLastNumFunc(), timeoutPromise])) as {
          lastPeopleNum: number;
        };
        // 设置下岗提示后台配置（实时获取最新配置）
        const { data: latestConfig } = await getGlobalsList();
        const [text1, text2] = (latestConfig?.ATTENDANCE_SWITCH_NOTIFY || '').split('|');
        if (lastPeopleNum <= 1) {
          // 检查是否允许最后一人下岗
          const allowLayoffs = checkConfigSwitch({
            configData: latestConfig,
            configKey: 'COOP_USER_LAST_ALLOW_LAYOFFS',
            defaultValue: true,
          });
          if (!allowLayoffs) {
            ElMessage.warning('当前你是本协同岗的最后一个在线用户，无法离岗！');
            return false;
          }
          text = text1 || '当前仅您一人在岗，是否确认下岗？';
        } else {
          text = text2 || '系统将自动分配下一个支撑人员，是否确认下岗？';
        }
      } catch {
        // 如果获取人数失败，使用默认提示
        text = '是否确认切换协同岗状态？';
      }
    }

    const res = await MessageBox({
      iconName: 'icon_warning',
      isLight: true,
      offset: ['45%', '20%'],
      text,
      title: '系统提示',
      type: 'ok',
    });

    if (res) {
      try {
        // 校验状态是否已变更
        if (switchContainer.value !== snapshotStatus) {
          ElMessage.warning('协同岗状态已变更，请重新操作');
          return false;
        }
        // 添加超时控制
        const timeoutPromise = new Promise<{ success: boolean; errorMsg?: string }>((_, reject) => {
          setTimeout(() => reject(new Error('状态切换超时')), 10_000);
        });

        const result = await Promise.race([handleRefreshFunc(snapshotStatus), timeoutPromise]);
        if (!result?.success) {
          // 如果有后端错误提示，显示后端提示；否则显示默认提示
          if (result?.errorMsg) {
            ElMessage.error(result.errorMsg);
          } else {
            ElMessage.warning('操作失败，请稍后重试');
          }
          return false;
        }
        if (switchContainer.value) {
          bigActiveName.value = 'first';
        }
        // 切换成功后进入10秒冷却
        startSwitchCooldown();
        return true;
      } catch {
        ElMessage.warning('操作超时，请稍后重试');
        return false;
      }
    }
    return false;
  } catch {
    ElMessage.error('操作异常，请稍后重试');
    return false;
  } finally {
    switchLoading.value = false;
  }
}

function handleXietong() {
  try {
    // 检查用户信息是否存在
    if (!userInfo.value?.userid) {
      ElMessage.error('用户信息异常');
      return;
    }

    // 更新store中的状态
    PIMStore.setcollaboration(switchContainer.value);

    // 刷新数据
    handleRefresh();
  } catch {
    ElMessage.error('状态更新异常，请稍后重试');
  }
}
// 检查网络连接状态
function checkNetworkConnection() {
  return navigator.onLine;
}

// 禁止选择大于今天的日期
function disabledDate(time: Date) {
  return time.getTime() > Date.now();
}

// WebView2 日期选择 workaround
const {
  datePickerRef,
  datePickerVisible,
  handleCalendarChange,
  handlePanelChange,
  handleDateChange,
  handleVisibleChange,
  popperOptions,
  teleported,
} = useWebView2DatePicker(timeRange);
// 自定义建群(old-way)
async function handleCustomizeGroup() {
  const params: any = {
    departmentId: undefined,
    includes: ['normal', 'cooperated'],
    selectedList: [userInfo.value?.userid],
    title: '联系人',
    type: 'cooperated',
  };

  // 兼容：
  // - Browser 旧版：openSelectMemberUI 打开弹窗，选择结果通过 onSelectedMembers 回调返回
  // - WebView2 新版：selectMembers 会直接返回选择结果（不再走 onSelectedMembers）
  const handler = async (data) => {
    // 兼容返回结构：
    // - WebView2 新版：{ errorCode, errorMsg, data: Array<{userId,userName,...}> }
    // - Browser 旧版：可能是 { data: { contactList: Array<{id,name,...}> } }（历史结构）
    let members: any[] = [];
    if (Array.isArray(data?.data)) {
      members = data.data;
    } else if (Array.isArray(data?.data?.contactList)) {
      members = data.data.contactList;
    } else if (Array.isArray(data)) {
      members = data;
    }

    // 提取成员ID列表（新版 userId / 旧版 id）
    const memberIds = members
      .map((user) => user.userId ?? user.id)
      .filter(Boolean);
    if (userInfo.value?.userid) {
      memberIds.unshift(userInfo.value.userid);
    }

    // 提取成员名称（新版 userName / 旧版 name）
    const memberNames = members
      .map((user) => user.userName ?? user.name)
      .filter(Boolean);
    if (userInfo.value?.username) {
      memberNames.unshift(userInfo.value.username);
    }

    // 限制最多只取前4个人的名称
    const displayName = memberNames.slice(0, 4).join(',');
    const groupName = displayName || '自定义群组';

    const params1 = {
      addMembers: memberIds,
      groupName,
      groupType: '3',
    };
    try {
      const result = await createGroup(params1);
      if (result && result.groupId) {
        // 打开聊天界面
        openChat({ groupId: result.groupId });
      }
    } catch (error) {
      console.error('创建群组时发生错误:', error);
    }
  };

  const selectRes = await openSelectMemberUI(params);
  if (selectRes) {
    await handler(selectRes);
    return;
  }

  await onSelectedMembers(handler);
}
// 自定义建群(new-way)
async function handleCustomizeGroupNew() {
  // 打开弹窗，用 Promise 包裹拿到选中结果
  const CID = 'createGroupByCostom';
  const selectedMembers: any[] | null = await new Promise((resolve) => {
    Dialog({
      cid: CID,
      content: createGroupByCostom,
      shade: true,
      data: {
        userId: userInfo.value?.userid,
        onConfirm: (members: any[]) => {
          Dialog(CID)?.close();   // 关弹窗
          resolve(members);       // 把选中人传出去
        },
        onCancel: () => {
          Dialog(CID)?.close();   // 关弹窗
          resolve(null);          // 取消视为 null
        },
      },
    });
  });

  // 用户取消 / 未选人
  if (!selectedMembers || !selectedMembers.length) return;

  // 拼接创建参数
  const selfId = userInfo.value?.userid ?? '';
  const selfName = userInfo.value?.username ?? '';

  const memberIds = [selfId, ...selectedMembers.map((u) => u.id)].filter(Boolean);
  const memberNames = [selfName, ...selectedMembers.map((u) => u.name)].filter(Boolean);
  const groupName = memberNames.slice(0, 4).join(',') || '自定义群组';

  // 调接口创建群
  try {
    const result = await createGroup({ addMembers: memberIds, groupName, groupType: '3', });
    if (result?.groupId) {
      console.log('[handleCustomizeGroupNew] 创建群组成功:');
      createGroupSuccess();
      openChat({ groupId: result.groupId });
    }
  } catch (err) {
    console.error('[handleCustomizeGroupNew] 创建群组失败:', err);
  }
}

function createGroupSuccess() {
   handleRefreshList();
   handleRefreshTagStatistics();
}

function handleCreateGroup() {
  Dialog({
    cid: 'noekeyCreateGroup',
    content: CreateGroup,
    shade: true,
    data: {
      currentTheme:currentTheme.value,
      onSuccess: function() {
        console.log('[handleCreateGroup] 创建群组成功:');
        createGroupSuccess();
      },
    }
  });
}
function handleCreateGroupByCoop() {
  Dialog({
    cid: 'CreateGroupByCoop',
    content: CreateGroupByCoop,
    shade: true,
    data: {
      currentTheme:currentTheme.value,
      onSuccess: function() {
        console.log('[handleCreateGroupByCoop] 创建群组成功:');
        createGroupSuccess();
      },
    }
  });
}
function handleCreateGroupByDispatch() {
  Dialog({
    cid: 'CreateGroupByDispatch',
    content: CreateGroupByDispatch,
    shade: true,
    data: {
      currentTheme:currentTheme.value,
      onSuccess: function() {
        console.log('[handleCreateGroupByDispatch] 创建群组成功:');
        createGroupSuccess();
      },
    }
  });
}
const handleRefreshList = () => {
  handleRefresh();
};
const handleRefreshTagStatistics = () => {
  refreshTagStatistics.value++;
};
async function handleClickTab(item) {
  // 切换到 AI tab 时刷新 isSessionMode
  if (item.id === 'ai') {
    const { code, data } = await getGlobalsList();
    if (code === 0) {
      isSessionMode.value = checkIsSessionMode(data);
    }
  }
  // 切换到非常用应用tab时，重置当前应用
  if (item.id !== 'app') {
    currentAppId.value = '';
  }
  activeTab.value = item.id;
}

// 点击应用项
function handleAppClick(url: string, item: any) {
  if (item.id == 25) {
    currentAppId.value = 'dutyInformation';
    return
  }
  let urlList = url.split(';');
  if(urlList.length >= 3){
    openUrl(urlList[2]);
  }else{
    openUrl(urlList[0]);
  }
}

// 返回应用列表
function handleAppBack() {
  currentAppId.value = '';
}
async function getNotifyData(){
  requestParams.value.userId = PIMStore.user?.userid || '';
  const {code,data,msg}:{code:string | number,data:any,msg:string} = await getNotifyList(requestParams.value);
  if(code === 0){
    notifyList.value = data?.records || [];
  } else {
    ElMessage.error(msg);
  }
}
</script>

<template>
  <div v-if="!effectLicense || systemLimit" class="index-wrap index-license">
    <div class="index-inner" :style="{ minWidth: '800px' }">
      系统功能受限，请联系管理员
    </div>
  </div>
  <div v-else class="index-wrap">
    <div class="index-inner" :style="{ minWidth: '800px' }">
      <!-- 第一部分：顶部横向布局 -->
      <div class="common-top">
      <div class="top-left-tabs">
        <div v-for="item in totalTabs" :key="item.id" class="tab-item" :class="{ 'is-active': activeTab === item.id }"
          @click="handleClickTab(item)">
          <img alt="tab-img" class="tab-img" :src="tabImgs[
            activeTab === item.id
              ? item.id + (currentTheme === 'light' ? 'Selected' : 'SelectedDark')
              : item.id + (currentTheme === 'light' ? '' : 'Dark')
            ]
            " />
        </div>
      </div>
      <!-- 开关-->
      <div class="switch-wrap">
        <!-- <ThemeSwitcher /> -->
        <el-tooltip
          class="box-item"
          :effect="currentTheme === 'light' ? 'light': 'dark'"
          :content="groupButtonConfig.notifyMessage.title"
          placement="top-start"
        >
          <div class="create-btns"
               size="default"
               @click="handleCreateNotify()"
               @mouseenter="isNotifyHover = true" @mouseleave="isNotifyHover = false">
            <img alt="create" :src="currentTheme === 'light'
              ? isNotifyHover
                ? dispatchHover
                : dispatch
              : dispatchDark
            "/>
            <span class="label">{{ groupButtonConfig.notifyMessage.title }}</span>
          </div>
        </el-tooltip>
         <el-tooltip
        class="box-item"
        :effect="currentTheme === 'light' ? 'light': 'dark'"
        :content="groupButtonConfig.customGroup.title"
        placement="top-start"
      >
        <div v-if="licenseInfo.LINKXGCF === '1' && groupButtonConfig.customGroup.show && ONE_KEY_CREATE_GROUP_SIGN"  class="create-btns" size="default"
          @click="SHOW_331_FEATURE ? handleCustomizeGroupNew() : handleCustomizeGroup()" @mouseenter="isCustomizeHover = true" @mouseleave="isCustomizeHover = false">
          <img alt="create" :src="currentTheme === 'light'
              ? isCustomizeHover
                ? customizeHover
                : customize
              : customizeDark
            " />
          <span class="label">{{ groupButtonConfig.customGroup.title }}</span>
        </div>
        </el-tooltip>
        <!-- 一键建群 -->
         <el-tooltip
        class="box-item"
        :effect="currentTheme === 'light' ? 'light': 'dark'"
        :content="groupButtonConfig.quickGroup.title"
        placement="top-start"
      >
      <div v-if="licenseInfo.LINKXGCF === '1' && groupButtonConfig.quickGroup.show" class="create-btns" size="default" @click="handleCreateGroup()"
        @mouseenter="isCreateHover = true" @mouseleave="isCreateHover = false">
        <img alt="create" :src="currentTheme === 'light' ? (isCreateHover ? createHover : create) : createDark" />
        <span class="label">{{ groupButtonConfig.quickGroup.title }}</span>
      </div>
      </el-tooltip>
        <!-- 职能建群 -->
         <el-tooltip
        class="box-item"
        :effect="currentTheme === 'light' ? 'light': 'dark'"
        :content="groupButtonConfig.functionGroup.title"
        placement="top-start"
      >
        <div v-if="licenseInfo.LINKXGCF === '1' && groupButtonConfig.functionGroup.show" class="create-btns" size="default" @click="handleCreateGroupByCoop()"
          @mouseenter="isCoopHover = true" @mouseleave="isCoopHover = false">
          <img alt="create" :src="currentTheme === 'light' ? (isCoopHover ? coopHover : coop) : coopDark" />
          <span class="label">{{ groupButtonConfig.functionGroup.title }}</span>
        </div>
        </el-tooltip>
        <!-- 一键调度 -->
         <el-tooltip
        class="box-item"
        :effect="currentTheme === 'light' ? 'light': 'dark'"
        :content="groupButtonConfig.quickDispatch.title"
        placement="top-start"
      >
        <div v-if="licenseInfo.LINKXGCF === '1' && groupButtonConfig.quickDispatch.show && isDispatchUserAllowed" class="create-btns" size="default" @click="handleCreateGroupByDispatch()"
          @mouseenter="isDispatchHover = true" @mouseleave="isDispatchHover = false">
          <img alt="create" :src="currentTheme === 'light' ? (isDispatchHover ? dispatchHover : dispatch) : dispatchDark" />
          <span class="label">{{ groupButtonConfig.quickDispatch.title }}</span>
        </div>
        </el-tooltip>
        <!-- 我的收藏 -->
        <div v-if="licenseInfo.LINKXGCF === '1'" class="create-btns" size="default" @click="myCollect()"
          @mouseenter="isCollectionHover = true" @mouseleave="isCollectionHover = false">
          <img alt="create" :src="currentTheme === 'light'
              ? isCollectionHover
                ? collectionHover
                : collection
              : isCollectionHover
                ? collectionHover
                : collectionDark
            " />
          <span class="label">我的收藏</span>
        </div>
        <div v-if="showXieTong && licenseInfo.LINKXGCF === '1'" class="line"></div>
        <div v-if="showXieTong && licenseInfo.LINKXGCF === '1'" class="switch-container">
          <div class="switch-title">协同岗</div>
          <el-switch v-model="switchContainer" :before-change="handleXietongBefore" :disabled="isSwitchCooldown"
            :loading="switchLoading" @change="handleXietong" />
        </div>
        <el-tooltip v-if="appVersion" :content="'版本号：' + appVersion" placement="top" :show-after="300">
          <div class="version-info">版本号：{{ appVersion }}</div>
        </el-tooltip>
      </div>
    </div>
    <div class="ai-wrap" :class="{ 'session-mode': isSessionMode }" v-if="activeTab === 'ai'">
      <div v-if="!AsyncAiModule" class="loading-placeholder">
        <slot name="loading">加载中...</slot>
      </div>
      <component v-else :is="AsyncAiModule" />
    </div>
    <!-- 常用应用 -->
    <div v-if="activeTab === 'app'" class="app-wrap">
      <div class="app-container">
        <!-- 卡片列表页 -->
        <div v-if="!currentAppId" class="app-card-list">
          <div
            v-for="app in appList"
            :key="app.id"
            class="app-card"
            @click="handleAppClick(app.url, app)"
          >
            <img class="app-icon" :src="`${getIp()}/linkx/desktop/admin${app.icon}`" alt="" />
            <span class="app-name">{{ app.name }}</span>
          </div>
        </div>
        <!-- 应用内容页 -->
        <div v-else class="app-content-full">
          <DutyInformation v-if="currentAppId === 'dutyInformation'" @back="handleAppBack" />
        </div>
      </div>
    </div>
    <div v-if="activeTab === 'cooperation'" class="collaboration-wrap">
      <div class="top-bar">
        <div class="left-tabs">
          <!-- <TdTab
          v-if="showTabs"
          class="tab"
          :data="tdTabs"
          :default-value="bigActiveName"
          @click="handleClick"
        /> -->
          <ScrollableTabs
            v-if="showTabs"
            ref="scrollableTabsRef"
            :modelValue="bigActiveName"
            :tabs="tdTabs"
            @tab-click="handleCustomTabClick"
            :scroll-distance="50"
          >
            <template #badge="{ tab }">
              <el-badge
                v-if="tab.id === 'first' && badgeNumberComputed > 0"
                :max="99"
                style="margin-left: 2px"
                :value="badgeNumberComputed"
              />
              <el-badge
                v-if="tab.id === 'third' && problemNumber > 0"
                :max="99"
                style="margin-left: 2px"
                :value="problemNumber"
              />
            </template>
          </ScrollableTabs>
        </div>
        <!-- v-if="switchContainer" -->

        <div class="right-components">
          <div v-if="(bigActiveName === 'first' || (currentTab?.isCustomNav && customTabAppendSwitch)) && tdTabs.length > 0" class="statics-filter">
            <!-- 节点下拉（新增） -->
            <div v-if="showNodeSelect" class="current-node">
              <div class="current-organization-title">节点：</div>
              <NodeSelect
                @change="handleNodeChange"
                @nodes-loaded="handleNodesLoaded"
                :appVersion="appVersion"
              />
            </div>
            <!-- 当前组织下拉框 -->
            <div class="current-organization">
              <div class="current-organization-title">当前组织：</div>
              <SelectTree v-if="isShowOldTree" :default-org="currentOrganization"
                :default-org-label="currentOrganizationLabel" :deparment-arr="deparmentArr" />
              <SelectTreeLazy v-else :confirm-data-get-flag="isSelectTreeDefaultGetComplate"
                :default-org="currentOrganization" :default-org-label="currentOrganizationLabel"
                :deparment-arr="deparmentArr" />
              <!-- <el-select v-model="currentOrganization" placeholder="请选择" size="small">
              <el-option
                v-for="item in deparmentArr"
                :key="item.departmentCode"
                :label="item.departmentName"
                :value="item.departmentCode"
              />
            </el-select> -->
            </div>
            <!-- 区间时间选择 -->
            <div class="time-range">
              <el-date-picker
                ref="datePickerRef"
                v-model="timeRange"
                v-model:visible="datePickerVisible"
                :disabled-date="disabledDate"
                end-placeholder="结束日期"
                format="YYYY-MM-DD"
                range-separator="至"
                :shortcuts="shortcuts"
                start-placeholder="开始日期"
                type="daterange"
                value-format="YYYY-MM-DD"
                :teleported="teleported"
                :popper-options="popperOptions"
                @calendar-change="handleCalendarChange"
                @panel-change="handlePanelChange"
                @change="handleDateChange"
                @visible-change="handleVisibleChange"
              />
            </div>
            <!-- 地图模式切换按钮 -->
            <!-- <div class="map-mode">
          <el-button size="default" type="primary">地图模式</el-button>
        </div> -->
            <!-- 刷新按钮 -->
            <div class="refresh">
              <el-button class="archived-btn" size="default" @click="handleRefresh()">
                <el-icon class="archive-icon">
                  <RefreshLeft />
                </el-icon>刷新
              </el-button>
              <el-button
                type="primary"
                class="export-btn"
                size="default"
                :loading="exportLoading"
                @click="handleExport"
              >
                <el-icon class="archive-icon" v-if="!exportLoading">
                  <Download />
                </el-icon>导出
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 第二部分：tab 内容展示区 -->
      <div class="tab-content">
        <!-- 空白页面 -->
        <div v-if="tdTabs.length === 0" class="tab-content-item">
          <div class="empty-content">
            <img alt="empty" class="empty-img" src="@/assets/images/pim/empty2.png" />
            <div class="title1">暂无协同岗信息</div>
            <div class="title2">请联系管理员</div>
          </div>
        </div>

        <!-- 协同群组 -->
        <div v-else-if="bigActiveName === 'second'" class="tab-content-item">
          <SecondCards :refresh-tag-statistics="refreshTagStatistics" />
          <ArchiveManagement :refresh-tag-statistics="refreshTagStatistics" />
        </div>
        <!-- 协同处理 -->
        <div v-else-if="bigActiveName === 'third'" class="tab-content-item">
          <ProblemSolve :hidden-title="true" :target="tabId" />
        </div>
        <!-- 任务页签 -->
        <div v-else-if="currentTab.navSubType==='task'" class="tab-content-item">
          <DynamicTaskManage :task-tab="currentTab" />
        </div>
         <!-- PC端自定义页签 iframe 展示 -->
        <div v-else-if="currentTab?.isCustomNav" class="tab-content-item pc-nav-iframe">
          <iframe :key="iframeKey" :src="buildIframeUrl(currentTab.url)" frameborder="0" style="width: 100%; height: 100%;" />
        </div>
        <!-- 协同监测统计 - 使用 v-show 避免组件销毁重建导致的性能问题 -->
        <div v-show="bigActiveName === 'first'" class="tab-content-item">
          <StatCards />
          <ChartArea />
          <TableArea />
        </div>

        <!-- 任务 -->
        <div v-if="currentTab?.navSubType==='taskStandardParts'" class="tab-content-item">
          <TaskStandardParts :task="currentTab" :key="currentTab.id" />
        </div>

      </div>
    </div>
    <!-- 已归档未归档弹框 -->
    <el-dialog class="container-dialog" v-model="showArchiveDialog" style="height: 75%" title="我的收藏" width="60%">
      <div class="archive-container">
        <div class="title-container">
          <div v-for="(item, index) in titleFilter" :key="index" class="title-item"
            :class="{ active: activeIndex === index }" @click="switchTitle(item, index)">
            {{ item }}
          </div>
        </div>
        <div class="dialog-table-wrapper">
          <ArchiveTable
            v-if="activeIndex === 0"
            :is-dialog="true"
            :is-my-archive="true"
            @refresh-list="handleRefreshList"
            @refresh-tag-statistics="handleRefreshTagStatistics"
          />
          <ArchivedTable
            v-if="activeIndex === 1"
            :is-dialog="true"
            :is-my-archive="true"
            @refresh-list="handleRefreshList"
          />
        </div>
      </div>
    </el-dialog>
    <!-- 推送消息弹窗 -->
    <el-dialog class="container-dialog" v-model="showNotifyDialog" style="height: 75%" title="推送消息" width="60%">
      <div class="notify-container" v-if="notifyList && notifyList.length > 0">
        <div v-for="(item, index) in notifyList" :key="index" class="notify-item">
          <div class="notify-name" :title="item.text">{{ item.text }}</div>
          <div class="notify-time">{{ item.receivedAt }}</div>
        </div>
      </div>
      <div class="notify-noData" v-else>
        暂无数据
      </div>
    </el-dialog>
    </div>
  </div>
</template>

<style lang="less" scoped>
.index-license {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1vw;
  color: var(--tabs-color);
}

.index-wrap {
  box-sizing: border-box;
  // display: flex;
  // flex-direction: column;
  // justify-content: space-between;
  width: 100%;
  max-width: 100vw;
  height: 100%;
  overflow-x: hidden;
  background: var(--background-color);
  box-shadow: inset 0 6px 10px -6px var(--box-shadow-color);
}

.index-inner {
  width: 100%;
  height: 100%;

  :deep(.el-overlay) {
    z-index: 1000 !important;
  }

  :deep(.el-dialog__title) {
    color: var(--text-color);
  }

  :deep(.el-dialog__headerbtn .el-dialog__close) {
    color: var(--text-color) !important;
  }

  .common-top {
    box-sizing: border-box;
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 64px;
    padding: 0 10px;
    background: var(--background-white-color);
    border: 1px solid var(--border-color);
    border-left: none;

    .top-left-tabs {
      display: flex;
      align-items: center;

      .tab-item {
        width: 94px;
        height: 30px;
        margin-right: 12px;
        cursor: pointer;

        img {
          width: 100%;
          height: 100%;
        }
      }
    }

    .switch-wrap {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .create-btns {
        display: flex;
        align-items: center;
        justify-content: center;
        height: 32px;
        padding: 0 8px;
        margin-right: 16px;
        line-height: 32px;
        cursor: pointer;
        border: 1px solid var(--border-color);
        border-radius: 1px;
        opacity: 1;

        img {
          width: 16px;
          height: 16px;
          margin-right: 4px;
        }

        .label {
            font-size: 14px;
            color: var(--text-color);
            max-width: 150px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }

        &:hover {
          .label {
            color: var(--tabs-active-color);
          }
        }
      }

      .line {
        width: 1px;
        height: 24px;
        margin: 0 24px 0 12px;
        background: var(--border-color);
        opacity: 1;
      }
    }

    .switch-container {
      display: flex;
      align-items: center;

      .switch-title {
        margin-right: 10px;
        color: var(--text-color);
      }
    }

    .version-info {
      margin-left: 21px;
      color: var(--text-color);
      font-size: 14px;
      white-space: nowrap;
      color: rgba(90, 99, 131, 1);
      font-weight: 400;
      line-height: 20px;
      overflow: hidden;
      text-overflow: ellipsis;
      max-width: 150px;
    }

    .switch-container-wrap {
      display: flex;
      align-items: center;
      justify-content: flex-end;
    }

    :deep(.el-switch) {
      .el-switch__core {
        background-color: #dcdfe6 !important;
        border-color: #dcdfe6 !important;
      }

      &.is-checked .el-switch__core {
        background-color: var(--text-switch-bg) !important;
        border-color: var(--text-switch-bg) !important;
      }

      // 加载状态样式
      &.is-loading .el-switch__core {
        cursor: not-allowed;
        opacity: 0.6;
      }
    }
  }

  .archived-btn {
    background: var(--button-bg);
    border: 1px solid var(--border-color);

    .el-icon {
      color: var(--tabs-color);
    }

    &:hover {
      background: var(--button-hover-bg);
      border: 1px solid var(--button-active-color);

      .el-icon {
        color: var(--tabs-active-color);
      }
    }
  }

  // 导出按钮：主题色深色背景 + 白色图标和文字（用 type="primary" 让全局灰色规则 :not(.el-button--primary) 不匹配）
  .export-btn {
    background: var(--primary-color);
    border-color: var(--primary-color);

    &:hover,
    &:focus {
      background: var(--primary-color);
      border-color: var(--primary-color);
      opacity: 0.85;
    }
  }

  .ai-wrap {
    box-sizing: border-box;
    width: 100%;
    height: calc(100% - 64px);
    padding: 30px 296px;
    background: var(--ai-bg-color);
    overflow: auto;

    &.session-mode {
      padding: 0;
    }
  }

  .app-wrap {
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    width: 100%;
    height: calc(100% - 64px);
    padding: 20px;
    background: var(--background-color);

    // 白色背景容器
    .app-container {
      flex: 1;
      width: 100%;
      // background: var(--background-white-color);
      border-radius: 8px;
      overflow: hidden;
    }

    // 卡片列表页
    .app-card-list {
      display: flex;
      flex-wrap: wrap;
      align-items: flex-start;
      justify-content: flex-start;
      gap: 12px;
      width: 100%;
      // height: 100%;
      // padding: 40px;

      .app-card {
        display: flex;
        flex-direction: row;
        align-items: center;
        gap: 18px;
        width: 340px;
        padding: 21px 18px;
        cursor: pointer;
        background: var(--card-bg);
        border: 1px solid var(--border-color);
        border-radius: 8px;
        box-shadow: 0 2px 8px rgb(0 0 0 / 8%);
        transition: all 0.2s;

        .app-icon {
          width: 60px;
          height: 60px;
          flex-shrink: 0;
        }

        .app-name {
          font-size: 20px;
          font-weight: 400;
          line-height: 30px;
          color: var(--text-color);
        }

        &:hover {
          box-shadow: 0 4px 16px rgb(0 0 0 / 12%);
          transform: translateY(-2px);
        }
      }
    }

    // 应用内容页（全屏）
    .app-content-full {
      width: 100%;
      height: 100%;
      overflow: hidden;
    }
  }

  .collaboration-wrap {
    width: 100%;
    height: calc(100% - 74px);

    // 协同警务区域的按钮样式（排除primary按钮，保持其白色文字）
    :deep(.el-button:not(.el-button--primary) > span) {
      color: var(--tabs-color) !important;

      &:hover {
        color: var(--tabs-active-color) !important;
      }
    }

    .top-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 54px;
      padding: 10px;
      margin: 10px;
      line-height: 54px;
      background-color: var(--background-white-color);
      border: 1px solid var(--border-color);
      box-shadow: 0 1px 4px rgb(0 0 0 / 10%);

      .left-tabs {
        flex: 1;
        min-width: 0;
        overflow: hidden;
        height: 100%;

        :deep(.el-badge__content--danger) {
          background-color: #e34242 !important;
        }
      }
        font-weight: 500;
      }

      :deep(.td-tab) {
        margin-bottom: 10px;

        .td-tabs__item {
          flex-shrink: 0;
          color: var(--tabs-color);

          &.is-active {
            color: var(--tabs-active-color);
          }

          &:first-child {
            margin: 0 14px;
          }
        }

        .td-tabs__active-bar {
          background-color: var(--tabs-active-color);
        }

        &-default {
          &::after {
            position: absolute;
            bottom: 0;
            left: 0;
            z-index: 1;
            width: 100%;
            height: 1px;
            content: '';
            border-bottom: 1px solid var(--tabs-active-color);
          }
        }
      }
    }

    .right-components {
      display: flex;
      flex-shrink: 0;
      align-items: center;
      justify-content: flex-end;
      min-width: 450px;
      width: fit-content;
      height: 54px;
      font-size: 14px;

      .group {
        margin-left: 15px;
      }

      .statics-filter {
        display: flex;
        align-items: center;
        width: fit-content;
      }

      :deep(.el-input__wrapper) {
        background: var(--background-white-color) !important;
        border: 1px solid var(--border-color) !important;
      }

      :deep(.el-range-input),
      :deep(.el-range-input::placeholder),
      :deep(.el-range-separator) {
        color: var(--text-color) !important;
      }

      .current-node {
        display: flex;
        align-items: center;
        flex-wrap: nowrap;
        margin-right: 15px;

        .current-organization-title {
          flex-shrink: 0;
          white-space: nowrap;
          margin-right: 10px;
          color: var(--text-color);
        }

        // 控制 NodeSelect 的宽度（与当前组织下拉保持一致）
        :deep(.el-select),
        :deep(.el-input) {
          width: 240px;
          min-width: 160px;
          max-width: 240px;
          flex-shrink: 1;
        }

        @media (max-width: 1200px) {
          :deep(.el-select),
          :deep(.el-input) {
            width: 200px;
          }
        }

        @media (max-width: 1000px) {
          :deep(.el-select),
          :deep(.el-input) {
            width: 180px;
          }
        }

        @media (max-width: 900px) {
          :deep(.el-select),
          :deep(.el-input) {
            width: 160px;
          }
        }
      }

      .current-organization {
        display: flex;
        align-items: center;
        flex-wrap: nowrap;
        margin-right: 15px;

        .current-organization-title {
          flex-shrink: 0;
          white-space: nowrap;
          margin-right: 10px;
          color: var(--text-color);
        }

        // 控制 SelectTree 和 SelectTreeLazy 的宽度
        :deep(.el-input) {
          width: 240px;
          min-width: 160px;
          max-width: 240px;
          flex-shrink: 1;
        }

        @media (max-width: 1200px) {
          :deep(.el-input) {
            width: 200px;
          }
        }

        @media (max-width: 1000px) {
          :deep(.el-input) {
            width: 180px;
          }
        }

        @media (max-width: 900px) {
          :deep(.el-input) {
            width: 160px;
          }
        }

        :deep(.el-select__placeholder),
        :deep(.el-select .el-select__placeholder span) {
          color: #333 !important;
        }

        :deep(.el-select__wrapper) {
          height: 34px;
          background: rgb(247 249 255 / 100%) !important;
          box-shadow: 0 0 0 1px rgb(199 209 242 / 100%) inset;
        }

        :deep(.el-input__inner) {
          height: 30px;
          padding: 0 10px;
          font-size: 12px;
          color: var(--text-color);
          background: var(--button-bg) !important;
        }

        :deep(.red-arrow) {
          color: var(--text-color);
        }
      }

      .time-range {
        margin-right: 15px;
      }

      .map-mode {
        margin-left: 10px;
      }

      // :deep(.el-button > span) {
      //   color: var(--text-color) !important;

      //   &:active {
      //     color: var(--button-active-color) !important;
      //   }
      // }
    }

    .tab-content {
      box-sizing: border-box;
      width: 100%;
      max-width: 100%;
      height: calc(100% - 74px);
      padding: 0 10px;
      overflow-x: hidden;
      background-color: var(--background-color);
      border-radius: 6px;

      :deep(.el-tabs__content) {
        margin-top: 10px;
      }

      .tab-content-item {
        display: flex;
        flex-direction: column;
        gap: 10px;
        justify-content: space-between;
        width: 100%;
        max-width: 100%;
        height: 100%;
        overflow: hidden;

        .empty-content {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          width: 100%;
          height: 100%;
          background-color: var(--background-color);
          border-radius: 6px;

          .empty-img {
            width: 200px;
            height: 200px;
          }

          .title1 {
            margin: 14px 0 16px;
            font-size: 24px;
            font-weight: 700;
            line-height: 34.75px;
            color: var(--text-color);
            letter-spacing: 0;
          }

          .title2 {
            font-size: 16px;
            font-weight: 400;
            line-height: 23.17px;
            color: var(--tabs-color);
            letter-spacing: 0;
          }
        }
      }

      .pc-nav-iframe {
        iframe {
          width: 100%;
          height: 100%;
          border: none;
        }
      }
    }
  }

.title-container {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: flex-start;
  padding: 6px 0;
  padding-top: 10px;

  .title-item {
    padding: 3px 16px;
    font-size: 14px;
    color: var(--tabs-color2);
    //   color: $--color-text-regular;
    cursor: pointer;
    background-color: var(--tabs-bg);
    border-radius: 16px;
    transition: all 0.3s;

    &:hover {
      background-color: #159aff;
    }

    &.active {
      color: var(--tabs-active-color2);
      background-color: var(--tabs-active-bg);
    }
  }
}

.dialog-table-wrapper {
  flex: 1;
  max-height: calc(100% - 30px);
  overflow: auto;
}
  :deep(.container-dialog .el-dialog__body){
    height: 90% !important;
  }
  .archive-container{
    height: 100%;
  }
.notify-container {
  padding: 6px 10px;
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;

  .notify-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 10px;
    border-bottom: 1px dashed rgba(0, 0, 0, 0.2);

    .notify-name {
      flex: 1;
      margin-right: 40px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .notify-time {
     width:  150px;
    }
  }
}
.notify-noData{
  padding-top: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

// :deep(.el-tabs__item) {
//   padding: 0 !important;
//   color: #6c6c6c !important; // 非高亮文字
//   background: none !important;
//   border-bottom: 2px solid #e8e8e8 !important; // 非高亮底部线
//   transition:
//     color 0.2s,
//     border-color 0.2s;
// }

// :deep(.el-tabs__item:hover) {
//   color: #264ed1 !important; // 悬浮高亮
//   background: none !important;
//   border-bottom: 2px solid #264ed1 !important; // 悬浮高亮底部线
// }

// :deep(.el-tabs__item.is-active) {
//   color: #264ed1 !important; // 选中高亮
//   background: none !important;
//   border-bottom: 2px solid #264ed1 !important; // 选中高亮底部线
// }</style>
