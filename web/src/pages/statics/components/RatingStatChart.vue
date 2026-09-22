<script setup lang="ts">
  import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue';
  import type { EChartsOption } from 'echarts';
  import BaseChart from './Charts/BaseChart.vue';
  import SectionHeader from './SectionHeader.vue';
  import { useStaticsState } from '@/store';
  import { themeService } from '@/data/useTheme';

  interface Props {
    title: string;
    apiFn: (params: any) => Promise<any>;
  }

  const props = defineProps<Props>();

  const StaticsStore = useStaticsState();

  // 评分类型选项
  const ratingTypeOptions = [
    { value: 1 as const, label: '直接评分' },
    { value: 2 as const, label: '综合评分' },
  ];

  // 评分类型：1-直接评分，2-综合评分（成员评分）
  const ratingType = ref<1 | 2>(1);
  // 图表数据
  const chartData = ref<{
    xAxis: string[];
    legend: string[];
    series: { name: string; data: number[] }[];
  } | null>(null);
  const loading = ref(false);

  // 容器尺寸监听
  const cardRef = ref<any>(null);
  const containerWidth = ref(600);
  const containerHeight = ref(300);
  let resizeObserver: ResizeObserver | null = null;

  const getCardEl = () => {
    if (!cardRef.value) return null;
    return cardRef.value.$el || cardRef.value;
  };

  const updateContainerSize = () => {
    const el = getCardEl();
    if (el) {
      containerWidth.value = el.clientWidth;
      containerHeight.value = el.clientHeight;
    }
  };

  onMounted(() => {
    const el = getCardEl();
    if (el) {
      updateContainerSize();
      resizeObserver = new ResizeObserver(() => {
        updateContainerSize();
      });
      resizeObserver.observe(el);
    }
    currentTheme.value = themeService.getCurrentTheme();
  });

  onBeforeUnmount(() => {
    if (resizeObserver) {
      resizeObserver.disconnect();
      resizeObserver = null;
    }
  });

  // 主题
  const currentTheme = ref('light');
  themeService.onThemeChange((theme) => {
    currentTheme.value = theme;
  });

  // 折线颜色
  const lineColors = ['#5470C6', '#91CC75', '#FAC858', '#EE6666'];

  // 根据容器尺寸判断是否为紧凑模式
  const isCompact = computed(() => containerWidth.value < 400 || containerHeight.value < 250);
  const isVeryCompact = computed(() => containerWidth.value < 300 || containerHeight.value < 200);

  // ECharts配置
  const chartOptions = computed<EChartsOption>(() => {
    if (!chartData.value) return {};

    const axisLabelColor = currentTheme.value === 'light' ? '#666' : '#FFFFFF';
    const axisLineColor = currentTheme.value === 'light' ? '#E5E7EB' : 'rgba(3, 47, 89, 1)';
    const splitLineColor = currentTheme.value === 'light' ? '#eee' : 'rgba(3, 47, 89, 1)';
    const tooltipBg = currentTheme.value === 'light' ? '#fff' : 'rgba(50, 50, 50, 0.8)';
    const tooltipTextColor = currentTheme.value === 'light' ? '#666' : '#fff';

    // 根据尺寸动态调整
    const showLegend = !isVeryCompact.value;
    const legendFontSize = isCompact.value ? 9 : 11;
    const axisFontSize = isCompact.value ? 9 : 11;
    const legendTop = isVeryCompact.value ? '0%' : '5%';
    const gridTop = isVeryCompact.value ? '5%' : isCompact.value ? '22%' : '20%';
    const gridBottom = isCompact.value ? '2%' : '5%';
    const gridLeft = isCompact.value ? '1%' : '3%';
    const gridRight = isCompact.value ? '2%' : '4%';

    return {
      tooltip: {
        trigger: 'axis',
        confine: true,
        backgroundColor: tooltipBg,
        textStyle: {
          color: tooltipTextColor,
          fontSize: 12,
        },
      },
      legend: {
        show: showLegend,
        data: chartData.value.legend,
        top: legendTop,
        textStyle: {
          color: axisLabelColor,
          fontSize: legendFontSize,
        },
        itemWidth: isCompact.value ? 10 : 25,
        itemHeight: isCompact.value ? 8 : 14,
      },
      grid: {
        left: gridLeft,
        right: gridRight,
        bottom: gridBottom,
        top: gridTop,
        containLabel: true,
      },
      xAxis: {
        type: 'category',
        boundaryGap: true,
        data: chartData.value.xAxis,
        axisLabel: {
          color: axisLabelColor,
          fontSize: axisFontSize,
          interval: 0,
          rotate: 30,
          formatter: (value: string) => value.length > 6 ? value.substring(0, 6) + '...' : value,
        },
        axisLine: {
          lineStyle: {
            color: axisLineColor,
          },
        },
      },
      yAxis: {
        type: 'value',
        axisLabel: {
          color: axisLabelColor,
          fontSize: axisFontSize,
        },
        axisLine: {
          lineStyle: {
            color: axisLineColor,
          },
          show: true,
        },
        splitLine: {
          show: true,
          lineStyle: {
            type: 'dashed',
            color: splitLineColor,
            width: 1,
          },
        },
      },
      series: chartData.value.series.map((item, index) => ({
        name: item.name,
        type: 'line',
        data: item.data,
        lineStyle: {
          color: lineColors[index % lineColors.length],
          width: 2,
        },
        itemStyle: {
          color: lineColors[index % lineColors.length],
        },
        emphasis: {
          itemStyle: {
            borderColor: '#fff',
            borderWidth: 2,
          },
        },
      })),
    };
  });

  // 请求数据
  const fetchData = () => {
    loading.value = true;
    const params = {
      type: ratingType.value,
      peerId: StaticsStore.peerId,
      departmentCode: StaticsStore.departmentCode,
      startTime: StaticsStore.dateRang.startTime,
      endTime: StaticsStore.dateRang.endTime,
    };
    props.apiFn(params).then((res) => {
      if (res && res.code === 0 && res.data && res.data.xAxis?.length > 0) {
        chartData.value = res.data;
      } else {
        chartData.value = null;
      }
    }).catch((error) => {
      console.error('获取评分统计数据失败:', error);
      chartData.value = null;
    }).finally(() => {
      loading.value = false;
    });
  };

  // 监听筛选条件和评分类型变化
  watch(
    [() => StaticsStore.peerId, () => StaticsStore.departmentCode, () => StaticsStore.dateRang, ratingType],
    () => {
      fetchData();
    },
    { deep: true },
  );

  onMounted(() => {
    fetchData();
  });
</script>

<template>
  <el-card class="rating-stat-card" shadow="hover" v-loading="loading" ref="cardRef">
    <div class="rating-stat-header">
      <SectionHeader :title="title" />
      <div class="rating-type-tabs">
        <span
          v-for="option in ratingTypeOptions"
          :key="option.value"
          class="tab-btn"
          :class="{ active: ratingType === option.value }"
          @click="ratingType = option.value"
        >{{ option.label }}</span>
      </div>
    </div>
    <div class="rating-stat-chart" v-if="chartData">
      <BaseChart height="100%" :options="chartOptions" width="100%" />
    </div>
    <div class="rating-stat-empty" v-else-if="!loading">
      <el-empty description="暂无评分数据" :image-size="60" />
    </div>
  </el-card>
</template>

<style lang="less" scoped>
  .rating-stat-card {
    height: 100%;
    display: flex;
    flex-direction: column;

    :deep(.el-card__body) {
      flex: 1;
      display: flex;
      flex-direction: column;
      padding: 10px;
      min-height: 0;
    }
  }

  .rating-stat-header {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    flex-shrink: 0;

    .rating-type-tabs {
      flex-shrink: 0;
      display: inline-flex;
      align-items: center;
      gap: 7px;
      padding: 3px;
      background: rgba(247, 247, 247, 1);
      border-radius: 4px;

      .tab-btn {
        padding: 2px 6px;
        color: rgba(176, 176, 176, 1);
        font-size: 12px;
        font-weight: 400;
        line-height: 17px;
        cursor: pointer;
        transition: all 0.2s;

        &.active {
          border-radius: 2px;
          background: rgba(255, 255, 255, 1);
          color: rgba(90, 99, 131, 1);
        }
      }
    }
  }

  .rating-stat-chart {
    flex: 1;
    min-height: 0;
  }

  .rating-stat-empty {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 0;
    overflow: hidden;

    :deep(.el-empty) {
      padding: 10px 0;
    }

    :deep(.el-empty__description p) {
      font-size: 12px;
    }
  }
</style>
