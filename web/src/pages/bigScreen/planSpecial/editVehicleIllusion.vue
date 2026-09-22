<script setup lang="ts">
  import { computed, onMounted, ref, unref, watch } from 'vue';

  import MessageBox from '@/components/MessageBox';
  import { useI18n } from '@/hooks';
  import { useResourceStore, useVehicleStore } from '@/store';

  const props = defineProps<{
    info: any;
  }>();

  const emit = defineEmits(['closeDialog']);

  const { t } = useI18n();

  const vehicleStore = useVehicleStore();
  const resourceStore = useResourceStore();

  const isHeaderCar = ref(false);
  const activeIcon = ref('');

  const iconList = computed(() => resourceStore.vehicleIcon);

  watch(
    () => resourceStore.vehicleIcon,
    (val) => {
      // 默认选中第一个
      if (!unref(activeIcon)) {
        activeIcon.value = val[0]?.id;
      }
    },
    {
      immediate: true,
    },
  );

  onMounted(() => {
    const { icon, isHeader } = props.info;
    isHeaderCar.value = isHeader;
    if (icon) {
      activeIcon.value = icon;
    }
  });

  function selectIcon(data) {
    activeIcon.value = data.id;
  }

  function handleClose() {
    emit('closeDialog');
  }

  async function illusion() {
    const { info } = props;
    const { updateVehicle, vehicleList } = vehicleStore;
    const headerIndex = vehicleList.findIndex((i) => i.isHeader);
    if (unref(isHeaderCar) && headerIndex !== -1 && !info.isHeader) {
      const res = await MessageBox({
        offset: ['45%', '20%'],
        text: t('resource.vehicle.isReplaceVehicle'),
        type: 'ok',
      });
      if (!res) {
        isHeaderCar.value = false;
      }
    }
    const vehicle = { ...info, icon: unref(activeIcon), isHeader: unref(isHeaderCar) };
    updateVehicle(vehicle);
    emit('closeDialog');
  }

  async function cancelIllusion() {
    const res = await MessageBox({
      offset: ['45%', '20%'],
      text: t('resource.vehicle.isCancelVehicle'),
      type: 'ok',
    });
    if (res) {
      vehicleStore.delVehicleById(props.info.vehicleId);
      emit('closeDialog');
    }
  }
</script>

<template>
  <TdFrameBox
    class="edit-vehicle-illusion"
    :dragger="true"
    :title="t('resource.resourceTab.vehicle')"
    @close-frame-box="handleClose"
  >
    <div class="title">{{ t('resource.resourceTab.vehicleIcon') }}</div>
    <div class="icon-list">
      <div
        v-for="item in iconList"
        :key="item"
        class="icon"
        :class="{ active: item.id === activeIcon }"
        @click="selectIcon(item)"
      >
        <img alt="" :src="item.iconInfo" />
      </div>
    </div>
    <div class="set-header">
      <span>{{ t('resource.vehicle.isSetHeadCar') }}</span>
      <ElSwitch v-model="isHeaderCar" />
    </div>
    <div class="footer">
      <TdButton
        v-if="info.vehicleId"
        :text="t('resource.vehicle.vehicleCancel')"
        type="normal"
        @click="cancelIllusion"
      />
      <TdButton :text="t('resource.vehicle.vehicleIcon')" type="normal" @click="illusion" />
    </div>
  </TdFrameBox>
</template>

<style scoped lang="less">
  .edit-vehicle-illusion {
    :deep(.frame-box-container) {
      padding: 10px !important;
    }

    .title {
      position: relative;
      padding-left: 6px;
      margin-bottom: 12px;

      &::before {
        position: absolute;
        top: 50%;
        left: 0;
        width: 4px;
        height: 14px;
        margin-top: -7px;
        content: '';
        background: rgb(26 188 157 / 100%);
      }
    }

    .icon-list {
      display: flex;
      width: 100%;
      padding-bottom: 8px;
      margin-bottom: 4px;
      overflow: auto hidden;

      .icon {
        display: flex;
        flex-shrink: 0;
        align-items: center;
        justify-content: center;
        width: 48px;
        height: 48px;
        margin-right: 16px;
        cursor: pointer;

        img {
          width: 18px;
          height: 36px;
        }
      }

      .active {
        background: rgb(173 204 240 / 7%);
        backdrop-filter: blur(20px);
        border: 1px solid rgb(255 255 255 / 20%);
      }
    }

    .set-header {
      margin-bottom: 12px;

      span {
        margin-right: 4px;
      }
    }

    .footer {
      display: flex;
      justify-content: space-between;
      margin-top: 10px;
      text-align: center;

      .td-button {
        flex: 1;

        &:nth-of-type(2) {
          margin-left: 6px;
        }
      }
    }

    :deep(.el-switch.is-checked .el-switch__core) {
      background: rgb(26 255 251 / 100%);
      border: 1px solid rgb(26 255 251 / 100%);
    }
  }
</style>
