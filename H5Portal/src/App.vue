<script setup>
  import { onMounted } from 'vue';

  import { taskApi } from '@/common/api/index.js';
  import { setPageUrlStore } from '@/common/config.js';
  import DC from '@/common/network/DC.js';
  import { isLoginExpire } from '@/common/network/utils.js';
  import { useCommon } from '@/hooks/useCommon.js';
  import WeSpaceSDK from '@/static/js/WeSpaceSDK.js';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { usePageUrlStore } from '@/stores/pageUrl.js';
  import { preheatOSInfo } from '@/utils/clientEnv.js';
  import { logJxVersion } from '@/utils/version.js';

  const { userStore } = useCommon();
  const communicationStore = useCommunicationStore();
  const pageUrlStore = usePageUrlStore();

  const { setToken, setUserInfo } = userStore;


  const updateBadge = async () => {
    try {
      console.log('[BadgeUpdate][DEBUG] updateBadge 被触发');
      // 尝试获取token，优先从userStore获取，如果没有则尝试从localStorage获取
      const token =
        userStore.userInfo?.accessToken ||
        localStorage.getItem('taskAccessToken') ||
        localStorage.getItem('token');
      console.log('[BadgeUpdate][DEBUG] token:', token ? '有效' : '无效', 'userStore.token:', !!userStore.userInfo?.accessToken, 'localStorage.taskAccessToken:', !!localStorage.getItem('taskAccessToken'), 'localStorage.token:', !!localStorage.getItem('token'));
      if (!token) return;

      // 延迟 1秒 再查询，防止后端数据尚未落库导致的查询不准确（解决竞态条件）
      await new Promise((resolve) => setTimeout(resolve, 1000));

      const statusCountRes = await taskApi.getTaskStatusCount({ token });
      console.log('[BadgeUpdate][DEBUG] getTaskStatusCount 返回:', JSON.stringify(statusCountRes));
      let pendingNum = 0;
      if (statusCountRes && statusCountRes.length > 0) {
        statusCountRes.forEach((item) => {
          if (item.status === '待处理') {
            pendingNum = +item.count;
          }
        });
      }
      console.log(
        `[BadgeUpdate] Status: ${statusCountRes ? 'Success' : 'Empty'}, PendingNum: ${pendingNum}, Token: ${token ? 'Valid' : 'Invalid'}`,
      );
      console.log('[BadgeUpdate][DEBUG] 查询到 pendingNum:', pendingNum);
      // WespaceSDK.setBadge 只支持给本页面的tab页签设置角标，不支持在协同页签给任务页签设置角标
      // await communicationStore.setBadge(pendingNum);
    } catch (error) {
      console.error('Global updateBadge error:', error);
    }
  };

  onMounted(async () => {
    // 注册全局任务更新监听
    DC.on('TASKS_UPDATE', 'CREATE', updateBadge);
    DC.on('TASKS_UPDATE', 'STATUS_CHANGE', updateBadge);

    console.log('onMounted');
    console.log('H5版本号：202601091419');

    // 初始化 WeSpaceSDK
    window.WeSpaceSDK = WeSpaceSDK;

    try {
      // 首先初始化pageUrl，确保httpBaseUrl在后续请求前已准备好
      await pageUrlStore.initPageUrl();

      // 设置store实例到config中，供后续使用
      await setPageUrlStore(pageUrlStore);

      // 初始化WeSpaceSDK
      await communicationStore.initWeSpaceSDK();

      // SDK就绪后重新预热操作系统信息，获取真实deviceType覆盖UA兜底值
      preheatOSInfo();

      // 获取警信版本信息
      logJxVersion();

      // 没有主题切换需求注释掉这两行代码
      // globalStore.setTheme(uni.getStorageSync('theme'))
      // gProps.setThemeIcon(globalStore.theme, 500)

      const token = localStorage.getItem('token') || null;

      if (token) {
        setToken(token);
        await setUserInfo();
      } else {
        // 强制登陆
      }
    } catch (err) {
      console.log('onLaunch error', err);
      const code = err?.data?.code || err?.statusCode;
      code && isLoginExpire(code);
    }
  });
</script>

<template>
  <router-view />
</template>

<style>
  @import '@/static/css/font-awesome.css';
  
  /* 解决鸿蒙系统 img 阻止点击事件冒泡问题 */
  img {
    pointer-events: none;
  }
  
  /* 白名单：图片本身需要响应点击的场景 */
  img.clickable {
    pointer-events: auto;
  }
  
  .container {
    overflow: hidden;
    min-height: 100vh;
    padding-bottom: 10px;
    background-color: #f7f7f7;
  }

  .navbar_title {
    font-weight: 400;
    font-size: 16px;
    color: #1a1a1a;
  }

  .placeholder-class {
    font-weight: 400;
    font-size: 14px;
    color: #cccccc;
  }

  .my-card {
    width: 345px;
    margin: 0 auto;
    padding: 16px 12px;
    border-radius: 10px;
    background-color: #ffffff;
  }

  html,
  body,
  #app {
    height: 100%;
  }
</style>
