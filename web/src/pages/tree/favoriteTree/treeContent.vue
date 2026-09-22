<script setup lang="ts">
  import { onActivated, ref, unref, useAttrs, watch } from 'vue';

  import { CategoryEnum } from '@/enums';
  import { getResourceTypes } from '@/pages/resource/resourceHelper';

  import Tree from './tree.vue';

  const props = defineProps<{
    defaultCheckKeys?: string[];
    navId: string;
    showResource?: any[];
  }>();
  const emit = defineEmits(['click', 'dblclick', 'checkChange']);

  const attrs = useAttrs();
  const treeComponents = getResourceTypes();

  const checkKeys = ref<string[]>([]);
  const checkDataMap = new Map();

  watch(
    () => props.defaultCheckKeys,
    () => {
      initCheckKeys();
    },
    { immediate: true },
  );

  onActivated(() => {
    initCheckKeys();
  });

  function showFavorite(item) {
    if (props.navId === 'all') {
      return item.show && item.id !== 'thirdEquipment';
    } else if (CategoryEnum.recorder === Number(props.navId)) {
      return (
        [CategoryEnum.GBRecorder, CategoryEnum.recorder].includes(Number(item.id)) && item.show
      );
    } else if (props.navId === 'thirdEquipment') {
      return [CategoryEnum.confTerminal, CategoryEnum.uav].includes(Number(item.id)) && item.show;
    } else {
      return item.id === props.navId && item.show;
    }
  }
  function initCheckKeys() {
    const keys = props.defaultCheckKeys || [];
    checkKeys.value = [...keys];
    for (const key of checkDataMap.keys()) {
      if (!keys.includes(key)) {
        checkDataMap.delete(key);
      }
    }
  }

  function checkChange(category, checks, origin) {
    const keys: string[] = [];
    checks.forEach((item) => {
      const id = item.resourceId;
      keys.push(id);
      const index = unref(checkKeys).indexOf(id);
      if (index === -1) {
        checkKeys.value.push(id);
        checkDataMap.set(id, { ...unref(item), category, id });
      }
    });
    const cancelCheckKeys: string[] = [];
    origin.forEach((item) => {
      const id = item.resourceId;
      if (keys.includes(id)) return;
      const index = unref(checkKeys).indexOf(id);
      if (index !== -1) {
        cancelCheckKeys.push(id);
        checkKeys.value.splice(index, 1);
        checkDataMap.delete(id);
      }
    });

    const ret: any[] = [];
    for (const value of checkDataMap.values()) {
      if (Number(value.category) === Number(category)) {
        ret.push(value);
      }
    }
    emit('checkChange', category, ret, cancelCheckKeys);
  }

  function handleTreeClick(data, type) {
    emit('click', data, type);
  }

  function handleTreeDblclick(data, type) {
    emit('dblclick', data, type);
  }

  function handleTotalChange(data) {
    unref(treeComponents).forEach((item) => {
      if (item.id === data.category) {
        item.total = data.total;
      }
    });
  }
</script>

<template>
  <div class="tree-content">
    <template v-for="item in treeComponents" :key="item.id">
      <Tree
        v-if="showFavorite(item)"
        v-bind="attrs"
        :category="Number(item.id)"
        :check-keys="checkKeys"
        :title="item.name"
        @check-change="checkChange"
        @click="handleTreeClick"
        @dblclick="handleTreeDblclick"
        @total-change="handleTotalChange"
      />
    </template>
  </div>
</template>

<style scoped lang="less">
  .tree-content {
    width: 100%;
    padding: 10px;
    overflow-y: scroll;
  }
</style>
