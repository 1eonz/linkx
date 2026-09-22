import { sdkCallbackPrint, triggerSDKMethods } from '../helper';

export const groupFunc = {
  // 添加动态群组
  addDynamicGroup(data: any) {
    const param = {
      ...data,
      callback: (data) => {
        sdkCallbackPrint('添加动态群组', data);
      },
    };
    return triggerSDKMethods('group', 'addDynamicGroup', param);
  },
  // 派接组创建
  addPatchGroup(name: string) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('添加派接组', data);
      },
      name,
    };
    return triggerSDKMethods('group', 'addPatchGroup', param);
  },
  // 添加派接组成员
  addPatchGroupMember({ grpid, memberlist }: any) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('添加派接组成员', data);
      },
      grpid,
      memberlist,
    };
    return triggerSDKMethods('group', 'addPatchGroupMember', param);
  },
  // 删除群组
  deleteDynamicGroupPersonData({ grpid }): Promise<any> {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('删除群组', data);
      },
      grpid,
    };
    return triggerSDKMethods('group', 'deleteDynamicGroup', param);
  },
  // 派接组删除
  deletePatchGroup({ grpid }) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('删除派接组', data);
      },
      grpid,
    };
    return triggerSDKMethods('group', 'deletePatchGroup', param);
  },
  // 删除派接组成员
  deletePatchGroupMember({ grpid, memberlist }: any) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('删除派接组成员', data);
      },
      grpid,
      memberlist,
    };
    return triggerSDKMethods('group', 'deletePatchGroupMember', param);
  },
  // 查询动态组成员
  getDynamicGroupInFoData(grpid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('查询动态组成员', data);
      },
      grpid,
    };
    return triggerSDKMethods('query', 'queryDynamicGroupMembers', param);
  },
  // 获取所有群组数据
  getGroupsData(data) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('获取所有群组数据', data);
      },
      limit: data.pageSize,
      offset: data.start,
    };
    return triggerSDKMethods('query', 'queryTalkingGroup', param);
  },
  // 加入组呼
  joinTalkingGroup(grpid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('加入组呼', data);
      },
      grpid,
    };
    return triggerSDKMethods('group', 'joinTalkingGroup', param);
  },
  // 组呼发起或抢权
  pttGroup(grpid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('组呼发起或抢权', data);
      },
      grpid,
    };
    return triggerSDKMethods('group', 'pttTalkingGroup', param);
  },
  // 组呼放权
  pttreleaseGroup(grpid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('组呼放权', data);
      },
      grpid,
    };
    return triggerSDKMethods('group', 'pttreleaseTalkingGroup', param);
  },
  // 获取动态群组
  queryDynamicGroup(data) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('获取动态群组', data);
      },
      limit: data.pageSize,
      offset: data.start,
    };
    return triggerSDKMethods('query', 'queryDynamicGroup', param);
  },
  // 查询群组属性
  queryGroupInfo(grpid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('查询群组属性', data);
      },
      grpid,
    };
    return triggerSDKMethods('query', 'queryTalkingGroupInfo', param);
  },
  // 查询群组成员
  queryGroupMembers(grpid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('查询群组成员', data);
      },
      grpid,
    };
    return triggerSDKMethods('query', 'queryTalkingGroupMembers', param);
  },
  // 查询派接组
  queryPatchGroup() {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('查询派接组', data);
      },
    };
    return triggerSDKMethods('query', 'queryPatchGroup', param);
  },
  // 查询派接组成员
  queryPatchGroupMembers(grpid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('查询派接组成员', data);
      },
      grpid,
    };
    return triggerSDKMethods('query', 'queryDynamicGroupMembers', param);
  },
  // 查询静态组
  queryStaticGroup() {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('查询静态组', data);
      },
    };
    return triggerSDKMethods('query', 'queryStaticGroup', param);
  },
  // 获取静态群组
  queryStaticGroupV1(data) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('获取静态群组', data);
      },
      limit: data.pageSize,
      offset: data.start,
    };
    return triggerSDKMethods('query', 'queryStaticGroupV1', param);
  },
  // 查询群组
  queryTalkingGroupV1() {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('查询群组', data);
      },
      limit: '1000',
      offset: '0',
    };
    return triggerSDKMethods('query', 'queryTalkingGroup', param);
  },
  // 修改动态群组
  setDynamicGroupPersonData({ addlist, dellist, grpid }: any) {
    const param = {
      addlist,
      callback: (data) => {
        sdkCallbackPrint('修改动态群组', data);
      },
      dellist,
      grpid,
    };
    return triggerSDKMethods('group', 'modifyDynamicGroup', param);
  },
  // 订阅并加入组呼
  subjoinTalkingGroup(grpid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('订阅并加入组呼', data);
      },
      grpid,
    };
    return triggerSDKMethods('group', 'subjoinTalkingGroup', param);
  },
  // 群组订阅
  subscribeGroup(grpid) {
    const param = {
      callback: (data) => {
        // 订阅群组没有事件，是直接回调告知订阅是否成功
        sdkCallbackPrint('群组订阅', data);
      },
      grpid,
    };
    return triggerSDKMethods('group', 'subscribeTalkingGroup', param);
  },
  // 群组不订阅
  unSubscribeGroup(grpid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('群组不订阅', data);
      },
      grpid,
    };
    return triggerSDKMethods('group', 'unsubscribeTalkingGroup', param);
  },
};
