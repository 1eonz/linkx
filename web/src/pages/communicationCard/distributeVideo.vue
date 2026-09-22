<script lang="ts" setup>
  import { computed, ref, unref } from 'vue';

  import { Message } from '@/components/Message';
  import { useI18n, useUtils } from '@/hooks';
  import AddMonitorEquipmentPersonsCard from '@/pages/resource/launchResourceSelector.vue';
  import { videoFunc } from '@/plugins/mspPlayer';

  const props = defineProps<{
    playItemCode?: string;
  }>();
  const emit = defineEmits(['closeDialog']);

  const { t } = useI18n();
  const { isMapPanel } = useUtils();
  const chooseList = ref<any[]>([]);
  const showFrame = ref(true);

  const showResource = computed(() => {
    const ret = ['terminal', 'seat'];
    return ret;
  });
  const chooseListData = computed(() => {
    const ret: any = [];
    unref(chooseList).forEach((item) => ret.push(...item.data));
    return ret;
  });

  function closeWindow() {
    emit('closeDialog');
  }

  // 视频分发
  function distribute() {
    const list = unref(chooseListData);
    if (list.length > 0) {
      const isdnList: any[] = [];
      list.forEach((item) => {
        // 人员身上有多少个通讯号 就都分发
        item.serviceAccounts?.forEach((service) => {
          isdnList.push({ isdn: service.account });
        });
      });
      if (list.length < 17) {
        videoFunc.videoDispatch(props.playItemCode, isdnList);
        emit('closeDialog');
      } else {
        Message(t('monitor.playManagement.quantityBeyondTheUpperLimit'));
      }
    } else {
      Message(t('monitor.playManagement.contactsCanNotBeEmpty'));
    }
  }
</script>

<template>
  <TdFrameBox
    v-show="showFrame"
    :dragger="true"
    size="normal"
    :title="t('communication.communicationFunction.videoDestribute')"
    @close-frame-box="closeWindow"
  >
    <div class="distribute-video">
      <AddMonitorEquipmentPersonsCard
        v-model:choose-list="chooseList"
        :add-type="1"
        :box-select="isMapPanel"
        :play-item-code="playItemCode"
        :show-resource="showResource"
        @choose-from-map="showFrame = true"
        @hide-frame="showFrame = false"
      />

      <div class="btn">
        <TdButton :text="t('common.cancel')" type="normal" @click="closeWindow" />
        <TdButton :text="t('common.determine')" type="normal" @click="distribute" />
      </div>
    </div>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  .frame-box__big {
    background: linear-gradient(180deg, rgba(6 41 74 / 64%) 0%, rgba(6 41 74 / 26%) 100%);
    backdrop-filter: blur(8px);
  }

  .distribute-video {
    padding: 10px 0;

    .btn {
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

  :deep(.td-dialog) {
    z-index: 2001 !important;
  }
</style>
