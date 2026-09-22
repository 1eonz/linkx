<script lang="ts" setup>
  import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';

  import { useDC, useEmitter, useSetInterval } from '@/hooks';
  import ComponentCardList from '@/pages/communicationCard/componentCardList.vue';
  import GroupDetail from '@/pages/resource/group/groupDetail.vue';
  import { useCommunicationStore, useResourceStore } from '@/store';

  const props = defineProps({
    activeGroupId: {
      default: '',
      type: String,
    },
    autoPlay: {
      default: () => {},
      type: Object,
    },
    boxSmall: {
      default: '',
      type: String,
    }, // 6*4布局，小图标样式
    id: {
      default: '',
      type: String,
    },
    isBig: {
      default: false,
      type: Boolean,
    },
    isShowLayerOperate: {
      default: true,
      type: Boolean,
    },
    nodeObject: {
      default: () => {},
      type: Object,
    },
    showSimple: {
      default: false,
      type: Boolean,
    },
    synthesizeFlag: {
      default: false,
      type: Boolean,
    },
    voiceIng: {
      default: false,
      type: Boolean,
    },
  });

  const communicateStore = useCommunicationStore();
  const resourceStore = useResourceStore();

  const groupId = ref('');
  const simple = ref(true);
  const scroll = ref(false);
  const onLineNum = ref<any>({});
  const groupInfo = ref<any>({});
  const metaObjectData = ref<any>([]);
  const userList = ref<any>([]);
  const resourceId = ref('');
  const tabId = ref('');
  const isMax = ref(false);
  let mapId = '';
  let clearTimer: any;

  const isSimple = computed(() => (props.showSimple ? simple : false));
  const tabLists = computed(() => metaObjectData.value);
  const canCall = computed(() => groupId.value !== '');

  watch(
    () => props.id,
    (val) => {
      resourceId.value = val;
      mapId = val;
      tabId.value = `${props.id}tab`;
    },
  );
  watch(
    () => communicateStore.hasComm,
    () => {
      getGroupsInfo(props.id);
    },
  );
  watch(
    () => props.id,
    () => {
      initGroupOperate();
    },
    { deep: true },
  );

  onMounted(() => {
    initGroupOperate();
  });

  onBeforeUnmount(() => {
    clearTimer?.();
  });

  useDC('ACM', 'group_create', (message) => {
    if (groupInfo.value.id === message.actionGroupId) {
      groupId.value = message.id;
    }
  });
  useDC('ACM', 'group_delete', (message) => {
    if (groupInfo.value.id === message.actionGroupId) {
      groupId.value = '';
    }
  });
  // 刷新群组的详情和成员
  useDC('GROUP_CHANGE_MESSAGE', 'update_group', () => {
    getGroupsInfo(props.id);
  });

  function initGroupOperate() {
    resourceId.value = props.id;
    tabId.value = props.id;
    isMax.value = props.isBig;
    mapId = `${props.id}right`;
    // 群组的详情
    getGroupsInfo(props.id);

    useEmitter(resourceId.value, (data) => {
      metaObjectData.value = metaObjectData.value.filter((item) => {
        return item.keyName !== data;
      });
    });
  }

  function openDetailCard() {
    simple.value = !simple.value;
  }

  // 获取群组详情-d服务器
  async function getGroupsInfo(id) {
    const { commonGroup, dynamicGroup } = resourceStore;
    const list = [...commonGroup, ...dynamicGroup];
    list.forEach((item) => {
      if (item.groupId === id) {
        groupId.value = id;
        groupInfo.value = item;
        userList.value = item.groupUserList;
        onLineNum.value = {
          all: item.allNumber,
          onLine: item.onlineNumber,
        };
      }
    });
  }

  function getTab(keyName) {
    const allEl = document.getElementById(tabId.value);
    const targetEl = document.getElementById(keyName);

    if (!allEl || !targetEl) return;

    let top = allEl.scrollTop;
    const scrollHeight = allEl.scrollHeight - allEl.clientHeight;
    const x = targetEl.offsetTop - allEl.offsetTop;
    const clearTimer = useSetInterval(() => {
      let speed = (top - x) / 10;
      if (speed > 0 && Math.abs(speed) < 1) {
        speed = 1;
      } else if (speed < 0 && Math.abs(speed) < 1) {
        speed = -1;
      }
      allEl.scrollTop = top - speed;
      top = allEl.scrollTop;
      if (allEl.scrollTop === x || allEl.scrollTop === scrollHeight) {
        clearTimer();
      }
    }, 1);
  }

  function getScroll() {
    const allEl = document.getElementById(tabId.value);
    if (!allEl) return;
    const top = allEl.scrollTop;
    scroll.value = top > 100;
  }
</script>

<template>
  <div class="communication-card" :class="{ 'big-card': isMax }">
    <div class="group-content">
      <div class="detail-group">
        <GroupDetail
          :active-group-id="activeGroupId"
          :auto-play="autoPlay"
          :big-screen="true"
          :box-small="boxSmall"
          :can-call="canCall"
          :comm-desk="true"
          :group-id="groupId"
          :group-info="groupInfo"
          :on-line-num="onLineNum"
          :person-list="userList"
          :resource-id="resourceId"
          :synthesize-flag="synthesizeFlag"
          :user-list="userList"
          :voice-ing="voiceIng"
          @open-detail-card="openDetailCard"
        />
      </div>

      <div v-show="scroll && (!communicateStore.hasComm || isMax) && !isSimple" class="tab-list">
        <span v-for="item in tabLists" :key="item.index" @click="getTab(item.keyName)">
          {{ item.name }}
        </span>
      </div>

      <div
        v-if="isMax && !isSimple"
        :id="tabId"
        :class="{
          'man-detail': !isMax,
          'man-detail-scroll': scroll,
          'detail-man': isMax,
        }"
        @scroll.passive="getScroll"
      >
        <ComponentCardList
          :context="groupInfo"
          :is-max="isMax"
          :map-id="mapId"
          :resource-id="resourceId"
          :user-list="userList"
        />
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .communication-card {
    position: relative;
    padding: 10px 0;

    .tab-list {
      display: flex;
      align-items: center;
      justify-content: space-around;
      width: 100%;
      height: 60px;

      span {
        display: inline-block;
        font-size: var(--font-size-default);
        font-style: normal;
        font-weight: 400;
        color: var(--text-default);
      }
    }

    .setting-icon {
      position: absolute;
      top: 10px;
      left: 10px;
      z-index: 100;
      display: flex;
      justify-content: space-around;
      width: 10px;
      height: 10px;
      font-size: 20px;
      cursor: pointer;
      fill: var(--icon-color-home-normal);
    }

    .card-operate {
      position: absolute;
      top: 5px;
      right: 5px;
      z-index: 2000;
      display: flex;
      justify-content: space-around;
      height: 30px;

      .operate-icon {
        width: 35px;
        height: 35px;
        padding: 0 5px;
        fill: var(--icon-color-frame-control);
      }
    }

    .group-content {
      display: flex;
      flex-direction: column;
      width: 100%;
      height: 100%;

      .detail-group {
        position: relative;
        width: 100%;
        min-height: 50px;
      }

      .man-detail {
        width: 100%;
        max-height: calc(100vh - 286px);
        padding: 0 2px 0 10px;
        overflow-y: scroll;
      }

      .man-detail-scroll {
        max-height: calc(100vh - 346px);
      }
    }

    .text {
      width: 100%;
      height: 200px;
      line-height: 200px;
      color: #2d6694;
      text-align: center;
    }

    :deep(.slider-box) {
      position: fixed;
      bottom: unset;
      margin-top: -136px;
    }
  }

  .big-card {
    display: flex;
    width: 100%;
    height: 100vh;

    .content {
      width: 450px;

      .detail-group {
        position: relative;
        width: 100%;
        height: 229px;
        margin-top: 0;
      }

      .detail-man {
        flex: 1;
        width: 100%;
        padding: 0 10px;
        overflow-y: scroll;
        background: rgb(24 34 40 / 100%);
      }
    }

    .content-right {
      position: relative;
      display: flex;
      flex: 1;
      flex-direction: column;
      max-height: 100%;
    }
  }
</style>
