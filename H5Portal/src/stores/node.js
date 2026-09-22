import { defineStore } from 'pinia';

import { getNodeList } from '@/common/api/node.js';
import { getCurrentNodeFromLocation } from '@/utils/nodeCache.js';
import { useUserStore } from '@/stores/user.js';

export const useNodeStore = defineStore('node', {
  state: () => ({
    nodeList: [],
    currentNode: null,
    loading: false,
  }),
  actions: {
    // 获取节点列表
    async fetchNodeList() {
      this.loading = true;
      try {
        const userStore = useUserStore();
        let userInfo = await userStore.getStoreUserInfo();
        const userId = userInfo?.userId;
        if (!userId) {
          console.warn('获取节点列表失败: userId 为空', userInfo);
          this.nodeList = [];
          return;
        }
        this.nodeList = await getNodeList(userId);
      } finally {
        this.loading = false;
      }
    },
    // 初始化当前节点信息（从 location 推导）
    initCurrentNode() {
      this.currentNode = getCurrentNodeFromLocation();
    },
  },
});
