<script lang="ts" setup>
  import { computed, onMounted, ref, unref, watch } from 'vue';

  import { useI18n, useUtils } from '@/hooks';
  import { startPlanSafety } from '@/pages/bigScreen/planSpecial/vehicle';
  import ResourceAbility from '@/pages/communicationCard/resourceAbilityButton/resourceAbility.vue';
  import {
    getResourceAbilities,
    queryResourceDetailsByInfo,
  } from '@/pages/resource/resourceHelper';
  import { useVehicleStore } from '@/store';

  const props = defineProps<{
    data: any;
  }>();

  const { t } = useI18n();

  const vehicleStore = useVehicleStore();

  const equipmentData = ref<any>({});
  const hasIllusion = ref(false);

  const organizationName = computed(() => {
    return unref(equipmentData).organizationName || '';
  });
  const isHeaderCar = computed(() => {
    return vehicleStore.headerCar?.id === props.data.id;
  });

  // 聚合列表切换时数据更新
  watch(
    () => props.data,
    () => {
      queryEquipmentData();
    },
  );

  watch(
    () => vehicleStore.vehicleList,
    (val) => {
      let illusion = false;
      val.forEach((item) => {
        if (item.id === props.data.id) {
          illusion = true;
        }
      });
      hasIllusion.value = illusion;
    },
    { immediate: true },
  );

  onMounted(() => {
    queryEquipmentData();
  });

  async function queryEquipmentData() {
    equipmentData.value = await queryResourceDetailsByInfo(props.data);
  }
</script>

<template>
  <div class="map-monitor-popup">
    <div class="info-box">
      <div class="avatar-box">
        <TdAvatar class="img" :info="data" />
        <div v-if="hasIllusion && !isHeaderCar" class="illusion">
          <Icon name="illusion" />
        </div>
      </div>
      <div class="right-box">
        <TdTooltip :content="data.name">
          <div class="name">{{ data.name }}</div>
        </TdTooltip>
        <TdTooltip :content="organizationName">
          <div class="org">{{ organizationName }}</div>
        </TdTooltip>
      </div>
      <div v-if="isHeaderCar" class="header-car-speed">
        <span class="speed">
          <span>{{ vehicleStore.headerCar.speed }}</span>
          <span>km/h</span>
        </span>
        <span>{{ t('alarm.speed') }}</span>
      </div>
    </div>
    <ResourceAbility
      :ability-data="equipmentData"
      :ability-values="getResourceAbilities(equipmentData)"
      resource-type="monitor"
    />

    <TdButton
      v-if="isHeaderCar && useUtils().isSpecial"
      class="start-safety"
      :text="t('planSafety.startPlan')"
      @click="startPlanSafety()"
    />
  </div>
</template>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  .map-monitor-popup {
    width: 100%;

    .info-box {
      display: flex;
      align-items: center;

      .avatar-box {
        position: relative;
        width: 50px;
        height: 50px;
        margin-right: 8px;

        .online::after,
        .offline::after {
          width: 14px;
          height: 14px;
        }

        .img {
          width: 50px;
          height: 50px;
          border-radius: 50%;

          :deep(.icon) {
            width: 50px !important;
            height: 50px !important;
          }
        }

        .illusion {
          position: absolute;
          top: 0;
          left: 0;
          display: flex;
          align-items: center;
          justify-content: center;
          width: 50px;
          height: 50px;
          background: url('@/assets/images/map/illusion_shadow.png') no-repeat;
          background-size: 100% 100%;

          .td-icon {
            width: 24px;
            height: 24px;
          }
        }
      }

      .right-box {
        display: flex;
        flex-direction: column;
        font-size: 14px;

        .name {
          .ellipsis1;

          width: 180px;
          padding-left: 4px;
          font-size: 16px;
          font-weight: 500;
          line-height: 24px;
          color: var(--text-title-first);
          word-break: break-all;
        }

        .org {
          .ellipsis1;

          width: 180px;
          padding-left: 4px;
          font-size: 14px;
          line-height: 22px;
          color: var(--text-title-second);
          word-break: break-all;
        }
      }

      .header-car-speed {
        .speed {
          display: flex;
          margin-right: 6px;

          span {
            font-size: 16px;
            font-weight: 500;
            line-height: 24px;
            color: rgb(41 227 87 / 100%);
          }

          span:nth-of-type(2) {
            font-size: 12px;
            font-weight: 400;
            line-height: 24px;
          }
        }
      }

      :deep(.operation) {
        justify-content: flex-end;
        width: 120px;
      }
    }

    .start-safety {
      width: 100%;
      height: 32px;
    }
  }
</style>
