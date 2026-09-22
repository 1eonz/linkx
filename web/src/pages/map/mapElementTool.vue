<script lang="ts" setup>
  import { computed, onMounted, ref, unref } from 'vue';

  import { TdAvatar } from '@/components';
  import { appConfig } from '@/config';
  import { useEmitter } from '@/hooks';
  import { mapIsReady, mapManager } from '@/plugins/map';
  import { useMapStore } from '@/store';

  const props = withDefaults(
    defineProps<{
      bigScreen?: boolean;
      mapId: string;
      showFly?: boolean;
      simple?: boolean;
    }>(),
    {
      showFly: true,
    },
  );

  const emit = defineEmits(['click']);

  const mapStore = useMapStore();
  const showBox = ref(false);
  const scaleRef = ref();
  const positionRef = ref();
  const selectItem = ref<any>({ type: '' });

  const mapTypes = computed(() => appConfig.mapList || []);

  onMounted(() => {
    const active = mapTypes.value[0];
    if (active) {
      selectItem.value = active;
    }

    initScaleAndPosition();

    const style = mapStore.getMapStyle();
    mapTypes.value.forEach((item) => {
      if (item.type === style) {
        selectItem.value = item;
      }
    });
  });

  async function initScaleAndPosition() {
    const map = mapManager.get(props.mapId);
    await mapIsReady(map);
    map.addScaleAndPosition(scaleRef.value, positionRef.value);
  }

  function handleZoom(num) {
    const map = mapManager.get(props.mapId);
    const zoom = map.getZoom() + num;
    const center = map.getCenter();
    map.setCenter(center, zoom);
  }

  function handleSetCenter() {
    emit('click');
  }

  function mapClick() {
    showBox.value = !showBox.value;
  }

  function clickOutside() {
    showBox.value = false;
  }

  function mapSetStyleClick(item) {
    if (unref(selectItem).id === item.id) {
      return;
    }

    appConfig.mapConfig = item;
    useEmitter().emit('mapTypeChange', item.mapType);
    setTimeout(initScaleAndPosition, 500);

    selectItem.value = unref(item);
    showBox.value = !showBox.value;
  }
</script>

<template>
  <div
    v-clickOutside="clickOutside"
    class="map-element-tool"
    :class="{ 'map-element-tool-show': showBox }"
  >
    <div
      class="scale-button"
      :class="{
        'scale-button-simple': simple,
      }"
    >
      <div v-show="showFly" class="btn" @click="handleSetCenter">
        <Icon class="icon-tool" name="alarm_position" />
      </div>
      <div class="btn" @click="handleZoom(1)">
        <Icon class="icon-tool" name="scale_enlarge" />
      </div>
      <div class="btn" @click="handleZoom(-1)">
        <Icon class="icon-tool" name="scale_minimize" />
      </div>
    </div>

    <div v-show="showBox" class="map-box ground-glass">
      <div
        v-for="item in mapTypes"
        :key="item.id"
        class="map-item"
        :class="[item.type]"
        @click="mapSetStyleClick(item)"
      >
        <TdAvatar v-if="item.icon" :url="item.icon" />
        <span class="children">{{ item.name }}</span>
      </div>
    </div>

    <div v-show="!showBox" class="map-select ground-glass">
      <div class="map-item" :class="[selectItem.type]" @click.stop="mapClick">
        <TdAvatar v-if="selectItem.icon" :url="selectItem.icon" />
        <span class="children">{{ selectItem.name }}</span>
      </div>
    </div>

    <div ref="positionRef" class="mouse-position"></div>

    <div class="scale-line">
      <div ref="scaleRef" class="scale-line-content"></div>
      <div class="scale"></div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .map-element-tool {
    position: absolute;
    right: 36px;
    bottom: -8px;
    z-index: 10;
    display: flex;
    align-items: center;

    &-show {
      z-index: 101;
    }

    .map-select {
      position: absolute;
      right: 0;
      bottom: 45px;
      width: 97px;
      height: 72px;
      padding: 0;
    }

    .map-box {
      position: absolute;
      right: -5px;
      bottom: 45px;
      display: flex;
      height: 72px;
      padding: 0;
    }

    .map-item {
      position: relative;
      float: left;
      width: 85px;
      height: 60px;
      margin: 5px;
      cursor: pointer;

      :deep(.td-avatar) {
        object-fit: contain;
      }

      &.white {
        background: url('@/assets/images/map/white_map.png') no-repeat;
        background-size: cover;
      }

      &.dark {
        background: url('@/assets/images/map/dark_map.png') no-repeat;
        background-size: cover;
      }

      &.raster {
        background: url('@/assets/images/map/raster_map.png') no-repeat;
        background-size: cover;
      }

      .td-avatar {
        width: 100%;
        height: 100%;
      }

      .children {
        position: absolute;
        right: 0;
        bottom: 0;
        display: inline-block;
        height: 16px;
        padding: 2px;
        font-size: 10px;
        line-height: 14.48px;
        color: rgb(255 255 255 / 100%);
        text-align: center;
        background-color: rgb(21 154 255 / 100%);
        border-top-left-radius: 2px;
      }
    }

    .scale-button {
      position: absolute;
      right: -16px;
      bottom: 130px;
      z-index: 10;
      display: flex;
      flex-direction: column;
      align-items: center;
      width: 56px;

      &-simple {
        bottom: 40px;
      }

      .btn {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 32px;
        height: 32px;
        margin-top: 4px;
        cursor: pointer;
        background: rgb(6 41 74 / 80%);
        border: 1px solid rgb(50 152 226 / 100%);
        box-shadow: inset 0 0 16px 1px rgb(14 111 179 / 100%);
        fill: var(--icon-color-default);

        .icon-tool {
          width: 20px;
          height: 20px;
          fill: var(--icon-color-default);
        }

        &:hover {
          background: var(--button-color-special-active);
          border: none;
          box-shadow: none;

          .icon-tool {
            fill: #fff;
          }
        }
      }

      .button-minimize {
        margin-top: 4px;
      }
    }

    .mouse-position {
      height: 43px;
      margin: 20px 15px 0 0;
      line-height: 43px;
      text-align: center;
      background: var(--background-default);
    }

    .scale-line {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-width: 50px;
      height: 28px;
      margin: 20px 0 0;
      background: var(--background-default);

      .scale-line-content {
        text-align: center;
      }

      .scale {
        position: relative;
        bottom: 2px;
        width: 46px;
        height: 7px;
        font-size: 12px;
        border: 2px solid #fff;
        border-top: 0;
      }
    }
  }
</style>
