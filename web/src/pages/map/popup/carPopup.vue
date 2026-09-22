<script lang="ts" setup>
  import { computed, onMounted, ref, watch } from 'vue';

  import { queryPoliceCar } from '@/api/equipment';
  import { useI18n } from '@/hooks';
  import ResourceAbility from '@/pages/communicationCard/resourceAbilityButton/resourceAbility.vue';
  import VoiceCallVolume from '@/pages/communicationCard/voiceCallVolume.vue';
  import { useCommunicationStore } from '@/store';
  import { isDef } from '@/utils/is';

  const props = defineProps({
    data: {
      default: () => {},
      type: Object,
    },
    title: {
      default: '',
      type: String,
    },
  });
  const { t } = useI18n();
  const communicationStore = useCommunicationStore();
  const infoData = ref<any>({});
  const showVoice = ref(false);

  const abilityValues = computed(() => {
    const { capability = '' } = infoData.value;
    return capability.split(',');
  });
  const carName = computed(() => {
    const { code, name } = infoData.value || {};
    return name || code;
  });
  const commId = computed(() => {
    return infoData.value.account;
  });

  watch(communicationStore.comm, (val) => {
    const comm = val[infoData.value.account];
    if (!comm) {
      showVoice.value = false;
      return;
    }
    showVoice.value = isDef(comm.voice);
  });
  watch(
    () => props.data,
    () => {
      queryPoliceCarData();
    },
  );

  onMounted(() => {
    queryPoliceCarData();
  });

  async function queryPoliceCarData() {
    const { code, data } = await queryPoliceCar(props.data.id);
    if (code === 0) {
      infoData.value = data;
    }
  }
</script>

<template>
  <div class="map-car-popup">
    <!-- info -->
    <div class="info-box">
      <div class="img-car"></div>
      <div class="info">
        <div class="info-name">
          <TdTooltip :content="carName">
            <span class="subtitle-name">
              {{ carName }}
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
        <TdTooltip :content="infoData.organizationName">
          <span class="organization-name">
            {{ infoData.organizationName }}
          </span>
        </TdTooltip>
      </div>
    </div>

    <!-- btn -->
    <ResourceAbility
      :ability-data="infoData"
      :ability-values="abilityValues"
      class="operation-btn"
      resource-type="car"
    />

    <!-- car-ext -->
    <div class="car-ext">
      <div class="ext-title">
        {{ t('resource.detailType.basics') }}
      </div>
      <div class="ext-position">
        <span class="name">
          {{ t('resource.detailType.currentPosition') }}
        </span>
        <span>{{ infoData.realTimePosition }}</span>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  .video-call {
    padding: 0 2px;
    margin-bottom: 10px;
  }

  .map-car-popup {
    position: relative;

    .info-box {
      display: flex;
      align-items: center;
      width: 100%;
      height: 100%;

      .img-car {
        width: 50px;
        height: 50px;
        margin-right: 8px;
        background: url('@/assets/images/map/car.png') no-repeat center center;
        background-color: #fff;
        background-size: 40px, 40px;
        border-radius: 25px;
      }

      .info {
        display: flex;
        flex-direction: column;
        width: calc(100% - 58px);
        font-size: 14px;

        .info-name {
          display: flex;
          align-items: center;

          .subtitle-name {
            .ellipsis1();

            max-width: 120px;
            margin-right: 8px;
            font-size: 16px;
            font-weight: bold;
            color: var(--text-title-first);
          }

          .voice-call {
            width: 80px;
          }
        }

        .organization-name {
          .ellipsis1();

          color: var(--text-title-second);
        }
      }
    }

    .operation-btn {
      width: 100%;
      margin-top: 10px;
    }

    .car-ext {
      position: relative;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      width: 100%;
      padding: 10px 8px;
      margin-top: 10px;
      background: var(--background-simple);

      .ext-title {
        font-size: 14px;
        color: var(--text-title-first);
      }

      .ext-position {
        .name {
          margin-right: 4px;
          color: var(--text-title-second);
        }

        span {
          font-size: 12px;
        }
      }
    }
  }
</style>
