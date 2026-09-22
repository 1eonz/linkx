import { useSocketManage } from '@/hooks';
// import { mspLogin } from '@/plugins/mspPlayer/mspPlayerLogin';
// import { useResourceStore } from '@/store';

// import { initControlTask } from './controlTask';
// import { initEventAndMission } from './eventAndMission';
// import { initFavorite } from './favorite';
// import { initGroupList } from './groupList';
// import { initMessage } from './message';
// import { initResource } from './resource';
// import { initVideoControlAlarm } from './videoControlAlarm';

/**
 * 初始化通信及加载全局数据
 */
export function initGlobalData() {
  // 登录通信
  // mspLogin();

  // 创建websocket
  useSocketManage().initWebSocket();

  // 收藏夹
  // if (usePermissions('DISPATCH')) {
  //   initFavorite();
  // }

  // 紧急短信
  // initMessage();

  // 设防
  // if (usePermissions('CONTROL')) {
  //   initControlTask();
  // }

  // 预警
  // if (usePermissions('ALARM')) {
  //   initVideoControlAlarm();
  // }

  // 加载admin配置的icon
  // useResourceStore().saveGlobalIcon();

  // const triggerResource = initResource();
  // const triggerEventAndMission = initEventAndMission();
  // const triggerGroup = initGroupList();
  // const initAsyncResource = () => {
  //   // 图上资源
  //   triggerResource();
  //   // // 任务
  //   // if (usePermissions('MISSION')) {
  //   //   triggerEventAndMission?.();
  //   // }
  //   // // 群组
  //   // if (usePermissions('DISPATCH')) {
  //   //   triggerGroup();
  //   // }
  // };

  // return { initAsyncResource };
}
