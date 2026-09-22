<script lang="ts" setup>
  import { onMounted, ref } from 'vue';

  import { queryMessageAlertDetails } from '@/api/message';
  import { useEmitter, useI18n } from '@/hooks';
  import ResourceAbility from '@/pages/communicationCard/resourceAbilityButton/resourceAbility.vue';
  import { openVideoPopup } from '@/pages/map/openVideoPopup';
  import { getInfoByAccount } from '@/pages/resource/resourceHelper';

  const props = defineProps<{
    data: any;
  }>();
  defineExpose({ pageNumChange });

  const { t } = useI18n();
  const messageData = ref<any>({});
  const abilityData = ref({});
  const abilityValues = ref([]);
  const loading = ref(true);

  onMounted(() => {
    getMessageData();
  });

  async function getResourceDataByAccount() {
    const data = await getInfoByAccount(props.data.fromIsdn, false);
    abilityData.value = data;
    const defaultAbility = new Set(['512001', '512006', '512007']);
    const arr = data.capability?.split(',') || [];
    abilityValues.value = arr.filter((item) => {
      return defaultAbility.has(item);
    });
    loading.value = false;
  }

  function callClick({ account, type }) {
    if (['monitor', 'video'].includes(type)) {
      openVideoPopup({
        account,
        infoData: {},
      });
    }
  }

  async function getMessageData() {
    getResourceDataByAccount();

    const { code, data } = await queryMessageAlertDetails({ id: props.data.id });
    if (code === 0) {
      messageData.value = data;
      useEmitter().emit('mapMessageCardDetailsChange', data.id);
    }
    loading.value = false;
  }

  function pageNumChange() {
    loading.value = true;
    getMessageData();
  }
</script>

<template>
  <div v-loading="loading" class="map-message-popup">
    <div class="message-title">
      <span class="title-text">{{ messageData.title }}</span>
    </div>

    <div class="message-details">
      <span class="message-status"> {{ t('message.status.name') }} </span>
      <span>
        {{ messageData.content || messageData.context }}
      </span>
    </div>

    <ResourceAbility
      :ability-data="abilityData"
      :ability-values="abilityValues"
      resource-type="equipment"
      @call-click="callClick"
    />
  </div>
</template>

<style lang="less" scoped>
  .map-message-popup {
    .message-title {
      .title-text {
        font-size: 14px;
        font-weight: bold;
        line-height: 20px;
        color: var(--text-title-first);
        word-break: break-all;
      }
    }

    .message-details {
      .message-status {
        color: var(--text-title-second);
      }

      span {
        margin-right: 4px;
        font-size: 12px;
        line-height: 16px;
        color: var(--text-default);
      }
    }
  }
</style>
