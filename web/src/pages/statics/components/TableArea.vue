<script setup lang="ts">
  import { onMounted, ref, watch, inject, type Ref } from 'vue';

  import {
    dispositionCount,
    getOnlineStatistics,
    getPersonnelVerificationStatistics,
    getReplyDurationStatistics,
    groupByUser,
    getCoopUserRatingStat,
  } from '@/api/statics';
  import { useEmitter } from '@/hooks';
  import { useStaticsState } from '@/store';

  import LineChart from './Charts/LineChart.vue';
  import RatingStatChart from './RatingStatChart.vue';
  import SectionHeader from './SectionHeader.vue';
  import SimpleTable from './SimpleTable.vue';

  interface LicenseInfo {
    HADR?: string;
    LINKXACF?: string;
    LINKXBCF?: string;
    LINKXBS?: string;
    LINKXCCF?: string;
    LINKXGCF?: string;
    LINKXNDI?: string;
    LINKXNum?: string;
    LINKXSDF?: string;
    LINKXTCF?: string;
  }
  interface UserGrowthData {
    series: number[];
    xAxis: string[];
  }
  // 人员核查统计图表数据
  const personnelVerificationStatisticsData = ref<UserGrowthData>({
    series: [],
    xAxis: [],
  });
  // 从父组件注入 license 信息，避免重复请求 API
  const licenseInfo = inject<Ref<LicenseInfo>>('licenseInfo', ref<LicenseInfo>({}));
  interface TableColumn {
    align?: 'center' | 'left' | 'right'; // 明确定义为字面量类型
    label: string;
    prop: string;
    width?: number | string;
  }
  const StaticsStore = useStaticsState();
  const AverageList = ref<any>([]);
  const userList = ref<any>([]);
  const collaborativeList = ref<any>([]);
  const replyList = ref<any>([]);
  const activeName = ref<'first' | 'second'>('first');

  const replyColumns: TableColumn[] = [
    { align: 'center', label: '', prop: 'iconUrl', width: '40' },
    { align: 'center', label: '名称', prop: 'postName' },
    { align: 'center', label: '所属单位', prop: 'orgName' },
    { align: 'center', label: '回复总数', prop: 'replyCount' },
  ];
  const averageColumns: TableColumn[] = [
    { align: 'center', label: '', prop: 'iconUrl', width: '40' },
    { align: 'center', label: '名称', prop: 'postName' },
    { align: 'center', label: '所属单位', prop: 'orgName' },
    { align: 'center', label: '平均回复时长(秒)', prop: 'avgReplyDuration', width: '130' },
  ];
  const personColumns: TableColumn[] = [
    { align: 'center', label: '人员', prop: 'userName' },
    { align: 'center', label: '所属组织', prop: 'departmentName' },
    { align: 'center', label: '协同群个数', prop: 'totalCount' },
  ];
  // 协同岗在线统计
  const collaborativeColumns: TableColumn[] = [
    { align: 'center', label: '', prop: 'iconUrl', width: '40' },
    { align: 'center', label: '协同岗名称', prop: 'postName' },
    { align: 'center', label: '在线情况', prop: 'onlineRatio' },
    { align: 'center', label: '在线人员', prop: 'onlineUsers' },
    { align: 'center', label: '支撑群数', prop: 'groupCount' },
  ];

  watch(
    [() => StaticsStore.peerId, () => StaticsStore.departmentCode, () => StaticsStore.dateRang],
    ([peerId, code, date]) => {
      init(peerId, code, date);
    },
    { deep: true, immediate: true },
  );

  onMounted(async () => {
    // license 信息已从父组件注入，无需再请求
    // 监听刷新事件
    useEmitter('refreshAllData', () => {
      init(StaticsStore.peerId, StaticsStore.departmentCode, StaticsStore.dateRang);
    });
  });
  async function init(peerId, departmentCode, date) {
    if (!departmentCode || !date.endTime) return;
    const params = { peerId, departmentCode, ...date };
    dispositionCountFunc(params);
    replyDurationFunc(params);
    groupByUserFunc(params);
    getOnlineStatisticsFunc(params);
    getPersonnelVerificationStatisticsFunc(params);
  }

  async function dispositionCountFunc(params) {
    const res = await dispositionCount(params);
    const { code, data } = res;
    if (code === 0) {
      replyList.value = data || [];
    }
  }

  async function replyDurationFunc(params) {
    const res: any = await getReplyDurationStatistics(params);
    const { code, data } = res;
    if (code === 0) {
      // 对 回复时长 做四舍五入处理
      AverageList.value = (data || []).map((item) => ({
        ...item,
        avgReplyDuration: Math.round(item.avgReplyDuration) || 0,
      }));
    }
  }

  async function groupByUserFunc(params) {
    const res: any = await groupByUser(params);
    const { code, data } = res;
    if (code === 0) {
      userList.value = data || [];
    }
  }
  // 获取协同岗在线统计
  async function getOnlineStatisticsFunc(params) {
    const res: any = await getOnlineStatistics(params);
    const { code, data } = res;
    if (code === 0) {
      collaborativeList.value = (data || []).map((item) => ({
        ...item,
        // 添加onlineRatio字段，格式为"onlineCount/totalCount"
        onlineRatio: `${item.onlineCount || 0}/${item.totalCount || 0}`,
      }));
    }
  }

  async function getPersonnelVerificationStatisticsFunc(params) {
    const res: any = await getPersonnelVerificationStatistics(params);
    const { code, data } = res;
    if (code === 0) {
      const series = data?.map((item) => item.count);
      const xAxis = data?.map((item) => item.date);
      personnelVerificationStatisticsData.value = { series, xAxis };
    }
  }
</script>

<template>
  <div class="table-area">
    <div class="table-area-row">
      <!-- 左侧组合模块 -->
      <div class="table-area-col">
        <el-card class="group-card" shadow="hover">
          <!-- 主标题 -->
          <div class="main-title other-chart-title">
            <SectionHeader title="协同处置消息榜单" />
            <div v-if="licenseInfo.LINKXGCF === '1'" class="chart-tabs">
              <el-tabs v-model="activeName" class="demo-tabs">
                <el-tab-pane label="回复总数榜" name="first" />
                <el-tab-pane label="平均回复时长榜" name="second" />
              </el-tabs>
            </div>
          </div>
          <div class="sub-card" style="border: none">
            <SimpleTable
              v-if="licenseInfo.LINKXGCF === '1' && activeName === 'first'"
              :auto-scroll="true"
              :avatar-columns="['iconUrl']"
              :columns="replyColumns"
              height="100%"
              :scroll-speed="0.5"
              :table-data="replyList"
            />
            <SimpleTable
              v-else-if="licenseInfo.LINKXGCF === '1' && activeName === 'second'"
              :auto-scroll="true"
              :avatar-columns="['iconUrl']"
              :columns="averageColumns"
              height="100%"
              :scroll-speed="0.5"
              :table-data="AverageList"
            />
            <div v-else class="chart-license" style="flex: 1">
              协同处置消息TOP10功能受限，请联系管理员
            </div>
          </div>
        </el-card>
      </div>
      <div class="table-area-col">
        <el-card class="table-card" shadow="hover">
          <div class="table-title">
            <SectionHeader title="人员核查统计" />
          </div>
          <div class="chart-container">
            <LineChart
              v-if="licenseInfo.LINKXGCF === '1'"
              :data="personnelVerificationStatisticsData"
              height="100%"
            />
            <div v-else class="chart-license">
              人员核查统计功能受限，请联系管理员
            </div>
          </div>
        </el-card>
      </div>
      <!-- 右侧模块 -->
      <div class="table-area-col">
        <el-card class="table-card" shadow="hover">
          <div class="table-title">
            <SectionHeader title="创群榜单-人员" />
          </div>
          <SimpleTable
            v-if="licenseInfo.LINKXGCF === '1'"
            :auto-scroll="true"
            :columns="personColumns"
            height="100%"
            :scroll-speed="0.5"
            :table-data="userList"
          />
          <div v-else class="chart-license">
            创群榜单-人员功能受限，请联系管理员
          </div>
        </el-card>
      </div>
      <div class="table-area-col">
        <el-card class="table-card" shadow="hover">
          <div class="table-title">
            <SectionHeader title="协同岗在线统计" />
          </div>
          <SimpleTable
            v-if="licenseInfo.LINKXGCF === '1'"
            :auto-scroll="true"
            :avatar-columns="['iconUrl']"
            :columns="collaborativeColumns"
            height="100%"
            :scroll-speed="0.5"
            :table-data="collaborativeList"
          />
          <div v-else class="chart-license">
            协同岗在线统计功能受限，请联系管理员
          </div>
        </el-card>
      </div>

      <!-- 协同岗评分 -->
      <div class="table-area-col">
        <RatingStatChart title="协同岗评分" :api-fn="getCoopUserRatingStat" />
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  :deep(.el-card) {
    background-color: var(--background-white-color);
    border: 1px solid var(--el-card-border);
  }

  .table-area {
    width: 100%;
    height: 100%;
    min-height: 0;
    overflow: auto;

    .table-area-row {
      display: flex;
      align-items: stretch;
      height: 100%;
    }

    .table-area-col {
      display: flex;
      flex-direction: column;
      width: 20%;
      min-height: 0;
      padding: 0 10px;
      box-sizing: border-box;
    }

    .main-title {
      flex-shrink: 0;
      padding-bottom: 10px;
      font-size: 18px;
      font-weight: bold;
      color: var(--text-color);
    }

    .group-card {
      display: flex;
      flex: 1;
      flex-direction: column;
      height: 100%;
      border: 1px solid var(--el-card_body);

      :deep(.el-card__body) {
        display: flex;
        flex: 1;
        flex-direction: column;
        min-height: 0;
        padding: 10px;
      }

      .sub-card {
        display: flex;
        flex: 1;
        flex-direction: column;
        min-height: 0;
        padding: 0 10px;
        background: var(--background-white-color);
        border: 1px solid var(--card-table-border);
        border-radius: 4px;
      }
    }

    .table-card {
      display: flex;
      flex: 1;
      flex-direction: column;
      width: 100%;
      height: 100%;
      border: 1px solid var(--el-card_body);

      :deep(.el-card__body) {
        display: flex;
        flex: 1;
        flex-direction: column;
        min-height: 0;
        padding: 10px;
      }

      .table-title {
        flex-shrink: 0;
        margin-bottom: 15px;
        font-size: 16px;
        font-weight: bold;
        color: #666;
      }

      .table-title1 {
        font-size: 0.7vw;
        font-weight: 600;
        color: var(--text-color);
      }

      .chart-container {
        display: flex;
        flex: 1;
        flex-direction: column;
        min-height: 0;
      }
    }

    .user-cell {
      display: flex;
      align-items: center;

      .user-name {
        margin-left: 10px;
      }
    }
  }

  @media (max-width: 768px) {
    .table-area {
      .el-col {
        margin-bottom: 15px;
      }
    }
  }

  .chart-license {
    display: flex;
    flex: 1;
    align-items: center;
    justify-content: center;
    min-height: 0;
    color: var(--tabs-color);
  }

  .chart-tabs {
    :deep(.el-tabs__item.is-active) {
      color: var(--tabs-color) !important;
    }

    :deep(.el-tabs__content) {
      margin: 0 !important;
    }

    :deep(.el-tabs__item) {
      height: 30px !important;
      padding: 0 8px !important;
      font-size: 12px;
      color: var(--tabs-color);

      &.is-active {
        color: var(--tabs-active-color) !important;
      }
    }

    :deep(.el-tabs__active-bar) {
      background: var(--tabs-active-color);
    }

    :deep(.el-tabs__nav-wrap::after) {
      background-color: var(--tab-default-border) !important;
    }

    :deep(.el-tabs__header) {
      margin: 0;
    }
  }

  .other-chart-title {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: 8px;

    .chart-tabs {
      flex-shrink: 0;
    }
  }
</style>
