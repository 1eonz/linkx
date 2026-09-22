import {
  flowConfigAll,
  flowMissionByLocation,
  flowMissionCount,
  flowMissionPaged,
} from '@/api/mission';
import { useI18n } from '@/hooks';
import eventAndMissionUtil from '@/pages/mission/eventAndMissionUtil';
import { setPosition } from '@/plugins/map';
import { store } from '@/store';

import dayjs from 'dayjs';
import { cloneDeep } from 'lodash-es';
import { defineStore } from 'pinia';

export type MissionStatus = 'finish' | 'pending' | 'underway';

type MissionCondition = {
  id: string;
  name: string;
}[];

interface MissionState {
  currentStatus: string;
  flowConfig: Map<string, any>;
  missionCondition: MissionCondition;
  missionData: any[];
  missionLocation: any[];
  missionTotal: {
    finish: number;
    pending: number;
    underway: number;
  };
}

function getCondition() {
  const { t } = useI18n();
  return [
    {
      id: 'pending',
      name: t('mission.missionList.pending'), // 待处理
    },
    {
      id: 'underway',
      name: t('mission.missionList.processing'), // 进行中
    },
    {
      id: 'finish',
      name: t('mission.missionList.completed'), // 已完成
    },
  ];
}

let queryTimes = 0;

const originTotal = {
  finish: 0,
  pending: 0,
  underway: 0,
};

export const useMissionStore = defineStore({
  actions: {
    // 删除任务
    deleteMissionItem(flowId) {
      this.queryMissionCount();

      // 过滤掉列表中被删除的数据
      const arr = this.missionData.filter((i) => i.flowId !== flowId);
      this.missionData = arr;

      // 过滤掉地图上被删除的数据
      const mapArr = this.missionLocation.filter((i) => i.flowId !== flowId);
      this.missionLocation = mapArr;
    },

    // 获取任务配置
    async getFlowConfigAll() {
      const { code, data } = await flowConfigAll(100, 1);
      if (code === 0) {
        // 河北先取type=1的
        data.records.forEach((item) => {
          this.flowConfig.set(item.id, item);
        });
      }
    },

    // 统计任务总数
    async queryMissionCount() {
      const { code, data } = await flowMissionCount();
      if (code === 0) {
        const total = cloneDeep(originTotal);
        data.forEach((item) => {
          Object.keys(this.getStatus).forEach((key) => {
            if (this.getStatus[key].includes(item.status)) {
              total[key] += item.cnt;
            }
          });
        });
        this.missionTotal = total;
      }
    },

    /**
     * 获取任务
     * @param status 状态
     * @param loadMore 加载更多
     */
    async queryMissionData(status = 'pending', loadMore = false) {
      this.currentStatus = status;
      this.missionCondition = getCondition();

      if (this.flowConfig.size === 0) {
        await this.getFlowConfigAll();
      }

      this.queryMissionCount();

      const param: any = {
        pageNum: 1,
        pageSize: 20,
      };

      if (status !== 'all') {
        param.status = this.getStatus[status].join(',');
      }

      if (loadMore) {
        const total = this.missionTotal[status];
        const current = this.missionData.length;
        if (current >= total) {
          return;
        }
        param.pageNum = Number.parseInt(String(current / 20)) + 1;
      }

      const { code, data } = await flowMissionPaged(param);
      if (code === 0) {
        const { records } = data;
        const arr = eventAndMissionUtil.evenAndMissionDataInit(records);
        if (loadMore) {
          const filter = arr.filter((item) => {
            const index = this.missionData.findIndex((i) => i.flowId === item.flowId);
            return index === -1;
          });
          this.missionData = [...this.missionData, ...filter];
        } else {
          this.missionData = arr;
        }
      }
    },

    // 获取任务地图展示数据
    async queryMissionLocation() {
      if (queryTimes >= 3) {
        return;
      }

      if (this.flowConfig.size === 0) {
        await this.getFlowConfigAll();
      }

      this.queryMissionCount();

      queryTimes++;
      const f = 'YYYY-MM-DD HH:mm:ss';
      const param = {
        maxCreateTime: dayjs().format(f),
        minCreateTime: dayjs().subtract(2, 'days').format(f), // TODO:查最近两天
      };
      const { code, data } = await flowMissionByLocation(param);
      if (code === 0) {
        this.missionLocation = data.map((i) => {
          i.id = i.flowId;
          return i;
        });
      }
    },

    // 更新任务
    updateMissionItem(data, type) {
      this.queryMissionCount();

      const d = eventAndMissionUtil.evenAndMissionDataInit([data])[0];

      // 处理列表更新
      const index = this.missionData.findIndex((i) => i.flowId === data.flowId);
      if (this.getStatus[this.currentStatus]?.includes(data.status)) {
        if (type === 'add') {
          this.missionData.unshift(d);
        } else if (index !== -1) {
          this.missionData.splice(index, 1, d);
        }
      }

      // 处理地图更新
      const mapArr = this.missionLocation.filter((i) => i.flowId !== data.flowId);
      mapArr.unshift({
        ...d.location,
        flowId: d.flowId,
        status: d.status,
      });
      this.missionLocation = mapArr;
    },
  },
  getters: {
    // 获取任务分类数据
    getMissionData(): any {
      const ret = this.missionData.map((item) => {
        setPosition(item);
        return item;
      });
      return ret;
    },
    getStatus(): any {
      const { statusMapping } = this.flowConfig.get('1') || {};
      return {
        finish: statusMapping?.doneStatus || [],
        pending: statusMapping?.notStartedStatus || [],
        underway: statusMapping?.inProcessStatus || [],
      };
    },
  },
  id: 'mission',
  state: (): MissionState => ({
    currentStatus: '', // 当前
    flowConfig: new Map(),
    missionCondition: getCondition(), // 任务状态分类
    missionData: [],
    missionLocation: [],
    missionTotal: cloneDeep(originTotal),
  }),
});

// Need to be used outside the setup
export function useMissionStoreWithOut() {
  return useMissionStore(store);
}
