<script lang="ts" setup>
  import { onBeforeUnmount, onMounted, ref, unref } from 'vue';

  import { queryOrganizationById } from '@/api/resource';
  import {
    queryCountAlarm, // 预警统计
    queryCountEquipment, // 设备统计
    queryCountSnapshotAndKeyPersonnel, // 重点人员与记录仪抓拍
    queryEvidenceDocuments, // 证据文件统计
    queryOrganizeTrafficStatistics, // 组织话务统计
    querySecondLevelDomain, // 二级组织统计
    queryTerminalStatistics, // 无线归档使用统计
  } from '@/api/statistics';
  import { appConfig } from '@/config';
  import { useI18n } from '@/hooks';

  import * as echarts from 'echarts';

  const { t } = useI18n();

  const adminCode = { adcode: '130700' };
  const organizationList = { organizationId: appConfig.userData.organizationId };
  const isShow = appConfig.settingData.FIXED_MONITORING_ALERT === '1'; // 是否显示固定监控预警

  // 获取当前日期
  const currentDate = new Date();

  // 获取当前月份的第一天
  const firstDayOfMonth = new Date(currentDate.getFullYear(), currentDate.getMonth(), 1);
  firstDayOfMonth.setHours(0, 0, 0, 0);

  // 获取当前月份的最后一天
  const lastDayOfMonth = new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 0);
  lastDayOfMonth.setHours(23, 59, 59, 999);

  // 获取当前日期的开始时间
  const startOfDay = new Date(
    currentDate.getFullYear(),
    currentDate.getMonth(),
    currentDate.getDate(),
  );
  startOfDay.setHours(0, 0, 0, 0);

  // 获取当前日期的结束时间
  const endOfDay = new Date(
    currentDate.getFullYear(),
    currentDate.getMonth(),
    currentDate.getDate(),
  );
  endOfDay.setHours(23, 59, 59, 999);
  const times = [
    {
      dateValue: {
        endTime: formatDate(endOfDay),
        startTime: formatDate(startOfDay),
      },
      id: 'day',
      name: t('common.dateDay.day'),
    },
    {
      dateValue: {
        endTime: formatDate(lastDayOfMonth),
        startTime: formatDate(firstDayOfMonth),
      },
      id: 'month',
      name: t('common.dateDay.month'),
    },
  ];

  let cycle = {
    endTime: formatDate(endOfDay),
    startTime: formatDate(startOfDay),
  };

  let params = {
    ...adminCode,
    ...cycle,
    ...organizationList,
  };
  let equipParams = {
    ...adminCode,
    ...organizationList,
  };
  const activeTime = ref('day');
  const evidenceList = ref<any>([]);
  const evidenceEmpty = ref<any>(false);
  const alarmList = ref<any>({});
  const orgNum = ref(0);
  const secondsDomainList = ref<any>([]);
  const navLink = ref<any[]>([]);
  const currentOrgList = ref<any[]>([]);

  const echartsInstance: any[] = [];

  const unit = {
    audio: 'minute',
    other: 'minute',
    picture: 'sheet',
    video: 'hour',
  };

  const size = {
    audio: 'MB',
    other: 'MB',
    picture: 'MB',
    video: 'GB',
  };
  onMounted(async () => {
    getOrganization(); // 组织列表
    initCharts();
  });

  function initCharts() {
    getCountAlarm(); // 获取预警数据及告警数据
    getEvidenceData(); // 证据文件
    organizationTrafficChart(); // 组织话务统计
    equipmentChart(); // 设备统计
    getCountSnapshotAndKeyPerson(); // 重点人员与记录仪抓拍统计
    organizationChart(); // 无线归档使用统计统计
    getSecondsDomainData(); // 二级域
  }

  onBeforeUnmount(() => {
    echartsInstance.forEach((item) => item.dispose());
    echartsInstance.length = 0;
  });

  // 格式化日期为YYYY-MM-DD hh:mm:ss
  function formatDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');

    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
  }

  // 获取组织
  async function getOrganization() {
    const param = {
      id: appConfig.userData.organizationId,
      isVideoConference: 0,
    };
    const { code, data } = await queryOrganizationById(param);
    if (code === 0) {
      const { children } = data[0];
      currentOrgList.value = children || [];
      navLink.value = [data[0]];
    }
  }

  function handlePath(data, index) {
    if (index < unref(navLink).length - 1) {
      unref(navLink).splice(index + 1);
      currentOrgList.value = data.children || [];
      organizationList.organizationId = data.id;
      params = { ...adminCode, ...organizationList, ...cycle };
      equipParams = { ...adminCode, ...organizationList };
      initCharts();
    }
  }

  function handleMenuClick(data) {
    navLink.value.push(data);
    currentOrgList.value = data.children || [];
    organizationList.organizationId = data.id;
    params = { ...adminCode, ...organizationList, ...cycle };
    equipParams = { ...adminCode, ...organizationList };
    initCharts();
  }

  function createBar(element, yLabel, yData) {
    const myChart = echarts.init(document.getElementById(element) as HTMLElement);
    const option = {
      dataZoom: [
        {
          backgroundColor: 'none',
          borderColor: 'transparent',
          brushSelect: false,
          dataBackground: {
            areaStyle: {
              color: 'none',
            },
            lineStyle: {
              width: 0,
            },
          },
          endValue: 5,
          filterColor: 'none',
          handleStyle: {
            border: 'none',
            borderWidth: 0,
            color: 'none',
          },
          height: '90%',
          selectedDataBackground: {
            areaStyle: {
              color: 'yellow',
            },
            lineStyle: {
              cap: 'round',
              color: 'rgb(50,153,227)',
              width: 4,
            },
          },
          showDetail: false,
          startValue: 0,
          type: 'slider',
          width: 4,
          yAxisIndex: 0,
          zoomLock: true,
        },
      ],

      grid: {
        bottom: 0,
        containLabel: true,
        height: '100%',
        left: '3%',
        right: '14%',
        top: '6%',
      },
      series: [
        {
          backgroundStyle: {
            borderRadius: [0, 30, 30, 0],
            color: 'rgba(255,255,255,0.1)',
          },
          barWidth: 10,
          data: yData,
          itemStyle: {
            normal: {
              barBorderRadius: [0, 30, 30, 0],
              color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                {
                  color: 'rgba(31, 174, 222, 1)',
                  offset: 1,
                },
                {
                  color: 'rgba(14, 96, 178, 1)',
                  offset: 0,
                },
              ]),
              shadowBlur: 0,
              shadowColor: 'rgba(87,220,222,0.7)',
            },
          },
          label: {
            normal: {
              position: [260, 0],
              show: true,
              textStyle: {
                color: 'rgba(255, 255, 255, 1)',
              },
            },
          },
          name: t('common.screen.number'),
          showBackground: true,
          type: 'bar',
          zlevel: 1,
        },
      ],
      tooltip: {
        axisPointer: {
          type: 'none',
        },
        formatter(params) {
          return (
            `${params[0].name}<br/>` +
            `<span style='display:inline-block;margin-right:5px;border-radius:10px;width:9px;height:9px;background-color:rgba(36,207,233,0.9)'></span>${
              params[0].seriesName
            } : ${params[0].value} <br/>`
          );
        },
        trigger: 'axis',
      },
      xAxis: {
        show: false,
        type: 'value',
      },
      yAxis: [
        {
          axisLabel: {
            margin: 15,
            overflow: 'truncate',
            show: true,
            textStyle: {
              color: 'rgba(153, 179, 200, 1)',
            },
            truncate: '...',
            width: 60,
          },
          axisLine: {
            show: false,
          },
          axisTick: {
            show: false,
          },
          data: yLabel,
          inverse: true,
          splitLine: {
            show: false,
          },
          type: 'category',
        },
      ],
    };
    myChart.setOption(option);
    echartsInstance.push(myChart);
  }

  // 预警统计
  async function getCountAlarm() {
    const { code, data } = await queryCountAlarm(params);
    if (code === 0) {
      // console.log('预警统计-getCountAlarm', data);
      alarmList.value = data;
    }
  }

  // 证据文件数据
  async function getEvidenceData() {
    const { code, data } = await queryEvidenceDocuments(params);
    if (code === 0) {
      // console.log('证据文件数据-getEvidenceData', data);
      if (data?.length > 0) {
        data.forEach((item) => {
          item.sizeNum = `${item.sizeNum} ${size[item.name]}`;
          item.durationNum = `${item.durationNum} ${t(`statistics.unit.${unit[item.name]}`)}`;
          item.name = t(`statistics.evidence.${item.name}`);
        });
        evidenceList.value = [data[0]];
        evidenceEmpty.value = false;
      } else {
        evidenceEmpty.value = true;
        evidenceList.value = [
          {
            durationNum: `- ${t('statistics.unit.hour')}`,
            name: t('mission.missionInformation.video'),
            num: '-',
            sizeNum: '- GB',
          },
        ];
      }
    }
  }

  // 组织话务统计
  async function organizationTrafficChart() {
    const { code, data } = await queryOrganizeTrafficStatistics(params);
    let area = [];
    let used = [];
    let usageRate = [];
    if (code === 0) {
      // console.log('组织话务统计-organizationTrafficChart', data);
      area = data.area;
      used = data.used;
      usageRate = data.usageRate;
    }
    const myChart = echarts.init(document.querySelector('#organization-traffic') as HTMLElement);
    const left = [120, 132, 101, 134, 190];
    const right = [0.1, 0.2, 0.3, 0.4, 0.8];
    const total = 10;

    const option = {
      color: [
        new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { color: used?.length > 0 ? 'rgba(24, 144, 255, 1)' : '#cecfd150', offset: 0 },
          { color: used?.length > 0 ? 'rgba(24, 144, 255, 1)' : '#cecfd150', offset: 1 },
        ]),
        new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { color: usageRate?.length > 0 ? 'rgba(30, 231, 231, 1)' : '#cecfd150', offset: 0 },
          { color: usageRate?.length > 0 ? 'rgba(30, 231, 231, 0.35)' : '#cecfd150', offset: 1 },
        ]),
      ],
      dataZoom: [
        {
          backgroundColor: 'none',
          borderColor: 'transparent',
          brushSelect: false,
          dataBackground: {
            areaStyle: {
              color: 'none',
            },
            lineStyle: {
              width: 0,
            },
          },
          endValue: 4,
          filterColor: 'none',
          handleStyle: {
            border: 'none',
            borderWidth: 0,
            color: 'none',
          },
          height: '90%',
          selectedDataBackground: {
            areaStyle: {
              color: 'yellow',
            },
            lineStyle: {
              cap: 'round',
              color: 'rgb(50,153,227)',
              width: 4,
            },
          },
          showDetail: false,
          startValue: 0,
          type: 'slider',
          width: 4,
          yAxisIndex: 0,
          zoomLock: true,
        },
      ],
      grid: {
        bottom: '8%',
        containLabel: true,
        left: '3%',
        right: '16%',
        top: '10%',
      },
      legend: [
        {
          data: [t('common.screen.usage')],
          itemHeight: 10,
          itemWidth: 10,
          right: '12',
          show: used?.length > 0,
          textStyle: {
            color: '#fff',
          },
        },
        {
          data: [t('common.screen.perUsage')],
          itemHeight: 10,
          itemWidth: 10,
          right: '25%',
          show: usageRate?.length > 0,
          textStyle: {
            color: '#fff',
          },
        },
      ],
      series: [
        {
          barWidth: 10,
          data: used?.length > 0 ? used.map((i) => 0 - i).reverse() : left.map((i) => 0 - i), // XY轴翻转，需要翻转后端返回数据
          name: t('common.screen.usage'),
          showBackground: true,
          stack: 'Total',
          type: 'bar',
        },
        {
          barWidth: 10,
          data:
            usageRate?.length > 0
              ? usageRate.map((i) => Number.parseFloat(`${i * 100}`).toFixed(2)).reverse()
              : right.map((i) => i * total), // XY轴翻转，需要翻转后端返回数据
          name: t('common.screen.perUsage'),
          showBackground: true,
          stack: 'Total',
          type: 'bar',
        },
      ],
      tooltip: {
        axisPointer: {
          type: 'none',
        },
        show: used?.length > 0,
        trigger: 'axis',
        valueFormatter: (value) => Math.abs(value),
      },
      xAxis: {
        axisLabel: {
          color: '#fff',
          formatter: (v) => {
            if (v < 0) {
              return Math.abs(v);
            } else if (v > 0) {
              return `${Number.parseFloat(v)}%`;
            } else {
              return 0;
            }
          },
          show: area?.length > 0,
        },
        axisLine: {
          show: true,
        },
        splitLine: {
          show: false,
        },
        type: 'value',
      },
      yAxis: [
        {
          axisLabel: {
            color: 'rgba(153, 179, 200, 1)',
            overflow: 'truncate',
            show: area?.length > 0,
            truncate: '...',
            width: 90,
          },
          axisLine: {
            lineStyle: {
              color: 'rgba(186, 231, 255, 1)',
              width: 1,
            },
            show: true,
          },
          axisTick: {
            show: false,
          },
          data: area.reverse(), // XY轴翻转，需要翻转后端返回数据
          type: 'category',
        },
      ],
    };
    myChart.setOption(option);
    echartsInstance.push(myChart);
  }

  // 设备
  async function equipmentChart() {
    const { code, data } = await queryCountEquipment(equipParams);
    const organizationNameList: string[] = [];
    const pttNumList: number[] = []; // 记录仪
    const cameraNumList: number[] = []; // 摄像头
    const pdtNumList: number[] = []; // PDT
    const deviceNumList: number[] = []; // 终端
    if (code === 0) {
      // console.log('设备统计-equipmentChart', data);
      data.forEach((element) => {
        organizationNameList.push(element.orgName);
        pdtNumList.push(element.pdtNum);
        deviceNumList.push(element.deviceNum);
        pttNumList.push(element.pttNum);
        cameraNumList.push(element.cameraNum);
      });
      // let maxVal = Math.max.apply(null, cameraNumList);
      // let minVal = Math.min.apply(null, cameraNumList);
      // maxVal = maxVal > 80 ? maxVal : 80;
      // let diffVal = maxVal - minVal;
      orgNum.value =
        cameraNumList.length > 0 ? cameraNumList.reduce((a: number, b: number) => a + b) : 0;
      const myChart = echarts.init(
        document.querySelector('#policeTermenalStatistics') as HTMLElement,
      );
      const option = {
        color: [
          new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { color: pttNumList.length > 0 ? 'rgba(30, 231, 231, 1)' : '#cecfd150', offset: 0 },
            { color: pttNumList.length > 0 ? 'rgba(30, 231, 231, 0.35)' : '#cecfd150', offset: 1 },
          ]),
          new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { color: cameraNumList.length > 0 ? 'rgba(24, 144, 255, 1)' : '#cecfd150', offset: 0 },
            {
              color: cameraNumList.length > 0 ? 'rgba(24, 144, 255, 0.35)' : '#cecfd150',
              offset: 1,
            },
          ]),
          new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { color: pdtNumList.length > 0 ? 'rgba(93, 218, 144, 1)' : '#cecfd150', offset: 0 },
            { color: pdtNumList.length > 0 ? 'rgba(93, 218, 144, 0.35)' : '#cecfd150', offset: 1 },
          ]),
          new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { color: deviceNumList.length > 0 ? 'rgba(249, 185, 58, 1)' : '#cecfd150', offset: 0 },
            {
              color: deviceNumList.length > 0 ? 'rgba(249, 185, 58, 0.35)' : '#cecfd150',
              offset: 1,
            },
          ]),
        ],
        dataZoom: [
          {
            backgroundColor: 'none',
            borderColor: 'transparent',
            bottom: 6,
            // rangeMode: [1, 12],
            // zoomLock: true,
            brushSelect: false,
            dataBackground: {
              areaStyle: {
                color: 'none',
              },
              lineStyle: {
                width: 0,
              },
            },
            endValue: 5,
            filterColor: 'none',
            handleStyle: {
              border: 'none',
              borderWidth: 0,
              color: 'none',
            },
            height: 4,
            left: '10%',
            selectedDataBackground: {
              areaStyle: {
                color: 'yellow',
              },
              lineStyle: {
                cap: 'round',
                color: 'rgb(50,153,227)',
                width: 7,
              },
            },
            show: true,
            showDetail: false,
            startValue: 0,
            // textStyle: {
            //   color: '#fff',
            // },
            type: 'slider',
            width: '86%',
          },
        ],
        // backgroundColor: 'rgba(6, 41, 74, 0.6)',
        grid: {
          bottom: 16,
          containLabel: true,
          left: 10,
          right: 16,
          top: '20%',
        },
        legend: {
          data: [
            t('videoControl.createDeployment.siteEnforcementRecorder'),
            t('homePage.navigateData.monitor'),
            'PDT',
            t('homePage.mapToolData.terminal'),
          ],
          itemHeight: 10,
          itemWidth: 10,
          show: organizationNameList.length > 0,
          textStyle: {
            color: '#fff',
            fontSize: 12,
          },
          top: '14px',
        },
        series: [
          {
            barGap: '50%',
            barWidth: 10,
            data: pttNumList.length > 0 ? pttNumList : [0, 0, 0, 0],
            name: t('videoControl.createDeployment.siteEnforcementRecorder'),
            tooltip: {
              valueFormatter: (value) => {
                return `${value} ${t('monitor.mapMonitor.unit')}`;
              },
            },
            type: 'bar',
          },
          {
            barWidth: 10,
            data: cameraNumList.length > 0 ? cameraNumList : [0, 0, 0, 0],
            name: t('homePage.navigateData.monitor'),
            tooltip: {
              valueFormatter: (value) => {
                return `${value} ${t('monitor.mapMonitor.unit')}`;
              },
            },
            type: 'bar',
          },
          {
            barWidth: 10,
            data: pdtNumList.length > 0 ? pdtNumList : [0, 0, 0, 0],
            name: 'PDT',
            tooltip: {
              valueFormatter: (value) => {
                return `${value} ${t('monitor.mapMonitor.unit')}`;
              },
            },
            type: 'bar',
          },
          {
            barWidth: 10,
            data: deviceNumList.length > 0 ? deviceNumList : [0, 0, 0, 0],
            name: t('homePage.mapToolData.terminal'),
            tooltip: {
              valueFormatter: (value) => {
                return `${value} ${t('monitor.mapMonitor.unit')}`;
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
              width: 80,
            },
            axisPointer: {
              type: 'shadow',
            },
            axisTick: {
              show: false,
            },
            data: organizationNameList.length > 0 ? organizationNameList : ['', '', '', ''],
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
            min: 0,
            nameTextStyle: {
              color: '#fff',
            },
            // max: roundUpToNearestTen(diffVal),
            // interval: Math.trunc(roundUpToNearestTen(diffVal) / 4),
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

  // 重点人员-记录仪抓拍统计
  async function getCountSnapshotAndKeyPerson() {
    const { code, data } = await queryCountSnapshotAndKeyPersonnel(params);
    if (code === 0) {
      const labelList: string[] = [];
      const faceCaptureNumList: number[] = [];
      const keyPersonnelNumList: number[] = [];
      data.statistics.forEach((element) => {
        labelList.push(element.orgName);
        faceCaptureNumList.push(element.faceCaptureNum);
        keyPersonnelNumList.push(element.keyPersonnelNum);
      });
      createBar('faceStatistic', labelList, faceCaptureNumList); // 记录仪人脸统计
      createBar('importantPerson', labelList, keyPersonnelNumList); // 重点人员统计
    }
  }

  // function roundUpToNearestTen(num) {
  //   let tens = Math.ceil(num / 10); // 向上取整获取十位数
  //   let roundedNum = tens * 10; // 十位数乘以10
  //   return roundedNum;
  // }

  // 无线归档使用统计
  async function organizationChart() {
    const { code, data } = await queryTerminalStatistics(equipParams);
    const organizationNameList: string[] = [];
    const deviceNumList: number[] = []; // 在线终端数
    const enabledCallbackNumList: number[] = []; // 已开通回传终端数
    const uploadedEvidenceNumList: number[] = []; // 已上传证据终端数
    if (code === 0) {
      // console.log('无线归档使用统计-organizationChart', data);
      data.forEach((element) => {
        organizationNameList.push(element.orgName);
        deviceNumList.push(element.onlineNum);
        enabledCallbackNumList.push(element.enabledCallbackNum);
        uploadedEvidenceNumList.push(element.uploadedEvidenceNum);
      });
    }
    // let maxVal = Math.max.apply(null, deviceNumList);
    // let minVal = Math.min.apply(null, deviceNumList);
    // maxVal = maxVal > 80 ? maxVal : 80;
    // let diffVal = maxVal - minVal;
    orgNum.value =
      deviceNumList.length > 0 ? deviceNumList.reduce((a: number, b: number) => a + b) : 0;
    const myChart = echarts.init(document.querySelector('#organization') as HTMLElement);
    const option = {
      color: [
        new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { color: deviceNumList.length > 0 ? 'rgba(30, 231, 231, 1)' : '#cecfd150', offset: 0 },
          { color: deviceNumList.length > 0 ? 'rgba(30, 231, 231, 0.35)' : '#cecfd150', offset: 1 },
        ]),
        new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          {
            color: enabledCallbackNumList.length > 0 ? 'rgba(24, 144, 255, 1)' : '#cecfd150',
            offset: 0,
          },
          {
            color: enabledCallbackNumList.length > 0 ? 'rgba(24, 144, 255, 0.35)' : '#cecfd150',
            offset: 1,
          },
        ]),
        new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          {
            color: uploadedEvidenceNumList.length > 0 ? 'rgba(249, 185, 58, 1)' : '#cecfd150',
            offset: 0,
          },
          {
            color: uploadedEvidenceNumList.length > 0 ? 'rgba(249, 185, 58, 0.35)' : '#cecfd150',
            offset: 1,
          },
        ]),
      ],
      dataZoom: [
        {
          backgroundColor: 'none',
          borderColor: 'transparent',
          bottom: 6,
          // rangeMode: [1, 12],
          // zoomLock: true,
          brushSelect: false,
          dataBackground: {
            areaStyle: {
              color: 'none',
            },
            lineStyle: {
              width: 0,
            },
          },
          endValue: 5,
          filterColor: 'none',
          handleStyle: {
            border: 'none',
            borderWidth: 0,
            color: 'none',
          },
          height: 4,
          left: '10%',
          selectedDataBackground: {
            areaStyle: {
              color: 'yellow',
            },
            lineStyle: {
              cap: 'round',
              color: 'rgb(50,153,227)',
              width: 7,
            },
          },
          show: true,
          showDetail: false,
          startValue: 0,
          // textStyle: {
          //   color: '#fff',
          // },
          type: 'slider',
          width: '86%',
        },
      ],
      // backgroundColor: 'rgba(6, 41, 74, 0.6)',
      grid: {
        bottom: 16,
        containLabel: true,
        left: 10,
        right: 16,
        top: '20%',
      },
      legend: {
        data: [
          t('common.screen.onlineTerminal'),
          t('common.screen.wirelessBackhaulTerminal'),
          t('common.screen.uploadedTerminal'),
        ],
        itemHeight: 10,
        itemWidth: 10,
        show: organizationNameList.length > 0,
        textStyle: {
          color: '#fff',
          fontSize: 12,
        },
        top: '14px',
      },
      series: [
        {
          barGap: '50%',
          barWidth: 10,
          data: deviceNumList.length > 0 ? deviceNumList : [0, 0, 0, 0],
          name: t('common.screen.onlineTerminal'),
          tooltip: {
            valueFormatter: (value) => {
              return `${value} 个`;
            },
          },
          type: 'bar',
        },
        {
          barWidth: 10,
          data: enabledCallbackNumList.length > 0 ? enabledCallbackNumList : [0, 0, 0, 0],
          name: t('common.screen.wirelessBackhaulTerminal'),
          tooltip: {
            valueFormatter: (value) => {
              return `${value} 个`;
            },
          },
          type: 'bar',
        },
        {
          barWidth: 10,
          data: uploadedEvidenceNumList.length > 0 ? uploadedEvidenceNumList : [0, 0, 0, 0],
          name: t('common.screen.uploadedTerminal'),
          tooltip: {
            valueFormatter: (value) => {
              return `${value} 个`;
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
            width: 80,
          },
          axisPointer: {
            type: 'shadow',
          },
          axisTick: {
            show: false,
          },
          data: organizationNameList.length > 0 ? organizationNameList : ['', '', '', ''],
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
          min: 0,
          nameTextStyle: {
            color: '#fff',
          },
          // max: roundUpToNearestTen(diffVal),
          // interval: Math.trunc(roundUpToNearestTen(diffVal) / 4),
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

  async function getSecondsDomainData() {
    // onlineNum 使用量,   count 配备量, useRate 使用率
    const { code, data } = await querySecondLevelDomain(equipParams);
    if (code === 0) {
      // console.log('二级域-getSecondsDomainData', data);
      secondsDomainList.value = data;
    }
  }

  function changeTime(data) {
    activeTime.value = data.id;
    cycle = data.dateValue;
    params = { ...adminCode, ...organizationList, ...cycle };
    initCharts();
  }
</script>

<template>
  <div class="screen-box">
    <div class="time">
      <span
        v-for="item in times"
        :key="item.id"
        :class="{ active: activeTime === item.id }"
        @click="changeTime(item)"
      >
        {{ item.name }}
      </span>
    </div>

    <div class="echarts-container">
      <div class="left-side">
        <div class="chart-item">
          <div class="title">{{ t('resource.plan.planList') }}</div>
          <div class="content">
            <div class="tree-content">
              <div class="organization 11">
                <div class="nav-link 22">
                  <template v-for="(item, index) in navLink" :key="item.id">
                    <div v-if="index <= 1 || index >= navLink.length - 2" class="link">
                      <span
                        :class="{ active: index < navLink.length - 1 }"
                        @click="handlePath(item, index)"
                      >
                        <span>{{ item.name }}</span>
                      </span>
                      <Icon
                        v-if="index < navLink.length - 1"
                        class="icon"
                        name="right_triangle_arrow"
                      />
                    </div>

                    <div v-if="index === 2 && navLink.length > 4" class="link omit">
                      <span class="active">...</span>
                      <Icon class="icon" name="right_triangle_arrow" />
                    </div>
                  </template>
                </div>
                <div class="sub-menu 33">
                  <div
                    v-for="item in currentOrgList"
                    :key="item.id"
                    class="menu-item"
                    @click="handleMenuClick(item)"
                  >
                    <TdTooltip :content="`${item.name}`">
                      <span>{{ `${item.name}` }}</span>
                    </TdTooltip>
                    <Icon class="icon" name="right_triangle_arrow" />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="chart-item">
          <div class="title">{{ t('common.screen.organizationTrafficStatistics') }}</div>
          <div class="content">
            <div id="organization-traffic" class="chart"> </div>
          </div>
        </div>
        <div class="chart-item police-statistic">
          <div class="title title-big">{{ t('common.screen.equipmentStatistics') }}</div>
          <div class="content">
            <div id="policeTermenalStatistics" class="chart"></div>
          </div>
        </div>
      </div>

      <div class="right-side">
        <div class="left">
          <div class="chart-item">
            <div class="title">{{ t('common.screen.alarmStatistics') }}</div>
            <div class="content police-info">
              <div class="info-item info-item-yellow all" style="justify-content: flex-start">
                <div class="alarm-item">
                  <img alt="" src="@/assets/images/screen/police_info_one.png" />
                  <div class="alarm-info">
                    <p v-if="alarmList.total">
                      <span>{{ alarmList.total }}</span>
                      <span>{{ t('common.screen.unit') }}</span>
                    </p>
                    <div v-else>-</div>
                    <span class="alarm-text">{{ t('common.screen.allAlarm') }}</span>
                  </div>
                </div>
              </div>
              <div class="info-item info-item-purple">
                <div class="alarm-item">
                  <img alt="" src="@/assets/images/screen/police_info_blue.png" />
                  <div class="alarm-info">
                    <p v-if="alarmList.faceAlarmNum">
                      <span>{{ alarmList.faceAlarmNum }}</span>
                      <span>{{ t('common.screen.unit') }}</span>
                    </p>
                    <div v-else>-</div>
                    <span class="alarm-text">{{ t('common.screen.faceAlarm') }}</span>
                  </div>
                </div>
              </div>
              <div class="info-item info-item-purple">
                <div class="alarm-item">
                  <img alt="" src="@/assets/images/screen/police_info_purple.png" />
                  <div class="alarm-info">
                    <p v-if="alarmList.carAlarmNum">
                      <span>{{ alarmList.carAlarmNum }}</span>
                      <span>{{ t('common.screen.unit') }}</span>
                    </p>
                    <div v-else>-</div>
                    <span class="alarm-text">{{ t('common.screen.plateAlarm') }}</span>
                  </div>
                </div>
              </div>
              <div class="info-item info-item-green">
                <div v-if="isShow" class="alarm-item">
                  <img alt="" src="@/assets/images/screen/police_info_green.png" />
                  <div class="alarm-info">
                    <p v-if="alarmList.fixedMonitoringAlertNum">
                      <span>{{ alarmList.fixedMonitoringAlertNum }}</span>
                      <span>{{ t('common.screen.unit') }}</span>
                    </p>
                    <div v-else>-</div>
                    <span class="alarm-text">{{ t('common.screen.monitorAlarm') }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="chart-item">
            <div class="title">{{ t('common.screen.personnelStatistics') }}</div>
            <div class="content organization">
              <div id="importantPerson" class="chart"></div>
            </div>
          </div>
          <div class="chart-item">
            <div class="title">{{ t('common.screen.recordFaceStatistics') }}</div>
            <div class="content faceStatistic">
              <div id="faceStatistic" class="chart"></div>
            </div>
          </div>
          <div class="chart-item evidence-chart">
            <div class="title">{{ t('common.screen.evidenceStatistics') }}</div>
            <div class="content evidence">
              <img alt="" class="evidence-img" src="@/assets/images/screen/evidence_img.png" />
              <div class="title-video">
                <div class="video-size">
                  {{ evidenceList.length > 0 ? evidenceList[0].sizeNum : '- GB' }}
                </div>
                <div class="video-name">{{ t('mission.missionInformation.video') }}</div>
              </div>
              <div v-for="(item, index) in evidenceList" :key="index" class="item">
                <span>{{ item.name }}</span>
                <span class="video-text">
                  {{ item.sizeNum }}{{ item.sizeNum ? '/' : '' }}{{ item.durationNum }}
                </span>
              </div>
            </div>
          </div>
          <div class="chart-item wiree-usages">
            <div class="title title-big">{{ t('common.screen.wifiUsagesStatistics') }}</div>
            <div class="content organization">
              <div id="organization" class="chart"></div>
            </div>
          </div>
        </div>
        <div class="right">
          <div class="title">{{ t('common.screen.organizationUsage') }}</div>
          <div class="content second-level">
            <div class="header">
              <span>{{ t('policeAdmin.track.organizationName') }}</span>
              <span>{{ t('common.screen.usage') }}</span>
              <span>{{ t('common.screen.equipped') }}</span>
              <span>{{ t('common.screen.perUsage') }}</span>
            </div>
            <div class="body">
              <div
                v-for="(item, index) in secondsDomainList.length > 0 ? secondsDomainList : 20"
                :key="index"
                class="item"
              >
                <span :title="`${secondsDomainList.length > 0 ? item.orgName : 0}`">
                  {{ secondsDomainList.length > 0 ? item.orgName : '-' }}
                </span>
                <span class="bar" :title="`${secondsDomainList.length > 0 ? item.onlineNum : 0}`">
                  <i class="second-title">{{ item.onlineNum }}</i>
                </span>
                <span class="bar" :title="`${item.count}`">
                  <i class="second-title">{{ item.count }}</i>
                </span>
                <span class="bar" :title="item.useRate">
                  <i class="second-title">
                    {{ item.useRate >= 0 ? `${parseFloat(item.useRate * 100).toFixed(2)}%` : '' }}
                  </i>
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .screen-box {
    position: relative;
    width: 100%;
    height: 100%;
    padding-top: 50px;
    overflow: hidden;
    background: url('@/assets/images/screen/screen_bg.png') no-repeat;
    background-size: 100% 100%;

    .time {
      position: absolute;
      top: 18px;
      left: 25px;
      z-index: 8888;
      display: flex;
      width: 57px;
      height: 20px;
      font-size: 12px;
      font-weight: 400;
      background: rgb(2 18 44 / 100%);
      border: 1px solid rgb(18 67 108 / 100%);

      span {
        display: flex;
        flex: 1;
        align-items: center;
        justify-content: center;
        cursor: pointer;

        &.active {
          background: rgb(23 140 247 / 100%);
        }
      }
    }
  }

  .echarts-container {
    display: flex;
    justify-content: space-between;
    width: 100%;
    height: 100%;

    .title {
      position: relative;
      width: 100%;
      height: 32px;
      padding-left: 28px;
      font-size: 16px;
      font-weight: 700;
      line-height: 32px;
      color: rgb(255 255 255 / 100%);
      letter-spacing: 0;
      background: url('@/assets/images/popup/frame_title_bg.png') no-repeat;
      background-size: 100% 100%;

      &-big {
        background: url('@/assets/images/popup/frame_title_bg_x2.png') no-repeat;
      }
    }

    .left-side {
      display: flex;
      flex-wrap: wrap;
      width: 40%;
      height: 100%;

      .chart-item {
        width: 48.6%;
        height: 31.2%;
        margin-right: 10px;
        margin-bottom: 16px;
        backdrop-filter: blur(8px);
        border: 1px solid transparent;
        border-image: linear-gradient(
          180deg,
          rgba(26 255 251 / 20%) 0%,
          rgba(26 255 251 / 60%) 100%
        );
        border-image-slice: 1;

        .content {
          width: 100%;
          height: calc(100% - 32px);
          background-color: rgb(6 41 74 / 60%);

          .chart {
            width: 100%;
            height: 100%;
          }

          .tree-content {
            flex: 1;
            height: 100%;
            padding: 6px 14px 0;

            .organization {
              height: 100%;

              .icon {
                width: 12px;
                height: 12px;
                margin: 0 4px;
                fill: rgb(153 206 251 / 100%);
              }

              .nav-link {
                display: flex;
                align-items: center;
                height: 30px;
                padding: 0 10px;
                margin-bottom: 6px;
                background-color: rgb(57 69 102 / 50%);

                .link {
                  display: flex;
                  align-items: center;

                  span {
                    font-size: 14px;
                    font-weight: 400;
                    cursor: pointer;
                  }

                  .active {
                    color: rgb(153 206 251 / 100%);
                  }
                }
              }

              .sub-menu {
                width: 100%;
                height: calc(100% - 42px);
                overflow-y: scroll;

                .menu-item {
                  display: flex;
                  align-items: center;
                  justify-content: space-between;
                  height: 24px;
                  margin: 0 0 0 18px;
                  cursor: pointer;

                  span {
                    font-size: 14px;
                    font-weight: 400;
                  }
                }
              }
            }
          }
        }

        .police-info {
          display: flex;
          flex-wrap: wrap;

          .gradient-color {
            top: 209px;
            left: 498px;
            width: 2px;
            height: 72px;
            background: radial-gradient(
              closest-side at 50% 50%,
              rgb(21 154 255 / 100%) 0%,
              rgb(21 154 255 / 0%) 100%
            );
            opacity: 0.5;
          }

          .info-equip {
            display: flex;
            width: 49%;
            height: 4.75rem;
            padding-top: 0.875rem;
            padding-left: 22px;

            .alarm-item {
              display: flex;
              align-items: center;
              font-size: 12px;

              .alarm-info {
                display: flex;
                flex-direction: column;
                color: #fff;
              }
            }

            &.all {
              p {
                position: relative;
                margin-bottom: 8px;

                span:nth-of-type(1) {
                  font-size: 28px;
                }
              }
            }

            &:nth-of-type(4),
            &:nth-of-type(8) {
              margin-right: 0;
            }

            p {
              display: flex;
              align-items: flex-end;
              justify-content: center;
              width: 100%;

              span:nth-of-type(1) {
                margin-right: 2px;
                font-size: 24px;
                font-weight: 400;
                line-height: 28px;
              }

              span:nth-of-type(2) {
                font-size: 12px;
                font-weight: 400;
              }
            }

            img {
              width: 67px;
              height: 41px;
              margin: -0 0 2px;
            }

            img.equipmentImg {
              width: 64px;
              height: 64px;
              margin: -0 0 2px;
            }

            & > span {
              display: block;
              font-size: 12px;
              font-weight: 400;
              text-align: center;
            }
          }

          .info-item {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            width: 49%;
            height: 4.75rem;
            padding-top: 0.875rem;

            .alarm-item {
              display: flex;
              align-items: center;
              font-size: 12px;

              .alarm-info {
                display: flex;
                flex-direction: column;
                color: #fff;
              }
            }

            &.all {
              p {
                position: relative;
                margin-bottom: 8px;

                span:nth-of-type(1) {
                  font-size: 28px;
                }
              }
            }

            &:nth-of-type(4),
            &:nth-of-type(8) {
              margin-right: 0;
            }

            p {
              display: flex;
              align-items: flex-end;
              justify-content: center;
              width: 100%;

              span:nth-of-type(1) {
                margin-right: 2px;
                font-size: 24px;
                font-weight: 400;
                line-height: 28px;
              }

              span:nth-of-type(2) {
                font-size: 12px;
                font-weight: 400;
              }
            }

            img {
              width: 67px;
              height: 41px;
              margin: -0 0 2px;
            }

            img.equipmentImg {
              width: 64px;
              height: 64px;
              margin: -0 0 2px;
            }

            & > span {
              display: block;
              font-size: 12px;
              font-weight: 400;
              text-align: center;
            }
          }
        }

        .equipment {
          display: flex;
          flex-wrap: wrap;

          .chart {
            width: 50%;
            height: 50%;
          }
        }
      }

      .police-statistic {
        width: 100%;
        height: 64.6%;
      }

      .capture {
        display: flex;
        justify-content: space-between;

        .capture-item {
          display: flex;
          flex-direction: column;
          align-items: center;

          span {
            color: rgb(4 255 246 / 100%);
          }

          p {
            span:nth-of-type(1) {
              font-size: 28px;
              font-weight: 400;
            }

            span:nth-of-type(2) {
              font-size: 12px;
              font-weight: 400;
            }
          }

          & > span {
            font-size: 12px;
            font-weight: 400;
          }
        }
      }
    }

    .right-side {
      display: flex;
      width: 60%;
      height: 100%;
      margin-right: 30px;

      .left {
        display: flex;
        flex-wrap: wrap;
        justify-content: space-between;
        width: 67%;

        .police-info {
          display: flex;
          flex-wrap: wrap;
          align-items: center;
          justify-content: center;
          padding: 16px;

          .info-item {
            display: flex;
            flex-direction: row;
            align-items: center;
            width: 50%;
            height: 4.75rem;
            padding-top: 0.875rem;

            .alarm-item {
              display: flex;
              align-items: center;

              .alarm-info {
                display: flex;
                flex-direction: column;

                .alarm-text {
                  font-size: 12px;
                  font-weight: 400;
                  line-height: 20px;
                  color: rgb(120 168 222 / 100%);
                  letter-spacing: 0;
                }
              }
            }

            &.all {
              p {
                position: relative;
                margin-bottom: 8px;

                span:nth-of-type(1) {
                  font-size: 28px;
                }
              }
            }

            &:nth-of-type(4),
            &:nth-of-type(8) {
              margin-right: 0;
            }

            p {
              display: flex;
              align-items: flex-end;
              justify-content: center;
              width: 100%;

              span:nth-of-type(1) {
                margin-right: 2px;
                font-size: 24px;
                font-weight: 400;
                line-height: 28px;
              }

              span:nth-of-type(2) {
                font-size: 12px;
                font-weight: 400;
              }
            }

            img {
              width: 68px;
              margin: 0 0 2px;
            }

            img.equipmentImg {
              width: 64px;
              height: 64px;
              margin: -0 0 2px;
            }

            & > span {
              display: block;
              font-size: 12px;
              font-weight: 400;
              text-align: center;
            }
          }
        }

        .chart-item {
          width: 48.6%;
          height: 31%;
          margin-right: 10px;
          margin-bottom: 16px;
          backdrop-filter: blur(8px);
          border: 1px solid transparent;
          border-image: linear-gradient(
            180deg,
            rgba(26 255 251 / 20%) 0%,
            rgba(26 255 251 / 60%) 100%
          );
          border-image-slice: 1;

          .content {
            width: 100%;
            height: calc(100% - 2.25rem);
            background-color: rgb(6 41 74 / 60%);
          }

          .evidence {
            position: relative;
            padding-top: 24px;

            .title-video {
              position: absolute;
              top: 32px;
              right: 136px;
              display: flex;
              flex-direction: column;
              align-items: center;
              justify-content: center;
              min-width: 85px;

              .video-size {
                height: 32px;
                font-size: 24px;
                font-weight: bold;
              }

              .video-name {
                font-size: 12px;
              }
            }

            .video-text {
              margin-right: 15px;
              font-size: 18px;
              font-weight: bold;
              color: rgb(208 222 238 / 100%);
              letter-spacing: 0.95px;
            }
          }

          &:nth-last-child {
            margin-bottom: 0;
          }

          #organization-traffic {
            width: 100%;
            height: 204px;
          }
        }

        .wiree-usages {
          width: 98.6%;
        }

        .evidence-chart {
          flex-grow: 0;
        }
      }

      .right {
        width: 33%;
        height: 98%;
        background-color: rgb(6 41 74 / 60%);
        backdrop-filter: blur(8px);
        border: 1px solid transparent;
        border-image: linear-gradient(
          180deg,
          rgba(26 255 251 / 20%) 0%,
          rgba(26 255 251 / 60%) 100%
        );
        border-image-slice: 1;

        .content {
          height: calc(100% - 32px);
          padding: 10px;
        }
      }

      .evidence {
        .evidence-img {
          width: 180px;
          height: 127px;
          margin: 0 auto;
        }

        .chart {
          width: 100%;
          height: 114px;
          margin-bottom: 10px;
        }

        .item {
          position: relative;
          display: flex;
          align-items: center;
          justify-content: space-between;
          height: 30px;
          padding: 0 9px 0 27px;
          margin: 8px 20px;
          background: url('@/assets/images/screen/chart_content_bg.png') no-repeat;
          background-size: 100% 100%;

          &::before {
            position: absolute;
            top: 13px;
            left: 13px;
            width: 2.37px;
            height: 2.5px;
            content: '';
            background: rgb(108 128 151 / 100%);
            border-radius: 0.0625rem;
          }

          &::after {
            position: absolute;
            left: 91px;
            width: 11px;
            height: 1px;
            content: '';
            background: rgb(102 225 223 / 100%);
          }

          span:nth-of-type(1) {
            font-size: 12px;
            font-weight: 500;
          }

          span:nth-of-type(2) {
            font-size: 16px;
            font-weight: 400;
          }
        }
      }

      .terminal-use {
        display: flex;
        flex: 1;
        align-items: center;
        justify-content: center;

        .chart {
          width: 33.3%;
          height: 100%;
        }
      }

      .faceStatistic {
        display: flex;
        flex: 1;
        align-items: center;
        justify-content: center;

        .chart {
          width: 100%;
          height: 100%;
        }
      }

      .organization,
      .terminal-use {
        display: flex;
        flex: 1;
        align-items: center;
        justify-content: center;

        .chart {
          width: 100%;
          height: 100%;
        }
      }

      .organizer {
        display: flex;
        flex: 1;
        align-items: center;
        justify-content: center;
        width: 100%;

        .chart {
          width: 33.3%;
          height: 130px;
        }
      }

      .second-level {
        .header {
          display: flex;
          justify-content: space-between;
          margin-bottom: 10px;

          span {
            flex: 1;
            text-align: left;

            &:nth-of-type(1) {
              text-align: left;
            }
          }
        }

        .body {
          height: calc(100% - 34px);
          overflow-y: scroll;

          .item {
            display: flex;
            justify-content: space-between;
            margin-bottom: 10px;

            span {
              flex: 1;
              overflow: hidden;
              font-size: 14px;
              font-weight: 400;
              line-height: 20.27px;
              color: rgb(153 206 251 / 100%);
              text-overflow: ellipsis;
              letter-spacing: 0;
              white-space: nowrap;
            }

            .bar {
              position: relative;

              .second-title {
                position: absolute;
                top: 0;
                left: 16px;
              }

              span {
                display: block;
                height: 100%;
                background: linear-gradient(
                  270deg,
                  rgb(24 144 255 / 100%) 0%,
                  rgb(30 231 231 / 100%) 100%
                );
              }
            }
          }
        }
      }
    }
  }
</style>
