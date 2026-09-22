<template>
  <div class="layer-panel">
    <img
      src="@/assets/images/map/close_layer.png"
      alt=""
      class="layer-close clickable"
      @click="closeLayerPanel"
    />
    <div class="layer-panel__header">
      <span class="layer-panel__title">资源显示</span>
    </div>
    <div class="layer-panel__grid">
      <div
        v-for="layer in layers"
        :key="layer.id"
        class="layer-panel__item"
        :class="{ active: layer.visible }"
      >
        <div class="layer-panel__content" @click="handleToggle(layer)">
          <span class="layer-panel__icon">
            <img
              v-if="layer.layerImage"
              :src="layer.layerImage"
              :alt="layer.name"
              draggable="false"
            />
            <img :src="layer.visible ? selected : unselected" class="layer-panel__icon-img" />
          </span>
          <span class="layer-panel__name">{{ layer.name }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  import selected from '@/assets/images/map/selected.png';
  import unselected from '@/assets/images/map/unselected.png';

  const props = defineProps({
    layers: {
      type: Array,
      default: () => [],
    },
    pointsMap: {
      type: Object,
      default: () => ({}),
    },
  });

  const emit = defineEmits(['toggle', 'setLayerPanel']);

  function closeLayerPanel() {
    emit('setLayerPanel', false);
  }

  function getCount(layerId) {
    const list = props.pointsMap?.[layerId];
    return Array.isArray(list) ? list.length : 0;
  }

  function getDisplayCount(count) {
    if (count > 99) return '99+';
    return count;
  }

  function handleToggle(layer) {
    emit('toggle', {
      id: layer.key,
      visible: !layer.visible,
    });
  }
</script>

<style lang="scss" scoped>
  .layer-panel {
    position: absolute;
    left: 0;
    bottom: 0;
    width: 100%;
    padding: 24px;
    border-radius: 16px 16px 0 0;
    background: rgba(255, 255, 255, 0.96);
    box-shadow: 0 14px 30px rgba(0, 0, 0, 0.12);
    z-index: 40;
    color: #1f2533;
    font-size: 12px;

    .layer-close {
      width: 16px;
      height: 16px;
      position: absolute;
      right: 16px;
      top: 8px;
    }

    &__header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 12px;
    }

    &__title {
      font-size: 14px;
      font-weight: 600;
    }

    &__grid {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 12px;
      max-height: 140px;
      overflow-y: auto;
    }

    &__item {
      display: flex;
      justify-content: center;
      transition: all 0.2s ease;
      color: inherit;

      &.active {
      }

      .layer-panel__icon {
        background: url('@/assets/images/map/back_layer.png') no-repeat center center;
        background-size: 100% 100%;
        padding: 7px;
        position: relative;

        img {
          width: 26px;
          height: 26px;
        }
        .layer-panel__icon-img {
          width: 10px;
          height: 10px;
          position: absolute;
          right: 0;
          bottom: 0;
        }
      }
      .layer-panel__name {
      }
    }

    &__content {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 6px;
      cursor: pointer;
      width: fit-content;
    }

    &__icon {
      position: relative;
      width: 36px;
      height: 36px;
      display: inline-flex;
      align-items: center;
      justify-content: center;

      img {
        width: 100%;
        height: 100%;
        object-fit: contain;
      }
    }

    &__icon-placeholder {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: #dfe5ef;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 16px;
    }

    &__badge {
      position: absolute;
      right: -4px;
      top: -4px;
      min-width: 18px;
      height: 18px;
      padding: 0 4px;
      border-radius: 9px;
      background: linear-gradient(135deg, #1f8ef1, #007aff);
      color: #fff;
      font-size: 10px;
      font-style: normal;
      line-height: 18px;
      text-align: center;
    }

    &__name {
      font-size: 12px;
      color: #4a4f63;
    }
  }
</style>
