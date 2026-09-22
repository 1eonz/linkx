<script setup lang="ts">
  import { onBeforeUnmount, onMounted, ref, unref, useAttrs, watch } from 'vue';

  import { CategoryEnum } from '@/enums';
  import { getResourceTypes } from '@/pages/resource/resourceHelper';

  import Tree from './tree.vue';

  const props = defineProps<{
    active: number;
    canDrag?: boolean;
    data: any[];
    keywords: string;
    operation?: boolean;
  }>();

  const emit = defineEmits(['click', 'checkChange']);
  const attrs = useAttrs();
  const treeComponents = ref<any[]>(getResourceTypes());
  const checkKeys = ref<string[]>([]);

  watch(
    () => props.data,
    (val) => {
      // 清理残留数据
      treeComponents.value.forEach((element) => {
        element.data = [];
      });
      if (val[0]) {
        init(val[0]);
      }
    },
    { deep: true },
  );

  onMounted(() => {
    if (props.data[0]) {
      init(props.data[0]);
    }
  });

  onBeforeUnmount(() => {});

  function init(val) {
    treeComponents.value = treeComponents.value.filter((item) => {
      return (
        item.id !== CategoryEnum.person &&
        item.id !== CategoryEnum.seat &&
        item.id !== CategoryEnum.confTerminal &&
        item.id !== 'thirdEquipment'
      );
    });
    treeComponents.value.forEach((element) => {
      if (val[element.id]) {
        // 性能优化,前端限制条数到4000
        element.data = val[element.id]?.slice(0, 4000);
        element.data.forEach((data) => {
          if (checkKeys.value.includes(data.id)) {
            data.checked = true;
          }
        });
      }
    });
  }

  function handleTreeClick(data, active) {
    emit('click', data, active);
  }

  function checkChange(checks, origin) {
    const keys: string[] = [];
    checks?.forEach((item) => {
      const { id } = item;
      keys.push(id);
      const index = unref(checkKeys).indexOf(id);
      if (index === -1) {
        checkKeys.value.push(id);
      }
    });

    const cancelCheckKeys: string[] = [];
    origin?.forEach((item) => {
      const { id } = item;
      if (keys.includes(id)) return;
      const index = unref(checkKeys).indexOf(id);
      if (index !== -1) {
        cancelCheckKeys.push(id);
        checkKeys.value.splice(index, 1);
      }
    });

    emit('checkChange', checks, cancelCheckKeys);
  }
</script>

<template>
  <div v-if="treeComponents.length > 0" class="tree-body">
    <template v-for="item in treeComponents" :key="item.id">
      <Tree
        v-bind="attrs"
        :active="active"
        :can-drag="canDrag"
        :info-data="item"
        :keywords="keywords"
        :operation="operation"
        @check-change="checkChange"
        @click="handleTreeClick"
      />
    </template>
  </div>
  <TdEmpty v-else />
</template>

<style scoped lang="less">
  .tree-body {
    padding: 0 10px;
    overflow: hidden auto;
  }
</style>
