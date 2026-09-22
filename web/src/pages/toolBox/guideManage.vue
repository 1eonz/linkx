<script lang="ts" setup>
  import { computed, onMounted } from 'vue';

  import { useI18n } from '@/hooks';
  import Guide from '@/pages/guide/index.vue';
  import { useMonitorStore } from '@/store';

  const { t } = useI18n();
  const monitorStore = useMonitorStore();

  const isShow = computed(() => {
    return monitorStore.allBtnShowType === 'guide';
  });

  onMounted(() => {});

  function closeFrame() {
    monitorStore.setAllBtnShowType('');
  }

  function handleClick() {
    monitorStore.setAllBtnShowType('guide');
  }
</script>

<template>
  <div class="guide-manage-tools">
    <TdTooltip :content="t('homePage.menus.lawEnforcementEncyclopedia')" placement="left">
      <div class="tool-btn" @click="handleClick">
        <Icon
          class="icon-tool"
          :name="isShow ? 'map_feature18' : 'map_feature18_active'"
          prefix="bigScreen"
        />
      </div>
    </TdTooltip>

    <TdFrameBox
      v-if="isShow"
      class="frame-box"
      :title="t('homePage.menus.lawEnforcementEncyclopedia')"
      @close-frame-box="closeFrame"
    >
      <Guide :show-tab="false" />
    </TdFrameBox>
  </div>
</template>

<style lang="less" scoped>
  .guide-manage-tools {
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

    .frame-box {
      position: fixed;
      top: 80px;
      right: 96px;
      z-index: 1210;
      height: calc(100vh - 124px);
    }
  }
</style>
