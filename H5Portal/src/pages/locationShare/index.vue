<template>
  <div class="location-share">
    <BottomMap
      mapId="locationShareMap"
      :center="mapCenter"
      :show-tool="!invaildGroup"
      @mapReady="handleMapReady"
    />
    <view
      class="status-bar-mask"
      :style="{
        height: paddingTop + 'px',
      }"
    ></view>
    <div class="close-btn" @click.stop="handleClose">
      <img src="@/assets/images/map/close_share.png" />
    </div>
    <div class="person-list" v-if="personList.length > 0">
      <div class="lists">
        <div class="person-item" v-for="(item, index) in personList" :key="index">
          <!-- 兼容后端字段名为 tumbAvatar / thumbAvatar 的情况 -->
          <img
            v-if="item.tumbAvatar || item.thumbAvatar"
            :src="item.tumbAvatar || item.thumbAvatar"
          />
          <img v-else src="@/assets/images/map/person_header.png" />
        </div>
      </div>

      <div class="desc">{{ personCount }}人正在共享位置</div>
    </div>

    <!-- 正在讲话提示框 -->
    <div 
      v-if="speakingUsers.length > 0 && personList.length > 0" 
      class="speaking-tip"
    >
      <div class="speaking-tip-left">
        <span class="speaking-names">{{ speakingUsersText }}</span>
        <span class="speaking-action">正在讲话</span>
        <img src="@/assets/svg/chat.svg" class="speaking-icon" />
      </div>
      <img src="@/assets/svg/chat-menu-down.svg" class="speaking-menu-icon" />
    </div>

    <div class="set-switch" v-if="!invaildGroup && ownerFlag && intercomEnabled">
      <van-switch
        v-model="switchContainer"
        :disabled="isDisabled"
        @change="handleXietongChange"
        size="18"
        active-color="#264ED1"
        inactive-color="#676970"
      />
      <span class="switch-desc" :class="{ 'open-btn': !switchContainer }"
        >{{ switchContainer ? '关闭' : '开启' }}对话</span
      >
    </div>

    <!-- 倒计时选择器 -->
     <div class="time-set" v-if="!invaildGroup">
        <CountdownPicker
          v-if="showDateFeature && show331Feature"
          title="位置共享定时"
          :api="fetchTimerOptions"
          @countdown-end="handleCountdownEnd"
        />
     </div>
    <div
      v-if="!invaildGroup && switchContainer && intercomEnabled"
      class="talk-btn"
      :class="{ 'disabled-btn': !switchContainer }"
      @touchstart.prevent="handleTalkStart"
      @touchend.prevent="handleTalkEnd"
      @mousedown.prevent="handleTalkStart"
      @mouseup.prevent="handleTalkEnd"
      @mouseleave="handleTalkEnd"
    >
      <img :src="currentTalkImg" />
    </div>

    <!-- PC端不支持 或 无效群组时的遮罩提示层 -->
    <div v-if="isPC || invaildGroup" class="no-content-mask">
      <view
        :style="{
          width: '100%',
          height: paddingTop + 'px',
          'background-color': '#F5F5F5',
        }"
      ></view>
      <!-- 顶部导航栏 -->
      <view class="top-nav-bar">
        <van-icon name="arrow-left" :size="adaptationSize.iconSize" color="#333" @click="goBack" />
        <text class="title">位置共享</text>
        <view class="right"></view>
      </view>
      <div class="content">{{ isPC ? '电脑端暂不支持' : (invaildMsg || '当前位置共享已结束') }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { showConfirmDialog, showToast } from 'vant';
  import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
  import { useRoute, onBeforeRouteLeave } from 'vue-router';

  // @ts-ignore vue default export shim
  import { addSelfToMap, addOrUpdatePersonToMap } from './tool.js';

  import on_talk from '@/assets/images/map/on_talk.png';
  import talking from '@/assets/images/map/talking.png';
  import {
    addUser,
    getGroupInfo,
    setTalk,
    getTalkStatus,
    groupHeartbeat,
    getLocationShare,
    exitLocationShareCombined,
  } from '@/common/api/group.js';
  import { tokenLogin } from '@/common/api/h5.js';
  import { getBaseUrl } from '@/common/config.js';
  import DC from '@/common/network/DC.js';
  import { useSocketManage } from '@/common/network/ws.js';
  import BottomMap from '@/pages/map/bottomMap.vue';
  import CountdownPicker from './components/CountdownPicker.vue';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { useLocationShareStore } from '@/stores/locationShare.js';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';
  import { useUserStore } from '@/stores/user.js';
  import { getDeviceType } from '@/common/utils/index.js'
  import { useCachedGlobalsConfig } from '@/hooks/useCachedGlobalsConfig';

  const userStore = useUserStore();
  const locationShareStore = useLocationShareStore();
  const communicationStore = useCommunicationStore();
  const { adaptationSize } = useDeviceAdapter();
  const { getConfig, getConfigs } = useCachedGlobalsConfig();
  const userInfo = ref<any>(null);
  let socketManage = null;
  const route = useRoute();
  const personList = ref<any>([]);
  const switchContainer = ref<boolean>(true);
  const countdownTime = ref<number>(0); // 倒计时时间（秒）
  /** 标记当前开关值是否来自 WebSocket/DC 推送，用于避免同步时再次触发 setTalk */
  const isUpdateFromWs = ref<boolean>(false);
  const isDisabled = ref<boolean>(false);
  const isTalking = ref<boolean>(false);
  const personType = ref<string | null>(null);
  const groupId = ref<string | number>('');
  const shareId = ref<string | number | null>(null);
  const nowLocation = ref<any>({}); //当前自己经纬度
  const invaildGroup = ref<boolean>(false); //是否无效群组
  const invaildMsg = ref<string>('');
  const groupInfo = ref<any>({}); //警务协同动态群组信息
  const initialMapCenter = ref<[number, number] | undefined>(undefined); // 初始地图中心点，只设置一次
  const mapCenter = computed(() => {
    // 如果已经设置了初始中心点，后续不再更新
    if (initialMapCenter.value) {
      return initialMapCenter.value;
    }
    // 只在第一次获取到经纬度时设置初始中心点
    const lng = nowLocation.value?.longitude;
    const lat = nowLocation.value?.latitude;
    if (lng && lat) {
      initialMapCenter.value = [Number(lng), Number(lat)];
      return initialMapCenter.value;
    }
    // 未获取到经纬度时返回 undefined，让子组件使用默认中心点
    return undefined;
  });
  
  //当前用户是否为群组
  const ownerFlag = computed(() => {
    const ownerId = groupInfo.value.ownerId;
    const userId = userInfo.value?.userid;
    if (ownerId == null || userId == null) return false;
    return String(ownerId) === String(userId);
  });

  const personCount = computed(() => personList.value.length);
  const currentTalkImg = computed(() => (isTalking.value ? talking : on_talk));
  const mapInstance = ref<any>(null); // 地图实例
  const mockTimer = ref<number | null>(null); // 模拟位置定时器
  // 记录已添加到地图的人员图层ID集合（用于清理不在新列表中的旧点位）
  const addedPersonLayerIds = ref<Set<string>>(new Set());
  // 避免多次执行关闭逻辑
  const hasClosed = ref<boolean>(false);
  const paddingTop = ref(0);
  let heartbeatTimer = null;
  // 331特性展示
  let show331Feature = ref(false)
  // 倒计时选择器显示开关
  let showDateFeature = ref(false)
  const intercomEnabled = ref<boolean>(false); // 对讲功能开关，默认显示
  // 标记"是否正在真正卸载页面"
  const isPageUnloading = ref(false);
  // 位置共享加入群组最大限制人数
  const LOCATION_SHARE_MAX_USERS = 10;
  // 是否为PC端
  const isPC = ref<boolean>(false);

  // 正在讲话的人员名称列表
  // - 有值：显示提示框
  // - 空数组：隐藏提示框
  // 通过 SDK 的 onTaken/onIdle 回调实时更新
  const speakingUsers = ref<string[]>([]);

  // 计算属性：生成人员名称文本（用'、'分割）
  const speakingUsersText = computed(() => {
    return speakingUsers.value.join('、');
  });

  watch(
    () => userInfo.value,
    async (newVal) => {
      if (!newVal) return;
      await openWebSocket({ clientId: 'CAPP-3000', token: newVal.aastoken });
    },
    {
      deep: true,
      immediate: true,
    },
  );

  onMounted(async () => {
    // 小程序关闭 H5 时回调：无法等待异步接口，使用 beacon 发送合并退出请求并同步调用 SDK 退出与取消订阅
    window.WeSpaceSDK?.onH5Close?.(() => {
      sendExitLocationShareBeacon();
      locationShareStore.dynamicGroupAutoQuit({ groupId: groupId.value });
      unSubscribeDevices();
    });

    window.WeSpaceSDK?.onH5Max?.(() => {
      // 最大化时重连 WebSocket + 刷新群组数据
      reconnectWebSocket();
      showPage();
    });

    window.WeSpaceSDK?.onH5Min?.(() => {
      // 最小化时关闭 WebSocket，避免后台接收事件
      handlePageDisappear();
    });
    // 判断是否为PC端
    isPC.value = getDeviceType() === 'pc';
    showPage();
    getFeatureConfigs();
    window.addEventListener('pagehide', pageHideHandler);
    window.addEventListener('beforeunload', beforeUnloadHandler);
    document.addEventListener('visibilitychange', visibilityChangeHandler);
    DC.on('ICP_DYNAMIC_GROUP', 'MEMBER_ADD', personChange);
    DC.on('ICP_DYNAMIC_GROUP', 'MEMBER_DEL', personChangeDel);
    DC.on('ICP_DYNAMIC_GROUP', 'GROUP_BUTTON', buttonChange);
    // 息屏锁屏状态改变
    window.WeSpaceSDK?.onVisibleChange(async (val) => {
      if (val === true || val === 'true') {
        // 页面变为可见时重连 WebSocket + 刷新数据
        reconnectWebSocket();
        showPage();
      }
    });
  });

  onUnmounted(async () => {
    clearInterval(heartbeatTimer);
    // 组件卸载时移除监听
    await safeHandleClose();
    window.removeEventListener('pagehide', pageHideHandler);
    window.removeEventListener('beforeunload', beforeUnloadHandler);
    document.removeEventListener('visibilitychange', visibilityChangeHandler);
    handlePageDisappear();
  });

  // 路由返回/离开当前页面时，主动调用 handleClose
  onBeforeRouteLeave(async () => {
    await safeHandleClose();
  });

  // 重连 WebSocket（最小化/息屏/切换后台恢复时调用）
  function reconnectWebSocket() {
    if (userInfo.value?.aastoken) {
      openWebSocket({ clientId: 'CAPP-3000', token: userInfo.value.aastoken });
    }
  }

  async function showPage() {
    initData();
  }

  // 只执行一次的安全关闭方法
  async function safeHandleClose() {
    if (hasClosed.value) return;
    handlePageDisappear();
  }

  // pagehide：页面隐藏时触发（含切后台），只关闭 WS，不退出位置共享
  // 兼容 App 内 H5 左滑返回等直接关闭 WebView 的场景
  const pageHideHandler = async () => {
    isPageUnloading.value = true;
    await safeHandleClose();
  };
  // beforeunload：页面真正销毁时触发（杀掉应用/关闭标签页），发送退出 beacon
  const beforeUnloadHandler = () => {
    sendExitLocationShareBeacon();
  };
  const visibilityChangeHandler = async () => {
    if (document.hidden) {
      await safeHandleClose();
      // 右滑返回：pagehide 已经处理了，这里不重复执行
      if (isPageUnloading.value) return;
    } else {
      // 亮屏/切回前台：重置卸载标记 + 重连 WebSocket + 刷新数据
      isPageUnloading.value = false;
      reconnectWebSocket();
      showPage();
    }
  };
  const goBack = async () => {
    await communicationStore.close();
  };
  const getPaddingTop = async () => {
    paddingTop.value = await communicationStore.fetchStatusBarHeight();
  };
  const buttonChange = (data) => {
    console.log(data.groupId, groupId.value, '====groupId.value');

    if (data.groupId !== groupId.value) return;
    const flag = data.status === 'true' || data.tatus === true;
    if (flag === switchContainer.value) return;
    isUpdateFromWs.value = true;
    switchContainer.value = flag;
  };
  const personChange = (data) => {
    //已经是无效群组则不更新，因为新建的动态群组id可能不变
    if (invaildGroup.value) {
      return;
    }
    if (data.groupId !== groupId.value) {
      return;
    }
    getGroupInfoFunc(false);
  };
  const personChangeDel = (data) => {
    const { userId } = data;
    if (data.groupId !== groupId.value) {
      return;
    }

    const ownerId = groupInfo.value.ownerId;
    // ownerId 为空时 includes('') === true，会导致误判退出
    if (ownerId && userId.includes(ownerId)) {
      if (ownerId === userInfo.value?.userid) {
        colsePage();
      } else {
        showToast('群主结束了位置共享');
        setTimeout(() => colsePage(), 3000);
      }
      return;
    }
    personChange(data);
  };
  async function initData() {
    getPaddingTop();
    await getParams();
    await getUserInfo();
    if (shareId.value) {
      await checkShareStatus();
      if (invaildGroup.value) return;
    }
    await getGroupInfoFunc(true);
    getTalkStatusFunc();
    setheartbeatTimer();
    // 仅影响位置共享页内的 set-switch 和 talk-btn，不影响应用入口
    try {
      const INTERCOM_FUNCTION_SIGN = await getConfig('INTERCOM_FUNCTION_SIGN', true);
      intercomEnabled.value = INTERCOM_FUNCTION_SIGN === true || INTERCOM_FUNCTION_SIGN === 'true';
    } catch (e) {
      // 读取失败时默认显示对讲功能
      intercomEnabled.value = true;
    }
    // 申请话权结果
    window.WeSpaceSDK.onFloorRequest(({ data }) => {
      console.log(data, '====申请话权结果=======');
    });
    // 话权释放回调（申请话权失败时会触发）
    window.WeSpaceSDK.onGroupRelease((res) => {
      console.log(res, '====onGroupRelease=======');
      const releaseReason = res?.data?.releaseReason;
      if (!releaseReason || releaseReason === 'CALL_RELEASE') return;
      const reasonMap: Record<string, string> = {
        FLOOR_ERROR: '抢权异常，平台关闭组呼',
        BUSINESS_CONFLICT: '业务冲突，无法发起语音',
        BUSINESS_CONFLICT_IN_CALL: '业务冲突，无法发起语音',
        OUT_LICENSE: 'License过期，无法发起语音',
        ACCEPT_FAILED: '接收失败，无法发起语音',
        PORT_CREATR_TIME_OUT: '网络异常，无法发起语音',
      };
      const msg = reasonMap[releaseReason] || '语音发起失败';
      showToast(msg);
    });
    // window.WeSpaceSDK.onIdle((res) => {
    //   // {groupId,speaker} 群组id，主讲号码
    //   console.log(res, '====监听讲话onIdle=======');
    // });
    //订阅别人位置变更通知
    window.WeSpaceSDK?.onReceiveGisInfo((res) => {
      // {isdn, location}  用户isdn，位置longitude, latitude 123,12
      const { success, data } = res;
      if (success) {
        updatePersonLocation(data);
      }
    });
    // 监听有人开始讲话
    window.WeSpaceSDK.onTaken((res) => {
      // {success, data: {groupNumber, speaker, speakerName}} 群组号，讲话人ID，讲话人名称
      console.log(res, '====监听讲话onTaken=======');

      // 解构数据
      const { success, data } = res;
      if (success && data?.speakerName) {
        speakingUsers.value = [data.speakerName];
      }
    });
    // 监听说话停止（群组空闲）
    window.WeSpaceSDK.onIdle((res) => {
      // 群组空闲状态改变
      console.log(res, '====监听讲话onIdle=======');

      // 清空正在讲话的人员列表
      speakingUsers.value = [];
    });
  }
  function setheartbeatTimer() {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer);
    }
    // 立即执行一次心跳
    groupHeartbeat({ groupId: groupId.value, userId: userInfo.value.userid });
    // 然后每30秒执行一次
    heartbeatTimer = setInterval(() => {
      groupHeartbeat({ groupId: groupId.value, userId: userInfo.value.userid });
    }, 30 * 1000);
  }

  //获取当前按钮状态
  async function getTalkStatusFunc() {
    const res = await getTalkStatus(groupId.value);
    const flag = res === 'true' || res === true;
    // 与当前一致时不改值、不设标志，避免 isUpdateFromWs 一直为 true 导致用户点击被误判
    if (flag === switchContainer.value) return;
    switchContainer.value = flag;
  }
  // 页面消失时执行的方法
  const handlePageDisappear = async () => {
    if (socketManage) {
      await socketManage.closeWebSocket();
      socketManage = null;
    }
  };
  async function openWebSocket(params) {
    // 先关闭旧连接，避免重复
    if (socketManage) {
      await socketManage.closeWebSocket();
      socketManage = null;
    }
    const res: any = await tokenLogin(params);
    if (res.accessToken) {
      socketManage = await useSocketManage();
      await socketManage.initWebSocket(res.accessToken);
      // 小程序读不到H5页面存的token, 这里存token以防报错重连ws使用
      const res2 = await userStore.getStoreUserInfo();
    }
  }
  //获取ICS群组信息
  async function getGroupInfoFunc(init) {
    const res: any = await getGroupInfo(groupId.value);
    if (!res) {
      invaildGroup.value = true;
      personList.value = [];
      return;
    }
    groupInfo.value = res || {};
    invaildGroup.value = false;

    // 保存旧的人员列表
    const oldPersonList = [...personList.value];
    const newPersonList = res.members || [];

    // 对比新旧列表，找出已退出的人员
    if (mapInstance.value && oldPersonList.length > 0) {
      const oldIsdnSet = new Set(oldPersonList.map((item: any) => item.isdn).filter(Boolean));
      const newIsdnSet = new Set(newPersonList.map((item: any) => item.isdn).filter(Boolean));

      // 找出在新列表中不存在的旧成员
      const removedIsdns: string[] = [];
      oldIsdnSet.forEach((isdn) => {
        if (!newIsdnSet.has(isdn)) {
          removedIsdns.push(isdn);
        }
      });

      // 从地图上移除已退出人员的图层
      removedIsdns.forEach((isdn) => {
        try {
          mapInstance.value.removeLayer(isdn);
          addedPersonLayerIds.value.delete(isdn);
        } catch (error) {
          // ignore
        }
      });
    }

    // 更新人员列表
    personList.value = newPersonList;

    //订阅之前先取消之前的订阅
    await unSubscribeDevices();
    subscribeDevices();
    if (init) {
      getGisInfo();
      await dynamicGroupAutoJoinFunc();
    }
  }
  //位置变更后，更新位置
  async function updatePersonLocation(data: any) {
    const { list, code } = data;
    if (code === 0 && Array.isArray(list)) {
      // 通过 isdn 去重，保留每个 isdn 的最后一项
      const uniqueMap = new Map();
      list.forEach((item: any) => {
        if (item.isdn) {
          uniqueMap.set(item.isdn, item);
        }
      });
      // 将去重后的数据转换为数组
      const uniqueList = Array.from(uniqueMap.values());
      // 更新地图上人员的位置
      if (mapInstance.value) {
        // 创建 personList 中所有成员的 isdn 集合，用于快速查找
        const personListIsdnSet = new Set(
          personList.value.map((person: any) => person.isdn).filter(Boolean),
        );

        for (const item of uniqueList) {
          if (!item.isdn) continue;

          // 判断人员列表中是否有该成员，没有则跳过更新
          if (!personListIsdnSet.has(item.isdn)) {
            continue;
          }

          // 从 personList 中查找对应的用户信息，获取完整的用户数据（包括头像）
          const personInfo = personList.value.find((person: any) => person.isdn === item.isdn);

          // 处理位置数据：可能是 item.location 对象，或者直接在 item 上
          const location = item.location.split(',');

          if (location[0] && location[1]) {
            // 如果是自己，更新自身当前位置（但不更新地图中心点）
            if (userInfo.value?.isdn && item.isdn === userInfo.value.isdn) {
              nowLocation.value = {
                longitude: Number(location[0]),
                latitude: Number(location[1]),
              };
              // 注意：不再通过更新 nowLocation 来驱动 mapCenter 变化
              // mapCenter 只在初始化时设置一次，后续保持不变
            }
            // 更新人员的位置（addMarkerCluster 内部会先清除旧图层）
            // 优先使用 personList 中的完整用户信息，包括头像
            await addOrUpdatePersonToMap({
              mapInstance: mapInstance.value,
              personInfo: {
                isdn: item.isdn,
                name: personInfo?.userName || personInfo?.name || item.name,
                // 优先使用 personList 中的头像（兼容 tumbAvatar 和 thumbAvatar）
                thumbAvatar:
                  personInfo?.tumbAvatar ||
                  personInfo?.thumbAvatar ||
                  personInfo?.avatar ||
                  item.thumbAvatar ||
                  item.tumbAvatar ||
                  item.avatar,
              },
              location: {
                longitude: location[0],
                latitude: location[1],
              },
            });
            // 记录已添加的图层ID
            addedPersonLayerIds.value.add(item.isdn);
          }
        }

        // 清除那些不在 personList 中的旧点位（只要 personList 中存在就不删除，即使本次 data 中没有位置更新）
        const toRemove: string[] = [];
        addedPersonLayerIds.value.forEach((isdn) => {
          // 只删除那些不在 personList 中的点位
          if (!personListIsdnSet.has(isdn)) {
            toRemove.push(isdn);
          }
        });

        // 移除旧图层
        toRemove.forEach((isdn) => {
          try {
            mapInstance.value.removeLayer(isdn);
            addedPersonLayerIds.value.delete(isdn);
            console.log('已清除旧点位，图层ID:', isdn);
          } catch (error) {
            console.log('清除旧点位失败:', isdn, error);
          }
        });
      }
    }
  }
  async function getUserInfo() {
    const res = await communicationStore.getUserInfo();
    if (res) {
      userInfo.value = res;
      // 获取到用户信息后，尝试添加到地图
      tryAddSelfToMap();
    }
  }
  //加入动态群组
  async function dynamicGroupAutoJoinFunc() {
    // 检查自己是否已在人员列表中
    const myUserId = userInfo.value?.userid;
    const isSelfInList = personList.value.some(
      (item: any) => String(item.userId) === String(myUserId)
    );

    // 只有当自己不在列表中时，才检查人数上限
    if (!isSelfInList) {
      const currentMemberCount = personList.value.length;
      if (currentMemberCount >= LOCATION_SHARE_MAX_USERS) {
        invaildGroup.value = true;
        invaildMsg.value = `位置共享群组人数已达上限（最多${LOCATION_SHARE_MAX_USERS}人）`;
        showToast(`位置共享群组人数已达上限（最多${LOCATION_SHARE_MAX_USERS}人）`);
        return;
      }
    }

    // 第一个动态群组接口
    const res = await locationShareStore.dynamicGroupAutoJoin({ groupId: groupId.value });
    console.log(res, '===========加入群组返回数据========');

    const { code } = res;
    if (code === 0) {
      const waitStartTime = new Date().toISOString();
      console.log(`[${waitStartTime}] WespaceSDK.dynamicGroupAutoJoin接口调用成功1111111111`);
      // 等待 2s 后再调用激活ICP-D通话接口
      await new Promise((resolve) => setTimeout(resolve, 2000));
      const waitEndTime = new Date().toISOString();
      console.log(`[${waitEndTime}] WespaceSDK.joinDynamicGroup接口开始调用2222222222`);
      // 激活ICP-D通话接口
      const joinRes = await locationShareStore.joinDynamicGroup({ groupId: groupId.value });
      console.log('joinDynamicGroup接口返回:', joinRes, '===========听这个群组返回数据========');
      // 加入ics群组
      const index = personList.value.findIndex((item) => item.userId === userInfo.value.userid);
      if (index > -1) {
        // 已经在成员列表中，也要设置初始化完成
        return;
      }
      const res1 = await addUser({
        groupId: groupId.value,
        members: [
          {
            groupId: groupId.value,
            userId: userInfo.value.userid,
            userName: userInfo.value.username,
            isdn: userInfo.value.isdn,
            tumbAvatar: userInfo.value.thumbAvatar || '',
          },
        ],
      });
      getGroupInfoFunc(false);
    } else {
      invaildGroup.value = true;
      invaildMsg.value = '没有位置共享权限';
    }
  }
  //申请话语权
  async function floorRequest() {
    const { data } = await locationShareStore.floorRequest({
      groupId: groupId.value,
    });
    console.log(`====${data}=====`);
  }
  //释放话语权
  async function floorRelease() {
    const { data } = await locationShareStore.floorRelease({
      groupId: groupId.value,
    });
    console.log(`====${data}=====`);
  }
  //获取自己当前经纬度
  async function getGisInfo() {
    try {
      const res = await locationShareStore.getGisInfo();

      // 检查返回数据是否有效
      if (!res || !res.longitude || !res.latitude) {
        showToast('获取位置信息失败，请检查定位权限');
        return;
      }

      nowLocation.value = res || {};
      // 获取到经纬度后，尝试添加到地图
      tryAddSelfToMap();
    } catch {
      showToast('获取位置信息失败，请稍后重试');
    }
  }
  //位置变更去监听
  async function unSubscribeDevices() {
    const ueList = personList.value.map((item: any) => item.isdn).join(',');
    await locationShareStore.unSubscribeDevices(ueList);
  }
  // 地图准备就绪回调
  function handleMapReady(mapContext: any) {
    mapInstance.value = mapContext;
    // 地图准备好后，尝试添加到地图
    tryAddSelfToMap();
  }

  // 检查是否可以添加图层，如果可以则添加
  function tryAddSelfToMap() {
    if (
      mapInstance.value &&
      userInfo.value &&
      nowLocation.value?.latitude &&
      nowLocation.value?.longitude
    ) {
      addSelfToMap({
        mapInstance: mapInstance.value,
        userInfo: userInfo.value,
        nowLocation: nowLocation.value,
      });
    }
  }
  //发起GIS订阅
  async function subscribeDevices() {
    const ueList = personList.value.map((item: any) => item.isdn).join(',');
    if (!ueList) {
      return;
    }
    try {
      await locationShareStore.subscribeDevices(ueList);
    } catch (e) {
      // ignore
    }
  }

  //退出位置共享
  async function handleClose() {
    let message = '确认退出当前位置共享？';
    if (ownerFlag.value) {
      message = '您为当前群主，确认退出并结束当前位置共享？';
    }
    showConfirmDialog({
      title: '提示',
      message,
    })
      .then(async () => {
        console.log('提示删除=====');

        colsePage();
      })
      .catch(() => {});
  }

async function fetchTimerOptions(): Promise<{ label: string; value: number }[]> {
   // 获取倒计时时间设置options
  const DEFAULT_OPTIONS: { label: string; value: number }[] = [
    { label: '30 分钟', value: 1800 },
    { label: '60 分钟', value: 3600 },
    { label: '90 分钟', value: 5400 },
    { label: '120 分钟', value: 7200 },
    { label: '关闭定时', value: -1 },
  ]

  let LOCATION_SHARE_TIME: string | null = null

  try {
    LOCATION_SHARE_TIME = await getConfig('LOCATION_SHARE_TIME', true);
  } catch (err) {
    return DEFAULT_OPTIONS
  }

  // ✅ 去掉多余的 new Promise 包裹，async 函数直接 return
  if (LOCATION_SHARE_TIME && typeof LOCATION_SHARE_TIME === 'string') {
    const timeValues = LOCATION_SHARE_TIME
      .split(',')
      .map(item => item.trim())
      .filter(item => item && !isNaN(Number(item)))
      .map(Number)

    // ✅ 解析结果为空也降级（配置值全部非法时）
    if (!timeValues.length) {
      return DEFAULT_OPTIONS
    }

    return [
      ...timeValues.map(minutes => ({ label: `${minutes} 分钟`, value: minutes * 60 })),
      { label: '关闭定时', value: -1 },
    ]
  }

  // 配置为空或格式错误，使用默认值
  return DEFAULT_OPTIONS
}

async function getFeatureConfigs() {
  try {
    // 参数2, 是否刷新缓存, 默认为false
    const { SHOW_331_FEATURE, SHARE_LOCATION_TIME } = await getConfigs(
      ['SHOW_331_FEATURE', 'SHARE_LOCATION_TIME'], 
      true
    );
    show331Feature.value = String(SHOW_331_FEATURE) == 'true';
    showDateFeature.value = String(SHARE_LOCATION_TIME) == 'true';
  } catch (error) {
    show331Feature.value = false;
    showDateFeature.value = false;
  }
}

  async function handleCountdownEnd() {
    console.log("倒计时结束");
    invaildGroup.value = true;
    invaildMsg.value = '位置共享定时已结束';
    await colsePage();
  }

  // 防止重复发送退出 beacon（onH5Close 和 beforeunload 可能同时触发）
  let beaconSent = false;

  // 页面被外部关闭时（小程序 onH5Close / pagehide / beforeunload）无法等待异步接口，
  // 使用 fetch keepalive 发送退出位置共享合并请求，确保页面关闭后请求仍能可靠送达
  function sendExitLocationShareBeacon() {
    if (beaconSent) return;
    if (!shareId.value || !groupId.value || !userInfo.value?.userid) return;
    beaconSent = true;
    const url = `${getBaseUrl()}/proxy/icp/v1/dynamic-group/exit-location-share`;
    const token = userStore.userInfo?.accessToken;
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `token ${token}`;
    const body = JSON.stringify({
      shareId: shareId.value,
      groupId: groupId.value,
      userId: userInfo.value.userid,
      isOwner: ownerFlag.value ? 1 : 0,
      exitType: ownerFlag.value ? 1 : 0,
      exitDesc: ownerFlag.value ? '群主结束了位置共享' : '用户主动退出',
      status: ownerFlag.value ? 'true' : undefined,
    });
    fetch(url, { method: 'POST', headers, body, keepalive: true }).catch((e) => {
      // ignore
    });
  }

  async function colsePage() {
    handlePageDisappear();

    // 调用合并后的退出位置共享接口（后端整合：群主设置按钮状态 + 退出位置共享 + 删除ICS群组成员）
    if (shareId.value && groupId.value && userInfo.value?.userid) {
      try {
        await exitLocationShareCombined({
          shareId: shareId.value,
          groupId: groupId.value,
          userId: userInfo.value.userid,
          isOwner: ownerFlag.value ? 1 : 0,
          exitType: ownerFlag.value ? 1 : 0,
          exitDesc: ownerFlag.value ? '群主结束了位置共享' : '用户主动退出',
          status: ownerFlag.value ? 'true' : undefined,
        });
      } catch (e) {
        console.error('[位置共享] 调用退出位置共享合并接口失败', e);
      }
    }
    const res = await locationShareStore.dynamicGroupAutoQuit({
      groupId: groupId.value,
    });
    console.log(res, '=====dynamicGroupAutoQuit');
    
    unSubscribeDevices();
    await communicationStore.closePage();
  }
  function getParams() {
    // 解析 URL 参数 type
    const queryType = route.query.groupId;
    if (queryType) {
      groupId.value = Array.isArray(queryType) ? queryType[0] : queryType;
      console.log('获取到的 groupId 参数:', groupId.value);
    } else {
      console.log('无法获取群组参数');
    }
    // 获取本次位置共享唯一标识
    const queryShareId = route.query.shareId;
    if (queryShareId) {
      shareId.value = Array.isArray(queryShareId) ? queryShareId[0] : queryShareId;
      console.log('获取到的 shareId 参数:', shareId.value);
    }
  }
  async function checkShareStatus() {
    try {
      const res = await getLocationShare(shareId.value);
      if (!res) {
        invaildGroup.value = true;
        invaildMsg.value = '当前位置共享已结束';
        return;
      }
      const { status, udcGroup } = res;
      if (status === 0) {
        invaildGroup.value = true;
        invaildMsg.value = '当前位置共享已结束';
        return;
      }
      if (udcGroup && String(udcGroup) !== String(groupId.value)) {
        invaildGroup.value = true;
        invaildMsg.value = '当前位置共享已结束';
        return;
      }
    } catch (e) {
      invaildGroup.value = true;
      invaildMsg.value = '当前位置共享已结束';
    }
  }
  async function handleXietongChange() {
    // 若开关值来自 WebSocket 同步，只更新了 UI，不再请求 setTalk，避免状态被重新设置
    console.log(isUpdateFromWs.value, '=====isUpdateFromWs.value');

    if (isUpdateFromWs.value) {
      isUpdateFromWs.value = false;
      return;
    }
    const res = await setTalk({
      groupId: groupId.value,
      status: switchContainer.value ? 'true' : 'false',
    });
    console.log(res, '=======设置对话按钮状态返回数据========');
  }
  async function handleTalkStart() {
    const res = await communicationStore.getIcpUserStatus();
    console.log(res, '=======获取用户状态返回数据========');
    if (res === 'offline') {
      showToast('通信状态异常，请稍后重试');
      return;
    }

    if (!switchContainer.value) return;
    // 先申请话语权
    await floorRequest();
    // 申请话语权后再设置说话状态
    isTalking.value = true;
  }
  function handleTalkEnd() {
    // 释放话语权
    floorRelease();
    isTalking.value = false;
  }

</script>

<style scoped lang="scss">
  .location-share {
    width: 100%;
    height: 100%;
    position: relative;

    .status-bar-mask {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      background-color: rgba(0, 0, 0, 0.5);
      z-index: 1;
    }

    .close-btn {
      width: 28px;
      height: 28px;
      position: absolute;
      top: 30px;
      left: 16px;
      z-index: 3;
      background: rgba(61, 61, 61, 1);
      border-radius: 50%;
      display: flex;
      justify-content: center;
      align-items: center;
      img {
        width: 16px;
        height: 16px;
      }
    }

    .person-list {
      height: 118px;
      opacity: 1;
      background: rgba(0, 0, 0, 0.5);
      width: 100%;
      position: absolute;
      top: 0;
      z-index: 1;

      .lists {
        display: flex;
        align-items: center;
        justify-content: center;
        flex-wrap: nowrap;
        overflow-x: auto;
        overflow-y: hidden;
        margin-top: 50px;
        /* 左侧多预留空间，避免被关闭按钮遮挡 */
        padding: 0 32px 0 32px;
        box-sizing: border-box;
        -ms-overflow-style: none; // IE/Edge
        scrollbar-width: none; // Firefox
        &::-webkit-scrollbar {
          display: none; // WebKit
        }

        .person-item {
          flex: 0 0 auto;
          &:not(:last-child) {
            margin-right: 8px;
          }
          img {
            width: 32px;
            height: 32px;
            border-radius: 50%;
          }
        }
      }

      .desc {
        font-size: 12px;
        font-weight: 400;
        letter-spacing: 0px;
        line-height: 17.38px;
        color: rgba(255, 255, 255, 1);
        text-align: center;
        margin-top: 4px;
      }
    }

    .speaking-tip {
      position: absolute;
      top: 125px; // person-list的高度
      width: 80%;
      // height: 40px;
      left: 10%;
      // transform: translateX(-50%);
      z-index: 2;

      display: flex;
      // align-items: center;
      justify-content: space-between;

      padding: 8px 16px;
      border-radius: 100px;
      box-shadow: 0px 2px 10px 0px rgba(0, 0, 0, 0.2);
      background: rgba(61, 61, 61, 0.9);

      font-size: 12px;
      line-height: 18px;
      color: #fff;

      .speaking-tip-left {
        display: flex;
        // align-items: center;
        gap: 4px;
        margin-top: 2px;

        .speaking-names {
          // 人员名称
        }

        .speaking-action {
          color: rgba(57, 206, 75, 1); // 绿色
          flex: none;
        }

        .speaking-icon {
          width: 16px;
          height: 16px;
          flex: none;
        }
      }

      .speaking-menu-icon {
        width: 22px;
        height: 22px;
        margin-left: 12px;
        flex: none;
      }
    }

    .set-switch {
      position: absolute;
      left: 16px;
      bottom: 36px;
      z-index: 2;
      display: flex;
      align-items: center;
      flex-direction: column;

      .switch-desc {
        font-size: 12px;
        font-weight: 400;
        letter-spacing: 0px;
        line-height: 17.38px;
        color: rgba(44, 110, 255, 1);
      }

      .open-btn {
        color: #676970;
      }
    }
    .time-set {
      position: absolute;
      right: 16px;
      top: 134px;
      z-index: 9999;
    }
    .talk-btn {
      position: absolute;
      bottom: 20px;
      left: calc(50% - 40px);
      width: 80px;
      height: 80px;
      display: flex;
      justify-content: center;
      align-items: center;
      border-radius: 50%;
      overflow: hidden;
      touch-action: none;

      img {
        width: 80px;
        height: 80px;
      }
    }

    .disabled-btn {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .no-content-mask {
      position: absolute;
      inset: 0;
      background-color: rgba(15, 14, 14, 0.7);
      z-index: 1001;

      .content {
        text-align: center;
        color: #fff;
        margin-top: 200px;
      }
    }
  }
  .top-nav-bar {
    display: flex;
    background-color: #f5f5f5;
    align-items: center;
    justify-content: space-between;
    padding-left: 16px;
    padding-right: 16px;

    .title {
      color: rgba(3, 8, 26, 1);
      font-size: 18px;
      height: 44px;
      line-height: 44px;
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
    .right {
      width: 18px;
    }
  }
</style>
