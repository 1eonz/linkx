<script lang="ts" setup>
  import { computed, ref } from 'vue';

  import { Message } from '@/components/Message';
  import { CategoryEnum } from '@/enums';
  import { useEmitter, useI18n, useUtils } from '@/hooks';
  import { openMessageSendCard } from '@/pages/communicationCard/openResourcesCard';
  import { createGroup } from '@/pages/resource/groupHelper';
  import {
    getAccountByEquipmentData,
    getInfoByAccount,
    getResourceTypes,
  } from '@/pages/resource/resourceHelper';
  import { monitorPlay } from '@/pages/resource/videoHelper';
  import CheckedTree from '@/pages/tree/checkedTree.vue';
  import { createConferenceHandler } from '@/pages/videoConference/common';
  import commOpt from '@/plugins/mspPlayer/commOpt';
  import { useMonitorStore, useTreeStore } from '@/store';
  import { delay } from '@/utils';
  import { isArray } from '@/utils/is';

  const props = defineProps({
    type: {
      default: '',
      type: String,
    },
  });
  const emit = defineEmits(['closeDialog']);

  const { t } = useI18n();
  const { isMapPanel } = useUtils();
  const treeStore = useTreeStore();
  const monitorStore = useMonitorStore();

  const filterText = ref('');

  const confirmDisable = computed(() => {
    const { type } = props;
    const { resourceCheckedList } = treeStore;

    let total = 0;
    Object.values(resourceCheckedList).forEach((item) => {
      total += item.length;
    });
    if (total === 0) {
      return true;
    }

    if (type === 'MessageSend') {
      return getData(type).length === 0;
    }
    return false;
  });
  const treeComponents = computed(() => {
    const arr = getResourceTypes();
    return arr.map((item) => {
      const { id, type } = item;
      const category = Number(id);
      let show = true;
      switch (props.type) {
        case 'Group': {
          // 一键建群
          show = [
            CategoryEnum.ballCamera,
            CategoryEnum.pdt,
            CategoryEnum.recorder,
            CategoryEnum.seat,
            CategoryEnum.terminal,
          ].includes(category);
          break;
        }
        case 'MessageSend': {
          // 一键短信
          show = [CategoryEnum.person, CategoryEnum.seat, CategoryEnum.terminal].includes(category);
          break;
        }
        case 'VideoConf': {
          // 一键组会
          show = [
            CategoryEnum.ballCamera,
            CategoryEnum.carPhoto,
            CategoryEnum.confTerminal,
            CategoryEnum.GBRecorder,
            CategoryEnum.monitor,
            CategoryEnum.person,
            CategoryEnum.recorder,
            CategoryEnum.seat,
            CategoryEnum.terminal,
            CategoryEnum.uav,
          ].includes(category);
          break;
        }
        case 'VideoWatch': {
          // 一键调阅
          show = [
            CategoryEnum.ballCamera,
            CategoryEnum.carPhoto,
            CategoryEnum.GBRecorder,
            CategoryEnum.monitor,
            CategoryEnum.person,
            CategoryEnum.recorder,
            CategoryEnum.terminal,
            CategoryEnum.terminal,
            CategoryEnum.uav,
          ].includes(category);
          break;
        }
        case 'VoiceConf': {
          // 一键呼叫
          show = [
            CategoryEnum.ballCamera,
            CategoryEnum.confTerminal,
            CategoryEnum.person,
            CategoryEnum.recorder,
            CategoryEnum.seat,
            CategoryEnum.terminal,
          ].includes(category);
          break;
        }
        default: {
          break;
        }
      }
      const data = treeStore.resourceCheckedList[type] || [];
      Object.assign(item, {
        data,
        disable: !show,
        total: data.length,
      });
      return item;
    });
  });

  function getData(type) {
    if (type === 'MessageSend') {
      const { person, seat, terminal } = treeStore.resourceCheckedList;
      return [...terminal, ...person, ...seat];
    }
    return [];
  }

  async function handleConfirm() {
    const { type } = props;
    const { resourceCheckedList } = treeStore;

    if (!commOpt.isCommReady()) {
      return;
    }

    if (type === 'MessageSend') {
      const arr = getData(type);
      const ret = arr.filter((i) => {
        const account = getAccountByEquipmentData(i);
        return !!account;
      });

      if (ret.length === 0) {
        Message(t('communication.communicationTips.communicationAccountNotAssociated'));
        return;
      }
      openMessageSendCard(arr);
    }

    if (type === 'Group') {
      const list = { ...resourceCheckedList };
      const { person } = list;
      if (isArray(person)) {
        for (const p of person) {
          for (const a of p.serviceAccounts) {
            const info = await getInfoByAccount(a.account);
            const target = list[CategoryEnum[info.category]];
            const index = target?.findIndex((i) => i.id === info.id);
            if (index === -1) {
              target.push(info);
            }
          }
        }
      }

      const ret: number[] = [];
      Object.keys(list).forEach((i) => {
        list[i].forEach((j) => {
          const account = getAccountByEquipmentData(j);
          if (account) {
            ret.push(account);
          }
        });
      });
      if (ret.length === 0) {
        Message(t('communication.communicationTips.communicationAccountNotAssociated'));
        return false;
      }
      createGroup(list);
    }

    treeStore.setResourceOneKeyType(type);

    // 地图屏时
    if (isMapPanel) {
      openCardOnMapCenter(type);
    } else {
      useEmitter().emit('confDesktopCreate');
      useEmitter().emit('monitorDesktopPlay');
    }

    handleClose();
  }

  // 在地图屏一键操作
  function openCardOnMapCenter(type) {
    const {
      ballCamera,
      carPhoto,
      confTerminal,
      GBRecorder,
      monitor,
      person,
      recorder,
      seat,
      terminal,
      uav,
    } = treeStore.resourceCheckedList;
    switch (type) {
      case 'VideoConf': {
        const checkedData = [
          ...recorder,
          ...terminal,
          ...monitor,
          ...carPhoto,
          ...GBRecorder,
          ...person,
          ...uav,
          ...confTerminal,
          ...seat,
          ...ballCamera,
        ];
        createConferenceHandler(checkedData, true, true, true);
        break;
      }
      case 'VideoWatch': {
        const checkedData = [
          ...recorder,
          ...terminal,
          ...monitor,
          ...carPhoto,
          ...GBRecorder,
          ...person,
          ...uav,
          ...ballCamera,
        ];
        playCheckedVideoWatch(checkedData);
        break;
      }
      case 'VoiceConf': {
        const checkedData = [
          ...recorder,
          ...terminal,
          ...person,
          ...confTerminal,
          ...seat,
          ...ballCamera,
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
        if (item.resourceType === 'Executor' || item.category === CategoryEnum.person) {
          item.serviceAccounts.forEach(async (service) => {
            const info = await getInfoByAccount(service.account);
            monitorPlay({
              ...item,
              account: service.account,
              id: info.id,
              name: item.name,
              serviceAccounts: [service],
            });
          });
        } else {
          monitorPlay(item);
        }
        await delay(700);
      }
    }
  }

  function handleClose() {
    emit('closeDialog');
    useEmitter().emit('checkedListCardClose');
    treeStore.setResourceOneKeyType('');
  }
</script>

<template>
  <TdFrameBox
    class="checked-list-card"
    :dragger="true"
    :title="t('common.tdcomp.chooseData')"
    @close-frame-box="handleClose"
  >
    <!-- 搜索 -->
    <TdInput
      v-model="filterText"
      class="search"
      :placeholder="t('common.search.inputContent')"
      type="searchInput"
    />

    <!-- 列表 -->
    <div class="content">
      <CheckedTree
        v-for="item in treeComponents"
        :key="item.id"
        :checked-data="item"
        :disable="item.disable"
        :filter-text="filterText"
      />
    </div>

    <!-- 按钮 -->
    <div class="buttons">
      <TdButton class="cancel-btn" :text="t('common.cancel')" type="normal" @click="handleClose" />
      <TdButton
        class="confirm-btn"
        :disable="confirmDisable"
        :text="t('common.determine')"
        type="normal"
        @click="handleConfirm"
      />
    </div>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  .checked-list-card {
    .search {
      margin: 10px 0;
    }

    .content {
      height: 490px;
      overflow-y: auto;
    }

    .buttons {
      display: flex;
      justify-content: flex-end;
      margin: 10px 0;
      text-align: center;

      .cancel-btn {
        width: 50px;
        height: 24px;
        margin-right: 8px;
      }

      .confirm-btn {
        width: 50px;
        height: 24px;
      }
    }
  }
</style>
