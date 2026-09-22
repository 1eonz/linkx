import { defineStore } from 'pinia';

export const useArchiveStore = defineStore('archive', {
  actions: {
    setArchivedRefreshFlag(flag) {
      this.archivedRefreshFlag = flag;
    },

    triggerRefresh() {
      this.refreshCount++;
    },

    triggerRefreshArchived() {
      this.refreshArchivedCount++;
    },

    triggerRefreshRelated() {
      this.refreshRelatedCount++;
    },
  },

  state: () => ({
    archivedRefreshFlag: false,
    refreshArchivedCount: 0,
    refreshCount: 0,
    refreshRelatedCount: 0,
  }),
});
