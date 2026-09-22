<script lang="ts" setup>
  import { computed, onMounted, ref, unref, watch } from 'vue';

  import { queryFacilitiesByEquipmentId } from '@/api/facility';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { VideoCallEnum } from '@/enums';
  import { useEmitter, useI18n } from '@/hooks';
  import {
    getAccountByEquipmentData,
    monitorCall,
    queryResourceDetailsByInfo,
  } from '@/pages/resource/resourceHelper';
  import { monitorPlay } from '@/pages/resource/videoHelper';
  import commOpt from '@/plugins/mspPlayer/commOpt';
  import { useCommunicationStore, useVehicleStore } from '@/store';
  import { isDef } from '@/utils/is';

  import CallWay from './callWay.vue';
  import { callWayDialogPosition } from './callWayPosition';
  import CarVideoWatch from './carVideoWatch.vue';

  const props = defineProps({
    account: {
      default: '',
      type: String,
    },
    btnType: {
      default: '',
      type: String,
    },
    info: {
      default: () => {},
      type: Object,
    },
    onlyCall: Boolean,
    resourceType: {
      default: '',
      type: String,
    },
    size: {
      default: '',
      type: String,
    },
  });
  defineExpose({ trigger: monitorShowClick });
  const { t } = useI18n();
  const communicationStore = useCommunicationStore();
  const vehicleStore = useVehicleStore();

  const popoverVisible = ref(false);
  const isActive = ref(false);
  const disable = ref(false);
  const facilities = ref([]);
  const btnRef = ref();

  const callIng = computed(() => {
    return unref(isActive) && !props.onlyCall;
  });
  const isSelf = computed(() => {
    const { info } = props;
    return appConfig.userData.id === info.id;
  });
  const iconName = computed(() => {
    const name = 'monitor_call';
    return unref(callIng) ? 'phone_hang_up' : name;
  });
  const btnTypeName = computed(() => {
    const { btnType } = props;
    if (btnType === 'icon') {
      return btnType;
    }
    return unref(callIng) ? 'radioWarn' : btnType;
  });
  const isdn = computed(() => {
    return getAccountByEquipmentData(props.info);
  });

  watch(
    () => props.info,
    () => {
      handlerComm();
    },
    { immediate: true },
  );

  onMounted(() => {
    handlerComm();
    useEmitter('commUpdate', handlerComm);
  });

  /**
   * 视频查看和视频点呼不可共存
   */
  function handlerComm() {
    const { serviceAccounts } = props.info;
    if (serviceAccounts?.length > 1) {
      return;
    }

    let comm: any = communicationStore.comm?.[unref(isdn)] || {};
    serviceAccounts.forEach((item) => {
      const { account } = item;
      const val = communicationStore.comm?.[account];
      if (val) {
        comm = val;
      }
    });

    const { monitor, video } = comm;
    const onlyOne = serviceAccounts.length < 2;
    isActive.value = isDef(monitor);
    disable.value = isDef(video) && onlyOne;
  }

  function closePopover() {
    popoverVisible.value = false;
  }

  // 视频查看
  async function monitorShowClick(e) {
    const { resourceType } = props;
    const { equipmentId } = props.info;
    const details = await queryResourceDetailsByInfo(props.info);
    if (props.account) {
      details.account = props.account;
      details.serviceAccounts = details.serviceAccounts.filter((i) => i.account === props.account);
    }

    const { account, serviceAccounts } = details;
    if (unref(isActive)) {
      if (props.onlyCall) return;
      if (serviceAccounts?.length > 1) {
        serviceAccounts.forEach((item) => {
          if (communicationStore.comm[item.account]) {
            commOpt.hangUp('monitor', appConfig.isdn, item.account);
          }
        });
      } else {
        commOpt.hangUp('monitor', appConfig.isdn, unref(isdn));
      }
      return;
    }

    if (serviceAccounts?.length > 1) {
      Dialog({
        cid: 'CallWay',
        content: resourceType === 'car' ? CarVideoWatch : CallWay,
        data: {
          facilities: unref(facilities),
          onSelect: (account) => {
            // 判断是否已经通话
            if (commOpt.isAlreadyCommForSend('monitor', account)) {
              return;
            }
            monitorCall(
              {
                ...details,
                account,
                serviceAccounts: serviceAccounts.filter((i) => i.account === account),
              },
              account,
            );
            Dialog('CallWay')?.close();
          },
          serviceAccounts,
        },
        offset: callWayDialogPosition(e),
        zIndexDefault: 1300,
      });
      return;
    }

    // 判断是否已经通话
    if (commOpt.isAlreadyCommForSend('monitor', account)) {
      return;
    }

    if (resourceType === 'car') {
      if (equipmentId) {
        getCarFacilities(equipmentId);
      } else {
        Message(t('communication.communicationTips.gatewayNotAssociated'));
      }
      return;
    }

    if (resourceType === 'monitor') {
      monitorPlay(details);
      return;
    }

    const { headerCarPopup } = vehicleStore;
    if (headerCarPopup) {
      commOpt.monitorCall({
        container: VideoCallEnum.MONITOR_CONTAINER + unref(isdn),
        toUid: unref(isdn),
        type: '0',
      });
    } else {
      monitorCall({ ...details, account });
    }
  }

  async function getCarFacilities(equipmentId) {
    const { code, data } = await queryFacilitiesByEquipmentId(equipmentId);
    if (code === 0) {
      facilities.value = data;
      if (data.length === 0) {
        Message(t('monitor.createMonitorGroup.noCameraYet'));
      } else if (data.length === 1) {
        monitorPlay(data[0]);
      } else {
        popoverVisible.value = !popoverVisible.value;
      }
    }
  }
</script>

<template>
  <!-- 视频查看 -->
  <TdTooltip :content="t('communication.communicationFunction.videoWatch')" placement="top">
    <TdButton
      :active="isActive"
      :class="{
        active: isActive,
      }"
      :disable="isSelf || disable"
      :icon-name="iconName"
      :size="size"
      :type="btnTypeName"
      @click.stop="monitorShowClick"
    />
  </TdTooltip>
  <ElPopover
    placement="right-start"
    trigger="click"
    :virtual-ref="btnRef"
    virtual-triggering
    :visible="popoverVisible"
    width="290"
  >
    <CarVideoWatch
      v-if="resourceType === 'car'"
      :facilities="facilities"
      @close="popoverVisible = false"
    />
    <CallWay
      v-else
      call-type="monitorCall"
      :data="info"
      :service-accounts="info.serviceAccounts"
      @set-sopover-visible="closePopover"
    />
  </ElPopover>
</template>

<style lang="less" scoped>
  .video-watch-btn {
    display: flex;
    align-items: center;
  }
</style>
