<template>
  <view v-if="systemLimit || !effectLicense" class="license-limit">系统功能受限，请联系管理员</view>
  <view class="container" v-else>
    <!-- 顶部导航栏 -->
    <view class="top-nav-bar" :style="{ 'padding-top': paddingTop + 'px' }">
      <div
          class="ai-img"
          v-if="
          permissionsArr.includes('ai') &&
          licensePermissions.LINKXACF === '1' &&
          licensePermissions.LINKXBS === '1'
        "
      >
        <img width="24px" height="24px" src="@/static/tabIcon/home_ai.png" class="clickable" @click="handleToAi()" />
      </div>
      <view v-else class="perch"></view>
      <text class="title" v-if="licensePermissions.LINKXBS === '1'">{{ title || '5110' }}</text>
      <!-- <view class="perch"></view> -->
      <view class="right">
        <XietongSwitch
            ref="xietongRef"
            v-if="licensePermissions.LINKXBS === '1' && licensePermissions.LINKXGCF === '1'"
        />
      </view>
    </view>

    <!-- 内容区域（已移除滚动） -->
    <view :style="{ 'padding-top': paddingTop + 50 + 'px' }">
      <!-- Banner/轮播图 -->
      <van-pull-refresh v-model="loading" @refresh="onRefresh">
        <view v-if="!effectLicense || licensePermissions.LINKXBS !== '1'" class="license-limit"
        >系统功能受限，请联系管理员</view
        >
        <!-- Banner/轮播图 -->
        <!-- <view @click="pushMessage">pushMessage</view> -->
        <Banner :licensePermissions="licensePermissions" />
        <!-- <view @click="pushMessage">通知测试</view> -->
        <!-- 消息/任务 -->
        <view v-if="iframeShow" class="third-part" id="third">
          <iframe id="myIframe" frameborder="0" width="100%" height="100%"></iframe>
        </view>

        <view
            v-else-if="licensePermissions.LINKXBS === '1' && licensePermissions.LINKXTCF === '1'"
            class="task-index-entry"
        >
          <view class="task-index-entry-header">
            <view class="title">
              任务统计
              <span class="taskBadge" v-if="taskList.length > 0">
                {{ taskNum }}
              </span></view
            >
            <view class="all" @click="goTask('全部')">
              <view class="left-title-arrow">
                <span>全部</span>
                <view class="arrow-right"></view>
              </view>
            </view>
          </view>
          <div v-if="taskList.length > 0" class="notice">
            <!-- <img src="@/static/5110/notice.png" width="23px" height="22px" /> -->
            <div class="notice-title">
              <span class="color-blue" style="top: 1px">通知</span>
              <span class="color-blue" style="bottom: 1px">提醒</span>
            </div>
            <div class="notice-center">
              <van-notice-bar :scrollable="false" background="#fff">
                <van-swipe
                    vertical
                    class="notice-swipe"
                    :autoplay="3000"
                    :touchable="false"
                    :show-indicators="false"
                >
                  <van-swipe-item
                      class="notice-text"
                      v-for="item in taskList"
                      :key="item.id"
                      @click="goNoticeDetail(item)"
                  >
                    <img
                        v-if="item.level === '紧急'"
                        src="@/static/5110/urgentBtnIcon.png"
                        height="14px"
                        style="margin-right: 6px; margin-top: -1px"
                    />
                    <img
                        v-else-if="item.level === '重要'"
                        src="@/static/5110/importantBtnIcom.png"
                        height="14px"
                        style="margin-right: 6px; margin-top: -1px"
                    />
                    <img
                        v-else-if="item.level === '关键'"
                        src="@/static/5110/keyBtnIcon.png"
                        height="14px"
                        style="margin-right: 6px; margin-top: -1px"
                    /><img
                      v-else
                      src="@/static/5110/generalBtnIcon.png"
                      height="14px"
                      style="margin-right: 6px; margin-top: -1px"
                  />
                    <span>{{ item.name }}</span></van-swipe-item
                  >
                </van-swipe>
              </van-notice-bar>
            </div>
            <!-- <view class="arrow-right" @click="goTask('全部')"></view> -->
          </div>
          <view class="task-index-entry-content">
            <van-swipe :autoplay="3000" :duration="1000" indicator-color="white">
              <van-swipe-item>
                <view class="first-swiper-page" v-if="firstSwiperData.length > 0">
                  <view class="left-menu">
                    <view
                        class="menu-item"
                        v-for="(item, index) in firstSwiperData"
                        :key="index"
                        @click="goTask('全部')"
                    >
                      <view class="menu-item-content">
                        <view class="menu-value">{{ item.value }}</view>
                        <view class="menu-title">{{ item.name }}</view>
                      </view>
                    </view>
                  </view>
                  <view class="right-echart">
                    <view class="my-echart">
                      <CircleEchart
                          height="64px"
                          width="64px"
                          :echartData="firstSwiperData"
                          :padAngle="5"
                      />
                      <text class="number">{{ firstSwiperDataTotal }}</text>
                    </view>
                    <view class="chart-text">统计占比</view>
                  </view>
                </view>
                <view class="swiper-empty" v-else>
                  <img class="empty-img" src="@/static/task/indexTypeEmpty.png" />
                  <view class="empty-text">暂无数据</view>
                </view>
              </van-swipe-item>
              <van-swipe-item>
                <view class="circle-data-content">
                  <view class="entry-left-content">
                    <view class="left-content-top">
                      <view
                          class="entry-content-conmon-item"
                          v-for="(item, index) in circleData"
                          :key="index"
                          @click="goTask(item.name)"
                      >
                        <view class="itme-num" :class="`color${index + 1}`">{{ item.value }}</view>
                        <view class="item-title">{{ item.name }}</view>
                      </view>
                    </view>
                    <view class="left-content-bottom">
                      <view class="progress-title">
                        <view class="icon"></view>
                        <view class="title-text">处置完成进度</view>
                        <view class="big-number">{{ complateRatio }}%</view>
                      </view>
                      <van-progress
                          class="progress"
                          color="#5DD76B"
                          track-color="#F5F5F5"
                          :show-pivot="false"
                          :percentage="complateRatio"
                          stroke-width="6"
                      />
                    </view>
                  </view>
                  <view class="entry-right-chart">
                    <view class="right-chart">
                      <CircleEchart
                          height="64px"
                          width="64px"
                          :echartData="circleData"
                          :padAngle="0"
                      />
                      <text class="number">{{ circleDataTotal }}</text>
                    </view>
                    <view class="chart-text">统计占比</view>
                  </view>
                </view>
              </van-swipe-item>
            </van-swipe>
          </view>
        </view>

        <!-- 常用应用区 -->
        <view
            class="common-app"
            v-if="licensePermissions.LINKXBS === '1' && licensePermissions.LINKXBCF === '1'"
        >
          <view
              class="section-header"
              @click="navigateToUrl('/pages/application?param=0', null, 'noTitleStyle')"
          >
            <text class="section-title">常用应用</text>
            <view class="arrow-right"></view>
          </view>
          <view class="app-grid">
            <van-grid v-if="currentApp.length" :column-num="4" :border="false">
              <van-grid-item
                  v-for="(item, index) in currentApp"
                  :key="index"
                  @click="handleClickApp(item)"
              >
                <view class="app-item" style="width: 100%; text-align: center">
                  <van-image
                      :src="`${transformImageUrl(`/admin-api${item.icon || ''}`)}`"
                      :width="adaptationSize.width"
                      :height="adaptationSize.height"
                      radius="14px"
                      @click="handleClickApp(item)"
                  />
                  <view class="app-name">{{ item.name }}</view>
                </view>
              </van-grid-item>
            </van-grid>
            <view v-else class="app-empty">
              <text class="empty-text">还没有应用哦</text>
              <view
                  class="empty-btn"
                  @click="navigateToUrl('/pages/application?param=1', null, 'noTitleStyle')"
              >
                添加
              </view>
            </view>
          </view>
        </view>
        <!-- 协同群组 -->
        <!-- 判断cappPrivJson里面是否存在‘1522392406668870009’-->
        <view
            class="collaborativeGroup"
            v-if="
            permissionsArr.includes('copilotStatistics') &&
            licensePermissions.LINKXBS === '1' &&
            licensePermissions.LINKXGCF === '1'
          "
        >
          <view class="title-text">
            <img
                v-if="!permissionsArr.includes('colFiling')"
                src="@/static/5110/box.svg"
                width="24px"
                height="24px"
            />
            <span>{{ showCollaborationFeatures ? '群组' : '协同群组' }}</span>
          </view>
          <view class="total-text" v-if="permissionsArr.includes('colFiling')">
            <view class="collaborating">
              <p>未归档</p>
              <text class="green-text">{{ totalCount.unArchived }}</text>
            </view>
            <view class="archived">
              <p>已归档</p>
              <text>{{ totalCount.archived }}</text>
            </view>
          </view>
          <view class="review" @click="goCollaborativeGroup">
            查看
            <view class="arrow-right"></view>
          </view>
        </view>

        <!-- 5110创群 -->
        <div
            v-if="licensePermissions.LINKXBS === '1' && licensePermissions.LINKXGCF === '1'"
            class="footer"
        >
          <van-button
            type="default"
              class="create-group"
              :style="{ padding: adaptationSize.btnPadding }"
              @click="createGroup"
          >
            <img
                src="@/static/5110/create_group1.png"
                :width="adaptationSize.createGroupWidth"
                :height="adaptationSize.createGroupHeight"
                style="flex-shrink: 0"
            />
            <div class="group-text" :style="{ fontSize: adaptationSize.fontSizeCreateGroup }">
              自定义建群
            </div>
          </van-button>
          <van-button type="default" class="create-group" @click="onkeyCreateGroup">
            <img
                src="@/static/5110/create_group2.png"
                :width="adaptationSize.createGroupWidth"
                :height="adaptationSize.createGroupHeight"
                style="flex-shrink: 0"
            />
            <div class="group-text" :style="{ fontSize: adaptationSize.fontSizeCreateGroup }">
              一键建群
            </div>
          </van-button>
        </div>
        <div v-if="licensePermissions.LINKXBS === '1'" class="group-desc">
          全局支撑一警，一警调动全局
        </div>
      </van-pull-refresh>
    </view>
  </view>
</template>

<script setup>
import qs from 'qs';
import { showToast } from 'vant';
import { ref, computed, onMounted, onBeforeUnmount, watch, onUnmounted } from 'vue';

import Banner from './banner.vue';
import CircleEchart from './components/circleEchart.vue';
import XietongSwitch from './components/xietongSwitch.vue';

import { taskSave, getTaskByMsgId, responsesTask, responsesTaskAll } from '@/common/api/h5.js';
import { h5Api, taskApi, groupApi } from '@/common/api/index.js';
import DC from '@/common/network/DC.js';
import { useSocketManage } from '@/common/network/ws.js';
  import { getGlobalsConfigByKey, checkConfigSwitch } from '@/common/utils';
import { useCommon } from '@/hooks/useCommon.js';
import { useApplicationStore } from '@/stores/application.js';
import { useCommunicationStore } from '@/stores/communication.js';
import { usePageUrlStore } from '@/stores/pageUrl.js';
import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';
import { useUserStore } from '@/stores/user.js';
import { queryStringToJson, locationShareFunc, debounce } from '@/utils';
import { sendNotification } from '@/utils/glassUtils.js';
import { transformImageUrl } from '@/utils/imgUrlParse.js';
  import { openAppByCheckPre } from '@/utils/appHandler.js';
import {
  heartbeatAddress,
  clearTimerLocation,
  checkLicenseStatus,
  filterMonitor,
} from '@/utils/totalFunc.js';

const { userStore: userStoreCommon } = useCommon();

const { setUserInfo } = userStoreCommon;

const loading = ref(false);
const iframeShow = ref(false);
const complateRatio = computed(
    () => circleData.value.find((item) => item.name === '已完成')?.ratio || 0,
); // 完成率
const circleData = ref([]);
const firstSwiperData = ref([]);
const firstSwiperDataTotal = computed(() =>
    firstSwiperData.value.reduce((prev, cur) => prev + cur.value, 0),
);
const circleDataTotal = ref(0);
const communicationStore = useCommunicationStore();
const applicationStore = useApplicationStore();
const userStore = useUserStore();
const pageUrlStore = usePageUrlStore();
// 使用设备适配
const { adaptationSize } = useDeviceAdapter();
const systemLimit = ref(false); //登录接口返回code为182，则系统限制

// 权限数组
const cappPrivJson = ref([]);

let socketManage = null;
const paddingTop = ref(0);
const title = ref('');
const xietongRef = ref();
const userInfo = ref(null);

const currentApp = computed(() => {
  const list = filterMonitor(applicationStore.currentApp, licensePermissions.value);
  // 限制最多返回8项
  return list.slice(0, 8);
});
const permissionsArr = computed(() => communicationStore.permissionsArr);

// 有效license。LINKXBS=1、其余全部至少一个不为0。或者日期到期了
const effectLicense = computed(() => {
  const obj = licensePermissions.value || {};
  if (obj.LINKXBS !== '1') return false;
  //检查过期状态
  if (obj.status === '0' || obj.status === '3' || obj.status === '5') {
    return false; // license已过期或者未激活
  }
  let flag = false;
  for (let i in obj) {
    if (i !== 'status' && i !== 'LINKXBS' && i !== 'expireDate') {
      if (obj[i] === '1') {
        flag = true;
      }
    }
  }
  return flag;
});
let closeFlag = false;
const accessTokenRef = ref('');
const licensePermissions = ref({ LINKXBS: '1', LINKXBCF: '1' });
const listParams = ref({
  pageNum: 1,
  pageSize: 10,
  archived: 0,
  userId: '',
  keywords: '', //搜索关键字
  tagName: '', //标签名称
  archivedTime: '',
  orgIds: '', //组织机构id,逗号分隔
});
const totalCount = ref({
  unArchived: 0,
  archived: 0,
});

  // 协同功能显示控制
  const showCollaborationFeatures = ref(false);

watch(
    () => userInfo.value?.aastoken,
    async (aastokenNew, aastokenOld) => {
      if (!aastokenNew || aastokenNew === aastokenOld) return;
      await tokenLogin({ clientId: 'CAPP-1000', token: aastokenNew });
      tasksUpdate();
      // communicationStore.h5permissions();
    },

    { immediate: true },
);

const getTaskStatusTotal = async () => {
  if (licensePermissions.value.LINKXBS !== '1' || licensePermissions.value.LINKXTCF !== '1')
    return;
  try {
    const params = {};
    // params.idCard = userInfo ? userInfo.idCard : "";
    const statusCountRes = await taskApi.getTaskStatusCount({
      ...params,
      token: accessTokenRef.value,
    });
    circleData.value = statusCountRes.map((item) => ({
      name: item.status,
      value: +item.count,
      ratio: item.ratio,
    }));
    circleDataTotal.value = circleData.value[0].value;
  } catch (err) {
    circleData.value = [
      { name: '全部', value: 0, ratio: 0, itemStyle: { color: '#1FAF9C' } },
      { name: '待处理', value: 0, ratio: 0, itemStyle: { color: '#868B98' } },
      { name: '进行中', value: 0, ratio: 0, itemStyle: { color: '#FFA05C' } },
      { name: '已完成', value: 0, ratio: 0, itemStyle: { color: '#5DD76B' } },
    ];
    circleDataTotal.value = circleData.value[0].value;
  }
};

const getTaskTypeTotal = async () => {
  try {
    const params = {};
    // params.idCard = userInfo.value ? userInfo.value?.idCard : "";
    const typeCountRes = await taskApi.getTaskTypeCount({
      ...params,
      token: accessTokenRef.value,
    });
    firstSwiperData.value = typeCountRes.map((item) => ({
      name: item.type,
      value: +item.count,
    }));
  } catch (err) {
    //测试数据
    firstSwiperData.value = [];
  }
};

// 获取未归档数据
async function getGroupList() {
  try {
    const res = await communicationStore.getUserInfo();
    const baseParams = {
      pageNum: listParams.value.pageNum,
      pageSize: listParams.value.pageSize,
      userId: res?.userid || '27740351353856',
      keywords: listParams.value.keywords,
      tagName: listParams.value.tagName,
      archivedTime: listParams.value.archivedTime,
      orgIds: listParams.value.orgIds,
    };
    // 并行请求未归档和已归档数据
    const [unArchivedRes, archivedRes] = await Promise.all([
      groupApi.getCollaborationGroupList({
        ...baseParams,
        archived: 0, // 未归档
        token: accessTokenRef.value,
      }),
      groupApi.getCollaborationGroupList({
        ...baseParams,
        archived: 2, // 已归档
        token: accessTokenRef.value,
      }),
    ]);

    totalCount.value.unArchived = unArchivedRes.total || 0;
    totalCount.value.archived = archivedRes.total || 0;
  } catch (error) {
    console.log(error);
  }
}
watch(
    () => userStore.userInfo?.accessToken,
    async () => {
      if (userStore.userInfo?.accessToken) {
        await getLicensePermissionsFunc();
        try {
          //token改变之后重新查询数据
          await afterLoginProcess();
          xietongRef.value?.getSwitchStatusFunc();
          communicationStore.h5permissions();
        } catch (error) {
          console.log('登录改变', error);
        }
      }
    },
);
onMounted(async () => {
  setDebugQueryString();
  await getUserInfo();
  getLicensePermissionsFunc();
    // 获取协同功能开关配置
    showCollaborationFeatures.value = await checkConfigSwitch();
  paddingTop.value = await communicationStore.fetchStatusBarHeight();
  initData();
  // 退出登录后重新登录
  window.WeSpaceSDK?.onIcpUserStatusChange((val) => {
    if (val === 'online') {
      initData();
    }
  });
  // 存储变更监听
  window.WeSpaceSDK?.onStorageChange('currentAppSet', () => {
    applicationStore.getCurrentApp();
  });
  // 息屏锁屏状态改变
  window.WeSpaceSDK?.onVisibleChange(async (val) => {
    if (val === true || val === 'true') {
      initData();
      setDebugQueryString();
      // 页面变为可见时重连 WebSocket
      handlePageDisappear();
      socketManage = await useSocketManage();
      socketManage.initWebSocket();
    }
  });
  // 监听iframe的点击事件的postmessage
  window.addEventListener('message', handleMessage, false);

  // 监听app关闭后，停止协同岗心跳检测
  window.WeSpaceSDK?.onClose(() => {
    if (closeFlag) return;
    clearTimerLocation();
    closeFlag = true;
  });

  //im聊天@协同岗时，创建待办任务
  window.WeSpaceSDK?.onAtCooperationUser((val) => {
    listenTask(val);
    console.log('[别人@我的消息]通知：上app通知栏，消息内容为：', val);
    sendNotificationToUser(val);
  });

  //@消息引用后更新任务状态
  window.WeSpaceSDK?.onCooperationUserSendMsg((param) => {
    console.log(JSON.stringify(param), '引用消息');
    quotationMssage(param);
  });
  // 推送消息监听
  // window.WeSpaceSDK.onRemotePushMessage((data) => {
  //   console.log("收到远程推送消息:", data);
  //   communicationStore.showNotification(data);
  // });
  // 归档成功
  DC.on('GROUP_ARCHIVING', 'GROUP_ARCHIVING', getGroupList);
  DC.on('GROUP_CREATE', 'GROUP_CREATE', getGroupList);
  // 归档状态变化时刷新统计数据
  DC.on('GROUP_ARCHIVE_CHANGE', 'ARCHIVE_SUCCESS', getGroupList);
  DC.on('PASSPORT', 'kickout', () => {
    console.log('====用户权限变更，重新登录！');
    kickoutMethod();
  });
  //轮播图跟新通知
  DC.on('CAROUSEL_UPDATE', 'UPDATE', (val) => {
    pushMessage(val, '轮播图更新');
    console.log('[轮播图更新]通知：上app通知栏，消息内容为：', val);
    sendNotification({ text: val?.title || '轮播图更新' });
  });
  DC.on('CAROUSEL_UPDATE', 'CREATE', (val) => {
    pushMessage(val, '轮播图新增');
    console.log('[轮播图新增]通知：上app通知栏，消息内容为：', val);
    sendNotification({ text: val?.title || '轮播图新增' });
  });
  DC.on('CAROUSEL_UPDATE', 'DEL', (val) => {
    console.log('[轮播图删除]通知：上app通知栏，消息内容为：', val);
    pushMessage(val, '轮播图删除');
    // sendNotification({ text: val?.title || "轮播图删除" });
  });
  //任务、眼镜推送-新增
  DC.on('TASKS_UPDATE', 'CREATE', (val) => {
    console.log('[任务新增]通知：上app通知栏，消息内容为：', val);
    taskGlassProcess(val, '新增');
    tasksUpdate();
  });
  //任务眼镜推送-新增
  DC.on('TASKS_UPDATE', 'STATUS_CHANGE', (val) => {
    console.log('[任务更新]通知：上app通知栏，消息内容为：', val);
    taskGlassProcess(val, '更新');
    tasksUpdate();
  });
  // 监听任务页面的角标更新，同步刷新首页任务列表
  DC.on('TASK_BADGE_UPDATE', 'REFRESH', () => {
    getTaskList('待处理');
  });
  //群AI助手推送
  DC.on('AT_GROUP_AI', 'RESPONSE', (val) => {
    if (val) {
      console.log('[群AI助手响应]通知：上app通知栏，消息内容为：', val);
      sendNotification({ text: val });
    } else {
      console.log('群AI助手响应数据获取失败', val);
    }
  });
  window.addEventListener('pagehide', handlePageDisappear);
  window.addEventListener('beforeunload', handlePageDisappear);
  document.addEventListener('visibilitychange', handlePageDisappear);
});
onBeforeUnmount(() => {
  // closePolling(); //关闭轮询
  handlePageDisappear();
  window.removeEventListener('message', handleMessage, false);
  communicationStore.removeStorageChange('currentAppSet');
});

// 页面消失时执行的方法
const handlePageDisappear = async () => {
  if (socketManage) {
    await socketManage.closeWebSocket();
    socketManage = null;
  }
};

onUnmounted(() => {
  handlePageDisappear();
  window.removeEventListener('pagehide', handlePageDisappear);
  window.removeEventListener('beforeunload', handlePageDisappear);
  document.removeEventListener('visibilitychange', handlePageDisappear);
});

const onRefresh = async () => {
  // await tokenLogin({ clientId: "CAPP-1000", token: userInfo.value?.aastoken })
  await setUserInfo();
  await afterLoginProcess();
  await getLicensePermissionsFunc();
  xietongRef.value?.getSwitchStatusFunc();
  communicationStore.h5permissions();
  setTimeout(() => {
    showToast('刷新成功');
    loading.value = false;
  }, 1000);
};
//获取license权限
async function getLicensePermissionsFunc() {
  const licensePermissionsRes = await h5Api.getLicensePermissions({});
  licensePermissions.value = licensePermissionsRes || {};
  console.log('licensePermissions', licensePermissionsRes);
  checkLicenseStatus(licensePermissionsRes);
}

async function kickoutMethod() {
  await getUserInfo();
  const aastokenNew = userInfo.value?.aastoken;
  if (!aastokenNew) {
    initData();
    return;
  }
  // await tokenLogin({ clientId: "CAPP-1000", token: aastokenNew })
  await afterLoginProcess();
}

async function tokenLogin(params) {
  await userStore.setUserInfo(params);
}
async function getLoginInfo() {
  return await userStore.getStoreUserInfo();
}
//防止重复登录，将之前登录和登录后的操作拆分开
async function afterLoginProcess() {
  const data = await getLoginInfo();
  // code 182：系统功能受限
  systemLimit.value = data?.code === 182;
  if (systemLimit.value) return;
  handlePageDisappear();
  cappPrivJson.value = data.roles?.[0]?.cappPrivJson || [];
  window.WeSpaceSDK.setStorage('cappPrivJson', JSON.stringify(cappPrivJson.value)); //智能体使用申请按钮需要
  accessTokenRef.value = data.accessToken;
  initData();
  await window.WeSpaceSDK.setStorage('wsAccessToken', data.accessToken);
  // 重连 WebSocket（不传参，统一从存储中获取）
  socketManage = await useSocketManage();
  await socketManage.initWebSocket();

  return data;
}

async function listenTask(val) {
  const BACKEND_GENERATION_TASK = await getGlobalsConfigByKey('BACKEND_GENERATION_TASK', true);
  if (BACKEND_GENERATION_TASK === 'true' || BACKEND_GENERATION_TASK === true) {
    console.log('新建任务直接走后台');
    return;
  }

  if (licensePermissions.value.LINKXBS !== '1' || licensePermissions.value.LINKXGCF !== '1') {
    console.log('license权限不足');
    return;
  }
  if (isExpiredTaskMessage(val.time)) {
    console.log('@协同岗时间大于当前时间超过1分钟，不创建待办任务。');
    return;
  }
  let params = {
    userId: val.sender,
    forceRemote: false,
  };
  let deptInfo = await window.WeSpaceSDK.getUserInfoByUserId(params);
  const deptments = deptInfo.userDepartments.filter((item) => item.isPrimary);
  if (deptments.length === 0) {
    console.log('任务创建者没有部门，无法创建任务');
    return;
  }
  const deptment = deptments[0];

  if (deptInfo) {
    const params = {
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
    };
    await taskSave(params);
  }
}
function removeAtFormat(text) {
  // 使用正则表达式匹配 [@数字:@任意字符 ] 这种格式并替换为空字符串
  return text.replace(/\[@\d+:@[^@]+ \]/g, '');
}

const sendNotificationToUser = debounce(async (val) => {
  let params = {
    userId: val.sender,
    forceRemote: false,
  };
  let deptInfo = await window.WeSpaceSDK.getUserInfoByUserId(params);
  if (deptInfo) {
    //添加智能眼镜消息发送
    const glassMessage = `${deptInfo.userName}在${
        val.groupName
    }群里@了我:${removeAtFormat(val.text)}`;
    console.log('[协同岗@我的消息]通知：上app通知栏，消息内容为：', glassMessage);
    sendNotification({
      text: glassMessage,
      msgid: val.msgid,
    });
  }
}, 500);
async function hasQuotationMssage(val, deptInfo, deptment) {
  if (!val.refmsgid) {
    console.log('回复内容没有引用消息，不予回复操作');
    return;
  }
  let taskInfo = await getTaskByMsgId({
    icsMsgId: val.refmsgid,
    postId: val.receiver,
  });
  if (!taskInfo?.taskId) {
    console.log('该引用消息没有对应任务，不予回复操作');
    return;
  }
  const userInfos = await groupApi.getColloration({
    userIds: [val.receiver],
  });
  let collorationInfo = {};
  if (userInfos && Array.isArray(userInfos)) {
    collorationInfo = userInfos[0] || {};
  }
  if (!val.text.includes('@')) {
    console.log('该消息没用@人，不予回复操作');
    return;
  }
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
}
async function noQuotationMssage(val, deptInfo, deptment) {
  //只有文本消息才算回复
  if (val.filetype !== 9) return;
  if (deptInfo) {
    const userInfos = await groupApi.getColloration({
      userIds: [val.receiver],
    });
    let collorationInfo = {}; //协同岗信息
    if (userInfos && Array.isArray(userInfos)) {
      collorationInfo = userInfos[0] || {};
    }
    console.log(collorationInfo, '============collorationInfo');
    const msgArr = {
      content: val.text,
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
}
async function quotationMssage(val) {
  let params = {
    userId: val.sender,
    forceRemote: false,
  };
  let deptInfo = await window.WeSpaceSDK.getUserInfoByUserId(params);
  const deptments = deptInfo.userDepartments.filter((item) => item.isPrimary);
  if (deptments.length === 0) {
    console.log('回复操作人没有部门，无法创建任务');
    return;
  }
  const deptment = deptments[0];
  // 添加开关判断
  const reokyDirectly = await getGlobalsConfigByKey('REPLY_DIRECTLY', true);
  const reokyDirectlyFlag = reokyDirectly === 'true' || reokyDirectly === true ? true : false;
  if (!reokyDirectlyFlag) {
    hasQuotationMssage(val, deptInfo, deptment);
  } else {
    noQuotationMssage(val, deptInfo, deptment);
  }
}
function isExpiredTaskMessage(time) {
  if (!time) return true;
  const currentTime = Date.now();
  return currentTime - Number(time) > 60 * 1000;
}
function convertJsonKeyQuotes(jsonFragment) {
  // 处理可能的外层双引号
  let cleanStr = jsonFragment.trim();
  if (cleanStr.startsWith('"') && cleanStr.endsWith('"')) {
    cleanStr = cleanStr.slice(1, -1);
  }

  // 匹配键值对模式
  const keyValuePattern = /"([^"]+)":\s*("([^"]*)"|([^,"]+))/g;

  return cleanStr.replace(keyValuePattern, (match, key, valueFull, valueStr, valueNonStr) => {
    // 处理键：用单引号包裹
    const formattedKey = `'${key}'`;

    // 处理值：保持原始引号或格式
    const formattedValue =
        valueStr !== undefined
            ? `'${valueStr}'` // 字符串值用单引号包裹
            : valueNonStr; // 数字、布尔等直接使用

    return `${formattedKey} : ${formattedValue}`;
  });
}

async function handleMessage(event) {
  console.log('全部消息', event);
  let data = event.data;
  console.log('收到消息：', data);
  if (!data) return;
  if (data.type === 'tab') {
    const str = JSON.stringify(data);
    const pureStr = str.slice(1, -1);
    const dealStr = convertJsonKeyQuotes(pureStr);
    console.log('dealStr---------', dealStr);
    const params = {
      appId: 'task',
      data: dealStr,
    };
    communicationStore.switchTab(params);
  } else if (data.type === 'xcx' && data.typeUrl) {
    jumpToFun(data.typeUrl);
  }
}
const jumpToFun = (data) => {
  let urlData = new URL(data);
  let obj = qs.parse(urlData.search.replace(/\?/g, ''));
  obj.pathname = urlData.pathname;
  let url = urlData.protocol + '//' + urlData.host;
  console.log(url, JSON.stringify(obj), '$$$$$$$$$$$$$$$$$$$$$$$$$');
  communicationStore.openUrlApp(url, obj);
};
async function initData() {
  try {
    // 初始化pageUrl store
    const url = await getGlobalsConfigByKey('h5', true);
    if (url && url !== '/') {
      iframeShow.value = true;
      // closePolling(); //关闭轮询
    } else {
      iframeShow.value = false;
      // openPolling(); //开启轮询
      getTaskStatusTotal();
      getTaskTypeTotal();
      getTaskList('待处理'); // 刷新任务列表
    }
    title.value = await getGlobalsConfigByKey('title');
    await xietongRef.value?.getSwitchStatusFunc();
    await getGroupList();
    heartbeatAddress();
    applicationStore.getBannerData();
    applicationStore.getCurrentApp(true);
    communicationStore.h5permissions();
    setIframeSrc(url);
  } catch (error) {
    console.log(error);
  }
}

// 点击进入ai
async function handleToAi() {
  await getAiBaseUrl();
  // 从全局配置读取交互模式，值为 session 时进入会话模式
    const interactionMode = (await getGlobalsConfigByKey('AI_AGENT_INTERACTION')) || '';

  // 根据模式判断跳转
  if (interactionMode === 'session') {
    // 会话模式：直接进入聊天页，并告知会话模式
      navigateToUrl('/pages/aiAssistant/index?sessionMode=true', null, 'noTitleStyle');
  } else {
    // 综合模式：直接进入聊天页面
      navigateToUrl('/pages/aiAssistant/index', null, 'noTitleStyle');
  }
}

function setIframeSrc(url) {
  const accountName = userInfo.value?.accountName;
  if (!accountName || !url) {
    console.log('--------accountName空', accountName, '--------url空', url);
    return;
  }
  const dom = document.getElementById('myIframe');
  const src = `${url}?accountName=${accountName}`;
  const message = userInfo.value;
  if (!dom) {
    addIframe(message, src);
    return;
  }
  dom.src = src;
  const targetDom = dom.contentWindow;
  targetDom.postMessage(message, src);
}

function addIframe(message, url) {
  const third = document.getElementById('third');
  console.log('-------------myIframe空');
  const iframe = document.createElement('iframe');
  iframe.src = url;
  iframe.style.width = '100%';
  iframe.style.height = '100%';
  iframe.frameborder = '0';
  iframe.id = 'myIframe';
  third.appendChild(iframe);
  const iframeDom = iframe.contentWindow;
  iframeDom.postMessage(message, url);
}

// 获取最新的aiBaseUrl
async function getAiBaseUrl() {
  try {
    const aiBaseUrl = (await getGlobalsConfigByKey('ai'))?.replace(/\/$/, '');
    await communicationStore.setStorage(
        'aiBaseUrl',
        unescape(encodeURIComponent(JSON.stringify(aiBaseUrl))),
    );
  } catch (e) {
    console.log(e);
  }
}

// 常用应用跳转
async function _handleClickApp(item) {
  try {
    const { name, params, url, appId, icon } = item;
    if (name === 'sdk') {
      await communicationStore.openUrl(url);
      return;
    }
    if (name === '设备调度') {
      const videoUrl = pageUrlStore.getVideoMonitorUrl;
      await communicationStore.openUrl(videoUrl, null, 'noTitleStyle');
    } else if (name === '位置共享') {
      handlePageDisappear();
      // 确保用户信息已获取
      if (!userInfo.value) {
        await getUserInfo();
      }
      locationShareFunc();
    } else if (name === '测试开关') {
      const url = `http://10.28.64.152:8001/linkx/h5portal/`;
      await communicationStore.openUrl(url);
    } else if (name === '测试yang') {
      const url = `http://172.28.89.159:8001/linkx/h5portal/pagesMain/yingYong`;
      await communicationStore.openUrl(url);
    } else if (name === '全国警信测试') {
      const url = `http://10.28.64.75:8001/linkx/h5portal/`;
      await communicationStore.openUrl(url);
    } else if (name === 'xl测试') {
      const url = `http://10.28.64.57:8001/linkx/h5portal/`;
      await communicationStore.openUrl(url);
      } else {
      await openAppByCheckPre(item);
    }
  } catch (error) {
    console.error('常用应用跳转失败:', error);
  }
}

// 使用防抖处理，防止重复点击（首次立即执行，300ms内的重复点击被忽略）
const handleClickApp = debounce(_handleClickApp, 300, true);
async function navigateToUrl(path, title = null, titleStyle = null) {
  if (path.includes('application')) {
    applicationStore.getCurrentApp();
  }
  const url = pageUrlStore.getFullPageUrl(path);
  console.log(url, '=====url页面跳转路径=====');
  await communicationStore.openUrl(url, title, titleStyle);
}

// 创群
async function createGroup() {
    if (showCollaborationFeatures.value) {
      navigateToUrl('/pages/customGroup', null, 'noTitleStyle');
    } else {
  // 定义输入框参数
  try {
    // 构建群组参数
    const groupParams = {
      groupName: '',
      groupType: 3,
      introduction: '',
      needSelectMember: true,
    };
    const groupDetail = await communicationStore.createGroup(groupParams);

    if (groupDetail && groupDetail.groupId) {
      // 构建打开聊天页面参数
      const chatParams = {
        id: groupDetail.groupId, // id(单聊id or 群聊id)
        category: 2, // 类型 1-单聊 2-群聊
      };

      //跳转聊天页面
      await communicationStore.sms(chatParams);
      setTimeout(() => {
        const params = {
          appId: 'ITEM_SESSION_PAGE',
        };
        communicationStore.switchTab(params);
      }, 1000);
    }
  } catch (error) {
    console.error('群组创建失败', error);
      }
  }
}

const _goCollaborativeGroup = async () => {
  try {
    await getUserInfo();
    const loginResult = getLoginInfo();
    // 检查token是否失效
    if (loginResult.code === 107) {
      showToast('登录已过期，请重新登录');
      return;
    }
    handlePageDisappear();
    await getGroupList();
    await navigateToUrl('/pages/archiveTable', null, 'noTitleStyle');
  } catch (error) {
    console.error('跳转协同群组失败:', error);
  }
};

// 使用防抖处理，防止重复点击（首次立即执行，300ms内的重复点击被忽略）
const goCollaborativeGroup = debounce(_goCollaborativeGroup, 300, true);

function onkeyCreateGroup() {
  navigateToUrl('/pages/createGroup?token=' + accessTokenRef.value, null, 'noTitleStyle');
}

// 获取用户信息
async function getUserInfo() {
  try {
    const res = await communicationStore.getUserInfo();
    if (res) {
      userInfo.value = res;
      // window.WeSpaceSDK.setStorage("aastoken", userInfo.value?.aastoken)
      console.log('打印用户信息', JSON.stringify(userInfo.value, null, 2));
    } else {
      console.error('获取用户信息失败或WeSpaceSDK不可用');
    }
  } catch (error) {
    console.error(`获取用户信息错误: ${error.message}`);
  }
}

const goTask = (type) => {
  let typeNum = 0;
  if (type === '进行中') {
    typeNum = 1;
  } else if (type === '已完成') {
    typeNum = 2;
  } else {
    typeNum = 0;
  }
  const params = {
    appId: 'ITEM_TASK_PAGE',
    data: JSON.stringify({ type: typeNum }),
  };
  communicationStore.switchTab(params);
};
// 通知提醒开发
const taskList = ref([]);
const taskNum = ref(0);
const getTaskList = async (status = '待处理') => {
  try {
    const res = await taskApi.getTaskPage({
      pageNum: 1,
      pageSize: 100,
      status,
      token: accessTokenRef.value,
    });
    taskList.value = res.records;
    taskNum.value = res.total;
  } catch (error) {
    console.log(error);
  }
};
const updateTaskStatus = async (item) => {
  if (item.status !== '待处理') return;
  const params = {
    taskNumber: item.number,
    action: 1,
    status: '进行中',
  };
  try {
    await taskApi.updateTaskStatusByTaskNumber({
      ...params,
      token: accessTokenRef.value,
    });
    // 处置成功后刷新任务列表
    getTaskList('待处理');
  } catch (e) {
    console.log(e);
  }
};
const goNoticeDetail = async (item) => {
  await updateTaskStatus(item);
  try {
    await communicationStore.openUrl(item.url);
  } catch (e) {
    console.log('打开页面失败');
  }
};

// 轮询代码start
// const shouldContinue = ref(false);
// const polling = async (pollingTime = 2000) => {
//   while (shouldContinue.value) {
//     try {
//       await getTaskList("待处理");
//       await new Promise((resolve) => setTimeout(resolve, pollingTime));
//     } catch (error) {
//       console.error(`请求 queryPageLoadingStatus 出错：`, error);
//       await new Promise((resolve) => setTimeout(resolve, pollingTime));
//     }
//   }
// };
// const openPolling = () => {
//   //如果已经开启了一个，不开启轮询
//   if (shouldContinue.value || !accessTokenRef.value) {
//     return;
//   }
//   shouldContinue.value = true;
//   polling();
// };
// const closePolling = () => {
//   shouldContinue.value = false;
// };
// 轮询代码end
//消息推送替换轮询start

let deboundTimer = null;
const tasksUpdate = () => {
  if (deboundTimer) {
    clearTimeout(deboundTimer);
  }
  deboundTimer = setTimeout(() => {
    getTaskList('待处理');
    getTaskStatusTotal();
    getTaskTypeTotal();
  }, 300);
};
//消息推送替换轮询end
// 通知提醒开发
const pushMessage = (message, type) => {
  const data = {
    url: message?.url,
    text: message?.title || type || '',
    contactId: userInfo.value.userid,
    // itemKey:"ITEM_POLICE_COOPERATION", //警务协作填写ITEM_POLICE_COOPERATION
    // category:1,
    // enableVoice:true,
    // enableVibration:true,
  };
  //重新获取轮播图
  applicationStore.getBannerData();
  communicationStore.showNotification(data);
};

const taskGlassProcess = (val, type) => {
  const idCard = userInfo.value?.idCard;
  const executors = val?.executors;
  const title = val?.name;
  const status = val?.status;
  if (executors?.length > 0 && idCard) {
    const index = executors?.findIndex((item) => item.idCard === idCard);
    if (index !== -1) {
      setTimeout(() => {
        const text = `任务${type},标题为${title},状态为${status}`;
        console.log('[任务推送]通知：上app通知栏，消息内容为：', text);
        sendNotification({ text });
      });
    }
  }
};

function setDebugQueryString() {
  try {
    const queryString = window?.location?.search;
    if (queryString) {
      const urlParams = new URLSearchParams(queryString);
      if (urlParams.get('debug') === 'true') {
        window?.WeSpaceSDK?.setStorage('debugQueryString', 'debug=true');
      } else {
        window?.WeSpaceSDK?.setStorage('debugQueryString', '');
      }
    } else {
      window?.WeSpaceSDK?.setStorage('debugQueryString', '');
    }
  } catch (error) {
    console.log(error);
  }
}
</script>

<style lang="scss" scoped>
.container {
  background-color: #f7f7f7;
  box-sizing: border-box;
  width: 100%;
  min-height: 100vh;
}

.top-nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #152584;
  padding-left: 16px;
  padding-right: 16px;
  white-space: nowrap;
  box-sizing: border-box;
  margin-bottom: 4px;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;

  .ai-img {
    width: 94px;
    height: 24px;
  }

  .title {
    height: 44px;
    line-height: 44px;
    color: #fff;
    font-size: 20px;
  }

  /* 平板适配 */
  @media screen and (min-height: 1200px) {
    .title {
      height: 88px;
      line-height: 88px;
      font-size: 0.8rem;
    }
  }

  @media screen and (min-height: 2001px) {
    .title {
      font-size: 0.6rem;
    }
  }

  .perch {
    width: 94px;
    height: 24px;
  }

  .right {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    white-space: nowrap;
    width: 94px;
  }
}

.right {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  white-space: nowrap;
}

.news-alerts {
  .news-item {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 1vw;

    .news-notice {
      display: inline-block;
      width: 22px;
      height: 22px;
    }
  }
}

.common-app,
.third-part {
  background-color: #fff;
  margin: 8px 16px 0;
  border-radius: 12px;
}

.third-part {
  height: 200px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;

  .section-title {
    font-size: 14px;
    font-weight: 500;
    line-height: 22px;
    height: 22px;
  }

  /* 平板适配 */
  @media screen and (min-height: 1200px) {
    .section-title {
      font-size: 0.8rem;
    }
  }

  @media screen and (min-height: 2001px) {
    .section-title {
      font-size: 0.6rem;
    }
  }

  .section-more {
    font-size: clamp(12px, 3vw, 14px);
    color: #999;
  }
}

/* 平板适配 */
@media screen and (min-height: 1200px) {
  .section-header {
    margin: 30px 0;
  }
}

.app-grid {
  :deep(.van-grid-item__content) {
    padding: 0px;
  }

  .app-item {
    margin-bottom: 12px;
  }

  .app-name {
    display: inline-block;
    font-size: 11px;
    color: #5a6383;
    font-weight: 500;
    line-height: 20px;
    margin-top: 2px;
    width: 100%;
    text-align: center;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  /* 平板适配 */
  @media screen and (min-height: 1200px) {
    .app-name {
      font-size: 0.8rem;
      line-height: 60px;
    }

    .app-item {
      margin-bottom: 60px;
    }
  }

  @media screen and (min-height: 2001px) {
    .app-name {
      font-size: 0.6rem;
    }
  }

  .app-empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;

    .empty-text {
      font-size: 14px;
      color: #898fa3;
    }

    .empty-btn {
      width: 70px;
      height: 28px;
      border-radius: 72px;
      background-color: #1e52f2;
      color: #fff;
      line-height: 28px;
      text-align: center;
      font-size: 12px;
      margin-top: 12px;
      margin-bottom: 20px;
    }

    /* 平板适配 */
    @media screen and (min-height: 1200px) {
      .empty-text {
        font-size: 0.8rem;
      }

      .empty-btn {
        width: 340px;
        height: 90px;
        line-height: 90px;
        font-size: 0.8rem;
        margin-top: 24px;
        margin-bottom: 80px;
      }
    }

    @media screen and (min-height: 2001px) {
      .empty-text,
      .empty-btn {
        font-size: 0.6rem;
      }
    }
  }
}

.collaborativeGroup {
  width: calc(100% - 32px);
  padding: 15px 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #e4e9f7;
  margin: 8px 16px;
  box-sizing: border-box;
  border-radius: 5px;

  .title-text {
    display: flex;
    align-items: center;

    img {
      margin-right: 4px;
    }

    span {
      font-weight: 500;
      font-size: 14px;
    }
  }

  .total-text {
    display: flex;
    flex: 1;
    justify-content: flex-start;
    margin-left: 10px;

    .collaborating,
    .archived {
      padding: 3px;
      background: #fafbfd;
      display: flex;
      align-items: center;
      color: #5a6383;
      margin-right: 10px;
      border-radius: 5px;

      p {
        font-size: 14px;
      }

      :deep(p) {
        margin-top: 0px;
        margin-bottom: 0px;
      }

      text {
        margin-left: 5px;
        font-weight: 800;
        font-size: 16px;
      }

      .green-text {
        color: #5dd76b;
      }
    }
  }

  @media screen and (min-height: 1200px) {
    .total-text {
      margin-left: 25px;
    }

    .collaborating,
    .archived {
      padding: 3px 35px;

      p {
        font-size: 16px;
      }

      text {
        font-size: 20px;
      }
    }
  }

  .review {
    font-size: 14px;
    display: flex;
    align-items: center;
    color: #264ed1;
  }
}

.footer {
  display: flex;
  justify-content: space-between;
  margin: 8px 16px;
  margin-bottom: 32px;

  :deep(.van-button) {
    padding: 0 !important;

    .van-button__content {
      width: 100%;

      .van-button__text {
        width: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
      }
    }
  }

  .create-group {
    background-color: #fff;
    border-radius: 12px;
    display: flex;
    align-items: center;
    width: 49%;
    height: 56px;
    justify-content: center;
    box-sizing: border-box;
    border: none;

    .group-text {
      line-height: 22px;
      margin-left: 10px;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      color: rgba(51, 51, 51, 1);
      font-weight: bold;

      .group-name {
        display: flex;
        align-items: center;
        font-size: 14px;
        font-weight: 700;

        .uv-icon {
          margin-top: 2px;
        }
      }
    }

    .group-btn {
      width: 70px;
      height: 28px;
      border-radius: 72px;
      background-color: #1e52f2;
      color: #fff;
      line-height: 28px;
      text-align: center;
      font-size: 12px;
    }
  }

  .one-key {
    background: rgba(212, 244, 236, 1);

    .group-text {
      color: rgba(3, 174, 137, 1);
      align-items: center;
      font-size: 14px;
      font-weight: 700;
    }
  }
}

.group-desc {
  text-align: center;
  font-size: 12px;
  line-height: 18px;
  font-weight: 400;
  color: #5a6383;
}

/* 平板适配 */
@media screen and (min-height: 1200px) {
  .group-desc {
    font-size: 0.6rem;
    margin-top: 40px;
  }
}

@media screen and (min-height: 2001px) {
  .group-desc {
    font-size: 0.4rem;
  }
}

:deep(.uni-modal__btn_primary) {
  color: #1e52f2 !important;
}

.task-index-entry {
  background-color: #fff;
  margin: 8px 16px;
  width: calc(100% - 32px);
  padding: 8px 12px;
  border-radius: 12px;

  .task-index-entry-header {
    display: flex;
    justify-content: space-between;

    .title {
      font-size: 14px;
      font-weight: 500;
      line-height: 22px;
      height: 22px;
    }

    .all {
      display: flex;
      align-items: center;

      span {
        font-size: 12px;
        font-weight: 500;
        height: 20px;
        margin-right: 6px;
        color: rgba(90, 99, 131, 1);
      }
    }
  }

  .task-index-entry-content {
    // margin-top: 8px;
    position: relative;

    .circle-data-content {
      height: 104px;
      display: flex;
      justify-content: space-between;
    }

    .entry-left-content {
      width: calc(100% - 79px);

      .left-content-top {
        display: flex;
        justify-content: space-between;
      }

      .left-content-bottom {
        margin-top: 8px;

        .progress-title {
          display: flex;
          align-items: flex-end;

          .icon {
            position: relative;
            bottom: 2px;
            width: 3px;
            height: 10px;
            border-radius: 2px;
            background: #5a6383;
            margin-right: 6px;
          }

          .title-text {
            font-size: 12px;
            font-weight: 400;
            color: rgba(90, 99, 131, 1);
          }

          .big-number {
            margin-left: 6px;
            position: relative;
            top: 2px;
            font-size: 18px;
            font-weight: 700;
            color: rgba(93, 215, 107, 1);
          }
        }

        .progress {
          margin-top: 8px;
        }
      }

      .entry-content-conmon-item {
        width: 56px;
        height: 50px;
        border-radius: 8px;
        border: 1px solid rgba(232, 232, 232, 1);
        display: flex;
        flex-direction: column;
        justify-content: center;

        .item-title {
          font-size: 12px;
          font-weight: 500;
          color: rgba(90, 99, 131, 1);
          text-align: center;
        }

        .itme-num {
          font-size: 16px;
          font-weight: 700;
          line-height: 22px;
          text-align: center;
        }

        .itme-num.color1 {
          color: rgba(51, 51, 51, 1);
        }

        .itme-num.color2 {
          color: rgba(255, 160, 92, 1);
        }

        .itme-num.color3 {
          color: rgba(93, 215, 107, 1);
        }

        .itme-num.color4 {
          color: rgba(134, 139, 152, 1);
        }
      }
    }

    .entry-right-chart {
      width: 64px;
      height: 104px;
      display: flex;
      flex-direction: column;
      align-content: center;
      justify-content: center;
      margin-left: 15px;

      .right-chart {
        width: 64px;
        height: 64px;
        position: relative;

        .number {
          position: absolute;
          left: 50%;
          top: 50%;
          z-index: 1;
          transform: translate(-50%, -50%);
          font-size: 12px;
          font-weight: 500;
          color: rgba(51, 51, 51, 1);
        }
      }

      .chart-text {
        font-size: 10px;
        font-weight: 400;
        line-height: 10px;
        color: rgba(90, 99, 131, 1);
        text-align: center;
        margin-top: 8px;
      }
    }
  }

  .first-swiper-page {
    display: flex;
    align-items: center;

    .left-menu {
      width: calc(100% - 84px);
      display: flex;
      flex-wrap: wrap;

      .menu-item {
        width: 50px;
        height: 48px;
        margin-right: calc((100% - 200px) / 3);

        .menu-item-content {
          display: flex;
          flex-direction: column;
          width: 50px;
          height: 48px;
          text-align: center;
          box-shadow: 0 1px 3px rgba(0, 0, 0, 0.07);
          margin-top: 4px;
          border-radius: 8px;

          .menu-value {
            font-size: 16px;
            font-weight: 700;
            line-height: 24px;
            color: rgba(51, 51, 51, 1);
          }

          .menu-title {
            font-size: 12px;
            font-weight: 400;
            line-height: 15px;
            color: rgba(90, 99, 131, 1);
          }
        }
      }

      .menu-item:nth-of-type(4n) {
        margin-right: 0px;
      }
    }

    .right-echart {
      width: 64px;
      height: 104px;
      margin-left: 20px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;

      .my-echart {
        width: 64px;
        height: 64px;
        position: relative;

        .number {
          position: absolute;
          left: 50%;
          top: 50%;
          z-index: 1;
          transform: translate(-50%, -50%);
          font-size: 14.22px;
          font-weight: 500;
          color: rgba(17, 110, 249, 1);
        }
      }

      .chart-text {
        font-size: 10px;
        font-weight: 400;
        color: rgba(90, 99, 131, 1);
        margin-top: 8px;
        text-align: center;
      }
    }
  }

  .swiper-dots {
    position: absolute;
    bottom: 0px;
    left: 50%;
    // 这里一定要注意兼容不然很可能踩坑
    transform: translate(-50%, 0);
    -webkit-transform: translate(-50%, 0);
    z-index: 999;
    display: flex;
    flex-direction: row;
    justify-content: center;

    .dot {
      width: 12px;
      height: 4px;
      transition: all 0.6s;
      background: rgba(217, 218, 222, 0.5);
    }

    .active {
      width: 12px;
      height: 4px;
      background: #4363e6;
    }
  }

  .swiper {
    width: 100%;
    height: 110px;
  }

  .swiper-empty {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;

    .empty-img {
      width: 80px;
      height: 80px;
    }

    .empty-text {
      font-size: 12px;
      font-weight: 400;
      letter-spacing: 0px;
      line-height: 17.38px;
      color: rgba(90, 99, 131, 1);
      text-align: center;
    }
  }
}

.left-title-arrow {
  display: flex;
  align-items: center;

  span {
    font-size: 12px;
    line-height: 21px;
    color: rgba(90, 99, 131, 1);
  }
}

.arrow-right {
  width: 7px;
  height: 7px;
  border-right: 1px solid rgb(96, 98, 102);
  border-bottom: 1px solid rgb(96, 98, 102);
  transform: rotate(-45deg);
}

.banner {
  margin-left: 16px;
  width: calc(100% - 32px);
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

.notice {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;

  .notice-title {
    display: flex;
    flex-direction: column;

    span {
      font-size: 12px;
      font-weight: bold;
      position: relative;
      font-style: italic;
    }

    .color-blue {
      color: rgba(41, 107, 234, 1);
    }

    .color-black {
      color: rgba(0, 0, 0, 1);
    }
  }

  .notice-center {
    margin-left: 4px;
    padding-left: 4px;
    flex: 1;
    border-left: 1px solid #dedede;

    .notice-swipe {
      height: 18px;
      // line-height: 18px;
    }

    :deep(.van-notice-bar) {
      height: 16px;
      padding: 0px;
    }

    .notice-text {
      display: flex;
      align-items: center;

      span {
        font-size: 12px;
        flex: 1;
        font-weight: 400;
        color: #333333;
        white-space: nowrap;
        /* 禁止换行 */
        overflow: hidden;
        /* 隐藏超出部分 */
        text-overflow: ellipsis;
        /* 超出显示省略号 */
      }
    }
  }
}

.taskBadge {
  display: inline-block;
  height: 16px;
  min-width: 16px;
  border-radius: 8px;
  background-color: rgba(245, 83, 83, 1);
  line-height: 16px;
  text-align: center;
  padding: 0px 4px;
  margin-left: 4px;
  font-size: 10px;
  font-weight: 400;
  color: #fff;
  position: relative;
  top: -1px;
}
</style>
