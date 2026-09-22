import { Message } from '@/components/Message';
import { useI18n } from '@/hooks';
import { store } from '@/store';

import { defineStore } from 'pinia';

export type MonitorItem = {
  [key: string]: any;
  id: string;
  type: string;
};

type MonitorList = MonitorItem[];

interface PlayingList {
  facilityId: string;
  sign: string;
}

interface MonitorState {
  allBtnShowType: string;
  cameraClasses: any;
  cameraDefaultFavorateId: string;
  cameraFavoritesData: string[];
  cameraIdHightLight: string;
  currentInterface: 'map' | 'video';
  currentLattice: number;
  dataOfMapMode: MonitorList;
  dataOfVideoWall: MonitorList;
  monitorDragData: any;
  monitorDrawerData: any[];
  playingList: PlayingList[];
}

function createEmptyItems(len) {
  const emptyArr: MonitorList = [];
  for (let i = 0; i < len; i++) {
    const obj = {
      id: `000000${Math.floor(Math.random() * 999_999)}`.slice(-6),
      type: 'emptyMonitor',
    };
    emptyArr.push(obj);
  }
  return emptyArr;
}

export const useMonitorStore = defineStore({
  actions: {
    // 地图模式
    addDataOfMapMode(data) {
      if (data.length > 1) {
        data.sort((a, b) => {
          return a.distance - b.distance;
        });
      }
      this.dataOfMapMode = [...this.dataOfMapMode, ...data];
    },

    addDataOfVideoWall(data) {
      const { dataOfVideoWall } = this;
      let index = -1;
      for (const i in dataOfVideoWall) {
        if (dataOfVideoWall[i].type === 'emptyMonitor') {
          index = Number(i);
          break;
        }
      }
      if (index === -1) {
        this.dataOfVideoWall.splice(0, 1, data);
      } else {
        this.dataOfVideoWall.splice(index, 1, data);
      }
    },

    addDataOfVideoWallFromDrag(data) {
      if (this.dataOfVideoWall[data.index]?.type === 'emptyMonitor') {
        this.dataOfVideoWall.splice(data.index, 1, data.receiveDragData);
      } else {
        // 目标位置不为空对象时
        this.dataOfVideoWall.splice(data.index, 0, data.receiveDragData);
      }
    },

    addMonitorDrawerData(data) {
      const { t } = useI18n();
      if (this.monitorDrawerData.length >= 16) {
        Message(t('monitor.monitorTips.maxLengthGoDesktop'));
        return;
      }

      // 应该根据 account 来确定数据的唯一性
      const index = this.monitorDrawerData.findIndex((i) => {
        return i.account === data.account;
      });
      if (index === -1) {
        this.monitorDrawerData.push(data);
      }
    },

    addToCameraFavoritesData(data) {
      this.cameraFavoritesData = data;
    },
    adjustmentDataOfMapModeFromDrag(data) {
      const trueCircleData: MonitorList = [];
      this.dataOfMapMode.forEach((item) => {
        trueCircleData.push(item);
      });
      [trueCircleData[data.index], trueCircleData[data.dragIndex]] = [
        trueCircleData[data.dragIndex],
        trueCircleData[data.index],
      ];
      this.dataOfMapMode = trueCircleData;
    },
    // 拖拽替换
    adjustmentDataOfVideoWallFromDrag(data) {
      const replaceArr = [...this.dataOfVideoWall];
      const { dragIndex, index } = data.data;
      if (data.mode === 'exchangePosition') {
        [replaceArr[index], replaceArr[dragIndex]] = [replaceArr[dragIndex], replaceArr[index]];
      } else {
        const trueLastIndex = this.idsArrOfTrueDataOfVideoWall.length - 1;

        [replaceArr[index], replaceArr[this.currentLattice - 1]] = [
          replaceArr[this.currentLattice - 1],
          replaceArr[index],
        ];
        [replaceArr[index], replaceArr[trueLastIndex]] = [
          replaceArr[trueLastIndex],
          replaceArr[index],
        ];
      }
      this.dataOfVideoWall = replaceArr;
    },
    adjustmentMapDataWithDrag(data) {
      const len = data.isChangeData ? 1 : 0;
      this.dataOfMapMode.splice(data.index, len, data.receiveDragData);
    },
    clearMonitorDrawerData() {
      this.monitorDrawerData = [];
    },
    deleteDataOfMapMode(deleteId) {
      const index = this.idsArrOfDataOfMapMode.indexOf(deleteId);
      if (index === -1) {
        return;
      }
      this.dataOfMapMode.splice(index, 1);
      if (deleteId === this.cameraIdHightLight) {
        this.cameraIdHightLight = '';
      }
    },
    deleteDataOfVideoWall(data) {
      const index = this.dataOfVideoWall.findIndex((item) => {
        return item.id === data;
      });
      if (index === -1) {
        return;
      }
      this.dataOfVideoWall.splice(index, 1, ...createEmptyItems(1));
    },
    deleteFromCameraFavoritesData(data: string[]) {
      for (let i = data.length - 1; i >= 0; i--) {
        const item = data[i];
        const index = this.cameraFavoritesData.indexOf(item);

        if (index !== -1) {
          this.cameraFavoritesData.splice(index, 1);
        }
      }
      if (this.cameraFavoritesData.length === 0) {
        this.cameraDefaultFavorateId = '';
      }
    },
    /**
     * 通过 account 删除监控弹窗数据
     * @param account
     */
    deleteMonitorDrawerDataByAccount(account) {
      const index = this.monitorDrawerData.findIndex((i) => {
        return i.account === account;
      });
      if (index !== -1) {
        this.monitorDrawerData.splice(index, 1);
      }
    },
    // 视频墙模式
    initDataOfVideoWall() {
      this.dataOfVideoWall = createEmptyItems(this.currentLattice);
    },
    setAllBtnShowType(data) {
      this.allBtnShowType = this.allBtnShowType === data ? '' : data;
    },
    setCameraDefaultFavorateId(data) {
      this.cameraDefaultFavorateId = data;
    },
    setCameraId(data) {
      this.cameraIdHightLight = data;
    },
    setMonitorDrawerData(data) {
      this.monitorDrawerData = data;
    },
    updateCurrentInterface(data) {
      this.currentInterface = data;
    },
    updateCurrentLattice(data) {
      this.currentLattice = Number(data);
    },
    updateDragMonitorData(data) {
      this.monitorDragData = data;
    },
    updatePlayingList(data) {
      if (data.isAdd) {
        this.playingList.push(data);
      } else {
        for (let i = 0; i < this.playingList.length; i++) {
          if (
            this.playingList[i].facilityId === data.facilityId &&
            this.playingList[i].sign === data.sign
          ) {
            this.playingList.splice(i, 1);
            break;
          }
        }
      }
    },
  },

  getters: {
    // 地图模式的id集合
    idsArrOfDataOfMapMode(state): string[] {
      return state.dataOfMapMode.map((item) => {
        return item.id;
      });
    },
    IdsArrOfMonitoringVideoData(): string[] {
      return this.monitoringVideoData.map((item) => {
        return item.id;
      });
    },
    // 视频墙模式的真实数据的id集合
    idsArrOfTrueDataOfVideoWall(): string[] {
      return this.trueDataOfVideoWall.map((item) => {
        return item.id;
      });
    },
    // 所有视频查看的数据集合
    monitoringVideoData(state): MonitorList {
      return [...this.trueDataOfVideoWall, ...state.dataOfMapMode];
    },
    playingFacilityIds(state): string[] {
      return state.playingList.map((item) => {
        return item.facilityId;
      });
    },
    // 视频墙模式的真实数据
    trueDataOfVideoWall(state): MonitorList {
      return state.dataOfVideoWall.filter((item) => {
        return item.type !== 'emptyMonitor';
      });
    },
  },
  id: 'monitor',
  state: (): MonitorState => ({
    // 地图上右边4个按钮的点击后侧边栏显示的状态
    allBtnShowType: '',
    cameraClasses: {}, // 摄像头的根层级，主要用于地图加载摄像头数据用，地图摄像头的图层都为其根层级的id
    cameraDefaultFavorateId: '',
    cameraFavoritesData: [], // 默认收藏夹数据
    cameraIdHightLight: '', // 摄像头点击三者联动高亮显示
    currentInterface: 'map', // 当前地图上的界面
    currentLattice: 4, // 当前的九宫格格式
    dataOfMapMode: [], // 地图上播放视频查看的数据
    dataOfVideoWall: [], // 视频墙播放视频查看的数据
    monitorDragData: {},
    monitorDrawerData: [], // 监控弹窗列表
    playingList: [],
  }),
});

// Need to be used outside the setup
export function useMonitorStoreWithOut() {
  return useMonitorStore(store);
}
