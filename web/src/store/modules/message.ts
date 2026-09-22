import { queryMessageAlertDetails, queryMessageAlertList } from '@/api/message';
import { store } from '@/store';

import { defineStore } from 'pinia';

interface MessageState {
  messageDetails: any;
  messagesData: any;
}

export const useMessageStore = defineStore({
  actions: {
    // 创建紧急短信
    addMessageItem(data) {
      this.messagesData.unshift(data);
    },

    // 删除紧急短信
    deleteMessageItem(id) {
      const index = this.messagesData.findIndex((i) => i.id === id);
      if (index !== -1) {
        this.messagesData.splice(index, 1);
      }
    },

    async queryMessageAlertDetails(id) {
      const { code, data } = await queryMessageAlertDetails({ id });
      if (code === 0 && data) {
        this.messageDetails = data;
      }
    },

    async queryMessageData() {
      const param = {
        pageNo: 1,
        pageSize: 1000,
      };
      const arr: any[] = [];

      const query = async () => {
        const { code, data } = await queryMessageAlertList(param);
        if (code === 0) {
          data.records?.forEach((item) => {
            const { lat, lon } = item;
            if (lon && lat) {
              item.position = [lon, lat];
            }
            arr.push(item);
          });

          if (arr.length < Number(data.total)) {
            param.pageNo++;
            await query();
          }
        }
      };

      await query();

      this.messagesData = arr;
    },

    setMessageDetails(data?) {
      this.messageDetails = data;
    },

    // 更新紧急短信
    updateMessageItem(data) {
      const index = this.messagesData.findIndex((i) => i.id === data.id);
      if (index !== -1) {
        this.messagesData.splice(index, 1);
      }
      this.messagesData.unshift(data);
    },
  },
  getters: {
    getMessages() {
      const ret: any = {
        0: [],
        3: [],
      };
      this.messagesData.forEach((item) => {
        if (item.status === 0) {
          ret[0].push(item);
        } else {
          ret[3].push(item);
        }
      });
      return ret;
    },
  },
  id: 'message',
  state: (): MessageState => ({
    messageDetails: {},
    messagesData: [], // 0待处理 3已处理（包括忽略）
  }),
});

// Need to be used outside the setup
export function useMessageStoreWithOut() {
  return useMessageStore(store);
}
