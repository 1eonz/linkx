<script setup lang="ts">
  import { onBeforeUnmount } from 'vue';

  import { Dialog } from '@/components/Dialog';
  import { useI18n } from '@/hooks';
  import QuickOperate from '@/pages/communicationCenter/components/quickOperate.vue';
  import { usePlanStore } from '@/store';

  import SupportGroupEdit from './supportGroupEdit.vue';
  import SupportGroupList from './supportGroupList.vue';

  const emit = defineEmits(['close']);
  const { t } = useI18n();
  const planStore = usePlanStore();

  onBeforeUnmount(() => {
    planStore.initChooseData();
  });

  function handleEdit() {
    Dialog({
      cid: 'SupportGroupEdit',
      content: SupportGroupEdit,
      offset: ['43%', '94px'],
    });
  }

  function handleClose() {
    emit('close');
    Dialog('SupportGroupEdit')?.close();
  }

  function checkChange(checkData) {
    planStore.addChooseSourcesList(checkData);
  }
</script>

<template>
  <TdFrameBox
    class="support-group-manage"
    :title="t('planSafety.detailTabs.group')"
    @close-frame-box="handleClose"
  >
    <SupportGroupList :operation="true" @check-change="checkChange" />
    <div class="buttons">
      <TdButton :text="t('monitor.monitorFunction.edit')" type="normal" @click="handleEdit" />
      <QuickOperate :is-support="true" />
    </div>
  </TdFrameBox>
</template>

<style scoped lang="less">
  .support-group-manage {
    position: fixed;
    top: 80px;
    right: 96px;
    z-index: 10;
    height: calc(100vh - 124px);

    :deep(.frame-box-container) {
      display: flex;
      flex-direction: column;
      padding: 0;
    }

    .buttons {
      padding: 10px;

      .td-button {
        width: 100%;
      }
    }
  }
</style>
