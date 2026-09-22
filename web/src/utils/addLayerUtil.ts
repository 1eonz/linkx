/**
 * @description 添加图层
 * 图层层级：预警=紧急短信(15/14) > 任务(13/12/11) > 警员(10/9) > 记录仪(8/7) > 终端(6/5) > PDT=无人机=车载图传=坐席=警车(4/3) > 摄像头PDT(2/1)
 */

import alarmCompletedImg from '@/assets/images/marker/alarm_completed_marker.png';
import alarmCompleted01Img from '@/assets/images/marker/alarm_completed_marker_01.png';
import alarmCompleted02Img from '@/assets/images/marker/alarm_completed_marker_02.png';
import alarmClusterImg from '@/assets/images/marker/alarm_marker_count.png';
import alarmWaitingImg from '@/assets/images/marker/alarm_waiting_marker.png';
import alarmWaiting01Img from '@/assets/images/marker/alarm_waiting_marker_01.png';
import alarmWaiting02Img from '@/assets/images/marker/alarm_waiting_marker_02.png';
import ballCameraOnlineImg from '@/assets/images/marker/ballCamera_marker.png';
import ballCameraClusterImg from '@/assets/images/marker/ballCamera_marker_count.png';
import ballCameraOfflineImg from '@/assets/images/marker/ballCamera_marker_offline.png';
import monitorOnlineImg from '@/assets/images/marker/camera_marker.png';
import monitorClusterImg from '@/assets/images/marker/camera_marker_count.png';
import monitorOfflineImg from '@/assets/images/marker/camera_marker_offline.png';
import customElseImg from '@/assets/images/marker/custom_else.png';
import customHouseImg from '@/assets/images/marker/custom_house.png';
import customHouseClusterImg from '@/assets/images/marker/custom_house_count.png';
import customSchoolImg from '@/assets/images/marker/custom_school.png';
import customSchoolClusterImg from '@/assets/images/marker/custom_school_count.png';
import GBRecordImg from '@/assets/images/marker/GB_recorder_marker.png';
import GBRecordClusterImg from '@/assets/images/marker/GB_recorder_marker_count.png';
import GBRecordOfflineImg from '@/assets/images/marker/GB_recorder_marker_offline.png';
import messageImg from '@/assets/images/marker/message_marker.png';
import messageClusterImg from '@/assets/images/marker/message_marker_count.png';
import pdtOnlineImg from '@/assets/images/marker/pdt_marker.png';
import pdtClusterImg from '@/assets/images/marker/pdt_marker_count.png';
import pdtOfflineImg from '@/assets/images/marker/pdt_marker_offline.png';
import policeCarImg from '@/assets/images/marker/police_car_marker.png';
import policeCarClusterImg from '@/assets/images/marker/police_car_marker_count.png';
import policeCarOfflineImg from '@/assets/images/marker/police_car_marker_offline.png';
import CommonMarker from '@/pages/map/marker/commonMarker.vue';
import PoliceMarker from '@/pages/map/marker/policeMarker.vue';
import AlarmPopup from '@/pages/map/popup/alarmPopup.vue';
import CarPopup from '@/pages/map/popup/carPopup.vue';
import CustomPopup from '@/pages/map/popup/customPopup.vue';
import DefensePopup from '@/pages/map/popup/defensePopup.vue';
import LandmarkPopup from '@/pages/map/popup/landmarkPopup.vue';
import MessagePopup from '@/pages/map/popup/messagePopup.vue';
import MissionPopup from '@/pages/map/popup/missionPopup.vue';
import MonitorPopup from '@/pages/map/popup/monitorPopup.vue';
import PolicePopup from '@/pages/map/popup/policePopup.vue';
import SearchPopup from '@/pages/map/popup/searchPopup.vue';

// 加载图片
import policeLeaderOfflineImg from '@/assets/images/marker/police_leader_offline.png';
import policeLeaderOnlineImg from '@/assets/images/marker/police_leader_online.png';
import policeClusterImg from '@/assets/images/marker/police_marker_count.png';
import policeOffImg from '@/assets/images/marker/police_offline.png';
import policeOnImg from '@/assets/images/marker/police_online.png';

// 记录仪开始
import recorderLeaderOfflineImg from '@/assets/images/marker/recorder_leader_offline.png';
import recorderLeaderOffline01Img from '@/assets/images/marker/recorder_leader_offline_01.png';
import recorderLeaderOffline02Img from '@/assets/images/marker/recorder_leader_offline_02.png';
import recorderLeaderOffline03Img from '@/assets/images/marker/recorder_leader_offline_03.png';
import recorderLeaderOffline04Img from '@/assets/images/marker/recorder_leader_offline_04.png';
import recorderLeaderOffline05Img from '@/assets/images/marker/recorder_leader_offline_05.png';
import recorderLeaderOnlineImg from '@/assets/images/marker/recorder_leader_online.png';
import recorderLeaderOnline01Img from '@/assets/images/marker/recorder_leader_online_01.png';
import recorderLeaderOnline02Img from '@/assets/images/marker/recorder_leader_online_02.png';
import recorderLeaderOnline03Img from '@/assets/images/marker/recorder_leader_online_03.png';
import recorderLeaderOnline04Img from '@/assets/images/marker/recorder_leader_online_04.png';
import recorderLeaderOnline05Img from '@/assets/images/marker/recorder_leader_online_05.png';
import recorderClusterImg from '@/assets/images/marker/recorder_marker_count.png';
import recorderOfflineImage from '@/assets/images/marker/recorder_offline.png';
import recorderOffline01Img from '@/assets/images/marker/recorder_offline_01.png';
import recorderOffline02Img from '@/assets/images/marker/recorder_offline_02.png';
import recorderOffline03Img from '@/assets/images/marker/recorder_offline_03.png';
import recorderOffline04Img from '@/assets/images/marker/recorder_offline_04.png';
import recorderOffline05Img from '@/assets/images/marker/recorder_offline_05.png';
import recorderOnlineImage from '@/assets/images/marker/recorder_online.png';
import recorderOnline01Img from '@/assets/images/marker/recorder_online_01.png';
import recorderOnline02Img from '@/assets/images/marker/recorder_online_02.png';
import recorderOnline03Img from '@/assets/images/marker/recorder_online_03.png';
import recorderOnline04Img from '@/assets/images/marker/recorder_online_04.png';
import recorderOnline05Img from '@/assets/images/marker/recorder_online_05.png';
import seatOnlineImg from '@/assets/images/marker/seat_marker.png';
import seatClusterImg from '@/assets/images/marker/seat_marker_count.png';
import seatOfflineImg from '@/assets/images/marker/seat_marker_offline.png';

// 记录仪结束
import simpleMarkerRedImg from '@/assets/images/marker/simple_marker_red.png';
import missionCompletedImg from '@/assets/images/marker/task_completed_marker.png';
import missionGoingImg from '@/assets/images/marker/task_going_marker.png';
import missionClusterImg from '@/assets/images/marker/task_marker_count.png';
import missionWaitingImg from '@/assets/images/marker/task_waiting_marker.png';
import terminalOnlineImg from '@/assets/images/marker/terminal_marker.png';
import terminalClusterImg from '@/assets/images/marker/terminal_marker_count.png';
import terminalOfflineImg from '@/assets/images/marker/terminal_marker_offline.png';
import uavOnlineImg from '@/assets/images/marker/uav_marker.png';
import uavClusterImg from '@/assets/images/marker/uav_marker_count.png';
import uavOfflineImg from '@/assets/images/marker/uav_marker_offline.png';
import { useCreateApp, useEmitter } from '@/hooks';
import { statusOptions } from '@/pages/policeAdmin/serviceStatus/common';
import { customLayerName } from '@/plugins/map';
import { useResourceStore } from '@/store';
import { domConvertsToBase64, imageToBase64 } from '@/utils';

function baseMarker({ data, image }) {
  const parent = document.createElement('div');
  const instance = useCreateApp(CommonMarker, {
    data,
    image,
  });
  const el = instance.mount(parent).$el;
  const ret = domConvertsToBase64(el, 100, 100);
  return ret;
}

// 摄像头
export async function addMonitorLayer(options) {
  const { data, isShow, layerId, map } = options;
  let image = '';
  let clusterImage = '';
  const isOnline = layerId === 'monitorOnline';
  image = isOnline ? monitorOnlineImg : monitorOfflineImg;
  clusterImage = monitorClusterImg;

  await imageToBase64(image).then((res) => {
    image = res as string;
  });

  map.addLayer({
    clusterImage,
    data,
    image,
    isShow,
    layerId,
    renderer(data, { mapType }) {
      if (mapType === 'openLayer') {
        return baseMarker({ data, image });
      }
    },
    windowTemplate: MonitorPopup,
    zIndex: isOnline ? 2 : 1,
  });
}

// 警员
export async function addPoliceManLayer(options) {
  const { data, isShow, layerId, map } = options;
  const isOnline = layerId === 'personOnline';
  let image = isOnline ? policeOnImg : policeOffImg;

  await imageToBase64(image).then((res) => {
    image = res as string;
  });

  let leaderOnlineImg;
  await imageToBase64(policeLeaderOnlineImg).then((res) => {
    leaderOnlineImg = res as string;
  });

  let leaderOfflineImg;
  await imageToBase64(policeLeaderOfflineImg).then((res) => {
    leaderOfflineImg = res as string;
  });

  await map.addLayer({
    clusterImage: policeClusterImg,
    data,
    image,
    isShow,
    layerId,
    renderer(data: any, options) {
      const { layerConfig, map, mapType } = options || {};
      let height = '50px';
      let bottom = '-16px';
      if (data?.leader) {
        height = '70px';
        bottom = '-10px';
        image = isOnline ? leaderOnlineImg : leaderOfflineImg;
      }

      switch (mapType) {
        case 'amap': {
          const { attendance } = data;
          let color;
          switch (attendance) {
            case 1: {
              color = '#7ec8ff';
              break;
            }
            case 2: {
              color = '#8d8e8e';
              break;
            }
            case 3: {
              color = '#fca701';
              break;
            }
          }
          let statusName = '';
          statusOptions().forEach((item) => {
            if (item.value === attendance) {
              statusName = item.label;
            }
          });

          const el = `<div
          xmlns="http://www.w3.org/1999/xhtml"
          style="
            display: flex;
            justify-content: center;
            align-items: center;
            width: 40px;
            height: ${height};
            position: relative;
          "
          >
            <img src="${image}" alt="" style="width: 40px; height: ${height}" />
            <div
              style="
                display: ${statusName ? 'block' : 'none'};
                height: 16px;
                position: absolute;
                bottom: 16px;
                color: black;
                line-height: 16px;
                text-align: center;
                font-size: 12px;
                padding: 0 2px;
                background-color: ${color};
              "
            >
              ${statusName}
            </div>
            <div
              style="
                height: 16px;
                position: absolute;
                bottom: ${bottom};
                line-height: 16px;
                text-align: center;
                color: #fff;
                font-size: 10px;
                white-space: nowrap;
                background-color: rgba(1, 1, 1, 0.5);
              "
            >
              ${data.name}
            </div>
          </div>`;
          return el;
        }
        case 'arcgis': {
          const fields = [
            {
              name: 'name',
              type: 'string',
            },
            {
              name: 'statusName',
              type: 'string',
            },
          ];
          layerConfig.fields.push(...fields);
          layerConfig.labelingInfo = [
            {
              allowOverrun: false,
              deconflictionStrategy: 'static',
              labelExpressionInfo: {
                expression: '$feature.statusName',
              },
              labelPlacement: 'center-center',
              symbol: {
                color: '#fff',
                font: {
                  size: '12px',
                  weight: 'bold',
                },
                haloColor: 'gray',
                haloSize: '15px',
                type: 'text',
                xoffset: 0,
                yoffset: -5,
              },
            },
            {
              allowOverrun: false,
              deconflictionStrategy: 'static',
              labelExpressionInfo: {
                expression: '$feature.name',
              },
              labelPlacement: 'center-center',
              symbol: {
                color: '#fff',
                font: {
                  size: '12px',
                  weight: 'bold',
                },
                type: 'text',
                xoffset: 0,
                yoffset: -15,
              },
            },
          ];

          break;
        }
        case 'mapabc':
        case 'mineMap': {
          if (data) {
            if (data.leader) {
              return isOnline ? 'leaderOnline' : 'leaderOffline';
            } else {
              return isOnline ? 'policeOnline' : 'policeOffline';
            }
          } else {
            const arr = [
              {
                id: 'policeOnline',
                img: policeOnImg,
              },
              {
                id: 'policeOffline',
                img: policeOffImg,
              },
              {
                id: 'leaderOnline',
                img: policeLeaderOnlineImg,
              },
              {
                id: 'leaderOffline',
                img: policeLeaderOfflineImg,
              },
            ];
            arr.forEach((item) => {
              map.loadImage(item.img, (error, img) => {
                if (!error && !map.hasImage(item.id)) {
                  map.addImage(item.id, img);
                }
              });
            });
          }

          break;
        }
        default: {
          const parent = document.createElement('div');
          const instance = useCreateApp(PoliceMarker, {
            data,
            image,
          });
          const el = instance.mount(parent).$el;
          return domConvertsToBase64(el, 100, 100);
        }
      }
    },
    windowTemplate: PolicePopup,
    zIndex: isOnline ? 10 : 9,
  });
}

// 警车
export function addPoliceCarLayer(options) {
  const { data, isShow, map } = options;
  map.addLayer({
    clusterImage: policeCarClusterImg,
    data,
    image: policeCarImg,
    isCluster: true, // 是否聚合
    isShow,
    layerId: 'policeCar',
    windowTemplate: CarPopup,
    zIndex: 4,
  });
}

// 任务
export function addMissionLayer(options) {
  const { data, isShow, layerId, map } = options;
  const img = {
    taskCompleted: missionCompletedImg,
    taskGoing: missionGoingImg,
    taskWaiting: missionWaitingImg,
  };
  const index = {
    taskCompleted: 11,
    taskGoing: 12,
    taskWaiting: 13,
  };
  map.addLayer({
    clusterImage: missionClusterImg,
    data,
    image: img[layerId],
    isShow,
    layerId,
    windowTemplate: MissionPopup,
    zIndex: index[layerId],
  });
}

// 预警
export async function addAlarmLayer(options) {
  const { data, isShow, layerId, map } = options;
  const img = {
    alarmCompleted: alarmCompletedImg,
    alarmWaiting: alarmWaitingImg,
  };
  const index = {
    alarmCompleted: 14,
    alarmWaiting: 15,
  };
  const imgObj = {
    alarmCompleted01Img,
    alarmCompleted02Img,
    alarmCompletedImg,
    alarmWaiting01Img,
    alarmWaiting02Img,
    alarmWaitingImg,
  };

  for (const key in imgObj) {
    await imageToBase64(imgObj[key]).then((res) => {
      imgObj[key] = res;
    });
  }

  map.addLayer({
    clusterImage: alarmClusterImg,
    data,
    image: img[layerId],
    isShow,
    layerId,
    renderer(data, options) {
      const { map, mapType } = options || {};

      let img = '';
      const width = '40px';
      const height = '50px';

      if (data) {
        if (layerId === 'alarmWaiting') {
          img = 'alarmWaitingImg';
          if (data.alarmLevel === '1') {
            img = 'alarmWaiting01Img';
          } else if (data.alarmLevel === '2') {
            img = 'alarmWaiting02Img';
          }
        } else {
          img = 'alarmCompletedImg';
          if (data.alarmLevel === '1') {
            img = 'alarmCompleted01Img';
          } else if (data.alarmLevel === '2') {
            img = 'alarmCompleted02Img';
          }
        }
      }

      if (mapType === 'mapabc' || mapType === 'mineMap') {
        if (data) {
          return img;
        } else {
          Object.keys(imgObj).forEach((key) => {
            map.loadImage(imgObj[key], (error, img) => {
              if (!error && !map.hasImage(key)) {
                map.addImage(key, img);
              }
            });
          });
        }
      }

      if (mapType === 'openLayer') {
        return baseMarker({ data, image: imgObj[img] });
      }

      const el = `<div
        xmlns="http://www.w3.org/1999/xhtml"
        style="
          display: flex;
          justify-content: center;
          align-items: center;
          width: ${width};
          height: 48px;
          position: relative;
        "
      >
        <img src="${imgObj[img]}" alt="" style="width: ${width}; height: ${height}" />
      </div>`;
      return el;
    },
    windowTemplate: AlarmPopup,
    zIndex: index[layerId],
  });
}

// 终端
export async function addTerminalLayer(options) {
  const { data, isShow, layerId, map } = options;
  const isOnline = layerId === 'terminalOnline';

  let image = isOnline ? terminalOnlineImg : terminalOfflineImg;
  await imageToBase64(image).then((res) => {
    image = res as string;
  });

  await map.addLayer({
    clusterImage: terminalClusterImg,
    data,
    image,
    isShow,
    layerId,
    renderer(data, { mapType }) {
      if (mapType === 'openLayer') {
        return baseMarker({ data, image });
      }
    },
    windowTemplate: PolicePopup,
    zIndex: isOnline ? 6 : 5,
  });
}

// 记录仪
export async function addRecorderLayer(options) {
  const { data, isShow, layerId, map } = options;
  const isOnline = layerId === 'recorderOnline';
  const img = isOnline ? recorderOnlineImage : recorderOfflineImage;

  const imgObj = {
    recorderClusterImg,
    recorderLeaderOffline01Img,
    recorderLeaderOffline02Img,
    recorderLeaderOffline03Img,
    recorderLeaderOffline04Img,
    recorderLeaderOffline05Img,
    recorderLeaderOfflineImg,
    recorderLeaderOnline01Img,
    recorderLeaderOnline02Img,
    recorderLeaderOnline03Img,
    recorderLeaderOnline04Img,
    recorderLeaderOnline05Img,
    recorderLeaderOnlineImg,
    recorderOffline01Img,
    recorderOffline02Img,
    recorderOffline03Img,
    recorderOffline04Img,
    recorderOffline05Img,
    recorderOfflineImage,
    recorderOnline01Img,
    recorderOnline02Img,
    recorderOnline03Img,
    recorderOnline04Img,
    recorderOnline05Img,
    recorderOnlineImage,
  };

  for (const key in imgObj) {
    await imageToBase64(imgObj[key]).then((res) => {
      imgObj[key] = res;
    });
  }

  await map.addLayer({
    clusterImage: recorderClusterImg,
    data,
    image: img,
    isShow,
    layerId,
    renderer(data, options) {
      const { map, mapType } = options || {};

      let img = '';
      let width = '40px';
      let height = '50px';

      if (data) {
        if (data.belong) {
          width = '85px';
        }
        if (data.leader || data.belong) {
          height = '80px';
        }
        if (isOnline && data.leader && data.belong) {
          const imgs = {
            1001: 'recorderLeaderOnline01Img',
            1002: 'recorderLeaderOnline02Img',
            1003: 'recorderLeaderOnline03Img',
            1004: 'recorderLeaderOnline04Img',
            1005: 'recorderLeaderOnline05Img',
          };
          img = imgs[data.belong] || 'recorderLeaderOnlineImg';
        } else if (isOnline && !data.leader && data.belong) {
          const imgs = {
            1001: 'recorderOnline01Img',
            1002: 'recorderOnline02Img',
            1003: 'recorderOnline03Img',
            1004: 'recorderOnline04Img',
            1005: 'recorderOnline05Img',
          };
          img = imgs[data.belong] || 'recorderOnlineImage';
        } else if (isOnline && data.leader && !data.belong) {
          img = 'recorderLeaderOnlineImg';
        } else if (isOnline && !data.leader && !data.belong) {
          img = 'recorderOnlineImage';
        } else if (!isOnline && data.leader && data.belong) {
          const imgs = {
            1001: 'recorderLeaderOffline01Img',
            1002: 'recorderLeaderOffline02Img',
            1003: 'recorderLeaderOffline03Img',
            1004: 'recorderLeaderOffline04Img',
            1005: 'recorderLeaderOffline05Img',
          };
          img = imgs[data.belong] || 'recorderLeaderOfflineImg';
        } else if (!isOnline && !data.leader && data.belong) {
          const imgs = {
            1001: 'recorderOffline01Img',
            1002: 'recorderOffline02Img',
            1003: 'recorderOffline03Img',
            1004: 'recorderOffline04Img',
            1005: 'recorderOffline05Img',
          };
          img = imgs[data.belong] || 'recorderOfflineImage';
        } else if (!isOnline && data.leader && !data.belong) {
          img = 'recorderLeaderOfflineImg';
        } else if (!isOnline && !data.leader && !data.belong) {
          img = 'recorderOfflineImage';
        }
      }

      if (mapType === 'mapabc' || mapType === 'mineMap') {
        if (data) {
          return img;
        } else {
          Object.keys(imgObj).forEach((key) => {
            map.loadImage(imgObj[key], (error, img) => {
              if (!error && !map.hasImage(key)) {
                map.addImage(key, img);
              }
            });
          });
        }
      }

      if (mapType === 'openLayer') {
        return baseMarker({ data, image: imgObj[img] });
      }

      const el = `<div
        xmlns="http://www.w3.org/1999/xhtml"
        style="
          display: flex;
          justify-content: center;
          align-items: center;
          width: ${width};
          height: 48px;
          position: relative;
        "
      >
        <img src="${imgObj[img]}" alt="" style="width: ${width}; height: ${height}" />
      </div>`;
      return el;
    },
    windowTemplate: PolicePopup,
    zIndex: isOnline ? 8 : 7,
  });
}

// 布控球
export async function addBallCameraLayer(options) {
  const { data, isShow, layerId, map } = options;
  const isOnline = layerId === 'ballCameraOnline';

  let image = isOnline ? ballCameraOnlineImg : ballCameraOfflineImg;
  await imageToBase64(image).then((res) => {
    image = res as string;
  });

  await map.addLayer({
    clusterImage: ballCameraClusterImg,
    data,
    image,
    isShow,
    layerId,
    renderer(data, { mapType }) {
      if (mapType === 'openLayer') {
        return baseMarker({ data, image });
      }
    },
    windowTemplate: PolicePopup,
    zIndex: isOnline ? 6 : 5,
  });
}

// PDT
export async function addPdtLayer(options) {
  const { data, isShow, layerId, map } = options;
  const isOnline = layerId === 'pdtOnline';

  let image = isOnline ? pdtOnlineImg : pdtOfflineImg;
  await imageToBase64(image).then((res) => {
    image = res as string;
  });

  map.addLayer({
    clusterImage: pdtClusterImg,
    data,
    image,
    isShow,
    layerId,
    renderer(data, { mapType }) {
      if (mapType === 'openLayer') {
        return baseMarker({ data, image });
      }
    },
    windowTemplate: PolicePopup,
    zIndex: isOnline ? 4 : 3,
  });
}

// 车载图传
export async function addCarPhotoLayer(options) {
  const { data, isShow, layerId, map } = options;
  const isOnline = layerId === 'carPhotoOnline';

  let image = isOnline ? policeCarImg : policeCarOfflineImg;
  await imageToBase64(image).then((res) => {
    image = res as string;
  });

  map.addLayer({
    clusterImage: policeCarClusterImg,
    data,
    image,
    isShow,
    layerId,
    renderer(data, { mapType }) {
      if (mapType === 'openLayer') {
        data.speed = 17.15;
        return baseMarker({ data, image });
      }
    },
    windowTemplate: MonitorPopup,
    zIndex: isOnline ? 4 : 3,
  });
}

// 国标记录仪
export async function addGBRecLayer(options) {
  const { data, isShow, layerId, map } = options;
  const isOnline = layerId === 'GBRecorderOnline';

  let image = isOnline ? GBRecordImg : GBRecordOfflineImg;
  await imageToBase64(image).then((res) => {
    image = res as string;
  });

  map.addLayer({
    clusterImage: GBRecordClusterImg,
    data,
    image,
    isShow,
    layerId,
    renderer(data, { mapType }) {
      if (mapType === 'openLayer') {
        return baseMarker({ data, image });
      }
    },
    windowTemplate: PolicePopup,
    zIndex: isOnline ? 8 : 7,
  });
}

// 自定义图层
export function addCustomLayer(options) {
  const { data, imgName, isCluster, isShow, layerId, layerName, map } = options;
  customLayerName[layerId] = layerName;
  const img = {
    custom_else: customElseImg,
    custom_house: customHouseImg,
    custom_school: customSchoolImg,
  };
  const clusterImg = {
    custom_else: customElseImg,
    custom_house: customHouseClusterImg,
    custom_school: customSchoolClusterImg,
  };
  map.addLayer({
    clusterImage: clusterImg[imgName],
    data,
    image: img[imgName],
    isCluster,
    isShow,
    layerId,
    windowTemplate: CustomPopup,
  });
}

// 紧急事件
export function addMessageLayer(options) {
  const { data, isShow, layerId, map } = options;
  map.addLayer({
    clusterImage: messageClusterImg,
    data,
    image: messageImg,
    isShow,
    layerId,
    windowTemplate: MessagePopup,
    zIndex: 15,
  });
}

// 资源搜索结果
export function addSearchLayer(options) {
  const { data, layerId, map } = options;
  map.addLayer({
    data,
    image: simpleMarkerRedImg,
    isCluster: false,
    isShow: true,
    layerId,
    windowTemplate: SearchPopup,
  });
}

// 添加三道防线/自定义图层
export async function addDefenseLayer(options) {
  const { data, map, type } = options;
  const { customIcon } = useResourceStore();
  data.forEach((element) => {
    const url = customIcon.find((icon) => {
      return icon.id === element.iconCode;
    });
    if (url) {
      map.addDefenseMarker(url.iconInfo, element, DefensePopup, type);
    }
  });
}

// 无人机
export async function addUavLayer(options) {
  const { data, isShow, layerId, map } = options;
  const isOnline = layerId === 'uavOnline';

  let image = isOnline ? uavOnlineImg : uavOfflineImg;
  await imageToBase64(image).then((res) => {
    image = res as string;
  });

  map.addLayer({
    clusterImage: uavClusterImg,
    data,
    image,
    isShow,
    layerId,
    renderer(data, { mapType }) {
      if (mapType === 'openLayer') {
        return baseMarker({ data, image });
      }
    },
    windowTemplate: MonitorPopup,
    zIndex: isOnline ? 4 : 3,
  });
}

// 坐席
export async function addSeatLayer(options) {
  const { data, isShow, layerId, map } = options;
  const isOnline = layerId === 'seatOnline';

  let image = isOnline ? seatOnlineImg : seatOfflineImg;
  await imageToBase64(image).then((res) => {
    image = res as string;
  });

  map.addLayer({
    clusterImage: seatClusterImg,
    data,
    image,
    isShow,
    layerId,
    renderer(data, { mapType }) {
      if (mapType === 'openLayer') {
        return baseMarker({ data, image });
      }
    },
    windowTemplate: PolicePopup,
    zIndex: isOnline ? 4 : 3,
  });
}

// 标绘
export function addPlotLayer(options) {
  const { data, editable = true, handle, map } = options;
  const { sandTableIcon, setSelectFeature } = useResourceStore();
  data.forEach((obj) => {
    // 图标
    const { imageName, type } = obj;
    if (type === 'icon' && imageName) {
      sandTableIcon.forEach((img) => {
        if (img.id === imageName) {
          map.addImageIcon(imageName, img.iconInfo);
          obj.iconUrl = img.iconInfo;
        }
      });
    }

    // 图形
    const param = {
      drawLayerId: 'plotLayer',
      handle,
      hex: true,
      highlight: true,
      ...obj,
      editable,
    };

    map.draw({
      ...param,
      change: (ret, id) => {
        if (ret) {
          data.forEach((item) => {
            if (item.id === id) {
              if (item.type === 'circle') {
                item.center = ret.center;
                item.radius = ret.radius;
              } else {
                item.path = ret;
              }
              useEmitter().emit('sandTableChange', item);
            }
          });
        } else {
          const index = data.findIndex((item) => {
            return item.id === id;
          });
          data.splice(index, 1);
          setSelectFeature('');
        }
      },
    });
  });
}

// 地标
export function addLandmarkLayer(options) {
  const { data, layerId, map } = options;
  const { landmarkIcon } = useResourceStore();
  data.forEach((item) => {
    const position = item.location.split(',');
    const url = landmarkIcon.find((icon) => {
      return icon.id === item.iconUrl;
    });
    if (url) {
      map.addMarkerAndPopup(url.iconInfo, item, {
        callback: () => {
          useEmitter().emit('clickLandmark', item.organizationId);
        },
        layerId,
        position,
        windowTemplate: LandmarkPopup,
      });
    }
  });
}
