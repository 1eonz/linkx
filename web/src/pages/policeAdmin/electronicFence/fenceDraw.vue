<script setup lang="ts">
  import { Dialog } from '@/components/Dialog';
  import { useI18n } from '@/hooks';

  import FencePopup from './fencePopup.vue';

  const { t } = useI18n();

  /**
   * 绘制围栏
   * @param type line/area
   */
  function drawFence(type) {
    const cid = 'FencePopup';
    Dialog({
      cid,
      content: FencePopup,
      data: {
        cid,
        drawType: type,
        handleType: 'add',
      },
      shade: true,
    });
  }
</script>

<template>
  <div class="fence-draw">
    <div class="box" @click="drawFence('area')">
      <div class="btn">
        <Icon class="icon" name="fence_area" />
      </div>
      <span>{{ t('policeAdmin.fence.drawArea') }}</span>
    </div>
    <div class="box" @click="drawFence('line')">
      <div class="btn">
        <Icon class="icon" name="fence_line" />
      </div>
      <span>{{ t('policeAdmin.fence.drawLine') }}</span>
    </div>
  </div>
</template>

<style scoped lang="less">
  .fence-draw {
    position: absolute;
    bottom: 40px;
    left: -180px;
    z-index: 100;

    .box {
      display: flex;
      align-items: center;
      margin-bottom: 12px;
      cursor: pointer;

      .btn {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 32px;
        height: 32px;
        padding: 2px;
        margin-right: 5px;
        background-image: url('@/assets/svg/menu_bg_inactive.svg');
        background-size: 100% 100%;

        &:hover {
          background-image: url('@/assets/svg/menu_bg_active.svg');
        }

        &_active {
          background-image: url('@/assets/svg/menu_bg_active.svg');
        }

        .icon {
          width: 14px;
          height: 14px;
          fill: #fff;
        }
      }
    }
  }
</style>
