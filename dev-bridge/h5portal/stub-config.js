/**
 * H5Portal 端 Stub 配置
 *
 * EVENT_REGISTER_METHODS: SDK 事件注册方法白名单（业务代码调这些方法注册回调，
 * 桩端把 cb 存到本地，等待 provider 转发事件触发）。
 * 注意: WeSpaceSDK 的回调注册 API 命名不统一，包含 on 前缀、subscribe 前缀等。
 *       onStorageChange 是参数化监听 (key, handler)，由 stub-core 的
 *       hasStorageChangeEvent 开关单独处理，不在此列表。
 */
import {
  spoofChromeWebview,
  spoofFlutterNativeBridge,
  spoofJsBridge,
} from '../page-scripts/env-spoofs.js';

export const EVENT_REGISTER_METHODS = new Set([
  // 通用事件
  'onVisibleChange',
  'onPushTokenChange',
  'onLogout',
  'onSwitchTab',
  'onClose',
  'onStateValue',

  // 用户/状态事件
  'onUserStatusChange',
  'onIcpUserStatusChange',
  'onIntentExecutor',

  // 消息/推送事件
  'subscribeMessage',
  'onRemotePushMessage',

  // 协同事件
  'onAtCooperationUser',
  'onCooperationUserSendMsg',
  'onCooperationGroupCreate',
  'onJoinGroup',

  // 通知/主题
  'onClickNotification',
  'onThemeChanged',

  // 文件共享
  'subscribeFileShare',

  // H5 窗口事件
  'onH5Min',
  'onH5Max',
  'onH5Close',

  // 话权事件
  'onFloorRequest',
  'onFloorRelease',
  'onTaken',
  'onIdle',
  'onGroupRelease',

  // GIS 位置
  'onReceiveGisInfo',

  // 人脸识别
  'onLiveDetectResult',
]);

// 环境伪造: 让 WeSpaceSDK.js 顶层的 window.chrome.webview.addEventListener 不抛错，
// 并让 isWebView2() 等判断通过
export const envSpoofs = [
  spoofChromeWebview,
  spoofFlutterNativeBridge,
  spoofJsBridge,
];
