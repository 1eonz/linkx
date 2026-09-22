<template>
  <view class="common-app" v-if="isVisible">
    <view class="section-header">
      <text class="section-title">{{ sectionTitle }}</text>
      <ArrowIcon @click="handleHeaderClick" />
    </view>
    <view class="app-grid">
      <van-grid v-if="appList.length" :column-num="columnNum" :border="false">
        <van-grid-item v-for="(item, index) in appList" :key="index" @click="handleAppClick(item)">
          <view class="app-item" style="width: 100%; text-align: center">
            <van-image
              :src="`${transformImageUrl(`/admin-api${item.icon || ''}`)}`"
              :width="adaptationSize.width"
              :height="adaptationSize.height"
              radius="14px"
              @click="handleAppClick(item)"
            />
            <view class="app-name">{{ item.name }}</view>
          </view>
        </van-grid-item>
      </van-grid>
      <view v-else class="app-empty">
        <text class="empty-text">还没有应用哦</text>
        <view class="empty-btn" @click="handleAddClick">添加</view>
      </view>
    </view>
  </view>
</template>

<script setup>
  import { ref, computed, onMounted } from 'vue';
  import { h5Api } from '@/common/api/index.js';
  import { NAVIGATE_APP_IDS } from '@/common/constants.js';
  import { useEmitter } from '@/hooks/useEmitter.js';
  import { useApplicationStore } from '@/stores/application.js';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { usePageUrlStore } from '@/stores/pageUrl.js';
  import { useSystemConfigStore } from '@/stores/systemConfig.js';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';
  import { queryStringToJson, locationShareFunc, debounce } from '@/utils';
  import { openAppByCheckPre } from '@/utils/appHandler.js';
  import { transformImageUrl } from '@/utils/imgUrlParse.js';
  import { filterMonitor } from '@/utils/totalFunc.js';
  import ArrowIcon from '@/components/ArrowIcon/index.vue';

  const props = defineProps({
    // license权限
    licensePermissions: {
      type: Object,
      default: () => ({}),
    },
    // 用户信息
    userInfo: {
      type: Object,
      default: () => ({}),
    },
    // 获取用户信息的方法（用于位置共享时确保用户信息已获取）
    onGetUserInfo: {
      type: Function,
      default: null,
    },
    // 配置对象
    section: {
      type: Object,
      default: () => ({}),
    },
    // custom配置对象（从section.custom解析）
    custom: {
      type: Object,
      default: () => ({}),
    },
  });
  const emit = defineEmits(['page-disappear']);

  const emitter = useEmitter();
  const applicationStore = useApplicationStore();
  const communicationStore = useCommunicationStore();
  const pageUrlStore = usePageUrlStore();
  const { adaptationSize } = useDeviceAdapter();
  const systemConfigStore = useSystemConfigStore();

  // 是否显示
  const isVisible = computed(() => {
    return props.licensePermissions.LINKXBS === '1' && props.licensePermissions.LINKXBCF === '1';
  });

  // 每行显示的应用数量（store 内部会自动判断设备类型）
  const columnNum = computed(() => {
    return systemConfigStore.getAppCountByDevice();
  });

  // 解析行数（默认为1，但最大数量默认为8）
  const rowCount = computed(() => {
    const count = props.custom?.rowCount;
    return count ? parseInt(count) : null;
  });

  // 最大展示个数
  const maxAppCount = computed(() => {
    // 如果配置了 rowCount，则使用 columnNum * rowCount
    if (rowCount.value !== null) {
      return columnNum.value * rowCount.value;
    }
    // 否则默认最大 8 个
    return 8;
  });

  // 应用列表
  const appList = computed(() => {
    const list = filterMonitor(applicationStore.currentApp, props.licensePermissions);
    return list.slice(0, maxAppCount.value);
  });

  // 初始化数据
  const initData = async () => {
    await systemConfigStore.fetchSystemConfig();
    await applicationStore.getCurrentApp(true);
  };

  // 刷新数据
  const refresh = async () => {
    await systemConfigStore.fetchSystemConfig();
    await applicationStore.getCurrentApp();
  };

  // 监听事件
  useEmitter('INDEX_INIT', initData);
  useEmitter('INDEX_REFRESH', refresh);

  onMounted(() => {
    initData();
  });

  // 页面跳转
  let jumpping = false;
  const navigateToUrl = async (path, title = null, titleStyle = null) => {
    if (jumpping) return;
    jumpping = true;
    try {
      const url = pageUrlStore.getFullPageUrl(path);
      await communicationStore.openUrl(url, title, titleStyle);
    } catch (error) {
      console.error(error, '跳转失败');
    } finally {
      jumpping = false;
    }
  };

  // 组件标题
  const sectionTitle = computed(() => props.section?.name || '常用应用');

  const handleHeaderClick = () => {
    const url = props.section?.url;
    if (url) {
      communicationStore.openUrl(url);
    } else {
      navigateToUrl('/pages/application?param=0', null, 'noTitleStyle');
    }
  };

  const handleAddClick = () => {
    navigateToUrl('/pages/application?param=1', null, 'noTitleStyle');
  };

  // 应用点击处理
  const _handleAppClick = async (item) => {
    try {
      const { name, url, id } = item;
      if (NAVIGATE_APP_IDS.includes(Number(id)) && url) {
        navigateToUrl(url, null, 'noTitleStyle');
        setUseRecord(id)
        return;
      }
      if (name === 'sdk') {
        await communicationStore.openUrl(url);
        return;
      }
      if (name === '设备调度') {
        const videoUrl = pageUrlStore.getVideoMonitorUrl;
        await communicationStore.openUrl(videoUrl, null, 'noTitleStyle');
      } else if (name === '位置共享') {
        // 关闭 WebSocket
        emit('page-disappear');
        // 确保用户信息已获取
        if (!props.userInfo && props.onGetUserInfo) {
          await props.onGetUserInfo();
        }
        locationShareFunc(item);
      } else if (name === '测试开关') {
        const testUrl = 'http://10.28.64.152:8001/linkx/h5portal/';
        await communicationStore.openUrl(testUrl);
      } else if (name === '测试yang') {
        const testUrl = 'http://172.28.89.159:8001/linkx/h5portal/pagesMain/yingYong';
        await communicationStore.openUrl(testUrl);
      } else if (name === '全国警信测试') {
        const testUrl = 'http://10.28.64.75:8001/linkx/h5portal/';
        await communicationStore.openUrl(testUrl);
      } else if (name === 'xl测试') {
        const testUrl = 'http://10.28.64.57:8001/linkx/h5portal/';
        await communicationStore.openUrl(testUrl);
      } else {
        await openAppByCheckPre(item);
      }
     await setUseRecord(id);
    } catch (error) {
      console.error('常用应用跳转失败:', error);
    }
  };

  const handleAppClick = debounce(_handleAppClick, 300, true);
 // 添加使用记录
  const setUseRecord = async (id) => {
    try {
      const type = systemConfigStore.getDeviceTypeValue();
      await h5Api.createAppUsedRecord(id, {
        appId: id,
        userId: props.userInfo.userid,
        client: type,
      });
    } catch (error) {
      console.error(`获取用户信息错误: ${error.message}`);
    }
  };
  // 暴露方法
  defineExpose({
    initData,
    refresh,
  });
</script>

<style lang="scss" scoped>
  .common-app {
    background-color: #fff;
    margin: 8px 16px 0;
    border-radius: 12px;
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
  }

  @media screen and (min-height: 1200px) {
    .section-header {
      margin: 30px 0;
    }
  }

  .app-grid {
    :deep(.van-grid-item__content) {
      padding: 0px;
      overflow: hidden;
    }

    :deep(.van-grid-item) {
      overflow: hidden;
      min-width: 0;
    }

    .app-item {
      margin-bottom: 12px;
      overflow: hidden;
    }

    .app-name {
      display: block;
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
      box-sizing: border-box;
      padding: 0 4px;
    }

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
</style>
