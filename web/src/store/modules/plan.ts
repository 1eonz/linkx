import { queryPlanGroupById, querySupportGroup, updatePlanGroup } from '@/api/plan';
import { Message } from '@/components/Message';
import { store } from '@/store';

import { defineStore } from 'pinia';

interface PlanState {
  chooseSourcesList: any[];
  planData: any;
  supportGroup: any[];
}

export const usePlanStore = defineStore({
  actions: {
    // 添加选中资源
    addChooseSourcesList(data, cancelCheckKeys?) {
      let arr: any = [];
      if (cancelCheckKeys) {
        const before = this.chooseSourcesList.filter((item) => !cancelCheckKeys.includes(item.id));
        arr = [...data, ...before];
      } else {
        arr = data;
      }
      this.chooseSourcesList = [...new Set(arr)];
    },

    // 删除选中的资源
    delChooseSourcesList(data?) {
      if (data) {
        const { id } = data;
        this.chooseSourcesList = this.chooseSourcesList.filter((item) => item.id !== id);
      } else {
        this.chooseSourcesList = [];
      }
    },

    initChooseData() {
      this.chooseSourcesList = [];
    },

    // 查询专项保障组详情
    async queryPlanData(id) {
      const { code, data } = await queryPlanGroupById({ id });
      if (code === 0) {
        this.planData = data;
        this.querySupportGroupData();
        return true;
      }
    },

    // 查询专项保障组成员
    async querySupportGroupData() {
      const { id } = this.planData;
      const { code, data } = await querySupportGroup({ id });
      if (code === 0) {
        this.supportGroup = data;
      }
    },
    // 修改转专项保障组成员
    async updatePlanData(group) {
      const param: any = {
        ...this.planData,
        supportGroup: JSON.stringify(group),
      };
      const { code, data } = await updatePlanGroup(param);
      if (code === 0) {
        Message({
          message: data.msg,
          type: 'success',
        });
        this.querySupportGroupData();
        return true;
      } else {
        Message({
          message: data.msg,
          type: 'warning',
        });
      }
    },
    // 修改专项保障视频轮巡
    async updatePlanDataVideo(videoPollingGroup) {
      const param: any = {
        ...this.planData,
      };
      param.videoPollingGroup = param.videoPollingGroup
        ? `${param.videoPollingGroup},${videoPollingGroup}`
        : videoPollingGroup;
      const { id } = this.planData;
      const { code, data } = await updatePlanGroup(param);
      if (code === 0) {
        Message({
          message: data.msg,
          type: 'success',
        });
        await this.queryPlanData(id);
        return true;
      } else {
        Message({
          message: data.msg,
          type: 'warning',
        });
      }
    },
  },
  getters: {
    planEquipmentData() {
      const ret: any = [];
      const filter = (data) => {
        data.forEach((item) => {
          if (item.category) {
            ret.push(item);
          } else if (item.children) {
            filter(item.children);
          }
        });
      };
      filter(this.supportGroup);
      return ret;
    },
  },
  id: 'plan',
  state: (): PlanState => ({
    chooseSourcesList: [],
    planData: {},
    supportGroup: [],
  }),
});

// Need to be used  outside the setup
export function usePlanStoreWithOut() {
  return usePlanStore(store);
}
