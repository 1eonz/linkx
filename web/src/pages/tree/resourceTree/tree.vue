<script lang="ts" setup>
  import { computed, ref, unref, useAttrs, watch } from 'vue';

  import { useI18n } from '@/hooks';

  import { debounce } from 'lodash-es';

  import TreeNode from './treeNode.vue';

  const props = defineProps<{
    checkKeys: string[];
    data: any;
  }>();

  const emit = defineEmits(['checkChange', 'click', 'dblclick', 'loadMore']);

  const { t } = useI18n();

  const attrs = useAttrs();
  const expand = ref(false);
  const checkAll = ref(false);
  const isIndeterminate = ref(false);
  const renderAfterExpand = ref(false); // 在第一次展开某个树节点后才渲染其子节点

  const nodeList = computed(() => {
    const { checkKeys, data } = props;
    const ret = data.children.map((item) => {
      item.check = checkKeys.includes(item.id);
      return item;
    });
    return ret;
  });

  watch(
    () => [props.checkKeys, props.data],
    () => {
      const { checkKeys } = props;
      const { allChild } = props.data;
      const checkLen =
        checkKeys.length > 0 ? allChild.filter((item) => checkKeys.includes(item.id)).length : 0;
      const len = allChild.length;
      checkAll.value = len !== 0 && checkLen !== 0 && checkLen === len;
      isIndeterminate.value = checkLen > 0 && checkLen < len;
    },
    { deep: true, immediate: true },
  );

  const handleLoadMore = debounce(() => {
    emit('loadMore', props.data);
  }, 500);

  function handleNodeClick(data, type) {
    emit('click', data, type);
  }

  function handleNodeDblclick(data, type) {
    emit('dblclick', data, type);
  }

  function handleCheckAllChange(check) {
    emit(
      'checkChange',
      props.data.id,
      check,
      props.data.allChild.filter((i) => i.id),
    );
  }

  function handleCheckChange(check, data) {
    emit('checkChange', props.data.id, check, [data]);
  }

  function handleExpand() {
    renderAfterExpand.value = true;
    expand.value = !unref(expand);
    if (!unref(expand)) {
      emit('loadMore', props.data, 20);
    }
  }
</script>

<template>
  <div class="resource-inner-tree">
    <div class="header" @click.stop="handleExpand">
      <Icon class="icon" :class="{ expand }" name="node_expand" />
      <TdCheckbox
        v-model="checkAll"
        class="checkbox"
        :indeterminate="isIndeterminate"
        @change="handleCheckAllChange"
        @click.stop
      />
      <span>{{ data.name }}</span>
    </div>

    <div v-if="renderAfterExpand" v-show="expand" class="tree-node-list">
      <div v-for="item in nodeList" :key="item.id" class="tree-node-container">
        <div v-if="item.loadMore" class="tree-node-container_more">
          <TdButton @click="handleLoadMore">{{ t('common.loadMore') }}</TdButton>
        </div>

        <div v-else class="tree-node-container_inner">
          <TdCheckbox
            v-model="item.check"
            class="checkbox"
            @change="(val) => handleCheckChange(val, item)"
          />
          <TreeNode
            v-bind="attrs"
            :node-data="item"
            @click="handleNodeClick"
            @dblclick="handleNodeDblclick"
          />
        </div>
      </div>

      <TdEmpty v-if="nodeList.length === 0" />
    </div>
  </div>
</template>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  .resource-tree {
    margin: 8px 0;

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
      .tree-node-container {
        margin: 8px 0;

        &_more {
          padding-left: 36px;
        }

        &_inner {
          position: relative;
          display: flex;
          align-items: center;
          height: 28px;
          padding-left: 36px;
          cursor: pointer;

          .checkbox {
            margin-right: 4px;
          }

          &:hover {
            background: linear-gradient(
              90deg,
              rgb(41 233 194 / 80%) 0%,
              rgb(55 219 157 / 28%) 100%
            );

            :deep(.operation) {
              display: flex;
            }
          }
        }
      }
    }
  }
</style>
