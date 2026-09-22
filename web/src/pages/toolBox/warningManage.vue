<script lang="ts" setup>
  import { computed, onMounted } from 'vue';

  import { useI18n } from '@/hooks';
  import WarningNotice from '@/pages/videoControl/warningNotice/index.vue';
  import { useAlarmStore, useMonitorStore } from '@/store';

  const { t } = useI18n();
  const monitorStore = useMonitorStore();
  const alarmStore = useAlarmStore();

  const count = computed(() => {
    return alarmStore.getAlarmData[0].length;
  });

  const isShow = computed(() => {
    return monitorStore.allBtnShowType === 'warning';
  });

  onMounted(() => {});

  function closeFrame() {
    monitorStore.setAllBtnShowType('');
  }

  function handleClick() {
    monitorStore.setAllBtnShowType('warning');
  }
</script>

<template>
  <div class="warning-manage-tools">
    <TdTooltip :content="t('homePage.menus.warningNotice')" placement="left">
      <div class="tool-btn" @click="handleClick">
        <ElBadge :hidden="count === 0" :max="99" :value="count">
          <Icon
            class="icon-tool"
            :name="isShow ? 'map_feature15_active' : 'map_feature15'"
            prefix="bigScreen"
          />
        </ElBadge>
      </div>
    </TdTooltip>

    <TdFrameBox
      v-if="isShow"
      class="frame-box-inner"
      :title="t('homePage.menus.warningNotice')"
      @close-frame-box="closeFrame"
    >
      <WarningNotice :show-tab="false" />
    </TdFrameBox>
  </div>
</template>

<style lang="less" scoped>
  .warning-manage-tools {
    position: relative;
    display: flex;
    align-items: center;

    .tool-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 44px;
      height: 44px;
      margin: 4px;
      cursor: pointer;
      background: url('@/assets/images/map/map_feature_btn.png') no-repeat;
      background-size: 100% 100%;

      :deep(.td-icon) {
        z-index: 1;
        width: 24px;
        height: 24px;
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
