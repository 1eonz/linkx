<script setup lang="ts">
  import { Dialog } from '@/components/Dialog';
  import { useI18n } from '@/hooks';

  import DevicePopup from './devicePopup.vue';

  const { t } = useI18n();

  /**
   * 设置预警设置
   * @param type
   */
  function setThreshold(type) {
    const cid = 'DevicePopup';
    Dialog({
      cid,
      content: DevicePopup,
      data: {
        cid,
        type,
      },
      offset: ['40%', '20%'],
      shade: true,
    });
  }
</script>

<template>
  <div class="device-threshold">
    <div class="box" @click="setThreshold(2)">
      <div class="btn">
        <Icon class="icon" name="set_battery" />
      </div>
      <span>{{ t('alarm.setBattery') }}</span>
    </div>
    <div class="box" @click="setThreshold(3)">
      <div class="btn">
        <Icon class="icon" name="set_storage" />
      </div>
      <span>{{ t('alarm.setStorage') }}</span>
    </div>
  </div>
</template>

<style scoped lang="less">
  .device-threshold {
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
