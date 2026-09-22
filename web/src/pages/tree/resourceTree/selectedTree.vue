<script setup lang="ts">
  import { computed, ref, unref } from 'vue';

  import { useEmitter, useI18n } from '@/hooks';
  import { getOnlineStatus } from '@/pages/resource/resourceHelper';
  import { useResourceStore, useTreeStore } from '@/store';

  import { cloneDeep } from 'lodash-es';

  const { t } = useI18n();
  const treeStore = useTreeStore();
  const resourceStore = useResourceStore();

  const defaultProps = {
    label: 'name',
    children: 'children',
  };

  const showTree = ref(false);

  const selectedList = computed(() => {
    const ret: any = [];
    Object.values(treeStore.resourceCheckedList).forEach((values) => {
      values.forEach((item) => {
        const { code, name } = item;
        ret.push({ ...item, check: true, leaf: true, name: code ? `${name}(${code})` : name });
      });
    });
    return ret;
  });
  const orgTreeData = computed(() => {
    const ret = cloneDeep(resourceStore.organization);
    const mapper = (data, parents: any = []) => {
      data.forEach((item) => {
        parents.forEach((p: any) => {
          if (!p.leafs) {
            p.leafs = [];
          }
          p.leafs.push(item.id);
        });

        if (item.children) {
          mapper(item.children, [...parents, item]);
        }
      });
    };
    mapper(ret);
    return ret;
  });
  const treeData = computed(() => {
    if (unref(selectedList).length === 0) {
      return [];
    }

    const org: any = {};
    unref(selectedList).forEach((item) => {
      const id = item.organizationId;
      if (!org[id]) {
        org[id] = [];
      }
      if (getOnlineStatus(item) === 'online') {
        org[id].unshift(item);
      } else {
        org[id].push(item);
      }
    });

    const mapper = (data) => {
      return data.filter((item) => {
        // 子节点资源不处理
        if (item.leaf) {
          return true;
        }

        const source = org[item.id] || [];
        if (source.length > 0) {
          if (!item.children) {
            item.children = [];
          }
          item.children.push(...source);
        }

        // 子节点资源包含当前组织下的资源
        const leafSource: any = [...source];
        item.leafs?.forEach((leaf) => {
          if (org[leaf]) {
            leafSource.push(...org[leaf]);
          }
        });

        let online = 0;
        const total = leafSource.length;
        if (total === 0) {
          return false;
        }

        leafSource.forEach((source) => {
          if (getOnlineStatus(source) === 'online') {
            online++;
          }
        });
        Object.assign(item, { online, total });

        if (item.children) {
          item.children = mapper(item.children);
        }

        return true;
      });
    };
    return mapper(cloneDeep(unref(orgTreeData)));
  });
  const onlineCount = computed(() => {
    let online = 0;
    unref(treeData).forEach((item) => {
      online += item.online;
    });
    return online;
  });

  useEmitter('closeSelectedTree', clickOutside);

  function handleExpand() {
    showTree.value = !unref(showTree);
  }

  function clickOutside() {
    showTree.value = false;
  }

  function handleCheckChange(data) {
    useEmitter().emit('removeCheckedList', data.id);
  }
</script>

<template>
  <div v-show="selectedList.length > 0" v-clickOutside="clickOutside" class="selected-tree">
    <div v-show="showTree" class="tree-wrapper" style="height: 548px; padding: 18px 10px">
      <ElTreeV2
        :data="treeData"
        default-expand-all
        :height="512"
        :item-size="38"
        :props="defaultProps"
      >
        <template #default="{ node, data }">
          <span v-if="data.leaf" class="custom-tree-node">
            <TdCheckbox
              v-model="data.check"
              class="checkbox"
              @change="() => handleCheckChange(data)"
            />
            <TdAvatar :info="data" />
            <TdTooltip :content="`${node.label}`">
              <span class="name">{{ node.label }}</span>
            </TdTooltip>
          </span>

          <span v-else class="custom-tree-node">
            <TdTooltip :content="`${node.label}`">
              <span class="name">{{ node.label }}({{ data.online }}/{{ data.total }})</span>
            </TdTooltip>
          </span>
        </template>
      </ElTreeV2>
    </div>

    <TdButton :active="showTree" class="expand-btn" @click.stop="handleExpand">
      <span>
        {{ t('resource.contact.haveChosen') }}：{{ onlineCount }}/{{ selectedList.length }}
      </span>
      <Icon name="list" />
    </TdButton>
  </div>
</template>

<style scoped lang="less">
  .selected-tree {
    position: relative;
    width: 100%;
    height: 32px;
    margin-bottom: 10px;

    .tree-wrapper {
      position: absolute;
      bottom: 42px;
      z-index: 100;
      width: 100%;
      background-color: rgb(6 41 74);
      border: 1px solid transparent;
      border-image: linear-gradient(
        180deg,
        rgba(26 255 251 / 20%) 0%,
        rgba(26 255 251 / 100%) 100%
      );
      border-image-slice: 1;
    }

    .expand-btn {
      position: absolute;
      bottom: 0;
      width: 100%;

      .td-icon {
        position: absolute;
        right: 10px;
        width: 16px;
        height: 16px;
      }
    }

    :deep(.el-tree-node__content),
    :deep(.el-tree-node) {
      min-height: 32px;
    }

    .custom-tree-node {
      display: flex;

      .avatar {
        margin: 0 4px;
      }
    }
  }
</style>
