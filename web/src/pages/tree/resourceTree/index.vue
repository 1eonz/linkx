<script setup lang="ts">
  import { ref, useAttrs } from 'vue';

  import { CategoryEnum } from '@/enums';

  import TreeNav from '../common/treeNav.vue';
  import PersonnelOrganization from '../personnelOrganization/index.vue';
  import TreeContent from './treeContent.vue';

  const props = defineProps<{
    showPim?: boolean;
    showResource?: any[]; // 展示的资源类型
  }>();
  const emit = defineEmits(['click', 'dblclick', 'checkChange', 'navChange']);
  defineExpose({ setCheckedKeys });
  const attrs = useAttrs();
  const navId = ref<number>(props.showPim ? CategoryEnum.person : 0);
  const contentRef = ref();

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

  function handleNavChange(val, data) {
    emit('navChange', val, data);
  }

  function setCheckedKeys(keys) {
    contentRef.value.setCheckedKeys(keys);
  }
</script>

<template>
  <div class="resource-tree">
    <div class="tree-body">
      <TreeNav :show-pim="showPim" :show-resource="showResource" @click="handleNav" />
      <KeepAlive>
        <PersonnelOrganization
          v-show="navId * 1 === CategoryEnum.person"
          v-bind="attrs"
          @check-change="checkChange"
          @click="handleClick"
          @dblclick="handleDblclick"
        />
      </KeepAlive>
      <KeepAlive>
        <TreeContent
          v-show="navId * 1 !== CategoryEnum.person"
          v-bind="attrs"
          ref="contentRef"
          :nav-id="navId"
          :show-pim="showPim"
          :show-resource="showResource"
          @check-change="checkChange"
          @click="handleClick"
          @dblclick="handleDblclick"
          @nav-change="handleNavChange"
        />
      </KeepAlive>
    </div>
  </div>
</template>

<style scoped lang="less">
  // 宽高由外部框决定
  .resource-tree {
    flex: 1;
    width: 100%;
    height: 0;

    .tree-body {
      display: flex;
      height: 100%;
    }
  }
</style>
