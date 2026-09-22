<script setup lang="ts">
  import type { EChartsOption, PieSeriesOption } from 'echarts';

  import { computed, inject, ref, watch } from 'vue';

  import BaseChart from './BaseChart.vue';

  interface PieDataItem {
    itemStyle?: {
      color?: string;
    };
    name: string;
    value: number;
  }

  const props = withDefaults(defineProps<Props>(), {
    colors: () => [
      '#AC81E6',
      '#6EAAFF',
      '#64C5D9',
      '#72D174',
      '#F0BA5D',
      '#FC7E7E',
      '#F4B6DB',
      '#F7E14D',
    ],
    height: '100%',
    innerSeries: () => ({
      data: [],
      label: {
        fontSize: 12,
        position: 'inner',
        show: true,
      },
      labelLine: {
        show: false,
      },
      name: '内圈数据',
      radius: [0, '45%'],
    }),
    isSetLastItem: true,
    lastXAxisLabel: 'rgba(255, 0, 0, 1)',
    legend: () => ({
      position: 'bottom',
      show: false,
    }),
    outerSeries: () => ({
      data: [],
      name: '外圈数据',
      radius: ['45%', '75%'],
    }),
    pieTotal: undefined,
    title: '',
    tooltipFormatter: '{a} <br/>{b}: {c} ({d}%)',
    width: '100%',
  });
  // 创建默认主题
  const defaultTheme = ref('light');
  const themeRef = inject('theme', defaultTheme);

  // 监听主题变化
  watch(
    themeRef,
    (newTheme) => {
      defaultTheme.value = newTheme;
    },
    { immediate: true },
  );

  interface SeriesConfig {
    label?: PieSeriesOption['label'];
    labelLine?: PieSeriesOption['labelLine'];
    name?: string;
    radius?: [number | string, number | string];
  }

  interface Props {
    colors?: string[];
    height?: string;
    innerSeries?: {
      data: PieDataItem[];
    } & SeriesConfig;
    isSetLastItem?: boolean;
    lastXAxisLabel?: string;
    legend?: {
      data?: string[];
      position?: 'bottom' | 'left' | 'right' | 'top';
      show?: boolean;
    };
    outerSeries?: {
      data: PieDataItem[];
    } & SeriesConfig;
    pieTotal?: number;
    title?: string;
    tooltipFormatter?: ((params: any) => string) | string;
    width?: string;
  }

  const outerSeriesConfig = computed(() => ({
    data: props.outerSeries?.data || [],
    label: {
      formatter: '{b|{b}:}{c|{c}}{per|({d}%)}',
      position: 'outer',
      rich: {
        a: {
          align: 'center',
          color: '#6E7079',
          lineHeight: 22,
        },
        b: {
          color: defaultTheme.value === 'light' ? '#4C5058' : '#FFFFFF',
          fontSize: 12,
          lineHeight: 33,
        },
        c: {
          color: defaultTheme.value === 'light' ? '#4C5058' : '#FFFFFF',
          fontSize: 12,
          lineHeight: 33,
        },
        hr: {
          borderColor: '#8C8D8E',
          height: 0,
          width: '100%',
        },
        per: {
          borderRadius: 4,
          color: defaultTheme.value === 'light' ? '#4C5058' : '#FFFFFF',
          padding: [3, 4],
        },
      },
      show: true,
    },
    labelLine: {
      length: 15,
      show: true,
    },
    name: props.outerSeries?.name || '外圈数据',
    radius: props.outerSeries?.radius || ['45%', '75%'],
  }));

  const chartOptions = computed<EChartsOption>(() => {
    const innerColors = props.colors.slice(0, 4);
    const outerColors = props.colors.slice(2);

    // 计算外圈数据的总和，不包含内圈数据
    const outerTotal = props.outerSeries.data.reduce((sum, item) => sum + item.value, 0);
    const outerSeriesData = props.outerSeries.data.map((item, index) => {
      const isLastItem = index === props.outerSeries.data.length - 1;
      let percentage = '0%';
      if (outerTotal > 0) {
        const percentageValue = (item.value / outerTotal) * 100;
        percentage = percentageValue === 0 ? '0%' : `${percentageValue.toFixed(2)}%`;
      }

      // 为最后一个文本设置红色样式
      return isLastItem && props.isSetLastItem
        ? {
            ...item,
            itemStyle: {
              color: item.itemStyle?.color || outerColors[index % outerColors.length],
            },
            label: {
              ...outerSeriesConfig.value.label,
              formatter: `{lastName|${item.name}:} {lastValue|${item.value}} {lastPer|(${percentage})}`,
              rich: {
                ...outerSeriesConfig.value.label?.rich,
                lastName: {
                  color: defaultTheme.value === 'light' ? props.lastXAxisLabel : '#FFEB3B',
                  fontSize: 14,
                },
                lastPer: {
                  color: defaultTheme.value === 'light' ? props.lastXAxisLabel : '#FFEB3B',
                  fontSize: 14,
                },
                lastValue: {
                  color: defaultTheme.value === 'light' ? props.lastXAxisLabel : '#FFEB3B',
                  fontSize: 14,
                },
              },
            },
          }
        : {
            ...item,
            itemStyle: {
              color: item.itemStyle?.color || outerColors[index % outerColors.length],
            },
          };
    });

    return {
      legend: {
        ...props.legend,
        itemGap: 8,
        itemWidth: 20,
        pageIconColor: defaultTheme.value === 'light' ? '#333' : '#fff', // 设置箭头颜色
        pageIconInactiveColor: defaultTheme.value === 'light' ? '#ccc' : '#888', // 设置非活动箭头颜色
        pageIconSize: 8,
        pageTextStyle: {
          color: defaultTheme.value === 'light' ? '#333' : '#fff',
          fontSize: 10,
        },
        textStyle: {
          color: defaultTheme.value === 'light' ? '#333' : '#fff',
          fontSize: 12,
          padding: [0, 0, 0, 4],
        },
        type: 'scroll',
      },
      series: [
        {
          selectedMode: 'single',
          type: 'pie',
          ...props.innerSeries,
          center: ['50%', '55%'],
          data: props.innerSeries.data.map((item, index) => ({
            ...item,
            itemStyle: {
              color: item.itemStyle?.color || innerColors[index % innerColors.length],
            },
          })),
          radius: props.innerSeries.radius,
        },
        {
          type: 'pie',
          ...outerSeriesConfig.value,
          center: ['50%', '55%'],
          data: outerSeriesData,
          radius: outerSeriesConfig.value.radius,
        },
      ] as PieSeriesOption[],
      title: {
        left: 'center',
        text: props.title,
      },
      tooltip: {
        backgroundColor: defaultTheme.value === 'light' ? '#fff' : 'rgba(50, 50, 50, 0.8)',
        formatter: props.tooltipFormatter,
        textStyle: {
          color: defaultTheme.value === 'light' ? '#666' : '#fff',
          fontSize: 12,
        },
        trigger: 'item',
      },
    };
  });
</script>

<template>
  <BaseChart :height="height" :options="chartOptions" :width="width" />
</template>
