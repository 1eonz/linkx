import { defineStore } from 'pinia';
import { ref } from 'vue';

import { tokenLogin } from '@/common/api/h5.js';

export const useUserStore = defineStore('user', () => {
  const token = ref('');
  const loginCount = ref(0);
  function setToken(payload) {
    token.value = payload;
    localStorage.setItem('token', payload);
  }

  const userInfo = ref({});
  const clientId = ref('CAPP-1000');
  async function loadUserInfo() {
    try {
      const sdkUserInfo = await window.WeSpaceSDK.getStorage('tokenLoginUserInfo');
      if (sdkUserInfo) {
        const decodedUserInfo = JSON.parse(decodeURIComponent(escape(sdkUserInfo)));
        userInfo.value = decodedUserInfo;
        return decodedUserInfo;
      } else {
        return userInfo.value;
      }
    } catch {
      return userInfo.value;
    }
  }
  async function getStoreUserInfo() {
    return await loadUserInfo();
  }
  async function setUserInfo(param) {
    let myParam = param;
    if (!param) {
      myParam = {
        clientId: clientId.value,
        token: '',
      };
      try {
        const userInfoRes = await window.WeSpaceSDK.getUserInfo();
        if (userInfoRes && userInfoRes?.aastoken) {
          myParam.token = userInfoRes?.aastoken;
        }
      } catch {}
    } else {
      clientId.value = myParam.clientId;
    }
    try {
      const res = await tokenLogin(myParam);
      // 在写入 Vue 响应式 ref 之前剔除 H5 不使用的大字段，避免深层响应式代理开销
      // imOrgPrivs 仅用于判断是否有权限（length），不使用具体内容，只保留长度
      if (res.imOrgPrivs) {
        res.imOrgPrivs = Array.isArray(res.imOrgPrivs) ? res.imOrgPrivs.length : res.imOrgPrivs;
      }
      // roles[].imOrgPrivJson 为部门权限树，H5 端不使用，不做缓存
      if (res.roles) {
        res.roles = res.roles.map((role) => {
          const { imOrgPrivJson, ...rest } = role;
          return rest;
        });
      }
      Object.assign(userInfo.value, res);
      // 缓存数据（大字段已在上方剔除，直接序列化即可）
      const jsonStr = JSON.stringify(userInfo.value);
      const unicodeSafeStr = unescape(encodeURIComponent(jsonStr));
      await window.WeSpaceSDK.setStorage('tokenLoginUserInfo', unicodeSafeStr);
      return res;
    } catch (e) {
      return Promise.reject(e);
    }
  }

  function setLoginCount(data) {
    loginCount.value = data;
  }

  return {
    token,
    setToken,
    userInfo,
    setUserInfo,
    loadUserInfo,
    getStoreUserInfo,
    loginCount,
    setLoginCount,
  };
});
