<script setup lang="ts">
  import { useI18n } from '@/hooks';

  import { getTimeStr } from './alarmCommon';

  withDefaults(
    defineProps<{
      alarmData: any;
    }>(),
    {
      alarmData: {
        address: '',
        alarmType: '',
        battery: '',
        deviceId: '',
        occurredTime: '',
        recoveredAddress: '',
        recoveredTime: '',
        remainingSize: '',
        state: '',
      },
    },
  );
  const { t } = useI18n();
</script>

<template>
  <div class="alarm-item">
    <div class="alarm-box common-card-default">
      <div class="box-item">
        <span class="item-name"> {{ t('alarm.deviceCode') }}：</span>
        <span class="item-value">{{ alarmData.deviceCode }} </span>
      </div>
      <div v-if="alarmData.alarmType === 2" class="box-item">
        <span class="item-name"> {{ t('alarm.alarmPower') }}：</span>
        <span class="item-value">
          <TdTag :label="`${alarmData.detail}%`" type="red" />
        </span>
      </div>
      <div v-if="alarmData.alarmType === 3" class="box-item">
        <span class="item-name"> {{ t('alarm.alarmStorage') }}：</span>
        <span class="item-value">
          <TdTag :label="`${alarmData.detail}MB`" type="red" />
        </span>
      </div>
    </div>
    <div class="alarm-box common-card-default">
      <div class="box-item">
        <span class="item-name"> {{ t('alarm.alarmTime') }}：</span>
        <span class="item-value">{{ getTimeStr(alarmData.occurredTime) }} </span>
      </div>
      <div class="box-item">
        <span class="item-name"> {{ t('alarm.alarmLocation') }}：</span>
        <span class="item-value">{{ alarmData.address }}</span>
      </div>
    </div>
    <div v-if="alarmData.state === 2" class="alarm-box common-card-default">
      <div class="box-item">
        <span class="item-name"> {{ t('alarm.recoveredTime') }}：</span>
        <span class="item-value">{{ getTimeStr(alarmData.recoveredTime) }} </span>
      </div>
      <div class="box-item">
        <span class="item-name"> {{ t('alarm.recoveredLocation') }}：</span>
        <span class="item-value">{{ alarmData.recoveredAddress }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .alarm-box {
    padding: 10px 14px;
    margin-top: 10px;

    .box-item {
      box-sizing: border-box;

      .item-name {
        font-size: var(--font-size-small);
        color: var(--text-title-second);
      }

      .item-value {
        font-size: var(--font-size-small);
        word-break: break-all;
      }
    }
  }
</style>
