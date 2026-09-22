import { resourceTypes } from '@/pages/resource/resourceHelper';
import { store } from '@/store';

import { defineStore } from 'pinia';

interface TreeState {
  resourceCheckedList: {
    [key: string]: any[];
  };
  resourceOneKeyType: string;
}

export function defaultCheckedList() {
  const ret: TreeState['resourceCheckedList'] = {};
  resourceTypes.forEach((i) => {
    ret[i] = [];
  });
  return ret;
}

export const useTreeStore = defineStore({
  actions: {
    clearResourceCheckedList() {
      this.resourceCheckedList = defaultCheckedList();
    },
    // 改变选中的资源
    setResourceCheckedList({ data, type }) {
      const obj = { ...this.resourceCheckedList };
      // 去重
      const keySet = new Set();
      const arr: any[] = [];
      data?.forEach((item) => {
        if (!keySet.has(item.id)) {
          keySet.add(item.id);
          arr.push(item);
        }
      });
      obj[type] = [...arr];
      this.resourceCheckedList = {
        ...obj,
      };
    },
    // 资源列表一键操作
    setResourceOneKeyType(type) {
      this.resourceOneKeyType = type;
    },
  },
  id: 'tree',
  state: (): TreeState => ({
    resourceCheckedList: defaultCheckedList(),
    resourceOneKeyType: '',
  }),
});

// Need to be used outside the setup
export function useTreeWithOut() {
  return useTreeStore(store);
}
