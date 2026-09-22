<script lang="ts" setup>
  import { computed, ref, unref } from 'vue';

  import { useEmitter } from '@/hooks';
  import { getOnlineStatus, getResourceAvatar } from '@/pages/resource/resourceHelper';

  const props = defineProps<{
    checkedData: any;
    disable: boolean;
    filterText: string;
  }>();

  const expand = ref(true);

  const nodeList = computed(() => {
    const { filterText } = props;
    const { data } = props.checkedData;
    const ret = data.filter((item: any) => {
      if (filterText === '') {
        return true;
      }
      const { code, name } = item;
      return name.includes(filterText) || code.includes(filterText);
    });
    return ret;
  });
  const total = computed(() => {
    return unref(nodeList).length;
  });

  function handleExpand() {
    expand.value = !unref(expand);
  }

  function handleRemove(data) {
    useEmitter().emit('removeCheckedList', data.id);
  }
</script>

<template>
  <div v-if="total" class="checked-tree" :class="disable ? 'disable' : ''">
    <div class="tree-node">
      <div class="header" @click="handleExpand">
        <Icon class="icon" :class="{ expand }" name="node_expand" />
        <span>{{ `${checkedData.name}(${total})` }}</span>
      </div>
      <div v-show="expand" class="tree-node-list">
        <div v-for="item in nodeList" :key="item.id" class="tree-node">
          <div class="node-info-item">
            <div class="avatar" :class="getOnlineStatus(item)">
              <Icon class="icon" :name="getResourceAvatar(item)" prefix="tree" />
            </div>
            <TdTooltip :content="`${item.name}(${item.code})`">
              <span class="name">{{ item.name }}({{ item.code }})</span>
            </TdTooltip>
          </div>

          <Icon class="remove" name="remove" @click="handleRemove(item)" />
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  .checked-tree {
    .header {
      display: flex;
      align-items: center;
      height: 28px;
      cursor: pointer;

      .icon {
        width: 10px;
        height: 6px;
        margin-right: 8px;
        transform: rotate(-90deg);
      }

      .expand {
        transform: rotate(0deg);
      }

      span {
        font-size: 14px;
        font-weight: 400;
      }
    }

    .tree-node-list {
      .tree-node {
        display: flex;
        align-items: center;
        justify-content: space-between;
        height: 28px;
        cursor: pointer;

        .node-info-item {
          display: flex;
          align-items: center;
          padding-left: 18px;

          .name {
            display: inline-block;
            width: 220px;
            height: 22px;
            margin-left: 4px;
            font-size: 14px;
            line-height: 22px;
            .ellipsis1();
          }
        }

        .remove {
          width: 14px;
          height: 14px;
          margin-right: 6px;
          cursor: pointer;
        }

        &:hover {
          background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);
        }
      }
    }
  }

  .disable {
    span {
      color: #a6a9ab !important;
    }
  }
</style>
