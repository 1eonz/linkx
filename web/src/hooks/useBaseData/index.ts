/**
 * 用定时器去取里面的数据
 */

import { resourceCategory } from '@/pages/resource/resourceHelper';
import { useResourceStoreWithOut } from '@/store';

import { throttle } from 'lodash-es';

const resourceOrigin = {}; // 全量资源 { 50001: { key: val } }
resourceCategory.forEach((category) => (resourceOrigin[category] = {}));
let resourceList = {}; // { 50001: [] }
let resourceOrganization = {};

const personFromIdCard = {}; // 绑定身份证号的人员（im每个人都需要绑定身份证号）

const transformResourceOriginToList = throttle(() => {
  const ret: any = {};
  Object.keys(resourceOrigin).forEach((category) => {
    const arr: any[] = [];
    const target = resourceOrigin[category];
    if (target) {
      Object.keys(target).forEach((id) => {
        arr.push(target[id]);
      });
    }
    ret[category] = arr;
  });
  resourceList = ret;
}, 1000);

const transformResourceOriginToOrganization = throttle(() => {
  const ret: any = {};
  Object.keys(resourceOrigin).forEach((category) => {
    const obj: any = {};
    const target = resourceOrigin[category];
    if (target) {
      Object.keys(target).forEach((id) => {
        const item = target[id];
        if (!obj[item.organizationId]) {
          obj[item.organizationId] = [];
        }
        obj[item.organizationId].push(item);
      });
    }
    ret[category] = obj;
  });
  resourceOrganization = ret;
}, 1000);

export const useBaseData = (): any => {
  const { getOrganizationMap } = useResourceStoreWithOut();

  // 缓存资源（设备人员监控）
  const setResourceOrigin = (category, data) => {
    if (!resourceOrigin[category]) {
      resourceOrigin[category] = {};
    }
    const resource = resourceOrigin[category];
    data.forEach((item) => {
      if (!getOrganizationMap.has(item.organizationId)) {
        return;
      }

      const target = resource[item.id];
      if (target) {
        Object.keys(item).forEach((key) => {
          const val = item[key];
          if (val !== undefined && val !== null) {
            target[key] = val;
          }
        });
      } else {
        resource[item.id] = item;
      }
    });

    transformResourceOriginToList();
    transformResourceOriginToOrganization();
  };

  // 更新资源定位
  const updateResourceOriginPosition = (category, data) => {
    setResourceOrigin(category, data);
  };

  const setPersonFromIdCard = (data) => {
    data.forEach((item) => {
      const { idCardNum } = item;
      if (idCardNum) {
        personFromIdCard[idCardNum] = item;
      }
    });
  };

  return {
    getResourceOrganization: resourceOrganization, // 获取资源树
    getResourceOrigin: resourceList, // 获取资源
    personFromIdCard,
    resourceOrigin,
    setPersonFromIdCard,
    setResourceOrigin,
    updateResourceOriginPosition,
  };
};
