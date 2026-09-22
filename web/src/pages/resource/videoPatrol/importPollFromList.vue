<script lang="ts" setup>
  import { computed, ref, unref } from 'vue';

  import { useI18n } from '@/hooks';
  import AddMonitorEquipmentPersonsCard from '@/pages/resource/launchResourceSelector.vue';
  import { getResourceTypes } from '@/pages/resource/resourceHelper';

  const props = defineProps<{
    default: any[];
  }>();
  const emit = defineEmits(['closeDialog', 'success']);
  const { t } = useI18n();

  const showGroupAddCard = ref(true);
  const chooseList = ref<any>(getResourceTypes());
  const showResource = ['carPhoto', 'monitor', 'recorder', 'ballCamera'];

  const defaultList = computed(() => {
    const ret = getResourceTypes();
    const obj = {};
    props.default.forEach((item) => {
      if (!obj[item.category]) {
        obj[item.category] = [];
      }
      obj[item.category].push(item);
    });
    ret.forEach((i) => {
      i.data = obj[i.id];
    });
    return ret;
  });

  async function confirm() {
    const arr: any[] = [];
    unref(chooseList).forEach((i) => {
      if (showResource.includes(i.type)) {
        arr.push(...i.data);
      }
    });
    emit('success', arr);
    closeWindow();
  }

  function closeWindow() {
    emit('closeDialog');
  }
</script>

<template>
  <!--新建/修改群组  -->
  <TdFrameBox
    v-show="showGroupAddCard"
    class="import-poll-from-list"
    :dragger="true"
    size="normal"
    :title="t('resource.poll.importFromList')"
    @close-frame-box="closeWindow"
  >
    <AddMonitorEquipmentPersonsCard
      v-model:choose-list="chooseList"
      :add-type="1"
      :box-select="false"
      :default-choose-list="defaultList"
      :show-resource="showResource"
      @choose-from-map="showGroupAddCard = true"
      @hide-frame="showGroupAddCard = false"
    />

    <div class="button-group">
      <TdButton :text="t('common.cancel')" type="normal" @click="closeWindow" />
      <TdButton :text="t('login.query')" type="normal" @click="confirm" />
    </div>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  .import-poll-from-list {
    :deep(.frame-box-container) {
      padding: 10px;
    }

    .button-group {
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
