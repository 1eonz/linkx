<script lang="ts" setup>
  import ChartGroupTree from '@/pages/tree/chartGroupTree/index.vue';
  import GroupTree from '@/pages/tree/groupTree.vue';
  // import { queryPatchGroups } from '@/api/group';
  import { openGroupDetailsPopup } from '@/comm/group';
  import MessageBox from '@/components/MessageBox';
  import { useI18n } from '@/hooks';
  import {
    createGroup,
    // createPatchGroup
  } from '@/pages/resource/groupHelper';
  import { useCommunicationStore } from '@/store';

  withDefaults(
    defineProps<{
      filterText?: string;
    }>(),
    {
      filterText: '',
    },
  );
  const { t } = useI18n();
  const communicationStore = useCommunicationStore();

  function addGroup() {
    createGroup();
  }

  // async function addPatchGroup() {
  //   const { code, data } = await queryPatchGroups({});
  //   if (code === 0) {
  //     let defaultList: any[] = [];
  //     data.forEach((element) => {
  //       element.userBOList.forEach((item) => {
  //         defaultList.push(item.membergroup);
  //       });
  //     });
  //     createPatchGroup(defaultList);
  //   }
  // }

  async function singleClick(data) {
    if (communicationStore.hasComm) {
      communicationStore.setGroupInfoCard({ ...data });
      if (data.grpnumber) {
        openGroupDetailsPopup({
          data,
          id: data.grpnumber,
          isPatchGroup: true,
        });
      } else {
        openGroupDetailsPopup({
          data,
          id: data.groupId,
        });
      }
    } else {
      const res = await MessageBox({
        offset: ['40%', '30%'],
        text: t('resource.group.communicationOutage'),
        type: 'ok',
      });
      if (res) {
        location.reload();
      }
    }
  }
</script>

<template>
  <div class="group-list">
    <div class="group-tree-content">
      <!-- 群聊组 -->
      <ChartGroupTree />
      <!-- 动态群组 -->
      <GroupTree
        :can-drag="true"
        :default-check-keys="[]"
        :filter-text="filterText"
        group-type="0"
        :show-collect="true"
        @click="singleClick"
      />
      <!-- 静态群组 -->
      <GroupTree
        :can-drag="true"
        :default-check-keys="[]"
        :filter-text="filterText"
        group-type="1"
        :show-collect="true"
        @click="singleClick"
      />
      <!-- 派接组 -->
      <!-- <GroupTree
        :filter-text="filterText"
        :show-collect="false"
        :can-drag="true"
        group-type="2"
        :default-check-keys="[]"
        @click="singleClick"
      /> -->
    </div>
    <!-- 按钮 -->
    <div class="group-btn">
      <TdButton
        class="btn"
        :text="t('resource.group.newDynamicGroup')"
        type="normal"
        @click="addGroup"
      />
      <!-- <TdButton
        class="btn"
        type="normal"
        @click="addPatchGroup"
        :text="t('resource.group.newPatchGroup')"
      /> -->
    </div>
  </div>
</template>

<style lang="less" scoped>
  .group-list {
    height: 100%;

    .group-tree-content {
      height: calc(100% - 36px);
      overflow-y: scroll;
    }

    .group-btn {
      display: flex;
      justify-content: space-between;

      .btn {
        width: 100%;
      }
    }
  }
</style>
