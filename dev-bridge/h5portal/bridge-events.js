/**
 * H5Portal 端 SDK 事件桥配置
 *
 * WeSpaceSDK 的回调注册 API 命名不统一（既有 onXxx 也有非 on 前缀的），
 * 这里显式列出业务实际使用的方法，避免误拦截。
 * 字段格式: { method: 注册方法名, handlerField: SDK 内部存储 handler 的字段 }
 * （handlerField 仅为文档说明，provider-core 只使用 method）
 *
 * 注意: onStorageChange 是参数化监听 (key, handler)，在 provider-core 里特殊处理，
 *       不在此列表。subscribeFileShare 注册时还会调用 web2WeSpaceCall，provider 端按需调用真实 SDK。
 */
export const BRIDGE_EVENTS = [
  // 通用事件
  { method: 'onVisibleChange', handlerField: 'visibleChangeHandler' },
  { method: 'onPushTokenChange', handlerField: 'pushTokenChangeHandler' },
  { method: 'onLogout', handlerField: 'logoutHandler' },
  { method: 'onSwitchTab', handlerField: 'switchTabHandler' },
  { method: 'onClose', handlerField: 'closeHandler' },
  { method: 'onStateValue', handlerField: 'stateValueHandler' },

  // 用户/状态事件
  { method: 'onUserStatusChange', handlerField: 'statusChangeHandler' },
  { method: 'onIcpUserStatusChange', handlerField: 'statusIcpChangeHandler' },
  { method: 'onIntentExecutor', handlerField: 'onIntentExecutorHandler' },

  // 消息/推送事件
  { method: 'subscribeMessage', handlerField: 'messageHandler' },
  { method: 'onRemotePushMessage', handlerField: 'pushMessageHandler' },

  // 协同事件
  { method: 'onAtCooperationUser', handlerField: 'onAtCooperationUserHandler' },
  { method: 'onCooperationUserSendMsg', handlerField: 'onCooperationUserSendMsgHandler' },
  { method: 'onCooperationGroupCreate', handlerField: 'onCooperationGroupCreateHandler' },
  { method: 'onJoinGroup', handlerField: 'joinGroupHandle' },

  // 通知/主题
  { method: 'onClickNotification', handlerField: 'clickNotificationHandler' },
  { method: 'onThemeChanged', handlerField: 'onThemeChangeHandler' },

  // 文件共享（注册时还会调用 web2WeSpaceCall）
  { method: 'subscribeFileShare', handlerField: 'shareFileHandler' },

  // H5 窗口事件
  { method: 'onH5Min', handlerField: 'h5MinHandler' },
  { method: 'onH5Max', handlerField: 'h5MaxHandler' },
  { method: 'onH5Close', handlerField: 'h5CloseHandler' },

  // 话权事件
  { method: 'onFloorRequest', handlerField: 'onFloorRequestHandler' },
  { method: 'onFloorRelease', handlerField: 'onFloorReleaseHandler' },
  { method: 'onTaken', handlerField: 'onTakenHandler' },
  { method: 'onIdle', handlerField: 'onIdleHandler' },
  { method: 'onGroupRelease', handlerField: 'onGroupReleaseHandler' },

  // GIS 位置
  { method: 'onReceiveGisInfo', handlerField: 'gisInfoCallHandler' },

  // 人脸识别
  { method: 'onLiveDetectResult', handlerField: 'onLiveDetectHandler' },
];
