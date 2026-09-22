// import { computed, onBeforeUnmount, unref, watch } from 'vue';
import { onBeforeUnmount } from 'vue';
// import { useRoute } from 'vue-router';

// import { queryEquipmentsByDistance, queryPoliceCarPage } from '@/api/equipment';
// import { queryEquipmentsByDistance } from '@/api/equipment';
// import { queryExecutorsByDistance } from '@/api/executor';
// import { getDefenseLayerList, getDefensePointListByLayerId } from '@/api/layer';
// import { queryOrderListByPage } from '@/api/order';
// import planMarker from '@/assets/images/marker/plan_marker.png';
// import { appConfig } from '@/config';
import { CategoryEnum } from '@/enums';
// import { CategoryEnum, LayerIdEnum } from '@/enums';
// import { useBaseData, useDC, useEmitter, usePermissions, useSetInterval, useUtils } from '@/hooks';
import { useBaseData, useDC, useEmitter } from '@/hooks';
// import { loadLandmarkLayer } from '@/pages/landmark/helper';
// import { getOnlineStatus } from '@/pages/resource/resourceHelper';
import { setPosition } from '@/plugins/map';
// import { getDifferentFromSource, mapIsReady, mapManager, setPosition } from '@/plugins/map';
import {
  // useAlarmStore,
  // useMapStore,
  // useMessageStore,
  // useMissionStore,
  // usePlanStore,
  useResourceStore,
  // useVehicleStore,
} from '@/store';
// import {
//   addAlarmLayer,
//   addBallCameraLayer,
//   addCarPhotoLayer,
//   addDefenseLayer,
//   addGBRecLayer,
//   addMessageLayer,
//   addMissionLayer,
//   addMonitorLayer,
//   addPdtLayer,
//   addPlotLayer,
//   addPoliceCarLayer,
//   addPoliceManLayer,
//   addRecorderLayer,
//   addSeatLayer,
//   addTerminalLayer,
//   addUavLayer,
// } from '@/utils/addLayerUtil';
// import { isArray } from '@/utils/is';

// import { debounce } from 'lodash-es';

import {
  // filterEquipmentsData,
  // filterPersonData,
  // queryAllMonitor,
  queryAllResource,
} from './helper';

export function initResource() {
  // const { isMapPanel } = useUtils();
  // const route = useRoute();
  // const mapStore = useMapStore();
  const resourceStore = useResourceStore();
  // const missionStore = useMissionStore();
  // const planStore = usePlanStore();
  // const vehicleStore = useVehicleStore();
  // const alarmStore = useAlarmStore();

  // const mapId = 'mapId_main';
  // const isPlan = computed(() => ['leadVehicle', 'planSpecial'].includes(route.name as string));
  // const planEquipmentKey = computed(() => planStore.planEquipmentData.map((i) => i.id));
  let clearTimer: any;
  const clearAlarmTimer: any = null;
  const waitingUpdateQueue: any = [];
  // const loadLayerSuccess = false;
  // const loadLayerQueue: any = [];

  // 设置摄像头的数据及添加相关的图层
  // const loadMonitorLayerInner = () => {
  //   const { mapChooseData } = resourceStore;
  //   const monitorOnline: any[] = [];
  //   const monitorOffline: any[] = [];

  //   const { getResourceOrigin } = useBaseData();
  //   const monitorData = getResourceOrigin[CategoryEnum.monitor] || [];
  //   monitorData.forEach((item) => {
  //     if (!item.position) {
  //       return;
  //     }
  //     const isOnline = item.bizStatus * 1 === 6;
  //     // 存在选看过滤时过滤选看数据，若无选看数据则正常显示地图
  //     if (mapChooseData.length > 0 && !mapChooseData.includes(item.id)) {
  //       return;
  //     }

  //     if (isOnline) {
  //       monitorOnline.push(item);
  //     } else {
  //       monitorOffline.push(item);
  //     }
  //   });
  //   const mapObj = mapManager.get(mapId);

  //   const onlineData = getDifferentFromSource(
  //     mapId,
  //     LayerIdEnum.monitorOnline,
  //     getLayerData(LayerIdEnum.monitorOnline, monitorOnline),
  //   );
  //   const offlineData = getDifferentFromSource(
  //     mapId,
  //     LayerIdEnum.monitorOffline,
  //     getLayerData(LayerIdEnum.monitorOffline, monitorOffline),
  //   );

  //   mapObj?.updateLayer(LayerIdEnum.monitorOnline, onlineData);
  //   mapObj?.updateLayer(LayerIdEnum.monitorOffline, offlineData);

  //   setTimeout(() => {
  //     refreshLayerZIndex(LayerIdEnum.monitorOnline);
  //     refreshLayerZIndex(LayerIdEnum.monitorOffline);
  //   }, 500);
  // };
  // const loadMonitorLayer = debounce(loadMonitorLayerInner, 500);

  // watch(
  //   () => vehicleStore.vehicleList,
  //   (val) => {
  //     loadVehicleLayer(val);
  //   },
  // );
  // watch(
  //   () => resourceStore.policeLayerChecked,
  //   () => {
  //     loadPolicemanLayer();
  //   },
  // );
  // watch(
  //   () => resourceStore.layerChecked,
  //   (newVal, oldVal) => {
  //     const newData = newVal.filter((item) => item.includes('defense_'));
  //     const oldData = oldVal.filter((item) => item.includes('defense_'));
  //     if (newData.length !== oldData.length) {
  //       loadDefenseLayer();
  //       return;
  //     }
  //     const newCustomData = newVal.filter((item) => item.includes('custom_'));
  //     const oldCustomData = oldVal.filter((item) => item.includes('custom_'));
  //     if (newCustomData.length !== oldCustomData.length) {
  //       loadCustomLayer();
  //     }
  //   },
  // );
  // 地图选看数据发生变化后进行地图刷新
  // watch(
  //   () => [resourceStore.mapChooseData, mapStore.showIconInfo, resourceStore.layerChecked],
  //   () => {
  //     loadEquipmentsLayer();
  //     loadMonitorLayerInner();
  //   },
  // );
  // watch(
  //   () => route.name,
  //   () => {
  //     if (useUtils().isSpecial) {
  //       addPlanDistrict();
  //     } else {
  //       delPlanDistrict();
  //     }
  //   },
  //   { deep: true, immediate: true },
  // );
  // watch(
  //   () => planStore.planEquipmentData,
  //   () => {
  //     loadMonitorLayer();
  //   },
  // );

  useEmitter('mapTypeChange', () => {
    setTimeout(init, 500);
  });

  // 在线状态更新
  useDC('RESOURCE_STATE', 'online_status', (messages) => {
    updateGpsAndOnlineStatus('status', messages);
  });

  // GPS位置更新
  useDC('LBS', 'update_gps', (messages) => {
    updateGpsAndOnlineStatus('gps', messages);
  });

  // 勤务状态更新
  useDC('attendance_status', 'attendance_status', (message) => {
    updateGpsAndOnlineStatus('attendance_status', [message]);
  });

  async function mounted() {
    await resourceStore.getOrganization();

    queryAllResource('equipment').then(handleUpdateQueue);

    // 加载摄像头
    // queryAllMonitor().then(() => {
    //   if (loadLayerSuccess) {
    //     loadMonitorLayer();
    //   } else {
    //     loadLayerQueue.push(loadMonitorLayer);
    //   }
    // });

    queryAllResource('person').then(handleUpdateQueue);

    init();
  }

  onBeforeUnmount(() => {
    clearTimer?.();
    clearAlarmTimer?.();
  });

  // 初始化
  function init() {
    // initResourceMapData();
    // refresh();
  }

  // 定时刷新
  // async function refresh() {
  //   clearTimer?.();

  //   await mapIsReady(mapId);
  //   const oldZC = '';
  //   let time = 0;
  //   if (time < 60) {
  //     time += 1;
  //   } else {
  //     time = 0;
  //   }
  //   const mapObj = mapManager.get(mapId);
  //   const mapType = appConfig.settingData.MAP_TYPE;
  //   clearTimer = useSetInterval(() => {
  //     if (isMapPanel) {
  //       loadPolicemanLayer();
  //       loadEquipmentsLayer();
  //       loadPoliceCarLayer();
  //       loadMissionLayer();
  //       loadAlarmLayer();
  //       loadEmergencyMessageLayer();
  //       // 只有mineMap才做部分摄像头加载/移动地图刷新摄像头
  //       if (mapType === 'MineMap') {
  //         const zoom = mapObj.getZoom();
  //         const center = mapObj.getCenter();
  //         const zc = JSON.stringify([zoom, center]);
  //         const zcChange = oldZC !== zc;
  //         if (zcChange) {
  //           loadMonitorLayer();
  //         } else if (time % 30 === 0) {
  //           // 摄像头数据太多，不能频繁更新，会导致视频卡顿
  //           loadMonitorLayer();
  //         }
  //       }
  //     }
  //   }, 3000);
  // }

  /**
   * gps和在线状态更新
   * @param type 推送类型
   * @param messages 消息
   * @param push 是否来自推送
   */
  function updateGpsAndOnlineStatus(type, messages: any[], push = true) {
    const { resourceOrigin, updateResourceOriginPosition } = useBaseData();
    const setNewValue = (category, message, id) => {
      const target = resourceOrigin[category][id];
      const index = waitingUpdateQueue.findIndex((i) => {
        return i.type === type && i.message.equipmentId === message.equipmentId;
      });
      if (index !== -1) {
        waitingUpdateQueue.splice(index, 1);
      }

      if (target) {
        switch (type) {
          case 'attendance_status': {
            target.attendance = message.status;
            break;
          }
          case 'gps': {
            // 人员定位按照设备优先级展示
            if (
              category === CategoryEnum.person &&
              target.serviceAccounts?.[0].id !== message.equipmentId
            ) {
              return;
            }

            setPosition(message);
            Object.assign(target, {
              direction: message.direction, // 方向
              position: message.position, // 经纬度
              speed: Number(message.speed).toFixed(2), // 速度
            });
            break;
          }
          case 'status': {
            target.bizStatus = message.status;
            break;
          }
          // No default
        }
        updateResourceOriginPosition(category, [target]);
      } else {
        // 存在数据还未获取推送就先到的情况
        if (push) {
          waitingUpdateQueue.push({ message, type });
        }
      }
    };

    messages.forEach(async (message) => {
      const { equipmentId, executorId, facilityId } = message;
      Object.keys(resourceOrigin).forEach((category) => {
        setNewValue(category, message, executorId);
        setNewValue(category, message, equipmentId);
        setNewValue(category, message, facilityId);
      });
    });
  }

  function handleUpdateQueue() {
    setTimeout(() => {
      waitingUpdateQueue.forEach((item) => {
        updateGpsAndOnlineStatus(item.type, [item.message], false);
      });
    }, 500);
  }

  // 加载资源
  // async function initResourceMapData() {
  //   initLayer();

  //   // 加载警车
  //   loadPoliceCarLayer();

  //   // 加载警情
  //   loadOrderLayer();

  //   // 加载记录仪、终端、pdt等设备
  //   loadEquipmentsLayer(true);

  //   // 加载警员
  //   loadPolicemanLayer(true);

  //   // 加载任务
  //   loadMissionLayer();

  //   // 加载预警
  //   loadAlarmLayer();

  //   // 加载紧急事件
  //   loadEmergencyMessageLayer();

  //   // 加载自定义图层
  //   loadCustomLayer();

  //   // 加载三道防线图层
  //   loadDefenseLayer();

  //   // 加载单位地标图层
  //   loadLandmarkLayer();
  // }

  // 添加图层
  // function addLayer(layerId, fnc) {
  //   const mapObj = mapManager.get(mapId);
  //   fnc({
  //     data: [],
  //     isShow: resourceStore.layerChecked.includes(layerId),
  //     layerId,
  //     map: mapObj,
  //   });
  // }

  // 初始化图层
  // async function initLayer() {
  //   await mapIsReady(mapId);
  //   addLayer(LayerIdEnum.monitorOffline, addMonitorLayer);
  //   addLayer(LayerIdEnum.monitorOnline, addMonitorLayer);
  //   addLayer(LayerIdEnum.uavOffline, addUavLayer);
  //   addLayer(LayerIdEnum.uavOnline, addUavLayer);
  //   addLayer(LayerIdEnum.policeCar, addPoliceCarLayer);
  //   addLayer(LayerIdEnum.GBRecorderOffline, addGBRecLayer);
  //   addLayer(LayerIdEnum.GBRecorderOnline, addGBRecLayer);
  //   addLayer(LayerIdEnum.carPhotoOffline, addCarPhotoLayer);
  //   addLayer(LayerIdEnum.carPhotoOnline, addCarPhotoLayer);
  //   addLayer(LayerIdEnum.pdtOffline, addPdtLayer);
  //   addLayer(LayerIdEnum.pdtOnline, addPdtLayer);
  //   addLayer(LayerIdEnum.recorderOffline, addRecorderLayer);
  //   addLayer(LayerIdEnum.recorderOnline, addRecorderLayer);
  //   addLayer(LayerIdEnum.terminalOffline, addTerminalLayer);
  //   addLayer(LayerIdEnum.terminalOnline, addTerminalLayer);
  //   addLayer(LayerIdEnum.personOffline, addPoliceManLayer);
  //   addLayer(LayerIdEnum.personOnline, addPoliceManLayer);
  //   addLayer(LayerIdEnum.seatOnline, addSeatLayer);
  //   addLayer(LayerIdEnum.seatOffline, addSeatLayer);
  //   addLayer(LayerIdEnum.ballCameraOnline, addBallCameraLayer);
  //   addLayer(LayerIdEnum.ballCameraOffline, addBallCameraLayer);
  //   addLayer(LayerIdEnum.taskCompleted, addMissionLayer);
  //   addLayer(LayerIdEnum.taskGoing, addMissionLayer);
  //   addLayer(LayerIdEnum.taskWaiting, addMissionLayer);
  //   addLayer(LayerIdEnum.alarmCompleted, addAlarmLayer);
  //   addLayer(LayerIdEnum.alarmWaiting, addAlarmLayer);
  //   addLayer(LayerIdEnum.message, addMessageLayer);

  //   loadLayerSuccess = true;

  //   setTimeout(() => {
  //     loadLayerQueue.forEach((i) => i());
  //   }, 500);
  // }

  // 图层层级控制 - 后加载的图层高
  // async function refreshLayerZIndex(layerId: string) {
  //   await mapIsReady(mapId);
  //   const mapObj = mapManager.get(mapId);
  //   const { layerChecked, mapChooseData } = resourceStore;
  //   let layerCheckedTemp = layerChecked;

  //   // 选看时固定图层显示，只显示设备与摄像头图层
  //   if (mapChooseData && mapChooseData.length > 0) {
  //     layerCheckedTemp = [
  //       LayerIdEnum.monitorOffline,
  //       LayerIdEnum.monitorOnline,
  //       LayerIdEnum.uavOffline,
  //       LayerIdEnum.uavOnline,
  //       LayerIdEnum.GBRecorderOffline,
  //       LayerIdEnum.GBRecorderOnline,
  //       LayerIdEnum.carPhotoOffline,
  //       LayerIdEnum.carPhotoOnline,
  //       LayerIdEnum.pdtOffline,
  //       LayerIdEnum.pdtOnline,
  //       LayerIdEnum.recorderOffline,
  //       LayerIdEnum.recorderOnline,
  //       LayerIdEnum.terminalOffline,
  //       LayerIdEnum.terminalOnline,
  //       LayerIdEnum.seatOnline,
  //       LayerIdEnum.seatOffline,
  //       LayerIdEnum.ballCameraOnline,
  //       LayerIdEnum.ballCameraOffline,
  //     ];
  //   }

  //   let show = layerCheckedTemp.includes(layerId as string);
  //   if (unref(isPlan)) {
  //     show = true;
  //   } else if (layerId?.includes('person')) {
  //     const duty = [LayerIdEnum.personFree, LayerIdEnum.personBusy, LayerIdEnum.personDimission];
  //     let hasDuty = false;
  //     duty.forEach((item) => {
  //       if (layerCheckedTemp.includes(item)) {
  //         hasDuty = true;
  //       }
  //     });
  //     if (hasDuty) {
  //       if (
  //         layerId === LayerIdEnum.personOnline &&
  //         !layerCheckedTemp.includes(LayerIdEnum.personOffline)
  //       ) {
  //         show = true;
  //       }
  //       if (
  //         layerId === LayerIdEnum.personOffline &&
  //         !layerCheckedTemp.includes(LayerIdEnum.personOnline)
  //       ) {
  //         show = true;
  //       }
  //     }
  //   }

  //   mapObj.setLayersVisible([layerId], show);
  // }

  // 加载终端、记录仪、布控球、PDT图层
  // async function loadEquipmentsLayer(init?: boolean) {
  //   const { getResourceOrigin, updateResourceOriginPosition } = useBaseData();

  //   const vehicleListKey = new Set(vehicleStore.vehicleList.map((i) => i.id));
  //   const update = async (layerId, data) => {
  //     await mapIsReady(mapId);
  //     const mapObj = mapManager.get(mapId);

  //     // 过滤掉车辆幻化设备
  //     const filterVehicle = data.filter((i) => !vehicleListKey.has(i.id));

  //     const layerData = getLayerData(layerId, filterVehicle);
  //     const diffData = getDifferentFromSource(mapId, layerId, layerData);

  //     mapObj?.updateLayer(layerId, diffData);
  //     refreshLayerZIndex(layerId);
  //   };

  //   let equipments: any = [];
  //   if (init) {
  //     const param = { onlineState: 0 };
  //     const { code, data = [] } = await queryEquipmentsByDistance(param);
  //     if (code === 0) {
  //       equipments = data;
  //     }
  //   } else {
  //     const layerIds: string[] = [];
  //     filterEquipmentsData([]).forEach((item) => {
  //       const id = item.layerId.replace(/O.*/, '');
  //       if (!layerIds.includes(id)) {
  //         layerIds.push(id);
  //         equipments.push(...(getResourceOrigin[CategoryEnum[id]] || []));
  //       }
  //     });
  //   }

  //   const obj: any = {};
  //   const filter = filterEquipmentsData(equipments);
  //   filter.forEach(({ data, layerId }: any) => {
  //     update(layerId, data);
  //     const id = layerId.replace(/O.*/, '');
  //     const arr = [...(obj[id] || []), ...data];
  //     obj[id] = arr;
  //   });

  //   if (init) {
  //     Object.keys(obj).forEach((key) => {
  //       updateResourceOriginPosition(
  //         CategoryEnum[key],
  //         obj[key].map((item) => {
  //           // 地图接口获取的在线状态有时不准
  //           return { ...item, bizStatus: undefined };
  //         }),
  //       );
  //     });
  //   }
  // }

  // 处理不受图层控制的资源
  // function getLayerData(layerId, data) {
  //   const layerData: any = [];
  //   const { layerChecked, mapChooseData } = resourceStore;
  //   // 预案选看时不进行图层勾选过滤,直接返回数据由地图图层显示控制显示逻辑
  //   if (mapChooseData && mapChooseData.length > 0) {
  //     return data;
  //   } else if (layerChecked.includes(layerId)) {
  //     return data;
  //   } else {
  //     // 未选中展示专项数据
  //     data.forEach((item) => {
  //       if (unref(planEquipmentKey).includes(item.id)) {
  //         layerData.push(item);
  //       }
  //     });
  //   }
  //   return layerData;
  // }

  // 加载警员图层
  // async function loadPolicemanLayer(init?: boolean) {
  //   if (appConfig.isLyg) {
  //     return;
  //   }

  //   const { getResourceOrigin, updateResourceOriginPosition } = useBaseData();
  //   let personData: any = [];
  //   if (init) {
  //     const { code, data = [] } = await queryExecutorsByDistance({});
  //     if (code === 0) {
  //       personData = data
  //         .filter((i) => i.location)
  //         .map((i) => ({ ...i, category: CategoryEnum.person }));
  //       updateResourceOriginPosition(CategoryEnum.person, personData);
  //     }
  //   } else {
  //     personData = getResourceOrigin[CategoryEnum.person] || [];
  //   }

  //   await mapIsReady(mapId);
  //   const mapObj = mapManager.get(mapId);

  //   const filterData = filterPersonData(personData);
  //   filterData.forEach((item) => {
  //     const { data, layerId } = item;
  //     const upData = getDifferentFromSource(mapId, layerId, getLayerData(layerId, data));
  //     mapObj?.updateLayer(layerId, upData);
  //     refreshLayerZIndex(layerId);
  //   });
  // }

  // 加载警车图层
  // async function loadPoliceCarLayer() {
  //   const { CAR_BREAKER } = appConfig.settingData;
  //   if (CAR_BREAKER !== '1') return;

  //   const carData: any[] = [];
  //   let currentPage = 1;
  //   const layerId = LayerIdEnum.policeCar;

  //   const queryData = async () => {
  //     const param = {
  //       currentPage,
  //       pageSize: 5000,
  //       queryWrapper: {
  //         cascadeQuery: true,
  //         ics: true,
  //       },
  //     };
  //     const { code, data } = await queryPoliceCarPage(param);

  //     if (code !== 0) return;

  //     data.records.forEach((item) => {
  //       const { lat, lon } = item;
  //       if (lon && lat) {
  //         item.position = [Number(lon), Number(lat)];
  //         carData.push(item);
  //       }
  //     });
  //     if (Number(data.current) < Number(data.pages)) {
  //       currentPage++;
  //       await queryData();
  //     }
  //   };
  //   await queryData();

  //   const mapObj = mapManager.get(mapId);
  //   const upData = getDifferentFromSource(mapId, layerId, carData);
  //   mapObj?.updateLayer(layerId, upData);
  //   refreshLayerZIndex(layerId);
  // }

  // 加载预警图层
  // async function loadAlarmLayer() {
  //   if (!usePermissions('ALARM') || useUtils().isSpecial) {
  //     return;
  //   }
  //   const alarmStatus = [0, 1];
  //   for (const status of alarmStatus) {
  //     await getAlarmData(status);
  //   }
  // }

  // 获取预警数据
  // async function getAlarmData(status: number) {
  //   if (appConfig.isLyg) {
  //     return;
  //   }

  //   const layerId = status === 0 ? LayerIdEnum.alarmWaiting : LayerIdEnum.alarmCompleted;
  //   const arr: any = [];
  //   alarmStore.getAlarmData[status].forEach((item) => {
  //     const { latitude, longitude } = item;
  //     if (latitude && longitude) {
  //       item.position = [longitude, latitude];
  //     }
  //     arr.push(item);
  //   });

  //   await mapIsReady(mapId);
  //   const mapObj = mapManager.get(mapId);
  //   const upData = getDifferentFromSource(mapId, layerId, arr);
  //   mapObj?.updateLayer(layerId, upData);
  //   refreshLayerZIndex(layerId);
  // }

  // 加载任务图层
  // async function loadMissionLayer() {
  //   if (!usePermissions('MISSION') || appConfig.isLyg || useUtils().isSpecial) {
  //     return;
  //   }

  //   const { getStatus, missionLocation, queryMissionLocation } = missionStore;
  //   if (missionLocation.length === 0) {
  //     await queryMissionLocation();
  //   }

  //   await mapIsReady(mapId);
  //   const mapObj = mapManager.get(mapId);

  //   const layers: any = {
  //     finish: {
  //       data: [],
  //       layerId: LayerIdEnum.taskCompleted,
  //     },
  //     pending: {
  //       data: [],
  //       layerId: LayerIdEnum.taskWaiting,
  //     },
  //     underway: {
  //       data: [],
  //       layerId: LayerIdEnum.taskGoing,
  //     },
  //   };
  //   missionLocation.forEach((item) => {
  //     Object.keys(getStatus).forEach((key) => {
  //       if (getStatus[key].includes(item.status)) {
  //         layers[key].data.push({
  //           ...item,
  //           position: [item.longitude, item.latitude],
  //         });
  //       }
  //     });
  //   });

  //   Object.values(layers).forEach(({ data, layerId }: any) => {
  //     if (data.length > 0) {
  //       const upData = getDifferentFromSource(mapId, layerId, data);
  //       mapObj?.updateLayer(layerId, upData);
  //       refreshLayerZIndex(layerId);
  //     }
  //   });
  // }

  // 加载紧急事件图层
  // async function loadEmergencyMessageLayer() {
  //   if (appConfig.isLyg || useUtils().isSpecial) {
  //     return;
  //   }

  //   await mapIsReady(mapId);
  //   const mapObj = mapManager.get(mapId);
  //   const { messagesData } = useMessageStore();
  //   const layerId = LayerIdEnum.message;
  //   const upData = getDifferentFromSource(
  //     mapId,
  //     layerId,
  //     messagesData.filter((i) => i.position),
  //   );
  //   mapObj?.updateLayer(layerId, upData);
  //   refreshLayerZIndex(layerId);
  // }

  // 加载110警情图层
  // function loadOrderLayer() {
  //   if (window) return; // 暂不加载
  //   const orderStatus = [
  //     { layerId: LayerIdEnum.orderWaiting, status: [0] },
  //     { layerId: LayerIdEnum.orderGoing, status: [1, 2] },
  //     { layerId: LayerIdEnum.orderCompleted, status: [3, 4] },
  //   ];
  //   orderStatus.forEach((item) => {
  //     getOrderData(item);
  //   });
  // }

  // 加载110警情数据
  // async function getOrderData(item: any) {
  //   const { layerId, status } = item;
  //   const param = {
  //     pageNum: 1,
  //     pageSize: 10_000,
  //     status,
  //   };
  //   const { code, data } = await queryOrderListByPage(param);
  //   if (code === 0 && data?.records) {
  //     const orderData: any[] = [];
  //     data.records.forEach((item) => {
  //       const { lat, lon } = item;
  //       if (lon && lat) {
  //         item.position = [lon, lat];
  //         orderData.push(item);
  //       }
  //     });

  //     const mapObj = mapManager.get(mapId);
  //     const upData = getDifferentFromSource(mapId, layerId, orderData);
  //     mapObj?.updateLayer(layerId, upData);
  //     refreshLayerZIndex(layerId);
  //   }
  // }

  // function getIds(val) {
  //   const { layerChecked } = resourceStore;
  //   const ids = layerChecked
  //     .filter((item) => {
  //       return item.includes(val);
  //     })
  //     .map((val) => {
  //       return val.split('_')[1] || '';
  //     });
  //   return ids;
  // }

  // 加载三道防线图层
  // async function loadDefenseLayer() {
  //   if (useUtils().isSpecial) {
  //     return;
  //   }
  //   await mapIsReady(mapId);
  //   const mapObj = mapManager.get(mapId);
  //   mapObj.deleteCustomMarker('defense');
  //   const defenseIds = getIds('defense_');
  //   const { code, data } = await getDefenseLayerList({ type: 1 });
  //   if (code === 0 && isArray(data)) {
  //     data.forEach(async (item) => {
  //       if (defenseIds.includes(item.id)) {
  //         addDefenseLayer({
  //           data: await getDefensePoint(item),
  //           map: mapObj,
  //           type: item.type,
  //         });
  //       }
  //     });
  //   }
  // }

  // async function getDefensePoint(item) {
  //   const { code, data } = await getDefensePointListByLayerId({ layerId: item.id });
  //   if (code === 0 && isArray(data)) {
  //     data.forEach((val) => {
  //       val.customLayerName = item.customLayerName;
  //     });
  //     return data;
  //   }
  // }

  // 加载自定义图层
  // async function loadCustomLayer() {
  //   if (useUtils().isSpecial) {
  //     return;
  //   }
  //   await mapIsReady(mapId);
  //   const mapObj = mapManager.get(mapId);
  //   mapObj.deleteCustomMarker('custom');
  //   const defenseIds = getIds('custom_');
  //   const { code, data } = await getDefenseLayerList({ type: 2 });
  //   if (code === 0 && isArray(data)) {
  //     data.forEach(async (item) => {
  //       if (defenseIds.includes(item.id)) {
  //         addDefenseLayer({
  //           data: await getDefensePoint(item),
  //           map: mapObj,
  //           type: item.type,
  //         });
  //       }
  //     });
  //   }
  // }

  // // 加载保障点
  // async function addPlanDistrict() {
  //   const { id } = route.query;
  //   const res = await planStore.queryPlanData(id);
  //   if (!res) {
  //     return;
  //   }

  //   await mapIsReady(mapId);
  //   const mapObj = mapManager.get(mapId);

  //   const { plottingMap, supportPosition } = planStore.planData;
  //   const position = supportPosition?.split(',') || [];
  //   mapObj.flyAction(position, 16, 10, 30);
  //   mapObj.addPlanMarker(planMarker, position);
  //   mapObj.deleteAllDraw();
  //   if (plottingMap) {
  //     const plotData = JSON.parse(plottingMap);
  //     addPlotLayer({
  //       data: plotData,
  //       disable: false,
  //       handle: 'modify',
  //       map: mapObj,
  //     });
  //     mapObj.setFeatureLock(true);
  //   }

  //   loadEquipmentsLayer();
  //   loadMonitorLayer();
  // }

  // // 删除保障点
  // async function delPlanDistrict() {
  //   await mapIsReady(mapId);
  //   const mapObj = mapManager.get(mapId);
  //   mapObj.deleteMarker?.(['plan']);
  //   if (mapManager.get('mapType') === 'Openlayers') {
  //     // 删除离线地图残留点位图层
  //     mapObj.removeLayer('planMarker');
  //   }
  //   mapObj.deleteAllDraw?.();
  //   loadEquipmentsLayer();
  //   loadMonitorLayer();
  // }

  // // 车辆幻化
  // async function loadVehicleLayer(data) {
  //   const _mapId = location.pathname.includes('leadVehicle') ? 'mapId_current' : mapId;
  //   await mapIsReady(_mapId);

  //   const mapObj = mapManager.get(_mapId);
  //   const current: string[] = [];

  //   data.forEach((item) => {
  //     const { category, id } = item;
  //     current.push(id);

  //     setPosition(item);
  //     if (item.position) {
  //       const layerId =
  //         CategoryEnum[category] + (getOnlineStatus(item) === 'online' ? 'Online' : 'Offline');

  //       mapObj.addVehicleMarker({
  //         ...item,
  //         iconUrl: resourceStore.getIcon[item.icon],
  //         layerId,
  //       });
  //     }
  //   });

  //   mapObj.vehicleMarkers?.forEach((item) => {
  //     if (!current.includes(item.id)) {
  //       mapObj.removeVehicleMarker(item);
  //     }
  //   });

  //   loadEquipmentsLayer();
  // }

  return mounted;
}
