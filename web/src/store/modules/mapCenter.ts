import { store } from '@/store';

import { defineStore } from 'pinia';

interface MapCenterState {
  chooseSourcesList: any[];
  chooseVideoSourcesList: any[];
  showResource: boolean;
  showStatic: boolean;
}

export const useMapCenterStore = defineStore({
  actions: {
    // 添加选中资源
    addChooseSourcesList(data) {
      const arr = [...this.chooseSourcesList];
      if (arr.length > 0) {
        for (const item of data) {
          const index = arr.findIndex((child) => {
            return item.id === child.id;
          });
          if (index === -1) {
            arr.unshift({
              ...item,
            });
          }
        }
        this.chooseSourcesList = [...arr];
      } else {
        this.chooseSourcesList = [...data];
      }
    },
    addChooseVideoSourcesList(data) {
      const arr = [...this.chooseVideoSourcesList];
      if (arr.length > 0) {
        for (const item of data) {
          const index = arr.findIndex((child) => {
            return item.id === child.id;
          });
          if (index === -1) {
            arr.unshift({
              ...item,
            });
          }
        }
        this.chooseVideoSourcesList = [...arr];
      } else {
        this.chooseVideoSourcesList = data.splice(0, 99_999);
      }
    },
    initChooseSourcesList() {
      this.chooseSourcesList = [];
    },
    initChooseVideoSourcesList() {
      this.chooseVideoSourcesList = [];
    },
    // 资源列表是否显示状态更改
    setShowResource(showResource) {
      this.showResource = showResource;
    },
    // 统计列表是否显示状态更改
    setShowStatic(showStatic) {
      this.showStatic = showStatic;
    },
  },
  id: 'mapCenter',
  state: (): MapCenterState => ({
    chooseSourcesList: [],
    chooseVideoSourcesList: [],
    showResource: false,
    showStatic: false,
  }),
});

export function useMapCenterStoreWithOut() {
  return useMapCenterStore(store);
}
