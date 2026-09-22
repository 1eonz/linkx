import type { ConfMember } from '@/pages/types/conference';

import { store } from '@/store';

import { defineStore } from 'pinia';

interface ConferenceState {
  broadcastMember: string[];
  broadcastOrWatch: '' | 'broadcast' | 'watch';
  confer: any;
  confMember: ConfMember[];
  confName: string;
  confProxy: Array<{ member: string; operation: string }>;
  createConferenceMemberList: ConfMember[];
  flexType: string;
  lastNotConnectedBroadcastMember: string[];
  supportCPModes: any[];
  uniqueCode: string;
}

export const getConfer = () => {
  return {
    belongId: '',
    chair: '',
    cid: '',
    conferenceId: '',
    conferenceMember: [],
    conferencePass: '',
    confName: '',
    isCalled: false,
    isMain: true,
    isVideo: false,
    speaking: '',
    status: '',
    uniqueCode: '',
    updateTime: '',
    wssUrl: '',
  };
};

export const useConferenceStore = defineStore({
  actions: {
    // 清空广播相关数据
    clearBroadcastData() {
      this.broadcastMember = [];
      this.flexType = '';
      this.broadcastMember = [];
    },
    // 清空会议数据
    clearConferData() {
      const confer = getConfer();
      Object.keys(confer).forEach((key) => {
        this.confer[key] = confer[key];
      });
      this.clearBroadcastData();
      this.confMember = [];
      this.uniqueCode = '';
      this.broadcastOrWatch = '';
      this.lastNotConnectedBroadcastMember = [];
    },
    // 设置广播人员
    setBroadcastMember(data) {
      this.broadcastMember = data;
    },
    setBroadcastOrWatch(data) {
      this.broadcastOrWatch = data;
    },
    setConfMember(data) {
      const members: ConfMember[] = [];
      data.forEach((item) => {
        const index = members.findIndex((i) => i.id === item.id || i.number === item.number);
        if (index === -1) {
          members.push(item);
        }
      });
      this.confMember = members;
    },
    setConfName(data) {
      this.confName = data;
    },
    setCreateConferenceMemberList(data) {
      this.createConferenceMemberList = data.slice(0, 19);
    },
    setFlexType(data) {
      this.flexType = data;
    },
    setLastNotConnectedBroadcastMember(data) {
      this.lastNotConnectedBroadcastMember = data;
    },
    setSupportCPModes(data) {
      this.supportCPModes = data;
    },
    setUniqueCode(data) {
      this.uniqueCode = data;
    },
    updateConferenceSpeaker(data) {
      this.confer.updateTime = Date.now();
      this.confer.chair = data.chair;
      this.confer.confName = data.confName;
      this.confer.speaking = data.speaking;
    },
    updateConferenceStatus(data) {
      this.confer.updateTime = Date.now();
      this.confer.status = data.status;
      this.confer.belongId = data.belongId;
      this.confer.isVideo = data.isVideo;
    },
    updateConferInfo(data) {
      const { cid, conferenceId } = data;
      const conferInfo = JSON.parse(localStorage.getItem('conferInfo') || '{}');
      if (cid) {
        conferInfo.cid = cid;
      }
      if (conferenceId) {
        conferInfo.conferenceId = conferenceId;
      }
      localStorage.setItem('conferInfo', JSON.stringify(conferInfo));

      this.confer.updateTime = Date.now();

      Object.keys(data).forEach((key) => {
        const val = data[key];
        if (val !== '' && val !== undefined && val !== null) {
          this.confer[key] = val;
        }
      });
    },
    updateConferMember(data) {
      this.confer.updateTime = Date.now();
      this.confer.conferenceMember = data;
    },
    updateConfMember(data) {
      const index = this.confMember.findIndex((item) => {
        return item.number === data.number;
      });

      if (index !== -1) {
        this.confMember.splice(index, 1, data);
      }
    },
    // 更新话权代理
    updateConfProxy(data) {
      const index = this.confProxy.findIndex((i) => i.member === data.member);

      if (index === -1) {
        this.confProxy.push(data);
      } else {
        this.confProxy[index] = data;
      }
    },
  },
  id: 'conference',
  state: (): ConferenceState => ({
    broadcastMember: [], // 广播人员
    broadcastOrWatch: '', // 当前是广播还是选看 broadcast/watch
    confer: getConfer(),
    confMember: [],
    confName: '',
    confProxy: [],
    createConferenceMemberList: [], // 创建会议的人员
    flexType: '', // 广播画面布局
    lastNotConnectedBroadcastMember: [], // 最新的没有连上会议的广播成员
    supportCPModes: [], // 支持的多画面
    uniqueCode: '', // 后台生成的会议唯一码
  }),
});

// Need to be used outside the setup
export function useConferenceStoreWithOut() {
  return useConferenceStore(store);
}
