import { queryEquipmentDetailById } from '@/api/equipment';
import { queryExecutorDetailByAccount, queryExecutorDetailById } from '@/api/executor';
import { queryFacilityDetailById } from '@/api/facility';
import { findSubscribeTalkingGroup, subscribeTalkingGroup } from '@/api/group';
import { queryServiceAccountTypeByAccount } from '@/api/resource';
import { Dialog } from '@/components/Dialog';
import { Message } from '@/components/Message';
import MessageBox from '@/components/MessageBox';
import { appConfig, bizStatus } from '@/config';
import { CategoryEnum, LayerIdEnum, PoliceDutyStatusEnum, VideoCallEnum } from '@/enums';
import { useEmitter, useI18n, usePermissions, useUtils } from '@/hooks';
import { openVideoPointCallPopup } from '@/pages/map/openVideoPopup';
import HistoryTrack from '@/pages/trackReplay/historyTrack.vue';
import RealTimeTrack from '@/pages/trackReplay/realTimeTrack.vue';
import { mapManager } from '@/plugins/map';
import { groupFunc } from '@/plugins/mspPlayer';
import commOpt from '@/plugins/mspPlayer/commOpt';
import {
  useCommunicateDispatchStore,
  useCommunicationStore,
  useConferenceStore,
  useMissionStore,
  useMonitorStore,
  useResourceStore,
  useVideoPollStore,
} from '@/store';
import { delay } from '@/utils';
import { isEmpty, isNumber } from '@/utils/is';

import { cloneDeep } from 'lodash-es';

type LayerShow = {
  layerId: string;
  visible: boolean;
};

/**
 * @description 语音点呼
 * @param {*} account
 * @param {*} dialout 电话呼叫
 * @returns
 */
export async function voicePointCall(account: string, dialout?: boolean) {
  const { t } = useI18n();

  if (!account) {
    Message(t('communication.communicationTips.communicationAccountNotAssociated'));
    return;
  }

  const { capability, type } = await getInfoByAccount(account);
  if (capability && !capability.includes('512001')) {
    Message(t('communication.communicationTips.equipmentCapabilityNotSupported'));
    return;
  }

  if (type === 3) {
    Message(t('communication.communicationTips.cantVoiceCallMonitor'));
    return;
  }

  const dialType = dialout ? 'dialout' : 'dial';
  commOpt.audioCall(account, dialType);
}

/**
 * @description 半双工点呼
 * @param {*} id
 * @param {*} resourceType
 * @returns
 */
export async function voiceHalfCall(data) {
  const { t } = useI18n();
  const { account, capability } = await queryResourceDetailsByInfo(data);

  if (!account) {
    Message(t('communication.communicationTips.communicationAccountNotAssociated'));
    return;
  }

  if (capability && !capability.includes('512001')) {
    Message(t('communication.communicationTips.equipmentCapabilityNotSupported'));
    return;
  }

  commOpt.voiceHalfCall(account);
}

/**
 * @description 视频点呼
 * @param {*} id
 * @param {*} resourceType
 * @param {*} onlineStatus
 * @returns
 */
export async function videoPointCall(data) {
  if (!commOpt.isCommReady()) {
    return;
  }

  const { t } = useI18n();
  const { onlineStatus } = data;
  const { account, capability } = await queryResourceDetailsByInfo(data);

  if (!account) {
    Message(t('communication.communicationTips.communicationAccountNotAssociated'));
    return;
  }

  if (commOpt.isAlreadyCommForSend('video', account)) {
    return;
  }

  if (onlineStatus !== 'online') {
    if (capability && !capability?.includes('512006')) {
      Message(t('communication.communicationTips.equipmentCapabilityNotSupported'));
      return;
    }

    openVideoPointCallPopup({ ...data, account });

    setTimeout(() => {
      commOpt.videoCall({
        container: VideoCallEnum.CONTAINER + account,
        fromUid: appConfig.isdn,
        toUid: account,
      });
    }, 300);
  }
}

/**
 * @description 视频查看
 * @param {*} id
 * @param {*} resourceType
 * @param {*} onlineStatus
 * @returns
 */
type MonitorCallParam = {
  [key: string]: any;
  resourceType?: string;
};
export async function monitorCall(data: MonitorCallParam, account?: string) {
  const { t } = useI18n();
  const serviceAccounts = data.serviceAccounts || [];
  if (!account) {
    account = getAccountByEquipmentData(data);
  }

  if (!account) {
    Message(t('communication.communicationTips.communicationAccountNotAssociated'));
    return;
  }

  for (const item of serviceAccounts) {
    // usage 0:icc 1:recorder 2:terminal 3:else
    if (item.account === account && item.usage === 0) {
      Message({
        message: t('communication.communicationTips.unableViewDispatcher'),
        type: 'warning',
      });
      return;
    }
  }

  if (data.onlineStatus !== 'online') {
    if (data.capability && !data.capability.includes('512007')) {
      Message(t('communication.communicationTips.equipmentCapabilityNotSupported'));
      return;
    }

    if (commOpt.isCommReady(account)) {
      useMonitorStore().addMonitorDrawerData(data);
    }
  }
}

/**
 * @description 动向回放
 * @param {string} id
 * @param {*} resourceType
 * @param {any} time
 */
type TrackPlayParam = {
  alarm?: boolean;
  infoData?: any;
  resourceType: string;
  showEquipmentTabs?: boolean;
  showSearch?: boolean;
  time?: Array<Date | string> | null;
  title?: string;
  tracks?: any[];
};
export async function trackPlay(opt: TrackPlayParam) {
  const { alarm, infoData, resourceType, showEquipmentTabs, showSearch, time, title, tracks } = opt;
  const config = {
    cid: 'trackCard',
    content: HistoryTrack,
    data: {
      alarm,
      infoData,
      resourceType,
      showEquipmentTabs,
      showSearch,
      time,
      title,
      tracks,
    },
    shade: true,
    zIndexDefault: 1500,
  };

  Dialog(config);
}

/**
 * @description 实时轨迹跟踪
 * @param {*} infoData
 * @param {*} settingTime
 * @returns
 */
export async function trackRealTimePlay({ infoData, settingTime }) {
  const config = {
    cid: 'realTimeTrackCard',
    content: RealTimeTrack,
    data: {
      infoData,
      settingTime,
    },
    shade: true,
  };

  Dialog(config);
}

/**
 * @description 单击定位
 * @param {*} data
 * @param {*} type
 * @param {*} layerVisible 固定显示
 * @returns
 */
export async function singleClick({ data, type = 'person' }, layerVisible?: boolean) {
  const { isMapPanel } = useUtils();
  if (!isMapPanel) {
    return;
  }

  // 会议终端没有图层
  if (Number(data.category) === CategoryEnum.confTerminal) {
    return;
  }

  if (Number(data.category) === CategoryEnum.carPhoto) {
    type = 'equipment';
  }

  const { t } = useI18n();
  if (!data) {
    Message(t('mission.missionMap.nonPositionInfo'));
    return;
  }

  const api = {
    equipment: queryEquipmentDetailById,
    monitor: queryFacilityDetailById,
    person: queryExecutorDetailById,
  };
  // 获取详情
  if (api[type]) {
    const { id, resourceId } = data;
    const res = await api[type]({ id: resourceId || id });
    if (res.code === 0 && res.data) {
      data = res.data;
    }
  }

  // 获取定位
  const { lat, latitude, location, lon, longitude, position } = data;
  if (!position) {
    if (location) {
      const { lat, lon } = location.coordinates[0];
      if (lat && lon) {
        data.position = [lon, lat];
      } else {
        Message(t('mission.missionMap.nonPositionInfo'));
        return;
      }
    } else if (lon && lat) {
      data.position = [lon, lat];
    } else if (latitude && longitude) {
      data.position = [longitude, latitude];
    } else {
      Message(t('mission.missionMap.nonPositionInfo'));
      return;
    }
  }

  const res = getFilterLayerSelectionStatus(data, type);
  if (!res) {
    return;
  }

  const { layerId, visible } = res as LayerShow;
  if (!layerVisible && !visible) {
    Message(t('resource.map.layerIsHidden'));
    return;
  }

  if (type === 'warn') {
    data.id = data.alarmId;
  }

  const maxZoom = Number(appConfig.settingData.MAX_ZOOM);
  const zoom = Math.min(maxZoom, 17);

  const map = mapManager.get('mapId_main');
  map.setCenter(data.position, zoom);
  map.addMarkerCustomPopup(layerId, data);
}

/**
 * 根据通信号获取人员/装备/摄像头信息
 * 设备被领用后展示警员信息
 * @param {*} account 通信号
 * @param {*} queryExecutor 查询领用人
 */
export async function getInfoByAccount(account: string, queryExecutor = true): Promise<any> {
  const param = { account };
  let ret = {};

  // 查看是否有领用人
  if (queryExecutor) {
    const { code, data } = await queryExecutorDetailByAccount(param);
    if (code === 0 && data) {
      return { ...data, account, category: CategoryEnum.person };
    }
  }

  // type 0 未知，1 人，2 装备，3 摄像头
  const { code, data } = await queryServiceAccountTypeByAccount(param);
  if (code === 0) {
    const { equipmentDetail, executorDetail, facility, type } = data;
    switch (type * 1) {
      case 1: {
        ret = { ...executorDetail, type };
        break;
      }
      case 2: {
        ret = { ...equipmentDetail, type };
        break;
      }
      case 3: {
        ret = { ...facility, type };
        break;
      }
    }
  }
  return ret;
}

/**
 * 根据通信号获取人员信息
 * @param {*} account 通信号
 */
export async function getExecutorInfoByAccount(account) {
  let ret = null;
  const param = { account };
  const { code, data } = await queryExecutorDetailByAccount(param);
  if (code === 0 && data) {
    ret = { ...data, category: CategoryEnum.person };
  }
  return ret;
}

/**
 * @description 返回筛选图层是否选中
 * @param {*} data
 * @param {*} type
 * @returns {boolean}
 */
function getFilterLayerSelectionStatus(data, type): boolean | LayerShow {
  const status = `${data.bizStatus || data.state || data.status}`;
  const category = Number(data.category);

  const ret = (layerId) => {
    return {
      layerId,
      visible: useResourceStore().layerChecked.includes(layerId),
    };
  };

  const online = getOnlineStatus(data) === 'online';

  switch (type) {
    case 'car': {
      return ret(LayerIdEnum.policeCar);
    }
    case 'equipment': {
      switch (category) {
        case CategoryEnum.ballCamera: {
          return online ? ret(LayerIdEnum.ballCameraOnline) : ret(LayerIdEnum.ballCameraOffline);
        }
        case CategoryEnum.carPhoto: {
          return online ? ret(LayerIdEnum.carPhotoOnline) : ret(LayerIdEnum.carPhotoOffline);
        }
        case CategoryEnum.confTerminal: {
          return online ? ret('confTerminalOnline') : ret('confTerminalOffline');
        }
        case CategoryEnum.GBRecorder: {
          return online ? ret(LayerIdEnum.GBRecorderOnline) : ret(LayerIdEnum.GBRecorderOffline);
        }
        case CategoryEnum.pdt: {
          return online ? ret(LayerIdEnum.pdtOnline) : ret(LayerIdEnum.pdtOffline);
        }
        case CategoryEnum.recorder: {
          return online ? ret(LayerIdEnum.recorderOnline) : ret(LayerIdEnum.recorderOffline);
        }
        case CategoryEnum.seat: {
          return online ? ret(LayerIdEnum.seatOnline) : ret(LayerIdEnum.seatOffline);
        }
        case CategoryEnum.terminal: {
          return online ? ret(LayerIdEnum.terminalOnline) : ret(LayerIdEnum.terminalOffline);
        }
        case CategoryEnum.uav: {
          return online ? ret(LayerIdEnum.uavOnline) : ret(LayerIdEnum.uavOffline);
        }
      }
      break;
    }
    case 'mission': {
      const { getStatus } = useMissionStore();
      if (getStatus.pending.includes(status)) {
        return ret(LayerIdEnum.taskWaiting);
      } else if (getStatus.underway.includes(status)) {
        return ret(LayerIdEnum.taskGoing);
      } else {
        return ret(LayerIdEnum.taskCompleted);
      }
    }
    case 'monitor': {
      if (category === CategoryEnum.monitor) {
        return online ? ret(LayerIdEnum.monitorOnline) : ret(LayerIdEnum.monitorOffline);
      }
      break;
    }
    case 'order': {
      if (status === '0') {
        return ret(LayerIdEnum.orderWaiting);
      } else if (['1', '2'].includes(status)) {
        return ret(LayerIdEnum.orderGoing);
      } else if (['3', '4'].includes(status)) {
        return ret(LayerIdEnum.orderCompleted);
      }
      break;
    }
    case 'person': {
      const { policeLayerChecked } = useResourceStore();
      const duty: Set<LayerIdEnum> = new Set([
        LayerIdEnum.personBusy,
        LayerIdEnum.personDimission,
        LayerIdEnum.personFree,
      ]);
      let hasDuty = false;
      policeLayerChecked.forEach((item) => {
        if (duty.has(item as LayerIdEnum)) {
          hasDuty = true;
        }
      });

      const layerId = online ? LayerIdEnum.personOnline : LayerIdEnum.personOffline;
      if (hasDuty) {
        const { attendance } = data;
        switch (attendance) {
          case PoliceDutyStatusEnum.BUSY: {
            return {
              layerId,
              visible: policeLayerChecked.includes(LayerIdEnum.personBusy),
            };
          }
          case PoliceDutyStatusEnum.DIMISSION: {
            return {
              layerId,
              visible: policeLayerChecked.includes(LayerIdEnum.personDimission),
            };
          }
          case PoliceDutyStatusEnum.FREE: {
            return {
              layerId,
              visible: policeLayerChecked.includes(LayerIdEnum.personFree),
            };
          }
          default: {
            return {
              layerId,
              visible: false,
            };
          }
        }
      } else {
        return ret(layerId);
      }
    }
    case 'warn': {
      const key = status === '1' ? LayerIdEnum.alarmCompleted : LayerIdEnum.alarmWaiting;
      return ret(key);
    }
  }

  return false;
}

/**
 * 获取在线状态
 * @param {*} bizStatus
 * @return offline/online
 */
export function getOnlineStatus(data): 'busy' | 'offline' | 'online' {
  if (data.bizStatus !== undefined) {
    return bizStatus[data.bizStatus];
  }
  if (data.status !== undefined) {
    return bizStatus[data.status];
  }
  return bizStatus[data.state];
}

/**
 * 获取资源图标
 * @param {*} data
 * @param {*} isBig 是否为大头像
 * @return url
 */
export function getResourceAvatar(data, isBig?) {
  const { category, isHeader, vehicle } = data;
  const categoryNum = Number(category);
  let img = '';

  // 头车
  if (isHeader) {
    img = 'header_car';
  } else if (vehicle) {
    img = 'vehicle';
  } else {
    switch (categoryNum) {
      case CategoryEnum.ballCamera: {
        img = 'ballCamera';
        break;
      }
      case CategoryEnum.carPhoto: {
        img = 'car';
        break;
      }
      case CategoryEnum.confTerminal: {
        img = 'conference';
        break;
      }
      case CategoryEnum.GBRecorder: {
        img = 'GBRecorder';
        break;
      }
      case CategoryEnum.monitor: {
        img = 'monitor';
        break;
      }
      case CategoryEnum.pdt: {
        img = 'pdt';
        break;
      }
      case CategoryEnum.person: {
        img = 'person';
        break;
      }
      case CategoryEnum.recorder: {
        img = 'recorder';
        break;
      }
      case CategoryEnum.seat: {
        img = 'seat';
        break;
      }
      case CategoryEnum.terminal: {
        img = 'terminal';
        break;
      }
      case CategoryEnum.uav: {
        img = 'uav';
        break;
      }
      default: {
        img = 'group';
      }
    }
  }

  if (isBig) {
    return `img-${img}`;
  }
  return `tree_${img}`;
}

/**
 * 多屏跳转
 * @param item
 * @param router
 * @returns
 */
export async function multiScreenJump(item, router) {
  if (location.pathname.includes(item.path)) {
    return;
  }
  const { t } = useI18n();
  const {
    addMonitorList,
    clearMonitor,
    monitorDesktopList,
    setCloseConferFlag,
    setMonitorDesktopListCopy,
  } = useCommunicateDispatchStore();
  const { uniqueCode } = useConferenceStore();
  const { isCommPanel } = useUtils();
  const { clearDrawerData, clearPollTimer, restartPoll, setPausePlayPoll } = useVideoPollStore();
  const monitorList = cloneDeep(monitorDesktopList).filter((i) => !!i);

  const { isExtended } = window.screen as any;
  if (isExtended) {
    setMonitorDesktopListCopy();
    clearMonitor();

    const pathInfo = router.resolve({ path: item.path });
    const { getScreenDetails } = window;
    // 屏幕的数组
    let screens: any = [];
    if (getScreenDetails) {
      const res = await window.getScreenDetails();
      screens = res.screens;
    } else {
      console.info('not supported window.getScreenDetails !');
    }

    // 副屏幕的获取 isPrimary(是否是主屏)
    const secondScreen = screens.find((screen) => screen.isPrimary === false) || {};
    const { availLeft, height, width } = secondScreen;

    const popup: any =
      window.open(pathInfo.href, 'My Popup', `left=0,top=0,width=${width},height=${height}`) || {};
    popup.moveTo(availLeft, 0);

    setTimeout(() => {
      const data = { data: monitorList, type: 'projection' };
      popup.postMessage(data, '*');
    }, 3000);
    return popup;
  } else {
    if (isCommPanel) {
      const monitoring = monitorList.length > 0;
      if ((monitoring && !clearPollTimer) || uniqueCode) {
        const type: string[] = [];
        if (uniqueCode) {
          type.push(t('videoConference.conference'));
        }
        if (monitoring) {
          type.push(t('desktop.name.monitor'));
        }

        const msg = t('resource.poll.leavePageWillClose') + type.join('，');
        const res = await MessageBox({
          text: msg,
          title: t('resource.poll.isLeavePage'),
          type: 'ok',
        });
        if (!res) return;
      }
    }

    // 轮巡列表切换需要重新播放
    if (clearPollTimer && isCommPanel) {
      setPausePlayPoll(true);
      clearDrawerData();
      setTimeout(restartPoll, 1000);
    }

    setCloseConferFlag();
    clearMonitor();

    router.push(item.path);

    if (clearPollTimer) {
      return;
    }

    // 进入投屏和投屏跳到通信调度页
    if (isCommPanel) {
      await delay(1000);
      const mapAddMonitor = () => {
        setTimeout(() => {
          if (monitorList.length > 0) {
            addMonitorList(monitorList.splice(0, 1)[0]);
            mapAddMonitor();
          }
        }, 500);
      };
      mapAddMonitor();
    }
  }
}

// 群组订阅/取消订阅 isSub-当前订阅状态
export async function handleSubscribeGroup(item, isSub: boolean): Promise<boolean> {
  const { t } = useI18n();
  const communicationStore = useCommunicationStore();

  if (!item || !communicationStore.hasComm) {
    return false;
  }

  const { groupId, groupType } = item;
  if (isSub) {
    const res = await groupFunc.unSubscribeGroup(groupId);
    if (res.rsp === '0') {
      return await saveToSubscribeGroup(groupId, groupType, isSub);
    } else {
      Message({ message: t('resource.group.failToUnSubGroup'), type: 'warning' });
    }
  } else {
    const { commonGroup, dynamicGroup } = useResourceStore();
    const arr = [...commonGroup, ...dynamicGroup].filter((item) => item.isSub === '1');
    if (arr.length >= 16) {
      Message(t('resource.group.overGroup'));
      return false;
    }
    const res = await groupFunc.subscribeGroup(groupId);
    if (res.rsp === '0') {
      return await saveToSubscribeGroup(groupId, groupType, isSub);
    } else {
      Message({ message: t('resource.group.failToSubGroup'), type: 'warning' });
    }
  }
  return false;
}

// 查询订阅状态
export async function findSubscribeGroupFunc(groupId) {
  const { data } = await findSubscribeTalkingGroup({
    groupId,
    userId: appConfig.isdn,
  });
  return data?.[0]?.speakerFixed === '1';
}

// 后台接口保存订阅状态
async function saveToSubscribeGroup(groupId, groupType, isSub: boolean): Promise<boolean> {
  const { t } = useI18n();
  const param = {
    category: groupType,
    groupId,
    speakerFixed: isSub ? '0' : '1',
    userId: appConfig.isdn,
  };
  const { code } = await subscribeTalkingGroup(param);
  if (code === 0) {
    Message(
      isSub ? t('resource.group.successToUnSubGroup') : t('resource.group.successToSubGroup'),
    );
    useEmitter().emit('queryGroupData');
    return true;
  }
  Message({
    message: isSub ? t('resource.group.failToUnSubGroup') : t('resource.group.failToSubGroup'),
    type: 'warning',
  });
  return false;
}

// 资源树类型
type ResourceTreeTypes = {
  data: any[];
  disable: boolean;
  icon: string;
  id: CategoryEnum | string;
  name: string;
  navShow: boolean;
  online: number;
  show: boolean;
  total: number;
  type: string;
}[];
export function getResourceTypes(): ResourceTreeTypes {
  const { t } = useI18n();
  const iseBC = usePermissions('eBC');

  const types = [
    {
      data: [],
      disable: true,
      icon: 'person',
      id: CategoryEnum.person,
      name: t('resource.policeResourceData.policeMan'),
      navShow: true,
      online: 0,
      show: true,
      total: 0,
      type: 'person',
    },
    {
      data: [],
      disable: true,
      icon: 'seat',
      id: CategoryEnum.seat,
      name: t('resource.resourceType.seat'),
      navShow: true,
      online: 0,
      show: true,
      total: 0,
      type: 'seat',
    },
    {
      data: [],
      disable: true,
      icon: 'pdt',
      id: CategoryEnum.pdt,
      name: t('resource.resourceType.pdt'),
      navShow: true,
      online: 0,
      show: iseBC,
      total: 0,
      type: 'pdt',
    },
    {
      data: [],
      disable: true,
      icon: 'monitor',
      id: CategoryEnum.monitor,
      name: t('resource.resourceType.monitor'),
      navShow: true,
      online: 0,
      show: true,
      total: 0,
      type: 'monitor',
    },
    {
      data: [],
      disable: true,
      icon: 'ballCamera',
      id: CategoryEnum.ballCamera,
      name: t('resource.resourceType.ballCamera'),
      navShow: true,
      online: 0,
      show: true,
      total: 0,
      type: 'ballCamera',
    },
    {
      data: [],
      disable: true,
      icon: 'car',
      id: CategoryEnum.carPhoto,
      name: t('resource.resourceType.carPhoto'),
      navShow: true,
      online: 0,
      show: iseBC,
      total: 0,
      type: 'carPhoto',
    },
    {
      data: [],
      disable: true,
      icon: 'recorder',
      id: CategoryEnum.recorder,
      name: t('resource.resourceType.recorder'),
      navShow: true,
      online: 0,
      show: true,
      total: 0,
      type: 'recorder',
    },
    {
      data: [],
      disable: true,
      icon: 'terminal',
      id: CategoryEnum.terminal,
      name: t('resource.resourceType.terminal'),
      navShow: true,
      online: 0,
      show: true,
      total: 0,
      type: 'terminal',
    },
    {
      data: [],
      disable: true,
      icon: 'standard',
      id: CategoryEnum.GBRecorder,
      name: t('resource.resourceType.GBRecorder'),
      navShow: false,
      online: 0,
      show: iseBC,
      total: 0,
      type: 'GBRecorder',
    },
    {
      data: [],
      disable: true,
      icon: 'standard',
      id: CategoryEnum.uav,
      name: t('resource.resourceType.uav'),
      navShow: false,
      online: 0,
      show: iseBC,
      total: 0,
      type: 'uav',
    },
    {
      data: [],
      disable: true,
      icon: 'standard',
      id: CategoryEnum.confTerminal,
      name: t('resource.resourceType.confTerminal'),
      navShow: false,
      online: 0,
      show: iseBC,
      total: 0,
      type: 'confTerminal',
    },
    {
      data: [],
      disable: true,
      icon: 'standard',
      id: 'thirdEquipment',
      name: t('resource.resourceType.tripartite'),
      navShow: true,
      online: 0,
      show: iseBC,
      total: 0,
      type: 'thirdEquipment',
    },
  ];
  return types;
}

// 资源类型集合
export const resourceTypes = Object.values(CategoryEnum).filter((i) => !isNumber(i));

// 资源category集合
export const resourceCategory = Object.values(CategoryEnum).filter((i) => isNumber(i));

/**
 * 获取资源功能
 * @export
 * @param {*} data 资源
 * @param {boolean} showCollect 是否收藏
 * @return  {array}
 */
export function getResourceAbilities(data, showCollect?) {
  const ret = data.capability?.split(',') || [];
  // 固定监控与车载图传支持视频查看
  if ([CategoryEnum.carPhoto, CategoryEnum.monitor].includes(Number(data.category))) {
    ret.push('512007');
  }
  if (showCollect) {
    ret.push('favorite');
  }
  return ret;
}

/**
 * 获取资源类型
 * @export
 * @param {*} data 资源
 * @return  {string} type
 */
export function getResourceType(data) {
  const { category, resourceType } = data;
  const categoryNum = Number(category);
  let ret = 'equipment';
  if (categoryNum === CategoryEnum.person || resourceType === 'Executor') {
    ret = 'person';
  }
  if ([CategoryEnum.carPhoto, CategoryEnum.monitor].includes(categoryNum)) {
    ret = 'monitor';
  }
  return ret;
}

// caller主叫 callee被叫
export async function getHistoryCallInfo(caller, callee) {
  let callerInfo;
  let calleeInfo;

  const { isdn, userData } = appConfig;
  if (caller === isdn) {
    callerInfo = userData;
  } else {
    calleeInfo = userData;
  }

  if (!calleeInfo) {
    calleeInfo = await getExecutorInfoByAccount(callee);
  }
  if (!callerInfo) {
    callerInfo = await getExecutorInfoByAccount(caller);
  }

  if (!calleeInfo) {
    calleeInfo = await getInfoByAccount(callee);
  }
  if (!callerInfo) {
    callerInfo = await getInfoByAccount(caller);
  }
  return { calleeInfo, callerInfo };
}

/**
 * 传入设备数据获取通信号 - 后端不统一通信号字段
 * @param data
 */
export function getAccountByEquipmentData(data) {
  const { account, accounts, category, code, serviceAccounts = [] } = data;
  const serviceAccount = serviceAccounts?.[0]?.account;
  if ([CategoryEnum.carPhoto, CategoryEnum.monitor].includes(Number(category))) {
    return account || accounts || code || serviceAccount;
  }
  return account || accounts || serviceAccount;
}

/**
 * 通过设备data查设备详情 ==> 尽量使用详情数据
 * @param info
 */
export async function queryResourceDetailsByInfo(info: any): Promise<any> {
  const { category, id } = info;

  const formatData = (data) => {
    return { ...data, account: getAccountByEquipmentData(data) };
  };

  // 通过account查详情
  const account = getAccountByEquipmentData(info);
  if (account) {
    const data = await getInfoByAccount(account);
    if (!isEmpty(data)) {
      // 只保留设备通信号
      if (Number(info.category) !== CategoryEnum.person) {
        data.serviceAccounts = info.serviceAccounts;
      }
      return formatData(data);
    }
  }

  // 通过id查详情
  if (id) {
    let api = queryFacilityDetailById;
    if (category !== CategoryEnum.monitor) {
      api = category === CategoryEnum.person ? queryExecutorDetailById : queryEquipmentDetailById;
    }

    const { code, data } = await api({ id });
    if (code === 0) {
      return formatData(data);
    }
  }

  // 兜底以防id/account都没有的情况
  return formatData(info);
}

/**
 * 通过人员id查询人员详情
 * @param id
 * @returns
 */
export async function queryPersonDetailById(id: string) {
  const { code, data } = await queryExecutorDetailById({ id });
  if (code === 0 && data) {
    return { ...data, category: CategoryEnum.person };
  }
  return null;
}

/**
 * 通过图片id查询图片url
 * @param id
 * @returns url
 */
export async function getIconUrlById(id: string) {
  let url = '';
  const { sandTableIcon } = useResourceStore();
  sandTableIcon.forEach((img) => {
    if (img.id === id) {
      url = img.iconInfo;
    }
  });
  return url;
}
