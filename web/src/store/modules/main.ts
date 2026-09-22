import { useRoute } from 'vue-router';

import { store } from '@/store';

import { defineStore } from 'pinia';

interface MainState {
  drawerVisible: boolean;
  hostIsdn: string;
  isFull: boolean;
  isLogin: boolean;
  layerIds: string[];
  minCardId: string;
  minCarLists: any;
  route: any;
  serverTime: number;
  showMessageDrawer: boolean;
  videoControlShowId: string;
  videoControlType: string;
  warningNoticeShowId: string;
}

export const useMainStore = defineStore({
  actions: {
    changeHostIsdn({ hostIsdn }) {
      this.hostIsdn = hostIsdn;
    },
    deleteLayerId(data) {
      const index = this.layerIds.indexOf(data);
      if (index !== -1) {
        this.layerIds.splice(index, 1);
      }
    },
    initializationMinCarLists(data) {
      this.minCarLists[data] = [];
    },
    setDrawerVisible(data: boolean) {
      this.drawerVisible = data;
    },
    setIsFull(data) {
      this.isFull = data;
    },
    setIsLogin(isLogin) {
      this.isLogin = isLogin;
    },
    // 设置弹窗id
    setLayerId(data) {
      if (this.layerIds.includes(data)) {
        return;
      }
      this.layerIds.push(data);
    },
    setServerTime(data) {
      this.serverTime = Number(data);
    },
    setShowMessageDrawer(data) {
      this.showMessageDrawer = data;
    },
    unDataMinCardId(id) {
      this.minCardId = id;
    },
    // 最小化卡片
    upDataMinCardLists({ add, del, id }: { add?: any; del?: any; id: string }) {
      if (add) {
        const index = this.minCarLists[id].findIndex((item) => {
          return item.id === add.id;
        });
        if (index !== -1) {
          console.log('重复添加');
          return;
        }
        this.minCarLists[id].push(add);
      } else {
        let index = 0;
        for (let i = 0; i < this.minCarLists[id].length; i++) {
          if (this.minCarLists[id][i].id === del.id) {
            index = i;
          }
        }
        this.minCarLists[id].splice(index, 1);
      }
    },
    updateVideoControlShowId(data) {
      this.videoControlShowId = data;
    },
    updateVideoControlType(data) {
      this.videoControlType = data;
    },
    updateWarningNoticeShowId(data: string) {
      this.warningNoticeShowId = data;
    },
  },
  id: 'main',
  state: (): MainState => ({
    drawerVisible: false, // 通话列表是否展开
    hostIsdn: '',
    isFull: false, // 是否全屏
    isLogin: false,
    layerIds: [], // 弹窗id
    minCardId: '', // 最小化的卡片类别id
    minCarLists: {}, // 最小化的卡片列表
    route: useRoute(),
    serverTime: 0, // 服务器时间
    showMessageDrawer: false,
    videoControlShowId: '',
    videoControlType: 'WarningNotice',
    warningNoticeShowId: '', // 显示详情卡片id
  }),
});

// Need to be used outside the setup
export function useMainStoreWithOut() {
  return useMainStore(store);
}
