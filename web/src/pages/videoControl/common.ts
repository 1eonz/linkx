import { Dialog } from '@/components/Dialog';
import { appConfig } from '@/config';
import { useEmitter, useI18n } from '@/hooks';
import AlarmRemind from '@/pages/notification/alarmRemind.vue';
import { guid } from '@/utils';

import ControlTaskDetail from './controlTask/controlTaskDetail.vue';
import WarningNoticeDetail from './warningNotice/warningNoticeDetail.vue';

const { t } = useI18n();
/**
 * 打开设防详情卡片
 * @param id
 */
export function openControlTaskCard(id: string) {
  Dialog({
    cid: `controlTaskDetail${id}`,
    content: ControlTaskDetail,
    data: {
      isDbClick: true,
      suspectTaskId: id,
    },
    offset: ['410px', '60px'],
  });
  useEmitter().emit('closeLastControlCid', id);
}

/**
 * 打开预警详情卡片
 * @param warningNoticeId
 * @param isMissionCard
 * @param warningData
 */
export function openWarningNoticeCard(
  warningNoticeId: string,
  isMissionCard?: boolean,
  warningData?: any,
) {
  Dialog({
    cid: 'warningNoticeDetail',
    content: WarningNoticeDetail,
    data: {
      isDbClick: true,
      isMissionCard, // 任务中弹出预警卡片样式
      warningData,
      warningNoticeId,
    },
    offset: ['530px', '84px'],
  });
  useEmitter().emit('closeLastWarningCid', warningNoticeId);
}

/**
 * 打开声音/弹窗/边框闪烁提示
 * @param infoData
 * @param message 提醒等级
 */
const cidList: string[] = [];
export function handleRemind(infoData: any, message: any) {
  const { remindSwitch } = appConfig;
  if (!remindSwitch) return;
  const { alarmId, config, flowId, popTime, remindLevel } = message;
  const configArr = config.split(',');
  let type = 3; // 1预警 2任务 3紧急事件
  if (alarmId) {
    type = 1;
  } else if (flowId) {
    type = 2;
  }
  configArr?.forEach((item) => {
    switch (item) {
      case '1': {
        // 声音
        useEmitter().emit('playVoiceRemind', type);
        break;
      }
      case '2': {
        // 弹窗
        if (cidList.length > 6) return;
        const cid = `remind${guid()}`;
        Dialog({
          cid,
          content: AlarmRemind,
          data: {
            infoData,
            popTime,
            remindLevel,
            type,
          },
          offset: ['80%', `${80 + cidList.length * 110}px`],
          onClose() {
            const index = cidList.indexOf(cid);
            cidList.splice(index, 1);
          },
          shade: false,
          zIndexDefault: 99_999,
        });
        cidList.push(cid);
        break;
      }
      case '3': {
        // 闪烁
        useEmitter().emit('changeBorderRemind', popTime);
        break;
      }
    }
  });
}

/**
 * 预警状态--文字
 * @param warningData
 */
export function statusText(warningData: any) {
  const { operationType, status } = warningData;
  if (status === 0) {
    return t('videoControl.warningInformation.untreated');
  } else {
    const texts = {
      1: t('videoControl.warningInformation.ignored'),
      2: t('videoControl.warningInformation.treated'),
    };
    return texts[operationType];
  }
}

/**
 * 预警状态--颜色
 * @param warningData
 */
export function statusColor(warningData: any) {
  const { status } = warningData;
  switch (status) {
    case 0: {
      return 'red';
    }
    default: {
      return 'gray';
    }
  }
}

/**
 * 预警等级--文字
 * @param warningData
 */
export function levelText(warningData: any) {
  const { alarmLevel } = warningData;
  switch (alarmLevel * 1) {
    case 1: {
      return t('videoControl.createEvent.eventLevelFirst');
    }
    case 2: {
      return t('videoControl.createEvent.eventLevelTwo');
    }
    case 3: {
      return t('videoControl.createEvent.eventLevelThree');
    }
    case 4: {
      return t('videoControl.createEvent.eventLevelFour');
    }
    default: {
      return t('videoControl.createEvent.eventLevelFirst');
    }
  }
}

/**
 * 预警等级--颜色
 * @param warningData
 */
export function levelColor(warningData: any): any {
  const { alarmLevel } = warningData;
  switch (alarmLevel * 1) {
    case 1: {
      return 'red';
    }
    case 2: {
      return 'orange';
    }
    case 3: {
      return 'yellow';
    }
    case 4: {
      return 'yellow';
    }
    default: {
      return 'red';
    }
  }
}

/**
 * 提醒等级--颜色
 * @param warningData
 */
export function remindColor(remindLevel: any) {
  switch (remindLevel * 1) {
    case 1: {
      return 'red';
    }
    case 2: {
      return 'orange';
    }
    case 3: {
      return 'yellow';
    }
    case 4: {
      return 'blue';
    }
    default: {
      return 'red';
    }
  }
}
