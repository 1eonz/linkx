<script lang="ts" setup>
  import { computed, onMounted, ref, unref, watchEffect } from 'vue';

  import { appConfig } from '@/config';
  import { useEmitter, useI18n } from '@/hooks';
  import { singleClick } from '@/pages/resource/resourceHelper';
  import { useAlarmStore, useMainStore } from '@/store';

  import { throttle } from 'lodash-es';

  import WarningNoticeDetail from './warningNoticeDetail.vue';
  import WarningNoticeSearch from './warningNoticeSearch.vue';
  import WarningNoticeSimple from './warningNoticeSimple.vue';

  const props = defineProps({
    showTab: {
      default: true,
      type: Boolean,
    },
  });

  const { t } = useI18n();
  const mainStore = useMainStore();
  const alarmStore = useAlarmStore();

  const query = ref(''); // 搜索框value
  const warningType = ref<any[]>([]);
  const activeId = ref(0); // 初始状态码
  let element: Element; // 用来控制切换tab页的时候 滚动条回到最上方

  // 任务新数据
  const showWarningData = computed(() => {
    const { getAlarmData } = alarmStore;
    if (unref(activeId) === 2) {
      return [...getAlarmData[0], ...getAlarmData[1]];
    }
    return getAlarmData[unref(activeId)];
  });
  const showSearch = computed(() => {
    return unref(query) !== '';
  });
  const activeItemId = computed<string>(() => mainStore.warningNoticeShowId);

  watchEffect(() => {
    // 未处理/已处理总条数变化，发生变化时动态改变各状态数据总数量
    let total = 0;
    warningType.value.forEach((item) => {
      const { value } = item;
      const count = alarmStore.getAlarmData[value]?.length;
      if (value === 2) {
        item.count = total;
      } else {
        total += count;
        item.count = count || 0;
      }
    });
  });

  useEmitter('mapAlarmCardDetailsChange', (data) => {
    mainStore.updateWarningNoticeShowId(data.alarmId);
  });

  onMounted(() => {
    activeId.value = props.showTab ? 0 : 2;
    initTab();
  });

  const scroll = throttle((e) => {
    scrollControlTask(e);
  }, 500);

  function initTab() {
    const tabs = [
      {
        count: 0,
        isPending: true,
        label: t('videoControl.warningInformation.untreated'),
        value: 0,
      },
      {
        count: 0,
        label: t('videoControl.warningInformation.treated'),
        value: 1,
      },
      {
        count: 0,
        label: t('videoControl.warningInformation.all'),
        value: 2,
      },
    ];
    const { eICSFeBC } = appConfig.settingData;
    if (eICSFeBC === '0') {
      activeId.value = 2;
      tabs.splice(0, 2);
    }
    warningType.value = tabs;
  }

  function warningClickItem(data) {
    const { alarmId } = data;
    const { updateWarningNoticeShowId, warningNoticeShowId } = mainStore;
    if (warningNoticeShowId === alarmId) {
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

  // 任务类型不同的数据过滤展示 点击tab，子组件传参给父组件，如果预警通知中内容面板滚动了并且滚动条距离不是0，那么切换选项卡时滚动条回到顶部
  function showChildMessage(data) {
    activeId.value = data.value;
    if (element?.scrollTop) {
      element.scrollTop = 0;
    }
  }

  // 滚动触发显示更多任务
  async function scrollControlTask(event) {
    element = event.srcElement;
    if (element) {
      // const { scrollTop, scrollHeight, clientHeight } = element;
    }
  }
</script>

<template>
  <div class="warning-notice">
    <!-- 统计tab -->
    <TdTabCount
      v-show="showTab"
      :active="activeId"
      class="tab-box"
      :tabs="warningType"
      @click="showChildMessage"
    />

    <!-- 搜索 -->
    <TdInput
      v-model="query"
      class="warning-notice-input"
      :placeholder="t('videoControl.warningInformation.reportedIncidentIdentifiedObject')"
      type="searchInput"
    />

    <!-- 搜索结果 -->
    <WarningNoticeSearch
      v-show="showSearch"
      class="warning-notice-search"
      :click-id="activeItemId"
      :search-content="query"
    />

    <!-- 预警列表 -->
    <div
      v-show="!showSearch"
      class="warning-notice-list"
      :class="showTab ? '' : 'all-list'"
      @scroll="scroll"
    >
      <WarningNoticeSimple
        v-for="(item, index) in showWarningData"
        :key="`${item.alarmId}${index}`"
        :click-id="activeItemId"
        :key-text="query"
        :warning-data="item"
        @click-item="warningClickItem"
      />
      <TdEmpty v-if="showWarningData.length === 0" />
    </div>

    <!-- 详情 -->
    <WarningNoticeDetail
      v-if="activeItemId"
      :class="showTab ? 'warning-notice-detail-right' : 'warning-notice-detail-left'"
      :warning-notice-id="activeItemId"
    />
  </div>
</template>

<style lang="less" scoped>
  .warning-notice {
    position: relative;
    display: flex;
    flex-direction: column;
    height: 100%;
    cursor: pointer;

    .tab-box {
      margin-top: 10px;
    }

    .warning-notice-input {
      margin: 10px 0;
    }

    .warning-notice-search {
      flex: 1;
      overflow: auto;
    }

    .warning-notice-list {
      flex: 1;
      overflow-y: auto;
    }

    .all-list {
      height: 760px;
    }

    .warning-notice-detail {
      &-right {
        position: absolute;
        top: -36px;
        left: 310px;
      }

      &-left {
        position: absolute;
        top: -36px;
        right: 310px;
      }
    }
  }
</style>
