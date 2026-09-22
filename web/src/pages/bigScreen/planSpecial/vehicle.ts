import { Dialog } from '@/components/Dialog';
import { Message } from '@/components/Message';
import { DialogEnum } from '@/enums';
import { useI18n } from '@/hooks';
import EditVehicleIllusion from '@/pages/bigScreen/planSpecial/editVehicleIllusion.vue';
import HeaderCarPopup from '@/pages/bigScreen/planSpecial/headerCarPopup.vue';
import PollTime from '@/pages/resource/videoPatrol/pollTime.vue';
import { usePlanStoreWithOut, useVehicleStoreWithOut, useVideoPollStoreWithOut } from '@/store';

const { t } = useI18n();

// 车辆幻化
export function editVehicleIllusionPopup(data) {
  const { vehicleList } = useVehicleStoreWithOut();
  vehicleList.forEach((item) => {
    if (item.id === data.id) {
      data = item;
    }
  });
  Dialog('EditVehicleIllusion')?.close();
  Dialog({
    cid: 'EditVehicleIllusion',
    content: EditVehicleIllusion,
    data: {
      info: data,
    },
    offset: ['980px', '100px'],
  });
}

/**
 * 开始预案保障
 * @param recover 退出全屏模式复原
 */
export function startPlanSafety(recover?: boolean) {
  const { headerCar } = useVehicleStoreWithOut();
  const { clearVideoPollTimer } = useVideoPollStoreWithOut();
  const { planData } = usePlanStoreWithOut();

  if (!headerCar) {
    Message(t('resource.vehicle.configVehicleHeadCar'));
    return;
  }

  if (recover) {
    startVideoPolling();
    return;
  }

  // 先关闭之前的轮巡
  clearVideoPollTimer();

  // 先配置轮巡时间
  Dialog({
    cid: 'pollTimeSetting',
    content: PollTime,
    data: {
      onPollStart: () => {
        startVideoPolling();
      },
      playingPoll: {
        groupName: planData.supportName,
      },
      pollVideos: [],
    },
  });
}

/**
 * 开始保障 - 开始轮巡/头车弹窗
 */
function startVideoPolling() {
  useVehicleStoreWithOut().setUnderProtection(true);
  Dialog({
    cid: DialogEnum.HEADER_CAR_POPUP,
    content: HeaderCarPopup,
    data: {},
  });
}
