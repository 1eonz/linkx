<template>
  <view v-if="systemLimit || !effectLicense" class="license-limit">
    系统功能受限，请联系管理员
  </view>
  <view class="container" v-else>
    <CommonApps
      ref="commonAppsRef"
      :licensePermissions="licensePermissions"
      :userInfo="userInfo"
      :onGetUserInfo="getUserInfo"
      :section="section"
      :custom="custom"
    />
  </view>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';

// 组件引入
import CommonApps from './components/CommonApps.vue';

// API 引入
import { h5Api } from '@/common/api/index.js';

// Store 引入
import { useCommunicationStore } from '@/stores/communication.js';
import { useUserStore } from '@/stores/user.js';

// Hook 引入
import { useEmitter } from '@/hooks/useEmitter.js';

// 工具函数引入
import { checkLicenseStatus } from '@/utils/totalFunc.js';

// Store 实例
const communicationStore = useCommunicationStore();
const userStore = useUserStore();
const emitter = useEmitter();

// 组件引用
const commonAppsRef = ref();

// 状态
const systemLimit = ref(false);
const userInfo = ref(null);
const accessToken = ref('');
const licensePermissions = ref({});

// CommonApps 组件配置
const section = ref({ name: '常用应用' });
const custom = ref({});

// 计算属性 - License 是否有效
const effectLicense = computed(() => {
  const obj = licensePermissions.value || {};
  if (obj.LINKXBS !== '1') return false;
  if (obj.status === '0' || obj.status === '3' || obj.status === '5') return false;
  for (let i in obj) {
    if (i !== 'status' && i !== 'LINKXBS' && i !== 'expireDate') {
      if (obj[i] === '1') return true;
    }
  }
  return false;
});

// 获取用户信息
const getUserInfo = async () => {
  try {
    const res = await communicationStore.getUserInfo();
    if (res) {
      userInfo.value = res;
      console.log('打印用户信息', JSON.stringify(userInfo.value, null, 2));
      // 缓存用户信息
      communicationStore.setStorage('cachedUserInfo', unescape(encodeURIComponent(JSON.stringify(res))));
    } else {
      console.error('获取用户信息失败或WeSpaceSDK不可用');
    }
  } catch (error) {
    console.error(`获取用户信息错误: ${error.message}`);
  }
};

// 获取 License 权限
const getLicensePermissions = async () => {
  const res = await h5Api.getLicensePermissions({});
  licensePermissions.value = res || {};
  console.log('licensePermissions', res);
  checkLicenseStatus(res);
  // 缓存 license 权限
  communicationStore.setStorage('cachedLicensePermissions', unescape(encodeURIComponent(JSON.stringify(res || {}))));
};

// 获取登录信息
const getLoginInfo = async () => {
  const data = await userStore.getStoreUserInfo();
  systemLimit.value = data?.code === 182;
  if (!systemLimit.value) {
    accessToken.value = data.accessToken;
  }
  return data;
};

// 初始化数据
const initData = async () => {
  // 并行请求互不依赖的数据
  await Promise.all([
    getUserInfo(),
    getLicensePermissions(),
    getLoginInfo(),
  ]);

  if (!systemLimit.value) {
    // 触发 CommonApps 组件初始化
    emitter.emit('INDEX_INIT');
  }
};

// 生命周期
onMounted(() => {
  initData();
});

onBeforeUnmount(() => {
  // 清理工作
});
</script>

<style lang="scss" scoped>
.container {
  background-color: #f7f7f7;
  box-sizing: border-box;
  width: 100%;
  min-height: 100vh;
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
</style>
