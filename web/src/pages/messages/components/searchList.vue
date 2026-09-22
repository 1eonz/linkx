<script lang="ts" setup>
  import { ref, unref, watch } from 'vue';

  import { queryMessageAlertList } from '@/api/message';
  import { useI18n } from '@/hooks';
  import { useMessageStore } from '@/store';

  import { debounce } from 'lodash-es';

  import ListItem from './listItem.vue';

  const props = defineProps<{
    query: string;
  }>();
  const emit = defineEmits(['clickItem']);
  const { t } = useI18n();
  const { queryMessageData } = useMessageStore();
  const searchTotal = ref(0); // 总条数
  const listData = ref<any[]>([]); // 告警对象列表
  const activeId = ref(0); // 控制卡片点击颜色
  let currentPage = 1; // 当前页码

  watch(
    () => props.query,
    () => {
      refreshSearchList();
    },
  );

  const searchList = debounce(async () => {
    const { query } = props;
    if (query === '') {
      currentPage = 1;
      listData.value = [];
      searchTotal.value = 0;
      return;
    }

    if (currentPage !== 1 && unref(listData).length >= unref(searchTotal)) {
      return;
    }

    const param = {
      pageNo: currentPage,
      pageSize: 20,
      title: query,
    };
    const { code, data } = await queryMessageAlertList(param);
    if (code === 0) {
      const { records = [], total } = data;
      listData.value = [...listData.value, ...records];
      searchTotal.value = Number(total);
      currentPage = currentPage + 1;
    } else {
      searchTotal.value = 0;
    }
  }, 500);
  function clickItem(data) {
    activeId.value = data.id;
    emit('clickItem', data);
  }
  function refreshSearchList() {
    listData.value = [];
    currentPage = 1;
    searchList();
    queryMessageData();
  }
</script>

<template>
  <!--  紧急事件搜索结果-->
  <div class="messages-search">
    <div v-show="listData.length > 0" class="messages-search-number">
      {{ searchTotal + t('videoControl.videoControlCommon.searchResultUnit') }}
    </div>
    <div class="messages-search-list" @scroll="searchList">
      <div v-if="listData.length > 0">
        <ListItem
          v-for="item in listData"
          :key="item.id"
          :click-id="activeId"
          :data="item"
          :key-text="query"
          @click-item="clickItem"
          @handle-success="refreshSearchList"
        />
      </div>
      <TdEmpty v-else />
    </div>
  </div>
</template>

<style lang="less" scoped>
  .messages-search {
    display: flex;
    flex-direction: column;

    .messages-search-number {
      display: flex;
      flex-direction: row-reverse;
      align-items: center;
      height: 14px;
      font-size: 14px;
      color: var(--text-title-second);
    }

    .messages-search-list {
      display: flex;
      flex-direction: column;
      width: 100%;
      overflow: hidden auto;
    }
  }
</style>
