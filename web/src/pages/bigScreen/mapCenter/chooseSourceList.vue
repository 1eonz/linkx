<script lang="ts" setup>
  import { onMounted, ref, unref } from 'vue';

  import { CategoryEnum } from '@/enums';
  import { useI18n, usePermissions } from '@/hooks';
  import QuickOperate from '@/pages/communicationCenter/components/quickOperate.vue';
  import { getOnlineStatus, getResourceAvatar } from '@/pages/resource/resourceHelper';
  import { useMapCenterStore } from '@/store';

  import { cloneDeep } from 'lodash-es';

  const emit = defineEmits(['closeDialog']);

  const { t } = useI18n();
  const mapCenterStore = useMapCenterStore();

  const showSearchResult = ref(false);
  const filterText = ref('');
  let oldTreeComponents = [
    {
      checkAll: false,
      expand: false,
      id: CategoryEnum.pdt,
      isIndeterminate: false,
      name: 'PDT',
      show: usePermissions('eBC'),
      children: [] as any[],
    },
    {
      checkAll: false,
      expand: false,
      id: CategoryEnum.monitor,
      isIndeterminate: false,
      name: t('resource.resourceType.monitor'),
      show: true,
      children: [] as any[],
    },
    {
      checkAll: false,
      expand: false,
      id: CategoryEnum.recorder,
      isIndeterminate: false,
      name: t('resource.resourceType.recorder'),
      show: true,
      children: [] as any[],
    },
    {
      checkAll: false,
      expand: false,
      id: CategoryEnum.terminal,
      isIndeterminate: false,
      name: t('resource.resourceType.terminal'),
      show: true,
      children: [] as any[],
    },
    {
      checkAll: false,
      expand: false,
      id: CategoryEnum.carPhoto,
      isIndeterminate: false,
      name: t('resource.resourceType.carPhoto'),
      show: usePermissions('eBC'),
      children: [] as any[],
    },
    {
      checkAll: false,
      expand: false,
      id: CategoryEnum.seat,
      isIndeterminate: false,
      name: t('resource.resourceType.seat'),
      show: true,
      children: [] as any[],
    },
    {
      checkAll: false,
      expand: false,
      id: CategoryEnum.GBRecorder,
      isIndeterminate: false,
      name: t('resource.resourceType.GBRecorder'),
      show: usePermissions('eBC'),
      children: [] as any[],
    },
    {
      checkAll: false,
      expand: false,
      id: CategoryEnum.uav,
      isIndeterminate: false,
      name: t('resource.resourceType.uav'),
      show: usePermissions('eBC'),
      children: [] as any[],
    },
    {
      checkAll: false,
      expand: false,
      id: CategoryEnum.confTerminal,
      isIndeterminate: false,
      name: t('resource.resourceType.confTerminal'),
      show: usePermissions('eBC'),
      children: [] as any[],
    },
  ];
  const treeComponents = ref([
    {
      checkAll: false,
      expand: false,
      id: CategoryEnum.pdt,
      isIndeterminate: false,
      name: 'PDT',
      show: usePermissions('eBC'),
      children: [] as any[],
    },
    {
      checkAll: false,
      expand: false,
      id: CategoryEnum.monitor,
      isIndeterminate: false,
      name: t('resource.resourceType.monitor'),
      show: true,
      children: [] as any[],
    },
    {
      checkAll: false,
      expand: false,
      id: CategoryEnum.recorder,
      isIndeterminate: false,
      name: t('resource.resourceType.recorder'),
      show: true,
      children: [] as any[],
    },
    {
      checkAll: false,
      expand: false,
      id: CategoryEnum.ballCamera,
      isIndeterminate: false,
      name: t('resource.resourceType.ballCamera'),
      show: true,
      children: [] as any[],
    },
    {
      checkAll: false,
      expand: false,
      id: CategoryEnum.terminal,
      isIndeterminate: false,
      name: t('resource.resourceType.terminal'),
      show: true,
      children: [] as any[],
    },
    {
      checkAll: false,
      expand: false,
      id: CategoryEnum.carPhoto,
      isIndeterminate: false,
      name: t('resource.resourceType.carPhoto'),
      show: usePermissions('eBC'),
      children: [] as any[],
    },
    {
      checkAll: false,
      expand: false,
      id: CategoryEnum.seat,
      isIndeterminate: false,
      name: t('resource.resourceType.seat'),
      show: true,
      children: [] as any[],
    },
    {
      checkAll: false,
      expand: false,
      id: CategoryEnum.GBRecorder,
      isIndeterminate: false,
      name: t('resource.resourceType.GBRecorder'),
      show: usePermissions('eBC'),
      children: [] as any[],
    },
    {
      checkAll: false,
      expand: false,
      id: CategoryEnum.uav,
      isIndeterminate: false,
      name: t('resource.resourceType.uav'),
      show: usePermissions('eBC'),
      children: [] as any[],
    },
    {
      checkAll: false,
      expand: false,
      id: CategoryEnum.confTerminal,
      isIndeterminate: false,
      name: t('resource.resourceType.confTerminal'),
      show: usePermissions('eBC'),
      children: [] as any[],
    },
  ]);

  onMounted(() => {
    initData();
  });

  function initData() {
    if (mapCenterStore.chooseSourcesList.length > 0) {
      treeComponents.value.forEach((item) => {
        item.children = [];
      });
      mapCenterStore.chooseSourcesList.forEach((item) => {
        const tempNode = treeComponents.value.filter((val) => val.id === item.category);
        if (tempNode.length > 0) {
          tempNode[0].checkAll = true;
          tempNode[0].children.push(cloneDeep(item));
        }
      });
      oldTreeComponents = cloneDeep(treeComponents.value);
    } else {
      treeComponents.value.forEach((item) => {
        item.children = [];
        item.expand = false;
        item.isIndeterminate = false;
        item.checkAll = false;
        item.show = false;
      });
    }
  }

  function closeFilterBox() {
    mapCenterStore.initChooseSourcesList();
    emit('closeDialog');
  }

  function handleExpand(tree) {
    tree.expand = !tree.expand;
  }

  function handleCheckAllChange(tree) {
    if (unref(tree.children).length === 0) {
      tree.checkAll = false;
      return;
    }
    tree.isIndeterminate = false;
    const checks = tree.children.filter((item) => item.check);
    if (checks.length === tree.children.length) {
      tree.children.forEach((item) => (item.check = false));
    } else {
      tree.children.forEach((item) => (item.check = true));
    }
    resetChooseSourcesList();
  }

  function handleCheckChange(tree) {
    const checks = tree.children.filter((item) => item.check);
    tree.checkAll = checks.length === tree.children.length;
    tree.isIndeterminate = checks.length > 0 && checks.length < tree.children.length;
    resetChooseSourcesList();
  }

  function resetChooseSourcesList() {
    // 重新设置已选列表
    mapCenterStore.initChooseSourcesList();
    treeComponents.value.forEach((item) => {
      mapCenterStore.addChooseSourcesList(
        item.children.filter((val) => {
          return val.check === true;
        }),
      );
    });
  }

  function queryList() {
    if (filterText.value === '') {
      treeComponents.value = cloneDeep(oldTreeComponents);
    } else {
      oldTreeComponents.forEach((item) => {
        const filters = item.children.filter(
          (val) => val.name.includes(filterText.value) || val.code.includes(filterText.value),
        );
        const tempNode = treeComponents.value.filter((val) => val.name === item.name);
        if (tempNode.length > 0) {
          tempNode[0].isIndeterminate = false;
          tempNode[0].checkAll = false;
          tempNode[0].children = cloneDeep(filters);
        }
      });
    }
    resetChooseSourcesList();
  }
</script>

<template>
  <TdFrameBox
    class="choose-source-list"
    :dragger="true"
    :title="t('common.tdcomp.chooseData')"
    @close-frame-box="closeFilterBox"
  >
    <!-- 搜索输入框 -->
    <div class="search">
      <TdInput
        v-model="filterText"
        :placeholder="t('common.search.inputContent')"
        type="searchInput"
      />
      <TdButton @click="queryList">{{ t('common.search.searchContext') }}</TdButton>
    </div>
    <!-- 圈选资源树 -->
    <div class="select">
      <div v-for="tree in treeComponents" v-show="tree.show" :key="tree.id" class="tree">
        <div class="header" @click="handleExpand(tree)">
          <Icon class="icon" :class="tree.expand ? 'expand' : 'icon'" name="node_expand" />
          <ElCheckbox
            v-model="tree.checkAll"
            class="checkbox"
            :indeterminate="tree.isIndeterminate"
            @change="handleCheckAllChange(tree)"
          />
          <span>{{ `${tree.name}(${tree.children.length})` }}</span>
        </div>
        <div v-show="tree.expand" class="tree-node-list">
          <div v-for="item in tree.children" :key="item.id" class="tree-node">
            <ElCheckbox v-model="item.check" class="checkbox" @change="handleCheckChange(tree)" />
            <div class="avatar" :class="getOnlineStatus(item)">
              <Icon class="icon" :name="getResourceAvatar(item)" prefix="tree" />
            </div>
            <TdTooltip :content="`${item.name}(${item.code})`">
              <span class="name">{{ item.name }}({{ item.code }})</span>
            </TdTooltip>
          </div>
        </div>
      </div>
    </div>
    <!-- 一键按钮 -->
    <QuickOperate v-show="!showSearchResult" :is-draw="true" />
  </TdFrameBox>
</template>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  :deep(.frame-box-container) {
    padding: 10px !important;
  }

  .choose-source-list {
    position: absolute;
    top: 85px;
    left: 450px;

    .search {
      display: flex;

      .td-input {
        margin-right: 4px;
      }
    }

    .select {
      height: 480px;
      margin-top: 10px;
      overflow-y: auto;
    }
  }

  .tree {
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
      padding-left: 36px;

      .tree-node {
        position: relative;
        display: flex;
        align-items: center;
        height: 28px;
        cursor: pointer;

        .checkbox {
          margin-right: 4px;
        }

        .name {
          display: inline-block;
          width: 210px;
          height: 22px;
          margin-left: 4px;
          font-size: 14px;
          line-height: 22px;
          .ellipsis1();
        }

        &:hover {
          background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);
        }
      }
    }
  }
</style>
