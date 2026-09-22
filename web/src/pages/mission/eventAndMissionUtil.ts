import { Dialog } from '@/components/Dialog';
import ImagePreview from '@/components/ImagePreview/index.vue';
import { Message } from '@/components/Message';
import VideoPlayer from '@/components/VideoPlayer/index.vue';
import { useI18n } from '@/hooks';
import { getLang } from '@/locales';
import SendPolice from '@/pages/mission/missionCenter/missionAxis/axisNode/sendPolice.vue';
import MissionCenterCard from '@/pages/mission/missionCenter/missionCenterCard.vue';
import { useMainStore, useMissionStore } from '@/store';

let cardTop = 5;
let cardCount = 0;

export default {
  closeCard() {
    cardCount--;
  },

  evenAndMissionDataInit(data): any {
    const lang = getLang() === 'en' ? 'en' : 'zh';
    const { flowConfig } = useMissionStore();

    const { t } = useI18n();
    const unknown = t('mission.missionList.unknown');

    const ret = data.map((item) => {
      const obj: any = {
        id: item.flowId,
        type: String(item.type),
      };

      const { payload } = item;
      const { i18nConf, view } = flowConfig.get(obj.type);

      // 等级
      const levelI18n = view.definition.find((i) => i.name === 'importantLevel').extensions
        .typeReference.reference[payload.importantLevel];
      obj.importantLevelName = i18nConf[lang][levelI18n];

      // 状态
      obj.stateName = i18nConf[lang][i18nConf.prefix.status + item.status] || unknown;

      obj.position = [payload.longitude, payload.latitude];

      return { ...item, ...obj };
    });
    return ret;
  },

  // 等级颜色
  levelColor(level) {
    switch (level) {
      case '1': {
        return 'red';
      }
      case '2': {
        return 'yellow';
      }
      case '3': {
        return 'green';
      }
      case '4': {
        return 'blue';
      }
      default: {
        return 'gray';
      }
    }
  },

  openMissionCard(flowId) {
    const { t } = useI18n();
    if (cardCount > 4) {
      Message(t('mission.missionCenter.msgWindowReachedLimitDetail'));
      return;
    }
    const { setLayerId } = useMainStore();
    const cid = `missionCard${flowId}`;
    Dialog({
      cid,
      content: MissionCenterCard,
      data: { flowId },
      offset: ['530px', '84px'],
    });
    setLayerId(cid);
    if (Dialog(`missionCard${flowId}`)) {
      return;
    }
    cardTop += 2;
    cardCount += 1;
    if (cardTop > 16) {
      cardTop = 5;
    }
  },

  openPlayImage(urlList, id) {
    const d = Dialog({
      cid: `imgPreview${id}`,
      content: ImagePreview,
      data: {
        imageUrl: urlList,
        maximize() {
          d.fullScreen();
        },
      },
    });
  },

  openPlayVideo(url, eventId) {
    const d = Dialog({
      cid: `videoPlayer${eventId}`,
      content: VideoPlayer,
      data: {
        itemId: eventId,
        maximize() {
          d.fullScreen();
        },
        videoUrl: url,
      },
    });
  },

  openSendPoliceCard(actionGroupTemplateId, position, eventId, missionId) {
    Dialog({
      cid: 'sendPoliceCard',
      content: SendPolice,
      data: {
        actionGroupTemplateId,
        eventId,
        missionId,
        position,
      },
      offset: [`${32}%`, `${0}%`],
    });
  },

  // 状态颜色
  statusColor(status) {
    let state = '';

    const { getStatus } = useMissionStore();
    Object.keys(getStatus).forEach((key) => {
      if (getStatus[key].includes(status)) {
        state = key;
      }
    });

    switch (state) {
      case 'finish': {
        return 'gray';
      }
      case 'pending': {
        return 'red';
      }
      case 'underway': {
        return 'yellow';
      }
      default: {
        return 'gray';
      }
    }
  },
};
