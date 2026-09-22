<script lang="ts" setup>
  import type { MenuItem } from '@/pages/types/home';

  import { computed, onBeforeUnmount, onMounted, ref, unref, watch, watchEffect } from 'vue';
  import { useRoute, useRouter } from 'vue-router';

  import { closeAllDialog, Dialog } from '@/components/Dialog';
  import { appConfig } from '@/config';
  import { useEmitter, useI18n, usePermissions } from '@/hooks';
  import DashboardList from '@/pages/dashboard/dashboardList.vue';
  import { conferenceCardDialog } from '@/pages/videoConference/common';
  import commOpt from '@/plugins/mspPlayer/commOpt';
  import {
    useAlarmStore,
    useCommunicateDispatchStore,
    useConferenceStore,
    useMainStore,
    useMessageStore,
    useMissionStore,
    useMonitorStore,
    useRouterStore,
    useVehicleStore,
    useVideoPollStore,
  } from '@/store';
  import { delay } from '@/utils';

  import { debounce } from 'lodash-es';

  import NavRight from './navRight.vue';

  const { t } = useI18n();
  const router = useRouter();
  const route = useRoute();
  const mainStore = useMainStore();
  const messageStore = useMessageStore();
  const conferenceStore = useConferenceStore();
  const missionStore = useMissionStore();
  const alarmStore = useAlarmStore();
  const routerStore = useRouterStore();
  const vehicleStore = useVehicleStore();
  const videoPollStore = useVideoPollStore();
  const communicateDispatchStore = useCommunicateDispatchStore();
  const monitorStore = useMonitorStore();

  const mainMenu = ref<MenuItem[]>([]);
  const isConf = ref(false);
  const conferId = ref('');
  const unifiedAccessCode = ref('');
  const pageTitle = ref('');
  const showMenu = ref(true);
  const showDashboard = ref(false);
  const clearTimer: any = null;

  const isShow = computed(() => {
    const { BACKGROUND_URL } = appConfig.settingData;
    return BACKGROUND_URL !== '0';
  });

  const noticeCount = computed(() => {
    const { eICSTDF } = appConfig.settingData;
    // license此权限配置为1才需要统计任务数据
    return eICSTDF === '1'
      ? messageStore.getMessages[0].length +
          missionStore.missionTotal.pending +
          alarmStore.getAlarmData[0].length
      : messageStore.getMessages[0].length + alarmStore.getAlarmData[0].length;
  });
  const titleSizeClass = computed(() => {
    const getByteLen = (str) => {
      let len = 0;
      for (let i = 0; i < str.length; i++) {
        str.charCodeAt(i) < 256 ? (len += 1) : (len += 2);
      }
      return len;
    };
    const len = getByteLen(unref(pageTitle));

    return {
      title_default: len <= 10,
      title_little: unref(pageTitle).length > 7,
      title_mini: len > 14,
      title_small: len > 10 && len <= 14,
    };
  });

  watch(
    route,
    (val) => {
      if (val.path === '/planSpecial') {
        pageTitle.value = `${val.query?.title}`;
        showMenu.value = false;
      } else if (val.path === '/leadVehicle') {
        showMenu.value = false;
      } else {
        pageTitle.value = appConfig.settingData.STATION_NAME;
        showMenu.value = true;
        pollMangerByMenu(val.path);
      }
    },
    {
      immediate: true,
    },
  );

  watch(conferenceStore.confer, (val) => {
    const { conferenceId, status } = val;
    if (['proceeding', 'success'].includes(status) && conferenceId) {
      isConf.value = true;
      conferId.value = conferenceId;
      unifiedAccessCode.value = val.unifiedAccessCode;
    } else {
      isConf.value = false;
      conferId.value = '';
      unifiedAccessCode.value = '';
    }
  });

  watchEffect(() => {
    if (route.path === '/home') {
      mainMenu.value.forEach((item) => {
        item.isActive = false;
      });
    } else {
      mainMenu.value.forEach((item) => {
        item.isActive = route.path.includes(item.componentName);
      });
    }
  });

  onMounted(() => {
    initMenuData();
    document.addEventListener('animationend', () => {
      mainStore.unDataMinCardId('');
    });
    useEmitter('showResource', showResourceHandler);
  });

  onBeforeUnmount(() => {
    clearTimer?.();
    document.removeEventListener('animationend', () => {
      mainStore.unDataMinCardId('');
    });
  });

  function showResourceHandler() {
    clickMainMenu('policeResource');
  }

  async function clickMainMenu(name: string) {
    const res = await beforeLeave(name);
    if (res) return;

    mainMenu.value.forEach((item) => {
      item.isActive = item.componentName === name;
    });

    // 看板
    if (name === 'dashboard') {
      showDashboard.value = true;
    } else {
      showDashboard.value = false;
      router.push(`/${name}`);
    }
  }

  // 跳转菜单 => 调度屏会议不需要弹窗其他菜单需要弹窗展示
  async function beforeLeave(pathName) {
    if (!usePermissions('CONFERENCE')) {
      return false;
    }
    const { name } = route;
    // 进入通信调度
    if (pathName === 'communicationCenter') {
      Dialog('conferenceCard')?.close();
      communicateDispatchStore.setCurActiveDeskTop('SynthesizeDesktop');
    }
    // 离开通信调度
    if (name === 'communicationCenter' && unref(isConf)) {
      communicateDispatchStore.setCurActiveDeskTop('SynthesizeDesktop');
      setTimeout(() => {
        conferenceCardDialog(true);
      }, 0);
    }
    return false;
  }

  function initMenuData() {
    const allMenu = [
      {
        componentName: 'coordination',
        isActive: false,
        isHover: false,
        name: t('homePage.navigateData.coordination'),
      },
      // {
      //   componentName: 'homeScreen',
      //   isActive: false,
      //   isHover: false,
      //   name: t('homePage.navigateData.homePage'),
      // },
      // {
      //   componentName: 'dashboard',
      //   isActive: false,
      //   isHover: false,
      //   name: t('homePage.navigateData.dashboard'),
      // },
      // {
      //   componentName: 'policeTask',
      //   isActive: false,
      //   isHover: false,
      //   name: t('homePage.navigateData.policeTask'),
      // },
      // {
      //   componentName: 'mapCommand',
      //   isActive: false,
      //   isHover: false,
      //   name: t('homePage.navigateData.mapCommand'),
      // },
      // {
      //   componentName: 'communicationCenter',
      //   isActive: false,
      //   isHover: false,
      //   name: t('homePage.navigateData.communicationCenter'),
      // },
      // {
      //   componentName: 'policeAdmin',
      //   isActive: false,
      //   isHover: false,
      //   name: t('homePage.menus.serviceManagement'),
      // },
      // {
      //   componentName: 'planSafety',
      //   isActive: false,
      //   isHover: false,
      //   name: t('homePage.menus.planSafety'),
      // },
    ];
    const menus: MenuItem[] = [];
    const { menuList } = routerStore;
    allMenu.forEach((item) => {
      const index = menuList.findIndex((menu) => {
        return menu.url.includes(item.componentName);
      });

      // 菜单名字由后台配置来
      if (item.componentName === 'coordination') {
        menus.push(item);
      }

      if (index !== -1) {
        menus.push({ ...item, name: menuList[index]?.name || item.name });
      }
    });

    mainMenu.value = menus;

    for (const data of allMenu) {
      mainStore.initializationMinCarLists(data.componentName);
    }
  }

  // 如果有轮巡，切换页面到通信调度或者离开通信调度页面需要重新轮巡
  async function pollMangerByMenu(path) {
    const { clearPollTimer, clearVideoPollTimer, pollTime, pollTimes, startPolling, videoList } =
      videoPollStore;

    if (clearPollTimer) {
      clearVideoPollTimer();
    }

    const commPath = '/communicationCenter';
    const rePoll = () => {
      if (clearPollTimer) {
        const _pollTimes = pollTimes;
        const _pollTime = pollTime;
        const _videoList = videoList;
        setTimeout(() => {
          startPolling(_pollTimes, _pollTime, _videoList);
        }, 1000);
      }
    };

    // 跳转到通信屏
    if (path === commPath) {
      rePoll();

      const { clearMonitorDrawerData, monitorDrawerData } = monitorStore;
      const { addMonitorList } = communicateDispatchStore;
      const arr = [...monitorDrawerData];
      if (arr.length > 0) {
        clearMonitorDrawerData();
        await delay(1000);
        addMonitorList(arr);
      }
    }

    // 离开通信屏
    if (router.options.history.state.back === commPath) {
      rePoll();

      const { addMonitorDrawerData } = monitorStore;
      const { clearMonitor, monitorDesktopList } = communicateDispatchStore;
      const arr = monitorDesktopList.filter((i) => !!i);
      if (arr.length > 0) {
        clearMonitor();
        await delay(1000);
        for (const element of arr) {
          addMonitorDrawerData(element);
          await delay(500);
        }
      }
    }
  }

  const goBack = debounce(() => {
    const { headerCar } = vehicleStore;
    if (headerCar?.account) {
      commOpt.hangUp('monitor', appConfig.isdn, headerCar.account);
    }

    if (route.name === 'planSpecial') {
      vehicleStore.setUnderProtection(false);
    }
    videoPollStore.clearVideoPollTimer();
    closeAllDialog();

    // 通信关闭需要时间，不然重新拉视频会拉不起
    setTimeout(() => {
      router.back();
    }, 1000);
  }, 500);
</script>

<template>
  <div class="menu-part-container">
    <div class="menu-box">
      <!-- 菜单栏 -->
      <ul v-if="showMenu" class="nav-left">
        <template v-for="item in mainMenu" :key="item.name">
          <li
            class="nav-item"
            :class="{
              active: item.isActive,
            }"
            :title="item.name"
            @click="clickMainMenu(item.componentName)"
          >
            <div
              v-show="mainStore.minCarLists[item.componentName].length > 0"
              class="min-list-has"
              :class="{ 'min-list-animation': mainStore.minCardId === item.componentName }"
            ></div>

            <div
              class="menu-button"
              :class="{
                active: item.isActive,
              }"
            >
              <div class="notify-box nav-item">
                <Icon
                  class="box-bg"
                  :name="item.isActive ? 'menu_bg_active' : 'menu_bg_inactive'"
                />

                <ElBadge
                  :hidden="
                    (item.componentName !== 'policeTask' && item.componentName !== 'mapCommand') ||
                    noticeCount === 0
                  "
                  :max="99"
                  :value="noticeCount"
                >
                  <div class="content">
                    <Icon class="icon" :name="item.componentName" />
                  </div>
                  <div class="unread"></div>
                </ElBadge>
              </div>
            </div>
          </li>
        </template>
      </ul>
      <div v-else class="go-back" @click="goBack()">
        <Icon class="btn" name="return" />
        <span>{{ t('common.back') }}</span>
      </div>
      <!-- 看板 -->
      <DashboardList v-show="showDashboard" @click="showDashboard = false" />
    </div>

    <div class="header-box">
      <div class="content">
        <img v-show="isShow" alt="" class="logo" src="@/assets/images/menu/header_logo.png" />
        <div class="title" :class="titleSizeClass">
          {{ pageTitle }}
        </div>
      </div>
    </div>

    <div class="menu-box">
      <NavRight :show-conf="true" :show-time="true" />
    </div>
  </div>
</template>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  .menu-part-container {
    display: flex;
    pointer-events: none;
  }

  .header-box {
    position: relative;
    height: 60px;
    margin-top: -8px;
    text-align: center;

    .content {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 820px;
      height: 100%;
    }

    .logo {
      z-index: 2;
      width: 40px;
      height: 40px;
      margin: 0 4px 0 8px;
    }

    .title {
      z-index: 2;
      display: flex;
      align-items: center;
      justify-content: center;
      font-style: italic;
      font-weight: bold;
      line-height: 32px;
      color: var(--text-title-first);

      &_default {
        font-size: 30px;
      }

      &_small {
        font-size: 28px;
      }

      &_mini {
        font-size: 20px;
      }

      &_little {
        font-size: 24px;
      }
    }
  }

  .menu-box {
    position: relative;
    display: flex;
    justify-content: space-between;
    width: calc(100%);
    height: 45px;
    pointer-events: auto;

    .nav-left {
      box-sizing: border-box;
      display: flex;
      margin-left: 20px;

      .nav-item {
        margin-top: 3px;
        background: url('@/assets/images/menu/nav_item_bg.png') no-repeat cover;
        background-size: cover;

        .menu-button {
          position: relative;
          display: flex;
          align-items: center;
          justify-content: center;

          &:hover {
            cursor: pointer;

            .nav-name {
              color: var(--text-color-active);
              text-shadow: 0 0 22px #fff;
            }
          }

          .nav-name {
            font-size: 16px;
            font-weight: bold;
            line-height: 16px;
            color: #a6cefd;
          }

          .red-notice {
            position: absolute;
            top: 16px;
            right: 50px;
            width: 13px;
            height: 13px;
            background-color: var(--icon-color-error);
            border: 0.5px solid #fff;
            border-radius: 50%;
          }

          .red-notice-right {
            top: 0;
            right: 5px;
          }

          .count-shrink {
            position: absolute;
            top: 5px;
            right: 30px;
            width: 0;
          }
        }

        .active {
          .nav-name {
            color: var(--text-color-active);
            text-shadow: 0 0 22px #fff;
          }
        }
      }
    }

    .go-back {
      position: absolute;
      top: 18px;
      left: 24px;
      z-index: 10;
      display: flex;
      align-items: center;
      height: 20px;
      pointer-events: all;
      cursor: pointer;

      &:hover {
        .btn {
          fill: #fff;
        }

        span {
          color: #fff;
        }
      }

      .btn {
        width: 16px;
        height: 16px;
        margin-right: 2px;
        fill: var(--text-title-second);
      }

      span {
        font-size: 16px;
        color: var(--text-title-second);
      }
    }
  }

  .min-list-has {
    position: absolute;
    top: 0;
    left: -15px;
    z-index: 999;
    width: 10px;
    height: 100%;
    background: rgb(77 175 250 / 40%);
  }

  .min-list-animation {
    animation: twinkling 1s infinite ease-in-out;
    animation-iteration-count: 1;
  }

  .notify-box {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    margin: 0 6px;
    cursor: pointer;

    .box-bg {
      width: 100%;
      height: 100%;
    }

    .el-badge {
      position: absolute;
    }

    .content {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 15px;
      height: 25px;
    }

    .icon {
      width: 14px;
      height: 14px;
      fill: #fff;
    }
  }
</style>
