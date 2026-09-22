<script setup lang="ts">
  import { ref, useAttrs } from 'vue';

  import TreeNav from '../common/treeNav.vue';
  import TreeContent from './treeContent.vue';

  const emit = defineEmits(['click', 'dblclick', 'checkChange']);

  const attrs = useAttrs();
  const navId = ref('all');

  function handleNav(id) {
    navId.value = id;
  }

  function handleClick(data, type) {
    emit('click', data, type);
  }

  function handleDblclick(data, type) {
    emit('dblclick', data, type);
  }

  function checkChange(category, checks, cancelCheckKeys) {
    emit('checkChange', category, checks, cancelCheckKeys);
  }
</script>

<template>
  <div class="resource-tree">
    <div class="tree-body">
      <TreeNav @click="handleNav" />
      <TreeContent
        v-bind="attrs"
        :nav-id="navId"
        @check-change="checkChange"
        @click="handleClick"
        @dblclick="handleDblclick"
      />
    </div>
  </div>
</template>

<style scoped lang="less">
  .resource-tree {
    flex: 1;
    height: 0;

    .tree-body {
      display: flex;
      height: 100%;
    }
  }
</style>
