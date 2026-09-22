<script setup lang="ts">
  import { onBeforeUnmount, onMounted, ref, watch } from 'vue';

  import { useSetInterval } from '@/hooks';
  import ResourceAbility from '@/pages/communicationCard/resourceAbilityButton/resourceAbility.vue';
  import {
    getOnlineStatus,
    getResourceAbilities,
    getResourceAvatar,
    getResourceType,
  } from '@/pages/resource/resourceHelper';
  import { usePlanStore } from '@/store';

  const props = defineProps<{
    autoUpdate?: boolean; // 自动更新
    keywords: string;
    operation?: boolean;
  }>();

  const emit = defineEmits(['click', 'checkChange']);
  const planStore = usePlanStore();
  let clearTimer: any = null;
  const treeData = ref<any[]>([]);
  const treeRef = ref();
  const treeProps = {
    isLeaf: 'isLeaf',
    label: 'name',
    children: 'children',
  };

  watch(
    () => planStore.supportGroup,
    (val) => {
      treeData.value = val;
    },
  );

  watch(
    () => props.keywords,
    (val) => {
      // eslint-disable-next-line unicorn/no-array-callback-reference
      treeRef.value!.filter(val);
    },
  );

  watch(
    () => planStore.chooseSourcesList,
    (val) => {
      const checkKeys = val.map((item) => item.id);
      treeRef.value?.setCheckedKeys(checkKeys);
    },
    { deep: true },
  );

  const filterNode = (value, data) => {
    if (!value) return true;
    return data.name.includes(value);
  };

  onMounted(() => {
    init();
  });

  onBeforeUnmount(() => {
    clearTimer?.();
  });

  function init() {
    planStore.querySupportGroupData();
    if (props.autoUpdate) {
      updateTiming();
    }
  }

  // 定时刷新
  function updateTiming() {
    clearTimer?.();
    clearTimer = useSetInterval(() => {
      planStore.querySupportGroupData();
    }, 15 * 1000);
  }

  function handleNodeClick(data) {
    emit('click', data);
  }

  function handleCheckChange() {
    const checks = treeRef.value?.getCheckedNodes(true);
    emit('checkChange', checks);
  }
</script>

<template>
  <div class="tree-body">
    <ElTree
      ref="treeRef"
      :data="treeData"
      default-expand-all
      :filter-node-method="filterNode"
      node-key="id"
      :props="treeProps"
      show-checkbox
      @check-change="handleCheckChange"
      @node-click="handleNodeClick"
    >
      <template #default="{ data }">
        <div class="node-name" :class="{ 'parent-name': !data.isLeaf }">
          <div v-if="!data.isLeaf" class="tree-leaf">
            <span class="name">{{ data.name }}</span>
          </div>
          <div v-else class="tree-leaf">
            <div class="avatar" :class="getOnlineStatus(data)">
              <Icon class="icon" :name="getResourceAvatar(data)" prefix="tree" />
            </div>
            <TdTooltip :content="`${data.name}(${data.code})`">
              <span :class="getResourceAvatar(data) === 'tree_pdt' ? 'pdt-name' : 'name'">
                {{ data.name }}({{ data.code }})
              </span>
            </TdTooltip>
          </div>
        </div>

        <ResourceAbility
          v-if="data.isLeaf"
          :ability-data="data"
          :ability-values="getResourceAbilities(data)"
          btn-type="icon"
          class="operation-btn"
          :drop-menu="true"
          :resource-type="getResourceType(data)"
          size="auto"
        />
      </template>
      <template #empty>
        <TdEmpty class="empty" />
      </template>
    </ElTree>
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/tree.less';

  .tree-body {
    position: relative;
    padding: 0 10px;
    overflow: hidden auto;

    .node-name {
      overflow: visible;
    }

    .empty {
      height: 430px;
    }

    .operation-btn {
      position: absolute;
      right: 10px;
      opacity: 0;
    }

    .tree-leaf {
      display: flex;
    }

    .name {
      display: inline-block;
      width: 200px;
      height: 22px;
      font-size: 14px;
      line-height: 22px;
      .ellipsis1();
    }

    .pdt-name {
      width: 580px;
    }
  }

  :deep(.el-tree-node) {
    .el-tree-node__content {
      &:hover {
        background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);

        .operation-btn {
          opacity: 1;
        }
      }
    }
  }
</style>
