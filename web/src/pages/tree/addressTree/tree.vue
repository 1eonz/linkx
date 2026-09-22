<script lang="ts" setup>
  import { computed, onMounted, ref, unref, watch } from 'vue';

  import { selectAddress } from '@/api/address';
  import { appConfig } from '@/config';
  import { useAddressStore } from '@/store';

  const props = withDefaults(
    defineProps<{
      filterText?: string;
      id: string;
      showBtn?: boolean;
      title: string;
    }>(),
    {
      showBtn: true,
    },
  );
  const emit = defineEmits(['click', 'update', 'cancel', 'delete']);
  const addressStore = useAddressStore();
  const nodeList = ref<any[]>([]);
  const expand = ref(false);
  const active = ref('');
  const keywords = ref('');

  const checkValue = computed(() => {
    const { ADDRESS_COLLECT_TYPE } = appConfig.settingData;
    return ADDRESS_COLLECT_TYPE;
  });

  watch(
    () => addressStore.addressData,
    () => {
      getNodeList();
    },
    { deep: true },
  );

  watch(
    () => addressStore.keywords,
    (val) => {
      keywords.value = val;
      getNodeList();
    },
    { deep: true },
  );

  onMounted(() => {
    const { ADDRESS_COLLECT_TYPE } = appConfig.settingData;
    expand.value = props.title === ADDRESS_COLLECT_TYPE;
    getNodeList();
  });

  async function getNodeList() {
    const params = {
      executorId: appConfig.resourceId,
      keywords: keywords.value,
      typeId: props.id,
    };
    const { code, data } = await selectAddress(params);
    if (code === 0) {
      nodeList.value = data;
    }
  }

  function handleNodeClick(data) {
    const { id } = data;
    active.value = active.value === id ? '' : id;
    emit('click', data, active.value);
  }

  function handleExpand() {
    expand.value = !unref(expand);
  }

  function handleDelete(data) {
    emit('cancel', data, active.value);
  }

  async function handleDeleteType() {
    let activeData: any = null;
    if (active.value) {
      const params = {
        executorId: appConfig.resourceId,
        typeId: props.id,
      };
      const { code, data } = await selectAddress(params);
      if (code === 0) {
        data.forEach((item) => {
          if (item.id === active.value) {
            activeData = item;
          }
        });
      }
    }
    emit('delete', props.id, activeData, active.value);
  }

  function handleUpdate(data) {
    emit('update', data);
  }
</script>

<template>
  <div class="address-tree">
    <div class="header">
      <Icon class="icon" :class="{ expand }" name="node_expand" @click.stop="handleExpand" />
      <span @click.stop="handleExpand">{{ title }}</span>
      <Icon
        v-if="title !== checkValue && showBtn"
        class="delete"
        name="option_delete"
        @click.stop="handleDeleteType"
      />
    </div>
    <div v-show="expand" class="tree-node-list">
      <div
        v-for="node in nodeList"
        :key="node.id"
        class="tree-node"
        :class="{ active: active === node.id }"
        @click.stop="handleNodeClick(node)"
      >
        <div class="node-title">
          <Icon class="icon" name="tree_address" prefix="tree" />
          <TdTooltip :content="node.name" placement="top">
            <div class="name" :class="{ 'long-name': !showBtn }">{{ node.name }}</div>
          </TdTooltip>
          <Icon
            v-if="showBtn"
            class="icon btn"
            name="option_edit"
            @click.stop="handleUpdate(node)"
          />
          <Icon
            v-if="showBtn"
            class="icon btn"
            name="btn_uncollected"
            @click.stop="handleDelete(node)"
          />
        </div>
        <div class="node-info">
          <div class="address" :class="{ 'long-address': !showBtn }">{{ node.address }}</div>
          <div v-if="node.remark && showBtn" class="remark">
            <Icon class="file-icon" name="category_file" />
            <span>{{ node.remark }}</span>
          </div>
        </div>
      </div>
      <TdEmpty v-show="nodeList.length === 0" />
    </div>
  </div>
</template>

<style lang="less" scoped>
  @import url('@/styles/mixin.less');

  .address-tree {
    .header {
      display: flex;
      align-items: center;
      height: 28px;
      cursor: pointer;

      .icon {
        width: 10px;
        height: 6px;
        margin: 0 4px 0 10px;
        transform: rotate(-90deg);
      }

      .expand {
        transform: rotate(0deg);
      }

      span {
        width: 260px;
        overflow: hidden;
        font-size: 14px;
        font-weight: 400;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .delete {
        width: 14px;
        height: 14px;
        opacity: 0;
      }

      &:hover {
        .delete {
          opacity: 1;
        }
      }
    }

    .tree-node-list {
      .tree-node {
        padding: 3px 0 3px 24px;
        cursor: pointer;

        .node-title {
          display: flex;
          align-items: center;

          .icon {
            width: 16px;
            height: 16px;
            margin-right: 4px;
            fill: #159aff;
          }

          .btn {
            display: none;
            margin-left: 8px;
          }

          .name {
            width: 250px;
            height: 22px;
            font-size: 14px;
            line-height: 22px;
            color: var(--text-title-first);
            .ellipsis1();
          }

          .long-name {
            width: 280px;
          }
        }

        .node-info {
          margin-top: 4px;

          .address {
            width: 270px;
            font-size: 12px;
            line-height: 16px;
            color: var(--text-color-minor);
            word-break: break-all;
          }

          .long-address {
            width: 300px;
          }

          .remark {
            display: flex;
            align-items: center;
            height: 20px;

            .file-icon {
              width: 12px;
              height: 12px;
              fill: var(--text-color-minor);
            }

            span {
              margin-left: 5px;
              font-size: 12px;
              line-height: 16px;
              color: var(--text-color-minor);
            }
          }
        }

        &:hover {
          background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);

          .node-title {
            .icon {
              display: block;
              fill: var(--text-default);
            }

            .name {
              width: 210px;
            }
          }

          .node-info {
            .address {
              color: var(--text-color-hover);
            }

            .remark {
              .file-icon {
                fill: var(--text-color-hover);
              }

              span {
                color: var(--text-color-hover);
              }
            }
          }
        }
      }

      .active {
        background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);

        .node-title {
          .icon {
            display: block;
            fill: var(--text-default);
          }

          .name {
            width: 210px;
          }
        }

        .node-info {
          .address {
            color: var(--text-color-hover) !important;
          }

          .remark {
            .file-icon {
              fill: var(--text-color-hover) !important;
            }

            span {
              color: var(--text-color-hover) !important;
            }
          }
        }
      }
    }
  }
</style>
