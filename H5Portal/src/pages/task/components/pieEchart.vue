<template>
  <view>
    <view>
      <view class="chart-title">{{ title }}</view>
      <view :style="`height:${height};position: relative;`">
        <!-- <l-echart ref="chartRef"></l-echart> -->
        <view ref="chartRef" :style="`height:${height};`"></view>
        <!-- 手写中心标签 -->
        <view class="center-label">
          <text class="number-text">{{ newTotal }}</text>
          <text class="total-text">总数</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
  import { ref, computed, onMounted, nextTick, watch } from 'vue';

  import echarts from '@/utils/echarts.js';
  const props = defineProps(['title', 'height', 'echartData', 'totalNum']); //totalNum
  //图表1
  const total = computed(() => {
    return props.echartData.reduce((sum, item) => sum + item.value, 0);
  });
  const newTotal = ref(0);
  const chartRef = ref(null);

  const getOption = () => {
    const t = total.value; // 当前所有数据的总和（后面会动态更新）
    newTotal.value = total.value;
    return {
      tooltip: {
        show: false,
        trigger: 'item',
        formatter: '{b}: {c} ({d}%)',
      },
      legend: {
        top: 'center',
        left: 0,
        bottom: 0,
        orient: 'vertical',
        itemGap: 8,
        itemHeight: 18,
        formatter: function (name) {
          const item = props.echartData.find((d) => d.name === name);
          if (item) {
            const percent = ((item.value / (t > 0 ? t : 1)) * 100).toFixed(2);
            return `${name}: ${percent}%`;
          }
          return name;
        },
      },
      grid: {
        left: '30%',
        right: '5%',
        top: 80,
        bottom: 80,
      },
      series: [
        {
          type: 'pie',
          radius: ['60%', '85%'],
          center: ['70%', '50%'],
          avoidLabelOverlap: true,
          label: {
            show: false,
            position: 'center',
            formatter: '{total|' + newTotal.value + '}\n{label|\n总数}',
            rich: {
              total: {
                fontSize: 32,
                fontWeight: '700',
                color: 'rgba(51, 51, 51, 1)',
              },
              label: {
                fontSize: 16,
                fontWeight: '400',
                color: 'rgba(51, 51, 51, 1)',
              },
            },
          },
          emphasis: {
            label: {
              show: false,
              fontSize: 18,
              fontWeight: 'bold',
            },
          },
          labelLine: {
            show: false,
          },
          data: props.echartData,
        },
      ],
      graphic: [
        {
          type: 'text',
          left: '65%',
          top: '55%', // 微调位置，让数字和文案视觉居中
          style: {
            // 文案和数字分两行显示
            text: ``, // 第一行："总数"，第二行：数字
            textAlign: 'center',
            fill: 'rgba(51, 51, 51, 1)',
            fontSize: 16, // 文案（总数）的字体大小
            fontWeight: 400, // 文案不加粗
          },
          z: 1,
        },
        {
          type: 'text',
          left: t < 10 ? '68%' : t < 100 ? '65%' : '63%',
          top: '36%', // 数字放在下方，更大更突出
          style: {
            text: '', // 只显示数字，比如 90
            textAlign: 'center',
            fill: 'rgba(51, 51, 51, 1)', // 数字颜色可以不同
            fontSize: 32, // 数字字体大一些
            fontWeight: 'bold', // 加粗
          },
          z: 1, // 确保数字在文案之上
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
        newTotal.value = 0;
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
    // ✅ 监听图例选中状态变化
    myChart.on('legendselectchanged', function (params) {
      // params.selected: { '测试1': true, '测试2': false, ... }
      const visibleData = props.echartData.filter((item) => params.selected[item.name]);
      newTotal.value = visibleData.reduce((sum, item) => sum + (item.value || 0), 0);
      // ✅ 只更新 graphic 中的文字，动态显示新的总数
      myChart.setOption({
        series: [
          {
            type: 'pie',
            radius: ['60%', '85%'],
            center: ['70%', '50%'],
            avoidLabelOverlap: true,
            label: {
              show: false,
              position: 'center',
              formatter: '{total|' + newTotal.value + '}\n{label|\n总数}',
              rich: {
                total: {
                  fontSize: 32,
                  fontWeight: '700',
                  color: 'rgba(51, 51, 51, 1)',
                },
                label: {
                  fontSize: 16,
                  fontWeight: '400',
                  color: 'rgba(51, 51, 51, 1)',
                },
              },
            },
            emphasis: {
              label: {
                show: false,
                fontSize: 18,
                fontWeight: 'bold',
              },
            },
            labelLine: {
              show: false,
            },
            data: props.echartData,
          },
        ],
        graphic: [
          {
            type: 'text',
            left: '65%',
            top: '55%', // 微调位置，让数字和文案视觉居中
            style: {
              // 文案和数字分两行显示
              text: '', // 第一行："总数"，第二行：数字
              textAlign: 'center',
              fill: 'rgba(51, 51, 51, 1)',
              fontSize: 16, // 文案（总数）的字体大小
              fontWeight: 400, // 文案不加粗
            },
            z: 1,
          },
          {
            type: 'text',
            left: newTotal.value < 10 ? '68%' : newTotal.value < 100 ? '65%' : '63%',
            top: '36%', // 数字放在下方，更大更突出
            style: {
              text: '', // 只显示数字，比如 90
              textAlign: 'center',
              fill: 'rgba(51, 51, 51, 1)', // 数字颜色可以不同
              fontSize: 32, // 数字字体大一些
              fontWeight: 'bold', // 加粗
            },
            z: 1, // 确保数字在文案之上
          },
        ],
      });
    });
  };

  watch(
    () => props.echartData,
    () => {
      renderChart();
    },
    { deep: true },
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

  .center-label {
    position: absolute;
    left: 70%;
    top: 50%;
    transform: translate(-50%, -50%);
    z-index: 10;
    text-align: center;
    pointer-events: none;

    .total-text {
      display: block;
      font-size: 16px;
      font-weight: 400;
      color: rgba(51, 51, 51, 1);
      text-align: center;
    }

    .number-text {
      display: block;
      font-size: 32px;
      font-weight: 700;
      line-height: 32px;
      color: rgba(51, 51, 51, 1);
      text-align: center;
    }
  }
</style>
