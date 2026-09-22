<template>
  <div
    class="map-element-tool"
    :class="{
      'map-element-tool-layer': layerPanelVisible && layersLength > 4,
      'map-element-tool-layer-min': layerPanelVisible && layersLength <= 4,
      'map-element-tool-detail': showDetail,
      'map-element-tool-list': showMarkList,
      'map-element-tool-list2': showMarkList && listLength === 2,
      'map-element-tool-list3': showMarkList && listLength === 3,
    }"
  >
    <div class="scale-button">
      <div v-show="showFly" class="btn" @click="handleSetCenter">
        <img class="icon-img" src="@/assets/images/map/position.png" alt="" />
      </div>
      <div class="btn" @click="handleZoom(1)">
        <span class="icon-tool">+</span>
      </div>
      <div class="btn" @click="handleZoom(-1)">
        <span class="icon-tool icon-tool2">—</span>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { onMounted, ref } from 'vue';

  import { mapIsReady, mapManager } from '@/plugins/map';

  const props = defineProps({
    mapId: {
      type: String,
      required: true,
    },
    showFly: {
      type: Boolean,
      default: true,
    },
    layerPanelVisible: {
      type: Boolean,
      default: false,
    },
    showDetail: {
      type: Boolean,
      default: false,
    },
    showMarkList: {
      type: Boolean,
      default: false,
    },
    listLength: {
      type: Number,
      default: 0,
    },
    layersLength: {
      type: Number,
      default: 0,
    },
  });

  const emit = defineEmits(['click']);

  function handleZoom(num) {
    const map = mapManager.get(props.mapId);
    if (map) {
      const zoom = map.getZoom() + num;
      const center = map.getCenter();
      map.setCenter(center, zoom);
    }
  }

  function handleSetCenter() {
    emit('click');
  }

  onMounted(async () => {
    const map = mapManager.get(props.mapId);
    if (map) {
      await mapIsReady(map);
    }
  });
</script>

<style lang="scss" scoped>
  .map-element-tool {
    position: absolute;
    right: 16px;
    bottom: 36px;
    z-index: 10;
    display: flex;
    align-items: center;

    .scale-button {
      position: relative;
      display: flex;
      flex-direction: column;
      align-items: center;
      width: 56px;

      .btn {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 44px;
        height: 44px;
        margin-top: 8px;
        cursor: pointer;
        background: #fff;
        border-radius: 8px;
        color: #fff;
        user-select: none;

        .icon-tool {
          font-size: 34px;
          color: rgba(59, 114, 255, 1);
        }
        .icon-tool2 {
          font-size: 18px;
          font-weight: bold;
        }

        .icon-img {
          width: 24px;
          height: 24px;
        }
      }
    }
  }
  .map-element-tool-layer-min {
    bottom: 150px;
  }
  .map-element-tool-layer {
    bottom: 220px;
  }
  .map-element-tool-detail {
    bottom: 242px;
  }
  .map-element-tool-list {
    bottom: 302px;
  }
  .map-element-tool-list2 {
    bottom: 206px;
  }
  .map-element-tool-list3 {
    bottom: 256px;
  }
</style>
