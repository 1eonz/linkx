<script lang="ts" setup>
  import { computed, onMounted, ref, unref, watch } from 'vue';

  import { flowMissionPaged } from '@/api/mission';
  import { useDC, useEmitter, useI18n } from '@/hooks';
  import eventAndMissionUtil from '@/pages/mission/eventAndMissionUtil';
  import { useMissionStore } from '@/store';

  import { debounce } from 'lodash-es';

  import MissionSimpleInfo from '../mission/missionSimpleInfo.vue';

  const props = defineProps({
    clickId: {
      default: () => {},
      type: String,
    },
    searchContent: {
      default: '',
      type: String,
    },
    searchTime: {
      default: '',
      type: String,
    },
    searchType: {
      default: '',
      type: String,
    },
    status: {
      default: '',
      type: String,
    },
  });
  const emit = defineEmits(['searchEventId', 'searchMissionId']);
  const { t } = useI18n();
  const searchTotal = ref(0);
  const searchList = ref<any[]>([]);

  const flowId = computed(() => props.clickId);

  const searchMissionList = debounce((e) => {
    if (!e) {
      return;
    }
    const { clientHeight, scrollHeight, scrollTop } = e.target;
    if (scrollTop + clientHeight >= scrollHeight) {
      queryList(true);
    }
  }, 500);

  watch(() => props.searchContent, queryList);
  watch(() => props.searchTime, queryList);

  // 为保证任务列表可靠刷新 使用以下两种方式刷新列表
  useDC('MISSION', 'update_mission', queryList);

  onMounted(() => {
    useEmitter('sendPoliceSuccess', () => {
      setTimeout(searchMissionList, 500);
    });
  });

  async function queryList(loadMore?) {
    const { searchContent, searchTime, status } = props;

    if (searchContent === '' && !searchTime && searchTime.length === 0) {
      searchList.value = [];
      searchTotal.value = 0;
      return;
    }

    let pageNum = 1;
    if (loadMore === true) {
      const len = unref(searchList).length;
      if (len >= unref(searchTotal)) {
        return;
      }
      pageNum = Math.ceil(len / 10) + 1;
    }

    const param: any = {
      maxCreateTime: searchTime ? getTime(searchTime, false) : '',
      minCreateTime: searchTime ? getTime(searchTime, true) : '',
      pageNum,
      pageSize: 10,
      search: searchContent,
    };

    if (status !== 'all') {
      param.status = useMissionStore().getStatus[status].join(',');
    }

    const { code, data } = await flowMissionPaged(param);
    if (code === 0) {
      if (data.records?.length > 0) {
        const missionData = eventAndMissionUtil.evenAndMissionDataInit(data.records);
        searchList.value = loadMore === true ? [...unref(searchList), ...missionData] : missionData;
        searchTotal.value = Number(data.total);
      } else {
        searchList.value = [];
      }
    }
  }

  function getTime(input, isStart) {
    const [start, end] = input;
    return isStart ? `${start} 00:00:00` : `${end} 23:59:59`;
  }

  function missionClickItem(data) {
    emit('searchMissionId', data.flowId, data);
  }
</script>

<template>
  <div class="search">
    <div v-if="searchList.length > 0" class="search-list-title">
      {{ searchTotal + t('common.search.searchResultUnit') }}
    </div>
    <TdEmpty v-else />

    <div v-if="searchType === 'mission'" class="search-list" @scroll="searchMissionList">
      <MissionSimpleInfo
        v-for="(item, index) in searchList"
        :key="index"
        :click-id="flowId"
        :key-text="searchContent"
        :mission="item"
        @click-item="missionClickItem(item)"
      />
    </div>
  </div>
</template>

<style lang="less" scoped>
  .search {
    display: flex;
    flex-direction: column;
    width: 100%;
    height: 100%;

    .spinner-tittle {
      padding-top: 10px;
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
    }
  }
</style>
