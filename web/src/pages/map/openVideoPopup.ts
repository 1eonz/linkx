import { Dialog } from '@/components/Dialog';
import { Message } from '@/components/Message';
import { useI18n } from '@/hooks';
import { useMainStoreWithOut } from '@/store';

import VideoPopup from './videoPopup.vue';
import VoicePopup from './voicePopup.vue';

type Popups = {
  cid: string;
  offsetIndex: number;
};

type Config = {
  account: number;
  infoData: any;
  onOpen?: () => void;
  title: string;
};

const popups: Popups[] = [];
const is1080P = document.documentElement.clientHeight < 1100;
const position = [
  ['75%', `${80}px`],
  ['75%', `${80 + 240}px`],
  ['75%', `${80 + 2 * 240}px`],
  is1080P ? ['50%', `${80}px`] : ['75%', `${80 + 3 * 240}px`],
];

const cids: string[] = []; // 视频查看卡片数
const queue: Config[] = []; // 播放队列

// 是否超出4屏
export function isMoreThan4VideoPopup(notTips?: boolean) {
  const { t } = useI18n();
  if (cids.length >= 4) {
    if (!notTips) {
      Message(`${t('monitor.monitorTips.maxPlay')} 4`);
    }
    return true;
  }
  return false;
}

// 视频弹窗公共方法
export function openVideoPopup(config) {
  const cid = `videoPopup${config.account}`;

  // 如果存在
  if (Dialog(cid) || cids.includes(cid)) return;

  cids.push(cid);
  queue.push(config);

  if (queue.length > 1) return;

  openPopup();
}

// 打开弹窗
async function openPopup() {
  const { account, infoData, onOpen, title } = queue[0];
  const cid = `videoPopup${account}`;

  // 延迟 防止快速点开然后关闭（通信有延时）
  setTimeout(() => {
    const { drawerVisible } = useMainStoreWithOut();
    let offsetIndex;

    const posi = position.map((item) => {
      return [drawerVisible ? '60%' : item[0], item[1]];
    });

    for (let i = 0; i < 4; i++) {
      const index = popups.findIndex((item) => {
        return item.offsetIndex === i;
      });
      if (index === -1) {
        offsetIndex = i;
        break;
      }
    }

    popups.push({
      cid,
      offsetIndex,
    });

    Dialog({
      cid,
      content: VideoPopup,
      data: {
        account,
        cid,
        infoData,
        title,
      },
      offset: posi[offsetIndex], // left top
      onClose: () => {
        const index = popups.findIndex((item) => {
          return item.cid === cid;
        });
        popups.splice(index, 1);

        if (cids.length === 0) return; // 不能减成负数
        cids.splice(index, 1);
      },
      onOpen: () => {
        onOpen?.();
      },
    });

    queue.splice(0, 1);
    if (queue.length > 0) {
      openPopup();
    }
  }, 0);
}

export function openVideoPointCallPopup(data) {
  const cid = `videoPointCallPopup${data.account}`;
  Dialog({
    cid,
    content: VideoPopup,
    data: {
      account: data.account,
      cid,
      infoData: data,
    },
  });
}

export function openVoicePointCallPopup(data) {
  const cid = `voicePointCallPopup${data.isdn}`;
  Dialog({
    cid,
    content: VoicePopup,
    data: {
      cid,
      data,
    },
    offset: ['85%', '80%'],
  });
}
