import { usePermissions } from '@/hooks';
import { store } from '@/store';
import { isArray } from '@/utils/is';

import { cloneDeep } from 'lodash-es';
import { defineStore } from 'pinia';

interface CommunicateDispatchState {
  closeConferFlag: number;
  curActiveDesktop: string;
  groupMessageDetail: any[];
  layoutMonitor: any;
  monitorCenterFull: boolean;
  monitorDesktopList: any[];
  monitorDesktopListCopy: any[];
  monitorPixel: string;
  oldDynamicGroup: any[];
  operateClick: boolean;
  operateShowTab: string;
  projectionCenter: boolean;
  selectedCommList: any[];
  windowOpen: any;
}

export const useCommunicateDispatchStore = defineStore({
  actions: {
    /**
     * 视频拖拽播放列表
     * @param data array直接替换列表，item新增或者插入
     * @param index
     */
    async addMonitorList(data, index?) {
      let arr = [...this.monitorDesktopList];
      if (isArray(data)) {
        arr = [...data];
      } else {
        if (index === undefined) {
          const index = arr.indexOf(null);
          if (index !== -1) {
            arr.splice(index, 1, data);
          }
        } else {
          arr.splice(index, 1, data);
        }
      }
      this.monitorDesktopList = [...arr, ...Array.from({ length: 16 - arr.length }).fill(null)];
    },
    clearMonitor() {
      this.monitorDesktopList = Array.from({ length: 16 }).fill(null);
    },
    // 删除通信桌面群组
    deleteCommList(data) {
      const arr = [...this.selectedCommList];
      const index = arr.findIndex((item) => {
        return item?.groupId === data.groupId;
      });
      if (index !== -1) {
        arr.splice(index, 1, null);
        this.setSelectedCommList(arr);
      }
    },
    // 删除视频播放列表项
    deleteMonitor(data) {
      const arr = [...this.monitorDesktopList];
      const index = arr.findIndex((item) => item?.id === data.id);
      if (index !== -1) {
        arr.splice(index, 1, null);
        this.monitorDesktopList = arr;
      }
    },
    initLayoutMonitor() {
      const data = localStorage.getItem('layoutMonitor');
      if (data) {
        this.layoutMonitor = JSON.parse(data);
      }
    },
    initOldDynamicGroup(data) {
      this.oldDynamicGroup = data;
    },
    // 初始化通讯屏数据
    initSelectedCommList() {
      const data = localStorage.getItem('selectedCommList');
      if (data) {
        this.selectedCommList = JSON.parse(data);
      }
    },
    // 重置视频列表，将有视频的放前面
    resetMonitorList() {
      const arr = this.monitorDesktopList.filter(Boolean);
      this.monitorDesktopList = [...arr, ...Array.from({ length: 16 - arr.length }).fill(null)];
    },
    setCloseConferFlag() {
      this.closeConferFlag = Date.now();
    },
    setCurActiveDeskTop(data) {
      this.curActiveDesktop = data;
    },
    setGroupMessage(groupMessage) {
      this.groupMessageDetail = groupMessage;
    },
    // 修改监控桌面布局
    setLayoutMonitor(layoutMonitor) {
      this.layoutMonitor = layoutMonitor;
      localStorage.setItem('layoutMonitor', JSON.stringify(layoutMonitor));
    },
    setMonitorCenterFull(flag) {
      this.monitorCenterFull = flag;
    },
    // 复制视频监控列表
    setMonitorDesktopListCopy() {
      this.monitorDesktopListCopy = cloneDeep(this.monitorDesktopList);
    },
    // 修改视频监控清晰度
    setMonitorPixel(data) {
      this.monitorPixel = data;
    },
    setOperateClick(operateClick) {
      this.operateClick = operateClick;
    },
    // 设置正在投屏中状态
    async setProjectionCenter(data) {
      if (this.projectionCenter && !data) {
        this.addMonitorList(this.monitorDesktopListCopy);
      }
      this.projectionCenter = data;
    },
    // 修改通讯屏群组数据
    setSelectedCommList(arr) {
      this.selectedCommList = [...arr];
      localStorage.setItem('selectedCommList', JSON.stringify(arr));
    },
    setWindowOpen(windowOpen) {
      this.windowOpen = windowOpen;
    },
  },
  id: 'communicateDispatch',
  state: (): CommunicateDispatchState => ({
    closeConferFlag: 0, // 一键关闭时，是否关闭会议
    curActiveDesktop: usePermissions('CONFERENCE') ? 'SynthesizeDesktop' : 'MonitorDesktop', // 监控屏当前选中桌面
    groupMessageDetail: [], // 是否发送群组短信
    layoutMonitor: {
      iconName: 'layout_3',
      id: 6,
      span: 8,
    }, // 监控桌面布局
    monitorCenterFull: false,
    monitorDesktopList: Array.from({ length: 16 }).fill(null), // 监控屏摄像头列表
    monitorDesktopListCopy: [],
    monitorPixel: '', // 视频播放清晰度
    oldDynamicGroup: [],
    operateClick: true, // 区别是操作台点击消息弹框还是群组弹框点击的
    operateShowTab: 'group', // 语音桌面显示tab页，左侧列表点击收藏之后切换到对应得tab页
    projectionCenter: false, // 是否正在投屏中
    selectedCommList: [], // 通信屏添加的群组列表
    windowOpen: {},
  }),
});

// Need to be used outside the setup
export function useCommunicateDispatchStoreWithOut() {
  return useCommunicateDispatchStore(store);
}
