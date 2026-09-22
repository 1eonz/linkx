import {
  addressbookBatch,
  addressbookUsersChatprofile,
  addressbookUsersId,
  batchGroupsMembers,
  downloadFile,
  getSessionList,
  groupDetails,
  groupQuery,
  messagesOfflinePage,
  modifyAddressbookUsersProfile,
  updateCollaboration,
  userLogin,
} from '@/api/pim';
import { Message } from '@/components/Message';
import { appConfig } from '@/config';
import {
  deleteChatFromDB,
  deleteMessages,
  getAllIndexedDBData,
  modifyMessages,
  saveMessages,
} from '@/plugins/pim/indexDB';
import { store } from '@/store';
import { isArray } from '@/utils/is';

import { debounce } from 'lodash-es';
import { defineStore } from 'pinia';

const getUserInfoQueue = new Set();
const getUserAvatarQueue = new Set();

interface PIMState {
  bondedStatus: boolean; // 是否为协同岗
  chartGroupList: any[];
  chatList: any[];
  chatListActive: number;
  collaboration: boolean; // 协同岗开关状态
  drafts: Map<number | string, string>; // 存储草稿的 Map，支持 string 和 number 类型的 key
  forwardMsgList: any[];
  groupMemberActive: any[];
  hiddenChatList: boolean; // 是否隐藏即时聊天窗口
  quoteMsg: any; // 引用信息内容
  systemLimit: boolean; // 登录接口返回是否存在系统功能受限情况
  user: { [key: string]: any; cooperationUser?: any; cooperationUsers?: any } | any;
  userAvatar: Map<string, any>;
  userIcs: any;
  userMap: Map<number, any>;
  userRoleAuth: any; // 获取用户菜单栏权限信息（协同群组，协同处置，协同统计）
}

export const usePIMStore = defineStore({
  actions: {
    // 清除所有草稿
    clearAllDraft() {
      this.drafts.clear();
      // 更新 localStorage
      localStorage.setItem('chat_drafts', JSON.stringify([...this.drafts.entries()]));
    },
    // 清除指定会话的聊天记录
    clearChatList(chat) {
      const index = this.chatList.findIndex((item) => item.id === chat.id);
      if (index !== -1) {
        this.chatList[index].messages = [];
      }
    },
    // 清除草稿
    clearDraft(sessionId: number | string) {
      this.drafts.delete(sessionId);
      // 更新 localStorage
      localStorage.setItem('chat_drafts', JSON.stringify([...this.drafts.entries()]));
    },

    // 创建群聊成功后的推送
    async createGroupSuccess(groupId) {
      const index = this.chartGroupList.findIndex((i) => {
        return i.id === groupId;
      });

      if (index !== -1) {
        return;
      }
      const { code, data } = await groupDetails(groupId, { isIncludeMember: 0 });
      if (code === 0) {
        this.chartGroupList.push(data);
      }
    },

    // 删除聊天（本地删除）
    deleteChat(data) {
      deleteChatFromDB(data);

      const index = this.chatList.findIndex((item) => item.id === data.id);
      if (index !== -1) {
        this.chatList.splice(index, 1);
      }

      if (data.id === this.chatListActive) {
        this.setChatListActive(this.chatList[0].sessionId);
      }
    },

    // 删除聊天消息（本地删除）
    deleteChatMsg(data) {
      const list = isArray(data) ? data : [data];
      list.forEach((item) => {
        const index = this.chatList.findIndex((i) => i.sessionId === this.chatListActive);
        const { messages } = this.chatList[index];
        this.chatList[index].messages = messages.filter((i) => i.msgId !== item.msgId);
        deleteMessages(item);
      });
    },

    // 修改会话列表群组名称
    editChatList(chat, newName) {
      const index = this.chatList.findIndex((item) => item.id === chat.id);
      if (index !== -1) {
        this.chatList[index].name = newName;
      }
    },

    // 全量查询所在群组
    async getChartGroupList() {
      const { code, data } = await groupQuery({});
      if (code === 0) {
        this.chartGroupList = data.filter((item) => {
          return item && item?.isDel === false;
        });
      }
    },

    // 获取会话列表
    async getChatList() {
      const allData = await getAllIndexedDBData();
      // 确保 sessionId 为数字类型
      const normalizedAllData = allData.map((item) => ({
        ...item,
        sessionId: Number(item.sessionId),
      }));
      this.chatList = normalizedAllData;
      for (let i = 0; i < normalizedAllData.length; i++) {
        await this.setChatItemDetails(i, true);
      }

      const { data } = await getSessionList({ userId: this.user.id });
      data?.data?.forEach((i) => {
        const sessionIdToFind = Number(i.sessionId);
        if (!sessionIdToFind || ![1, 2].includes(i.sessionType)) {
          return;
        }
        const index = this.chatList.findIndex((j) => Number(j.sessionId) === sessionIdToFind);

        if (index === -1) {
          const item = { ...i, messages: [], sessionId: sessionIdToFind };
          this.chatList.push(item);
          this.setChatItemDetails(item, true);
        } else {
          // 直接使用已找到的索引，避免重复查找
          const existingChat = this.chatList[index];
          const item = {
            ...i,
            messages: existingChat?.messages || [],
            sessionId: sessionIdToFind,
          };

          // 更新现有会话项
          this.chatList[index] = item;
          this.setChatItemDetails(item, true);
        }
      });
    },

    // 打开协同岗开光，重新获取协同群组的历史消息
    async getCooperationGroupHistoryMsg(obj) {
      const chat = {
        messages: [],
        sessionId: obj.groupId,
        sessionType: 2,
        ...obj,
      };
      const index = this.chatList.findIndex((j) => j.sessionId === chat.sessionId);
      if (index !== -1) {
        const message = this.chatList.find((j) => j.sessionId === chat.sessionId);
        if (message?.messages?.length > 0) return;
        const item = { ...chat, messages: message?.messages || [] };
        this.setChatItemDetails(item, true);
      }
    },

    // 获取草稿
    getDraft(sessionId: number | string): string | undefined {
      return this.drafts.get(sessionId);
    },

    // 获取群组成员列表
    async getGroupMembers(groupId) {
      const params = [
        {
          id: Number(groupId),
          tag: 0,
        },
      ];
      const { code, data } = await batchGroupsMembers(params);
      if (code === 0) {
        this.setMemberActive(data[0]?.items);
      }
    },

    // 批量获取指定人员列表的人员详情
    async getGroupMembersName(members) {
      const ids: any = members?.map((item) => item.userId);
      const param = { ids: ids?.join(',') };
      const { code, data } = await addressbookBatch(param);
      if (code === 0) {
        return data.successList;
      }
      return [];
    },

    // 获取消息对象
    getMsgSessionId(message, sessionType?) {
      const { category, from, to } = message;
      let ret = this.user.id === Number(from) ? to : from;
      if (category === 2 || sessionType === 2) {
        ret = to;
      } else if (message.id) {
        ret = message.id;
      }
      return Number(ret);
    },

    // 获取离线消息
    async getOfflineMsg(chat, init) {
      const { sessionId, sessionType } = chat;
      const messages = chat.messages || [];
      let currentSeq = 1;

      const getMsg = (res) => {
        const offlineMessages: any = [];
        const imMsgs = res.data?.imMsgs || [];
        const seq = { index: 0, seqid: 1 };
        for (const [i, imMsg] of imMsgs.entries()) {
          const { sessionSeqId } = imMsg.data;
          if (sessionSeqId) {
            seq.index = i;
            seq.seqid = sessionSeqId;
            break;
          }
        }

        imMsgs.forEach((i, ind) => {
          if (!i.data.sessionSeqId) {
            i.data.sessionSeqId = seq.seqid - (seq.index - ind);
          }

          const index = messages.findIndex((j) => j.msgId === i.data.msgId);
          if (index === -1) {
            const d = { ...i.data, msg: i.data.msg || {}, notifyType: i.notifyType };
            messages.push(d);
            offlineMessages.push(d);
          } else {
            messages[index].sessionSeqId = i.data.sessionSeqId;
          }
        });

        if (imMsgs.length > 0) {
          currentSeq = imMsgs[0].data.sessionSeqId;
        }

        return offlineMessages;
      };
      const params: any = {
        category: sessionType,
        limit: init ? 100 : 20,
        plaintext: 1,
        sessionId,
        toSessionSeqId: init ? 9_999_999 : chat.currentSeq,
        type: 1,
        userId: this.user.id,
      };
      if (sessionType === 1 && sessionId === this.user.id) {
        params.toSeq = init ? 9_999_999 : chat.currentSeq;
        delete params.toSessionSeqId;
      }
      const res = await messagesOfflinePage(params);
      const offlineMessages = getMsg(res);
      saveMessages(offlineMessages, sessionType);

      const index = this.chatList.findIndex((i) => Number(i.sessionId) === Number(sessionId));
      if (index !== -1) {
        messages.sort((a, b) => a.time - b.time);
        this.chatList[index] = { ...this.chatList[index], currentSeq, messages };
      }
    },

    // 获取指定用户头像
    async getUserAvatar(id) {
      if (this.userAvatar.has(id)) {
        return this.userAvatar.get(id);
      }

      if (getUserAvatarQueue.has(id)) {
        return '';
      }

      getUserAvatarQueue.add(id);
      const res: any = await downloadFile(id, '50_50', {
        forceAsync: false,
        isInline: true,
      });
      const attachBlob = new Blob([res]);
      const imgUrl = URL.createObjectURL(attachBlob);
      this.userAvatar.set(id, imgUrl);
      getUserAvatarQueue.delete(id);

      return imgUrl;
    },

    // 获取指定用户详情
    async getUserInfo(_id) {
      const id = Number(_id);
      if (!id) {
        return null;
      }

      if (this.userMap.has(id)) {
        return this.userMap.get(id);
      }

      if (getUserInfoQueue.has(id)) {
        return;
      }

      getUserInfoQueue.add(id);

      try {
        const { code, data } = await addressbookUsersId(id);
        if (code === 0) {
          this.userMap.set(id * 1, data);
        }
        getUserInfoQueue.delete(id);
        return data;
      } catch {
        getUserInfoQueue.delete(id);
        return null;
      }
    },

    // 群组变更通知 + 更新群组信息
    groupChange(msg) {
      const { chatList } = usePIMStore();
      const { groupId, operationType } = msg.msg.cmcontainer;

      const getIndex = () => chatList.findIndex((item) => item.sessionId === groupId);

      // 新建
      if (operationType === 6) {
        groupDetails(groupId, { isIncludeMember: 0 }).then(async ({ code, data }) => {
          await this.createGroupSuccess(groupId);
          if (code === 0 && getIndex() === -1) {
            chatList.push({
              createTime: Date.now(),
              sessionId: groupId,
              sessionType: 2,
              ...data,
              messages: [msg],
            });
          }
          this.setChatListActive(groupId);
        });
        return;
      }

      if (operationType === 2 || operationType === 3 || operationType === 1) {
        this.updateGroupInfo();
      }

      // 重新获取群组详情
      if (getIndex() !== -1) {
        groupDetails(groupId, { isIncludeMember: 0 }).then(({ code, data }) => {
          if (code === 0) {
            // 更新群组信息
            const index = getIndex();
            chatList[index] = {
              ...chatList[index],
              ...data,
              messages: chatList[index].messages,
            };
          }
        });
      }
    },

    // 处理消息通知
    async handleMessageNotifications(type, data) {
      const { category } = data;

      // 如果是新建的会话需要先添加会话列表
      const sessionId = this.getMsgSessionId(data);
      const index = this.chatList.findIndex((i) => i.sessionId === sessionId);
      if (index === -1) {
        const chat = {
          createTime: Date.now(),
          sessionId,
          sessionType: category,
        };

        this.chatList.unshift(chat);
        await this.setChatItemDetails(chat, true);
      }

      switch (type) {
        // 已读回执
        case 'ACK_READ': {
          // 会话列表状态刷新
          const index = this.chatList.findIndex((i) => i.id === sessionId);
          this.chatList[index].messages?.forEach((i) => {
            i.read = true;
          });
          break;
        }
        case 'groupNotice': {
          const { operationType } = data.msg.cmcontainer;

          if ([1, 2, 3, 4, 5, 6, 7, 11].includes(operationType)) {
            this.updateChatListLatestMsg(data);
            this.groupChange(data);
          }
          break;
        }
        // 彩信消息/短信消息/卡片消息
        case 'MEDIA_MSG':
        case 'MERGE_FORWARD':
        case 'NAME_CARD':
        case 'TEXT_MSG': {
          this.updateChatListLatestMsg(data);
          break;
        }
        // 撤回消息
        case 'WITHDRAW_MSG': {
          this.updateWithdraw(data);
          break;
        }
      }
    },

    // 用户信息变更通知
    async handleUpdateUser(data, type) {
      switch (type) {
        case 'chatProfileNotice': {
          const index = this.user.chatProfiles.findIndex((i) => i.targetId === data.targetId);
          if (index === -1) {
            this.user.chatProfiles.push(data);
          } else {
            this.user.chatProfiles[index] = data;
          }
          break;
        }
        case 'update_user': {
          data.forEach((item) => {
            if (item.id === this.user.id) {
              this.updateUser();
            }
          });
          break;
        }
      }
    },

    // 判断chatlist里边有没有某一个群组会话，没有的话就重新获取群组信息
    judgeChatInList(sessionId) {
      const index = this.chatList.findIndex((i) => i.sessionId === sessionId);
      if (index === -1) {
        const item = { createTime: Date.now(), messages: [], sessionId, sessionType: 2 };
        this.chatList.push(item);
        this.setChatItemDetails(item, true);
      }
    },
    // 协同岗绑定或者删除人员，需要重新登录im，serviceStatus才会改变
    async loginIm() {
      const username = appConfig.userData?.idCardNum;
      if (!username) {
        Message({
          duration: 3000,
          message: '获取身份证号失败，无法完成IM登录！',
          type: 'error',
        });
        return;
      }

      const { code } = await userLogin({
        deviceType: '3',
        forceLogin: 1, // 1为强制登录，0非强制
        grantType: 'code',
        identityType: '3',
        loginType: '1',
        scope: 'all',
        state: '',
        username,
      });

      return code;
    },

    // 修改用户信息
    async modifyUser(data) {
      const { code, msg } = await modifyAddressbookUsersProfile(data);
      if (code === 0) {
        Message({
          message: msg,
          type: 'success',
        });
        this.updateUser();
      } else {
        Message({
          message: msg,
          type: 'error',
        });
      }
    },

    setBondedStatus(flag) {
      this.bondedStatus = flag;
    },

    // 会话免打扰
    async setChatDoNotDisturb(chat, disturb: boolean) {
      const param = {
        isMuteNotifications: disturb ? 1 : 0,
        targetId: chat.sessionId,
        type: chat.sessionType,
      };
      const { code, msg } = await addressbookUsersChatprofile(param);
      if (code === 0) {
        Message({
          message: msg,
          type: 'success',
        });
        this.updateUser();
      } else {
        Message({
          message: msg,
          type: 'error',
        });
      }
    },

    /**
     * 设置会话列表项的详情
     * 初始化时需要拉取离线消息，合并本地消息
     * @param chat
     * @param init 是否初始化
     */
    async setChatItemDetails(chat, init?: boolean) {
      const { sessionId, sessionType } = chat;

      switch (sessionType) {
        // 单聊
        case 1: {
          const data = await this.getUserInfo(sessionId);
          if (!data) return;
          const list = [...this.chatList];
          const index = list.findIndex((i) => Number(i.sessionId) === Number(data?.id));

          // 协同用户不能发消息，但是现场出现协同chat
          if (data?.type === 2) {
            this.chatList.splice(index, 1);
            this.deleteChat(chat);
            return;
          }

          if (index !== -1) {
            this.chatList[index] = {
              ...list[index],
              ...data,
              messages: chat.messages || [],
              name: data.name || data.undefinedName.split(',').slice(0, 4).join(','),
            };
          }
          break;
        }
        // 群聊
        case 2: {
          const { code, data } = await groupDetails(sessionId, { isIncludeMember: 0 });
          const list = [...this.chatList];
          const index = list.findIndex((i) => Number(i.sessionId) === Number(sessionId));
          if (code === 0) {
            if (index !== -1) {
              // 初始化时群组标识为删除的情况直接从左侧显示列表中剔除
              if (data?.isDel) {
                this.chatList.splice(index, 1);
                return;
              }

              this.chatList[index] = {
                ...list[index],
                ...data,
                messages: chat.messages || [],
                name: data.name || data.undefinedName.split(',').slice(0, 4).join(','),
              };
            }
          } else {
            this.chatList.splice(index, 1);
            deleteChatFromDB(chat);
          }
          break;
        }
      }

      if (init) {
        this.getOfflineMsg(chat, true);
      }
    },

    // 设置当前会话
    setChatListActive(active) {
      this.chatListActive = Number(active);
    },

    setcollaboration(flag) {
      this.collaboration = flag;
    },

    // 设置草稿
    setDraft(sessionId: number | string, content: string) {
      if (content) {
        this.drafts.set(sessionId, content);
      } else {
        this.drafts.delete(sessionId);
      }
      // 保存到 localStorage
      localStorage.setItem('chat_drafts', JSON.stringify([...this.drafts.entries()]));
    },

    // 保存本地合并转发消息
    setForwardMsgList(data) {
      this.forwardMsgList = data;
    },
    setHiddenChatList(flag) {
      this.hiddenChatList = flag;
    },

    // 设置群组成员列表
    async setMemberActive(members) {
      const all: any = [];
      const successList = await this.getGroupMembersName(members);
      successList.forEach((x) => {
        members.forEach((y) => {
          if (x.id === y.userId && !y.isDel) {
            all.push({ ...x, ...y });
          }
        });
      });
      this.groupMemberActive = all;
    },

    setQuoteMsg(msg) {
      this.quoteMsg = msg;
    },

    setSystemLimit(flag) {
      this.systemLimit = flag;
    },

    // 置顶会话
    async setTopChat(chat, top: boolean) {
      const stickyOnTopNumber = this.user?.chatProfiles?.filter((i) => i.isStickyOnTop).length;
      if (stickyOnTopNumber >= 10) {
        Message('最大置顶10条会话');
        return;
      }

      const param = {
        isStickyOnTop: top ? 1 : 0,
        targetId: chat.sessionId,
        type: chat.sessionType,
      };
      const { code, msg } = await addressbookUsersChatprofile(param);
      if (code === 0) {
        Message({
          message: msg,
          type: 'success',
        });
        this.updateUser();
      } else {
        Message({
          message: msg,
          type: 'error',
        });
      }
    },

    setUserIcs(data) {
      this.userIcs = data;
    },

    setUserInfo(data) {
      this.userMap.set(data.id * 1, data);
    },

    setUserMyInfo(data: any) {
      this.user = data;
    },

    // 获取用户协同页面菜单栏权限信息
    setUserRoleAuth(data) {
      this.userRoleAuth = data;
    },

    // 发起聊天
    startChat(data) {
      if (data.sessionType === 2) {
        return;
      }

      const index = this.chatList.findIndex((i) => {
        return i.sessionId === data.sessionId;
      });

      if (index === -1) {
        this.chatList.unshift({
          ...data,
          createTime: Date.now(),
        });
      } else {
        const item = this.chatList.splice(index, 1)[0];
        this.chatList.unshift(item);
      }
      this.setChatListActive(data.sessionId);
      this.setChatItemDetails(data);
    },

    // 更新消息列表最新消息
    updateChatListLatestMsg(data) {
      const sessionId = this.getMsgSessionId(data);
      const index = this.chatList.findIndex((i) => i.sessionId === sessionId);
      if (index !== -1) {
        // 自己给自己发消息
        if (data.to === data.from) {
          data.read = true;
        }

        // 判断该条信息是否存在于消息列表，给自己发消息时，会返回多条
        const { messages } = this.chatList[index];
        if (!messages) {
          setTimeout(() => {
            this.updateChatListLatestMsg(data);
          }, 500);
          return;
        }

        const messagesIndex = messages.findIndex((i) => i.msgId === data.msgId);
        if (messagesIndex === -1) {
          this.chatList[index].messages.push(data);
        }
      }
    },

    // 更新群组信息
    updateGroupInfo(init?) {
      if (!init) {
        this.getChartGroupList();
      }
      this.updateUser();
    },

    // 更新指定用户详情
    async updateUerInfo(id) {
      if (!id) {
        return null;
      }
      if (getUserInfoQueue.has(id)) {
        return;
      }
      getUserInfoQueue.add(id);
      const { code, data } = await addressbookUsersId(id);
      if (code === 0) {
        this.userMap.set(id * 1, data);
      }
      getUserInfoQueue.delete(id);
      return data;
    },

    // 更新用户信息
    updateUser: debounce(async (init?: boolean) => {
      console.log(init);
    }, 500),

    // 收到消息撤回更新消息状态
    updateWithdraw(data) {
      const msgId = data.msg.srcMsgId;
      // 更新会话列表
      this.chatList.forEach((chat) => {
        const id = this.getMsgSessionId(data);
        if (chat.id !== id) {
          return;
        }
        const index = chat.messages.findIndex((i) => i.msgId === msgId);
        if (index !== -1) {
          const target = chat.messages[index];
          target.revokeMsgType = target.msgType;
          target.msgType = 7;

          modifyMessages(data, {
            msg: Object.assign(data.msg, target.msg),
          });
        }
      });
    },

    // 更新协同用户接口
    async updateXietong(state, flag?) {
      const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
      if (!userInfo || !userInfo.userId) {
        return;
      }

      const { code } = await updateCollaboration({
        collaboration: state,
        userId: userInfo.userId,
      });

      if (state) {
        localStorage.setItem('_hasLoggedIn', 'false');
        this.chatListActive = 0;
      }

      if (code === 0 && !flag) {
        this.setcollaboration(!state);
        // 设置标记，表示已经调用过 updateXietong
        localStorage.setItem('hasUpdatedXietong', 'true');
      }
    },
    async updateXietongGroup() {
      await this.updateUser();
    },
    // 协同用户变更
    async xietongChange() {
      await this.updateUser();
    },
  },
  getters: {
    // 当前展示会话
    currentChat(): any {
      const index = this.chatList.findIndex((i) => i.sessionId === this.chatListActive);
      return this.chatList[index] || {};
    },
    currentChatMessages(): any {
      return this.currentChat.messages || [];
    },
  },
  id: 'PIM',
  state: (): PIMState => ({
    bondedStatus: false,
    chartGroupList: [],
    chatList: [],
    chatListActive: 0,
    collaboration: false,
    drafts: new Map(JSON.parse(localStorage.getItem('chat_drafts') || '[]')), // 从 localStorage 初始化草稿
    forwardMsgList: [],
    groupMemberActive: [],
    hiddenChatList: false,
    quoteMsg: {},
    systemLimit: false,
    user: null,
    userAvatar: new Map(),
    userIcs: {},
    userMap: new Map(),
    userRoleAuth: {},
  }),
});

// Need to be used outside the setup
export function usePIMStateWithOut() {
  return usePIMStore(store);
}
