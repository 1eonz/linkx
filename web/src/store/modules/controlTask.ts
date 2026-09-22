import { queryVideoControlTaskList } from '@/api/videoControl';
import { appConfig } from '@/config';
import { store } from '@/store';

import { defineStore } from 'pinia';

type ControlTaskData = {
  [propName: number]: {
    data: any[];
    total: number;
  };
};

interface WarningState {
  controlTaskData: ControlTaskData;
}

const typeList = [1, 2, 999];
const pageSize = 20;

export const useControlTaskStore = defineStore({
  actions: {
    // 改变状态
    changeEnable(data) {
      // 从之前列表删除
      const oldEnable = data.enable === 1 ? 2 : 1;
      const index = this.controlTaskData[oldEnable].data.findIndex((i) => {
        return i.suspectTaskId === data.suspectTaskId;
      });
      if (index !== -1) {
        this.controlTaskData[oldEnable].data.splice(index, 1);
        this.controlTaskData[oldEnable].total--;
      }

      // 插入到新列表
      this.controlTaskData[data.enable].data.unshift(data);
      this.controlTaskData[data.enable].total++;

      // 修改全部列表状态
      this.controlTaskData[999].data.forEach((i) => {
        if (i.suspectTaskId === data.suspectTaskId) {
          i.enable = data.enable;
        }
      });
    },

    // 创建
    createControlTask(data) {
      if (this.controlTaskData[data.enable]) {
        this.controlTaskData[data.enable].data.unshift(data);
        this.controlTaskData[data.enable].total++;
      }
      this.controlTaskData[999].data.unshift(data);
      this.controlTaskData[999].total++;
    },

    // 删除/结束
    deleteControlTask(id, data?) {
      typeList.forEach((type) => {
        const index = this.controlTaskData[type].data.findIndex((i) => {
          return i.suspectTaskId === id;
        });
        if (index !== -1) {
          // 结束
          if (data && type === 999) {
            this.controlTaskData[type].data.splice(index, 1, data);
          } else {
            this.controlTaskData[type].data.splice(index, 1);
            this.controlTaskData[type].total--;
          }
        }
      });
    },

    // 初始化设防列表
    async initControlTask() {
      // 启用状态 enable=0 未开始 enable=1 已启用（设防中） enable=2 已停用（暂停） enable=3 已停止（结束） enable=4 删除
      for (const enable of typeList) {
        const param: any = {
          commandCenterId: appConfig.userData.belongOrgId,
          pageNo: 1,
          pageSize,
        };
        if (enable !== 999) {
          param.enable = enable;
        }
        const { code, data } = await queryVideoControlTaskList(param);
        if (code === 0) {
          this.controlTaskData[enable].data = data.results;
          this.controlTaskData[enable].total = Number(data.total);
        }
      }
    },

    // 加载更多
    async loadMoreControlTask(enable) {
      const { data, total } = this.controlTaskData[enable];
      if (data.length < total) {
        const param: any = {
          commandCenterId: appConfig.userData.belongOrgId,
          pageNo: Number.parseInt(`${data.length / pageSize}`) + 1,
          pageSize,
        };
        if (enable !== 999) {
          param.enable = enable;
        }
        const res = await queryVideoControlTaskList(param);
        if (res.code === 0) {
          res.data.results.forEach((item) => {
            const index = this.controlTaskData[enable].data.findIndex((i) => {
              return i.suspectTaskId === item.suspectTaskId;
            });
            if (index === -1) {
              this.controlTaskData[enable].data.push(item);
            }
          });
          this.controlTaskData[enable].total = Number(res.data.total);
        }
      }
    },

    // 修改
    updateControlTask(data) {
      typeList.forEach((type) => {
        const index = this.controlTaskData[type].data.findIndex((i) => {
          return i.suspectTaskId === data.suspectTaskId;
        });
        if (index !== -1) {
          this.controlTaskData[type].data.splice(index, 1);
          this.controlTaskData[type].total--;
        }
      });

      const enable = '12'.includes(data.enable) ? Number(data.enable) : 999;
      this.controlTaskData[enable].data.unshift(data);
      this.controlTaskData[enable].total++;
    },
  },
  id: 'controlTask',
  state: (): WarningState => ({
    controlTaskData: {
      1: {
        data: [],
        total: 0,
      },
      2: {
        data: [],
        total: 0,
      },
      999: {
        data: [],
        total: 0,
      },
    },
  }),
});

// Need to be used outside the setup
export function useControlTaskStoreWithOut() {
  return useControlTaskStore(store);
}
