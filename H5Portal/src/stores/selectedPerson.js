import { defineStore } from 'pinia';
import { showFailToast } from 'vant';

export const useSelectedPersons = defineStore('selectedPersons', {
  state: () => ({
    list: [], // 已选中的人员
    selectedTags: [], // 选中的标签列表
  }),

  getters: {
    // 获取选中人员列表
    getSelectedPersons: (state) => state.list,

    // 选中数量
    selectedCount: (state) => state.list.length,

    // 选中人员id
    getSelectedPersonsId: (state) => state.list.map((item) => item.id),
  },

  actions: {
    // 批量移除人员（普通选中的）
    deletePersons(personIds) {
      if (!personIds || personIds.length === 0) return;
      this.list = this.list.filter((item) => !personIds.includes(item.id));
    },

    // 删除用户（包括普通选中的）
    removeUser(userId) {
      // 从普通选中列表删除
      const listIndex = this.list.findIndex((item) => item.id === userId);
      if (listIndex > -1) {
        this.list.splice(listIndex, 1);
      }
    },

    // 批量删除用户
    batchRemoveUsers(userIds) {
      if (!userIds || userIds.length === 0) return;
      userIds.forEach((userId) => {
        this.removeUser(userId);
      });
    },
    // 切换选中状态
    changeSelectedPersons(data) {
      const index = this.list.findIndex((item) => item.id === data.id);
      if (index > -1) {
        this.list.splice(index, 1);
      } else {
        if (this.list.length >= 999) {
          showFailToast('最多可选999人');
          return;
        }
        this.list.push(data);
      }
    },
    // 批量添加
    batchSelectItems(dataList) {
      dataList.forEach((item) => {
        const index = this.list.findIndex((existingItem) => existingItem.id === item.id);
        if (index === -1) {
          this.list.push(item);
        }
      });
    },
    // 批量移除
    batchRemoveItems(dataList) {
      const removeIds = new Set(dataList.map((item) => item.id));
      this.list = this.list.filter((item) => !removeIds.has(item.id));
    },
    // 清空所有选中人员
    clearSelectedPersons() {
      this.list = [];
    },

    // 切换标签选中状态
    toggleTag(tag) {
      const tagId = tag.id;
      const index = this.selectedTags.findIndex((t) => t.id === tagId);
      if (index > -1) {
        this.selectedTags.splice(index, 1);
        return false; // 返回 false 表示取消选中
      } else {
        this.selectedTags.push(tag);
        return true; // 返回 true 表示选中
      }
    },

    // 获取选中的标签ID列表
    getSelectedTagIds() {
      return this.selectedTags.map((tag) => tag.id);
    },

    // 清空标签相关状态
    clearTags() {
      this.selectedTags = [];
    },

    // 清空所有（包括人员和标签）
    clearAll() {
      this.list = [];
      this.clearTags();
    },
  },
});
