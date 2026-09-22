<script lang="ts" setup>
  import type { SearchBoxData } from '#/components';

  import { onMounted, ref, shallowRef, unref, watch } from 'vue';

  import { useI18n } from '@/hooks';

  import { debounce } from 'lodash-es';

  defineOptions({
    name: 'TdSearch',
  });

  const props = withDefaults(
    defineProps<{
      checkedKeys?: string[];
      data?: SearchBoxData[];
      modelValue?: any;
      placeholder?: string;
      searchMaxHeight?: string;
      showCheckbox?: boolean;
      showSearch?: boolean;
    }>(),
    {
      searchMaxHeight: 'calc(100vh - 10rem)',
      showSearch: true,
    },
  );

  const emit = defineEmits([
    'filterChange',
    'closeSearch',
    'clickItem',
    'dbClickItem',
    'focus',
    'blur',
    'checkChange',
    'update:modelValue',
  ]);

  type SearchResult = {
    [propName: string]: any;
    id: string;
  };

  interface Nodes extends SearchBoxData {
    count: number;
    search: SearchResult[];
    show: boolean;
    showMore: boolean;
    transfer: string;
  }

  const { t } = useI18n();

  let nodes: Nodes[] = [];
  const searchNodes = ref<Nodes[]>([]);
  const filterText = ref('');
  const isShowSearch = ref(false);
  const transformMen = 'drop_down';
  const totalCount = ref(0);
  const searchContentMaxHeight = ref({});
  let clickTimes = 0;

  const onChangeFilterText = debounce(lazyInit, 500);

  watch(filterText, (val) => {
    emit('update:modelValue', val);
    emit('filterChange', val);
    if (val === '') {
      totalCount.value = 0;
      nodes = [];
      searchNodes.value = [];
      emit('closeSearch', val);
    } else {
      onChangeFilterText();
    }
  });

  onMounted(() => {
    isShowSearch.value = props.showSearch;
    searchContentMaxHeight.value = { 'max-height': props.searchMaxHeight };
  });

  async function isShowMore(searchNode) {
    const { count, searchResult } = await searchNode.loadMore(unref(filterText));
    const showMore = count > searchResult.length + searchNode.search.length;
    searchNodes.value.forEach((item) => {
      if (item.title === searchNode.title) {
        item.showMore = showMore;
        item.search = [...searchNode.search, ...searchResult];
      }
    });
  }

  function lazyInit() {
    totalCount.value = 0;
    nodes = [];
    searchNodes.value = [];
    initLoad();
  }

  async function initLoad() {
    const { data } = props;
    for (const i in data) {
      const item = data[i];
      if (item.title === undefined) return;
      if (unref(filterText) === '') {
        emit('closeSearch', '');
        return;
      }

      const { count, searchResult } = await item.lazyLoad?.(unref(filterText));
      const tmp = {
        count,
        loadMore: item.loadMore,
        render: shallowRef(item.render),
        search: searchResult,
        show: true,
        showMore: count > searchResult.length,
        title: item.title,
        transfer: transformMen,
      };
      nodes = nodes.filter((item) => tmp.title !== item.title);
      nodes.push(tmp);
    }
    searchNodes.value = nodes;
    totalCount.value = 0;
    unref(searchNodes).forEach((item) => {
      totalCount.value += Number(item.count);
    });
  }

  function clickNode(data) {
    clickTimes += 1;
    setTimeout(() => {
      if (clickTimes === 1) {
        emit('clickItem', data);
      }
      if (clickTimes === 2) {
        emit('dbClickItem', data);
      }
      clickTimes = 0;
    }, 300);
  }

  function checkChange(node, checked) {
    emit('checkChange', node, checked);
  }

  function isShow(title) {
    for (const i in searchNodes) {
      if (searchNodes[i].title === title) {
        searchNodes[i].show = !searchNodes[i].show;
        searchNodes[i].transfer = searchNodes[i].show ? 'drop_down' : 'return';
        return;
      }
    }
  }

  function focus() {
    emit('focus');
  }

  function blur() {
    emit('blur');
  }
</script>

<template>
  <div class="search-node">
    <div v-if="isShowSearch" class="search-module">
      <TdInput
        v-model="filterText"
        :placeholder="placeholder || t('common.search.inputContent')"
        style="width: 100%; font-size: 14px"
        type="searchInput"
        @blur="blur"
        @focus="focus"
      />
    </div>

    <div v-if="searchNodes.length > 0" class="search-result">
      <div class="search-head"> {{ t('common.tdcomp.searchResult') }}({{ totalCount }}) </div>
      <div class="search-line"></div>
    </div>

    <div v-if="filterText !== ''" class="search-content" :style="searchContentMaxHeight">
      <div v-for="item in searchNodes" :key="item.title" class="search-nodes">
        <div v-if="item.title !== '' && item.search.length > 0" class="search-icon">
          <TdButton
            class="search-transfer"
            :icon-name="item.transfer"
            type="iconNormal"
            @click="isShow(item.title)"
          />
          <span class="search-title">{{ item.title }}</span>
        </div>
        <div v-show="item.show">
          <component
            :is="item.render"
            v-for="node in item.search"
            :key="node.id"
            :checked-keys="checkedKeys"
            class="search-child-node"
            :data="node"
            @check-change="(checked) => checkChange(node, checked)"
            @click="clickNode(node)"
          />

          <div v-if="item.showMore" class="search-more" @click="isShowMore(item)">
            <TdButton
              class="click-show-more-contact"
              icon-direction="iconRight"
              icon-name="drop_down"
              :text="t('common.tdcomp.showMore')"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .search-node {
    width: 100%;

    .search-module {
      position: relative;
      display: inline-flex;
      width: 100%;
      line-height: 36px;
      border: 0;

      .search-img {
        width: 18px;
        height: 18px;
        padding: 9px;
        fill: var(--icon-color-normal);
      }

      .search-close {
        .search-delete {
          width: 14px;
          height: 14px;
          padding: 9px 9px 0;
          fill: var(--text-title-second);
        }
      }
    }

    .search-result {
      z-index: 10;

      .search-head {
        display: flex;
        align-items: center;
        padding: 10px 0;
        font-size: var(--font-size-medium);
        color: var(--text-color-button);
      }

      .search-line {
        color: rgb(50 67 74);
        border-bottom: 1px solid;
      }
    }

    .search-content {
      overflow: hidden auto;

      .search-nodes {
        padding: 10px 0;

        .search-icon {
          .search-transfer {
            width: 35px;
            height: 35px;
            cursor: pointer;
            fill: var(--icon-color-normal);
          }

          .search-title {
            display: inline-block;
            align-items: center;
            height: 20px;
            padding-left: 8px;
            font-size: 21px;
            color: var(--text-color-button);
          }
        }

        .search-child-node {
          padding: 8px 0;
          cursor: pointer;

          &:hover {
            background-color: var(--background-hover);
          }
        }

        .search-more {
          margin: 10px 0 20px 26px;

          .click-show-more-contact {
            width: 126px;
            height: 30px;
            font-size: 15px;
            fill: var(--icon-color-drop-down);
          }
        }
      }
    }
  }
</style>
