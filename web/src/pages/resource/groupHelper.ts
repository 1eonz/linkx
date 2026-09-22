import { openGroupDetailsPopup } from '@/comm/group';
import { Dialog } from '@/components/Dialog';
import { appConfig } from '@/config';
import { useI18n } from '@/hooks';
import GroupAdd from '@/pages/resource/group/groupAdd.vue';
import patchGroupAdd from '@/pages/resource/group/patchGroupAdd.vue';
import { getOnlineStatus } from '@/pages/resource/resourceHelper';
import { useResourceStore } from '@/store';

/**
 * 创建群组
 */
export function createGroup(defaultUserList?) {
  const { t } = useI18n();
  const { id, name, onlineStatus } = appConfig.userData;
  const groupManager = {
    bizStatus: onlineStatus || '1',
    id,
    isCreator: 1,
    isdn: appConfig.isdn,
    name,
  };
  Dialog({
    cid: 'create_dynamic_group',
    content: GroupAdd,
    data: {
      defaultUserList,
      groupMembers: [groupManager],
      operateType: 'add',
      title: t('resource.group.newDynamicGroup'),
    },
    offset: ['600px', '100px'],
  });
}

/**
 * 创建派接组
 */
export function createPatchGroup(defaultUserList?) {
  const { t } = useI18n();
  const { id, name } = appConfig.userData;
  const groupManager = {
    id,
    isCreator: 1,
    isdn: appConfig.isdn,
    name,
  };
  Dialog({
    cid: 'create_patch_group',
    content: patchGroupAdd,
    data: {
      defaultUserList,
      groupMembers: [groupManager],
      operateType: 'add',
      title: t('resource.group.newGroup'),
    },
    offset: ['600px', '100px'],
  });
}

/**
 * 设置在线比
 * @param data
 */
export function sortGroupOnline(data: any[]): any[] {
  return data.map((item) => {
    const list = item.groupUserList || [];
    const onList: any = [];
    const offList: any = [];
    list.forEach((item) => {
      const status = getOnlineStatus(item);
      if (status === 'online' || status === 'busy') {
        onList.push(item);
      } else {
        offList.push(item);
      }
    });
    return {
      ...item,
      allNumber: list.length,
      groupUserList: [...onList, ...offList],
      onlineNumber: onList.length,
    };
  });
}

/**
 * 格式化群组
 * @param data
 */
export function getGroupData(data: any[]) {
  const arr = sortGroupOnline(data);
  const ret = arr.map((item) => {
    return {
      allNumber: item.allNumber,
      executor_id: item.setupdcId,
      groupId: item.groupId,
      groupPriority: item.groupPriority,
      groupType: item.groupType,
      groupUserList: item.groupUserList,
      isChecked: false,
      isParent: 'false',
      isSub: item.speakerFixed,
      name: item.groupName,
      nodeId: item.groupId,
      onlineNumber: item.onlineNumber,
      parentId: item.departmentId,
      parentName: item.departmentName,
      unReadMsgType: true,
    };
  });
  return ret;
}

/**
 * 动态群组创建成功后弹出群组详情
 * @param data
 * @returns
 */
export async function createDynamicGroupNotify(data) {
  if (data.rsp !== '0') {
    return;
  }
  const { dynamicGroup } = useResourceStore();
  const { grpid, isdn } = data.value;
  const index = dynamicGroup.findIndex((i) => i.groupId === grpid);
  if (index === -1 && isdn === appConfig.isdn) {
    openGroupDetailsPopup({ id: grpid });
  }
}
