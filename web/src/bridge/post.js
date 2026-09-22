/**
 * 环境判断
 * 新版 SDK 是 WebView2 宿主，旧版是浏览器宿主
 */

import { isWebView2, isWindows10OrLater } from '../utils/env';
import { queryCoopPostByUserId } from '@/api/collaboration';
import { ElMessage } from 'element-plus'


// 默认头像
const defaultAvater = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFAAAABQCAYAAACOEfKtAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAAEnQAABJ0Ad5mH3gAAAI5SURBVHhe7Zy9TcRAEIVdADk5Eik5LVAGDRDQChJNINEHtAEZERGZ4RnM+ay588+b9YzP70lfcHg1t/dpWE+01U/qLmeX1/X5zX19cffUcPXwvnngA/Rd/bH7gEVWAbHDECl5U+lJlLw5dCRW5gIxDN4XlbpvPk0XSuB8MKVU1gMxDgl0QAJJJJBEAkkkkEQCSSSQRAJJJJBEAkkkkEQCSVILvH3+aLCeZSGVQMh6fftq6Ad/e3z5TCc0jUDIGRustWpEkEKg1XFDySIxXOCUzusH4q2aSxIqkJHXJvpMDBXoFav2UoQJ9Oi+NpFdeBICI8/CMIFz3ryHskmB3rG+YwnUgSQnITByqA4TiDenVySQzCbHGOAxykR2HwgVCNiz0Kq5JOECmX/l6O4D4QJbpnQi1kaee13SCARjzsQMXdcllcAWdBdEgbbbsnRcn5QC14QEkkggSYhAnGc429pzzlozBq86DIsKxA8+lCkCvOp4sJjAMXPemB/vVceL4gKPdcuhWAK86nhTVOCYbjkWCPCoYe3NiyIC53RL6ZQS6S4QG82aEhLdBWYOjgNrzwybEohYe2aQQBIJJJFAEneB7NxWMqt4iWScAdusYowBGWfB1QzSXdCNGbD25kVRgVtAAkkkkEQCSSSQRAJJJJBEAkkkkEQCSSSQRAJJKt0VzSGBBLpDlaQRiKt8rYdimP97pNWF02m6rxUoidPoyNsJBJI4TE8e2PvQgEWS+QumFAAfzdXve66q+huC1AGjaQ7USwAAAABJRU5ErkJggg==';

/**
 * SDK 实例缓存
 */
let sdkInstance = null;
let initPromise = null;

/**
 * 初始化 SDK
 * 根据当前环境加载对应的 SDK
 * @returns {Promise<void>}
 */
export const initSDK = async () => {
  if (sdkInstance) return;
  if (initPromise) return initPromise;

  initPromise = (async () => {
    if (isWebView2()) {
      const module = await import('./WeSpaceSDK-cspc.js');
      sdkInstance = module.default;
    } else {
      const module = await import('./WeSpaceSDK-bspc.js');
      sdkInstance = module.default;
    }
  })();

  return initPromise;
};

/**
 * 获取当前环境的 SDK 实例（确保已初始化）
 */
const getSDK = async () => {
  await initSDK();
  if (!sdkInstance) {
    throw new Error('[WeSpaceSDK] SDK 未初始化');
  }
  return sdkInstance;
};

/**
 * 获取 SDK 实例（用于外部模块访问，可能为 null；请优先用 getSDK()）
 * @returns {object|null} SDK 实例
 */
export const getSDKInstance = () => {
  return sdkInstance;
};

/**
 * 安全调用 SDK 方法
 * @param {string} methodName 方法名
 * @param {any} params 参数
 * @param {any} defaultValue 方法不存在时的默认返回值
 */
const safeCall = async (methodName, params, defaultValue = undefined) => {
  const sdk = await getSDK();
  if (typeof sdk[methodName] === 'function') {
    return sdk[methodName](params);
  }
  console.warn(`[WeSpaceSDK] 方法 ${methodName} 在当前环境不支持`);
  return defaultValue;
};

// ==================== 用户信息相关 ====================

/**
 * @name 获取用户信息
 * @returns {Promise<Object>}
 * 
 * WebView2 新版返回字段兼容映射：
 * - userid → id
 * - username → name
 * - thumbAvatar → avatar
 * - userDepartments → department
 * - getCooperationInfo() → cooperationUsers / cooperationUser
 */
export const getUserInfo = async () => {
  const sdk = await getSDK();
  const data = await sdk.getUserInfo();

  if (!data) {
    return data;
  }

  // 当前用户ID
  const currentUserId = data.userid || data.id;

  // 所有环境统一通过接口获取协同用户信息
  let cooperationUsers = [];
  await queryCoopPostByUserId({ userId: currentUserId })
    .then((res) => {
      if (res && res.code === 0 && Array.isArray(res.data)) {
        cooperationUsers = res.data.map((item) => ({
          userId: item.userId,
          name: item.name || item.postName,
          groupIds: item.groupIds || [],
          avatar: '',
          isdn: item.isdn || '',
          serviceStatus: item.serviceStatus || null,
        }));
      }
    })
    .catch((e) => {
      console.warn('[WeSpaceSDK] queryCoopPostByUserId 调用失败:', e);
    });

  // cooperationUser：优先取协同岗列表的第一个
  const cooperationUser = cooperationUsers[0] || null;

  // WebView2 环境：兼容字段映射
  if (isWebView2()) {
    return {
      ...data,
      id: currentUserId,
      name: data.username || data.name,
      avatar: data.thumbAvatar || data.avatar || defaultAvater,
      department:
        data.userDepartments?.find((d) => d.isPrimary) ||
        data.userDepartments?.[0] ||
        data.department,
      cooperationUsers,
      cooperationUser,
    };
  }

  // Browser 环境：保留原有数据，覆盖协同用户信息
  return {
    ...data,
    cooperationUsers,
    cooperationUser,
  };
};

// ==================== 聊天窗口相关 ====================

/**
 * @name 打开聊天窗口
 * @param {object} params
 * @param {string} params.groupId 群组ID (旧版)
 * @param {string} params.msgid 消息ID，用于高亮 (新版)
 * @param {boolean} params.showClose 显示关闭按钮，默认true (旧版)
 * @param {boolean} params.draggable 窗口是否可拖拽 (旧版)
 * @param {string} params.width 窗口宽度 (旧版)
 * @param {string} params.height 窗口高度 (旧版)
 * @param {object} params.position 窗口位置信息 (旧版)
 * 
 * @returns {Promise<object|void>}
 * - 旧版(bspc): 返回 { result: 'close' } 表示窗口已关闭
 * - 新版(cspc): 无返回值
 */
export const openChatUI = async (params, isReply = false) => {
  const sdk = await getSDK();
  if (isWebView2()) {
    // cspc 环境：不支持窗口布局参数
    if (params?.draggable || params?.width || params?.height || params?.position || params?.showClose) {
      console.warn('[WeSpaceSDK] 新版 openChatUI 不支持窗口布局参数 (draggable, width, height, position, showClose)，这些参数将被忽略');
    }
    const newParams = isReply ? {
      type: params?.type || '2',
      isdn: params?.isdn || params?.groupId,
      msgid: params?.msgid,
      atUserId: params?.atUserId,
      RefMsgId: params?.msgid,
    } : {
      type: params?.type || '2',
      isdn: params?.isdn || params?.groupId,
      msgid: params?.msgid,
    };
    console.info('[WeSpaceSDK] openChatUI 调用参数:', newParams);
    return sdk.openChatUI(newParams);
  }
  // bspc 环境：支持窗口布局参数，默认显示关闭按钮
  return sdk.openChatUI({
    ...params,
    showClose: params?.showClose ?? true,
  });
};

export const closeChatUI = async () => {
  const sdk = await getSDK();
  return sdk.closeChatUI();
};

export const openChat = async (params) => {
  const sdk = await getSDK();
  if (isWebView2()) {
    const newParams = {
      id: params?.id || params?.groupId,
      category: params?.category || '2',
    };
    return sdk.sms(newParams);
  }
  return sdk.openChat(params);
};

// ==================== 选人组件相关 ====================

export const openSelectMemberUI = async (params) => {
  const sdk = await getSDK();

  // WebView2 新版：selectMembers 会直接返回选择结果（不走 onSelectedMembers 回调）
  if (isWebView2()) {
    const newParams = {
      maxNumber: params?.maxLength || 49,
      addMembers: params?.checkuser?.map((u) => u.userId).join(',') || '',
      fullPathCode: params?.departmentId || '',
      mode: params?.mode || '0',
    };
    return sdk.selectMembers(newParams);
  }

  // 浏览器旧版：仍保持原有 openSelectMemberUI + onSelectedMembers 回调机制
  return sdk.openSelectMemberUI(params);
};

/**
 * 兼容选人结果：
 * - Browser(旧版)：注册回调，等待宿主通过 onSelectedMembers 推送结果
 * - WebView2(新版)：selectMember/selectMembers 会直接返回选择结果；这里不再二次触发选人 UI
 */
export const onSelectedMembers = async (handler) => {
  if (isWebView2()) {
    console.warn('[WeSpaceSDK] WebView2 环境下 onSelectedMembers 不会收到宿主回调，请使用 openSelectMemberUI 的返回值');
    return;
  }

  return safeCall('onSelectedMembers', handler);
};

// ==================== 群组相关 ====================

/**
 * @name 创建群聊
 * @param {object} params
 * @param {array} params.addMembers 邀请的成员id列表
 * @param {string} params.groupName 群名称
 * @param {string} params.groupType 2:普通群，3:协同群 (旧版) | 1:群聊组，3:协同组 (新版)
 * 
 * 新版兼容说明：
 * - 新版 groupType 值不同：旧版 2=普通群，新版 1=群聊组
 * - 新版 addMembers 是逗号分隔的字符串，旧版是数组
 */
export const createGroup = async (params) => {
  const sdk = await getSDK();
  if (isWebView2()) {
    // 新版 SDK
    let groupType = params.groupType;
    // 旧版 groupType 2 对应新版 1
    if (groupType === '2' || groupType === 2) {
      groupType = '1';
    }
    const newParams = {
      groupName: params.groupName,
      groupType: String(groupType),
      addMembers: Array.isArray(params.addMembers)
        ? params.addMembers.join(',')
        : params.addMembers,
      needSelectMember: params.needSelectMember || false,
    };
    return sdk.createGroup(newParams);
  } else {
    // 旧版 SDK
    return sdk.createGroup(params);
  }
};

/**
 * @name 加入群组
 * @param {object} params
 * @param {string | number} params.groupId 群组ID
 * @param {string} [params.ownerId] 群主ID (旧版)
 * @param {string} [params.addWording] 申请理由 (新版)
 */
export const joinGroup = async (params) => {
  const sdk = await getSDK();
  if (isWebView2()) {
    // 新版 SDK
    const newParams = {
      groupId: params.groupId,
      ownerId: params.ownerId || '', // sdk 参数名不同  文档里写的ownerId
      addWording: params.addWording || '',
    };
    return await sdk.joinGroup(newParams);
  } else {
    // 旧版 SDK
    return await sdk.joinGroup(params);
  }
};

// ==================== 消息相关 ====================

/**
 * @name 回复消息
 * @param {object} params
 * @param {string} params.groupId 群组id
 * @param {string} params.message 回复的文本消息
 * @param {object | undefined} params.quote 引用消息
 * @param {string} params.quote.msgId 引用消息Id
 * 
 * 新版兼容说明：
 * - 新版使用 sendTextMsg 方法
 * - 新版不支持引用消息功能 (quote 参数)
 */
export const reply = async (params) => {
  const sdk = await getSDK();
  if (isWebView2()) {
    // 新版 SDK 使用 sendTextMsg，但不支持引用消息
    console.warn('[WeSpaceSDK] 新版 SDK 不支持 reply 的引用消息功能，已降级为 sendTextMsg');
    const newParams = {
      type: '2', // 默认群聊
      contacts: params.groupId,
      text: params.message,
    };
    return sdk.sendTextMsg(newParams);
  } else {
    // 旧版 SDK
    return sdk.reply(params);
  }
};

/**
 * 高亮群组消息
 * @param {Object} params
 * @param {String} params.groupId 群组ID
 * @param {String} params.msgId 消息ID
 * 
 * 新版兼容说明：
 * - 新版 SDK 已取消此独立方法
 * - 新版通过 openChatUI 的 msgid 参数实现高亮功能
 * - 此方法在新版环境下会输出警告，不会自动调用 openChatUI
 * 
 * 使用建议：
 * // 旧版代码
 * openChatUI({ groupId: '123' });
 * setTimeout(() => highlightMsg({ groupId: '123', msgId: 'msg001' }), 1500);
 * 
 * // 新版代码（推荐）
 * openChatUI({ type: '2', isdn: '123', msgid: 'msg001' });
 */
export const highlightMsg = async (params) => {
  const sdk = await getSDK();
  if (isWebView2()) {
    // 新版 SDK 不支持 highlightMsg
    // 注意：这里不自动调用 openChatUI，因为通常调用 highlightMsg 时聊天窗口已经打开
    // 如果需要同时打开窗口并高亮，应该直接使用 openChatUI({ ..., msgid: '...' })
    console.warn('[WeSpaceSDK] 新版 SDK 已取消 highlightMsg 方法，请在 openChatUI 中使用 msgid 参数实现高亮功能');
    return;
  } else {
    // 旧版 SDK
    return sdk.highlightMsg(params);
  }
};

/**
 * 发送文本消息
 * @param {Object} params
 * @param {String} params.groupId 群组ID (旧版)
 * @param {String} params.text 发送的文本
 * @param {String | undefined} params.srcMsgId 引用的消息ID (旧版，新版不支持)
 * @param {String} params.type 类型 "1"-单聊 "2"-群聊 (新版)
 * @param {String} params.contacts 群组id或用户id (新版)
 * 
 * 新版兼容说明：
 * - 新版需要 type 和 contacts 参数
 * - 新版不支持引用消息功能 (srcMsgId 参数)
 */
export const sendTextMsg = async (params) => {
  const sdk = await getSDK();
  if (isWebView2()) {
    // 新版 SDK
    if (params.srcMsgId) {
      console.warn('[WeSpaceSDK] 新版 SDK 不支持 sendTextMsg 的引用消息功能 (srcMsgId 参数)');
    }
    const newParams = {
      type: params.type || '2', // 默认群聊
      contacts: params.contacts || String(params.groupId),
      text: params.text,
    };
    console.log(newParams, 'newParams');
    return sdk.sendTextMsg(newParams);
  } else {
    // 旧版 SDK
    return sdk.sendTextMsg(params);
  }
};

// ==================== 主题相关 ====================

/**
 * 切换主题
 * 注意：新版 SDK 已取消此方法
 */
export const switchTheme = async (params) => {
  if (isWebView2()) {
    console.warn('[WeSpaceSDK] 新版 SDK 已取消 switchTheme 方法');
    return;
  }
  return safeCall('switchTheme', params);
};

/**
 * @name 获取主题
 * @returns {Promise<Object>}
 * @property {string} theme 主题
 */
export const getCurrentTheme = async () => {
  const sdk = await getSDK();
  if (isWebView2()) {
    // 新版 SDK 使用 getTheme 方法
    return sdk.getTheme();
  } else {
    // 旧版 SDK
    return sdk.getCurrentTheme();
  }
};

// ==================== 页面跳转相关 ====================

/**
 * @name 打开URL页面
 * @param {object|string} params 参数对象或URL字符串
 * @param {string} params.url 页面地址
 * @param {string} params.title 页面标题 (新版)
 * @param {string} params.titleStyle 页面样式 (新版)
 * @param {boolean} params.watermark 是否显示水印 (新版)
 */
export const openUrl = async (params) => {
  const sdk = await getSDK();
  if (isWebView2()) {
    // 新版 SDK 参数为独立参数
    const url = typeof params === 'string' ? params : params.url;
    const title = params.title || '';
    const titleStyle = params.titleStyle || '';
    const watermark = params.watermark || false;
    return await sdk.openUrl(url, title, titleStyle, watermark);
  } else {
    // 旧版 SDK 参数为对象
    return await sdk.openUrl(typeof params === 'string' ? { url: params } : params);
  }
};

/**
 * 打开窗口
 */
export const openWindow = async (url) => {
  if (isWebView2Env()) {
    openUrl(url);
  } else {
    window.open(url, '_blank');
  }
}

/**
 * @name 关闭页面
 */
export const close = async () => {
  const sdk = await getSDK();
  return await sdk.close();
};

// ==================== 导出环境判断方法 ====================

/**
 * 判断是否为 WebView2 环境
 */
export const isWebView2Env = isWebView2;

/**
 * 获取当前 SDK 版本信息
 */
export const getSDKVersion = () => {
  return isWebView2() ? 'new (WebView2)' : 'old (Browser)';
};

// ==================== 卡片消息相关 ====================

/**
 * @name 发送自定义卡片
 * @param {object} param
 * @param {string} param.level 级别blue/orange/yellow/red(一般/关键/重要/紧急)
 * @param {string} param.title 标题
 * @param {string} param.describe 描述
 * @param {string} [param.jumpType] 跳转类型 (1-普通url 2-全屏url 3-小程序)
 * @param {string} [param.appUrl] 跳转小程序时生效,需要和小程序配置的url保持一致
 * @param {string} [param.url] 跳转url
 * @param {string} [param.thumb] 缩略图(base64格式)
 * @param {string} [param.type] 类型 0-三方卡片(默认值) 1-任务卡片
 * @param {string} [param.time] 时间戳 精确到秒(type=1必填)
 * @param {string} [param.taskTypeName] 任务类型名称(type=1必填)
 * @param {string} [param.levelName] 级别名，可不填，不填时不显示(最大长度6，超过截取前6个字符)
 * 
 * @returns {Promise<any>}
 */
export const sendCustomCard = async (param) => {
  const sdk = await getSDK();
  return await sdk.sendCustomCard(param);
};

// ==================== 角标相关 ====================

/**
 * @name 设置角标
 * @param {object} params 角标参数
 * @param {number} params.count 角标数量
 * @param {string} params.appId 应用ID 布局的itemKey
 * 
 * 仅在 WebView2 (cspc) 环境下生效
 */
export const setBadge = async (params) => {
  // 仅在 cspc 环境下触发
  if (!isWebView2()) {
    return;
  }
  const sdk = await getSDK();
  if (typeof sdk.setBadge === 'function') {
    const param = {
      count: params?.count,
      appId: params?.appId,
    };
    return sdk.setBadge(param);
  }
};

// ==================== 文件下载相关 ====================

/**
 * @name 下载文件
 * @param {object} params
 * @param {string} params.url 文件URL
 * @param {string} params.fileName 文件名
 * @param {string} [params.fileSize] 文件大小,单位字节。不需要换算单位，content-length 字段的值。
 * @param {string} [params.header] 请求 token，警信不做校验，解析为 k-v直接用于下载，调用方（H5）和下载文件对应的服务器（服务端）协商好就可以，警信只做透传。
 * 
 * @returns {Promise<any>}
 */
export const downloadFile = async (params) => {

  // 仅在 cspc 环境并且windows10以上触发
  if(isWebView2()){
    const sdk = await getSDK();

    if(isWindows10OrLater()) {
      await sdk.downloadFile(params); 
    } else {
      // windows10以下警务协同为浏览器窗口打开 需要使用浏览器下载方式
      await createDownloadLink(params);
    }
  } else {
    await createDownloadLink(params);
  }
};

/**
 * 创建a标签下载文件
 * @param {object} params
 * @param {string} params.url 文件URL
 * @param {string} params.fileName 文件名
 */
export const createDownloadLink = async (params) => {
  try {
    const response = await fetch(params.url);
    if (!response.ok) {
      ElMessage.error(`下载失败：${response.status}`);
      return Promise.reject(`下载失败：${response.status}`);
    }
    const blob = await response.blob();

    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = params.fileName;
    link.click();
    URL.revokeObjectURL(link.href);
    link.remove();

    ElMessage.warning({
      message: '开始下载,请在右上角下载记录中查看下载进度！',
      type: 'warning',
    });
    return Promise.resolve();
  } catch (err) {
    ElMessage.error('下载失败，请稍后重试');
    console.error('[downloadFile] failed:', err);
    return Promise.reject(err);
  }
};
