<script setup lang="ts">
  import { onMounted, ref } from 'vue';
  import { useRoute } from 'vue-router';

  import { useI18n } from '@/hooks';
  import { getSupportLevel, getSupportType } from '@/pages/planSafety/common';
  import { usePlanStore } from '@/store';
  import dateUtil from '@/utils/dateUtil';

  const { t } = useI18n();
  const route = useRoute();
  const planStore = usePlanStore();
  const infoData = ref<any>({});
  const beginTime = ref('');
  const endTime = ref('');

  onMounted(() => {
    initPlanData();
  });

  async function initPlanData() {
    const id = route.query.id;
    const res = await planStore.queryPlanData(id);
    if (res) {
      getPlanData();
    }
  }

  function getPlanData() {
    infoData.value = planStore.planData;
    const { supportBeginTime, supportEndTime } = infoData.value;
    beginTime.value = dateUtil.format(Number(supportBeginTime));
    endTime.value = dateUtil.format(Number(supportEndTime));
  }
</script>

<template>
  <div class="plan-info">
    <div class="title"> {{ t('planSafety.normalInfo') }} </div>
    <div class="item">
      <span class="name">{{ `${t('planSafety.planName')}：` }}</span>
      <span class="info">{{ infoData.supportName }}</span>
    </div>
    <div class="item">
      <span class="name">{{ `${t('planSafety.planType')}：` }}</span>
      <span class="info">{{ getSupportType(infoData.supportType) }}</span>
    </div>
    <div class="item">
      <span class="name">{{ `${t('planSafety.planLevel')}：` }}</span>
      <span class="info">{{ getSupportLevel(infoData.supportLevel) }}</span>
    </div>
    <div class="item">
      <span class="name">{{ `${t('planSafety.planTime')}：` }}</span>
      <span class="info"> {{ beginTime }} ~ {{ endTime }} </span>
    </div>
    <div class="item">
      <span class="name">{{ `${t('planSafety.planAddress')}：` }}</span>
      <span class="info">{{ infoData.supportAddress }}</span>
    </div>
    <div class="item">
      <span class="name">{{ `${t('planSafety.planContent')}：` }}</span>
      <span class="info">
        {{ infoData.supportDesc }}
      </span>
    </div>
  </div>
</template>

<style scoped lang="less">
  .plan-info {
    padding: 10px 8px;

    .title {
      position: relative;
      height: 14px;
      padding: 0 10px;
      margin-bottom: 8px;
      font-size: 14px;
      font-weight: 500;
      line-height: 14px;

      &::before {
        position: absolute;
        top: 0;
        left: 0;
        width: 4px;
        height: 100%;
        content: '';
        background-color: rgb(26 255 251 / 100%);
      }
    }

    .item {
      display: flex;
      margin-bottom: 8px;

      .name {
        font-size: 14px;
        font-weight: 400;
        color: var(--text-title-second);
      }

      .info {
        width: 310px;
        max-height: 530px;
        overflow-y: scroll;
        font-size: 14px;
        font-weight: 400;
        line-height: 21px;
        word-break: break-all;
      }
    }
  }
</style>
