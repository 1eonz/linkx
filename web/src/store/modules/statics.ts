import { store } from '@/store';

import { defineStore } from 'pinia';

interface StaticsState {
  answerTotal: number; // 逾期回复总数
  dateRang: any;
  departmentCode: string;
  peerId: string; // 当前节点 ID，空字符串表示"当前节点"
  zeroUserOnlineTotal: number; // 无人员在岗协同岗个数
}

export const useStaticsState = defineStore({
  actions: {
    setAnswerTotal(num) {
      this.answerTotal = num;
    },
    setDateRang(data) {
      this.dateRang = data;
    },
    setDepartmentCode(code) {
      this.departmentCode = code;
    },
    setPeerId(peerId: string | null) {
      this.peerId = peerId || '';
    },
    setZeroUserOnlineTotal(num) {
      this.zeroUserOnlineTotal = num;
    },
  },
  id: 'map',
  state: (): StaticsState => ({
    answerTotal: 0,
    dateRang: { endTime: '', startTime: '' },
    departmentCode: '',
    peerId: '',
    zeroUserOnlineTotal: 0,
  }),
});

// Need to be used outside the setup
export function useStaticsStoreWithOut() {
  return useStaticsState(store);
}
