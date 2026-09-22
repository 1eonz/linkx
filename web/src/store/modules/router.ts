import { getGlobalsList } from '@/api/dictionary';
import { getMenuList, getRolePermissions } from '@/api/login';
import { appConfig } from '@/config';
import { useI18n, usePermissions } from '@/hooks';
import { store } from '@/store';

import { defineStore } from 'pinia';

type MenuItem = {
  children: object[];
  id: string;
  name: string;
  url: string;
};

type RouterState = {
  menuList: MenuItem[];
};

export const useRouterStore = defineStore({
  actions: {
    async getGlobalsOptions() {
      const { t } = useI18n();
      const { code, data } = await getGlobalsList();
      if (code === 0) {
        const { STATION_NAME } = data || {};
        const title = STATION_NAME || t('login.title');
        document.title = title;
        appConfig.settingData = data;
      }
    },
    async getMenu() {
      const { code, data } = await getRolePermissions();
      // type为0表示超级管理员
      if (code !== 0 || data.type === 0) {
        return false;
      }
      const menuPermissions = data.menus;
      {
        const { code, data } = await getMenuList({ applicationId: '' });
        if (code !== 0) return false;
        const menuList = data.filter(({ id, url }) => {
          const permissions = menuPermissions.includes(id);
          if (!permissions) {
            return false;
          }
          const iseBc = usePermissions('eBC');
          switch (url) {
            case 'communicationCenter': {
              return iseBc && usePermissions('DISPATCH');
            }
            case 'dashboard': {
              return iseBc && usePermissions('HOME');
            }
            case 'homeScreen': {
              return iseBc && usePermissions('HOME');
            }
            case 'mapCommand': {
              return usePermissions('DISPATCH');
            }
            case 'planSafety': {
              return iseBc && usePermissions('SAFETY');
            }
            case 'policeAdmin': {
              return usePermissions('ADMIN');
            }
            case 'policeTask': {
              return (
                usePermissions('CONTROL') || usePermissions('ALARM') || usePermissions('MISSION')
              );
            }
            case 'region': {
              return usePermissions('REGION');
            }
            default: {
              return permissions;
            }
          }
        });
        this.menuList = menuList;
      }
      return true;
    },
  },
  id: 'router',
  state: (): RouterState => ({
    menuList: [],
  }),
});

// Need to be used outside the setup
export function useRouterStoreWithOut() {
  return useRouterStore(store);
}
