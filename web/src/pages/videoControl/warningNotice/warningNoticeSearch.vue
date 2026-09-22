<script lang="ts" setup>
  import { computed, onMounted, ref, unref, watch } from 'vue';

  import { queryVideoControlWarningList } from '@/api/videoControl';
  import { useEmitter, useI18n } from '@/hooks';
  import { singleClick } from '@/pages/resource/resourceHelper';
  import { useMainStore } from '@/store';

  import { debounce } from 'lodash-es';

  import WarningNoticeSimple from './warningNoticeSimple.vue';

  const props = defineProps<{
    searchContent: string;
  }>();

  const { t } = useI18n();
  const mainStore = useMainStore();

  const searchTotal = ref(0); // 总条数
  const searchList = ref<any[]>([]); // 告警对象列表
  let currentPage = 1; // 当前页码

  const activeItemId = computed(() => mainStore.warningNoticeShowId);

  const searchWarningList = debounce(async () => {
    const { searchContent } = props;

    if (searchContent === '') {
      searchList.value = [];
      currentPage = 1;
      return;
    }

    if (currentPage !== 1 && unref(searchList).length >= unref(searchTotal)) {
      return;
    }

    const param = {
      pageNo: currentPage,
      pageSize: 20,
      title: searchContent,
    };
    const { code, data } = await queryVideoControlWarningList(param);
    if (code === 0 && data.total > 0) {
      searchList.value = [...unref(searchList), ...data.results];
      searchTotal.value = data.total;
      currentPage = currentPage + 1;
    } else {
      searchTotal.value = 0;
      searchList.value = [];
    }
  }, 500);

  watch(
    () => props.searchContent,
    () => {
      searchList.value = [];
      currentPage = 1;
      searchTotal.value = 0;
      searchWarningList();
    },
  );

  onMounted(() => {
    useEmitter('alarmIgnore', () => {
      searchWarningList();
    });
  });

  function clickItem(data) {
    const { alarmId } = data;
    const { updateWarningNoticeShowId } = mainStore;
    if (unref(activeItemId) === alarmId) {
      updateWarningNoticeShowId('');
    } else {
      updateWarningNoticeShowId(alarmId);
      // 增加单击定位功能，修改地图中心点和层级
      singleClick({
        data,
        type: 'warn',
      });
    }
  }
</script>

<template>
  <div class="warning-notice-search">
    <div v-show="searchList.length > 0" class="search-list-title">
      {{ searchTotal + t('videoControl.videoControlCommon.searchResultUnit') }}
    </div>
    <div class="search-list" @scroll="searchWarningList">
      <div v-if="searchList.length === 0" class="no-list">
        {{ t('videoControl.videoControlCommon.searchNoResult') }}
      </div>
      <template v-else>
        <WarningNoticeSimple
          v-for="item in searchList"
          :key="item.alarmId"
          :click-id="activeItemId"
          :key-text="searchContent"
          :warning-data="item"
          @click-item="clickItem"
        />
      </template>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .warning-notice-search {
    display: flex;
    flex-direction: column;
    width: 100%;
    height: 100%;

    .spinner-tittle {
      padding-top: 10px;
    }

    .no-list {
      width: 100%;
      margin-top: 15px;
      font-size: var(--font-size-medium);
      color: var(--text-title-second);
    }

    .search-list-title {
      display: flex;
      flex-direction: row-reverse;
      align-items: center;
      height: 14px;
      margin-bottom: 10px;
      font-size: 14px;
      color: var(--text-title-second);
    }

    .search-list {
      display: flex;
      flex-direction: column;
      width: 100%;
      height: 100%;
      overflow: hidden auto;

      .spinner-tittle {
        padding-top: 10px;
      }
    }
  }
</style>
