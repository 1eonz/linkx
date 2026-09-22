<script lang="ts" setup>
  import { computed, nextTick, onMounted, ref, unref } from 'vue';

  import { Message } from '@/components/Message';
  import { LayerIdEnum } from '@/enums';
  import { useEmitter, useI18n } from '@/hooks';
  import { mapManager } from '@/plugins/map';
  import { useMessageStore } from '@/store';

  import { debounce } from 'lodash-es';

  import ListItem from './components/listItem.vue';
  import MessagesDetails from './components/messagesDetails.vue';
  import SearchList from './components/searchList.vue';

  const props = defineProps({
    showTab: {
      default: true,
      type: Boolean,
    },
  });

  const { t } = useI18n();
  const messageStore = useMessageStore();

  const query = ref('');
  const activeStatus = ref(0);
  const activeId = ref('');
  const isShowCenterCard = ref(false);
  const detailsRef = ref();
  const listRef = ref();
  const start = ref(10);

  const showSearch = computed(() => unref(query) !== '');
  const showListData = computed(() => {
    const { getMessages } = messageStore;
    let ret: any[] = [];
    ret =
      unref(activeStatus) === 999
        ? [...getMessages[0], ...getMessages[3]]
        : getMessages[unref(activeStatus)];
    return ret.slice(0, unref(start));
  });
  const btnType = computed(() => {
    const { getMessages, messagesData } = messageStore;
    const ret = [
      {
        count: getMessages[0].length,
        isPending: true,
        label: t('message.status.pending'),
        value: 0,
      },
      {
        count: getMessages[3].length,
        label: t('message.status.processed'),
        value: 3,
      },
      {
        count: messagesData.length,
        label: t('message.status.all'),
        value: 999,
      },
    ];
    return ret;
  });

  useEmitter('mapMessageCardDetailsChange', (id) => {
    activeId.value = id;
  });

  onMounted(() => {
    activeStatus.value = props.showTab ? 0 : 999;
  });

  const scroll = debounce(scrollLoad, 500);

  async function updateListData() {
    messageStore.queryMessageData();
  }

  function tabClick(data) {
    if (unref(activeStatus) !== data.value) {
      activeStatus.value = data.value;
      start.value = 10;
      listRef.value.scrollTop = 0;
      isShowCenterCardClose();
    }
  }

  function clickItem(data) {
    const layerId = LayerIdEnum.message;
    const mapObj = mapManager.get('mapId_main');
    const visible = mapObj.getLayerVisible(layerId);

    isShowCenterCard.value = true;
    activeId.value = data.id;

    if (!visible) {
      Message(t('resource.map.layerIsHidden'));
      return;
    }

    const { lat, lon } = data;
    if (!(lon && lat)) {
      Message(t('mission.missionMap.nonPositionInfo'));
      return;
    }

    mapObj.addMarkerCustomPopup(layerId, data);
  }

  // 滚动加载
  async function scrollLoad() {
    const { clientHeight, scrollHeight, scrollTop } = listRef.value;
    if (scrollTop + clientHeight >= scrollHeight) {
      const { getMessages, messagesData } = messageStore;
      const status = unref(activeStatus);
      const target = status === 999 ? messagesData : getMessages[status];
      if (unref(showListData).length < target.length) {
        start.value += 10;
      }
    }
  }

  function isShowCenterCardClose() {
    isShowCenterCard.value = false;
    if (detailsRef.value) {
      detailsRef.value.ignoreVal = false;
    }
  }

  function handleIgnore(type, data) {
    activeId.value = data.id;
    isShowCenterCard.value = true;
    nextTick(() => {
      if (type) {
        detailsRef.value.setIgnoreVal(type);
      } else {
        detailsRef.value.effectWarningNotice(data);
      }
    });
  }
</script>

<template>
  <div class="messages-container">
    <!-- 统计tab -->
    <TdTabCount
      v-show="showTab"
      :active="activeStatus"
      class="tab-box"
      :tabs="btnType"
      @click="tabClick"
    />

    <!-- 搜索 -->
    <TdInput
      v-model="query"
      class="messages-input"
      :placeholder="t('message.searchPlaceholder')"
      type="searchInput"
    />

    <!-- 搜索结果 -->
    <SearchList
      v-show="showSearch"
      :class="showTab ? 'messages-search' : 'all-list'"
      :query="query"
      @click-item="clickItem"
    />

    <!-- 列表 -->
    <div
      v-show="!showSearch"
      ref="listRef"
      :class="showTab ? 'messages-list' : 'all-list'"
      @scroll="scroll"
    >
      <ListItem
        v-for="item in showListData"
        :key="item.alarmId"
        :click-id="activeId"
        :data="item"
        @click-item="clickItem"
        @handle-ignore="handleIgnore"
        @handle-success="updateListData"
        @is-show-center-card-close="isShowCenterCardClose"
      />
      <TdEmpty v-if="showListData.length === 0" />
    </div>

    <!-- 点击弹出事件的弹框 -->
    <MessagesDetails
      v-if="isShowCenterCard"
      ref="detailsRef"
      :class="showTab ? 'messages-detail-right' : 'messages-detail-left'"
      :messages-id="activeId"
      @handle-success="updateListData"
      @is-show-center-card-close="isShowCenterCardClose"
    />
  </div>
</template>

<style lang="less" scoped>
  .messages-container {
    position: relative;
    display: flex;
    flex-direction: column;
    height: 100%;
    pointer-events: all;

    .tab-box {
      margin-top: 10px;
    }

    .messages-input {
      margin: 10px 0;
    }

    .messages-search {
      flex: 1;
      overflow-y: auto;
    }

    .messages-list {
      flex: 1;
      overflow-y: auto;
    }

    .all-list {
      height: 760px;
      overflow-y: auto;
    }

    .messages-detail {
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
