import { queryEquipmentTypeByGisFlag } from '@/api/equipment';
import { createVehicleList, deleteVehicle, queryVehicle } from '@/api/vehicle';
import { Message } from '@/components/Message';
import { useBaseData, useSetInterval } from '@/hooks';
import { mapManager, setPosition } from '@/plugins/map';
import { store } from '@/store';

import { defineStore } from 'pinia';

interface VehicleState {
  clearTimer: any;
  headerCar: any;
  headerCarPopup: any;
  headerCarTrackData: any[];
  planId: string;
  underProtection: boolean; // 是否开始保障
  vehicleEquipmentType: number[];
  vehicleList: any[];
}

const headerCarTrackLayerId = 'headerCarTrackLayer';

export const useVehicleStore = defineStore({
  actions: {
    // 列表添加车辆幻化 保障组id
    async addVehicleList(data) {
      const param: any[] = [];
      const curKeys: string[] = [];

      const create = (i, confirmDelete) => {
        return {
          confirmDelete,
          equipmentId: i.id,
          icon: i.icon,
          id: i.vehicleId,
          plangroupId: this.planId,
          type: i.isHeader ? 1 : 0,
        };
      };

      data.forEach((i) => {
        curKeys.push(i.id);
        param.push(create(i, false));
      });
      this.vehicleList.forEach((i) => {
        if (!curKeys.includes(i.id)) {
          param.push(create(i, true));
        }
      });
      const { code, msg } = await createVehicleList([...param]);
      if (code === 0) {
        this.queryVehicleList(this.planId);
        Message(msg);
      } else {
        Message({
          message: msg,
          type: 'error',
        });
      }
    },
    // 通过幻化id删除车辆幻化
    async delVehicleById(id) {
      const { code, msg } = await deleteVehicle(id);
      if (code === 0) {
        this.queryVehicleList(this.planId);
        Message(msg);
      } else {
        Message({
          message: msg,
          type: 'error',
        });
      }
    },
    // 初始化车辆幻化列表
    initVehicleList(planId) {
      this.clearTimer?.();
      this.queryEquipmentType();
      this.queryVehicleList(planId);
      this.clearTimer = useSetInterval(this.updateVehicleList, 1000);
    },
    // 查询那些设备可以幻化
    async queryEquipmentType() {
      const { code, data } = await queryEquipmentTypeByGisFlag();
      if (code === 0) {
        const arr: number[] = [];
        data.forEach((item) => {
          if (item.vehicleIllusionSwitch === 0) {
            arr.push(item.category);
          }
        });
        this.vehicleEquipmentType = arr;
      }
    },
    // 查询车辆幻化列表
    async queryVehicleList(planId) {
      this.planId = planId;

      const { code, data } = await queryVehicle({ plangroupId: planId });
      if (code === 0) {
        let header: any = null;
        this.vehicleList = data.map((item) => {
          setPosition(item);

          const { equipmentId, id, speed, type } = item;
          const ret = {
            ...item,
            id: equipmentId,
            isHeader: type === 1,
            speed: Number(speed) ? Number(speed).toFixed(2) : 0,
            vehicle: true,
            vehicleId: id,
          };

          if (item.type === 1) {
            header = ret;
          }
          return ret;
        });

        const mapObj = mapManager.get('mapId_main');
        if (this.headerCar?.id !== header?.id && mapObj?.isReady) {
          this.headerCarTrackData = [];
          mapObj.removeLayer(headerCarTrackLayerId);
        }
        this.headerCar = header;
      }
    },
    // 重置
    resetVehicle() {
      this.clearTimer?.();
      this.vehicleList = [];
      this.headerCarTrackData = [];
    },
    // 头车弹窗 - 过滤其他地方弹出重复视频
    setHeaderCarPopup(data) {
      this.headerCarPopup = data;
    },
    // 开始保障或者结束保障标识
    setUnderProtection(start: boolean) {
      this.underProtection = start;
    },
    // 更新车辆幻化 没有就添加
    updateVehicle(data) {
      const { id, isHeader } = data;
      let arr = [...this.vehicleList];
      arr = arr.map((item) => {
        return item.id === id ? data : { ...item, isHeader: isHeader ? false : item.isHeader };
      });

      const index = arr.findIndex((i) => i.id === id);
      if (index === -1) {
        arr.push(data);
      }
      this.addVehicleList(arr);
    },
    // 跟新幻化列表定位和速度/头车轨迹
    updateVehicleList() {
      const { resourceOrigin } = useBaseData();
      const { pathname } = location;

      const arr: any[] = [...this.vehicleList];
      arr.forEach((item) => {
        const origin = resourceOrigin[item.category]?.[item.equipmentId];
        if (origin) {
          const update = {
            direction: origin.direction,
            position: origin.position,
            speed: origin.speed ? Number(origin.speed).toFixed(2) : 0,
          };
          Object.assign(item, update);

          // 更新头车及轨迹
          const _mapId = pathname.includes('leadVehicle') ? 'mapId_current' : 'mapId_main';
          const mapObj = mapManager.get(_mapId);
          if (this.headerCar?.id === item.id && mapObj?.isReady) {
            if (
              this.underProtection &&
              (pathname.includes('planSpecial') || pathname.includes('leadVehicle'))
            ) {
              mapObj.setCenter(origin.position);
            }

            if (
              JSON.stringify(this.headerCarTrackData[this.headerCarTrackData.length - 1]) !==
              JSON.stringify(origin.position)
            ) {
              this.headerCarTrackData.push(origin.position);

              mapObj.removeLayer(headerCarTrackLayerId);
              mapObj.addLineLayer({
                layerId: headerCarTrackLayerId,
                path: this.headerCarTrackData,
                style: {
                  fillOutlineColor: 'gray',
                  fillOutlineWidth: 2,
                },
              });
            }
          }
        }
      });
      this.vehicleList = arr;
    },
  },
  id: 'vehicle',
  state: (): VehicleState => ({
    clearTimer: null,
    headerCar: null,
    headerCarPopup: null,
    headerCarTrackData: [],
    planId: '',
    underProtection: false,
    vehicleEquipmentType: [], // 那些设备类型可以幻化
    vehicleList: [],
  }),
});

// Need to be used outside the setup
export function useVehicleStoreWithOut() {
  return useVehicleStore(store);
}
