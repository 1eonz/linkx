<script setup lang="ts">
  import { ref } from 'vue';

  import simpleMarkerImg from '@/assets/images/marker/simple_marker.png';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { useEmitter, useI18n } from '@/hooks';
  import CollectPopup from '@/pages/map/popup/collectPopup.vue';
  import AddressTree from '@/pages/tree/addressTree/index.vue';
  import { mapManager } from '@/plugins/map';
  import { useAddressStore } from '@/store';

  const emit = defineEmits(['close']);
  const { t } = useI18n();
  const addressStore = useAddressStore();
  const mapId = ref('mapId_main');
  const keywords = ref('');
  const formData = ref<any>({});
  const activeCid = ref('');
  const showBox = ref(true);
  const cid = ref('');
  const state = ref(1);
  const activePosition = ref<any[]>([]);

  function handleCreateType() {
    useEmitter().emit('createAddressType');
  }

  function handleClick(data, active) {
    const map = mapManager.get(mapId.value);
    const position = data.location.split(',');
    if (active) {
      activePosition.value = position;
      const maxZoom = Number(appConfig.settingData.MAX_ZOOM);
      const zoom = Math.min(maxZoom, 17);
      map.setCenter(position, zoom);
      map.addAddressMarker('look', simpleMarkerImg, { position });
    } else {
      activePosition.value = [];
      map.deleteMarker(['look']);
    }
  }

  function handleUpdate(data) {
    showBox.value = false;
    state.value = 2;
    formData.value = data;
  }

  function handleStart() {
    if (activePosition.value.length === 2) {
      handleClose(false);
    } else {
      Message(t('planSafety.selectPoint'));
    }
  }
  function handleAdd() {
    showBox.value = false;
    state.value = 1;
    const map = mapManager.get(mapId.value);
    map.deleteMarker(['look']);
    map.addAddressMarker('add', simpleMarkerImg, {
      callback: (location, address) => {
        formData.value.name = address;
        formData.value.address = address;
        formData.value.location = location;
        cid.value = location;
      },
    });
  }
  function handleClose(isButtonClose) {
    const map = mapManager.get(mapId.value);
    map.deleteMarker(['add', 'look']);
    Dialog(activeCid.value)?.close();
    if (isButtonClose) {
      emit('close');
    } else {
      emit('close', activePosition.value);
    }
  }

  function handleCancel(data, active) {
    const map = mapManager.get(mapId.value);
    addressStore.delAddressData(data.id);
    if (active === data.id) {
      map.deleteMarker(['look']);
    }
  }

  function handleCloseCollect() {
    showBox.value = true;
    formData.value = {};
  }
</script>

<template>
  <TdFrameBox
    v-if="showBox"
    class="address-manage position"
    :title="t('resource.resourceTab.pointCollection')"
    @close-frame-box="handleClose(true)"
  >
    <div class="title">
      <TdInput
        v-model="keywords"
        :placeholder="t('videoControl.createEvent.enterName')"
        type="searchInput"
      />
      <TdButton :text="t('common.create')" type="normal" @click="handleAdd" />
    </div>
    <AddressTree
      :keywords="keywords"
      @cancel="handleCancel"
      @click="handleClick"
      @update="handleUpdate"
    />
    <div class="btn">
      <TdButton
        :text="t('monitor.monitorFunction.createGroup')"
        type="normal"
        @click="handleCreateType"
      />
      <TdButton
        v-if="false"
        :text="t('resource.resourceTab.circleControl')"
        type="normal"
        @click="handleStart"
      />
    </div>
  </TdFrameBox>

  <CollectPopup
    v-else
    :cid="cid"
    class="position"
    :data="formData"
    :state="state"
    @close="handleCloseCollect"
  />
</template>

<style scoped lang="less">
  .position {
    position: fixed;
    top: 80px;
    right: 96px;
  }

  .address-manage {
    z-index: 1210;
    height: calc(100vh - 120px);

    :deep(.frame-box-container) {
      display: flex;
      flex-direction: column;
      padding: 0;
    }

    .title {
      display: flex;
      padding: 16px 10px 10px;

      .td-input {
        margin-right: 4px;
      }
    }

    .btn {
      display: flex;
      justify-content: space-between;
      padding: 10px;

      .td-button {
        width: 100%;
      }
    }
  }
</style>
