<script setup lang="ts">
  import { onMounted, ref } from 'vue';

  import { Dialog } from '@/components/Dialog';
  import { useI18n } from '@/hooks';
  import AddSafetyGroup from '@/pages/planSafety/addPlanSafety/addSafetyGroup.vue';
  import { usePlanStore } from '@/store';

  const { t } = useI18n();
  const planStore = usePlanStore();
  const defaultGroup = ref<any[]>([]);
  const changeData = ref<any[]>([]);

  onMounted(() => {
    defaultGroup.value = [...planStore.supportGroup];
  });

  function change(data) {
    changeData.value = data;
  }
  async function handleConfirm() {
    const res = await planStore.updatePlanData(changeData.value);
    if (res) {
      handleClose();
    }
  }

  function handleClose() {
    Dialog('SupportGroupEdit')?.close();
  }
</script>

<template>
  <TdFrameBox
    class="support-group-edit"
    :dragger="true"
    size="small"
    :title="t('planSafety.editPlanGroup')"
    @close-frame-box="handleClose"
  >
    <AddSafetyGroup :default-group="defaultGroup" @change="change" />
    <div class="buttons">
      <TdButton :text="t('common.cancel')" type="normal" @click="handleClose" />
      <TdButton :text="t('common.determine')" type="normal" @click="handleConfirm" />
    </div>
  </TdFrameBox>
</template>

<style scoped lang="less">
  .support-group-edit {
    :deep(.frame-box-container) {
      padding: 10px;
    }

    .buttons {
      display: flex;
      justify-content: flex-end;
      margin-top: 10px;
      text-align: center;

      .td-button {
        width: 60px;
        height: 32px;
        margin-left: 12px;
      }
    }
  }
</style>
