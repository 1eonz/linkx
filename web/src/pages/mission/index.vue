<script lang="ts" setup>
  import { computed, onMounted, ref, unref, watch } from 'vue';

  import { Dialog } from '@/components/Dialog';
  import { useEmitter, useI18n } from '@/hooks';
  import { singleClick } from '@/pages/resource/resourceHelper';
  import { useMainStore, useMissionStore } from '@/store';

  import { throttle } from 'lodash-es';

  import MissionCenterCard from './missionCenter/missionCenterCard.vue';
  import MissionSearch from './missionSearch.vue';
  import MissionSimpleInfo from './missionSimpleInfo.vue';

  type MissionType = {
    count: number;
    isPending: boolean;
    label: string;
    value: number | string;
  };

  const props = defineProps({
    showTab: {
      default: true,
      type: Boolean,
    },
  });

  const { t } = useI18n();
  const mainStore = useMainStore();
  const missionStore = useMissionStore();

  const timeFormat = 'YYYY-MM-DD';
  const searchTime = ref(''); // 时间搜索框
  const missionKeyWord = ref('');
  const activeId = ref<any>('all'); // 初始状态码
  const clickEmpty = ref({});
  const showCenterCard = ref(false);
  const countArray = ref({});
  const missionCardShowId = ref('');
  const listRef = ref();

  const showSearch = computed(() => missionKeyWord.value !== '' || searchTime.value);
  const missionType = computed(() => {
    const { missionCondition } = missionStore;
    const ret: MissionType[] = [];
    missionCondition.forEach((item) => {
      const total = missionStore.missionTotal[item.id];
      const { id, name } = item;
      ret.push({
        count: total,
        isPending: props.showTab ? id === 'pending' : false,
        label: name,
        value: id,
      });
    });
    if (!props.showTab) {
      ret.push({
        count: 0,
        isPending: true,
        label: t('mission.missionList.all'),
        value: 'all',
      });
    }
    return ret;
  });
  // 任务数据
  const showingMissions = computed(() => missionStore.getMissionData);

  watch(activeId, (oldVal, newVal) => {
    if (oldVal !== newVal) {
      showCenterCard.value = false;
    }
  });

  useEmitter('closeMissionDetails', isShowCenterCardClose);

  useEmitter('mapMissionCardDetailsChange', (id) => {
    missionCardShowId.value = id;
  });

  onMounted(async () => {
    activeId.value = props.showTab ? 'pending' : 'all';

    await missionStore.queryMissionData(unref(activeId));

    missionType.value.forEach((item) => {
      countArray[item.value] = {
        count: 2,
        stateName: item.label,
      };

      // 初始化每个tab项和点击过的任务关联关系
      clickEmpty[item.value] = {
        id: null,
        stateName: item.label,
      };
    });
  });

  // 滚动触发显示更多任务
  const scroll = throttle(() => {
    const { clientHeight, scrollHeight, scrollTop } = listRef.value; // 滚动高度;内容高度;可见高度
    if (scrollTop + clientHeight >= scrollHeight - 1) {
      missionStore.queryMissionData(unref(activeId), true);
    }
  }, 500);

  function dateClear() {
    searchTime.value = '';
  }

  function isShowCenterCardClose() {
    showCenterCard.value = false;
    missionCardShowId.value = '';
  }

  // 任务类型不同的数据过滤展示
  function showChildMessage(data) {
    activeId.value = data.value;
    listRef.value.scrollTo(0, 0);
    missionStore.queryMissionData(unref(activeId));
  }

  function missionClickItem(flowId, missionData) {
    if (missionCardShowId.value === flowId) {
      isShowCenterCardClose();
      return;
    }

    // 关闭点击图标打开的警情弹窗
    mainStore.layerIds.forEach((id: string) => {
      if (id.includes('missionCard')) {
        Dialog(id)?.close();
        mainStore.deleteLayerId(id);
      }
    });

    if (missionCardShowId.value === flowId || !showCenterCard.value) {
      showCenterCard.value = !showCenterCard.value;
    }

    if (clickEmpty[activeId.value]) {
      clickEmpty[activeId.value].id = flowId;
    } else {
      clickEmpty[activeId.value] = { id: flowId };
    }
    missionCardShowId.value = flowId;

    // 增加单击定位功能，修改地图中心点和层级
    singleClick({
      data: missionData,
      type: 'mission',
    });
  }
</script>

<template>
  <!-- 任务 -->
  <div class="my-mission">
    <!-- 统计tab -->
    <TdTabCount
      v-show="showTab"
      :active="activeId"
      class="tab-box"
      :tabs="missionType"
      @click="showChildMessage"
    />
    <!-- 搜索 -->
    <TdInput
      v-model="missionKeyWord"
      class="mission-input"
      :placeholder="t('mission.missionList.searchPlaceHolder')"
      type="searchInput"
    />
    <!-- 时间搜索 -->
    <div class="date-picker">
      <ElDatePicker
        v-model="searchTime"
        :end-placeholder="t('mission.missionList.endTime')"
        :format="timeFormat"
        popper-class="date-picker-popper"
        :start-placeholder="t('mission.missionList.startTime')"
        type="daterange"
        :value-format="timeFormat"
      />
      <Icon class="clear-btn" name="date_clear" @click="dateClear" />
    </div>
    <!-- 搜索结果 -->
    <MissionSearch
      v-show="showSearch"
      :class="showTab ? 'mission-search' : 'all-list'"
      :click-id="missionCardShowId"
      :search-content="missionKeyWord"
      :search-time="searchTime"
      search-type="mission"
      :status="activeId"
      @search-mission-id="missionClickItem"
    />

    <!-- 任务列表 -->
    <div
      v-show="!showSearch"
      ref="listRef"
      :class="showTab ? 'mission-list' : 'all-list'"
      @scroll="scroll"
    >
      <MissionSimpleInfo
        v-for="item in showingMissions"
        :key="item.flowId"
        :click-id="missionCardShowId"
        :mission="item"
        @click-item="missionClickItem(item.flowId, item)"
      />
      <TdEmpty v-if="showingMissions.length === 0" />
    </div>

    <!-- 点击弹出事件的弹框 -->
    <MissionCenterCard
      v-if="showCenterCard"
      :class="showTab ? 'mission-detail-right' : 'mission-detail-left'"
      :click-item="true"
      :flow-id="missionCardShowId"
      @is-show-center-card-close="isShowCenterCardClose"
    />
  </div>
</template>

<style lang="less" scoped>
  :deep(.el-date-editor.el-input__wrapper) {
    width: 100%;
    height: 32px;
  }

  .my-mission {
    position: relative;
    display: flex;
    flex-direction: column;
    height: 100%;
    cursor: pointer;

    .date-picker {
      height: 32px;
      margin-bottom: 10px;

      .date-picker-popper {
        .el-picker-panel {
          position: absolute;
          left: -240px;
        }
      }

      .clear-btn {
        position: relative;
        top: -20px;
        left: 270px;
        width: 10px;
        height: 10px;
        cursor: pointer;
      }
    }

    .tab-box {
      margin-top: 10px;
    }

    .mission-input {
      margin: 10px 0;
    }

    .mission-search {
      flex: 1;
      overflow: auto;
    }

    .mission-list {
      flex: 1;
      overflow-y: auto;
    }

    .all-list {
      height: 760px;
      overflow: auto;
    }

    .mission-detail {
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
