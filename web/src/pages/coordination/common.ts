import { getGlobalsList } from '@/api/dictionary';
import { tasksSave } from '@/api/pim';
import { getTaskByMsgId, responsesTask, responsesTaskAll } from '@/api/problemSolve';
import { Dialog } from '@/components/Dialog';
import MessageBox from '@/components/MessageBox';
import { useEmitter } from '@/hooks';
import { usePIMStore } from '@/store';

import dayjs from 'dayjs';
import { cloneDeep } from 'lodash-es';

import ChatHistory from './chatPanel/chatHistory.vue';
import ForwardMessage from './chatPanel/forwardMessage.vue';
import MemberDetails from './chatPanel/memberDetails.vue';

// 为window添加__xietongCache属性类型定义
declare global {
  interface Window {
    __xietongCache?: Record<string, boolean>;
  }
}

// 消息记录
export const imChatHistory = (onToChat?: (item: any) => void) => {
  const cid = 'PIM_HISTORY';

  Dialog(cid)?.close();

  Dialog({
    cid,
    content: ChatHistory,
    data: {
      cid,
      onToChat,
    },
    offset: ['45%', '20%'],
  });
};

// 展示时间
export const timeView = (time) => {
  function isBeforeToday(targetDate) {
    const todayStart = dayjs().startOf('day'); // 当天零点
    return dayjs(targetDate).isBefore(todayStart);
  }

  return isBeforeToday(time) ? dayjs(time).format('YYYY-MM-DD HH:mm') : dayjs(time).format('HH:mm');
};

// 人员详情
export const openMemberDetails = (data) => {
  const cid = 'MemberDetails';
  Dialog(cid)?.close();
  Dialog({
    cid,
    content: MemberDetails,
    data: {
      userData: data,
    },
    // offset: ['40%', '15%'], // 人员详情弹窗位置
  });
};

// 获取@所有人消息
export const getAllAtMatches = async (atMatches) => {
  const { user } = usePIMStore();
  // 判断消息中是否@all
  if (atMatches.some((item) => item.match('@all'))) {
    const userIdXietong = user?.cooperationUser?.userId;
    const result: string[] = [`[@${userIdXietong}:@]`];
    return result;
  }
  // 去重
  const uniqueArray = [...new Set(atMatches)];
  console.log(uniqueArray, '==uniqueArray');

  return uniqueArray;
};

// 新增协同任务
export const addTasks = async (message) => {
  const { content, from, groupId, groupName, msgId, seq } = message;
  // fromRealUserId;
  const { bondedStatus, user, userIcs } = usePIMStore();

  // 只有文本消息并且非转发的文本消息才会创建任务
  if (!content) {
    return;
  }
  // if (Number(fromRealUserId) === user.userid) return;
  const { dept } = from;
  const groupIds = user?.cooperationUser?.groupIds || [];
  if (groupIds.length === 0) {
    console.log('没有支撑群组');
    return;
  }
  if (!groupIds.includes(Number(groupId))) {
    console.log('协同岗支撑群组不包含当前聊天群组');
    return;
  }
  const atRegex = /(\[@.*?\])/g;
  const atMatches = content.text.match(atRegex) || [];

  const newAtMatches = await getAllAtMatches(atMatches);

  for (const at of newAtMatches) {
    const regex = /\[@(.*?):@/;
    const atId = (at as string)?.match(regex)?.[1];
    console.log(atId, '===atId');

    if (Number(atId) !== user?.cooperationUser.userId) {
      console.warn('非@对象', atId, user?.cooperationUser.userId);
      continue;
    }

    // 非协同用户
    if (!bondedStatus) {
      console.warn('非协同用户', bondedStatus);
      continue;
    }

    try {
      const { code } = await tasksSave({
        fromUserDepartmentId: dept.departmentId,
        fromUserDepartmentName: dept.departmentName,
        fromUserId: from.id,
        fromUserName: from.name,
        fromUserNick: from.name,
        groupId,
        groupName,
        icsMsgId: msgId,
        isDeleted: 0,
        msgFileId: '',
        postId: user?.cooperationUser?.userId, // 协同岗id
        seqid: seq,
        status: 1,
        text: content.text,
        toExecutorId: userIcs.id, // h5服务登录的id
        userId: user.userid, // im登录用户id
      });
      if (code === 0) {
        useEmitter().emit('refreshTabsData');
        useEmitter().emit('refreshTableData');
        console.log('任务创建成功');
      }
    } catch (error) {
      console.error('创建任务失败:', error);
    }
  }
};

// 批量回复
export const batchReply = async (message, data) => {
  if (!message) return;
  const { category, from, fromRealUserId, msg } = message;
  const { user, userIcs } = usePIMStore();
  const { cooperationUser, department, userid, username } = user;
  if (
    category !== 2 ||
    !msg ||
    (Number(from) !== Number(userid) && Number(fromRealUserId) !== Number(userid))
  ) {
    return;
  }
  const PIMStore = usePIMStore();

  // 判断发出消息之人是否为协同岗
  if (!PIMStore.collaboration) return;

  // 判断是否有引用
  const srcMsgId = msg?.srcMsgId;
  if (!srcMsgId) return;
  // 判断是否@了发起任务人员
  const atRegex = /(\[@.*?\])/g;
  const atMatches = msg.text.match(atRegex) || [];
  let atPerson = false;
  for (const at of atMatches) {
    const regex = /\[@(.*?):@/;
    const id2 = at.match(regex)[1];
    if (Number(id2) === Number(data.fromUserId)) {
      atPerson = true;
    }
  }

  if (!atPerson) return;
  // 获取from用户详情

  const msgArr = [
    {
      content: message.msg.text,
      departmentId: department.departmentId,
      departmentName: department.departmentName,
      fromExecutorId: userIcs.id,
      msgFileId: message.msgId,
      postId: cooperationUser?.userId,
      seqid: message.seq,
      taskId: data.taskId,
      userId: userid,
      userName: `${cooperationUser?.name || ''}(${username})`,
      userNick: `${cooperationUser?.name || ''}(${username})`,
    },
  ];
  const { code } = await responsesTask(msgArr);
  if (code === 0) {
    // 触发事件通知details组件刷新数据
    useEmitter().emit('refreshDetailsQuery', data.taskId);
    // 触发刷新tabs数据事件
    useEmitter().emit('refreshTabsData');
  }
};

const noQuotationMssage = async (message) => {
  if (!message) return;
  if (message?.msgType !== 1) return;
  const { category, from, fromRealUserId, msg } = message;
  const { user, userIcs } = usePIMStore();
  const { cooperationUser, department, userid, username } = user;
  if (
    category !== 2 ||
    !msg ||
    (Number(from) !== Number(userid) && Number(fromRealUserId) !== Number(userid))
  ) {
    return;
  }
  const PIMStore = usePIMStore();

  // 判断发出消息之人是否为协同岗
  if (!PIMStore.collaboration) return;
  // 获取from用户详情
  const msgArr = {
    content: message.msg.text,
    departmentId: department.departmentId,
    departmentName: department.departmentName,
    fromExecutorId: userIcs.id,
    groupId: message.to,
    msgFileId: message.msgId,
    postId: cooperationUser?.userId,
    seqid: message.seq,
    userId: userid,
    userName: `${cooperationUser?.name || ''}(${username})`,
    userNick: `${cooperationUser?.name || ''}(${username})`,
  };
  const { code } = await responsesTaskAll(msgArr);
  if (code === 0) {
    // 触发事件通知details组件刷新数据
    useEmitter().emit('refreshDetailsQuery');
    // 触发刷新tabs数据事件
    useEmitter().emit('refreshTabsData');
  }
};
async function getReplyDirectly() {
  const { code, data } = await getGlobalsList();
  if (code === 0) {
    return !!(data?.REPLY_DIRECTLY === 'true' || data?.REPLY_DIRECTLY === true);
  }
  return false;
}

export const processTasks = async (message) => {
  const replyDirectly = await getReplyDirectly();
  if (replyDirectly) {
    noQuotationMssage(message);
  } else {
    const srcMsgId = message.msg?.srcMsgId;
    if (srcMsgId) {
      const { user } = usePIMStore();
      // 判断引用消息是否含有对应的协同任务，存在srcMsgId且能查询到警务协同任务的为回复
      const { code, data } = await getTaskByMsgId({
        icsMsgId: srcMsgId,
        postId: user?.cooperationUser?.userId,
      });
      if (code === 0 && data) {
        batchReply(message, data);
      }
    }
  }
};

// 合并转发类型名称
export const getCategoryNames = (messageList) => {
  if (!messageList || messageList.length === 0) {
    return '';
  }
  const { category } = messageList[0];
  if (category === 1) {
    const result: any = messageList.map((item) => item.name);
    const name = [...new Set(result)].join('和');
    return name ? `${name}的聊天记录` : '聊天记录';
  }
  const obj = {
    // 1: '点对点',
    2: '群聊',
    3: '公众号',
    4: '多人转发',
  };
  return obj[category] ? `${obj[category]}的聊天记录` : `${obj[2]}的聊天记录`;
};

// 会话列表排序
export const chatListSort = (data) => {
  const chatProfiles = {}; // 获取置顶和免打扰信息
  usePIMStore().user?.chatProfiles?.forEach((item) => {
    chatProfiles[item.targetId] = item;
  });

  const list = cloneDeep(data);
  const ret = list.sort((a, b) => {
    const targetA = chatProfiles[a.sessionId];
    const targetB = chatProfiles[b.sessionId];

    // 从小到大排序，置顶+最后一条消息时间越近越小

    const effectMessagesA = a.messages.filter((item) => {
      if (item.msg?.cmcontainer) {
        return item.msg?.cmcontainer?.operationType !== 11;
      }
      return true;
    });
    const effectMessagesB = b.messages.filter((item) => {
      if (item.msg?.cmcontainer) {
        return item.msg?.cmcontainer?.operationType !== 11;
      }
      return true;
    });
    const timeA = effectMessagesA?.[effectMessagesA.length - 1]?.time || a.createTime || 1;
    const valA = -(targetA?.isStickyOnTop ? Date.now() + timeA : timeA);
    const timeB = effectMessagesB?.[effectMessagesB.length - 1]?.time || b.createTime || 1;
    const valB = -(targetB?.isStickyOnTop ? Date.now() + timeB : timeB);
    return valA - valB;
  });

  return ret;
};

export const getGroupOperationType = (latestMsg, f) => {
  const { memberList, noticeDto, operateId, operationType } = latestMsg;
  const { user } = usePIMStore();
  if (noticeDto) {
    return '[群公告]';
  }
  const operateName = operateId === user.id ? '你' : f.name;
  const memberName = memberList?.map((i) => i.name).join(',');
  const obj = {
    1: `${operateName}邀请${memberName}加入群聊`,
    2: `${operateName}退出群聊`,
    3: `${operateName}将${memberName}移除群聊`,
    4: `${operateName}修改群名称`,
    //  5: '[群成员资料变更]', //  包括禁言状态、角色、新增成员等
    6: `${operateName}创建群聊`,
    7: `${operateName}解散群聊`,
    8: '[群公告]',
    9: `${operateName}申请加入群聊`, // 包括邀请加群，通过扫二维码方式主动加群等 --群管理员/群主接收
  };
  return obj[operationType];
};

// 删除聊天记录
export async function handleDeleteMessage(data) {
  const res = await MessageBox({
    isLight: true,
    offset: ['45%', '20%'],
    text: '确定删除吗？',
    type: 'ok',
    zIndexDefault: 2000,
  });
  if (res) {
    usePIMStore().deleteChatMsg(data);
  }
}

// 合并转发
export function openForwardMsgs(msg, groupId, groupMembers, callBack?, isNested = false) {
  const arr = [...msg.forwardMsgs];
  const title = msg.title || getCategoryNames(arr);
  const msgList: any = [];
  arr.forEach((item) => {
    if (item.msgType === 8) {
      let forwardMsgs = {};
      if (item.userTxt) {
        try {
          // 尝试解析 JSON 字符串
          forwardMsgs = JSON.parse(item.userTxt);
        } catch {
          // 如果解析失败，说明是普通字符串，直接使用
        }
      }
      const obj = {
        ...forwardMsgs,
        ...item,
        forwardMsg: true,
      };
      if (!obj.msg) {
        obj.msg = forwardMsgs;
      }
      msgList.push(obj);
      return;
    }
    if (!item.msg && item.msgType === 2) {
      item.msg = {
        duration: item.duration,
        fileKey: item.fileKey,
        fileName: item.fileName,
        fileSize: item.fileSize,
        fileType: item.fileType,
        videoThumb: item.videoThumb,
      };
    }
    if(item.text.includes('@all')){
      item.text = item.text.replace('@all','@所有人')
    }
    msgList.push(item);
  });
  Dialog({
    cid: isNested ? 'msgListForwardNested' : 'msgListForward',
    content: ForwardMessage,
    data: {
      closeDialog: () => {
        callBack?.();
      },
      groupId,
      groupMembers,
      msgList,
      title,
    },
    offset: ['50%', '50%'],
    zIndexDefault: 9999,
  });
}

// 过滤不需要展示的消息
export function filterMessage(data) {
  if (data.notifyType === 'GROUP_EVENT') {
    const t = data.msg.cmcontainer;

    if (t.operationType === 4 && t.operateId === -1) {
      return false;
    }

    return [1, 2, 3, 4, 5, 6, 7, 11].includes(t.operationType);
  }

  if (data.msg?.srcMsgIds) {
    return false;
  }

  return data.notifyType !== 'ACK_READ';
}

// 返回用户是否在群聊里面
export function checkUserInGroup(groupId) {
  const { chartGroupList, user } = usePIMStore();
  const cooperationGroupIds = user.cooperationUser?.groupIds || [];
  const groupIds = [...cooperationGroupIds, ...chartGroupList.map((i) => i.id)].map(Number);
  const include = groupIds.includes(Number(groupId));
  return include;
}
