import { showConfirmDialog, showToast } from "vant";
import { queryStringToJson, imageToBase64 } from "./index.js";
import { getDeviceType,isHarmonyOS,getGlobalsConfigByKey } from '@/common/utils';
import { transformImageUrl } from "./imgUrlParse.js";
import { useCommunicationStore } from "@/stores/communication.js";
import { getPrerequisiteInfo } from "@/common/api/h5.js";

/**
 * 根据设备类型解析多环境 URL
 * @param {string} url 可能包含多个环境的 URL，用分号分隔
 * @returns {string} 根据当前设备类型返回对应的 URL
 * @example
 * // URL 格式: 'mobile_url;tablet_url;pc_url'
 * // 例如: 'https://m.baidu.com;https://t.baidu.com;https://www.baidu.com'
 * parseUrlByDevice('https://m.baidu.com;https://t.baidu.com;https://www.baidu.com')
 * // mobile 设备返回 'https://m.baidu.com'
 * // tablet 设备返回 'https://t.baidu.com'
 * // pc 设备返回 'https://www.baidu.com'
 */
function parseUrlByDevice(url) {
  if (!url) return url;
  
  const urls = url.split(';');
  // 如果只有一个 URL，直接返回
  if (urls.length === 1) return url.trim();
  
  // 根据设备类型选择对应 URL
  const deviceType = getDeviceType(); // 'mobile' | 'tablet' | 'pc'
  const indexMap = { mobile: 0, tablet: 1, pc: 2 };
  const index = indexMap[deviceType];
  
  // 返回对应环境的 URL，如果不存在则降级使用第一个
  return (urls[index] || urls[0]).trim();
}

/**
 * 打开前置应用并在之后打开目标应用
 * @param {string} prerequisite 前置应用ID
 * @param {Object} appInfo 目标应用信息
 */
async function openPreAppByPrerequisite(prerequisite, appInfo = null) {
  if (prerequisite) {
    try {
      // 获取前置应用配置
      const preAppRes = await getPrerequisiteInfo({ id: prerequisite });
      console.log('preAppRes 333333', preAppRes);
      if (preAppRes) {
        const preAppInfo = preAppRes;
        console.log('preAppInfo 444444', preAppInfo);
        if (preAppInfo.type === 3) {
          const preAppOpenSuccess = await openTargetApp(preAppInfo);
          console.log('preAppOpenSuccess 222222', preAppOpenSuccess);
          // 前置应用打开成功  
          if (preAppOpenSuccess) {
            console.log('前置应用打开成功preAppOpenSuccess 111111', preAppOpenSuccess);
            if(appInfo){
              try {
                const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));
                await sleep(1000); // 等待1秒
                console.log('等1000ms 开始打开目标应用'); 
                openTargetApp(appInfo).then(res => {
                  if (res) {
                    console.log('打开目标应用成功333333 appInfo', appInfo);
                  } else {
                    console.log('打开目标应用失败');
                    showToast('打开目标应用失败');
                  }
                });
              } catch (error) {
                showToast('打开目标应用失败');
              }
            }
          } else {
            console.log('前置应用打开失败，不继续打开目标应用');
            showToast('打开前置应用失败');
          }
          return preAppOpenSuccess;
        } else {
          showToast('打开失败, 请检查该前置应用的应用类型');
          return false;
        }
      }
    } catch (error) {
      showToast('获取前置应用信息失败');
      return false;
    }
  } else {
    // 无前置应用id: 直接打开目标应用
    return await openTargetApp(appInfo);
  }
};

/**
 * 封装打开目标应用的逻辑
 * @param {Object} appInfo 应用信息对象
 */
export async function openTargetApp(appInfo) {
  if (!appInfo) {
    console.error(`openTargetApp: appInfo参数值为${appInfo}`);
    return false;
  }
  if (Object.prototype.toString.call(appInfo) !== '[object Object]') {
    console.error(`openTargetApp: appInfo参数值为${appInfo}`);
    return false;
  }

  const { params, appId, icon, name, type } = appInfo;
  // 根据设备类型解析多环境 URL
  const url = parseUrlByDevice(appInfo.url);
  const communicationStore = useCommunicationStore();

  if (!communicationStore) {
    console.error('openTargetApp: communicationStore获取失败');
    return false;
  }

  if (type === 0) {
    if (url && appId) {
      // url和appid都存在，当作本地小程序打开
      console.log('H5: url && appId', url, appId);
      try {
        let newParams = params ? queryStringToJson(params) : {};
        const iconUrl = icon ? transformImageUrl(`/admin-api${icon}`) : '';
        console.log('iconUrl', iconUrl);

        let iconBase64 = '';
        if (iconUrl) {
          try {
            iconBase64 = await imageToBase64(iconUrl);
          } catch (error) {
            console.error('转换 icon 为 base64 失败:', error);
          }
        }

        console.log('iconUrl', iconUrl, 'iconBase64', iconBase64);
        const openLocalUrlAppflag = await communicationStore.openLocalUrlApp(url, newParams, appId, iconBase64, name);
        console.log('openLocalUrlAppflag', openLocalUrlAppflag);

        if (!openLocalUrlAppflag) showToast(`打开本地小程序失败`);
        return openLocalUrlAppflag;
      } catch (error) {
        showToast(`打开本地小程序失败`);
        return false;
      }
    } else if (appId) {
      // 只有appId, 当作小程序跳转
      console.log('H5: 只有appId', appId);
      try {
        const openAppletFlag = await communicationStore.openApplet(appId);
        console.log('openAppletFlag', openAppletFlag);

        if (!openAppletFlag) showToast(`打开小程序失败`);
        return openAppletFlag;
      } catch (error) {
        showToast(`打开小程序失败`);
        return false;
      }
    } else if (url) {
      // 只有url，当作H5跳转
      console.log('H5: 只有url', url);
      try {
        const fullUrl = params ? `${url}?${params}` : url;
        const openUrlFlag = await communicationStore.openUrl(fullUrl, null, 'onlyStatusBar');
        console.log('openUrlFlag', openUrlFlag);

        if (!openUrlFlag) showToast(`打开H5应用失败`);
        return openUrlFlag;
      } catch (error) {
        console.error('openTargetApp: 打开H5应用失败', error);
        showToast(`打开H5应用失败`);
        return false;
      }
    } else {
      showToast(`H5应用或小程序缺少必要参数`);
      return false;
    }
  } else  {
   const { packageHm , packageAndroid , activity } = appInfo;
    // 原生应用 || 前置应用
    console.info('原生应用 111111', appInfo);
   const packageName = isHarmonyOS() ? packageHm : packageAndroid;
   const activityName = isHarmonyOS() ? activity : undefined;
    console.info('是否是鸿蒙设备', isHarmonyOS());
    console.info('应用包名', packageName,activityName);
    if (!packageName) {
      console.error(`openTargetApp: 原生应用缺少package包名参数，appInfo: ${appInfo}`);
      return false;
    }
    try {
      const openAppFlag = await communicationStore.openApp(packageName,activityName);
      console.log('openAppFlag', openAppFlag);

      if (!openAppFlag) showToast(`打开原生应用失败`);
      return openAppFlag;
    } catch (error) {
      console.error('openTargetApp: 打开原生应用失败', error);
      showToast(`打开原生应用失败`);
      return false;
    }
  } 
  return false;
}

/**
 * 处理带前置检查的应用打开逻辑（三类区应用）
 * @param {Object} appInfo 应用信息对象
 */
export async function openAppByCheckPre(appInfo) {
  console.log('openAppByCheckPre 1111111', appInfo);
  if (!appInfo) {
    console.error(`openAppByCheckPre: appInfo参数值为${appInfo}`);
    return false;
  }
  if (Object.prototype.toString.call(appInfo) !== '[object Object]') {
    console.error(`openAppByCheckPre: appInfo参数值为${appInfo}`);
    return false;
  }

  const { zone, prerequisite, type } = appInfo;

  // 非三类区，直接打开目标应用
  if (zone !== 3) return await openTargetApp(appInfo);

  // 三类区应用 //h5应用暂时和app一样逻辑
  // if (type !== 1) {
  //   // type 0:H5 1:app 3:前置应用
  //   // 三类区非App应用
  //   return await openPreAppByPrerequisite(prerequisite, appInfo);
  // } else {
    // 三类区&&App应用：打开弹窗确认
    let noticeMsg;
    try {
      noticeMsg = await getGlobalsConfigByKey('DAIL_APP_NOTICE_MSG');
    } catch (e) {
      console.error('获取 DAIL_APP_NOTICE_MSG 失败:', e);
    }
    noticeMsg = noticeMsg || '是否已经开启三类区拨号程序？';
    const hasOpenPreApp = await new Promise((resolve) => {
      showConfirmDialog({
        title: '确认',
        message: noticeMsg,
        confirmButtonText: '是',
        cancelButtonText: '否'
      })
        .then(() => resolve(true))
        .catch(() => resolve(false));
    });
    console.log('hasOpenPreApp 222222', hasOpenPreApp);
    if (hasOpenPreApp) {
      // 点是：直接调取目标应用
      return await openTargetApp(appInfo);
    } else {
      // 点否：调前置应用，再打开目标应用
      if (!prerequisite) {
        showToast('未配置前置应用');
        return false;
      }
       // 当前前置应用只能是app，目标应用是app则无法正常打开，所以只打开前置，后续让用户自行打开目标应用
      return await openPreAppByPrerequisite(prerequisite);
    }
  // }
}
