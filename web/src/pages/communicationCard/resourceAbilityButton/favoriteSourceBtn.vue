<script setup lang="ts">
  import { computed, unref } from 'vue';

  import { CategoryEnum } from '@/enums';
  import { useI18n } from '@/hooks';
  import { useFavoriteStore } from '@/store';

  import { debounce } from 'lodash-es';

  const props = defineProps({
    btnType: {
      default: 'radioSpecial',
      type: String,
    },
    info: {
      default: () => {},
      type: Object,
    },
    layoutColumn: {
      default: false,
      type: Boolean,
    },
    size: {
      default: '',
      type: String,
    },
  });
  const emit = defineEmits(['callClick']);

  const { t } = useI18n();
  const favoriteStore = useFavoriteStore();

  const favorite = computed(() => {
    const { category, id, resourceId } = props.info;
    const categoryStr = (category || CategoryEnum.person).toString();
    const infoId = resourceId || id;
    const arr = favoriteStore.favoriteResources?.[categoryStr] || [];
    let isFavorite = false;
    arr.forEach((item) => {
      const itemId = item.resourceId || item.id;
      if (itemId === infoId) {
        isFavorite = true;
      }
    });
    return isFavorite;
  });
  const tips = computed(() => {
    return unref(favorite)
      ? t('monitor.monitorFunction.cancelCollection')
      : t('monitor.monitorFunction.cameraCollection');
  });

  const handleFavoriteResource = debounce(async () => {
    if (favorite.value) {
      const res = await favoriteStore.delFavoriteResources(props.info);
      if (!res) return;
    } else {
      const res = await favoriteStore.addFavoriteResources(props.info);
      if (!res) return;
    }
    emit('callClick');
  }, 500);
  defineExpose({ trigger: handleFavoriteResource });
</script>

<template>
  <!-- 收藏 -->
  <div class="favorite-source-btn">
    <div v-if="layoutColumn" class="layout-column-btn" @click.stop="handleFavoriteResource">
      {{ tips }}
    </div>
    <TdTooltip v-else :content="tips" placement="top">
      <TdButton
        :icon-name="favorite ? 'btn_uncollected' : 'btn_collected'"
        :size="size"
        :type="btnType"
        @click.stop="handleFavoriteResource"
      />
    </TdTooltip>
  </div>
</template>

<style lang="less" scoped>
  .favorite-source-btn {
    display: flex;
    align-items: center;
  }
</style>
