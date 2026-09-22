<script lang="ts" setup>
  import { computed, ref, unref } from 'vue';

  import { useI18n } from '@/hooks';
  import JurisdictionList from '@/pages/bigScreen/mapCenter/jurisdiction/jurisdictionList.vue';
  import { useMonitorStore } from '@/store';

  const { t } = useI18n();
  const monitorStore = useMonitorStore();

  const listRef = ref();
  const operate = ref('');

  const isShow = computed(() => {
    return monitorStore.allBtnShowType === 'jurisdiction';
  });
  const frameTitle = computed(() => {
    const title = {
      create: t('resource.jurisdiction.create'),
      edit: t('resource.jurisdiction.edit'),
    };
    return title[unref(operate)] || t('resource.jurisdiction.manage');
  });

  function operateChange(val) {
    operate.value = val;
  }

  function closeFrame() {
    listRef.value.reset();
    monitorStore.setAllBtnShowType('');
  }

  function handleClick() {
    monitorStore.setAllBtnShowType('jurisdiction');
  }
</script>

<template>
  <div class="jurisdiction-tools">
    <TdTooltip :content="t('resource.jurisdiction.manage')" placement="left">
      <div class="tool-btn" @click="handleClick">
        <Icon
          class="icon-tool"
          :name="isShow ? 'map_feature16_active' : 'map_feature16'"
          prefix="bigScreen"
        />
      </div>
    </TdTooltip>
    <TdFrameBox
      v-show="isShow"
      class="frame-box-inner"
      :title="frameTitle"
      @close-frame-box="closeFrame"
    >
      <JurisdictionList ref="listRef" @operate-change="operateChange" />
    </TdFrameBox>
  </div>
</template>

<style lang="less" scoped>
  .jurisdiction-tools {
    position: relative;
    display: flex;
    align-items: center;

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

    .frame-box-inner {
      position: fixed;
      top: 80px;
      right: 96px;
      z-index: 1210;
      height: calc(100vh - 124px);
    }
  }
</style>
