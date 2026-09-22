import { store } from '@/store';

import { defineStore } from 'pinia';

interface MainState {
  userInfo?: any; // User information
}

export const useMainStore = defineStore({
  actions: {},
  id: 'main',
  state: (): MainState => ({}),
});

// Need to be used outside the setup
export function useMainStoreWithOut() {
  return useMainStore(store);
}
