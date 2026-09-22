<script lang="ts" setup>
  import { onMounted, ref, watch } from 'vue';

  import { useI18n } from '@/hooks';

  import { cloneDeep } from 'lodash-es';

  const props = defineProps({
    eventId: {
      default: '',
      type: String,
    },
    id: {
      required: true,
      type: String,
    },
    mapCompType: {
      default: '',
      type: String,
    },
    position: {
      default: () => {},
      type: Object,
    },
    simpleType: {
      default: '',
      type: String,
    },
  });

  const { t } = useI18n();
  const positionData = ref<any>({});
  const editing = ref(false);
  const historyAddress = ref('');

  watch(
    () => props.position,
    () => {
      const { position } = props;
      positionData.value = cloneDeep(position);
      positionData.value.latLon =
        positionData.value.latLon && position.latLon.coordinates
          ? `(` +
            `N${positionData.value.latLon.coordinates[0].lon},` +
            `E${positionData.value.latLon.coordinates[0].lat})`
          : '';
      historyAddress.value = positionData.value.address;
    },
  );

  onMounted(() => {
    const { position } = props;
    positionData.value = cloneDeep(position);
    positionData.value.latLon =
      position.latLon && position.latLon.coordinates
        ? `(` +
          `N${position.latLon.coordinates[0].lon},` +
          `E${position.latLon.coordinates[0].lat})`
        : '';
    historyAddress.value = positionData.value.address;
  });

  function edit() {
    editing.value = true;
  }
</script>

<template>
  <div class="position-part common-list-item" :style="{ height: 'auto' }">
    <div class="address">
      <span class="address-title">
        {{ t('mission.missionList.eventAddr') }}
      </span>
      <div class="edit-content">
        <TdTooltip :content="positionData.address">
          <span class="words-show" @click="edit">
            {{ positionData.address }}
          </span>
        </TdTooltip>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  .tips-tittle {
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--button-color-guide-default);

    .tips-icon {
      width: 15px;
      height: 13px;
      margin-left: 3px;
    }
  }

  .position-part {
    display: flex;
    flex-direction: column;
    justify-content: space-around;
    width: 100%;
    font-size: 14px;

    .address {
      display: flex;
      align-items: flex-start;

      .address-title {
        display: inline-block;
        width: 56px;
        margin-right: 4px;
        color: var(--text-title-second);
      }

      .edit-content {
        position: relative;
        display: flex;
        align-items: center;
        max-width: calc(100% - 70px);

        .words-show {
          width: 200px;
          color: var(--text-color-normal);
          word-break: break-all;
          .ellipsis(2);
        }
      }
    }
  }
</style>
