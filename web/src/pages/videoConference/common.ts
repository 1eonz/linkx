import type {
  AddConferItemParams,
  AddConfMembersParam,
  ConfMember,
} from '@/pages/types/conference';

import {
  addConferenceItems,
  createConference,
  deleteConference,
  queryConferenceItems,
} from '@/api/conference';
import { Dialog } from '@/components/Dialog';
import { Message } from '@/components/Message';
import appConfig from '@/config/appConfig';
import { CategoryEnum } from '@/enums';
import { useI18n } from '@/hooks';
import { getAccountByEquipmentData } from '@/pages/resource/resourceHelper';
import ConferenceCard from '@/pages/videoConference/conference/conferenceCard.vue';
import { confFunc, makeSureEndConf } from '@/plugins/mspPlayer';
import commOpt from '@/plugins/mspPlayer/commOpt';
import { useCommunicationStore, useConferenceStore } from '@/store';

import { throttle } from 'lodash-es';

// 打开会议弹窗
export function conferenceCardDialog(isMap?: boolean) {
  return new Promise((resolve) => {
    Dialog({
      cid: 'conferenceCard',
      content: ConferenceCard,
      fullScreenZIndex: 'auto',
      offset: [isMap ? '250px' : '600px', '100px'],
      onOpen: () => {
        setTimeout(() => {
          resolve(true);
        }, 1000);
      },
    });
  });
}

/**
 * @description 创建会议
 * @param {array} data 成员
 * @param {boolean} isVideo 视频/语音
 * @param {boolean} dialog 弹窗
 * @param {boolean} isMap 是否在地图屏
 */
export async function createConferenceHandler(data, isVideo = true, dialog = false, isMap = false) {
  const { t } = useI18n();
  const communicationStore = useCommunicationStore();
  const { confer, setCreateConferenceMemberList } = useConferenceStore();

  if (!commOpt.isCommReady()) {
    return;
  }

  if (confer.status === 'ringing') {
    Message(t('videoConference.createConference.notCreateConfWhenRinging'));
    return;
  }

  const memberList: any[] = [];
  data.forEach((item) => {
    if (item.resourceType === 'Executor' || item.category === CategoryEnum.person) {
      item.serviceAccounts?.forEach((i) => {
        memberList.push({
          ...i,
          name: item.name,
        });
      });
      return;
    }
    memberList.push(item);
  });

  if (repeatNumber(memberList)) {
    return;
  }

  if (memberList.length > 19) {
    Message({
      message: t('videoConference.addConfMembers.notMoreThan20Error'),
      type: 'error',
    });
    return;
  }

  // 点呼和会议是冲突的
  const commList: any = Object.values(communicationStore.comm);
  for (const item of commList) {
    if (item.video || item.voice) {
      Message({
        message: t('communication.communicationTips.communicationError'),
        type: 'error',
      });
      return;
    }
  }

  if (dialog) {
    await conferenceCardDialog(isMap);
  }

  await makeSureEndConf();

  setCreateConferenceMemberList(memberList);

  const memberInfos: AddConfMembersParam[] = [];
  memberList.forEach((item) => {
    const { groupId, groupName, name } = item;
    const member = {
      h265: 'true',
      isCamera: 'false',
      isWatchOnly: 'false',
      name: name || groupName,
      number: groupId || getAccountByEquipmentData(item),
    };
    const index = memberInfos.findIndex((i) => i.number === member.number);
    if (index === -1) {
      memberInfos.push(member);
    }
  });

  confFunc.createConf(memberInfos, String(isVideo));
}

/**
 * @description 保存会议信息
 * @param {object} confer 详情
 */
export async function saveConferenceInfoHandler(confer) {
  const { t } = useI18n();
  const { confName, createConferenceMemberList, setUniqueCode } = useConferenceStore();
  const { conferenceId, conferencePass, isVideo, unifiedAccessCode } = confer;
  const createParams = {
    confId: conferenceId,
    isVideo,
    name:
      confName || `${t('videoConference.conference')}-${Math.floor(Math.random() * 8999) + 1000}`,
    password: conferencePass,
    unifiedAccessCode,
  };
  const { code, data } = await createConference(createParams);
  if (code !== 0) {
    Message({
      message: t('videoConference.statusMsg.creteFailure'),
      type: 'error',
    });
    return false;
  }
  const uniqueCode = data;
  setUniqueCode(uniqueCode);

  const { name } = appConfig.userData;
  const addConferItemParams: AddConferItemParams[] = [];
  const chair = {
    account: appConfig.isdn,
    accountType: CategoryEnum.person,
    confId: uniqueCode,
    isChairman: 1,
    name,
  };
  addConferItemParams.push(chair);
  createConferenceMemberList.forEach((item) => {
    const { category, groupId, groupName, name } = item;
    addConferItemParams.push({
      account: groupId || getAccountByEquipmentData(item),
      accountType: groupId ? 'group' : category || CategoryEnum.person,
      confId: uniqueCode,
      isChairman: 0,
      name: name || groupName,
    });
  });
  await addConferenceItems(addConferItemParams);
}

/**
 * @description 删除会议
 */
export async function deleteConferenceHandler(): Promise<any> {
  const { uniqueCode } = useConferenceStore();
  if (uniqueCode) {
    return await deleteConference({ id: uniqueCode });
  }
  return {};
}

/**
 * @description 校验通信号是否重复
 * @param {array}  memberList 成员
 * @returns Boolean
 */
export function repeatNumber(memberList) {
  const { t } = useI18n();
  const number: string[] = [];
  let hasSelf = false;
  const fourGR: string[] = [];
  memberList.forEach((item) => {
    const num = item.account || item.code || item.groupId;
    const { capability } = item;
    // 过滤4G记录仪
    if (
      Number(item.category) === CategoryEnum.recorder &&
      capability &&
      (!capability.includes('512007') || !capability.includes('512006'))
    ) {
      fourGR.push(item.name);
    }
    // 不能选自己
    if (num === appConfig.isdn) {
      hasSelf = true;
    }
    number.push(num);
  });

  if (hasSelf) {
    Message(t('monitor.monitorTips.canNotPickYourSelf'));
    return true;
  }

  if (fourGR.length > 0) {
    const msg =
      fourGR.join(',') + t('communication.communicationTips.equipmentCapabilityNotSupported');
    Message(msg);
    return true;
  }

  return false;
}

// 获取与会人员
export const getConferenceItems = throttle(async () => {
  const { confer, setConfMember, uniqueCode } = useConferenceStore();
  const { chair, conferenceMember, status } = confer;
  if (status === 'end') {
    setConfMember([]);
    return;
  }

  if (!uniqueCode) {
    return;
  }
  const { code, data } = await queryConferenceItems({ confId: uniqueCode });
  if (code !== 0) {
    return;
  }

  const confMemberMap = new Map();
  data.forEach((item) => {
    confMemberMap.set(item.account, item);
  });

  const ret: ConfMember[] = [];
  conferenceMember.forEach((item) => {
    const { name, number } = item;
    const isChairman = chair === number ? 1 : 0;
    // const memberTemp = {
    //   disconnectReason: 'None',
    //   isDisplaySite: '1',
    //   isMute: '0',
    //   isVideo: '1',
    //   name: '',
    //   notMember: true, // 不是与会成员，重呼的时候得调邀请接口
    //   number,
    //   participantStatus: 'Disconnected',
    //   type: conferenceMember[0]?.type,
    // };
    const confItem = confMemberMap.get(number);
    const member = {
      ...item,
      bizStatus: confItem.bizStatus,
      category: confItem?.accountType || CategoryEnum.terminal, // 查不到的为外部邀请用终端显示
      id: confItem?.id || number,
      isChairman,
      name: confItem?.name || name,
      status: confItem.status,
    };

    if (isChairman) {
      ret.unshift(member);
    } else {
      ret.push(member);
    }
  });

  setConfMember(ret);
}, 500);
