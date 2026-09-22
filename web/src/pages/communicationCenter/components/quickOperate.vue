<script lang="ts" setup>
  import { nextTick, onMounted, ref, watch } from 'vue';
  import { useRoute } from 'vue-router';

  import { getAccountsByEquipmentIds } from '@/api/equipment';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import { CategoryEnum } from '@/enums';
  import { useEmitter, useI18n, usePermissions } from '@/hooks';
  import { openMessageSendCard } from '@/pages/communicationCard/openResourcesCard';
  import { createGroup } from '@/pages/resource/groupHelper';
  import { monitorPlay } from '@/pages/resource/videoHelper';
  import SelectedTree from '@/pages/tree/resourceTree/selectedTree.vue';
  import { createConferenceHandler } from '@/pages/videoConference/common';
  import {
    defaultCheckedList,
    useMapCenterStore,
    useMonitorStore,
    usePlanStore,
    useTreeStore,
  } from '@/store';
  import { delay } from '@/utils';

  import CheckedListCard from './checkedListCard.vue';

  const props = defineProps({
    isDraw: {
      default: false, // 地图圈选true
      type: Boolean,
    },
    isSupport: {
      default: false, // 保障组true
      type: Boolean,
    },
  });

  const { t } = useI18n();
  const route = useRoute();
  const treeStore = useTreeStore();
  const mapCenterStore = useMapCenterStore();
  const monitorStore = useMonitorStore();
  const planStore = usePlanStore();

  const quickOperateArr = ref<any[]>([]);
  const activeId = ref('');

  watch(route, () => {
    nextTick(() => {
      activeId.value = '';
      Dialog('checkedListCard')?.close();
    });
  });

  useEmitter('checkedListCardClose', () => {
    activeId.value = '';
  });

  useEmitter('checkedListCardClose', closeListener);

  onMounted(() => {
    initOperate();
  });

  function closeListener() {
    activeId.value = '';
  }

  function initOperate() {
    const showConf = usePermissions('CONFERENCE') && usePermissions('eBC');
    const OperateArr = [
      {
        icon: 'incoming',
        id: 'VoiceConf',
        name: t('resource.operateBtn.voiceConf'),
        show: showConf,
      },
      {
        icon: 'monitor',
        id: 'VideoWatch',
        jump: true,
        name: t('resource.operateBtn.videoWatch'),
        prefix: 'bigScreen',
        show: true,
      },
      {
        icon: 'conference',
        id: 'VideoConf',
        name: t('resource.operateBtn.videoConf'),
        prefix: 'bigScreen',
        show: showConf,
      },
      {
        icon: 'messages',
        id: 'MessageSend',
        name: t('resource.operateBtn.messageSend'),
        show: true,
      },
      {
        icon: 'group',
        id: 'Group',
        name: t('resource.operateBtn.group'),
        prefix: 'bigScreen',
        show: true,
      },
    ];
    quickOperateArr.value = OperateArr;
  }

  // 一键功能点击
  function operateFunc(item) {
    const { id } = item;
    if (props.isDraw || props.isSupport) {
      handleQuick(id);
    } else {
      openCheckedListCard(id);
    }
  }

  function openCheckedListCard(id) {
    let total = 0;
    Object.values(treeStore.resourceCheckedList).forEach((item) => {
      total += item.length;
    });

    if (total === 0) {
      Message(t('common.tdcomp.chooseNoData'));
      return;
    }

    activeId.value = id;

    Dialog({
      cid: 'checkedListCard',
      content: CheckedListCard,
      data: {
        type: id,
      },
      offset: ['45%', '20%'],
    });
  }

  // 获取数据account
  async function getAccount(items) {
    const ids = items.map((i) => i.id);
    const { code, data } = await getAccountsByEquipmentIds(ids);
    if (code === 0) {
      const obj = {};
      data.forEach((item) => {
        obj[item.id] = item.account;
      });
      items.forEach((element) => {
        element.account = obj[element.id];
      });
    }
  }

  async function handleQuick(type) {
    const store = props.isDraw ? mapCenterStore : planStore;
    const { chooseSourcesList } = store;
    if (chooseSourcesList.length === 0) {
      Message(t('common.tdcomp.chooseNoData'));
      return;
    }

    activeId.value = type;

    await getAccount(chooseSourcesList);
    const checkedList = defaultCheckedList();
    chooseSourcesList.forEach((item) => {
      const { account, category, code } = item;
      if (category === CategoryEnum.monitor && !code) {
        return;
      }
      if (category !== CategoryEnum.monitor && !account) {
        return;
      }
      checkedList[CategoryEnum[category]]?.push(item);
    });

    const allChecked: any[] = [];
    Object.values(checkedList).forEach((item) => allChecked.push(...item));

    switch (type) {
      case 'Group': {
        // 一键建群
        createGroup({
          ballCamera: checkedList.ballCamera,
          confTerminal: checkedList.confTerminal,
          pdt: checkedList.pdt,
          person: checkedList.person,
          recorder: checkedList.recorder,
          seat: checkedList.seat,
          terminal: checkedList.terminal,
        });
        break;
      }
      case 'MessageSend': {
        // 一键信息
        const checkedData = [
          ...checkedList.ballCamera,
          ...checkedList.recorder,
          ...checkedList.terminal,
          ...checkedList.person,
          ...checkedList.confTerminal,
          ...checkedList.seat,
        ];
        openMessageSendCard(checkedData);
        break;
      }
      case 'VideoConf': {
        // 一键组会
        createConferenceHandler(allChecked, true, true, true);
        break;
      }
      case 'VideoWatch': {
        // 一键调阅
        playCheckedVideoWatch(allChecked);
        break;
      }
      case 'VoiceConf': {
        // 一键呼叫
        const checkedData = [
          ...checkedList.ballCamera,
          ...checkedList.recorder,
          ...checkedList.terminal,
          ...checkedList.person,
          ...checkedList.confTerminal,
          ...checkedList.seat,
        ];
        createConferenceHandler(checkedData, false, true, true);
        break;
      }
    }
  }

  // 地图屏批量视频查看
  async function playCheckedVideoWatch(data) {
    const drawerLen = monitorStore.monitorDrawerData.length;
    const len = 16 - drawerLen;
    if (len < data.length) {
      Message(t('monitor.monitorTips.maxLengthGoDesktop'));
    }
    for (let i = 0; i < len; i++) {
      const item = data[i];
      if (item) {
        monitorPlay(item);
        await delay(500);
      }
    }
  }
</script>

<template>
  <div class="quick-operate">
    <SelectedTree v-if="!isSupport" />

    <div class="btn-wrapper">
      <template v-for="item in quickOperateArr" :key="item.id">
        <TdTooltip v-if="item.show" :content="item.name" placement="top">
          <div
            class="btn"
            :class="item.id === activeId ? 'active' : ''"
            @click.stop="operateFunc(item)"
          >
            <Icon
              :color="
                item.id === activeId
                  ? 'linear-gradient(180deg, rgba(141, 246, 246, 1) 0%, rgba(20, 167, 252, 1) 100%)'
                  : 'rgba(153, 206, 251, 1)'
              "
              :name="item.icon"
              :prefix="item.prefix"
            />
          </div>
        </TdTooltip>
      </template>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .quick-operate {
    position: relative;
    width: 100%;
    margin-top: 8px;

    .btn-wrapper {
      display: flex;
    }

    .btn {
      display: flex;
      flex: 1;
      align-items: center;
      justify-content: center;
      height: 36px;
      margin-left: 8px;
      font-size: 12px;
      font-weight: 400;
      cursor: pointer;
      background: url('@/assets/images/button/rhombus_btn_bg.png') no-repeat;
      background-size: 100% 100%;

      &:nth-of-type(1) {
        margin-left: 0;
      }

      :deep(.td-icon) {
        width: 16px;
        height: 16px;
      }
    }

    .active {
      background: url('@/assets/images/button/rhombus_btn_bg_active.png') no-repeat;
      background-size: 100% 100%;
    }
  }
</style>
