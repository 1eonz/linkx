<script setup lang="ts">
  import { computed, onMounted, ref, unref, watch } from 'vue';

  import { Message } from '@/components/Message';
  import { useI18n } from '@/hooks';
  import AddMonitorEquipmentPersonsCard from '@/pages/resource/launchResourceSelector.vue';
  import { getResourceTypes } from '@/pages/resource/resourceHelper';
  import { useResourceStore, useVehicleStore } from '@/store';

  import { cloneDeep } from 'lodash-es';

  const emit = defineEmits(['closeDialog', 'confirm']);
  const vehicleStore = useVehicleStore();
  const resourceStore = useResourceStore();
  const { t } = useI18n();
  const chooseList = ref<any[]>([]);
  const showPopup = ref(true);
  const headerCar = ref<any>({});

  watch(
    chooseList,
    () => {
      chooseList.value.forEach((item) => {
        item.data.forEach((i) => {
          if (!i.icon) {
            i.icon = resourceStore.vehicleIcon[0]?.id;
          }
        });
      });
    },
    {
      deep: true,
      immediate: true,
    },
  );

  const showResource = computed(() => {
    const arr: string[] = [];
    getResourceTypes().forEach((i: any) => {
      if (
        (i.show && vehicleStore.vehicleEquipmentType.includes(i.id)) ||
        i.id === 'thirdEquipment'
      ) {
        arr.push(i.type);
      }
    });
    return arr;
  });
  const menuOptions = computed(() => {
    return resourceStore.vehicleIcon.map((i) => {
      return {
        label: '',
        url: i.iconInfo,
        value: i.id,
      };
    });
  });

  onMounted(() => {
    initList();
  });

  function initList() {
    const arr = getResourceTypes();
    const obj = {};
    cloneDeep(vehicleStore.vehicleList).forEach((item) => {
      if (!obj[item.category]) {
        obj[item.category] = [];
      }
      obj[item.category].push(item);
      if (item.isHeader) {
        headerCar.value = item;
      }
    });
    arr.forEach((item) => {
      if (obj[item.id]) {
        item.data = obj[item.id];
      }
    });
    chooseList.value = arr;
  }

  function handleClose() {
    emit('closeDialog');
  }

  function setHeader(data?) {
    headerCar.value = data || {};
  }

  function handleCancel() {
    handleClose();
  }

  async function handleConfirm() {
    const list: any[] = [];
    unref(chooseList).forEach((item: any) => {
      item.data.forEach((i) => {
        list.push({ ...i, icon: i.icon, isHeader: i.id === unref(headerCar).id });
      });
    });
    if (list.length <= 0) {
      Message(t('common.tdcomp.chooseNoData'));
      return;
    }
    vehicleStore.addVehicleList(list);
    emit('confirm', list);
    handleClose();
  }

  function iconSelect(data, val) {
    chooseList.value.forEach((item: any) => {
      item.data.forEach((i) => {
        if (i.id === data.id) {
          i.icon = val;
        }
      });
    });
  }
</script>

<template>
  <TdFrameBox
    v-show="showPopup"
    class="add-vehicle-illusion"
    :dragger="true"
    size="normal"
    :title="t('common.createVehicle')"
    @close-frame-box="handleClose"
  >
    <AddMonitorEquipmentPersonsCard
      v-model:choose-list="chooseList"
      :add-type="1"
      :box-select="true"
      class="select-resource"
      :default-choose-list="chooseList"
      :show-resource="showResource"
      @choose-from-map="showPopup = true"
      @hide-frame="showPopup = false"
    >
      <template #ability="data">
        <div class="btn">
          <TdDropdownMenu :options="menuOptions" @click="(val) => iconSelect(data, val)">
            <div class="drop-menu-inner">
              <img :src="resourceStore.getIcon[data.icon]" />
              <Icon name="drop_down" />
            </div>
          </TdDropdownMenu>

          <span v-if="headerCar.id === data.id" class="set-header" @click="setHeader()">
            {{ t('resource.vehicle.headCarCancel') }}
          </span>
          <span v-else class="set-header" @click="setHeader(data)">{{
            t('resource.vehicle.setHeadCar')
          }}</span>
        </div>
      </template>
    </AddMonitorEquipmentPersonsCard>

    <div class="footer">
      <TdButton :text="t('common.cancel')" type="normal" @click="handleCancel" />
      <TdButton :text="t('common.determine')" type="normal" @click="handleConfirm" />
    </div>
  </TdFrameBox>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .add-vehicle-illusion {
    :deep(.frame-box-container) {
      padding: 10px;
    }

    :deep(.choose-from-list) {
      .item-info .name {
        max-width: 120px;
        .ellipsis1();
      }

      .btn {
        display: flex;
        align-items: center;

        .drop-menu-inner {
          position: relative;
          display: flex;
          align-items: center;
          padding-right: 4px;
          margin-right: 4px;

          &::after {
            position: absolute;
            top: 3px;
            right: 0;
            width: 1px;
            height: 12px;
            content: '';
            background: rgb(153 206 251 / 100%);
          }

          img {
            width: 8px;
            height: 16px;
            margin-right: 4px;
          }
        }

        .set-header {
          position: relative;
          padding-right: 4px;
          margin-right: 4px;
          font-size: 12px;
          font-weight: 400;
          cursor: pointer;

          &::after {
            position: absolute;
            top: 3px;
            right: 0;
            width: 1px;
            height: 12px;
            content: '';
            background: rgb(153 206 251 / 100%);
          }
        }

        .td-icon {
          cursor: pointer;
        }
      }
    }

    :deep(.select-resource) {
      .operation {
        opacity: 1 !important;
      }
    }

    .footer {
      display: flex;
      align-items: center;
      justify-content: flex-end;
      margin-top: 10px;

      .td-button {
        width: 60px;
        height: 32px;
        margin-left: 10px;
      }
    }
  }
</style>
