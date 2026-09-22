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
  import { ref, computed, onMounted, nextTick, watch } from 'vue';

  import echarts from '@/utils/echarts.js';
  const props = defineProps(['title', 'height', 'echartData']);
  //图表1
  const total = computed(() => {
    return props.echartData.reduce((sum, item) => sum + item.value, 0);
  });
  const chartRef = ref(null);

  const getOption = () => {
    const total = props.echartData.reduce((sum, item) => sum + item.value, 0);
    // 为每个数据项添加占比信息（也可以直接在 formatter 里计算，这里提前处理更清晰）
    const dataWithPercent = props.echartData.map((item) => ({
      ...item,
      percent: ((item.value / (total > 0 ? total : 1)) * 100).toFixed(2), // 保留一位小数
    }));
    return {
      series: [
        {
          type: 'pie',
          radius: ['40%', '70%'], // 环形图：内半径40%，外半径70%
          center: ['50%', '50%'],
          data: dataWithPercent, // 使用带 percent 的数据
          label: {
            show: true,
            position: 'outside', // 标签在扇区外侧
            formatter: function (params) {
              // params.name：名称
              // params.value：数值
              // params.percent：注意：ECharts 默认有 percent 字段，但这里我们自己计算了更精确的
              const percent =
                params.percent !== undefined
                  ? params.percent.toFixed(1)
                  : params.data.percent !== undefined
                    ? params.data.percent
                    : '0.0';
              // 推荐使用我们自己计算的 data.percent，所以直接取：
              const displayPercent = params.data.percent ? params.data.percent : '0.0';
              return `${params.name}\n${displayPercent}%`; //`${params.name}\n${params.value}\n${displayPercent}%`;
            },
            lineHeight: 16, // 行高，控制三行间距
            fontSize: 12,
            color: '#333',
          },
          labelLine: {
            show: true,
            length: 10, // 第一段引导线长度
            length2: 15, // 第二段引导线长度
          },
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)',
            },
            label: {
              fontSize: 14,
              fontWeight: 'bold',
            },
          },
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
    await new Promise((resolve) => setTimeout(resolve, 200)); // 延迟 200ms
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
