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
    const echartDataNumArr = props.echartData.map((item) => item.value);
    let min = 0,
      max = 100;
    if (echartDataNumArr.length > 0) {
      min = Math.min(...echartDataNumArr);
      max = Math.max(...echartDataNumArr);
    }
    return {
      tooltip: {
        trigger: 'item',
        formatter: '{b} : {c} ({d}%)', //{a} <br/>{b} : {c}%
      },
      series: [
        {
          name: '转化率',
          type: 'funnel',
          left: '20%',
          top: 0,
          bottom: 0,
          min: min,
          max: max,
          minSize: '5%',
          maxSize: '100%',
          sort: 'descending',
          gap: 0,
          height: '100%',
          label: {
            show: true,
            position: 'left',
            formatter: '{d}%',
            color: '#333333',
            fontWeight: '400',
            fontSize: '12px',
          },
          labelLine: {
            length: 10,
            lineStyle: {
              width: 1,
              type: 'solid',
            },
          },
          itemStyle: {
            borderColor: '#fff',
            borderWidth: 2,
            borderRadius: 5,
          },
          emphasis: {
            label: {
              fontSize: 16,
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
  }
</style>
