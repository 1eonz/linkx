<script lang="ts" setup>
  import { computed, onMounted, ref, unref, watch } from 'vue';

  import { Dialog } from '@/components/Dialog';
  import { CategoryEnum } from '@/enums';
  import { useEmitter, useI18n } from '@/hooks';
  import GroupList from '@/pages/communicationCenter/components/groupList.vue';
  import PlanAdd from '@/pages/resource/plan/planAdd.vue';
  import { getResourceTypes } from '@/pages/resource/resourceHelper';
  import TreeNav from '@/pages/tree/common/treeNav.vue';
  import PlanTree from '@/pages/tree/planTree/index.vue';

  import SingleTypeList from './singleTypeList.vue';

  const props = withDefaults(
    defineProps<{
      activeId?: string;
      canDrag?: boolean;
      data?: any;
      defaultCheckKeys?: string[];
      noMap?: boolean;
      operation?: boolean;
      placeholder?: string;
      showCheckbox?: boolean;
      showPlan?: boolean;
      showResource?: any[];
    }>(),
    {
      showResource: () =>
        getResourceTypes()
          .filter((i) => i.show)
          .map((i) => i.type),
    },
  );
  const emit = defineEmits(['click', 'dblclick', 'focus', 'blur', 'change', 'checkChange']);

  const { t } = useI18n();

  const filterText = ref('');
  const navId = ref('all');
  const treeComponents = ref<any>(getResourceTypes());
  const underQuery = ref<number[]>([]);
  const singleTypeListRef = ref();

  const showSearch = computed(() => {
    return unref(filterText) !== '' && props.activeId !== 'GroupTree';
  });
  const showGroup = computed(() => {
    return props.activeId === 'GroupList';
  });
  const showEmpty = computed(() => {
    let total = 0;
    unref(treeComponents).forEach((i) => {
      total += i.total;
    });
    return total === 0;
  });
  const loading = computed(() => unref(underQuery).length > 0);

  watch(
    filterText,
    (val) => {
      if (val === '') {
        navId.value = 'all';
      }
      emit('change', val);
    },
    { deep: true },
  );

  useEmitter('clearFilterText', clearListener);

  onMounted(() => {
    init();
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
  }

  function clearListener(val) {
    if (val === 'GroupList') {
      filterText.value = '';
    }
  }

  function handleNav(id) {
    navId.value = id;
  }

  function createPlan() {
    Dialog({
      cid: 'create_plan',
      content: PlanAdd,
      data: {
        operateType: 'add',
        title: t('resource.plan.createPlan'),
      },
    });
  }

  function showItem(item) {
    const nav = navId.value;
    return (
      nav === 'all' ||
      nav === item.id ||
      (nav === 'thirdEquipment' &&
        [CategoryEnum.confTerminal, CategoryEnum.uav].includes(item.id)) ||
      (Number(nav) === CategoryEnum.recorder &&
        [CategoryEnum.GBRecorder, CategoryEnum.recorder].includes(item.id))
    );
  }

  function handleFocus() {
    useEmitter().emit('closeSelectedTree');
    emit('focus');
  }

  function handelBlur() {
    emit('blur');
  }

  function handleClick(data) {
    emit('click', data);
  }

  function handleDblclick(data) {
    emit('dblclick', data);
  }

  function checkChange(category, checks, cancelCheckKeys) {
    emit('checkChange', category, checks, cancelCheckKeys);
  }

  function totalChange(id, total) {
    treeComponents.value.forEach((item) => {
      if (item.id === id) {
        item.total = total;
      }
    });
  }

  function fetchStart(category) {
    if (!unref(underQuery).includes(category)) {
      underQuery.value.push(category);
    }
  }

  function fetchEnd(category) {
    underQuery.value = unref(underQuery).filter((i) => i !== category);
  }

  function handleCancel() {
    singleTypeListRef.value.forEach((item) => {
      item.abortFetch();
    });
    underQuery.value = [];
  }
</script>

<template>
  <div class="resource-search-wrap">
    <!-- 搜索输入框 -->
    <div class="search-input">
      <TdInput
        v-model="filterText"
        :placeholder="placeholder || t('common.search.inputContent')"
        type="searchInput"
        @blur="handelBlur"
        @focus="handleFocus"
      />

      <slot name="suffix"></slot>
      <TdButton v-if="showPlan" :text="t('common.create')" type="normal" @click="createPlan" />
    </div>

    <div v-show="showSearch" class="search-body">
      <!-- 搜索群组 -->
      <GroupList v-if="showGroup" :filter-text="filterText" />

      <PlanTree
        v-else-if="showPlan"
        :default-check-keys="defaultCheckKeys"
        :filter-text="filterText"
        @check-change="checkChange"
      />

      <!-- 搜索通讯录 -->
      <div v-else class="search-box">
        <TreeNav :nav-id="navId" :show-resource="showResource" @click="handleNav" />

        <div v-show="!showEmpty && !loading" class="search-data">
          <template v-for="item in treeComponents" :key="item.id">
            <SingleTypeList
              v-if="item.show"
              v-show="showItem(item)"
              :can-drag="canDrag"
              :data="item"
              :default-check-keys="defaultCheckKeys"
              :filter-text="filterText"
              :nav-id="navId"
              :no-map="noMap"
              :operation="operation"
              :show-checkbox="showCheckbox"
              @check-change="checkChange"
              @click="handleClick"
              @dblclick="handleDblclick"
              @fetch-end="fetchEnd"
              @fetch-start="fetchStart"
              @total-change="(total) => totalChange(item.id, total)"
            />
          </template>
        </div>

        <TdLoading v-show="loading" cancel @on-cancel="handleCancel" />
        <TdEmpty v-show="showEmpty && !loading" />
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .resource-search-wrap {
    display: flex;
    flex-direction: column;
    width: 100%;

    .search-input {
      position: relative;
      display: flex;
      justify-content: space-between;
      width: 100%;
      height: 32px;
      margin: 10px 0;

      :deep(.td-input) {
        flex: 1;
        height: 32px;

        .td-input-inner {
          color: #fff;
          border: none;
        }

        input:focus {
          background: inherit;
        }
      }

      :deep(.td-button) {
        width: 75px;
        height: 32px;
        margin-left: 4px;
      }
    }

    .search-body {
      flex: 1;
      height: 0;
    }

    .search-box {
      display: flex;
      height: 100%;

      .search-data {
        width: 100%;
        height: 100%;
        overflow-y: scroll;
      }
    }
  }
</style>
