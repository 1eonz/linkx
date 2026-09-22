import { queryOrganizationById } from '@/api/resource';
import { getIconList } from '@/api/sandTable';
import appConfig from '@/config/appConfig';
import { drawJurisdiction } from '@/pages/bigScreen/mapCenter/jurisdiction/helper';
import { store } from '@/store';
import { getIp } from '@/utils';
import { getToken } from '@/utils/auth';

import { cloneDeep } from 'lodash-es';
import { defineStore } from 'pinia';

interface ResourceState {
  activeGroupIds: string[];
  commonGroup: any[];
  customIcon: any[];
  deleteGroupId: string[];
  dynamicGroup: any[];
  landmarkIcon: any[];
  layerChecked: string[];
  mapChooseData: string[];
  mapChoosePlanId: string;
  organization: any[];
  patchGroup: any[];
  policeLayerChecked: string[];
  sandTableIcon: any[];
  selectFeatureData: any;
  speedAlarmTime: string;
  vehicleIcon: any[];
}

export const useResourceStore = defineStore({
  actions: {
    // 新增未读短信数
    addGroupUnReadMsg(data) {
      const dynamicGroup = this.dynamicGroup.map((item) => {
        if (item.groupId === data.groupid) {
          item.unReadMsgLength += 1;
        }
        return item;
      });
      const commonGroup = this.commonGroup.map((item) => {
        if (item.groupId === data.groupid) {
          item.unReadMsgLength += 1;
        }
        return item;
      });
      this.dynamicGroup = dynamicGroup;
      this.commonGroup = commonGroup;
    },

    // 通过id删除动态群组
    deleteDynamicGroupById(id) {
      this.deleteGroupId.push(id);
      this.dynamicGroup = this.dynamicGroup.filter((item) => item.groupId !== id);
    },

    // 获取组织
    async getOrganization() {
      const param = {
        id: appConfig.userData.organizationId,
      };
      const { code, data } = await queryOrganizationById(param);
      if (code === 0 && data.length > 0) {
        this.organization = data;
        drawJurisdiction({ adcode: data[0].code });
      }
    },

    // 保存静态群组数据
    initCommonGroup(data) {
      this.commonGroup = data;
    },

    // 保存动态数据
    initDynamicGroup(data) {
      this.dynamicGroup = data.filter((item) => !this.deleteGroupId.includes(item.groupId));
      this.deleteGroupId = [];
    },

    // 保存派接组数据
    initPatchGroup(data) {
      this.patchGroup = data;
    },

    // 缓存图上标绘、车辆幻化、图标单位icon图标
    async saveGlobalIcon() {
      const loadImg = (iconType, data) => {
        for (const [i, { iconInfo, id, type }] of data.entries()) {
          const url = `${getIp()}/linkx/desktop${iconInfo}`;
          const xhr = new XMLHttpRequest();

          xhr.open('GET', url, true);
          xhr.responseType = 'blob';
          xhr.setRequestHeader('Authorization', `token ${getToken()}`);
          xhr.addEventListener('load', () => {
            if (xhr.status === 200) {
              const binary = [xhr.response];
              const icon = {
                iconInfo: URL.createObjectURL(new Blob(binary)),
                id,
                type,
              };
              const target = {
                1: this.sandTableIcon,
                2: this.vehicleIcon,
                3: this.landmarkIcon,
                4: this.customIcon,
              };
              target[iconType][i] = icon;
            }
          });
          xhr.send();
        }
      };

      const res1 = await getIconList(1);
      if (res1.code === 0) {
        loadImg(1, res1.data);
      }
      const res2 = await getIconList(2);
      if (res2.code === 0) {
        loadImg(2, res2.data);
      }
      const res3 = await getIconList(3);
      if (res3.code === 0) {
        loadImg(3, res3.data);
      }
      const res4 = await getIconList(4);
      if (res4.code === 0) {
        loadImg(4, res4.data);
      }
    },

    setActiveGroupId(index, data) {
      if (index > 0 && index <= 4) {
        this.activeGroupIds[index - 1] = data;
      }
    },

    setGroupsData({ contentType, groupType, id }) {
      switch (contentType) {
        case 'clearSubNum': {
          this.dynamicGroup.map((item) => {
            if (item.groupId === id) {
              item.unReadMsgLength = 0;
              item.unReadMsgType = false;
            }
            return item;
          });
          this.commonGroup.map((item) => {
            if (item.groupId === id) {
              item.unReadMsgLength = 0;
              item.unReadMsgType = false;
            }
            return item;
          });
          break;
        }
        case 'shutDownGroupCard': {
          if (groupType === '0' || groupType === '9') {
            this.dynamicGroup.map((item) => {
              if (item.groupId === id) {
                item.unReadMsgType = true;
              }
              return item;
            });
          } else if (groupType === '1') {
            this.commonGroup.map((item) => {
              if (item.groupId === id) {
                item.unReadMsgType = true;
              }
              return item;
            });
          }
          break;
        }
      }
    },

    setLayerChecked(data) {
      this.layerChecked = data;
    },

    // 地图选看数据
    setMapChooseData(data, planId?) {
      this.mapChoosePlanId = data.length > 0 ? planId : '';
      this.mapChooseData = data;
    },

    setPoliceLayerChecked(data) {
      this.policeLayerChecked = data;
    },

    // 保存二次编辑所选图形样式
    setSelectFeature(data) {
      this.selectFeatureData = data;
    },

    // 超速告警刷新时间设置
    setSpeedAlarmTime(data) {
      this.speedAlarmTime = data;
    },

    // 保存接收的消息
    unReadMessage({ isdn, value }) {
      const data = JSON.parse(localStorage.getItem('unReadMsgStorage') || ' {}');
      if (data?.[isdn]) {
        data[isdn].push(value);
      } else {
        data[isdn] = [value];
      }
      if (data[isdn].length > 60) {
        data[isdn] = data[isdn].slice(-60);
      }
      localStorage.setItem('unReadMsgStorage', JSON.stringify(data));
    },

    updateGroup(type, data) {
      if (type === 'dynamicGroup') {
        this.dynamicGroup = data;
      } else {
        this.commonGroup = data;
      }
    },

    // 更新派接组
    updatePatchGroup({ deleteNum = 1, groupItem, index }) {
      if (groupItem) {
        this.patchGroup.splice(index, deleteNum, cloneDeep(groupItem));
      } else {
        this.patchGroup.splice(index, deleteNum);
      }
    },
  },
  getters: {
    // 获取沙盘绘制、车辆幻化、地标单位图标url
    getIcon(state) {
      const ret: any = {};
      const { landmarkIcon, sandTableIcon, vehicleIcon } = state;
      const arr = [...sandTableIcon, ...vehicleIcon, ...landmarkIcon];
      arr.forEach((item) => {
        if (item) {
          ret[item.id] = item.iconInfo;
        }
      });
      return ret;
    },
    // 获取组织id列表
    getOrganizationIds() {
      const ret: string[] = [];
      const mapper = (data) => {
        data.map((item) => {
          if (item.children) {
            mapper(item.children);
          }
          return ret.push(item.id);
        });
      };
      mapper(this.organization);
      return ret;
    },
    // 组织列表Map组装
    getOrganizationMap() {
      const mapData = new Map();
      const mapper = (data) => {
        data.map((item) => {
          if (item.children) {
            mapper(item.children);
          }
          return mapData.set(item.id, item.name);
        });
      };
      mapper(this.organization);
      return mapData;
    },
  },
  id: 'resource',
  state: (): ResourceState => ({
    activeGroupIds: ['', '', '', ''], // 一屏统览活跃群组集合
    commonGroup: [], // 静态组
    customIcon: [], // 自定义图层图标
    deleteGroupId: [],
    dynamicGroup: [], // 动态组
    landmarkIcon: [], // 地标单位
    layerChecked: [], // 被选中的图层
    mapChooseData: [], // 地图选看数据
    mapChoosePlanId: '',
    organization: [], // 组织
    patchGroup: [], // 派接组
    policeLayerChecked: [], // 被选中的警员图层
    sandTableIcon: [], // 图上标绘icon
    selectFeatureData: '', // 二次编辑所选图层样式
    speedAlarmTime: '15', // 超速告警刷新间隔
    vehicleIcon: [], // 车辆幻化
  }),
});

// Need to be used outside the setup
export function useResourceStoreWithOut() {
  return useResourceStore(store);
}
