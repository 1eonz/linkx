<script setup lang="ts">
  import type { ECharts, EChartsOption } from 'echarts';

  import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';

  import * as echarts from 'echarts';

  let resizeObserver: ResizeObserver | null = null;
  let rafId: number | null = null;

  const props = defineProps({
    height: {
      default: '100%',
      type: String,
    },
    options: {
      required: true,
      type: Object as () => EChartsOption,
    },
    width: {
      default: '100%',
      type: String,
    },
  });

  const chartRef = ref<HTMLElement>();
  // 定义 chart 变量以存储 ECharts 实例
  let chart: ECharts | null = null;
  // 初始化图表
  const initChart = () => {
    if (!chartRef.value) return;
    chart = echarts.init(chartRef.value);
    chart.setOption(props.options);
  };
  // 调整图表大小（用 RAF 合并频繁 resize）
  const resizeChart = () => {
    if (rafId != null) cancelAnimationFrame(rafId);
    rafId = requestAnimationFrame(() => {
      chart?.resize();
    });
  };

  onMounted(() => {
    nextTick(() => {
      // 初始化 ECharts
      initChart();
      window.addEventListener('resize', resizeChart);

      // 监听容器尺寸变化（比 window.resize 更可靠）
      if (typeof ResizeObserver !== 'undefined' && chartRef.value) {
        resizeObserver = new ResizeObserver(() => {
          resizeChart();
        });
        resizeObserver.observe(chartRef.value);
      }

      // 初次强制同步一次尺寸
      resizeChart();
    });
  });

  onBeforeUnmount(() => {
    window.removeEventListener('resize', resizeChart);

    if (resizeObserver) {
      resizeObserver.disconnect();
      resizeObserver = null;
    }

    if (rafId != null) {
      cancelAnimationFrame(rafId);
      rafId = null;
    }

    chart?.dispose();
  });
  // 监听配置项的变化，更新图表
  watch(
    () => props.options,
    (newVal) => {
      chart?.setOption(newVal);
    },
    { deep: true },
  );
</script>

<template>
  <div ref="chartRef" :style="{ width, height }"></div>
</template>
