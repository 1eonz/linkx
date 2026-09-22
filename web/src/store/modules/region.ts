import { store } from '@/store';

import { defineStore } from 'pinia';

interface RegionState {
  regionData: any[];
}

export const useRegionStore = defineStore({
  actions: {
    setRegionData(data: any) {
      this.regionData = data;
    },
  },
  id: 'region',
  state: (): RegionState => ({
    regionData: [],
  }),
});

// Need to be used outside the setup
export function useRegionStoreWithOut() {
  return useRegionStore(store);
}
