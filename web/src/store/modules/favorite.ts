import { addFavorite, delFavorite, listFavorite } from '@/api/favorite';
import { Message } from '@/components/Message';
import { CategoryEnum } from '@/enums';
import { useBaseData, useI18n, useSetInterval } from '@/hooks';
import { getOnlineStatus, resourceCategory } from '@/pages/resource/resourceHelper';
import { store } from '@/store';

import { defineStore } from 'pinia';

interface FavoriteState {
  favoriteResources: {
    [key: number]: any[];
  };
}

const { t } = useI18n();

function getFavoriteResources() {
  const ret: FavoriteState['favoriteResources'] = {};
  resourceCategory.forEach((i) => {
    ret[i] = [];
  });
  return ret;
}

export const useFavoriteStore = defineStore({
  actions: {
    // 新增收藏
    async addFavoriteResources(info) {
      const { category, code, id, name } = info;
      const categoryStr = (category || CategoryEnum.person).toString();
      const param = {
        category: categoryStr,
        code,
        id,
        name,
        resourceId: id,
      };
      const res = await addFavorite(param);
      if (res.code === 0) {
        Message(t('resource.favorite.succeedAdd'));
      } else {
        Message(t('resource.favorite.failedAdd'));
        return false;
      }

      const data = [...this.favoriteResources[categoryStr]];
      const target = { ...info, resourceId: info.id };
      if (getOnlineStatus(info) === 'offline') {
        data.push(target);
      } else {
        data.unshift(target);
      }
      this.setFavoriteResources({ category: categoryStr, data });
      return true;
    },

    // 取消收藏
    async delFavoriteResources(info) {
      const { category, id, resourceId } = info;
      const delId = resourceId || id;
      const categoryStr = (category || CategoryEnum.person).toString();
      const param = {
        category: categoryStr,
        resourceId: delId,
      };
      const { code } = await delFavorite(param);
      if (code === 0) {
        Message(t('resource.favorite.succeedDel'));
      } else {
        Message({ message: t('resource.favorite.failedDel'), type: 'warning' });
        return false;
      }

      const target = this.favoriteResources[categoryStr];
      const data = target.filter((item) => {
        const itemId = item.resourceId || item.id;
        return itemId !== delId;
      });
      this.setFavoriteResources({ category: categoryStr, data });
      return true;
    },

    // 初始化收藏
    async initFavoriteResources() {
      for (const item of resourceCategory) {
        await this.queryFavoritesList(item);
      }

      this.updateFavorite();
    },

    // 查询收藏列表
    async queryFavoritesList(category) {
      const param = {
        category,
        pageSize: 1000,
        start: 1,
      };
      const { code, data } = await listFavorite(param);
      if (code === 0) {
        this.setFavoriteResources({ category, data });
      }
    },

    // 修改收藏
    setFavoriteResources({ category, data }) {
      this.favoriteResources[category] = data.map((item) => {
        return { ...item, category: Number(item.category) };
      });
    },

    // 定时更新
    updateFavorite() {
      useSetInterval(() => {
        const { resourceOrigin } = useBaseData();
        Object.keys(this.favoriteResources).forEach((key) => {
          this.favoriteResources[key].forEach((item) => {
            const target = resourceOrigin[key]?.[item.resourceId];
            if (target) {
              item.bizStatus = target.bizStatus;
            }
          });
        });
      }, 5 * 1000);
    },
  },
  id: 'favorite',
  state: (): FavoriteState => ({
    favoriteResources: getFavoriteResources(),
  }),
});

// Need to be used outside the setup
export function useFavoriteStoreWithOut() {
  return useFavoriteStore(store);
}
