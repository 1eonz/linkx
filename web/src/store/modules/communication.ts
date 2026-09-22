import { getAllCallRecordPage, getCalledRecordPage, querySmsList, saveCallRecord } from '@/api/sms';
import { appConfig } from '@/config';
import { CategoryEnum } from '@/enums';
import { useEmitter } from '@/hooks';
import { getHistoryCallInfo } from '@/pages/resource/resourceHelper';
import { mrsFunc } from '@/plugins/mspPlayer';
import { store } from '@/store';
import { delay } from '@/utils';

import { cloneDeep } from 'lodash-es';
import { defineStore } from 'pinia';

export type MsgCache = {
  content: string; // 内容
  fromId: string; // 消息来源id
  fromName: string; // 消息来源name
  groupId: string; // 群组id
  groupName: string; // 群组name
  isGroup: boolean; // 是否是群组
  isRead: any; // 是否已读
  msgBelongId: string; // 消息来源id
  sendTime: any;
  status: string; // 消息状态
  time: string; // 时间
  title: any;
  type: string; // 消息类型
  userId: string;
};

type HistoryDial = {
  belongId: string; // 发送者id
  belongName: string; // 发送者name
  callee: string; // 被呼号码
  calleeName: string; // 被呼姓名
  caller: string; // 主呼号码
  callerName: string; // 主呼名字
  callType: string; // 通话类型
  cid: string; // 通话cid
  endTime: number; // 通话结束时间
  isCalled: boolean; // 是否是被呼叫者
  isConnect: boolean; // 是否接通 0-未接通 1-接通
  isRead: boolean; // 是否已读 0-未读 1-已读
  startTime: number; // 通话开始时间
  status: string; // 通话状态
};

interface CommunicationState {
  comm: any;
  communicateCallId: string[];
  communicationExDesc: string;
  communicationMsgStatus: number;
  distributeStatus: any;
  groupInfoCard: any;
  hasComm: boolean;
  // 未接、已接（来电/去电） 语音点呼/视频点呼/视频查看(视频回传/视频监控/视频分享)
  historyDialList: HistoryDial[];
  mediaData: any;
  missedCallNum: number;
  missedCallRead: boolean;
  monitorCallId: string[];
  mrsList: any;
  msgListCache: MsgCache[];
  muteActionGroup: [];
  patchGroupGrpId: string;
  sdkInitStatus: string; // sdk初始化状态
  temporarilyDialList: HistoryDial[];
  updateNowGroupMsgCacheId: string;
  videoCallId: string[];
  voiceCallId: string[];
}

export const useCommunicationStore = defineStore({
  actions: {
    addCid2media({ id, media }) {
      this.mediaData[id] = media;
    },
    // 短消息
    addCommunicateCallId(data) {
      this.communicateCallId.push(data);
    },
    // 删除所有的通信
    deleteAllComm() {
      const { comm } = this;
      Object.keys(comm).forEach((key) => {
        if (key !== 'updateInfo') {
          const target = comm[key];
          this.deleteComm({
            isdn: key,
            type: Object.keys(target)[0],
          });
        }
      });

      useEmitter().emit('commUpdate', this.comm);
    },
    // 删除通信
    deleteComm(data) {
      const { isdn, type } = data;
      const { comm } = this;

      const phone = (appConfig.settingData.DIALOUT_PREFIX || '') + isdn;

      if (comm && (comm[isdn] || comm[phone])) {
        Object.keys(comm).forEach((key) => {
          if (key === 'updateInfo') {
            this.comm[key] = {
              ...comm[key],
              isdn,
              opt: 'delete',
              type,
              updateTime: Date.now(),
            };
            return;
          }
          if (key === isdn || key === phone) {
            delete this.comm[key]?.[type];

            if (Object.keys(this.comm[key]).length === 0) {
              Reflect.deleteProperty(this.comm, key);
            }
          }
        });
      }

      useEmitter().emit('commUpdate', this.comm);
    },
    deleteDistributeStatus(data) {
      if (
        !this.distributeStatus ||
        !Object.prototype.hasOwnProperty.call(this.distributeStatus, data.isdn)
      ) {
        return;
      }
      Reflect.deleteProperty(this.distributeStatus[data.isdn], data.peerId);
      if (Object.keys(this.distributeStatus[data.isdn]).length === 0) {
        Reflect.deleteProperty(this.distributeStatus, data.isdn);
      }
      this.distributeStatus.updateInfo.updateTime = Date.now();
      this.distributeStatus.updateInfo.isdn = data.isdn;
    },
    detCommunicateCallId(data) {
      this.communicateCallId = this.communicateCallId.filter((item) => {
        return item !== data;
      });
    },
    detMonitorCallId(data) {
      this.monitorCallId = this.monitorCallId.filter((item) => {
        return item !== data;
      });
    },
    detVideoCallId(data) {
      this.videoCallId = this.videoCallId.filter((item) => {
        return item !== data;
      });
    },
    async getAllCallRecordPage(pageNum = 1) {
      const param = {
        account: appConfig.isdn,
        endTime: '',
        pageNum,
        pageSize: 999,
        startTime: '',
      };

      const { code, data } = await getAllCallRecordPage(param); // 查询所有通话记录
      if (code === 0) {
        this.setHistoryDialList(data.records);
        localStorage.setItem('callRecordUpdate', JSON.stringify(data.records.length));
      }
    },
    async getCalledRecordPage(status) {
      const param = {
        account: appConfig.isdn,
        endTime: '',
        pageNum: 1,
        pageSize: 10,
        startTime: '',
        status, // 0-未接 1-已接通
      };
      const { code: callCode, total } = await getCalledRecordPage(param); // 查询未接来电
      if (callCode === 0) {
        this.setMissCalledNum(total);
      }
    },
    initHistoryDialList() {
      this.getAllCallRecordPage();
    },
    async loadMoreMrsList() {
      const { data, query, total } = this.mrsList;
      if (data.length >= total) return;

      query.offset = `${data.length}`;
      const { fileTotalNum, rsp, list } = await mrsFunc.queryRecord(query);
      const filter = (list || []).filter((i: any) => i.call_type === query.callType);
      if (rsp === '0') {
        Object.assign(this.mrsList, {
          data: [...data, ...filter],
          query,
          total: Number(fileTotalNum),
        });
      }
    },
    // 查询短信列表
    async queryMessageList() {
      const { isdn } = appConfig;
      const param = {
        account: isdn,
        pageNum: 1,
        pageSize: 999,
        title: '',
      };
      const { code, data } = await querySmsList(param);

      if (code === 0) {
        this.msgListCache = data.records.map((i) => {
          return { ...i, msgBelongId: i.msgBelongto };
        });
      }
    },
    async queryMrsList(query) {
      const { fileTotalNum, rsp, list } = await mrsFunc.queryRecord(query);
      const data = (list || []).filter((i: any) => i.call_type === query.callType);
      if (rsp === '0') {
        Object.assign(this.mrsList, {
          data,
          query,
          total: Number(fileTotalNum),
        });
      } else {
        Object.assign(this.mrsList, {
          data: [],
          query,
          total: 0,
        });
      }
      return data;
    },
    // 拒接来电
    async rejectDial(info) {
      const { cid, fromUid, toUid, type } = info;
      const { callerInfo } = await getHistoryCallInfo(fromUid, toUid);
      const param = {
        belongId: fromUid,
        belongName: callerInfo.name,
        callee: toUid, // 被呼
        calleeName: appConfig.userData.name,
        caller: fromUid,
        callerName: callerInfo.name, // 主呼 --待查
        callType: type,
        cid,
        endTime: '',
        isCalled: true,
        isConnect: false,
        isRead: false,
        startTime: Date.now(),
        status: 'release',
      };
      // icc被呼：主呼调度员(icc)不存，其余icc存
      await (Number(callerInfo.category) === CategoryEnum.seat
        ? delay(1000)
        : this.saveCallRecords(param));
      await this.getAllCallRecordPage();
    },
    async saveCallRecords(param) {
      await saveCallRecord(param); // 保存主呼-未接，被呼-接通
    },
    setCommunicationExDesc(data) {
      this.communicationExDesc = data;
    },
    setGroupInfoCard(data) {
      this.groupInfoCard[data.groupId] = { ...data };
    },
    setHasComm(data) {
      this.hasComm = data;
    },
    // 历史通讯
    setHistoryDialList(data) {
      this.historyDialList = data;
    },
    setMissCalledNum(num) {
      this.missedCallNum = num;
    },
    setMissCalledRead(isRead) {
      this.missedCallRead = isRead;
    },
    setPatchGroupGrpId(grpId) {
      this.patchGroupGrpId = grpId;
    },
    setSdkInitStatus(data) {
      this.sdkInitStatus = data;
    },
    setTemporarilyDialList(data) {
      this.temporarilyDialList = [...data];
    },
    /**
     * 更新通信状态
     * 所有使用comm的地方都不应该使用watch监听(性能问题)
     * @param data
     * @returns
     */
    updateComm(data) {
      const { isdn, status, type, value } = data;
      const updateInfo = { ...cloneDeep(data), updateTime: Date.now() };

      if (!this.comm[isdn]) {
        this.comm[isdn] = {
          [type]: { ...value, ...status },
        };
        updateInfo.opt = 'add';
      } else if (!this.comm[isdn][type]) {
        this.comm[isdn][type] = { ...value, ...status };
        updateInfo.opt = 'add';
      } else if (status) {
        const { device } = this.comm[isdn][type];
        this.comm[isdn][type] = Object.assign(this.comm[isdn][type], {
          ...value,
          ...status,
          device,
        });
        updateInfo.opt = 'update';
      }
      this.comm.updateInfo = updateInfo;

      useEmitter().emit('commUpdate', this.comm);
    },
    updateDistributeStatus(data) {
      this.distributeStatus.updateInfo.updateTime = Date.now();
      this.distributeStatus.updateInfo.isdn = data.isdn;
      if (
        !Object.prototype.hasOwnProperty.call(this.distributeStatus, data.isdn) ||
        !this.distributeStatus[data.isdn]
      ) {
        this.distributeStatus[data.isdn] = {};
        this.distributeStatus[data.isdn][data.peerId] = data.status;
      } else {
        this.distributeStatus[data.isdn][data.peerId] = data.status;
      }
    },
    // 通话列表已读未读
    updateHistoryDialRead() {
      const index = this.historyDialList.findIndex((i) => !i.isRead);
      if (index !== -1) {
        const arr = this.historyDialList.map((item) => {
          return { ...item, isRead: true };
        });
        this.historyDialList = arr;
        localStorage.setItem('historyDialList', JSON.stringify(arr));
      }
    },
    // 更新短信已读状态
    updateUnreadStatus(chatId) {
      const arr = [...this.msgListCache];
      this.msgListCache = arr.map((item: any) => {
        if (item.chatId === chatId) {
          item.isRead = 1;
        }
        return item;
      });
    },
  },
  id: 'communication',
  state: (): CommunicationState => ({
    // 通信状态相关
    comm: {
      updateInfo: {
        groupsIsdn: [],
        isdn: '',
        message: {
          eventName: '',
          rsp: '',
          value: {},
        }, // 初始数据
        opt: '',
        status: {},
        type: '',
        updateTime: '',
      },
    },
    communicateCallId: [], // 短消息
    communicationExDesc: '',
    communicationMsgStatus: 0, // 通信code变化
    distributeStatus: {
      updateInfo: {
        isdn: '',
        updateTime: '',
      },
    },
    groupInfoCard: {}, // 群组详情7.21
    hasComm: false, // 用于表示用户是否具有通信账户
    historyDialList: [], // 通话记录
    mediaData: {},
    missedCallNum: 0,
    missedCallRead: true,
    monitorCallId: [], // 视频查看
    mrsList: {
      data: [],
      query: {},
      total: 0,
    },
    msgListCache: [], // 短信列表
    muteActionGroup: [], // 静音的群组
    patchGroupGrpId: '',
    sdkInitStatus: '',
    temporarilyDialList: [],
    updateNowGroupMsgCacheId: '',
    videoCallId: [], // 视频点呼
    voiceCallId: [], // 语音点呼
  }),
});

// Need to be used outside the setup
export function useCommunicationStoreWithOut() {
  return useCommunicationStore(store);
}
