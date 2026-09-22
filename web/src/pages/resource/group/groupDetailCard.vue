<script lang="ts" setup>
  import { computed, onMounted, ref } from 'vue';

  import { useDragResize, useEmitter, useI18n, useSetInterval } from '@/hooks';
  import ComponentCardList from '@/pages/communicationCard/componentCardList.vue';
  import SmsComp from '@/pages/communicationCard/sms/smsComp.vue';
  import { useCommunicationStore, useResourceStore } from '@/store';

  import GroupDetail from './groupDetail.vue';
  import GroupPersonList from './groupPersonList.vue';

  const props = defineProps<{
    activeInfo?: any;
    groupId: string;
    showSimple?: boolean;
    uid: string;
  }>();
  const emit = defineEmits(['closeDialog']);

  const { t } = useI18n();
  const resourceStore = useResourceStore();
  const communicationStore = useCommunicationStore();

  const componentCardRef = ref();
  const scroll = ref(false);
  const tabList = ref<any[]>([]);
  const isReady = ref(false);
  const childGroupRef = ref();
  const light = ref(false);
  const size = ref<any>('mini');

  const communicateShow = computed(() => {
    return communicationStore.communicateCallId.includes(props.groupId);
  });
  const groupInfo = computed<any>(() => {
    const allGroup = [...resourceStore.commonGroup, ...resourceStore.dynamicGroup];
    let ret = {};
    allGroup.forEach((item) => {
      if (item.groupId === props.groupId) {
        ret = { ...item };
      }
    });
    return ret;
  });
  const userList = computed(() => groupInfo.value.groupUserList || []);
  const onLineNum = computed(() => {
    const { allNumber, onlineNumber } = groupInfo.value;
    return {
      all: allNumber,
      onLine: onlineNumber,
    };
  });

  useDragResize({
    callback() {
      const el = document.getElementById(props.uid) as HTMLElement;
      const width = el.offsetWidth;
      if (width < 412) {
        size.value = 'mini';
      } else if (width >= 412 && width < 698) {
        size.value = 'another';
      } else if (width >= 698 && width < 800) {
        size.value = 'small';
      } else {
        size.value = 'big';
      }
    },
    minH: 514,
    minW: 308,
    uid: props.uid,
  });

  onMounted(() => {
    const { groupId } = props;
    communicationStore.addCommunicateCallId(groupId);
  });

  useEmitter(props.groupId, (data) => {
    tabList.value = tabList.value.filter((item) => {
      return item.keyName !== data;
    });
  });

  function closeCard() {
    const { groupId } = props;
    const params = {
      contentType: 'shutDownGroupCard',
      groupType: groupInfo.value.groupType,
      id: groupId,
    };
    resourceStore.setGroupsData(params);
    communicationStore.detCommunicateCallId(groupId);
    emit('closeDialog');
  }

  function getTab(keyName) {
    const el = componentCardRef.value;
    const targetEl = document.getElementById(keyName);

    if (!el || !targetEl) return;

    let top = el.scrollTop;
    const scrollHeight = el.scrollHeight - el.clientHeight;
    const x = targetEl.offsetTop - el.offsetTop;
    const clearTimer = useSetInterval(() => {
      let speed = (top - x) / 10;
      if (speed > 0 && Math.abs(speed) < 1) {
        speed = 1;
      } else if (speed < 0 && Math.abs(speed) < 1) {
        speed = -1;
      }
      el.scrollTop = top - speed;
      top = el.scrollTop;
      if (el.scrollTop === x || el.scrollTop === scrollHeight) {
        clearTimer();
      }
    }, 1);
  }

  function getScroll() {
    const el = componentCardRef.value;
    if (!el) return;
    const top = el.scrollTop;
    scroll.value = top > 100;
  }

  function handleClickStyle() {
    light.value = true;
  }

  function handleBlur() {
    light.value = false;
  }
</script>

<template>
  <TdFrameBox
    class="group-detail-card"
    :class="{ 'box-light': light }"
    :dragger="true"
    :size="size"
    :title="t('communication.communicationFunction.groupMessage')"
    @close-frame-box="closeCard"
  >
    <div class="group-content">
      <div class="detail-group">
        <GroupDetail
          :id="`${groupId}id`"
          ref="childGroupRef"
          :active-info="activeInfo"
          :can-call="!!groupId"
          :group-id="groupId"
          :group-info="groupInfo"
          :on-line-num="onLineNum"
          :person-list="userList"
          :resource-id="groupId"
          tabindex="0"
          :user-list="userList"
          @blur="handleBlur"
          @click="handleClickStyle"
        />

        <GroupPersonList :group-info="groupInfo" :person-list="userList" />
      </div>

      <SmsComp
        v-if="communicateShow"
        :id="groupId"
        :active-info="activeInfo"
        :group-id="groupId"
        type="group"
        :user-list="userList"
      />

      <div v-show="scroll && !communicateShow && !showSimple" class="tab-list">
        <span v-for="item in tabList" :key="item.index" @click="getTab(item.keyName)">
          {{ item.name }}
        </span>
      </div>

      <div
        v-if="isReady && communicateShow"
        ref="componentCardRef"
        class="man-detail"
        :class="{
          'man-detail-scroll': scroll,
        }"
        @scroll.passive="getScroll"
      >
        <ComponentCardList
          :context="groupInfo"
          :map-id="groupId"
          :resource-id="groupId"
          :user-list="userList"
        />
      </div>
    </div>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  .box-light {
    border: 0.125rem solid #1afffb;
    box-shadow: 0 0.0625rem 0.25rem rgb(0 199 145 / 50%);
  }

  .group-detail-card {
    height: 800px;

    :deep(.frame-box-container) {
      padding: 0 !important;
    }

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

    .group-content {
      display: flex;
      flex-direction: column;
      width: 100%;
      height: 100%;
      padding-top: 11px;

      .detail-group {
        position: relative;
        width: 100%;
        min-height: 50px;
        padding: 0 8px 8px;
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
  }
</style>
