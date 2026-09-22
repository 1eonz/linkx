<script lang="ts" setup>
  import { onMounted, ref, unref } from 'vue';

  import { deleteRegion, queryRegionList } from '@/api/region';
  import { Message } from '@/components/Message';
  import { useEmitter, useI18n } from '@/hooks';
  import { mapIsReady, mapManager } from '@/plugins/map';
  import { useResourceStore } from '@/store';

  import CreateJurisdiction from './createJurisdiction.vue';
  import { drawJurisdiction, filterRegionLayer } from './helper';
  import JurisdictionListItem from './jurisdictionListItem.vue';

  const emit = defineEmits(['operateChange']);

  defineExpose({ reset });

  const { t } = useI18n();
  const resourceStore = useResourceStore();

  const operateType = ref('');
  const regionItem = ref({});
  const jurisdictionList = ref([]);

  useEmitter('mapTypeChange', (mapType) => {
    setTimeout(getRegionList, mapType === 'MineMap' ? 2000 : 1500);
  });

  onMounted(() => {
    getRegionList();
  });

  function handleCreateRegion() {
    operateChange('create');
  }

  function operateChange(data) {
    operateType.value = data;
    emit('operateChange', data);
  }

  function closeCreate() {
    operateChange('');
  }

  // 编辑自定义辖区
  function handleEdit(regionData) {
    operateChange('edit');
    regionItem.value = regionData;
  }

  // 获取自定义辖区列表及地图绘制辖区
  async function getRegionList() {
    const { code, data, msg } = await queryRegionList();
    if (code === 0) {
      jurisdictionList.value = data;
      delMapRegion();
      drawJurisdiction({ activeId: '', areaList: unref(jurisdictionList) });
    } else if (msg) {
      Message({ message: msg, type: 'warning' });
    }
  }

  // 删除自定义辖区
  async function handleDelete(id) {
    const { code } = await deleteRegion(id);
    if (code === 0) {
      getRegionList();
    }
  }

  async function delMapRegion() {
    await mapIsReady('mapId_main');
    const mapObj = mapManager.get('mapId_main');
    mapObj?.closeBoxToSelect();
    mapObj?.endDraw();
    mapObj?.deleteLayer(filterRegionLayer('region'));
  }

  function handleClick(activeId) {
    if (!resourceStore.layerChecked.includes('region')) {
      Message(t('resource.map.layerIsHidden'));
      return;
    }

    delMapRegion();
    drawJurisdiction({ activeId, areaList: unref(jurisdictionList) });
  }

  function reset() {
    closeCreate();
    getRegionList();
  }
</script>

<template>
  <div v-if="!operateType" class="jurisdiction-list">
    <div class="content">
      <template v-for="item in jurisdictionList" :key="item.id">
        <JurisdictionListItem
          :item="item"
          @handle-click="handleClick"
          @handle-delete="handleDelete"
          @handle-edit="handleEdit"
        />
      </template>
    </div>

    <TdButton
      class="btn"
      :text="t('resource.jurisdiction.createBtn')"
      @click="handleCreateRegion"
    />
  </div>
  <CreateJurisdiction
    v-else
    :jurisdiction-list="jurisdictionList"
    :operate-type="operateType"
    :region-item="regionItem"
    @reset="reset"
  />
</template>

<style lang="less" scoped>
  .jurisdiction-list {
    height: 100%;

    .content {
      height: calc(100% - 60px);
      overflow-y: auto;
    }

    .btn {
      width: 100%;
      height: 40px;
      margin-top: 10px;
    }
  }
</style>
