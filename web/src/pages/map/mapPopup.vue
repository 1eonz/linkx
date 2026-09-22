<script lang="ts" setup>
  import { computed, ref } from 'vue';

  import { useEmitter, useI18n } from '@/hooks';
  import { getLayerName, mapManager } from '@/plugins/map';

  const props = defineProps({
    data: {
      default: () => {},
      type: Object,
    },
    layerId: {
      default: '',
      type: String,
    },
    mapId: {
      default: '',
      type: String,
    },
    template: {
      default: () => {},
      type: Object,
    },
  });

  const { t } = useI18n();
  const detailsText = ref('');
  const sectionRef = ref();

  const hasFooter = computed(() => {
    return mapManager.get('mapType') !== 'ArcgisMap';
  });
  const title = computed(() => {
    return t(getLayerName(props.layerId));
  });

  // 关闭弹窗
  function closePopup() {
    const map = mapManager.get(props.mapId);
    map.closeCustomPopup();
    sectionRef.value?.close?.();
    useEmitter().emit('closeMapPopup');
  }
  function setDetailsText(text) {
    detailsText.value = text;
  }
  function details() {
    sectionRef.value.details();
  }
</script>

<template>
  <div class="map-popup-container ground-glass">
    <div class="header">
      <div class="title">{{ title }} </div>
      <Icon class="close-btn" name="close" @click="closePopup()" />
      <img alt="" class="header-bg" src="@/assets/images/popup/frame_title_bg.png" />
    </div>
    <div class="section">
      <component
        :is="template"
        ref="sectionRef"
        :data="data"
        :layer-id="layerId"
        :map-id="mapId"
        @set-details-text="setDetailsText"
      />
      <div v-if="detailsText" class="btn" @click="details">
        <Icon class="details-icon" name="category_detail" />
        {{ detailsText }}
      </div>
    </div>

    <div v-if="hasFooter" class="footer"></div>
  </div>
</template>

<style lang="less" scoped>
  .map-popup-container {
    position: relative;
    display: flex;
    flex-direction: column;
    width: 360px;

    .header {
      position: relative;
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 100%;
      height: 34px;
      padding: 0 10px 0 28px;

      .title {
        z-index: 1;
        font-size: 16px;
        color: var(--text-title-first);
        letter-spacing: 2px;
      }

      .close-btn {
        z-index: 1;
        width: 20px;
        height: 20px;
        cursor: pointer;
      }

      .header-bg {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
      }
    }

    .section {
      position: relative;
      box-sizing: border-box;
      width: 100%;
      padding: 10px;

      .btn {
        display: flex;
        align-items: center;
        margin-right: 10px;
        font-size: 14px;
        color: var(--text-title-second);
        cursor: pointer;

        .details-icon {
          width: 16px;
          height: 16px;
          margin-right: 3px;
        }
      }
    }

    .footer {
      &::before {
        position: absolute;
        left: 50%;
        z-index: 12;
        width: 0;
        height: 0;
        margin-left: -2px;
        content: '';
        border-top: 18px solid var(--background-default);
        border-right: 8px solid transparent;
        border-left: 8px solid transparent;
      }

      &::after {
        position: absolute;
        left: 50%;
        z-index: 10;
        width: 0;
        height: 0;
        margin-left: -3px;
        content: '';
        border-top: 20px solid var(--border-color-pop);
        border-right: 9px solid transparent;
        border-left: 9px solid transparent;
      }
    }
  }
</style>
