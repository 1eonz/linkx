import { Popup, showToast } from 'vant';
import { createApp, h } from 'vue';

import { upsert, getDymicGroupList, createLocationShare } from '@/common/api/group.js';
import { useCommunicationStore } from '@/stores/communication.js';
import { useLocationShareStore } from '@/stores/locationShare.js';
import { usePageUrlStore } from '@/stores/pageUrl.js';
import { transformImageUrl } from "./imgUrlParse.js";

function selectGroupByActionSheet(records = []) {
  const actions = (records || []).map((item, idx) => ({
    name: item.groupName || item.name || item.title || `群组${idx + 1}（${item.groupId || '-'}）`,
    groupId: item.groupId,
    raw: item,
  }));

  return new Promise((resolve) => {
    if (!actions.length) {
      resolve(null);
      return;
    }

    const host = document.createElement('div');
    document.body.appendChild(host);

    const app = createApp({
      data() {
        return { show: true };
      },
      render() {
        const close = () => {
          resolve(null);
          this.show = false;
        };

        const onPick = (action) => {
          resolve(action?.raw || null);
          this.show = false;
        };

        return h(
          Popup,
          {
            show: this.show,
            position: 'bottom',
            round: true,
            closeOnClickOverlay: true,
            onClickOverlay: close,
            onClosed: () => {
              app.unmount();
              host.remove();
            },
            'onUpdate:show': (val) => {
              this.show = val;
              if (val === false) resolve(null);
            },
          },
          {
            default: () =>
              h(
                'div',
                {
                  style: {
                    padding: '12px 12px 8px',
                    boxSizing: 'border-box',
                  },
                },
                [
                  // header：右上角取消
                  h(
                    'div',
                    {
                      style: {
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        paddingBottom: '8px',
                      },
                    },
                    [
                      h(
                        'div',
                        {
                          style: {
                            fontSize: '15px',
                            fontWeight: 600,
                            color: '#323233',
                          },
                        },
                        '请选择要进入的位置共享群组',
                      ),
                      h(
                        'div',
                        {
                          style: {
                            width: '28px',
                            height: '28px',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            fontSize: '20px',
                            lineHeight: '20px',
                            color: '#969799',
                            borderRadius: '14px',
                            cursor: 'pointer',
                            userSelect: 'none',
                          },
                          onClick: close,
                        },
                        '×',
                      ),
                    ],
                  ),
                  // list：最大高度 + 可滚动
                  h(
                    'div',
                    {
                      style: {
                        maxHeight: '60vh',
                        overflowY: 'auto',
                        WebkitOverflowScrolling: 'touch',
                      },
                    },
                    actions.map((action) =>
                      h(
                        'div',
                        {
                          style: {
                            padding: '12px 4px',
                            borderTop: '1px solid #ebedf0',
                            fontSize: '14px',
                            color: '#323233',
                            cursor: 'pointer',
                          },
                          onClick: () => onPick(action),
                        },
                        action.name,
                      ),
                    ),
                  ),
                  h('div', { style: { height: '8px' } }),
                ],
              ),
          },
        );
      },
    });

    app.mount(host);
  });
}
// 使用 canvas 将图片转成 base64
const base64Cache = new Map();

export function imageToBase64(imageUrl) {
  if (base64Cache.has(imageUrl)) {
    return Promise.resolve(base64Cache.get(imageUrl));
  }

  return new Promise((resolve, reject) => {
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');
    const img = new Image();
    img.crossOrigin = 'Anonymous';

    img.addEventListener('load', () => {
      canvas.width = img.width;
      canvas.height = img.height;
      ctx.drawImage(img, 0, 0, img.width, img.height);

      const dataURL = canvas.toDataURL('image/png');
      base64Cache.set(imageUrl, dataURL);
      resolve(dataURL);
    });

    img.onerror = function (error) {
      reject(error);
    };

    img.src = imageUrl;
  });
}

export function preloadIconsToBase64(appList) {
  if (!Array.isArray(appList)) return;
  appList.forEach((app) => {
    const icon = app.icon;
    if (!icon) return;
    const iconUrl = transformImageUrl(`/admin-api${icon}`);
    if (iconUrl && !base64Cache.has(iconUrl)) {
      imageToBase64(iconUrl).catch(() => {});
    }
  });
}
/**
 * 将查询参数字符串转换为JSON对象
 * @param {string} str - 形如id=122345&name=9996的字符串
 * @returns {object} 转换后的JSON对象
 */
export function queryStringToJson(str) {
  // 初始化空对象用于存储结果
  const result = {};
  // 处理空字符串的情况
  if (!str) {
    return result;
  }
  // 第一步：按&分割成键值对数组
  const keyValuePairs = str.split('&');
  // 遍历每个键值对
  keyValuePairs.forEach((pair) => {
    // 第二步：按=分割键和值（使用split('=', 2)避免值中包含=的情况）
    const [key, value] = pair.split('=', 2);
    // 处理键存在的情况，解码（处理参数中有特殊字符的情况，比如%20代表空格）
    if (key) {
      const decodedKey = decodeURIComponent(key);
      const decodedValue = value ? decodeURIComponent(value) : '';
      result[decodedKey] = decodedValue;
    }
  });
  return result;
}

/**
 * 防抖：在连续触发时，按指定策略合并执行
 * - immediate=true：首次立即执行，随后在 delay 内的触发被忽略
 * - immediate=false：停止触发 delay 后执行一次
 * 返回函数带 cancel() 方法用于取消未执行的定时器
 */
export function debounce(func, delay = 300, immediate = false) {
  let timeoutId = null;
  const debounced = function (...args) {
    const context = this;
    clearTimeout(timeoutId);

    if (immediate && !timeoutId) {
      func.apply(context, args);
    }

    timeoutId = setTimeout(() => {
      timeoutId = null;
      if (!immediate) {
        func.apply(context, args);
      }
    }, delay);
  };

  debounced.cancel = () => {
    clearTimeout(timeoutId);
    timeoutId = null;
  };

  return debounced;
}

export function getBaseUrlAll() {
  const pathname = location.pathname || '';
  console.log('pathname', pathname)
  let basePath = '/linkx/h5portal';
  const linkxIndex = pathname.indexOf('/linkx/h5portal');
  if (linkxIndex !== -1) {
    // 提取从开头到 /linkx/h5portal 结束的完整路径（包括网关前缀）
    basePath = pathname.substring(0, linkxIndex + '/linkx/h5portal'.length);
  }
  return basePath;
}

export function getFullPageUrl() {
  const origin = location.origin || '';
  // 提取base路径部分，支持网关前缀（如 /zxwg/linkx/h5portal）
  const basePath = getBaseUrlAll();
  // 移除URL末尾的斜杠，确保路径格式统一
  const baseUrl = (origin + basePath).replace(/\/$/, '');
  return baseUrl;
}

let _locationShareRunning = false;

// 打开本地小程序
export async function openLocalUrlApp({appInfo, url}) {
  console.log(222)
  const communicationStore = useCommunicationStore();
  console.log(333)
  const { params, appId, icon, name, type, id } = appInfo;
  console.log(444)
  let newParams = params ? queryStringToJson(params) : {};
  console.log(555)
  const iconUrl = icon ? transformImageUrl(`/admin-api${icon}`) : '';
  console.log(666)
  let iconBase64 = '';
  if (iconUrl) {
    try {
      iconBase64 = await imageToBase64(iconUrl);
    } catch (error) {
      console.error('转换 icon 为 base64 失败:', error);
    }
  }
  console.log('iconUrl', iconUrl, 'iconBase64', iconBase64);
  const openLocalUrlAppflag = await communicationStore.openLocalUrlApp(url, newParams, id, iconBase64, name);
  console.log('openLocalUrlAppflag', openLocalUrlAppflag);

  if (!openLocalUrlAppflag) showToast(`打开本地小程序失败`);
}

export async function locationShareFunc(appInfo) {
   // bugfix: 点击位置共享后到拉聊天界面之间延迟导致用户可以再操作
  if (_locationShareRunning) {
    console.warn('[locationShare] 正在执行中，忽略重复调用');
    return;
  }
  _locationShareRunning = true;

  const pageUrlStore = usePageUrlStore();
  const communicationStore = useCommunicationStore();
  const locationShareStore = useLocationShareStore();
  const res = await communicationStore.getUserInfo();
  const userInfo = res || {};
  try {
    //先判断是否有已加入的动态群组，有则不再新建,直接进入老群组
    const myGroup = await getDymicGroupList({
      userId: userInfo.userid,
      pageNum: 1,
      pageSize: 100,
    });
    
    // 安全地获取 records 数组
    const recordsList = Array.isArray(myGroup?.records) ? myGroup.records : [];

    let dynamicGroupRes = '';
    if (recordsList.length > 0) {
      // 进入已有动态群组前，先弹出列表选择
      const selectedRecord = await selectGroupByActionSheet(recordsList);
      // 用户取消选择，直接退出
      if (!selectedRecord) return;

      dynamicGroupRes = { ...selectedRecord, code: 0 } || {};
      if (!dynamicGroupRes?.groupId) {
        showToast('群组信息异常，无法进入');
        return;
      }
      const videoUrl = pageUrlStore.getLocationShareUrl;
       // 将群组id拼接到videoUrl后边
      const videoUrlWithGroupId = `${videoUrl}?groupId=${dynamicGroupRes.groupId}`;
      await openLocalUrlApp({
        appInfo,
        url: videoUrlWithGroupId,
      });
      return;
    }
    const groupName = '位置共享' + getCurrentDateTime();
    // 新建动态群组
    const groupPrams = {
      groupName, //群组名称
      addMembers: userInfo.isdn, //Isdn列表
      type: 2, //动态组
    };
    console.log(groupPrams, '=======新建动态群组参数=======');
    dynamicGroupRes = await locationShareStore.createDynamicGroup(groupPrams);
    console.log(
      userInfo.isdn,
      JSON.stringify(dynamicGroupRes),
      '=======新建动态群组返回信息=======',
    );

    // 如果创建群组失败，直接返回
    if (!dynamicGroupRes || dynamicGroupRes.code !== 0 || !dynamicGroupRes.groupId || dynamicGroupRes.groupId === 'null') {
      console.error(dynamicGroupRes.code !== 0 ? '创建动态群组失败' : '动态群组id为空');
      showToast('创建动态群组失败');
      return;
    }

    const groupId = dynamicGroupRes.groupId;

    const shareId = await createLocationShare({
      userId: userInfo.userid,
      isdn: userInfo.isdn,
      udcGroup: groupId,
    });
    if (!shareId) {
      showToast('创建位置共享失败');
      await locationShareStore.deleteDynamicGroup({ groupId });
      return;
    }
    console.log('[locationShare] 创建位置共享成功, shareId:', shareId);

    const videoUrl = pageUrlStore.getLocationShareUrl;
    // 将群组id拼接到videoUrl后边
    const videoUrlWithGroupId = `${videoUrl}?groupId=${groupId}&shareId=${shareId}&debug=true`;

    // 分享卡片
    const res = await communicationStore.sendCustomCard({
      urlType: 'app',
      level: '普通',
      title: '位置共享',
      jumpType: '4',
      describe: userInfo.username + '发起了位置共享，点击加入',
      appUrl: videoUrlWithGroupId,
      url: videoUrlWithGroupId,
      thumb: '',
    });
    console.log(JSON.stringify(res), '=======分享卡片返回值========');

    if (res.code === 0) {
      // 分享卡片成功，跳转
      const res = await upsert({
        groupName,
        groupId: groupId,
        ownerId: userInfo.userid,
        members: [
          {
            userId: userInfo.userid,
            userName: userInfo.username,
            isdn: userInfo.isdn,
            tumbAvatar: userInfo.thumbAvatar || '',
          },
        ],
      });
      console.log(res, '=======ICS创建动态群组返回值========');
      //ICP动态组有点问题，我们创建动态组后等2秒再加入动态组
      setTimeout(() => {
        openLocalUrlApp({
          appInfo,
          url: videoUrlWithGroupId,
        });
      }, 2000);
    } else {
      // 分享卡片失败或取消，删除动态群组
      await locationShareStore.deleteDynamicGroup({
        groupId: groupId,
      });
    }
  } catch (error) {
    console.error('位置共享功能执行失败:', error);
    console.error('错误堆栈:', error?.stack);
    showToast('位置共享功能执行失败');
  } finally {
     // bugfix: 点击位置共享后到拉聊天界面之间延迟导致用户可以再操作
    _locationShareRunning = false;
  }
}

//获取当前日期年月日时分秒
export function getCurrentDateTime() {
  const now = new Date();

  // 补零函数：确保个位数前面加 0
  const padZero = (num) => num.toString().padStart(2, '0');

  // const year = now.getFullYear();
  // const month = padZero(now.getMonth() + 1); // 补零
  // const day = padZero(now.getDate()); // 补零
  const hour = padZero(now.getHours()); // 补零
  const minute = padZero(now.getMinutes()); // 补零
  const second = padZero(now.getSeconds()); // 补零

  // 拼接成常用格式
  // return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
  return `${hour}时${minute}分${second}秒`;
}
