import { collectAddress, deleteAddress, selectAddress, updateAddress } from '@/api/address';
import { Message } from '@/components/Message';
import { appConfig } from '@/config';
import { useI18n } from '@/hooks';
import { store } from '@/store';

import { defineStore } from 'pinia';

interface AddressState {
  addressData: any[];
  addressType: any[];
  keywords: any;
}

const { t } = useI18n();

export const useAddressStore = defineStore({
  actions: {
    // 新增收藏
    async addAddressData(params) {
      const res = await collectAddress(params);
      if (res.code === 0) {
        Message(t('resource.favorite.succeedAdd'));
        this.initAddressData();
      } else {
        Message({ message: t('resource.favorite.failedAdd'), type: 'warning' });
        return false;
      }
      return true;
    },

    // 取消收藏
    async delAddressData(id) {
      const { code } = await deleteAddress({ id });
      if (code === 0) {
        Message(t('resource.favorite.succeedDel'));
        this.initAddressData();
      } else {
        Message({ message: t('resource.favorite.failedDel'), type: 'warning' });
        return false;
      }
      return true;
    },

    // 初始化收藏
    async initAddressData() {
      const params = {
        executorId: appConfig.resourceId,
      };
      this.queryAddressData(params);
    },

    // 查询收藏地址列表
    async queryAddressData(params) {
      const { code, data } = await selectAddress(params);
      if (code === 0) {
        this.setAddressData(data);
      }
    },

    // 更新收藏地址
    setAddressData(data) {
      this.addressData = data;
    },

    // 保存地点类型
    setAddressType(data) {
      this.addressType = data;
    },

    // 保存关键词
    setKeywords(str) {
      this.keywords = str;
    },

    // 修改单个收藏地址
    async updateAddressData(params) {
      const { code } = await updateAddress(params);
      if (code === 0) {
        Message(t('resource.contact.successChanged'));
        this.initAddressData();
      } else {
        Message({ message: t('monitor.createMonitorGroup.modifyFail'), type: 'warning' });
        return false;
      }
      return true;
    },
  },
  id: 'address',
  state: (): AddressState => ({
    addressData: [],
    addressType: [],
    keywords: '',
  }),
});

// Need to be used outside the setup
export function useAddressStoreWithOut() {
  return useAddressStore(store);
}
