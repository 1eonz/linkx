<script lang="ts" setup>
  import { computed, ref, unref, watch } from 'vue';

  import { useI18n } from '@/hooks';
  import { getOnlineStatus, getResourceAvatar } from '@/pages/resource/resourceHelper';

  const props = defineProps<{
    active: number;
    canDrag?: boolean; // 可拖拽
    infoData: any;
    keywords?: string;
    operation?: boolean; // 显示操作按钮
  }>();

  const emit = defineEmits(['click', 'checkChange']);
  const { t } = useI18n();

  const nodeList = ref<any[]>([]);
  const expand = ref(false);
  const checkAll = ref(false);
  const isIndeterminate = ref(false);
  const pageNum = ref(10);

  const showList = computed(() => {
    return nodeList.value.slice(0, unref(pageNum));
  });

  watch(
    nodeList,
    (val) => {
      const checkLen = val.filter((item) => item.checked).length;
      isIndeterminate.value = checkLen > 0 && checkLen < val.length;
    },
    { deep: true },
  );

  watch(
    props.infoData,
    () => {
      getNodeList();
    },
    { deep: true },
  );

  watch(
    () => props.active,
    () => {
      pageNum.value = 10;
    },
  );

  function loadMore() {
    pageNum.value += 10;
  }

  function getNodeList() {
    nodeList.value = props.infoData.data;
  }

  function handleNodeClick(data) {
    emit('click', data);
  }

  function handleCheckAllChange(checked) {
    if (unref(nodeList).length === 0) {
      checkAll.value = false;
      return;
    }
    nodeList.value.forEach((item) => (item.checked = checked));
    handleCheckChange();
  }

  function handleCheckChange() {
    const checks = unref(nodeList).filter((item) => item.checked);
    emit('checkChange', checks, unref(nodeList));
  }

  function handleExpand() {
    expand.value = !unref(expand);
  }
</script>

<template>
  <div class="circle-tree">
    <div class="header" @click.stop="handleExpand">
      <Icon class="hide" :class="{ expand }" name="node_expand" />
      <TdCheckbox
        v-model="checkAll"
        class="checkbox"
        :indeterminate="isIndeterminate"
        @change="handleCheckAllChange"
      />
      <TdTooltip :content="infoData.name">
        <span>{{ `${infoData.name}(${nodeList.length})` }}</span>
      </TdTooltip>
    </div>
    <div v-show="expand" class="tree-node-list">
      <div v-for="item in showList" :key="item.id" class="tree-node">
        <TdCheckbox v-model="item.checked" class="checkbox" @change="handleCheckChange" />
        <div class="node-info" @click.stop="handleNodeClick(item)">
          <div class="avatar" :class="getOnlineStatus(item)">
            <Icon class="icon" :name="getResourceAvatar(item)" prefix="tree" />
          </div>
          <TdTooltip :content="`${item.name}(${item.code})`">
            <span class="name">{{ item.name }}({{ item.code }})</span>
          </TdTooltip>
        </div>
        <div class="node-distance">
          <Icon class="icon" name="distance" />
          <span class="name">{{ `${item.pointDistance}KM` }}</span>
        </div>
      </div>
      <TdButton v-if="pageNum <= nodeList.length - 1" class="more" @click="loadMore">
        {{ t('common.loadMore') }}
      </TdButton>
    </div>
  </div>
</template>

<style lang="less" scoped>
  @import url('@/styles/mixin.less');

  .circle-tree {
    height: 100%;

    .header {
      display: flex;
      align-items: center;
      height: 28px;
      cursor: pointer;

      .hide {
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

      .avatar {
        margin: 0 4px;
      }

      span {
        width: 226px;
        font-size: 14px;
        font-weight: 400;
        .ellipsis1();
      }

      .operation {
        opacity: 0;
      }

      .item {
        display: flex;
      }

      &:hover {
        background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);

        .operation {
          opacity: 1;
        }
      }
    }

    .tree-node-list {
      .tree-node {
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
            width: 150px;
            height: 22px;
            margin-left: 4px;
            font-size: 14px;
            line-height: 22px;
            .ellipsis1();
          }
        }

        .node-distance {
          display: flex;
          align-items: center;

          .name {
            margin-left: 2px;
            font-size: 12px;
            font-weight: 400;
            line-height: 22px;
            color: rgb(153 206 251 / 100%);
            letter-spacing: 0;
          }
        }

        .operation {
          opacity: 0;
        }

        &:hover {
          background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);

          .node-info {
            .name {
              width: 150px;
            }
          }

          .operation {
            opacity: 1;
          }
        }
      }

      .more {
        margin-left: 36px;
      }
    }
  }
</style>
