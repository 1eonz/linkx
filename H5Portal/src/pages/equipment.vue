<template>
  <view class="container">
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
      <text class="title">设备通讯录</text>
      <view class="right"></view>
    </view>
    <view class="equipment-box">
      <view class="search-input">
        <van-search
          v-model="keywords"
          ref="uvSearchRef"
          placeholder="请输入用户姓名"
          show-action
          @update:modelValue="handleChange"
          class="my-custom-search"
        >
          <template #action>
            <div @click="handleSearch">搜索</div>
          </template>
        </van-search>
      </view>
      <!-- 分类 -->
      <view class="tabs">
        <view
          v-for="(item, index) in tabList"
          :key="index"
          @click="tabChange(item)"
          class="tab-item"
          :class="{ 'tab-item-active': current === item.type }"
          >{{ item.name }}</view
        >
      </view>
      <!-- 组织/层级-面包屑 -->
      <view class="breadcrumb" v-if="breadcrumbList.length > 0 && !showSearch">
        <view
          v-for="(item, index) in breadcrumbList"
          :key="index"
          class="breadcrumb-item"
          @click="handleBreadcrumbClick(index)"
        >
          <van-icon
            v-if="index === 0"
            name="arrow"
            :size="adaptationSize.iconSizeBread"
            color="rgba(38, 99, 255, 1)"
            style="margin-top: 2px"
            @click="handleBreadcrumbClickTop()"
          />
          <view
            :class="{
              'active-breadcrumb': index === breadcrumbList.length - 1,
            }"
            >{{ item.name }}</view
          >
          <van-icon
            v-if="index < breadcrumbList.length - 1"
            name="arrow"
            :size="adaptationSize.iconSizeBread"
            :color="index === breadcrumbList.length - 1 ? '#909399' : 'rgba(38, 99, 255, 1)'"
            style="margin-top: 2px"
          />
        </view>
      </view>
      <div class="list-scroll-view" style="height: calc(100vh - 200px); overflow-y: auto">
        <van-list
          v-model:loading="listLoading"
          :finished="finished"
          finished-text="没有更多了"
          @load="loadMoreData"
        >
          <!-- 列表内容：搜索结果 或 部门/设备 -->
          <div v-show="showSearch" class="list-content">
            <!-- 搜索结果列表项 -->
            <view class="apply-auth-content" v-if="!hasAIAuth && applyAuthStatus !== 1 && false">
              <img class="apply-img" :src="emptyIcon" width="88px" height="88px" />
              <text class="text">暂无数据权限</text>
              <van-button
                v-if="applyAuthStatus === 0"
                class="apply-btn"
                type="primary"
                size="small"
              >
                审批中
              </van-button>
              <van-button
                v-else
                class="apply-btn"
                type="primary"
                size="small"
                @click="openApplyForm"
              >
                申请
              </van-button>
            </view>
            <template v-else>
              <div v-for="(item, index) in searchList" :key="'device-' + index" class="list-item">
                <view class="status" :class="getState(item)"></view>
                <img
                  class="device-icon"
                  :src="getDeviceIcon(item)"
                  :width="adaptationSize.width"
                  :height="adaptationSize.height"
                />
                <view class="device-text">
                  <text class="device-id">{{ item?.NodeName || item.NodeAlias }}</text>
                  <text class="department">{{ item?.Department }}</text>
                </view>
                <EquipmentAbility :item="item" :department="item?.Department" :current="current" />
              </div>
            </template>
            <!-- 空白占位: 预留tab高度防止被tab遮挡 -->
            <div class="list-bottom-placeholder"></div>
          </div>

          <div v-show="!showSearch" class="list-content">
            <view class="apply-auth-content" v-if="!hasAIAuth && applyAuthStatus !== 1 && false">
              <img class="apply-img" :src="emptyIcon" width="88px" height="88px" />
              <text class="text">暂无数据权限</text>
              <van-button
                v-if="applyAuthStatus === 0"
                class="apply-btn"
                type="primary"
                size="small"
              >
                审批中
              </van-button>
              <van-button
                v-else
                class="apply-btn"
                type="primary"
                size="small"
                @click="openApplyForm"
              >
                申请
              </van-button>
            </view>
            <template v-else>
              <!-- 部门列表项 -->
              <div
                v-for="(item, index) in departmentList"
                :key="'dept-' + index"
                class="list-item"
                @click="handleDepartmentClick(item)"
              >
                <img
                  class="device-icon"
                  :src="getDeviceIcon(item)"
                  :width="adaptationSize.width"
                  :height="adaptationSize.height"
                />
                <text class="device-id">{{ item.NodeName || item.LevelName || item.name }}</text>
              </div>

              <!-- 设备列表项 -->
              <div
                v-for="(item, index) in equipmentList"
                :key="'device-' + index"
                class="list-item"
              >
                <view class="status" :class="getState(item)"></view>
                <img
                  class="device-icon"
                  :src="getDeviceIcon(item)"
                  :width="adaptationSize.width"
                  :height="adaptationSize.height"
                />
                <text class="device-id">{{ item?.NodeName || item.NodeAlias }}</text>
                <EquipmentAbility
                  :item="item"
                  :current-department-id="currentDepartmentId"
                  :breadcrumb-list="breadcrumbList"
                  :current="current"
                  :used-icon="usedIcon"
                  :current-name="currentName"
                />
              </div>
            </template>
            <!-- 空白占位: 预留tab高度防止被tab遮挡 -->
            <div class="list-bottom-placeholder"></div>
          </div>
        </van-list>
      </div>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { showToast } from 'vant';
  import { ref, reactive, nextTick, onMounted, onBeforeUnmount, computed, watch } from 'vue';

  import {
    getCameraPoints,
    getCameraLevel,
    getEquipmentPoints,
    getDepartment,
    getDeviceLayerList,
  } from '@/common/api/map.js';
  import EquipmentAbility from '@/components/equipment-ability/equipment-ability.vue';
  import emptyIcon from '@/static/equipment/emptyIcon.png';
  import monitorIcon from '@/static/equipment/monitor.png';
  import organizationIcon from '@/static/equipment/organization.png';
  import recorderIcon from '@/static/equipment/recorder.png';
  import terminalIcon from '@/static/equipment/terminal.png';
  import personIcon from '@/static/equipment/person.png';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { usePageUrlStore } from '@/stores/pageUrl.js';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';
  import { usePointLoader } from '@/pages/map/usePointLoader.js';
  import { transformImageUrl } from '@/utils/imgUrlParse.js';
  const applyPermissionList = ref([]);
  const pageUrlStore = usePageUrlStore();
  const listLoading = ref(false);
  const finished = ref(false);
  // 使用设备适配
  const { adaptationSize } = useDeviceAdapter();

  const communicationStore = useCommunicationStore();

  let inputEl = null;

  const uvSearchRef = ref();
  const paddingTop = ref(0);
  const tabList = ref<any[]>([]);
  const current = ref('');
  const currentName = ref('');
  const isdnTypeId = ref(''); //当前tab的id
  const usedIcon = ref(''); //当前tab的图标
  const breadcrumbList = ref<any[]>([]);
  const loadStatus = ref<'loadmore' | 'nomore' | 'loading' | 'fail'>('loadmore');
  const currentDepartmentId = ref('');
  const departmentList = ref<any[]>([]);
  const equipmentList = ref<any[]>([]);
  const isLoading = ref(false);
  const showSearch = ref(false);
  const keywords = ref('');
  const searchList = ref<any[]>([]);
  const searchLoadStatus = ref<'loadmore' | 'nomore' | 'loading' | 'fail'>('loadmore');
  const scrollTop = ref(0);
  // const categoryBase: Record<string, number> = {};

  const applyAuthStatus = ref(3); //状态0审批中 1同意 2拒绝 3未申请
  const pageNum = ref(1);
  const pageSize = 100;
  const permissionsArr = computed(() => communicationStore.permissionsArr);
  const userInfo = computed(() => communicationStore.userInfo);
  watch(
    () => userInfo.value?.idCard,
    async (idCard) => {
      if (!idCard) return;
      await communicationStore.h5permissions();
      hasAIAuth.value = permissionsArr.value.includes('video');
    },
    { immediate: true },
  );

  const goBack = async () => {
    await communicationStore.close();
  };
  const { getEquipmentType } = usePointLoader();
  const deviceLayers = ref<any[]>([]);
  const fetchDeviceLayers = async () => {
    try {
      const res = await getDeviceLayerList();
      const layers = res || [];
      if (Array.isArray(layers) && layers.length > 0) {
        deviceLayers.value = layers.filter(
          (item) => item.isShow === 1 && getEquipmentType(item) !== 'person',
        );
        tabList.value = deviceLayers.value.map((item) => {
          const imgUrl = item.iconUri ? transformImageUrl(`/map-api${item.iconUri}`) : '';
          const type = getEquipmentType(item);
          if (type === 'camera' || type === 'fixedCamera') {
            item.iconUri = imgUrl || monitorIcon;
          } else if (type === 'gatewayProxy') {
             item.iconUri = imgUrl || personIcon;
          } else if (type === 'surveillance') {
             item.iconUri = imgUrl || terminalIcon;
          } else {
            item.iconUri = imgUrl || recorderIcon;
          }
            return {
            type,
            ...item,
          };
        });
        if (tabList.value.length > 0 && !current.value) {
          current.value = tabList.value[0].type;
          isdnTypeId.value = tabList.value[0].id;
          usedIcon.value = tabList.value[0].iconUri;
          currentName.value = tabList.value[0].name;
        }
      }
    } catch (error) {
      console.error('获取配置失败:', error);
    }
  };
  const handleBreadcrumbClick = async (index: number) => {
    if (index === breadcrumbList.value.length - 1) return;
    breadcrumbList.value = breadcrumbList.value.slice(0, index + 1);
    currentDepartmentId.value = breadcrumbList.value[index].departmentId;
    initLoad();
  };

  const handleBreadcrumbClickTop = () => {
    currentDepartmentId.value = '';
    breadcrumbList.value = [];
    initLoad();
  };
  const judgeAuth = (item) => {
    //权限判断
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
    if (applyListInit.indexOf(item.type) != -1) {
      applyAuthStatus.value = 0; //审核中
    } else if (applyListACCEPT.indexOf(item.type) != -1) {
      applyAuthStatus.value = 1; //同意
    } else if (applyListREFUSE.indexOf(item.type) != -1) {
      applyAuthStatus.value = 2; //拒绝
    } else {
      applyAuthStatus.value = 3; //其他情况，如未申请
    }
    //权限判断
  };
  const tabChange = async (item: any) => {
    
    if (current.value === item.type) return;
    judgeAuth(item);
    currentDepartmentId.value = '';
    breadcrumbList.value = [];
    isdnTypeId.value = item.id;
    if (keywords.value) {
      current.value = item.type;
      handleSearch();
      return;
    }
    current.value = item.type;
    currentName.value = item.name;
    usedIcon.value = item.iconUri;
    initLoad();
  };


  const loadMoreData = async () => {
    if (searchLoadStatus.value === 'nomore') {
      finished.value = true;
    }
    if (showSearch.value) {
      if (searchLoadStatus.value === 'loading' || searchLoadStatus.value === 'nomore') {
        listLoading.value = false;
        return;
      }
      pageNum.value = pageNum.value + 1;
      loadMoreSearchList();
    } else {
      if (loadStatus.value === 'loading' || loadStatus.value === 'nomore') {
        listLoading.value = false;
        return;
      }
      pageNum.value = pageNum.value + 1;
      await loadDepartmentAndDevices();
    }
    listLoading.value = false;
  };

  const loadDepartmentAndDevices = async () => {
    if (isLoading.value && loadStatus.value === 'loading') {
      return;
    }
    // 如果是首次加载，先清空列表
    if (pageNum.value === 1) {
      departmentList.value = [];
      equipmentList.value = [];
    }
    isLoading.value = true;
    loadStatus.value = 'loading';
    try {
      const activeTab = tabList.value.find((t) => t.type === current.value);
      const isCamera = activeTab?.category === '1';
      if (isCamera) {
        const orgLevelArr = await getCameraLevelData();
        const deviceList = await getCameraData();
        departmentList.value = [...orgLevelArr];
        equipmentList.value = [...equipmentList.value, ...deviceList];
        queryStateData(deviceList);
      } else {
        departmentList.value = await getDepartmentData();
        const deviceList = await getEquipmentData();
        equipmentList.value = [...equipmentList.value, ...deviceList];
        queryStateData(deviceList);
      }
    } catch (e) {
      loadStatus.value = 'fail';
    }
    isLoading.value = false;
  };

  const getDeviceIcon = (item: any) => {
    if (Number(item.NodeType) === 0) {
      return organizationIcon;
    }
    return usedIcon.value || monitorIcon;
  };

  const handleDepartmentClick = (item: any) => {
    breadcrumbList.value.push({
      name: item.NodeName || item.LevelName || item.name,
      departmentId: item.NodeDN || item.LevelId || item.departmentId,
      type: item.type || '',
    });
    currentDepartmentId.value = item.NodeDN || item.LevelId || item.departmentId;
    initLoad();
  };

  const initLoad = () => {
    pageNum.value = 1;
    equipmentList.value = [];
    departmentList.value = [];
    scrollTop.value = 1;
    loadStatus.value = 'loadmore';
    finished.value = true;
    listLoading.value = false;
    nextTick(() => {
      scrollTop.value = 0;
    });
    setTimeout(() => {
      loadDepartmentAndDevices();
    }, 0);
  };

  const queryStateData = async (data: any[]) => {
    if (data.length === 0) {
      return;
    }
    const activeTab = tabList.value.find((t) => t.type === current.value);
    const equipmentArr =
      data?.map((item) => {
        return item?.CameraDN || item?.NodeDN;
      }) || [];
    const equipmentParams = {
      type: activeTab?.category === '1' ? '0' : '1',
      list: equipmentArr.join(';'),
    };
    if (equipmentArr.length) {
      try {
        await communicationStore.queryOnlineState(equipmentParams);
      } catch (e) {
        // 错误处理
      }
    }
  };

  const getState = (item: any) => {
    const online = item?.online;
    if ('13'.includes(online)) {
      return 'online';
    } else if ('02'.includes(online)) {
      return 'offline';
    }
    return 'offline';
  };

  const setMergedListState = (data: any[]) => {
    const stateMap = new Map();
    data?.forEach((x) => stateMap.set(x.isdn, x.online));
    equipmentList.value.forEach((item) => {
      const key = item.NodeDN || item.CameraDN || '';
      if (stateMap.has(key)) item.online = stateMap.get(key);
    });
  };

  const setSearchListState = (data: any[]) => {
    searchList.value?.forEach((item) => {
      data?.forEach((x) => {
        if (item?.NodeDN === x.isdn) {
          item.online = x.online;
        }
      });
    });
  };

  const handleChange = (val: string) => {
    if (!val) {
      showSearch.value = false;
      handleClear();
    }
  };

  const handleClear = () => {
    showSearch.value = false;
    keywords.value = '';
    searchList.value = [];
    pageNum.value = 1;
    searchLoadStatus.value = 'loadmore';
    handleBreadcrumbClickTop();
  };

  const handleSearch = () => {
    if (!keywords.value) return;
    searchList.value = [];
    showSearch.value = true;
    isLoading.value = true;
    pageNum.value = 1;
    searchLoadStatus.value = 'loading';
    const activeTab = tabList.value.find((t) => t.type === current.value);
    const isCamera = activeTab?.category === '1';
    if (isCamera) {
      handleSearchCamera();
    } else {
      handleSearchContact();
    }
  };

  const loadMoreSearchList = () => {
    isLoading.value = true;
    searchLoadStatus.value = 'loading';
    const activeTab = tabList.value.find((t) => t.type === current.value);
    const isCamera = activeTab?.category === '1';
    if (isCamera) {
      handleSearchCamera();
    } else {
      handleSearchContact();
    }
  };

  const handleSearchCamera = async () => {
    const params = {
      pageNum: pageNum.value,
      pageSize,
      search: keywords.value,
      onlineFirst: true,
      isdnTypeId: isdnTypeId.value,
    };
    try {
      const res = await getCameraPoints(params);
      console.log('获取摄像头', '/icp/camera', res);
      const { records } = res || {};
      if (records.length === 0) {
        if (pageNum.value === 1) {
          searchList.value = [];
        }
        searchLoadStatus.value = 'nomore';
        finished.value = true;
        return;
      }
      const cameraData: any[] = [];
      records?.forEach((item: any) => {
        const { isdn, alias, levelName, name } = item;
        cameraData.push({
          NodeName: name || alias,
          NodeDN: isdn,
          Department: levelName,
          PTZControl: '1',
          category: 1,
        });
      });
      searchList.value = [...searchList.value, ...cameraData];
      queryStateData(cameraData);
      searchLoadStatus.value = records.length < pageSize ? 'nomore' : 'loadmore';
      if (searchLoadStatus.value === 'nomore') {
        finished.value = true;
      }
    } catch (e) {
      console.error('获取摄像头失败', '/icp/camera', e);
      searchLoadStatus.value = 'fail';
    }
    isLoading.value = false;
  };

  const handleSearchContact = async () => {
    const params = {
      pageNum: pageNum.value,
      pageSize,
      search: keywords.value,
      onlineFirst: true,
      isdnTypeId: isdnTypeId.value,
    };
    try {
      const res = await getEquipmentPoints(params);
      console.log('获取设备', '/icp/user', res);
      const { records } = res || {};
      const filtered = records || [];
      if (filtered.length === 0) {
        if (pageNum.value === 1) {
          searchList.value = [];
        }
        searchLoadStatus.value = 'nomore';
        finished.value = true;
        return;
      }
      const contactdata: any[] = [];
      filtered?.forEach((item: any) => {
        const { isdn, alias, category, departmntName, name } = item;
        const obj = {
          NodeName: name || alias,
          NodeDN: isdn,
          Department: departmntName,
          category: category,
          iconUri: usedIcon.value,
        };
        contactdata.push(obj);
      });
      searchList.value = [...searchList.value, ...contactdata];
      queryStateData(contactdata);
      searchLoadStatus.value = filtered.length < pageSize ? 'nomore' : 'loadmore';
    } catch (e) {
      console.error('获取设备失败', '/icp/user', e);
      searchLoadStatus.value = 'fail';
    }
    isLoading.value = false;
  };

  const getIcpUserStatus = async () => {
    const res = await communicationStore.getIcpUserStatus();
    showMsgToast(res);
  };

  const getPaddingTop = async () => {
    paddingTop.value = await communicationStore.fetchStatusBarHeight();
  };

  const showMsgToast = (data: any) => {
    if (data === 'offline') {
      showToast('网络开小差啦，请稍后重试');
    }
  };

  const handleKeyDown = (e) => {
    if (e.keyCode === 13) {
      handleSearch();
    }
  };
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
    //     applyPermissionList.value = res?.records || [];
    //     const item = tabList.find((item) => item.type === current.value);
    //     judgeAuth(item);
    //   })
    //   .catch((err) => {
    //     console.log(err);
    //   });
  }

  onMounted(async () => {
    // getCappPrivJson();
    getApplyPermission();
    getPaddingTop();
    await communicationStore.getUserInfo();
    await fetchDeviceLayers();
    initLoad();
    getIcpUserStatus();
    window.WeSpaceSDK.onStateValue((val: string) => {
      const data = JSON.parse(val);
      console.log('onStateValue', data);
      if (data.length) {
        if (showSearch.value) {
          setSearchListState(data);
        } else {
          setMergedListState(data);
        }
      }
    });
    window.WeSpaceSDK.onIcpUserStatusChange((val: string) => {
      const data = JSON.parse(val);
      showMsgToast(data);
    });
    window.WeSpaceSDK.onVisibleChange(() => {
      setTimeout(() => {
        //重新获取按钮权限
        getApplyPermission();
      }, 300);
    });
    setTimeout(() => {
      // 通过 DOM 查询 uv-search 内部 input
      // 这里假设 uv-search 内部有 input 元素
      inputEl = uvSearchRef.value?.$el?.querySelector('input');
      if (inputEl) {
        inputEl.addEventListener('keydown', handleKeyDown);
      }
    }, 500); // 延迟，确保 DOM 渲染完成
  });

  onBeforeUnmount(() => {
    // window.WeSpaceSDK.removeStateValue();
    if (inputEl) {
      inputEl.removeEventListener('keydown', handleKeyDown);
    }
  });
  async function navigateToUrl(path, title = null, titleStyle = null) {
    const url = pageUrlStore.getFullPageUrl(path);
    await communicationStore.openUrl(url, title, titleStyle);
  }
  function openApplyForm() {
    const activeTab = tabList.value.find((t) => t.type === current.value);
    const item = { name: activeTab?.name || '', index: '' };
    navigateToUrl(
      '/pages/aiAssistant/applyPermissionForm?formType=1&params=' + JSON.stringify(item),
      null,
      'noTitleStyle',
    );
  }

  //判断智能体是否需要申请,是否有AI权限
  const hasAIAuth = ref(false);
  // 获取摄像头层级
  async function getCameraLevelData() {
    const params = {
      higherLevelNumber: currentDepartmentId.value,
    };
    try {
      const data = await getCameraLevel(params);
      console.log('获取摄像头层级', '/icp/camera-level/tree/privs', data);
      const res =
        data?.map((i: any) => ({
          type: 'level',
          NodeType: 0,
          NodeDN: i?.levelNumber,
          NodeName: i?.nodeName,
        })) || [];
      return res;
    } catch (e) {
      console.error('获取摄像头层级失败', '/icp/camera-level/tree/privs', e);
      return [];
    }
  }

  // 获取摄像头
  async function getCameraData() {
    const params = {
      pageNum: pageNum.value,
      pageSize,
      onlineFirst: true,
      cameraLevel: currentDepartmentId.value === '' ? '-1' : currentDepartmentId.value,
      isdnTypeId: isdnTypeId.value,
    };
    try {
      const res = await getCameraPoints(params);
      console.log('获取摄像头', '/icp/camera', res);
      const { records } = res || {};
      if (records.length < pageSize) {
        loadStatus.value = 'nomore';
        finished.value = true;
      } else {
        loadStatus.value = 'loadmore';
        finished.value = false;
      }
      if (records.length === 0) {
        return [];
      }
      const result =
        records?.map((i: any) => ({
          NodeType: 1,
          NodeName: i?.name || i?.alias,
          NodeDN: i?.isdn,
          Department: i?.levelName,
          PTZControl: '1',
          state: i?.statusValue,
          category: 1,
          iconUri: usedIcon.value,
        })) || [];
      return result;
    } catch (e) {
      console.error('获取摄像头失败', '/icp/camera', e);
      return [];
    }
  }

  // 获取设备部门
  async function getDepartmentData() {
    const params = {
      upperDepartmentId: currentDepartmentId.value,
    };
    try {
      const data = await getDepartment(params);
      console.log('获取部门', '/icp/department/tree/privs', data);
      const res =
        data?.map((i: any) => ({
          type: 'org',
          NodeType: 0,
          NodeDN: i?.departmentid,
          NodeName: i?.departmentname,
        })) || [];
      return res;
    } catch (e) {
      console.error('获取部门失败', '/icp/department/tree/privs', e);
      return [];
    }
  }

  // 获取设备
  async function getEquipmentData() {
    const params = {
      pageNum: pageNum.value,
      pageSize,
      onlineFirst: true,
      isdnTypeId: isdnTypeId.value,
      departmentId: currentDepartmentId.value === '' ? '-1' : currentDepartmentId.value,
    };
    try {
      const res = await getEquipmentPoints(params);
      console.log('获取设备', '/icp/user', res);
      const { records } = res || {};
      const filtered = records || []
      if (filtered.length < pageSize) {
        loadStatus.value = 'nomore';
        finished.value = true;
      } else {
        loadStatus.value = 'loadmore';
        finished.value = false;
      }
      if (filtered.length === 0) {
        return [];
      }
      const result =
        filtered?.map((i: any) => ({
          NodeType: 1,
          NodeName: i?.name || i?.alias,
          NodeDN: i?.isdn,
          Department: i?.departmntName,
          state: i?.statusValue,
          category: i?.category,
          iconUri: usedIcon.value,
        })) || [];
      return result;
    } catch (e) {
      console.error('获取设备失败', '/icp/user', e);
      return [];
    }
  }
</script>

<style lang="scss" scoped>
  .container {
    display: flex;
    flex-direction: column;
    height: 100vh;
    background-color: #f5f5f5;
    position: relative;
  }
  // 添加空白占位样式
  .list-bottom-placeholder {
    height: 80px;
    width: 100%;
    pointer-events: none;
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
  .equipment-box {
    width: 100%;
    height: 100%;
    background: #fff;
    border-radius: 18px 18px 0 0;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    z-index: 1;
  }

  .tabs {
    width: 100%;
    height: 31px;
    display: flex;
    box-sizing: border-box;
    border-bottom: 1px solid rgba(239, 239, 239, 1);
    padding-left: 16px;
    overflow-x: auto;
    white-space: nowrap;
    -webkit-overflow-scrolling: touch;

    &::-webkit-scrollbar {
      display: none;
    }

    .tab-item {
      font-size: 16px;
      font-weight: 400;
      letter-spacing: 0px;
      line-height: 31px;
      color: rgba(134, 139, 152, 1);
      margin-right: 24px;
      flex-shrink: 0;
    }
    .tab-item-active {
      color: rgba(38, 99, 255, 1);
      border-bottom: 1px solid rgba(38, 99, 255, 1);
    }
    /* 平板适配 */
    @media screen and (min-height: 1200px) {
      .tab-item {
        font-size: 0.8rem;
        line-height: 55px;
        margin-right: 50px;
      }
    }
    @media screen and (min-height: 2001px) {
      .tab-item {
        font-size: 0.6rem;
      }
    }
  }

  .breadcrumb {
    display: flex;
    align-items: center;
    padding: 10px;
    width: 100%;
    justify-content: flex-start;
    font-size: 14px;
    color: rgba(38, 99, 255, 1);
    background-color: #ffffff;
    white-space: nowrap;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    border-bottom: 1px solid rgba(239, 239, 239, 1);
  }

  .breadcrumb-item {
    display: flex;
    align-items: center;
    flex-shrink: 0;
  }

  .breadcrumb-item view {
    margin-right: 5px;
  }

  .active-breadcrumb {
    color: rgba(134, 139, 152, 1);
  }
  /* 平板适配 */
  @media screen and (min-height: 1200px) {
    .tabs {
      height: 45px;
    }
    .active-breadcrumb {
      font-size: 0.8rem;
    }
  }
  @media screen and (min-height: 2001px) {
    .active-breadcrumb {
      font-size: 0.6rem;
    }
  }

  .list-scroll-view {
    flex: 1;
    height: 0;
    background-color: #ffffff;
    margin-bottom: 10px;
  }

  .list-content {
    width: 100%;
    padding: 0 10px 0 0px;
    box-sizing: border-box;
    min-height: 100%;
  }

  .list-scroll-view::-webkit-scrollbar {
    width: 0;
    display: none;
  }

  .list-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 0;
    border-bottom: 1px solid #eee;
    position: relative;
  }

  .status {
    width: 14px;
    height: 14px;
    border: 2px solid #fff;
    border-radius: 7px;
    position: absolute;
    bottom: 5px;
    left: 26px;
  }

  .online {
    background-color: rgba(53, 199, 89, 1);
  }
  .offline {
    background-color: rgba(158, 158, 158, 1);
  }

  .device-icon {
    width: 40px;
    height: 40px;
    margin-right: 10px;
    flex-shrink: 0;
  }

  .device-text,
  .device-id {
    display: -webkit-box;
    -webkit-line-clamp: 1;
    -webkit-box-orient: vertical;
    overflow: hidden;
    text-overflow: ellipsis;
    flex: 1 1 0%;
    min-width: 0;
    font-size: 14px;
    color: #303133;
    position: relative;
    z-index: 0;
  }
  /* 平板适配 */
  @media screen and (min-height: 1200px) {
    .device-text,
    .device-id {
      font-size: 0.8rem;
    }
  }
  @media screen and (min-height: 2001px) {
    .device-text,
    .device-id {
      font-size: 0.6rem;
    }
  }
  .department {
    font-size: 14px;
    color: rgba(134, 139, 152, 1);
  }

  .item-actions {
    display: flex;
    gap: 15px;
  }

  .my-custom-search {
    background-color: #ffffff;
    border-radius: 8px;
    margin: 12px 16px;
    padding: 8px;

    :deep(.van-search__field) {
      background-color: #f2f2f2;
      border-radius: 6px;
      padding: 0px 10px;
    }

    :deep(.van-search__action) {
      color: rgba(38, 99, 255, 1);
      font-size: 16px;
    }

    :deep(.van-field__control) {
      font-size: 16px;
    }
  }
  .apply-auth-content {
    margin-top: 100px;
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    .apply-img {
      margin-bottom: 16px;
    }
    .text {
      font-size: 16px;
      font-weight: 400;
      line-height: 24px;
      color: #868b98;
    }
    .apply-btn {
      margin-top: 20px;
      width: 68px;
    }
  }
</style>