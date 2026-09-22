import { queryOrderListByPage } from '@/api/order';
import { store } from '@/store';

import { defineStore } from 'pinia';

interface OrderData {
  [propName: number]: {
    data: any[];
    size: number;
    total: number;
  };
}
interface OrderState {
  currentPageNo: number;
  orderData: OrderData;
}

const pageSize = 20;

export const useOrderStore = defineStore({
  actions: {
    // 加载更多
    loadMoreOrderData(status) {
      const { size, total } = this.orderData[status];

      if (size >= total) return;

      this.orderData[status].size = size;
      Object.assign(this.orderData[status], { size });
      this.queryOrderData();
    },
    // 初始化预警通知数据
    async queryOrderData(pageNo?) {
      const orderStatus = [
        { id: 0, status: [0] },
        { id: 1, status: [1, 2] },
        { id: 3, status: [3, 4] },
      ];
      const { currentPageNo } = this;
      orderStatus.forEach(async (item) => {
        const { id, status } = item;
        const param = {
          pageNum: pageNo || currentPageNo,
          pageSize: 20,
          status,
        };
        const { code, data } = await queryOrderListByPage(param);
        if (code === 0 && data) {
          const { records, total } = data;
          if (records.length === 0 && currentPageNo !== 1) return;

          let recordsData: any = [];
          if (currentPageNo === 1) {
            // 在第一页 ，都改变
            recordsData = records;
          } else {
            // 不在第一页

            if (pageNo === 1) {
              // 定时器只改变总数
              recordsData = this.orderData[id].data;
            } else {
              // 触底加载
              recordsData = [...this.orderData[id].data, ...records];
            }
          }
          this.setOrderData({ data: recordsData, id, total });
        }
      });
    },
    setCurrentPageNo(pageNo) {
      this.currentPageNo = pageNo;
    },
    setOrderData({ data, id, total }) {
      Object.assign(this.orderData[id], {
        data,
        total: Number(total),
      });
    },
  },
  id: 'order',
  state: (): OrderState => ({
    currentPageNo: 1,
    orderData: {
      0: {
        // 待处理
        data: [],
        size: pageSize,
        total: 0,
      },
      1: {
        // 进行中
        data: [],
        size: pageSize,
        total: 0,
      },
      3: {
        // 已处理
        data: [],
        size: pageSize,
        total: 0,
      },
    },
  }),
});

// Need to be used outside the setup
export function useOrderStoreWithOut() {
  return useOrderStore(store);
}
