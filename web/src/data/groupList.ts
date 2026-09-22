import { onBeforeUnmount } from 'vue';

import { queryGroups, queryPatchGroups } from '@/api/group';
import { appConfig } from '@/config';
import { useBaseData, useDC, useEmitter, useSetInterval } from '@/hooks';
import { getGroupData, sortGroupOnline } from '@/pages/resource/groupHelper';
import { groupFunc } from '@/plugins/mspPlayer';
import { useCommunicationStore, useResourceStore } from '@/store';

import { cloneDeep, debounce } from 'lodash-es';

export function initGroupList() {
  const communicationStore = useCommunicationStore();
  const resourceStore = useResourceStore();
  let clearTimer: any;
  let clearUpdateUserTimer: any;

  // 初始化群组数据
  const queryGroupData = debounce(() => {
    queryCommonGroupData();
    queryDynamicGroupData();
    queryPatchGroupData();
    communicationStore.queryMessageList();
  }, 300);

  function mounted() {
    queryGroupData();
    updateUserList();
  }

  onBeforeUnmount(() => {
    clearTimer?.();
    clearUpdateUserTimer?.();
  });

  useDC('GROUP_CHANGE_MESSAGE', 'update_group', queryGroupData);
  useEmitter('queryGroupData', queryGroupData);

  // 查询动态组
  async function queryDynamicGroupData() {
    const { isdn } = appConfig;
    const { code, data } = await queryGroups({
      groupType: '0',
      isdn,
    });
    if (code === 0 && data.length > 0) {
      const dynamicGroupData = getGroupData(data);
      subjoinTalkingGroup('dynamicGroup', dynamicGroupData);
      resourceStore.initDynamicGroup(dynamicGroupData);
    }
  }

  // 查询静态组
  async function queryCommonGroupData() {
    const { isdn } = appConfig;
    const { code, data } = await queryGroups({
      groupType: '1',
      isdn,
    });
    if (code === 0 && data.length > 0) {
      const commonGroupData = getGroupData(data);
      subjoinTalkingGroup('commonGroup', commonGroupData);
      resourceStore.initCommonGroup(commonGroupData);
    }
  }

  // 查询派接组
  async function queryPatchGroupData() {
    let patchGroupData: any = [];
    const { isdn } = appConfig;
    const param = {
      setUpdcId: isdn,
    };
    const { code, data } = await queryPatchGroups(param);
    if (code === 0 && data.length > 0) {
      patchGroupData = data;
    }
    resourceStore.initPatchGroup(cloneDeep(patchGroupData));
  }

  // 订阅并加入组呼
  const isSub = {};
  function subjoinTalkingGroup(type, data: any[]) {
    if (isSub[type]) {
      return;
    }
    isSub[type] = true;
    clearTimer = useSetInterval(
      () => {
        // 判断-d是否成功登录
        if (!communicationStore.hasComm || isSub[type] === 'isSub') {
          return;
        }
        isSub[type] = 'isSub';
        data.forEach((item) => {
          if (item.isSub === '1') {
            groupFunc.subscribeGroup(item.groupId);
            groupFunc.subjoinTalkingGroup(item.groupId);
          }
        });
      },
      500,
      true,
    );
  }

  function update(type, data) {
    const { resourceOrigin } = useBaseData();
    data.forEach((group) => {
      group.groupUserList.forEach((user) => {
        const { category, id } = user;
        if (category) {
          const target = resourceOrigin[category][id];
          if (target) {
            user.bizStatus = target.bizStatus;
          }
        } else {
          const arr = Object.keys(resourceOrigin);
          for (const category of arr) {
            const target = resourceOrigin[category][id];
            if (target) {
              user.category = category;
              user.bizStatus = target.bizStatus;
              break;
            }
          }
        }
      });
    });
    resourceStore.updateGroup(type, sortGroupOnline(data));
  }

  // 定时更新群组成员在线状态
  function updateUserList() {
    clearUpdateUserTimer?.();
    clearUpdateUserTimer = useSetInterval(() => {
      const { commonGroup, dynamicGroup } = resourceStore;
      update('commonGroup', [...commonGroup]);
      update('dynamicGroup', [...dynamicGroup]);
    }, 1000);
  }

  return mounted;
}
