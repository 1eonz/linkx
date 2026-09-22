import { defineStore } from 'pinia';

import { h5Api } from '@/common/api/index.js';

// WeSpaceSDK 通信管理
export const useCommunicationStore = defineStore('communication', {
  state: () => ({
    // 用户信息
    userInfo: null,
    // 用户状态
    userStatus: 'offline',
    // 主题信息
    theme: null,
    // 状态栏高度
    statusBarHeight: 0,
    // 是否可见
    isVisitable: false,
    // 推送token
    pushToken: null,
    // 存储变更监听器
    storageChangeHandlers: new Map(),
    // 下载任务
    downloadTasks: new Map(),
    // 权限管理支持的数据
    permissionsArr: [],
  }),

  getters: {
    // 获取用户信息
    userInfoGetter: (state) => state.userInfo,
    // 获取用户状态
    getStatus: (state) => state.userStatus,
    // 获取主题
    getTheme: (state) => state.theme,
    // 获取状态栏高度
    getStatusBarHeight: (state) => state.statusBarHeight,
    // 是否在线
    isOnline: (state) => state.userStatus === 'online',
    // 是否可见
    getIsVisitable: (state) => state.isVisitable,
  },

  actions: {
    // 初始化WeSpaceSDK
    async initWeSpaceSDK() {
      try {
        // 检查WeSpaceSDK是否可用
        if (typeof window !== 'undefined' && window.WeSpaceSDK) {
          console.log('WeSpaceSDK 初始化成功');
          return true;
        } else {
          console.warn('WeSpaceSDK 不可用');
          return false;
        }
      } catch (error) {
        console.error('WeSpaceSDK 初始化失败:', error);
        return false;
      }
    },

    // 设置事件监听器
    setupEventListeners() {
      if (!window.WeSpaceSDK) return;

      // 用户状态变更监听
      window.WeSpaceSDK.onUserStatusChange((status) => {
        this.userStatus = status;
        console.log('用户状态变更:', status);
      });

      // 页面关闭监听
      window.WeSpaceSDK.onClose(() => {
        console.log('页面即将关闭');
        // 可以在这里做一些清理工作
      });

      // 推送消息监听
      window.WeSpaceSDK.onRemotePushMessage((data) => {
        console.log('收到远程推送消息:', data);
        // 显示通知
        this.showNotification(data);
      });

      // 存储变更监听
      window.WeSpaceSDK.onStorageChange('userInfo', (value) => {
        console.log('用户信息存储变更:', value);
        try {
          this.userInfo = JSON.parse(value);
        } catch {
          this.userInfo = value;
        }
      });
    },

    // 获取用户信息
    async getUserInfo() {
      try {
        if (window.WeSpaceSDK) {
          const userInfo = await window.WeSpaceSDK.getUserInfo();
          this.userInfo = userInfo;
          return userInfo;
        }
        return null;
      } catch (error) {
        console.error('获取用户信息失败:', error);
        return null;
      }
    },

    /**
     * 公共方法：确保获取到包含部门信息的用户信息
     * - 会在本地已有 userInfo 的基础上做有限次重试
     * - 只负责和 WeSpaceSDK 交互，不做业务字段映射
     * @param {number} maxRetry 最大重试次数
     * @param {number} delay 每次重试之间的等待时间（毫秒）
     * @returns {Promise<object|null>} 最终获取到的用户信息（可能不含部门）
     */
    async ensureUserInfoWithDept(maxRetry = 3, delay = 400) {
      // 如果当前已经有带部门信息的 userInfo，直接返回
      if (
        this.userInfo &&
        Array.isArray(this.userInfo.userDepartments) &&
        this.userInfo.userDepartments.length > 0
      ) {
        return this.userInfo;
      }

      let lastUserInfo = this.userInfo;

      for (let i = 0; i < maxRetry; i++) {
        // 主动从 SDK 再拉一次
        const res = await this.getUserInfo();
        lastUserInfo = res || lastUserInfo;

        if (
          res &&
          Array.isArray(res.userDepartments) &&
          res.userDepartments.length > 0
        ) {
          return res;
        }

        // 最后一次不再等待
        if (i < maxRetry - 1 && delay > 0) {
          await new Promise((resolve) => setTimeout(resolve, delay));
        }
      }

      if (lastUserInfo?.idCard) {
        try {
          const data = await h5Api.queryUserByIdCard({
            idCard: lastUserInfo?.idCard,
          });
          if (data.userDepartments) {
            lastUserInfo.userDepartments = data.userDepartments;
          }
        } catch (error) {
          console.error('获取用户信息获取失败:', error);
        }
      }

      // 多次重试仍未拿到部门信息，返回最后一次的结果（可能为 null）
      return lastUserInfo || null;
    },

    // 获取用户状态
    async getUserStatus() {
      try {
        if (window.WeSpaceSDK) {
          const status = await window.WeSpaceSDK.getUserStatus();
          this.userStatus = status;
          return status;
        }
        return 'offline';
      } catch (error) {
        console.error('获取用户状态失败:', error);
        return 'offline';
      }
    },

    // 获取用户状态
    async getIcpUserStatus() {
      try {
        if (window.WeSpaceSDK) {
          const status = await window.WeSpaceSDK.getIcpUserStatus();
          this.userStatus = status;
          return status;
        }
        return 'offline';
      } catch (error) {
        console.error('获取用户状态失败:', error);
        return 'offline';
      }
    },

    //权限管理
    async h5permissions() {
      if (!this.userInfo.idCard) return;
      const data = await h5Api.h5permissions({
        'id-card-num': this.userInfo.idCard,
      });
      const arr = [];
      data.map((item) => {
        arr.push(item.url);
      });
      this.permissionsArr = arr;
    },

    // 获取GIS信息
    async getGisInfo() {
      try {
        if (window.WeSpaceSDK) {
          return await window.WeSpaceSDK.getGisInfo();
        }
        return null;
      } catch (error) {
        console.error('获取GIS信息失败:', error);
        return null;
      }
    },

    // 设置角标
    async setBadge(param) {
      try {
        if (window.WeSpaceSDK) {
          await window.WeSpaceSDK.setBadge(param);
          return true;
        }
        return false;
      } catch (error) {
        console.error('设置角标失败:', error);
        return false;
      }
    },

    // 显示通知
    async showNotification(data) {
      try {
        if (window.WeSpaceSDK) {
          await window.WeSpaceSDK.showNotification(data);
          return true;
        }
        return false;
      } catch (error) {
        console.error('显示通知失败:', error);
        return false;
      }
    },

    // 打开新页面
    async openUrl(url, title, titleStyle) {
      try {
        // 当前页面 URL 的 debug 参数优先：带 debug=true 才传递给子页面，各页签互不影响；
        // URL 上没有 debug 参数时，再读存储（兼容 index.vue / LocationApp.vue 写入的原有链路）
        let debugQueryString;
        const currentDebugParam = new URLSearchParams(window.location.search).get('debug');
        if (currentDebugParam !== null) {
          debugQueryString = currentDebugParam === 'true' ? 'debug=true' : '';
        } else {
          debugQueryString = await window?.WeSpaceSDK?.getStorage('debugQueryString');
        }
        if (url && debugQueryString) {
          const hasQueryParams = /\?.+/.test(url.split('#')[0]);
          if (hasQueryParams) {
            url = `${url}&${debugQueryString}`;
          } else {
            url = `${url}?${debugQueryString}`;
          }
        }
      } catch {}
      try {
        if (window.WeSpaceSDK) {
          await window.WeSpaceSDK.openUrl(url, title || '', titleStyle || 'onlyStatusBar');
          console.log('打开页面成功url', url, title, titleStyle);
          return true;
        }
        console.log('WeSpaceSDK不存在',window.WeSpaceSDK);
        return false;
      } catch (error) {
        console.error('打开页面失败:', error);
        return false;
      }
    },
    // 打开新页面
    async openUrlApp(url, obj) {
      try {
        if (window.WeSpaceSDK) {
          await window.WeSpaceSDK.openUrlApp(url, obj);
          console.log('打开页面成功', url);
          return true;
        }
        return false;
      } catch (error) {
        console.error('打开页面失败:', error);
        return false;
      }
    },

    // 打开本地应用
    async openApp(packageName, abilityName) {
      try {
        if (window.WeSpaceSDK) {
          // package 应用包名 activity:abilityName 鸿蒙启动页面,非必填
          await window.WeSpaceSDK.openApp({ package: packageName ,activity:abilityName});
          return true;
        }
        return false;
      } catch (error) {
        console.error('打开应用失败:', error);
        return false;
      }
    },

    // 打开本地小程序
    async openLocalUrlApp(url, param, id, thumb, name) {
      try {
        if (window.WeSpaceSDK) {
          await window.WeSpaceSDK.openLocalUrlApp(url, param, id, thumb, name);
          console.log('打开本地小程序成功', url, param, id, thumb, name);
          return true;
        }
        return false;
      } catch (error) {
        console.error('打开本地小程序失败:', error);
        return false;
      }
    },
    // 打开小程序
    async openApplet(appId) {
      try {
        if (window.WeSpaceSDK) {
          await window.WeSpaceSDK.openApplet({ appId });
          return true;
        }
        return false;
      } catch (error) {
        console.error('打开小程序失败:', error);
        return false;
      }
    },

    // 关闭当前页面
    async close() {
      try {
        if (window.WeSpaceSDK) {
          await window.WeSpaceSDK.close();
          return true;
        }
        return false;
      } catch (error) {
        console.error('关闭页面失败:', error);
        return false;
      }
    },

    // 关闭小程序页面（openUrlApp / openLocalUrlApp 打开的小程序）
    async closePage() {
      try {
        if (window.jsBridge) {
          const result = await window.jsBridge.invoke('closePage');
          if (result && result.errorCode !== 0) {
            console.error('关闭小程序页面失败:', result.errorMsg);
            return false;
          }
          return true;
        }
        return false;
      } catch (error) {
        console.error('关闭小程序页面失败:', error);
        return false;
      }
    },

    // 设置存储
    async setStorage(key, value) {
      try {
        if (window.WeSpaceSDK) {
          await window.WeSpaceSDK.setStorage(key, value);
          return true;
        }
        return false;
      } catch (error) {
        console.error('设置存储失败:', error);
        return false;
      }
    },

    // 获取存储
    async getStorage(key) {
      try {
        if (window.WeSpaceSDK) {
          return await window.WeSpaceSDK.getStorage(key);
        }
        return null;
      } catch (error) {
        console.error('获取存储失败:', error);
        return null;
      }
    },

    // 监听存储变更
    onStorageChange(key, handler) {
      if (window.WeSpaceSDK) {
        this.storageChangeHandlers.set(key, handler);
        window.WeSpaceSDK.onStorageChange(key, handler);
      }
    },

    // 移除存储变更监听
    removeStorageChange(key) {
      if (window.WeSpaceSDK) {
        this.storageChangeHandlers.delete(key);
        window.WeSpaceSDK.removeStorageChange(key);
      }
    },

    // 创建群组
    async createGroup(params) {
      try {
        if (window.WeSpaceSDK) {
          return await window.WeSpaceSDK.createGroup(params);
        }
        return null;
      } catch (error) {
        console.error('创建群组失败:', error);
        return null;
      }
    },

    // 打开聊天页面
    async sms(params) {
      try {
        if (window.WeSpaceSDK) {
          return await WeSpaceSDK.sms(params);
        }
        return null;
      } catch (error) {
        console.error('打开聊天页面失败:', error);
        return null;
      }
    },

    // 发送自定义卡片
    async sendCustomCard(params) {
      try {
        if (window.WeSpaceSDK) {
          return await window.WeSpaceSDK.sendCustomCard(params);
        }
        return false;
      } catch (error) {
        console.error('发送自定义卡片失败:', error);
        return false;
      }
    },

    // 下载文件
    async download(downloadParam) {
      try {
        if (window.WeSpaceSDK) {

          console.log('下载文件参数:', downloadParam);
          
          const task = await window.WeSpaceSDK.download(downloadParam);
          this.downloadTasks.set(task.taskId, task);
          return task;
        }
        return null;
      } catch (error) {
        console.error('下载文件失败:', error);
        return null;
      }
    },

    /**
     * @name 下载文件(apph5安卓也能下载、鸿蒙不支持)
     * @param {object} params
     * @param {string} params.url 文件URL
     * @param {string} params.fileName 文件名
     * @param {string} [params.fileSize] 文件大小,单位字节。不需要换算单位，content-length 字段的值。
     * @param {string} [params.header] 请求 token，警信不做校验，解析为 k-v直接用于下载，调用方（H5）和下载文件对应的服务器（服务端）协商好就可以，警信只做透传。
     * 
     * @returns {Promise<any>}
     */
    async downloadFile(params) {
      try {
        if (window.WeSpaceSDK) {
          console.log('下载文件参数:', params,window.WeSpaceSDK.encryptedDownload);
          return await window.WeSpaceSDK.encryptedDownload(params);
        }
        return null;
      } catch (error) {
        console.error('下载文件失败:', error);
        return null;
      }
    },

    // 获取推送token
    async getPushToken() {
      try {
        if (window.WeSpaceSDK) {
          const token = await window.WeSpaceSDK.getPushToken();
          this.pushToken = token;
          return token;
        }
        return null;
      } catch (error) {
        console.error('获取推送token失败:', error);
        return null;
      }
    },

    // 监听推送token变更
    onPushTokenChange(handler) {
      if (window.WeSpaceSDK) {
        window.WeSpaceSDK.onPushTokenChange(handler);
      }
    },

    // 监听登出事件
    onLogout(handler) {
      if (window.WeSpaceSDK) {
        window.WeSpaceSDK.onLogout(handler);
      }
    },

    // 监听用户状态变更
    onUserStatusChange(handler) {
      if (window.WeSpaceSDK) {
        window.WeSpaceSDK.onUserStatusChange(handler);
      }
    },

    // 订阅消息
    subscribeMessage(handler) {
      if (window.WeSpaceSDK) {
        window.WeSpaceSDK.subscribeMessage(handler);
      }
    },

    // 推送消息
    pushMessage(data) {
      if (window.WeSpaceSDK) {
        window.WeSpaceSDK.pushMessage(data);
      }
    },

    // 点对点语音通话
    p2pCall(isdn) {
      if (window.WeSpaceSDK) {
        window.WeSpaceSDK.p2pCall(isdn);
      }
    },

    // 点对点视频通话
    p2pVideoCall(isdn) {
      if (window.WeSpaceSDK) {
        window.WeSpaceSDK.p2pVideoCall(isdn);
      }
    },

    // 群组通话
    groupCall(isdn) {
      if (window.WeSpaceSDK) {
        window.WeSpaceSDK.groupCall(isdn);
      }
    },

    // 会议
    meeting(isdns) {
      if (window.WeSpaceSDK) {
        window.WeSpaceSDK.meeting(isdns);
      }
    },

    // 监听
    monitor(isdn, type) {
      if (window.WeSpaceSDK) {
        window.WeSpaceSDK.monitor(isdn, type);
      }
    },

    // 发送短信
    sms(isdn, type) {
      if (window.WeSpaceSDK) {
        window.WeSpaceSDK.sms(isdn, type);
      }
    },

    // 订阅文件分享
    subscribeFileShare(handler) {
      if (window.WeSpaceSDK) {
        window.WeSpaceSDK.subscribeFileShare(handler);
      }
    },

    // 获取分享文件
    async getShareFiles() {
      try {
        if (window.WeSpaceSDK) {
          return await window.WeSpaceSDK.getShareFiles();
        }
        return null;
      } catch (error) {
        console.error('获取分享文件失败:', error);
        return null;
      }
    },

    // 清理资源
    cleanup() {
      // 清理存储变更监听器
      this.storageChangeHandlers.clear();
      // 清理下载任务
      this.downloadTasks.clear();
    },

    // 打开AI助手
    async openAi() {
      try {
        if (window.WeSpaceSDK) {
          return await window.WeSpaceSDK.openAi();
        }
        return null;
      } catch (error) {
        console.error('打开AI助手失败:', error);
        return null;
      }
    },

    // 获取状态栏高度
    async fetchStatusBarHeight() {
      try {
        if (window.WeSpaceSDK) {
          const height = await window.WeSpaceSDK.getStatusBarHeight();
          this.statusBarHeight = height;
          return height;
        }
        return 0;
      } catch (error) {
        console.error('获取状态栏高度失败:', error);
        return 0;
      }
    },

    // 获取主题
    async fetchTheme() {
      try {
        if (window.WeSpaceSDK) {
          const theme = await window.WeSpaceSDK.getTheme();
          this.theme = theme;
          return theme;
        }
        return null;
      } catch (error) {
        console.error('获取主题失败:', error);
        return null;
      }
    },

    // 打开页面（openPage）
    openPage(pageInfo) {
      if (window.WeSpaceSDK) {
        window.WeSpaceSDK.openPage(pageInfo);
      }
    },

    // 通知消息（notifyMessage）
    notifyMessage(message) {
      if (window.WeSpaceSDK) {
        window.WeSpaceSDK.notifyMessage(message);
      }
    },

    // 监听通知栏点击事件
    onClickNotification(handler) {
      if (window.WeSpaceSDK) {
        window.WeSpaceSDK.onClickNotification(handler);
      }
    },

    // 分享监控卡片
    async shareMonitorCall(param) {
      try {
        if (window.WeSpaceSDK) {
          return await window.WeSpaceSDK.shareMonitorCall(param);
        }
        return null;
      } catch (error) {
        console.error('分享监控卡片失败:', error);
        return null;
      }
    },

    // 发起监控
    async createMonitorCall(param) {
      try {
        if (window.WeSpaceSDK) {
          return await window.WeSpaceSDK.createMonitorCall(param);
        }
        return null;
      } catch (error) {
        console.error('发起监控失败:', error);
        return null;
      }
    },
    // 发起音视频点呼
    async createCall(param) {
      try {
        if (window.WeSpaceSDK) {
          return await window.WeSpaceSDK.createCall(param);
        }
        return null;
      } catch (error) {
        console.error('发起音视频点呼失败:', error);
        return null;
      }
    },

    // 获取摄像头
    async getCamera(param) {
      try {
        if (window.WeSpaceSDK) {
          return await window.WeSpaceSDK.getCamera(param);
        }
        return null;
      } catch (error) {
        console.error('获取摄像头失败:', error);
        return null;
      }
    },

    // 获取部门层级
    async getTreeDepartment(param) {
      try {
        if (window.WeSpaceSDK) {
          return await window.WeSpaceSDK.getTreeDepartment(param);
        }
        return null;
      } catch (error) {
        console.error('获取部门层级失败:', error);
        return null;
      }
    },

    // 模糊搜索设备
    async searchTopContact(param) {
      try {
        if (window.WeSpaceSDK) {
          return await window.WeSpaceSDK.searchTopContact(param);
        }
        return null;
      } catch (error) {
        console.error('搜索设备失败:', error);
        return null;
      }
    },
    // 模糊搜索摄像头
    async searchCamera(param) {
      try {
        if (window.WeSpaceSDK) {
          return await window.WeSpaceSDK.searchCamera(param);
        }
        return null;
      } catch (error) {
        console.error('搜索摄像头失败:', error);
        return null;
      }
    },
    // 批量获取设备在离线状态
    async queryOnlineState(param) {
      try {
        if (window.WeSpaceSDK) {
          return await window.WeSpaceSDK.queryOnlineState(param);
        }
        return null;
      } catch (error) {
        console.error('获取部门层级失败:', error);
        return null;
      }
    },

    // 切换到指定Tab页签
    async switchTab(param) {
      try {
        if (window.WeSpaceSDK) {
          await window.WeSpaceSDK.switchTab(param);
          return true;
        }
        return false;
      } catch (error) {
        console.error('切换Tab失败:', error);
        return false;
      }
    },
  },
});
