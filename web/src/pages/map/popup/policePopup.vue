<script lang="ts" setup>
  import { computed, onBeforeUnmount, onMounted, ref, unref, watch } from 'vue';

  import { queryEquipmentDetailById, queryEquipmentExt } from '@/api/equipment';
  import { Message } from '@/components/Message';
  import { CategoryEnum } from '@/enums';
  import { useI18n, useSetInterval, useUtils } from '@/hooks';
  import { startPlanSafety } from '@/pages/bigScreen/planSpecial/vehicle';
  import ComponentCardList from '@/pages/communicationCard/componentCardList.vue';
  import { removeVoiceHalfCallKeyUpFunction } from '@/pages/communicationCard/openResourcesCard';
  import ResourceAbility from '@/pages/communicationCard/resourceAbilityButton/resourceAbility.vue';
  import VoiceCallVolume from '@/pages/communicationCard/voiceCallVolume.vue';
  import { statusOptions } from '@/pages/policeAdmin/serviceStatus/common';
  import { getOnlineStatus, queryPersonDetailById } from '@/pages/resource/resourceHelper';
  import { useCommunicationStore, useVehicleStore } from '@/store';
  import { isDef } from '@/utils/is';

  const props = defineProps({
    data: {
      default: () => {},
      type: Object,
    },
    layerId: {
      default: '',
      type: String,
    },
  });

  const { t } = useI18n();
  const communicationStore = useCommunicationStore();
  const vehicleStore = useVehicleStore();

  const extData = ref(null);
  const showVoice = ref(false);
  const commId = ref('');
  const abilityValues = ref<any[]>([]);
  const personData = ref({
    headShot: '',
  });
  const equipmentData = ref({});
  const hasIllusion = ref(false);
  let clearTimer: any = null;

  const isPerson = computed(() => {
    return props.layerId.includes('person');
  });
  const infoData = computed((): any => {
    return unref(isPerson) ? personData.value : equipmentData.value;
  });
  const organizationName = computed(() => {
    const { organizationName } = unref(infoData);
    return organizationName || '';
  });
  const resourceType = computed(() => {
    return isPerson.value ? 'person' : 'equipment';
  });
  // 名称/编号
  const resourceName = computed(() => {
    const { code, name } = unref(infoData) || {};
    return name || code;
  });
  // 在岗状态名称
  const policeStatus = computed(() => {
    const { attendance, category } = props.data;
    if (category === CategoryEnum.ballCamera) {
      return false;
    }

    let statusName = '';
    statusOptions().forEach((item) => {
      if (item.value === attendance) {
        statusName = item.label;
      }
    });
    return statusName;
  });
  // 在岗状态颜色
  const statusColor = computed(() => {
    const colors = ['', 'blue', 'gray', 'yellow'];
    const { attendance } = props.data;

    return (colors[attendance] || 'gray') as 'blue' | 'gray' | 'yellow';
  });
  const isHeaderCar = computed(() => {
    return vehicleStore.headerCar?.id === props.data.id;
  });

  // 聚合列表切换时数据更新
  watch(
    () => props.data,
    (newVal, oldVal) => {
      if (newVal.id !== oldVal?.id) {
        commHandler();
        initData();
      }
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

  watch(communicationStore.comm, commHandler);

  clearTimer = useSetInterval(initData, 5000, true);

  defineExpose({
    close: () => {
      clearTimer();
    },
  });

  onMounted(() => {
    removeVoiceHalfCallKeyUpFunction();
  });

  onBeforeUnmount(() => {
    clearTimer();
  });

  async function initData() {
    await (unref(isPerson) ? queryPersonDetail() : queryEquipmentData());
  }

  // 人员详情
  async function queryPersonDetail() {
    const data = await queryPersonDetailById(props.data.id);
    if (data) {
      personData.value = { ...data, category: CategoryEnum.person };
    } else {
      Message(t('resource.manDetail.failedObtainIndividualInformation'));
    }
  }

  // 设备详情
  async function queryEquipmentData() {
    const { id } = props.data;
    const res = await queryEquipmentDetailById({ id });
    if (res.code === 0) {
      abilityValues.value = res.data.capability?.split(',') || [];
      equipmentData.value = res.data;
    }

    const { code, data } = await queryEquipmentExt({ id });
    if (code === 0) {
      extData.value = data;
    }
  }

  // 通信状态
  function commHandler() {
    let comm: any = null;
    props.data.serviceAccounts?.forEach((item) => {
      const { account } = item;
      const val = communicationStore.comm[account];
      if (val) {
        comm = val;
        commId.value = account;
      }
    });
    if (!comm) {
      showVoice.value = false;
      return;
    }
    if (comm.voice) {
      showVoice.value = true;
    }
    showVoice.value = isDef(comm.voice);
  }
</script>

<template>
  <div class="map-police-popup">
    <!-- info -->
    <div class="info-box">
      <div class="avatar-box" :class="[getOnlineStatus(infoData)]">
        <TdAvatar
          class="img"
          :info="{ ...infoData, isHeader: isHeaderCar }"
          :url="infoData.headShot || ''"
        />

        <div v-if="hasIllusion && !isHeaderCar" class="illusion">
          <Icon name="illusion" />
        </div>
      </div>

      <div class="info">
        <div class="info-name" :class="{ 'info-name-person': isPerson }">
          <div class="text-box">
            <TdTooltip :content="resourceName">
              <span class="subtitle-name">
                {{ resourceName }}
              </span>
            </TdTooltip>

            <!-- voice-call -->
            <VoiceCallVolume
              v-show="showVoice"
              class="voice-call"
              :commu-id="commId"
              :show-btn="false"
            />
          </div>

          <TdTag v-if="policeStatus" :label="policeStatus" :type="statusColor" />
        </div>
        <TdTooltip :content="organizationName">
          <span v-show="!isPerson" class="organization-name"> {{ organizationName }}</span>
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

    <!-- btn -->
    <ResourceAbility
      :ability-data="infoData"
      :ability-values="abilityValues"
      class="equipment-operation"
      :resource-type="resourceType"
      :voice-half="layerId === 'pdt'"
    />

    <!-- person ext -->
    <div v-if="isPerson" class="person-ext">
      <ComponentCardList :info-data="infoData" type="person" />
    </div>

    <!-- equipment ext -->
    <div v-if="extData" class="ext-box">
      <div class="ext-item">
        <div v-show="infoData.manufacturer" class="title">
          {{ infoData.manufacturer }}
        </div>
        <span v-show="infoData.battery" class="text">
          <Icon class="icon" name="equipment_battery" />
          <span>{{ infoData.battery }}%</span>
        </span>
        <span v-show="infoData.remainingSize" class="text">
          <Icon class="icon" name="equipment_storage" />
          <span>{{ infoData.remainingSize }}M</span>
        </span>
      </div>
      <div class="ext-item">
        <div v-show="infoData.demsUserName" class="title">
          {{ t('resource.cardDetailData.demsName') }}
        </div>
        <span v-show="infoData.demsUserName" class="text">
          {{ infoData.demsUserName }}
        </span>
        <span v-show="infoData.demsUserCode" class="text">
          {{ infoData.demsUserCode }}
        </span>
      </div>
    </div>

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

  .map-police-popup {
    position: relative;

    .info-box {
      display: flex;
      align-items: center;
      width: 100%;
      height: 100%;

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

      .info {
        display: flex;
        flex-direction: column;
        font-size: 14px;

        .info-name {
          display: flex;
          align-items: center;
          width: 100%;

          .text-box {
            display: flex;
            align-items: center;
          }

          .subtitle-name {
            .ellipsis1();

            max-width: 150px;
            margin-right: 8px;
            font-size: 16px;
            font-weight: bold;
            color: var(--text-title-first);
          }
        }

        .info-name-person {
          .text-box {
            display: flex;
            flex-direction: column;
            align-items: flex-start;
            justify-content: center;
          }

          .voice-call {
            .ellipsis1();

            max-width: 150px;
          }
        }

        .organization-name {
          .ellipsis1();

          color: var(--text-title-second);
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

      .online {
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
    }

    .ext-box {
      .ext-item {
        box-sizing: border-box;
        padding: 10px 8px;
        background: var(--background-simple);

        &:nth-child(1) {
          margin-bottom: 8px;
        }

        .title {
          font-size: 16px;
          color: var(--text-title-first);
        }

        .text {
          margin-right: 8px;
          font-size: 14px;
          color: var(--text-title-second);

          span {
            color: var(--text-title-second);
          }
        }

        .icon {
          width: 14px;
          height: 14px;
        }
      }
    }

    .start-safety {
      width: 100%;
      height: 32px;
    }
  }
</style>
