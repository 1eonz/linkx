import { onMounted } from 'vue';

import { useFavoriteStore } from '@/store';

export function initFavorite() {
  const { initFavoriteResources } = useFavoriteStore();

  onMounted(() => {
    initFavoriteResources();
  });
}
