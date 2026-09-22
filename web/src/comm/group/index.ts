import { Dialog } from '@/components/Dialog';
import { setZIndex } from '@/components/Dialog/src/helper';
import GroupDetailCard from '@/pages/resource/group/groupDetailCard.vue';
import PatchGroupDetailCard from '@/pages/resource/group/patchGroupDetailCard.vue';

interface GroupDetails {
  data?: any;
  id: string;
  isPatchGroup?: boolean;
  offset?: any;
  simple?: boolean;
}

/**
 * 群组消息弹窗
 * 聊天窗口只能打开一个
 */
const groupDialogCidList: string[] = [];
const pop = 'GroupDetailCard';

export function openGroupDetailsPopup(props: GroupDetails) {
  const { data, id, isPatchGroup = false, offset, simple = true } = props;
  const cid = pop + id;

  if (groupDialogCidList.includes(cid)) {
    setZIndex(cid);
    return;
  }

  const len = groupDialogCidList.length;
  const offsetNow = [`${434 + len * 10}px`, `${94 + len * 10}px`];

  const config = {
    cid,
    content: isPatchGroup ? PatchGroupDetailCard : GroupDetailCard,
    data: isPatchGroup
      ? {
          data,
          infoId: id,
        }
      : {
          groupId: id,
          showSimple: simple,
          uid: cid,
        },
    offset: offset || offsetNow,
    onClose() {
      const index = groupDialogCidList.indexOf(cid);
      groupDialogCidList.splice(index, 1);
    },
  };

  Dialog(config);
  groupDialogCidList.push(cid);
}

// 关闭去群组详情弹窗
export function closeGroupDetailsPopup(groupId) {
  Dialog(pop + groupId)?.close();
}
