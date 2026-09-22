<script lang="ts" setup>
  import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
  // import { useRoute } from 'vue-router';

  // import { getTypeList } from '@/api/dictionary';
  // import { queryExecutorDetailById, selectRemindSwitch } from '@/api/executor';
  // import { queryExecutorDetailById } from '@/api/executor';
  import { getSwitchStatus } from '@/api/xietong';
  import { closeChatUI, getCurrentTheme, getUserInfo } from '@/bridge/post.js';

  // import { selectListMap } from '@/api/lbsLocation';
  // import { selectWeblogSwitch } from '@/api/webLog';
  // import { appConfig } from '@/config';
  import { useDC, useEmitter, useI18n, useUtils } from '@/hooks'; // usePermissions
  import {
    demsTokenInvalid,
    invalidLoginOut,
    kickoutMethod,
    lockScreenMethod,
    refreshTokenMethod,
  } from '@/pages/login/loginHandle';
  // import { loginPIM } from '@/plugins/pim';
  import { registerPimWssEvent } from '@/plugins/pim/registerEvent';
  // import { homeRouters } from '@/router/modules';
  import { useMainStore, usePIMStore } from '@/store';
  // import { loginICS } from '@/utils/loginIcs';

  // import { useCommunicateDispatchStore, useMainStore, useVehicleStore } from '@/store';

  import { themeService } from '@/data/useTheme';

  import { ElLoading } from 'element-plus';

  // import LeftAside from './leftAside.vue';
  // import MainMap from './mainMap.vue';
  // import MenuPart from './menuPart.vue';
  import VoiceRemind from './voiceRemind.vue';

  const PIMStore = usePIMStore();
  const { t } = useI18n();
  // const route = useRoute();
  // const { initVehicleList } = useVehicleStore();
  const mainStore = useMainStore();
  // const { setMonitorPixel } = useCommunicateDispatchStore();

  const demsLoginToken = localStorage.getItem('demsLoginToken');
  // const showMap = ref<boolean>(getMapPanel());
  const changeStyle = ref<boolean>(getCoordination());
  const ready = ref(false);
  const isRemind = ref(false);
  let isMounted = false;

  // const leftAsideMenus = computed(() => {
  //   const data = homeRouters.find((i) => route.path.includes(i.path))?.children || [];
  //   const getChildren = (children) => {
  //     children?.filter(({ name }) => {
  //       switch (name) {
  //         case 'videoControl': {
  //           return usePermissions('CONTROL');
  //         }
  //         case 'warningNotice': {
  //           return usePermissions('ALARM');
  //         }
  //         default: {
  //           return true;
  //         }
  //       }
  //     });
  //   };
  //   const menu = data?.filter(({ name, children }) => {
  //     switch (name) {
  //       case 'createControlTask': {
  //         return usePermissions('CONTROL');
  //       }
  //       case 'emergency': {
  //         return usePermissions('EMERGENCY');
  //       }
  //       case 'mission': {
  //         return usePermissions('MISSION');
  //       }
  //       case 'videoPatrolControl': {
  //         getChildren(children);
  //         return true;
  //       }
  //       default: {
  //         return true;
  //       }
  //     }
  //   });
  //   return menu;
  // });
  const isFull = computed(() => {
    // fixed-当元素祖先的 transform、perspective、filter 或 backdrop-filter 属性非 none 时，容器由视口改为该祖先。
    return mainStore.isFull;
  });
  const userInfo = computed(() => PIMStore.user);

  // watch(
  //   () => PIMStore.collaboration,
  //   (collaboration) => {
  //     if (collaboration) {
  //       heartbeatXietong();
  //     } else {
  //       clearTimerXietong();
  //     }
  //   },
  //   { immediate: true },
  // );

  // watch(
  //   () => route.name,
  //   () => {
  //     // 是否是警务协同
  //     const { id, planId } = route.query;
  //     changeStyle.value = getCoordination();
  //     showMap.value = getMapPanel();
  //     if (usePermissions('SAFETY')) {
  //       initVehicleList(planId || id);
  //     }
  //   },
  //   {
  //     immediate: true,
  //   },
  // );

  registerDC();
  registerPimWssEvent();

  useEmitter('changeBorderRemind', changeRemind);

  watch(
    () => PIMStore.user,
    (user: any) => {
      if (!user?.userid || isMounted) return;
      getSwitchStatusFunc();
      isMounted = true;
    },
    { immediate: true },
  );

  // watch(
  //   () => PIMStore.collaboration,
  //   (collaboration: boolean) => {
  //     if (!collaboration) return;
  //     PIMStore.getCooperationGroupHistoryMsg();
  //   },
  //   { immediate: true },
  // );

  onMounted(async () => {
    // loginICS(PIMStore.user);
    const loading: any = ElLoading.service({
      background: 'rgb(3, 49, 105)',
      lock: true,
      text: t('common.loading'),
    });

    // await queryUserInfo();
    // await queryMapTypes();

    ready.value = true;

    loading.close();

    // loginPIM();

    demsRefreshToken();
    getCurrentThemeFunc();

    // 添加页面可见性监听，当页面隐藏时关闭聊天弹窗
    document.addEventListener('visibilitychange', handleVisibilityChange);
    // 添加页面卸载监听，确保页面关闭时关闭聊天弹窗
    window.addEventListener('beforeunload', handleBeforeUnload);
  });

  onUnmounted(() => {
    closeChatUI();
    // 清理事件监听器
    document.removeEventListener('visibilitychange', handleVisibilityChange);
    window.removeEventListener('beforeunload', handleBeforeUnload);
  });
  async function getCurrentThemeFunc() {
    const data = await getCurrentTheme();
    if (!data) return;
    // 兼容：WebView2 可能返回颜色值（例如 "#152584FF"）
    let theme = data.theme;
    if (typeof theme === 'string') {
      const t = theme.trim().toLowerCase();
      if (t === 'ligth') theme = 'light';
    }
    themeService.setTheme(theme);

    // themeService.setTheme('light');
    // console.log(data, '===============初始化获取主题===========');
  }
  // 获取协同岗绑定和开关状态
  async function getSwitchStatusFunc() {
    console.log('开关状态变更');

    const { userid } = userInfo.value;
    if (!userid) return;
    const { code, data } = await getSwitchStatus({ userId: userid });
    if (code === 0 && data) {
      const { bondedStatus, switchStatus } = data as {
        bondedStatus: boolean;
        switchStatus: boolean;
      };
      PIMStore.setcollaboration(switchStatus && bondedStatus);
      PIMStore.setBondedStatus(bondedStatus);
    }
  }
  // 用户信息
  // async function queryUserInfo() {
  // const local = localStorage.getItem('userInfo');
  // const userInfo = local ? JSON.parse(local) : null;
  // if (userInfo) {
  //   const { organizationId, resourceId } = userInfo;
  //   Object.assign(appConfig, { organizationId, resourceId });
  // }

  // await getTypeData();

  // await getExecutorDetail();

  // await getRemindSwitch();
  // 非警务协同需求代码屏蔽
  // await getLogSwitch();
  // }

  function registerDC() {
    if (!demsLoginToken) {
      // 用户10分钟内未操作，页面出提示然后退出登录
      useDC('PASSPORT', 'lockScreen', (message) => {
        console.log('lockScreen', message);
        lockScreenMethod();
      });
    }

    // token过期，用户离线
    useDC('PASSPORT', 'offline', () => {
      console.log('Access_token expired，user offline');
      kickoutMethod(t('login.logon.userOffline'));
    });

    // 其他地方登录
    useDC('PASSPORT', 'kickout', () => {
      console.log('Login elsewhere');
      kickoutMethod(t('用户权限变更，请重新登录！'));
    });

    // token快要过期
    useDC('PASSPORT', 'token_eighth_timeout', async () => {
      console.log('Access_token expire 1/8');
      refreshTokenMethod(t('login.logon.sessionException'));
    });

    // token快要过期
    useDC('PASSPORT', 'token_twelfth_timeout', async () => {
      console.log('Access_token expire 1/12');
      refreshTokenMethod(t('login.logon.sessionException'));
    });
    // license变更
    useDC('LICENSE', 'license_updated', async () => {
      invalidLoginOut(t('login.logon.licenseTip'));
    });
    // 协同岗管理处绑定或者删除绑定状态更新
    useDC('CLOUDCMD_IM_JINGXIN', 'collaboration_update', async () => {
      setTimeout(() => {
        getSwitchStatusFunc();
      }, 500);
      // 重新获取im的用户信息
      const res: any = await getUserInfo();
      if (res) {
        PIMStore.setUserMyInfo(res);
      }
    });

    useDC('CLOUDCMD_IM_JINGXIN', 'switch_status', ({ personId, type }) => {
      setTimeout(() => {
        const { userid } = userInfo.value;
        if (Number(personId) !== Number(userid)) return;
        if (type === '上岗' && PIMStore.collaboration) return;
        if (type === '下岗' && !PIMStore.collaboration) return;
        getSwitchStatusFunc();
      }, 500);
    });
  }

  // function getMapPanel() {
  //   const { isMapPanel } = useUtils();
  //   return isMapPanel;
  // }

  function getCoordination() {
    const { isCoordination } = useUtils();
    return isCoordination;
  }

  // 定时刷新dems token 查看是否失效
  async function demsRefreshToken() {
    if (demsLoginToken) {
      const text = t('login.logon.demsTokenAreInvalid');
      demsTokenInvalid(text);
    }
  }

  // 离线地图配置
  // async function queryMapTypes() {
  //   const { code, data } = await selectListMap({ activation: 1 });
  //   if (code === 0) {
  //     appConfig.mapList = data.map((i) => {
  //       try {
  //         const config = JSON.parse(i.configuration);

  //         // 如果租户配置了中心点，则取租户中心点
  //         const { TENANT_MAP_CENTER } = appConfig.settingData;
  //         if (TENANT_MAP_CENTER) {
  //           config.center = TENANT_MAP_CENTER.split(',');
  //         }

  //         i.configuration = config;
  //       } catch {
  //         //
  //       }
  //       return i;
  //     });
  //     appConfig.mapConfig = data[0];
  //   }
  // }

  // 边框闪烁
  async function changeRemind(val) {
    isRemind.value = true;
    setTimeout(() => {
      isRemind.value = false;
    }, val * 1000);
  }

  // 处理页面可见性变化
  function handleVisibilityChange() {
    if (document.hidden) {
      // 页面隐藏时关闭聊天弹窗
      closeChatUI();
    }
  }

  // 处理页面卸载
  function handleBeforeUnload() {
    // 页面卸载时关闭聊天弹窗
    closeChatUI();
  }

  // async function getTypeData() {
  //   const res = await getTypeList();
  //   if (res.code !== 0) {
  //     return;
  //   }
  //   res.data.forEach((item) => {
  //     const { code, itemList } = item;
  //     appConfig.dictionaryList[code] = itemList;
  //     if (code === '512') {
  //       appConfig.dictionary[code] = itemList;
  //     }
  //     if (Array.isArray(itemList)) {
  //       itemList.forEach((data) => {
  //         appConfig.dictionary[code + data.value] = data.name;
  //       });
  //     }
  //   });
  // }

  // async function getExecutorDetail() {
  //   const { code, data } = await queryExecutorDetailById({ id: appConfig.resourceId });
  //   if (code !== 0 || !data) {
  //     // kickoutMethod(t('login.logon.loginInvalid'));
  //     return;
  //   }
  //   Object.assign(appConfig.userData, data, {
  //     categoryId: data.category,
  //   });
  //   appConfig.isdn = localStorage.getItem('isdncode');
  //   appConfig.isdnPass = localStorage.getItem('isdnpass');
  // }

  // 获取日志开关
  // async function getLogSwitch() {
  //   const res = await selectWeblogSwitch({ userId: appConfig.resourceId });
  //   if (res.code === 0) {
  //     const { webLogSwitch } = res.data;
  //     Object.assign(appConfig, { logSwitch: webLogSwitch });
  //   }
  // }

  // 提醒开关
  // async function getRemindSwitch() {
  //   const res = await selectRemindSwitch({ userId: appConfig.resourceId });
  //   if (res.code === 0) {
  //     const { remindSwitch, resolution, videoFillMode } = res.data;
  //     Object.assign(appConfig, { remindSwitch, resolution, videoResizeMode: videoFillMode });

  //     const { DEVICE_RESOLUTION_RATIO } = appConfig.settingData;
  //     setMonitorPixel(resolution || DEVICE_RESOLUTION_RATIO);
  //   }
  // }
</script>

<template>
  <div
    v-if="ready"
    class="cloudcmd-container"
    :class="{
      'disable-ground-glass': isFull,
      'container-light': changeStyle,
      'container-dark': !changeStyle,
    }"
    :style="{ minWidth: '800px' }"
  >
    <!-- <MainMap
      class="main-map"
      :class="{
        'main-map-hide': !showMap,
      }"
    /> -->

    <ElContainer>
      <!-- <ElHeader class="header-bg">
        <MenuPart class="cloudcmd-header" />
      </ElHeader> -->
      <!-- <div class="cloudcmd-border" :class="isRemind ? 'border-left-red' : 'border-left'"></div> -->
      <ElContainer class="cloudcmd-main">
        <!-- <ElAside>
          <LeftAside :menus="leftAsideMenus" />
        </ElAside> -->
        <ElMain class="cloudcmd-section" :class="changeStyle ? 'section-light' : 'section-dark'">
          <RouterView v-slot="{ Component }">
            <KeepAlive>
              <component :is="Component" />
            </KeepAlive>
          </RouterView>
        </ElMain>
      </ElContainer>
      <!-- <div class="cloudcmd-border" :class="isRemind ? 'border-right-red' : 'border-right'"></div> -->
      <!-- <ElFooter class="cloudcmd-footer" /> -->
      <VoiceRemind />
    </ElContainer>
  </div>
</template>

<style lang="less" scoped>
  @import '@/styles/reset.less';
  @import '@/styles/mixin.less';

  :deep(.el-dialog__title) {
    color: var(--text-color);
  }

  :deep(.el-dialog__content) {
    color: var(--text-color);
  }

  .container-dark {
    background: #080c11;
  }

  .container-light {
    background-color: rgb(218 241 252);
  }

  .cloudcmd-container {
    width: 100vw;
    height: 100vh;
    color: var(--color-title);

    .header-bg {
      width: 100%;
      height: 100%;

      .cloudcmd-header {
        position: relative;
        width: 100%;
        height: 90px;
        background: url('@/assets/images/menu/top_bg.png') repeat-x;
        background-size: 100% 100%;
      }
    }

    .cloudcmd-main {
      height: calc(100vh - 100px);
      //padding: 0 30px;
      //margin-top: -30px;
    }

    .cloudcmd-footer {
      position: absolute;
      bottom: 0;
      z-index: 666;
      width: 100%;
      height: 52px;
      pointer-events: none;
      background: url('@/assets/images/menu/bottom_bg.png') repeat-x;
      background-size: 100% 100%;

      &::after {
        width: 100%;
        height: 100%;
        content: '';
        background: url('@/assets/images/menu/bottom_gif.gif') repeat-x;
        background-size: 100% 100%;
      }
    }

    .cloudcmd-border {
      position: absolute;
      top: 60px;
      width: 30%;
      height: calc(100vh - 70px);
      pointer-events: none;
    }

    .border-left {
      left: 0;
      background: url('@/assets/images/menu/left_bg.png') repeat-x;
      background-size: 100% 100%;

      &::after {
        position: absolute;
        left: 5px;
        width: 43%;
        height: 100%;
        content: '';
        background: url('@/assets/images/menu/left_gif.gif') repeat-x;
        background-size: 100% 100%;
      }
    }

    .border-left-red {
      left: 0;
      background: url('@/assets/images/menu/left_bg_red.png') repeat-x;
      background-size: 100% 100%;

      &::after {
        position: absolute;
        left: 5px;
        width: 43%;
        height: 100%;
        content: '';
        background: url('@/assets/images/menu/left_gif_red.gif') repeat-x;
        background-size: 100% 100%;
      }
    }

    .border-right {
      right: 0;
      background: url('@/assets/images/menu/right_bg.png') repeat-x;
      background-size: 100% 100%;

      &::after {
        position: absolute;
        right: 5px;
        width: 43%;
        height: 100%;
        content: '';
        background: url('@/assets/images/menu/right_gif.gif') repeat-x;
        background-size: 100% 100%;
      }
    }

    .border-right-red {
      right: 0;
      background: url('@/assets/images/menu/right_bg_red.png') repeat-x;
      background-size: 100% 100%;

      &::after {
        position: absolute;
        right: 5px;
        width: 43%;
        height: 100%;
        content: '';
        background: url('@/assets/images/menu/right_gif_red.gif') repeat-x;
        background-size: 100% 100%;
      }
    }

    .el-aside {
      width: 72px;
      height: 100vh;
    }

    .main-map {
      position: absolute;
      width: 100vw;

      &-hide {
        z-index: -999;
      }
    }

    .cloudcmd-section {
      height: 100vh;
    }

    .section-dark {
      background-color: var(--background-content);
    }

    .section-light {
      background-color: rgb(218 241 252);
    }
  }
</style>
