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
                  <text class="device-id">{{ item?.NodeName || item?.NodeAlias }}</text>
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

  import EquipmentAbility from '@/components/equipment-ability/equipment-ability.vue';
  import emptyIcon from '@/static/equipment/emptyIcon.png';
  import monitorIcon from '@/static/equipment/monitor.png';
  import organizationIcon from '@/static/equipment/organization.png';
  import recorderIcon from '@/static/equipment/recorder.png';
  import terminalIcon from '@/static/equipment/terminal.png';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { usePageUrlStore } from '@/stores/pageUrl.js';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';

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
  const tabList = [
    { name: '摄像头', type: 'monitor' },
    { name: '记录仪', type: 'recorder' },
    { name: '布控球', type: 'terminal' },
  ];
  const current = ref('monitor');
  const breadcrumbList = ref<any[]>([]);
  const loadStatus = ref<'loadmore' | 'nomore' | 'loading' | 'fail'>('loadmore');
  const currentDepartmentId = ref('0');
  const departmentList = ref<any[]>([]);
  const equipmentList = ref<any[]>([]);
  const offsetId = ref('0');
  const limit = ref('20');
  const isLoading = ref(false);
  const showSearch = ref(false);
  const keywords = ref('');
  const searchList = ref<any[]>([]);
  const stateData = ref<any[]>([]);
  const searchOffset = ref('0');
  const searchLimit = ref('20');
  const searchLoadStatus = ref<'loadmore' | 'nomore' | 'loading' | 'fail'>('loadmore');
  const scrollTop = ref(0);
  const deviceIconMap: Record<string, string> = {
    monitor: monitorIcon,
    recorder: recorderIcon,
    terminal: terminalIcon,
  };
  const categoryBase = reactive({
    recorder: '101',
    terminal: '103',
  });

  const applyAuthStatus = ref(3); //状态0审批中 1同意 2拒绝 3未申请

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

  const handleBreadcrumbClick = async (index: number) => {
    if (index === breadcrumbList.value.length - 1) return;
    breadcrumbList.value = breadcrumbList.value.slice(0, index + 1);
    currentDepartmentId.value = breadcrumbList.value[index].departmentId;
    initLoad();
  };

  const handleBreadcrumbClickTop = () => {
    currentDepartmentId.value = '0';
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
    if (keywords.value) {
      current.value = item.type;
      handleSearch();
      return;
    }
    current.value = item.type;
    handleBreadcrumbClickTop();
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
      loadMoreSearchList();
    } else {
      if (loadStatus.value === 'loading' || loadStatus.value === 'nomore') {
        listLoading.value = false;
        return;
      }
      await loadDepartmentAndDevices();
    }
    listLoading.value = false;
  };

  const loadDepartmentAndDevices = async () => {
    // 防止重复调用
    if (isLoading.value && loadStatus.value === 'loading') {
      return;
    }
    // 如果是首次加载（offsetId 为 "0"），先清空列表
    if (offsetId.value === '0') {
      departmentList.value = [];
      equipmentList.value = [];
    }
    isLoading.value = true;
    loadStatus.value = 'loading';
    const { value: offsetIdVal } = offsetId;
    const { value: limitVal } = limit;
    const { value: currentDepartmentIdVal } = currentDepartmentId;
    let orgArr: any[] = [],
      orgLevelArr: any[] = [];
    try {
      if (current.value === 'monitor') {
        const params = {
          nodeId: currentDepartmentIdVal,
          offsetId: offsetIdVal,
          limit: limitVal,
        };
        const cameraRes = (await communicationStore.getCamera(params)) || [];
        offsetId.value = cameraRes?.offsetid || '0';
        if (cameraRes?.cameraTreeSubNodeList) {
          orgLevelArr =
            cameraRes.cameraTreeSubNodeList
              ?.filter((i: any) => Number(i.NodeType) === 0)
              .map((i: any) => ({
                type: 'level',
                NodeType: 0,
                NodeDN: i.subLevel?.LevelId,
                NodeName: i.subLevel?.LevelName,
              })) || [];
        }
        let deviceList: any[] = [];
        if (cameraRes.cameraTreeSubNodeList) {
          deviceList =
            cameraRes?.cameraTreeSubNodeList
              ?.filter((i: any) => Number(i.NodeType) === 1)
              .map((i: any) => ({
                NodeType: 1,
                NodeName: i.camera?.CameraName || i.camera?.alias,
                NodeDN: i.camera?.CameraDN,
                Department: i.camera?.Department,
                PTZControl: i.camera?.PTZControl,
                state: i.camera?.state,
                category: 1,
              })) || [];
        }
        departmentList.value = [...departmentList.value, ...orgLevelArr];
        equipmentList.value = [...equipmentList.value, ...deviceList];
        queryStateData(deviceList);
      } else {
        const treeRes =
          (await communicationStore.getTreeDepartment({
            nodeDN: currentDepartmentIdVal,
            offsetId: offsetIdVal,
            limit: limitVal,
            category: categoryBase[current.value],
          })) || [];
        offsetId.value = treeRes?.offsetId || '0';
        orgArr =
          treeRes.organizationList
            ?.filter((i: any) => Number(i.NodeType) === 0)
            .map((i: any) => ({ ...i, type: 'org' })) || [];
        let deviceList: any[] = [];
        if (treeRes?.organizationList) {
          deviceList =
            treeRes?.organizationList?.filter((i: any) => Number(i.NodeType) === 1) || [];
        }
        departmentList.value = [...departmentList.value, ...orgArr];
        equipmentList.value = [...equipmentList.value, ...deviceList];
        queryStateData(deviceList);
      }
      if (offsetId.value === '0') {
        loadStatus.value = 'nomore';
      } else {
        loadStatus.value = 'loadmore';
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
    return deviceIconMap[current.value] || monitorIcon;
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
    offsetId.value = '0';
    equipmentList.value = [];
    departmentList.value = [];
    scrollTop.value = 1;
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
    const equipmentArr =
      data?.map((item) => {
        return item?.CameraDN || item?.NodeDN;
      }) || [];
    const equipmentParams = {
      type: current.value === 'monitor' ? '0' : '1',
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
    searchOffset.value = '0';
    searchLoadStatus.value = 'loadmore';
    handleBreadcrumbClickTop();
  };

  const handleSearch = () => {
    if (!keywords.value) return;
    searchList.value = [];
    showSearch.value = true;
    isLoading.value = true;
    searchOffset.value = '0';
    searchLoadStatus.value = 'loading';
    if (current.value === 'monitor') {
      handleSearchCamera();
    } else {
      handleSearchContact();
    }
  };

  const loadMoreSearchList = () => {
    isLoading.value = true;
    searchLoadStatus.value = 'loading';
    const page = Number(searchOffset.value) + Number(searchLimit.value);
    searchOffset.value = page + '';
    if (current.value === 'monitor') {
      handleSearchCamera();
    } else {
      handleSearchContact();
    }
  };

  const handleSearchCamera = async () => {
    const params = {
      offset: searchOffset.value,
      limit: searchLimit.value,
      searchCondition: keywords.value,
    };
    const cameraData: any[] = [];
    try {
      const resData = (await communicationStore.searchCamera(params)) || [];
      if (resData.length === 0) {
        if (offsetId.value === '0') {
          searchList.value = [];
        }
        searchLoadStatus.value = 'nomore';
        finished.value = true;
        return;
      }
      resData?.forEach((item: any) => {
        const { CameraDN, CameraName, alias, PTZControl, levelNumberName } = item;
        let department = '';
        if (levelNumberName) {
          const arr = levelNumberName.split('>');
          const newArr = arr.filter((item: string) => item);
          department = newArr.join('>');
        }
        cameraData.push({
          NodeName: CameraName || alias,
          NodeDN: CameraDN,
          Department: department,
          PTZControl: PTZControl,
          category: 1,
        });
      });
      searchList.value = [...searchList.value, ...cameraData];
      queryStateData(cameraData);
      searchLoadStatus.value = resData.length < Number(searchLimit.value) ? 'nomore' : 'loadmore';
      if (searchLoadStatus.value === 'nomore') {
        finished.value = true;
      }
    } catch (e) {
      searchLoadStatus.value = 'fail';
    }
    isLoading.value = false;
  };

  const handleSearchContact = async () => {
    const params = {
      searchCondition: keywords.value,
      category: categoryBase[current.value],
      offset: searchOffset.value,
      limit: searchLimit.value,
    };
    try {
      const resData = (await communicationStore.searchTopContact(params)) || [];
      if (resData.length === 0) {
        if (offsetId.value === '0') {
          searchList.value = [];
        }
        searchLoadStatus.value = 'nomore';
        finished.value = true;
        return;
      }
      const data: any[] = [];
      resData?.forEach((item: any) => {
        const { userDN, alias, userName, Category, department } = item;
        const obj = {
          NodeName: userName || alias,
          NodeDN: userDN,
          Department: department,
          category: Category,
        };
        data.push(obj);
      });
      searchList.value = [...searchList.value, ...data];
      queryStateData(data);
      searchLoadStatus.value = data.length < Number(searchLimit.value) ? 'nomore' : 'loadmore';
    } catch (e) {
      searchLoadStatus.value = 'fail';
    }
    isLoading.value = false;
  };

  const getEquipmentType = (data: any) => {
    const category = Number(data?.category || data?.userCategory);
    const apptype = Number(data?.apptype);
    if (category === 1 || (category === 100 && apptype === 111)) {
      return 'monitor';
    } else if ('11,101'.includes(category + '') || (category === 100 && apptype === 109)) {
      return 'recorder';
    } else if (category === 103) {
      return 'terminal';
    }
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
    initLoad();
    getIcpUserStatus();
    window.WeSpaceSDK.onStateValue((val: string) => {
      const data = JSON.parse(val);
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
    const item = { name: '', index: '' };
    if (current.value === 'monitor') {
      item.name = '摄像头';
    } else if (current.value === 'recorder') {
      item.name = '记录仪';
    } else {
      item.name = '布控球';
    }
    navigateToUrl(
      '/pages/aiAssistant/applyPermissionForm?formType=1&params=' + JSON.stringify(item),
      null,
      'noTitleStyle',
    );
  }

  //获取勾选权限
  const cappPrivJson = ref([]);
  //判断智能体是否需要申请,是否有AI权限
  const hasAIAuth = ref(false);
  // const getCappPrivJson = async () => {
  //   try {
  //     cappPrivJson.value = JSON.parse(
  //       await window.WeSpaceSDK.getStorage("cappPrivJson")
  //     );
  //   } catch (error) {
  //     console.log("cappPrivJson获取失败", error);
  //   }
  // };
  //获取勾选权限
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

    .tab-item {
      font-size: 16px;
      font-weight: 400;
      letter-spacing: 0px;
      line-height: 31px;
      color: rgba(134, 139, 152, 1);
      margin-right: 24px;
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
    padding: 0 10px;
    box-sizing: border-box;
    min-height: 100%;
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
