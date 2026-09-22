<script lang="ts" setup>
  import { ref, watch } from 'vue';

  import { useI18n } from '@/hooks';
  import { mapManager } from '@/plugins/map';

  const props = defineProps({
    distance: {
      default: '',
      type: [String, Number],
    },
    id: {
      default: '',
      type: String,
    },
    mapId: {
      default: '',
      type: String,
    },
    type: {
      default: 0,
      type: Number,
    },
  });
  const emit = defineEmits(['clear']);
  const { t } = useI18n();
  const typeName = ref('');
  const typeUnit = ref('');

  watch(
    () => props.type,
    (val) => {
      if (val) {
        typeName.value = t('resource.address.totalArea');
        typeUnit.value = t('resource.address.squareKilometers');
      } else {
        typeName.value = t('resource.address.totalLength');
        typeUnit.value = t('resource.address.kilometers');
      }
    },
    { deep: true, immediate: true },
  );

  function clear() {
    const { id, mapId } = props;
    if (id) {
      const map = mapManager.get(mapId);
      const marker = { id };
      map.removeMarkers([marker]);
      map.deleteFeatureById(id);
    } else {
      emit('clear');
    }
  }
</script>

<template>
  <div class="map-range-popup">
    <span class="type">{{ typeName }}</span>
    <span class="number">{{ Number(distance).toFixed(3) }}</span>
    <span class="unit">{{ typeUnit }}</span>
    <span class="btn" @click="clear">{{ t('resource.address.clear') }}</span>
  </div>
</template>

<style lang="less" scoped>
  .map-range-popup {
    display: flex;
    align-items: center;
    height: 20px;
    padding: 8px;
    background: rgb(6 41 74 / 60%) !important;
    backdrop-filter: blur(8px) !important;
    border-radius: 2px !important;
    box-shadow: 0 1px 4px rgb(10 255 231/ 100%) !important;

    span {
      height: 20px;
      font-size: 12px;
      line-height: 20px;
    }

    .number {
      margin: 0 2px;
      color: #ff6319;
    }

    .btn {
      margin-left: 8px;
      color: #f00;
      cursor: pointer;
    }
  }
</style>
