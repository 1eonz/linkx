<script lang="ts" setup>
  import { computed } from 'vue';

  import { useI18n } from '@/hooks';
  import ResourceList from '@/pages/communicationCenter/resourceList.vue';
  import { useMonitorStore, useResourceStore } from '@/store';

  const monitorStore = useMonitorStore();
  const resourceStore = useResourceStore();
  const { t } = useI18n();

  const isShow = computed(() => {
    return monitorStore.allBtnShowType === 'resource';
  });

  function handleClick() {
    if (resourceStore.mapChooseData.length > 0) {
      const { setMapChooseData } = resourceStore;
      setMapChooseData([]);
    }
    monitorStore.setAllBtnShowType('resource');
  }
</script>

<template>
  <div class="resource-manage-tools">
    <TdTooltip :content="t('homePage.mapToolData.resource')" placement="left">
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
      class="frame-box"
      :show-collect="true"
      @close-dialog="handleClick"
    />
  </div>
</template>

<style lang="less" scoped>
  .resource-manage-tools {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: flex-end;
    margin-top: 8px;

    .tool-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 40px;
      height: 40px;
      margin: 2px 4px;
      cursor: pointer;
      background: url('@/assets/images/map/map_feature_btn.png') no-repeat;
      background-size: 100% 100%;

      :deep(.td-icon) {
        z-index: 1;
        width: 20px;
        height: 20px;
      }

      &.active {
        position: relative;
        background: url('@/assets/images/map/map_feature_btn_active.png') no-repeat;
        background-size: 100% 100%;

        &::after {
          position: absolute;
          width: 100%;
          height: 100%;
          content: '';
          background: url('@/assets/images/map/map_feature_btn_active.gif') no-repeat;
          background-size: 100% 100%;
        }
      }
    }

    .frame-box {
      position: absolute;
      top: 0;
      right: 68px;
      z-index: 10;
      width: 420px;
      height: calc(100vh - 124px);
    }
  }
</style>
