<script lang="ts" setup>
  import { ref } from 'vue';

  import { useI18n } from '@/hooks';
  import ResourceList from '@/pages/communicationCenter/resourceList.vue';
  import { useResourceStore } from '@/store';

  const resourceStore = useResourceStore();
  const isShow = ref(false);
  const { t } = useI18n();

  function handleClick() {
    if (resourceStore.mapChooseData.length > 0) {
      const { setMapChooseData } = resourceStore;
      setMapChooseData([]);
    }
    isShow.value = !isShow.value;
  }
</script>

<template>
  <div class="resource-manage-tools">
    <TdTooltip v-if="!isShow" :content="t('homePage.mapToolData.resource')" placement="left">
      <div class="tool-btn" @click="handleClick">
        <Icon
          class="icon-tool"
          :name="isShow ? 'map_feature9_active' : 'map_feature9'"
          prefix="bigScreen"
        />
      </div>
    </TdTooltip>

    <ResourceList
      v-if="isShow"
      class="map-command"
      :show-collect="true"
      @close-dialog="handleClick"
    />
  </div>
</template>

<style lang="less" scoped>
  .resource-manage-tools {
    position: absolute;
    top: 85px;
    left: 15px;
    display: flex;
    align-items: center;
    justify-content: flex-end;

    .tool-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 48px;
      height: 48px;
      margin: 6px;
      cursor: pointer;
      background: url('@/assets/images/map/map_feature_btn.png') no-repeat;
      background-size: 100% 100%;

      :deep(.td-icon) {
        z-index: 1;
        width: 24px;
        height: 24px;
      }

      &.active {
        background: url('@/assets/images/map/map_feature_btn_active.png') no-repeat;
        background-size: 100% 100%;

        &::after {
          width: 100%;
          height: 100%;
          content: '';
          background: url('@/assets/images/map/map_feature_btn_active.gif') no-repeat;
          background-size: 100% 100%;
        }
      }
    }

    .map-command {
      position: absolute;
      top: 0;
      left: 0;
      z-index: 10;
      width: 420px;
      height: calc(100vh - 124px);
    }
  }
</style>
