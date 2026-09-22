import { createPatchGroup, createPatchGroupUser, deletePatchGroups } from '@/api/group';
import { Message } from '@/components/Message';
import MessageBox from '@/components/MessageBox';
import { useEmitter, useI18n } from '@/hooks';
import { createDynamicGroupNotify } from '@/pages/resource/groupHelper';
import { mediaFunc } from '@/plugins/mspPlayer';
import { sdkCallbackPrint, triggerSDKMethods } from '@/plugins/mspPlayer/helper';
import { useCommunicationStore } from '@/store';

import { communicationStatus } from '../commStatus';
import messageUtil from '../messageUtil';
import storeCidIsdnRelation from '../storeCidIsdnRelation';

function groupRegister() {
  const { t } = useI18n();
  const communicationStore = useCommunicationStore();

  init();

  function init() {
    // 组呼开始事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('组呼开始事件', data);
        groupCall(data);
      },
      eventName: 'OnTalkingGroupCallPTTStart',
      eventType: 'GroupCallNotify',
    });

    // 组呼加入通知事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('组呼加入通知事件', data);
      },
      eventName: 'OnTalkingGroupCallJoinNotify',
      eventType: 'GroupCallNotify',
    });

    // 组呼资源通知
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('组呼资源通知', data);
        handleGroup(data);
      },
      eventName: 'OnGroupCallStatusNotify',
      eventType: 'GroupCallNotify',
    });

    // 组呼抢权或放权成功事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('组呼抢权或放权成功事件', data);
      },
      eventName: 'OnTalkingGroupCallPTTSuccess',
      eventType: 'GroupCallNotify',
    });

    // 组呼空闲事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('组呼空闲事件', data);
        handleGroup(data);
      },
      eventName: 'OnTalkingGroupCallPTTIdle',
      eventType: 'GroupCallNotify',
    });

    // 组呼释放事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('组呼释放事件', data);
        handleGroup(data);
      },
      eventName: 'OnTalkingGroupCallRelease',
      eventType: 'GroupCallNotify',
    });

    // 接收组呼事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('接收组呼事件', data);
        handleGroup(data);
      },
      eventName: 'OnTalkingGroupCallStart',
      eventType: 'GroupCallNotify',
    });

    // 组呼发起失败事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('组呼发起失败事件', data);
        handleGroup(data);
      },
      eventName: 'OnTalkingGroupCallFailure',
      eventType: 'GroupCallNotify',
    });

    // 组呼抢权或放权失败事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('组呼抢权或放权失败事件', data);
        Message({
          message: t('communication.communicationStatus.groupRejected'),
          type: 'error',
        });
        useEmitter().emit('OnTalkingGroupCallPTTFailure');
      },
      eventName: 'OnTalkingGroupCallPTTFailure',
      eventType: 'GroupCallNotify',
    });

    // 组呼抢权事件 (其他人说话)
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('组呼抢权事件', data);
        groupCall(data);
      },
      eventName: 'OnTalkingGroupCallPTTNotify',
      eventType: 'GroupCallNotify',
    });

    // 群组刷新调度事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('群组刷新调度事件', data);
      },
      eventName: 'OnTalkingGroupStatusChange',
      eventType: 'GroupNotify',
    });

    // 通知当前用户，群组中的成员的新增和删除。所有调度员会收到该事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('动态组用户配置更新', data);
      },
      eventName: 'OnDynamicGroupMemberChange',
      eventType: 'GroupNotify',
    });

    // 上报动态组和静态组中用户的增加和删除的通知事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('上报动态组和静态组中用户的增加和删除的通知事件', data);
        useEmitter().emit('queryGroupData');
      },
      eventName: 'OnTalkingGroupMemberChange',
      eventType: 'GroupNotify',
    });

    // 动态组创建、修改成功通知
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('动态组创建、修改成功通知', data);
        createDynamicGroupNotify(data);
      },
      eventName: 'OnDynamicGroupChangeSuccess',
      eventType: 'GroupNotify',
    });

    // 动态组操作失败通知
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('动态组操作失败通知', data);
      },
      eventName: 'OnDynamicGroupOptFailed',
      eventType: 'GroupNotify',
    });

    // 派接组更新事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('派接组更新事件', data);
        const { groupinfo, opt } = data;
        if (opt === 'create') {
          // 派接组ID dcpatchindex
          const { grpnumber } = groupinfo;
          communicationStore.setPatchGroupGrpId(grpnumber);
          addPatchGroup(groupinfo);
        }
        if (opt === 'delete') {
          const { grpnumber } = groupinfo;
          deletePatchGroup(grpnumber);
        }
        useEmitter().emit('queryGroupData');
      },
      eventName: 'OnPatchGroupStatusChange',
      eventType: 'GroupNotify',
    });

    // 派接组成员配置更新事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('派接组成员配置更新事件', data);
        const { groupinfo, opt } = data;
        if (opt === 'create') {
          updatePatchGroup(groupinfo);
        }
      },
      eventName: 'OnPatchGroupMemberChange',
      eventType: 'GroupNotify',
    });

    // 派接组成员配置更新失败事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('派接组成员配置更新失败事件', data);
      },
      eventName: 'OnChangePatchGroupMemberFailure',
      eventType: 'GroupNotify',
    });

    // 派接组操作失败通知事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('派接组操作失败通知事件', data);
      },
      eventName: 'OnPatchGroupOptFailed',
      eventType: 'GroupNotify',
    });
  }

  function groupCall(data) {
    const { cid, grpid } = data.value;
    const _cid = cid || storeCidIsdnRelation.getCidByIsdn(grpid, 'group');

    console.log(_cid);

    // 群组组呼音量
    const media = communicationStore.mediaData[grpid];
    if (media) {
      mediaFunc.setVolume(_cid, media.volume || 1); // 最小为1
    }

    if (cid) {
      storeCidIsdnRelation.add(cid, grpid, 'group');
    }

    handleGroup(data);
  }

  async function handleGroup(data) {
    const groupStat = messageUtil.groupMessage(data); // 主叫
    if (groupStat) {
      // 将群组中的speaker的isdn转换为rescId
      if (['1005', '1007'].includes(data.rsp)) {
        Message(groupStat.status.msg);
      }
      handleMessage('group', data, groupStat);
    }
  }

  async function deletePatchGroup(data) {
    const param = {
      id: data,
    };
    const { code } = await deletePatchGroups(param);
    if (code === 0) {
      console.log('deletePatchGroup success');
    }
  }

  async function addPatchGroup(groupinfo) {
    const param = {
      dcpatchindex: groupinfo.dcpatchindex,
      grpnumber: groupinfo.grpnumber,
      pgname: groupinfo.pgname,
      pgpriority: groupinfo.pgpriority,
      setUpdcId: groupinfo.setupdcid,
      userBOList: [],
    };
    const { code } = await createPatchGroup(param);
    if (code === 0) {
      console.log('addPatchGroup success');
    }
  }

  async function updatePatchGroup(groupinfo) {
    const param = {
      groupnumber: groupinfo.groupnumber,
      membergroup: groupinfo.membergroup,
    };
    const { code } = await createPatchGroupUser(param);
    if (code === 0) {
      console.log('updatePatchGroup success');
    }
  }

  function handleMessage(type, message, commStatus) {
    const { from, grpid } = message.value;
    const { NOT_SUPPORT, SEND_COMMAND_FAILED, SEND_COMMAND_TIMEOUT } = communicationStatus();

    let updateIsdn = '';
    if (type === 'group') {
      updateIsdn = grpid;
      commStatus.from_uid = from;
    }

    // 如果没有权限、没有license和不支持等，用弹框提示
    if (
      commStatus.status === NOT_SUPPORT.status ||
      commStatus.status === SEND_COMMAND_TIMEOUT.status ||
      commStatus.status === SEND_COMMAND_FAILED.status
    ) {
      MessageBox({
        text: commStatus.status.msg,
      });
    }

    // 更新状态
    if (commStatus.endFlag === 0) {
      communicationStore.deleteComm({
        isdn: updateIsdn,
        status: commStatus,
        type,
      });
    } else {
      communicationStore.updateComm({
        isdn: updateIsdn,
        message,
        status: commStatus,
        type,
      });
    }
  }
}

export default groupRegister;
