import { Message } from '@/components/Message';
import { useI18n, useSetInterval, useUtils } from '@/hooks';
import { videoPollPlay } from '@/pages/resource/videoHelper';
import { store, useVehicleStore } from '@/store';

import { defineStore } from 'pinia';

import { useCommunicateDispatchStore } from './communicateDispatch';

export type VideoPollItem = {
  [key: string]: any;
  id: string;
  type: string;
};

interface VideoPollState {
  clearPollTimer: any;
  pausePlayPoll: boolean;
  playingPoll: any;
  pollTime: number;
  pollTimes: number;
  remainingTime: number;
  videoList: any[];
  videoPollDrawerData: any[];
}

let currentPlayIndex = 0;
let currentPlayTimes = 0;

export const useVideoPollStore = defineStore({
  actions: {
    // 轮巡列表添加视频
    addVideoPollDrawerData(data) {
      const { t } = useI18n();
      if (this.videoPollDrawerData.length >= 16) {
        Message(t('monitor.monitorTips.maxLengthGoDesktop'));
        return;
      }
      const index = this.videoPollDrawerData.findIndex((i) => {
        return i.id === data.id;
      });
      if (index === -1) {
        this.videoPollDrawerData.push(data);
      }
    },

    // 清空轮巡列表
    clearDrawerData() {
      this.videoList = [];
      this.videoPollDrawerData = [];

      const { clearMonitor } = useCommunicateDispatchStore();
      clearMonitor();
    },

    // 清除轮巡
    clearVideoPollTimer() {
      currentPlayIndex = 0;
      currentPlayTimes = 0;
      this.pausePlayPoll = false;
      this.clearPollTimer?.();
      this.clearPollTimer = null;
      this.clearDrawerData();
    },

    /**
     * 播放轮巡
     * @param ahead 提前预播放(视频拉流慢，长时间没有画面)
     * @returns
     */
    async polling(ahead?: boolean) {
      const { isCommPanel } = useUtils();
      const { addMonitorList, clearMonitor, monitorDesktopList, setCurActiveDeskTop } =
        useCommunicateDispatchStore();

      let curPlayCount = 0;
      curPlayCount = isCommPanel
        ? monitorDesktopList.filter((i) => !!i).length
        : this.videoPollDrawerData.length;

      const { pollTimes } = this;
      if (pollTimes && currentPlayTimes >= pollTimes) {
        if (ahead) {
          return;
        }
        if (!ahead && curPlayCount <= 4) {
          Message('轮巡已结束！');
          this.clearVideoPollTimer();
          this.clearDrawerData();
          clearMonitor();
          return;
        }
      }

      if (isCommPanel) {
        setCurActiveDeskTop('MonitorDesktop');
        if (!ahead) {
          const arr: any[] = [];
          monitorDesktopList.forEach((item) => {
            if (item?.ahead) {
              arr.push({ ...item, ahead: false });
            }
          });
          addMonitorList(arr);
        }
      } else {
        if (!ahead) {
          const arr: any[] = [];
          this.videoPollDrawerData.forEach((item) => {
            if (item.ahead) {
              arr.push({ ...item, ahead: false });
            }
          });
          this.videoPollDrawerData = arr;
        }
      }

      // 大于4的情况是已经有预播放
      if (curPlayCount > 4) {
        return;
      }

      const play = (item) => {
        Object.assign(item, { ahead });
        isCommPanel ? addMonitorList(item) : videoPollPlay(item);
      };

      // 保障中头车和轮巡列表重复则轮巡列表不播放，并向后一位
      const { headerCar, underProtection } = useVehicleStore();
      const arr = this.videoList.slice(currentPlayIndex, (currentPlayIndex += 4));
      arr.forEach((item, index) => {
        if (underProtection && item.id === headerCar?.equipmentId) {
          const after = this.videoList[currentPlayIndex++];
          if (after) {
            setTimeout(() => {
              play(after);
            }, 0);
          }
          return;
        }

        // 轮巡列表数量小于等于4不再重复播放
        if (this.videoList.length <= 4 && currentPlayTimes > 0) {
          play(item);
        } else {
          // 视频不能同时拉流，否则可能出现部分没有画面的情况
          setTimeout(() => {
            play(item);
          }, 500 * index);
        }
      });

      if (currentPlayIndex >= this.videoList.length) {
        currentPlayIndex = 0;
        currentPlayTimes++;
      }
    },

    // 重新播放轮巡
    restartPoll() {
      const pollTimes = this.pollTimes;
      const pollTime = this.pollTime;
      const videoList = this.videoList;

      this.clearVideoPollTimer();
      this.startPolling(pollTimes, pollTime, videoList);
    },

    // 暂停轮巡
    setPausePlayPoll(data) {
      this.pausePlayPoll = data;
    },

    // 设置当前播放的轮巡组信息
    setPlayingPoll(data) {
      this.playingPoll = data;
    },

    // 设置轮巡剩余时间
    setRemainingTime(data) {
      if (!this.pausePlayPoll) {
        this.remainingTime = data;
      }
    },

    /**
     * 开始轮巡
     * @param times 次数
     * @param time 时长
     * @param videoList 视频列表
     */
    startPolling(times: number, time: number, videoList: any[]) {
      this.clearPollTimer?.();

      this.pollTimes = times; // 轮巡次数
      this.pollTime = time; // 定时器时间
      this.videoList = videoList;

      this.setRemainingTime(time);

      this.polling();
      this.clearPollTimer = useSetInterval(() => {
        const { remainingTime } = this;
        if (remainingTime !== 0) {
          // 提前5s预先播放
          if (remainingTime === 5 && videoList.length > 4) {
            this.polling(true);
          }
          this.setRemainingTime(remainingTime - 1);
          return;
        }
        this.setRemainingTime(time);
        this.polling();
      }, 1 * 1000);
    },

    // 更新轮巡列表
    updateVideoList(data: any[]) {
      this.clearDrawerData();
      this.startPolling(this.pollTimes, this.pollTime, data);
    },
  },
  id: 'videoPoll',
  state: (): VideoPollState => ({
    clearPollTimer: null, // 轮巡定时器
    pausePlayPoll: false, // 暂停轮巡
    playingPoll: {}, // 当前播放的轮巡组
    pollTime: 0, // 定时器时间
    pollTimes: 0, // 轮巡次数
    remainingTime: 0, // 轮巡剩余的时间
    videoList: [], // 轮巡视频列表（所有）
    videoPollDrawerData: [], // 轮巡弹窗列表
  }),
});

// Need to be used outside the setup
export function useVideoPollStoreWithOut() {
  return useVideoPollStore(store);
}
