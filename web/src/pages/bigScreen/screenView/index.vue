<script lang="ts" setup>
  import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
  import { useRoute, useRouter } from 'vue-router';

  import { queryPersonGroupListByParam } from '@/api/plan';
  import { queryCountEquipment, queryOrgStatistics } from '@/api/statistics';
  import TelImg from '@/assets/images/screen/capture_4.png';
  import PdtImg from '@/assets/images/screen/capture_5.png';
  import FacilityImg from '@/assets/images/screen/capture_6.png';
  import CarImg from '@/assets/images/screen/capture_7.png';
  import RecorderImg from '@/assets/images/screen/capture_8.png';
  import ThirdRecorderImg from '@/assets/images/screen/capture_9.png';
  import UvImg from '@/assets/images/screen/capture_10.png';
  import MeetingImg from '@/assets/images/screen/capture_11.png';
  import SeatsImg from '@/assets/images/screen/capture_12.png';
  import { appConfig } from '@/config';
  import EquipmentCount from '@/pages/communicationCenter/components/equipmentCount.vue';
  import { EMap, mapIsReady, mapManager } from '@/plugins/map';

  import * as echarts from 'echarts';

  import EchartsMap from './echartsMap.vue';
  import GroupNode from './groupNode.vue';
  import TreeContent from './treeContent.vue';

  const adminCode = { adcode: appConfig.settingData.ADCODE };
  const organization = { organizationId: appConfig.userData.organizationId };
  const organizationList = { organizationId: appConfig.userData.organizationId };
  const video = ref();
  const route = useRoute();
  const router = useRouter();
  const navId = ref('all');
  const showResource = ['seat'];
  const equipmentData = ref<any[]>([]);
  const tabType = ref('all');
  const mapId = 'mainMap';
  const isShowMap = ref(true);
  const isLoading = ref(true);
  const planDatas = ref<any[]>([]);

  let equipParams = {
    ...adminCode,
    ...organizationList,
  };

  const echartsInstance: any[] = [];
  const form = reactive<any>({ pageSize: 8, start: 1 });

  watch(route, () => {
    if (route.path.includes('screenView')) {
      video.value?.play();
      handleClick('all');
    }
  });

  onMounted(async () => {
    EMap({ mapId });
    initCharts();
    await mapIsReady(mapId);
    isShowMap.value = false;
    isLoading.value = false;
  });

  onBeforeUnmount(() => {
    mapManager.get(mapId)?.destroyMap();
    mapManager.delete(mapId);

    echartsInstance.forEach((item) => item.dispose());
    echartsInstance.length = 0;
  });

  async function queryData(params) {
    const { code, data } = await queryPersonGroupListByParam(params);
    planDatas.value = [];
    if (code === 0) {
      planDatas.value = data.records;
    }
  }

  function handleClick(type) {
    tabType.value = type;
    const params = {
      ...form,
      state: 0,
    };
    if (type === 'all') {
      queryData(params);
      return;
    }
    queryData({ ...params, planGroupType: tabType.value });
  }

  // function getStatusText(status) {
  //   if (status === 0) {
  //     return '未开始';
  //   }
  //   if (status === 1) {
  //     return '保障中';
  //   }
  //   if (status === 2) {
  //     return '已结束';
  //   }
  //   return '未开始';
  // }

  // function getStatusCss(status) {
  //   if (status === 0) {
  //     return 'img-no-start';
  //   }
  //   if (status === 1) {
  //     return 'img-start';
  //   }
  //   if (status === 2) {
  //     return 'img-end';
  //   }
  //   return 'img-no-start';
  // }

  function initCharts() {
    organizationChart();
    queryCountEquipmentData();
    const params = {
      ...form,
      state: 0,
    };
    queryData(params);
  }

  async function queryCountEquipmentData() {
    const { code, data } = await queryCountEquipment(equipParams);
    let deviceNum = '-';
    let deviceOnlineNum = '-';
    let pdtNum = '-';
    let pdtOnlineNum = '-';
    let facilityNum = '-';
    let facilityOnlineNum = '-';
    let policeCarNum = '-';
    let policeCarOnlineNum = '-';
    let pttNum = '-';
    let pttOnlineNum = '-';
    let thirdRecorderNum = '-';
    let thirdRecorderOnlineNum = '-';
    let droneNum = '-';
    let droneOnlineNum = '-';
    let conferenceTerminalNum = '-';
    let conferenceTerminalOnlineNum = '-';
    let seatNum = '-';
    let seatOnlineNum = '-';
    if (code === 0 && data) {
      deviceNum = data.deviceNum;
      deviceOnlineNum = data.deviceOnlineNum;
      pdtNum = data.pdtNum;
      pdtOnlineNum = data.pdtOnlineNum;
      facilityNum = data.facilityNum;
      facilityOnlineNum = data.facilityOnlineNum;
      policeCarNum = data.policeCarNum;
      policeCarOnlineNum = data.policeCarOnlineNum;
      pttNum = data.pttNum;
      pttOnlineNum = data.pttOnlineNum;
      thirdRecorderNum = data.thirdRecorderNum;
      thirdRecorderOnlineNum = data.thirdRecorderOnlineNum;
      droneNum = data.droneNum;
      droneOnlineNum = data.droneOnlineNum;
      conferenceTerminalNum = data.conferenceTerminalNum;
      conferenceTerminalOnlineNum = data.conferenceTerminalOnlineNum;
      seatNum = data.seatNum;
      seatOnlineNum = data.seatOnlineNum;
    }
    equipmentData.value = [];
    equipmentData.value.push({
      icon: TelImg,
      name: '警务终端',
      online: deviceOnlineNum,
      progressWidth:
        deviceNum === '-' || Number(deviceNum) === 0
          ? 0
          : (Number(deviceOnlineNum) / Number(deviceNum)) * 100,
      total: deviceNum,
    });
    equipmentData.value.push({
      icon: PdtImg,
      name: 'PDT',
      online: pdtOnlineNum,
      progressWidth:
        pdtNum === '-' || Number(pdtNum) === 0 ? 0 : (Number(pdtOnlineNum) / Number(pdtNum)) * 100,
      total: pdtNum,
    });
    equipmentData.value.push({
      icon: FacilityImg,
      name: '固定监控',
      online: facilityOnlineNum,
      progressWidth:
        facilityNum === '-' || Number(facilityNum) === 0
          ? 0
          : (Number(facilityOnlineNum) / Number(facilityNum)) * 100,
      total: facilityNum,
    });
    equipmentData.value.push({
      icon: CarImg,
      name: '车载图传',
      online: policeCarOnlineNum,
      progressWidth:
        policeCarNum === '-' || Number(policeCarNum) === 0
          ? 0
          : (Number(policeCarOnlineNum) / Number(policeCarNum)) * 100,
      total: policeCarNum,
    });
    equipmentData.value.push({
      icon: RecorderImg,
      name: '记录仪',
      online: pttOnlineNum,
      progressWidth:
        pttNum === '-' || Number(pttNum) === 0 ? 0 : (Number(pttOnlineNum) / Number(pttNum)) * 100,
      total: pttNum,
    });
    equipmentData.value.push({
      icon: ThirdRecorderImg,
      name: '国标记录仪',
      online: thirdRecorderOnlineNum,
      progressWidth:
        thirdRecorderNum === '-' || Number(thirdRecorderNum) === 0
          ? 0
          : (Number(thirdRecorderOnlineNum) / Number(thirdRecorderNum)) * 100,
      total: thirdRecorderNum,
    });
    equipmentData.value.push({
      icon: UvImg,
      name: '无人机',
      online: droneOnlineNum,
      progressWidth:
        droneNum === '-' || Number(droneNum) === 0
          ? 0
          : (Number(droneOnlineNum) / Number(droneNum)) * 100,
      total: droneNum,
    });
    equipmentData.value.push({
      icon: MeetingImg,
      name: '会议终端',
      online: conferenceTerminalOnlineNum,
      progressWidth:
        conferenceTerminalNum === '-' || Number(conferenceTerminalNum) === 0
          ? 0
          : (Number(conferenceTerminalOnlineNum) / Number(conferenceTerminalNum)) * 100,
      total: conferenceTerminalNum,
    });
    equipmentData.value.push({
      icon: SeatsImg,
      name: '坐席',
      online: seatOnlineNum,
      progressWidth:
        seatNum === '-' || Number(seatNum) === 0
          ? 0
          : (Number(seatOnlineNum) / Number(seatNum)) * 100,
      total: seatNum,
    });
  }

  function roundUpToNearestTen(num) {
    const tens = Math.ceil(num / 10); // 向上取整获取十位数
    const roundedNum = tens * 10; // 十位数乘以10
    return roundedNum;
  }

  function navChange(val, data) {
    isShowMap.value = val.length > 2;
    if (val.length > 2) {
      adminCode.adcode = data.administrativeArea;
      organization.organizationId = data.code;
      equipParams = {
        ...adminCode,
        organizationId: data.code,
      };
    } else {
      adminCode.adcode = data.code;
      equipParams = { ...adminCode, ...organizationList };
    }
    initCharts();
  }

  // 组织统计
  async function organizationChart() {
    const { code, data } = await queryOrgStatistics(equipParams);
    if (code === 0) {
      const { organizationNameList, orgNumList, userNumList } = data;
      let maxVal = Math.max.apply(null, userNumList);
      const minVal = Math.min.apply(null, userNumList);
      maxVal = Math.max(maxVal, 80);
      const diffVal = maxVal - minVal;
      const myChart = echarts.init(document.querySelector('#organization') as HTMLElement);
      const option = {
        color: [
          new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { color: orgNumList.length > 0 ? 'rgba(30, 231, 231, 1)' : '#cecfd150', offset: 0 },
            { color: orgNumList.length > 0 ? 'rgba(30, 231, 231, 0.35)' : '#cecfd150', offset: 1 },
          ]),
          new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { color: userNumList.length > 0 ? 'rgba(24, 144, 255, 1)' : '#cecfd150', offset: 0 },
            { color: userNumList.length > 0 ? 'rgba(24, 144, 255, 0.35)' : '#cecfd150', offset: 1 },
          ]),
        ],
        dataZoom: [
          {
            bottom: 6,
            brushSelect: false,
            endValue: organizationNameList.length > 0 ? 4 : 48,
            height: 8,
            left: '10%',
            rangeMode: [1, 12],
            show: organizationNameList.length > 5,
            startValue: 1,
            textStyle: false,
            type: 'slider',
            width: '80%',
            zoomLock: true,
          },
        ],
        grid: {
          bottom: 0,
          containLabel: true,
          height: '80%',
          left: '3%',
          right: '4%',
          top: '10%',
        },
        legend: {
          data: ['下属组织数量', '下属警员数量'],
          itemHeight: 10,
          itemWidth: 10,
          show: organizationNameList.length > 0,
          textStyle: {
            color: '#fff',
            fontSize: 12,
          },
          top: '-4px',
        },
        series: [
          {
            barGap: '50%',
            barWidth: 10,
            data: orgNumList.length > 0 ? orgNumList : [10, 20, 30, 40],
            name: '下属组织数量',
            tooltip: {
              valueFormatter: (value) => {
                return `${value} 个`;
              },
            },
            type: 'bar',
          },
          {
            barWidth: 10,
            data: userNumList.length > 0 ? userNumList : [10, 20, 30, 40],
            name: '下属警员数量',
            tooltip: {
              valueFormatter: (value) => {
                return `${value} 人`;
              },
            },
            type: 'bar',
          },
        ],
        tooltip: {
          axisPointer: {
            crossStyle: {
              color: '#999',
            },
            type: 'cross',
          },
          show: organizationNameList.length > 0,
          trigger: 'axis',
        },
        xAxis: [
          {
            axisLabel: {
              interval: 0,
              overflow: 'truncate',
              show: organizationNameList.length > 0,
              // show: true,
              textStyle: {
                color: '#fff',
              },
              truncate: '...',
              width: 60,
            },
            axisPointer: {
              type: 'shadow',
            },
            axisTick: {
              show: false,
            },
            data: organizationNameList.length > 0 ? organizationNameList : ['1', '2', '3', '4'],
            type: 'category',
          },
        ],
        yAxis: [
          {
            axisLabel: {
              interval: 0,
              show: true,
              textStyle: {
                color: '#fff',
              },
            },
            interval: Math.trunc(roundUpToNearestTen(diffVal) / 4),
            max: roundUpToNearestTen(diffVal),
            min: 0,
            nameTextStyle: {
              color: '#fff',
            },
            splitLine: {
              show: false,
            },
            type: 'value',
          },
        ],
      };
      myChart.setOption(option);
      echartsInstance.push(myChart);
    }
  }

  function handleClickMap(adcode: string) {
    adminCode.adcode = adcode;
    equipParams = { ...adminCode, ...organizationList };
    // console.log('===adcode==', adcode, equipParams);
    initCharts();
  }

  function addPlan() {
    router.push({
      name: 'planSafety',
      query: {
        opt: 'add',
      },
    });
  }

  function startPlan(data) {
    const { id, supportName } = data;
    router.push({
      name: 'planSpecial',
      query: {
        id,
        title: supportName,
      },
    });
  }
</script>

<template>
  <div class="screen-box">
    <div class="left-side">
      <div class="chart-item-top ground-glass">
        <div class="title"><span class="title-text">值班信息</span></div>
        <div class="content">
          <div id="traffic" class="chart">
            <TreeContent
              :is-main="true"
              :nav-id="navId"
              :show-resource="showResource"
              @nav-change="navChange"
            />
          </div>
        </div>
      </div>
      <div class="chart-item-bottom ground-glass">
        <div class="title"><span class="title-text">资源统计</span></div>
        <div class="content">
          <div class="chart-count">
            <EquipmentCount
              :ad-code="adminCode.adcode"
              :organization-id="organization.organizationId"
            />
          </div>
          <div v-for="item in equipmentData" :key="item.name" class="chart">
            <div class="count">
              <div class="count-img">
                <img alt="" class="img" :src="item.icon" />
              </div>
              <div class="count-name"> {{ item.name }} </div>
              <div class="count-text">
                <span>{{ item.online }}</span>
                <span class="count-all">{{ `/${item.total}` }}</span>
              </div>
            </div>
            <div class="td-slider">
              <div class="slider-background-wrapper">
                <div class="slider-background"></div>
              </div>
              <div
                class="slider-background-color"
                :style="{ width: `${item.progressWidth}%` }"
              ></div>
              <div class="slider-button-border">
                <div
                  class="slider-button"
                  :style="{
                    left:
                      item.progressWidth < 70 && item.progressWidth > 0
                        ? `${item.progressWidth - 1}%`
                        : `${item.progressWidth}%`,
                  }"
                ></div>
              </div>
            </div>
          </div>
          <div id="organization" class="chart-bar"></div>
        </div>
      </div>
    </div>
    <!-- 地图 -->
    <div v-show="isShowMap" :class="isLoading ? 'out-map' : 'track-map'">
      <div :id="mapId" class="map-container"></div>
    </div>
    <div v-show="!isShowMap" class="chart-map">
      <EchartsMap @handle-click-map="handleClickMap" />
    </div>

    <div class="right-side">
      <div class="chart-item-top ground-glass">
        <div class="title"><span class="title-text">活跃群组</span></div>
        <div class="content-empty">
          <GroupNode id="group_1" :key-code="49" node-id="group_1" />
          <GroupNode id="group_2" :key-code="50" node-id="group_2" />
          <GroupNode id="group_3" :key-code="51" node-id="group_3" />
          <GroupNode id="group_4" :key-code="52" node-id="group_4" />
        </div>
      </div>

      <div class="chart-item-bottom ground-glass">
        <div class="title"><span class="title-text">保障列表</span></div>
        <div v-if="planDatas.length > 0 || tabType !== 'all'" class="bottom-content">
          <div class="content-radio">
            <div
              class="content-radio-item"
              :class="{
                'content-radio-item-active': tabType === 'all',
              }"
              @click="handleClick('all')"
            >
              全部保障
            </div>
            <div
              class="content-radio-item"
              :class="{
                'content-radio-item-active': tabType === '0',
              }"
              @click="handleClick('0')"
            >
              重大安保
            </div>
            <div
              class="content-radio-item"
              :class="{
                'content-radio-item-active': tabType === '1',
              }"
              @click="handleClick('1')"
            >
              要人安保
            </div>
          </div>
          <div v-for="data in planDatas" :key="data.id" class="content-list">
            <img alt="" class="img" src="@/assets/images/screen/progress_point.png" />
            <TdTooltip :content="data.supportName" placement="top">
              <span class="name" @click="startPlan(data)">{{ data.supportName }}</span>
            </TdTooltip>
            <!-- <div :class="getStatusCss(data.state)">{{ getStatusText(data.state) }}</div> -->
          </div>
        </div>
        <div v-else class="content-empty">
          <img alt="" class="img-plan" src="@/assets/images/screen/plan_back.png" />

          <TdButton class="btn-item" text="新建保障" type="normal" @click="addPlan" />
        </div>
      </div>
    </div>
  </div>
  <video
    ref="video"
    autoplay="true"
    class="login-bg"
    loop="true"
    muted
    src="@/assets/video/screen_bg.mp4"
    type="video/mp4"
  ></video>
</template>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  .screen-box {
    position: relative;
    z-index: 2;
    width: 100%;
    height: 100%;
    padding-top: 2px;
    overflow: hidden;
  }

  .login-bg {
    position: absolute;
    top: 0;
    left: 0;
    display: block;
    width: 100%;
    height: 100%;
    object-fit: fill;
  }

  // .echarts-container {
  //   display: flex;
  //   width: 100%;
  //   height: 100%;
  //   justify-content: space-between;
  //   z-index: 4;

  .title {
    width: 412px;
    text-align: left;
    background: url('@/assets/images/screen/screen_title.png') no-repeat;
    background-size: contain;

    .title-text {
      margin-left: 40px;
      font-family: '优设标题黑';
      font-size: 26px;
      font-weight: 400;
      letter-spacing: 2px;
      vertical-align: top;
    }
  }

  .left-side {
    position: absolute;
    z-index: 5;
    display: flex;
    flex-wrap: wrap;
    width: 412px;
    height: calc(100vh - 10px);
    margin-left: 15px;

    .chart-item-top {
      width: 412px;
      height: 232px;
      margin-bottom: 10px;
      // border: 1px solid rgba(0, 0, 0, 1);
      // border-image: linear-gradient(180deg, rgba(26, 255, 251, 0) 0%, rgba(26, 255, 251, 1) 100%)
      //   30 30;
      // background: linear-gradient(180deg, rgba(6, 41, 74, 0.64) 0%, rgba(6, 41, 74, 0.26) 100%);
      // backdrop-filter: blur(8px);

      .content {
        width: 100%;
        height: calc(100% - 50px);

        .chart {
          width: 100%;
          height: 100%;
          overflow: hidden scroll;
        }
      }

      :deep(.tree-content) {
        overflow-y: hidden;
      }

      &:nth-of-type(odd) {
        margin-right: 10px;
      }
    }

    .chart-item-bottom {
      width: 412px;
      height: calc(100% - 350px);
      margin-bottom: 110px;
      // border: 1px solid rgba(0, 0, 0, 1);
      // border-image: linear-gradient(180deg, rgba(26, 255, 251, 0) 0%, rgba(26, 255, 251, 1) 100%)
      //   30 30;
      // background: linear-gradient(180deg, rgba(6, 41, 74, 0.64) 0%, rgba(6, 41, 74, 0.26) 100%);
      // backdrop-filter: blur(8px);

      .content {
        width: 100%;
        height: calc(100% - 40px);
        overflow: hidden scroll;

        .chart {
          width: 100%;
          height: 35px;
          margin-bottom: 18px;
          margin-left: 15px;

          .count {
            display: flex;
            width: 374px;
            height: 35px;
            font-size: 16px;
            font-weight: 400;
            line-height: 23px;
            color: rgb(230 255 255 / 100%);
            text-align: right;
            letter-spacing: 0;
            background-image: url('@/assets/images/screen/count.png');
            background-repeat: no-repeat;
            background-size: contain;

            .count-img {
              width: 50px;

              .img {
                position: relative;
                bottom: 5px;
                left: 11px;
                width: 24px;
              }
            }

            .count-name {
              width: 270px;
              font-size: 16px;
              font-weight: 700;
              line-height: 24px;
              color: rgb(255 255 255 / 100%);
              text-align: left;
              letter-spacing: 0;
            }

            .count-text {
              position: relative;
              right: 0;
              width: 100px;

              .count-all {
                color: rgb(230 255 255 / 50%);
              }
            }
          }
        }

        .chart-count {
          width: 388px;
          margin-bottom: 20px;
          margin-left: 10px;
        }

        .chart-bar {
          width: 372px;
          height: 110px;
          margin-left: 15px;
        }
      }

      &:nth-of-type(odd) {
        margin-right: 10px;
      }
    }
  }

  .right-side {
    position: absolute;
    top: 0;
    right: 0;
    z-index: 5;
    display: flex;
    flex-wrap: wrap;
    width: 412px;
    height: calc(100vh - 10px);
    margin-right: 15px;

    .chart-item-top {
      display: flex;
      flex-direction: column;
      width: 412px;
      height: 622px;
      margin-bottom: 10px;

      .content-empty {
        width: 412px;
        height: 622px;

        .btn-item {
          width: 200px;
          height: 40px;
          margin-top: 65%;
        }
      }

      .content {
        width: 100%;
        height: calc(100% - 10px);

        .chart {
          width: 100%;
          height: 100%;
        }
      }
    }

    .chart-item-bottom {
      display: flex;
      flex-direction: column;
      width: 412px;
      height: calc(100% - 740px);
      margin-bottom: 110px;

      .content {
        width: 100%;
        height: calc(100vh - 10px);

        .chart {
          width: 100%;
          height: 100%;
        }
      }

      .bottom-content {
        width: 412px;
        height: calc(100vh - 740px);
        overflow: hidden scroll;

        .content-radio {
          display: flex;
          align-items: center;
          width: 252px;
          height: 26px;
          margin-left: 10px;
          font-size: 12px;
          font-weight: 400;
          line-height: 17.38px;
          color: rgb(255 255 255 / 100%);
          letter-spacing: 0;
          background: rgb(0 29 66 / 100%);
          border: 0.5px solid rgb(255 255 255 / 19%);
          border-radius:
            2px 0,
            0,
            0;

          .content-radio-item {
            width: 77px;
            height: 18px;
            margin-left: 5px;
            text-align: center;
            cursor: pointer;
            border-radius: 2px;

            &-active {
              background: linear-gradient(
                180deg,
                rgb(7 143 143 / 100%) 0%,
                rgb(7 143 143 / 0%) 100%
              );
            }
          }
        }

        .content-list {
          display: flex;
          align-items: center;
          width: 388px;
          height: 24px;
          margin-top: 7px;
          margin-left: 10px;
          background: rgb(173 204 240 / 7%);

          .name {
            width: 350px;
            margin-left: 5px;
            font-size: 12px;
            font-weight: 400;
            line-height: 17.38px;
            color: rgb(255 255 255 / 100%);
            letter-spacing: 0;
            .ellipsis1();

            &:hover {
              color: #0affe7;
              cursor: pointer;
            }
          }

          .img-no-start {
            width: 80px;
            height: 24px;
            font-size: 12px;
            font-weight: 500;
            line-height: 21px;
            color: rgb(255 255 255 / 100%);
            text-align: center;
            letter-spacing: 0;
            background-image: url('@/assets/images/resource/tag_noStart.png');
            background-repeat: no-repeat;
            background-size: 100% 100%;
          }

          .img-start {
            width: 80px;
            height: 24px;
            font-size: 12px;
            font-weight: 500;
            line-height: 21px;
            color: rgb(255 255 255 / 100%);
            text-align: center;
            letter-spacing: 0;
            background-image: url('@/assets/images/resource/tag_start.png');
            background-repeat: no-repeat;
            background-size: 100% 100%;
          }

          .img-end {
            width: 80px;
            height: 24px;
            font-size: 12px;
            font-weight: 500;
            line-height: 21px;
            color: rgb(255 255 255 / 100%);
            text-align: center;
            letter-spacing: 0;
            background-image: url('@/assets/images/resource/tag_end.png');
            background-repeat: no-repeat;
            background-size: 100% 100%;
          }

          .img {
            width: 16px;
            height: 16px;
            margin-left: 5px;
          }
        }
      }

      .content-empty {
        width: 412px;
        text-align: center;

        .img-plan {
          width: 388px;
          height: 285px;
          margin-left: 10px;
        }

        .btn-item {
          position: fixed;
          top: 50%;
          left: 100px;
          width: 200px;
          height: 40px;
        }
      }
    }

    .right {
      height: 100%;
    }

    .organization {
      display: flex;
      flex: 1;
      align-items: center;
      justify-content: center;

      .chart {
        width: 100%;
        height: 130px;
      }
    }

    .organizer {
      display: flex;
      flex: 1;
      // height: calc(100% - 20px);
      align-items: center;
      justify-content: center;
      width: 100%;

      .chart {
        width: 33.3%;
        height: 130px;
      }
    }
  }

  .out-map {
    position: absolute;
    top: 0;
    left: -9999px;
    z-index: 1;
    display: flex;
    flex-direction: column;
    width: 100%;
    height: 100%;
    background-color: var(--background-dark-map);

    .map-container {
      flex: 1;
    }
  }

  .chart-map {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
  }

  .track-map {
    position: absolute;
    top: 0;
    left: 0;
    z-index: 1;
    display: flex;
    flex-direction: column;
    width: 100%;
    height: 100%;
    background-color: var(--background-dark-map);

    .map-container {
      flex: 1;
    }
  }

  .td-slider {
    position: relative;
    width: 374px;

    .slider-background-wrapper {
      position: relative;
      width: 100%;
      height: 18px;

      .slider-background {
        position: absolute;
        top: 6px;
        width: 100%;
        height: 3px;
        vertical-align: middle;
        background: rgb(230 247 255 / 10%);
      }
    }

    .slider-background-color {
      position: absolute;
      top: -15px;
      height: 3px;
      margin: 16px 0;
      margin-top: 21px;
      vertical-align: middle;
      background: linear-gradient(
        116.57deg,
        rgb(170 208 255 / 0%) 0%,
        rgb(105 158 248 / 100%) 100%
      );
    }

    .slider-button-border {
      position: relative;
      width: calc(100% - 13px);

      .slider-button {
        position: absolute;
        top: -18px;
        width: 29px;
        height: 29px;
        user-select: none;
        background-image: url('@/assets/images/screen/progress_point.png');
        background-repeat: no-repeat;
        background-size: 50% 50%;
      }
    }
  }
</style>
