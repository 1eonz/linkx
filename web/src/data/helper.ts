import { queryEquipmentsByPage } from '@/api/equipment';
import { queryExecutorsByParamAndPage } from '@/api/executor';
import { dumpFacilities } from '@/api/facility';
import { CategoryEnum, LayerIdEnum, PoliceDutyStatusEnum } from '@/enums';
import { useBaseData } from '@/hooks';
import { statusOptions } from '@/pages/policeAdmin/serviceStatus/common';
import { setPosition } from '@/plugins/map';
import { useResourceStoreWithOut } from '@/store';

export type Category = keyof typeof CategoryEnum;

type FilterPerson = {
  data: any[];
  isOnline: boolean;
  layerId: string;
};

type FilterEquipment = {
  data: any[];
  layerId: string;
};

// 过滤人员数据
export function filterPersonData(data: any[]): Array<FilterPerson> {
  const onlineData: any[] = [];
  const offlineData: any[] = [];

  const { policeLayerChecked } = useResourceStoreWithOut();
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

  data.forEach((item) => {
    const bizStatus = Number(item.bizStatus);
    const isOfflineStatus = [0, 3, 5].includes(bizStatus);
    const isOnlineStatus = [1, 2, 4, 6].includes(bizStatus);
    const { attendance } = item;

    setPosition(item);
    if (!item.position) {
      return;
    }

    if (attendance) {
      statusOptions().forEach((status) => {
        if (status.value === attendance) {
          item.statusName = status.label;
        }
      });
    }

    if (item?.serviceAccounts?.length > 0) {
      item.isdn = item.serviceAccounts[0].account;
    }

    let show = true;
    if (hasDuty) {
      const { attendance } = item;
      show =
        (attendance === PoliceDutyStatusEnum.FREE &&
          policeLayerChecked.includes(LayerIdEnum.personFree)) ||
        (attendance === PoliceDutyStatusEnum.BUSY &&
          policeLayerChecked.includes(LayerIdEnum.personBusy)) ||
        (attendance === PoliceDutyStatusEnum.DIMISSION &&
          policeLayerChecked.includes(LayerIdEnum.personDimission));
    }

    if (!show) {
      return;
    }

    if (isOfflineStatus) {
      offlineData.push(item);
    } else if (isOnlineStatus) {
      onlineData.push(item);
    }
  });

  return [
    { data: offlineData, isOnline: false, layerId: LayerIdEnum.personOffline },
    { data: onlineData, isOnline: true, layerId: LayerIdEnum.personOnline },
  ];
}

// 过滤设备数据
export function filterEquipmentsData(data: any[]): Array<FilterEquipment> {
  const terminalOnlineData: any = [];
  const terminalOfflineData: any = [];
  const recorderOnlineData: any = [];
  const recorderOfflineData: any = [];
  const pdtOnlineData: any = [];
  const pdtOfflineData: any = [];
  const uavOnlineData: any = [];
  const uavOfflineData: any = [];
  const carPhotoOnlineData: any = [];
  const carPhotoOfflineData: any = [];
  const GBRecOnlineData: any = [];
  const GBRecOfflineData: any = [];
  const seatOnlineData: any = [];
  const seatOfflineData: any = [];
  const ballCameraOnlineData: any = [];
  const ballCameraOfflineData: any = [];

  data.forEach((item) => {
    const { attendance, position } = item;
    if (!position) {
      setPosition(item);
    }

    if (!item.position) {
      return;
    }

    if (attendance) {
      statusOptions().forEach((status) => {
        if (status.value === attendance) {
          item.statusName = status.label;
        }
      });
    }

    // 存在选看过滤时过滤选看数据，若无选看数据则正常显示地图
    const { mapChooseData } = useResourceStoreWithOut();
    if (mapChooseData.length > 0 && !mapChooseData.includes(item.id)) {
      return;
    }

    const { bizStatus, category } = item;
    switch (Number(category)) {
      case CategoryEnum.ballCamera: {
        bizStatus === '4' && ballCameraOnlineData.push(item);
        bizStatus === '3' && ballCameraOfflineData.push(item);
        break;
      }
      case CategoryEnum.carPhoto: {
        bizStatus === '4' && carPhotoOnlineData.push(item);
        bizStatus === '3' && carPhotoOfflineData.push(item);
        break;
      }
      case CategoryEnum.GBRecorder: {
        bizStatus === '4' && GBRecOnlineData.push(item);
        bizStatus === '3' && GBRecOfflineData.push(item);
        break;
      }
      case CategoryEnum.pdt: {
        bizStatus === '4' && pdtOnlineData.push(item);
        bizStatus === '3' && pdtOfflineData.push(item);
        break;
      }
      case CategoryEnum.recorder: {
        bizStatus === '4' && recorderOnlineData.push(item);
        bizStatus === '3' && recorderOfflineData.push(item);
        break;
      }
      case CategoryEnum.seat: {
        bizStatus === '4' && seatOnlineData.push(item);
        bizStatus === '3' && seatOfflineData.push(item);
        break;
      }
      case CategoryEnum.terminal: {
        bizStatus === '4' && terminalOnlineData.push(item);
        bizStatus === '3' && terminalOfflineData.push(item);
        break;
      }
      case CategoryEnum.uav: {
        bizStatus === '4' && uavOnlineData.push(item);
        bizStatus === '3' && uavOfflineData.push(item);
        break;
      }
      default: {
        break;
      }
    }
  });

  return [
    {
      data: carPhotoOfflineData,
      layerId: LayerIdEnum.carPhotoOffline,
    },
    {
      data: carPhotoOnlineData,
      layerId: LayerIdEnum.carPhotoOnline,
    },

    {
      data: GBRecOfflineData,
      layerId: LayerIdEnum.GBRecorderOffline,
    },
    {
      data: GBRecOnlineData,
      layerId: LayerIdEnum.GBRecorderOnline,
    },

    {
      data: pdtOfflineData,
      layerId: LayerIdEnum.pdtOffline,
    },
    {
      data: pdtOnlineData,
      layerId: LayerIdEnum.pdtOnline,
    },
    {
      data: uavOfflineData,
      layerId: LayerIdEnum.uavOffline,
    },
    {
      data: uavOnlineData,
      layerId: LayerIdEnum.uavOnline,
    },

    {
      data: recorderOfflineData,
      layerId: LayerIdEnum.recorderOffline,
    },
    {
      data: recorderOnlineData,
      layerId: LayerIdEnum.recorderOnline,
    },
    {
      data: terminalOfflineData,
      layerId: LayerIdEnum.terminalOffline,
    },
    {
      data: terminalOnlineData,
      layerId: LayerIdEnum.terminalOnline,
    },
    {
      data: seatOfflineData,
      layerId: LayerIdEnum.seatOffline,
    },
    {
      data: seatOnlineData,
      layerId: LayerIdEnum.seatOnline,
    },

    {
      data: ballCameraOfflineData,
      layerId: LayerIdEnum.ballCameraOffline,
    },
    {
      data: ballCameraOnlineData,
      layerId: LayerIdEnum.ballCameraOnline,
    },
    {
      data: ballCameraOfflineData,
      layerId: LayerIdEnum.ballCameraOffline,
    },
    {
      data: ballCameraOnlineData,
      layerId: LayerIdEnum.ballCameraOnline,
    },
  ];
}

// 查询全量设备/人员
export async function queryAllResource(type: 'equipment' | 'person'): Promise<any[]> {
  const { getOrganizationIds } = useResourceStoreWithOut();
  const equipmentData: any[] = [];
  const query = async (start: number) => {
    const param: any = {
      pageSize: 1000,
      start,
    };
    let api = queryEquipmentsByPage;
    if (type === 'equipment') {
      param.organizations = getOrganizationIds;
    } else {
      param.organizationIds = getOrganizationIds;
      api = queryExecutorsByParamAndPage;
    }
    const { code, data } = await api(param);
    if (code === 0) {
      const { records, total } = data;
      equipmentData.push(...records);
      if (equipmentData.length < Number(total)) {
        await query(start + param.pageSize);
      }
    }
  };
  await query(1);

  const equipments: any = {};
  equipmentData.forEach((item) => {
    if (type === 'person') {
      item.category = CategoryEnum.person;
    }
    if (!equipments[item.category]) {
      equipments[item.category] = [];
    }
    equipments[item.category].push(item);
  });

  const { setPersonFromIdCard, setResourceOrigin } = useBaseData();
  if (type === 'person') {
    setPersonFromIdCard(equipments[CategoryEnum.person]);
  }

  Object.keys(equipments).forEach((category) => {
    setResourceOrigin(category, equipments[category]);
  });
  return new Promise((resolve) => resolve([]));
}

/**
 * 查询所有摄像头数据
 * 后端返回的csv格式
 * @returns Promise
 */
export async function queryAllMonitor(): Promise<any[]> {
  const { getOrganizationMap } = useResourceStoreWithOut();
  const res = await dumpFacilities();
  const list = res.replaceAll('\r', '').split('\n');
  const field = [
    'id',
    'name',
    'organizationId',
    'bizStatus',
    'accounts',
    'code',
    'lat',
    'lon',
    'address',
  ];

  const data: any[] = [];
  list.forEach((str) => {
    if (!str) {
      return;
    }
    const val = str.split(',');
    const item: any = { category: CategoryEnum.monitor };
    val.forEach((v, index) => {
      item[field[index]] = v;
    });

    const { lat, lon } = item;
    if (lon && lat) {
      item.position = [lon, lat];
    }
    // 根据组织id获取缓存中的组织名
    item.organizationName = getOrganizationMap.get(item.organizationId);

    data.push(item);

    return item;
  });

  const { setResourceOrigin } = useBaseData();
  setResourceOrigin(CategoryEnum.monitor, data);

  return new Promise((resolve) => {
    resolve(data);
  });
}
