<script lang="ts" setup>
  import { computed, ref } from 'vue';

  import { useI18n } from '@/hooks';
  import { getLayerName, mapManager } from '@/plugins/map';

  import { debounce } from 'lodash-es';

  const props = defineProps({
    data: {
      default: () => [],
      type: Array,
    },
    layerId: {
      default: '',
      type: String,
    },
    mapId: {
      default: '',
      type: String,
    },
    position: {
      default: () => [],
      type: Array,
    },
    template: {
      default: () => {},
      type: Object,
    },
  });

  const { t } = useI18n();
  const index = ref(0);
  const detailsText = ref('');
  const itemRef = ref();

  const title = computed(() => {
    const ret = getLayerName(props.layerId);
    return t(ret) + t('resource.map.list');
  });
  const hasFooter = computed(() => {
    return mapManager.get('mapType') !== 'ArcgisMap';
  });

  const paging = debounce((num) => {
    if (num === -1 && index.value === 0) {
      return;
    }
    if (num === 1 && index.value === props.data.length - 1) {
      return;
    }
    index.value += num;
    setTimeout(() => {
      itemRef.value.pageNumChange?.();
    });
  }, 500);

  function zoomTo() {
    const mapObj = mapManager.get(props.mapId);
    const zoom = mapObj.getZoom();
    mapObj.setCenter(props.position, zoom + 3);
    mapObj.closeCustomPopup();
  }

  function setDetailsText(text) {
    detailsText.value = text;
  }

  function details() {
    itemRef.value.details();
  }

  // 关闭弹窗
  function closePopup() {
    itemRef.value?.close?.();
    const mapObj = mapManager.get(props.mapId);
    mapObj.closeCustomPopup();
  }
</script>

<template>
  <div class="map-cursor-container ground-glass">
    <div class="header">
      <div class="title">{{ title }}</div>
      <Icon class="close-btn" name="close" @click="closePopup" />
      <img alt="" class="header-bg" src="@/assets/images/popup/frame_title_bg.png" />
    </div>

    <div class="section">
      <component
        :is="template"
        ref="itemRef"
        class="cursor-list-item"
        :data="data[index]"
        :layer-id="layerId"
        :map-id="mapId"
        @set-details-text="setDetailsText"
      />
      <div class="footer-tool">
        <div class="left-btn">
          <div class="btn" @click="zoomTo">
            <Icon class="icon-btn" name="category_zoom" />
            {{ t('resource.map.zoomTo') }}
          </div>
          <div v-if="detailsText" class="btn" @click="details">
            <Icon class="icon-btn" name="category_detail" />
            {{ detailsText }}
          </div>
        </div>
        <div class="right-paging">
          <Icon class="icon-btn" name="paging_left" @click="paging(-1)" />
          <span class="count">{{ `${index + 1}/${data.length}` }}</span>
          <Icon class="icon-btn" name="paging_right" @click="paging(1)" />
        </div>
      </div>
    </div>

    <div v-if="hasFooter" class="footer"></div>
  </div>
</template>

<style lang="less" scoped>
  .map-cursor-container {
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
      background-size: contain;

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
      width: 100%;
      padding: 10px;

      .footer-tool {
        display: flex;
        align-items: center;
        justify-content: space-between;

        .left-btn {
          display: flex;

          .btn {
            display: flex;
            align-items: center;
            margin-right: 10px;
            color: var(--text-title-second);
            cursor: pointer;

            .icon-btn {
              width: 16px;
              height: 16px;
              margin-right: 3px;
            }
          }
        }

        .right-paging {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 0 5px;
          background-color: var(--background-secondary);

          .count {
            padding: 0 5px;
            color: var(--text-title-second);
          }

          .icon-btn {
            width: 16px;
            height: 16px;
            cursor: pointer;
          }
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
