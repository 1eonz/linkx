<script setup lang="ts">
  import type { EChartsOption } from 'echarts';

  import { computed, inject, ref, watch } from 'vue';

  import BaseChart from './BaseChart.vue';

  const props = withDefaults(defineProps<Props>(), {
    barWidth: '60%',
    colors: () => ({
      background: 'rgba(246,248,250,0.8)',
      bar: '#1B61F0',
      label: '#FF7A7A',
    }),
    direction: 'vertical',
    height: '100%',
    maxLabelLength: 10, // 默认最大显示6个字符
    showLabel: true,
    title: '',
    unit: '', // 默认空字符串表示没有单位
    width: '100%',
    isShowAll: false,
  });
  // 创建默认主题
  const defaultTheme = ref('light');
  const themeRef = inject('theme', defaultTheme);

  // 监听主题变化,第一次渲染时执行
  watch(
    themeRef,
    (newTheme) => {
      console.log('主题变化为:', newTheme);
      defaultTheme.value = newTheme;
    },
    { immediate: true },
  );
  interface Props {
    barWidth?: number | string;
    colors?: {
      background?: string;
      bar?: string;
      label?: string;
    };
    data: {
      series: number[];
      xAxis: string[];
    };
    direction?: string;
    height?: string;
    isShowAll?: boolean;
    itemColors?: (
      | {
          colorStops: { color: string; offset: number }[];
          type: 'linear';
          x: number;
          x2: number;
          y: number;
          y2: number;
        }
      | string
      | undefined
    )[]; // 新增：为每个柱子指定颜色（支持字符串或渐变对象）
    labelColors?: (string | undefined)[]; // 新增：为每个标签指定颜色
    maxLabelLength?: number; // 新增属性：标签最大长度
    showLabel?: boolean;
    title?: string;
    unit?: string; // 新增单位参数
    width?: string;
  }

  // 截断文本
  const truncateText = (text: string, maxLength: number): string => {
    if (!text) return '';
    return text.length > maxLength ? `${text.slice(0, maxLength)}...` : text;
  };
  const initialRange = ref({ end: 100, start: 0 });
  const calculateInitialRange = () => {
    const dataLength = props.data.xAxis.length;
    const maxDisplayBars = 10;
    const shouldLog = dataLength > 20; // 只在数据量大时记录

    if (dataLength <= maxDisplayBars) {
      shouldLog && console.log('[Chart] 数据量少，显示全部');
      return { end: 100, start: 0 };
    }

    const start = 100 - (maxDisplayBars / dataLength) * 100;
    shouldLog &&
      console.log('[Chart] 计算显示范围', {
        dataLength,
        end: 100,
        start: Math.round(start),
      });
    return { end: 100, start };
  };
  watch(
    () => props.data.xAxis,
    (newXAxis) => {
      if (newXAxis.length === 0 || props.isShowAll) return;

      initialRange.value = calculateInitialRange();
    },
    { immediate: true },
  );

  const isHorizontal = props.direction === 'horizontal';
  let topValue;
  if (props.title) {
    topValue = '15%';
  } else {
    topValue = isHorizontal ? '2%' : '12%';
  }

  const chartOptions = computed<EChartsOption>(() => ({
    dataZoom: [
      {
        end: initialRange.value.end,
        filterMode: 'weakFilter', // 新增：保持轴标签
        [props.direction === 'horizontal' ? 'yAxisIndex' : 'xAxisIndex']: 0,
        start: initialRange.value.start,
        type: 'inside',
        zoomLock: true, // 新增：锁定缩放状态
        zoomOnMouseWheel: true,
      },
    ],
    grid: {
      bottom: isHorizontal ? '4%' : '2%',
      containLabel: true,
      left: '3%',
      right: isHorizontal ? '10%' : '4%',
      top: topValue,
    },
    responsive: true,
    series: [
      {
        backgroundStyle: {
          borderRadius: isHorizontal ? [0, 4, 4, 0] : [4, 4, 0, 0],
          color:
            defaultTheme.value === 'light' ? props.colors.background : 'rgba(255, 255, 255, 0.1)',
        },
        barCategoryGap: '20%', // 不同类目间的柱间距
        barGap: '30%', // 柱间间隔（同一系列的柱间距）
        barWidth: props.barWidth,
        data: props.data.series.map((value, index) => {
          const itemColor = props.itemColors?.[index];
          const defaultBarColor =
            defaultTheme.value === 'light'
              ? props.colors.bar
              : ({
                  colorStops: [
                    { color: 'rgba(26, 255, 251, 1)', offset: 0 },
                    { color: 'rgba(28, 125, 189, 1)', offset: 1 },
                  ],
                  type: 'linear' as const,
                  x: 0,
                  x2: 0,
                  y: 0,
                  y2: 1,
                } as any);
          const labelColor = props.labelColors?.[index];
          const defaultLabelColor = defaultTheme.value === 'light' ? props.colors.label : '#FFFFFF';

          const dataItem: any = {
            itemStyle: {
              borderRadius: isHorizontal ? [0, 4, 4, 0] : [4, 4, 0, 0],
              color: itemColor || defaultBarColor,
            },
            // 将原始名称存储在自定义属性中，供tooltip使用
            rawName: props.data.xAxis[index],
            value,
          };

          if (props.showLabel) {
            dataItem.label = {
              color: labelColor || defaultLabelColor,
              fontSize: 12,
              formatter: `${value}${props.unit}`,
              position: isHorizontal ? 'right' : 'top',
              show: true,
            };
          }

          return dataItem;
        }),
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowColor: 'rgba(0, 0, 0, 0.3)',
          },
        },
        name: props.title,
        showBackground: true,
        type: 'bar',
      },
    ],
    title: {
      left: 'center',
      text: props.title,
      textStyle: {
        color: '#333',
      },
    },
    tooltip: {
      axisPointer: {
        type: 'shadow',
      },
      backgroundColor: defaultTheme.value === 'light' ? '#fff' : 'rgba(50, 50, 50, 0.8)',
      // 限制tooltip的最大宽度
      extraCssText: 'max-width: 300px; white-space: normal;',
      formatter: (params: any) => {
        const paramArray = Array.isArray(params) ? params : [params];
        return paramArray
          .map((param: any) => {
            // 获取显示名称
            const displayName = param.data?.rawName || param.axisValue || param.name || '';
            // 获取数值，优先从data.value获取
            const value = param.data?.value ?? param.value ?? 0;
            const isOverdue = displayName === '逾期';
            const textColor = isOverdue
              ? defaultTheme.value === 'light'
                ? '#ff0000'
                : '#ffd700' // light: 红色, dark: 黄色
              : defaultTheme.value === 'light'
                ? '#666'
                : '#fff';
            // 获取柱子颜色，如果是渐变对象则使用第一个颜色
            const rawBarColor = param.data?.itemStyle?.color || props.colors.bar;
            const barColor =
              typeof rawBarColor === 'object' && rawBarColor?.colorStops
                ? rawBarColor.colorStops[0]?.color || props.colors.bar
                : rawBarColor;

            return `
              <div style="font-weight:bold;margin-bottom:5px;color:${textColor};word-wrap:break-word;word-break:break-all;max-width:300px;">${displayName}</div>
              <div style="color:${textColor};margin-bottom:3px;white-space:nowrap;">
                <span style="display:inline-block;width:10px;height:10px;background:${barColor};margin-right:5px;"></span>
                ${param.seriesName || ''}: ${value}${props.unit}
              </div>
            `;
          })
          .join('');
      },
      textStyle: {
        color: defaultTheme.value === 'light' ? '#666' : '#fff',
        fontSize: 12,
      },
      trigger: 'axis',
    },
    xAxis: isHorizontal
      ? {
          axisLabel: {
            color: defaultTheme.value === 'light' ? '#666' : '#FFFFFF',
            fontSize: 12,
            formatter: (value: string) => `${value}`, // x轴标签显示单位
          },
          axisLine: {
            lineStyle: { color: defaultTheme.value === 'light' ? '#E5E7EB' : 'rgba(3, 47, 89, 1)' },
          },
          minInterval: 1,
          splitLine: {
            lineStyle: {
              color: defaultTheme.value === 'light' ? '#F3F4F6' : 'rgba(3, 47, 89, 1)',
              type: 'dashed',
            },
          },
          splitNumber: 3,
          type: 'value',
        }
      : {
          axisLabel: {
            color: defaultTheme.value === 'light' ? '#666' : '#FFFFFF',
            fontSize: 12,
            formatter: (value: string) => truncateText(value, props.maxLabelLength), // 截断x轴标签
            // rotate: 10, // 旋转标签避免重叠
          },
          axisLine: {
            lineStyle: { color: defaultTheme.value === 'light' ? '#E5E7EB' : 'rgba(3, 47, 89, 1)' },
          },
          axisTick: { alignWithLabel: true },
          data: props.data.xAxis.map((item) => truncateText(item, props.maxLabelLength)), // 截断x轴数据
          type: 'category',
        },
    yAxis: isHorizontal
      ? {
          axisLabel: {
            color: defaultTheme.value === 'light' ? '#666' : '#FFFFFF',
            fontSize: 12,
            formatter: (value: string) => truncateText(value, props.maxLabelLength), // 截断y轴标签
          },
          axisLine: {
            lineStyle: { color: defaultTheme.value === 'light' ? '#E5E7EB' : 'rgba(3, 47, 89, 1)' },
            show: true,
          },
          data: props.data.xAxis.map((item) => truncateText(item, props.maxLabelLength)), // 截断y轴数据
          type: 'category',
        }
      : {
          axisLabel: {
            color: defaultTheme.value === 'light' ? '#666' : '#FFFFFF',
            fontSize: 12,
            formatter: (value: number) => `${value}${props.unit}`, // y轴标签显示单位
          },
          // splitNumber: 10,
          axisLine: {
            lineStyle: { color: defaultTheme.value === 'light' ? '#E5E7EB' : 'rgba(3, 47, 89, 1)' },
            show: true,
          },
          minInterval: 1,
          splitLine: {
            lineStyle: {
              color: defaultTheme.value === 'light' ? '#F3F4F6' : 'rgba(3, 47, 89, 1)',
              type: 'dashed',
            },
          },
          type: 'value',
        },
  }));
</script>

<template>
  <BaseChart :height="height" :options="chartOptions" :width="width" />
</template>
