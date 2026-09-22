<script setup lang="ts">
  import { computed } from 'vue';

  import ResourceAbility from '@/pages/communicationCard/resourceAbilityButton/resourceAbility.vue';
  import { getResourceAbilities, getResourceType } from '@/pages/resource/resourceHelper';

  import Draggable from 'vuedraggable';

  const props = defineProps<{
    canDrag?: boolean; // 可拖拽
    isMain?: boolean;
    nodeData: any;
    operation?: boolean; // 显示操作按钮
    showCollect?: boolean; // 显示收藏按钮
  }>();
  const emit = defineEmits(['click', 'dblclick']);

  const node = computed(() => {
    const { nodeData } = props;
    const { code, name } = nodeData;
    return {
      ...nodeData,
      label: code ? `${name}(${code})` : name,
      resourceType: getResourceType(nodeData),
    };
  });

  function handleNodeClick(data) {
    emit('click', data, data.resourceType);
  }

  function handleNodeDblclick(data) {
    emit('dblclick', data, data.resourceType);
  }
</script>

<template>
  <div class="tree-node">
    <Draggable
      chosen-class="chosen-class"
      class="node-info"
      :data-info="JSON.stringify(node)"
      :disabled="!canDrag"
      drag-class="drag-class"
      :force-fallback="true"
      ghost-class="ghost-class"
      :group="{ name: 'bigDrag', pull: 'clone', put: false, sort: false }"
      item-key="id"
      :list="[node]"
      @click="handleNodeClick(nodeData)"
      @dblclick="handleNodeDblclick(nodeData)"
    >
      <template #item="{ element }">
        <div class="drag-box">
          <TdTooltip v-if="element.children" :content="element.name">
            <span class="name">{{ element.name }}</span>
          </TdTooltip>
          <template v-else>
            <TdAvatar :info="element" />
            <TdTooltip :content="`${element.label}`">
              <span class="name">{{ element.label }}</span>
            </TdTooltip>
          </template>
        </div>
      </template>
    </Draggable>

    <ResourceAbility
      v-if="!nodeData.children && (operation || isMain)"
      :ability-data="nodeData"
      :ability-values="getResourceAbilities(nodeData, showCollect)"
      btn-type="icon"
      class="operation"
      :drop-menu="true"
      :is-main="isMain"
      :resource-type="node.resourceType"
      size="auto"
    />
  </div>
</template>

<style scoped lang="less">
  @import url('@/styles/mixin.less');

  .tree-node {
    position: relative;
    display: flex;
    flex: 1;
    align-items: center;
    height: 28px;
    cursor: pointer;

    .drag-box {
      display: flex;
    }

    .checkbox {
      margin-right: 4px;
    }

    .node-info {
      display: flex;
      align-items: center;
    }

    .name {
      .ellipsis1();

      display: inline-block;
      width: 220px;
      height: 22px;
      margin-left: 4px;
      font-size: 14px;
      line-height: 22px;
    }

    .operation {
      position: absolute;
      right: 8px;
      display: none;
    }
  }
</style>
