<template>
  <view>
    <view>
      <view class="chart-title">{{ title }}</view>
      <view ref="chartRef" :style="`height:${height};`">
        <!-- <l-echart ref="chartRef"></l-echart> -->
      </view>
    </view>
  </view>
</template>

<script setup>
  import { ref, onMounted, nextTick, watch } from 'vue';

  import echarts from '@/utils/echarts.js';
  const props = defineProps(['title', 'height', 'echartData']);
  const chartRef = ref(null);
  const getOption = () => {
    return {
      grid: {
        left: 0,
        right: 10,
        top: 10,
        bottom: 0,
        containLabel: true,
      },
      // tooltip: {
      //   trigger: 'axis',
      //   label: {
      //     show: true
      //   }
      // },
      tooltip: {
        trigger: 'axis',
        formatter: '{b} : {c}', //{a} <br/>{b} : {c}%
      },
      xAxis: {
        axisLine: {
          show: true, //是否显示X轴轴线
        },
        splitLine: {
          show: false, //是否显示X轴方向上的分隔线
        },
        axisLabel: {
          //设置X轴的坐标标签样式
          textStyle: {
            color: '#757790',
          },
        },
        axisTick: {
          show: true,
          alignWithLabel: true, //坐标刻度与标签对齐
          lineStyle: {
            //设置X轴刻度样式
            width: 5,
            color: '#757790',
          },
        },
        data: props.echartData.map((item) => item.name),
      },
      yAxis: {
        axisLabel: {
          //不展示坐标标签为0的
          formatter: function (value) {
            if (Number(value) === 0) {
              return '';
            } else {
              return value;
            }
          },
          textStyle: {
            padding: [0, 0, -25, 8], //通过padding设置来实现坐标轴标签在轴线内
            color: '#757790',
            align: 'left',
          },
        },
        axisLine: {
          show: false, //不展示Y轴轴线
        },
        splitLine: {
          show: true,
          lineStyle: {
            //以虚线形式来展示Y轴刻度方向上的分隔线
            type: 'dashed',
            color: '#E5E5E5',
          },
        },
        axisTick: {
          show: false, //不显示Y轴刻度
        },
      },
      series: [
        {
          smooth: false, //开启平滑处理
          symbol: 'circle', // 显示拐点
          symbolSize: 6, // 拐点大小
          type: 'line',
          lineStyle: {
            color: '#2663FF',
            width: 1,
          },
          itemStyle: {
            //设置折线样式
            color: '#3770F5',
            borderWidth: 1,
            borderColor: '#3770F5',
          },
          areaStyle: {
            //设置面积的线性渐变颜色
            normal: {
              color: new echarts.graphic.LinearGradient(
                0,
                0,
                0,
                1,
                [
                  {
                    offset: 0,
                    color: 'rgba(38, 99, 255, 0.6)',
                  },
                  {
                    offset: 1,
                    color: 'rgba(38, 99, 255, 0)',
                  },
                ],
                false,
              ),
            },
          },
          data: props.echartData,
        },
      ],
    };
  };
  let myChart = null;
  const renderChart = async () => {
    if (!chartRef.value) return;
    // 数据为空时，清空图表
    if (props.echartData.length <= 0) {
      if (myChart) {
        myChart.clear();
      }
      return;
    }
    await new Promise((resolve) => setTimeout(resolve, 100)); // 延迟 100ms
    // 复用已有实例
    if (!myChart) {
      myChart = echarts.init(chartRef.value);
    }
    const option = getOption();
    myChart.setOption(option, true);
  };
  watch(
    () => props.echartData,
    () => {
      renderChart();
    },
  );
  onMounted(() => {
    nextTick(() => {
      renderChart();
    });
  });
</script>

<style lang="scss" scoped>
  .chart-title {
    font-size: 16px;
    font-weight: 500;
    line-height: 32px;
    color: rgba(51, 51, 51, 1);
    margin-bottom: 10px;
  }
</style>
