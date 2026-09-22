import { store } from '@/store';

import { defineStore } from 'pinia';

interface landmarkState {
  lastLandmarkData: any[];
}

export const useLandmarkStore = defineStore({
  actions: {
    // 保存组织的地标单位
    setLastLandmarkData(data) {
      this.lastLandmarkData = data;
    },
  },
  id: 'Landmark',
  state: (): landmarkState => ({
    lastLandmarkData: [],
  }),
});

// Need to be used outside the setup
export function useLandmarkStoreWithOut() {
  return useLandmarkStore(store);
}
