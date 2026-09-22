<script setup lang="ts">
  import { computed, onBeforeUnmount, onMounted, ref, unref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';

  import { queryEquipmentDetailById } from '@/api/equipment';
  import MessageBox from '@/components/MessageBox';
  import { useI18n } from '@/hooks';
  import MonitorCard from '@/pages/communicationCard/monitorCard/monitorCard.vue';
  import ResourceAbility from '@/pages/communicationCard/resourceAbilityButton/resourceAbility.vue';
  import { getOnlineStatus } from '@/pages/resource/resourceHelper';
  import { useMonitorStore, useVehicleStore, useVideoPollStore } from '@/store';

  import { debounce } from 'lodash-es';

  const emit = defineEmits(['closeDialog']);

  const { t } = useI18n();

  const route = useRoute();
  const router = useRouter();
  const vehicleStore = useVehicleStore();
  const videoPollStore = useVideoPollStore();
  const monitorStore = useMonitorStore();

  const carInfo = ref<any>({});
  const ability = ref([]);
  const organizationName = ref('');
  const full = ref(false);

  const showCard = computed(() => {
    return Object.keys(carInfo.value).length;
  });

  onMounted(() => {
    const { headerCar } = vehicleStore;

    monitorStore.deleteMonitorDrawerDataByAccount(headerCar.account);

    carInfo.value = { ...headerCar };
    ability.value = headerCar.capability?.split(',') || [];
    getEquipmentInfo();

    vehicleStore.setHeaderCarPopup(headerCar);
  });

  onBeforeUnmount(() => {
    vehicleStore.setHeaderCarPopup(null);
  });

  function fullScreen(data) {
    full.value = data;
  }

  async function getEquipmentInfo() {
    const { code, data } = await queryEquipmentDetailById({ id: unref(carInfo).equipmentId });
    if (code === 0) {
      organizationName.value = data.organizationName;
    }
  }

  const handleLeave = debounce(async () => {
    const res = await MessageBox({
      cancelText: t('common.cancel'),
      confirmText: t('videoConference.conferenceButton.end'),
      text: t('resource.vehicle.isEndPlan'),
    });
    if (res) {
      vehicleStore.setUnderProtection(false);
      videoPollStore.clearVideoPollTimer();
      emit('closeDialog');
    }
  }, 500);

  const handleFull = debounce(async () => {
    emit('closeDialog');
    videoPollStore.clearVideoPollTimer();
    const { equipmentId } = vehicleStore.headerCar;
    setTimeout(() => {
      router.push({
        name: 'leadVehicle',
        query: { id: equipmentId, planId: route.query.id },
      });
    }, 1000);
  }, 500);
</script>

<template>
  <TdFrameBox
    class="header-car-popup"
    :class="{ 'header-car-popup__full': full }"
    :dragger="true"
    :show-close-btn="false"
    :title="t('resource.vehicle.realtimeSituationOfTargetVehicle')"
  >
    <div class="popup-inner">
      <MonitorCard
        v-if="showCard"
        :monitor-info="carInfo"
        :show-header="false"
        @on-full-screen="fullScreen"
      />
      <div class="car-info">
        <div class="left">
          <div class="avatar-box" :class="[getOnlineStatus(carInfo)]"></div>
          <div class="info">
            <TdTooltip :content="carInfo.name">
              <span class="name">{{ carInfo.name }}</span>
            </TdTooltip>
            <span>{{ organizationName }}</span>
          </div>
        </div>
        <div class="speed">
          <span>
            <span>{{ carInfo.speed }}</span>
            <span>km/h</span>
          </span>
          <span>{{ t('alarm.speed') }}</span>
        </div>
      </div>
      <ResourceAbility
        :ability-data="carInfo"
        :ability-values="ability"
        class="equipment-operation"
      />
      <div class="footer">
        <TdButton :text="t('resource.vehicle.endPlan')" type="normal" @click="handleLeave" />
        <TdButton
          :text="t('resource.vehicle.toggleFullScreen')"
          type="normal"
          @click="handleFull"
        />
      </div>
    </div>
  </TdFrameBox>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .header-car-popup {
    width: 310px;

    :deep(.frame-box-container) {
      padding: 0 !important;
    }

    .monitor-card {
      width: 308px;
      height: 173px;
    }

    .car-info {
      display: flex;
      justify-content: space-between;
      padding: 10px 10px 0;

      .left {
        display: flex;

        .avatar-box {
          position: relative;
          width: 48px;
          height: 48px;
          margin-right: 6px;
          background: url('@/assets/images/map/header_car.png') no-repeat;
          background-size: 100% 100%;
        }

        .info {
          display: flex;
          flex-direction: column;

          .name {
            max-width: 140px;
            .ellipsis1();
          }

          span:nth-of-type(2) {
            font-size: 14px;
            font-weight: 400;
            color: rgb(120 168 222 / 100%);
          }
        }

        .online {
          position: relative;

          &::after {
            position: absolute;
            top: -2px;
            right: -2px;
            width: 14px;
            height: 14px;
            content: '';
            background: var(--icon-color-online);
            border: 0.5px solid #fff;
            border-radius: 50%;
          }
        }

        .offline {
          position: relative;

          &::after {
            position: absolute;
            top: -2px;
            right: -2px;
            width: 14px;
            height: 14px;
            content: '';
            background: var(--icon-color-offline);
            border: 0.5px solid #fff;
            border-radius: 50%;
          }
        }

        .avatar {
          width: 48px;
          height: 48px;
          margin-right: 4px;

          :deep(.td-icon) {
            width: 100% !important;
            height: 100% !important;
          }
        }
      }

      .speed {
        display: flex;
        flex-direction: column;
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
          color: rgb(120 168 222 / 100%);
        }
      }
    }

    .footer {
      display: flex;
      justify-content: space-between;
      width: 100%;
      padding: 0 10px;
      margin-bottom: 10px;

      .td-button {
        width: 140px;
      }
    }
  }

  .header-car-popup__full {
    backdrop-filter: none !important;
    transform: none !important;
  }
</style>
