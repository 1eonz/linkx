<script setup lang="ts">
  import { computed, onBeforeUnmount, onMounted, ref, unref, useAttrs, watch } from 'vue';
  import { useRoute } from 'vue-router';

  import { queryAlternativeByPage } from '@/api/plan';
  import { appConfig } from '@/config';
  import { CategoryEnum } from '@/enums';
  import { useEmitter, useSetInterval } from '@/hooks';
  import { useResourceStore } from '@/store';

  import Tree from './tree.vue';

  const props = defineProps<{
    activeCount?: string;
    defaultCheckKeys?: string[];
    filterText?: string; // 搜索关键字
    from?: number; // 来源
    iccOnly?: boolean;
    showResource?: any[];
  }>();
  const emit = defineEmits(['click', 'dblclick', 'checkChange']);
  defineExpose({ setCheckedKeys });

  const attrs = useAttrs();
  const resourceStore = useResourceStore();
  const { setMapChooseData } = useResourceStore();
  const route = useRoute();

  let clearTimer: any = null;
  const treeComponents = ref<any[]>([]);
  const treeRef = ref();
  const checkKeys = ref<string[]>([]);
  const checkDataMap = new Map();
  const orgId = ref('');

  const showEmpty = computed(() => {
    return treeComponents.value.length === 0;
  });
  const filterText = computed(() => props.filterText);

  watch(
    () => props.defaultCheckKeys,
    (val) => {
      if (val) {
        checkKeys.value = [...val];
        for (const key of checkDataMap.keys()) {
          if (!val.includes(key)) {
            checkDataMap.delete(key);
          }
        }
      }
    },
    { immediate: true },
  );
  watch(filterText, (val) => {
    if (val && val !== '') {
      treeComponents.value = [];
      getLeafs();
    }
  });
  watch(route, () => {
    setMapChooseData([]);
  });

  useEmitter('planChange', updateLeafs);

  onMounted(() => {
    init();
  });

  onBeforeUnmount(() => {
    clearTimer?.();
  });

  function init() {
    const { showResource } = props;
    if (showResource) {
      treeComponents.value.forEach((item) => {
        if (item.show) {
          item.show = showResource.includes(item.type);
        }
      });
    }
    updateLeafs();
  }

  function updateLeafs() {
    clearTimer?.();
    clearTimer = useSetInterval(getLeafs, 15 * 1000, true);
  }

  function mapWatch(id) {
    orgId.value = id;
  }

  async function getLeafs() {
    const { id } = appConfig.userData;
    const param = {
      executorId: id,
      pageNum: 1,
      pageSize: 999,
      searchParam: props.filterText,
    };
    const { code, data } = await queryAlternativeByPage(param);
    if (code === 0) {
      const newArr: any[] = [];
      data.records.forEach(({ equipments, id, name }) => {
        if (resourceStore.mapChoosePlanId === id) {
          setMapChooseData(
            equipments.map((i) => i.id),
            id,
          );
        }

        newArr.push({
          data: equipments,
          id,
          name,
          show: true,
        });
      });
      treeComponents.value = newArr;
    }
  }

  function checkChange(category, checks, origin) {
    const keys: string[] = [];
    checks.forEach((item) => {
      const { id } = item;
      keys.push(id);
      const index = unref(checkKeys).indexOf(id);
      if (index === -1) {
        checkKeys.value.push(id);
        checkDataMap.set(id, { ...unref(item), category });
      }
    });
    const cancelCheckKeys: string[] = [];
    origin.forEach((item) => {
      const { id } = item;
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

  // 设置目前选中的节点
  function setCheckedKeys(data) {
    const obj: any = {};
    unref(treeComponents).forEach((item) => {
      obj[item.id] = [];
    });

    checkDataMap.clear();
    const keys = data.map((item) => {
      if (item.resourceType === 'Executor') {
        item.category = CategoryEnum.person;
      }
      checkDataMap.set(item.id, item);
      return item.id;
    });
    checkKeys.value = keys;

    for (const data of checkDataMap.values()) {
      const { category } = data;
      obj[category].push(data);
    }
    Object.keys(obj).forEach((key) => {
      emit('checkChange', key, obj[key]);
    });
  }
</script>

<template>
  <div class="tree-content">
    <template v-for="item in treeComponents" :key="item.id">
      <Tree
        v-if="item.show"
        v-show="!showEmpty"
        v-bind="attrs"
        ref="treeRef"
        :active-count="activeCount"
        :check-keys="checkKeys"
        :from="from"
        :org-id="orgId"
        :resource-info="item"
        @check-change="checkChange"
        @click="handleTreeClick"
        @dblclick="handleTreeDblclick"
        @map-watch="mapWatch"
      />
    </template>

    <TdEmpty v-if="showEmpty" />
  </div>
</template>

<style scoped lang="less">
  .tree-content {
    flex: 1;
    overflow-y: scroll;
  }
</style>
