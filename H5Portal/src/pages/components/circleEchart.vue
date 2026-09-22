<template>
  <view ref="chartRef" :style="`width:${width};height:${height};`">
    <!-- <l-echart ref="chartRef"></l-echart> -->
  </view>
</template>

<script setup>
  import { ref, onMounted, nextTick, watch, onBeforeUnmount } from 'vue';

  import echarts from '@/utils/echarts.js';
  const props = defineProps(['width', 'height', 'echartData', 'padAngle']);
  //图表1
  const chartRef = ref(null);
  const myChart = ref(null);
  const getOption = () => {
    const data = props.echartData
      .filter((item) => item.name !== '全部')
      .filter((item) => item.value !== 0);
    const option = {
      // 1. 不要图例
      legend: {
        show: false,
      },

      // 2. 不要 tooltip（直接不写此配置项即可，或设置为 show: false）
      // tooltip: { show: false }, // 可写可不写，推荐直接不写

      // 3. 环形图主体
      series: [
        {
          name: '案件状态',
          type: 'pie',
          // 环形图内外半径
          radius: ['50%', '90%'],

          // 4. 不要显示每个扇形上的文字
          label: {
            show: false,
          },
          padAngle: data.length <= 1 ? 0 : +props.padAngle,
          // 5. 不要显示扇形连接线
          labelLine: {
            show: false,
          },

          // 6. 🚫 去掉点击/悬浮高亮变形效果：不设置 emphasis 或设置为空
          // 方法1：直接不写 emphasis 配置（推荐）
          // 方法2：写 emphasis 但让它不生效（如下，全部隐藏）
          emphasis: {
            disabled: true, // ECharts 5.0+ 支持，直接禁用高亮
            // 或者传统方式：让强调态不显示任何变化
            // label: { show: false },
            // itemStyle: { opacity: 1 },
            // scale: false
          },

          // 7. 数据
          data: data,

          // 8. ✅ 环形图中心显示文字（关键！）
          graphic: {
            type: 'text',
            left: 'center',
            top: 'center',
            style: {
              // 你可以自定义要显示的内容，比如：
              text: '已完成\n4/16\n(25%)', // 推荐

              textAlign: 'center',
              fill: '#333', // 文字颜色
              fontSize: 12, // 字体大小，可根据容器微调
              fontWeight: 'bold',
              lineHeight: 16, // 行高，避免文字重叠
            },
          },
        },
      ],
    };
    return option;
  };
  // 处理窗口尺寸变化、或页面重新可见时调用
  const handleResize = () => {
    if (myChart.value) {
      myChart.value.resize(); // 🟢 关键！重新调整图表尺寸
    }
  };
  const renderChart = async () => {
    if (!chartRef.value) return;
    // 数据为空时，清空图表
    const data = props.echartData
      .filter((item) => item.name !== '全部')
      .filter((item) => item.value !== 0);
    if (data.length <= 0) {
      if (myChart.value) {
        myChart.value.clear();
      }
      return;
    }
    if (!myChart.value) {
      myChart.value = echarts.init(chartRef.value);
    }
    const option = getOption();
    myChart.value.setOption(option, true);
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
      window.addEventListener('resize', handleResize);
    });
  });
  onBeforeUnmount(() => {
    // 页面卸载时销毁图表实例，防止内存泄漏
    if (myChart.value) {
      myChart.value.dispose();
      myChart.value = null;
    }
    window.removeEventListener('resize', handleResize);
  });
</script>

<style lang="scss" scoped></style>
