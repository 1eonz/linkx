import { store } from '@/store';

import { defineStore } from 'pinia';

interface MapState {
  mapStyle: string;
  showIconInfo: boolean;
}

export const useMapStore = defineStore({
  actions: {
    getMapStyle() {
      return this.mapStyle;
    },
    setMapStyle(data) {
      this.mapStyle = data;
    },
    setShowIconInfo(data) {
      this.showIconInfo = data;
    },
  },
  id: 'map',
  state: (): MapState => ({
    mapStyle: 'dark',
    showIconInfo: false,
  }),
});

// Need to be used outside the setup
export function useMapStoreWithOut() {
  return useMapStore(store);
}
