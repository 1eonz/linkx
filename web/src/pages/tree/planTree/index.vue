<script setup lang="ts">
  import { ref, useAttrs } from 'vue';

  import TreeContent from './treeContent.vue';

  withDefaults(
    defineProps<{
      defaultCheckKeys?: string[]; // 默认选择项
      filterText?: string; // 搜索关键字
      from?: number; // 来源
      showResource?: any[]; // 展示的资源类型
    }>(),
    {
      filterText: '',
    },
  );
  const emit = defineEmits(['click', 'dblclick', 'checkChange']);
  defineExpose({ setCheckedKeys });

  const attrs = useAttrs();
  const contentRef = ref();

  function handleClick(data, type) {
    emit('click', data, type);
  }

  function handleDblclick(data, type) {
    emit('dblclick', data, type);
  }

  function checkChange(category, data, checkChange) {
    emit('checkChange', category, data, checkChange);
  }

  function setCheckedKeys(keys) {
    contentRef.value.setCheckedKeys(keys);
  }
</script>

<template>
  <div class="plan-tree">
    <div class="tree-body">
      <TreeContent
        v-bind="attrs"
        ref="contentRef"
        :default-check-keys="defaultCheckKeys"
        :filter-text="filterText"
        :from="from"
        :show-resource="showResource"
        @check-change="checkChange"
        @click="handleClick"
        @dblclick="handleDblclick"
      />
    </div>
  </div>
</template>

<style scoped lang="less">
  .plan-tree {
    flex: 1;
    height: 100%;

    .tree-body {
      display: flex;
      height: 100%;
    }
  }
</style>
