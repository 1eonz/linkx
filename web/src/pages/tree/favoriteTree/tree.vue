<script lang="ts" setup>
  import { computed, ref, unref, watch } from 'vue';

  import ResourceAbility from '@/pages/communicationCard/resourceAbilityButton/resourceAbility.vue';
  import {
    getOnlineStatus,
    getResourceAbilities,
    getResourceAvatar,
    getResourceType,
  } from '@/pages/resource/resourceHelper';
  import { useFavoriteStore } from '@/store';

  import Draggable from 'vuedraggable';

  const props = defineProps<{
    canDrag?: boolean;
    category: number;
    checkKeys: string[];
    operation?: boolean;
    showCollect?: boolean;
    title: string;
  }>();
  const emit = defineEmits(['checkChange', 'click', 'dblclick', 'totalChange']);

  const favoriteStore = useFavoriteStore();

  const nodeList = ref<any[]>([]);
  const expand = ref(false);
  const checkAll = ref(false);
  const isIndeterminate = ref(false);

  const onlineCount = computed(() => {
    let online = 0;
    const total = unref(nodeList).length;
    unref(nodeList).forEach((item) => {
      if (getOnlineStatus(item) === 'online') {
        online++;
      }
    });
    return `${online}/${total}`;
  });

  watch(
    nodeList,
    (val) => {
      const checkLen = val.filter((item) => item.check).length;
      const len = val.length;
      checkAll.value = len !== 0 && checkLen === len;
      isIndeterminate.value = checkLen > 0 && checkLen < len;
    },
    { deep: true },
  );
  watch(
    () => props.checkKeys,
    () => {
      setCheck();
    },
  );
  watch(
    () => favoriteStore.favoriteResources,
    () => {
      getNodeList();
    },
    { deep: true, immediate: true },
  );

  async function getNodeList() {
    const { category } = props;
    nodeList.value = (favoriteStore.favoriteResources?.[category] || []).map((item) => {
      return {
        ...item,
        check: props.checkKeys.includes(item.resourceId),
        resourceType: getResourceType(item),
      };
    });
  }

  async function setCheck() {
    nodeList.value.forEach((item) => {
      item.check = props.checkKeys.includes(item.resourceId);
    });
  }

  function handleNodeClick(data) {
    emit('click', data, data.resourceType);
  }

  function handleNodeDblclick(data) {
    emit('dblclick', data, data.resourceType);
  }

  function handleCheckAllChange(check) {
    if (unref(nodeList).length === 0) {
      checkAll.value = false;
      return;
    }
    nodeList.value.forEach((item) => (item.check = check));
    handleCheckChange();
  }

  function handleCheckChange(val?, item?) {
    if (item) {
      item.check = val;
    }
    const checks = unref(nodeList).filter((item) => item.check);
    emit('checkChange', props.category, checks, unref(nodeList));
  }

  function handleExpand() {
    expand.value = !unref(expand);
  }
</script>

<template>
  <div class="favorite-tree">
    <div class="header" @click.stop="handleExpand">
      <Icon class="icon" :class="{ expand }" name="node_expand" />
      <TdCheckbox
        v-model="checkAll"
        class="checkbox"
        :indeterminate="isIndeterminate"
        @change="handleCheckAllChange"
      />
      <span>{{ `${title}(${onlineCount})` }}</span>
    </div>
    <div v-show="expand" class="tree-node-list">
      <div v-for="item in nodeList" :key="item.id" class="tree-node">
        <TdCheckbox v-model="item.check" class="checkbox" @change="handleCheckChange" />
        <div class="avatar" :class="getOnlineStatus(item)">
          <Icon class="icon" :name="getResourceAvatar(item)" prefix="tree" />
        </div>

        <TdTooltip :content="`${item.name}(${item.code})`">
          <Draggable
            class="node-info"
            :data-info="JSON.stringify(item)"
            :disabled="!canDrag"
            :group="{ name: 'bigDrag', pull: 'clone', put: false, sort: false }"
            item-key="id"
            :list="[item]"
            @click.stop="handleNodeClick(item)"
            @dblclick.stop="handleNodeDblclick(item)"
          >
            <template #item>
              <span class="name">{{ item.name }}({{ item.code }})</span>
            </template>
          </Draggable>
        </TdTooltip>

        <ResourceAbility
          v-if="operation"
          :ability-data="{
            ...item,
            id: item.resourceType === 'Executor' ? item.executorId : item.resourceId,
          }"
          :ability-values="getResourceAbilities(item, showCollect)"
          btn-type="icon"
          class="operation"
          :drop-menu="true"
          :resource-type="item.resourceType"
          size="auto"
        />
      </div>

      <TdEmpty v-if="nodeList.length === 0" />
    </div>
  </div>
</template>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  .favorite-tree {
    .header {
      display: flex;
      align-items: center;
      height: 28px;
      cursor: pointer;

      .icon {
        width: 10px;
        height: 6px;
        transform: rotate(-90deg);
      }

      .expand {
        transform: rotate(0deg);
      }

      .checkbox {
        margin: 0 4px 0 8px;
      }

      span {
        font-size: 14px;
        font-weight: 400;
      }
    }

    .tree-node-list {
      .tree-node {
        position: relative;
        display: flex;
        align-items: center;
        height: 28px;
        padding-left: 36px;
        cursor: pointer;

        .checkbox {
          margin-right: 4px;
        }

        .node-info {
          display: flex;
          align-items: center;

          .name {
            display: inline-block;
            width: 260px;
            height: 22px;
            margin-left: 4px;
            font-size: 14px;
            line-height: 22px;
            .ellipsis1();
          }
        }

        .operation {
          position: absolute;
          right: 0;
          display: none;
        }

        &:hover {
          background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);

          .operation {
            display: flex;
          }
        }
      }
    }
  }
</style>
