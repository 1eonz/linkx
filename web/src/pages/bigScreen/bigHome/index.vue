<script lang="ts" setup>
  import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
  import { useRoute } from 'vue-router';

  import { getTypeList } from '@/api/dictionary';
  import { queryExecutorDetailById } from '@/api/executor';
  import { registerPostMessage } from '@/comm/postMessage';
  import { appConfig } from '@/config';
  // import { initGlobalData } from '@/data';
  import { useDC, useI18n } from '@/hooks';
  import {
    demsTokenInvalid,
    heartTest,
    invalidLoginOut,
    kickoutMethod,
    lockScreenMethod,
    refreshTokenMethod,
  } from '@/pages/login/loginHandle';
  import commOpt from '@/plugins/mspPlayer/commOpt';
  import { useMainStore, useResourceStore, useVehicleStore } from '@/store';

  import { ElLoading } from 'element-plus';

  import NavHeader from '../components/navHeader.vue';

  const { t } = useI18n();
  const route = useRoute();
  const mainStore = useMainStore();
  const resourceStore = useResourceStore();
  const { initVehicleList } = useVehicleStore();

  const isReady = ref(false);
  // 判断是否是dems token直接登录，如果是没有未操作退出登录
  const demsLoginToken = localStorage.getItem('demsLoginToken');

  const isFull = computed(() => {
    // fixed-当元素祖先的 transform、perspective、filter 或 backdrop-filter 属性非 none 时，容器由视口改为该祖先。
    return mainStore.isFull;
  });

  watch(
    () => route.name,
    () => {
      const { id, planId } = route.query;
      initVehicleList(planId || id);
    },
    {
      immediate: true,
    },
  );

  // const { initAsyncResource } = initGlobalData();

  // 用户10分钟内未操作，页面出提示然后退出登录
  useDC('PASSPORT', 'lockScreen', (message) => {
    console.log('lockScreen', message);
    if (!demsLoginToken) {
      lockScreenMethod();
    }
  });

  // token过期，用户离线
  useDC('PASSPORT', 'offline', () => {
    console.log('Access_token expired，user offline');
    kickoutMethod(t('login.logon.userOffline'));
  });

  // 其他地方登录
  useDC('PASSPORT', 'kickout', () => {
    console.log('Login elsewhere');
    kickoutMethod(t('login.logon.userLoggedOtherDevice'));
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

  onMounted(async () => {
    registerPostMessage();

    await init();

    resourceStore.saveGlobalIcon();

    isReady.value = true;

    // initAsyncResource();

    // 定时刷新dems token 查看是否失效
    if (demsLoginToken) {
      demsRefreshToken();
    }

    window.addEventListener('beforeunload', () => {
      commOpt.hangUpAll();
    });
  });

  onBeforeUnmount(() => {
    window.removeEventListener('beforeunload', () => {
      commOpt.hangUpAll();
    });
  });

  async function init() {
    const loading: any = ElLoading.service({
      background: 'rgb(3, 49, 105)',
      lock: true,
      text: t('common.loading'),
    });

    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
    if (userInfo) {
      const { organizationId, resourceId } = userInfo;
      Object.assign(appConfig, { organizationId, resourceId });
    }

    await getTypeList().then(({ code, data }) => {
      if (code !== 0) return;
      data.forEach((item) => {
        appConfig.dictionaryList[item.code] = item.itemList;
        if (item.code === '512') {
          appConfig.dictionary[item.code] = item.itemList;
        }
        if (Array.isArray(item.itemList)) {
          item.itemList.forEach((data) => {
            appConfig.dictionary[item.code + data.value] = data.name;
          });
        }
      });
    });

    await queryExecutorDetailById({ id: appConfig.resourceId }).then(({ code, data }) => {
      if (code !== 0 || !data) {
        // kickoutMethod(t('login.logon.loginInvalid'));
        return;
      }

      Object.assign(appConfig.userData, data, {
        categoryId: data.category,
      });
      const isdncode = localStorage.getItem('isdncode');
      const isdnpass = localStorage.getItem('isdnpass');
      appConfig.isdn = isdncode;
      appConfig.isdnPass = isdnpass;
    });

    loading.close();

    // 心跳检测
    heartTest();
  }

  // 刷新dems token
  async function demsRefreshToken() {
    const text = t('login.logon.demsTokenAreInvalid');
    demsTokenInvalid(text);
  }
</script>

<template>
  <div class="home-layout" :class="{ 'disable-ground-glass': isFull }">
    <template v-if="isReady">
      <NavHeader
        :class="{
          'map-center-header': ['/mapCenter', '/planSpecial'].includes(route.path),
        }"
      />

      <!-- 如果不缓存，数据多了页面间切换体验不好 -->
      <RouterView v-slot="{ Component }">
        <KeepAlive>
          <component :is="Component" />
        </KeepAlive>
      </RouterView>
    </template>

    <div class="side left">
      <img alt="" class="side-border" src="@/assets/images/communicate/side_left_bg.png" />
      <img alt="" src="@/assets/images/communicate/side_left_bg.gif" />
    </div>
    <div class="side right">
      <img alt="" class="side-border" src="@/assets/images/communicate/side_right_bg.png" />
      <img alt="" src="@/assets/images/communicate/side_right_bg.gif" />
    </div>
  </div>
</template>

<style lang="less" scoped>
  .home-layout {
    position: relative;
    width: 100vw;
    height: 100vh;
    background: url('@/assets/images/communicate/back-img.png') no-repeat;
    background-size: 100% 100%;

    .map-center-header {
      position: absolute;
      top: 0;
      left: 0;
      z-index: 10;
      pointer-events: none;
    }

    .side {
      position: absolute;
      top: 66px;
      z-index: 100;
      width: 292px;
      height: calc(100vh - 66px);
      pointer-events: none;

      &.left {
        left: 0;

        .side-border {
          object-fit: cover;
          object-position: left top;
        }
      }

      &.right {
        right: 0;

        .side-border {
          object-fit: cover;
          object-position: right top;
        }
      }

      img {
        width: 100%;
        height: 100%;

        &:nth-of-type(2) {
          position: absolute;
          top: 0;
          left: 0;
        }
      }
    }
  }
</style>
