<script lang="ts" setup>
  import { computed, onMounted } from 'vue';

  import { useI18n } from '@/hooks';
  import Messages from '@/pages/messages/index.vue';
  import { useMessageStore, useMonitorStore } from '@/store';

  const monitorStore = useMonitorStore();
  const messageStore = useMessageStore();
  const { t } = useI18n();
  const isShow = computed(() => {
    return monitorStore.allBtnShowType === 'emergency';
  });
  const count = computed(() => {
    return messageStore.getMessages[0].length;
  });

  onMounted(() => {});

  function closeFrame() {
    monitorStore.setAllBtnShowType('');
  }

  function handleClick() {
    monitorStore.setAllBtnShowType('emergency');
  }
</script>

<template>
  <div class="emergency-manage-tools">
    <TdTooltip :content="t('homePage.menus.emergencyMessages')" placement="left">
      <div class="tool-btn" @click="handleClick">
        <ElBadge :hidden="count === 0" :max="99" :value="count">
          <Icon
            class="icon-tool"
            :name="isShow ? 'map_feature4_active' : 'map_feature4'"
            prefix="bigScreen"
          />
        </ElBadge>
      </div>
    </TdTooltip>

    <TdFrameBox
      v-if="isShow"
      class="frame-box-inner"
      :title="t('homePage.menus.emergencyMessages')"
      @close-frame-box="closeFrame"
    >
      <Messages :show-tab="false" />
    </TdFrameBox>
  </div>
</template>

<style lang="less" scoped>
  .emergency-manage-tools {
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
