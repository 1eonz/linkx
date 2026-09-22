<script lang="ts" setup>
  import { ref, watch } from 'vue';

  import ResourceList from '@/pages/communicationCenter/resourceList.vue';
  import { useMapCenterStore } from '@/store';

  const showResource = ref(false);
  const mapCenterStore = useMapCenterStore();

  watch(
    () => showResource.value,
    (val) => {
      mapCenterStore.setShowResource(val);
    },
  );

  function changeShowResource() {
    showResource.value = !showResource.value;
  }
</script>

<template>
  <div class="all-resource-list-box">
    <ResourceList
      v-if="showResource"
      class="all-resource-list"
      :show-collect="true"
      @close-dialog="changeShowResource"
    />
    <div v-show="!showResource" class="btn" @click.stop="changeShowResource">
      <Icon name="resource_list" prefix="bigScreen" />
    </div>
  </div>
</template>

<style lang="less" scoped>
  .all-resource-list-box {
    position: relative;

    .all-resource-list {
      position: absolute;
      top: 94px;
      left: 20px;
      z-index: 4;
      box-sizing: border-box;
      width: 412px;
      height: calc(100vh - 120px);

      :deep(.resource-box) {
        height: calc(100% - 190px);
      }
    }

    .btn {
      position: absolute;
      top: 94px;
      left: 16px;
      cursor: pointer;

      :deep(.td-icon) {
        width: 60px;
        height: 60px;
      }
    }
  }
</style>
