import { queryVideoControlWarningList } from '@/api/videoControl';
import { store } from '@/store';

import { defineStore } from 'pinia';

interface AlarmState {
  alarmData: any[];
}

export const useAlarmStore = defineStore({
  actions: {
    // 创建预警
    addAlarmItem(data) {
      this.updateAlarmItem(data);
    },

    // 删除预警
    deleteAlarmItem(alarmId) {
      const index = this.alarmData.findIndex((i) => i.alarmId === alarmId);
      if (index !== -1) {
        this.alarmData.splice(index, 1);
      }
    },

    // 初始化预警通知数据
    async queryAlarmData() {
      const param = {
        pageNo: 1,
        pageSize: 1000,
      };

      const arr: any = [];
      const query = async () => {
        const { code, data } = await queryVideoControlWarningList(param);
        if (code === 0) {
          const { results, total } = data;
          arr.push(...results.map((i) => ({ ...i, id: i.alarmId })));
          if (arr.length < Number(total)) {
            param.pageNo++;
            await query();
          }
        }
      };
      await query();
      this.alarmData = arr;
    },

    // 更新预警
    updateAlarmItem(data) {
      const index = this.alarmData.findIndex((i) => i.alarmId === data.alarmId);
      if (index !== -1) {
        this.alarmData.splice(index, 1);
      }
      this.alarmData.unshift(data);
    },
  },
  getters: {
    getAlarmData() {
      const ret: any = {
        0: [],
        1: [],
      };

      this.alarmData.forEach((item) => {
        if (item.status === 0) {
          ret[0].push(item);
        } else {
          ret[1].push(item);
        }
      });
      return ret;
    },
  },
  id: 'alarm',
  state: (): AlarmState => ({
    alarmData: [],
  }),
});

// Need to be used outside the setup
export function useAlarmStoreWithOut() {
  return useAlarmStore(store);
}
