<script lang="ts" setup>
  import { ref, watch } from 'vue';

  import { queryVideoControlTaskList } from '@/api/videoControl';
  import { useI18n } from '@/hooks';

  import { debounce } from 'lodash-es';

  import ControlTaskSimple from './controlTaskSimple.vue';

  const props = defineProps({
    clickId: {
      default: null,
      type: [Number, String],
    },
    searchContent: {
      default: '',
      type: String,
    },
  });
  const emit = defineEmits(['searchMissionId']);
  const { t } = useI18n();
  const searchList = ref<any[]>([]);
  const searchTotal = ref<number>(0);
  const taskCardShowId = ref<string>('');

  const searchControlTaskList = debounce(async () => {
    const { searchContent } = props;

    if (searchContent === '') {
      taskCardShowId.value = '';
      searchList.value = [];
      return;
    }

    const params = {
      name: searchContent,
      pageNo: 1,
      pageSize: 10_000,
    };
    const { code, data } = await queryVideoControlTaskList(params);
    if (code === 0) {
      searchList.value = data.results;
      searchTotal.value = Number(data.total);
    } else {
      searchTotal.value = 0;
    }
  }, 500);

  watch(
    () => props.searchContent,
    () => {
      searchControlTaskList();
    },
  );
  watch(
    () => props.clickId,
    (val) => {
      taskCardShowId.value = val as string;
    },
  );

  function clickItem(taskId) {
    taskCardShowId.value = taskId;
    emit('searchMissionId', taskId);
  }
</script>

<template>
  <div class="control-task-search">
    <div v-if="searchList.length > 0" class="search-list-title">
      {{ searchTotal + t('videoControl.videoControlCommon.searchResultUnit') }}
    </div>
    <TdEmpty v-else />

    <div class="search-list">
      <ControlTaskSimple
        v-for="item in searchList"
        :key="item.suspectTaskId"
        :click-id="taskCardShowId"
        :key-text="searchContent"
        :task="item"
        @click-item="clickItem"
      />
    </div>
  </div>
</template>

<style lang="less" scoped>
  .control-task-search {
    display: flex;
    flex-direction: column;
    width: 100%;

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
      overflow: hidden auto;
    }
  }
</style>
